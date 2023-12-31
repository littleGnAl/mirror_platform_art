/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.art;

/**
 * Represents the path to the runtime artifacts of a dex file (i.e., ART files generated by the
 * runtime, not by dexopt).
 *
 * @hide
 */
parcelable RuntimeArtifactsPath {
    /** The name of the package. */
    @utf8InCpp String packageName;
    /** The absolute path starting with '/' to the dex file (i.e., APK or JAR file). */
    @utf8InCpp String dexPath;
    /** The instruction set of the dexopt artifacts. */
    @utf8InCpp String isa;
}
