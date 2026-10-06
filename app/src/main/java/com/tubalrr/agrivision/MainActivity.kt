package com.tubalrr.agrivision

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
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

        setContent {
            AgriVisionApp(
                onExportBackup = { exportBackup.launch("agrivision-backup.json") },
                onImportBackup = { importBackup.launch(arrayOf("application/json", "text/plain")) },
                onExportCasePackage = { exportCasePackage.launch("agrivision-da-case-package.json") }
            )
        }
    }
}
