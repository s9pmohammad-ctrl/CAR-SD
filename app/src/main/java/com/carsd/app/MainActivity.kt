package com.carsd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CarSdApp() }
    }
}

private val Bg = Color(0xFF080B10)
private val Card = Color(0xFF111722)
private val Accent = Color(0xFF46D7FF)
private val TextDim = Color(0xFF9AA7B7)

@Composable
fun CarSdApp(vm: CarSdViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Accent,
                background = Bg,
                surface = Card
            )
        ) {
            Surface(Modifier.fillMaxSize(), color = Bg) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Header(ui, vm)
                    AudioCard(ui, vm)
                    FanCard(ui, vm)
                    ButtonLightCard(ui, vm)
                    McuCard(ui, vm)
                    DiagnosticsCard(ui)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(ui: UiState, vm: CarSdViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1520)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CAR SD", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("کنترل هوشمند مانیتور خودرو", color = TextDim)
                }
                AssistChip(onClick = { vm.detect() }, label = { Text("شناسایی مجدد") })
            }
            Text(ui.message, color = Accent)
        }
    }
}

@Composable
private fun AudioCard(ui: UiState, vm: CarSdViewModel) {
    SectionCard("صدای پیش‌فرض") {
        Text("صدای موزیک هنگام روشن شدن", color = TextDim)
        SliderLine(ui.settings.mediaVolume, 0..30) {
            vm.updateSettings { s -> s.copy(mediaVolume = it) }
        }
        Text("صدای مکالمه", color = TextDim)
        SliderLine(ui.settings.callVolume, 0..15) {
            vm.updateSettings { s -> s.copy(callVolume = it) }
        }
        Button(onClick = vm::saveAndApplyAudio, modifier = Modifier.fillMaxWidth()) {
            Text("ذخیره و اعمال")
        }
        Text(
            "اگر رام دستگاه در بوت دوباره صدا را روی ۱۰ می‌گذارد، CAR SD بعد از Boot Completed مقدار ذخیره‌شده را دوباره اعمال می‌کند.",
            color = TextDim,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun FanCard(ui: UiState, vm: CarSdViewModel) {
    val p = ui.profile
    SectionCard("فن هوشمند") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            FanGraphic(ui.fanSpinning)
        }
        Text(
            "دمای فعلی: ${ui.cpuTempC?.let { "%.1f °C".format(it) } ?: "نامشخص"}",
            color = if (ui.cpuTempC != null) Accent else TextDim
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("حالت خودکار", modifier = Modifier.weight(1f))
            Switch(
                checked = ui.settings.fanAuto,
                onCheckedChange = { enabled -> vm.updateSettings { it.copy(fanAuto = enabled) } }
            )
        }
        Text("دمای روشن شدن: ${ui.settings.fanOnTempC}°", color = TextDim)
        Slider(
            value = ui.settings.fanOnTempC.toFloat(),
            onValueChange = { v -> vm.updateSettings { it.copy(fanOnTempC = v.toInt()) } },
            valueRange = 40f..90f
        )
        Text("قدرت فن دستی: ${ui.settings.fanManualPercent}٪", color = TextDim)
        Slider(
            value = ui.settings.fanManualPercent.toFloat(),
            onValueChange = { v -> vm.updateSettings { it.copy(fanManualPercent = v.toInt()) } },
            onValueChangeFinished = { vm.setManualFan(ui.settings.fanManualPercent) },
            valueRange = 0f..100f
        )
        Text(
            if (p?.fanDetected == true) "مسیر فن پیدا شد" else "مسیر استاندارد فن پیدا نشد؛ احتمالاً کنترل فن از MCU اختصاصی Helix انجام می‌شود.",
            color = if (p?.fanDetected == true) Accent else TextDim
        )
    }
}

@Composable
private fun FanGraphic(spinning: Boolean) {
    val transition = rememberInfiniteTransition(label = "fan")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (spinning) 700 else 120000, easing = LinearEasing)
        ),
        label = "angle"
    )
    Canvas(Modifier.size(190.dp)) {
        val c = center
        drawCircle(Color(0xFF1A2534), radius = size.minDimension * .46f)
        drawCircle(Color(0xFF0B1018), radius = size.minDimension * .37f)
        rotate(angle, c) {
            repeat(7) { i ->
                val a = Math.toRadians((i * (360.0 / 7.0)))
                val start = Offset(
                    c.x + cos(a).toFloat() * size.minDimension * .09f,
                    c.y + sin(a).toFloat() * size.minDimension * .09f
                )
                val end = Offset(
                    c.x + cos(a + .65).toFloat() * size.minDimension * .34f,
                    c.y + sin(a + .65).toFloat() * size.minDimension * .34f
                )
                drawLine(Accent, start, end, strokeWidth = 18f, cap = StrokeCap.Round)
            }
        }
        drawCircle(Color(0xFFD9F8FF), radius = size.minDimension * .055f)
    }
}

@Composable
private fun ButtonLightCard(ui: UiState, vm: CarSdViewModel) {
    val detected = ui.profile?.ledDetected == true
    SectionCard("نور دکمه‌های فیزیکی") {
        Text(
            if (detected) "LED استاندارد شناسایی شد." else "LED استاندارد شناسایی نشد؛ احتمال کنترل توسط MCU وجود دارد.",
            color = if (detected) Accent else TextDim
        )
        Text("روشنایی", color = TextDim)
        var brightness by remember { mutableFloatStateOf(70f) }
        Slider(
            value = brightness,
            onValueChange = { brightness = it },
            onValueChangeFinished = { vm.setLedBrightness(brightness.toInt()) },
            valueRange = 0f..100f
        )
        Text(
            "تغییر RGB واقعی فقط وقتی فعال می‌شود که مسیر RGB یا فرمان MCU دستگاه مشخص شود. این نسخه فعلاً روشنایی مسیرهای استاندارد sysfs را کنترل می‌کند.",
            style = MaterialTheme.typography.bodySmall,
            color = TextDim
        )
    }
}

@Composable
private fun McuCard(ui: UiState, vm: CarSdViewModel) {
    val fp = ui.fingerprint
    SectionCard("MCU Match / T440") {
        Text("پروفایل: ${fp?.matchedProfile ?: "نامشخص"}", color = if (fp?.matchedProfile != null) Accent else TextDim)
        Text("اطمینان تطبیق: ${fp?.confidence ?: 0}٪", color = TextDim)
        fp?.reasons?.take(5)?.forEach { Text("• $it", color = TextDim) }
        Button(onClick = vm::exportReport, modifier = Modifier.fillMaxWidth()) { Text("ساخت گزارش کامل T440") }
        ui.reportPath?.let { Text("مسیر گزارش:\n$it", color = TextDim, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DiagnosticsCard(ui: UiState) {
    val p = ui.profile
    SectionCard("تشخیص سخت‌افزار") {
        Status("Root", p?.root == true)
        Status("سنسور دما", !p?.temperaturePaths.isNullOrEmpty())
        Status("فن / PWM", p?.fanDetected == true)
        Status("نور کلیدها", p?.ledDetected == true)
        if (p != null) {
            Text("Temp paths: ${p.temperaturePaths.size}", color = TextDim)
            Text("PWM paths: ${p.fanPwmPaths.size}", color = TextDim)
            Text("LED paths: ${p.ledPaths.size}", color = TextDim)
        }
    }
}

@Composable
private fun Status(label: String, ok: Boolean) {
    Row {
        Text(label, Modifier.weight(1f))
        Text(if (ok) "✓ شناسایی شد" else "—", color = if (ok) Accent else TextDim)
    }
}

@Composable
private fun SliderLine(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(value.toString(), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Card),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                content()
            }
        )
    }
}
