package com.mana.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.mana.os.core.ManaSmartReceptionistPolicy

/**
 * User-selected Android call-screening integration.
 *
 * This service only identifies known/unknown callers and notifies the owner.
 * It never answers, rejects, records, or uploads a call. Android requires a
 * response within a short screening deadline, so the requested 10-second
 * auto-answer delay is deliberately not simulated here.
 */
class ManaCallScreeningService : CallScreeningService() {
    private val policy = ManaSmartReceptionistPolicy()

    override fun onScreenCall(details: Call.Details) {
        if (Build.VERSION.SDK_INT >= 29 && details.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(details, allowCall())
            return
        }

        val number = details.handle?.schemeSpecificPart.orEmpty()
        val knownContact = runCatching {
            if (number.isNotBlank()) ManaContactIntelligence(this).findByPhone(number) else null
        }.getOrNull()
        val decision = policy.onIncomingCall(numberKnown = knownContact != null)

        // Always let the call proceed. Screening is informational and user-controlled.
        respondToCall(details, allowCall())

        if (decision.unknownCaller) {
            notifyUnknownCaller(number)
        }
    }

    private fun allowCall() = CallResponse.Builder()
        .setDisallowCall(false)
        .setRejectCall(false)
        .setSkipCallLog(false)
        .setSkipNotification(false)
        .build()

    private fun notifyUnknownCaller(number: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "MANA unknown callers", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val open = PendingIntent.getActivity(
            this,
            4114,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("MANA: неизвестный номер")
            .setContentText(number.ifBlank { "Номер скрыт" })
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify((number.hashCode() and 0x7fffffff).coerceAtLeast(1), notification)
    }

    companion object {
        private const val CHANNEL_ID = "mana_unknown_callers"
    }
}
