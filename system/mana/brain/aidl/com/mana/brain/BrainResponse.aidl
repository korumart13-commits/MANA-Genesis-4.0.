package com.mana.brain;

/** Provider provenance is always returned; callers must not infer local/cloud origin. */
parcelable BrainResponse {
    String requestId;
    String text;
    String provider;
    boolean local;
    int errorCode;
    String errorMessage;
}
