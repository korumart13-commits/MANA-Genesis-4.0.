package com.mana.launcher

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.mana.os.core.DataSensitivity
import com.mana.os.core.ManaActionRequest
import com.mana.os.core.ManaActionSpec
import com.mana.os.core.ManaActionRisk
import com.mana.os.core.ManaAutonomyController
import com.mana.os.core.ManaDefaultActions
import com.mana.os.core.ManaDeviceActionExecutor
import com.mana.os.core.ManaSecurityService
import com.mana.os.core.SecurityAction
import com.mana.os.core.SecurityCapability
import com.mana.os.core.SecurityGrant

/**
 * Executes the small, documented Android API allowlist available to an ordinary APK.
 *
 * This is deliberately not a DevicePolicyManager, AccessibilityService, shell, root,
 * hidden-API or firmware adapter. Every resource must be explicitly enabled by the
 * owner before a capability is granted. Android's own permission/special-access
 * checks are repeated immediately before each operation.
 */
class ManaAndroidActionAdapter(private val context: Context) {
    private val appContext = context.applicationContext
    private val cameraManager = appContext.getSystemService(CameraManager::class.java)
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)

    val supportedResources: Set<String> = setOf(
        "device.apps.launch",
        "device.torch",
        "device.display.brightness",
        "device.audio.volume",
        "device.notifications.dnd"
    )

    fun createController(ownerEnabledResources: Set<String>): ManaAutonomyController {
        require(ownerEnabledResources.all { it in supportedResources }) {
            "Unknown Android capability requested"
        }
        val security = ManaSecurityService()
        ownerEnabledResources.forEach { resource ->
            security.grant(
                SecurityGrant(
                    SecurityCapability("mana.brain", resource, SecurityAction.EXECUTE)
                )
            )
        }
        val controller = ManaAutonomyController(security, ManaDeviceActionExecutor(::execute))
        ManaDefaultActions.specs
            .filter { it.resource in ownerEnabledResources && it.resource in supportedResources }
            .forEach(controller::register)
        return controller
    }

    private fun execute(request: ManaActionRequest): String = when (request.action) {
        "launch_app" -> launchApp(request.parameters.getValue("package"))
        "set_torch" -> setTorch(request.parameters.getValue("enabled").toBooleanStrict())
        "set_brightness" -> setBrightness(request.parameters.getValue("level").toInt())
        "set_volume" -> setVolume(
            request.parameters.getValue("stream"),
            request.parameters.getValue("level").toInt()
        )
        "set_do_not_disturb" -> setDoNotDisturb(request.parameters.getValue("enabled").toBooleanStrict())
        else -> error("No Android implementation for action: ${request.action}")
    }

    private fun launchApp(packageName: String): String {
        require(packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) {
            "Invalid package name"
        }
        val launch = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: error("No launchable activity found or package is not visible")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(launch)
        return "Launch request sent to $packageName"
    }

    private fun setTorch(enabled: Boolean): String {
        check(appContext.checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            "Grant the Android Camera permission to MANA before controlling the torch"
        }
        val manager = cameraManager ?: error("Camera service unavailable")
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: error("No flash unit available")
        manager.setTorchMode(cameraId, enabled)
        return "Torch ${if (enabled) "enabled" else "disabled"}"
    }

    private fun setBrightness(level: Int): String {
        require(level in 0..255) { "Brightness must be between 0 and 255" }
        check(Settings.System.canWrite(appContext)) {
            "Grant MANA the Android 'Modify system settings' special access first"
        }
        check(Settings.System.putInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, level)) {
            "Android rejected the brightness change"
        }
        return "Brightness set to $level"
    }

    private fun setVolume(streamName: String, level: Int): String {
        val stream = when (streamName) {
            "music" -> AudioManager.STREAM_MUSIC
            "ring" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "notification" -> AudioManager.STREAM_NOTIFICATION
            "voice_call" -> AudioManager.STREAM_VOICE_CALL
            else -> error("Unsupported audio stream")
        }
        val maximum = audioManager.getStreamMaxVolume(stream)
        require(level in 0..maximum) { "Volume must be between 0 and $maximum for $streamName" }
        audioManager.setStreamVolume(stream, level, 0)
        return "$streamName volume set to $level/$maximum"
    }

    private fun setDoNotDisturb(enabled: Boolean): String {
        val manager = notificationManager ?: error("Notification service unavailable")
        check(manager.isNotificationPolicyAccessGranted) {
            "Grant MANA Android Notification Policy Access first"
        }
        manager.setInterruptionFilter(
            if (enabled) NotificationManager.INTERRUPTION_FILTER_PRIORITY
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
        return "Do Not Disturb ${if (enabled) "enabled" else "disabled"}"
    }
}
