//
//  AgeEntryView.swift
//  HeartChart
//

import SwiftUI

/// Collects the user's age, which drives every zone line on the chart.
/// Shown automatically on first launch and editable any time after.
struct AgeEntryView: View {
    @Binding var age: Int
    @Environment(\.dismiss) private var dismiss

    @State private var selectedAge: Int

    init(age: Binding<Int>) {
        _age = age
        _selectedAge = State(initialValue: HeartRateZones.validAges.contains(age.wrappedValue) ? age.wrappedValue : 45)
    }

    private var zones: HeartRateZones { HeartRateZones(age: selectedAge) }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker(String(localized: "age_title", defaultValue: "Age"), selection: $selectedAge) {
                        ForEach(HeartRateZones.validAges, id: \.self) { value in
                            Text("\(value)").tag(value)
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
                        age = selectedAge
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

#Preview("Small iPhone") {
    AgeEntryView(age: .constant(45))
        .previewDevice(PreviewDevice(rawValue: "iPhone 17e"))
}

#Preview("iPad") {
    AgeEntryView(age: .constant(55))
        .previewDevice(PreviewDevice(rawValue: "iPad Pro 13-inch (M4)"))
}
#endif
