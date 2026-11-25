package com.mana.brain;

import com.mana.brain.BrainRequest;
import com.mana.brain.IBrainCallback;

/**
 * Stable Binder boundary for MANA Assistant and future OS clients.
 * Implementations must enforce caller permissions and fail closed.
 */
interface IBrainService {
    void submit(in BrainRequest request, IBrainCallback callback);
    boolean isReady();
    String getEngineVersion();
}
