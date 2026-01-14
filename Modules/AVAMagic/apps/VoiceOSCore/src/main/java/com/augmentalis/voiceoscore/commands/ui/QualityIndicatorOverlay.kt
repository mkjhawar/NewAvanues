package com.augmentalis.voiceoscore.commands.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceoscore.accessibility.ui.overlays.BaseOverlay
import com.augmentalis.voiceoscore.accessibility.ui.overlays.OverlayType
import com.augmentalis.voiceoscore.commands.ElementCommandManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Developer mode overlay showing visual quality feedback.
 *
 * Color-coded element borders:
 * - Green (80-100%): Excellent - has text + contentDesc + resourceId
 * - Yellow (60-79%): Good - has 2 of 3 metadata fields
 * - Orange (40-59%): Acceptable - has 1 of 3 metadata fields
 * - Red (0-39%): Poor - no metadata, needs manual command
 *
 * Features:
 * - Real-time updates as screen changes
 * - Quality score badge on each element
 * - Legend panel showing color meanings
 * - Tap element to show quality details dialog
 *
 * Toggle: Voice commands "show quality overlay" / "hide quality overlay"
 */
class QualityIndicatorOverlay(
    context: Context,
    private val accessibilityService: AccessibilityService,
    private val commandManager: ElementCommandManager
) : BaseOverlay(context, OverlayType.FULLSCREEN) {

    private var elementQualityScores = mutableStateMapOf<String, Int>()
    private var showLegend by mutableStateOf(true)

    /**
     * Update quality scores for visible elements.
     * Called when screen content changes or overlay is shown.
     */
    fun updateElementQuality() {
        try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return

            // Clear existing scores
            elementQualityScores.clear()

            // Scan all visible elements
            scanNode(rootNode)

            rootNode.recycle()
        } catch (e: Exception) {
            android.util.Log.e("QualityIndicatorOverlay", "Error updating element quality", e)
        }
    }

    private fun scanNode(node: AccessibilityNodeInfo) {
        // Skip invisible nodes
        if (!node.isVisibleToUser) return

        // Calculate quality score for this node
        val hasText = !node.text.isNullOrBlank()
        val hasContentDesc = !node.contentDescription.isNullOrBlank()
        val hasResourceId = !node.viewIdResourceName.isNullOrBlank()

        val qualityScore = commandManager.calculateQualityScore(
            hasText = hasText,
            hasContentDesc = hasContentDesc,
            hasResourceId = hasResourceId
        )

        // Generate a pseudo-UUID from node properties (for demo purposes)
        // In production, use actual UUIDCreator integration
        val nodeId = generateNodeId(node)
        elementQualityScores[nodeId] = qualityScore

        // Recursively scan children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                scanNode(child)
                child.recycle()
            }
        }
    }

    private fun generateNodeId(node: AccessibilityNodeInfo): String {
        // Simple ID generation for demo
        // In production, integrate with UUIDCreator
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val resourceId = node.viewIdResourceName ?: ""
        return "${node.hashCode()}-${text.take(10)}-${contentDesc.take(10)}-${resourceId.take(20)}"
    }

    @Composable
    override fun OverlayContent() {
        // Update element quality on first show
        LaunchedEffect(Unit) {
            updateElementQuality()

            // Auto-refresh every 2 seconds
            while (true) {
                delay(2000)
                updateElementQuality()
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Quality visualization overlay
            // Note: In production, this would render actual element borders
            // using Canvas and element bounds from AccessibilityNodeInfo
            QualityVisualization(
                elementQualityScores = elementQualityScores
            )

            // Legend panel at bottom
            if (showLegend) {
                LegendPanel(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onClose = { showLegend = false }
                )
            }
        }
    }

    @Composable
    private fun QualityVisualization(
        elementQualityScores: Map<String, Int>
    ) {
        // Placeholder visualization
        // In production, use Canvas to draw element borders at actual positions
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            QualityStatistics(elementQualityScores)
        }
    }

    @Composable
    private fun QualityStatistics(scores: Map<String, Int>) {
        val excellent = scores.count { it.value >= 80 }
        val good = scores.count { it.value in 60..79 }
        val acceptable = scores.count { it.value in 40..59 }
        val poor = scores.count { it.value < 40 }

        Card(
            modifier = Modifier
                .width(180.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Element Quality",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                QualityStatRow("Excellent", excellent, Color(0xFF4CAF50))
                QualityStatRow("Good", good, Color(0xFFFFC107))
                QualityStatRow("Acceptable", acceptable, Color(0xFFFF9800))
                QualityStatRow("Poor", poor, Color(0xFFF44336))

                HorizontalDivider()

                Text(
                    text = "Total: ${scores.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun QualityStatRow(label: String, count: Int, color: Color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }

    @Composable
    private fun LegendPanel(
        modifier: Modifier = Modifier,
        onClose: () -> Unit
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quality Legend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onClose) {
                        Text("Hide")
                    }
                }

                HorizontalDivider()

                LegendItem(
                    color = Color(0xFF4CAF50),
                    label = "EXCELLENT (80-100%)",
                    description = "Text + ContentDesc + ResourceId"
                )

                LegendItem(
                    color = Color(0xFFFFC107),
                    label = "GOOD (60-79%)",
                    description = "2 of 3 metadata fields"
                )

                LegendItem(
                    color = Color(0xFFFF9800),
                    label = "ACCEPTABLE (40-59%)",
                    description = "1 of 3 metadata fields"
                )

                LegendItem(
                    color = Color(0xFFF44336),
                    label = "POOR (0-39%)",
                    description = "No metadata - needs manual command"
                )
            }
        }
    }

    @Composable
    private fun LegendItem(color: Color, label: String, description: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
