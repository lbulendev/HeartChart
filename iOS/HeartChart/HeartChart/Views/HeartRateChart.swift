//
//  HeartRateChart.swift
//  HeartChart
//

import Charts
import SwiftUI

/// The live chart: heart rate over the rolling window, with red rules for
/// the age-based target zone (low/high) and a dashed red rule at the
/// estimated maximum. Stateless, so previews render without a sensor.
struct HeartRateChart: View {
    let samples: [HeartRateSample]
    let zones: HeartRateZones

    var body: some View {
        Chart {
            // Contiguous runs draw as separate series, so a sensor outage
            // (strap off) renders as a gap instead of a line across it.
            ForEach(Array(HeartRateSample.segments(of: samples).enumerated()), id: \.offset) { index, segment in
                ForEach(segment) { sample in
                    LineMark(
                        x: .value("Time", sample.date),
                        y: .value("BPM", sample.bpm),
                        series: .value("Run", index)
                    )
                    .foregroundStyle(.teal)
                    .interpolationMethod(.monotone)
                }
            }

            RuleMark(y: .value("Zone low", zones.targetLow))
                .foregroundStyle(.red)
                .annotation(position: .top, alignment: .leading) {
                    zoneLabel(String(localized: "chart_low_label", defaultValue: "Low \(zones.targetLow)"))
                }

            RuleMark(y: .value("Zone high", zones.targetHigh))
                .foregroundStyle(.red)
                .annotation(position: .top, alignment: .leading) {
                    zoneLabel(String(localized: "chart_high_label", defaultValue: "High \(zones.targetHigh)"))
                }

            RuleMark(y: .value("Max", zones.maxHeartRate))
                .foregroundStyle(.red)
                .lineStyle(StrokeStyle(lineWidth: 1.5, dash: [6, 4]))
                .annotation(position: .top, alignment: .leading) {
                    zoneLabel(String(localized: "chart_max_label", defaultValue: "Max \(zones.maxHeartRate)"))
                }
        }
        .chartYScale(domain: zones.chartFloor...zones.chartCeiling)
        .chartYAxis {
            // Round-number gridlines across the whole domain (see axisTicks).
            AxisMarks(values: zones.axisTicks) { _ in
                AxisGridLine()
                AxisValueLabel()
            }
        }
        .chartYAxisLabel(String(localized: "bpm_unit", defaultValue: "BPM"))
        .chartXAxis {
            AxisMarks(values: .automatic(desiredCount: 4)) { _ in
                AxisGridLine()
                AxisValueLabel(format: .dateTime.hour().minute().second())
            }
        }
        .accessibilityLabel(String(localized: "chart_accessibility_label", defaultValue: "Heart rate chart"))
    }

    private func zoneLabel(_ text: String) -> some View {
        Text(text)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(.red)
    }
}

#if DEBUG
private func previewSamples() -> [HeartRateSample] {
    (0..<120).map { second in
        HeartRateSample(
            date: Date(timeIntervalSinceNow: Double(second - 120)),
            bpm: 80 + Int(55 * sin(Double(second) / 40)) + Int.random(in: -3...3)
        )
    }
}

#Preview("Workout ramp") {
    HeartRateChart(samples: previewSamples(), zones: HeartRateZones(age: 45))
        .padding()
}

#Preview("Empty") {
    HeartRateChart(samples: [], zones: HeartRateZones(age: 55))
        .padding()
}

#Preview("Small iPhone") {
    HeartRateChart(samples: previewSamples(), zones: HeartRateZones(age: 45))
        .padding()
        .previewDevice(PreviewDevice(rawValue: "iPhone 17e"))
}

#Preview("iPad") {
    HeartRateChart(samples: previewSamples(), zones: HeartRateZones(age: 50))
        .padding()
        .previewDevice(PreviewDevice(rawValue: "iPad Pro 13-inch (M4)"))
}
#endif
