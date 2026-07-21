package com.curiousbeagle.android.heartchart.data

import java.util.UUID

/**
 * Age-based heart rate guidance using the standard 220 − age estimate:
 * the target zone spans 50–85% of maximum. Matches the published table
 * (45y → 88–149, max 175; 50y → 85–145, max 170; 55y → 83–140, max 165).
 */
data class HeartRateZones(val age: Int) {

    val maxHeartRate: Int get() = 220 - age

    /** Bottom of the target zone: 50% of maximum. */
    val targetLow: Int get() = Math.round(maxHeartRate * 0.5).toInt()

    /** Top of the target zone: 85% of maximum. */
    val targetHigh: Int get() = Math.round(maxHeartRate * 0.85).toInt()

    companion object {
        /** Ages outside this range make the 220 − age estimate meaningless. */
        val validAges = 10..100
    }
}

/** One heart-rate reading from the sensor. */
data class HeartRateSample(
    val date: Long,
    val bpm: Int,
    /** Unique identity for list/chart keys — two readings can share a timestamp. */
    val id: UUID = UUID.randomUUID(),
)
