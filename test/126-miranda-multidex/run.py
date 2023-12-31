#!/bin/bash
#
# Copyright (C) 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys


def run(ctx, args):
  ctx.default_run(args)

  # The problem was first exposed in a no-verify setting, as that changes the resolution path
  # taken. Make sure we also test in that environment.
  ctx.default_run(args, verify=False)

  line = "OpenJDK 64-Bit Server VM warning: Options -Xverify:none and -noverify were deprecated in JDK 13 and will likely be removed in a future release."
  ctx.run(fr"sed -i -E '/{line}/d' '{args.stderr_file}'")
