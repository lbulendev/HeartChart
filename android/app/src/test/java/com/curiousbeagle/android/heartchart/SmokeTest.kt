package com.curiousbeagle.android.heartchart

import com.curiousbeagle.android.heartchart.bluetooth.HeartRateMonitor
import com.curiousbeagle.android.heartchart.data.HeartRateZones
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

/**
 * Critical path only: compute zones for an age, decode a typical sensor
 * payload, land a reading in the monitor. Mirrors the iOS SmokeTests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Tag("smoke")
class SmokeTest {

    @Nested
    @Tag("zones")
    inner class Zones {
        @Test
        fun `a 45-year-old gets the published zone lines`() {
            val zones = HeartRateZones(age = 45)

            assertEquals(88, zones.targetLow)
            assertEquals(149, zones.targetHigh)
            assertEquals(175, zones.maxHeartRate)
        }
    }

    @Nested
    @Tag("parsing")
    inner class Parsing {
        @Test
        fun `a typical 8-bit measurement decodes`() {
            assertEquals(72, HeartRateMonitor.heartRate(byteArrayOf(0x00, 72)))
        }
    }

    @Nested
    @Tag("pairing")
    inner class Monitor {
        @Test
        fun `a fresh monitor starts unpaired with no readings`() = runTest {
            val monitor = makeMonitor(this)
            advanceUntilIdle()

            assertFalse(monitor.isPaired)
            assertNull(monitor.currentHeartRate.value)
            assertTrue(monitor.samples.value.isEmpty())
            assertTrue(monitor.discovered.value.isEmpty())
        }

        @Test
        fun `a recorded reading updates the current rate and the chart window`() = runTest {
            val monitor = makeMonitor(this)

            monitor.record(72)

            assertEquals(72, monitor.currentHeartRate.value)
            assertEquals(listOf(72), monitor.samples.value.map { it.bpm })
        }
    }
}
