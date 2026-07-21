//
//  HeartRateZones.swift
//  HeartChart
//

import Foundation

/// Age-based heart rate guidance using the standard 220 − age estimate:
/// the target zone spans 50–85% of maximum. Matches the published table
/// (45y → 88–149, max 175; 50y → 85–145, max 170; 55y → 83–140, max 165).
nonisolated struct HeartRateZones: Equatable, Sendable {
    let age: Int

    /// Ages outside this range make the 220 − age estimate meaningless.
    static let validAges = 10...100

    var maxHeartRate: Int { 220 - age }

    /// Bottom of the target zone: 50% of maximum.
    var targetLow: Int { Int((Double(maxHeartRate) * 0.5).rounded()) }

    /// Top of the target zone: 85% of maximum.
    var targetHigh: Int { Int((Double(maxHeartRate) * 0.85).rounded()) }
}

/// One heart-rate reading from the sensor.
nonisolated struct HeartRateSample: Identifiable, Equatable, Sendable {
    let id = UUID()
    let date: Date
    let bpm: Int
}
