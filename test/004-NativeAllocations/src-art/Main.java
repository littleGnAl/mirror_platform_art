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

import java.lang.Runtime;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.PhantomReference;
import java.util.concurrent.atomic.AtomicInteger;
import dalvik.system.VMRuntime;

public class Main {
    static Object deadlockLock = new Object();
    static VMRuntime runtime = VMRuntime.getRuntime();
    static volatile boolean aboutToDeadlock = false;
    static final long MAX_EXPECTED_GC_DURATION_MS = 2000;

    // Save ref as a static field to ensure it doesn't get GC'd before the
    // referent is enqueued.
    static PhantomReference ref = null;

    static class DeadlockingFinalizer {
        protected void finalize() throws Exception {
            aboutToDeadlock = true;
            synchronized (deadlockLock) { }
        }
    }

    private static void $noinline$allocateDeadlockingFinalizer() {
        new DeadlockingFinalizer();
    }

    static AtomicInteger finalizeCounter = new AtomicInteger(0);

    static class IncrementingFinalizer {
        protected void finalize() throws Exception {
            finalizeCounter.incrementAndGet();
        }
    }

    private static void $noinline$allocateIncrementingFinalizer() {
        new IncrementingFinalizer();
    }

    public static PhantomReference $noinline$allocPhantom(ReferenceQueue<Object> queue) {
        return new PhantomReference(new Object(), queue);
    }

    // Test that calling registerNativeAllocation triggers a GC eventually
    // after a substantial number of registered native bytes.
    private static void checkRegisterNativeAllocation() throws Exception {
        long maxMem = Runtime.getRuntime().maxMemory();
        int size = (int)(maxMem / 32);
        int allocationCount = 256;
        final long startTime = System.currentTimeMillis();

        int initialFinalizeCount = finalizeCounter.get();
        ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        ref = $noinline$allocPhantom(queue);
        long total = 0;
        int i;
        for (i = 0; !ref.isEnqueued() && i < allocationCount; ++i) {
            runtime.registerNativeAllocation(size);
            total += size;

            // Allocate a new finalizable object each time, so that we can see if anything
            // was finalized while we were running.
            $noinline$allocateIncrementingFinalizer();

            // Sleep a little bit to ensure not all of the calls to
            // registerNativeAllocation complete while GC is in the process of
            // running.
            Thread.sleep(MAX_EXPECTED_GC_DURATION_MS / allocationCount);
        }

        // Wait up to MAX_EXPECTED_GC_DURATION_MS to give GC a chance to finish
        // running. If the reference isn't enqueued after that, then it is
        // pretty unlikely (though technically still possible) that GC was
        // triggered as intended.
        if (queue.remove(MAX_EXPECTED_GC_DURATION_MS) == null) {
            System.out.println("GC failed to complete after " + i
                + " iterations, is_enqueued = " + ref.isEnqueued());
            System.out.println("  size = " + size + ", elapsed msecs = "
                + (System.currentTimeMillis() - startTime));
            System.out.println("  original maxMemory() = " + maxMem + " current maxMemory() = "
                + Runtime.getRuntime().maxMemory());
            System.out.println("  Initial finalize count = " + initialFinalizeCount
                + " current finalize count = " + finalizeCounter.get());
            Thread.sleep(2 * MAX_EXPECTED_GC_DURATION_MS);
            System.out.println("  After delay, queue.poll() = " + queue.poll()
                + " is_enqueued = " + ref.isEnqueued());
            System.out.println("    elapsed msecs = " + (System.currentTimeMillis() - startTime));
            Runtime.getRuntime().gc();
            System.runFinalization();
            System.out.println("  After GC, queue.poll() = " + queue.poll()
                + " is_enqueued = " + ref.isEnqueued());
        }

        while (total > 0) {
            runtime.registerNativeFree(size);
            total -= size;
        }
    }

    // Call registerNativeAllocation repeatedly at a high rate to trigger the case of blocking
    // registerNativeAllocation. Stop before we risk exhausting the finalizer timeout.
    private static void triggerBlockingRegisterNativeAllocation() throws Exception {
        final long startTime = System.currentTimeMillis();
        final long finalizerTimeoutMs = VMRuntime.getRuntime().getFinalizerTimeoutMs();
        final long quittingTime = startTime + finalizerTimeoutMs - MAX_EXPECTED_GC_DURATION_MS;
        long maxMem = Runtime.getRuntime().maxMemory();
        int size = (int)(maxMem / 5);
        int allocationCount = 10;

        long total = 0;
        for (int i = 0; i < allocationCount && System.currentTimeMillis() < quittingTime; ++i) {
            runtime.registerNativeAllocation(size);
            total += size;
        }

        while (total > 0) {
            runtime.registerNativeFree(size);
            total -= size;
        }
    }

    public static void main(String[] args) throws Exception {
        // Test that registerNativeAllocation triggers GC.
        // Run this a few times in a loop to reduce the chances that the test
        // is flaky and make sure registerNativeAllocation continues to work
        // after the first GC is triggered.
        for (int i = 0; i < 20; ++i) {
            checkRegisterNativeAllocation();
        }

        // Test that we don't get a deadlock if we call
        // registerNativeAllocation with a blocked finalizer.
        synchronized (deadlockLock) {
            $noinline$allocateDeadlockingFinalizer();
            while (!aboutToDeadlock) {
                Runtime.getRuntime().gc();
            }

            // Do more allocations now that the finalizer thread is deadlocked so that we force
            // finalization and timeout.
            triggerBlockingRegisterNativeAllocation();
        }
        System.out.println("Test complete");
    }
}

