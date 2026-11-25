package com.mana.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap

/**
 * Optional default-dialer integration. Inert until the owner explicitly grants
 * ROLE_DIALER and enables the 10-second auto-answer preference.
 */
class ManaInCallService : InCallService() {
    private val handler = Handler(Looper.getMainLooper())
    private val callbacks = ConcurrentHashMap<Call, Call.Callback>()
    private val answerTasks = ConcurrentHashMap<Call, Runnable>()

    override fun onCreate() {
        super.onCreate()
        activeService = this
        ensureCallNotificationChannel()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        if (!calls.contains(call)) calls.add(call)
        currentCall = call
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                if (state != Call.STATE_RINGING) cancelAutoAnswer(call)
                if (currentCall === call && state == Call.STATE_DISCONNECTED) {
                    currentCall = calls.lastOrNull { it !== call && it.state != Call.STATE_DISCONNECTED }
                }
                refreshCallNotification()
                broadcastCallChanged()
            }
        }
        callbacks[call] = callback
        call.registerCallback(callback)
        refreshCallNotification()
        scheduleAutoAnswerIfEnabled(call)
        broadcastCallChanged()
    }

    override fun onCallRemoved(call: Call) {
        cancelAutoAnswer(call)
        callbacks.remove(call)?.let { runCatching { call.unregisterCallback(it) } }
        calls.remove(call)
        if (currentCall === call) currentCall = calls.lastOrNull { it.state != Call.STATE_DISCONNECTED }
        refreshCallNotification()
        broadcastCallChanged()
        super.onCallRemoved(call)
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        launchInCallUi(showDialpad)
    }

    private fun refreshCallNotification() {
        val ringing = calls.lastOrNull { it.state == Call.STATE_RINGING }
        if (ringing != null) {
            postIncomingCallNotification(ringing)
            return
        }
        val ongoing = currentCall?.takeIf { it.state != Call.STATE_DISCONNECTED }
        if (ongoing != null) postOngoingCallNotification(ongoing) else cancelCallNotification()
    }
    private fun ensureCallNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CALL_CHANNEL_ID) == null) {
                val channel = NotificationChannel(CALL_CHANNEL_ID, "MANA Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Incoming and ongoing phone calls handled by MANA"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null) // Telecom/system supplies the ringtone.
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun callActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ManaCallActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun fullScreenCallPendingIntent(): PendingIntent {
        val intent = Intent(this, ManaDialerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(ManaDialerActivity.EXTRA_INCOMING_CALL, true)
        return PendingIntent.getActivity(this, 7101, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun personFor(call: Call): Person {
        val number = call.details.handle?.schemeSpecificPart ?: "Неизвестный номер"
        return Person.Builder().setName(number).setImportant(true).build()
    }

    private fun postIncomingCallNotification(call: Call) {
        val person = personFor(call)
        val answer = callActionPendingIntent(ManaCallActionReceiver.ACTION_ANSWER, 7102)
        val decline = callActionPendingIntent(ManaCallActionReceiver.ACTION_REJECT, 7103)
        val fullScreen = fullScreenCallPendingIntent()
        val builder = Notification.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Входящий вызов")
            .setContentText(person.name)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setStyle(Notification.CallStyle.forIncomingCall(person, decline, answer))
        } else {
            builder.addAction(android.R.drawable.sym_action_call, "Ответить", answer)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отклонить", decline)
        }
        getSystemService(NotificationManager::class.java).notify(CALL_NOTIFICATION_ID, builder.build())
    }

    private fun postOngoingCallNotification(call: Call) {
        if (call.state == Call.STATE_DISCONNECTED) { cancelCallNotification(); return }
        val person = personFor(call)
        val end = callActionPendingIntent(ManaCallActionReceiver.ACTION_END, 7104)
        val builder = Notification.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_outgoing)
            .setContentTitle("Звонок MANA")
            .setContentText(person.name)
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(fullScreenCallPendingIntent())
        if (Build.VERSION.SDK_INT >= 31) builder.setStyle(Notification.CallStyle.forOngoingCall(person, end))
        else builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Завершить", end)
        getSystemService(NotificationManager::class.java).notify(CALL_NOTIFICATION_ID, builder.build())
    }

    private fun cancelCallNotification() {
        getSystemService(NotificationManager::class.java).cancel(CALL_NOTIFICATION_ID)
    }
    private fun launchInCallUi(showDialpad: Boolean = false) {
        val launch = Intent(this, ManaDialerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(ManaDialerActivity.EXTRA_INCOMING_CALL, true)
            .putExtra(ManaDialerActivity.EXTRA_SHOW_DIALPAD, showDialpad)
        runCatching { startActivity(launch) }
    }

    private fun scheduleAutoAnswerIfEnabled(call: Call) {
        val enabled = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_AUTO_ANSWER, false)
        if (!enabled || call.state != Call.STATE_RINGING) return
        val task = Runnable {
            val anotherCallIsActive = calls.any { it !== call && it.state == Call.STATE_ACTIVE }
            if (calls.contains(call) && call.state == Call.STATE_RINGING && !anotherCallIsActive) {
                runCatching { call.answer(VideoProfile.STATE_AUDIO_ONLY) }
            }
            answerTasks.remove(call)
        }
        answerTasks[call] = task
        handler.postDelayed(task, AUTO_ANSWER_DELAY_MS)
    }

    private fun cancelAutoAnswer(call: Call) {
        answerTasks.remove(call)?.let(handler::removeCallbacks)
    }

    private fun broadcastCallChanged() {
        sendBroadcast(Intent(ACTION_CALL_STATE_CHANGED).setPackage(packageName))
    }

    override fun onDestroy() {
        answerTasks.values.forEach(handler::removeCallbacks)
        answerTasks.clear()
        callbacks.forEach { (call, callback) -> runCatching { call.unregisterCallback(callback) } }
        callbacks.clear()
        calls.clear()
        cancelCallNotification()
        currentCall = null
        activeService = null
        super.onDestroy()
    }

    companion object {
        const val PREFS = "mana.receptionist"
        const val KEY_AUTO_ANSWER = "auto_answer_enabled"
        const val ACTION_CALL_STATE_CHANGED = "com.mana.launcher.CALL_STATE_CHANGED"
        const val AUTO_ANSWER_DELAY_MS = 10_000L
        private const val CALL_CHANNEL_ID = "mana_calls"
        private const val CALL_NOTIFICATION_ID = 7100

        private val calls = CopyOnWriteArrayList<Call>()

        @Volatile var currentCall: Call? = null
            private set
        @Volatile var activeService: ManaInCallService? = null
            private set

        fun activeCalls(): List<Call> = calls.toList()

        fun answerCurrentCall() {
            currentCall?.takeIf { it.state == Call.STATE_RINGING }?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun rejectCurrentCall() {
            currentCall?.takeIf { it.state == Call.STATE_RINGING }?.reject(false, null)
        }

        fun endCurrentCall() {
            currentCall?.disconnect()
        }

        fun selectNextCall() {
            val active = calls.filter { it.state != Call.STATE_DISCONNECTED }
            if (active.size < 2) return
            val index = active.indexOf(currentCall)
            currentCall = active[(index + 1).mod(active.size)]
            activeService?.broadcastCallChanged()
        }
        fun holdCurrentCall() {
            currentCall?.let { call ->
                if (call.state == Call.STATE_ACTIVE) call.hold()
                else if (call.state == Call.STATE_HOLDING) call.unhold()
            }
        }

        fun setMuted(muted: Boolean) {
            activeService?.setMuted(muted)
        }

        @Suppress("DEPRECATION")
        fun setSpeaker(enabled: Boolean) {
            activeService?.setAudioRoute(
                if (enabled) android.telecom.CallAudioState.ROUTE_SPEAKER
                else android.telecom.CallAudioState.ROUTE_EARPIECE
            )
        }
    }
}
