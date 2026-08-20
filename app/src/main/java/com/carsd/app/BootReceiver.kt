package com.carsd.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED && intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        val prefs = CarPrefs(context)
        val s = prefs.load()
        val pending = goAsync()
        val h = Handler(Looper.getMainLooper())
        val delays = listOf(2000L, 5000L, 10000L)
        delays.forEachIndexed { i, d ->
            h.postDelayed({
                val caps = AdaptiveProbe.detect()
                AdaptiveController(context).applyAudio(s.mediaVolume, s.callVolume, caps)
                if (i == delays.lastIndex) pending.finish()
            }, d)
        }
    }
}
