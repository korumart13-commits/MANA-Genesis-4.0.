package com.mana.brain;

import com.mana.brain.BrainResponse;

/** Callback for an asynchronous inference request. */
oneway interface IBrainCallback {
    void onResult(in BrainResponse response);
}
