package com.augmentalis.webavanue

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.*
import com.augmentalis.webavanue.ArcLayout
import com.augmentalis.webavanue.ArcOrientation
import com.augmentalis.webavanue.isLandscape
import com.augmentalis.ava.core.theme.OceanTheme

/**
 * ARLayoutPreview - Interactive demo screen for testing spatial arc layout
 *
 * Features:
 * - Sample data demonstration
 * - Orientation toggle (portrait/landscape simulation)
 * - Glass level preview (LIGHT/MEDIUM/HEAVY)
 * - Gesture visualization
 * - Real-time state display
 * - Configuration controls
 *
 * Purpose:
 * Validate AR/XR spatial design before integration with BrowserScreen.
 * Test arc positioning, scaling, gestures, and glassmorphic styling.
 *
 * Usage:
 * Navigate to this screen from developer menu or settings.
 * Swipe to rotate arc, tap items, adjust settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARLayoutPreview(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Demo state
    var currentIndex by remember { mutableStateOf(0) }
    var selectedGlassLevel by remember { mutableStateOf(GlassLevel.MEDIUM) }
    var forceOrientation by remember { mutableStateOf<ArcOrientation?>(null) }
    var showGestureHints by remember { mutableStateOf(true) }
    var showDebugInfo by remember { mutableStateOf(false) }

    // Sample data
    val demoItems = remember {
        listOf(
            DemoItem(1, "Spatial Design", "Arc positioning test", Color(0xFF4A90E2)),
            DemoItem(2, "Glassmorphism", "Heavy blur effect", Color(0xFF7B68EE)),
            DemoItem(3, "Depth Perception", "Progressive scaling", Color(0xFF50C878)),
            DemoItem(4, "Gesture Control", "Swipe and tap", Color(0xFFFF6B6B)),
            DemoItem(5, "Ocean Theme", "Unified styling", Color(0xFFFFD93D)),
            DemoItem(6, "Smooth Animation", "Spring physics", Color(0xFFFF8C42)),
            DemoItem(7, "Orientation Aware", "Portrait/Landscape", Color(0xFF6C5CE7)),
            DemoItem(8, "Focus Indicator", "Center emphasis", Color(0xFFE056FD)),
        )
    }

    // Determine actual orientation
    val deviceLandscape = isLandscape()
    val effectiveOrientation = forceOrientation ?:
        if (deviceLandscape) ArcOrientation.VERTICAL else ArcOrientation.HORIZONTAL

    Box(modifier = modifier.fillMaxSize()) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OceanTheme.background.copy(alpha = 0.95f))
        )

        // Main content
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                glassLevel = GlassLevel.MEDIUM,
                border = GlassDefaults.borderSubtle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        onClick = onBack,
                        glass = true,
                        glassLevel = GlassLevel.LIGHT
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = OceanTheme.textPrimary
                        )
                    }

                    Text(
                        text = "AR Layout Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = OceanTheme.textPrimary
                    )

                    GlassIconButton(
                        onClick = { showDebugInfo = !showDebugInfo },
                        glass = true,
                        glassLevel = GlassLevel.LIGHT
                    ) {
                        Icon(
                            imageVector = if (showDebugInfo) Icons.Default.BugReport else Icons.Default.Info,
                            contentDescription = "Toggle debug",
                            tint = if (showDebugInfo) OceanTheme.primary else OceanTheme.textSecondary
                        )
                    }
                }
            }

            // Arc layout demo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ArcLayout(
                    items = demoItems,
                    currentIndex = currentIndex,
                    onIndexChange = { currentIndex = it },
                    onItemClick = { item ->
                        if (demoItems.indexOf(item) == currentIndex) {
                            // Center item clicked - show toast or action
                        } else {
                            currentIndex = demoItems.indexOf(item)
                        }
                    },
                    onItemLongPress = { item ->
                        if (demoItems.indexOf(item) == currentIndex) {
                            // Long press action
                        }
                    },
                    orientation = effectiveOrientation,
                    arcRadius = if (effectiveOrientation == ArcOrientation.VERTICAL) 500.dp else 400.dp,
                    itemSpacing = if (effectiveOrientation == ArcOrientation.VERTICAL) 35f else 45f,
                    centerScale = 1.0f,
                    sideScale = 0.6f,
                    modifier = Modifier.fillMaxSize()
                ) { item, index, isCenterItem ->
                    Box(
                        modifier = Modifier.pointerInput(item) {
                            detectTapGestures(
                                onTap = {
                                    if (isCenterItem) {
                                        // Center tap
                                    } else {
                                        currentIndex = index
                                    }
                                },
                                onLongPress = {
                                    if (isCenterItem) {
                                        // Center long press
                                    }
                                }
                            )
                        }
                    ) {
                        DemoItemCard(
                            item = item,
                            isCenterItem = isCenterItem,
                            glassLevel = selectedGlassLevel
                        )
                    }
                }

                // Gesture hints
                if (showGestureHints) {
                    Text(
                        text = if (effectiveOrientation == ArcOrientation.VERTICAL)
                            "↑ Swipe ↓" else "← Swipe to rotate arc →",
                        style = MaterialTheme.typography.bodySmall,
                        color = OceanTheme.textTertiary.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(
                                if (effectiveOrientation == ArcOrientation.VERTICAL)
                                    Alignment.CenterEnd
                                else
                                    Alignment.BottomCenter
                            )
                            .padding(
                                end = if (effectiveOrientation == ArcOrientation.VERTICAL) 24.dp else 0.dp,
                                bottom = if (effectiveOrientation == ArcOrientation.VERTICAL) 0.dp else 100.dp
                            )
                    )
                }

                // Item counter
                Text(
                    text = "${currentIndex + 1} / ${demoItems.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanTheme.textPrimary.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .background(
                            color = OceanTheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Debug info overlay
                if (showDebugInfo) {
                    DebugInfoOverlay(
                        currentIndex = currentIndex,
                        totalItems = demoItems.size,
                        orientation = effectiveOrientation,
                        glassLevel = selectedGlassLevel,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }

            // Control panel
            ControlPanel(
                selectedGlassLevel = selectedGlassLevel,
                onGlassLevelChange = { selectedGlassLevel = it },
                forceOrientation = forceOrientation,
                onOrientationChange = { forceOrientation = it },
                showGestureHints = showGestureHints,
                onToggleGestureHints = { showGestureHints = !showGestureHints },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Demo item card with glassmorphic styling
 */
@Composable
private fun DemoItemCard(
    item: DemoItem,
    isCenterItem: Boolean,
    glassLevel: GlassLevel,
    modifier: Modifier = Modifier
) {
    val cardSize = if (isCenterItem) {
        modifier.size(width = 200.dp, height = 150.dp)
    } else {
        modifier.size(width = 120.dp, height = 90.dp)
    }

    val elevation = if (isCenterItem) 8.dp else 4.dp

    GlassCard(
        modifier = cardSize,
        glassLevel = glassLevel,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(12.dp),
        border = GlassDefaults.border
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                item.color.copy(alpha = 0.3f),
                                item.color.copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon representation
                Box(
                    modifier = Modifier
                        .size(if (isCenterItem) 48.dp else 32.dp)
                        .background(
                            color = item.color.copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = item.title,
                    style = if (isCenterItem)
                        MaterialTheme.typography.titleSmall
                    else
                        MaterialTheme.typography.bodySmall,
                    color = OceanTheme.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                if (isCenterItem) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = OceanTheme.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            // Focus indicator
            if (isCenterItem) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(8.dp)
                        .background(
                            color = OceanTheme.primary,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Control panel for demo configuration
 */
@Composable
private fun ControlPanel(
    selectedGlassLevel: GlassLevel,
    onGlassLevelChange: (GlassLevel) -> Unit,
    forceOrientation: ArcOrientation?,
    onOrientationChange: (ArcOrientation?) -> Unit,
    showGestureHints: Boolean,
    onToggleGestureHints: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        glassLevel = GlassLevel.MEDIUM,
        border = GlassDefaults.borderSubtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Glass level selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Glass Level:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanTheme.textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassLevel.values().forEach { level ->
                        GlassChip(
                            onClick = { onGlassLevelChange(level) },
                            label = { Text(level.name) },
                            glass = true,
                            glassLevel = if (selectedGlassLevel == level) GlassLevel.HEAVY else GlassLevel.LIGHT
                        )
                    }
                }
            }

            // Orientation selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Orientation:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanTheme.textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("Auto", null, Icons.Default.PhoneAndroid),
                        Triple("H", ArcOrientation.HORIZONTAL, Icons.Default.ViewCarousel),
                        Triple("V", ArcOrientation.VERTICAL, Icons.Default.ViewColumn)
                    ).forEach { (label, orientation, icon) ->
                        GlassIconButton(
                            onClick = { onOrientationChange(orientation) },
                            glass = true,
                            glassLevel = if (forceOrientation == orientation) GlassLevel.HEAVY else GlassLevel.LIGHT,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (forceOrientation == orientation) OceanTheme.primary else OceanTheme.textSecondary
                            )
                        }
                    }
                }
            }

            // Toggle switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gesture Hints:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OceanTheme.textPrimary
                )

                Switch(
                    checked = showGestureHints,
                    onCheckedChange = { onToggleGestureHints() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OceanTheme.primary,
                        checkedTrackColor = OceanTheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

/**
 * Debug info overlay
 */
@Composable
private fun DebugInfoOverlay(
    currentIndex: Int,
    totalItems: Int,
    orientation: ArcOrientation,
    glassLevel: GlassLevel,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.width(200.dp),
        glassLevel = GlassLevel.HEAVY,
        border = GlassDefaults.border
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Debug Info",
                style = MaterialTheme.typography.titleSmall,
                color = OceanTheme.textPrimary
            )

            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = OceanTheme.textTertiary.copy(alpha = 0.3f)
            )

            DebugRow("Current Index", "$currentIndex")
            DebugRow("Total Items", "$totalItems")
            DebugRow("Orientation", orientation.name)
            DebugRow("Glass Level", glassLevel.name)
            DebugRow("Arc Radius", if (orientation == ArcOrientation.VERTICAL) "500dp" else "400dp")
            DebugRow("Item Spacing", if (orientation == ArcOrientation.VERTICAL) "35°" else "45°")
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = OceanTheme.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = OceanTheme.textPrimary
        )
    }
}

/**
 * Demo item data class
 */
private data class DemoItem(
    val id: Int,
    val title: String,
    val description: String,
    val color: Color
)
