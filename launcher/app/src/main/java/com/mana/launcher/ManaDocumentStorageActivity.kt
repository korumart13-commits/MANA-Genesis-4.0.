package com.mana.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import com.mana.os.core.EncryptedFileManaDiaryStore
import com.mana.os.core.ManaDiaryEntry
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Executors

/**
 * SAF-scoped file review. MANA never silently deletes originals: quarantine is a
 * verified copy into MANA_Review; originals remain untouched until the owner acts.
 */
class ManaDocumentStorageActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var root: LinearLayout
    private lateinit var store: EncryptedFileManaDiaryStore
    private var selectedTree: Uri? = null
    private val candidates = linkedMapOf<String, DocumentFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = EncryptedFileManaDiaryStore(
            File(filesDir, "mana/storage-quarantine.bin"),
            ManaAndroidKeyProvider(),
            "mana.storage-quarantine.v1"
        )
        selectedTree = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_TREE, null)?.let(Uri::parse)
        buildUi()
        if (selectedTree != null) scan() else refresh()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(24))
            setBackgroundColor(0xFF071426.toInt())
        }
        root.addView(text("Файлы MANA", 24, 0xFFFFFFFF.toInt(), true))
        root.addView(text(
            "MANA анализирует только выбранную тобой папку. Важные документы, фотографии и APK не помечаются автоматически. Карантин создаёт проверенную копию и не удаляет оригинал.",
            13, 0xFF9CB6D4.toInt()
        ))
        root.addView(button("Выбрать папку для анализа") { chooseFolder() })
        root.addView(button("Повторить безопасное сканирование") { scan() })
        root.addView(text("Найденные кандидаты", 18, 0xFFFFFFFF.toInt(), true))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun chooseFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQUEST_TREE)
    }

    @Deprecated("SAF folder picker result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TREE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            contentResolver.takePersistableUriPermission(uri, flags)
            selectedTree = uri
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_TREE, uri.toString()).apply()
            scan()
        }
    }

    private fun scan() {
        val uri = selectedTree
        if (uri == null) {
            showMessage("Сначала выбери папку, к которой MANA получит доступ.")
            return
        }
        root.removeViews(4, (root.childCount - 4).coerceAtLeast(0))
        root.addView(text("Сканирование выбранной папки…", 13, 0xFF69C6FF.toInt()))
        executor.execute {
            val found = linkedMapOf<String, DocumentFile>()
            val tree = DocumentFile.fromTreeUri(this, uri)
            if (tree != null) walk(tree, found, 0)
            val alreadyQuarantined = synchronized(store) { store.readAll().filter { TAG in it.tags }.map { it.body }.toSet() }
            found.keys.removeAll(alreadyQuarantined)
            runOnUiThread {
                if (root.childCount > 4) root.removeViews(4, root.childCount - 4)
                candidates.clear()
                candidates.putAll(found)
                root.addView(text("Кандидатов: ${found.size}. Оригиналы не удаляются.", 13, 0xFF69C6FF.toInt()))
                found.forEach { (key, file) ->
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(10), dp(9), dp(10), dp(9))
                        setBackgroundColor(0xFF102B50.toInt())
                    }
                    card.addView(text(file.name ?: "Без имени", 14, 0xFFFFFFFF.toInt(), true))
                    card.addView(text("${formatBytes(file.length())} • ${reason(file)}", 11, 0xFF9CB6D4.toInt()))
                    card.addView(button("Скопировать в карантин MANA_Review") { quarantine(key) })
                    root.addView(card)
                }
                root.addView(text("Карантин MANA_Review", 18, 0xFFFFFFFF.toInt(), true))
                renderQuarantine()
            }
        }
    }

    private fun walk(folder: DocumentFile, found: MutableMap<String, DocumentFile>, depth: Int) {
        if (depth > 5 || found.size >= MAX_CANDIDATES) return
        folder.listFiles().forEach { child ->
            if (found.size >= MAX_CANDIDATES) return
            if (child.isDirectory) {
                if (child.name != QUARANTINE_DIR) walk(child, found, depth + 1)
            } else if (child.isFile && isCandidate(child)) {
                found[child.uri.toString()] = child
            }
        }
    }

    private fun isCandidate(file: DocumentFile): Boolean {
        val name = (file.name ?: "").lowercase()
        val extension = name.substringAfterLast('.', "")
        if (extension in PROTECTED_EXTENSIONS) return false
        val ageDays = if (file.lastModified() > 0L) (System.currentTimeMillis() - file.lastModified()) / DAY_MS else 0L
        return extension in TEMP_EXTENSIONS || (ageDays >= 365 && extension in OLD_LOG_EXTENSIONS)
    }

    private fun reason(file: DocumentFile): String {
        val name = (file.name ?: "").lowercase()
        val ext = name.substringAfterLast('.', "")
        return when {
            ext in TEMP_EXTENSIONS -> "временный/служебный файл"
            else -> "старый журнал, старше года"
        }
    }

    private fun quarantine(key: String) {
        val source = candidates[key] ?: return
        executor.execute {
            try {
                val tree = selectedTree?.let { DocumentFile.fromTreeUri(this, it) } ?: error("Папка недоступна.")
                val folder = tree.findFile(QUARANTINE_DIR) ?: tree.createDirectory(QUARANTINE_DIR)
                    ?: error("Не удалось создать карантин.")
                val originalName = source.name ?: "unnamed.bin"
                val targetName = uniqueName(folder, originalName)
                val target = folder.createFile("application/octet-stream", targetName) ?: error("Не удалось создать копию.")
                try {
                    val expectedHash = copyAndHash(source.uri, target.uri)
                    val targetHash = sha256(target.uri)
                    require(expectedHash == targetHash) { "Проверка копии не прошла." }
                    val entry = ManaDiaryEntry(
                        id = UUID.randomUUID().toString(),
                        title = originalName.take(200),
                        body = source.uri.toString(),
                        tags = setOf(
                            TAG,
                            "quarantine-uri:${target.uri}",
                            "sha256:$expectedHash",
                            "bytes:${source.length()}"
                        ),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                    synchronized(store) { store.writeAll((store.readAll() + entry).takeLast(MAX_CANDIDATES)) }
                    runOnUiThread {
                        showMessage("Копия проверена и помещена в карантин. Оригинал оставлен на месте.")
                        scan()
                    }
                } catch (error: Exception) {
                    target.delete()
                    throw error
                }
            } catch (error: Exception) {
                runOnUiThread { showMessage("Не удалось поместить файл в карантин: ${error.message}") }
            }
        }
    }

    private fun renderQuarantine() {
        val entries = synchronized(store) { store.readAll().filter { TAG in it.tags }.sortedByDescending { it.createdAt } }
        if (entries.isEmpty()) {
            root.addView(text("Карантин пуст.", 12, 0xFF9CB6D4.toInt()))
            return
        }
        entries.forEach { entry ->
            val uri = entry.tags.firstOrNull { it.startsWith("quarantine-uri:") }?.removePrefix("quarantine-uri:").orEmpty()
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(9), dp(10), dp(9))
                setBackgroundColor(0xFF102B50.toInt())
            }
            card.addView(text(entry.title, 14, 0xFFFFFFFF.toInt(), true))
            card.addView(text("Копия • ${entry.tags.firstOrNull { it.startsWith("bytes:") }?.removePrefix("bytes:") ?: "?"} байт", 11, 0xFF9CB6D4.toInt()))
            card.addView(button("Восстановить копию в выбранную папку") { restore(entry, uri) })
            card.addView(button("Удалить только копию из карантина") { confirmDeleteCopy(entry, uri) })
            root.addView(card)
        }
    }

    private fun restore(entry: ManaDiaryEntry, quarantineUri: String) {
        executor.execute {
            try {
                val source = DocumentFile.fromSingleUri(this, Uri.parse(quarantineUri)) ?: error("Копия недоступна.")
                val expected = entry.tags.firstOrNull { it.startsWith("sha256:") }?.removePrefix("sha256:") ?: error("Нет контрольной суммы.")
                require(sha256(source.uri) == expected) { "Копия карантина изменилась; восстановление остановлено." }
                val tree = selectedTree?.let { DocumentFile.fromTreeUri(this, it) } ?: error("Папка недоступна.")
                val name = uniqueName(tree, entry.title)
                val target = tree.createFile("application/octet-stream", name) ?: error("Не удалось создать файл восстановления.")
                try {
                    val copied = copyAndHash(source.uri, target.uri)
                    require(copied == expected && sha256(target.uri) == expected) { "Проверка восстановленной копии не прошла." }
                    runOnUiThread { showMessage("Файл восстановлен как $name. Копия в карантине сохранена.") }
                } catch (error: Exception) {
                    target.delete()
                    throw error
                }
            } catch (error: Exception) {
                runOnUiThread { showMessage("Восстановление не выполнено: ${error.message}") }
            }
        }
    }

    private fun confirmDeleteCopy(entry: ManaDiaryEntry, quarantineUri: String) {
        AlertDialog.Builder(this)
            .setTitle("Удалить копию из карантина?")
            .setMessage("Оригинал в исходной папке не затрагивается. Удаляется только проверенная копия MANA_Review.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить копию") { _, _ ->
                executor.execute {
                    val copy = DocumentFile.fromSingleUri(this, Uri.parse(quarantineUri))
                    val deleted = copy?.delete() == true
                    if (deleted) synchronized(store) {
                        store.writeAll(store.readAll().filterNot { it.id == entry.id })
                    }
                    runOnUiThread { showMessage(if (deleted) "Копия удалена; оригинал не тронут." else "Не удалось удалить копию.") ; scan() }
                }
            }.show()
    }

    private fun copyAndHash(source: Uri, target: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = contentResolver.openInputStream(source) ?: error("Не удалось прочитать источник.")
        val output = contentResolver.openOutputStream(target, "w") ?: error("Не удалось записать копию.")
        input.use { src -> output.use { dst ->
            val buffer = ByteArray(256 * 1024)
            var total = 0L
            while (true) {
                val count = src.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_FILE_BYTES) { "Файл превышает безопасный лимит 2 ГБ." }
                digest.update(buffer, 0, count)
                dst.write(buffer, 0, count)
            }
            dst.flush()
        } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        (contentResolver.openInputStream(uri) ?: error("Не удалось проверить файл.")).use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun uniqueName(folder: DocumentFile, original: String): String {
        val safe = original.map { ch -> if (ch == '/' || ch == '\\' || ch < ' ' || ch == '\u007f') '_' else ch }.joinToString("").take(120).ifBlank { "file.bin" }
        if (folder.findFile(safe) == null) return safe
        val base = safe.substringBeforeLast('.', safe)
        val ext = safe.substringAfterLast('.', "")
        return "${base}_${System.currentTimeMillis()}".let { if (ext.isBlank()) it else "$it.$ext" }
    }

    private fun refresh() {
        if (selectedTree == null) {
            root.addView(text("Папка ещё не выбрана.", 12, 0xFFFFC878.toInt()))
            return
        }
        renderQuarantine()
    }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFF123A66.toInt())
        gravity = Gravity.CENTER
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(44)).apply { topMargin = dp(7) }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun showMessage(value: String) = runOnUiThread {
        AlertDialog.Builder(this).setMessage(value).setPositiveButton("ОК", null).show()
    }

    private fun formatBytes(value: Long): String = when {
        value >= 1024L * 1024L -> "${value / (1024L * 1024L)} МБ"
        value >= 1024L -> "${value / 1024L} КБ"
        else -> "$value Б"
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_TREE = 5101
        private const val PREFS = "mana.storage"
        private const val KEY_TREE = "selected_tree_uri"
        private const val QUARANTINE_DIR = "MANA_Review"
        private const val TAG = "mana.storage.quarantine"
        private const val MAX_CANDIDATES = 500
        private const val MAX_FILE_BYTES = 2L * 1024L * 1024L * 1024L
        private const val DAY_MS = 86_400_000L
        private val TEMP_EXTENSIONS = setOf("tmp", "temp", "log", "old", "bak", "cache", "part", "crdownload")
        private val OLD_LOG_EXTENSIONS = setOf("log", "txt")
        private val PROTECTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "mp4", "mov", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "apk", "aab")
    }
}
