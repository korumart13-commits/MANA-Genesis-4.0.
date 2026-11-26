package com.mana.brain;

import com.mana.brain.ManaBrainRequest;
import com.mana.brain.ManaBrainResponse;
import com.mana.brain.IManaBrainCallback;

/**
 * Versioned Binder contract between Android system clients and the MANA Brain host.
 * Implementations must enforce caller permissions and never trust caller-supplied identity.
 */
interface IManaBrain {
    int getProtocolVersion();
    String getEngineState();

    /**
     * Asynchronous request. Implementations must return promptly and deliver exactly one
     * callback result or error. Cloud use and sensitive-data handling are per-request.
     */
    void think(in ManaBrainRequest request, IManaBrainCallback callback);
}
