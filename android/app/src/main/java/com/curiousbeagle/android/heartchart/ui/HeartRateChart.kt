package com.curiousbeagle.android.heartchart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiousbeagle.android.heartchart.R
import com.curiousbeagle.android.heartchart.data.HeartRateSample
import com.curiousbeagle.android.heartchart.data.HeartRateZones
import com.curiousbeagle.android.heartchart.data.segments
import com.curiousbeagle.android.heartchart.ui.theme.HeartChartTheme
import kotlin.math.sin

/**
 * The port of the iOS Swift Charts view, drawn with Compose Canvas (no
 * third-party chart library): heart rate over the rolling window, red rules
 * for the age-based target zone, and a dashed red rule at the estimated max.
 * Stateless, so previews render without a sensor.
 */
@Composable
fun HeartRateChart(
    samples: List<HeartRateSample>,
    zones: HeartRateZones,
    modifier: Modifier = Modifier,
    windowMillis: Long = 3 * 60 * 1000,
) {
    val textMeasurer = rememberTextMeasurer()
    // Teal: clearly distinct from the red zone/max rules in light and dark.
    val lineColor = Color(0xFF00897B)
    val labelStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val lowLabel = stringResource(R.string.chart_low_label, zones.targetLow)
    val highLabel = stringResource(R.string.chart_high_label, zones.targetHigh)
    val maxLabel = stringResource(R.string.chart_max_label, zones.maxHeartRate)
    val chartDescription = stringResource(R.string.chart_accessibility_label)

    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
            .semantics { contentDescription = chartDescription }
    ) {
        val minBpm = zones.chartFloor
        val maxBpm = zones.chartCeiling
        val bpmRange = (maxBpm - minBpm).toFloat()
        fun yFor(bpm: Int): Float = size.height * (1f - (bpm - minBpm) / bpmRange)

        // Round-number gridlines across the whole domain (see axisTicks).
        val axisStyle = TextStyle(fontSize = 9.sp, color = axisColor)
        zones.axisTicks.forEach { value ->
            drawRule(yFor(value), gridColor)
            drawText(
                textMeasurer.measure("$value", axisStyle),
                topLeft = Offset(0f, (yFor(value) - 12f).coerceAtLeast(0f)),
            )
        }

        // Zone rules: solid red low/high, dashed red max — with labels.
        drawRule(yFor(zones.targetLow), Color.Red)
        drawRule(yFor(zones.targetHigh), Color.Red)
        drawRule(
            yFor(zones.maxHeartRate), Color.Red,
            PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
        )
        drawLabel(textMeasurer, lowLabel, yFor(zones.targetLow), labelStyle)
        drawLabel(textMeasurer, highLabel, yFor(zones.targetHigh), labelStyle)
        drawLabel(textMeasurer, maxLabel, yFor(zones.maxHeartRate), labelStyle)

        // The heart rate line over the time window ending now. Contiguous
        // runs draw as separate paths, so a sensor outage (strap off)
        // renders as a gap instead of a line across it.
        if (samples.size >= 2) {
            val end = samples.maxOf { it.date }
            val start = end - windowMillis
            val strokeWidth = with(density) { 2.dp.toPx() }
            samples.filter { it.date >= start }.segments().forEach { segment ->
                val path = Path()
                segment.forEachIndexed { index, sample ->
                    val x = size.width * (sample.date - start).toFloat() / windowMillis.toFloat()
                    val y = yFor(sample.bpm)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = lineColor, style = Stroke(width = strokeWidth))
            }
        }
    }
}

private fun DrawScope.drawRule(y: Float, color: Color, dash: PathEffect? = null) {
    drawLine(
        color = color,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 2f,
        pathEffect = dash,
    )
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    y: Float,
    style: TextStyle,
) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(24f, (y - layout.size.height - 2f).coerceAtLeast(0f)))
}

// MARK: Previews

private fun previewSamples(): List<HeartRateSample> {
    val now = System.currentTimeMillis()
    return (0 until 120).map { second ->
        HeartRateSample(
            date = now - (120 - second) * 1000L,
            bpm = 80 + (55 * sin(second / 40.0)).toInt() + (-3..3).random(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HeartRateChartPreview() {
    HeartChartTheme {
        HeartRateChart(samples = previewSamples(), zones = HeartRateZones(age = 45))
    }
}

@Preview(name = "Empty", showBackground = true)
@Composable
private fun HeartRateChartEmptyPreview() {
    HeartChartTheme {
        HeartRateChart(samples = emptyList(), zones = HeartRateZones(age = 55))
    }
}

@Preview(name = "Small phone", showBackground = true, device = "spec:width=320dp,height=568dp,dpi=320")
@Composable
private fun HeartRateChartSmallPreview() {
    HeartChartTheme {
        HeartRateChart(samples = previewSamples(), zones = HeartRateZones(age = 45))
    }
}

@Preview(name = "Tablet", showBackground = true, device = Devices.TABLET)
@Composable
private fun HeartRateChartTabletPreview() {
    HeartChartTheme {
        HeartRateChart(samples = previewSamples(), zones = HeartRateZones(age = 50))
    }
}
