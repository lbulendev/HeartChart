//
//  RegressionTests.swift
//  HeartChartTests
//

import Foundation
import Testing
@testable import HeartChart

// Pinned edge cases. Each test documents the specific failure it prevents
// from coming back.
@Suite("Regression", .tags(.regression))
struct RegressionTests {

    @Suite("Zones", .tags(.zones))
    struct Zones {
        struct AgeCase: Sendable, CustomTestStringConvertible {
            let age: Int
            let low: Int
            let high: Int
            let max: Int
            var testDescription: String { "\(age) years" }
        }

        // The published table the chart's red lines must match, verbatim.
        @Test("Zones match the published age table", arguments: [
            AgeCase(age: 8, low: 70, high: 110, max: 220),
            AgeCase(age: 16, low: 60, high: 100, max: 220),
            AgeCase(age: 20, low: 100, high: 170, max: 200),
            AgeCase(age: 30, low: 95, high: 162, max: 190),
            AgeCase(age: 35, low: 93, high: 157, max: 185),
            AgeCase(age: 40, low: 90, high: 153, max: 180),
            AgeCase(age: 45, low: 88, high: 149, max: 175),
            AgeCase(age: 50, low: 85, high: 145, max: 170),
            AgeCase(age: 55, low: 83, high: 140, max: 165),
            AgeCase(age: 60, low: 80, high: 136, max: 160),
            AgeCase(age: 65, low: 78, high: 132, max: 155),
            AgeCase(age: 70, low: 75, high: 128, max: 150),
        ])
        func zonesMatchTable(testCase: AgeCase) {
            let zones = HeartRateZones(age: testCase.age)

            #expect(zones.targetLow == testCase.low)
            #expect(zones.targetHigh == testCase.high)
            #expect(zones.maxHeartRate == testCase.max)
        }

        struct NearestCase: Sendable, CustomTestStringConvertible {
            let age: Int
            let bracketStart: Int
            var testDescription: String { "\(age) → bracket starting \(bracketStart)" }
        }

        // A containing range wins outright; other ages snap to the NEAREST
        // bracket — never interpolate — with ties going to the younger one.
        @Test("Ages resolve to the containing or nearest bracket", arguments: [
            NearestCase(age: 6, bracketStart: 6),    // youth range boundaries
            NearestCase(age: 12, bracketStart: 6),
            NearestCase(age: 13, bracketStart: 13),
            NearestCase(age: 19, bracketStart: 13),
            NearestCase(age: 24, bracketStart: 20),
            NearestCase(age: 27, bracketStart: 30),
            NearestCase(age: 25, bracketStart: 20),  // tie → younger
            NearestCase(age: 33, bracketStart: 35),
            NearestCase(age: 68, bracketStart: 70),
            NearestCase(age: 5, bracketStart: 6),    // below the table clamps up
            NearestCase(age: 100, bracketStart: 70), // above the table clamps down
        ])
        func nearestBracket(testCase: NearestCase) {
            #expect(HeartRateZones(age: testCase.age).bracket.ages.lowerBound == testCase.bracketStart)
        }
    }

    @Suite("Parsing", .tags(.parsing))
    struct Parsing {
        // The H9 switches to the 16-bit format above 255 bpm (and some
        // sensors always use it); flag bit 0 selects it, little-endian.
        @Test("A 16-bit measurement decodes little-endian")
        func decodesUInt16() {
            #expect(HeartRateMonitor.heartRate(from: Data([0x01, 0x2C, 0x01])) == 300)
        }

        // Real payloads carry energy-expended and RR-interval fields after
        // the heart rate; extra flag bits must not derail the value read.
        @Test("Extra flag bits don't break parsing")
        func ignoresExtraFlags() {
            #expect(HeartRateMonitor.heartRate(from: Data([0x10, 65, 0x40, 0x02])) == 65)
        }

        // A truncated notification (connection drop mid-write) must never
        // crash the app or produce a garbage reading.
        @Test("Truncated payloads return nil instead of crashing", arguments: [
            Data(),
            Data([0x00]),
            Data([0x01, 0x2C]),
        ])
        func truncatedPayloads(data: Data) {
            #expect(HeartRateMonitor.heartRate(from: data) == nil)
        }
    }

    @Suite("Monitor", .tags(.pairing))
    @MainActor
    struct Monitor {
        // Unpairing must forget everything: the stored id, the live
        // reading, and the paired flag — a half-cleared state reconnects
        // to a sensor the user removed.
        @Test("Unpair clears the stored sensor and the live reading")
        func unpairClearsState() {
            let defaults = makeIsolatedDefaults()
            defaults.set(UUID().uuidString, forKey: "pairedSensorID")
            let monitor = makeMonitor(defaults: defaults)
            monitor.record(88)
            #expect(monitor.isPaired)

            monitor.unpair()

            #expect(!monitor.isPaired)
            #expect(monitor.currentHeartRate == nil)
            #expect(defaults.string(forKey: "pairedSensorID") == nil)
        }

        // The chart window must not grow without bound during a long
        // workout; samples older than the window get trimmed.
        @Test("Samples older than the rolling window are trimmed")
        func trimsOldSamples() {
            let monitor = makeMonitor()
            monitor.record(70)
            monitor.record(75)
            #expect(monitor.samples.count == 2)

            // Trim as if the window has fully elapsed since recording.
            monitor.trimSamples(now: .now.addingTimeInterval(HeartRateMonitor.sampleWindow + 1))
            #expect(monitor.samples.isEmpty)

            // Fresh samples survive a trim at the current time.
            monitor.record(80)
            monitor.trimSamples(now: .now)
            #expect(monitor.samples.map(\.bpm) == [80])
        }

        @Test("The newest reading wins the current-rate display")
        func latestReadingWins() {
            let monitor = makeMonitor()
            monitor.record(70)
            monitor.record(102)
            #expect(monitor.currentHeartRate == 102)
            #expect(monitor.samples.map(\.bpm) == [70, 102])
        }

        // A dropped connection must raise the red banner (with its
        // category), not just quietly flip the footer text.
        @Test("Losing contact while paired raises the connection-lost banner")
        func disconnectRaisesBanner() {
            let defaults = makeIsolatedDefaults()
            defaults.set(UUID().uuidString, forKey: "pairedSensorID")
            let monitor = makeMonitor(defaults: defaults)
            monitor.record(90)

            monitor.handleDisconnection()

            #expect(monitor.connectionError == .connectionLost)
            #expect(monitor.currentHeartRate == nil)
            #expect(monitor.state == .reconnecting)
        }

        @Test("A failed connect attempt raises the connection-failed banner")
        func connectFailureRaisesBanner() {
            let defaults = makeIsolatedDefaults()
            defaults.set(UUID().uuidString, forKey: "pairedSensorID")
            let monitor = makeMonitor(defaults: defaults)

            monitor.handleConnectionFailure()

            #expect(monitor.connectionError == .connectionFailed)
            #expect(monitor.state == .reconnecting)
        }

        // A successful reconnect must clear the banner — a stale error over
        // a live chart erodes trust in every future banner.
        @Test("Reconnecting clears the banner")
        func reconnectClearsBanner() {
            let defaults = makeIsolatedDefaults()
            defaults.set(UUID().uuidString, forKey: "pairedSensorID")
            let monitor = makeMonitor(defaults: defaults)
            monitor.handleDisconnection()
            #expect(monitor.connectionError != nil)

            monitor.handleConnected(name: "Polar H9")

            #expect(monitor.connectionError == nil)
            #expect(monitor.state == .connected(name: "Polar H9"))
        }

        // An unpaired disconnect (the user just removed the sensor) must
        // NOT raise an error banner.
        @Test("Disconnecting after unpair stays silent")
        func unpairedDisconnectStaysSilent() {
            let monitor = makeMonitor()

            monitor.handleDisconnection()

            #expect(monitor.connectionError == nil)
        }

        @Test("Unpair clears any active banner")
        func unpairClearsBanner() {
            let defaults = makeIsolatedDefaults()
            defaults.set(UUID().uuidString, forKey: "pairedSensorID")
            let monitor = makeMonitor(defaults: defaults)
            monitor.handleConnectionFailure()
            #expect(monitor.connectionError != nil)

            monitor.unpair()

            #expect(monitor.connectionError == nil)
        }
    }
}
