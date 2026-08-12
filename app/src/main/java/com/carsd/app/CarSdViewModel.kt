package com.carsd.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class UiState(
    val settings: CarSettings = CarSettings(),
    val profile: HardwareProfile? = null,
    val cpuTempC: Float? = null,
    val message: String = "آماده",
    val fanSpinning: Boolean = false
)

class CarSdViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = CarPrefs(app)
    private val audio = AudioController(app)
    private val _ui = MutableStateFlow(UiState(settings = prefs.load()))
    val ui = _ui.asStateFlow()

    init {
        detect()
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val p = _ui.value.profile
                if (p != null) {
                    val temp = HardwareProbe.readTemperatureC(p)
                    _ui.value = _ui.value.copy(cpuTempC = temp)
                    autoFanTick(temp, p)
                }
                delay(2500)
            }
        }
    }

    fun detect() {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.value = _ui.value.copy(message = "در حال شناسایی سخت‌افزار...")
            val p = HardwareProbe.scan()
            _ui.value = _ui.value.copy(
                profile = p,
                message = "شناسایی انجام شد"
            )
        }
    }

    fun updateSettings(block: (CarSettings) -> CarSettings) {
        _ui.value = _ui.value.copy(settings = block(_ui.value.settings))
    }

    fun saveAndApplyAudio() {
        val s = _ui.value.settings
        prefs.save(s)
        val r = audio.apply(s.mediaVolume, s.callVolume)
        _ui.value = _ui.value.copy(
            message = if (r.isSuccess) "صدای پیش‌فرض ذخیره و اعمال شد" else "اعمال صدا ناموفق بود: ${r.exceptionOrNull()?.message}"
        )
    }

    fun setManualFan(percent: Int) {
        val p = _ui.value.profile ?: return
        updateSettings { it.copy(fanManualPercent = percent, fanAuto = false) }
        prefs.save(_ui.value.settings)
        viewModelScope.launch(Dispatchers.IO) {
            val r = HardwareProbe.writeFanPercent(p, percent)
            _ui.value = _ui.value.copy(
                fanSpinning = r.isSuccess && percent > 0,
                message = if (r.isSuccess) "فن روی $percent٪ تنظیم شد" else "کنترل فن ممکن نشد: ${r.exceptionOrNull()?.message}"
            )
        }
    }

    fun setLedBrightness(percent: Int) {
        val p = _ui.value.profile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val r = HardwareProbe.writeButtonBrightness(p, percent)
            _ui.value = _ui.value.copy(
                message = if (r.isSuccess) "نور دکمه‌ها تنظیم شد" else "کنترل نور ممکن نشد: ${r.exceptionOrNull()?.message}"
            )
        }
    }

    private fun autoFanTick(temp: Float?, p: HardwareProfile) {
        val s = _ui.value.settings
        if (!s.fanAuto || temp == null || p.fanPwmPaths.isEmpty()) return
        val shouldOn = temp >= s.fanOnTempC
        val shouldOff = temp <= s.fanOffTempC
        when {
            shouldOn && !_ui.value.fanSpinning -> {
                HardwareProbe.writeFanPercent(p, s.fanManualPercent)
                _ui.value = _ui.value.copy(fanSpinning = true)
            }
            shouldOff && _ui.value.fanSpinning -> {
                HardwareProbe.writeFanPercent(p, 0)
                _ui.value = _ui.value.copy(fanSpinning = false)
            }
        }
    }
}
