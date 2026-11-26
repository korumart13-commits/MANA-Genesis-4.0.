package com.mana.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * MANA Settings — polished user-space settings hub.
 *
 * Protected radio, permission, role and security controls remain owned by Android.
 * This activity is not a privileged replacement for com.android.settings.
 */
public final class ManaSettingsActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 20, 38);
    private static final int SURFACE = Color.rgb(16, 43, 80);
    private static final int BLUE = Color.rgb(23, 101, 168);
    private static final int CYAN = Color.rgb(84, 200, 245);
    private static final int TEXT = Color.rgb(234, 244, 255);
    private static final int MUTED = Color.rgb(145, 169, 197);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(0);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(NAVY);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView eyebrow = text("GENESIS 4.0  /  SYSTEM", 11, CYAN, true);
        root.addView(eyebrow);
        TextView title = text("MANA Settings", 30, TEXT, true);
        LinearLayout.LayoutParams titleParams = params(-1, -2, 0, 5);
        root.addView(title, titleParams);
        TextView subtitle = text("Один центр управления твоей MANA. Защищённые системные действия выполняются штатными средствами Android.", 14, MUTED, false);
        root.addView(subtitle, params(-1, -2, 0, 18));

        LinearLayout identity = card();
        identity.addView(text("СОСТОЯНИЕ УСТРОЙСТВА", 10, CYAN, true));
        identity.addView(text("MANA интерфейс · Android " + Build.VERSION.RELEASE, 17, TEXT, true), params(-1, -2, 0, 6));
        identity.addView(text(Build.MANUFACTURER + " " + Build.MODEL + "  •  API " + Build.VERSION.SDK_INT, 12, MUTED, false));
        root.addView(identity, params(-1, -2, 0, 18));

        section(root, "СИСТЕМА", "Основные параметры телефона");
        action(root, "Сеть и Wi-Fi", "Подключения, Wi-Fi и мобильная сеть", Settings.ACTION_WIRELESS_SETTINGS);
        action(root, "Bluetooth и устройства", "Наушники, часы и другие устройства", Settings.ACTION_BLUETOOTH_SETTINGS);
        action(root, "Экран и яркость", "Яркость, размер текста и экран", Settings.ACTION_DISPLAY_SETTINGS);
        action(root, "Звук и вибрация", "Громкость, мелодии и режимы", Settings.ACTION_SOUND_SETTINGS);
        action(root, "Батарея", "Энергосбережение и расход заряда", Settings.ACTION_BATTERY_SAVER_SETTINGS);
        action(root, "Приложения", "Установленные приложения и их настройки", Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS);

        section(root, "MANA BRAIN", "Интеллект, память и взаимодействие");
        action(root, "Открыть MANA", "Чат, аватар и голосовой помощник", "com.mana.genesis", "com.mana.launcher.MainActivity");
        action(root, "Разрешения MANA", "Открыть системную страницу разрешений", Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.mana.genesis"));
        action(root, "Конфиденциальность", "Панель безопасности и приватности Android", Settings.ACTION_SECURITY_SETTINGS);

        section(root, "ПЕРСОНАЛИЗАЦИЯ", "Внешний вид и поведение");
        action(root, "Обои и стиль", "Оформление рабочего стола Android", Settings.ACTION_DISPLAY_SETTINGS);
        action(root, "Уведомления", "Каналы и уведомления приложений", Settings.ACTION_APP_NOTIFICATION_SETTINGS);

        section(root, "О MANA", "Версии и сведения");
        LinearLayout about = card();
        about.addView(text("MANA Genesis 4.0", 18, TEXT, true));
        about.addView(text("Пользовательский интерфейс MANA работает поверх Android. Сведения о версии Android, патче безопасности и состоянии Verified Boot отображаются системой и не подменяются MANA.", 13, MUTED, false), params(-1, -2, 0, 8));
        about.addView(text("Build: " + Build.DISPLAY, 11, MUTED, false), params(-1, -2, 0, 8));
        root.addView(about, params(-1, -2, 0, 16));

        setContentView(scroll);
    }

    private void section(LinearLayout root, String name, String description) {
        TextView heading = text(name, 11, CYAN, true);
        root.addView(heading, params(-1, -2, 0, 4));
        root.addView(text(description, 13, MUTED, false), params(-1, -2, 0, 8));
    }

    private void action(LinearLayout root, String title, String detail, String action) {
        addActionCard(root, title, detail, () -> open(new Intent(action)));
    }

    private void action(LinearLayout root, String title, String detail, String action, Uri data) {
        addActionCard(root, title, detail, () -> {
            Intent intent = new Intent(action);
            intent.setData(data);
            open(intent);
        });
    }

    private void action(LinearLayout root, String title, String detail, String packageName, String className) {
        addActionCard(root, title, detail, () -> {
            Intent intent = new Intent();
            intent.setClassName(packageName, className);
            open(intent);
        });
    }

    private void addActionCard(LinearLayout root, String title, String detail, Runnable click) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(16), dp(13), dp(16), dp(13));
        item.setBackground(background(SURFACE, 16));
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v -> click.run());
        item.addView(text(title, 16, TEXT, true));
        item.addView(text(detail, 12, MUTED, false), params(-1, -2, 0, 4));
        root.addView(item, params(-1, -2, 0, 8));
    }

    private void open(Intent intent) {
        try { startActivity(intent); }
        catch (Exception unavailable) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
            catch (Exception ignored) { }
        }
    }

    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackground(background(SURFACE, 20));
        return box;
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), Color.argb(45, 84, 200, 245));
        return drawable;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams params(int width, int height, int top, int bottom) {
        LinearLayout.LayoutParams result = new LinearLayout.LayoutParams(width, height);
        result.topMargin = dp(top);
        result.bottomMargin = dp(bottom);
        return result;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
