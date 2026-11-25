package com.mana.launcher

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural real-time 3D MANA bust for the floating overlay.
 * The character is rendered with lit 3D meshes, articulated limbs, hair,
 * facial features and idle/blink animation. It does not load an external asset.
 */
class ManaAvatar3DView(
    context: Context,
    private val onDrag: (Float, Float) -> Unit,
    private val onTap: () -> Unit
) : GLSurfaceView(context) {
    private val frameHandler = Handler(Looper.getMainLooper())
    private val frameTick = object : Runnable {
        override fun run() {
            requestRender()
            frameHandler.postDelayed(this, 42L)
        }
    }
    private var downX = 0f
    private var downY = 0f
    private var moved = false

    init {
        setEGLContextClientVersion(2)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(AvatarRenderer())
        renderMode = RENDERMODE_WHEN_DIRTY
        isClickable = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        frameHandler.post(frameTick)
    }

    override fun onDetachedFromWindow() {
        frameHandler.removeCallbacks(frameTick)
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
                if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > 7f * resources.displayMetrics.density) moved = true
                if (moved) {
                    onDrag(dx, dy)
                    downX = event.rawX
                    downY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) onTap()
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

    private inner class AvatarRenderer : Renderer {
        private lateinit var sphere: SphereMesh
        private var program = 0
        private var aPosition = 0
        private var aNormal = 0
        private var uMvp = 0
        private var uModel = 0
        private var uColor = 0
        private val projection = FloatArray(16)
        private val view = FloatArray(16)
        private val model = FloatArray(16)
        private val mvp = FloatArray(16)
        private val temp = FloatArray(16)
        private var startTime = 0L
        private var widthPx = 1
        private var heightPx = 1

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            sphere = SphereMesh(24, 32)
            val vertex = """
                uniform mat4 uMvp;
                uniform mat4 uModel;
                attribute vec3 aPosition;
                attribute vec3 aNormal;
                varying vec3 vNormal;
                varying vec3 vPosition;
                void main() {
                    vec4 world = uModel * vec4(aPosition, 1.0);
                    vPosition = world.xyz;
                    vNormal = normalize(mat3(uModel) * aNormal);
                    gl_Position = uMvp * vec4(aPosition, 1.0);
                }
            """.trimIndent()
            val fragment = """
                precision mediump float;
                uniform vec4 uColor;
                varying vec3 vNormal;
                varying vec3 vPosition;
                void main() {
                    vec3 n = normalize(vNormal);
                    vec3 lightDir = normalize(vec3(-0.35, 0.65, 1.0));
                    float diffuse = max(dot(n, lightDir), 0.0);
                    float rim = pow(1.0 - max(dot(n, vec3(0.0, 0.0, 1.0)), 0.0), 2.0);
                    vec3 lit = uColor.rgb * (0.48 + diffuse * 0.62) + vec3(0.10, 0.32, 0.58) * rim * 0.22;
                    gl_FragColor = vec4(lit, uColor.a);
                }
            """.trimIndent()
            program = linkProgram(vertex, fragment)
            aPosition = GLES20.glGetAttribLocation(program, "aPosition")
            aNormal = GLES20.glGetAttribLocation(program, "aNormal")
            uMvp = GLES20.glGetUniformLocation(program, "uMvp")
            uModel = GLES20.glGetUniformLocation(program, "uModel")
            uColor = GLES20.glGetUniformLocation(program, "uColor")
            startTime = System.nanoTime()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            widthPx = width.coerceAtLeast(1)
            heightPx = height.coerceAtLeast(1)
            GLES20.glViewport(0, 0, widthPx, heightPx)
            Matrix.perspectiveM(projection, 0, 33f, widthPx.toFloat() / heightPx, 0.1f, 30f)
            Matrix.setLookAtM(view, 0, 0f, -0.55f, 7.1f, 0f, -0.65f, 0f, 0f, 1f, 0f)
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            GLES20.glUseProgram(program)
            sphere.bind(aPosition, aNormal)
            val t = (System.nanoTime() - startTime) / 1_000_000_000f
            val bob = sin(t * 1.45f) * 0.035f
            val sway = sin(t * 0.82f) * 2.2f
            val blink = if ((t % 4.6f) in 0.0f..0.16f) 0.12f else 1f

            // Back hair silhouette and long side locks.
            draw(-0.02f, -0.04f + bob, -0.34f, 0.63f, 0.96f, 0.38f, 0f, hair)
            draw(-0.43f, -0.40f + bob, -0.12f, 0.19f, 0.76f, 0.22f, -7f + sway, hair)
            draw(0.42f, -0.38f + bob, -0.12f, 0.19f, 0.78f, 0.22f, 8f + sway, hair)
            draw(-0.53f, -0.83f + bob, -0.10f, 0.13f, 0.42f, 0.16f, -14f + sway, hairHighlight)
            draw(0.52f, -0.80f + bob, -0.10f, 0.13f, 0.43f, 0.16f, 14f + sway, hairHighlight)

            // Torso, waist, skirt, legs, boots, shoulders and articulated arms.
            draw(0f, -0.94f + bob, 0f, 0.57f, 0.70f, 0.31f, 0f, coat)
            draw(0f, -1.48f + bob, 0.01f, 0.37f, 0.22f, 0.25f, 0f, collar)
            draw(0f, -1.72f + bob, 0.01f, 0.48f, 0.32f, 0.34f, 0f, coat)
            draw(-0.19f, -2.12f + bob, 0.02f, 0.145f, 0.48f, 0.17f, -2f + sway, stockings)
            draw(0.19f, -2.12f + bob, 0.02f, 0.145f, 0.48f, 0.17f, 2f + sway, stockings)
            draw(-0.19f, -2.56f + bob, 0.08f, 0.18f, 0.22f, 0.25f, -2f, boots)
            draw(0.19f, -2.56f + bob, 0.08f, 0.18f, 0.22f, 0.25f, 2f, boots)
            draw(0f, -1.68f + bob, 0.355f, 0.045f, 0.045f, 0.025f, 0f, glow)
            draw(-0.47f, -0.60f + bob, 0f, 0.22f, 0.34f, 0.25f, -20f + sway, coat)
            draw(0.47f, -0.60f + bob, 0f, 0.22f, 0.34f, 0.25f, 20f + sway, coat)
            draw(-0.60f, -0.98f + bob, 0.02f, 0.16f, 0.48f, 0.17f, -8f, coat)
            draw(0.60f, -0.98f + bob, 0.02f, 0.16f, 0.48f, 0.17f, 8f, coat)
            draw(0f, -0.30f + bob, 0.04f, 0.22f, 0.20f, 0.20f, 0f, collar)
            draw(0f, -0.12f + bob, 0.02f, 0.18f, 0.27f, 0.18f, 0f, skin)

            // Head and face.
            draw(0f, 0.49f + bob, 0.03f, 0.46f, 0.61f, 0.39f, sway, skin)
            draw(-0.47f, 0.47f + bob, 0.02f, 0.075f, 0.16f, 0.10f, 0f, skin)
            draw(0.47f, 0.47f + bob, 0.02f, 0.075f, 0.16f, 0.10f, 0f, skin)
            // Hair cap and swept fringe.
            draw(0f, 0.92f + bob, -0.01f, 0.49f, 0.32f, 0.42f, sway, hair)
            draw(-0.22f, 0.78f + bob, 0.29f, 0.23f, 0.25f, 0.13f, -22f + sway, hair)
            draw(0.10f, 0.83f + bob, 0.31f, 0.27f, 0.20f, 0.13f, 13f + sway, hair)
            draw(0.33f, 0.70f + bob, 0.27f, 0.13f, 0.28f, 0.12f, 28f + sway, hair)
            // Eyes, irises, highlights, nose and mouth.
            draw(-0.17f, 0.52f + bob, 0.398f, 0.085f, 0.085f * blink, 0.035f, 0f, eyeWhite)
            draw(0.17f, 0.52f + bob, 0.398f, 0.085f, 0.085f * blink, 0.035f, 0f, eyeWhite)
            draw(-0.17f, 0.52f + bob, 0.427f, 0.046f, 0.058f * blink, 0.022f, 0f, iris)
            draw(0.17f, 0.52f + bob, 0.427f, 0.046f, 0.058f * blink, 0.022f, 0f, iris)
            draw(-0.185f, 0.545f + bob, 0.447f, 0.016f, 0.020f, 0.012f, 0f, eyeWhite)
            draw(0.155f, 0.545f + bob, 0.447f, 0.016f, 0.020f, 0.012f, 0f, eyeWhite)
            draw(0f, 0.34f + bob, 0.415f, 0.035f, 0.050f, 0.035f, 0f, skinShadow)
            val mouthOpen = if (isSpeaking) 0.060f + (sin(t * 18f).coerceAtLeast(0f) * 0.035f) else 0.012f
            draw(0f, 0.20f + bob, 0.400f, 0.078f, mouthOpen, 0.025f, 0f, mouth)
            draw(0f, 0.205f + bob, 0.425f, 0.070f, if (isSpeaking) 0.024f else 0.012f, 0.018f, 0f, lips)
            draw(-0.30f, 0.31f + bob, 0.31f, 0.09f, 0.045f, 0.02f, 0f, blush)
            draw(0.30f, 0.31f + bob, 0.31f, 0.09f, 0.045f, 0.02f, 0f, blush)

            // Blue accents and a small chest emblem.
            draw(0f, -0.63f + bob, 0.318f, 0.10f, 0.10f, 0.025f, 0f, accent)
            draw(0f, -0.63f + bob, 0.345f, 0.045f, 0.045f, 0.018f, 0f, glow)
            sphere.unbind(aPosition, aNormal)
        }

        private fun draw(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, rz: Float, color: FloatArray) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, x, y, z)
            Matrix.rotateM(model, 0, rz, 0f, 0f, 1f)
            Matrix.scaleM(model, 0, sx, sy, sz)
            Matrix.multiplyMM(temp, 0, view, 0, model, 0)
            Matrix.multiplyMM(mvp, 0, projection, 0, temp, 0)
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
            GLES20.glUniform4fv(uColor, 1, color, 0)
            sphere.draw()
        }

        private fun linkProgram(vertexSource: String, fragmentSource: String): Int {
            fun compile(type: Int, source: String): Int {
                val shader = GLES20.glCreateShader(type)
                GLES20.glShaderSource(shader, source)
                GLES20.glCompileShader(shader)
                val status = IntArray(1)
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
                check(status[0] == GLES20.GL_TRUE) { GLES20.glGetShaderInfoLog(shader) }
                return shader
            }
            val vs = compile(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fs = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            val p = GLES20.glCreateProgram()
            GLES20.glAttachShader(p, vs)
            GLES20.glAttachShader(p, fs)
            GLES20.glLinkProgram(p)
            val status = IntArray(1)
            GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) { GLES20.glGetProgramInfoLog(p) }
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
            return p
        }
    }

    private class SphereMesh(latitudes: Int, longitudes: Int) {
        private val vertices: FloatBuffer
        private val count: Int

        init {
            val data = ArrayList<Float>()
            for (lat in 0 until latitudes) {
                val theta0 = PI * lat / latitudes
                val theta1 = PI * (lat + 1) / latitudes
                for (lon in 0 until longitudes) {
                    val phi0 = 2.0 * PI * lon / longitudes
                    val phi1 = 2.0 * PI * (lon + 1) / longitudes
                    val p00 = point(theta0, phi0)
                    val p01 = point(theta0, phi1)
                    val p10 = point(theta1, phi0)
                    val p11 = point(theta1, phi1)
                    add(data, p00); add(data, p10); add(data, p11)
                    add(data, p00); add(data, p11); add(data, p01)
                }
            }
            count = data.size / 6
            val raw = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
            vertices = raw.asFloatBuffer().apply { data.forEach(::put); position(0) }
        }

        private fun point(theta: Double, phi: Double): FloatArray {
            val x = (sin(theta) * cos(phi)).toFloat()
            val y = cos(theta).toFloat()
            val z = (sin(theta) * sin(phi)).toFloat()
            return floatArrayOf(x, y, z, x, y, z)
        }

        private fun add(out: MutableList<Float>, p: FloatArray) { p.forEach(out::add) }

        fun bind(position: Int, normal: Int) {
            vertices.position(0)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 24, vertices)
            vertices.position(3)
            GLES20.glEnableVertexAttribArray(normal)
            GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, 24, vertices)
            vertices.position(0)
        }

        fun draw() = GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)

        fun unbind(position: Int, normal: Int) {
            GLES20.glDisableVertexAttribArray(position)
            GLES20.glDisableVertexAttribArray(normal)
        }
    }

    companion object {
        private val skin = floatArrayOf(0.96f, 0.76f, 0.69f, 1f)
        private val skinShadow = floatArrayOf(0.77f, 0.48f, 0.44f, 1f)
        private val hair = floatArrayOf(0.045f, 0.065f, 0.11f, 1f)
        private val hairHighlight = floatArrayOf(0.12f, 0.24f, 0.38f, 1f)
        private val coat = floatArrayOf(0.025f, 0.055f, 0.105f, 1f)
        private val stockings = floatArrayOf(0.055f, 0.10f, 0.18f, 1f)
        private val boots = floatArrayOf(0.012f, 0.025f, 0.052f, 1f)
        private val mouth = floatArrayOf(0.18f, 0.035f, 0.07f, 1f)
        private val collar = floatArrayOf(0.10f, 0.19f, 0.31f, 1f)
        private val eyeWhite = floatArrayOf(0.93f, 0.96f, 1f, 1f)
        private val iris = floatArrayOf(0.08f, 0.52f, 0.86f, 1f)
        private val lips = floatArrayOf(0.66f, 0.22f, 0.32f, 1f)
        @Volatile var isSpeaking = false
        private val blush = floatArrayOf(0.95f, 0.38f, 0.43f, 0.48f)
        private val accent = floatArrayOf(0.05f, 0.43f, 0.78f, 1f)
        private val glow = floatArrayOf(0.32f, 0.78f, 1f, 1f)
    }
}
