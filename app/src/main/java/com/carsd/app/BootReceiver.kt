package com.carsd.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val s = CarPrefs(context).load()
            AudioController(context).apply(s.mediaVolume, s.callVolume)
            // Fan/LED are intentionally not forced at boot until the user confirms
            // the detected hardware profile in the app.
        }
    }
}
