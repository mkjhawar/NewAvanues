/**
 * CommandLabelOverlay.kt - Voice command label overlay
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-03
 * 
 * Displays voice command labels over UI elements with intelligent positioning
 * and collision detection. Port of Legacy Avenue's VoiceCommandOverlayView.
 */
package com.augmentalis.voiceos.accessibility.ui.overlays

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import androidx.compose.foundation.BorderStroke

/**
 * Data class representing a voice command label
 */
data class VoiceCommandLabel(
    val id: String,
    val command: String,
    val bounds: Rect,
    val isClickable: Boolean = true,
    val isDuplicate: Boolean = false,
    val duplicateIndex: Int = 0,
    val confidence: Float = 1.0f
)

/**
 * Overlay for displaying voice command labels over UI elements
 */
class CommandLabelOverlay(
    context: Context,
    private val onCommandSelected: (String) -> Unit = {}
) : BaseOverlay(context, OverlayType.POSITIONED) {
    
    
    companion object {
        private const val TAG = "CommandLabelOverlay"
        private const val MIN_LABEL_SPACING = 4 // dp
        private const val LABEL_PADDING = 8 // dp
        private const val COLLISION_THRESHOLD = 20 // pixels
        private const val MAX_LABELS_VISIBLE = 50
        private const val FADE_ANIMATION_MS = 300L
    }
    
    // Mutable state for command labels
    private var commandLabels = mutableStateListOf<VoiceCommandLabel>()
    private var screenWidth = 0
    private var screenHeight = 0
    
    /**
     * Update command labels from accessibility nodes
     */
    fun updateCommandLabels(nodes: List<AccessibilityNodeInfo>) {
        overlayScope.launch {
            val newLabels = processNodes(nodes)
            val positionedLabels = applyCollisionDetection(newLabels)
            
            // Animate the update
            commandLabels.clear()
            commandLabels.addAll(positionedLabels.take(MAX_LABELS_VISIBLE))
            
            if (commandLabels.isNotEmpty() && !isVisible()) {
                show()
            } else if (commandLabels.isEmpty() && isVisible()) {
                hide()
            }
        }
    }
    
    /**
     * Process accessibility nodes into command labels
     */
    private fun processNodes(nodes: List<AccessibilityNodeInfo>): List<VoiceCommandLabel> {
        return nodes.mapNotNull { node ->
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            
            // Skip invalid or off-screen elements
            if (bounds.isEmpty || !isValidBounds(bounds)) {
                return@mapNotNull null
            }
            
            val command = extractCommand(node) ?: return@mapNotNull null
            
            VoiceCommandLabel(
                id = node.hashCode().toString(),
                command = command,
                bounds = bounds,
                isClickable = node.isClickable,
                confidence = calculateConfidence(node)
            )
        }
    }
    
    /**
     * Extract voice command from node
     */
    private fun extractCommand(node: AccessibilityNodeInfo): String? {
        // Priority order: contentDescription > text > hintText
        return node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: node.hintText?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }
    
    /**
     * Calculate confidence score for command
     */
    private fun calculateConfidence(node: AccessibilityNodeInfo): Float {
        var confidence = 1.0f
        
        // Reduce confidence for generic labels
        val text = extractCommand(node)?.lowercase() ?: return 0f
        if (text in listOf("button", "image", "icon", "view")) {
            confidence *= 0.5f
        }
        
        // Increase confidence for clickable elements
        if (node.isClickable) confidence *= 1.2f
        
        // Increase confidence for focused elements
        if (node.isFocused) confidence *= 1.5f
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Check if bounds are valid for display
     */
    private fun isValidBounds(bounds: Rect): Boolean {
        return bounds.width() > 0 && 
               bounds.height() > 0 &&
               bounds.left >= 0 && 
               bounds.top >= 0 &&
               bounds.right <= screenWidth &&
               bounds.bottom <= screenHeight
    }
    
    /**
     * Apply collision detection to prevent overlapping labels
     */
    private fun applyCollisionDetection(labels: List<VoiceCommandLabel>): List<VoiceCommandLabel> {
        if (labels.isEmpty()) return labels
        
        val sortedLabels = labels.sortedByDescending { it.confidence }
        val adjustedLabels = mutableListOf<VoiceCommandLabel>()
        val occupiedRects = mutableListOf<Rect>()
        
        for (label in sortedLabels) {
            var adjustedBounds = Rect(label.bounds)
            var attempts = 0
            
            // Try to find non-colliding position
            while (hasCollision(adjustedBounds, occupiedRects) && attempts < 5) {
                adjustedBounds = adjustPosition(adjustedBounds, occupiedRects)
                attempts++
            }
            
            if (attempts < 5) {
                adjustedLabels.add(label.copy(bounds = adjustedBounds))
                occupiedRects.add(adjustedBounds)
            }
        }
        
        return adjustedLabels
    }
    
    /**
     * Check if bounds collide with occupied rects
     */
    private fun hasCollision(bounds: Rect, occupiedRects: List<Rect>): Boolean {
        return occupiedRects.any { occupied ->
            Rect.intersects(
                Rect(bounds.left - COLLISION_THRESHOLD, 
                     bounds.top - COLLISION_THRESHOLD,
                     bounds.right + COLLISION_THRESHOLD,
                     bounds.bottom + COLLISION_THRESHOLD),
                occupied
            )
        }
    }
    
    /**
     * Adjust position to avoid collisions
     */
    private fun adjustPosition(bounds: Rect, occupiedRects: List<Rect>): Rect {
        val adjusted = Rect(bounds)
        
        // Try moving down first
        adjusted.offset(0, bounds.height() + COLLISION_THRESHOLD)
        if (!hasCollision(adjusted, occupiedRects)) return adjusted
        
        // Try moving right
        adjusted.set(bounds)
        adjusted.offset(bounds.width() + COLLISION_THRESHOLD, 0)
        if (!hasCollision(adjusted, occupiedRects)) return adjusted
        
        // Try moving up
        adjusted.set(bounds)
        adjusted.offset(0, -(bounds.height() + COLLISION_THRESHOLD))
        if (!hasCollision(adjusted, occupiedRects)) return adjusted
        
        // Try moving left
        adjusted.set(bounds)
        adjusted.offset(-(bounds.width() + COLLISION_THRESHOLD), 0)
        
        return adjusted
    }
    
    @Composable
    override fun OverlayContent() {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        
        LaunchedEffect(configuration) {
            screenWidth = configuration.screenWidthDp * density.density.toInt()
            screenHeight = configuration.screenHeightDp * density.density.toInt()
        }
        
        // Display command labels
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            commandLabels.forEach { label ->
                CommandLabel(
                    label = label,
                    onClick = { onCommandSelected(label.command) }
                )
            }
        }
    }
    
    @Composable
    private fun CommandLabel(
        label: VoiceCommandLabel,
        @Suppress("UNUSED_PARAMETER")
        onClick: () -> Unit
    ) {
        val density = LocalDensity.current
        
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = label.bounds.left,
                        y = label.bounds.top
                    )
                }
                .width(with(density) { label.bounds.width().toDp() })
                .alpha(label.confidence)
        ) {
            Surface(
                modifier = Modifier
                    .padding(MIN_LABEL_SPACING.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.8f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (label.isDuplicate) Color.Yellow else Color.White
                )
            ) {
                Row(
                    modifier = Modifier.padding(LABEL_PADDING.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (label.isDuplicate) {
                        // Show duplicate index
                        Text(
                            text = "${label.duplicateIndex}",
                            color = Color.Yellow,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    
                    Text(
                        text = label.command,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        maxLines = 2
                    )
                }
            }
        }
    }
    
    /**
     * Clear all command labels
     */
    fun clearLabels() {
        commandLabels.clear()
        if (isVisible()) {
            hide()
        }
    }
    
    /**
     * Highlight a specific command
     */
    fun highlightCommand(command: String) {
        overlayScope.launch {
            val index = commandLabels.indexOfFirst { it.command == command }
            if (index != -1) {
                // Temporarily increase confidence for highlighting
                val highlighted = commandLabels[index].copy(confidence = 1.0f)
                commandLabels[index] = highlighted
                
                // Reset after delay
                delay(1000)
                // Reset to original confidence (can't recalculate without original node)
                commandLabels[index] = highlighted.copy(confidence = 0.8f)
            }
        }
    }
    
    override fun dispose() {
        overlayScope.cancel()
        clearLabels()
        super.dispose()
    }
}

