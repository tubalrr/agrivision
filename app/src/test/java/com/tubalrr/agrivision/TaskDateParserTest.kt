package com.tubalrr.agrivision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TaskDateParserTest {
    private val today = "2026-10-11"

    @Test
    fun recognizesCurrentAndLegacyDateFormats() {
        assertEquals(LocalDate.of(2026, 10, 10), parseFarmTaskDate("2026-10-10"))
        assertEquals(LocalDate.of(2026, 10, 10), parseFarmTaskDate("10/10/2026"))
        assertEquals(LocalDate.of(2026, 10, 10), parseFarmTaskDate("10 Oct 2026"))
        assertEquals(LocalDate.of(2026, 10, 10), parseFarmTaskDate("Oct 10, 2026"))
        assertEquals(LocalDate.of(2026, 10, 10), parseFarmTaskDate("October 10, 2026"))
    }

    @Test
    fun identifiesPastDatesButNotTodayOrFutureDatesAsOverdue() {
        assertTrue(isTaskOverdue("2026-10-10", today))
        assertTrue(isTaskOverdue("10/10/2026", today))
        assertFalse(isTaskOverdue("2026-10-11", today))
        assertFalse(isTaskOverdue("2026-10-12", today))
        assertFalse(isTaskOverdue("Tomorrow", today))
        assertFalse(isTaskOverdue("Today", today))
    }

    @Test
    fun invalidAndMissingDatesAreNotMarkedOverdue() {
        assertNull(parseFarmTaskDate("2026-02-31"))
        assertNull(parseFarmTaskDate(""))
        assertFalse(isTaskOverdue("2026-02-31", today))
        assertFalse(isTaskOverdue("unknown", today))
        assertFalse(isTaskOverdue("2026-10-10", "not-a-date"))
    }
}
