package com.cute.battery

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var preview: CuteBatteryView
    private lateinit var serviceStatus: TextView
    private var s = OverlaySettings()
    private var previewOverride = false

    private val styleIds = mapOf(
        OverlaySettings.STYLE_EYE to R.id.styleEye,
        OverlaySettings.STYLE_CAT to R.id.styleCat,
        OverlaySettings.STYLE_BUNNY to R.id.styleBunny,
        OverlaySettings.STYLE_BEAR to R.id.styleBear,
        OverlaySettings.STYLE_HEART to R.id.styleHeart,
    )
    private val colorIds = mapOf(
        OverlaySettings.COLOR_AUTO to R.id.colorAuto,
        OverlaySettings.COLOR_PINK to R.id.colorPink,
        OverlaySettings.COLOR_LAVENDER to R.id.colorLavender,
        OverlaySettings.COLOR_MINT to R.id.colorMint,
    )
    private val pillIds = mapOf(
        OverlaySettings.PILL_NONE to R.id.pillNone,
        OverlaySettings.PILL_BLACK to R.id.pillBlack,
        OverlaySettings.PILL_WHITE to R.id.pillWhite,
    )

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (previewOverride) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0 && scale > 0) preview.level = level * 100 / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            preview.charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        s = Prefs.load(this)
        preview = findViewById(R.id.preview)
        serviceStatus = findViewById(R.id.serviceStatus)
        preview.settings = s

        findViewById<Button>(R.id.openOverlay).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }

        // Android 13+: ask to show the small "Cute Battery is on" notification.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        // Preview-only controls
        findViewById<SeekBar>(R.id.previewLevel).apply {
            progress = preview.level
            onChange { previewOverride = true; preview.level = it }
        }
        findViewById<Switch>(R.id.previewCharging).setOnCheckedChangeListener { _, on ->
            previewOverride = true
            preview.charging = on
        }

        // Saved settings
        bindSwitch(R.id.enabled, s.enabled) {
            update(s.copy(enabled = it))
            if (it) OverlayService.start(this) else OverlayService.stop(this)
            refreshStatus()
        }
        bindSwitch(R.id.hideInFullscreen, s.hideInFullscreen) { update(s.copy(hideInFullscreen = it)) }
        bindSwitch(R.id.lightOutline, s.lightOutline) { update(s.copy(lightOutline = it)) }
        bindSwitch(R.id.showPercent, s.showPercent) { update(s.copy(showPercent = it)) }

        bindGroup(R.id.styleGroup, styleIds, s.style) { update(s.copy(style = it)) }
        bindGroup(R.id.colorGroup, colorIds, s.color) { update(s.copy(color = it)) }
        bindGroup(R.id.pillGroup, pillIds, s.pill) { update(s.copy(pill = it)) }

        bindSlider(R.id.size, R.id.sizeLabel, s.sizeDp, min = 14, label = "Size") {
            update(s.copy(sizeDp = it))
        }
        bindSlider(R.id.offsetX, R.id.offsetXLabel, s.offsetXDp, min = 0, label = "Distance from right edge") {
            update(s.copy(offsetXDp = it))
        }
        bindSlider(R.id.offsetY, R.id.offsetYLabel, s.offsetYDp, min = 0, label = "Distance from top") {
            update(s.copy(offsetYDp = it))
        }

        updatePreviewBar()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.let { batteryReceiver.onReceive(this, it) }
        if (s.enabled) OverlayService.start(this)
        refreshStatus()
    }

    private fun refreshStatus() {
        serviceStatus.text = when {
            !Settings.canDrawOverlays(this) ->
                "⚪ Step 1: allow Cute Battery to display over other apps"
            s.enabled -> "✅ Cute Battery is on"
            else -> "⚪ Cute Battery is paused. Turn on \"Show cute battery\" below."
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(batteryReceiver)
    }

    /** Save and refresh. The overlay service listens for changes and updates itself. */
    private fun update(n: OverlaySettings) {
        s = n
        Prefs.save(this, s)
        preview.settings = s
        updatePreviewBar()
    }

    private fun updatePreviewBar() {
        // Dark fake status bar for white outline, light one for dark outline.
        findViewById<android.view.View>(R.id.previewBar)
            .setBackgroundColor(if (s.lightOutline) 0xFF2B2130.toInt() else 0xFFF2E9EE.toInt())
    }

    // ---------- small helpers ----------

    private fun bindSwitch(id: Int, value: Boolean, onChange: (Boolean) -> Unit) {
        findViewById<Switch>(id).apply {
            isChecked = value
            setOnCheckedChangeListener { _, on -> onChange(on) }
        }
    }

    private fun bindGroup(id: Int, ids: Map<String, Int>, value: String, onChange: (String) -> Unit) {
        findViewById<RadioGroup>(id).apply {
            ids[value]?.let { check(it) }
            setOnCheckedChangeListener { _, checkedId ->
                ids.entries.firstOrNull { it.value == checkedId }?.let { onChange(it.key) }
            }
        }
    }

    private fun bindSlider(
        id: Int, labelId: Int, value: Int, min: Int, label: String, onValue: (Int) -> Unit,
    ) {
        val labelView = findViewById<TextView>(labelId)
        labelView.text = "$label: $value dp"
        findViewById<SeekBar>(id).apply {
            progress = value - min
            onChange {
                val v = it + min
                labelView.text = "$label: $v dp"
                onValue(v)
            }
        }
    }

    private fun SeekBar.onChange(block: (Int) -> Unit) {
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) block(progress)
            }
            override fun onStartTrackingTouch(bar: SeekBar) {}
            override fun onStopTrackingTouch(bar: SeekBar) {}
        })
    }
}
