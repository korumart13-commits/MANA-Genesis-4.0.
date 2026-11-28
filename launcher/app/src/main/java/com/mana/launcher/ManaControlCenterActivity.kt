package com.mana.launcher

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Safe quick-settings hub: delegates protected device toggles to Android Settings. */
class ManaControlCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(4, 7, 14)
        window.navigationBarColor = android.graphics.Color.rgb(4, 7, 14)
        setContent { ManaControlCenterScreen() }
    }
}

@androidx.compose.runtime.Composable
private fun ManaControlCenterScreen() {
    val context = LocalContext.current
    val navy = Color(0xFF071426)
    val panel = Color(0xFF102B50)
    val accent = Color(0xFF54C8F5)
    val foreground = Color(0xFFEAF4FF)
    val muted = Color(0xFF91A9C5)
    Column(
        modifier = Modifier.fillMaxSize().background(navy).verticalScroll(rememberScrollState()).padding(18.dp)
    ) {
        Text("MANA • CONTROL CENTER", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Центр управления", color = foreground, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Быстрый доступ к системным настройкам телефона", color = muted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            ControlTile("◉", "Wi-Fi", "Сети и подключение", panel, foreground, muted, Modifier.weight(1f)) {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            ControlTile("ᛒ", "Bluetooth", "Наушники и часы", panel, foreground, muted, Modifier.weight(1f)) {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
            ControlTile("◖", "Звук", "Громкость и режимы", panel, foreground, muted, Modifier.weight(1f)) {
                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
            }
            ControlTile("☼", "Экран", "Яркость и дисплей", panel, foreground, muted, Modifier.weight(1f)) {
                context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("СИСТЕМА И БЕЗОПАСНОСТЬ", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        ControlTile("⌑", "Защита телефона", "PIN, пароль, биометрия и блокировка", panel, foreground, muted, Modifier.fillMaxWidth()) {
            context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
        }
        Spacer(Modifier.height(8.dp))
        ControlTile("▣", "Разрешения MANA", "Управление доступом приложения", panel, foreground, muted, Modifier.fillMaxWidth()) {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
        Spacer(Modifier.height(8.dp))
        ControlTile("⚙", "Все настройки Android", "Штатные параметры устройства", panel, foreground, muted, Modifier.fillMaxWidth()) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "MANA не меняет защищённые системные переключатели скрытно. Каждый системный параметр открывается в официальном интерфейсе Android.",
            color = muted, fontSize = 11.sp
        )
    }
}

@androidx.compose.runtime.Composable
private fun ControlTile(
    symbol: String,
    title: String,
    subtitle: String,
    panel: Color,
    foreground: Color,
    muted: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).background(panel)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, color = Color(0xFF54C8F5), fontSize = 25.sp, modifier = Modifier.padding(end = 11.dp))
        Column {
            Text(title, color = foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = muted, fontSize = 10.sp)
        }
    }
}
