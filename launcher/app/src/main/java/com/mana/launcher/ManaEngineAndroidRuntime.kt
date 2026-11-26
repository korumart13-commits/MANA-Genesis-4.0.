package com.mana.launcher

import android.content.Context
import android.os.Build
import com.mana.os.core.EncryptedFileMicroworldStore
import com.mana.os.core.ManaEngineHealth
import com.mana.os.core.ManaEngineRequest
import com.mana.os.core.ManaEngineRuntime
import com.mana.os.core.ManaHydraRegistry
import com.mana.os.core.ManaMemoryService
import com.mana.os.core.ManaMicroworldService
import com.mana.os.core.ManaReasoningPort
import com.mana.os.core.ManaReasoningRequest
import com.mana.os.core.ManaReasoningResult
import com.mana.os.core.ManaSecurityService
import com.mana.os.core.ManaShogun
import com.mana.os.core.ManaSystemCore
import com.mana.os.core.ManaSystemContext
import com.mana.os.core.ManaSystemModule
import com.mana.os.core.DeviceFacts
import java.io.File

/**
 * Android composition root for the shared MANA Engine runtime.
 * This is an application-process integration prototype, not a privileged OS daemon.
 */
class ManaEngineAndroidRuntime(
    context: Context,
    private val brain: ManaBrainBridge,
    memory: ManaMemoryService,
    keyProvider: ManaAndroidKeyProvider
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val microworld = ManaMicroworldService(
        EncryptedFileMicroworldStore(
            File(appContext.filesDir, "mana/microworld.bin"),
            keyProvider,
            "mana.microworld.v1"
        )
    )
    private val security = ManaSecurityService()
    private val hydra = ManaHydraRegistry(security)
    private val shogun = ManaShogun(hydra)
    private val reasoning = ManaReasoningPort { request ->
        val result = brain.respondDetailed(
            prompt = request.prompt,
            allowCloud = request.allowCloud,
            sensitive = request.sensitive,
            preferredCloudProvider = request.preferredCloudProvider
        )
        ManaReasoningResult(result.text, result.provider, result.local)
    }
    private val system = ManaSystemCore(
        DeviceFacts(
            product = Build.PRODUCT.ifBlank { "android-product" },
            device = Build.DEVICE.ifBlank { "android-device" },
            androidApi = Build.VERSION.SDK_INT,
            kernelRelease = System.getProperty("os.version") ?: "unknown",
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        )
    )
    private val runtime: ManaEngineRuntime

    init {
        system.register(object : ManaSystemModule {
            override val id = "engine.memory"
            override fun start(context: ManaSystemContext) { context.services["memory"] = memory }
        })
        system.register(object : ManaSystemModule {
            override val id = "engine.microworld"
            override val dependencies = setOf("engine.memory")
            override fun start(context: ManaSystemContext) { context.services["microworld"] = microworld }
        })
        system.register(object : ManaSystemModule {
            override val id = "engine.brain"
            override fun start(context: ManaSystemContext) { context.services["reasoning"] = reasoning }
        })
        system.register(object : ManaSystemModule {
            override val id = "engine.shogun"
            override val dependencies = setOf("engine.brain", "engine.memory", "engine.microworld")
            override fun start(context: ManaSystemContext) {
                context.services["shogun"] = shogun
                context.services["hydra"] = hydra
            }
        })
        runtime = ManaEngineRuntime(system, reasoning, memory, microworld, shogun)
    }

    fun start(): ManaEngineHealth = runtime.start()

    suspend fun think(
        text: String,
        allowCloud: Boolean = false,
        sensitive: Boolean = false,
        preferredCloudProvider: String? = null,
        includePersonalMemory: Boolean = true,
        memoryQuery: String = text,
        allowPersonalMemoryToCloud: Boolean = false
    ) = runtime.think(
        ManaEngineRequest(
            text = text,
            allowCloud = allowCloud,
            sensitive = sensitive,
            preferredCloudProvider = preferredCloudProvider,
            includePersonalMemory = includePersonalMemory,
            memoryQuery = memoryQuery,
            allowPersonalMemoryToCloud = allowPersonalMemoryToCloud
        )
    )

    fun health(): ManaEngineHealth = runtime.health()

    override fun close() {
        runtime.stop()
    }
}
