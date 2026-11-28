package com.mana.launcher

import android.app.WallpaperManager
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Creates fresh local MANA artwork and updates LOCK SCREEN ONLY. */
class ManaWallpaperWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(ManaWallpaperStudioActivity.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(ManaWallpaperStudioActivity.KEY_ROTATION, false)) return Result.success()
        return try {
            val intervalMinutes = prefs.getInt(ManaWallpaperStudioActivity.KEY_INTERVAL, 15).coerceIn(15, 180)
            val metrics = applicationContext.resources.displayMetrics
            val generationSlot = (System.currentTimeMillis() / (intervalMinutes * 60_000L)).toInt()
            val bitmap = ManaOfflineWallpaperRenderer.render(
                metrics.widthPixels.coerceAtLeast(720), metrics.heightPixels.coerceAtLeast(1280), generationSlot
            )
            WallpaperManager.getInstance(applicationContext).setBitmap(
                bitmap, null, true, WallpaperManager.FLAG_LOCK
            )
            bitmap.recycle()
            Result.success()
        } catch (_: SecurityException) {
            Result.failure()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK = "mana_lockscreen_art_refresh"
    }
}
