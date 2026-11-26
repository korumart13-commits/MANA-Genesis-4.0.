package com.mana.brain;

import com.mana.brain.BrainBinderRequest;
import com.mana.brain.IBrainCallback;

/** Private Binder boundary for MANA Assistant and Android HOME clients. */
interface IBrainService {
    void submit(in BrainBinderRequest request, IBrainCallback callback);
    boolean isReady();
    String getEngineVersion();
}
