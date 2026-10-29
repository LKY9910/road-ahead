package com.roadahead.feed

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class SignalBudgetTest {
    @Test fun `daily budget resumes and resets at Seoul midnight`() {
        val before = Instant.parse("2026-10-28T14:59:59Z")
        val after = Instant.parse("2026-10-28T15:00:00Z")
        assertEquals(42, savedSignalPolls("2026-10-28", 42, before))
        assertEquals(0, savedSignalPolls("2026-10-28", 42, after))
        assertEquals("2026-10-29", signalBudgetDay(after))
        assertEquals(4_500, savedSignalPolls("2026-10-28", 9_000, before))
        assertEquals(0, savedSignalPolls("2026-10-28", -1, before))
    }
}
