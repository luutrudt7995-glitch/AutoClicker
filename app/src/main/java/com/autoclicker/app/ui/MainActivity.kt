package com.autoclicker.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.autoclicker.app.R
import com.autoclicker.app.model.AppSettings
import com.autoclicker.app.model.ClickMode
import com.autoclicker.app.service.AutoClickAccessibilityService
import com.autoclicker.app.service.OverlayService
import com.autoclicker.app.utils.PreferenceManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private var isRunning = false

    // Views
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvClickCount: TextView
    private lateinit var tvDelayValue: TextView
    private lateinit var tvSwipeCount: TextView
    private lateinit var radioGroupMode: RadioGroup
    private lateinit var radioFixed: RadioButton
    private lateinit var radioRandom: RadioButton
    private lateinit var switchSwipeMode: com.google.android.material.switchmaterial.SwitchMaterial
    private lateinit var cardClickConfig: CardView
    private lateinit var cardSwipeConfig: CardView
    private lateinit var btnStartStop: Button
    private lateinit var btnSettings: Button
    private lateinit var btnSwipeConfig: Button

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val running = intent.getBooleanExtra(AutoClickAccessibilityService.EXTRA_IS_RUNNING, false)
            val count = intent.getLongExtra(AutoClickAccessibilityService.EXTRA_CLICK_COUNT, -1L)
            isRunning = running
            updateRunningUI(running, count)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Init views
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvClickCount = findViewById(R.id.tvClickCount)
        tvDelayValue = findViewById(R.id.tvDelayValue)
        tvSwipeCount = findViewById(R.id.tvSwipeCount)
        radioGroupMode = findViewById(R.id.radioGroupMode)
        radioFixed = findViewById(R.id.radioFixed)
        radioRandom = findViewById(R.id.radioRandom)
        switchSwipeMode = findViewById(R.id.switchSwipeMode)
        cardClickConfig = findViewById(R.id.cardClickConfig)
        cardSwipeConfig = findViewById(R.id.cardSwipeConfig)
        btnStartStop = findViewById(R.id.btnStartStop)
        btnSettings = findViewById(R.id.btnSettings)
        btnSwipeConfig = findViewById(R.id.btnSwipeConfig)

        settings = PreferenceManager.loadSettings(this)
        setupListeners()
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        settings = PreferenceManager.loadSettings(this)
        refreshSettingsDisplay()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            statusReceiver,
            IntentFilter(AutoClickAccessibilityService.ACTION_STATUS)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver)
    }

    private fun setupListeners() {
        radioGroupMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioFixed -> ClickMode.FIXED
                R.id.radioRandom -> ClickMode.RANDOM
                else -> ClickMode.FIXED
            }
            settings = settings.copy(clickMode = mode)
            PreferenceManager.saveSettings(this, settings)
            refreshSettingsDisplay()
        }

        switchSwipeMode.setOnCheckedChangeListener { _, checked ->
            settings = settings.copy(isSwipeMode = checked)
            PreferenceManager.saveSettings(this, settings)
            refreshModePanel()
        }

        btnStartStop.setOnClickListener {
            if (!checkAccessibilityEnabled()) {
                showAccessibilityDialog(); return@setOnClickListener
            }
            if (!checkOverlayPermission()) {
                requestOverlayPermission(); return@setOnClickListener
            }
            if (isRunning) {
                stopService()
            } else {
                startOverlayService()
                if (settings.isSwipeMode) {
                    sendAction(AutoClickAccessibilityService.ACTION_START_SWIPE)
                } else {
                    sendAction(AutoClickAccessibilityService.ACTION_START_CLICK)
                }
            }
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnSwipeConfig.setOnClickListener {
            startActivity(Intent(this, SwipeConfigActivity::class.java))
        }
    }

    private fun refreshSettingsDisplay() {
        settings = PreferenceManager.loadSettings(this)
        when (settings.clickMode) {
            ClickMode.FIXED -> radioFixed.isChecked = true
            ClickMode.RANDOM -> radioRandom.isChecked = true
        }
        tvDelayValue.text = when (settings.clickMode) {
            ClickMode.FIXED -> "${settings.fixedDelay} ms"
            ClickMode.RANDOM -> "${settings.randomDelayMin} – ${settings.randomDelayMax} ms"
        }
        switchSwipeMode.isChecked = settings.isSwipeMode
        tvSwipeCount.text = "${settings.swipePoints.size} điểm vuốt đã cài"
        refreshModePanel()
    }

    private fun refreshModePanel() {
        val isSwipe = settings.isSwipeMode
        cardClickConfig.visibility = if (isSwipe) android.view.View.GONE else android.view.View.VISIBLE
        cardSwipeConfig.visibility = if (isSwipe) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun updateRunningUI(running: Boolean, count: Long) {
        isRunning = running
        btnStartStop.text = if (running) "⏹ DỪNG LẠI" else "▶ BẮT ĐẦU"
        btnStartStop.setBackgroundColor(
            getColor(if (running) R.color.stop_red else R.color.start_green)
        )
        if (count >= 0) {
            tvClickCount.text = "Số lần click: $count"
        } else if (!running) {
            tvClickCount.text = "Số lần click: 0"
        }
        tvStatusBadge.text = if (running) "● ĐANG CHẠY" else "○ ĐÃ DỪNG"
        tvStatusBadge.setTextColor(
            getColor(if (running) R.color.start_green else R.color.text_secondary)
        )
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopService() {
        sendAction(AutoClickAccessibilityService.ACTION_STOP_CLICK)
        sendAction(AutoClickAccessibilityService.ACTION_STOP_SWIPE)
    }

    private fun sendAction(action: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(action))
    }

    private fun checkPermissions() {
        if (!checkOverlayPermission()) {
            AlertDialog.Builder(this)
                .setTitle("Cần quyền hiển thị trên ứng dụng khác")
                .setMessage("Auto Clicker cần quyền hiển thị nổi trên các ứng dụng khác để hoạt động.")
                .setPositiveButton("Cấp quyền") { _, _ -> requestOverlayPermission() }
                .setNegativeButton("Bỏ qua", null)
                .show()
        }
        if (!checkAccessibilityEnabled()) {
            showAccessibilityDialog()
        }
    }

    private fun checkOverlayPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun checkAccessibilityEnabled(): Boolean {
        return AutoClickAccessibilityService.isServiceRunning || isAccessibilityServiceEnabled()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedService = "$packageName/${AutoClickAccessibilityService::class.java.name}"
        return try {
            val enabled = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            enabled.contains(expectedService)
        } catch (e: Exception) { false }
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cần bật Dịch vụ trợ năng")
            .setMessage(
                "Auto Clicker cần bật Accessibility Service để thực hiện click/vuốt tự động.\n\n" +
                "Bước:\n1. Nhấn \"Mở cài đặt\"\n2. Tìm \"Auto Clicker\"\n3. Bật dịch vụ"
            )
            .setPositiveButton("Mở cài đặt") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton("Bỏ qua", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java)); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
