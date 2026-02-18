package com.augmentalis.annotationavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.augmentalis.annotationavanue.model.AnnotationColors
import com.augmentalis.annotationavanue.model.AnnotationTool
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * Toolbar for annotation tools: tool selection, color presets, undo/redo, clear.
 * All buttons have AVID voice semantics.
 * Uses AvanueUI v5.1 theme tokens exclusively.
 */
@Composable
fun AnnotationToolbar(
    currentTool: AnnotationTool,
    currentColor: Long,
    onToolSelected: (AnnotationTool) -> Unit,
    onColorSelected: (Long) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Tool selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton("pen tool", Icons.Default.Create, "Pen", currentTool == AnnotationTool.PEN, colors.primary) {
                onToolSelected(AnnotationTool.PEN)
            }
            ToolButton("highlighter", Icons.Default.Create, "Highlighter", currentTool == AnnotationTool.HIGHLIGHTER, colors.tertiary) {
                onToolSelected(AnnotationTool.HIGHLIGHTER)
            }
            ToolButton("draw rectangle", Icons.Default.CropSquare, "Rectangle", currentTool == AnnotationTool.RECTANGLE, colors.primary) {
                onToolSelected(AnnotationTool.RECTANGLE)
            }
            ToolButton("draw circle", Icons.Default.Circle, "Circle", currentTool == AnnotationTool.CIRCLE, colors.primary) {
                onToolSelected(AnnotationTool.CIRCLE)
            }
            ToolButton("draw arrow", Icons.AutoMirrored.Filled.ArrowForward, "Arrow", currentTool == AnnotationTool.ARROW, colors.primary) {
                onToolSelected(AnnotationTool.ARROW)
            }
            ToolButton("draw line", Icons.Default.Remove, "Line", currentTool == AnnotationTool.LINE, colors.primary) {
                onToolSelected(AnnotationTool.LINE)
            }
            ToolButton("eraser tool", Icons.Default.Circle, "Eraser", currentTool == AnnotationTool.ERASER, colors.error) {
                onToolSelected(AnnotationTool.ERASER)
            }

            // Undo
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.semantics { contentDescription = "Voice: click undo annotation" }
            ) {
                Icon(
                    Icons.Default.Undo, "Undo",
                    tint = if (canUndo) colors.textPrimary else colors.textPrimary.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }
            // Redo
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.semantics { contentDescription = "Voice: click redo annotation" }
            ) {
                Icon(
                    Icons.Default.Redo, "Redo",
                    tint = if (canRedo) colors.textPrimary else colors.textPrimary.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }
            // Clear
            IconButton(
                onClick = onClear,
                modifier = Modifier.semantics { contentDescription = "Voice: click clear annotations" }
            ) {
                Icon(Icons.Default.Delete, "Clear", tint = colors.error, modifier = Modifier.size(20.dp))
            }
        }

        // Color presets row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AnnotationColors.PRESETS.forEach { preset ->
                val isSelected = currentColor == preset
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(preset.toULong()), CircleShape)
                        .then(
                            if (isSelected) Modifier.border(2.dp, colors.primary, CircleShape)
                            else Modifier.border(1.dp, colors.borderSubtle, CircleShape)
                        )
                        .clickable { onColorSelected(preset) }
                        .semantics { contentDescription = "Voice: click color" }
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    voiceLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    selectedTint: Color,
    onClick: () -> Unit
) {
    val colors = AvanueTheme.colors
    val tint = if (isSelected) selectedTint else colors.textPrimary.copy(alpha = 0.5f)
    val bgColor = if (isSelected) selectedTint.copy(alpha = 0.1f) else Color.Transparent

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .semantics { contentDescription = "Voice: click $voiceLabel" }
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
    }
}
