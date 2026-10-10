package com.tubalrr.agrivision

import androidx.compose.foundation.background

import androidx.compose.material.icons.outlined.CalendarMonth

import androidx.compose.material.icons.outlined.ArrowForward

import androidx.compose.material.icons.outlined.Add

import androidx.compose.material.icons.outlined.WbSunny

import androidx.compose.material.icons.outlined.Spa

import androidx.compose.material.icons.outlined.Map

import androidx.compose.ui.draw.clip

import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
internal fun FarmScreen(
    padding: PaddingValues,
    farm: com.tubalrr.agrivision.domain.model.FarmRecord,
    onOpenMap: () -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    livestock: List<Livestock>,
    livestockLifecycleEvents: List<LivestockLifecycleEvent>,
    crops: List<CropRecord>,
    inventory: List<InventoryItem>,
    inventoryTransactions: List<InventoryTransaction>,
    equipment: List<EquipmentRecord>,
    fields: List<com.tubalrr.agrivision.domain.model.FieldRecord>,
    cropLifecycleEvents: List<CropLifecycleEvent>,
    onAddLivestock: (Livestock) -> Unit,
    onAddLivestockLifecycleEvent: (LivestockLifecycleEvent) -> Unit,
    onAddCrop: (CropRecord) -> Unit,
    onAddInventory: (InventoryItem) -> Unit,
    onAddEquipment: (EquipmentRecord) -> Unit,
    onAddCropLifecycleEvent: (CropLifecycleEvent) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddFarmRecordDialog(
            category = category,
            onDismiss = { showAddDialog = false },
            onAddLivestock = {
                onAddLivestock(it)
                showAddDialog = false
            },
            onAddCrop = {
                onAddCrop(it)
                showAddDialog = false
            },
            onAddInventory = {
                onAddInventory(it)
                showAddDialog = false
            },
            onAddEquipment = {
                onAddEquipment(it)
                showAddDialog = false
            }
        )
    }

    val totalHeads = livestock.sumOf { it.currentPopulation }
    val lowStockCount = inventory.count {
        it.stock <= 0.0 || it.status.contains("low", ignoreCase = true)
    }
    val location = listOf(farm.barangay, farm.municipality, farm.province)
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { "Your farm workspace" }
    val estimatedArea = fields.sumOf { it.areaHectares }
    val sectionTitle = when (category) {
        "Livestock" -> "Livestock Management"
        "Crops" -> "Crop Management"
        "Fields" -> "Registered Fields"
        "Inventory" -> "Inputs & Inventory"
        else -> "Farm Equipment"
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1C6845), AgriGreenDeep, Color(0xFF0A2A1C))
                        )
                    )
                    .border(1.dp, AgriGreen.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
                    .padding(21.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Spa,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.07f),
                    modifier = Modifier
                        .size(142.dp)
                        .align(Alignment.TopEnd)
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Spa, null, tint = AgriGold, modifier = Modifier.size(15.dp))
                                Text(
                                    "FARM CONTROL CENTER",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.9.sp
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Outlined.WbSunny, null, tint = AgriGold, modifier = Modifier.size(23.dp))
                    }
                    Text(
                        farm.farmName.ifBlank { "Your Farm" },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2
                    )
                    Text(
                        location,
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your land, livestock and resources—organized in one place.",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FarmHeroStat("ANIMALS", totalHeads.toString(), Modifier.weight(1f))
                        FarmHeroStat("FIELDS", fields.size.toString(), Modifier.weight(1f))
                        FarmHeroStat("CROP CYCLES", crops.size.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionTitle("Farm at a glance")
                    Text(
                        if (lowStockCount > 0) "$" + lowStockCount + " stock alerts" else "Overview",
                        color = if (lowStockCount > 0) AgriDanger else AgriMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FarmOverviewMetric(
                        title = "Livestock groups",
                        value = livestock.size.toString(),
                        detail = totalHeads.toString() + " total heads",
                        icon = Icons.Outlined.Pets,
                        accent = Color(0xFFDFF0E3)
                    )
                    FarmOverviewMetric(
                        title = "Crop cycles",
                        value = crops.size.toString(),
                        detail = "Active records",
                        icon = Icons.Outlined.LocalFlorist,
                        accent = Color(0xFFE9F3D8)
                    )
                    FarmOverviewMetric(
                        title = "Farm fields",
                        value = fields.size.toString(),
                        detail = if (fields.isEmpty()) "Not registered" else String.format(java.util.Locale.US, "%.1f ha mapped", estimatedArea),
                        icon = Icons.Outlined.Map,
                        accent = Color(0xFFE0ECF6)
                    )
                    FarmOverviewMetric(
                        title = "Low stock",
                        value = lowStockCount.toString(),
                        detail = if (lowStockCount == 0) "No alerts" else "Needs checking",
                        icon = Icons.Outlined.Inventory2,
                        accent = if (lowStockCount == 0) Color(0xFFDFF0E3) else Color(0xFFFFE5D9)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Explore your farm")
                Text(
                    "Choose a workspace to view and update its records.",
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    FarmCategory("Livestock", Icons.Outlined.Pets, totalHeads.toString() + " heads", category == "Livestock") {
                        onCategoryChange("Livestock")
                    }
                    FarmCategory("Crops", Icons.Outlined.LocalFlorist, crops.size.toString() + " cycles", category == "Crops") {
                        onCategoryChange("Crops")
                    }
                    FarmCategory("Fields", Icons.Outlined.Map, fields.size.toString() + " mapped", category == "Fields") {
                        onCategoryChange("Fields")
                    }
                    FarmCategory("Inventory", Icons.Outlined.Inventory2, inventory.size.toString() + " items", category == "Inventory") {
                        onCategoryChange("Inventory")
                    }
                    FarmCategory("Equipment", Icons.Outlined.PrecisionManufacturing, equipment.size.toString() + " assets", category == "Equipment") {
                        onCategoryChange("Equipment")
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    SectionTitle(sectionTitle)
                    Text(
                        when (category) {
                            "Livestock" -> "Groups, population and lifecycle logs"
                            "Crops" -> "Planting progress and lifecycle records"
                            "Fields" -> "Area, crop assignments and map access"
                            "Inventory" -> "Stock levels, usage and acquisition records"
                            else -> "Tools, machines and maintenance notes"
                        },
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (category == "Equipment") {
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp)
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
        }

        when (category) {
            "Livestock" -> {
                item {
                    LivestockLifecycleSection(
                        livestock = livestock,
                        lifecycleEvents = livestockLifecycleEvents,
                        inventory = inventory,
                        onAddLivestock = onAddLivestock,
                        onAddLifecycleEvent = onAddLivestockLifecycleEvent
                    )
                }
            }

            "Crops" -> {
                item {
                    CropLifecycleSection(
                        crops = crops,
                        fields = fields,
                        lifecycleEvents = cropLifecycleEvents,
                        inventory = inventory,
                        onAddCrop = onAddCrop,
                        onAddLifecycleEvent = onAddCropLifecycleEvent
                    )
                }
            }

            "Fields" -> {
                if (fields.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = AgriCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(66.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFFE0ECF6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Map, null, tint = Color(0xFF356C9D), modifier = Modifier.size(32.dp))
                                }
                                Text("Start with your first field", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Register field boundaries, area and crop assignments on the Field Map.",
                                    color = AgriMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(onClick = onOpenMap, shape = RoundedCornerShape(14.dp)) {
                                    Icon(Icons.Outlined.Map, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text("Open Field Map")
                                }
                            }
                        }
                    }
                } else {
                    items(fields, key = { it.fieldId }) { field ->
                        FieldOverviewCard(field = field, onOpenMap = onOpenMap)
                    }
                    item {
                        OutlinedButton(
                            onClick = onOpenMap,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Outlined.Map, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Field Map")
                        }
                    }
                }
            }

            "Inventory" -> {
                item {
                    InventorySection(
                        inventory = inventory,
                        transactions = inventoryTransactions,
                        onAdd = onAddInventory
                    )
                }
            }

            "Equipment" -> {
                if (equipment.isEmpty()) {
                    item {
                        InfoCard(
                            "No equipment registered yet",
                            "Add your farm tools and machines so maintenance status and notes stay together."
                        )
                    }
                } else {
                    items(equipment) { item ->
                        FarmRecordCard(item.name, item.status, item.note, Icons.Outlined.PrecisionManufacturing)
                    }
                }
                item { AddHint("Keep maintenance and repair history for every machine or tool.") }
            }
        }
    }
}

@Composable
internal fun AddFarmRecordDialog(
    category: String,
    onDismiss: () -> Unit,
    onAddLivestock: (Livestock) -> Unit,
    onAddCrop: (CropRecord) -> Unit,
    onAddInventory: (InventoryItem) -> Unit,
    onAddEquipment: (EquipmentRecord) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }

    val title = when (category) {
        "Livestock" -> "Add Livestock"
        "Crops" -> "Add Crop"
        "Inventory" -> "Add Inventory"
        else -> "Add Equipment"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (category == "Crops") "Field / crop name" else "Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            when (category) {
                                "Livestock" -> "Animal type"
                                "Crops" -> "Crop type"
                                "Inventory" -> "Quantity"
                                else -> "Status"
                            }
                        )
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            when (category) {
                                "Livestock" -> "Quantity / heads"
                                "Crops" -> "Area"
                                "Inventory" -> "Stock status"
                                else -> "Maintenance note"
                            }
                        )
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            when (category) {
                                "Livestock" -> "Health status"
                                "Crops" -> "Growth stage"
                                "Inventory" -> "Item name / unit"
                                else -> "Equipment name / detail"
                            }
                        )
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    when (category) {
                        "Livestock" -> onAddLivestock(
                            Livestock(
                                name.trim(),
                                type.ifBlank { "Other" }.trim(),
                                value.toIntOrNull() ?: 0,
                                detail.ifBlank { "Healthy" }.trim()
                            )
                        )
                        "Crops" -> onAddCrop(
                            CropRecord(
                                name.trim(),
                                type.ifBlank { "Other" }.trim(),
                                value.ifBlank { "—" }.trim(),
                                detail.ifBlank { "Land Preparation" }.trim()
                            )
                        )
                        "Inventory" -> onAddInventory(
                            InventoryItem(
                                detail.ifBlank { name }.trim(),
                                type.ifBlank { "1" }.trim(),
                                value.ifBlank { "Good" }.trim()
                            )
                        )
                        else -> onAddEquipment(
                            EquipmentRecord(
                                name.trim(),
                                type.ifBlank { "Ready" }.trim(),
                                value.ifBlank { detail }.trim()
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@Composable
internal fun FarmCategory(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(112.dp)
            .height(106.dp)
            .border(1.dp, if (selected) AgriGreen else AgriLine, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = if (selected) AgriGreen else AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 0.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color.White.copy(alpha = 0.16f) else AgriGreenSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = name,
                    tint = if (selected) Color.White else AgriGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else AgriText,
                maxLines = 1
            )
            Text(
                count,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White.copy(alpha = 0.8f) else AgriMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FarmHeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(15.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        Text(label, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun FarmOverviewMetric(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Card(
        modifier = Modifier.width(152.dp).height(126.dp).border(1.dp, AgriLine, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AgriGreenDeep, modifier = Modifier.size(19.dp))
            }
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(value, color = AgriText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(detail, color = AgriMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun FieldOverviewCard(
    field: com.tubalrr.agrivision.domain.model.FieldRecord,
    onOpenMap: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AgriLine, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFE0ECF6)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Map, null, tint = Color(0xFF356C9D), modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(field.name, color = AgriText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        field.location.ifBlank { if (field.latitude != null && field.longitude != null) "GPS coordinates available" else "Location not set" },
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                StatusBadge(field.currentStatus)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(AgriCream).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("AREA", color = AgriMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(String.format(java.util.Locale.US, "%.2f ha", field.areaHectares), color = AgriText, fontWeight = FontWeight.ExtraBold)
                }
                Column(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(AgriCream).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("CURRENT CROP", color = AgriMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(field.crop.ifBlank { "Not assigned" }, color = AgriText, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = AgriMuted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Planting: " + field.plantingDate.ifBlank { "—" } + "  ·  Harvest: " + field.expectedHarvest.ifBlank { "—" },
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onOpenMap) {
                    Text("Map")
                    Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
            if (field.boundaryPoints.isNotEmpty()) {
                Text(field.boundaryPoints.size.toString() + " boundary points saved", color = AgriGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
