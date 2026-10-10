package com.tubalrr.agrivision

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun AgriVisionApp(
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onExportCasePackage: () -> Unit = {},
    onExportDaReport: (String, DaReportFormat) -> Unit = { _, _ -> },
    farmViewModel: FarmViewModel = viewModel()
) {
    var selected by remember { mutableStateOf(0) }
    // Keep the selected farm section when switching tabs or opening a dashboard shortcut.
    var farmCategory by remember { mutableStateOf("Livestock") }

    val farmer by farmViewModel.farmer.collectAsStateWithLifecycle()
    val farm by farmViewModel.farm.collectAsStateWithLifecycle()
    val fields by farmViewModel.fields.collectAsStateWithLifecycle()
    val cropLifecycleEvents by farmViewModel.cropLifecycleEvents.collectAsStateWithLifecycle()
    val livestockLifecycleEvents by farmViewModel.livestockLifecycleEvents.collectAsStateWithLifecycle()
    val inventoryTransactions by farmViewModel.inventoryTransactions.collectAsStateWithLifecycle()
    val feedLogs by farmViewModel.feedLogs.collectAsStateWithLifecycle()
    val livestock by farmViewModel.livestock.collectAsStateWithLifecycle()
    val crops by farmViewModel.crops.collectAsStateWithLifecycle()
    val production by farmViewModel.production.collectAsStateWithLifecycle()
    val expenses by farmViewModel.expenses.collectAsStateWithLifecycle()
    val sales by farmViewModel.sales.collectAsStateWithLifecycle()
    val inventory by farmViewModel.inventory.collectAsStateWithLifecycle()
    val equipment by farmViewModel.equipment.collectAsStateWithLifecycle()
    val tasks by farmViewModel.tasks.collectAsStateWithLifecycle()
    val assistance by farmViewModel.assistance.collectAsStateWithLifecycle()
    val fieldIncidents by farmViewModel.fieldIncidents.collectAsStateWithLifecycle()
    val incidentEvents by farmViewModel.incidentEvents.collectAsStateWithLifecycle()
    val farmerProfile by farmViewModel.profile.collectAsStateWithLifecycle()
    val reportSubmission by farmViewModel.reportSubmission.collectAsStateWithLifecycle()
    val totalSales by farmViewModel.totalSales.collectAsStateWithLifecycle()
    val totalExpenses by farmViewModel.totalExpenses.collectAsStateWithLifecycle()
    val productionCount by farmViewModel.productionCount.collectAsStateWithLifecycle()
    val totalLivestock by farmViewModel.totalLivestock.collectAsStateWithLifecycle()
    val netIncome = totalSales - totalExpenses
    val openTasks = tasks.count { !it.done }

    val scheme = lightColorScheme(
        primary = AgriGreen,
        onPrimary = Color.White,
        background = AgriCream,
        surface = AgriCard,
        onBackground = AgriText,
        onSurface = AgriText,
        secondary = AgriGold,
        outline = AgriLine
    )

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            containerColor = AgriCream,
            bottomBar = { AgriBottomBar(selected) { selected = it } }
        ) { padding ->
            when (selected) {
                0 -> DashboardScreen(
                    padding = padding,
                    livestock = livestock,
                    crops = crops,
                    production = production,
                    expenses = expenses,
                    inventory = inventory,
                    tasks = tasks,
                    fieldIncidents = fieldIncidents,
                    incidentEvents = incidentEvents,
                    assistance = assistance,
                    farmer = farmer,
                    farm = farm,
                    fields = fields,
                    farmViewModel = farmViewModel,
                    onOpenTasks = { selected = 4 },
                    onOpenInventory = {
                        farmCategory = "Inventory"
                        selected = 2
                    },
                    totalAnimals = totalLivestock,
                    totalExpenses = totalExpenses,
                    totalSales = totalSales,
                    netIncome = netIncome,
                    openTasks = openTasks
                )
                1 -> FieldMapScreen(
                    padding = padding,
                    farm = farm,
                    fields = fields,
                    crops = crops,
                    incidents = fieldIncidents,
                    farmViewModel = farmViewModel
                )
                2 -> FarmScreen(
                    padding = padding,
                    category = farmCategory,
                    onCategoryChange = { farmCategory = it },
                    livestock = livestock,
                    livestockLifecycleEvents = livestockLifecycleEvents,
                    crops = crops,
                    inventory = inventory,
                    inventoryTransactions = inventoryTransactions,
                    equipment = equipment,
                    fields = fields,
                    cropLifecycleEvents = cropLifecycleEvents,
                    onAddLivestock = farmViewModel::addLivestock,
                    onAddLivestockLifecycleEvent = farmViewModel::addLivestockLifecycleEvent,
                    onAddCrop = farmViewModel::addCrop,
                    onAddInventory = farmViewModel::addInventory,
                    onAddEquipment = farmViewModel::addEquipment,
                    onAddCropLifecycleEvent = farmViewModel::addCropLifecycleEvent
                )
                3 -> ProductionFinanceScreen(
                    padding = padding,
                    production = production,
                    crops = crops,
                    livestock = livestock,
                    expenses = expenses,
                    sales = sales,
                    totalExpenses = totalExpenses,
                    totalSales = totalSales,
                    netIncome = netIncome,
                    totalAnimals = totalLivestock,
                    openTasks = openTasks,
                    farmerProfile = farmerProfile,
                    assistance = assistance,
                    fieldIncidents = fieldIncidents,
                    incidentEvents = incidentEvents,
                    onExportCasePackage = onExportCasePackage,
                    onExportDaReport = onExportDaReport,
                    exportStatus = farmViewModel.backupStatus.collectAsStateWithLifecycle().value,
                    reportSubmission = reportSubmission,
                    onProduction = farmViewModel::addProduction,
                    feedLogs = feedLogs,
                    onFeedLog = farmViewModel::addFeedLog,
                    onExpense = farmViewModel::addExpense,
                    onSale = farmViewModel::addSale,
                    onSubmissionSaved = farmViewModel::saveReportSubmission
                )
                4 -> TasksScreen(
                    padding = padding,
                    tasks = tasks,
                    inventory = inventory,
                    onAddTask = farmViewModel::addTask,
                    onToggleTask = farmViewModel::toggleTask
                )
                else -> ProfileScreen(
                    padding = padding,
                    profile = farmerProfile,
                    farmer = farmer,
                    farm = farm,
                    fields = fields,
                    onProfileSaved = farmViewModel::saveProfile,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    backupStatus = farmViewModel.backupStatus.collectAsStateWithLifecycle().value,
                    assistance = assistance,
                    onAddAssistance = farmViewModel::addAssistance,
                    onUpdateAssistance = farmViewModel::updateAssistance,
                    onAddField = farmViewModel::addField,
                    onUpdateField = farmViewModel::updateField
                )
            }
        }
    }
}

@Composable
internal fun AgriBottomBar(selected: Int, onSelected: (Int) -> Unit) {
    val items = listOf(
        "Dashboard" to Icons.Outlined.Dashboard,
        "Map" to Icons.Outlined.Map,
        "Farm" to Icons.Outlined.Yard,
        "Reports" to Icons.Outlined.Assessment,
        "Tasks" to Icons.Outlined.Checklist,
        "Profile" to Icons.Outlined.Person
    )
    NavigationBar(
        containerColor = AgriCard,
        tonalElevation = 0.dp,
        modifier = Modifier.height(82.dp),
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelected(index) },
                icon = {
                    Icon(
                        item.second,
                        contentDescription = item.first,
                        tint = if (selected == index) AgriGreen else AgriMuted,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        item.first,
                        fontWeight = if (selected == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = AgriGreen,
                    unselectedTextColor = AgriMuted,
                    indicatorColor = AgriGreenSoft
                )
            )
        }
    }
}
