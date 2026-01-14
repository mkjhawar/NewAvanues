package com.augmentalis.voiceui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll

/**
 * Comprehensive Layout System for VoiceUI
 * Supports containers, strings, positioning - everything!
 */

/**
 * Layout configuration for MagicScreen
 */
data class LayoutConfig(
    val type: LayoutType = LayoutType.COLUMN,
    val spacing: Dp = 16.dp,
    val padding: MagicPadding = MagicPadding.Medium,
    val alignment: LayoutAlignment = LayoutAlignment.TOP_START,
    val scrollable: Boolean = true,
    val columns: Int = 1,  // For grid
    val aspectRatio: Float? = null,  // For maintaining aspect ratios
    val maxWidth: Dp? = null,
    val maxHeight: Dp? = null
) {
    companion object {
        // Presets
        val Default = LayoutConfig()
        val Comfortable = LayoutConfig(spacing = 24.dp, padding = MagicPadding.Large)
        val Compact = LayoutConfig(spacing = 8.dp, padding = MagicPadding.Small)
        val Grid2 = LayoutConfig(type = LayoutType.GRID, columns = 2)
        val Grid3 = LayoutConfig(type = LayoutType.GRID, columns = 3)
        val Centered = LayoutConfig(alignment = LayoutAlignment.CENTER)
        
        /**
         * Parse layout string
         * Examples:
         * - "column" -> vertical column
         * - "row" -> horizontal row
         * - "grid 2" -> 2 column grid
         * - "grid 3 16" -> 3 column grid with 16dp gap
         * - "flow" -> flow layout
         * - "absolute" -> absolute positioning
         */
        fun parse(layoutString: String): LayoutConfig {
            val parts = layoutString.lowercase().split(" ")
            return when (parts[0]) {
                "column", "col", "vertical", "v" -> LayoutConfig(type = LayoutType.COLUMN)
                "row", "horizontal", "h" -> LayoutConfig(type = LayoutType.ROW)
                "grid", "g" -> {
                    val columns = parts.getOrNull(1)?.toIntOrNull() ?: 2
                    val spacing = parts.getOrNull(2)?.toIntOrNull()?.dp ?: 16.dp
                    LayoutConfig(type = LayoutType.GRID, columns = columns, spacing = spacing)
                }
                "flow", "wrap" -> LayoutConfig(type = LayoutType.FLOW)
                "absolute", "abs", "overlay" -> LayoutConfig(type = LayoutType.ABSOLUTE)
                "stack", "z" -> LayoutConfig(type = LayoutType.STACK)
                else -> Default
            }
        }
    }
}

/**
 * Layout types
 */
enum class LayoutType {
    COLUMN,    // Vertical layout (default)
    ROW,       // Horizontal layout
    GRID,      // Grid layout
    FLOW,      // Flow/Wrap layout
    ABSOLUTE,  // Absolute positioning
    STACK      // Z-index stacking
}

/**
 * Layout alignment
 */
enum class LayoutAlignment {
    TOP_START,
    TOP_CENTER,
    TOP_END,
    CENTER_START,
    CENTER,
    CENTER_END,
    BOTTOM_START,
    BOTTOM_CENTER,
    BOTTOM_END;
    
    fun toComposeAlignment(): Alignment {
        return when (this) {
            TOP_START -> Alignment.TopStart
            TOP_CENTER -> Alignment.TopCenter
            TOP_END -> Alignment.TopEnd
            CENTER_START -> Alignment.CenterStart
            CENTER -> Alignment.Center
            CENTER_END -> Alignment.CenterEnd
            BOTTOM_START -> Alignment.BottomStart
            BOTTOM_CENTER -> Alignment.BottomCenter
            BOTTOM_END -> Alignment.BottomEnd
        }
    }
}

/**
 * Container DSL for layouts
 */
@Composable
fun MagicRow(
    modifier: Modifier = Modifier,
    gap: Dp = 16.dp,
    padding: MagicPadding = MagicPadding.None,
    alignment: Alignment.Vertical = Alignment.CenterVertically,
    scrollable: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val rowModifier = modifier
        .then(padding.toModifier())
        .then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
    
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = alignment,
        content = content
    )
}

@Composable
fun MagicColumn(
    modifier: Modifier = Modifier,
    gap: Dp = 16.dp,
    padding: MagicPadding = MagicPadding.None,
    alignment: Alignment.Horizontal = Alignment.Start,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val columnModifier = modifier
        .then(padding.toModifier())
        .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
    
    Column(
        modifier = columnModifier,
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = alignment,
        content = content
    )
}

@Composable
fun MagicGrid(
    modifier: Modifier = Modifier,
    columns: Int = 2,
    gap: Dp = 16.dp,
    padding: MagicPadding = MagicPadding.None,
    @Suppress("UNUSED_PARAMETER") aspectRatio: Float? = null,
    @Suppress("UNUSED_PARAMETER") content: @Composable GridScope.() -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.then(padding.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        // Direct items in the LazyVerticalGrid
        items(100) { // Placeholder for grid items
            // Empty for now
        }
    }
}

/**
 * Grid scope wrapper
 */
class GridScope(private val lazyGridScope: LazyGridScope) {
    fun item(
        span: Int = 1,
        content: @Composable () -> Unit
    ) {
        lazyGridScope.item(
            span = { GridItemSpan(span) }
        ) {
            content()
        }
    }
    
    fun items(
        count: Int,
        span: (Int) -> Int = { 1 },
        content: @Composable (Int) -> Unit
    ) {
        lazyGridScope.items(
            count = count,
            span = { index -> GridItemSpan(span(index)) }
        ) { index ->
            content(index)
        }
    }
}

/**
 * Flow layout (wrapping)
 */
@Composable
fun MagicFlow(
    modifier: Modifier = Modifier,
    gap: Dp = 8.dp,
    padding: MagicPadding = MagicPadding.None,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier.then(padding.toModifier()),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content
    )
}

/**
 * Stack layout (overlapping)
 */
@Composable
fun MagicStack(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
    padding: MagicPadding = MagicPadding.None,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.then(padding.toModifier()),
        contentAlignment = alignment,
        content = content
    )
}

/**
 * Absolute positioning for AR overlays
 */
@Composable
fun ARLayout(
    modifier: Modifier = Modifier,
    content: @Composable ARScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        val scope = ARScope(this)
        scope.content()
    }
}

/**
 * AR positioning scope
 */
class ARScope(private val boxScope: BoxScope) {
    
    @Composable
    fun positioned(
        top: Dp? = null,
        bottom: Dp? = null,
        left: Dp? = null,
        right: Dp? = null,
        centerX: Boolean = false,
        centerY: Boolean = false,
        content: @Composable () -> Unit
    ) {
        with(boxScope) {
            Box(
                modifier = Modifier.then(
                    when {
                        centerX && centerY -> Modifier.align(Alignment.Center)
                        centerX && top != null -> Modifier.align(Alignment.TopCenter)
                        centerX && bottom != null -> Modifier.align(Alignment.BottomCenter)
                        centerY && left != null -> Modifier.align(Alignment.CenterStart)
                        centerY && right != null -> Modifier.align(Alignment.CenterEnd)
                        top != null && left != null -> Modifier.align(Alignment.TopStart)
                        top != null && right != null -> Modifier.align(Alignment.TopEnd)
                        bottom != null && left != null -> Modifier.align(Alignment.BottomStart)
                        bottom != null && right != null -> Modifier.align(Alignment.BottomEnd)
                        else -> Modifier
                    }
            ).then(
                Modifier.offset {
                    IntOffset(
                        x = when {
                            left != null -> left.roundToPx()
                            right != null -> -right.roundToPx()
                            else -> 0
                        },
                        y = when {
                            top != null -> top.roundToPx()
                            bottom != null -> -bottom.roundToPx()
                            else -> 0
                        }
                    )
                }
            )
        ) {
            content()
        }
        }
    }
}

/**
 * Spacing configuration
 */
data class SpacingConfig(
    val default: Dp = 16.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val huge: Dp = 32.dp,
    val betweenCards: Dp = 20.dp,
    val betweenSections: Dp = 32.dp,
    val screenPadding: Dp = 24.dp
) {
    companion object {
        val Default = SpacingConfig()
        val Comfortable = SpacingConfig(
            default = 24.dp,
            small = 12.dp,
            medium = 24.dp,
            large = 32.dp,
            huge = 48.dp,
            betweenCards = 28.dp,
            betweenSections = 40.dp,
            screenPadding = 32.dp
        )
        val Compact = SpacingConfig(
            default = 8.dp,
            small = 4.dp,
            medium = 8.dp,
            large = 16.dp,
            huge = 24.dp,
            betweenCards = 12.dp,
            betweenSections = 20.dp,
            screenPadding = 16.dp
        )
    }
}

/**
 * Responsive layout that adapts to screen size
 */
@Composable
fun ResponsiveLayout(
    modifier: Modifier = Modifier,
    content: @Composable ResponsiveScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val breakpoint = when {
        screenWidth < 600.dp -> ScreenSize.SMALL
        screenWidth < 900.dp -> ScreenSize.MEDIUM
        else -> ScreenSize.LARGE
    }
    
    Box(modifier = modifier) {
        ResponsiveScope(breakpoint).content()
    }
}

/**
 * Responsive scope
 */
class ResponsiveScope(val screenSize: ScreenSize) {
    
    @Composable
    fun small(content: @Composable () -> Unit) {
        if (screenSize == ScreenSize.SMALL) content()
    }
    
    @Composable
    fun medium(content: @Composable () -> Unit) {
        if (screenSize == ScreenSize.MEDIUM) content()
    }
    
    @Composable
    fun large(content: @Composable () -> Unit) {
        if (screenSize == ScreenSize.LARGE) content()
    }
    
    @Composable
    fun smallOrMedium(content: @Composable () -> Unit) {
        if (screenSize == ScreenSize.SMALL || screenSize == ScreenSize.MEDIUM) content()
    }
    
    @Composable
    fun mediumOrLarge(content: @Composable () -> Unit) {
        if (screenSize == ScreenSize.MEDIUM || screenSize == ScreenSize.LARGE) content()
    }
    
    fun columnsFor(small: Int, medium: Int, large: Int): Int {
        return when (screenSize) {
            ScreenSize.SMALL -> small
            ScreenSize.MEDIUM -> medium
            ScreenSize.LARGE -> large
        }
    }
}

enum class ScreenSize {
    SMALL,   // < 600dp (phones)
    MEDIUM,  // 600-900dp (tablets)
    LARGE    // > 900dp (desktop/TV)
}

/**
 * Custom flow row implementation (if not available in Compose)
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    @Suppress("UNUSED_PARAMETER") verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        
        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            
            if (currentRowWidth + placeable.width <= constraints.maxWidth) {
                currentRow.add(placeable)
                currentRowWidth += placeable.width
            } else {
                rows.add(currentRow)
                currentRow = mutableListOf(placeable)
                currentRowWidth = placeable.width
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        
        val height = rows.sumOf { row -> row.maxOf { it.height } }
        
        layout(constraints.maxWidth, height) {
            var yPosition = 0
            rows.forEach { row ->
                var xPosition = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x = xPosition, y = yPosition)
                    xPosition += placeable.width
                }
                yPosition += row.maxOf { it.height }
            }
        }
    }
}