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
    farmViewModel: FarmViewModel = viewModel()
) {
    var selected by remember { mutableStateOf(0) }

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
                    farmViewModel = farmViewModel,
                    totalAnimals = totalLivestock,
                    totalExpenses = totalExpenses,
                    totalSales = totalSales,
                    netIncome = netIncome,
                    openTasks = openTasks
                )
                1 -> FarmScreen(
                    padding = padding,
                    livestock = livestock,
                    crops = crops,
                    inventory = inventory,
                    equipment = equipment,
                    onAddLivestock = farmViewModel::addLivestock,
                    onAddCrop = farmViewModel::addCrop,
                    onAddInventory = farmViewModel::addInventory,
                    onAddEquipment = farmViewModel::addEquipment
                )
                2 -> ProductionFinanceScreen(
                    padding = padding,
                    production = production,
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
                    reportSubmission = reportSubmission,
                    onProduction = farmViewModel::addProduction,
                    onExpense = farmViewModel::addExpense,
                    onSale = farmViewModel::addSale,
                    onSubmissionSaved = farmViewModel::saveReportSubmission
                )
                3 -> TasksScreen(
                    padding = padding,
                    tasks = tasks,
                    inventory = inventory,
                    onAddTask = farmViewModel::addTask,
                    onToggleTask = farmViewModel::toggleTask
                )
                else -> ProfileScreen(
                    padding = padding,
                    profile = farmerProfile,
                    onProfileSaved = farmViewModel::saveProfile,
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup,
                    assistance = assistance,
                    onAddAssistance = farmViewModel::addAssistance,
                    onUpdateAssistance = farmViewModel::updateAssistance
                )
            }
        }
    }
}

@Composable
internal fun AgriBottomBar(selected: Int, onSelected: (Int) -> Unit) {
    val items = listOf(
        "Dashboard" to Icons.Outlined.Dashboard,
        "Farm" to Icons.Outlined.Yard,
        "Reports" to Icons.Outlined.Assessment,
        "Tasks" to Icons.Outlined.Checklist,
        "Profile" to Icons.Outlined.Person
    )
    NavigationBar(
        containerColor = AgriCard,
        tonalElevation = 0.dp,
        modifier = Modifier.height(78.dp)
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
