package com.mana.system.brain.tests;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.InstrumentationTestCase;

import com.mana.brain.IManaBrain;
import com.mana.brain.IManaBrainCallback;
import com.mana.brain.ManaBrainRequest;
import com.mana.brain.ManaBrainResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Device-side test for the actual platform Brain -> app Engine Binder chain.
 * The test APK is platform-signed and deliberately does not require model files or cloud keys.
 */
public final class ManaBrainBinderIntegrationTest extends InstrumentationTestCase {
    private static final String DISCONNECTED =
            "UNAVAILABLE_ENGINE_BACKEND_DISCONNECTED";

    public void testPlatformBrainConnectsToEngineBridgeAndForwardsRequest() throws Exception {
        Context context = getInstrumentation().getTargetContext();
        assertEquals("Test APK must hold the platform signature permission.",
                PackageManager.PERMISSION_GRANTED,
                context.checkSelfPermission("com.mana.permission.BIND_BRAIN"));

        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch callbackDelivered = new CountDownLatch(1);
        AtomicReference<IManaBrain> brainRef = new AtomicReference<>();
        AtomicReference<String> callbackCode = new AtomicReference<>();
        AtomicReference<String> callbackMessage = new AtomicReference<>();

        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                brainRef.set(IManaBrain.Stub.asInterface(service));
                connected.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                brainRef.set(null);
            }

            @Override
            public void onBindingDied(ComponentName name) {
                brainRef.set(null);
                connected.countDown();
            }

            @Override
            public void onNullBinding(ComponentName name) {
                connected.countDown();
            }
        };

        Intent intent = new Intent("com.mana.action.BIND_BRAIN").setComponent(
                new ComponentName("com.mana.system.brain",
                        "com.mana.system.brain.ManaBrainService"));

        assertTrue("ManaBrainService must be installed and resolvable.",
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE));
        try {
            assertTrue("Timed out waiting for platform Brain Binder.",
                    connected.await(20, TimeUnit.SECONDS));
            IManaBrain brain = brainRef.get();
            assertNotNull("Platform Brain Binder was not delivered.", brain);
            assertEquals("Unexpected MANA Brain AIDL protocol.", 1,
                    brain.getProtocolVersion());

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            String state = brain.getEngineState();
            while (DISCONNECTED.equals(state) && System.nanoTime() < deadline) {
                Thread.sleep(200);
                state = brain.getEngineState();
            }
            assertTrue(
                    "Engine Bridge Binder connected but its runtime is not ready: " + state,
                    "READY".equals(state) || "READY_NO_LOCAL_MODEL".equals(state));

            ManaBrainRequest request = new ManaBrainRequest();
            request.text = "MANA Binder integration health check";
            request.allowCloud = false;
            request.sensitive = false;
            request.includePersonalMemory = false;
            request.allowPersonalMemoryToCloud = false;

            brain.think(request, new IManaBrainCallback.Stub() {
                @Override
                public void onResult(ManaBrainResponse response) throws RemoteException {
                    callbackMessage.set(response == null ? null : response.text);
                    callbackDelivered.countDown();
                }

                @Override
                public void onError(String code, String message) throws RemoteException {
                    callbackCode.set(code);
                    callbackMessage.set(message);
                    callbackDelivered.countDown();
                }
            });

            assertTrue("No callback was returned from the Engine Bridge.",
                    callbackDelivered.await(60, TimeUnit.SECONDS));
            assertFalse("Request reached the bridge before its engine runtime was ready.",
                    "ENGINE_NOT_READY".equals(callbackCode.get()));
            assertFalse("Request failed because the backend Binder was disconnected.",
                    "ENGINE_UNAVAILABLE".equals(callbackCode.get())
                            || "ENGINE_DISCONNECTED".equals(callbackCode.get()));
            assertTrue("Backend callback did not contain a result or diagnostic.",
                    callbackMessage.get() != null || callbackCode.get() != null);
        } finally {
            context.unbindService(connection);
        }
    }
}
