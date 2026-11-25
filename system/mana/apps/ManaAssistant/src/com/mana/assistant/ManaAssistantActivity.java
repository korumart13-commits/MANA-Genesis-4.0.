package com.mana.assistant;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class ManaAssistantActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 20, 38);
    private static final int BLUE = Color.rgb(84, 200, 245);
    private TextView status;
    private EditText prompt;

    @Override
    public void onCreate(Bundle state) {
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
        title.setTextColor(Color.WHITE);
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView identity = new TextView(this);
        identity.setText("Genesis 4.0  •  Единый системный помощник");
        identity.setTextColor(BLUE);
        identity.setTextSize(14);
        root.addView(identity);

        status = new TextView(this);
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(15);
        status.setPadding(0, dp(22), 0, dp(18));
        status.setText("Интерфейс MANA добавлен в исходники ОС. Движок LiteRT-LM ещё не подключён; ответы ИИ пока недоступны.");
        root.addView(status);

        prompt = new EditText(this);
        prompt.setTextColor(Color.WHITE);
        prompt.setHintTextColor(Color.GRAY);
        prompt.setHint("Напиши сообщение Мане");
        prompt.setMinLines(2);
        prompt.setGravity(48);
        root.addView(prompt);

        Button send = new Button(this);
        send.setText("Отправить");
        send.setAllCaps(false);
        send.setOnClickListener(this::onSend);
        root.addView(send);
        setContentView(scroll);
    }

    private void onSend(View ignored) {
        String text = prompt.getText().toString().trim();
        if (text.isEmpty()) {
            status.setText("Сначала введи сообщение.");
            return;
        }
        status.setText("Сообщение принято интерфейсом. Сервис вывода ещё не подключён; запрос не отправлялся в сеть и не обработан моделью.");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
