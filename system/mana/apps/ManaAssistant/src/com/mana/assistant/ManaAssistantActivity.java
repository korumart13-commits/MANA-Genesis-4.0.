package com.mana.assistant;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class ManaAssistantActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 20, 38);       // #071426
    private static final int SURFACE = Color.rgb(16, 43, 80);   // #102B50
    private static final int PRIMARY = Color.rgb(23, 101, 168); // #1765A8
    private static final int BLUE = Color.rgb(84, 200, 245);    // #54C8F5
    private static final int TEXT = Color.rgb(234, 244, 255);   // #EAF4FF
    private static final int MUTED = Color.rgb(145, 169, 197);  // #91A9C5
    private TextView status;
    private EditText prompt;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(NAVY);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(30), dp(22), dp(22));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("MANA");
        title.setTextColor(TEXT);
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView identity = new TextView(this);
        identity.setText("Genesis 4.0  •  Единый системный помощник");
        identity.setTextColor(BLUE);
        identity.setTextSize(14);
        root.addView(identity);

        status = new TextView(this);
        status.setTextColor(MUTED);
        status.setTextSize(15);
        status.setPadding(0, dp(22), 0, dp(18));
        status.setText("Системная точка входа MANA. Открой companion-приложение, чтобы использовать ИИ, локальную память и 3D-аватар.");
        root.addView(status);

        prompt = new EditText(this);
        prompt.setTextColor(TEXT);
        prompt.setHintTextColor(MUTED);
        prompt.setBackgroundColor(SURFACE);
        prompt.setHint("Сообщение для Маны (передаётся в приложение)");
        prompt.setMinLines(2);
        prompt.setGravity(48);
        root.addView(prompt);

        Button open = new Button(this);
        open.setText("Открыть MANA Genesis");
        open.setAllCaps(false);
        open.setOnClickListener(this::openCompanion);
        root.addView(open);

        Button settings = new Button(this);
        settings.setText("Настройки приложения MANA");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:com.mana.genesis"));
            try { startActivity(intent); }
            catch (ActivityNotFoundException error) { status.setText("Настройки приложения недоступны на этой прошивке."); }
        });
        root.addView(settings);
        setContentView(scroll);
    }

    private void openCompanion(View ignored) {
        Intent intent = new Intent();
        intent.setClassName("com.mana.genesis", "com.mana.launcher.MainActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("com.mana.launcher.OPEN_AVATAR_FROM_SYSTEM_ASSISTANT", true);
        String initialPrompt = prompt.getText().toString().trim();
        if (!initialPrompt.isEmpty()) intent.putExtra("com.mana.genesis.INITIAL_PROMPT", initialPrompt);
        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException error) {
            status.setText("Приложение MANA Genesis не установлено. Системная оболочка не содержит языковой модели. Установи companion-сборку, чтобы открыть ИИ и аватар.");
            Toast.makeText(this, "MANA Genesis companion не найден.", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
