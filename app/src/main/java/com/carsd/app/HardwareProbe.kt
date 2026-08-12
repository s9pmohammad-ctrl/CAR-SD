package com.carsd.app

import java.io.File

data class HardwareProfile(
    val root: Boolean,
    val temperaturePaths: List<String>,
    val fanPwmPaths: List<String>,
    val fanRpmPaths: List<String>,
    val ledPaths: List<String>
) {
    val fanDetected: Boolean get() = fanPwmPaths.isNotEmpty() || fanRpmPaths.isNotEmpty()
    val ledDetected: Boolean get() = ledPaths.isNotEmpty()
}

object HardwareProbe {
    private val tempRoots = listOf("/sys/class/thermal", "/sys/class/hwmon")
    private val ledRoot = File("/sys/class/leds")

    private fun children(path: String): List<File> =
        runCatching { File(path).listFiles()?.toList().orEmpty() }.getOrDefault(emptyList())

    fun scan(): HardwareProfile {
        val temps = mutableListOf<String>()
        val pwm = mutableListOf<String>()
        val rpm = mutableListOf<String>()
        val leds = mutableListOf<String>()

        children("/sys/class/thermal").forEach { zone ->
            val t = File(zone, "temp")
            if (t.canRead()) temps += t.absolutePath
        }

        children("/sys/class/hwmon").forEach { hw ->
            hw.listFiles()?.forEach { f ->
                when {
                    f.name.matches(Regex("temp\\d+_input")) && f.canRead() -> temps += f.absolutePath
                    f.name.matches(Regex("pwm\\d+")) -> pwm += f.absolutePath
                    f.name.matches(Regex("fan\\d+_input")) && f.canRead() -> rpm += f.absolutePath
                }
            }
        }

        ledRoot.listFiles()?.forEach { led ->
            val n = led.name.lowercase()
            if (listOf("key", "button", "rgb", "panel", "backlight").any { n.contains(it) }) {
                val brightness = File(led, "brightness")
                if (brightness.exists()) leds += brightness.absolutePath
            }
        }

        return HardwareProfile(
            root = RootShell.hasRoot(),
            temperaturePaths = temps.distinct(),
            fanPwmPaths = pwm.distinct(),
            fanRpmPaths = rpm.distinct(),
            ledPaths = leds.distinct()
        )
    }

    fun readTemperatureC(profile: HardwareProfile): Float? {
        val values = profile.temperaturePaths.mapNotNull { p ->
            runCatching {
                val raw = File(p).readText().trim().toFloat()
                if (raw > 1000f) raw / 1000f else raw
            }.getOrNull()
        }.filter { it in -20f..150f }
        return values.maxOrNull()
    }

    fun writeFanPercent(profile: HardwareProfile, percent: Int): Result<Unit> = runCatching {
        require(profile.fanPwmPaths.isNotEmpty()) { "No PWM fan path detected" }
        val value = (percent.coerceIn(0, 100) * 255 / 100)
        profile.fanPwmPaths.forEach { path ->
            val f = File(path)
            if (f.canWrite()) f.writeText(value.toString())
            else RootShell.exec("printf '%s' '$value' > '${path.replace("'", "'\\''")}'").getOrThrow()
        }
    }

    fun writeButtonBrightness(profile: HardwareProfile, percent: Int): Result<Unit> = runCatching {
        require(profile.ledPaths.isNotEmpty()) { "No button LED path detected" }
        val v = (percent.coerceIn(0, 100) * 255 / 100)
        profile.ledPaths.forEach { path ->
            val f = File(path)
            if (f.canWrite()) f.writeText(v.toString())
            else RootShell.exec("printf '%s' '$v' > '${path.replace("'", "'\\''")}'").getOrThrow()
        }
    }
}
