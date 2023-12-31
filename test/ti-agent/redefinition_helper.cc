/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "common_helper.h"

#include <cstdio>
#include <deque>
#include <map>
#include <sstream>
#include <string>
#include <vector>

#include "jni.h"
#include "jvmti.h"

#include "jvmti_helper.h"
#include "test_env.h"

namespace art {

enum class RedefineType {
  kNormal,
  kStructural,
};

static void SetupCommonRedefine();
static void SetupCommonRetransform(RedefineType type);
static void SetupCommonTransform();
template <bool is_redefine>
static void throwCommonRedefinitionError(jvmtiEnv* jvmti,
                                         JNIEnv* env,
                                         jint num_targets,
                                         jclass* target,
                                         jvmtiError res) {
  // Get the last error message which might give more details on what went wrong.
  using GetLastError = jvmtiError (*)(jvmtiEnv* env, char** msg);
  GetLastError get_last_error =
      GetExtensionFunction<GetLastError>(env, jvmti, "com.android.art.misc.get_last_error_message");
  char* error_msg = nullptr;
  if (get_last_error != nullptr) {
    get_last_error(jvmti_env, &error_msg);
  }

  std::stringstream err;
  char* error = nullptr;
  jvmti->GetErrorName(res, &error);
  err << "Failed to " << (is_redefine ? "redefine" : "retransform") << " class";
  if (num_targets > 1) {
    err << "es";
  }
  err << " <";
  for (jint i = 0; i < num_targets; i++) {
    char* signature = nullptr;
    char* generic = nullptr;
    jvmti->GetClassSignature(target[i], &signature, &generic);
    if (i != 0) {
      err << ", ";
    }
    err << signature;
    jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
    jvmti->Deallocate(reinterpret_cast<unsigned char*>(generic));
  }
  err << "> due to " << error;
  if (error_msg != nullptr) {
    err << " (" << error_msg << ")";
  }
  std::string message = err.str();
  jvmti->Deallocate(reinterpret_cast<unsigned char*>(error));
  jvmti->Deallocate(reinterpret_cast<unsigned char*>(error_msg));
  env->ThrowNew(env->FindClass("java/lang/Exception"), message.c_str());
}

#define CONFIGURATION_COMMON_REDEFINE 0
#define CONFIGURATION_COMMON_RETRANSFORM 1
#define CONFIGURATION_COMMON_TRANSFORM 2
#define CONFIGURATION_STRUCTURAL_TRANSFORM 3

extern "C" JNIEXPORT void JNICALL Java_art_Redefinition_nativeSetTestConfiguration(JNIEnv*,
                                                                                   jclass,
                                                                                   jint type) {
  switch (type) {
    case CONFIGURATION_COMMON_REDEFINE: {
      SetupCommonRedefine();
      return;
    }
    case CONFIGURATION_COMMON_RETRANSFORM: {
      SetupCommonRetransform(RedefineType::kNormal);
      return;
    }
    case CONFIGURATION_COMMON_TRANSFORM: {
      SetupCommonTransform();
      return;
    }
    case CONFIGURATION_STRUCTURAL_TRANSFORM: {
      SetupCommonRetransform(RedefineType::kStructural);
      return;
    }
    default: {
      LOG(FATAL) << "Unknown test configuration: " << type;
    }
  }
}

template<RedefineType kType>
static bool SupportsAndIsJVM() {
  if constexpr (kType == RedefineType::kStructural) {
    return false;
  } else {
    return IsJVM();
  }
}


namespace common_redefine {

template <RedefineType kType>
static jvmtiError CallRedefineEntrypoint(JNIEnv* env,
                                         jvmtiEnv* jvmti,
                                         jint num_defs,
                                         const jvmtiClassDefinition* defs) {
  decltype(jvmti->functions->RedefineClasses) entrypoint = nullptr;
  if constexpr (kType == RedefineType::kNormal) {
    entrypoint = jvmti->functions->RedefineClasses;
  } else {
    entrypoint = GetExtensionFunction<decltype(entrypoint)>(
        env, jvmti_env, "com.android.art.class.structurally_redefine_classes");
  }
  if (entrypoint == nullptr) {
    LOG(INFO) << "Could not find entrypoint!";
    return JVMTI_ERROR_NOT_AVAILABLE;
  }
  return entrypoint(jvmti, num_defs, defs);
}

static void throwRedefinitionError(jvmtiEnv* jvmti,
                                   JNIEnv* env,
                                   jint num_targets,
                                   jclass* target,
                                   jvmtiError res) {
  return throwCommonRedefinitionError<true>(jvmti, env, num_targets, target, res);
}

template<RedefineType kType>
static void DoMultiClassRedefine(jvmtiEnv* jvmti_env,
                                 JNIEnv* env,
                                 jint num_redefines,
                                 jclass* targets,
                                 jbyteArray* class_file_bytes,
                                 jbyteArray* dex_file_bytes) {
  std::vector<jvmtiClassDefinition> defs;
  for (jint i = 0; i < num_redefines; i++) {
    jbyteArray desired_array = SupportsAndIsJVM<kType>() ? class_file_bytes[i] : dex_file_bytes[i];
    jint len = static_cast<jint>(env->GetArrayLength(desired_array));
    const unsigned char* redef_bytes = reinterpret_cast<const unsigned char*>(
        env->GetByteArrayElements(desired_array, nullptr));
    defs.push_back({targets[i], static_cast<jint>(len), redef_bytes});
  }
  jvmtiError res = CallRedefineEntrypoint<kType>(env, jvmti_env, num_redefines, defs.data());
  if (res != JVMTI_ERROR_NONE) {
    throwRedefinitionError(jvmti_env, env, num_redefines, targets, res);
  }
}

template<RedefineType kType>
static void DoClassRedefine(jvmtiEnv* jvmti_env,
                            JNIEnv* env,
                            jclass target,
                            jbyteArray class_file_bytes,
                            jbyteArray dex_file_bytes) {
  return DoMultiClassRedefine<kType>(jvmti_env, env, 1, &target, &class_file_bytes, &dex_file_bytes);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_art_Redefinition_isStructurallyModifiable(JNIEnv* env, jclass, jclass target) {
  using ArtCanStructurallyRedefineClass =
      jvmtiError (*)(jvmtiEnv * env, jclass k, jboolean * result);
  ArtCanStructurallyRedefineClass can_redef = GetExtensionFunction<ArtCanStructurallyRedefineClass>(
      env, jvmti_env, "com.android.art.class.is_structurally_modifiable_class");
  if (can_redef == nullptr || env->ExceptionCheck()) {
    return false;
  }
  jboolean result = false;
  JvmtiErrorToException(env, jvmti_env, can_redef(jvmti_env, target, &result));
  return result;
}

extern "C" JNIEXPORT void JNICALL Java_art_Redefinition_doCommonStructuralClassRedefinition(
    JNIEnv* env, jclass, jclass target, jbyteArray dex_file_bytes) {
  DoClassRedefine<RedefineType::kStructural>(jvmti_env, env, target, nullptr, dex_file_bytes);
}

// Magic JNI export that classes can use for redefining classes.
// To use classes should declare this as a native function with signature (Ljava/lang/Class;[B[B)V
extern "C" JNIEXPORT void JNICALL Java_art_Redefinition_doCommonClassRedefinition(
    JNIEnv* env, jclass, jclass target, jbyteArray class_file_bytes, jbyteArray dex_file_bytes) {
  DoClassRedefine<RedefineType::kNormal>(jvmti_env, env, target, class_file_bytes, dex_file_bytes);
}

// Magic JNI export that classes can use for redefining classes.
// To use classes should declare this as a native function with signature
// ([Ljava/lang/Class;[[B[[B)V
extern "C" JNIEXPORT void JNICALL Java_art_Redefinition_doCommonMultiStructuralClassRedefinition(
    JNIEnv* env,
    jclass,
    jobjectArray targets,
    jobjectArray dex_file_bytes) {
  std::vector<jclass> classes;
  std::vector<jbyteArray> class_files;
  std::vector<jbyteArray> dex_files;
  jint len = env->GetArrayLength(targets);
  if (len != env->GetArrayLength(dex_file_bytes)) {
    env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                  "the three array arguments passed to this function have different lengths!");
    return;
  }
  for (jint i = 0; i < len; i++) {
    classes.push_back(static_cast<jclass>(env->GetObjectArrayElement(targets, i)));
    dex_files.push_back(static_cast<jbyteArray>(env->GetObjectArrayElement(dex_file_bytes, i)));
    class_files.push_back(nullptr);
  }
  return DoMultiClassRedefine<RedefineType::kStructural>(jvmti_env,
                                                         env,
                                                         len,
                                                         classes.data(),
                                                         class_files.data(),
                                                         dex_files.data());
}

// Magic JNI export that classes can use for redefining classes.
// To use classes should declare this as a native function with signature
// ([Ljava/lang/Class;[[B[[B)V
extern "C" JNIEXPORT void JNICALL Java_art_Redefinition_doCommonMultiClassRedefinition(
    JNIEnv* env,
    jclass,
    jobjectArray targets,
    jobjectArray class_file_bytes,
    jobjectArray dex_file_bytes) {
  std::vector<jclass> classes;
  std::vector<jbyteArray> class_files;
  std::vector<jbyteArray> dex_files;
  jint len = env->GetArrayLength(targets);
  if (len != env->GetArrayLength(class_file_bytes) || len != env->GetArrayLength(dex_file_bytes)) {
    env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"),
                  "the three array arguments passed to this function have different lengths!");
    return;
  }
  for (jint i = 0; i < len; i++) {
    classes.push_back(static_cast<jclass>(env->GetObjectArrayElement(targets, i)));
    dex_files.push_back(static_cast<jbyteArray>(env->GetObjectArrayElement(dex_file_bytes, i)));
    class_files.push_back(static_cast<jbyteArray>(env->GetObjectArrayElement(class_file_bytes, i)));
  }
  return DoMultiClassRedefine<RedefineType::kNormal>(jvmti_env,
                                                     env,
                                                     len,
                                                     classes.data(),
                                                     class_files.data(),
                                                     dex_files.data());
}

// Get all capabilities except those related to retransformation.
jint OnLoad(JavaVM* vm,
            [[maybe_unused]] char* options,
            [[maybe_unused]] void* reserved) {
  if (vm->GetEnv(reinterpret_cast<void**>(&jvmti_env), JVMTI_VERSION_1_0)) {
    printf("Unable to get jvmti env!\n");
    return 1;
  }
  SetupCommonRedefine();
  return 0;
}

}  // namespace common_redefine

namespace common_retransform {

struct CommonTransformationResult {
  std::vector<unsigned char> class_bytes;
  std::vector<unsigned char> dex_bytes;

  CommonTransformationResult(size_t class_size, size_t dex_size)
      : class_bytes(class_size), dex_bytes(dex_size) {}

  CommonTransformationResult() = default;
  CommonTransformationResult(CommonTransformationResult&&) = default;
  CommonTransformationResult(CommonTransformationResult&) = default;
};

// Map from class name to transformation result.
std::map<std::string, std::deque<CommonTransformationResult>> gTransformations;
bool gPopTransformations = true;

extern "C" JNIEXPORT void JNICALL Java_art_Redefinition_addCommonTransformationResult(
    JNIEnv* env, jclass, jstring class_name, jbyteArray class_array, jbyteArray dex_array) {
  const char* name_chrs = env->GetStringUTFChars(class_name, nullptr);
  std::string name_str(name_chrs);
  env->ReleaseStringUTFChars(class_name, name_chrs);
  CommonTransformationResult trans(env->GetArrayLength(class_array),
                                   env->GetArrayLength(dex_array));
  if (env->ExceptionOccurred()) {
    return;
  }
  env->GetByteArrayRegion(class_array,
                          0,
                          env->GetArrayLength(class_array),
                          reinterpret_cast<jbyte*>(trans.class_bytes.data()));
  if (env->ExceptionOccurred()) {
    return;
  }
  env->GetByteArrayRegion(dex_array,
                          0,
                          env->GetArrayLength(dex_array),
                          reinterpret_cast<jbyte*>(trans.dex_bytes.data()));
  if (env->ExceptionOccurred()) {
    return;
  }
  if (gTransformations.find(name_str) == gTransformations.end()) {
    std::deque<CommonTransformationResult> list;
    gTransformations[name_str] = std::move(list);
  }
  gTransformations[name_str].push_back(std::move(trans));
}

// The hook we are using.
void JNICALL CommonClassFileLoadHookRetransformable(jvmtiEnv* jvmti_env,
                                                    [[maybe_unused]] JNIEnv* jni_env,
                                                    [[maybe_unused]] jclass class_being_redefined,
                                                    [[maybe_unused]] jobject loader,
                                                    const char* name,
                                                    [[maybe_unused]] jobject protection_domain,
                                                    [[maybe_unused]] jint class_data_len,
                                                    [[maybe_unused]] const unsigned char* class_dat,
                                                    jint* new_class_data_len,
                                                    unsigned char** new_class_data) {
  std::string name_str(name);
  if (gTransformations.find(name_str) != gTransformations.end() &&
      gTransformations[name_str].size() > 0) {
    CommonTransformationResult& res = gTransformations[name_str][0];
    const std::vector<unsigned char>& desired_array = IsJVM() ? res.class_bytes : res.dex_bytes;
    unsigned char* new_data;
    CHECK_EQ(JVMTI_ERROR_NONE, jvmti_env->Allocate(desired_array.size(), &new_data));
    memcpy(new_data, desired_array.data(), desired_array.size());
    *new_class_data = new_data;
    *new_class_data_len = desired_array.size();
    if (gPopTransformations) {
      gTransformations[name_str].pop_front();
    }
  }
}

extern "C" JNIEXPORT void Java_art_Redefinition_setPopRetransformations(JNIEnv*,
                                                                        jclass,
                                                                        jboolean enable) {
  gPopTransformations = enable;
}

extern "C" JNIEXPORT void Java_art_Redefinition_popTransformationFor(JNIEnv* env,
                                                                         jclass,
                                                                         jstring class_name) {
  const char* name_chrs = env->GetStringUTFChars(class_name, nullptr);
  std::string name_str(name_chrs);
  env->ReleaseStringUTFChars(class_name, name_chrs);
  if (gTransformations.find(name_str) != gTransformations.end() &&
      gTransformations[name_str].size() > 0) {
    gTransformations[name_str].pop_front();
  } else {
    std::stringstream err;
    err << "No transformations found for class " << name_str;
    std::string message = err.str();
    env->ThrowNew(env->FindClass("java/lang/Exception"), message.c_str());
  }
}

extern "C" JNIEXPORT void Java_art_Redefinition_enableCommonRetransformation(JNIEnv* env,
                                                                                 jclass,
                                                                                 jboolean enable) {
  jvmtiError res = jvmti_env->SetEventNotificationMode(enable ? JVMTI_ENABLE : JVMTI_DISABLE,
                                                       JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
                                                       nullptr);
  if (res != JVMTI_ERROR_NONE) {
    JvmtiErrorToException(env, jvmti_env, res);
  }
}

static void throwRetransformationError(jvmtiEnv* jvmti,
                                       JNIEnv* env,
                                       jint num_targets,
                                       jclass* targets,
                                       jvmtiError res) {
  return throwCommonRedefinitionError<false>(jvmti, env, num_targets, targets, res);
}

static void DoClassRetransformation(jvmtiEnv* jvmti_env, JNIEnv* env, jobjectArray targets) {
  std::vector<jclass> classes;
  jint len = env->GetArrayLength(targets);
  classes.reserve(len);
  for (jint i = 0; i < len; i++) {
    classes.push_back(static_cast<jclass>(env->GetObjectArrayElement(targets, i)));
  }
  jvmtiError res = jvmti_env->RetransformClasses(len, classes.data());
  if (res != JVMTI_ERROR_NONE) {
    throwRetransformationError(jvmti_env, env, len, classes.data(), res);
  }
}

extern "C" JNIEXPORT void JNICALL Java_art_Redefinition_doCommonClassRetransformation(
    JNIEnv* env, jclass, jobjectArray targets) {
  jvmtiCapabilities caps;
  jvmtiError caps_err = jvmti_env->GetCapabilities(&caps);
  if (caps_err != JVMTI_ERROR_NONE) {
    env->ThrowNew(env->FindClass("java/lang/Exception"),
                  "Unable to get current jvmtiEnv capabilities");
    return;
  }

  // Allocate a new environment if we don't have the can_retransform_classes capability needed to
  // call the RetransformClasses function.
  jvmtiEnv* real_env = nullptr;
  if (caps.can_retransform_classes != 1) {
    JavaVM* vm = nullptr;
    if (env->GetJavaVM(&vm) != 0 ||
        vm->GetEnv(reinterpret_cast<void**>(&real_env), JVMTI_VERSION_1_0) != 0) {
      env->ThrowNew(env->FindClass("java/lang/Exception"),
                    "Unable to create temporary jvmtiEnv for RetransformClasses call.");
      return;
    }
    SetStandardCapabilities(real_env);
  } else {
    real_env = jvmti_env;
  }
  DoClassRetransformation(real_env, env, targets);
  if (caps.can_retransform_classes != 1) {
    real_env->DisposeEnvironment();
  }
}

// Get all capabilities except those related to retransformation.
jint OnLoad(JavaVM* vm,
            [[maybe_unused]] char* options,
            [[maybe_unused]] void* reserved) {
  if (vm->GetEnv(reinterpret_cast<void**>(&jvmti_env), JVMTI_VERSION_1_0)) {
    printf("Unable to get jvmti env!\n");
    return 1;
  }
  SetupCommonRetransform(RedefineType::kNormal);
  return 0;
}

}  // namespace common_retransform

namespace common_transform {

// Get all capabilities except those related to retransformation.
jint OnLoad(JavaVM* vm,
            [[maybe_unused]] char* options,
            [[maybe_unused]] void* reserved) {
  if (vm->GetEnv(reinterpret_cast<void**>(&jvmti_env), JVMTI_VERSION_1_0)) {
    printf("Unable to get jvmti env!\n");
    return 1;
  }
  SetupCommonTransform();
  return 0;
}

}  // namespace common_transform

static void SetupCommonRedefine() {
  jvmtiCapabilities caps = GetStandardCapabilities();
  caps.can_retransform_classes = 0;
  caps.can_retransform_any_class = 0;
  jvmti_env->AddCapabilities(&caps);
}

static void SetupCommonRetransform(RedefineType type) {
  SetStandardCapabilities(jvmti_env);
  if (type == RedefineType::kNormal) {
    current_callbacks.ClassFileLoadHook =
        common_retransform::CommonClassFileLoadHookRetransformable;
    jvmtiError res = jvmti_env->SetEventCallbacks(&current_callbacks, sizeof(current_callbacks));
    CHECK_EQ(res, JVMTI_ERROR_NONE);
  } else {
    jvmtiError res = jvmti_env->SetExtensionEventCallback(
        GetExtensionEventId(jvmti_env, "com.android.art.class.structural_dex_file_load_hook"),
        reinterpret_cast<jvmtiExtensionEvent>(
            common_retransform::CommonClassFileLoadHookRetransformable));
    CHECK_EQ(res, JVMTI_ERROR_NONE);
  }
  common_retransform::gTransformations.clear();
}

static void SetupCommonTransform() {
  // Don't set the retransform caps
  jvmtiCapabilities caps = GetStandardCapabilities();
  caps.can_retransform_classes = 0;
  caps.can_retransform_any_class = 0;
  jvmti_env->AddCapabilities(&caps);

  // Use the same callback as the retransform test.
  current_callbacks.ClassFileLoadHook = common_retransform::CommonClassFileLoadHookRetransformable;
  jvmtiError res = jvmti_env->SetEventCallbacks(&current_callbacks, sizeof(current_callbacks));
  CHECK_EQ(res, JVMTI_ERROR_NONE);
  common_retransform::gTransformations.clear();
}

}  // namespace art
