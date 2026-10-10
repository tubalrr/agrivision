package com.tubalrr.agrivision

import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.platform.LocalView
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
    val context = LocalContext.current
    val view = LocalView.current
    val preferences = remember(context) {
        context.getSharedPreferences("agrivision_preferences", android.content.Context.MODE_PRIVATE)
    }

    var rememberLastTab by remember {
        mutableStateOf(preferences.getBoolean("remember_last_tab", false))
    }
    var hapticNavigation by remember {
        mutableStateOf(preferences.getBoolean("haptic_navigation", true))
    }
    var confirmBackupImport by remember {
        mutableStateOf(preferences.getBoolean("confirm_backup_import", true))
    }
    var compactNavigation by remember {
        mutableStateOf(preferences.getBoolean("compact_navigation", false))
    }
    var selected by remember {
        mutableStateOf(
            if (preferences.getBoolean("remember_last_tab", false)) {
                preferences.getInt("last_selected_tab", 0).coerceIn(0, 5)
            } else 0
        )
    }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(selected, rememberLastTab) {
        if (rememberLastTab) {
            preferences.edit().putInt("last_selected_tab", selected).apply()
        } else {
            preferences.edit().remove("last_selected_tab").apply()
        }
    }

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
        if (showSettings) {
            AppSettingsDialog(
                rememberLastTab = rememberLastTab,
                onRememberLastTabChange = {
                    rememberLastTab = it
                    preferences.edit().putBoolean("remember_last_tab", it).apply()
                    if (it) {
                        preferences.edit().putInt("last_selected_tab", selected).apply()
                    } else {
                        preferences.edit().remove("last_selected_tab").apply()
                    }
                },
                hapticNavigation = hapticNavigation,
                onHapticNavigationChange = {
                    hapticNavigation = it
                    preferences.edit().putBoolean("haptic_navigation", it).apply()
                },
                confirmBackupImport = confirmBackupImport,
                onConfirmBackupImportChange = {
                    confirmBackupImport = it
                    preferences.edit().putBoolean("confirm_backup_import", it).apply()
                },
                compactNavigation = compactNavigation,
                onCompactNavigationChange = {
                    compactNavigation = it
                    preferences.edit().putBoolean("compact_navigation", it).apply()
                },
                onReset = {
                    rememberLastTab = false
                    hapticNavigation = true
                    confirmBackupImport = true
                    compactNavigation = false
                    preferences.edit()
                        .putBoolean("remember_last_tab", false)
                        .putBoolean("haptic_navigation", true)
                        .putBoolean("confirm_backup_import", true)
                        .putBoolean("compact_navigation", false)
                        .remove("last_selected_tab")
                        .apply()
                },
                onDismiss = { showSettings = false }
            )
        }

        Scaffold(
            containerColor = AgriCream,
            bottomBar = {
                AgriBottomBar(
                    selected = selected,
                    compact = compactNavigation,
                    onSelected = { next ->
                        if (hapticNavigation) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        selected = next
                    }
                )
            }
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
                    farm = farm,
                    onOpenMap = { selected = 1 },
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
                    confirmBeforeImport = confirmBackupImport,
                    onOpenSettings = { showSettings = true },
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
internal fun AgriBottomBar(
    selected: Int,
    compact: Boolean = false,
    onSelected: (Int) -> Unit
) {
    val items = listOf(
        "Dashboard" to Icons.Outlined.Dashboard,
        "Map" to Icons.Outlined.Map,
        "Farm" to Icons.Outlined.Yard,
        "Reports" to Icons.Outlined.Assessment,
        "Tasks" to Icons.Outlined.Checklist,
        "Profile" to Icons.Outlined.Person
    )
    val barShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)

    NavigationBar(
        containerColor = AgriCard,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 70.dp else 82.dp)
            .clip(barShape)
            .border(1.dp, AgriLine, barShape),
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
                        modifier = Modifier.size(if (selected == index) 22.dp else 20.dp)
                    )
                },
                label = {
                    Text(
                        item.first,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AgriGreen,
                    selectedTextColor = AgriGreen,
                    unselectedIconColor = AgriMuted,
                    unselectedTextColor = AgriMuted,
                    indicatorColor = AgriGreenSoft
                )
            )
        }
    }
}
