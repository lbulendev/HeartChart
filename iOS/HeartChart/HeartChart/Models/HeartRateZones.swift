//
//  HeartRateZones.swift
//  HeartChart
//

import Foundation

/// Age-based heart rate guidance from the published table. Youth rows are
/// age RANGES (6–12, 13–19); adult rows are single ages. Lookup: the
/// bracket whose range contains the age wins; otherwise the nearest
/// bracket by distance to its range — a 27-year-old gets the 30-year
/// bracket — with ties going to the younger bracket.
nonisolated struct HeartRateZones: Equatable, Sendable {
    let age: Int

    struct Bracket: Equatable, Sendable {
        let ages: ClosedRange<Int>
        let low: Int
        let high: Int
        let max: Int
    }

    /// The published age table, ascending and non-overlapping.
    static let brackets: [Bracket] = [
        Bracket(ages: 6...12, low: 70, high: 110, max: 220),
        Bracket(ages: 13...19, low: 60, high: 100, max: 220),
        Bracket(ages: 20...20, low: 100, high: 170, max: 200),
        Bracket(ages: 30...30, low: 95, high: 162, max: 190),
        Bracket(ages: 35...35, low: 93, high: 157, max: 185),
        Bracket(ages: 40...40, low: 90, high: 153, max: 180),
        Bracket(ages: 45...45, low: 88, high: 149, max: 175),
        Bracket(ages: 50...50, low: 85, high: 145, max: 170),
        Bracket(ages: 55...55, low: 83, high: 140, max: 165),
        Bracket(ages: 60...60, low: 80, high: 136, max: 160),
        Bracket(ages: 65...65, low: 78, high: 132, max: 155),
        Bracket(ages: 70...70, low: 75, high: 128, max: 150),
    ]

    /// Ages outside this range make the published table meaningless.
    static let validAges = 6...100

    /// Containing range first; otherwise nearest by distance to the range.
    /// min(by:) keeps the earlier element on ties, so equidistant ages
    /// snap to the younger bracket.
    var bracket: Bracket {
        if let exact = Self.brackets.first(where: { $0.ages.contains(age) }) {
            return exact
        }
        return Self.brackets.min { distance(to: $0) < distance(to: $1) }!
    }

    private func distance(to bracket: Bracket) -> Int {
        if age < bracket.ages.lowerBound { return bracket.ages.lowerBound - age }
        if age > bracket.ages.upperBound { return age - bracket.ages.upperBound }
        return 0
    }

    var maxHeartRate: Int { bracket.max }

    /// Bottom of the target zone.
    var targetLow: Int { bracket.low }

    /// Top of the target zone.
    var targetHigh: Int { bracket.high }
}

/// One heart-rate reading from the sensor.
nonisolated struct HeartRateSample: Identifiable, Equatable, Sendable {
    let id = UUID()
    let date: Date
    let bpm: Int
}
