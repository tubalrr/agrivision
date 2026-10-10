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


@Composable
internal fun StatusBadge(status: String) {
    val normalized = status.trim().lowercase()
    val (background, foreground) = when {
        normalized.contains("complete") || normalized == "verified" ||
                normalized == "approved" || normalized == "healthy" ||
                normalized == "ready" || normalized == "received" ->
            AgriGreenSoft to AgriGreen
        normalized.contains("critical") || normalized.contains("rejected") ||
                normalized.contains("overdue") || normalized.contains("high") ->
            Color(0xFFFFE5E1) to AgriDanger
        normalized.contains("low") || normalized.contains("pending") ||
                normalized.contains("review") || normalized.contains("draft") ||
                normalized.contains("assistance") ->
            AgriGoldSoft to AgriWarning
        else -> Color(0xFFF0F3EF) to AgriMuted
    }

    Text(
        status.ifBlank { "Unknown" },
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        color = foreground,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun FarmRecordCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AgriLine, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(AgriGreenSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(value, color = AgriGreen, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun SimpleRecordCard(title: String, value: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FarmRecordCard(title, value, detail, icon)
}

@Composable
internal fun StatusRow(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(detail, color = AgriMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun StatCard(
    title: String,
    value: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier.border(1.dp, AgriLine.copy(alpha = .85f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = .72f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AgriGreen, modifier = Modifier.size(21.dp))
            }
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AgriText)
            Text(detail, color = AgriGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun DashboardMiniStat(title: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun SummaryCard(title: String, value: String, body: String) {
    Card(
        Modifier.fillMaxWidth().border(1.dp, AgriLine, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriGreenSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(title, color = AgriMuted, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AgriGreen)
            Text(body, color = AgriText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun AttentionCard(primary: String, secondary: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = AgriCard)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(primary, fontWeight = FontWeight.Bold)
            Text(secondary, color = AgriMuted)
        }
    }
}

@Composable
internal fun AddHint(text: String) {
    Card(
        Modifier.fillMaxWidth().border(1.dp, AgriGold.copy(alpha = .25f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = AgriGoldSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Lightbulb, null, tint = AgriGreen, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, color = AgriText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ScreenHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier
                    .width(22.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(AgriGold)
            )
            Text(
                "FARM WORKSPACE",
                color = AgriGreen,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = AgriText
        )
        Text(subtitle, color = AgriMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AgriGreen)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = AgriText
        )
    }
}

@Composable
internal fun InfoCard(title: String, body: String) {
    Card(
        Modifier.fillMaxWidth().border(1.dp, AgriLine, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = AgriCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, color = AgriMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal fun money(value: Double): String {
    return String.format(java.util.Locale.US, "%,.0f", value)
}
