package com.mana.brain;

/** A single owner-authorized inference request. Cloud is denied unless explicitly allowed. */
parcelable BrainRequest {
    String requestId;
    String prompt;
    boolean allowCloud;
    boolean sensitive;
    String preferredCloudProvider;
}
