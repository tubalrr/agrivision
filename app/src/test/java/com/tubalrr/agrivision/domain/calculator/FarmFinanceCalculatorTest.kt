package com.tubalrr.agrivision.domain.calculator

import com.tubalrr.agrivision.ExpenseRecord
import com.tubalrr.agrivision.SaleRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FarmFinanceCalculatorTest {
    private val today = LocalDate.of(2026, 10, 11)

    @Test fun selectedPeriodsIncludeOnlyParseableDatesInRange() {
        val sales = listOf(
            SaleRecord("Current", 100.0, "2026-10-03"),
            SaleRecord("Previous", 80.0, "2026-09-03"),
            SaleRecord("Legacy", 40.0, "Today")
        )
        assertEquals(listOf("Current"), FarmFinanceCalculator.filterSales(sales, "This month", today).map { it.product })
        assertEquals(listOf("Previous"), FarmFinanceCalculator.filterSales(sales, "Last month", today).map { it.product })
        assertEquals(3, FarmFinanceCalculator.filterSales(sales, "All time", today).size)
        assertEquals(1, FarmFinanceCalculator.undatedCount(sales, emptyList()))
    }

    @Test fun dateParsingRejectsRelativeLabelsAndInvalidDates() {
        assertEquals(LocalDate.of(2026, 10, 11), FarmFinanceCalculator.parseDate("2026-10-11"))
        assertEquals(LocalDate.of(2026, 10, 3), FarmFinanceCalculator.parseDate("3 Oct 2026"))
        assertNull(FarmFinanceCalculator.parseDate("Today"))
        assertNull(FarmFinanceCalculator.parseDate("2026-02-31"))
    }

    @Test fun dateParsingRejectsImpossibleDatesAcrossLegacyFormats() {
        assertNull(FarmFinanceCalculator.parseDate("31/02/2026"))
        assertNull(FarmFinanceCalculator.parseDate("02/31/2026"))
        assertNull(FarmFinanceCalculator.parseDate("31 Apr 2026"))
        assertNull(FarmFinanceCalculator.parseDate("February 30, 2026"))
    }

    @Test fun trendSeparatesIncomeAndExpensesByMonthAndLeavesUnknownDatesOut() {
        val sales = listOf(
            SaleRecord("Oct", 1200.0, "2026-10-03"),
            SaleRecord("Sep", 900.0, "2026-09-03"),
            SaleRecord("Unknown", 999.0, "")
        )
        val expenses = listOf(
            ExpenseRecord("Feed", 300.0, "Feed", "2026-10-04"),
            ExpenseRecord("Labor", 100.0, "Worker", "2026-09-04")
        )
        val months = FarmFinanceCalculator.monthlyTrend(sales, expenses, today, 2)
        assertEquals(2, months.size)
        assertEquals(2026, months[0].month.year)
        assertEquals(9, months[0].month.monthValue)
        assertEquals(900.0, months[0].income, 0.001)
        assertEquals(100.0, months[0].expenses, 0.001)
        assertEquals(800.0, months[0].net, 0.001)
        assertEquals(1200.0, months[1].income, 0.001)
        assertEquals(300.0, months[1].expenses, 0.001)
        assertEquals(900.0, months[1].net, 0.001)
    }

    @Test fun unpaidTotalsUseExplicitStatusOnly() {
        val sales = listOf(
            SaleRecord("A", 500.0, "2026-10-02", paymentStatus = "Unpaid"),
            SaleRecord("B", 700.0, "2026-10-03")
        )
        val expenses = listOf(
            ExpenseRecord("Feed", 200.0, "Supplier", "2026-10-02", paymentStatus = "Unpaid"),
            ExpenseRecord("Seed", 100.0, "Paid supplier", "2026-10-03")
        )
        assertEquals(500.0, FarmFinanceCalculator.unpaidReceivables(sales), 0.001)
        assertEquals(200.0, FarmFinanceCalculator.unpaidPayables(expenses), 0.001)
    }
}
