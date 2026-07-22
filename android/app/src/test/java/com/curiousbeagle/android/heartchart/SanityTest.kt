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
        fun `the valid age range covers children through seniors, rejects nonsense`() {
            assertTrue(6 in HeartRateZones.validAges)
            assertTrue(45 in HeartRateZones.validAges)
            assertTrue(100 in HeartRateZones.validAges)
            assertFalse(5 in HeartRateZones.validAges)
            assertFalse(101 in HeartRateZones.validAges)
        }

        // The bracket lookup assumes an ascending, non-overlapping table;
        // a careless row edit would silently skew every match.
        @Test
        fun `the bracket table is ascending and non-overlapping`() {
            val brackets = HeartRateZones.brackets
            assertEquals(12, brackets.size)
            brackets.zipWithNext().forEach { (earlier, later) ->
                assertTrue(earlier.ages.last < later.ages.first)
            }
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
