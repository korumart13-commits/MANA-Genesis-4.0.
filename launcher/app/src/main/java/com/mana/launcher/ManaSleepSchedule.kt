package com.mana.launcher

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/** Daily bedtime/wake schedule. Exact alarms are used only when Android grants access. */
object ManaSleepScheduler {
    const val PREFS = "mana.life.control"
    const val KEY_ENABLED = "sleep_schedule_enabled"
    const val KEY_BED_HOUR = "bedtime_hour"
    const val KEY_BED_MINUTE = "bedtime_minute"
    const val KEY_WAKE_HOUR = "wake_hour"
    const val KEY_WAKE_MINUTE = "wake_minute"

    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return
        scheduleOne(context, true, prefs.getInt(KEY_BED_HOUR, 22), prefs.getInt(KEY_BED_MINUTE, 0))
        scheduleOne(context, false, prefs.getInt(KEY_WAKE_HOUR, 7), prefs.getInt(KEY_WAKE_MINUTE, 0))
    }

    fun cancel(context: Context) {
        cancelOne(context, true)
        cancelOne(context, false)
    }

    fun disable(context: Context) {
        cancel(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, false).putBoolean("sleep_mode", false).apply()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.isNotificationPolicyAccessGranted) {
            val previous = prefs.getInt("previous_interruption_filter", NotificationManager.INTERRUPTION_FILTER_ALL)
            runCatching { manager.setInterruptionFilter(previous) }
        }
    }

    private fun scheduleOne(context: Context, bedtime: Boolean, hour: Int, minute: Int) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, bedtime)
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        if (Build.VERSION.SDK_INT >= 31 && alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
        }
    }

    private fun cancelOne(context: Context, bedtime: Boolean) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, bedtime)
        alarm.cancel(pending)
        pending.cancel()
    }

    private fun pendingIntent(context: Context, bedtime: Boolean): PendingIntent {
        val intent = Intent(context, ManaSleepScheduleReceiver::class.java)
            .putExtra(ManaSleepScheduleReceiver.EXTRA_BEDTIME, bedtime)
        val requestCode = if (bedtime) 8411 else 8412
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class ManaSleepScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val bedtime = intent?.getBooleanExtra(EXTRA_BEDTIME, false) ?: return
        val prefs = context.getSharedPreferences(ManaSleepScheduler.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(ManaSleepScheduler.KEY_ENABLED, false)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (bedtime) {
            prefs.edit().putBoolean("sleep_mode", true).apply()
            if (manager.isNotificationPolicyAccessGranted) {
                val current = manager.currentInterruptionFilter
                if (current != NotificationManager.INTERRUPTION_FILTER_NONE) {
                    prefs.edit().putInt("previous_interruption_filter", current).apply()
                }
                runCatching { manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE) }
            }
        } else {
            prefs.edit().putBoolean("sleep_mode", false).apply()
            if (manager.isNotificationPolicyAccessGranted) {
                val previous = prefs.getInt("previous_interruption_filter", NotificationManager.INTERRUPTION_FILTER_ALL)
                runCatching { manager.setInterruptionFilter(previous) }
            }
        }
        ManaSleepScheduler.schedule(context)
    }

    companion object {
        const val EXTRA_BEDTIME = "mana.sleep.bedtime"
    }
}
