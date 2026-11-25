package com.mana.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Notification actions are routed to the active Telecom Call owned by InCallService. */
class ManaCallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER -> ManaInCallService.answerCurrentCall()
            ACTION_REJECT -> ManaInCallService.rejectCurrentCall()
            ACTION_END -> ManaInCallService.endCurrentCall()
        }
    }

    companion object {
        const val ACTION_ANSWER = "com.mana.launcher.call.ANSWER"
        const val ACTION_REJECT = "com.mana.launcher.call.REJECT"
        const val ACTION_END = "com.mana.launcher.call.END"
    }
}
