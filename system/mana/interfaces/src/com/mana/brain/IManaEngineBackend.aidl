package com.mana.brain;

import com.mana.brain.ManaBrainRequest;
import com.mana.brain.IManaEngineBackendCallback;

/** Private cross-process adapter from the platform Brain host to the single app-owned engine. */
interface IManaEngineBackend {
    int getProtocolVersion();
    String getEngineState();
    void think(in ManaBrainRequest request, IManaEngineBackendCallback callback);
}
