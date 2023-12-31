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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

public class Main {
    public static void main(String[] args) throws Exception {
        $noinline$testFieldReads();
        $noinline$testArrayReadsWithConstIndex();
        $noinline$testArrayReadsWithNonConstIndex();
        $noinline$testGcRoots();
        $noinline$testUnsafeGet();
        $noinline$testUnsafeCas();
        $noinline$testUnsafeCasRegression();
        $noinline$testVarHandleCompareAndSet();
        $noinline$testVarHandleCompareAndExchange();
        $noinline$testVarHandleGetAndSet();
        $noinline$testReferenceRefersTo();
    }

    public static void $noinline$testFieldReads() {
        // Initialize local variables for comparison.
        Object f0000 = manyFields.testField0000;
        Object f1024 = manyFields.testField1024;
        Object f4444 = manyFields.testField4444;
        Object f4999 = manyFields.testField4999;

        // Continually check reads from `manyFields` while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            ManyFields mf = manyFields;  // Load the volatile `manyFields` once on each iteration.
            // Test reference field access.
            $noinline$assertSameObject(f0000, mf.testField0000);
            $noinline$assertDifferentObject(f0000, mf.testField0001);
            $noinline$assertSameObject(f1024, mf.testField1024);
            $noinline$assertSameObject(f4444, mf.testField4444);
            $noinline$assertDifferentObject(f4999, mf.testField4998);
            $noinline$assertSameObject(f4999, mf.testField4999);
        }
    }

    public static void $noinline$testArrayReadsWithConstIndex() {
        // Initialize local variables for comparison.
        Object f0000 = new Integer(0);
        Object f1024 = new Integer(1024);
        Object f4444 = new Integer(4444);
        Object f4999 = new Integer(4999);
        // Initialize largeArray for comparison.
        largeArray[0] = f0000;
        Object tmp = new Integer(1);
        largeArray[1] = tmp;
        largeArray[1024] = f1024;
        largeArray[4444] = f4444;
        tmp = new Integer(4998);
        largeArray[4998] = tmp;
        largeArray[4999] = f4999;
        tmp = null;  // Do not keep a reference to objects in largeArray[1] or largeArray[4998].

        // Continually check reads from `largeArray` with constant indexes while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            Object[] la = largeArray;    // Load the volatile `largeArray` once on each iteration.
            // Test array access with constant index.
            $noinline$assertSameObject(f0000, la[0]);
            $noinline$assertDifferentObject(f0000, la[1]);
            $noinline$assertSameObject(f1024, la[1024]);
            $noinline$assertSameObject(f4444, la[4444]);
            $noinline$assertDifferentObject(f4999, la[4998]);
            $noinline$assertSameObject(f4999, la[4999]);
        }
    }

    public static void $noinline$testArrayReadsWithNonConstIndex() {
        // Initialize local variables for comparison.
        Object f0000 = new Integer(0);
        Object f1024 = new Integer(1024);
        Object f4444 = new Integer(4444);
        Object f4999 = new Integer(4999);
        // Initialize largeArray for comparison.
        largeArray[0] = f0000;
        Object tmp = new Integer(1);
        largeArray[1] = tmp;
        largeArray[1024] = f1024;
        largeArray[4444] = f4444;
        tmp = new Integer(4998);
        largeArray[4998] = tmp;
        largeArray[4999] = f4999;
        tmp = null;  // Do not keep a reference to objects in largeArray[1] or largeArray[4998].
        // Read indexes, they cannot be considered constant because the variables are volatile.
        int i0 = index0;
        int i1 = index1;
        int i1024 = index1024;
        int i4444 = index4444;
        int i4998 = index4998;
        int i4999 = index4999;

        // Continually check reads from `largeArray` with non-constant indexes while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            Object[] la = largeArray;    // Load the volatile `largeArray` once on each iteration.
            // Test array access with non-constant index.
            $noinline$assertSameObject(f0000, la[i0]);
            $noinline$assertDifferentObject(f0000, la[i1]);
            $noinline$assertSameObject(f1024, la[i1024]);
            $noinline$assertSameObject(f4444, la[i4444]);
            $noinline$assertDifferentObject(f4999, la[i4998]);
            $noinline$assertSameObject(f4999, la[i4999]);

            la = largeArray;
            // Group the ArrayGets so they aren't divided by a function call; this will enable
            // interm. address sharing for arm64.
            Object tmp1 = la[i0];
            Object tmp2 = la[i0 + 1];
            Object tmp3 = la[i0 + 1024];
            Object tmp4 = la[i0 + 4444];
            Object tmp5 = la[i0 + 4998];
            Object tmp6 = la[i0 + 4999];

            $noinline$assertSameObject(f0000, tmp1);
            $noinline$assertDifferentObject(f0000, tmp2);
            $noinline$assertSameObject(f1024, tmp3);
            $noinline$assertSameObject(f4444, tmp4);
            $noinline$assertDifferentObject(f4999, tmp5);
            $noinline$assertSameObject(f4999, tmp6);
        }
    }

    public static void $noinline$testGcRoots() {
        // Initialize strings, hide this under a condition based on a volatile field.
        String testString0 = null;
        String testString1 = null;
        String testString2 = null;
        String testString3 = null;
        if (index0 != 12345678) {
            // By having this in the const-string instructions in an if-block, we avoid
            // GVN eliminating identical const-string instructions in the loop below.
            testString0 = "testString0";
            testString1 = "testString1";
            testString2 = "testString2";
            testString3 = "testString3";
        }

        // Continually check reads from `manyFields` and `largeArray` while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            // Test GC roots.
            if (index0 != 12345678) {
              $noinline$assertSameObject(testString0, "testString0");
              $noinline$assertSameObject(testString1, "testString1");
              $noinline$assertSameObject(testString2, "testString2");
              $noinline$assertSameObject(testString3, "testString3");
            }
            // TODO: Stress GC roots (const-class, kBssEntry/kReferrersClass).
        }
    }

    public static void $noinline$testUnsafeGet() throws Exception {
        // Initialize local variables for comparison.
        Object f0000 = manyFields.testField0000;
        Object f1024 = manyFields.testField1024;
        Object f4444 = manyFields.testField4444;
        Object f4999 = manyFields.testField4999;
        // Initialize Unsafe.
        Unsafe unsafe = getUnsafe();
        long f0000Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField0000"));
        long f0001Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField0001"));
        long f1024Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField1024"));
        long f4444Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField4444"));
        long f4998Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField4998"));
        long f4999Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField4999"));

        // Continually check unsafe.GetObject() while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            ManyFields mf = manyFields;  // Load the volatile `manyFields` once on each iteration.
            // Test Unsafe.getObject().
            $noinline$assertSameObject(f0000, unsafe.getObject(mf, f0000Offset));
            $noinline$assertDifferentObject(f0000, unsafe.getObject(mf, f0001Offset));
            $noinline$assertSameObject(f1024, unsafe.getObject(mf, f1024Offset));
            $noinline$assertSameObject(f4444, unsafe.getObject(mf, f4444Offset));
            $noinline$assertDifferentObject(f4999, unsafe.getObject(mf, f4998Offset));
            $noinline$assertSameObject(f4999, unsafe.getObject(mf, f4999Offset));
        }
    }

    public static void $noinline$testUnsafeCas() throws Exception {
        // Initialize local variables for comparison.
        Object f0000 = manyFields.testField0000;
        Object f1024 = manyFields.testField1024;
        Object f4444 = manyFields.testField4444;
        Object f4999 = manyFields.testField4999;
        // Initialize Unsafe.
        Unsafe unsafe = getUnsafe();
        long f0000Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField0000"));
        long f0001Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField0001"));
        long f1024Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField1024"));
        long f4444Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField4444"));
        long f4998Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField4998"));
        long f4999Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField4999"));

        // Continually check Unsafe.compareAndSwapObject() while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            ManyFields mf = manyFields;  // Load the volatile `manyFields` once on each iteration.
            // Test Unsafe.compareAndSwapObject().
            $noinline$assertEqual(
                   false, unsafe.compareAndSwapObject(mf, f0000Offset, f1024, f4444));
            $noinline$assertEqual(
                   false, unsafe.compareAndSwapObject(mf, f0001Offset, f1024, f4444));
            $noinline$assertEqual(
                   true, unsafe.compareAndSwapObject(mf, f1024Offset, f1024, f4444));
            $noinline$assertEqual(
                   true, unsafe.compareAndSwapObject(mf, f1024Offset, f4444, f1024));
            $noinline$assertEqual(
                   false, unsafe.compareAndSwapObject(mf, f1024Offset, f4444, f1024));
            $noinline$assertEqual(
                   false, unsafe.compareAndSwapObject(mf, f4444Offset, f1024, f4444));
            $noinline$assertEqual(
                   false, unsafe.compareAndSwapObject(mf, f4998Offset, f1024, f4444));
            $noinline$assertEqual(
                   false, unsafe.compareAndSwapObject(mf, f4999Offset, f1024, f4444));
        }
    }

    public static void $noinline$testUnsafeCasRegression() throws Exception {
        // Initialize local variables for comparison.
        Object f0000 = manyFields.testField0000;
        // Initialize Unsafe.
        Unsafe unsafe = getUnsafe();
        long f0001Offset =
            unsafe.objectFieldOffset(ManyFields.class.getField("testField0001"));

        // Continually check Unsafe.compareAndSwapObject() while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            ManyFields mf = manyFields;  // Load the volatile `manyFields` once on each iteration.

            // With https://android-review.googlesource.com/729224 , the intrinsic could
            // erroneously clobber r0 on ARM for Baker read barriers because the introspection
            // entrypoint would read the destination register from bits 12-15 of the instruction
            // ADD (register, T3) with no shift, assuming to see LDR (immediate, T3), getting
            // the output register number as 0 instead of the actual destination in bits 8-11.
            // As a regression test, call a $noinline$ method which returns the result in r0,
            // do the UnsafeCasObject and check the result of the $noinline$ call (register
            // allocator should leave the result in r0, clobbered by the broken intrinsic).
            int x = $noinline$foo();
            unsafe.compareAndSwapObject(mf, f0001Offset, f0000, null);  // Ignore the result.
            if (x != 42) {
              throw new Error();
            }
        }
    }

    public static void $noinline$testVarHandleCompareAndSet() throws Exception {
        // Initialize local variables for comparison.
        Object f0000 = manyFields.testField0000;
        Object f1024 = manyFields.testField1024;
        Object f4444 = manyFields.testField4444;
        Object f4999 = manyFields.testField4999;
        // Initialize VarHandle objects.
        VarHandle f0000vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField0000", Object.class);
        VarHandle f0001vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField0001", Object.class);
        VarHandle f1024vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField1024", Object.class);
        VarHandle f4444vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField4444", Object.class);
        VarHandle f4998vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField4998", Object.class);
        VarHandle f4999vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField4999", Object.class);

        // Continually check VarHandle.compareAndSet() while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            ManyFields mf = manyFields;  // Load the volatile `manyFields` once on each iteration.
            // Test VarHandle.compareAndSet().
            $noinline$assertEqual(false, f0000vh.compareAndSet(mf, f1024, f4444));
            $noinline$assertEqual(false, f0001vh.compareAndSet(mf, f1024, f4444));
            $noinline$assertEqual(true, f1024vh.compareAndSet(mf, f1024, f4444));
            $noinline$assertEqual(true, f1024vh.compareAndSet(mf, f4444, f1024));
            $noinline$assertEqual(false, f1024vh.compareAndSet(mf, f4444, f1024));
            $noinline$assertEqual(false, f4444vh.compareAndSet(mf, f1024, f4444));
            $noinline$assertEqual(false, f4998vh.compareAndSet(mf, f1024, f4444));
            $noinline$assertEqual(false, f4999vh.compareAndSet(mf, f1024, f4444));
        }
    }

    public static void $noinline$testVarHandleCompareAndExchange() throws Exception {
        // Initialize local variables for comparison.
        Object f0000 = manyFields.testField0000;
        Object f0001 = manyFields.testField0001;
        Object f1024 = manyFields.testField1024;
        Object f4444 = manyFields.testField4444;
        Object f4998 = manyFields.testField4998;
        Object f4999 = manyFields.testField4999;
        // Initialize VarHandle objects.
        VarHandle f0000vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField0000", Object.class);
        VarHandle f0001vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField0001", Object.class);
        VarHandle f1024vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField1024", Object.class);
        VarHandle f4444vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField4444", Object.class);
        VarHandle f4998vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField4998", Object.class);
        VarHandle f4999vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField4999", Object.class);

        // Continually check VarHandle.compareAndExchange() while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            ManyFields mf = manyFields;  // Load the volatile `manyFields` once on each iteration.
            // Test VarHandle.compareAndExchange(). Use reference comparison, not equals().
            $noinline$assertSameObject(
                    f0000, f0000vh.compareAndExchange(mf, f1024, f4444));  // Unchanged.
            $noinline$assertSameObject(
                    f0001, f0001vh.compareAndExchange(mf, f1024, f4444));  // Unchanged.
            $noinline$assertSameObject(
                    f1024, f1024vh.compareAndExchange(mf, f1024, f4444));  // Replaced.
            $noinline$assertSameObject(
                    f4444, f1024vh.compareAndExchange(mf, f4444, f1024));  // Replaced.
            $noinline$assertSameObject(
                    f1024, f1024vh.compareAndExchange(mf, f4444, f1024));  // Unchanged.
            $noinline$assertSameObject(
                    f4444, f4444vh.compareAndExchange(mf, f1024, f4444));  // Unchanged.
            $noinline$assertSameObject(
                    f4998, f4998vh.compareAndExchange(mf, f1024, f4444));  // Unchanged.
            $noinline$assertSameObject(
                    f4999, f4999vh.compareAndExchange(mf, f1024, f4444));  // Unchanged.
        }
    }

    public static void $noinline$testVarHandleGetAndSet() throws Exception {
        // Initialize local variables for comparison.
        Object f0000 = manyFields.testField0000;
        Object f0001 = manyFields.testField0001;
        Object f1024 = manyFields.testField1024;
        Object f4444 = manyFields.testField4444;
        Object f4998 = manyFields.testField4998;
        Object f4999 = manyFields.testField4999;
        // Initialize VarHandle objects.
        VarHandle f0000vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField0000", Object.class);
        VarHandle f0001vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField0001", Object.class);
        VarHandle f1024vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField1024", Object.class);
        VarHandle f4444vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField4444", Object.class);
        VarHandle f4998vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField4998", Object.class);
        VarHandle f4999vh =
            MethodHandles.lookup().findVarHandle(ManyFields.class, "testField4999", Object.class);

        // Continually check VarHandle.getAndSet() while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and
        // stress the read barrier implementation if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            ManyFields mf = manyFields;  // Load the volatile `manyFields` once on each iteration.
            // Test VarHandle.getAndSet(). Use reference comparison, not equals().
            $noinline$assertSameObject(f0000, f0000vh.getAndSet(mf, f0000));  // Unchanged.
            $noinline$assertSameObject(f0001, f0001vh.getAndSet(mf, f0001));  // Unchanged.
            $noinline$assertSameObject(f1024, f1024vh.getAndSet(mf, f4444));  // Replaced.
            $noinline$assertSameObject(f4444, f1024vh.getAndSet(mf, f1024));  // Replaced.
            $noinline$assertSameObject(f1024, f1024vh.getAndSet(mf, f1024));  // Unchanged.
            $noinline$assertSameObject(f4444, f4444vh.getAndSet(mf, f4444));  // Unchanged.
            $noinline$assertSameObject(f4998, f4998vh.getAndSet(mf, f4998));  // Unchanged.
            $noinline$assertSameObject(f4999, f4999vh.getAndSet(mf, f4999));  // Unchanged.
        }
    }

    public static void $noinline$testReferenceRefersTo() throws Exception {
        // Initialize local variables for comparison.
        manyFields.testField0000 = new Object();
        manyFields.testField1024 = new Object();
        manyFields.testField4444 = new Object();
        manyFields.testField4999 = new Object();
        WeakReference<Object> f0000 = new WeakReference<Object>(manyFields.testField0000);
        WeakReference<Object> f1024 = new WeakReference<Object>(manyFields.testField1024);
        WeakReference<Object> f4444 = new WeakReference<Object>(manyFields.testField4444);
        WeakReference<Object> f4999 = new WeakReference<Object>(manyFields.testField4999);

        // Continually check reads from `manyFields` while allocating
        // over 64MiB memory (with heap size limited to 16MiB), ensuring we run GC and stress the
        // read barrier implementation in Reference.refersTo() if concurrent collector is enabled.
        for (int i = 0; i != 64 * 1024; ++i) {
            $noinline$allocateAtLeast1KiB();
            ManyFields mf = manyFields;  // Load the volatile `manyFields` once on each iteration.
            // Test Reference.refersTo() with reference field access.
            $noinline$assertEqual(true, f0000.refersTo(mf.testField0000));
            $noinline$assertEqual(false, f0000.refersTo(mf.testField0001));
            $noinline$assertEqual(true, f1024.refersTo(mf.testField1024));
            $noinline$assertEqual(true, f4444.refersTo(mf.testField4444));
            $noinline$assertEqual(false, f4999.refersTo(mf.testField4998));
            $noinline$assertEqual(true, f4999.refersTo(mf.testField4999));
        }
    }

    public static int $noinline$foo() { return 42; }

    public static void $noinline$assertDifferentObject(Object lhs, Object rhs) {
        if (lhs == rhs) {
            throw new Error("Same objects: " + lhs + " and " + rhs);
        }
    }

    public static void $noinline$assertSameObject(Object lhs, Object rhs) {
        if (lhs != rhs) {
            throw new Error("Different objects: " + lhs + " and " + rhs);
        }
    }

    public static void $noinline$assertEqual(boolean expected, boolean actual) {
        if (expected != actual) {
            throw new Error("Expected " + expected +", got " + actual);
        }
    }

    public static Unsafe getUnsafe() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    public static void $noinline$allocateAtLeast1KiB() {
        // Give GC more work by allocating Object arrays.
        memory[allocationIndex] = new Object[1024 / 4];
        ++allocationIndex;
        if (allocationIndex == memory.length) {
            allocationIndex = 0;
        }
    }

    // Make these volatile to avoid load elimination.
    public static volatile ManyFields manyFields = new ManyFields();
    public static volatile Object[] largeArray = new Object[5000];
    public static volatile int index0 = 0;
    public static volatile int index1 = 1;
    public static volatile int index1024 = 1024;
    public static volatile int index4444 = 4444;
    public static volatile int index4998 = 4998;
    public static volatile int index4999 = 4999;

    // We shall retain some allocated memory and release old allocations
    // so that the GC has something to do.
    public static Object[] memory = new Object[1024];
    public static int allocationIndex = 0;
}

class ManyFields extends ManyFieldsBase3 {
    public Object testField4000 = new Integer(4000);
    public Object testField4001 = new Integer(4001);
    public Object testField4002 = new Integer(4002);
    public Object testField4003 = new Integer(4003);
    public Object testField4004 = new Integer(4004);
    public Object testField4005 = new Integer(4005);
    public Object testField4006 = new Integer(4006);
    public Object testField4007 = new Integer(4007);
    public Object testField4008 = new Integer(4008);
    public Object testField4009 = new Integer(4009);
    public Object testField4010 = new Integer(4010);
    public Object testField4011 = new Integer(4011);
    public Object testField4012 = new Integer(4012);
    public Object testField4013 = new Integer(4013);
    public Object testField4014 = new Integer(4014);
    public Object testField4015 = new Integer(4015);
    public Object testField4016 = new Integer(4016);
    public Object testField4017 = new Integer(4017);
    public Object testField4018 = new Integer(4018);
    public Object testField4019 = new Integer(4019);
    public Object testField4020 = new Integer(4020);
    public Object testField4021 = new Integer(4021);
    public Object testField4022 = new Integer(4022);
    public Object testField4023 = new Integer(4023);
    public Object testField4024 = new Integer(4024);
    public Object testField4025 = new Integer(4025);
    public Object testField4026 = new Integer(4026);
    public Object testField4027 = new Integer(4027);
    public Object testField4028 = new Integer(4028);
    public Object testField4029 = new Integer(4029);
    public Object testField4030 = new Integer(4030);
    public Object testField4031 = new Integer(4031);
    public Object testField4032 = new Integer(4032);
    public Object testField4033 = new Integer(4033);
    public Object testField4034 = new Integer(4034);
    public Object testField4035 = new Integer(4035);
    public Object testField4036 = new Integer(4036);
    public Object testField4037 = new Integer(4037);
    public Object testField4038 = new Integer(4038);
    public Object testField4039 = new Integer(4039);
    public Object testField4040 = new Integer(4040);
    public Object testField4041 = new Integer(4041);
    public Object testField4042 = new Integer(4042);
    public Object testField4043 = new Integer(4043);
    public Object testField4044 = new Integer(4044);
    public Object testField4045 = new Integer(4045);
    public Object testField4046 = new Integer(4046);
    public Object testField4047 = new Integer(4047);
    public Object testField4048 = new Integer(4048);
    public Object testField4049 = new Integer(4049);
    public Object testField4050 = new Integer(4050);
    public Object testField4051 = new Integer(4051);
    public Object testField4052 = new Integer(4052);
    public Object testField4053 = new Integer(4053);
    public Object testField4054 = new Integer(4054);
    public Object testField4055 = new Integer(4055);
    public Object testField4056 = new Integer(4056);
    public Object testField4057 = new Integer(4057);
    public Object testField4058 = new Integer(4058);
    public Object testField4059 = new Integer(4059);
    public Object testField4060 = new Integer(4060);
    public Object testField4061 = new Integer(4061);
    public Object testField4062 = new Integer(4062);
    public Object testField4063 = new Integer(4063);
    public Object testField4064 = new Integer(4064);
    public Object testField4065 = new Integer(4065);
    public Object testField4066 = new Integer(4066);
    public Object testField4067 = new Integer(4067);
    public Object testField4068 = new Integer(4068);
    public Object testField4069 = new Integer(4069);
    public Object testField4070 = new Integer(4070);
    public Object testField4071 = new Integer(4071);
    public Object testField4072 = new Integer(4072);
    public Object testField4073 = new Integer(4073);
    public Object testField4074 = new Integer(4074);
    public Object testField4075 = new Integer(4075);
    public Object testField4076 = new Integer(4076);
    public Object testField4077 = new Integer(4077);
    public Object testField4078 = new Integer(4078);
    public Object testField4079 = new Integer(4079);
    public Object testField4080 = new Integer(4080);
    public Object testField4081 = new Integer(4081);
    public Object testField4082 = new Integer(4082);
    public Object testField4083 = new Integer(4083);
    public Object testField4084 = new Integer(4084);
    public Object testField4085 = new Integer(4085);
    public Object testField4086 = new Integer(4086);
    public Object testField4087 = new Integer(4087);
    public Object testField4088 = new Integer(4088);
    public Object testField4089 = new Integer(4089);
    public Object testField4090 = new Integer(4090);
    public Object testField4091 = new Integer(4091);
    public Object testField4092 = new Integer(4092);
    public Object testField4093 = new Integer(4093);
    public Object testField4094 = new Integer(4094);
    public Object testField4095 = new Integer(4095);
    public Object testField4096 = new Integer(4096);
    public Object testField4097 = new Integer(4097);
    public Object testField4098 = new Integer(4098);
    public Object testField4099 = new Integer(4099);
    public Object testField4100 = new Integer(4100);
    public Object testField4101 = new Integer(4101);
    public Object testField4102 = new Integer(4102);
    public Object testField4103 = new Integer(4103);
    public Object testField4104 = new Integer(4104);
    public Object testField4105 = new Integer(4105);
    public Object testField4106 = new Integer(4106);
    public Object testField4107 = new Integer(4107);
    public Object testField4108 = new Integer(4108);
    public Object testField4109 = new Integer(4109);
    public Object testField4110 = new Integer(4110);
    public Object testField4111 = new Integer(4111);
    public Object testField4112 = new Integer(4112);
    public Object testField4113 = new Integer(4113);
    public Object testField4114 = new Integer(4114);
    public Object testField4115 = new Integer(4115);
    public Object testField4116 = new Integer(4116);
    public Object testField4117 = new Integer(4117);
    public Object testField4118 = new Integer(4118);
    public Object testField4119 = new Integer(4119);
    public Object testField4120 = new Integer(4120);
    public Object testField4121 = new Integer(4121);
    public Object testField4122 = new Integer(4122);
    public Object testField4123 = new Integer(4123);
    public Object testField4124 = new Integer(4124);
    public Object testField4125 = new Integer(4125);
    public Object testField4126 = new Integer(4126);
    public Object testField4127 = new Integer(4127);
    public Object testField4128 = new Integer(4128);
    public Object testField4129 = new Integer(4129);
    public Object testField4130 = new Integer(4130);
    public Object testField4131 = new Integer(4131);
    public Object testField4132 = new Integer(4132);
    public Object testField4133 = new Integer(4133);
    public Object testField4134 = new Integer(4134);
    public Object testField4135 = new Integer(4135);
    public Object testField4136 = new Integer(4136);
    public Object testField4137 = new Integer(4137);
    public Object testField4138 = new Integer(4138);
    public Object testField4139 = new Integer(4139);
    public Object testField4140 = new Integer(4140);
    public Object testField4141 = new Integer(4141);
    public Object testField4142 = new Integer(4142);
    public Object testField4143 = new Integer(4143);
    public Object testField4144 = new Integer(4144);
    public Object testField4145 = new Integer(4145);
    public Object testField4146 = new Integer(4146);
    public Object testField4147 = new Integer(4147);
    public Object testField4148 = new Integer(4148);
    public Object testField4149 = new Integer(4149);
    public Object testField4150 = new Integer(4150);
    public Object testField4151 = new Integer(4151);
    public Object testField4152 = new Integer(4152);
    public Object testField4153 = new Integer(4153);
    public Object testField4154 = new Integer(4154);
    public Object testField4155 = new Integer(4155);
    public Object testField4156 = new Integer(4156);
    public Object testField4157 = new Integer(4157);
    public Object testField4158 = new Integer(4158);
    public Object testField4159 = new Integer(4159);
    public Object testField4160 = new Integer(4160);
    public Object testField4161 = new Integer(4161);
    public Object testField4162 = new Integer(4162);
    public Object testField4163 = new Integer(4163);
    public Object testField4164 = new Integer(4164);
    public Object testField4165 = new Integer(4165);
    public Object testField4166 = new Integer(4166);
    public Object testField4167 = new Integer(4167);
    public Object testField4168 = new Integer(4168);
    public Object testField4169 = new Integer(4169);
    public Object testField4170 = new Integer(4170);
    public Object testField4171 = new Integer(4171);
    public Object testField4172 = new Integer(4172);
    public Object testField4173 = new Integer(4173);
    public Object testField4174 = new Integer(4174);
    public Object testField4175 = new Integer(4175);
    public Object testField4176 = new Integer(4176);
    public Object testField4177 = new Integer(4177);
    public Object testField4178 = new Integer(4178);
    public Object testField4179 = new Integer(4179);
    public Object testField4180 = new Integer(4180);
    public Object testField4181 = new Integer(4181);
    public Object testField4182 = new Integer(4182);
    public Object testField4183 = new Integer(4183);
    public Object testField4184 = new Integer(4184);
    public Object testField4185 = new Integer(4185);
    public Object testField4186 = new Integer(4186);
    public Object testField4187 = new Integer(4187);
    public Object testField4188 = new Integer(4188);
    public Object testField4189 = new Integer(4189);
    public Object testField4190 = new Integer(4190);
    public Object testField4191 = new Integer(4191);
    public Object testField4192 = new Integer(4192);
    public Object testField4193 = new Integer(4193);
    public Object testField4194 = new Integer(4194);
    public Object testField4195 = new Integer(4195);
    public Object testField4196 = new Integer(4196);
    public Object testField4197 = new Integer(4197);
    public Object testField4198 = new Integer(4198);
    public Object testField4199 = new Integer(4199);
    public Object testField4200 = new Integer(4200);
    public Object testField4201 = new Integer(4201);
    public Object testField4202 = new Integer(4202);
    public Object testField4203 = new Integer(4203);
    public Object testField4204 = new Integer(4204);
    public Object testField4205 = new Integer(4205);
    public Object testField4206 = new Integer(4206);
    public Object testField4207 = new Integer(4207);
    public Object testField4208 = new Integer(4208);
    public Object testField4209 = new Integer(4209);
    public Object testField4210 = new Integer(4210);
    public Object testField4211 = new Integer(4211);
    public Object testField4212 = new Integer(4212);
    public Object testField4213 = new Integer(4213);
    public Object testField4214 = new Integer(4214);
    public Object testField4215 = new Integer(4215);
    public Object testField4216 = new Integer(4216);
    public Object testField4217 = new Integer(4217);
    public Object testField4218 = new Integer(4218);
    public Object testField4219 = new Integer(4219);
    public Object testField4220 = new Integer(4220);
    public Object testField4221 = new Integer(4221);
    public Object testField4222 = new Integer(4222);
    public Object testField4223 = new Integer(4223);
    public Object testField4224 = new Integer(4224);
    public Object testField4225 = new Integer(4225);
    public Object testField4226 = new Integer(4226);
    public Object testField4227 = new Integer(4227);
    public Object testField4228 = new Integer(4228);
    public Object testField4229 = new Integer(4229);
    public Object testField4230 = new Integer(4230);
    public Object testField4231 = new Integer(4231);
    public Object testField4232 = new Integer(4232);
    public Object testField4233 = new Integer(4233);
    public Object testField4234 = new Integer(4234);
    public Object testField4235 = new Integer(4235);
    public Object testField4236 = new Integer(4236);
    public Object testField4237 = new Integer(4237);
    public Object testField4238 = new Integer(4238);
    public Object testField4239 = new Integer(4239);
    public Object testField4240 = new Integer(4240);
    public Object testField4241 = new Integer(4241);
    public Object testField4242 = new Integer(4242);
    public Object testField4243 = new Integer(4243);
    public Object testField4244 = new Integer(4244);
    public Object testField4245 = new Integer(4245);
    public Object testField4246 = new Integer(4246);
    public Object testField4247 = new Integer(4247);
    public Object testField4248 = new Integer(4248);
    public Object testField4249 = new Integer(4249);
    public Object testField4250 = new Integer(4250);
    public Object testField4251 = new Integer(4251);
    public Object testField4252 = new Integer(4252);
    public Object testField4253 = new Integer(4253);
    public Object testField4254 = new Integer(4254);
    public Object testField4255 = new Integer(4255);
    public Object testField4256 = new Integer(4256);
    public Object testField4257 = new Integer(4257);
    public Object testField4258 = new Integer(4258);
    public Object testField4259 = new Integer(4259);
    public Object testField4260 = new Integer(4260);
    public Object testField4261 = new Integer(4261);
    public Object testField4262 = new Integer(4262);
    public Object testField4263 = new Integer(4263);
    public Object testField4264 = new Integer(4264);
    public Object testField4265 = new Integer(4265);
    public Object testField4266 = new Integer(4266);
    public Object testField4267 = new Integer(4267);
    public Object testField4268 = new Integer(4268);
    public Object testField4269 = new Integer(4269);
    public Object testField4270 = new Integer(4270);
    public Object testField4271 = new Integer(4271);
    public Object testField4272 = new Integer(4272);
    public Object testField4273 = new Integer(4273);
    public Object testField4274 = new Integer(4274);
    public Object testField4275 = new Integer(4275);
    public Object testField4276 = new Integer(4276);
    public Object testField4277 = new Integer(4277);
    public Object testField4278 = new Integer(4278);
    public Object testField4279 = new Integer(4279);
    public Object testField4280 = new Integer(4280);
    public Object testField4281 = new Integer(4281);
    public Object testField4282 = new Integer(4282);
    public Object testField4283 = new Integer(4283);
    public Object testField4284 = new Integer(4284);
    public Object testField4285 = new Integer(4285);
    public Object testField4286 = new Integer(4286);
    public Object testField4287 = new Integer(4287);
    public Object testField4288 = new Integer(4288);
    public Object testField4289 = new Integer(4289);
    public Object testField4290 = new Integer(4290);
    public Object testField4291 = new Integer(4291);
    public Object testField4292 = new Integer(4292);
    public Object testField4293 = new Integer(4293);
    public Object testField4294 = new Integer(4294);
    public Object testField4295 = new Integer(4295);
    public Object testField4296 = new Integer(4296);
    public Object testField4297 = new Integer(4297);
    public Object testField4298 = new Integer(4298);
    public Object testField4299 = new Integer(4299);
    public Object testField4300 = new Integer(4300);
    public Object testField4301 = new Integer(4301);
    public Object testField4302 = new Integer(4302);
    public Object testField4303 = new Integer(4303);
    public Object testField4304 = new Integer(4304);
    public Object testField4305 = new Integer(4305);
    public Object testField4306 = new Integer(4306);
    public Object testField4307 = new Integer(4307);
    public Object testField4308 = new Integer(4308);
    public Object testField4309 = new Integer(4309);
    public Object testField4310 = new Integer(4310);
    public Object testField4311 = new Integer(4311);
    public Object testField4312 = new Integer(4312);
    public Object testField4313 = new Integer(4313);
    public Object testField4314 = new Integer(4314);
    public Object testField4315 = new Integer(4315);
    public Object testField4316 = new Integer(4316);
    public Object testField4317 = new Integer(4317);
    public Object testField4318 = new Integer(4318);
    public Object testField4319 = new Integer(4319);
    public Object testField4320 = new Integer(4320);
    public Object testField4321 = new Integer(4321);
    public Object testField4322 = new Integer(4322);
    public Object testField4323 = new Integer(4323);
    public Object testField4324 = new Integer(4324);
    public Object testField4325 = new Integer(4325);
    public Object testField4326 = new Integer(4326);
    public Object testField4327 = new Integer(4327);
    public Object testField4328 = new Integer(4328);
    public Object testField4329 = new Integer(4329);
    public Object testField4330 = new Integer(4330);
    public Object testField4331 = new Integer(4331);
    public Object testField4332 = new Integer(4332);
    public Object testField4333 = new Integer(4333);
    public Object testField4334 = new Integer(4334);
    public Object testField4335 = new Integer(4335);
    public Object testField4336 = new Integer(4336);
    public Object testField4337 = new Integer(4337);
    public Object testField4338 = new Integer(4338);
    public Object testField4339 = new Integer(4339);
    public Object testField4340 = new Integer(4340);
    public Object testField4341 = new Integer(4341);
    public Object testField4342 = new Integer(4342);
    public Object testField4343 = new Integer(4343);
    public Object testField4344 = new Integer(4344);
    public Object testField4345 = new Integer(4345);
    public Object testField4346 = new Integer(4346);
    public Object testField4347 = new Integer(4347);
    public Object testField4348 = new Integer(4348);
    public Object testField4349 = new Integer(4349);
    public Object testField4350 = new Integer(4350);
    public Object testField4351 = new Integer(4351);
    public Object testField4352 = new Integer(4352);
    public Object testField4353 = new Integer(4353);
    public Object testField4354 = new Integer(4354);
    public Object testField4355 = new Integer(4355);
    public Object testField4356 = new Integer(4356);
    public Object testField4357 = new Integer(4357);
    public Object testField4358 = new Integer(4358);
    public Object testField4359 = new Integer(4359);
    public Object testField4360 = new Integer(4360);
    public Object testField4361 = new Integer(4361);
    public Object testField4362 = new Integer(4362);
    public Object testField4363 = new Integer(4363);
    public Object testField4364 = new Integer(4364);
    public Object testField4365 = new Integer(4365);
    public Object testField4366 = new Integer(4366);
    public Object testField4367 = new Integer(4367);
    public Object testField4368 = new Integer(4368);
    public Object testField4369 = new Integer(4369);
    public Object testField4370 = new Integer(4370);
    public Object testField4371 = new Integer(4371);
    public Object testField4372 = new Integer(4372);
    public Object testField4373 = new Integer(4373);
    public Object testField4374 = new Integer(4374);
    public Object testField4375 = new Integer(4375);
    public Object testField4376 = new Integer(4376);
    public Object testField4377 = new Integer(4377);
    public Object testField4378 = new Integer(4378);
    public Object testField4379 = new Integer(4379);
    public Object testField4380 = new Integer(4380);
    public Object testField4381 = new Integer(4381);
    public Object testField4382 = new Integer(4382);
    public Object testField4383 = new Integer(4383);
    public Object testField4384 = new Integer(4384);
    public Object testField4385 = new Integer(4385);
    public Object testField4386 = new Integer(4386);
    public Object testField4387 = new Integer(4387);
    public Object testField4388 = new Integer(4388);
    public Object testField4389 = new Integer(4389);
    public Object testField4390 = new Integer(4390);
    public Object testField4391 = new Integer(4391);
    public Object testField4392 = new Integer(4392);
    public Object testField4393 = new Integer(4393);
    public Object testField4394 = new Integer(4394);
    public Object testField4395 = new Integer(4395);
    public Object testField4396 = new Integer(4396);
    public Object testField4397 = new Integer(4397);
    public Object testField4398 = new Integer(4398);
    public Object testField4399 = new Integer(4399);
    public Object testField4400 = new Integer(4400);
    public Object testField4401 = new Integer(4401);
    public Object testField4402 = new Integer(4402);
    public Object testField4403 = new Integer(4403);
    public Object testField4404 = new Integer(4404);
    public Object testField4405 = new Integer(4405);
    public Object testField4406 = new Integer(4406);
    public Object testField4407 = new Integer(4407);
    public Object testField4408 = new Integer(4408);
    public Object testField4409 = new Integer(4409);
    public Object testField4410 = new Integer(4410);
    public Object testField4411 = new Integer(4411);
    public Object testField4412 = new Integer(4412);
    public Object testField4413 = new Integer(4413);
    public Object testField4414 = new Integer(4414);
    public Object testField4415 = new Integer(4415);
    public Object testField4416 = new Integer(4416);
    public Object testField4417 = new Integer(4417);
    public Object testField4418 = new Integer(4418);
    public Object testField4419 = new Integer(4419);
    public Object testField4420 = new Integer(4420);
    public Object testField4421 = new Integer(4421);
    public Object testField4422 = new Integer(4422);
    public Object testField4423 = new Integer(4423);
    public Object testField4424 = new Integer(4424);
    public Object testField4425 = new Integer(4425);
    public Object testField4426 = new Integer(4426);
    public Object testField4427 = new Integer(4427);
    public Object testField4428 = new Integer(4428);
    public Object testField4429 = new Integer(4429);
    public Object testField4430 = new Integer(4430);
    public Object testField4431 = new Integer(4431);
    public Object testField4432 = new Integer(4432);
    public Object testField4433 = new Integer(4433);
    public Object testField4434 = new Integer(4434);
    public Object testField4435 = new Integer(4435);
    public Object testField4436 = new Integer(4436);
    public Object testField4437 = new Integer(4437);
    public Object testField4438 = new Integer(4438);
    public Object testField4439 = new Integer(4439);
    public Object testField4440 = new Integer(4440);
    public Object testField4441 = new Integer(4441);
    public Object testField4442 = new Integer(4442);
    public Object testField4443 = new Integer(4443);
    public Object testField4444 = new Integer(4444);
    public Object testField4445 = new Integer(4445);
    public Object testField4446 = new Integer(4446);
    public Object testField4447 = new Integer(4447);
    public Object testField4448 = new Integer(4448);
    public Object testField4449 = new Integer(4449);
    public Object testField4450 = new Integer(4450);
    public Object testField4451 = new Integer(4451);
    public Object testField4452 = new Integer(4452);
    public Object testField4453 = new Integer(4453);
    public Object testField4454 = new Integer(4454);
    public Object testField4455 = new Integer(4455);
    public Object testField4456 = new Integer(4456);
    public Object testField4457 = new Integer(4457);
    public Object testField4458 = new Integer(4458);
    public Object testField4459 = new Integer(4459);
    public Object testField4460 = new Integer(4460);
    public Object testField4461 = new Integer(4461);
    public Object testField4462 = new Integer(4462);
    public Object testField4463 = new Integer(4463);
    public Object testField4464 = new Integer(4464);
    public Object testField4465 = new Integer(4465);
    public Object testField4466 = new Integer(4466);
    public Object testField4467 = new Integer(4467);
    public Object testField4468 = new Integer(4468);
    public Object testField4469 = new Integer(4469);
    public Object testField4470 = new Integer(4470);
    public Object testField4471 = new Integer(4471);
    public Object testField4472 = new Integer(4472);
    public Object testField4473 = new Integer(4473);
    public Object testField4474 = new Integer(4474);
    public Object testField4475 = new Integer(4475);
    public Object testField4476 = new Integer(4476);
    public Object testField4477 = new Integer(4477);
    public Object testField4478 = new Integer(4478);
    public Object testField4479 = new Integer(4479);
    public Object testField4480 = new Integer(4480);
    public Object testField4481 = new Integer(4481);
    public Object testField4482 = new Integer(4482);
    public Object testField4483 = new Integer(4483);
    public Object testField4484 = new Integer(4484);
    public Object testField4485 = new Integer(4485);
    public Object testField4486 = new Integer(4486);
    public Object testField4487 = new Integer(4487);
    public Object testField4488 = new Integer(4488);
    public Object testField4489 = new Integer(4489);
    public Object testField4490 = new Integer(4490);
    public Object testField4491 = new Integer(4491);
    public Object testField4492 = new Integer(4492);
    public Object testField4493 = new Integer(4493);
    public Object testField4494 = new Integer(4494);
    public Object testField4495 = new Integer(4495);
    public Object testField4496 = new Integer(4496);
    public Object testField4497 = new Integer(4497);
    public Object testField4498 = new Integer(4498);
    public Object testField4499 = new Integer(4499);
    public Object testField4500 = new Integer(4500);
    public Object testField4501 = new Integer(4501);
    public Object testField4502 = new Integer(4502);
    public Object testField4503 = new Integer(4503);
    public Object testField4504 = new Integer(4504);
    public Object testField4505 = new Integer(4505);
    public Object testField4506 = new Integer(4506);
    public Object testField4507 = new Integer(4507);
    public Object testField4508 = new Integer(4508);
    public Object testField4509 = new Integer(4509);
    public Object testField4510 = new Integer(4510);
    public Object testField4511 = new Integer(4511);
    public Object testField4512 = new Integer(4512);
    public Object testField4513 = new Integer(4513);
    public Object testField4514 = new Integer(4514);
    public Object testField4515 = new Integer(4515);
    public Object testField4516 = new Integer(4516);
    public Object testField4517 = new Integer(4517);
    public Object testField4518 = new Integer(4518);
    public Object testField4519 = new Integer(4519);
    public Object testField4520 = new Integer(4520);
    public Object testField4521 = new Integer(4521);
    public Object testField4522 = new Integer(4522);
    public Object testField4523 = new Integer(4523);
    public Object testField4524 = new Integer(4524);
    public Object testField4525 = new Integer(4525);
    public Object testField4526 = new Integer(4526);
    public Object testField4527 = new Integer(4527);
    public Object testField4528 = new Integer(4528);
    public Object testField4529 = new Integer(4529);
    public Object testField4530 = new Integer(4530);
    public Object testField4531 = new Integer(4531);
    public Object testField4532 = new Integer(4532);
    public Object testField4533 = new Integer(4533);
    public Object testField4534 = new Integer(4534);
    public Object testField4535 = new Integer(4535);
    public Object testField4536 = new Integer(4536);
    public Object testField4537 = new Integer(4537);
    public Object testField4538 = new Integer(4538);
    public Object testField4539 = new Integer(4539);
    public Object testField4540 = new Integer(4540);
    public Object testField4541 = new Integer(4541);
    public Object testField4542 = new Integer(4542);
    public Object testField4543 = new Integer(4543);
    public Object testField4544 = new Integer(4544);
    public Object testField4545 = new Integer(4545);
    public Object testField4546 = new Integer(4546);
    public Object testField4547 = new Integer(4547);
    public Object testField4548 = new Integer(4548);
    public Object testField4549 = new Integer(4549);
    public Object testField4550 = new Integer(4550);
    public Object testField4551 = new Integer(4551);
    public Object testField4552 = new Integer(4552);
    public Object testField4553 = new Integer(4553);
    public Object testField4554 = new Integer(4554);
    public Object testField4555 = new Integer(4555);
    public Object testField4556 = new Integer(4556);
    public Object testField4557 = new Integer(4557);
    public Object testField4558 = new Integer(4558);
    public Object testField4559 = new Integer(4559);
    public Object testField4560 = new Integer(4560);
    public Object testField4561 = new Integer(4561);
    public Object testField4562 = new Integer(4562);
    public Object testField4563 = new Integer(4563);
    public Object testField4564 = new Integer(4564);
    public Object testField4565 = new Integer(4565);
    public Object testField4566 = new Integer(4566);
    public Object testField4567 = new Integer(4567);
    public Object testField4568 = new Integer(4568);
    public Object testField4569 = new Integer(4569);
    public Object testField4570 = new Integer(4570);
    public Object testField4571 = new Integer(4571);
    public Object testField4572 = new Integer(4572);
    public Object testField4573 = new Integer(4573);
    public Object testField4574 = new Integer(4574);
    public Object testField4575 = new Integer(4575);
    public Object testField4576 = new Integer(4576);
    public Object testField4577 = new Integer(4577);
    public Object testField4578 = new Integer(4578);
    public Object testField4579 = new Integer(4579);
    public Object testField4580 = new Integer(4580);
    public Object testField4581 = new Integer(4581);
    public Object testField4582 = new Integer(4582);
    public Object testField4583 = new Integer(4583);
    public Object testField4584 = new Integer(4584);
    public Object testField4585 = new Integer(4585);
    public Object testField4586 = new Integer(4586);
    public Object testField4587 = new Integer(4587);
    public Object testField4588 = new Integer(4588);
    public Object testField4589 = new Integer(4589);
    public Object testField4590 = new Integer(4590);
    public Object testField4591 = new Integer(4591);
    public Object testField4592 = new Integer(4592);
    public Object testField4593 = new Integer(4593);
    public Object testField4594 = new Integer(4594);
    public Object testField4595 = new Integer(4595);
    public Object testField4596 = new Integer(4596);
    public Object testField4597 = new Integer(4597);
    public Object testField4598 = new Integer(4598);
    public Object testField4599 = new Integer(4599);
    public Object testField4600 = new Integer(4600);
    public Object testField4601 = new Integer(4601);
    public Object testField4602 = new Integer(4602);
    public Object testField4603 = new Integer(4603);
    public Object testField4604 = new Integer(4604);
    public Object testField4605 = new Integer(4605);
    public Object testField4606 = new Integer(4606);
    public Object testField4607 = new Integer(4607);
    public Object testField4608 = new Integer(4608);
    public Object testField4609 = new Integer(4609);
    public Object testField4610 = new Integer(4610);
    public Object testField4611 = new Integer(4611);
    public Object testField4612 = new Integer(4612);
    public Object testField4613 = new Integer(4613);
    public Object testField4614 = new Integer(4614);
    public Object testField4615 = new Integer(4615);
    public Object testField4616 = new Integer(4616);
    public Object testField4617 = new Integer(4617);
    public Object testField4618 = new Integer(4618);
    public Object testField4619 = new Integer(4619);
    public Object testField4620 = new Integer(4620);
    public Object testField4621 = new Integer(4621);
    public Object testField4622 = new Integer(4622);
    public Object testField4623 = new Integer(4623);
    public Object testField4624 = new Integer(4624);
    public Object testField4625 = new Integer(4625);
    public Object testField4626 = new Integer(4626);
    public Object testField4627 = new Integer(4627);
    public Object testField4628 = new Integer(4628);
    public Object testField4629 = new Integer(4629);
    public Object testField4630 = new Integer(4630);
    public Object testField4631 = new Integer(4631);
    public Object testField4632 = new Integer(4632);
    public Object testField4633 = new Integer(4633);
    public Object testField4634 = new Integer(4634);
    public Object testField4635 = new Integer(4635);
    public Object testField4636 = new Integer(4636);
    public Object testField4637 = new Integer(4637);
    public Object testField4638 = new Integer(4638);
    public Object testField4639 = new Integer(4639);
    public Object testField4640 = new Integer(4640);
    public Object testField4641 = new Integer(4641);
    public Object testField4642 = new Integer(4642);
    public Object testField4643 = new Integer(4643);
    public Object testField4644 = new Integer(4644);
    public Object testField4645 = new Integer(4645);
    public Object testField4646 = new Integer(4646);
    public Object testField4647 = new Integer(4647);
    public Object testField4648 = new Integer(4648);
    public Object testField4649 = new Integer(4649);
    public Object testField4650 = new Integer(4650);
    public Object testField4651 = new Integer(4651);
    public Object testField4652 = new Integer(4652);
    public Object testField4653 = new Integer(4653);
    public Object testField4654 = new Integer(4654);
    public Object testField4655 = new Integer(4655);
    public Object testField4656 = new Integer(4656);
    public Object testField4657 = new Integer(4657);
    public Object testField4658 = new Integer(4658);
    public Object testField4659 = new Integer(4659);
    public Object testField4660 = new Integer(4660);
    public Object testField4661 = new Integer(4661);
    public Object testField4662 = new Integer(4662);
    public Object testField4663 = new Integer(4663);
    public Object testField4664 = new Integer(4664);
    public Object testField4665 = new Integer(4665);
    public Object testField4666 = new Integer(4666);
    public Object testField4667 = new Integer(4667);
    public Object testField4668 = new Integer(4668);
    public Object testField4669 = new Integer(4669);
    public Object testField4670 = new Integer(4670);
    public Object testField4671 = new Integer(4671);
    public Object testField4672 = new Integer(4672);
    public Object testField4673 = new Integer(4673);
    public Object testField4674 = new Integer(4674);
    public Object testField4675 = new Integer(4675);
    public Object testField4676 = new Integer(4676);
    public Object testField4677 = new Integer(4677);
    public Object testField4678 = new Integer(4678);
    public Object testField4679 = new Integer(4679);
    public Object testField4680 = new Integer(4680);
    public Object testField4681 = new Integer(4681);
    public Object testField4682 = new Integer(4682);
    public Object testField4683 = new Integer(4683);
    public Object testField4684 = new Integer(4684);
    public Object testField4685 = new Integer(4685);
    public Object testField4686 = new Integer(4686);
    public Object testField4687 = new Integer(4687);
    public Object testField4688 = new Integer(4688);
    public Object testField4689 = new Integer(4689);
    public Object testField4690 = new Integer(4690);
    public Object testField4691 = new Integer(4691);
    public Object testField4692 = new Integer(4692);
    public Object testField4693 = new Integer(4693);
    public Object testField4694 = new Integer(4694);
    public Object testField4695 = new Integer(4695);
    public Object testField4696 = new Integer(4696);
    public Object testField4697 = new Integer(4697);
    public Object testField4698 = new Integer(4698);
    public Object testField4699 = new Integer(4699);
    public Object testField4700 = new Integer(4700);
    public Object testField4701 = new Integer(4701);
    public Object testField4702 = new Integer(4702);
    public Object testField4703 = new Integer(4703);
    public Object testField4704 = new Integer(4704);
    public Object testField4705 = new Integer(4705);
    public Object testField4706 = new Integer(4706);
    public Object testField4707 = new Integer(4707);
    public Object testField4708 = new Integer(4708);
    public Object testField4709 = new Integer(4709);
    public Object testField4710 = new Integer(4710);
    public Object testField4711 = new Integer(4711);
    public Object testField4712 = new Integer(4712);
    public Object testField4713 = new Integer(4713);
    public Object testField4714 = new Integer(4714);
    public Object testField4715 = new Integer(4715);
    public Object testField4716 = new Integer(4716);
    public Object testField4717 = new Integer(4717);
    public Object testField4718 = new Integer(4718);
    public Object testField4719 = new Integer(4719);
    public Object testField4720 = new Integer(4720);
    public Object testField4721 = new Integer(4721);
    public Object testField4722 = new Integer(4722);
    public Object testField4723 = new Integer(4723);
    public Object testField4724 = new Integer(4724);
    public Object testField4725 = new Integer(4725);
    public Object testField4726 = new Integer(4726);
    public Object testField4727 = new Integer(4727);
    public Object testField4728 = new Integer(4728);
    public Object testField4729 = new Integer(4729);
    public Object testField4730 = new Integer(4730);
    public Object testField4731 = new Integer(4731);
    public Object testField4732 = new Integer(4732);
    public Object testField4733 = new Integer(4733);
    public Object testField4734 = new Integer(4734);
    public Object testField4735 = new Integer(4735);
    public Object testField4736 = new Integer(4736);
    public Object testField4737 = new Integer(4737);
    public Object testField4738 = new Integer(4738);
    public Object testField4739 = new Integer(4739);
    public Object testField4740 = new Integer(4740);
    public Object testField4741 = new Integer(4741);
    public Object testField4742 = new Integer(4742);
    public Object testField4743 = new Integer(4743);
    public Object testField4744 = new Integer(4744);
    public Object testField4745 = new Integer(4745);
    public Object testField4746 = new Integer(4746);
    public Object testField4747 = new Integer(4747);
    public Object testField4748 = new Integer(4748);
    public Object testField4749 = new Integer(4749);
    public Object testField4750 = new Integer(4750);
    public Object testField4751 = new Integer(4751);
    public Object testField4752 = new Integer(4752);
    public Object testField4753 = new Integer(4753);
    public Object testField4754 = new Integer(4754);
    public Object testField4755 = new Integer(4755);
    public Object testField4756 = new Integer(4756);
    public Object testField4757 = new Integer(4757);
    public Object testField4758 = new Integer(4758);
    public Object testField4759 = new Integer(4759);
    public Object testField4760 = new Integer(4760);
    public Object testField4761 = new Integer(4761);
    public Object testField4762 = new Integer(4762);
    public Object testField4763 = new Integer(4763);
    public Object testField4764 = new Integer(4764);
    public Object testField4765 = new Integer(4765);
    public Object testField4766 = new Integer(4766);
    public Object testField4767 = new Integer(4767);
    public Object testField4768 = new Integer(4768);
    public Object testField4769 = new Integer(4769);
    public Object testField4770 = new Integer(4770);
    public Object testField4771 = new Integer(4771);
    public Object testField4772 = new Integer(4772);
    public Object testField4773 = new Integer(4773);
    public Object testField4774 = new Integer(4774);
    public Object testField4775 = new Integer(4775);
    public Object testField4776 = new Integer(4776);
    public Object testField4777 = new Integer(4777);
    public Object testField4778 = new Integer(4778);
    public Object testField4779 = new Integer(4779);
    public Object testField4780 = new Integer(4780);
    public Object testField4781 = new Integer(4781);
    public Object testField4782 = new Integer(4782);
    public Object testField4783 = new Integer(4783);
    public Object testField4784 = new Integer(4784);
    public Object testField4785 = new Integer(4785);
    public Object testField4786 = new Integer(4786);
    public Object testField4787 = new Integer(4787);
    public Object testField4788 = new Integer(4788);
    public Object testField4789 = new Integer(4789);
    public Object testField4790 = new Integer(4790);
    public Object testField4791 = new Integer(4791);
    public Object testField4792 = new Integer(4792);
    public Object testField4793 = new Integer(4793);
    public Object testField4794 = new Integer(4794);
    public Object testField4795 = new Integer(4795);
    public Object testField4796 = new Integer(4796);
    public Object testField4797 = new Integer(4797);
    public Object testField4798 = new Integer(4798);
    public Object testField4799 = new Integer(4799);
    public Object testField4800 = new Integer(4800);
    public Object testField4801 = new Integer(4801);
    public Object testField4802 = new Integer(4802);
    public Object testField4803 = new Integer(4803);
    public Object testField4804 = new Integer(4804);
    public Object testField4805 = new Integer(4805);
    public Object testField4806 = new Integer(4806);
    public Object testField4807 = new Integer(4807);
    public Object testField4808 = new Integer(4808);
    public Object testField4809 = new Integer(4809);
    public Object testField4810 = new Integer(4810);
    public Object testField4811 = new Integer(4811);
    public Object testField4812 = new Integer(4812);
    public Object testField4813 = new Integer(4813);
    public Object testField4814 = new Integer(4814);
    public Object testField4815 = new Integer(4815);
    public Object testField4816 = new Integer(4816);
    public Object testField4817 = new Integer(4817);
    public Object testField4818 = new Integer(4818);
    public Object testField4819 = new Integer(4819);
    public Object testField4820 = new Integer(4820);
    public Object testField4821 = new Integer(4821);
    public Object testField4822 = new Integer(4822);
    public Object testField4823 = new Integer(4823);
    public Object testField4824 = new Integer(4824);
    public Object testField4825 = new Integer(4825);
    public Object testField4826 = new Integer(4826);
    public Object testField4827 = new Integer(4827);
    public Object testField4828 = new Integer(4828);
    public Object testField4829 = new Integer(4829);
    public Object testField4830 = new Integer(4830);
    public Object testField4831 = new Integer(4831);
    public Object testField4832 = new Integer(4832);
    public Object testField4833 = new Integer(4833);
    public Object testField4834 = new Integer(4834);
    public Object testField4835 = new Integer(4835);
    public Object testField4836 = new Integer(4836);
    public Object testField4837 = new Integer(4837);
    public Object testField4838 = new Integer(4838);
    public Object testField4839 = new Integer(4839);
    public Object testField4840 = new Integer(4840);
    public Object testField4841 = new Integer(4841);
    public Object testField4842 = new Integer(4842);
    public Object testField4843 = new Integer(4843);
    public Object testField4844 = new Integer(4844);
    public Object testField4845 = new Integer(4845);
    public Object testField4846 = new Integer(4846);
    public Object testField4847 = new Integer(4847);
    public Object testField4848 = new Integer(4848);
    public Object testField4849 = new Integer(4849);
    public Object testField4850 = new Integer(4850);
    public Object testField4851 = new Integer(4851);
    public Object testField4852 = new Integer(4852);
    public Object testField4853 = new Integer(4853);
    public Object testField4854 = new Integer(4854);
    public Object testField4855 = new Integer(4855);
    public Object testField4856 = new Integer(4856);
    public Object testField4857 = new Integer(4857);
    public Object testField4858 = new Integer(4858);
    public Object testField4859 = new Integer(4859);
    public Object testField4860 = new Integer(4860);
    public Object testField4861 = new Integer(4861);
    public Object testField4862 = new Integer(4862);
    public Object testField4863 = new Integer(4863);
    public Object testField4864 = new Integer(4864);
    public Object testField4865 = new Integer(4865);
    public Object testField4866 = new Integer(4866);
    public Object testField4867 = new Integer(4867);
    public Object testField4868 = new Integer(4868);
    public Object testField4869 = new Integer(4869);
    public Object testField4870 = new Integer(4870);
    public Object testField4871 = new Integer(4871);
    public Object testField4872 = new Integer(4872);
    public Object testField4873 = new Integer(4873);
    public Object testField4874 = new Integer(4874);
    public Object testField4875 = new Integer(4875);
    public Object testField4876 = new Integer(4876);
    public Object testField4877 = new Integer(4877);
    public Object testField4878 = new Integer(4878);
    public Object testField4879 = new Integer(4879);
    public Object testField4880 = new Integer(4880);
    public Object testField4881 = new Integer(4881);
    public Object testField4882 = new Integer(4882);
    public Object testField4883 = new Integer(4883);
    public Object testField4884 = new Integer(4884);
    public Object testField4885 = new Integer(4885);
    public Object testField4886 = new Integer(4886);
    public Object testField4887 = new Integer(4887);
    public Object testField4888 = new Integer(4888);
    public Object testField4889 = new Integer(4889);
    public Object testField4890 = new Integer(4890);
    public Object testField4891 = new Integer(4891);
    public Object testField4892 = new Integer(4892);
    public Object testField4893 = new Integer(4893);
    public Object testField4894 = new Integer(4894);
    public Object testField4895 = new Integer(4895);
    public Object testField4896 = new Integer(4896);
    public Object testField4897 = new Integer(4897);
    public Object testField4898 = new Integer(4898);
    public Object testField4899 = new Integer(4899);
    public Object testField4900 = new Integer(4900);
    public Object testField4901 = new Integer(4901);
    public Object testField4902 = new Integer(4902);
    public Object testField4903 = new Integer(4903);
    public Object testField4904 = new Integer(4904);
    public Object testField4905 = new Integer(4905);
    public Object testField4906 = new Integer(4906);
    public Object testField4907 = new Integer(4907);
    public Object testField4908 = new Integer(4908);
    public Object testField4909 = new Integer(4909);
    public Object testField4910 = new Integer(4910);
    public Object testField4911 = new Integer(4911);
    public Object testField4912 = new Integer(4912);
    public Object testField4913 = new Integer(4913);
    public Object testField4914 = new Integer(4914);
    public Object testField4915 = new Integer(4915);
    public Object testField4916 = new Integer(4916);
    public Object testField4917 = new Integer(4917);
    public Object testField4918 = new Integer(4918);
    public Object testField4919 = new Integer(4919);
    public Object testField4920 = new Integer(4920);
    public Object testField4921 = new Integer(4921);
    public Object testField4922 = new Integer(4922);
    public Object testField4923 = new Integer(4923);
    public Object testField4924 = new Integer(4924);
    public Object testField4925 = new Integer(4925);
    public Object testField4926 = new Integer(4926);
    public Object testField4927 = new Integer(4927);
    public Object testField4928 = new Integer(4928);
    public Object testField4929 = new Integer(4929);
    public Object testField4930 = new Integer(4930);
    public Object testField4931 = new Integer(4931);
    public Object testField4932 = new Integer(4932);
    public Object testField4933 = new Integer(4933);
    public Object testField4934 = new Integer(4934);
    public Object testField4935 = new Integer(4935);
    public Object testField4936 = new Integer(4936);
    public Object testField4937 = new Integer(4937);
    public Object testField4938 = new Integer(4938);
    public Object testField4939 = new Integer(4939);
    public Object testField4940 = new Integer(4940);
    public Object testField4941 = new Integer(4941);
    public Object testField4942 = new Integer(4942);
    public Object testField4943 = new Integer(4943);
    public Object testField4944 = new Integer(4944);
    public Object testField4945 = new Integer(4945);
    public Object testField4946 = new Integer(4946);
    public Object testField4947 = new Integer(4947);
    public Object testField4948 = new Integer(4948);
    public Object testField4949 = new Integer(4949);
    public Object testField4950 = new Integer(4950);
    public Object testField4951 = new Integer(4951);
    public Object testField4952 = new Integer(4952);
    public Object testField4953 = new Integer(4953);
    public Object testField4954 = new Integer(4954);
    public Object testField4955 = new Integer(4955);
    public Object testField4956 = new Integer(4956);
    public Object testField4957 = new Integer(4957);
    public Object testField4958 = new Integer(4958);
    public Object testField4959 = new Integer(4959);
    public Object testField4960 = new Integer(4960);
    public Object testField4961 = new Integer(4961);
    public Object testField4962 = new Integer(4962);
    public Object testField4963 = new Integer(4963);
    public Object testField4964 = new Integer(4964);
    public Object testField4965 = new Integer(4965);
    public Object testField4966 = new Integer(4966);
    public Object testField4967 = new Integer(4967);
    public Object testField4968 = new Integer(4968);
    public Object testField4969 = new Integer(4969);
    public Object testField4970 = new Integer(4970);
    public Object testField4971 = new Integer(4971);
    public Object testField4972 = new Integer(4972);
    public Object testField4973 = new Integer(4973);
    public Object testField4974 = new Integer(4974);
    public Object testField4975 = new Integer(4975);
    public Object testField4976 = new Integer(4976);
    public Object testField4977 = new Integer(4977);
    public Object testField4978 = new Integer(4978);
    public Object testField4979 = new Integer(4979);
    public Object testField4980 = new Integer(4980);
    public Object testField4981 = new Integer(4981);
    public Object testField4982 = new Integer(4982);
    public Object testField4983 = new Integer(4983);
    public Object testField4984 = new Integer(4984);
    public Object testField4985 = new Integer(4985);
    public Object testField4986 = new Integer(4986);
    public Object testField4987 = new Integer(4987);
    public Object testField4988 = new Integer(4988);
    public Object testField4989 = new Integer(4989);
    public Object testField4990 = new Integer(4990);
    public Object testField4991 = new Integer(4991);
    public Object testField4992 = new Integer(4992);
    public Object testField4993 = new Integer(4993);
    public Object testField4994 = new Integer(4994);
    public Object testField4995 = new Integer(4995);
    public Object testField4996 = new Integer(4996);
    public Object testField4997 = new Integer(4997);
    public Object testField4998 = new Integer(4998);
    public Object testField4999 = new Integer(4999);
}
