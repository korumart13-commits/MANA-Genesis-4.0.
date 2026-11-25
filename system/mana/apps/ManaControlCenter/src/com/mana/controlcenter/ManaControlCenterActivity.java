package com.mana.controlcenter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ManaControlCenterActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 20, 38);
    private static final int BLUE = Color.rgb(84, 200, 245);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(NAVY);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(24));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("MANA Control Center");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView status = new TextView(this);
        status.setText("Быстрый доступ к системным панелям Android. Изменение защищённых радиомодулей и системных параметров остаётся под контролем Android.");
        status.setTextColor(BLUE);
        status.setTextSize(13);
        status.setPadding(0, dp(8), 0, dp(18));
        root.addView(status);

        add(root, "Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        add(root, "Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS);
        add(root, "Мобильная сеть", Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        add(root, "Яркость и экран", Settings.ACTION_DISPLAY_SETTINGS);
        add(root, "Громкость и звук", Settings.ACTION_SOUND_SETTINGS);
        add(root, "Энергосбережение", Settings.ACTION_BATTERY_SAVER_SETTINGS);
        add(root, "Геолокация", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        add(root, "Уведомления", Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        setContentView(scroll);
    }

    private void add(LinearLayout root, String label, String action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> {
            try { startActivity(new Intent(action)); }
            catch (Exception ignored) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        root.addView(button, params);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
