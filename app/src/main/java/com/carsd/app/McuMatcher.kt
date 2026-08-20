package com.carsd.app

import android.os.Build
import java.io.File

data class McuFingerprint(
    val matchedProfile: String?,
    val confidence: Int,
    val reasons: List<String>,
    val candidatePackages: List<String>,
    val binderServices: List<String>,
    val candidateDeviceNodes: List<String>,
    val rawProps: List<String>
)

object McuMatcher {
    private fun sh(cmd: String): String = runCatching {
        ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start()
            .inputStream.bufferedReader().use { it.readText() }
    }.getOrDefault("")

    fun scan(): McuFingerprint {
        val props = sh("getprop").lineSequence().filter { it.isNotBlank() }.toList()
        val all = props.joinToString("\n")
        var score = 0
        val reasons = mutableListOf<String>()
        if (all.contains("F52L", true)) { score += 35; reasons += "F52L platform signature" }
        if (all.contains("S212A70", true)) { score += 20; reasons += "S212A70 app family" }
        if (all.contains("UP01", true)) { score += 20; reasons += "UP01 MCU family" }
        if (all.contains("CAN0017", true)) { score += 15; reasons += "CAN0017 signature" }
        if (Build.MODEL.contains("T440", true)) { score += 5; reasons += "T440 model hint" }

        val profile = when {
            score >= 70 -> "F52L_UP01_CAN0017"
            score >= 45 -> "F52L_GENERIC"
            else -> null
        }

        val packageKeys = listOf("nwd", "mcu", "can", "car", "setting", "vehicle", "syu", "tw", "ts")
        val packages = sh("pm list packages").lineSequence()
            .map { it.removePrefix("package:").trim() }
            .filter { p -> packageKeys.any { p.lowercase().contains(it) } }
            .distinct().sorted().take(120).toList()

        val serviceKeys = listOf("mcu", "can", "car", "vehicle", "audio", "led", "fan", "nwd")
        val services = sh("service list").lineSequence()
            .filter { line -> serviceKeys.any { line.lowercase().contains(it) } }
            .map { it.trim() }.take(120).toList()

        val nodeKeys = listOf("mcu", "can", "uart", "tty", "rpmsg", "pwm", "fan", "led")
        val nodes = mutableListOf<String>()
        listOf("/dev", "/sys/class", "/sys/devices/platform").forEach { root ->
            runCatching {
                File(root).walkTopDown().maxDepth(3).forEach { f ->
                    if (nodeKeys.any { f.name.lowercase().contains(it) }) nodes += f.absolutePath
                }
            }
        }

        return McuFingerprint(profile, score.coerceAtMost(100), reasons,
            packages, services, nodes.distinct().take(180), props.take(400))
    }
}
