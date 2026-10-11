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
}
