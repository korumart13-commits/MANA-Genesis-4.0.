package com.mana.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.mana.brain.BrainRequest;
import com.mana.brain.BrainResponse;
import com.mana.brain.IBrainCallback;
import com.mana.brain.IBrainService;

/**
 * MANA HOME — Android HOME role implementation.
 *
 * This is a user-space launcher, not a replacement for privileged SystemUI,
 * framework services, bootloader, kernel or the Android operating system.
 */
public final class ManaHomeActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 20, 38);
    private static final int SURFACE = Color.rgb(16, 43, 80);
    private static final int BLUE = Color.rgb(84, 200, 245);
    private static final int TEXT = Color.rgb(234, 244, 255);
    private static final int MUTED = Color.rgb(145, 169, 197);
    private static final int COLUMNS = 4;

    private final List<AppEntry> allApps = new ArrayList<>();
    private GridLayout appGrid;
    private EditText searchBox;
    private TextView clock;
    private TextView date;
    private TextView emptyState;
    private TextView brainStatus;
    private EditText brainPrompt;
    private IBrainService brainService;
    private boolean brainBound;

    private static final class AppEntry {
        final String label;
        final String packageName;
        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        getWindow().getDecorView().setSystemUiVisibility(0);
        collectApplications();
        buildHome();
        renderApplications("");
        updateDateTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appGrid != null) {
            collectApplications();
            renderApplications(searchBox == null ? "" : searchBox.getText().toString());
            updateDateTime();
        }
    }

    private void collectApplications() {
        allApps.clear();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> resolved = pm.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null) continue;
            String packageName = info.activityInfo.packageName;
            if (getPackageName().equals(packageName)) continue;
            String label = String.valueOf(info.loadLabel(pm));
            allApps.add(new AppEntry(label, packageName));
        }
        Collections.sort(allApps, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private void buildHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(NAVY);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView brand = text("MANA", 30, TEXT, true);
        brandRow.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView edition = text("GENESIS 4.0", 11, BLUE, true);
        brandRow.addView(edition);
        root.addView(brandRow);

        clock = text("", 42, TEXT, true);
        clock.setPadding(0, dp(22), 0, 0);
        root.addView(clock);
        date = text("", 14, MUTED, false);
        root.addView(date);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(18), dp(16), dp(18), dp(16));
        hero.setBackground(panelBackground(BLUE, 22));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroParams.topMargin = dp(22);
        root.addView(hero, heroParams);

        hero.addView(text("ТВОЯ MANA", 12, BLUE, true));
        TextView welcome = text("Я рядом. Чем займёмся?", 20, TEXT, true);
        welcome.setPadding(0, dp(7), 0, dp(12));
        hero.addView(welcome);

        TextView talk = action("Открыть MANA  →", true);
        talk.setOnClickListener(v -> openAssistant());
        hero.addView(talk);

        TextView section = text("Приложения", 20, TEXT, true);
        section.setPadding(0, dp(26), 0, dp(12));
        root.addView(section);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextColor(TEXT);
        searchBox.setHintTextColor(MUTED);
        searchBox.setHint("Поиск приложений");
        searchBox.setTextSize(15);
        searchBox.setPadding(dp(16), dp(11), dp(16), dp(11));
        searchBox.setBackground(panelBackground(MUTED, 16));
        root.addView(searchBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        appGrid = new GridLayout(this);
        appGrid.setColumnCount(COLUMNS);
        appGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = dp(14);
        root.addView(appGrid, gridParams);

        emptyState = text("Приложения не найдены", 14, MUTED, false);
        emptyState.setGravity(Gravity.CENTER);
        emptyState.setPadding(0, dp(24), 0, dp(24));
        root.addView(emptyState);
        setContentView(scroll);

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderApplications(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void renderApplications(String filter) {
        if (appGrid == null) return;
        appGrid.removeAllViews();
        String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        int count = 0;
        for (AppEntry app : allApps) {
            if (!app.label.toLowerCase(Locale.ROOT).contains(needle)) continue;
            appGrid.addView(appTile(app), tileParams());
            count++;
        }
        emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
    }

    private View appTile(AppEntry app) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(4), dp(12), dp(4), dp(12));
        tile.setBackground(panelBackground(MUTED, 16));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setContentDescription(app.label);
        tile.setOnClickListener(v -> launch(app.packageName));

        TextView glyph = text(firstGlyph(app.label), 23, BLUE, true);
        glyph.setGravity(Gravity.CENTER);
        tile.addView(glyph, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        TextView label = text(app.label, 11, TEXT, false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        tile.addView(label, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));
        return tile;
    }

    private GridLayout.LayoutParams tileParams() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dp(86);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        return params;
    }

    private String firstGlyph(String label) {
        if (label == null || label.trim().isEmpty()) return "◈";
        return label.trim().substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private void openAssistant() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.mana.assistant");
        if (intent == null) {
            Toast.makeText(this, "Приложение MANA Assistant пока не установлено", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    private void launch(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) startActivity(intent);
    }

    private void updateDateTime() {
        Date now = new Date();
        clock.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(now));
        date.setText(DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(now));
    }

    private TextView action(String value, boolean primary) {
        TextView view = text(value, 15, primary ? NAVY : TEXT, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(14), dp(12), dp(14), dp(12));
        view.setBackground(panelBackground(primary ? BLUE : MUTED, 14));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable panelBackground(int strokeColor, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(SURFACE);
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), Color.argb(80, Color.red(strokeColor),
                Color.green(strokeColor), Color.blue(strokeColor)));
        return shape;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
