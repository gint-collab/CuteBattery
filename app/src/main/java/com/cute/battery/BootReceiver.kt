package com.cute.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Turns the cute battery back on after the phone restarts or the app is updated. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (Prefs.load(context).enabled) {
                    runCatching { OverlayService.start(context) }
                }
            }
        }
    }
}
