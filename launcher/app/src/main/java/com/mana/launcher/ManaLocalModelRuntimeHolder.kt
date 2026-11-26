package com.mana.launcher

import android.content.Context
import java.io.File

/**
 * One LiteRT-LM engine per companion app process.
 * The Activity UI and exported provider Binder service share this instance instead of
 * loading a second copy of a potentially multi-gigabyte model.
 */
object ManaLocalModelRuntimeHolder {
    @Volatile private var instance: LocalModelRuntime? = null

    fun get(context: Context): LocalModelRuntime =
        instance ?: synchronized(this) {
            instance ?: LocalModelRuntime(File(context.applicationContext.cacheDir, "mana-litert-cache"))
                .also { instance = it }
        }
}
