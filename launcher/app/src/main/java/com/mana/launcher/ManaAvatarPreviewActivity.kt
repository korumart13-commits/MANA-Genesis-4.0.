package com.mana.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.node.ModelNode

/** Full-body animated glTF preview; touch gestures orbit the character. */
class ManaAvatarPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(4, 7, 14)
        window.navigationBarColor = android.graphics.Color.rgb(4, 7, 14)
        val avatarAsset = ManaAvatarAssetSelector.assetName(this)
        setContent {
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)
            val isSpeaking = ManaAvatarState.isSpeaking
            val expression = ManaAvatarState.expressionIndex
            val animationName = when { isSpeaking -> "Speaking"; expression == 1 -> "Happy"; expression == 2 -> "Playful"; else -> "Idle" }
            SceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                cameraManipulator = rememberCameraManipulator()
            ) {
                rememberModelInstance(modelLoader, avatarAsset)?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 2.25f,
                        autoAnimate = false,
                        animationName = animationName,
                        animationLoop = true
                    )
                }
            }
        }
    }
}
