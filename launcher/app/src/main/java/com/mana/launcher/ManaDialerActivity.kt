package com.mana.launcher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.telecom.TelecomManager
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

/** Optional MANA phone UI; never becomes the default dialer without owner approval. */
class ManaDialerActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var numberInput: EditText
    private lateinit var callStatus: TextView
    private var pendingNumber: String? = null
    private var receiverRegistered = false
    private var muted = false

    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refreshCallControls() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (intent?.action == Intent.ACTION_DIAL) {
            numberInput.setText(intent.data?.schemeSpecificPart.orEmpty())
        }
        if (intent?.getBooleanExtra(EXTRA_INCOMING_CALL, false) == true) refreshCallControls()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_DIAL) numberInput.setText(intent.data?.schemeSpecificPart.orEmpty())
        refreshCallControls()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(ManaInCallService.ACTION_CALL_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(callReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(callReceiver, filter)
        receiverRegistered = true
        refreshCallControls()
    }

    override fun onPause() {
        if (receiverRegistered) runCatching { unregisterReceiver(callReceiver) }
        receiverRegistered = false
        super.onPause()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(24))
            setBackgroundColor(0xFF071426.toInt())
        }
        root.addView(text("MANA PHONE", 25, 0xFFFFFFFF.toInt(), true))
        root.addView(text("Автоответ после 10 секунд требует, чтобы владелец сам назначил MANA приложением телефона Android. Без этой роли звонки продолжает обслуживать текущий системный номеронабиратель.", 12, 0xFF9CB6D4.toInt()))
        root.addView(button("Назначить MANA приложением телефона") { requestDialerRole() })

        val autoAnswer = CheckBox(this).apply {
            text = "Включить автоответ через 10 секунд"
            setTextColor(0xFFFFFFFF.toInt())
            isChecked = getSharedPreferences(ManaInCallService.PREFS, MODE_PRIVATE)
                .getBoolean(ManaInCallService.KEY_AUTO_ANSWER, false)
            setOnCheckedChangeListener { _, checked ->
                val roleHeld = Build.VERSION.SDK_INT >= 29 &&
                    getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)
                if (checked && !roleHeld) {
                    isChecked = false
                    showMessage("Сначала нужно явно назначить MANA приложением телефона.")
                } else {
                    getSharedPreferences(ManaInCallService.PREFS, MODE_PRIVATE).edit()
                        .putBoolean(ManaInCallService.KEY_AUTO_ANSWER, checked).apply()
                }
            }
        }
        root.addView(autoAnswer)
        root.addView(text(
            "Важно: текущий прототип умеет принять звонок, но Android не предоставляет обычному приложению доступ к двустороннему аудиопотоку PSTN. Поэтому MANA пока не может разговаривать с абонентом или делать расшифровку. Не включай автоответ для повседневного использования, пока голосовой тракт не будет отдельно подтверждён.",
            11, 0xFFFFC98A.toInt()
        ))

        numberInput = EditText(this).apply {
            hint = "Номер телефона"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9CB6D4.toInt())
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            isSingleLine = true
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        root.addView(numberInput, LinearLayout.LayoutParams(-1, dp(50)))
        val keypad = GridLayout(this).apply { columnCount = 3 }
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#").forEach { digit ->
            val key = Button(this).apply {
                text = digit
                textSize = 20f
                setOnClickListener {
                    val at = numberInput.selectionStart.coerceAtLeast(0)
                    numberInput.text.insert(at, digit)
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(48)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            keypad.addView(key, params)
        }
        root.addView(keypad, LinearLayout.LayoutParams(-1, -2))
        root.addView(button("Позвонить") { placeCall() })
        root.addView(button("Показать историю вызовов") { showCallHistory() })

        callStatus = text("Нет активного звонка.", 14, 0xFF69C6FF.toInt(), true)
        root.addView(callStatus)
        root.addView(button("Ответить") { ManaInCallService.answerCurrentCall(); refreshCallControls() })
        root.addView(button("Отклонить входящий") { ManaInCallService.rejectCurrentCall(); refreshCallControls() })
        root.addView(button("Завершить звонок") { ManaInCallService.endCurrentCall(); refreshCallControls() })
        root.addView(button("Удержать / продолжить звонок") { ManaInCallService.holdCurrentCall(); refreshCallControls() })
        root.addView(button("Громкая связь") { ManaInCallService.setSpeaker(true) })
        root.addView(button("Динамик телефона") { ManaInCallService.setSpeaker(false) })
        root.addView(button("Выключить / включить микрофон") {
            muted = !muted
            ManaInCallService.setMuted(muted)
            showMessage(if (muted) "Микрофон выключен." else "Микрофон включён.")
        })
        root.addView(button("Обновить состояние звонка") { refreshCallControls() })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun requestDialerRole() {
        if (Build.VERSION.SDK_INT < 29) {
            showMessage("Роль приложения телефона требует Android 10 или новее.")
            return
        }
        val roles = getSystemService(RoleManager::class.java)
        if (!roles.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            showMessage("Роль приложения телефона недоступна на этой прошивке.")
            return
        }
        if (roles.isRoleHeld(RoleManager.ROLE_DIALER)) {
            showMessage("MANA уже назначена приложением телефона.")
            return
        }
        startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_DIALER), REQUEST_DIALER_ROLE)
    }

    private fun placeCall() {
        val number = numberInput.text.toString().trim()
        if (number.isBlank()) { showMessage("Введи номер телефона."); return }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pendingNumber = number
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
            return
        }
        try {
            val telecom = getSystemService(TelecomManager::class.java)
            telecom.placeCall(Uri.fromParts("tel", number, null), Bundle())
        } catch (error: Exception) {
            showMessage("Не удалось начать звонок: " + error.message)
        }
    }

    private fun showCallHistory() {
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), REQUEST_CALL_LOG)
            return
        }
        try {
            val rows = mutableListOf<String>()
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE, CallLog.Calls.DURATION),
                null, null, CallLog.Calls.DATE + " DESC"
            )?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count++ < 20) {
                    val number = cursor.getString(0).orEmpty()
                    val date = DateFormat.getDateTimeInstance().format(Date(cursor.getLong(1)))
                    val type = when (cursor.getInt(2)) {
                        CallLog.Calls.INCOMING_TYPE -> "Входящий"
                        CallLog.Calls.OUTGOING_TYPE -> "Исходящий"
                        CallLog.Calls.MISSED_TYPE -> "Пропущенный"
                        else -> "Вызов"
                    }
                    rows += "$type • $number • $date • ${cursor.getLong(3)} сек."
                }
            }
            AlertDialog.Builder(this).setTitle("Последние вызовы")
                .setMessage(rows.takeIf { it.isNotEmpty() }?.joinToString("\n") ?: "История вызовов пуста.")
                .setPositiveButton("Закрыть", null).show()
        } catch (error: Exception) {
            showMessage("Не удалось прочитать историю вызовов: " + error.message)
        }
    }

    private fun refreshCallControls() {
        val calls = ManaInCallService.activeCalls()
        callStatus.text = if (calls.isEmpty()) "Нет активного звонка." else calls.joinToString("\n") { call ->
            val number = call.details.handle?.schemeSpecificPart ?: "номер скрыт"
            val state = when (call.state) {
                android.telecom.Call.STATE_RINGING -> "Входящий"
                android.telecom.Call.STATE_DIALING -> "Набор"
                android.telecom.Call.STATE_ACTIVE -> "Активен"
                android.telecom.Call.STATE_HOLDING -> "Удерживается"
                android.telecom.Call.STATE_DISCONNECTED -> "Завершён"
                else -> "Вызов"
            }
            "$state: $number"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DIALER_ROLE) {
            showMessage(if (resultCode == RESULT_OK) "MANA назначена приложением телефона. Теперь можно включить автоответ через 10 секунд." else "Роль телефона не изменена.")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CALL_PHONE && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            pendingNumber?.let { numberInput.setText(it); pendingNumber = null; placeCall() }
        }
        if (requestCode == REQUEST_CALL_LOG && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) showCallHistory()
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun showMessage(value: String) =
        android.widget.Toast.makeText(this, value, android.widget.Toast.LENGTH_LONG).show()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_INCOMING_CALL = "mana.incoming.call"
        const val EXTRA_SHOW_DIALPAD = "mana.show.dialpad"
        private const val REQUEST_DIALER_ROLE = 7401
        private const val REQUEST_CALL_PHONE = 7402
        private const val REQUEST_CALL_LOG = 7403
    }
}
