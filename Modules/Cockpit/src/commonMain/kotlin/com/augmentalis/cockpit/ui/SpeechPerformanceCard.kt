/**
 * SpeechPerformanceCard.kt - Dashboard card for speech engine metrics
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Created: 2026-02-24
 *
 * Displays a compact overview of speech engine performance:
 * engine name, model, latency, RTF, confidence, success rate,
 * total transcriptions, and detected language.
 * Color-coded health indicator (green/yellow/red) based on thresholds.
 */
package com.augmentalis.cockpit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueCard
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.speechrecognition.SpeechMetricsSnapshot

/**
 * Compact dashboard card showing live speech engine performance.
 *
 * Displays:
 * - Engine name + model size with health status indicator
 * - Key metrics: latency, RTF, confidence, success rate
 * - Total transcriptions and detected language
 *
 * Health status colors:
 * - GOOD (green): success >= 80%, latency < 2s
 * - WARNING (yellow): success >= 50% or latency < 5s
 * - CRITICAL (red): success < 50% or latency >= 5s
 * - IDLE (gray): no transcriptions yet
 */
@Composable
fun SpeechPerformanceCard(
    metrics: SpeechMetricsSnapshot,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    val healthColor = when (metrics.healthStatus) {
        SpeechMetricsSnapshot.HealthStatus.GOOD -> Color(0xFF4CAF50)
        SpeechMetricsSnapshot.HealthStatus.WARNING -> Color(0xFFFFC107)
        SpeechMetricsSnapshot.HealthStatus.CRITICAL -> Color(0xFFF44336)
        SpeechMetricsSnapshot.HealthStatus.IDLE -> colors.textPrimary.copy(alpha = 0.3f)
    }

    AvanueCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Voice: speech performance" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: engine name + health indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = healthColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = metrics.engineName,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                metrics.modelSize?.let { model ->
                    Text(
                        text = " ($model)",
                        color = colors.textPrimary.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Health dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(healthColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = metrics.healthStatus.name,
                    color = healthColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics grid — 2 rows of 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricCell(
                    label = "Latency",
                    value = "${metrics.avgLatencyMs}ms",
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "RTF",
                    value = "%.2f".format(metrics.avgRTF),
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "Confidence",
                    value = "%.0f%%".format(metrics.avgConfidence * 100),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricCell(
                    label = "Success",
                    value = "%.0f%%".format(metrics.successRate),
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "Total",
                    value = "${metrics.totalTranscriptions}",
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label = "Language",
                    value = metrics.detectedLanguage ?: "auto",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Column(modifier = modifier.padding(horizontal = 2.dp)) {
        Text(
            text = label,
            color = colors.textPrimary.copy(alpha = 0.4f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
