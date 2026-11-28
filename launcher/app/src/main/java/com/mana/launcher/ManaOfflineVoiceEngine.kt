package com.mana.launcher

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bundled offline Russian female neural voice for MANA.
 * The voice model and native inference run locally; no network or Android system TTS engine is used.
 */
class ManaOfflineVoiceEngine(context: Context) : AutoCloseable {
    private val app = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "MANA-Offline-Voice") }
    private val closed = AtomicBoolean(false)
    @Volatile private var tts: OfflineTts? = null

    fun initialize(callback: (Result<Unit>) -> Unit) {
        if (closed.get()) {
            callback(Result.failure(IllegalStateException("MANA voice engine is closed")))
            return
        }
        executor.execute {
            try {
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = "mana_voice/ru_RU-irina-medium.onnx",
                            tokens = "mana_voice/tokens.txt",
                            dataDir = "mana_voice/espeak-ng-data"
                        ),
                        numThreads = 2,
                        debug = false,
                        provider = "cpu"
                    ),
                    maxNumSentences = 1
                )
                tts = OfflineTts(assetManager = app.assets, config = config)
                callback(Result.success(Unit))
            } catch (error: Throwable) {
                callback(Result.failure(error))
            }
        }
    }

    fun speak(
        text: String,
        onStarted: () -> Unit,
        onFinished: () -> Unit,
        onError: (Throwable) -> Unit = {}
    ) {
        val clean = text.trim().take(4000)
        if (clean.isEmpty() || closed.get()) return
        executor.execute {
            try {
                val engine = tts ?: error("Офлайн-голос Маны ещё загружается")
                val audio = engine.generate(clean, sid = 0, speed = 0.94f)
                val samples = audio.samples
                require(samples.isNotEmpty()) { "Офлайн-голос не вернул аудиоданные" }
                val format = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(audio.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(format)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(maxOf(32_768, audio.sampleRate * 2 * 4))
                    .build()
                try {
                    onStarted()
                    track.play()
                    var offset = 0
                    while (offset < samples.size && !closed.get()) {
                        val count = minOf(8192, samples.size - offset)
                        val written = track.write(samples, offset, count, AudioTrack.WRITE_BLOCKING)
                        check(written > 0) { "AudioTrack write failed: $written" }
                        offset += written
                    }
                    while (!closed.get() && track.playbackHeadPosition.toLong() < samples.size.toLong()) {
                        Thread.sleep(25)
                    }
                } finally {
                    runCatching { track.pause() }
                    runCatching { track.flush() }
                    track.release()
                    onFinished()
                }
            } catch (error: Throwable) {
                onFinished()
                onError(error)
            }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        executor.execute {
            runCatching { tts?.release() }
            tts = null
        }
        executor.shutdown()
    }
}
