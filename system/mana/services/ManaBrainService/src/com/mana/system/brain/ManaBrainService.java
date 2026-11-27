package com.mana.system.brain;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.mana.brain.IManaBrain;
import com.mana.brain.IManaBrainCallback;
import com.mana.brain.IManaEngineBackend;
import com.mana.brain.IManaEngineBackendCallback;
import com.mana.brain.ManaBrainRequest;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Platform-facing Binder endpoint for MANA Brain.
 *
 * Requests are validated here and forwarded to the one app-process engine owner over
 * a signature-protected AIDL backend. No duplicate model or memory instance is created here.
 */
public final class ManaBrainService extends Service {
    public static final String ACTION_BIND = "com.mana.action.BIND_BRAIN";
    public static final String BIND_PERMISSION = "com.mana.permission.BIND_BRAIN";
    private static final String TAG = "ManaBrainService";
    private static final int PROTOCOL_VERSION = 1;
    private static final String STATE_DISCONNECTED = "UNAVAILABLE_ENGINE_BACKEND_DISCONNECTED";
    private static final int MAX_PENDING_REQUESTS = 32;
    private static final int MAX_BACKEND_REBIND_ATTEMPTS = 5;
    private static final long BACKEND_REBIND_BASE_MS = 2_000L;
    private static final long BACKEND_REBIND_MAX_MS = 60_000L;

    @SuppressWarnings("FieldCanBeLocal")
    private final ThreadPoolExecutor requests = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(MAX_PENDING_REQUESTS),
            r -> {
                Thread thread = new Thread(r, "MANA-Brain-Binder");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());

    private volatile IManaEngineBackend backend;
    private boolean backendBindingRequested;
    private int backendRebindAttempts;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ServiceConnection backendConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IManaEngineBackend candidate = IManaEngineBackend.Stub.asInterface(service);
            try {
                if (candidate != null && candidate.getProtocolVersion() == PROTOCOL_VERSION) {
                    String engineState = candidate.getEngineState();
                    if ("READY".equals(engineState) || "READY_NO_LOCAL_MODEL".equals(engineState)) {
                        backend = candidate;
                        Log.i(TAG, "Connected to the ready MANA Engine backend: " + engineState);
                        backendRebindAttempts = 0;
                        mainHandler.removeCallbacks(backendRebind);
                    } else {
                        backend = null;
                        Log.w(TAG, "MANA Engine backend is not ready yet: " + engineState);
                        scheduleBackendRebind();
                    }
                } else {
                    backend = null;
                    Log.e(TAG, "MANA Engine backend protocol mismatch.");
                    scheduleBackendRebind();
                }
            } catch (RemoteException | SecurityException failure) {
                backend = null;
                Log.e(TAG, "Unable to negotiate or authorize MANA Engine backend.", failure);
                scheduleBackendRebind();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            backend = null;
            Log.w(TAG, "MANA Engine backend disconnected.");
        }

        @Override
        public void onBindingDied(ComponentName name) {
            backend = null;
            Log.e(TAG, "MANA Engine backend binding died; scheduling a bounded rebind.");
            scheduleBackendRebind();
        }

        @Override
        public void onNullBinding(ComponentName name) {
            backend = null;
            Log.e(TAG, "MANA Engine backend returned a null Binder; scheduling a bounded rebind.");
            scheduleBackendRebind();
        }
    };

    private final Runnable backendRebind = () -> {
        if (backendBindingRequested) {
            try {
                unbindService(backendConnection);
            } catch (IllegalArgumentException ignored) {
                // The binding may already have been removed by the system.
            }
            backendBindingRequested = false;
        }
        bindEngineBackend();
    };

    private final IManaBrain.Stub binder = new IManaBrain.Stub() {
        @Override
        public int getProtocolVersion() {
            enforceAuthorizedCaller();
            return PROTOCOL_VERSION;
        }

        @Override
        public String getEngineState() {
            enforceAuthorizedCaller();
            IManaEngineBackend current = backend;
            if (current == null) return STATE_DISCONNECTED;
            try {
                return current.getEngineState();
            } catch (RemoteException | SecurityException failure) {
                backend = null;
                return STATE_DISCONNECTED;
            }
        }

        @Override
        public void think(ManaBrainRequest request, IManaBrainCallback callback)
                throws RemoteException {
            enforceAuthorizedCaller();
            if (callback == null) return;
            if (request == null) {
                callback.onError("INVALID_REQUEST", "MANA Brain request is missing.");
                return;
            }
            final String validationError = ManaBrainRequestPolicy.validate(
                    request.text,
                    request.sensitive,
                    request.allowCloud,
                    request.preferredCloudProvider,
                    request.includePersonalMemory,
                    request.allowPersonalMemoryToCloud);
            if (validationError != null) {
                callback.onError("POLICY_DENIED", validationError);
                return;
            }

            try {
                requests.execute(() -> forwardToEngine(request, callback));
            } catch (RejectedExecutionException overloaded) {
                callback.onError("SERVICE_BUSY",
                        "MANA Brain request queue is full or the service is shutting down.");
            }
        }
    };

    private void forwardToEngine(ManaBrainRequest request, IManaBrainCallback callback) {
        IManaEngineBackend current = backend;
        if (current == null) {
            deliverError(callback, "ENGINE_UNAVAILABLE",
                    "The MANA Engine backend is not installed, not signed with the system key, or not connected.");
            return;
        }
        try {
            current.think(request, new IManaEngineBackendCallback.Stub() {
                @Override
                public void onResult(String text, String provider, boolean local, int memoryItemsUsed) {
                    try {
                        com.mana.brain.ManaBrainResponse response = new com.mana.brain.ManaBrainResponse();
                        response.text = text;
                        response.provider = provider;
                        response.local = local;
                        response.memoryItemsUsed = memoryItemsUsed;
                        callback.onResult(response);
                    } catch (RemoteException disconnected) {
                        Log.w(TAG, "MANA Brain client disconnected before result delivery.", disconnected);
                    }
                }

                @Override
                public void onError(String code, String message) {
                    deliverError(callback, code, message);
                }
            });
        } catch (RemoteException | SecurityException disconnected) {
            backend = null;
            deliverError(callback, disconnected instanceof SecurityException
                    ? "ENGINE_PERMISSION_DENIED" : "ENGINE_DISCONNECTED",
                    "The local MANA Engine Binder call failed or was denied.");
        }
    }

    private void deliverError(IManaBrainCallback callback, String code, String message) {
        try {
            callback.onError(code, message);
        } catch (RemoteException disconnected) {
            Log.w(TAG, "MANA Brain client disconnected before error delivery.", disconnected);
        }
    }

    private void enforceAuthorizedCaller() {
        if (checkCallingPermission(BIND_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("Caller lacks " + BIND_PERMISSION);
        }
    }

    private void bindEngineBackend() {
        if (backendBindingRequested) return;
        Intent backendIntent = new Intent("com.mana.action.BIND_ENGINE_BACKEND")
                .setComponent(new ComponentName(
                        "com.mana.launcher",
                        "com.mana.launcher.ManaEngineBridgeService"));
        try {
            backendBindingRequested = bindService(
                    backendIntent, backendConnection, BIND_AUTO_CREATE);
            if (!backendBindingRequested) {
                Log.w(TAG, "MANA Engine backend service is not currently resolvable.");
                scheduleBackendRebind();
            }
        } catch (SecurityException | IllegalStateException failure) {
            backendBindingRequested = false;
            Log.e(TAG, "MANA Engine backend binding was denied.", failure);
            scheduleBackendRebind();
        }
    }

    private void scheduleBackendRebind() {
        if (backendRebindAttempts >= MAX_BACKEND_REBIND_ATTEMPTS) {
            Log.e(TAG, "MANA Engine backend rebind limit reached; remaining fail-closed.");
            return;
        }
        long multiplier = 1L << backendRebindAttempts;
        long delayMs = Math.min(BACKEND_REBIND_MAX_MS,
                BACKEND_REBIND_BASE_MS * multiplier);
        backendRebindAttempts++;
        mainHandler.removeCallbacks(backendRebind);
        mainHandler.postDelayed(backendRebind, delayMs);
        Log.w(TAG, "Scheduled MANA Engine backend rebind attempt "
                + backendRebindAttempts + " in " + delayMs + " ms.");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bindEngineBackend();
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent == null || !ACTION_BIND.equals(intent.getAction())) return null;
        return binder;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacks(backendRebind);
        requests.shutdownNow();
        backend = null;
        if (backendBindingRequested) {
            try {
                unbindService(backendConnection);
            } catch (IllegalArgumentException ignored) {
                // Binding may already have been removed by the system.
            }
            backendBindingRequested = false;
        }
        super.onDestroy();
    }
}
