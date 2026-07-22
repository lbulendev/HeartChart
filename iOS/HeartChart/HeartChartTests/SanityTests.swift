//
//  SanityTests.swift
//  HeartChartTests
//

import Foundation
import Testing
@testable import HeartChart

// Contract checks on helpers, defaults, and persistence keys. A failure
// here usually means the scaffolding drifted, not that the product broke.
@Suite("Sanity", .tags(.sanity))
struct SanityTests {

    @Suite("Zones", .tags(.zones))
    struct Zones {
        @Test("The valid age range covers children through seniors, rejects nonsense")
        func validAgeRange() {
            #expect(HeartRateZones.validAges.contains(6))
            #expect(HeartRateZones.validAges.contains(45))
            #expect(HeartRateZones.validAges.contains(100))
            #expect(!HeartRateZones.validAges.contains(5))
            #expect(!HeartRateZones.validAges.contains(101))
        }

        // The bracket lookup assumes an ascending, non-overlapping table;
        // a careless row edit would silently skew every match.
        @Test("The bracket table is ascending and non-overlapping")
        func bracketTableIsOrdered() {
            let brackets = HeartRateZones.brackets
            #expect(brackets.count == 12)
            for (earlier, later) in zip(brackets, brackets.dropFirst()) {
                #expect(earlier.ages.upperBound < later.ages.lowerBound)
            }
        }
    }

    @Suite("Samples", .tags(.parsing))
    struct Samples {
        @Test("Each sample gets a unique identity for chart marks")
        func uniqueIdentity() {
            let now = Date.now
            let a = HeartRateSample(date: now, bpm: 70)
            let b = HeartRateSample(date: now, bpm: 70)
            #expect(a.id != b.id)
        }
    }

    @Suite("Pairing persistence", .tags(.pairing))
    @MainActor
    struct PairingPersistence {
        @Test("A stored sensor id restores as paired on init")
        func restoresPairedSensor() {
            let defaults = makeIsolatedDefaults()
            let id = UUID()
            defaults.set(id.uuidString, forKey: "pairedSensorID")

            let monitor = makeMonitor(defaults: defaults)

            #expect(monitor.isPaired)
            #expect(monitor.pairedSensorID == id)
        }

        @Test("A corrupt stored id degrades to unpaired, not a crash")
        func corruptStoredID() {
            let defaults = makeIsolatedDefaults()
            defaults.set("not-a-uuid", forKey: "pairedSensorID")

            let monitor = makeMonitor(defaults: defaults)

            #expect(!monitor.isPaired)
        }
    }
}
