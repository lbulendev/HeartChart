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

        // The chart's y-domain is the zone lines plus 20 bpm of margin —
        // never down to zero — with gridlines at a readable stride.
        @Test
        fun `chart scale spans low-20 to max+20 with a readable stride`() {
            val adult = HeartRateZones(age = 45)   // low→max span 87 → stride 15
            assertEquals(68, adult.chartFloor)
            assertEquals(195, adult.chartCeiling)
            assertEquals(15, adult.axisStride)

            val twenty = HeartRateZones(age = 20)  // span 100 → stride 15
            assertEquals(15, twenty.axisStride)

            val child = HeartRateZones(age = 8)    // span 150 → stride 20
            assertEquals(50, child.chartFloor)
            assertEquals(240, child.chartCeiling)
            assertEquals(20, child.axisStride)
        }

        // Gridline labels must land on round numbers (140/145, never 143):
        // every tick is a multiple of the stride, inside the domain.
        @Test
        fun `axis ticks land on round numbers within the domain`() {
            val fiftyFive = HeartRateZones(age = 55)  // floor 63, ceiling 185, stride 15
            assertEquals(listOf(75, 90, 105, 120, 135, 150, 165, 180), fiftyFive.axisTicks)

            val child = HeartRateZones(age = 8)       // floor 50, ceiling 240, stride 20
            assertEquals(60, child.axisTicks.first())
            assertEquals(240, child.axisTicks.last())

            listOf(fiftyFive, child, HeartRateZones(age = 20)).forEach { zones ->
                zones.axisTicks.forEach { tick ->
                    assertEquals(0, tick % 5)
                    assertTrue(tick >= zones.chartFloor && tick <= zones.chartCeiling)
                }
            }
        }

        // Picker labels show each bracket's full covered span: single-age
        // rows extend to the next row's start; the last row is open-ended.
        @Test
        fun `bracket labels render full covered ranges`() {
            assertEquals("6–12", HeartRateZones(age = 8).bracketLabel)
            assertEquals("20–29", HeartRateZones(age = 20).bracketLabel)
            assertEquals("45–49", HeartRateZones(age = 45).bracketLabel)
            assertEquals("65–69", HeartRateZones(age = 65).bracketLabel)
            assertEquals("70+", HeartRateZones(age = 80).bracketLabel)
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
