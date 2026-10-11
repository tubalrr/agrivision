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
        val data = validateData(data(snapshot, type))
        sink(
            when (format) {
                DaReportFormat.JSON -> json(snapshot, type, data).toString(2).toByteArray(Charsets.UTF_8)
                DaReportFormat.CSV -> csvUtf8Bytes(csv(data))
                DaReportFormat.PDF -> pdf(snapshot, type, data)
            }
        )
    }

    internal data class Data(val columns: List<String>, val rows: List<List<String>>)

    /**
     * Fail fast if a report table would silently lose or shift values during export.
     * Empty tables are valid; they still retain their column headers.
     */
    internal fun validateData(data: Data): Data {
        require(data.columns.isNotEmpty()) { "A report must define at least one column" }
        require(data.columns.all { it.isNotBlank() }) { "Report column names cannot be blank" }
        data.rows.forEachIndexed { index, row ->
            require(row.size == data.columns.size) {
                "Report row ${index + 1} has ${row.size} values for ${data.columns.size} columns"
            }
        }
        return data
    }

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
            s.farm?.let { farm ->
                listOf(listOf(
                    farm.farmId, farm.farmName, farm.farmerId, farm.province, farm.municipality,
                    farm.barangay, farm.totalArea, farm.landTenure, farm.commodities, farm.registryStatus
                ))
            } ?: emptyList()
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
        "Expenses" -> Data(
            listOf("Expense ID", "Date", "Category", "Amount", "Payment Status", "Source Type", "Source ID", "Field ID", "Note"),
            s.expenses.map { listOf(it.expenseId, it.date, it.category, money(it.amount), it.paymentStatus, it.sourceType, it.sourceId, it.fieldId, it.note) }
        )
        "Sales" -> Data(
            listOf("Sale ID", "Date", "Product", "Amount", "Income Category", "Payment Status", "Source Type", "Source ID", "Field ID"),
            s.sales.map { listOf(it.saleId, it.date, it.product, money(it.amount), it.incomeCategory, it.paymentStatus, it.sourceType, it.sourceId, it.fieldId) }
        )
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
        put("recordCount", d.rows.size)
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
        append(d.columns.joinToString(",") { escapeCsvCell(it) }).append('\n')
        d.rows.forEach { row ->
            append(d.columns.indices.joinToString(",") { escapeCsvCell(row.getOrElse(it) { "" }) }).append('\n')
        }
    }

    private fun pdf(s: FarmSnapshot, type: String, d: Data): ByteArray {
        val doc = PdfDocument()
        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 32f
        val contentWidth = pageWidth - (margin * 2)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            color = android.graphics.Color.rgb(31, 78, 54)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val reportPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            color = android.graphics.Color.rgb(31, 78, 54)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = android.graphics.Color.rgb(84, 94, 88)
        }
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            color = android.graphics.Color.WHITE
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7f
            color = android.graphics.Color.rgb(38, 44, 40)
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7f
            color = android.graphics.Color.rgb(92, 101, 95)
        }
        val tableHeaderFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(31, 78, 54)
            style = Paint.Style.FILL
        }
        val alternateRowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(244, 248, 245)
            style = Paint.Style.FILL
        }
        val tableGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(200, 210, 202)
            strokeWidth = 0.6f
        }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = 0f

        fun finishPage() {
            page?.let { doc.finishPage(it) }
            page = null
            canvas = null
        }

        fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> =
            wrapReportText(text, maxWidth) { candidate -> paint.measureText(candidate) }

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
            canvas!!.drawText("Generated: " + now() + " • Records: " + d.rows.size, margin, y, metaPaint)
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
                    column.contains("Description", true) -> 2.8f
                    column.contains("Notes", true) || column.contains("Outcome", true) -> 2.2f
                    column.contains("Name", true) || column.contains("Commodity", true) ||
                        column.contains("Program", true) || column.contains("Location", true) ||
                        column.contains("Product", true) -> 1.5f
                    column.contains("Amount", true) || column.contains("Quantity", true) ||
                        column.contains("Population", true) || column.contains("Latitude", true) ||
                        column.contains("Longitude", true) -> 0.9f
                    column.length > 14 -> 1.2f
                    else -> 1f
                }
            }
            val total = weights.sum()
            return weights.map { contentWidth * (it / total) }.toFloatArray()
        }

        fun drawTableHeader(widths: FloatArray) {
            val c = canvas ?: return
            val headerLineHeight = 8.5f
            val wrappedHeaders = d.columns.mapIndexed { i, column ->
                wrap(column, headPaint, (widths.getOrElse(i) { 0f } - 6f).coerceAtLeast(12f))
            }
            val lineCount = wrappedHeaders.maxOfOrNull { it.size } ?: 1
            val headerHeight = (10f + lineCount * headerLineHeight + 6f).coerceAtLeast(22f)
            val top = y - 4f
            val right = pageWidth - margin

            // A dark green band and wrapped labels keep full column names readable.
            c.drawRect(margin, top, right, top + headerHeight, tableHeaderFillPaint)
            var x = margin
            wrappedHeaders.forEachIndexed { i, lines ->
                c.drawLine(x, top, x, top + headerHeight, tableGridPaint)
                lines.forEachIndexed { lineIndex, line ->
                    c.drawText(line, x + 3f, top + 11f + lineIndex * headerLineHeight, headPaint)
                }
                x += widths.getOrElse(i) { 0f }
            }
            c.drawLine(right, top, right, top + headerHeight, tableGridPaint)
            y = top + headerHeight
            c.drawLine(margin, y, right, y, tableGridPaint)
            y += 8f
        }

        fun availableLineCount(lineHeight: Float): Int =
            ((pageHeight - 34f - y - 5f).coerceAtLeast(0f) / lineHeight).toInt()

        var bodyRowIndex = 0

        fun drawRow(values: List<String>, widths: FloatArray, paint: Paint) {
            val lineHeight = 9f
            val wrapped = values.mapIndexed { i, value ->
                wrap(value, paint, (widths.getOrElse(i) { 0f } - 6f).coerceAtLeast(12f))
            }
            val maxLines = wrapped.maxOfOrNull { it.size } ?: 1
            val useAlternateFill = bodyRowIndex++ % 2 == 1
            var lineOffset = 0

            // Keep normal rows together when they fit on a fresh page. Extra-tall rows
            // split into page-sized fragments, while row shading and borders stay aligned.
            if (maxLines > availableLineCount(lineHeight)) {
                footer()
                startPage()
                drawTableHeader(widths)
            }

            while (lineOffset < maxLines) {
                val capacity = availableLineCount(lineHeight)
                if (capacity <= 0) {
                    footer()
                    startPage()
                    drawTableHeader(widths)
                    continue
                }

                val lineCount = minOf(capacity, maxLines - lineOffset)
                val segmentTop = y - 5f
                val segmentBottom = y + lineCount * lineHeight + 1f
                val right = pageWidth - margin
                val c = canvas!!

                if (useAlternateFill) {
                    c.drawRect(margin, segmentTop, right, segmentBottom, alternateRowFillPaint)
                }

                var gridX = margin
                widths.forEach { width ->
                    c.drawLine(gridX, segmentTop, gridX, segmentBottom, tableGridPaint)
                    gridX += width
                }
                c.drawLine(right, segmentTop, right, segmentBottom, tableGridPaint)
                c.drawLine(margin, segmentBottom, right, segmentBottom, tableGridPaint)

                var x = margin
                wrapped.forEachIndexed { i, lines ->
                    val endLine = minOf(lineOffset + lineCount, lines.size)
                    for (lineIndex in lineOffset until endLine) {
                        c.drawText(
                            lines[lineIndex],
                            x + 3f,
                            y + (lineIndex - lineOffset + 1) * lineHeight,
                            paint
                        )
                    }
                    x += widths.getOrElse(i) { 0f }
                }

                y += lineCount * lineHeight + 5f
                lineOffset += lineCount
                if (lineOffset < maxLines) {
                    footer()
                    startPage()
                    drawTableHeader(widths)
                }
            }
        }

        val widths = columnWidths()
        startPage()
        if (d.columns.isNotEmpty()) {
            drawTableHeader(widths)
            if (d.rows.isEmpty()) {
                canvas!!.drawText("No records found for this report.", margin, y + 8f, bodyPaint)
            } else {
                d.rows.forEach { row -> drawRow(row, widths, bodyPaint) }
            }
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

    private fun short(v: String) = v.replace('\n', ' ').replace('\r', ' ').let { if (it.length > 24) it.take(21) + "..." else it }
    private fun number(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else String.format(Locale.US, "%.2f", v)
    private fun money(v: Double) = String.format(Locale.US, "%.2f", v)
    private fun now() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
}


/**
 * Escapes a CSV field for spreadsheet applications.
 *
 * CSV quoting protects the file structure. Prefixing dangerous user-controlled values
 * with an apostrophe prevents spreadsheet applications from evaluating them as formulas.
 */
internal fun escapeCsvCell(value: String): String {
    val firstSignificant = value.firstOrNull { !it.isWhitespace() }
    val numericLiteral = value.trim().matches(Regex("-?(?:\\d+(?:\\.\\d*)?|\\.\\d+)"))
    val formulaLike = firstSignificant in setOf('=', '+', '-', '@', '＝', '＋', '－', '＠')
    val safeValue = if (formulaLike && !numericLiteral) "'$value" else value
    val escaped = safeValue.replace("\"", "\"\"")
    return if (escaped.contains(',') || escaped.contains('"') || escaped.contains('\n') || escaped.contains('\r')) {
        "\"" + escaped + "\""
    } else {
        escaped
    }
}


/**
 * Adds a UTF-8 byte-order mark to CSV text so desktop spreadsheet apps such as Excel
 * can reliably recognize Filipino names, peso symbols, and other non-ASCII characters.
 * Formula-leading cell protection is applied before this encoding step.
 */
internal fun csvUtf8Bytes(csvText: String): ByteArray =
    byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + csvText.toByteArray(Charsets.UTF_8)


/**
 * Wraps report text to the requested measured width. Long unbroken IDs and notes
 * are split on Unicode code-point boundaries instead of overflowing their table cell.
 * Explicit line breaks are retained.
 */
internal fun wrapReportText(
    text: String,
    maxWidth: Float,
    measure: (String) -> Float
): List<String> {
    require(maxWidth > 0f) { "maxWidth must be greater than zero" }

    val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
    val output = mutableListOf<String>()

    normalized.split('\n').forEach { paragraph ->
        if (paragraph.isBlank()) {
            output += ""
            return@forEach
        }

        var current = ""
        for (word in paragraph.trim().split(Regex("\\s+"))) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (measure(candidate) <= maxWidth) {
                current = candidate
                continue
            }

            if (current.isNotEmpty()) {
                output += current
                current = ""
            }

            if (measure(word) <= maxWidth) {
                current = word
                continue
            }

            var chunk = ""
            var offset = 0
            while (offset < word.length) {
                val codePoint = word.codePointAt(offset)
                val piece = String(Character.toChars(codePoint))
                val combined = chunk + piece

                if (chunk.isNotEmpty() && measure(combined) > maxWidth) {
                    output += chunk
                    chunk = piece
                    if (measure(chunk) > maxWidth) {
                        output += chunk
                        chunk = ""
                    }
                } else if (chunk.isEmpty() && measure(piece) > maxWidth) {
                    // A single glyph wider than the cell cannot be split further.
                    output += piece
                } else {
                    chunk = combined
                }
                offset += Character.charCount(codePoint)
            }
            current = chunk
        }

        if (current.isNotEmpty()) output += current
    }

    return output.ifEmpty { listOf("") }
}
