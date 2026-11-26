package com.mana.assistant;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import com.mana.brain.BrainBinderRequest;
import com.mana.brain.BrainBinderResponse;
import com.mana.brain.BrainCall;
import com.mana.brain.BrainCompletion;
import com.mana.brain.IBrainCallback;
import com.mana.brain.IBrainService;
import com.mana.brain.ILocalInferenceCallback;
import com.mana.brain.ILocalInferenceService;
import com.mana.brain.ManaBrainAndroidRuntime;
import com.mana.brain.ManaCapabilityRequest;
import com.mana.brain.ManaCapabilityResponse;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Signature-protected system Binder endpoint with a real LiteRT-LM companion-process bridge. */
public final class ManaBrainBinderService extends Service {
    public static final String BIND_PERMISSION = "com.mana.permission.BIND_BRAIN";
    public static final int ERROR_INVALID_REQUEST = 1000;
    public static final int ERROR_INFERENCE_UNAVAILABLE = 1002;
    public static final int ERROR_INTERNAL = 1003;
    public static final int ERROR_BUSY = 1004;
    private static final int MAX_PROMPT_CHARS = 32768;

    private volatile ILocalInferenceService localInference;
    private ManaAndroidCapabilityExecutor capabilityExecutor;
    private boolean localInferenceBound;
    private final Object providerLock = new Object();

    private final ServiceConnection localConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (providerLock) {
                localInference = ILocalInferenceService.Stub.asInterface(service);
                providerLock.notifyAll();
            }
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            synchronized (providerLock) { localInference = null; }
        }
        @Override public void onBindingDied(ComponentName name) {
            synchronized (providerLock) { localInference = null; localInferenceBound = false; }
            try { unbindService(this); } catch (IllegalArgumentException ignored) { }
        }
        @Override public void onNullBinding(ComponentName name) {
            synchronized (providerLock) { localInference = null; localInferenceBound = false; }
            try { unbindService(this); } catch (IllegalArgumentException ignored) { }
        }
    };

    // Inference never runs on a Binder pool thread. The bounded queue prevents memory exhaustion.
    private final ThreadPoolExecutor inferenceExecutor = new ThreadPoolExecutor(
            1, 2, 30L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8),
            runnable -> new Thread(runnable, "MANA-Brain-Inference"),
            new ThreadPoolExecutor.AbortPolicy());

    private final IBrainService.Stub binder = new IBrainService.Stub() {
        @Override
        public void submit(BrainBinderRequest request, IBrainCallback callback) {
            enforceAuthorizedCaller();
            if (callback == null) return;
            if (request == null || request.requestId == null
                    || request.requestId.trim().isEmpty()
                    || request.prompt == null || request.prompt.trim().isEmpty()
                    || request.prompt.length() > MAX_PROMPT_CHARS
                    || (request.preferredCloudProvider != null
                        && request.preferredCloudProvider.trim().isEmpty())) {
                sendError(callback, request == null ? null : request.requestId,
                        ERROR_INVALID_REQUEST, "Request is missing required fields or exceeds limits.");
                return;
            }
            if (request.allowCloud || request.sensitive) {
                // The system Binder endpoint is local-only until an independently reviewed
                // cloud-consent contract is implemented. Never silently forward cloud requests.
                sendError(callback, request.requestId, ERROR_INFERENCE_UNAVAILABLE,
                        "This Binder endpoint currently accepts local inference only.");
                return;
            }
            try {
                inferenceExecutor.execute(() -> submitLocal(request, callback));
            } catch (RejectedExecutionException overloaded) {
                sendError(callback, request.requestId, ERROR_BUSY,
                        "MANA Brain is busy. Retry after the active requests finish.");
            }
        }

        @Override public boolean isReady() {
            enforceAuthorizedCaller();
            ILocalInferenceService provider = awaitLocalProvider();
            if (provider == null) return false;
            try { return provider.isModelReady(); }
            catch (RemoteException unavailable) { localInference = null; return false; }
        }

        @Override public String getEngineVersion() {
            enforceAuthorizedCaller();
            return ManaBrainAndroidRuntime.get().engineVersion();
        }

        @Override public ManaCapabilityResponse executeCapability(ManaCapabilityRequest request) {
            enforceAuthorizedCaller();
            if (request == null) {
                ManaCapabilityResponse response = new ManaCapabilityResponse();
                response.allowed = false;
                response.executed = false;
                response.denialReason = "INVALID_REQUEST";
                response.message = "Capability request is required.";
                return response;
            }
            return capabilityExecutor.execute(request);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        capabilityExecutor = new ManaAndroidCapabilityExecutor(this);
        ensureLocalProviderBound();
    }

    @Override public IBinder onBind(Intent intent) { return binder; }

    private void ensureLocalProviderBound() {
        synchronized (providerLock) {
            if (localInferenceBound) return;
            Intent provider = new Intent();
            provider.setComponent(new ComponentName(
                    "com.mana.genesis", "com.mana.launcher.ManaLocalInferenceService"));
            localInferenceBound = bindService(provider, localConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private ILocalInferenceService awaitLocalProvider() {
        ensureLocalProviderBound();
        synchronized (providerLock) {
            long deadline = System.currentTimeMillis() + 2500L;
            while (localInference == null && localInferenceBound) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0L) break;
                try { providerLock.wait(remaining); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return localInference;
        }
    }

    private void submitLocal(BrainBinderRequest request, IBrainCallback callback) {
        ILocalInferenceService provider = awaitLocalProvider();
        if (provider == null) {
            sendError(callback, request.requestId, ERROR_INFERENCE_UNAVAILABLE,
                    "MANA Genesis local inference provider is not installed or not bound.");
            return;
        }
        try {
            provider.generate(request.requestId, request.prompt, new ILocalInferenceCallback.Stub() {
                @Override public void onComplete(String requestId, String text, int errorCode, String errorMessage) {
                    if (errorCode != 0 || text == null || text.trim().isEmpty()) {
                        sendError(callback, requestId, errorCode == 0 ? ERROR_INTERNAL : errorCode,
                                errorMessage == null ? "Local inference returned no text." : errorMessage);
                        return;
                    }
                    BrainBinderResponse response = new BrainBinderResponse();
                    response.requestId = requestId;
                    response.text = text;
                    response.provider = "litertlm-local";
                    response.local = true;
                    response.errorCode = 0;
                    response.errorMessage = null;
                    try { callback.onResult(response); }
                    catch (RemoteException ignored) { /* Client disconnected. */ }
                }
            });
        } catch (RemoteException unavailable) {
            localInference = null;
            sendError(callback, request.requestId, ERROR_INFERENCE_UNAVAILABLE,
                    "MANA Genesis local inference provider disconnected.");
        }
    }

    @Override public void onDestroy() {
        inferenceExecutor.shutdownNow();
        if (localInferenceBound) {
            try { unbindService(localConnection); } catch (IllegalArgumentException ignored) { }
            localInferenceBound = false;
        }
        localInference = null;
        super.onDestroy();
    }

    private void enforceAuthorizedCaller() {
        enforceCallingPermission(BIND_PERMISSION, "Caller is not authorized to access MANA Brain.");
        final int callerUid = Binder.getCallingUid();
        final String[] packages = getPackageManager().getPackagesForUid(callerUid);
        if (packages == null || packages.length == 0) {
            throw new SecurityException("Binder caller has no package identity.");
        }
        for (String packageName : packages) {
            if (getPackageManager().checkSignatures(getPackageName(), packageName)
                    == PackageManager.SIGNATURE_MATCH) return;
        }
        throw new SecurityException("Binder caller signature does not match the MANA service signer.");
    }

    private void sendError(IBrainCallback callback, String requestId, int code, String message) {
        BrainBinderResponse response = new BrainBinderResponse();
        response.requestId = requestId;
        response.provider = "none";
        response.local = false;
        response.errorCode = code;
        response.errorMessage = message;
        try { callback.onResult(response); }
        catch (RemoteException ignored) { /* Client disconnected. */ }
    }
}
