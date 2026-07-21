//
//  PairingView.swift
//  HeartChart
//

import SwiftUI

/// Scans for heart rate sensors and pairs the one the user taps.
struct PairingView: View {
    let monitor: HeartRateMonitor
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Group {
                if monitor.state == .bluetoothUnavailable {
                    ContentUnavailableView(
                        String(localized: "pairing_bt_off_title", defaultValue: "Bluetooth Is Off"),
                        systemImage: "antenna.radiowaves.left.and.right.slash",
                        description: Text(String(
                            localized: "pairing_bt_off_message",
                            defaultValue: "Turn on Bluetooth to find your heart rate sensor."
                        ))
                    )
                } else if monitor.discovered.isEmpty {
                    ContentUnavailableView {
                        Label(
                            String(localized: "pairing_searching", defaultValue: "Searching…"),
                            systemImage: "sensor.tag.radiowaves.forward"
                        )
                    } description: {
                        Text(String(
                            localized: "pairing_hint",
                            defaultValue: "Moisten the electrodes and wear the strap — the Polar H9 only advertises while worn."
                        ))
                    }
                } else {
                    List(monitor.discovered) { sensor in
                        Button {
                            monitor.pair(sensor)
                            dismiss()
                        } label: {
                            Label(sensor.name, systemImage: "heart.fill")
                        }
                    }
                }
            }
            .navigationTitle(Text(String(localized: "pairing_title", defaultValue: "Pair Sensor")))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "cancel", defaultValue: "Cancel")) { dismiss() }
                }
            }
        }
        .onAppear { monitor.startScanning() }
        .onDisappear { monitor.stopScanning() }
    }
}

#if DEBUG
private func previewMonitor() -> HeartRateMonitor {
    HeartRateMonitor(defaults: UserDefaults(suiteName: "HeartChart.previews")!)
}

#Preview {
    PairingView(monitor: previewMonitor())
}

#Preview("Small iPhone") {
    PairingView(monitor: previewMonitor())
        .previewDevice(PreviewDevice(rawValue: "iPhone 17e"))
}

#Preview("iPad") {
    PairingView(monitor: previewMonitor())
        .previewDevice(PreviewDevice(rawValue: "iPad Pro 13-inch (M4)"))
}
#endif
