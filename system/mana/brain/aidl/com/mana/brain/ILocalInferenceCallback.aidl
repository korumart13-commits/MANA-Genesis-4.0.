package com.mana.brain;

/** Completion callback from the companion process hosting the LiteRT-LM runtime. */
oneway interface ILocalInferenceCallback {
    void onComplete(String requestId, String text, int errorCode, String errorMessage);
}
