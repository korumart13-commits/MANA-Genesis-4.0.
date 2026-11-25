package com.mana.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class ManaHomeActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 20, 38);
    private static final int BLUE = Color.rgb(84, 200, 245);
    private LinearLayout appList;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        buildHome();
        loadApplications();
    }

    private void buildHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(NAVY);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(30), dp(22), dp(24));
        scroll.addView(root);

        TextView brand = new TextView(this);
        brand.setText("MANA");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(34);
        brand.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(brand);

        TextView subtitle = new TextView(this);
        subtitle.setText("Genesis 4.0  •  Домашний экран");
        subtitle.setTextColor(BLUE);
        subtitle.setTextSize(14);
        root.addView(subtitle);

        Button assistant = new Button(this);
        assistant.setText("Открыть MANA");
        assistant.setAllCaps(false);
        assistant.setOnClickListener(v -> openAssistant());
        root.addView(assistant, margins(dp(18)));

        TextView heading = new TextView(this);
        heading.setText("Приложения");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(20);
        heading.setPadding(0, dp(22), 0, dp(10));
        root.addView(heading);

        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        root.addView(appList);
        setContentView(scroll);
    }

    private void loadApplications() {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(query, 0);
        apps.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(
                String.valueOf(a.loadLabel(getPackageManager())),
                String.valueOf(b.loadLabel(getPackageManager()))));
        for (ResolveInfo info : apps) {
            if (info.activityInfo == null ||
                    getPackageName().equals(info.activityInfo.packageName)) continue;
            String label = String.valueOf(info.loadLabel(getPackageManager()));
            String packageName = info.activityInfo.packageName;
            Button item = new Button(this);
            item.setText(label);
            item.setAllCaps(false);
            item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            item.setOnClickListener(v -> launch(packageName));
            appList.addView(item, margins(dp(5)));
        }
    }

    private void openAssistant() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.mana.assistant");
        if (intent != null) startActivity(intent);
    }

    private void launch(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) startActivity(intent);
    }

    private LinearLayout.LayoutParams margins(int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = bottom;
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
