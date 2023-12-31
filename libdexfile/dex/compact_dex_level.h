/*
 * Copyright (C) 2017 The Android Open Source Project
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

#ifndef ART_LIBDEXFILE_DEX_COMPACT_DEX_LEVEL_H_
#define ART_LIBDEXFILE_DEX_COMPACT_DEX_LEVEL_H_

#include <string>

#include "dex_file.h"

namespace art {

// Optimization level for compact dex generation.
// TODO(b/256664509): Clean this up.
enum class CompactDexLevel {
  // Level none means not generated.
  kCompactDexLevelNone,
  // Level fast means optimizations that don't take many resources to perform.
  kCompactDexLevelFast,
};

#ifdef ART_DEFAULT_COMPACT_DEX_LEVEL
#define ART_DEFAULT_COMPACT_DEX_LEVEL_VALUE_fast CompactDexLevel::kCompactDexLevelFast
#define ART_DEFAULT_COMPACT_DEX_LEVEL_VALUE_none CompactDexLevel::kCompactDexLevelNone

#define ART_DEFAULT_COMPACT_DEX_LEVEL_DEFAULT APPEND_TOKENS_AFTER_EVAL( \
    ART_DEFAULT_COMPACT_DEX_LEVEL_VALUE_, \
    ART_DEFAULT_COMPACT_DEX_LEVEL)

static_assert(ART_DEFAULT_COMPACT_DEX_LEVEL_DEFAULT == CompactDexLevel::kCompactDexLevelNone,
              "ART_DEFAULT_COMPACT_DEX_LEVEL_DEFAULT != none is no longer supported");
#endif

}  // namespace art

#endif  // ART_LIBDEXFILE_DEX_COMPACT_DEX_LEVEL_H_
