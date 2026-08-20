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
    val fingerprint: McuFingerprint? = null,
    val cpuTempC: Float? = null,
    val message: String = "آماده",
    val fanSpinning: Boolean = false,
    val reportPath: String? = null
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
                _ui.value.profile?.let { p ->
                    val t = HardwareProbe.readTemperatureC(p)
                    _ui.value = _ui.value.copy(cpuTempC = t)
                    autoFanTick(t, p)
                }
                delay(2500)
            }
        }
    }

    fun detect() = viewModelScope.launch(Dispatchers.IO) {
        _ui.value = _ui.value.copy(message = "در حال تطبیق MCU...")
        val hw = HardwareProbe.scan()
        val fp = McuMatcher.scan()
        _ui.value = _ui.value.copy(profile = hw, fingerprint = fp,
            message = fp.matchedProfile?.let { "MCU Match: $it (${fp.confidence}٪)" }
                ?: "پروفایل MCU هنوز قطعی نیست")
    }

    fun exportReport() = viewModelScope.launch(Dispatchers.IO) {
        val fp = _ui.value.fingerprint ?: return@launch
        val f = DiagnosticReport.build(getApplication(), fp, _ui.value.profile)
        _ui.value = _ui.value.copy(reportPath = f.absolutePath, message = "گزارش ساخته شد: ${f.name}")
    }

    fun updateSettings(block: (CarSettings) -> CarSettings) { _ui.value = _ui.value.copy(settings = block(_ui.value.settings)) }

    fun saveAndApplyAudio() {
        val s = _ui.value.settings
        prefs.save(s)
        val r = audio.apply(s.mediaVolume, s.callVolume)
        _ui.value = _ui.value.copy(message = if (r.isSuccess) "صدای Android ذخیره شد" else "اعمال صدا ناموفق بود")
    }

    fun setManualFan(percent: Int) {
        val p = _ui.value.profile ?: return
        updateSettings { it.copy(fanManualPercent = percent, fanAuto = false) }
        prefs.save(_ui.value.settings)
        viewModelScope.launch(Dispatchers.IO) {
            val r = HardwareProbe.writeFanPercent(p, percent)
            _ui.value = _ui.value.copy(fanSpinning = r.isSuccess && percent > 0,
                message = if (r.isSuccess) "فن sysfs روی $percent٪ تنظیم شد" else "کنترل MCU هنوز قفل است؛ گزارش لازم است")
        }
    }

    fun setLedBrightness(percent: Int) {
        val p = _ui.value.profile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val r = HardwareProbe.writeButtonBrightness(p, percent)
            _ui.value = _ui.value.copy(message = if (r.isSuccess) "نور تنظیم شد" else "LED احتمالاً از MCU کنترل می‌شود")
        }
    }

    private fun autoFanTick(temp: Float?, p: HardwareProfile) {
        val s = _ui.value.settings
        if (!s.fanAuto || temp == null || p.fanPwmPaths.isEmpty()) return
        if (temp >= s.fanOnTempC && !_ui.value.fanSpinning) {
            HardwareProbe.writeFanPercent(p, s.fanManualPercent); _ui.value = _ui.value.copy(fanSpinning = true)
        } else if (temp <= s.fanOffTempC && _ui.value.fanSpinning) {
            HardwareProbe.writeFanPercent(p, 0); _ui.value = _ui.value.copy(fanSpinning = false)
        }
    }
}
