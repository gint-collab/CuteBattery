package com.cute.battery

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager

/**
 * Keeps the cute battery floating at the top of the screen.
 *
 * Uses the "Display over other apps" permission (TYPE_APPLICATION_OVERLAY)
 * instead of Accessibility. Android draws this kind of window *under* the
 * status bar, so the real status bar icons appear on top of it.
 *
 * Runs as a foreground service (with a quiet notification) so Android
 * doesn't stop it in the background.
 */
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlay: CuteBatteryView? = null
    private var statusBarVisible = true

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = updateBattery(intent)
    }

    // Kept as a field: SharedPreferences only holds a weak reference to listeners.
    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> applySettings() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val settings = Prefs.load(this)
        val view = CuteBatteryView(this).apply { this.settings = settings }
        overlay = view

        // Optional: hide while an app is full screen (videos, games).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.setOnApplyWindowInsetsListener { v, insets ->
                statusBarVisible = insets.isVisible(WindowInsets.Type.statusBars())
                updateVisibility()
                v.onApplyWindowInsets(insets)
            }
        }

        wm.addView(view, buildParams(settings))
        applySettings()

        // ACTION_BATTERY_CHANGED is "sticky": registering returns the current state right away.
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.let { updateBattery(it) }

        Prefs.get(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Cute Battery", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Keeps the cute battery on screen"
                setShowBadge(false)
            }
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_battery)
            .setContentTitle("Cute Battery is on")
            .setContentText("Tap to change the style")
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun buildParams(s: OverlaySettings) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        dp(s.sizeDp),
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
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
        updateVisibility()
        runCatching { wm.updateViewLayout(view, buildParams(s)) }
    }

    private fun updateVisibility() {
        val view = overlay ?: return
        val hide = Prefs.load(this).hideInFullscreen && !statusBarVisible
        view.visibility = if (hide) View.INVISIBLE else View.VISIBLE
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

    override fun onDestroy() {
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        if (overlay != null) runCatching { unregisterReceiver(batteryReceiver) }
        overlay?.let { runCatching { windowManager?.removeView(it) } }
        overlay = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            context.startForegroundService(Intent(context, OverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
