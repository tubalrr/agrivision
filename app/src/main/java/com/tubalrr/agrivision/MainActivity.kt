package com.tubalrr.agrivision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private var pendingReportType: String? = null
    private val farmViewModel: FarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val exportBackup = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) farmViewModel.exportBackup(uri)
        }

        val importBackup = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) farmViewModel.importBackup(uri)
        }

        val exportCasePackage = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) farmViewModel.exportCasePackage(uri)
        }

        val exportReportPdf = registerForActivityResult(
            ActivityResultContracts.CreateDocument(DaReportFormat.PDF.mimeType)
        ) { uri ->
            if (uri != null) pendingReportType?.let { farmViewModel.exportDaReport(uri, it, DaReportFormat.PDF) }
        }
        val exportReportCsv = registerForActivityResult(
            ActivityResultContracts.CreateDocument(DaReportFormat.CSV.mimeType)
        ) { uri ->
            if (uri != null) pendingReportType?.let { farmViewModel.exportDaReport(uri, it, DaReportFormat.CSV) }
        }
        val exportReportJson = registerForActivityResult(
            ActivityResultContracts.CreateDocument(DaReportFormat.JSON.mimeType)
        ) { uri ->
            if (uri != null) pendingReportType?.let { farmViewModel.exportDaReport(uri, it, DaReportFormat.JSON) }
        }

        setContent {
            AgriVisionApp(
                onExportBackup = { exportBackup.launch("agrivision-backup.json") },
                onImportBackup = { importBackup.launch(arrayOf("application/json", "text/plain")) },
                onExportCasePackage = { exportCasePackage.launch("agrivision-da-case-package.json") },
                onExportDaReport = { reportType, format ->
                    pendingReportType = reportType
                    when (format) {
                        DaReportFormat.PDF -> exportReportPdf.launch(DaReportExporter.fileName(reportType, format))
                        DaReportFormat.CSV -> exportReportCsv.launch(DaReportExporter.fileName(reportType, format))
                        DaReportFormat.JSON -> exportReportJson.launch(DaReportExporter.fileName(reportType, format))
                    }
                }
            )
        }
    }
}
