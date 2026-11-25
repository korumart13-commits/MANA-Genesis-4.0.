package com.mana.launcher

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.mana.os.core.ManaActionRequest
import com.mana.os.core.ManaAutonomyController

/**
 * Owner-facing capability and action screen. No capability is enabled by default.
 * Android permissions/special access are requested only after the owner turns on
 * the corresponding switch. Every action is sent through the typed MANA policy layer.
 */
class ManaDeviceControlsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("mana.device-controls", MODE_PRIVATE) }
    private lateinit var adapter: ManaAndroidActionAdapter
    private var controller: ManaAutonomyController? = null
    private lateinit var status: TextView
    private var pendingCapability: String? = null
    private val capabilitySwitches = mutableMapOf<String, Switch>()
    private val cameraRequest = 7201
    private var incomingOwnerActionHandled = false

    private val capabilities = listOf(
        Capability("device.apps.launch", "Запуск приложений", "Разрешает MANA открывать выбранные приложения."),
        Capability("device.torch", "Фонарик", "Требуется системное разрешение Камера."),
        Capability("device.display.brightness", "Яркость экрана", "Требуется специальный доступ «Изменение системных настроек»."),
        Capability("device.audio.volume", "Громкость", "Управление громкостью мультимедиа, звонка и будильника."),
        Capability("device.notifications.dnd", "Не беспокоить", "Требуется доступ к политике уведомлений; включение режима подтверждается отдельно.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ManaAndroidActionAdapter(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (pendingCapability != "device.torch") reconcilePendingCapability()
        reconcileRevokedCapabilities()
        refreshController()
        if (::status.isInitialized) status.text = "Активные разрешения MANA: ${enabledResources().size} из ${capabilities.size}"
        handleIncomingOwnerAction()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 20)
            setBackgroundColor(android.graphics.Color.rgb(4, 12, 25))
        }
        root.addView(TextView(this).apply {
            text = "УПРАВЛЕНИЕ УСТРОЙСТВОМ"
            textSize = 22f
            setTextColor(android.graphics.Color.rgb(230, 243, 255))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Каждую возможность ты включаешь отдельно. MANA не получает root, shell или доступ к прошивке."
            textSize = 14f
            setTextColor(android.graphics.Color.rgb(155, 183, 211))
            setPadding(0, 10, 0, 18)
        })
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        capabilities.forEach { capability ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 12, 14, 12)
                setBackgroundColor(android.graphics.Color.rgb(12, 31, 55))
            }
            val toggle = Switch(this).apply {
                text = capability.title
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                isChecked = prefs.getBoolean(capability.resource, false)
                setOnCheckedChangeListener { _, checked ->
                    if (checked) requestCapability(capability.resource)
                    else {
                        prefs.edit().putBoolean(capability.resource, false).apply()
                        refreshController()
                        status.text = "Возможность отключена: ${capability.title}"
                    }
                }
            }
            capabilitySwitches[capability.resource] = toggle
            row.addView(toggle)
            row.addView(TextView(this).apply {
                text = capability.description
                textSize = 12f
                setTextColor(android.graphics.Color.rgb(155, 183, 211))
                setPadding(0, 5, 0, 0)
            })
            val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
            body.addView(row, lp)
        }

        body.addView(TextView(this).apply {
            text = "КОМАНДЫ УСТРОЙСТВУ"
            textSize = 13f
            setTextColor(android.graphics.Color.rgb(105, 198, 255))
            setPadding(0, 18, 0, 8)
        })
        val packageInput = field("Имя пакета приложения, например com.android.settings")
        body.addView(packageInput)
        body.addView(actionButton("Открыть приложение") {
            dispatch("launch_app", mapOf("package" to packageInput.text.toString().trim()))
        })
        body.addView(actionButton("Включить фонарик") { dispatch("set_torch", mapOf("enabled" to "true")) })
        body.addView(actionButton("Выключить фонарик") { dispatch("set_torch", mapOf("enabled" to "false")) })

        val brightnessInput = field("Яркость: число от 0 до 255").apply { setText("128") }
        body.addView(brightnessInput)
        body.addView(actionButton("Применить яркость") {
            dispatch("set_brightness", mapOf("level" to brightnessInput.text.toString().trim()))
        })

        val volumeInput = field("Громкость мультимедиа: 0–максимум").apply { setText("5") }
        body.addView(volumeInput)
        body.addView(actionButton("Применить громкость") {
            dispatch("set_volume", mapOf("stream" to "music", "level" to volumeInput.text.toString().trim()))
        })
        body.addView(actionButton("Включить «Не беспокоить»") {
            AlertDialog.Builder(this)
                .setTitle("Подтверди действие")
                .setMessage("MANA включит режим «Не беспокоить». Продолжить?")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Включить") { _, _ ->
                    dispatch("set_do_not_disturb", mapOf("enabled" to "true"), ownerConfirmed = true)
                }.show()
        })
        body.addView(actionButton("Выключить «Не беспокоить»") {
            dispatch("set_do_not_disturb", mapOf("enabled" to "false"), ownerConfirmed = true)
        })

        status = TextView(this).apply {
            textSize = 13f
            setTextColor(android.graphics.Color.rgb(170, 210, 245))
            setPadding(0, 18, 0, 12)
        }
        body.addView(status)
        root.addView(ScrollView(this).apply { addView(body) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(actionButton("Закрыть") { finish() })
        setContentView(root)
        status.text = "Активные разрешения MANA: ${enabledResources().size} из ${capabilities.size}"
    }

    private fun field(hintText: String) = EditText(this).apply {
        hint = hintText
        setSingleLine(true)
        setTextColor(android.graphics.Color.WHITE)
        setHintTextColor(android.graphics.Color.LTGRAY)
        inputType = android.text.InputType.TYPE_CLASS_TEXT
        setPadding(12, 8, 12, 8)
    }

    private fun actionButton(title: String, run: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        setOnClickListener { run() }
    }

    private fun requestCapability(resource: String) {
        pendingCapability = resource
        when (resource) {
            "device.torch" -> {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    enablePendingCapability()
                } else requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraRequest)
            }
            "device.display.brightness" -> {
                if (Settings.System.canWrite(this)) enablePendingCapability()
                else startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            }
            "device.notifications.dnd" -> {
                val manager = getSystemService(NotificationManager::class.java)
                if (manager.isNotificationPolicyAccessGranted) enablePendingCapability()
                else startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
            else -> enablePendingCapability()
        }
    }

    private fun reconcilePendingCapability() {
        val resource = pendingCapability ?: return
        val granted = when (resource) {
            "device.torch" -> checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            "device.display.brightness" -> Settings.System.canWrite(this)
            "device.notifications.dnd" -> getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
            else -> true
        }
        if (granted) enablePendingCapability()
        else {
            prefs.edit().putBoolean(resource, false).apply()
            capabilitySwitches[resource]?.isChecked = false
            pendingCapability = null
            refreshController()
            if (::status.isInitialized) status.text = "Системный доступ не предоставлен. Возможность осталась выключенной."
        }
    }

    private fun enablePendingCapability() {
        val resource = pendingCapability ?: return
        prefs.edit().putBoolean(resource, true).apply()
        capabilitySwitches[resource]?.isChecked = true
        pendingCapability = null
        refreshController()
        if (::status.isInitialized) status.text = "Разрешение включено. MANA может использовать только соответствующую функцию."
    }

    private fun reconcileRevokedCapabilities() {
        val active = enabledResources()
        capabilities.forEach { capability ->
            if (capability.resource != pendingCapability &&
                prefs.getBoolean(capability.resource, false) &&
                capability.resource !in active
            ) {
                prefs.edit().putBoolean(capability.resource, false).apply()
                capabilitySwitches[capability.resource]?.isChecked = false
            }
        }
    }

    private fun enabledResources(): Set<String> = capabilities
        .filter { prefs.getBoolean(it.resource, false) }
        .map { it.resource }
        .filter { resource ->
            when (resource) {
                "device.torch" -> checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                "device.display.brightness" -> Settings.System.canWrite(this)
                "device.notifications.dnd" -> getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
                else -> true
            }
        }.toSet()

    private fun refreshController() {
        controller = runCatching { adapter.createController(enabledResources()) }.getOrNull()
    }

    private fun handleIncomingOwnerAction() {
        if (incomingOwnerActionHandled) return
        val action = intent.getStringExtra("mana.owner.action") ?: return
        incomingOwnerActionHandled = true
        val requestId = intent.getStringExtra("mana.owner.request_id")
        val parameters = mutableMapOf<String, String>()
        listOf("package", "enabled", "level", "stream").forEach { key ->
            intent.getStringExtra("mana.owner.param.$key")?.let { parameters[key] = it }
        }
        intent.removeExtra("mana.owner.action")
        intent.removeExtra("mana.owner.request_id")
        parameters.keys.forEach { intent.removeExtra("mana.owner.param.$it") }

        AlertDialog.Builder(this)
            .setTitle("Подтверди команду MANA")
            .setMessage("MANA распознала действие «$action» с параметрами: $parameters. Выполнить его?")
            .setNegativeButton("Отмена") { _, _ ->
                status.text = "Команда отменена владельцем."
            }
            .setPositiveButton("Выполнить") { _, _ ->
                dispatch(action, parameters, ownerConfirmed = true, requestId = requestId)
            }
            .show()
    }

    private fun dispatch(
        action: String,
        parameters: Map<String, String>,
        ownerConfirmed: Boolean = false,
        requestId: String? = null
    ) {
        val current = controller ?: run {
            status.text = "Контроллер недоступен. Проверь включённые разрешения."
            return
        }
        val result = current.request(
            ManaActionRequest(
                requestId = requestId ?: "owner-ui-${System.currentTimeMillis()}",
                action = action,
                parameters = parameters,
                ownerConfirmed = ownerConfirmed
            )
        )
        status.text = "${result.outcome}: ${result.message}"
        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequest) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) enablePendingCapability()
            else {
                val resource = pendingCapability
                if (resource != null) {
                    prefs.edit().putBoolean(resource, false).apply()
                    capabilitySwitches[resource]?.isChecked = false
                }
                pendingCapability = null
                refreshController()
                status.text = "Разрешение Камера не предоставлено. Управление фонариком отключено."
            }
        }
    }

    private data class Capability(val resource: String, val title: String, val description: String)
}
