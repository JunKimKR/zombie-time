package com.zombietime.app.data

import org.junit.Assert.*
import org.junit.Test

class RecoveryTest {
    private fun running() = RecoveryState(startedElapsed = 1000, durationMinutes = 5, bootCount = 4)
    @Test fun noEarlyOrDuplicateReward() {
        val s = running()
        assertEquals(s, s.complete(300999, 4, "2026-09-21"))
        val done = s.complete(301000, 4, "2026-09-21")
        assertEquals(1, done.seeds)
        assertEquals(5, done.totalMinutes)
        assertEquals(done, done.complete(400000, 4, "2026-09-21"))
    }
    @Test fun rebootCancelsWithoutReward() {
        val done = running().complete(500000, 5, "2026-09-21")
        assertFalse(done.active)
        assertEquals(0, done.seeds)
    }
    @Test fun clockRollbackCancelsWithoutReward() {
        assertFalse(running().complete(999, 4, "2026-09-21").active)
    }
    @Test fun streakRequiresConsecutiveDates() {
        val s = running().copy(streak = 3, lastDate = "2026-09-20")
        assertEquals(4, s.complete(301000, 4, "2026-09-21").streak)
        assertEquals(3, s.complete(301000, 4, "2026-09-20").streak)
        assertEquals(1, s.complete(301000, 4, "2026-09-22").streak)
    }
    @Test fun cannotOverspendOrDoubleCharge() {
        val s = RecoveryState(seeds = 6)
        assertEquals(s, s.selectGarden("moon"))
        assertEquals(s, s.selectGarden("unknown"))
        val bought = s.selectGarden("forest")
        assertEquals(0, bought.seeds)
        assertEquals("forest", bought.garden)
        assertEquals(bought, bought.selectGarden("forest"))
        assertEquals(0, bought.selectGarden("peach").selectGarden("forest").seeds)
    }
}
