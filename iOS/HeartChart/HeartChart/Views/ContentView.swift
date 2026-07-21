//
//  ContentView.swift
//  HeartChart
//
//  Created by larrybulen on 7/21/26.
//

import SwiftUI

struct ContentView: View {
    @Environment(HeartRateMonitor.self) private var monitor
    @AppStorage("userAge") private var age = 0

    @State private var showingAgeEntry = false
    @State private var showingPairing = false
    @State private var confirmingUnpair = false

    private var zones: HeartRateZones { HeartRateZones(age: age) }
    private var hasAge: Bool { HeartRateZones.validAges.contains(age) }

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                currentReading
                if hasAge {
                    HeartRateChart(samples: monitor.samples, zones: zones)
                        .frame(maxHeight: .infinity)
                }
                statusFooter
            }
            .padding()
            .navigationTitle(Text(String(localized: "app_title", defaultValue: "HeartChart")))
            .overlay(alignment: .top) {
                if let error = monitor.connectionError {
                    ErrorBanner(error: error) {
                        monitor.retryConnection()
                    }
                    .transition(.move(edge: .top).combined(with: .opacity))
                }
            }
            .animation(.easeInOut(duration: 0.25), value: monitor.connectionError)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        showingAgeEntry = true
                    } label: {
                        Label(
                            hasAge
                                ? String(localized: "age_with_value", defaultValue: "Age \(age)")
                                : String(localized: "set_age", defaultValue: "Set Age"),
                            systemImage: "person.crop.circle"
                        )
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    if monitor.isPaired {
                        Button(
                            String(localized: "unpair_button", defaultValue: "Unpair"),
                            systemImage: "personalhotspot.slash"
                        ) {
                            confirmingUnpair = true
                        }
                    } else {
                        Button(
                            String(localized: "pair_button", defaultValue: "Pair"),
                            systemImage: "sensor.tag.radiowaves.forward"
                        ) {
                            showingPairing = true
                        }
                    }
                }
            }
            .sheet(isPresented: $showingAgeEntry) {
                AgeEntryView(age: $age)
            }
            .sheet(isPresented: $showingPairing) {
                PairingView(monitor: monitor)
            }
            .confirmationDialog(
                String(localized: "unpair_confirm_title", defaultValue: "Unpair this sensor?"),
                isPresented: $confirmingUnpair,
                titleVisibility: .visible
            ) {
                Button(
                    String(localized: "unpair_button", defaultValue: "Unpair"),
                    role: .destructive
                ) { monitor.unpair() }
            }
            .onAppear {
                if !hasAge { showingAgeEntry = true }
            }
        }
    }

    private var currentReading: some View {
        HStack(spacing: 12) {
            Image(systemName: "heart.fill")
                .font(.system(size: 44))
                .foregroundStyle(.red)
                .symbolEffect(.pulse, isActive: monitor.currentHeartRate != nil)
            VStack(alignment: .leading) {
                Text(monitor.currentHeartRate.map(String.init) ?? "--")
                    .font(.system(size: 56, weight: .bold, design: .rounded))
                    .contentTransition(.numericText())
                Text(String(localized: "bpm_unit", defaultValue: "BPM"))
                    .font(.headline)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var statusFooter: some View {
        switch monitor.state {
        case .bluetoothUnavailable:
            statusLabel(
                String(localized: "status_bluetooth_off", defaultValue: "Bluetooth is off"),
                systemImage: "antenna.radiowaves.left.and.right.slash"
            )
        case .idle:
            statusLabel(
                String(localized: "status_not_paired", defaultValue: "No sensor paired — tap Pair to find your Polar H9"),
                systemImage: "sensor.tag.radiowaves.forward"
            )
        case .scanning:
            statusLabel(
                String(localized: "status_searching", defaultValue: "Searching for sensors…"),
                systemImage: "magnifyingglass"
            )
        case .connecting(let name):
            statusLabel(
                String(localized: "status_connecting", defaultValue: "Connecting to \(name)…"),
                systemImage: "dot.radiowaves.left.and.right"
            )
        case .connected(let name):
            statusLabel(
                String(localized: "status_connected", defaultValue: "Connected to \(name)"),
                systemImage: "checkmark.circle.fill"
            )
        case .reconnecting:
            statusLabel(
                String(localized: "status_reconnecting", defaultValue: "Sensor out of range — reconnecting…"),
                systemImage: "arrow.triangle.2.circlepath"
            )
        }
    }

    private func statusLabel(_ text: String, systemImage: String) -> some View {
        Label(text, systemImage: systemImage)
            .font(.footnote)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .leading)
    }
}

#if DEBUG
#Preview {
    ContentView()
        .environment(HeartRateMonitor())
}

#Preview("Small iPhone") {
    ContentView()
        .environment(HeartRateMonitor())
        .previewDevice(PreviewDevice(rawValue: "iPhone 17e"))
}

#Preview("iPad") {
    ContentView()
        .environment(HeartRateMonitor())
        .previewDevice(PreviewDevice(rawValue: "iPad Pro 13-inch (M4)"))
}
#endif
