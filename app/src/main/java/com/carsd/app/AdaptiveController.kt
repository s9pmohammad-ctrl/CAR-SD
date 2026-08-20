package com.carsd.app

import android.content.Context
import android.media.AudioManager
import java.io.File

/**
 * Safe adaptive controller for K2501/NWD head units.
 * It only writes to interfaces that are positively identified and writable.
 * Unknown MCU/PWM channels are never guessed.
 */
data class AdaptiveCapabilities(
    val nwdAudioService: Boolean,
    val nwdManagerService: Boolean,
    val startupVolumeProperty: String?,
    val keyLedBrightnessNodes: List<String>,
    val rgbLedNodes: List<String>,
    val pwmChips: List<String>,
    val root: Boolean
)

object AdaptiveProbe {
    private fun shell(cmd: String): String = runCatching {
        ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start()
            .inputStream.bufferedReader().use { it.readText() }
    }.getOrDefault("")

    fun detect(): AdaptiveCapabilities {
        val services = shell("service list")
        val props = shell("getprop")
        val ledRoot = File("/sys/class/leds")
        val leds = ledRoot.listFiles()?.toList().orEmpty()
        val keyNodes = leds.filter {
            val n = it.name.lowercase()
            listOf("key", "button", "panel", "backlight").any { k -> n.contains(k) }
        }.mapNotNull {
            val b = File(it, "brightness")
            if (b.exists()) b.absolutePath else null
        }
        val rgbNodes = leds.filter {
            File(it, "multi_intensity").exists() || File(it, "multi_index").exists() || it.name.contains("rgb", true)
        }.map { it.absolutePath }
        val pwms = File("/sys/class/pwm").listFiles()?.filter { it.name.startsWith("pwmchip") }?.map { it.absolutePath }.orEmpty()
        val root = RootShell.hasRoot()
        val startup = Regex("\\[persist\\.sys\\.ori_music_volume\\]: \\[(.*?)\\]").find(props)?.groupValues?.getOrNull(1)
        return AdaptiveCapabilities(
            nwdAudioService = services.contains("nwdaudio"),
            nwdManagerService = services.contains("nwdmanager"),
            startupVolumeProperty = startup,
            keyLedBrightnessNodes = keyNodes,
            rgbLedNodes = rgbNodes,
            pwmChips = pwms,
            root = root
        )
    }
}

class AdaptiveController(private val context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun applyAudio(media: Int, call: Int, caps: AdaptiveCapabilities): Result<String> = runCatching {
        val mm = media.coerceIn(0, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
        val cc = call.coerceIn(0, audio.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL))
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, mm, 0)
        audio.setStreamVolume(AudioManager.STREAM_VOICE_CALL, cc, 0)
        if (caps.root && caps.startupVolumeProperty != null) {
            RootShell.exec("setprop persist.sys.ori_music_volume $media").getOrThrow()
            "Android audio + NWD startup volume applied"
        } else {
            "Android audio applied; NWD startup property is read-only without root/system privilege"
        }
    }

    fun setKeyBrightness(percent: Int, caps: AdaptiveCapabilities): Result<String> = runCatching {
        require(caps.keyLedBrightnessNodes.isNotEmpty()) { "No safe key-light node detected" }
        var changed = 0
        for (path in caps.keyLedBrightnessNodes) {
            val f = File(path)
            val max = runCatching { File(f.parentFile, "max_brightness").readText().trim().toInt() }.getOrDefault(255)
            val value = percent.coerceIn(0,100) * max / 100
            if (f.canWrite()) {
                f.writeText(value.toString()); changed++
            } else if (caps.root) {
                RootShell.exec("printf '%s' '$value' > '${path.replace("'", "'\\''")}'").getOrThrow(); changed++
            }
        }
        require(changed > 0) { "Key-light node found but not writable" }
        "Key-light brightness changed on $changed node(s)"
    }

    fun fanSafetyStatus(caps: AdaptiveCapabilities): String = when {
        caps.pwmChips.isEmpty() -> "No PWM controller detected"
        else -> "PWM hardware detected, but fan channel is not proven. Unsafe blind PWM writes are blocked."
    }
}
