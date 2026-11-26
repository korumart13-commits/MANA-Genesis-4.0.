package com.mana.system.brain;

import android.app.Service;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import com.mana.brain.IManaBrain;
import com.mana.brain.IManaBrainCallback;
import com.mana.brain.ManaBrainRequest;

/**
 * Platform-facing Binder endpoint for MANA Brain.
 *
 * The endpoint deliberately reports unavailable until a real backend adapter is connected.
 * It never fabricates model output or silently forwards prompts to a cloud provider.
 */
public final class ManaBrainService extends Service {
    public static final String ACTION_BIND = "com.mana.action.BIND_BRAIN";
    public static final String BIND_PERMISSION = "com.mana.permission.BIND_BRAIN";
    private static final int PROTOCOL_VERSION = 1;
    private static final String STATE_UNAVAILABLE = "UNAVAILABLE_NO_ENGINE_BACKEND";

    private final IManaBrain.Stub binder = new IManaBrain.Stub() {
        @Override
        public int getProtocolVersion() {
            enforceAuthorizedCaller();
            return PROTOCOL_VERSION;
        }

        @Override
        public String getEngineState() {
            enforceAuthorizedCaller();
            return STATE_UNAVAILABLE;
        }

        @Override
        public void think(ManaBrainRequest request, IManaBrainCallback callback)
                throws RemoteException {
            enforceAuthorizedCaller();
            if (callback == null) {
                return;
            }
            if (request == null || request.text == null || request.text.trim().isEmpty()) {
                callback.onError("INVALID_REQUEST", "A non-empty prompt is required.");
                return;
            }
            if (request.sensitive && request.allowCloud) {
                callback.onError("POLICY_DENIED",
                        "Sensitive requests must not be sent to a cloud provider.");
                return;
            }
            if (request.allowPersonalMemoryToCloud
                    && (!request.allowCloud || !request.includePersonalMemory)) {
                callback.onError("POLICY_DENIED",
                        "Cloud memory export requires cloud and personal-memory consent.");
                return;
            }

            // Fail closed until the real local inference backend is connected and tested.
            callback.onError("ENGINE_UNAVAILABLE",
                    "MANA Brain platform service is installed, but its inference backend "
                            + "has not yet been integrated.");
        }
    };

    private void enforceAuthorizedCaller() {
        if (checkCallingPermission(BIND_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Caller lacks " + BIND_PERMISSION);
        }
    }

    @Override
    public IBinder onBind(android.content.Intent intent) {
        if (intent == null || !ACTION_BIND.equals(intent.getAction())) {
            return null;
        }
        return binder;
    }
}
