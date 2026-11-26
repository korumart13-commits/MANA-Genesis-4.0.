package com.mana.brain;

/** Result of evaluating or executing a MANA OS capability. */
parcelable ManaCapabilityResponse {
    String requestId;
    String capabilityId;
    boolean allowed;
    boolean executed;
    String denialReason;
    String message;
}
