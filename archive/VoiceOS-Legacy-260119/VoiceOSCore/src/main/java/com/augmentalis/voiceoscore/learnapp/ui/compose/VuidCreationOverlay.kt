/**
 * VuidCreationOverlay.kt - Compose equivalent of learnapp_overlay_vuid_creation.xml
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v12.1)
 * Created: 2025-12-22
 *
 * Material3 Compose debug overlay for VUID creation monitoring
 */

package com.augmentalis.voiceoscore.learnapp.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * VUID Creation Stats
 *
 * Data class for VUID creation statistics
 */
data class VuidCreationStats(
    val appName: String = "Unknown",
    val detected: Int = 0,
    val created: Int = 0,
    val filtered: Int = 0,
    val topFilteredType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isProcessing: Boolean = false
)

/**
 * VUID Creation Overlay (Compose)
 *
 * Replaces learnapp_overlay_vuid_creation.xml with Material3 Compose
 *
 * @param stats Current VUID creation statistics
 * @param modifier Optional modifier
 */
@Composable
fun VuidCreationOverlay(
    stats: VuidCreationStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .wrapContentSize()
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.88f)
        )
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 200.dp)
                .padding(12.dp)
        ) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VUID Creation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = if (stats.isProcessing) "⏳" else "✓",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // App Name
            Text(
                text = "App: ${stats.appName}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = Color.DarkGray
            )

            // Stats Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Detected Row
                StatRow(
                    label = "Detected:",
                    value = stats.detected.toString(),
                    color = Color(0xFF669900) // holo_green_dark
                )

                // Created Row
                StatRow(
                    label = "Created:",
                    value = stats.created.toString(),
                    color = Color(0xFF0099CC) // holo_blue_dark
                )

                // Rate Row
                val rate = if (stats.detected > 0) {
                    ((stats.created.toFloat() / stats.detected) * 100).toInt()
                } else 0
                StatRow(
                    label = "Rate:",
                    value = "$rate%",
                    color = Color(0xFFFF8800) // holo_orange_dark
                )

                // Filtered Row
                StatRow(
                    label = "Filtered:",
                    value = stats.filtered.toString(),
                    color = Color.DarkGray
                )
            }

            // Top Filtered Type (if available)
            if (stats.topFilteredType != null) {
                Text(
                    text = stats.topFilteredType,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Timestamp
            val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            Text(
                text = "Updated: ${formatter.format(Date(stats.timestamp))}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .wrapContentWidth(Alignment.End)
            )
        }
    }
}

/**
 * Stat Row Component
 *
 * Single statistic row with label and value
 */
@Composable
private fun StatRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
