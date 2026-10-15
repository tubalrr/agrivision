package com.tubalrr.agrivision.domain.calculator

import com.tubalrr.agrivision.CropRecord
import com.tubalrr.agrivision.ExpenseRecord
import com.tubalrr.agrivision.ProductionRecord
import com.tubalrr.agrivision.SaleRecord
import java.util.Locale

data class CropProfitability(
    val cropId: String,
    val cropLabel: String,
    val fieldId: String,
    val areaHectares: Double,
    val income: Double,
    val expenses: Double,
    val harvestQuantity: Double,
    val harvestUnit: String,
    val netProfit: Double,
    val costPerHectare: Double?
)

object FarmCostingCalculator {
    fun summarize(
        crops: List<CropRecord>,
        expenses: List<ExpenseRecord>,
        sales: List<SaleRecord>,
        production: List<ProductionRecord>
    ): List<CropProfitability> = crops.map { crop ->
        val linkedExpenses = expenses.filter { it.sourceType.equals("Crop", true) && it.sourceId == crop.cropId }
        val linkedSales = sales.filter { it.sourceType.equals("Crop", true) && it.sourceId == crop.cropId }
        val harvests = production.filter {
            it.sourceType.equals("Crop", true) && it.sourceId == crop.cropId && it.productionType.equals("Harvest", true)
        }
        val area = Regex("""-?\d+(?:\.\d+)?""").find(crop.area.trim())?.value
            ?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
        val expenseTotal = linkedExpenses.sumOf { it.amount.coerceAtLeast(0.0) }
        val incomeTotal = linkedSales.sumOf { it.amount.coerceAtLeast(0.0) }
        val parsedHarvests = harvests.mapNotNull { raw ->
            val match = Regex("""^\s*(\d+(?:\.\d+)?)\s*(.*)$""").find(raw.quantity) ?: return@mapNotNull null
            val value = match.groupValues[1].toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: return@mapNotNull null
            value to match.groupValues[2].trim()
        }
        val unit = parsedHarvests.groupBy { it.second.lowercase(Locale.US) }.maxByOrNull { it.value.size }?.key.orEmpty()
        val harvestQuantity = parsedHarvests.filter { it.second.equals(unit, true) }.sumOf { it.first }
        CropProfitability(
            cropId = crop.cropId,
            cropLabel = listOf(crop.name, crop.crop).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Crop" },
            fieldId = crop.fieldId,
            areaHectares = area,
            income = incomeTotal,
            expenses = expenseTotal,
            harvestQuantity = harvestQuantity,
            harvestUnit = unit,
            netProfit = incomeTotal - expenseTotal,
            costPerHectare = if (area > 0.0) expenseTotal / area else null
        )
    }

    fun unallocatedExpenseTotal(expenses: List<ExpenseRecord>): Double =
        expenses.filter { it.sourceType.isBlank() || it.sourceId.isBlank() }.sumOf { it.amount.coerceAtLeast(0.0) }

    fun unallocatedIncomeTotal(sales: List<SaleRecord>): Double =
        sales.filter { it.sourceType.isBlank() || it.sourceId.isBlank() }.sumOf { it.amount.coerceAtLeast(0.0) }
}

data class LivestockProfitability(
    val groupId: String,
    val livestockLabel: String,
    val currentHeads: Int,
    val income: Double,
    val expenses: Double,
    val feedCost: Double,
    val netProfit: Double,
    val costPerHead: Double?,
    val productionByUnit: Map<String, Double>,
    val feedUsageByUnit: Map<String, Double>
)

fun FarmCostingCalculator.summarizeLivestock(
    livestock: List<com.tubalrr.agrivision.Livestock>,
    expenses: List<ExpenseRecord>,
    sales: List<SaleRecord>,
    production: List<ProductionRecord>,
    feedLogs: List<com.tubalrr.agrivision.data.local.FeedLogRecord>
): List<LivestockProfitability> = livestock.map { group ->
    val linkedExpenses = expenses.filter { it.sourceType.equals("Livestock", true) && it.sourceId == group.groupId }
    val linkedSales = sales.filter { it.sourceType.equals("Livestock", true) && it.sourceId == group.groupId }
    val linkedProduction = production.filter { it.sourceType.equals("Livestock", true) && it.sourceId == group.groupId }
    val outputByUnit = linkedProduction.mapNotNull { record ->
        val match = Regex("""^\s*(\d+(?:\.\d+)?)\s*(.*)$""").find(record.quantity) ?: return@mapNotNull null
        val amount = match.groupValues[1].toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: return@mapNotNull null
        val unit = match.groupValues[2].trim().ifBlank { "unit" }
        unit.lowercase(Locale.US) to amount
    }.groupBy { it.first }.mapValues { (_, values) -> values.sumOf { it.second } }
    val groupFeedLogs = feedLogs.filter { it.livestockId == group.groupId }
    val feedByUnit = groupFeedLogs
        .groupBy { it.unit.trim().ifBlank { "unit" } }
        .mapValues { (_, rows) -> rows.sumOf { it.quantityKg.coerceAtLeast(0.0) } }
    val feedCost = groupFeedLogs.sumOf {
        it.costAmount.takeIf { amount -> amount.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    }
    val linkedExpenseCost = linkedExpenses.sumOf { it.amount.coerceAtLeast(0.0) }
    val cost = linkedExpenseCost + feedCost
    val income = linkedSales.sumOf { it.amount.coerceAtLeast(0.0) }
    LivestockProfitability(
        group.groupId,
        listOf(group.name, group.kind).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Livestock group" },
        group.currentPopulation.coerceAtLeast(0),
        income,
        cost,
        feedCost,
        income - cost,
        group.currentPopulation.takeIf { it > 0 }?.let { cost / it },
        outputByUnit,
        feedByUnit
    )
}
