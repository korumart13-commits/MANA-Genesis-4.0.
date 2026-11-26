package com.mana.brain;

import com.mana.brain.ILocalInferenceCallback;

/** Narrow local-only inference bridge from the system Binder service to MANA Genesis. */
interface ILocalInferenceService {
    boolean isModelReady();
    void generate(String requestId, String prompt, ILocalInferenceCallback callback);
}
