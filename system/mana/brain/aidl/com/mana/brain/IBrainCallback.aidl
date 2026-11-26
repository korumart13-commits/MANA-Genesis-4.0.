package com.mana.brain;

import com.mana.brain.BrainBinderResponse;

/** Callback for an asynchronous inference request. */
oneway interface IBrainCallback {
    void onResult(in BrainBinderResponse response);
}
