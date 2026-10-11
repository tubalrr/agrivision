package com.tubalrr.agrivision.domain.calculator

import com.tubalrr.agrivision.ExpenseRecord
import com.tubalrr.agrivision.SaleRecord
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MonthlyFinanceSummary(
    val month: YearMonth,
    val income: Double,
    val expenses: Double,
    val salesCount: Int,
    val expenseCount: Int
) {
    val net: Double get() = income - expenses
}

object FarmFinanceCalculator {
    val periods = listOf("This month", "Last month", "This year", "All time")
    private val dateFormatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("d/M/uuuu", Locale.US),
        DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US),
        DateTimeFormatter.ofPattern("d MMM uuuu", Locale.US),
        DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US),
        DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US)
    )

    fun parseDate(raw: String): LocalDate? {
        val value = raw.trim()
        if (value.isBlank() || value.equals("today", true) ||
            value.equals("yesterday", true) || value.equals("unknown", true)
        ) return null
        return dateFormatters.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(value, formatter) }.getOrNull()
        }
    }

    fun isInPeriod(raw: String, period: String, today: LocalDate = LocalDate.now()): Boolean {
        if (period == "All time") return true
        val date = parseDate(raw) ?: return false
        val currentMonth = YearMonth.from(today)
        return when (period) {
            "This month" -> YearMonth.from(date) == currentMonth
            "Last month" -> YearMonth.from(date) == currentMonth.minusMonths(1)
            "This year" -> date.year == today.year
            else -> false
        }
    }

    fun filterSales(sales: List<SaleRecord>, period: String, today: LocalDate = LocalDate.now()): List<SaleRecord> =
        sales.filter { isInPeriod(it.date, period, today) }

    fun filterExpenses(expenses: List<ExpenseRecord>, period: String, today: LocalDate = LocalDate.now()): List<ExpenseRecord> =
        expenses.filter { isInPeriod(it.date, period, today) }

    fun undatedCount(sales: List<SaleRecord>, expenses: List<ExpenseRecord>): Int =
        sales.count { parseDate(it.date) == null } + expenses.count { parseDate(it.date) == null }

    fun unpaidReceivables(sales: List<SaleRecord>): Double =
        sales.filter { it.paymentStatus.equals("Unpaid", ignoreCase = true) }.sumOf { it.amount }

    fun unpaidPayables(expenses: List<ExpenseRecord>): Double =
        expenses.filter { it.paymentStatus.equals("Unpaid", ignoreCase = true) }.sumOf { it.amount }

    fun monthlyTrend(
        sales: List<SaleRecord>,
        expenses: List<ExpenseRecord>,
        today: LocalDate = LocalDate.now(),
        monthCount: Int = 6
    ): List<MonthlyFinanceSummary> {
        val safeCount = monthCount.coerceIn(1, 36)
        val lastMonth = YearMonth.from(today)
        return (safeCount - 1 downTo 0).map { offset ->
            val month = lastMonth.minusMonths(offset.toLong())
            val matchingSales = sales.filter { parseDate(it.date)?.let { date -> YearMonth.from(date) == month } == true }
            val matchingExpenses = expenses.filter { parseDate(it.date)?.let { date -> YearMonth.from(date) == month } == true }
            MonthlyFinanceSummary(
                month,
                matchingSales.sumOf { it.amount },
                matchingExpenses.sumOf { it.amount },
                matchingSales.size,
                matchingExpenses.size
            )
        }
    }
}
