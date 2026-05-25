package com.autoclicker.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.autoclicker.app.model.AppSettings
import com.autoclicker.app.model.ClickMode
import com.autoclicker.app.model.SwipePoint
import com.autoclicker.app.utils.PreferenceManager
import com.autoclicker.app.utils.RandomDelayGenerator

class AutoClickAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_START_CLICK  = "com.autoclicker.START_CLICK"
        const val ACTION_STOP_CLICK   = "com.autoclicker.STOP_CLICK"
        const val ACTION_START_SWIPE  = "com.autoclicker.START_SWIPE"
        const val ACTION_STOP_SWIPE   = "com.autoclicker.STOP_SWIPE"
        const val ACTION_STATUS       = "com.autoclicker.STATUS"
        const val EXTRA_IS_RUNNING    = "is_running"
        const val EXTRA_MODE          = "mode"
        const val EXTRA_CLICK_COUNT   = "click_count"

        var instance: AutoClickAccessibilityService? = null
        var isServiceRunning = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isClickRunning = false
    private var isSwipeRunning = false
    private var clickCount = 0L
    private lateinit var settings: AppSettings
    private var swipeIndex = 0

    // ── Broadcast receiver ───────────────────────────────────────────────────
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            settings = PreferenceManager.loadSettings(context)
            when (intent.action) {
                ACTION_START_CLICK -> startClicking()
                ACTION_STOP_CLICK  -> stopClicking()
                ACTION_START_SWIPE -> startSwiping()
                ACTION_STOP_SWIPE  -> stopSwiping()
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceRunning = true
        settings = PreferenceManager.loadSettings(this)

        val filter = IntentFilter().apply {
            addAction(ACTION_START_CLICK)
            addAction(ACTION_STOP_CLICK)
            addAction(ACTION_START_SWIPE)
            addAction(ACTION_STOP_SWIPE)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
        broadcastStatus(false, "idle")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isServiceRunning = false
        stopClicking()
        stopSwiping()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { stopClicking(); stopSwiping() }

    // ── Click logic ──────────────────────────────────────────────────────────
    private fun startClicking() {
        if (isClickRunning) return
        isClickRunning = true
        clickCount = 0
        RandomDelayGenerator.reset()
        broadcastStatus(true, "clicking")
        scheduleNextClick()
    }

    private fun stopClicking() {
        isClickRunning = false
        handler.removeCallbacksAndMessages("click")
        broadcastStatus(false, "idle")
    }

    private fun scheduleNextClick() {
        if (!isClickRunning) return
        val delay = when (settings.clickMode) {
            ClickMode.FIXED  -> settings.fixedDelay
            ClickMode.RANDOM -> RandomDelayGenerator.nextRandomDelay(
                settings.randomDelayMin, settings.randomDelayMax
            )
        }
        handler.postDelayed({
            if (isClickRunning) {
                performClick(settings.clickX, settings.clickY)
                clickCount++
                broadcastCount(clickCount)
                scheduleNextClick()
            }
        }, delay)
    }

    // ── Swipe logic ──────────────────────────────────────────────────────────
    private fun startSwiping() {
        if (isSwipeRunning) return
        if (settings.swipePoints.isEmpty()) return
        isSwipeRunning = true
        swipeIndex = 0
        broadcastStatus(true, "swiping")
        scheduleNextSwipe()
    }

    private fun stopSwiping() {
        isSwipeRunning = false
        handler.removeCallbacksAndMessages("swipe")
        broadcastStatus(false, "idle")
    }

    private fun scheduleNextSwipe() {
        if (!isSwipeRunning) return
        val points = settings.swipePoints
        if (points.isEmpty()) { stopSwiping(); return }
        val pt = points[swipeIndex % points.size]
        handler.postDelayed({
            if (isSwipeRunning) {
                performSwipe(pt)
                swipeIndex++
                scheduleNextSwipe()
            }
        }, pt.delayBefore)
    }

    // ── Gesture helpers ──────────────────────────────────────────────────────
    private fun performClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun performSwipe(pt: SwipePoint) {
        val path = Path().apply {
            moveTo(pt.startX, pt.startY)
            lineTo(pt.endX, pt.endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, pt.duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // ── Broadcasts ───────────────────────────────────────────────────────────
    private fun broadcastStatus(running: Boolean, mode: String) {
        val intent = Intent(ACTION_STATUS).apply {
            putExtra(EXTRA_IS_RUNNING, running)
            putExtra(EXTRA_MODE, mode)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastCount(count: Long) {
        val intent = Intent(ACTION_STATUS).apply {
            putExtra(EXTRA_IS_RUNNING, true)
            putExtra(EXTRA_CLICK_COUNT, count)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}
