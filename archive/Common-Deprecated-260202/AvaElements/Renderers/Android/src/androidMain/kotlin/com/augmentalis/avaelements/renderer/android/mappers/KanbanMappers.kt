package com.augmentalis.avaelements.renderer.android.mappers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.flutter.material.charts.*

/**
 * Android Compose mappers for Kanban, Heatmap, and TreeMap components
 *
 * @since 3.0.0-flutter-parity
 */

/**
 * Render Heatmap component using Canvas
 */
@Composable
fun HeatmapMapper(component: Heatmap) {
    if (!component.isValid()) {
        Text(
            text = "Invalid heatmap data",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val (rows, columns) = component.getDimensions()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            component.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Heatmap Grid
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((component.cellSize * rows).dp)
            ) {
                val cellWidth = component.cellSize.dp.toPx()
                val cellHeight = component.cellSize.dp.toPx()

                component.data.forEachIndexed { rowIndex, row ->
                    row.forEachIndexed { colIndex, value ->
                        val x = colIndex * cellWidth
                        val y = rowIndex * cellHeight
                        val color = parseColor(component.getColorForValue(value))

                        // Draw cell
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(cellWidth - 2.dp.toPx(), cellHeight - 2.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )

                        // Draw value if enabled
                        if (component.showValues) {
                            // Note: Text drawing in Canvas requires TextMeasurer
                            // For simplicity, we'll skip text rendering here
                            // In production, use drawText with rememberTextMeasurer()
                        }
                    }
                }
            }

            // Row labels
            if (component.rowLabels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rows: ${component.rowLabels.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Render TreeMap component using Canvas
 */
@Composable
fun TreeMapMapper(component: TreeMap) {
    if (!component.isValid()) {
        Text(
            text = "Invalid treemap data",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    val totalValue = component.getTotalValue()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(component.height.dp)
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            component.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Simple vertical layout for treemap items (proportional heights)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                component.items.forEach { item ->
                    val proportion = if (totalValue > 0) {
                        item.value / totalValue
                    } else {
                        1f / component.items.size
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(proportion)
                            .clickable { component.onItemClick?.invoke(item) },
                        colors = CardDefaults.cardColors(
                            containerColor = parseColor(item.getEffectiveColor("#2196F3"))
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (component.showLabels) {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                if (component.showValues) {
                                    Text(
                                        text = String.format("%.1f%%", item.getPercentage(totalValue)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Render Kanban component
 */
@Composable
fun KanbanMapper(component: Kanban) {
    if (!component.isValid()) {
        Text(
            text = "Invalid kanban data",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = component.getAccessibilityDescription()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            component.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Kanban Board (Horizontal scrolling columns)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(component.columns) { column ->
                    KanbanColumnMapper(
                        column = column,
                        onCardClick = { cardId ->
                            component.onCardClick?.invoke(column.id, cardId)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Render Kanban Column
 */
@Composable
fun KanbanColumnMapper(
    column: Kanban.KanbanColumnData,
    onCardClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Column header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = column.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "${column.cards.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // WIP limit indicator
            column.maxCards?.let { maxCards ->
                if (column.isAtCapacity()) {
                    Text(
                        text = "At capacity ($maxCards)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(column.cards) { card ->
                    KanbanCardMapper(
                        card = card,
                        onClick = { onCardClick(card.id) }
                    )
                }
            }
        }
    }
}

/**
 * Render Kanban Card
 */
@Composable
fun KanbanCardMapper(
    card: Kanban.KanbanCardData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = card.getAccessibilityDescription()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Priority indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            parseColor(card.getPriorityColor()),
                            CircleShape
                        )
                )
            }

            // Description
            card.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Tags
            if (card.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    card.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Assignee
            card.assignee?.let {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
