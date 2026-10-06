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
        "Farm Inputs", "Feed Logs", "Crop Lifecycle", "Livestock Lifecycle",
        "Inventory Transactions", "Expenses", "Sales", "Incidents", "Incident Audit",
        "Assistance", "Harvest", "Field Summary"
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
        "Feed Logs" -> Data(
            listOf("Feed Log ID", "Livestock ID", "Date", "Feed", "Quantity", "Unit", "Notes"),
            s.feedLogs.map { listOf(it.feedLogId, it.livestockId, it.date, it.feedName, number(it.quantityKg), it.unit, it.notes) }
        )
        "Crop Lifecycle" -> Data(
            listOf("Event ID", "Crop ID", "Field ID", "Stage", "Date", "Input", "Quantity", "Unit", "Notes"),
            s.cropLifecycleEvents.map { listOf(it.eventId, it.cropId, it.fieldId, it.stage, it.date, it.inputName, it.quantity, it.unit, it.notes) }
        )
        "Livestock Lifecycle" -> Data(
            listOf("Event ID", "Livestock ID", "Stage", "Date", "Input", "Quantity", "Unit", "Amount", "Notes"),
            s.livestockLifecycleEvents.map { listOf(it.eventId, it.livestockId, it.stage, it.date, it.inputName, number(it.quantity), it.unit, money(it.amount), it.notes) }
        )
        "Inventory Transactions" -> Data(
            listOf("Transaction ID", "Inventory ID", "Type", "Quantity", "Unit", "Date", "Source Type", "Source ID", "Notes"),
            s.inventoryTransactions.map { listOf(it.transactionId, it.inventoryId, it.type, number(it.quantity), it.unit, it.date, it.sourceType, it.sourceId, it.notes) }
        )
        "Expenses" -> Data(listOf("Date", "Category", "Amount", "Note"), s.expenses.map { listOf(it.date, it.category, money(it.amount), it.note) })
        "Sales" -> Data(listOf("Date", "Product", "Amount", "Income Category"), s.sales.map { listOf(it.date, it.product, money(it.amount), it.incomeCategory) })
        "Incidents" -> Data(
            listOf("Incident ID", "Date", "Type", "Commodity", "Field ID", "Severity", "Status", "Affected Area", "Description", "Reviewer"),
            s.fieldIncidents.map { listOf(it.id, it.date, it.type, it.commodity, it.fieldId, it.severity, it.status, it.affectedArea, it.description, it.reviewer) }
        )
        "Incident Audit" -> Data(
            listOf("Event ID", "Incident ID", "From Status", "Status", "Actor", "Timestamp", "Note"),
            s.incidentEvents.map {
                listOf(
                    it.eventId,
                    it.incidentId,
                    it.fromStatus,
                    it.status,
                    it.actor,
                    it.timestamp.toString(),
                    it.note
                )
            }
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
        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 32f
        val contentWidth = pageWidth - (margin * 2)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val reportPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9f }
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 7f }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 7f }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = 0f

        fun finishPage() {
            page?.let { doc.finishPage(it) }
            page = null
            canvas = null
        }

        fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
            val clean = text.replace('\n', ' ').replace('\r', ' ').trim()
            if (clean.isEmpty()) return listOf("")
            val words = clean.split(Regex("\\s+"))
            val lines = mutableListOf<String>()
            var current = ""
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                    current = candidate
                } else {
                    lines += current
                    current = word
                }
            }
            if (current.isNotEmpty()) lines += current
            return lines
        }

        fun startPage() {
            finishPage()
            pageNo++
            page = doc.startPage(
                PdfDocument.PageInfo.Builder(
                    pageWidth.toInt(),
                    pageHeight.toInt(),
                    pageNo
                ).create()
            )
            canvas = page!!.canvas
            y = margin
            canvas!!.drawText("AgriVision", margin, y, headerPaint)
            y += 18f
            canvas!!.drawText("DA-READY AGRICULTURAL RECORD", margin, y, metaPaint)
            y += 18f
            canvas!!.drawText(type, margin, y, reportPaint)
            y += 16f
            canvas!!.drawText(
                "Farmer: " + s.profile.farmerName.ifBlank { "Not registered" },
                margin, y, metaPaint
            )
            y += 12f
            canvas!!.drawText(
                "Farm: " + (s.farm?.farmName ?: s.profile.farmName).ifBlank { "Not registered" },
                margin, y, metaPaint
            )
            y += 12f
            canvas!!.drawText("Generated: " + now(), margin, y, metaPaint)
            y += 16f
            canvas!!.drawLine(margin, y, pageWidth - margin, y, metaPaint)
            y += 12f
        }

        fun footer() {
            val c = canvas ?: return
            val footerY = pageHeight - 18f
            c.drawLine(margin, footerY - 8f, pageWidth - margin, footerY - 8f, footerPaint)
            c.drawText("AgriVision • DA-ready record • Page $pageNo", margin, footerY, footerPaint)
        }

        fun columnWidths(): FloatArray {
            if (d.columns.isEmpty()) return floatArrayOf()
            val weights = d.columns.map { column ->
                when {
                    column.contains("Description", true) || column.contains("Notes", true) -> 2.4f
                    column.contains("Name", true) || column.contains("Commodity", true) ||
                        column.contains("Program", true) || column.contains("Location", true) -> 1.6f
                    column.length > 14 -> 1.25f
                    else -> 1f
                }
            }
            val total = weights.sum()
            return weights.map { contentWidth * (it / total) }.toFloatArray()
        }

        fun drawTableHeader(widths: FloatArray) {
            val c = canvas ?: return
            var x = margin
            d.columns.forEachIndexed { i, column ->
                val header = wrap(column, headPaint, (widths[i] - 4f).coerceAtLeast(12f)).firstOrNull().orEmpty()
                c.drawText(header, x + 2f, y, headPaint)
                x += widths[i]
            }
            y += 12f
            c.drawLine(margin, y, pageWidth - margin, y, headPaint)
            y += 10f
        }

        fun drawRow(values: List<String>, widths: FloatArray, paint: Paint) {
            val lineHeight = 9f
            val wrapped = values.mapIndexed { i, value ->
                wrap(value, paint, (widths.getOrElse(i) { 0f } - 4f).coerceAtLeast(12f))
            }
            val rowHeight = (wrapped.maxOfOrNull { it.size } ?: 1) * lineHeight + 5f
            if (y + rowHeight > pageHeight - 34f) {
                footer()
                startPage()
                drawTableHeader(widths)
            }
            var x = margin
            wrapped.forEachIndexed { i, lines ->
                lines.forEachIndexed { lineIndex, line ->
                    canvas!!.drawText(line, x + 2f, y + (lineIndex + 1) * lineHeight, paint)
                }
                x += widths.getOrElse(i) { 0f }
            }
            y += rowHeight
        }

        val widths = columnWidths()
        startPage()
        if (d.columns.isNotEmpty()) {
            drawTableHeader(widths)
            d.rows.forEach { row -> drawRow(row, widths, bodyPaint) }
        } else {
            canvas!!.drawText("No records found for this report.", margin, y, bodyPaint)
        }

        footer()
        finishPage()

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
