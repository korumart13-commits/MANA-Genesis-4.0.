package com.mana.launcher.tests;

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

/** Runs under the ManaLauncher UID and exercises the same Assistant Binder path as HOME UI. */
public final class ManaLauncherBrainIntegrationTest extends InstrumentationTestCase {
    public void testLauncherUidBindsAndSubmitsToAssistant() throws Exception {
        Context context = getInstrumentation().getTargetContext();
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<IBrainService> serviceRef = new AtomicReference<>();
        AtomicReference<BrainBinderResponse> responseRef = new AtomicReference<>();

        ServiceConnection connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                serviceRef.set(IBrainService.Stub.asInterface(binder));
                connected.countDown();
            }
            @Override public void onServiceDisconnected(ComponentName name) { serviceRef.set(null); }
        };
        Intent intent = new Intent();
        intent.setClassName("com.mana.assistant", "com.mana.assistant.ManaBrainBinderService");
        assertTrue("Launcher UID could not bind Assistant service",
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE));
        try {
            assertTrue("Assistant Binder connection timed out", connected.await(10, TimeUnit.SECONDS));
            IBrainService service = serviceRef.get();
            assertNotNull(service);
            BrainBinderRequest request = new BrainBinderRequest();
            request.requestId = UUID.randomUUID().toString();
            request.prompt = "Return a short local integration test response.";
            request.allowCloud = false;
            request.sensitive = false;
            request.preferredCloudProvider = null;

            service.submit(request, new IBrainCallback.Stub() {
                @Override public void onResult(BrainBinderResponse response) {
                    responseRef.set(response);
                    completed.countDown();
                }
            });
            assertTrue("Assistant did not answer the Launcher Binder request",
                    completed.await(60, TimeUnit.SECONDS));
            BrainBinderResponse response = responseRef.get();
            assertNotNull(response);
            assertEquals(request.requestId, response.requestId);
            assertNotNull(response.provider);
            assertTrue("Expected generated local text or a truthful provider-unavailable error",
                    response.errorCode != 0 || (response.text != null && !response.text.trim().isEmpty()));
            if (response.errorCode == 0) {
                assertTrue(response.local);
                assertEquals("litertlm-local", response.provider);
            }
        } finally {
            context.unbindService(connection);
        }
    }
}
