//
//  SmokeTests.swift
//  HeartChartTests
//

import Foundation
import Testing
@testable import HeartChart

// Critical path only: compute zones for an age, decode a typical sensor
// payload, land a reading in the monitor. If anything here fails, stop and
// fix before reading other results.
@Suite("Smoke", .tags(.smoke))
struct SmokeTests {

    @Suite("Zones", .tags(.zones))
    struct Zones {
        @Test("A 45-year-old gets the published zone lines")
        func computesZones() {
            let zones = HeartRateZones(age: 45)

            #expect(zones.targetLow == 88)
            #expect(zones.targetHigh == 149)
            #expect(zones.maxHeartRate == 175)
        }
    }

    @Suite("Parsing", .tags(.parsing))
    struct Parsing {
        @Test("A typical 8-bit measurement decodes")
        func decodesTypicalMeasurement() {
            #expect(HeartRateMonitor.heartRate(from: Data([0x00, 72])) == 72)
        }
    }

    @Suite("Monitor", .tags(.pairing))
    @MainActor
    struct Monitor {
        @Test("A fresh monitor starts unpaired with no readings")
        func startsClean() {
            let monitor = makeMonitor()

            #expect(!monitor.isPaired)
            #expect(monitor.currentHeartRate == nil)
            #expect(monitor.samples.isEmpty)
            #expect(monitor.discovered.isEmpty)
        }

        @Test("A recorded reading updates the current rate and the chart window")
        func recordsReading() {
            let monitor = makeMonitor()

            monitor.record(72)

            #expect(monitor.currentHeartRate == 72)
            #expect(monitor.samples.map(\.bpm) == [72])
        }
    }
}
