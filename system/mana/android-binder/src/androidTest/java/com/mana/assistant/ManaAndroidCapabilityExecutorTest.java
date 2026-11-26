package com.mana.assistant;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mana.brain.ManaCapabilityRequest;
import com.mana.brain.ManaCapabilityResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class ManaAndroidCapabilityExecutorTest {
    private ManaAndroidCapabilityExecutor executor;

    @Before public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        executor = new ManaAndroidCapabilityExecutor(context);
    }

    @Test public void unknownCapabilityFailsClosed() {
        ManaCapabilityRequest request = new ManaCapabilityRequest();
        request.requestId = "test-unknown";
        request.capabilityId = "device.root_shell";

        ManaCapabilityResponse response = executor.execute(request);

        assertFalse(response.allowed);
        assertFalse(response.executed);
        assertEquals("NOT_REGISTERED", response.denialReason);
    }

    @Test public void sensitiveCapabilityRemainsDisabledWithoutTrustedConsentFlow() {
        ManaCapabilityRequest request = new ManaCapabilityRequest();
        request.requestId = "test-contacts";
        request.capabilityId = "contacts.read";
        request.ownerConfirmed = true; // Untrusted Binder field must not grant consent.

        ManaCapabilityResponse response = executor.execute(request);

        assertFalse(response.allowed);
        assertFalse(response.executed);
        assertEquals("DISABLED", response.denialReason);
    }

    @Test public void appLaunchRequiresAnExplicitInstalledTarget() {
        ManaCapabilityRequest request = new ManaCapabilityRequest();
        request.requestId = "test-launch";
        request.capabilityId = "apps.launch";

        ManaCapabilityResponse response = executor.execute(request);

        assertFalse(response.executed);
        assertEquals("MISSING_TARGET", response.denialReason);
    }

    @Test public void settingsPageMustBeAllowlisted() {
        ManaCapabilityRequest request = new ManaCapabilityRequest();
        request.requestId = "test-settings";
        request.capabilityId = "device.settings.open";
        request.settingsPage = "arbitrary-intent";

        ManaCapabilityResponse response = executor.execute(request);

        assertTrue(response.allowed);
        assertFalse(response.executed);
        assertEquals("UNSUPPORTED_SETTINGS_PAGE", response.denialReason);
        assertNotNull(response.message);
    }
}
