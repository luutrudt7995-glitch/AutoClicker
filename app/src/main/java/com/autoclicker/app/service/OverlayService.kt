package com.autoclicker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.autoclicker.app.R
import com.autoclicker.app.model.AppSettings
import com.autoclicker.app.ui.MainActivity
import com.autoclicker.app.utils.PreferenceManager

class OverlayService : Service() {

    companion object {
        const val ACTION_START = "com.autoclicker.overlay.START"
        const val ACTION_STOP  = "com.autoclicker.overlay.STOP"
        private const val CHANNEL_ID = "auto_clicker_channel"
        private const val NOTIF_ID   = 1001
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isRunning = false
    private lateinit var settings: AppSettings

    // ── Broadcast receiver for status updates from Accessibility Service ─────
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val running = intent.getBooleanExtra(AutoClickAccessibilityService.EXTRA_IS_RUNNING, false)
            val count   = intent.getLongExtra(AutoClickAccessibilityService.EXTRA_CLICK_COUNT, -1L)
            updateOverlayStatus(running, count)
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        settings = PreferenceManager.loadSettings(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(
            statusReceiver,
            IntentFilter(AutoClickAccessibilityService.ACTION_STATUS)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        if (overlayView == null) showOverlay()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Overlay window ───────────────────────────────────────────────────────
    private fun showOverlay() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_control, null)
        overlayView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        // Drag logic
        var initialX = 0; var initialY = 0
        var touchX = 0f;  var touchY = 0f
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        // Button: play/stop click
        view.findViewById<ImageButton>(R.id.btnToggleClick).setOnClickListener {
            settings = PreferenceManager.loadSettings(this)
            if (!isRunning) {
                if (settings.isSwipeMode) {
                    sendBroadcast(AutoClickAccessibilityService.ACTION_START_SWIPE)
                } else {
                    sendBroadcast(AutoClickAccessibilityService.ACTION_START_CLICK)
                }
            } else {
                sendBroadcast(AutoClickAccessibilityService.ACTION_STOP_CLICK)
                sendBroadcast(AutoClickAccessibilityService.ACTION_STOP_SWIPE)
            }
        }

        // Button: open settings
        view.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            val i = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(i)
        }

        windowManager.addView(view, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }

    private fun updateOverlayStatus(running: Boolean, count: Long) {
        isRunning = running
        overlayView?.let { v ->
            val btn   = v.findViewById<ImageButton>(R.id.btnToggleClick)
            val label = v.findViewById<TextView>(R.id.tvStatus)
            btn.setImageResource(
                if (running) R.drawable.ic_stop else R.drawable.ic_play
            )
            label.text = if (running) {
                if (count >= 0) "✓ $count" else "▶ ON"
            } else "■ OFF"
        }
    }

    private fun sendBroadcast(action: String) {
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent(action))
    }

    // ── Notification ─────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Auto Clicker",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Auto Clicker overlay running" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Clicker đang chạy")
            .setContentText("Nhấn để mở cài đặt")
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
