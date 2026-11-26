package com.mana.brain;

import com.mana.brain.BrainBinderRequest;
import com.mana.brain.IBrainCallback;
import com.mana.brain.ManaCapabilityRequest;
import com.mana.brain.ManaCapabilityResponse;

/** Private Binder boundary for MANA Assistant and Android HOME clients. */
interface IBrainService {
    void submit(in BrainBinderRequest request, IBrainCallback callback);
    boolean isReady();
    String getEngineVersion();
    ManaCapabilityResponse executeCapability(in ManaCapabilityRequest request);
}
