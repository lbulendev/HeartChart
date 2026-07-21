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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curiousbeagle.android.heartchart.R
import com.curiousbeagle.android.heartchart.data.HeartRateSample
import com.curiousbeagle.android.heartchart.data.HeartRateZones
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
    val lineColor = MaterialTheme.colorScheme.primary
    val labelStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Red)
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant

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
        val minBpm = minOf(40, (samples.minOfOrNull { it.bpm } ?: 60) - 10)
        val maxBpm = zones.maxHeartRate + 15
        val bpmRange = (maxBpm - minBpm).toFloat()
        fun yFor(bpm: Int): Float = size.height * (1f - (bpm - minBpm) / bpmRange)

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

        // The heart rate line over the time window ending now.
        if (samples.size >= 2) {
            val end = samples.maxOf { it.date }
            val start = end - windowMillis
            val path = Path()
            samples.filter { it.date >= start }.forEachIndexed { index, sample ->
                val x = size.width * (sample.date - start).toFloat() / windowMillis.toFloat()
                val y = yFor(sample.bpm)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path,
                color = lineColor,
                style = Stroke(width = with(density) { 2.dp.toPx() })
            )
        }

        // Minimal y-axis reference labels.
        val axisStyle = TextStyle(fontSize = 9.sp, color = axisColor)
        listOf(minBpm, (minBpm + maxBpm) / 2).forEach { bpm ->
            drawText(
                textMeasurer.measure("$bpm", axisStyle),
                topLeft = Offset(0f, (yFor(bpm) - 12f).coerceAtLeast(0f)),
            )
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
