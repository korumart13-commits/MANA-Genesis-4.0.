package com.mana.launcher

import android.content.Intent
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
                broadcastCallChanged()
            }
        }
        callbacks[call] = callback
        call.registerCallback(callback)
        launchInCallUi()
        scheduleAutoAnswerIfEnabled(call)
        broadcastCallChanged()
    }

    override fun onCallRemoved(call: Call) {
        cancelAutoAnswer(call)
        callbacks.remove(call)?.let { runCatching { call.unregisterCallback(it) } }
        calls.remove(call)
        if (currentCall === call) currentCall = calls.lastOrNull { it.state != Call.STATE_DISCONNECTED }
        broadcastCallChanged()
        super.onCallRemoved(call)
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        launchInCallUi(showDialpad)
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
        currentCall = null
        activeService = null
        super.onDestroy()
    }

    companion object {
        const val PREFS = "mana.receptionist"
        const val KEY_AUTO_ANSWER = "auto_answer_enabled"
        const val ACTION_CALL_STATE_CHANGED = "com.mana.launcher.CALL_STATE_CHANGED"
        const val AUTO_ANSWER_DELAY_MS = 10_000L

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
