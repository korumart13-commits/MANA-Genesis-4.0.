package com.mana.brain;

/** Per-request policy and data contract. Cloud and memory export are opt-in. */
parcelable ManaBrainRequest {
    String text;
    boolean allowCloud = false;
    boolean sensitive = false;
    String preferredCloudProvider;
    boolean includePersonalMemory = false;
    boolean allowPersonalMemoryToCloud = false;
}
