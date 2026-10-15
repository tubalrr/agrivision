package com.tubalrr.agrivision.domain.calculator

import com.tubalrr.agrivision.CropRecord
import com.tubalrr.agrivision.ExpenseRecord
import com.tubalrr.agrivision.ProductionRecord
import com.tubalrr.agrivision.SaleRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FarmCostingCalculatorTest {
    @Test fun onlyExplicitlyLinkedTransactionsAffectCropProfit() {
        val crop = CropRecord(name = "Field A", crop = "Rice", area = "2.5", stage = "Growing", cropId = "C1", fieldId = "F1")
        val rows = FarmCostingCalculator.summarize(
            crops = listOf(crop),
            expenses = listOf(
                ExpenseRecord("Seeds", 1200.0, "Seed", "2026-10-01", "Crop", "C1", "F1"),
                ExpenseRecord("Fuel", 500.0, "Tractor", "2026-10-02")
            ),
            sales = listOf(
                SaleRecord("Rice", 3000.0, "2026-10-03", "Crop Sales", "Crop", "C1", "F1"),
                SaleRecord("Other", 700.0, "2026-10-04")
            ),
            production = listOf(
                ProductionRecord("Rice", "100 kg", "2026-10-05", sourceType = "Crop", sourceId = "C1", fieldId = "F1", areaHectares = 2.5)
            )
        )
        val result = rows.single()
        assertEquals(1200.0, result.expenses, 0.001)
        assertEquals(3000.0, result.income, 0.001)
        assertEquals(1800.0, result.netProfit, 0.001)
        assertEquals(480.0, result.costPerHectare!!, 0.001)
        assertEquals(100.0, result.harvestQuantity, 0.001)
        assertEquals("kg", result.harvestUnit)
    }

    @Test fun zeroAreaDoesNotDivideByZero() {
        val crop = CropRecord(name = "Field B", crop = "Corn", area = "0", stage = "Planting", cropId = "C2")
        val result = FarmCostingCalculator.summarize(listOf(crop), emptyList(), emptyList(), emptyList()).single()
        assertNull(result.costPerHectare)
        assertEquals(0.0, result.netProfit, 0.001)
    }

    @Test fun unlinkedRowsRemainSeparatelyVisible() {
        val expenses = listOf(ExpenseRecord("Labor", 900.0, "Workers"))
        val sales = listOf(SaleRecord("Other", 1200.0, "2026-10-01"))
        assertEquals(900.0, FarmCostingCalculator.unallocatedExpenseTotal(expenses), 0.001)
        assertEquals(1200.0, FarmCostingCalculator.unallocatedIncomeTotal(sales), 0.001)
    }

    @Test fun livestockProfitUsesOnlyLinkedFinanceAndReportsFeedUsageSeparately() {
        val group = com.tubalrr.agrivision.Livestock(
            name = "Layer flock", kind = "Chicken", count = 100, status = "Active",
            groupId = "L1", initialPopulation = 100, currentPopulation = 80
        )
        val result = FarmCostingCalculator.summarizeLivestock(
            livestock = listOf(group),
            expenses = listOf(
                ExpenseRecord("Feed", 2500.0, "Layer crumble", "2026-10-02", "Livestock", "L1"),
                ExpenseRecord("Labor", 600.0, "Worker", "2026-10-03")
            ),
            sales = listOf(
                SaleRecord("Eggs", 4200.0, "2026-10-05", "Livestock Sales", "Livestock", "L1"),
                SaleRecord("Other", 999.0, "2026-10-06")
            ),
            production = listOf(
                ProductionRecord("Eggs", "24 trays", "2026-10-06", sourceType = "Livestock", sourceId = "L1")
            ),
            feedLogs = listOf(
                com.tubalrr.agrivision.data.local.FeedLogRecord(
                    livestockId = "L1", date = "2026-10-01", feedName = "Layer crumble",
                    quantityKg = 30.0, unit = "kg", costAmount = 400.0
                )
            )
        ).single()
        assertEquals(4200.0, result.income, 0.001)
        assertEquals(2900.0, result.expenses, 0.001)
        assertEquals(400.0, result.feedCost, 0.001)
        assertEquals(1300.0, result.netProfit, 0.001)
        assertEquals(36.25, result.costPerHead!!, 0.001)
        assertEquals(24.0, result.productionByUnit["trays"] ?: 0.0, 0.001)
        assertEquals(30.0, result.feedUsageByUnit["kg"] ?: 0.0, 0.001)
    }

}
