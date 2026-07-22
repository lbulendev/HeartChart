//
//  AgeEntryView.swift
//  HeartChart
//

import SwiftUI

/// Collects the user's age bracket, which drives every zone line on the
/// chart. The picker offers the published table's rows as whole ranges
/// ("6–12", "13–19", "20" … "70"); the bracket's first age is what
/// persists, so previously stored exact ages still resolve.
struct AgeEntryView: View {
    @Binding var age: Int
    @Environment(\.dismiss) private var dismiss

    @State private var selectedBracketStart: Int

    init(age: Binding<Int>) {
        _age = age
        let current = age.wrappedValue
        let start = HeartRateZones.validAges.contains(current)
            ? HeartRateZones(age: current).bracket.ages.lowerBound
            : 45
        _selectedBracketStart = State(initialValue: start)
    }

    private var zones: HeartRateZones { HeartRateZones(age: selectedBracketStart) }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker(
                        String(localized: "age_title", defaultValue: "Age"),
                        selection: $selectedBracketStart
                    ) {
                        ForEach(HeartRateZones.brackets, id: \.ages.lowerBound) { bracket in
                            Text(HeartRateZones.label(for: bracket)).tag(bracket.ages.lowerBound)
                        }
                    }
                    .pickerStyle(.wheel)
                } header: {
                    Text(String(localized: "age_section", defaultValue: "Your age"))
                } footer: {
                    Text(String(
                        localized: "age_footer",
                        defaultValue: "Age sets the target zone and maximum heart rate lines on the chart."
                    ))
                }

                Section(String(localized: "zones_section", defaultValue: "Your zones")) {
                    LabeledContent(
                        String(localized: "target_zone_label", defaultValue: "Target zone"),
                        value: String(localized: "zone_range_value", defaultValue: "\(zones.targetLow)–\(zones.targetHigh) bpm")
                    )
                    LabeledContent(
                        String(localized: "max_heart_rate_label", defaultValue: "Max heart rate"),
                        value: String(localized: "bpm_value", defaultValue: "\(zones.maxHeartRate) bpm")
                    )
                }
            }
            .navigationTitle(Text(String(localized: "age_title", defaultValue: "Age")))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "save", defaultValue: "Save")) {
                        age = selectedBracketStart
                        dismiss()
                    }
                }
            }
        }
    }
}

#if DEBUG
#Preview {
    AgeEntryView(age: .constant(45))
}

#Preview("Youth bracket") {
    AgeEntryView(age: .constant(9))
}

#Preview("Small iPhone") {
    AgeEntryView(age: .constant(45))
        .previewDevice(PreviewDevice(rawValue: "iPhone 17e"))
}

#Preview("iPad") {
    AgeEntryView(age: .constant(55))
        .previewDevice(PreviewDevice(rawValue: "iPad Pro 13-inch (M4)"))
}
#endif
