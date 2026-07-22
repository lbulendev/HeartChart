package com.curiousbeagle.android.heartchart

import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor
import com.curiousbeagle.android.heartchart.data.HeartRateZones
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Pinned edge cases. Each test documents the specific failure it prevents
 * from coming back. Mirrors the iOS RegressionTests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Tag("regression")
class RegressionTest {

    @Nested
    @Tag("zones")
    inner class Zones {
        // The published table the chart's red lines must match, verbatim.
        @ParameterizedTest(name = "{0} years")
        @CsvSource(
            "8, 70, 110, 220",
            "16, 60, 100, 220",
            "20, 100, 170, 200",
            "30, 95, 162, 190",
            "35, 93, 157, 185",
            "40, 90, 153, 180",
            "45, 88, 149, 175",
            "50, 85, 145, 170",
            "55, 83, 140, 165",
            "60, 80, 136, 160",
            "65, 78, 132, 155",
            "70, 75, 128, 150",
            )
        fun `zones match the published age table`(age: Int, low: Int, high: Int, max: Int) {
            val zones = HeartRateZones(age)

            assertEquals(low, zones.targetLow)
            assertEquals(high, zones.targetHigh)
            assertEquals(max, zones.maxHeartRate)
        }

        // A containing range wins outright; other ages snap to the NEAREST
        // bracket — never interpolate — with ties going to the younger one.
        @ParameterizedTest(name = "{0} → bracket starting {1}")
        @CsvSource(
            "6, 6",     // youth range boundaries
            "12, 6",
            "13, 13",
            "19, 13",
            "24, 20",
            "27, 30",
            "25, 20",   // tie → younger
            "33, 35",
            "68, 70",
            "5, 6",     // below the table clamps up
            "100, 70",  // above the table clamps down
        )
        fun `ages resolve to the containing or nearest bracket`(age: Int, bracketStart: Int) {
            assertEquals(bracketStart, HeartRateZones(age).bracket.ages.first)
        }
    }

    @Nested
    @Tag("parsing")
    inner class Parsing {
        // Flag bit 0 selects the 16-bit little-endian format.
        @Test
        fun `a 16-bit measurement decodes little-endian`() {
            assertEquals(300, HeartRateMonitor.heartRate(byteArrayOf(0x01, 0x2C, 0x01)))
        }

        // Real payloads carry RR intervals after the heart rate; extra flag
        // bits must not derail the value read.
        @Test
        fun `extra flag bits don't break parsing`() {
            assertEquals(65, HeartRateMonitor.heartRate(byteArrayOf(0x10, 65, 0x40, 0x02)))
        }

        // Kotlin's signed Byte must not corrupt readings above 127.
        @Test
        fun `8-bit readings above 127 decode unsigned`() {
            assertEquals(180, HeartRateMonitor.heartRate(byteArrayOf(0x00, 180.toByte())))
        }

        // A truncated notification must never crash or produce garbage.
        @Test
        fun `truncated payloads return null instead of crashing`() {
            assertNull(HeartRateMonitor.heartRate(byteArrayOf()))
            assertNull(HeartRateMonitor.heartRate(byteArrayOf(0x00)))
            assertNull(HeartRateMonitor.heartRate(byteArrayOf(0x01, 0x2C)))
        }
    }

    @Nested
    @Tag("pairing")
    inner class Monitor {
        // Unpairing must forget everything — a half-cleared state reconnects
        // to a sensor the user removed.
        @Test
        fun `unpair clears the stored sensor, the reading, and the banner`() = runTest {
            val settings = makeSettings()
            settings.setPairedSensorAddress("AA:BB:CC:DD:EE:FF")
            val transport = FakeSensorTransport()
            val monitor = makeMonitor(this, transport, settings)
            advanceUntilIdle()
            monitor.record(88)
            monitor.handleConnectionFailure()

            monitor.unpair()
            advanceUntilIdle()

            assertNull(monitor.pairedSensorAddress.value)
            assertNull(monitor.currentHeartRate.value)
            assertNull(monitor.connectionError.value)
            assertNull(settings.pairedSensorAddress())
            assertTrue(transport.disconnected)
        }

        // A dropped connection must raise the red banner, not just quietly
        // flip the footer text.
        @Test
        fun `losing contact while paired raises the connection-lost banner`() = runTest {
            val settings = makeSettings()
            settings.setPairedSensorAddress("AA:BB:CC:DD:EE:FF")
            val monitor = makeMonitor(this, settings = settings)
            advanceUntilIdle()
            monitor.record(90)

            monitor.handleDisconnection()

            assertEquals(HeartRateMonitor.SensorError.CONNECTION_LOST, monitor.connectionError.value)
            assertNull(monitor.currentHeartRate.value)
            assertEquals(HeartRateMonitor.ConnectionState.Reconnecting, monitor.state.value)
        }

        @Test
        fun `a failed connect attempt raises the connection-failed banner`() = runTest {
            val settings = makeSettings()
            settings.setPairedSensorAddress("AA:BB:CC:DD:EE:FF")
            val monitor = makeMonitor(this, settings = settings)
            advanceUntilIdle()

            monitor.handleConnectionFailure()

            assertEquals(HeartRateMonitor.SensorError.CONNECTION_FAILED, monitor.connectionError.value)
        }

        // A successful reconnect must clear the banner — a stale error over
        // a live chart erodes trust in every future banner.
        @Test
        fun `reconnecting clears the banner`() = runTest {
            val settings = makeSettings()
            settings.setPairedSensorAddress("AA:BB:CC:DD:EE:FF")
            val monitor = makeMonitor(this, settings = settings)
            advanceUntilIdle()
            monitor.handleDisconnection()

            monitor.handleConnected("Polar H9")

            assertNull(monitor.connectionError.value)
            assertEquals(
                HeartRateMonitor.ConnectionState.Connected("Polar H9"),
                monitor.state.value,
            )
        }

        // An unpaired disconnect (the user just removed the sensor) must
        // NOT raise an error banner.
        @Test
        fun `disconnecting after unpair stays silent`() = runTest {
            val monitor = makeMonitor(this)
            advanceUntilIdle()

            monitor.handleDisconnection()

            assertNull(monitor.connectionError.value)
        }

        // The chart window must not grow without bound during a long workout.
        @Test
        fun `samples older than the rolling window are trimmed`() = runTest {
            val monitor = makeMonitor(this)
            val now = System.currentTimeMillis()
            monitor.record(70, now = now)
            monitor.record(75, now = now)
            assertEquals(2, monitor.samples.value.size)

            monitor.trimSamples(now + HeartRateMonitor.SAMPLE_WINDOW_MILLIS + 1)
            assertTrue(monitor.samples.value.isEmpty())

            monitor.record(80, now = now)
            monitor.trimSamples(now)
            assertEquals(listOf(80), monitor.samples.value.map { it.bpm })
        }

        @Test
        fun `the newest reading wins the current-rate display`() = runTest {
            val monitor = makeMonitor(this)
            monitor.record(70)
            monitor.record(102)

            assertEquals(102, monitor.currentHeartRate.value)
            assertEquals(listOf(70, 102), monitor.samples.value.map { it.bpm })
        }
    }
}
