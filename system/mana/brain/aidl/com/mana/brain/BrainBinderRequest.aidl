package com.mana.brain;

/** Binder wire request. Kept distinct from the engine's Kotlin BrainRequest. */
parcelable BrainBinderRequest {
    String requestId;
    String prompt;
    boolean allowCloud;
    boolean sensitive;
    String preferredCloudProvider;
}
