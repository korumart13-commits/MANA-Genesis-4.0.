package com.mana.brain;

/** Exactly one terminal callback for an accepted backend request. */
oneway interface IManaEngineBackendCallback {
    void onResult(String text, String provider, boolean local, int memoryItemsUsed);
    void onError(String code, String message);
}
