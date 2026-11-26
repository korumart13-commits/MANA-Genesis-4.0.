package com.mana.brain;

parcelable ManaBrainRequest {
    String text;
    boolean allowCloud = false;
    boolean sensitive = false;
    String preferredCloudProvider;
    boolean includePersonalMemory = false;
}
