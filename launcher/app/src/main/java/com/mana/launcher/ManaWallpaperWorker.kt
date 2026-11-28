package com.mana.launcher

import android.app.WallpaperManager
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Battery-friendly, offline daily wallpaper refresh. Android controls the exact run time. */
class ManaWallpaperWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(ManaWallpaperStudioActivity.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(ManaWallpaperStudioActivity.KEY_ROTATION, false)) return Result.success()
        return try {
            val metrics = applicationContext.resources.displayMetrics
            val bitmap = ManaOfflineWallpaperRenderer.render(
                metrics.widthPixels.coerceAtLeast(720),
                metrics.heightPixels.coerceAtLeast(1280),
                (System.currentTimeMillis() / TimeUnitMillis.DAY).toInt()
            )
            val manager = WallpaperManager.getInstance(applicationContext)
            val flags = prefs.getInt("target_flags", WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            manager.setBitmap(bitmap, null, true, flags)
            bitmap.recycle()
            Result.success()
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK = "mana_daily_wallpaper_refresh"
    }
}

private object TimeUnitMillis {
    const val DAY = 86_400_000L
}
