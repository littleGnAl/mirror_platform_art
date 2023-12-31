/*
 * Copyright (C) 2015 The Android Open Source Project
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

public class Main {

  /// CHECK-START: int Main.$noinline$BoolCond_IntVarVar(boolean, int, int) register (after)
  /// CHECK:               Select [{{i\d+}},{{i\d+}},{{z\d+}}]

  /// CHECK-START-ARM64: int Main.$noinline$BoolCond_IntVarVar(boolean, int, int) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            csel ne

  /// CHECK-START-X86_64: int Main.$noinline$BoolCond_IntVarVar(boolean, int, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  /// CHECK-START-X86: int Main.$noinline$BoolCond_IntVarVar(boolean, int, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  public static int $noinline$BoolCond_IntVarVar(boolean cond, int x, int y) {
    return cond ? x : y;
  }

  /// CHECK-START: int Main.$noinline$BoolCond_IntVarCst(boolean, int) register (after)
  /// CHECK:               Select [{{i\d+}},{{i\d+}},{{z\d+}}]

  /// CHECK-START-ARM64: int Main.$noinline$BoolCond_IntVarCst(boolean, int) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            csinc ne

  /// CHECK-START-X86_64: int Main.$noinline$BoolCond_IntVarCst(boolean, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  /// CHECK-START-X86: int Main.$noinline$BoolCond_IntVarCst(boolean, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  public static int $noinline$BoolCond_IntVarCst(boolean cond, int x) {
    return cond ? x : 1;
  }

  /// CHECK-START: int Main.$noinline$BoolCond_IntCstVar(boolean, int) register (after)
  /// CHECK:               Select [{{i\d+}},{{i\d+}},{{z\d+}}]

  /// CHECK-START-ARM64: int Main.$noinline$BoolCond_IntCstVar(boolean, int) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            csinc eq

  /// CHECK-START-X86_64: int Main.$noinline$BoolCond_IntCstVar(boolean, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  /// CHECK-START-X86: int Main.$noinline$BoolCond_IntCstVar(boolean, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  public static int $noinline$BoolCond_IntCstVar(boolean cond, int y) {
    return cond ? 1 : y;
  }

  /// CHECK-START: long Main.$noinline$BoolCond_LongVarVar(boolean, long, long) register (after)
  /// CHECK:               Select [{{j\d+}},{{j\d+}},{{z\d+}}]

  /// CHECK-START-ARM64: long Main.$noinline$BoolCond_LongVarVar(boolean, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            csel ne

  /// CHECK-START-X86_64: long Main.$noinline$BoolCond_LongVarVar(boolean, long, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/neq

  /// CHECK-START-X86: long Main.$noinline$BoolCond_LongVarVar(boolean, long, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne
  /// CHECK-NEXT:                     cmovnz/ne

  public static long $noinline$BoolCond_LongVarVar(boolean cond, long x, long y) {
    return cond ? x : y;
  }

  /// CHECK-START: long Main.$noinline$BoolCond_LongVarCst(boolean, long) register (after)
  /// CHECK:               Select [{{j\d+}},{{j\d+}},{{z\d+}}]

  /// CHECK-START-ARM64: long Main.$noinline$BoolCond_LongVarCst(boolean, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            csinc ne

  /// CHECK-START-X86_64: long Main.$noinline$BoolCond_LongVarCst(boolean, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/neq

  /// CHECK-START-X86: long Main.$noinline$BoolCond_LongVarCst(boolean, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne
  /// CHECK-NEXT:                     cmovnz/ne

  public static long $noinline$BoolCond_LongVarCst(boolean cond, long x) {
    return cond ? x : 1L;
  }

  /// CHECK-START: long Main.$noinline$BoolCond_LongCstVar(boolean, long) register (after)
  /// CHECK:               Select [{{j\d+}},{{j\d+}},{{z\d+}}]

  /// CHECK-START-ARM64: long Main.$noinline$BoolCond_LongCstVar(boolean, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            csinc eq

  /// CHECK-START-X86_64: long Main.$noinline$BoolCond_LongCstVar(boolean, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/neq

  /// CHECK-START-X86: long Main.$noinline$BoolCond_LongCstVar(boolean, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne
  /// CHECK-NEXT:                     cmovnz/ne

  public static long $noinline$BoolCond_LongCstVar(boolean cond, long y) {
    return cond ? 1L : y;
  }

  /// CHECK-START: float Main.$noinline$BoolCond_FloatVarVar(boolean, float, float) register (after)
  /// CHECK:               Select [{{f\d+}},{{f\d+}},{{z\d+}}]

  /// CHECK-START-ARM64: float Main.$noinline$BoolCond_FloatVarVar(boolean, float, float) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            fcsel ne

  public static float $noinline$BoolCond_FloatVarVar(boolean cond, float x, float y) {
    return cond ? x : y;
  }

  /// CHECK-START: float Main.$noinline$BoolCond_FloatVarCst(boolean, float) register (after)
  /// CHECK:               Select [{{f\d+}},{{f\d+}},{{z\d+}}]

  /// CHECK-START-ARM64: float Main.$noinline$BoolCond_FloatVarCst(boolean, float) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            fcsel ne

  public static float $noinline$BoolCond_FloatVarCst(boolean cond, float x) {
    return cond ? x : 1.0f;
  }

  /// CHECK-START: float Main.$noinline$BoolCond_FloatCstVar(boolean, float) register (after)
  /// CHECK:               Select [{{f\d+}},{{f\d+}},{{z\d+}}]

  /// CHECK-START-ARM64: float Main.$noinline$BoolCond_FloatCstVar(boolean, float) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            fcsel ne

  public static float $noinline$BoolCond_FloatCstVar(boolean cond, float y) {
    return cond ? 1.0f : y;
  }

  /// CHECK-START: int Main.$noinline$IntNonmatCond_IntVarVar(int, int, int, int) register (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK-NEXT:                     Select [{{i\d+}},{{i\d+}},<<Cond>>]

  /// CHECK-START-ARM64: int Main.$noinline$IntNonmatCond_IntVarVar(int, int, int, int) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            csel le

  /// CHECK-START-X86_64: int Main.$noinline$IntNonmatCond_IntVarVar(int, int, int, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK-NEXT:                     Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovle/ng

  /// CHECK-START-X86: int Main.$noinline$IntNonmatCond_IntVarVar(int, int, int, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK-NEXT:                     Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovle/ng

  public static int $noinline$IntNonmatCond_IntVarVar(int a, int b, int x, int y) {
    return a > b ? x : y;
  }

  /// CHECK-START: int Main.$noinline$IntMatCond_IntVarVar(int, int, int, int) register (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK-NEXT:       <<Sel:i\d+>>  Select [{{i\d+}},{{i\d+}},{{z\d+}}]
  /// CHECK-NEXT:                     Add [<<Cond>>,<<Sel>>]

  /// CHECK-START-ARM64: int Main.$noinline$IntMatCond_IntVarVar(int, int, int, int) disassembly (after)
  /// CHECK:               LessThanOrEqual
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            cset le
  /// CHECK:               Select
  /// CHECK-NEXT:            csel le

  /// CHECK-START-X86_64: int Main.$noinline$IntMatCond_IntVarVar(int, int, int, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovle/ng

  /// CHECK-START-X86: int Main.$noinline$IntMatCond_IntVarVar(int, int, int, int) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovle/ng

  public static int $noinline$IntMatCond_IntVarVar(int a, int b, int x, int y) {
    int result = (a > b ? x : y);
    return result + (a > b ? 0 : 1);
  }

  /// CHECK-START: long Main.$noinline$IntNonmatCond_LongVarVar(int, int, long, long) register (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK-NEXT:                     Select [{{j\d+}},{{j\d+}},<<Cond>>]

  /// CHECK-START-ARM64: long Main.$noinline$IntNonmatCond_LongVarVar(int, int, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            csel le

  /// CHECK-START-X86_64: long Main.$noinline$IntNonmatCond_LongVarVar(int, int, long, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK-NEXT:                     Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovle/ngq

  /// CHECK-START-X86: long Main.$noinline$IntNonmatCond_LongVarVar(int, int, long, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK-NEXT:                     Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovle/ng
  /// CHECK-NEXT:                     cmovle/ng

  public static long $noinline$IntNonmatCond_LongVarVar(int a, int b, long x, long y) {
    return a > b ? x : y;
  }

  /// CHECK-START: long Main.$noinline$IntMatCond_LongVarVar(int, int, long, long) register (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK:            <<Sel1:j\d+>> Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:            <<Sel2:j\d+>> Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          Add [<<Sel2>>,<<Sel1>>]

  /// CHECK-START-ARM64: long Main.$noinline$IntMatCond_LongVarVar(int, int, long, long) disassembly (after)
  /// CHECK:               LessThanOrEqual
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            cset le
  /// CHECK:               Select
  /// CHECK-NEXT:            csel le

  /// CHECK-START-X86_64: long Main.$noinline$IntMatCond_LongVarVar(int, int, long, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovle/ngq
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/neq

  /// CHECK-START-X86: long Main.$noinline$IntMatCond_LongVarVar(int, int, long, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{i\d+}},{{i\d+}}]
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK-NEXT:                     cmovle/ng
  /// CHECK-NEXT:                     cmovle/ng
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne
  /// CHECK-NEXT:                     cmovnz/ne

  public static long $noinline$IntMatCond_LongVarVar(int a, int b, long x, long y) {
    long result = (a > b ? x : y);
    return result + (a > b ? 0L : 1L);
  }

  /// CHECK-START: long Main.$noinline$LongNonmatCond_LongVarVar(long, long, long, long) register (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{j\d+}},{{j\d+}}]
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]

  /// CHECK-START-ARM64: long Main.$noinline$LongNonmatCond_LongVarVar(long, long, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            csel le

  /// CHECK-START-X86_64: long Main.$noinline$LongNonmatCond_LongVarVar(long, long, long, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{j\d+}},{{j\d+}}]
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovle/ngq

  public static long $noinline$LongNonmatCond_LongVarVar(long a, long b, long x, long y) {
    return a > b ? x : y;
  }

  /// CHECK-START-ARM: long Main.$noinline$LongEqNonmatCond_LongVarVar(long, long, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp {{r\d+}}, {{r\d+}}
  /// CHECK-NEXT:            it eq
  /// CHECK-NEXT:            cmpeq {{r\d+}}, {{r\d+}}
  /// CHECK-NEXT:            it eq

  public static long $noinline$LongEqNonmatCond_LongVarVar(long a, long b, long x, long y) {
    return a == b ? x : y;
  }

  /// CHECK-START-ARM: long Main.$noinline$LongNonmatCondCst_LongVarVar(long, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            mov ip, #52720
  /// CHECK-NEXT:            movt ip, #35243
  /// CHECK-NEXT:            cmp {{r\d+}}, ip
  /// CHECK-NEXT:            sbcs ip, {{r\d+}}, #{{\d+}}
  /// CHECK-NEXT:            it ge

  public static long $noinline$LongNonmatCondCst_LongVarVar(long a, long x, long y) {
    return a > 0x89ABCDEFL ? x : y;
  }

  /// CHECK-START-ARM: long Main.$noinline$LongNonmatCondCst_LongVarVar2(long, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            mov ip, #{{\d+}}
  /// CHECK-NEXT:            movt ip, #{{\d+}}
  /// CHECK-NEXT:            cmp {{r\d+}}, ip

  public static long $noinline$LongNonmatCondCst_LongVarVar2(long a, long x, long y) {
    return a > 0x0123456789ABCDEFL ? x : y;
  }

  /// CHECK-START-ARM: long Main.$noinline$LongNonmatCondCst_LongVarVar3(long, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp {{r\d+}}, {{r\d+}}
  /// CHECK-NOT:             sbcs
  /// CHECK-NOT:             cmp

  public static long $noinline$LongNonmatCondCst_LongVarVar3(long a, long x, long y) {
    return a > 0x7FFFFFFFFFFFFFFFL ? x : y;
  }

  /// CHECK-START-ARM: long Main.$noinline$LongNonmatCondCst_LongVarVar4(long, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            orrs ip, {{r\d+}}, {{r\d+}}
  /// CHECK-NOT:             cmp
  /// CHECK-NOT:             sbcs

  public static long $noinline$LongNonmatCondCst_LongVarVar4(long a, long x, long y) {
    return a == 0 ? x : y;
  }

  /// CHECK-START-ARM: long Main.$noinline$LongNonmatCondCst_LongVarVar5(long, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            orrs ip, {{r\d+}}, {{r\d+}}
  /// CHECK-NOT:             cmp
  /// CHECK-NOT:             sbcs

  public static long $noinline$LongNonmatCondCst_LongVarVar5(long a, long x, long y) {
    return a != 0 ? x : y;
  }

  /// CHECK-START-ARM: long Main.$noinline$LongNonmatCondCst_LongVarVar6(long, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp {{r\d+}}, #0
  /// CHECK-NOT:             cmp
  /// CHECK-NOT:             sbcs

  public static long $noinline$LongNonmatCondCst_LongVarVar6(long a, long x, long y) {
    return a >= 0 ? x : y;
  }

  /// CHECK-START-ARM: long Main.$noinline$LongNonmatCondCst_LongVarVar7(long, long, long) disassembly (after)
  /// CHECK:               Select
  /// CHECK-NEXT:            cmp {{r\d+}}, #0
  /// CHECK-NOT:             cmp
  /// CHECK-NOT:             sbcs

  public static long $noinline$LongNonmatCondCst_LongVarVar7(long a, long x, long y) {
    return a < 0 ? x : y;
  }

  /// CHECK-START: long Main.$noinline$LongMatCond_LongVarVar(long, long, long, long) register (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{j\d+}},{{j\d+}}]
  /// CHECK:            <<Sel1:j\d+>> Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:            <<Sel2:j\d+>> Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          Add [<<Sel2>>,<<Sel1>>]

  /// CHECK-START-ARM64: long Main.$noinline$LongMatCond_LongVarVar(long, long, long, long) disassembly (after)
  /// CHECK:               LessThanOrEqual
  /// CHECK-NEXT:            cmp
  /// CHECK-NEXT:            cset le
  /// CHECK:               Select
  /// CHECK-NEXT:            csel le

  /// CHECK-START-X86_64: long Main.$noinline$LongMatCond_LongVarVar(long, long, long, long) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{j\d+}},{{j\d+}}]
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovle/ngq
  /// CHECK:                          Select [{{j\d+}},{{j\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/neq

  public static long $noinline$LongMatCond_LongVarVar(long a, long b, long x, long y) {
    long result = (a > b ? x : y);
    return result + (a > b ? 0L : 1L);
  }

  /// CHECK-START: int Main.$noinline$FloatLtNonmatCond_IntVarVar(float, float, int, int) register (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{f\d+}},{{f\d+}}]
  /// CHECK-NEXT:                     Select [{{i\d+}},{{i\d+}},<<Cond>>]

  /// CHECK-START-ARM64: int Main.$noinline$FloatLtNonmatCond_IntVarVar(float, float, int, int) disassembly (after)
  /// CHECK:               LessThanOrEqual
  /// CHECK:               Select
  /// CHECK-NEXT:            fcmp
  /// CHECK-NEXT:            csel le

  public static int $noinline$FloatLtNonmatCond_IntVarVar(float a, float b, int x, int y) {
    return a > b ? x : y;
  }

  /// CHECK-START: int Main.$noinline$FloatGtNonmatCond_IntVarVar(float, float, int, int) register (after)
  /// CHECK:            <<Cond:z\d+>> GreaterThanOrEqual [{{f\d+}},{{f\d+}}]
  /// CHECK-NEXT:                     Select [{{i\d+}},{{i\d+}},<<Cond>>]

  /// CHECK-START-ARM64: int Main.$noinline$FloatGtNonmatCond_IntVarVar(float, float, int, int) disassembly (after)
  /// CHECK:               GreaterThanOrEqual
  /// CHECK:               Select
  /// CHECK-NEXT:            fcmp
  /// CHECK-NEXT:            csel hs

  public static int $noinline$FloatGtNonmatCond_IntVarVar(float a, float b, int x, int y) {
    return a < b ? x : y;
  }

  /// CHECK-START: float Main.$noinline$FloatGtNonmatCond_FloatVarVar(float, float, float, float) register (after)
  /// CHECK:            <<Cond:z\d+>> GreaterThanOrEqual [{{f\d+}},{{f\d+}}]
  /// CHECK-NEXT:                     Select [{{f\d+}},{{f\d+}},<<Cond>>]

  /// CHECK-START-ARM64: float Main.$noinline$FloatGtNonmatCond_FloatVarVar(float, float, float, float) disassembly (after)
  /// CHECK:               GreaterThanOrEqual
  /// CHECK:               Select
  /// CHECK-NEXT:            fcmp
  /// CHECK-NEXT:            fcsel hs

  public static float $noinline$FloatGtNonmatCond_FloatVarVar(float a, float b, float x, float y) {
    return a < b ? x : y;
  }

  /// CHECK-START: int Main.$noinline$FloatLtMatCond_IntVarVar(float, float, int, int) register (after)
  /// CHECK:            <<Cond:z\d+>> LessThanOrEqual [{{f\d+}},{{f\d+}}]
  /// CHECK-NEXT:       <<Sel:i\d+>>  Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK-NEXT:                     Add [<<Cond>>,<<Sel>>]

  /// CHECK-START-ARM64: int Main.$noinline$FloatLtMatCond_IntVarVar(float, float, int, int) disassembly (after)
  /// CHECK:               LessThanOrEqual
  /// CHECK-NEXT:            fcmp
  /// CHECK-NEXT:            cset le
  /// CHECK:               Select
  /// CHECK-NEXT:            csel le

  public static int $noinline$FloatLtMatCond_IntVarVar(float a, float b, int x, int y) {
    int result = (a > b ? x : y);
    return result + (a > b ? 0 : 1);
  }

  /// CHECK-START: int Main.$noinline$FloatGtMatCond_IntVarVar(float, float, int, int) register (after)
  /// CHECK:            <<Cond:z\d+>> GreaterThanOrEqual [{{f\d+}},{{f\d+}}]
  /// CHECK-NEXT:       <<Sel:i\d+>>  Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK-NEXT:                     Add [<<Cond>>,<<Sel>>]

  /// CHECK-START-ARM64: int Main.$noinline$FloatGtMatCond_IntVarVar(float, float, int, int) disassembly (after)
  /// CHECK:               GreaterThanOrEqual
  /// CHECK-NEXT:            fcmp
  /// CHECK-NEXT:            cset hs
  /// CHECK:               Select
  /// CHECK-NEXT:            csel hs

  public static int $noinline$FloatGtMatCond_IntVarVar(float a, float b, int x, int y) {
    int result = (a < b ? x : y);
    return result + (a < b ? 0 : 1);
  }

  /// CHECK-START: float Main.$noinline$FloatGtMatCond_FloatVarVar(float, float, float, float) register (after)
  /// CHECK:            <<Cond:z\d+>> GreaterThanOrEqual
  /// CHECK-NEXT:       <<Sel:f\d+>>  Select [{{f\d+}},{{f\d+}},<<Cond>>]

  /// CHECK-START-ARM64: float Main.$noinline$FloatGtMatCond_FloatVarVar(float, float, float, float) disassembly (after)
  /// CHECK:               GreaterThanOrEqual
  /// CHECK-NEXT:            fcmp
  /// CHECK-NEXT:            cset hs
  /// CHECK:               Select
  /// CHECK-NEXT:            fcsel hs

  public static float $noinline$FloatGtMatCond_FloatVarVar(float a, float b, float x, float y) {
    float result = (a < b ? x : y);
    return result + (a < b ? 0 : 1);
  }

  /// CHECK-START: int Main.$noinline$BoolCond_0_m1(boolean) register (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]

  /// CHECK-START-ARM64: int Main.$noinline$BoolCond_0_m1(boolean) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK-NEXT:                     cmp {{w\d+}}, #0x0 (0)
  /// CHECK-NEXT:                     csetm {{w\d+}}, eq

  /// CHECK-START-X86_64: int Main.$noinline$BoolCond_0_m1(boolean) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  /// CHECK-START-X86: int Main.$noinline$BoolCond_0_m1(boolean) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  public static int $noinline$BoolCond_0_m1(boolean cond) {
    return cond ? 0 : -1;
  }

  /// CHECK-START: int Main.$noinline$BoolCond_m1_0(boolean) register (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]

  /// CHECK-START-ARM64: int Main.$noinline$BoolCond_m1_0(boolean) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK-NEXT:                     cmp {{w\d+}}, #0x0 (0)
  /// CHECK-NEXT:                     csetm {{w\d+}}, ne

  /// CHECK-START-X86_64: int Main.$noinline$BoolCond_m1_0(boolean) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  /// CHECK-START-X86: int Main.$noinline$BoolCond_m1_0(boolean) disassembly (after)
  /// CHECK:            <<Cond:z\d+>> ParameterValue
  /// CHECK:                          Select [{{i\d+}},{{i\d+}},<<Cond>>]
  /// CHECK:                          cmovnz/ne

  public static int $noinline$BoolCond_m1_0(boolean cond) {
    return cond ? -1 : 0;
  }

  public static void assertEqual(int expected, int actual) {
    if (expected != actual) {
      throw new Error("Assertion failed: " + expected + " != " + actual);
    }
  }

  public static void assertEqual(float expected, float actual) {
    if (expected != actual) {
      throw new Error("Assertion failed: " + expected + " != " + actual);
    }
  }

  public static void main(String[] args) {
    assertEqual(5, $noinline$BoolCond_IntVarVar(true, 5, 7));
    assertEqual(7, $noinline$BoolCond_IntVarVar(false, 5, 7));
    assertEqual(5, $noinline$BoolCond_IntVarCst(true, 5));
    assertEqual(1, $noinline$BoolCond_IntVarCst(false, 5));
    assertEqual(1, $noinline$BoolCond_IntCstVar(true, 7));
    assertEqual(7, $noinline$BoolCond_IntCstVar(false, 7));

    assertEqual(5L, $noinline$BoolCond_LongVarVar(true, 5L, 7L));
    assertEqual(7L, $noinline$BoolCond_LongVarVar(false, 5L, 7L));
    assertEqual(5L, $noinline$BoolCond_LongVarCst(true, 5L));
    assertEqual(1L, $noinline$BoolCond_LongVarCst(false, 5L));
    assertEqual(1L, $noinline$BoolCond_LongCstVar(true, 7L));
    assertEqual(7L, $noinline$BoolCond_LongCstVar(false, 7L));

    assertEqual(5, $noinline$BoolCond_FloatVarVar(true, 5, 7));
    assertEqual(7, $noinline$BoolCond_FloatVarVar(false, 5, 7));
    assertEqual(5, $noinline$BoolCond_FloatVarCst(true, 5));
    assertEqual(1, $noinline$BoolCond_FloatVarCst(false, 5));
    assertEqual(1, $noinline$BoolCond_FloatCstVar(true, 7));
    assertEqual(7, $noinline$BoolCond_FloatCstVar(false, 7));

    assertEqual(5, $noinline$IntNonmatCond_IntVarVar(3, 2, 5, 7));
    assertEqual(7, $noinline$IntNonmatCond_IntVarVar(2, 3, 5, 7));
    assertEqual(5, $noinline$IntMatCond_IntVarVar(3, 2, 5, 7));
    assertEqual(8, $noinline$IntMatCond_IntVarVar(2, 3, 5, 7));
    assertEqual(5, $noinline$IntNonmatCond_LongVarVar(3, 2, 5L, 7L));
    assertEqual(7, $noinline$IntNonmatCond_LongVarVar(2, 3, 5L, 7L));
    assertEqual(5, $noinline$IntMatCond_LongVarVar(3, 2, 5L, 7L));
    assertEqual(8, $noinline$IntMatCond_LongVarVar(2, 3, 5L, 7L));
    assertEqual(5, $noinline$LongMatCond_LongVarVar(3L, 2L, 5L, 7L));
    assertEqual(8, $noinline$LongMatCond_LongVarVar(2L, 3L, 5L, 7L));

    assertEqual(0xAAAAAAAA55555555L, $noinline$LongNonmatCond_LongVarVar(3L,
                                                                         2L,
                                                                         0xAAAAAAAA55555555L,
                                                                         0x8888888877777777L));
    assertEqual(0x8888888877777777L, $noinline$LongNonmatCond_LongVarVar(2L,
                                                                         2L,
                                                                         0xAAAAAAAA55555555L,
                                                                         0x8888888877777777L));
    assertEqual(0x8888888877777777L, $noinline$LongNonmatCond_LongVarVar(2L,
                                                                         3L,
                                                                         0xAAAAAAAA55555555L,
                                                                         0x8888888877777777L));
    assertEqual(0xAAAAAAAA55555555L, $noinline$LongNonmatCond_LongVarVar(0x0000000100000000L,
                                                                         0x00000000FFFFFFFFL,
                                                                         0xAAAAAAAA55555555L,
                                                                         0x8888888877777777L));
    assertEqual(0x8888888877777777L, $noinline$LongNonmatCond_LongVarVar(0x00000000FFFFFFFFL,
                                                                         0x0000000100000000L,
                                                                         0xAAAAAAAA55555555L,
                                                                         0x8888888877777777L));

    assertEqual(0x8888888877777777L, $noinline$LongEqNonmatCond_LongVarVar(2L,
                                                                           3L,
                                                                           0xAAAAAAAA55555555L,
                                                                           0x8888888877777777L));
    assertEqual(0xAAAAAAAA55555555L, $noinline$LongEqNonmatCond_LongVarVar(2L,
                                                                           2L,
                                                                           0xAAAAAAAA55555555L,
                                                                           0x8888888877777777L));
    assertEqual(0x8888888877777777L, $noinline$LongEqNonmatCond_LongVarVar(0x10000000000L,
                                                                           0L,
                                                                           0xAAAAAAAA55555555L,
                                                                           0x8888888877777777L));

    assertEqual(5L, $noinline$LongNonmatCondCst_LongVarVar2(0x7FFFFFFFFFFFFFFFL, 5L, 7L));
    assertEqual(7L, $noinline$LongNonmatCondCst_LongVarVar2(2L, 5L, 7L));

    assertEqual(7L, $noinline$LongNonmatCondCst_LongVarVar3(2L, 5L, 7L));

    long[] long_inputs = {
        0L, 1L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 2L, 0x100000000L, 0xFFFFFFFF00000000L, -9000L};

    long[] expected_1 = {5L, 7L, 7L, 7L, 7L, 7L, 7L, 7L, 7L};

    for (int i = 0; i < long_inputs.length; i++) {
      assertEqual(expected_1[i], $noinline$LongNonmatCondCst_LongVarVar4(long_inputs[i], 5L, 7L));
    }

    long[] expected_2 = {7L, 5L, 5L, 5L, 5L, 5L, 5L, 5L, 5L};

    for (int i = 0; i < long_inputs.length; i++) {
      assertEqual(expected_2[i], $noinline$LongNonmatCondCst_LongVarVar5(long_inputs[i], 5L, 7L));
    }

    long[] expected_3 = {5L, 5L, 7L, 7L, 5L, 5L, 5L, 7L, 7L};

    for (int i = 0; i < long_inputs.length; i++) {
      assertEqual(expected_3[i], $noinline$LongNonmatCondCst_LongVarVar6(long_inputs[i], 5L, 7L));
    }

    long[] expected_4 = {7L, 7L, 5L, 5L, 7L, 7L, 7L, 5L, 5L};

    for (int i = 0; i < long_inputs.length; i++) {
      assertEqual(expected_4[i], $noinline$LongNonmatCondCst_LongVarVar7(long_inputs[i], 5L, 7L));
    }

    assertEqual(7L, $noinline$LongNonmatCondCst_LongVarVar7(0L, 5L, 7L));
    assertEqual(7L, $noinline$LongNonmatCondCst_LongVarVar7(2L, 5L, 7L));
    assertEqual(5L, $noinline$LongNonmatCondCst_LongVarVar7(-9000L, 5L, 7L));

    assertEqual(5, $noinline$FloatLtNonmatCond_IntVarVar(3, 2, 5, 7));
    assertEqual(7, $noinline$FloatLtNonmatCond_IntVarVar(2, 3, 5, 7));
    assertEqual(7, $noinline$FloatLtNonmatCond_IntVarVar(Float.NaN, 2, 5, 7));
    assertEqual(7, $noinline$FloatLtNonmatCond_IntVarVar(2, Float.NaN, 5, 7));

    assertEqual(5, $noinline$FloatGtNonmatCond_IntVarVar(2, 3, 5, 7));
    assertEqual(7, $noinline$FloatGtNonmatCond_IntVarVar(3, 2, 5, 7));
    assertEqual(7, $noinline$FloatGtNonmatCond_IntVarVar(Float.NaN, 2, 5, 7));
    assertEqual(7, $noinline$FloatGtNonmatCond_IntVarVar(2, Float.NaN, 5, 7));

    assertEqual(5, $noinline$FloatGtNonmatCond_FloatVarVar(2, 3, 5, 7));
    assertEqual(7, $noinline$FloatGtNonmatCond_FloatVarVar(3, 2, 5, 7));
    assertEqual(7, $noinline$FloatGtNonmatCond_FloatVarVar(Float.NaN, 2, 5, 7));
    assertEqual(7, $noinline$FloatGtNonmatCond_FloatVarVar(2, Float.NaN, 5, 7));

    assertEqual(5, $noinline$FloatLtMatCond_IntVarVar(3, 2, 5, 7));
    assertEqual(8, $noinline$FloatLtMatCond_IntVarVar(2, 3, 5, 7));
    assertEqual(8, $noinline$FloatLtMatCond_IntVarVar(Float.NaN, 2, 5, 7));
    assertEqual(8, $noinline$FloatLtMatCond_IntVarVar(2, Float.NaN, 5, 7));

    assertEqual(5, $noinline$FloatGtMatCond_IntVarVar(2, 3, 5, 7));
    assertEqual(8, $noinline$FloatGtMatCond_IntVarVar(3, 2, 5, 7));
    assertEqual(8, $noinline$FloatGtMatCond_IntVarVar(Float.NaN, 2, 5, 7));
    assertEqual(8, $noinline$FloatGtMatCond_IntVarVar(2, Float.NaN, 5, 7));

    assertEqual(5, $noinline$FloatGtMatCond_FloatVarVar(2, 3, 5, 7));
    assertEqual(8, $noinline$FloatGtMatCond_FloatVarVar(3, 2, 5, 7));
    assertEqual(8, $noinline$FloatGtMatCond_FloatVarVar(Float.NaN, 2, 5, 7));
    assertEqual(8, $noinline$FloatGtMatCond_FloatVarVar(2, Float.NaN, 5, 7));

    assertEqual(0, $noinline$BoolCond_0_m1(true));
    assertEqual(-1, $noinline$BoolCond_0_m1(false));
    assertEqual(-1, $noinline$BoolCond_m1_0(true));
    assertEqual(0, $noinline$BoolCond_m1_0(false));
  }
}
