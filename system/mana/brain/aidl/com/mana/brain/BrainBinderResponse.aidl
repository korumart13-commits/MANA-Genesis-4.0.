package com.mana.brain;

/** Binder wire response. Kept distinct from the engine's Kotlin BrainResponse. */
parcelable BrainBinderResponse {
    String requestId;
    String text;
    String provider;
    boolean local;
    int errorCode;
    String errorMessage;
}
