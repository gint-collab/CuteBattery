package com.cute.battery

import android.content.Context
import android.content.SharedPreferences

/** Everything the user can change, saved so the overlay service can read it. */
data class OverlaySettings(
    val enabled: Boolean = true,
    val style: String = STYLE_EYE,        // eye, cat, bunny, bear, heart
    val color: String = COLOR_AUTO,       // auto, pink, lavender, mint
    val pill: String = PILL_NONE,         // none, black, white (background bubble)
    val lightOutline: Boolean = true,     // white outline for dark status bars
    val showPercent: Boolean = true,
    val sizeDp: Int = 22,
    val offsetXDp: Int = 60,              // distance from the right edge
    val offsetYDp: Int = 2,               // distance from the top edge
    val hideInFullscreen: Boolean = false, // hide while videos/games are full screen
) {
    companion object {
        const val STYLE_EYE = "eye"
        const val STYLE_CAT = "cat"
        const val STYLE_BUNNY = "bunny"
        const val STYLE_BEAR = "bear"
        const val STYLE_HEART = "heart"

        const val COLOR_AUTO = "auto"
        const val COLOR_PINK = "pink"
        const val COLOR_LAVENDER = "lavender"
        const val COLOR_MINT = "mint"

        const val PILL_NONE = "none"
        const val PILL_BLACK = "black"
        const val PILL_WHITE = "white"
    }
}

object Prefs {
    private const val NAME = "cute_battery"

    fun get(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun load(context: Context): OverlaySettings {
        val p = get(context)
        val d = OverlaySettings()
        return OverlaySettings(
            enabled = p.getBoolean("enabled", d.enabled),
            style = p.getString("style", d.style) ?: d.style,
            color = p.getString("color", d.color) ?: d.color,
            pill = p.getString("pill", d.pill) ?: d.pill,
            lightOutline = p.getBoolean("lightOutline", d.lightOutline),
            showPercent = p.getBoolean("showPercent", d.showPercent),
            sizeDp = p.getInt("sizeDp", d.sizeDp),
            offsetXDp = p.getInt("offsetXDp", d.offsetXDp),
            offsetYDp = p.getInt("offsetYDp", d.offsetYDp),
            hideInFullscreen = p.getBoolean("hideInFullscreen", d.hideInFullscreen),
        )
    }

    fun save(context: Context, s: OverlaySettings) {
        get(context).edit()
            .putBoolean("enabled", s.enabled)
            .putString("style", s.style)
            .putString("color", s.color)
            .putString("pill", s.pill)
            .putBoolean("lightOutline", s.lightOutline)
            .putBoolean("showPercent", s.showPercent)
            .putInt("sizeDp", s.sizeDp)
            .putInt("offsetXDp", s.offsetXDp)
            .putInt("offsetYDp", s.offsetYDp)
            .putBoolean("hideInFullscreen", s.hideInFullscreen)
            .apply()
    }
}
