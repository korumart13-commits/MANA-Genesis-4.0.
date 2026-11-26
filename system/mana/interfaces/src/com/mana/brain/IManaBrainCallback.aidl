package com.mana.brain;

import com.mana.brain.ManaBrainResponse;

oneway interface IManaBrainCallback {
    void onResult(in ManaBrainResponse response);
    void onError(String code, String message);
}
