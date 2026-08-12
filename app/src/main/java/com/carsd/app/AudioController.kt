package com.carsd.app

import android.content.Context
import android.media.AudioManager

class AudioController(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun mediaMax() = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    fun callMax() = audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)

    fun apply(media: Int, call: Int): Result<Unit> = runCatching {
        val safeMedia = media.coerceIn(0, mediaMax())
        val safeCall = call.coerceIn(0, callMax())
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, safeMedia, 0)
        audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, safeCall, 0)
    }
}
