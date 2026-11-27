package com.mana.system.brain.tests;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.test.InstrumentationTestCase;

import com.mana.brain.IManaBrain;
import com.mana.system.brain.ManaBrainService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Device-side test for the actual platform Brain -> app Engine Binder chain.
 *
 * The test APK is platform-signed so it can bind through the same signature permission
 * as the product's system components. It intentionally tests the backend handshake/state,
 * not inference quality or model availability.
 */
public final class ManaBrainBinderIntegrationTest extends InstrumentationTestCase {
    public void testPlatformBrainConnectsToEngineBridge() throws Exception {
        Context context = getInstrumentation().getTargetContext();
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<IManaBrain> brainRef = new AtomicReference<>();

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

        Intent intent = new Intent(ManaBrainService.ACTION_BIND).setComponent(
                new ComponentName(
                        "com.mana.system.brain",
                        "com.mana.system.brain.ManaBrainService"));

        assertTrue("ManaBrainService must be installed and resolvable",
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE));
        try {
            assertTrue("Timed out waiting for platform Brain Binder",
                    connected.await(20, TimeUnit.SECONDS));
            IManaBrain brain = brainRef.get();
            assertNotNull("Platform Brain Binder was not delivered", brain);
            assertEquals("Unexpected MANA Brain AIDL protocol", 1,
                    brain.getProtocolVersion());

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            String state = brain.getEngineState();
            while ("UNAVAILABLE_ENGINE_BACKEND_DISCONNECTED".equals(state)
                    && System.nanoTime() < deadline) {
                Thread.sleep(100);
                state = brain.getEngineState();
            }

            assertFalse(
                    "ManaBrainService never completed the IManaEngineBackend handshake; "
                            + "state=" + state,
                    "UNAVAILABLE_ENGINE_BACKEND_DISCONNECTED".equals(state));
        } finally {
            context.unbindService(connection);
        }
    }
}
