package com.mana.system.brain;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.InstrumentationTestCase;

import com.mana.brain.IManaBrain;
import com.mana.brain.IManaBrainCallback;
import com.mana.brain.ManaBrainRequest;
import com.mana.brain.ManaBrainResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Device-side test for the real cross-process path:
 * test APK -> ManaBrainService -> ManaEngineBridgeService.
 *
 * The test intentionally does not require a downloaded local model or cloud credentials.
 * It verifies the signature-protected Binder connection and that requests reach the backend.
 */
public final class ManaBrainBinderIntegrationTest extends InstrumentationTestCase {
    private static final String BRAIN_ACTION = "com.mana.action.BIND_BRAIN";
    private static final String BRAIN_PACKAGE = "com.mana.system.brain";
    private static final String BRAIN_CLASS = "com.mana.system.brain.ManaBrainService";
    private static final String DISCONNECTED = "UNAVAILABLE_ENGINE_BACKEND_DISCONNECTED";

    public void testPlatformBrainConnectsToSingleAppEngineBackend() throws Exception {
        final Context testContext = getInstrumentation().getContext();
        assertEquals(
                "Integration test must be signed with the same platform certificate.",
                Context.PERMISSION_GRANTED,
                testContext.checkSelfPermission("com.mana.permission.BIND_BRAIN"));

        final CountDownLatch connected = new CountDownLatch(1);
        final CountDownLatch completed = new CountDownLatch(1);
        final IManaBrain[] brain = new IManaBrain[1];
        final String[] callbackCode = new String[1];
        final String[] callbackText = new String[1];

        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                brain[0] = IManaBrain.Stub.asInterface(service);
                connected.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                brain[0] = null;
            }

            @Override
            public void onBindingDied(ComponentName name) {
                brain[0] = null;
                connected.countDown();
            }

            @Override
            public void onNullBinding(ComponentName name) {
                connected.countDown();
            }
        };

        Intent intent = new Intent(BRAIN_ACTION)
                .setComponent(new ComponentName(BRAIN_PACKAGE, BRAIN_CLASS));
        assertTrue("ManaBrainService is not installed or resolvable.",
                testContext.bindService(intent, connection, Context.BIND_AUTO_CREATE));

        try {
            assertTrue("Timed out waiting for ManaBrainService Binder.",
                    connected.await(20, TimeUnit.SECONDS));
            assertNotNull("ManaBrainService returned no Binder.", brain[0]);
            assertEquals("Unexpected MANA Brain protocol version.", 1,
                    brain[0].getProtocolVersion());

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            String state = brain[0].getEngineState();
            while (DISCONNECTED.equals(state) && System.nanoTime() < deadline) {
                Thread.sleep(250);
                state = brain[0].getEngineState();
            }
            assertFalse("ManaBrainService never connected to ManaEngineBridgeService.",
                    DISCONNECTED.equals(state));

            ManaBrainRequest request = new ManaBrainRequest();
            request.text = "MANA Binder integration health check";
            request.allowCloud = false;
            request.sensitive = false;
            request.includePersonalMemory = false;
            request.allowPersonalMemoryToCloud = false;

            brain[0].think(request, new IManaBrainCallback.Stub() {
                @Override
                public void onResult(ManaBrainResponse response) throws RemoteException {
                    callbackText[0] = response == null ? null : response.text;
                    completed.countDown();
                }

                @Override
                public void onError(String code, String message) throws RemoteException {
                    callbackCode[0] = code;
                    callbackText[0] = message;
                    completed.countDown();
                }
            });

            assertTrue("Request did not receive a backend callback.",
                    completed.await(60, TimeUnit.SECONDS));
            assertFalse("Request was rejected because the backend Binder was disconnected.",
                    "ENGINE_UNAVAILABLE".equals(callbackCode[0])
                            || "ENGINE_DISCONNECTED".equals(callbackCode[0]));
            assertTrue("Backend callback did not contain a result or diagnostic.",
                    callbackText[0] != null || callbackCode[0] != null);
        } finally {
            testContext.unbindService(connection);
        }
    }
}
