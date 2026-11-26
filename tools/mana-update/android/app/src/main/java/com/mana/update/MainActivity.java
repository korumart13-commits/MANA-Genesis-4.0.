package com.mana.update;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
  private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(7,20,38));
    getWindow().setNavigationBarColor(Color.rgb(7,20,38));
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setGravity(Gravity.CENTER_HORIZONTAL);
    root.setPadding(dp(24), dp(40), dp(24), dp(24));
    root.setBackgroundColor(Color.rgb(7,20,38));
    TextView title = new TextView(this);
    title.setText("MANA Update"); title.setTextColor(Color.rgb(84,200,245)); title.setTextSize(30); title.setGravity(Gravity.CENTER);
    root.addView(title, new LinearLayout.LayoutParams(-1, -2));
    TextView state = new TextView(this);
    state.setText("Автоматическая проверка устройства завершена"); state.setTextColor(Color.WHITE); state.setTextSize(18); state.setPadding(0, dp(28), 0, dp(16));
    root.addView(state);
    TextView details = new TextView(this);
    details.setText("Производитель: " + Build.MANUFACTURER + "\nБренд: " + Build.BRAND + "\nМодель: " + Build.MODEL + "\nУстройство: " + Build.DEVICE + "\nAndroid: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\nСборка: " + Build.DISPLAY + "\n\nРежим: безопасная подготовка.\nСистемный OTA-пакет пока не вложен.\nСкрытая установка и обход защиты отключены.");
    details.setTextColor(Color.rgb(234,244,255)); details.setTextSize(15);
    root.addView(details, new LinearLayout.LayoutParams(-1, -2));
    TextView footer = new TextView(this);
    footer.setText("MANA работает только в пределах разрешений Android и поддерживаемых механизмов обновления производителя."); footer.setTextColor(Color.rgb(145,169,197)); footer.setTextSize(13); footer.setPadding(0, dp(28), 0, 0);
    root.addView(footer);
    setContentView(root);
  }
}
