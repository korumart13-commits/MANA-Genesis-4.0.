package com.mana.update;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.database.Cursor;
import android.content.ContentResolver;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.OpenableColumns;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
  private static final int PICK_APK = 41;
  private static final int ALLOW_UNKNOWN = 42;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private Uri selectedApk;
  private String verifiedSha256;
  private TextView deviceInfo, fileInfo, status;
  private EditText expectedHash;
  private Button installButton;

  private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
  private TextView text(String value, int size, int color) {
    TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t;
  }
  private Button button(String value) {
    Button b = new Button(this); b.setText(value); b.setAllCaps(false); return b;
  }
  private void gap(LinearLayout root, int h) {
    View v = new View(this); root.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
  }
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(7,20,38));
    getWindow().setNavigationBarColor(Color.rgb(7,20,38));
    LinearLayout page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(22), dp(28), dp(22), dp(28));
    page.setBackgroundColor(Color.rgb(7,20,38));
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.addView(text("MANA", 34, Color.rgb(84,200,245)));
    root.addView(text("Установщик компонентов", 19, Color.WHITE));
    gap(root, 18);
    root.addView(text("1. Проверка устройства", 17, Color.rgb(84,200,245)));
    deviceInfo = text("", 14, Color.rgb(234,244,255));
    deviceInfo.setPadding(0, dp(8), 0, dp(8));
    root.addView(deviceInfo);
    gap(root, 10);
    root.addView(text("2. Выбор APK-компонента", 17, Color.rgb(84,200,245)));
    Button choose = button("Выбрать APK-файл");
    root.addView(choose);
    choose.setOnClickListener(v -> {
      Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      i.addCategory(Intent.CATEGORY_OPENABLE);
      i.setType("application/vnd.android.package-archive");
      startActivityForResult(i, PICK_APK);
    });
    fileInfo = text("Файл не выбран", 13, Color.rgb(145,169,197));
    fileInfo.setPadding(0, dp(8), 0, dp(8));
    root.addView(fileInfo);
    gap(root, 8);
    root.addView(text("3. Контрольная сумма SHA-256", 17, Color.rgb(84,200,245)));
    root.addView(text("Вставь SHA-256 из доверенного источника, откуда получен APK. Без совпадения файл не будет передан установщику Android.", 13, Color.rgb(145,169,197)));
    expectedHash = new EditText(this);
    expectedHash.setSingleLine(true);
    expectedHash.setTextColor(Color.WHITE);
    expectedHash.setHintTextColor(Color.GRAY);
    expectedHash.setHint("64 символа SHA-256");
    expectedHash.setTextSize(13);
    expectedHash.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(84,200,245)));
    root.addView(expectedHash, new LinearLayout.LayoutParams(-1, dp(54)));
    Button verify = button("Проверить файл");
    root.addView(verify);
    verify.setOnClickListener(v -> verifySelectedFile());
    gap(root, 10);
    status = text("Ожидание выбора файла.", 14, Color.WHITE);
    root.addView(status);
    installButton = button("Установить проверенный компонент");
    installButton.setEnabled(false);
    root.addView(installButton);
    installButton.setOnClickListener(v -> launchSystemInstaller());
    gap(root, 18);
    root.addView(text("4. Завершение", 17, Color.rgb(84,200,245)));
    root.addView(text("Android покажет системное окно установки. Подтверди установку там. MANA не обходит защиту Android и не меняет системные разделы. Этот инструмент устанавливает APK-приложения; прошивку или OTA он не прошивает.", 13, Color.rgb(145,169,197)));
    page.addView(root);
    scroll.addView(page);
    setContentView(scroll);
    deviceInfo.setText("Производитель: " + Build.MANUFACTURER +
      "\nБренд: " + Build.BRAND +
      "\nМодель: " + Build.MODEL +
      "\nКод устройства: " + Build.DEVICE +
      "\nAndroid: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")" +
      "\nСборка: " + Build.DISPLAY);
  }

  private void verifySelectedFile() {
    if (selectedApk == null) { status.setText("Сначала выбери APK-файл."); return; }
    String expected = expectedHash.getText().toString().trim().replace(" ", "").toLowerCase(Locale.ROOT);
    if (!expected.matches("[0-9a-f]{64}")) { status.setText("Введи корректную SHA-256 сумму из 64 шестнадцатеричных символов."); return; }
    status.setText("Проверяю SHA-256…");
    installButton.setEnabled(false);
    final Uri file = selectedApk;
    worker.execute(() -> {
      String actual = "";
      String error = null;
      try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = getContentResolver().openInputStream(file)) {
          if (in == null) throw new IllegalStateException("Не удалось открыть файл");
          byte[] buffer = new byte[64 * 1024];
          int n;
          while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        actual = out.toString();
      } catch (Exception e) { error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
      final String result = actual;
      final String failure = error;
      runOnUiThread(() -> {
        if (failure != null) { status.setText("Ошибка проверки: " + failure); return; }
        if (!result.equals(expected)) {
          verifiedSha256 = null;
          status.setText("НЕ СОВПАЛО. APK не будет установлен. Фактическая SHA-256: " + result);
          return;
        }
        verifiedSha256 = result;
        status.setText("Проверка успешна. SHA-256 совпадает. Можно передать APK системному установщику.");
        installButton.setEnabled(true);
      });
    });
  }

  private void launchSystemInstaller() {
    if (selectedApk == null || verifiedSha256 == null) {
      status.setText("Сначала выбери файл и успешно проверь SHA-256."); return;
    }
    if (!getPackageManager().canRequestPackageInstalls()) {
      status.setText("Разреши установку приложений из этого источника в настройках Android, затем вернись и нажми кнопку установки.");
      Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
        Uri.parse("package:" + getPackageName()));
      startActivityForResult(settings, ALLOW_UNKNOWN);
      return;
    }
    try {
      Intent install = new Intent(Intent.ACTION_VIEW);
      install.setDataAndType(selectedApk, "application/vnd.android.package-archive");
      install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(install);
      status.setText("Открыт системный установщик Android. Заверши установку в его окне.");
    } catch (Exception e) {
      status.setText("Не удалось открыть установщик: " + e.getMessage());
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PICK_APK && resultCode == RESULT_OK && data != null && data.getData() != null) {
      selectedApk = data.getData();
      verifiedSha256 = null;
      String name = queryName(selectedApk);
      fileInfo.setText("Выбран: " + name + "\nПовторно проверь SHA-256 перед установкой.");
      status.setText("Файл выбран. Ожидается проверка SHA-256.");
      installButton.setEnabled(false);
    } else if (requestCode == ALLOW_UNKNOWN) {
      status.setText(getPackageManager().canRequestPackageInstalls()
        ? "Разрешение получено. Нажми «Установить проверенный компонент» ещё раз."
        : "Разрешение не предоставлено. Установка не запускалась.");
    }
  }

  private String queryName(Uri uri) {
    try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
      if (c != null && c.moveToFirst()) {
        int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        if (i >= 0) return c.getString(i);
      }
    } catch (Exception ignored) {}
    return uri.getLastPathSegment() == null ? "APK-файл" : uri.getLastPathSegment();
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    worker.shutdownNow();
  }
}
