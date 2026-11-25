package com.mana.launcher

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.Matrix
import android.util.Base64
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import com.google.android.filament.View as FilamentView
import com.google.android.filament.Renderer
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * Filament-backed floating avatar view. Loads the original local MANA glTF
 * asset, including its idle/blink animation. The model is bundled locally;
 * no network request or remote model dependency is used.
 */
class ManaFilamentAvatarView(
    context: Context,
    private val onDrag: (Float, Float) -> Unit,
    private val onTap: () -> Unit,
    private val onDragEnd: () -> Unit
) : SurfaceView(context), Choreographer.FrameCallback {
    private var viewer: ModelViewer? = null
    private var running = false
    private var downX = 0f
    private var downY = 0f
    private var moved = false

    init {
        Utils.init()
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        isClickable = true
        createViewer()
    }

    private fun createViewer() {
        if (viewer != null) return
        val modelViewer = ModelViewer(this)
        modelViewer.autoPlayAnimations = true
        modelViewer.view.blendMode = FilamentView.BlendMode.TRANSLUCENT
        modelViewer.renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = Renderer.ClearOptions.Clear.COLOR or Renderer.ClearOptions.Clear.DEPTH
            clearColor = floatArrayOf(0f, 0f, 0f, 0f)
        }
        viewer = modelViewer
        try {
            val json = context.assets.open("mana_avatar_genesis.gltf").use { it.readBytes() }
            val gltfBuffer = ByteBuffer.wrap(json)
            modelViewer.loadModelGltf(gltfBuffer) { uri ->
                if (uri.startsWith("data:")) {
                    val encoded = uri.substringAfter("base64,", "")
                    require(encoded.isNotEmpty()) { "MANA avatar buffer URI is invalid." }
                    ByteBuffer.wrap(Base64.decode(encoded, Base64.DEFAULT))
                } else {
                    context.assets.open(uri).use { ByteBuffer.wrap(it.readBytes()) }
                }
            }
            modelViewer.transformToUnitCube()
        } catch (error: Exception) {
            modelViewer.destroy()
            viewer = null
            throw IllegalStateException("Не удалось загрузить 3D-модель MANA: ${error.message}", error)
        }
    }

    fun startRendering() {
        if (running) return
        running = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stopRendering() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running || !isAttachedToWindow) return
        viewer?.render(frameTimeNanos)
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startRendering()
    }

    override fun onDetachedFromWindow() {
        stopRendering()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                moved = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (abs(dx) + abs(dy) > 7f * resources.displayMetrics.density) moved = true
                if (moved) {
                    onDrag(dx, dy)
                    downX = event.rawX
                    downY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (moved) onDragEnd() else onTap()
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
