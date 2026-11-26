package com.mana.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.role.RoleManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

/**
 * Runs a lightweight, permission-respecting lifecycle check after boot or app replacement.
 * It deliberately does not auto-open an Activity or load the multi-GB model in the background:
 * Android controls HOME selection and background launch policy, and model loading is resource-heavy.
 */
class ManaBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        // Restore owner-enabled sleep alarms after reboot or MANA update.
        runCatching { ManaSleepScheduler.schedule(context) }
        val request = OneTimeWorkRequestBuilder<ManaBootInitializationWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private const val WORK_NAME = "mana-boot-initialization"
    }
}

/** Records MANA startup readiness without waking the local LLM or requesting new permissions. */
class ManaBootInitializationWorker(
    context: Context,
    parameters: WorkerParameters
) : Worker(context, parameters) {
    override fun doWork(): Result = try {
        val app = applicationContext
        val catalog = app.getSharedPreferences("mana-model-catalog", Context.MODE_PRIVATE)
        val path = catalog.getString("active_model_path", null)
        val modelFile = path?.let(::File)
        val modelPresent = modelFile?.isFile == true &&
            modelFile.length() == ManaModelCatalog.MODEL_SIZE
        val homeRoleHeld = if (android.os.Build.VERSION.SDK_INT >= 29) {
            app.getSystemService(RoleManager::class.java)
                ?.isRoleHeld(RoleManager.ROLE_HOME) == true
        } else {
            false
        }
        app.getSharedPreferences("mana.lifecycle", Context.MODE_PRIVATE).edit()
            .putLong("last_boot_check_ms", System.currentTimeMillis())
            .putString(
                "startup_state",
                if (modelPresent) "model_available" else "model_import_required"
            )
            .putBoolean("home_role_held", homeRoleHeld)
            .putString("app_version", runCatching {
                app.packageManager.getPackageInfo(app.packageName, 0).versionName
            }.getOrNull())
            .apply()
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
