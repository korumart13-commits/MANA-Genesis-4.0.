package com.mana.assistant;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.mana.brain.BrainRequest;
import com.mana.brain.BrainResponse;
import com.mana.brain.IBrainCallback;
import com.mana.brain.IBrainService;

/**
 * Protected Android Binder endpoint for MANA Brain.
 *
 * This endpoint intentionally fails closed until the canonical Kotlin/JVM engine
 * is packaged into the Android product. It never fabricates an AI answer.
 */
public final class ManaBrainBinderService extends Service {
    public static final String BIND_PERMISSION = "com.mana.permission.BIND_BRAIN";
    public static final int ERROR_INVALID_REQUEST = 1000;
    public static final int ERROR_ENGINE_NOT_PACKAGED = 1001;
    private static final int MAX_PROMPT_CHARS = 32768;

    private final IBrainService.Stub binder = new IBrainService.Stub() {
        @Override
        public void submit(BrainRequest request, IBrainCallback callback) {
            enforceAuthorizedCaller();
            if (callback == null) return;

            BrainResponse response = new BrainResponse();
            response.requestId = request == null ? null : request.requestId;
            response.provider = "none";
            response.local = false;

            if (request == null || request.requestId == null
                    || request.requestId.trim().isEmpty()
                    || request.prompt == null || request.prompt.trim().isEmpty()
                    || request.prompt.length() > MAX_PROMPT_CHARS
                    || (request.preferredCloudProvider != null
                        && request.preferredCloudProvider.trim().isEmpty())) {
                response.errorCode = ERROR_INVALID_REQUEST;
                response.errorMessage = "Request is missing required fields or exceeds limits.";
            } else {
                response.errorCode = ERROR_ENGINE_NOT_PACKAGED;
                response.errorMessage =
                        "MANA Brain transport is available, but the canonical inference engine "
                        + "has not yet been packaged into this Android product.";
            }

            try {
                callback.onResult(response);
            } catch (RemoteException ignored) {
                // The requesting process may have disconnected; no work was started.
            }
        }

        @Override
        public boolean isReady() {
            enforceAuthorizedCaller();
            return false;
        }

        @Override
        public String getEngineVersion() {
            enforceAuthorizedCaller();
            return "not-packaged";
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        enforceAuthorizedCaller();
        return binder;
    }

    private void enforceAuthorizedCaller() {
        enforceCallingPermission(BIND_PERMISSION,
                "Caller is not authorized to access MANA Brain.");
    }
}
