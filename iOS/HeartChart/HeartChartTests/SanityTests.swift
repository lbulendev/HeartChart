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

        // The chart's y-domain is the zone lines plus 20 bpm of margin —
        // never down to zero — with gridlines at a readable stride.
        @Test("Chart scale spans low−20 to max+20 with a readable stride")
        func chartScale() {
            let adult = HeartRateZones(age: 45)   // low→max span 87 → stride 15
            #expect(adult.chartFloor == 68)
            #expect(adult.chartCeiling == 195)
            #expect(adult.axisStride == 15)

            let twenty = HeartRateZones(age: 20)  // span 100 → stride 15
            #expect(twenty.axisStride == 15)

            let child = HeartRateZones(age: 8)    // span 150 → stride 20
            #expect(child.chartFloor == 50)
            #expect(child.chartCeiling == 240)
            #expect(child.axisStride == 20)
        }

        // Gridline labels must land on round numbers (140/145, never 143):
        // every tick is a multiple of the stride, inside the domain.
        @Test("Axis ticks land on round numbers within the domain")
        func axisTicksAreRound() {
            let fiftyFive = HeartRateZones(age: 55)  // floor 63, ceiling 185, stride 15
            #expect(fiftyFive.axisTicks == [75, 90, 105, 120, 135, 150, 165, 180])

            let child = HeartRateZones(age: 8)       // floor 50, ceiling 240, stride 20
            #expect(child.axisTicks.first == 60)
            #expect(child.axisTicks.last == 240)

            for zones in [fiftyFive, child, HeartRateZones(age: 20)] {
                for tick in zones.axisTicks {
                    #expect(tick % 5 == 0)
                    #expect(tick >= zones.chartFloor && tick <= zones.chartCeiling)
                }
            }
        }

        // Picker labels show each bracket's full covered span: single-age
        // rows extend to the next row's start; the last row is open-ended.
        @Test("Bracket labels render full covered ranges")
        func bracketLabels() {
            #expect(HeartRateZones(age: 8).bracketLabel == "6–12")
            #expect(HeartRateZones(age: 20).bracketLabel == "20–29")
            #expect(HeartRateZones(age: 45).bracketLabel == "45–49")
            #expect(HeartRateZones(age: 65).bracketLabel == "65–69")
            #expect(HeartRateZones(age: 80).bracketLabel == "70+")
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
