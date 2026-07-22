package com.curiousbeagle.android.heartchart.data

import java.util.UUID

/**
 * Age-based heart rate guidance from the published table. Youth rows are
 * age RANGES (6–12, 13–19); adult rows are single ages. Lookup: the
 * bracket whose range contains the age wins; otherwise the nearest
 * bracket by distance to its range — a 27-year-old gets the 30-year
 * bracket — with ties going to the younger bracket.
 */
data class HeartRateZones(val age: Int) {

    data class Bracket(val ages: IntRange, val low: Int, val high: Int, val max: Int)

    /**
     * Containing range first; otherwise nearest by distance to the range.
     * minByOrNull keeps the earlier element on ties, so equidistant ages
     * snap to the younger bracket.
     */
    val bracket: Bracket
        get() = brackets.firstOrNull { age in it.ages }
            ?: brackets.minByOrNull { distance(it) }!!

    private fun distance(bracket: Bracket): Int = when {
        age < bracket.ages.first -> bracket.ages.first - age
        age > bracket.ages.last -> age - bracket.ages.last
        else -> 0
    }

    val maxHeartRate: Int get() = bracket.max

    /** Bottom of the target zone. */
    val targetLow: Int get() = bracket.low

    /** Top of the target zone. */
    val targetHigh: Int get() = bracket.high

    companion object {
        /** The published age table, ascending and non-overlapping. */
        val brackets = listOf(
            Bracket(ages = 6..12, low = 70, high = 110, max = 220),
            Bracket(ages = 13..19, low = 60, high = 100, max = 220),
            Bracket(ages = 20..20, low = 100, high = 170, max = 200),
            Bracket(ages = 30..30, low = 95, high = 162, max = 190),
            Bracket(ages = 35..35, low = 93, high = 157, max = 185),
            Bracket(ages = 40..40, low = 90, high = 153, max = 180),
            Bracket(ages = 45..45, low = 88, high = 149, max = 175),
            Bracket(ages = 50..50, low = 85, high = 145, max = 170),
            Bracket(ages = 55..55, low = 83, high = 140, max = 165),
            Bracket(ages = 60..60, low = 80, high = 136, max = 160),
            Bracket(ages = 65..65, low = 78, high = 132, max = 155),
            Bracket(ages = 70..70, low = 75, high = 128, max = 150),
        )

        /** Ages outside this range make the published table meaningless. */
        val validAges = 6..100
    }
}

/** One heart-rate reading from the sensor. */
data class HeartRateSample(
    val date: Long,
    val bpm: Int,
    /** Unique identity for list/chart keys — two readings can share a timestamp. */
    val id: UUID = UUID.randomUUID(),
)
