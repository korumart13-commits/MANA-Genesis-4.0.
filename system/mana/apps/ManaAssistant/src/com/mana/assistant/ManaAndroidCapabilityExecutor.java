package com.mana.assistant;

import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import com.mana.brain.CapabilityContext;
import com.mana.brain.CapabilityDecision;
import com.mana.brain.DenialReason;
import com.mana.brain.ManaCapability;
import com.mana.brain.ManaCapabilityRegistry;
import com.mana.brain.ManaCapabilityRequest;
import com.mana.brain.ManaCapabilityResponse;

import java.util.HashSet;
import java.util.Set;

/**
 * Android platform adapter for the MANA Brain capability registry.
 *
 * Only explicit, allowlisted Android intents are executed here. This class
 * does not use shell commands, hidden APIs, accessibility injection, or
 * permission bypasses.
 */
final class ManaAndroidCapabilityExecutor {
    private final Context context;
    private final ManaCapabilityRegistry registry;

    ManaAndroidCapabilityExecutor(Context context) {
        this.context = context.getApplicationContext();
        this.registry = ManaCapabilityRegistry.Companion.platformDefaults();
    }

    ManaCapabilityResponse execute(ManaCapabilityRequest request) {
        ManaCapabilityResponse response = new ManaCapabilityResponse();
        response.requestId = request == null ? null : request.requestId;
        response.capabilityId = request == null ? null : request.capabilityId;
        response.allowed = false;
        response.executed = false;

        if (request == null || request.requestId == null || request.requestId.trim().isEmpty()
                || request.capabilityId == null || request.capabilityId.trim().isEmpty()) {
            response.denialReason = "INVALID_REQUEST";
            response.message = "A request ID and registered capability ID are required.";
            return response;
        }

        ManaCapability capability = registry.get(request.capabilityId);
        if (capability == null) {
            return denied(response, DenialReason.NOT_REGISTERED, "Capability is not registered.");
        }

        Set<String> grantedPermissions = new HashSet<>();
        for (String permission : capability.getRequiredPermissions()) {
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permission);
            }
        }

        Set<String> grantedAccess = currentSpecialAccess();
        // enabledByDefault is only an initial conservative policy. Sensitive
        // operations stay disabled until a separately persisted owner-consent
        // and UI flow is implemented.
        boolean enabled = capability.getEnabledByDefault();
        CapabilityContext gate = new CapabilityContext(
                grantedPermissions,
                grantedAccess,
                false, // Binder-supplied booleans are not proof of owner consent.
                isSupported(request.capabilityId),
                enabled);
        CapabilityDecision decision = registry.evaluate(request.capabilityId, gate);
        if (!(decision instanceof CapabilityDecision.Allowed)) {
            DenialReason reason = ((CapabilityDecision.Denied) decision).getReason();
            return denied(response, reason, "MANA OS denied this action after checking device access.");
        }

        response.allowed = true;
        try {
            if ("apps.launch".equals(request.capabilityId)) {
                return launchApp(request, response);
            }
            if ("device.settings.open".equals(request.capabilityId)) {
                return openSettings(request, response);
            }
            response.allowed = false;
            response.denialReason = "NO_PLATFORM_ADAPTER";
            response.message = "This capability is registered but has no Android execution adapter yet.";
            return response;
        } catch (SecurityException | ActivityNotFoundException error) {
            response.allowed = false;
            response.denialReason = "PLATFORM_REJECTED";
            response.message = "Android rejected the requested operation: " + error.getClass().getSimpleName();
            return response;
        }
    }

    private ManaCapabilityResponse launchApp(ManaCapabilityRequest request,
                                               ManaCapabilityResponse response) {
        if (request.targetPackage == null || request.targetPackage.trim().isEmpty()) {
            response.allowed = false;
            response.denialReason = "MISSING_TARGET";
            response.message = "A target package name is required.";
            return response;
        }
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(request.targetPackage);
        if (launch == null) {
            response.allowed = false;
            response.denialReason = "APP_NOT_LAUNCHABLE";
            response.message = "The target package is not installed or has no launcher activity.";
            return response;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
        response.executed = true;
        response.message = "Android accepted the application launch request.";
        return response;
    }

    private ManaCapabilityResponse openSettings(ManaCapabilityRequest request,
                                                 ManaCapabilityResponse response) {
        String action = settingsAction(request.settingsPage);
        if (action == null) {
            response.allowed = false;
            response.denialReason = "UNSUPPORTED_SETTINGS_PAGE";
            response.message = "Only allowlisted Android settings pages can be opened.";
            return response;
        }
        context.startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        response.executed = true;
        response.message = "Android accepted the settings screen request.";
        return response;
    }

    private String settingsAction(String page) {
        if ("wifi".equals(page)) return Settings.ACTION_WIFI_SETTINGS;
        if ("bluetooth".equals(page)) return Settings.ACTION_BLUETOOTH_SETTINGS;
        if ("display".equals(page)) return Settings.ACTION_DISPLAY_SETTINGS;
        if ("sound".equals(page)) return Settings.ACTION_SOUND_SETTINGS;
        if ("security".equals(page)) return Settings.ACTION_SECURITY_SETTINGS;
        if ("apps".equals(page)) return Settings.ACTION_APPLICATION_SETTINGS;
        if ("about".equals(page)) return Settings.ACTION_DEVICE_INFO_SETTINGS;
        return null;
    }

    private boolean isSupported(String capabilityId) {
        if ("apps.launch".equals(capabilityId) || "device.settings.open".equals(capabilityId)) {
            return true;
        }
        if ("calls.answer".equals(capabilityId)) {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        }
        return "device.notifications.read".equals(capabilityId)
                || "contacts.read".equals(capabilityId)
                || "updates.install".equals(capabilityId);
    }

    private boolean hasNotificationListenerAccess() {
        String enabled = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);
        if (enabled == null || enabled.isEmpty()) return false;
        for (String entry : enabled.split(":")) {
            ComponentName component = ComponentName.unflattenFromString(entry);
            if (component != null && context.getPackageName().equals(component.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> currentSpecialAccess() {
        Set<String> access = new HashSet<>();
        if (Build.VERSION.SDK_INT >= 18 && hasNotificationListenerAccess()) {
            access.add("notification_listener");
        }
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager roles = context.getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleHeld(RoleManager.ROLE_DIALER)) {
                access.add("role_dialer");
            }
        }
        return access;
    }

    private ManaCapabilityResponse denied(ManaCapabilityResponse response,
                                          DenialReason reason, String message) {
        response.allowed = false;
        response.executed = false;
        response.denialReason = reason.name();
        response.message = message;
        return response;
    }
}
