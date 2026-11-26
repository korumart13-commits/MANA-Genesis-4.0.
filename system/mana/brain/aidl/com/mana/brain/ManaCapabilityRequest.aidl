package com.mana.brain;

/** Request to invoke one allowlisted MANA OS capability. */
parcelable ManaCapabilityRequest {
    String requestId;
    String capabilityId;
    String targetPackage;
    String settingsPage;
    boolean ownerConfirmed;
}
