package com.rizmi.cleanshield

import com.rizmi.cleanshield.domain.model.ProtectionStats
import org.junit.Assert.*
import org.junit.Test

class StatsCalculatorTest {

    @Test
    fun testBlockPercentageZeroDivisionSafety() {
        val emptyStats = ProtectionStats(
            totalQueries = 0,
            blockedQueries = 0,
            forwardedQueries = 0,
            failedQueries = 0
        )
        assertEquals(0.0, emptyStats.blockPercentage, 0.001)
        assertFalse(emptyStats.blockPercentage.isNaN())
        assertFalse(emptyStats.blockPercentage.isInfinite())
    }

    @Test
    fun testBlockPercentageAccurateCalculation() {
        val stats1 = ProtectionStats(
            totalQueries = 100,
            blockedQueries = 25,
            forwardedQueries = 70,
            failedQueries = 5
        )
        assertEquals(25.0, stats1.blockPercentage, 0.001)

        val stats2 = ProtectionStats(
            totalQueries = 3,
            blockedQueries = 1,
            forwardedQueries = 2,
            failedQueries = 0
        )
        assertEquals(33.333, stats2.blockPercentage, 0.01)
    }

    @Test
    fun testAllowedQueriesMapping() {
        val stats = ProtectionStats(
            totalQueries = 50,
            blockedQueries = 10,
            forwardedQueries = 40
        )
        assertEquals(40L, stats.allowedQueries)
    }
}
