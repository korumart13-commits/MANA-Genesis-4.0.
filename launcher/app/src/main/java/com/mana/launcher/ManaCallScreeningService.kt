package com.mana.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import com.mana.os.core.ManaSmartReceptionistPolicy

/**
 * Owner-enabled call screening. It never answers, rejects or records a call.
 * Android requires an immediate screening response; the requested 10-second
 * receptionist delay cannot be implemented by this API and is not simulated.
 */
class ManaCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        val number = details.handle?.takeIf { it.scheme == "tel" }?.schemeSpecificPart.orEmpty()
        val known = runCatching {
            ManaContactIntelligence(this).findByPhone(number) != null
        }.getOrDefault(false)
        val decision = ManaSmartReceptionistPolicy().onIncomingCall(numberKnown = known)

        // Preserve the incoming call. The owner remains in control.
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .setSilenceCall(false)
            .build()
        respondToCall(details, response)

        if (decision.unknownCaller) notifyOwner(number)
    }

    private fun notifyOwner(number: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "MANA unknown caller alerts", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
            else @Suppress("DEPRECATION") Notification.Builder(this)
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MANA: неизвестный номер")
            .setContentText(if (number.isBlank()) "Входящий номер не определён" else "Входящий вызов: $number")
            .setAutoCancel(true)
            .build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "mana_unknown_call_alerts"
    }
}
