package com.mana.assistant;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import com.mana.brain.BrainCall;
import com.mana.brain.BrainCompletion;
import com.mana.brain.BrainRequest;
import com.mana.brain.BrainResponse;
import com.mana.brain.IBrainCallback;
import com.mana.brain.IBrainService;
import com.mana.brain.ManaBrainAndroidRuntime;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Signature-protected Binder adapter over the canonical Kotlin MANA Brain engine. */
public final class ManaBrainBinderService extends Service {
    public static final String BIND_PERMISSION = "com.mana.permission.BIND_BRAIN";
    public static final int ERROR_INVALID_REQUEST = 1000;
    public static final int ERROR_INFERENCE_UNAVAILABLE = 1002;
    public static final int ERROR_INTERNAL = 1003;
    public static final int ERROR_BUSY = 1004;
    private static final int MAX_PROMPT_CHARS = 32768;

    // Inference never runs on a Binder pool thread. The bounded queue prevents memory exhaustion.
    private final ThreadPoolExecutor inferenceExecutor = new ThreadPoolExecutor(
            1, 2, 30L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8),
            runnable -> new Thread(runnable, "MANA-Brain-Inference"),
            new ThreadPoolExecutor.AbortPolicy());

    private final IBrainService.Stub binder = new IBrainService.Stub() {
        @Override
        public void submit(BrainRequest request, IBrainCallback callback) {
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

            final BrainCall call;
            try {
                call = new BrainCall(request.requestId, request.prompt, request.allowCloud,
                        request.sensitive, request.preferredCloudProvider);
            } catch (IllegalArgumentException invalid) {
                sendError(callback, request.requestId, ERROR_INVALID_REQUEST, "Request validation failed.");
                return;
            }

            try {
                inferenceExecutor.execute(() -> ManaBrainAndroidRuntime.get().submit(call,
                        new BrainCompletion() {
                            @Override
                            public void onComplete(com.mana.brain.BrainResult result, Throwable error) {
                                if (error != null || result == null) {
                                    int code = error instanceof IllegalStateException
                                            ? ERROR_INFERENCE_UNAVAILABLE : ERROR_INTERNAL;
                                    String message = code == ERROR_INFERENCE_UNAVAILABLE
                                            ? "No permitted inference provider is currently available."
                                            : "MANA Brain could not complete this request.";
                                    sendError(callback, request.requestId, code, message);
                                    return;
                                }
                                BrainResponse response = new BrainResponse();
                                response.requestId = result.getRequestId();
                                response.text = result.getText();
                                response.provider = result.getProvider();
                                response.local = result.getLocal();
                                response.errorCode = 0;
                                response.errorMessage = null;
                                try { callback.onResult(response); }
                                catch (RemoteException ignored) { /* Client disconnected. */ }
                            }
                        }));
            } catch (RejectedExecutionException overloaded) {
                sendError(callback, request.requestId, ERROR_BUSY,
                        "MANA Brain is busy. Retry after the active requests finish.");
            }
        }

        @Override public boolean isReady() {
            enforceAuthorizedCaller();
            return ManaBrainAndroidRuntime.get().isReady();
        }

        @Override public String getEngineVersion() {
            enforceAuthorizedCaller();
            return ManaBrainAndroidRuntime.get().engineVersion();
        }
    };

    @Override public IBinder onBind(Intent intent) {
        // Android enforces the signature-level service permission before returning the binder.
        return binder;
    }

    @Override public void onDestroy() {
        inferenceExecutor.shutdownNow();
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
                    == PackageManager.SIGNATURE_MATCH) {
                return;
            }
        }
        throw new SecurityException("Binder caller signature does not match the MANA service signer.");
    }

    private void sendError(IBrainCallback callback, String requestId, int code, String message) {
        BrainResponse response = new BrainResponse();
        response.requestId = requestId;
        response.provider = "none";
        response.local = false;
        response.errorCode = code;
        response.errorMessage = message;
        try { callback.onResult(response); }
        catch (RemoteException ignored) { /* Client disconnected. */ }
    }
}
