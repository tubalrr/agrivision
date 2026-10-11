package com.tubalrr.agrivision

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val InventoryCategories = listOf(
    "Fertilizer",
    "Seeds",
    "Feed",
    "Medicine",
    "Pesticide",
    "Tools",
    "Fuel",
    "Other"
)

private val InventoryUnits = listOf(
    "bag",
    "sack",
    "kg",
    "liter",
    "bottle",
    "piece",
    "box",
    "unit"
)

@Composable
internal fun InventorySection(
    inventory: List<InventoryItem>,
    transactions: List<InventoryTransaction>,
    onAdd: (InventoryItem) -> Unit,
    onRecordMovement: (String, String, Double, String, String, (String?) -> Unit) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var movementItem by remember { mutableStateOf<InventoryItem?>(null) }
    var movementMessage by remember { mutableStateOf<String?>(null) }

    if (showDialog) {
        AddInventoryItemDialog(
            onDismiss = { showDialog = false },
            onSave = {
                onAdd(it)
                showDialog = false
            }
        )
    }

    movementItem?.let { selectedItem ->
        RecordInventoryMovementDialog(
            item = selectedItem,
            onDismiss = { movementItem = null },
            onSave = { type, quantity, date, notes ->
                onRecordMovement(selectedItem.inventoryId, type, quantity, date, notes) { error ->
                    movementMessage = error ?: "Stock movement saved."
                }
                movementItem = null
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(
            modifier = Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Input & Inventory", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Text(
                        "Stock, acquisition details and automatic usage deductions.",
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Button(
                    onClick = { showDialog = true },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("+ Add Stock")
                }
            }

            movementMessage?.let { message ->
                Text(
                    message,
                    color = if (message.startsWith("Stock movement saved")) AgriGreen else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (inventory.isEmpty()) {
                Text(
                    "Register fertilizer, seeds, feed, medicine, pesticides, tools or fuel.",
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                inventory.forEach { item ->
                    val used = transactions
                        .filter { it.inventoryId == item.inventoryId && it.type == "USAGE" }
                        .sumOf { it.quantity }

                    InventoryItemCard(
                        item = item,
                        used = used,
                        transactions = transactions.count { it.inventoryId == item.inventoryId },
                        onRecordMovement = { movementMessage = null; movementItem = item }
                    )
                }
            }

            val recentUsage = transactions.filter { it.type == "USAGE" && it.sourceType != "Manual" }.take(8)
            if (recentUsage.isNotEmpty()) {
                Text("Recent Usage", color = AgriGreen, fontWeight = FontWeight.Bold)
                recentUsage.forEach { transaction ->
                    val item = inventory.firstOrNull { it.inventoryId == transaction.inventoryId }
                    Text(
                        transaction.date + " · " +
                                (item?.name ?: "Inventory") + " · -" +
                                formatInventoryNumber(transaction.quantity) + " " +
                                transaction.unit + " · " + transaction.sourceType,
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            val manualMovements = transactions.filter { it.sourceType == "Manual" }.take(8)
            if (manualMovements.isNotEmpty()) {
                Text("Recent Stock Movements", color = AgriGreen, fontWeight = FontWeight.Bold)
                manualMovements.forEach { transaction ->
                    val item = inventory.firstOrNull { it.inventoryId == transaction.inventoryId }
                    val label = when (transaction.type) {
                        "PURCHASE" -> "Stock in"
                        "USAGE" -> "Used"
                        "LOSS" -> "Loss / waste"
                        else -> transaction.type
                    }
                    val sign = if (transaction.type == "PURCHASE") "+" else "−"
                    Text(
                        transaction.date + " · " + (item?.name ?: "Inventory") + " · " +
                            sign + formatInventoryNumber(transaction.quantity) + " " + transaction.unit +
                            " · " + label +
                            (transaction.notes.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                        color = AgriMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    used: Double,
    transactions: Int,
    onRecordMovement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCream)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    Text(
                        item.category + " · " + item.unit,
                        color = AgriGreen,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (item.stock <= 0.0) MaterialTheme.colorScheme.errorContainer else AgriGreenSoft
                ) {
                    Text(
                        if (item.stock <= 0.0) "Out of Stock" else item.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        color = if (item.stock <= 0.0) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            AgriGreen
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InventoryMetric("Remaining", formatInventoryNumber(item.stock) + " " + item.unit, Modifier.weight(1f))
                InventoryMetric("Used", formatInventoryNumber(used) + " " + item.unit, Modifier.weight(1f))
            }

            Text(
                "Purchase price: ₱" + String.format(Locale.US, "%.2f", item.purchasePrice),
                color = AgriText,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Supplier: " + item.supplier.ifBlank { "—" },
                color = AgriText,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Acquired: " + item.dateAcquired.ifBlank { "—" } +
                        " · Expiry: " + item.expiryDate.ifBlank { "—" },
                color = AgriMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Transactions: " + transactions,
                color = AgriMuted,
                style = MaterialTheme.typography.labelSmall
            )
            OutlinedButton(
                onClick = onRecordMovement,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp)
            ) {
                Text("Record stock movement")
            }
        }
    }
}

@Composable
private fun InventoryMetric(
    label: String,
    value: String,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Column(Modifier.padding(9.dp)) {
            Text(label, color = AgriMuted, style = MaterialTheme.typography.labelSmall)
            Text(value, color = AgriText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RecordInventoryMovementDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }
    var type by remember { mutableStateOf("USAGE") }
    var quantity by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        runCatching {
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: calendar.time
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
                    Calendar.getInstance().apply { set(year, month, day) }.time
                )
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    val amount = quantity.toDoubleOrNull()
    val addsStock = type == "PURCHASE"
    val amountValid = amount != null && amount.isFinite() && amount > 0.0 &&
        (addsStock || amount <= item.stock)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record stock movement", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, color = AgriGreen)
                Text(
                    "Available: " + formatInventoryNumber(item.stock) + " " + item.unit,
                    color = AgriMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "PURCHASE" to "Stock in",
                        "USAGE" to "Use",
                        "LOSS" to "Loss / waste"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = type == value,
                            onClick = { type = value },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quantity (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = quantity.isNotBlank() && !amountValid,
                    supportingText = {
                        if (type != "PURCHASE") {
                            Text("Cannot record more than the available stock.")
                        }
                    },
                    singleLine = true
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Movement date: $date")
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("e.g. used for field 2") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = amountValid,
                onClick = { onSave(type, amount!!, date, notes.trim()) }
            ) {
                Text(when (type) {
                    "PURCHASE" -> "Add to stock"
                    "USAGE" -> "Record usage"
                    else -> "Record loss"
                })
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun AddInventoryItemDialog(
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit
) {
    val context = LocalContext.current
    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }
    var stock by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("bag") }
    var purchasePrice by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var dateAcquired by remember { mutableStateOf(today) }
    var expiryDate by remember { mutableStateOf("") }
    var pickerTarget by remember { mutableStateOf("") }

    if (pickerTarget.isNotBlank()) {
        val calendar = Calendar.getInstance()
        val current = if (pickerTarget == "acquired") dateAcquired else expiryDate
        try {
            calendar.time = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .parse(current.ifBlank { today }) ?: calendar.time
        } catch (_: Exception) {
        }

        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply { set(year, month, day) }
                val value = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.time)
                if (pickerTarget == "acquired") {
                    dateAcquired = value
                } else {
                    expiryDate = value
                }
                pickerTarget = ""
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { pickerTarget = "" }
            show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Inventory Stock", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 540.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Item name") },
                        supportingText = {
                            Text("Use the same item name in farm activities for automatic deduction.")
                        },
                        singleLine = true
                    )
                }
                item {
                    Text("Category", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InventoryCategories.forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { category = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { stock = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Stock") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { purchasePrice = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Purchase price") },
                            singleLine = true
                        )
                    }
                }
                item {
                    Text("Unit", color = AgriGreen, fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        InventoryUnits.forEach { option ->
                            FilterChip(
                                selected = unit == option,
                                onClick = { unit = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = supplier,
                        onValueChange = { supplier = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Supplier") },
                        singleLine = true
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { pickerTarget = "acquired" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Date acquired: " + dateAcquired)
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { pickerTarget = "expiry" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Expiry date: " + expiryDate.ifBlank { "Set date" })
                    }
                }
            }
        },
        confirmButton = {
            val stockValue = stock.toDoubleOrNull()
            val priceValue = purchasePrice.toDoubleOrNull() ?: 0.0
            TextButton(
                enabled = name.isNotBlank() && stockValue != null && stockValue >= 0.0,
                onClick = {
                    val value = stockValue ?: 0.0
                    onSave(
                        InventoryItem(
                            name = name.trim(),
                            quantity = formatInventoryNumber(value) + " " + unit,
                            status = if (value <= 0.0) "Out of Stock" else "In Stock",
                            inventoryId = "INV-" + System.currentTimeMillis(),
                            category = category,
                            stock = value,
                            unit = unit,
                            purchasePrice = priceValue.coerceAtLeast(0.0),
                            supplier = supplier.trim(),
                            dateAcquired = dateAcquired.trim(),
                            expiryDate = expiryDate.trim()
                        )
                    )
                }
            ) { Text("Save Stock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatInventoryNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString()
    else String.format(Locale.US, "%.2f", value)
