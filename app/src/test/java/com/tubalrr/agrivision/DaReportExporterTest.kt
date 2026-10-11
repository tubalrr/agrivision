package com.tubalrr.agrivision

import org.junit.Assert.assertEquals
import org.junit.Test

class DaReportExporterTest {
    @Test
    fun formulaLeadingTextIsForcedToText() {
        assertEquals("'=1+1", escapeCsvCell("=1+1"))
        assertEquals("'  =1+1", escapeCsvCell("  =1+1"))
        assertEquals("'+SUM(A1:A2)", escapeCsvCell("+SUM(A1:A2)"))
        assertEquals("'-cmd", escapeCsvCell("-cmd"))
        assertEquals("'@SUM(A1:A2)", escapeCsvCell("@SUM(A1:A2)"))
        assertEquals("'＝1+1", escapeCsvCell("＝1+1"))
        assertEquals("'\t=1+1", escapeCsvCell("\t=1+1"))
    }

    @Test
    fun normalNumericValuesRemainNumeric() {
        assertEquals("-125.50", escapeCsvCell("-125.50"))
        assertEquals("125.50", escapeCsvCell("125.50"))
        assertEquals("0", escapeCsvCell("0"))
    }

    @Test
    fun csvStructureIsEscapedAfterFormulaProtection() {
        assertEquals("\"Feed, \"\"premium\"\"\"", escapeCsvCell("Feed, \"premium\""))
        assertEquals("\"'=1, \"\"bad\"\"\"", escapeCsvCell("=1, \"bad\""))
        assertEquals("", escapeCsvCell(""))
        assertEquals("Regular feed", escapeCsvCell("Regular feed"))
    }

    @Test
    fun csvUtf8ExportHasBomAndPreservesFilipinoText() {
        val csv = "Farmer,Barangay,Amount\nJosé,Malangag,₱1250.00\n"
        val bytes = csvUtf8Bytes(csv)

        assertEquals(0xEF, bytes[0].toInt() and 0xFF)
        assertEquals(0xBB, bytes[1].toInt() and 0xFF)
        assertEquals(0xBF, bytes[2].toInt() and 0xFF)
        assertEquals(csv, String(bytes, Charsets.UTF_8).removePrefix("\uFEFF"))
    }

    @Test
    fun longUnbrokenReportTokensAreSplitToFitCellWidth() {
        val lines = wrapReportText("farm-ID-123456789", 5f) { it.length.toFloat() }

        assertEquals(listOf("farm-", "ID-12", "34567", "89"), lines)
        org.junit.Assert.assertTrue(lines.all { it.length <= 5 })
    }

    @Test
    fun reportTextWrappingKeepsWordBoundariesAndExplicitLineBreaks() {
        assertEquals(
            listOf("one two", "three"),
            wrapReportText("one two three", 7f) { it.length.toFloat() }
        )
        assertEquals(
            listOf("first line", "second"),
            wrapReportText("first line\nsecond", 11f) { it.length.toFloat() }
        )
    }

    @Test
    fun longColumnHeadingsKeepAllWordsWhenWrapped() {
        assertEquals(
            listOf("Current", "Population"),
            wrapReportText("Current Population", 10f) { it.length.toFloat() }
        )
    }

    @Test
    fun reportTextWrappingDoesNotSplitSurrogatePairs() {
        val lines = wrapReportText("ab😀cd", 2f) { it.length.toFloat() }

        assertEquals(listOf("ab", "😀", "cd"), lines)
        assertEquals(1, lines.sumOf { line -> line.count { Character.isHighSurrogate(it) } })
        assertEquals(1, lines.sumOf { line -> line.count { Character.isLowSurrogate(it) } })
    }

    private fun testSnapshot(
        production: List<ProductionRecord> = emptyList()
    ): com.tubalrr.agrivision.data.local.FarmSnapshot =
        com.tubalrr.agrivision.data.local.FarmSnapshot(
            profile = FarmerProfile(
                farmerName = "Test Farmer",
                farmerId = "F-001",
                contact = "09170000000",
                province = "North Cotabato",
                municipality = "Antipas",
                barangay = "Malangag",
                farmName = "Test Farm",
                farmSize = "1.0 ha",
                landTenure = "Owned",
                commodities = "Rice"
            ),
            livestock = emptyList(),
            crops = emptyList(),
            production = production,
            expenses = emptyList(),
            sales = emptyList(),
            inventory = emptyList(),
            equipment = emptyList(),
            tasks = emptyList(),
            assistance = emptyList(),
            fieldIncidents = emptyList(),
            incidentEvents = emptyList(),
            reportSubmission = ReportSubmission(
                status = "Draft",
                submittedDate = "",
                referenceNo = ""
            )
        )

    @Test
    fun largeCsvAndJsonExportsPreserveEveryRecord() {
        val records = (0 until 1_200).map { index ->
            ProductionRecord(
                product = "Crop-$index",
                quantity = "$index kg",
                period = "2026-10-11"
            )
        }
        val snapshot = testSnapshot(records)
        var csvBytes = byteArrayOf()
        var jsonBytes = byteArrayOf()

        DaReportExporter.write(snapshot, "Crop Production", DaReportFormat.CSV) {
            csvBytes = it
        }
        DaReportExporter.write(snapshot, "Crop Production", DaReportFormat.JSON) {
            jsonBytes = it
        }

        val csvText = String(csvBytes, Charsets.UTF_8).removePrefix("\uFEFF")
        val csvRowsIncludingHeader = csvText.trimEnd('\n').split('\n')
        assertEquals(records.size + 1, csvRowsIncludingHeader.size)
        org.junit.Assert.assertTrue(csvRowsIncludingHeader.last().contains("Crop-1199"))

        val json = org.json.JSONObject(String(jsonBytes, Charsets.UTF_8))
        assertEquals(records.size, json.getInt("recordCount"))
        val jsonRows = json.getJSONArray("records")
        assertEquals(records.size, jsonRows.length())
        assertEquals("Crop-1199", jsonRows.getJSONObject(records.lastIndex).getString("Product"))
    }

    @Test
    fun missingFarmRegistryExportsZeroDataRows() {
        val snapshot = testSnapshot()
        var csvBytes = byteArrayOf()
        var jsonBytes = byteArrayOf()

        DaReportExporter.write(snapshot, "Farm Registry", DaReportFormat.CSV) {
            csvBytes = it
        }
        DaReportExporter.write(snapshot, "Farm Registry", DaReportFormat.JSON) {
            jsonBytes = it
        }

        val csvLines = String(csvBytes, Charsets.UTF_8)
            .removePrefix("\uFEFF")
            .trimEnd('\n')
            .split('\n')
        assertEquals("Farm ID,Farm Name,Farmer ID,Province,Municipality,Barangay,Area,Land Tenure,Commodities,Registry Status", csvLines.single())

        val json = org.json.JSONObject(String(jsonBytes, Charsets.UTF_8))
        assertEquals(0, json.getInt("recordCount"))
        assertEquals(0, json.getJSONArray("records").length())
    }

    @Test
    fun emptyReportTablesAreValidButMalformedRowsAreRejected() {
        val emptyTable = DaReportExporter.validateData(
            DaReportExporter.Data(columns = listOf("Record ID"), rows = emptyList())
        )
        assertEquals(0, emptyTable.rows.size)

        try {
            DaReportExporter.validateData(
                DaReportExporter.Data(
                    columns = listOf("Record ID", "Name"),
                    rows = listOf(listOf("R-1"))
                )
            )
            org.junit.Assert.fail("A row with missing values should be rejected")
        } catch (expected: IllegalArgumentException) {
            org.junit.Assert.assertTrue(expected.message.orEmpty().contains("values for"))
        }
    }

}
