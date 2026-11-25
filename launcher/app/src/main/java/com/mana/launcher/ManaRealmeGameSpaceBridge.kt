package com.mana.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Owner-invoked bridge to the OEM Realme/ColorOS game hub.
 *
 * The OEM app remains the authority for its private performance controls.
 * MANA only discovers and opens its public launcher entry point; it does not
 * call hidden APIs, alter protected settings, or inject input into games.
 */
class ManaRealmeGameSpaceBridge(private val context: Context) {
    data class GameSpaceApp(val packageName: String, val label: String)

    companion object {
        /** Package IDs observed in Realme/ColorOS distributions; device presence is checked at runtime. */
        val SUPPORTED_PACKAGES = listOf(
            "com.coloros.gamespaceui",
            "com.oplus.games"
        )
    }

    fun findInstalled(): GameSpaceApp? {
        val pm = context.packageManager
        for (packageName in SUPPORTED_PACKAGES) {
            val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: continue
            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                packageName
            }
            if (launchIntent.component != null) return GameSpaceApp(packageName, label)
        }
        return null
    }

    /** Launches the installed OEM game hub only after the owner invokes this method. */
    fun open(): Boolean {
        val app = findInstalled() ?: return false
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: android.content.ActivityNotFoundException) {
            false
        }
    }
}
