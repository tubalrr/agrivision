package com.tubalrr.agrivision.data.local

import android.content.Context
import com.tubalrr.agrivision.AssistanceRecord
import com.tubalrr.agrivision.CropRecord
import com.tubalrr.agrivision.EquipmentRecord
import com.tubalrr.agrivision.ExpenseRecord
import com.tubalrr.agrivision.FarmTask
import com.tubalrr.agrivision.FarmerProfile
import com.tubalrr.agrivision.FieldIncident
import com.tubalrr.agrivision.IncidentEvent
import com.tubalrr.agrivision.InventoryItem
import com.tubalrr.agrivision.Livestock
import com.tubalrr.agrivision.ProductionRecord
import com.tubalrr.agrivision.ReportSubmission
import com.tubalrr.agrivision.SaleRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale

object LegacyPreferencesMigrator {

    private const val PREFS = "agrivision_farm"
    private const val META_KEY = "legacy_prefs_migrated_v1"

    suspend fun migrateIfNeeded(
        context: Context,
        database: AgriDatabase,
        repository: FarmRepository
    ) = withContext(Dispatchers.IO) {
        val dao = database.farmDao()
        if (dao.getMeta(META_KEY) == "1") return@withContext

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hasLegacyData = prefs.all.isNotEmpty()

        if (hasLegacyData) {
            val snapshot = readSnapshot(prefs)
            repository.replaceAll(snapshot)
            prefs.edit().clear().apply()
        }

        dao.upsertMeta(AppMetaEntity(META_KEY, "1"))
    }

    private fun readSnapshot(prefs: android.content.SharedPreferences): FarmSnapshot {
        val farmId = FarmRepository.DEFAULT_FARM_ID

        val profile = FarmerProfile(
            farmerName = prefs.getString("farmerName", "") ?: "",
            farmerId = prefs.getString("farmerId", "") ?: "",
            contact = prefs.getString("contact", "") ?: "",
            province = prefs.getString("province", "") ?: "",
            municipality = prefs.getString("municipality", "") ?: "",
            barangay = prefs.getString("barangay", "") ?: "",
            farmName = prefs.getString("farmName", "") ?: "",
            farmSize = prefs.getString("farmSize", "") ?: "",
            landTenure = prefs.getString("landTenure", "") ?: "",
            commodities = prefs.getString("commodities", "") ?: "",
            registryStatus = prefs.getString("registryStatus", "For Review") ?: "For Review",
            reviewNotes = prefs.getString("reviewNotes", "") ?: ""
        )

        return FarmSnapshot(
            profile = profile,
            livestock = parseLivestock(prefs.getString("livestock", "[]") ?: "[]"),
            crops = parseCrops(prefs.getString("crops", "[]") ?: "[]"),
            production = parseProduction(prefs.getString("production", "[]") ?: "[]"),
            expenses = parseExpenses(prefs.getString("expenses", "[]") ?: "[]"),
            sales = parseSales(prefs.getString("sales", "[]") ?: "[]"),
            inventory = parseInventory(prefs.getString("inventory", "[]") ?: "[]"),
            equipment = parseEquipment(prefs.getString("equipment", "[]") ?: "[]"),
            tasks = parseTasks(prefs.getString("tasks", "[]") ?: "[]"),
            assistance = parseAssistance(prefs.getString("assistance", "[]") ?: "[]"),
            fieldIncidents = parseFieldIncidents(prefs.getString("fieldIncidents", "[]") ?: "[]"),
            incidentEvents = parseIncidentEvents(prefs.getString("incidentEvents", "[]") ?: "[]"),
            reportSubmission = ReportSubmission(
                status = prefs.getString("reportStatus", "Draft") ?: "Draft",
                submittedDate = prefs.getString("reportSubmittedDate", "") ?: "",
                referenceNo = prefs.getString("reportReferenceNo", "") ?: ""
            )
        )
    }

    private fun parseLivestock(raw: String): List<Livestock> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Livestock(o.optString("name"), o.optString("kind"), o.optInt("count", 0), o.optString("status"))
        }
    }

    private fun parseCrops(raw: String): List<CropRecord> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            CropRecord(o.optString("name"), o.optString("crop"), o.optString("area"), o.optString("stage"))
        }
    }

    private fun parseProduction(raw: String): List<ProductionRecord> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            ProductionRecord(o.optString("product"), o.optString("quantity"), o.optString("period"))
        }
    }

    private fun parseExpenses(raw: String): List<ExpenseRecord> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            ExpenseRecord(
                category = o.optString("category"),
                amount = o.optDouble("amount", 0.0),
                note = o.optString("note"),
                date = o.optString("date")
            )
        }
    }

    private fun parseSales(raw: String): List<SaleRecord> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            SaleRecord(
                product = o.optString("product"),
                amount = o.optDouble("amount", 0.0),
                date = o.optString("date"),
                incomeCategory = o.optString("incomeCategory", "Other Income")
            )
        }
    }

    private fun parseInventory(raw: String): List<InventoryItem> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            InventoryItem(o.optString("name"), o.optString("quantity"), o.optString("status"))
        }
    }

    private fun parseEquipment(raw: String): List<EquipmentRecord> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            EquipmentRecord(o.optString("name"), o.optString("status"), o.optString("note"))
        }
    }

    private fun parseTasks(raw: String): List<FarmTask> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            FarmTask(o.optString("title"), o.optString("category"), o.optString("date"), o.optBoolean("done", false))
        }
    }

    private fun parseAssistance(raw: String): List<AssistanceRecord> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            AssistanceRecord(
                program = o.optString("program"),
                assistanceType = o.optString("assistanceType"),
                dateReceived = o.optString("dateReceived"),
                quantity = o.optString("quantity"),
                status = o.optString("status", "Applied"),
                source = o.optString("source"),
                incidentId = o.optString("incidentId"),
                requestId = o.optString("requestId"),
                approvedDate = o.optString("approvedDate"),
                distributedDate = o.optString("distributedDate"),
                completedDate = o.optString("completedDate"),
                distributionDetails = o.optString("distributionDetails"),
                outcome = o.optString("outcome"),
                reviewNotes = o.optString("reviewNotes")
            )
        }
    }

    private fun parseFieldIncidents(raw: String): List<FieldIncident> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            FieldIncident(
                id = o.optString("id"),
                type = o.optString("type", "Other"),
                commodity = o.optString("commodity"),
                affectedArea = o.optString("affectedArea"),
                date = o.optString("date"),
                severity = o.optString("severity", "Moderate"),
                description = o.optString("description"),
                status = o.optString("status", "Draft"),
                evidenceUri = o.optString("evidenceUri"),
                reviewNotes = o.optString("reviewNotes")
            )
        }
    }

    private fun parseIncidentEvents(raw: String): List<IncidentEvent> {
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            IncidentEvent(
                incidentId = o.optString("incidentId"),
                status = o.optString("status"),
                note = o.optString("note"),
                timestamp = o.optLong("timestamp")
            )
        }
    }
}
