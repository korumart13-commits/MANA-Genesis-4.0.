package com.mana.system.brain;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import com.mana.brain.IManaBrain;
import com.mana.brain.IManaBrainCallback;
import com.mana.brain.ManaBrainRequest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private final ExecutorService requests = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "MANA-Brain-Binder");
        thread.setDaemon(true);
        return thread;
    });

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
            final String validationError = ManaBrainRequestPolicy.validate(
                    request.text,
                    request.sensitive,
                    request.allowCloud,
                    request.includePersonalMemory,
                    request.allowPersonalMemoryToCloud);
            if (validationError != null) {
                callback.onError("POLICY_DENIED", validationError);
                return;
            }

            // Never run inference or wait for a backend on a Binder transaction thread.
            requests.execute(() -> {
                try {
                    callback.onError("ENGINE_UNAVAILABLE",
                            "MANA Brain platform service is installed, but its inference backend "
                                    + "has not yet been integrated.");
                } catch (RemoteException ignored) {
                    // The client may have disconnected while the request was queued.
                }
            });
        }
    };

    private void enforceAuthorizedCaller() {
        if (checkCallingPermission(BIND_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Caller lacks " + BIND_PERMISSION);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent == null || !ACTION_BIND.equals(intent.getAction())) {
            return null;
        }
        return binder;
    }

    @Override
    public void onDestroy() {
        requests.shutdownNow();
        super.onDestroy();
    }
}
