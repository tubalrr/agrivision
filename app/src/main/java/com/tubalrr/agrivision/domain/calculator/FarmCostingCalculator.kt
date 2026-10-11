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
