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

#ifndef ART_RUNTIME_ARCH_RISCV64_ASM_SUPPORT_RISCV64_S_
#define ART_RUNTIME_ARCH_RISCV64_ASM_SUPPORT_RISCV64_S_

#include "asm_support_riscv64.h"

// Define special registers.

// Register holding Thread::Current().
#define xSELF s1

#endif  // ART_RUNTIME_ARCH_RISCV64_ASM_SUPPORT_RISCV64_S_