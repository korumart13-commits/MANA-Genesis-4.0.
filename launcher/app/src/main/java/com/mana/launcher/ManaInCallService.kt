package com.mana.launcher

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile

/**
 * Optional default-dialer integration. It is inert until the owner explicitly
 * grants ROLE_DIALER and enables the 10-second auto-answer preference.
 */
class ManaInCallService : InCallService() {
    private val handler = Handler(Looper.getMainLooper())
    private var activeCallback: Call.Callback? = null
    private var autoAnswerTask: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        activeService = this
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        activeCallback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                if (state != Call.STATE_RINGING) cancelAutoAnswer()
                broadcastCallChanged()
            }
        }
        call.registerCallback(activeCallback!!)
        val launch = Intent(this, ManaDialerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(ManaDialerActivity.EXTRA_INCOMING_CALL, true)
        runCatching { startActivity(launch) }
        scheduleAutoAnswerIfEnabled(call)
        broadcastCallChanged()
    }

    override fun onCallRemoved(call: Call) {
        cancelAutoAnswer()
        activeCallback?.let { runCatching { call.unregisterCallback(it) } }
        activeCallback = null
        if (currentCall === call) currentCall = null
        broadcastCallChanged()
        super.onCallRemoved(call)
    }

    private fun scheduleAutoAnswerIfEnabled(call: Call) {
        val enabled = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_AUTO_ANSWER, false)
        if (!enabled || call.state != Call.STATE_RINGING) return
        val task = Runnable {
            if (currentCall === call && call.state == Call.STATE_RINGING) {
                runCatching { call.answer(VideoProfile.STATE_AUDIO_ONLY) }
            }
        }
        autoAnswerTask = task
        handler.postDelayed(task, AUTO_ANSWER_DELAY_MS)
    }

    private fun cancelAutoAnswer() {
        autoAnswerTask?.let(handler::removeCallbacks)
        autoAnswerTask = null
    }

    private fun broadcastCallChanged() {
        sendBroadcast(Intent(ACTION_CALL_STATE_CHANGED).setPackage(packageName))
    }

    override fun onDestroy() {
        cancelAutoAnswer()
        activeService = null
        currentCall = null
        super.onDestroy()
    }

    companion object {
        const val PREFS = "mana.receptionist"
        const val KEY_AUTO_ANSWER = "auto_answer_enabled"
        const val ACTION_CALL_STATE_CHANGED = "com.mana.launcher.CALL_STATE_CHANGED"
        const val AUTO_ANSWER_DELAY_MS = 10_000L

        @Volatile var currentCall: Call? = null
            private set
        @Volatile var activeService: ManaInCallService? = null
            private set

        fun answerCurrentCall() {
            currentCall?.takeIf { it.state == Call.STATE_RINGING }?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun rejectCurrentCall() {
            currentCall?.takeIf { it.state == Call.STATE_RINGING }?.reject(false, null)
        }

        fun endCurrentCall() {
            currentCall?.disconnect()
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
