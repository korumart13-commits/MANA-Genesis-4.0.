package com.mana.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.node.ModelNode

/**
 * MANA avatar studio: the same character identity with selectable wardrobe,
 * expression preview and an explicitly owner-enabled floating avatar.
 */
class ManaAvatarPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(4, 7, 14)
        window.navigationBarColor = android.graphics.Color.rgb(4, 7, 14)
        setContent { AvatarStudioScreen() }
    }
}

@androidx.compose.runtime.Composable
private fun AvatarStudioScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mana.settings", Context.MODE_PRIVATE) }
    var outfit by remember { mutableIntStateOf(prefs.getInt("avatar_outfit_index", 0).coerceIn(0, 2)) }
    var accessory by remember { mutableStateOf(prefs.getBoolean("avatar_accessory_enabled", true)) }
    var floating by remember { mutableStateOf(prefs.getBoolean("floating_avatar_enabled", false)) }
    var expression by remember { mutableIntStateOf(0) }
    val asset = remember(outfit, accessory) { ManaAvatarAssetSelector.assetName(outfit, accessory) }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val navy = Color(0xFF071426)
    val panel = Color(0xFF102B50)
    val accent = Color(0xFF54C8F5)
    val foreground = Color(0xFFEAF4FF)
    val muted = Color(0xFF91A9C5)

    Column(
        modifier = Modifier.fillMaxSize().background(navy).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("MANA AVATAR STUDIO", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Твоя Мана", color = foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("GENESIS 4.0 • КОНСТАНТА Ω", color = muted, fontSize = 10.sp)
            }
            Text("Ω", color = accent, fontSize = 34.sp, fontWeight = FontWeight.Light)
        }

        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A1D38))
        ) {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                cameraManipulator = rememberCameraManipulator()
            ) {
                rememberModelInstance(modelLoader, asset)?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 2.25f,
                        autoAnimate = false,
                        animationName = when (expression) {
                            1 -> "Happy"
                            2 -> "Playful"
                            3 -> "Speaking"
                            else -> "Idle"
                        },
                        animationLoop = true
                    )
                }
            }
            Text(
                "Поверни модель жестом двумя пальцами или перетащи сцену",
                color = foreground,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .padding(10.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xAA071426)).padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("ОБРАЗ MANA", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Genesis", "Casual", "Evening").forEachIndexed { index, name ->
                val selected = outfit == index
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Color(0xFF1765A8) else panel)
                        .clickable {
                            outfit = index
                            prefs.edit().putInt("avatar_outfit_index", index).apply()
                        }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name, color = foreground, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Спокойная", "Радостная", "Игривость", "Говорит").forEachIndexed { index, name ->
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (expression == index) Color(0xFF1765A8) else panel)
                        .clickable { expression = index }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name, color = foreground, fontSize = 9.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(panel)
                .clickable {
                    accessory = !accessory
                    prefs.edit().putBoolean("avatar_accessory_enabled", accessory).apply()
                }.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (accessory) "✓  Аксессуары включены" else "＋  Аксессуары выключены", color = foreground, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val intent = Intent(context, ManaFloatingAvatarService::class.java)
                if (floating) {
                    intent.action = ManaFloatingAvatarService.ACTION_STOP
                    context.startService(intent)
                    prefs.edit().putBoolean("floating_avatar_enabled", false).apply()
                    floating = false
                } else if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) {
                    Toast.makeText(context, "Разреши отображение поверх других приложений для плавающего аватара MANA.", Toast.LENGTH_LONG).show()
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    )
                } else {
                    try {
                        prefs.edit().putBoolean("floating_avatar_enabled", true).apply()
                        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
                        else context.startService(intent)
                        floating = true
                    } catch (error: Exception) {
                        prefs.edit().putBoolean("floating_avatar_enabled", false).apply()
                        Toast.makeText(context, "Не удалось запустить аватар: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (floating) Color(0xFF8A354A) else Color(0xFF1765A8))
        ) {
            Text(if (floating) "ОСТАНОВИТЬ ПЛАВАЮЩУЮ МАНУ" else "ПОКАЗЫВАТЬ МАНУ ПОВЕРХ ПРИЛОЖЕНИЙ", color = foreground, fontSize = 11.sp)
        }
        Text(
            "Плавающий аватар работает только после твоего разрешения Android и остаётся обычным приложением, не системным процессом.",
            color = muted, fontSize = 10.sp, modifier = Modifier.padding(top = 7.dp, bottom = 3.dp)
        )
    }
}
