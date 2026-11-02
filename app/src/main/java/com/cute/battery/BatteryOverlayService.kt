package com.cute.battery

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Keeps the cute battery floating over the status bar (top-right corner).
 * Uses TYPE_ACCESSIBILITY_OVERLAY, the only window type a normal app can
 * place above the status bar without root.
 */
class BatteryOverlayService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlay: CuteBatteryView? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = updateBattery(intent)
    }

    // Kept as a field: SharedPreferences only holds a weak reference to listeners.
    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> applySettings() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val settings = Prefs.load(this)
        val view = CuteBatteryView(this).apply { this.settings = settings }
        overlay = view
        wm.addView(view, buildParams(settings))
        applySettings()

        // ACTION_BATTERY_CHANGED is "sticky": registering returns the current state right away.
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.let { updateBattery(it) }

        Prefs.get(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun buildParams(s: OverlaySettings) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        dp(s.sizeDp),
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.END
        x = dp(s.offsetXDp)
        y = dp(s.offsetYDp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun applySettings() {
        val view = overlay ?: return
        val wm = windowManager ?: return
        val s = Prefs.load(this)
        view.settings = s
        view.visibility = if (s.enabled) View.VISIBLE else View.GONE
        wm.updateViewLayout(view, buildParams(s))
    }

    private fun updateBattery(intent: Intent) {
        val view = overlay ?: return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level >= 0 && scale > 0) view.level = level * 100 / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        view.charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not needed: we only draw, we never read the screen.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        runCatching { unregisterReceiver(batteryReceiver) }
        overlay?.let { runCatching { windowManager?.removeView(it) } }
        overlay = null
        super.onDestroy()
    }
}
