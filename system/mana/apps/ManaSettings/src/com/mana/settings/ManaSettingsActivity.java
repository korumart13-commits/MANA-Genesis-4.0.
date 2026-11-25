package com.mana.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ManaSettingsActivity extends Activity {
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
        title.setText("MANA Settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView note = new TextView(this);
        note.setText("Единый центр настроек MANA. Защищённые системные переключатели открываются в штатных настройках Android — без обхода разрешений и скрытых привилегий.");
        note.setTextColor(BLUE);
        note.setTextSize(13);
        note.setPadding(0, dp(8), 0, dp(18));
        root.addView(note);

        add(root, "Сеть и Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        add(root, "Bluetooth и устройства", Settings.ACTION_BLUETOOTH_SETTINGS);
        add(root, "Экран и яркость", Settings.ACTION_DISPLAY_SETTINGS);
        add(root, "Звук и вибрация", Settings.ACTION_SOUND_SETTINGS);
        add(root, "Батарея и энергосбережение", Settings.ACTION_BATTERY_SAVER_SETTINGS);
        add(root, "Приложения и разрешения", Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);
        add(root, "Безопасность и конфиденциальность", Settings.ACTION_SECURITY_SETTINGS);
        add(root, "Разрешения MANA", Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:com.mana.genesis"));
        setContentView(scroll);
    }

    private void add(LinearLayout root, String label, String action) {
        add(root, label, action, null);
    }

    private void add(LinearLayout root, String label, String action, Uri data) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(action);
            if (data != null) intent.setData(data);
            try { startActivity(intent); }
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
