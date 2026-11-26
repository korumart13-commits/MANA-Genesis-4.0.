package com.mana.launcher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.database.Cursor
import android.provider.OpenableColumns
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors

/** APK component installer embedded in the single MANA Genesis application. */
class ManaInstallerActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var selected: Uri? = null
    private var verifiedHash: String? = null
    private lateinit var fileLabel: TextView
    private lateinit var status: TextView
    private lateinit var expectedHash: EditText
    private lateinit var install: Button

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun label(value: String, size: Float, color: Int) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
    }
    private fun button(value: String) = Button(this).apply { text = value; isAllCaps = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(7, 20, 38)
        window.navigationBarColor = Color.rgb(7, 20, 38)
        val navy = Color.rgb(7, 20, 38)
        val cyan = Color.rgb(84, 200, 245)
        val white = Color.rgb(234, 244, 255)
        val muted = Color.rgb(145, 169, 197)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(28))
            setBackgroundColor(navy)
        }
        root.addView(label("MANA Installer", 28f, cyan))
        root.addView(label("Установка APK-компонентов внутри MANA Genesis 4.0", 14f, white))
        root.addView(label(
            "Телефон: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT}\nСборка: ${Build.DISPLAY}",
            13f, muted
        ).apply { setPadding(0, dp(16), 0, dp(18)) })
        root.addView(label("1. Выбери APK-файл", 17f, cyan))
        root.addView(button("Выбрать APK") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/vnd.android.package-archive"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            startActivityForResult(intent, REQUEST_PICK)
        })
        fileLabel = label("Файл пока не выбран", 13f, muted).apply { setPadding(0, dp(10), 0, dp(16)) }
        root.addView(fileLabel)
        root.addView(label("2. Сверь SHA-256", 17f, cyan))
        root.addView(label(
            "Вставь контрольную сумму из доверенного источника. Совпадение защищает от подмены файла, но не доказывает безопасность неизвестного издателя.",
            12f, muted
        ).apply { setPadding(0, dp(6), 0, dp(8)) })
        expectedHash = EditText(this).apply {
            hint = "64 шестнадцатеричных символа"
            setSingleLine(true); setTextColor(white); setHintTextColor(muted)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(cyan))
        }
        root.addView(expectedHash)
        root.addView(button("Проверить SHA-256") { verifyFile() })
        status = label("Ожидается выбор APK.", 13f, white).apply { setPadding(0, dp(14), 0, dp(10)) }
        root.addView(status)
        install = button("Передать APK установщику Android").apply { isEnabled = false }
        install.setOnClickListener { launchInstaller() }
        root.addView(install)
        root.addView(label(
            "3. Заверши установку в системном окне Android. Если потребуется, разреши MANA устанавливать приложения из этого источника. MANA не устанавливает APK скрытно, не получает root и не прошивает системные разделы.",
            12f, muted
        ).apply { setPadding(0, dp(18), 0, 0) })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun verifyFile() {
        val uri = selected
        if (uri == null) { status.text = "Сначала выбери APK."; return }
        val expected = expectedHash.text.toString().trim().replace(" ", "").lowercase(Locale.ROOT)
        if (!expected.matches(Regex("[0-9a-f]{64}"))) {
            status.text = "Нужна корректная SHA-256 сумма из 64 символов."; return
        }
        status.text = "Вычисляю SHA-256…"
        install.isEnabled = false
        verifiedHash = null
        worker.execute {
            val result = runCatching {
                val digest = MessageDigest.getInstance("SHA-256")
                contentResolver.openInputStream(uri)?.buffered()?.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                } ?: error("Не удалось открыть выбранный файл")
                digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
            }
            runOnUiThread {
                result.onSuccess { actual ->
                    if (actual == expected) {
                        verifiedHash = actual
                        status.text = "SHA-256 совпадает. Файл готов к передаче системному установщику."
                        install.isEnabled = true
                    } else {
                        status.text = "Контрольная сумма НЕ совпала. Установка заблокирована. Фактическая сумма: $actual"
                    }
                }.onFailure { status.text = "Ошибка проверки: ${it.message}" }
            }
        }
    }

    private fun launchInstaller() {
        val uri = selected
        if (uri == null || verifiedHash == null) {
            status.text = "Сначала выбери APK и успешно проверь SHA-256."; return
        }
        if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            status.text = "Разреши установку приложений из MANA в настройках Android, затем вернись сюда."
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }
        runCatching {
            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            startActivity(installIntent)
            status.text = "Открыт системный установщик Android. Подтверди установку в его окне."
        }.onFailure { status.text = "Не удалось открыть установщик: ${it.message}" }
    }

    private fun displayName(uri: Uri): String {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else "APK-файл"
            } else "APK-файл"
        } catch (_: Exception) { "APK-файл" } finally { cursor?.close() }
    }

    @Deprecated("Deprecated Android callback retained for broad API compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            selected = uri
            verifiedHash = null
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            fileLabel.text = "Выбран: ${displayName(uri)}"
            status.text = "Файл выбран. Теперь проверь SHA-256."
            install.isEnabled = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        worker.shutdownNow()
    }

    companion object { private const val REQUEST_PICK = 7412 }
}
