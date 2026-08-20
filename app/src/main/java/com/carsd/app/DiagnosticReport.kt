package com.carsd.app

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticReport {
    fun build(context: Context, fp: McuFingerprint, hw: HardwareProfile?): File {
        val dir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "CAR_SD_T440_$stamp.txt").also { out ->
            out.writeText(buildString {
                appendLine("CAR SD T440 / F52L diagnostic report")
                appendLine("Android ${Build.VERSION.RELEASE} / SDK ${Build.VERSION.SDK_INT}")
                appendLine("MODEL=${Build.MODEL} DEVICE=${Build.DEVICE} BOARD=${Build.BOARD}")
                appendLine("PROFILE=${fp.matchedProfile ?: "UNKNOWN"} CONFIDENCE=${fp.confidence}%")
                fp.reasons.forEach { appendLine("MATCH: $it") }
                appendLine("\nHARDWARE")
                appendLine("ROOT=${hw?.root}")
                hw?.temperaturePaths?.forEach { appendLine("TEMP=$it") }
                hw?.fanPwmPaths?.forEach { appendLine("FAN_PWM=$it") }
                hw?.fanRpmPaths?.forEach { appendLine("FAN_RPM=$it") }
                hw?.ledPaths?.forEach { appendLine("LED=$it") }
                appendLine("\nPACKAGES")
                fp.candidatePackages.forEach { appendLine(it) }
                appendLine("\nBINDER_SERVICES")
                fp.binderServices.forEach { appendLine(it) }
                appendLine("\nDEVICE_NODES")
                fp.candidateDeviceNodes.forEach { appendLine(it) }
                appendLine("\nGETPROP")
                fp.rawProps.forEach { appendLine(it) }
                appendLine("\nSAFETY: no undocumented MCU write performed")
            })
        }
    }
}
