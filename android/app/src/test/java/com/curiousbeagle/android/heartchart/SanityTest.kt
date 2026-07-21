package com.curiousbeagle.android.heartchart

import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor
import com.curiousbeagle.android.heartchart.data.HeartRateSample
import com.curiousbeagle.android.heartchart.data.HeartRateZones
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Contract checks on helpers, defaults, and persistence. A failure here
 * usually means the scaffolding drifted, not that the product broke.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Tag("sanity")
class SanityTest {

    @Nested
    @Tag("zones")
    inner class Zones {
        @Test
        fun `the valid age range covers adults and rejects nonsense`() {
            assertTrue(45 in HeartRateZones.validAges)
            assertTrue(10 in HeartRateZones.validAges)
            assertTrue(100 in HeartRateZones.validAges)
            assertFalse(0 in HeartRateZones.validAges)
            assertFalse(101 in HeartRateZones.validAges)
        }

        @Test
        fun `zone bounds round half-up like the published table`() {
            // 55y: 50% of 165 is 82.5 → 83; 85% is 140.25 → 140.
            val zones = HeartRateZones(age = 55)
            assertEquals(83, zones.targetLow)
            assertEquals(140, zones.targetHigh)
        }
    }

    @Nested
    @Tag("parsing")
    inner class Samples {
        @Test
        fun `each sample gets a unique identity for chart keys`() {
            val a = HeartRateSample(date = 0, bpm = 70)
            val b = HeartRateSample(date = 0, bpm = 70)
            assertNotEquals(a.id, b.id)
        }
    }

    @Nested
    @Tag("pairing")
    inner class PairingPersistence {
        @Test
        fun `a stored sensor address restores as paired on init`() = runTest {
            val settings = makeSettings()
            settings.setPairedSensorAddress("AA:BB:CC:DD:EE:FF")
            val transport = FakeSensorTransport()

            val monitor = makeMonitor(this, transport, settings)
            advanceUntilIdle()

            assertTrue(monitor.isPaired)
            assertEquals("AA:BB:CC:DD:EE:FF", monitor.pairedSensorAddress.value)
            // Restoring also re-issues the pending connect.
            assertEquals(listOf("AA:BB:CC:DD:EE:FF"), transport.connectRequests)
            assertEquals(HeartRateMonitor.ConnectionState.Reconnecting, monitor.state.value)
        }

        @Test
        fun `pairing persists the address for the next launch`() = runTest {
            val settings = makeSettings()
            val monitor = makeMonitor(this, settings = settings)
            advanceUntilIdle()

            monitor.pair(HeartRateMonitor.DiscoveredSensor("11:22:33:44:55:66", "Polar H9"))
            advanceUntilIdle()

            assertEquals("11:22:33:44:55:66", settings.pairedSensorAddress())
        }
    }
}
