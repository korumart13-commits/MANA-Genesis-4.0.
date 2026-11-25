package com.mana.brain;

import com.mana.brain.BrainRequest;
import com.mana.brain.IBrainCallback;

/**
 * Private Binder boundary for MANA Assistant and future OS clients. This interface is currently unstable and must be versioned before independent API delivery.
 * Implementations must enforce caller permissions and fail closed.
 */
interface IBrainService {
    void submit(in BrainRequest request, IBrainCallback callback);
    boolean isReady();
    String getEngineVersion();
}
