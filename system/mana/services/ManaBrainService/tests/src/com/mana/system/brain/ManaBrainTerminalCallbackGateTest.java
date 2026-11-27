package com.mana.system.brain;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public final class ManaBrainTerminalCallbackGateTest {
    @Test public void onlyFirstTerminalCallbackWinsSequentially() {
        ManaBrainTerminalCallbackGate gate = new ManaBrainTerminalCallbackGate();
        assertFalse(gate.isDelivered());
        assertTrue(gate.tryDeliver());
        assertTrue(gate.isDelivered());
        assertFalse(gate.tryDeliver());
        assertFalse(gate.tryDeliver());
    }

    @Test public void concurrentCallbacksOnlyDeliverOnce() throws Exception {
        final int callers = 64;
        ManaBrainTerminalCallbackGate gate = new ManaBrainTerminalCallbackGate();
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        Thread[] threads = new Thread[callers];
        for (int i = 0; i < callers; i++) {
            threads[i] = new Thread(() -> {
                ready.countDown();
                try {
                    if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("start gate timed out");
                    if (gate.tryDeliver()) winners.incrementAndGet();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(interrupted);
                }
            });
            threads[i].start();
        }
        assertTrue("worker threads did not become ready", ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        for (Thread thread : threads) thread.join(5_000);
        for (Thread thread : threads) assertFalse("worker thread did not finish", thread.isAlive());
        assertEquals(1, winners.get());
    }
}
