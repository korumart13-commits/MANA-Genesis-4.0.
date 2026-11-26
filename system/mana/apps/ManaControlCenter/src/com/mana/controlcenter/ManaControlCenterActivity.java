package com.mana.controlcenter;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ManaControlCenterActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 20, 38);       // #071426
    private static final int SURFACE = Color.rgb(16, 43, 80);  // #102B50
    private static final int PRIMARY = Color.rgb(23, 101, 168);// #1765A8
    private static final int ACCENT = Color.rgb(84, 200, 245);// #54C8F5
    private static final int TEXT = Color.rgb(234, 244, 255); // #EAF4FF
    private static final int SECONDARY = Color.rgb(145, 169, 197);// #91A9C5

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(NAVY);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(24));
        scroll.addView(root);

        TextView eyebrow = new TextView(this);
        eyebrow.setText("MANA OS  •  GENESIS 4.0");
        eyebrow.setTextColor(ACCENT);
        eyebrow.setTextSize(12);
        eyebrow.setLetterSpacing(0.12f);
        root.addView(eyebrow);

        TextView title = new TextView(this);
        title.setText("Control Center");
        title.setTextColor(TEXT);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, dp(5), 0, 0);
        root.addView(title);

        TextView status = new TextView(this);
        status.setText("Быстрый доступ к системным настройкам. Защищённые функции и разрешения остаются под контролем Android.");
        status.setTextColor(SECONDARY);
        status.setTextSize(14);
        status.setPadding(0, dp(8), 0, dp(20));
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
        button.setTextColor(TEXT);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(18), dp(10), dp(18), dp(10));
        GradientDrawable background = new GradientDrawable();
        background.setColor(SURFACE);
        background.setCornerRadius(dp(14));
        background.setStroke(dp(1), PRIMARY);
        button.setBackground(background);
        button.setOnClickListener(v -> {
            try { startActivity(new Intent(action)); }
            catch (Exception ignored) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(58));
        params.bottomMargin = dp(10);
        root.addView(button, params);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
