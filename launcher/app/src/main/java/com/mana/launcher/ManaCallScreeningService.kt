package com.mana.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.content.Context
import com.mana.os.core.ManaSmartReceptionistPolicy

/**
 * Owner-enabled call screening. It never answers, rejects or records a call.
 * Android requires an immediate screening response; the requested 10-second
 * receptionist delay cannot be implemented by this API and is not simulated.
 */
class ManaCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        // Respond first: Android expects screening services to answer promptly.
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .setSilenceCall(false)
            .build()
        respondToCall(details, response)

        val number = details.handle?.takeIf { it.scheme == "tel" }?.schemeSpecificPart.orEmpty()
        // Local contact lookup and owner notification must never delay ringing.

        Thread {
            val known = runCatching {
                ManaContactIntelligence(this).findByPhone(number) != null
            }.getOrDefault(false)
            val decision = ManaSmartReceptionistPolicy().onIncomingCall(numberKnown = known)
            if (decision.unknownCaller) notifyOwner(number)
        }.start()
    }

    private fun notifyOwner(number: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
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
            .setContentText(if (number.isBlank()) "Входящий номер не определён" else "Входящий вызов с неизвестного номера")
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification) }
    }

    companion object {
        private const val CHANNEL_ID = "mana_unknown_call_alerts"
    }
}
