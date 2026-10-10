package com.tubalrr.agrivision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AppSettingsDialog(
    rememberLastTab: Boolean,
    onRememberLastTabChange: (Boolean) -> Unit,
    hapticNavigation: Boolean,
    onHapticNavigationChange: (Boolean) -> Unit,
    confirmBackupImport: Boolean,
    onConfirmBackupImportChange: (Boolean) -> Unit,
    compactNavigation: Boolean,
    onCompactNavigationChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                color = AgriGreenSoft,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.size(52.dp)
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, tint = AgriGreen, modifier = Modifier.size(27.dp))
                }
            }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("App Preferences", fontWeight = FontWeight.ExtraBold)
                Text("AgriVision · Version 1.8.0", color = AgriMuted, style = MaterialTheme.typography.labelMedium)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item {
                    SettingsOption(
                        title = "Remember last section",
                        description = "Open the last tab you used the next time you launch AgriVision.",
                        icon = Icons.Outlined.BookmarkBorder,
                        checked = rememberLastTab,
                        onCheckedChange = onRememberLastTabChange
                    )
                }
                item {
                    SettingsOption(
                        title = "Navigation haptics",
                        description = "Use a light vibration when switching tabs.",
                        icon = Icons.Outlined.TouchApp,
                        checked = hapticNavigation,
                        onCheckedChange = onHapticNavigationChange
                    )
                }
                item {
                    SettingsOption(
                        title = "Confirm backup restore",
                        description = "Ask before opening the file picker to import a backup.",
                        icon = Icons.Outlined.Backup,
                        checked = confirmBackupImport,
                        onCheckedChange = onConfirmBackupImportChange
                    )
                }
                item {
                    SettingsOption(
                        title = "Compact navigation",
                        description = "Reduce the bottom navigation bar height to give content a little more room.",
                        icon = Icons.Outlined.ViewCompact,
                        checked = compactNavigation,
                        onCheckedChange = onCompactNavigationChange
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = AgriGreenSoft)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Your farm data stays local", fontWeight = FontWeight.Bold, color = AgriGreen)
                            Text(
                                "These preferences are saved on this device. They do not upload profile or farm information.",
                                color = AgriMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = { TextButton(onClick = onReset) { Text("Reset defaults") } }
    )
}

@Composable
private fun SettingsOption(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = AgriGreenSoft,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = AgriGreen, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = AgriText, style = MaterialTheme.typography.bodyMedium)
                Text(description, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
