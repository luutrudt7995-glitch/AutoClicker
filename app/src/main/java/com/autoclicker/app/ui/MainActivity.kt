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
import com.autoclicker.app.databinding.ActivityMainBinding
import com.autoclicker.app.model.AppSettings
import com.autoclicker.app.model.ClickMode
import com.autoclicker.app.service.AutoClickAccessibilityService
import com.autoclicker.app.service.OverlayService
import com.autoclicker.app.utils.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: AppSettings
    private var isRunning = false

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val running = intent.getBooleanExtra(AutoClickAccessibilityService.EXTRA_IS_RUNNING, false)
            val count   = intent.getLongExtra(AutoClickAccessibilityService.EXTRA_CLICK_COUNT, -1L)
            isRunning = running
            updateRunningUI(running, count)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        settings = PreferenceManager.loadSettings(this)

        setupUI()
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

    // ── Setup ────────────────────────────────────────────────────────────────
    private fun setupUI() {
        refreshSettingsDisplay()
        updateRunningUI(false, -1)
    }

    private fun setupListeners() {
        // Mode toggle
        binding.radioGroupMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioFixed  -> ClickMode.FIXED
                R.id.radioRandom -> ClickMode.RANDOM
                else             -> ClickMode.FIXED
            }
            settings = settings.copy(clickMode = mode)
            PreferenceManager.saveSettings(this, settings)
            refreshSettingsDisplay()
        }

        // Toggle click/swipe mode
        binding.switchSwipeMode.setOnCheckedChangeListener { _, checked ->
            settings = settings.copy(isSwipeMode = checked)
            PreferenceManager.saveSettings(this, settings)
            refreshModePanel()
        }

        // Start / Stop main button
        binding.btnStartStop.setOnClickListener {
            if (!checkAccessibilityEnabled()) {
                showAccessibilityDialog(); return@setOnClickListener
            }
            if (!checkOverlayPermission()) {
                requestOverlayPermission(); return@setOnClickListener
            }
            if (isRunning) {
                stopService(); return@setOnClickListener
            }
            startOverlayService()
            if (settings.isSwipeMode) {
                sendAction(AutoClickAccessibilityService.ACTION_START_SWIPE)
            } else {
                sendAction(AutoClickAccessibilityService.ACTION_START_CLICK)
            }
        }

        // Settings button
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Swipe config button
        binding.btnSwipeConfig.setOnClickListener {
            startActivity(Intent(this, SwipeConfigActivity::class.java))
        }
    }

    private fun refreshSettingsDisplay() {
        settings = PreferenceManager.loadSettings(this)

        // Mode radio
        when (settings.clickMode) {
            ClickMode.FIXED  -> binding.radioFixed.isChecked  = true
            ClickMode.RANDOM -> binding.radioRandom.isChecked = true
        }

        // Delays
        binding.tvDelayValue.text = when (settings.clickMode) {
            ClickMode.FIXED  -> "${settings.fixedDelay} ms"
            ClickMode.RANDOM -> "${settings.randomDelayMin} – ${settings.randomDelayMax} ms"
        }

        // Swipe mode
        binding.switchSwipeMode.isChecked = settings.isSwipeMode
        binding.tvSwipeCount.text = "${settings.swipePoints.size} điểm vuốt đã cài"

        refreshModePanel()
    }

    private fun refreshModePanel() {
        val isSwipe = settings.isSwipeMode
        binding.cardClickConfig.visibility = if (isSwipe) android.view.View.GONE  else android.view.View.VISIBLE
        binding.cardSwipeConfig.visibility = if (isSwipe) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun updateRunningUI(running: Boolean, count: Long) {
        isRunning = running
        binding.btnStartStop.text = if (running) "⏹ DỪNG LẠI" else "▶ BẮT ĐẦU"
        binding.btnStartStop.setBackgroundColor(
            getColor(if (running) R.color.stop_red else R.color.start_green)
        )
        if (count >= 0) {
            binding.tvClickCount.text = "Số lần click: $count"
        } else if (!running) {
            binding.tvClickCount.text = "Số lần click: 0"
        }
        binding.tvStatusBadge.text = if (running) "● ĐANG CHẠY" else "○ ĐÃ DỪNG"
        binding.tvStatusBadge.setTextColor(
            getColor(if (running) R.color.start_green else R.color.text_secondary)
        )
    }

    // ── Service control ──────────────────────────────────────────────────────
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

    // ── Permission checks ────────────────────────────────────────────────────
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
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun checkAccessibilityEnabled(): Boolean {
        return AutoClickAccessibilityService.isServiceRunning ||
                isAccessibilityServiceEnabled()
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

    // ── Menu ─────────────────────────────────────────────────────────────────
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
