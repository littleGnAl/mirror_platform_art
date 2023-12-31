/*
 * Copyright (C) 2013 The Android Open Source Project
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

#ifndef ART_COMPILER_TRAMPOLINES_TRAMPOLINE_COMPILER_H_
#define ART_COMPILER_TRAMPOLINES_TRAMPOLINE_COMPILER_H_

#include <stdint.h>
#include <memory>
#include <vector>

#include "arch/instruction_set.h"
#include "base/macros.h"
#include "offsets.h"

namespace art HIDDEN {

enum EntryPointCallingConvention {
  // ABI of calls to a method's native code, only used for native methods.
  kJniAbi,
  // ABI of calls to a method's quick code entry point.
  kQuickAbi
};

// Create code that will invoke the function held in thread local storage.
EXPORT std::unique_ptr<const std::vector<uint8_t>> CreateTrampoline32(
    InstructionSet isa, EntryPointCallingConvention abi, ThreadOffset32 entry_point_offset);
EXPORT std::unique_ptr<const std::vector<uint8_t>> CreateTrampoline64(
    InstructionSet isa, EntryPointCallingConvention abi, ThreadOffset64 entry_point_offset);

}  // namespace art

#endif  // ART_COMPILER_TRAMPOLINES_TRAMPOLINE_COMPILER_H_
