package com.mana.system.brain;

import java.util.concurrent.atomic.AtomicBoolean;

/** Allows exactly one terminal result/error callback for a single Binder request. */
final class ManaBrainTerminalCallbackGate {
    private final AtomicBoolean delivered = new AtomicBoolean(false);

    boolean tryDeliver() {
        return delivered.compareAndSet(false, true);
    }
}
