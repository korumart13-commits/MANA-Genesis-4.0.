package com.mana.launcher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.mana.os.core.EncryptedFileManaDiaryStore
import com.mana.os.core.EncryptedFileManaFutureUpdateStore
import com.mana.os.core.ManaFutureUpdateService
import com.mana.os.core.ManaUpdateIdea
import com.mana.os.core.ManaUpdateStage
import java.io.File
import java.util.Locale

/** Encrypted owner idea inbox; it records workflow state but does not build or install an OS update. */
class ManaFutureUpdatesActivity : Activity() {
    private lateinit var service: ManaFutureUpdateService
    private lateinit var list: LinearLayout
    private lateinit var ideaInput: EditText
    private var initializationError: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            service = ManaFutureUpdateService(
                EncryptedFileManaFutureUpdateStore(
                    EncryptedFileManaDiaryStore(
                        File(filesDir, "mana/future-updates.bin"),
                        ManaAndroidKeyProvider(),
                        "mana.future.v1"
                    )
                )
            )
        } catch (error: Exception) {
            initializationError = error.message
        }

        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF071426.toInt()) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(28))
        }
        scroll.addView(root)
        root.addView(text("MANA Future Updates", 24, 0xFFFFFFFF.toInt(), true))
        root.addView(text(
            "Идеи хранятся локально в AES-GCM зашифрованном файле. Голосовой ввод сохраняет только распознанный текст, не аудиозапись. Этот экран ведёт журнал этапов, но сам не собирает и не устанавливает обновления.",
            12, 0xFF9CB6D4.toInt(), false
        ))
        if (initializationError != null) {
            root.addView(text("Хранилище недоступно: $initializationError", 14, 0xFFFFA6A6.toInt(), false))
            setContentView(scroll)
            return
        }

        ideaInput = EditText(this).apply {
            hint = "Опиши идею будущего обновления MANA"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9CB6D4.toInt())
            minLines = 3
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        root.addView(ideaInput, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })
        root.addView(button("Записать идею голосом") { startVoiceCapture() })
        root.addView(button("Сохранить идею") { saveIdea() })
        root.addView(text("Сохранённые идеи", 18, 0xFFFFFFFF.toInt(), true).apply {
            setPadding(0, dp(20), 0, dp(8))
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        setContentView(scroll)
        refresh()
    }

    private fun startVoiceCapture() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи, какую функцию добавить в MANA")
        }
        try { startActivityForResult(intent, REQUEST_SPEECH) }
        catch (_: Exception) { showMessage("На устройстве нет доступного распознавателя речи.") }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startVoiceCapture()
            else showMessage("Микрофон не разрешён.")
        }
    }

    @Deprecated("Speech recognizer activity result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) ideaInput.setText(spoken)
        }
    }

    private fun saveIdea() {
        val description = ideaInput.text.toString().trim()
        if (description.isBlank()) {
            showMessage("Сначала напиши или надиктуй идею.")
            return
        }
        try {
            service.capture(description)
            ideaInput.setText("")
            refresh()
            showMessage("Идея сохранена в зашифрованный список.")
        } catch (error: Exception) {
            showMessage("Не удалось сохранить идею: ${error.message}")
        }
    }

    private fun refresh() {
        list.removeAllViews()
        val ideas = service.list().sortedByDescending { it.updatedAt }
        if (ideas.isEmpty()) {
            list.addView(text("Пока нет сохранённых идей.", 13, 0xFF9CB6D4.toInt(), false))
            return
        }
        ideas.forEach { idea ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(0xFF102B50.toInt())
            }
            card.addView(text(idea.description, 15, 0xFFFFFFFF.toInt(), true))
            card.addView(text("Этап: ${stageLabel(idea.stage)}", 12, 0xFF69C6FF.toInt(), false))
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val next = nextStage(idea.stage)
            if (next != null) actions.addView(button("Далее") { advance(idea, next) })
            if (idea.stage != ManaUpdateStage.INSTALLED && idea.stage != ManaUpdateStage.REJECTED) {
                actions.addView(button("Отклонить") { confirmReject(idea) })
            }
            actions.addView(button("Удалить") { confirmDelete(idea) })
            card.addView(actions)
            val params = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
            list.addView(card, params)
        }
    }

    private fun advance(idea: ManaUpdateIdea, next: ManaUpdateStage) {
        val action = {
            try {
                service.advance(idea.id, next, ownerApproved = next == ManaUpdateStage.OWNER_APPROVED)
                refresh()
            } catch (error: Exception) { showMessage("Этап не изменён: ${error.message}") }
        }
        if (next == ManaUpdateStage.OWNER_APPROVED) {
            AlertDialog.Builder(this)
                .setTitle("Подтвердить реализацию идеи?")
                .setMessage("Это подтверждает только твоё решение включить идею в план. MANA ещё не собрала и не установила обновление.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Подтвердить", { _, _ -> action() })
                .show()
        } else action()
    }

    private fun confirmReject(idea: ManaUpdateIdea) {
        AlertDialog.Builder(this)
            .setTitle("Отклонить идею?")
            .setMessage(idea.description)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Отклонить") { _, _ ->
                runCatching { service.advance(idea.id, ManaUpdateStage.REJECTED, ownerApproved = true) }
                    .onSuccess { refresh() }
                    .onFailure { showMessage(it.message ?: "Не удалось отклонить идею.") }
            }.show()
    }

    private fun confirmDelete(idea: ManaUpdateIdea) {
        AlertDialog.Builder(this)
            .setTitle("Удалить идею?")
            .setMessage("Запись будет удалена из локального зашифрованного хранилища.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ ->
                runCatching { service.delete(idea.id, ownerApproved = true) }
                    .onSuccess { refresh() }
                    .onFailure { showMessage(it.message ?: "Не удалось удалить идею.") }
            }.show()
    }

    private fun nextStage(stage: ManaUpdateStage): ManaUpdateStage? = when (stage) {
        ManaUpdateStage.IDEA -> ManaUpdateStage.PLANNED
        ManaUpdateStage.PLANNED -> ManaUpdateStage.BUILDING
        ManaUpdateStage.BUILDING -> ManaUpdateStage.TESTED
        ManaUpdateStage.TESTED -> ManaUpdateStage.OWNER_APPROVED
        ManaUpdateStage.OWNER_APPROVED, ManaUpdateStage.INSTALLED, ManaUpdateStage.REJECTED -> null
    }

    private fun stageLabel(stage: ManaUpdateStage): String = when (stage) {
        ManaUpdateStage.IDEA -> "Идея"
        ManaUpdateStage.PLANNED -> "Запланировано"
        ManaUpdateStage.BUILDING -> "В разработке (отметка владельца)"
        ManaUpdateStage.TESTED -> "Тестирование отмечено владельцем"
        ManaUpdateStage.OWNER_APPROVED -> "Одобрено владельцем"
        ManaUpdateStage.INSTALLED -> "Установлено"
        ManaUpdateStage.REJECTED -> "Отклонено"
    }

    private fun button(title: String, action: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun showMessage(value: String) =
        android.widget.Toast.makeText(this, value, android.widget.Toast.LENGTH_LONG).show()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_AUDIO = 7101
        private const val REQUEST_SPEECH = 7102
    }
}
