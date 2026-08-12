package com.carsd.app

import android.content.Context

data class CarSettings(
    val mediaVolume: Int = 6,
    val callVolume: Int = 4,
    val fanAuto: Boolean = true,
    val fanOnTempC: Int = 65,
    val fanOffTempC: Int = 55,
    val fanManualPercent: Int = 70,
    val ledR: Int = 0,
    val ledG: Int = 180,
    val ledB: Int = 255
)

class CarPrefs(context: Context) {
    private val p = context.getSharedPreferences("car_sd", Context.MODE_PRIVATE)

    fun load() = CarSettings(
        mediaVolume = p.getInt("media", 6),
        callVolume = p.getInt("call", 4),
        fanAuto = p.getBoolean("fan_auto", true),
        fanOnTempC = p.getInt("fan_on", 65),
        fanOffTempC = p.getInt("fan_off", 55),
        fanManualPercent = p.getInt("fan_manual", 70),
        ledR = p.getInt("led_r", 0),
        ledG = p.getInt("led_g", 180),
        ledB = p.getInt("led_b", 255)
    )

    fun save(s: CarSettings) {
        p.edit()
            .putInt("media", s.mediaVolume)
            .putInt("call", s.callVolume)
            .putBoolean("fan_auto", s.fanAuto)
            .putInt("fan_on", s.fanOnTempC)
            .putInt("fan_off", s.fanOffTempC)
            .putInt("fan_manual", s.fanManualPercent)
            .putInt("led_r", s.ledR)
            .putInt("led_g", s.ledG)
            .putInt("led_b", s.ledB)
            .apply()
    }
}
