package com.mana.launcher

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession

/** Opens MANA's avatar panel when the user invokes the selected system assistant. */
class ManaVoiceInteractionSession(context: android.content.Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        setUiEnabled(false)
        val launch = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_AVATAR, true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startAssistantActivity(launch)
        hide()
    }
}
