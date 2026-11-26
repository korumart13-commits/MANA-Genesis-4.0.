package com.mana.assistant.tests;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.test.InstrumentationTestCase;

import com.mana.brain.BrainBinderRequest;
import com.mana.brain.BrainBinderResponse;
import com.mana.brain.IBrainCallback;
import com.mana.brain.IBrainService;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Device-side verification of the protected Assistant Binder endpoint and AIDL contract. */
public final class ManaBrainBinderTest extends InstrumentationTestCase {
    public void testProtectedServiceExposesEngineMetadata() throws Exception {
        ServiceHandle handle = bindBrain();
        try {
            assertNotNull(handle.service);
            assertNotNull(handle.service.getEngineVersion());
            assertFalse(handle.service.getEngineVersion().trim().isEmpty());
        } finally {
            handle.close();
        }
    }

    public void testSubmitReturnsCorrelatedLocalResultOrExplicitError() throws Exception {
        ServiceHandle handle = bindBrain();
        try {
            CountDownLatch completed = new CountDownLatch(1);
            AtomicReference<BrainBinderResponse> responseRef = new AtomicReference<>();
            BrainBinderRequest request = new BrainBinderRequest();
            request.requestId = UUID.randomUUID().toString();
            request.prompt = "Reply with a short greeting.";
            request.allowCloud = false;
            request.sensitive = false;
            request.preferredCloudProvider = null;

            handle.service.submit(request, new IBrainCallback.Stub() {
                @Override public void onResult(BrainBinderResponse response) {
                    responseRef.set(response);
                    completed.countDown();
                }
            });

            assertTrue("Binder callback timed out", completed.await(45, TimeUnit.SECONDS));
            BrainBinderResponse response = responseRef.get();
            assertNotNull(response);
            assertEquals(request.requestId, response.requestId);
            assertNotNull(response.provider);
            assertTrue("Must return generated text or an explicit provider error",
                    response.errorCode != 0 || (response.text != null && !response.text.trim().isEmpty()));
            if (response.errorCode == 0) assertEquals("litertlm-local", response.provider);
        } finally {
            handle.close();
        }
    }

    private ServiceHandle bindBrain() throws Exception {
        Context context = getInstrumentation().getTargetContext();
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<IBrainService> serviceRef = new AtomicReference<>();
        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                serviceRef.set(IBrainService.Stub.asInterface(binder));
                connected.countDown();
            }
            @Override public void onServiceDisconnected(ComponentName name) { serviceRef.set(null); }
        };
        Intent intent = new Intent();
        intent.setClassName("com.mana.assistant", "com.mana.assistant.ManaBrainBinderService");
        assertTrue("Unable to bind to MANA Brain", context.bindService(intent, connection, Context.BIND_AUTO_CREATE));
        assertTrue("MANA Brain service connection timed out", connected.await(10, TimeUnit.SECONDS));
        IBrainService service = serviceRef.get();
        assertNotNull(service);
        return new ServiceHandle(context, connection, service);
    }

    private static final class ServiceHandle {
        final Context context;
        final ServiceConnection connection;
        final IBrainService service;
        ServiceHandle(Context context, ServiceConnection connection, IBrainService service) {
            this.context = context;
            this.connection = connection;
            this.service = service;
        }
        void close() { context.unbindService(connection); }
    }
}
