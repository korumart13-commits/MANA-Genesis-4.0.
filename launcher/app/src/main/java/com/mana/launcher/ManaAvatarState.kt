package com.mana.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Process-wide UI state shared by the assistant screen and the floating avatar. */
object ManaAvatarState {
    var isSpeaking by mutableStateOf(false)
    var expressionIndex by mutableIntStateOf(0)
}

