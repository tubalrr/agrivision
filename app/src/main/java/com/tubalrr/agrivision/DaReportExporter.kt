package com.tubalrr.agrivision

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.tubalrr.agrivision.data.local.FarmSnapshot
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DaReportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv"),
    JSON("json", "application/json")
}

object DaReportExporter {
    val reportTypes = listOf(
        "Farmer Profile", "Farm Registry", "Crop Production", "Livestock Inventory",
        "Farm Inputs", "Expenses", "Sales", "Incidents", "Assistance", "Harvest", "Field Summary"
    )

    fun fileName(type: String, format: DaReportFormat) =
        "agrivision-" + type.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "-").trim('-') + "." + format.extension

    fun write(snapshot: FarmSnapshot, type: String, format: DaReportFormat, sink: (ByteArray) -> Unit) {
        val data = data(snapshot, type)
        sink(
            when (format) {
                DaReportFormat.JSON -> json(snapshot, type, data).toString(2).toByteArray(Charsets.UTF_8)
                DaReportFormat.CSV -> csv(data).toByteArray(Charsets.UTF_8)
                DaReportFormat.PDF -> pdf(snapshot, type, data)
            }
        )
    }

    private data class Data(val columns: List<String>, val rows: List<List<String>>)

    private fun data(s: FarmSnapshot, type: String): Data = when (type) {
        "Farmer Profile" -> Data(
            listOf("Field", "Value"),
            listOf(
                listOf("Farmer Name", s.profile.farmerName), listOf("Farmer ID", s.profile.farmerId),
                listOf("Contact", s.profile.contact), listOf("Province", s.profile.province),
                listOf("Municipality", s.profile.municipality), listOf("Barangay", s.profile.barangay),
                listOf("Registry Status", s.profile.registryStatus), listOf("Review Notes", s.profile.reviewNotes)
            )
        )
        "Farm Registry" -> Data(
            listOf("Farm ID", "Farm Name", "Farmer ID", "Province", "Municipality", "Barangay", "Area", "Land Tenure", "Commodities", "Registry Status"),
            listOf(listOf(
                s.farm?.farmId.orEmpty(), s.farm?.farmName.orEmpty(), s.farm?.farmerId.orEmpty(),
                s.farm?.province.orEmpty(), s.farm?.municipality.orEmpty(), s.farm?.barangay.orEmpty(),
                s.farm?.totalArea.orEmpty(), s.farm?.landTenure.orEmpty(), s.farm?.commodities.orEmpty(),
                s.farm?.registryStatus.orEmpty()
            ))
        )
        "Crop Production" -> Data(
            listOf("Product", "Quantity", "Period", "Source Type", "Source ID", "Field ID", "Area (ha)", "Production Type"),
            s.production.map { listOf(it.product, it.quantity, it.period, it.sourceType, it.sourceId, it.fieldId, number(it.areaHectares), it.productionType) }
        )
        "Livestock Inventory" -> Data(
            listOf("Name", "Kind", "Current Population", "Initial Population", "Status", "Group ID"),
            s.livestock.map { listOf(it.name, it.kind, it.currentPopulation.toString(), it.initialPopulation.toString(), it.status, it.groupId) }
        )
        "Farm Inputs" -> {
            val inventory = s.inventory.map { listOf("Inventory", it.name, it.category, it.stock.toString(), it.unit, it.supplier, it.dateAcquired, it.expiryDate) }
            val crop = s.cropLifecycleEvents.filter { it.inputName.isNotBlank() }
                .map { listOf("Crop Input", it.inputName, it.stage, it.quantity.toString(), it.unit, "", it.date, it.notes) }
            val livestock = s.livestockLifecycleEvents.filter { it.inputName.isNotBlank() }
                .map { listOf("Livestock Input", it.inputName, it.stage, it.quantity.toString(), it.unit, "", it.date, it.notes) }
            Data(listOf("Record Type", "Input", "Category/Stage", "Quantity", "Unit", "Supplier", "Date", "Notes"), inventory + crop + livestock)
        }
        "Expenses" -> Data(listOf("Date", "Category", "Amount", "Note"), s.expenses.map { listOf(it.date, it.category, money(it.amount), it.note) })
        "Sales" -> Data(listOf("Date", "Product", "Amount", "Income Category"), s.sales.map { listOf(it.date, it.product, money(it.amount), it.incomeCategory) })
        "Incidents" -> Data(
            listOf("Incident ID", "Date", "Type", "Commodity", "Field ID", "Severity", "Status", "Affected Area", "Description", "Reviewer"),
            s.fieldIncidents.map { listOf(it.id, it.date, it.type, it.commodity, it.fieldId, it.severity, it.status, it.affectedArea, it.description, it.reviewer) }
        )
        "Assistance" -> Data(
            listOf("Request ID", "Program", "Type", "Date Received", "Quantity", "Status", "Source", "Incident ID", "Outcome"),
            s.assistance.map { listOf(it.requestId, it.program, it.assistanceType, it.dateReceived, it.quantity, it.status, it.source, it.incidentId, it.outcome) }
        )
        "Harvest" -> Data(
            listOf("Product", "Quantity", "Period", "Source Type", "Source ID", "Field ID", "Area (ha)"),
            s.production.filter { it.productionType.equals("Harvest", true) }
                .map { listOf(it.product, it.quantity, it.period, it.sourceType, it.sourceId, it.fieldId, number(it.areaHectares)) }
        )
        "Field Summary" -> Data(
            listOf("Field ID", "Name", "Area (ha)", "Location", "Crop", "Planting Date", "Expected Harvest", "Status", "Latitude", "Longitude"),
            s.fields.map { listOf(it.fieldId, it.name, number(it.areaHectares), it.location, it.crop, it.plantingDate, it.expectedHarvest, it.currentStatus, it.latitude?.toString().orEmpty(), it.longitude?.toString().orEmpty()) }
        )
        else -> error("Unsupported DA report: " + type)
    }

    private fun json(s: FarmSnapshot, type: String, d: Data) = JSONObject().apply {
        put("app", "AgriVision")
        put("reportVersion", 1)
        put("reportType", type)
        put("generatedAt", now())
        put("farmerId", s.profile.farmerId)
        put("farmId", s.farm?.farmId ?: "")
        put("columns", JSONArray(d.columns))
        put("records", JSONArray().apply {
            d.rows.forEach { row ->
                put(JSONObject().apply {
                    d.columns.forEachIndexed { i, c -> put(c, row.getOrElse(i) { "" }) }
                })
            }
        })
    }

    private fun csv(d: Data) = buildString {
        append(d.columns.joinToString(",") { cell(it) }).append('\n')
        d.rows.forEach { row -> append(d.columns.indices.joinToString(",") { cell(row.getOrElse(it) { "" }) }).append('\n') }
    }

    private fun pdf(s: FarmSnapshot, type: String, d: Data): ByteArray {
        val doc = PdfDocument()
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val meta = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }
        val head = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 7.5f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 7f }
        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
        var canvas = page.canvas
        var y = 42f
        fun nextPage() {
            doc.finishPage(page)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNo).create())
            canvas = page.canvas
            y = 42f
        }
        canvas.drawText("AgriVision - DA-Ready Report", 32f, y, title)
        y += 22f
        canvas.drawText(type, 32f, y, Paint(meta).apply { textSize = 13f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
        y += 16f
        canvas.drawText("Farmer: " + s.profile.farmerName.ifBlank { "Not registered" }, 32f, y, meta)
        y += 12f
        canvas.drawText("Farm: " + (s.farm?.farmName ?: s.profile.farmName).ifBlank { "Not registered" }, 32f, y, meta)
        y += 12f
        canvas.drawText("Generated: " + now(), 32f, y, meta)
        y += 22f
        val width = 531f / d.columns.size.coerceAtLeast(1)
        fun row(values: List<String>, p: Paint) {
            if (y > 810f) nextPage()
            values.forEachIndexed { i, v ->
                canvas.drawText(short(v), 32f + i * width, y, p)
            }
            y += 14f
        }
        row(d.columns, head)
        y += 4f
        d.rows.forEach { row(it, body) }
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun cell(v: String): String {
        val x = v.replace("\"", "\"\"")
        return if (x.contains(',') || x.contains('"') || x.contains('\n') || x.contains('\r')) "\"" + x + "\"" else x
    }
    private fun short(v: String) = v.replace('\n', ' ').replace('\r', ' ').let { if (it.length > 24) it.take(21) + "..." else it }
    private fun number(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.2f", v)
    private fun money(v: Double) = String.format(Locale.US, "%.2f", v)
    private fun now() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
}
