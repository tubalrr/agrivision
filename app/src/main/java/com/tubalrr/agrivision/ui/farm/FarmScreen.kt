package com.tubalrr.agrivision

import androidx.compose.foundation.layout.*
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

    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth().border(1.dp, AgriGreen.copy(alpha = .25f), RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = AgriGreenDeep),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("MY FARM", color = AgriGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text("Everything on your farm.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("Manage livestock, crops, inputs and equipment in one organized workspace.", color = Color.White.copy(alpha = .78f))
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FarmCategory("Livestock", Icons.Outlined.Pets, category == "Livestock") { onCategoryChange("Livestock") }
                FarmCategory("Crops", Icons.Outlined.LocalFlorist, category == "Crops") { onCategoryChange("Crops") }
                FarmCategory("Inventory", Icons.Outlined.Inventory2, category == "Inventory") { onCategoryChange("Inventory") }
                FarmCategory("Equipment", Icons.Outlined.PrecisionManufacturing, category == "Equipment") { onCategoryChange("Equipment") }
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
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle("Farm Equipment")
                        TextButton(onClick = { showAddDialog = true }) { Text("+ Add") }
                    }
                }
                items(equipment) { item ->
                    FarmRecordCard(item.name, item.status, item.note, Icons.Outlined.PrecisionManufacturing)
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
    selected: Boolean,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(94.dp)
            .height(86.dp)
            .border(
                1.dp,
                if (selected) AgriGreen else AgriLine,
                cardShape
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AgriGreen else AgriCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = name,
                tint = if (selected) Color.White else AgriGreen,
                modifier = Modifier.size(23.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) Color.White else AgriText
            )
        }
    }
}
(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(90.dp).height(86.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) AgriGreenSoft else AgriCard
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
