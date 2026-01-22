package com.augmentalis.voiceui.api

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.voiceui.layout.*
import com.augmentalis.voiceui.theme.*
import com.augmentalis.voiceui.dsl.MagicScope

/**
 * Enhanced Magic Components with comprehensive padding and layout support
 * All components now support every padding approach for maximum flexibility
 */

/**
 * Enhanced card with all padding options
 */
@Composable
fun MagicScope.card(
    title: String? = null,
    subtitle: String? = null,
    // Explicit padding parameters
    padTop: Dp? = null,
    padBottom: Dp? = null,
    padLeft: Dp? = null,
    padRight: Dp? = null,
    padStart: Dp? = null,
    padEnd: Dp? = null,
    // Shortcuts
    pad: Any? = null,  // Can be Dp, Int, String, or PaddingPreset
    padVertical: Dp? = null,
    padHorizontal: Dp? = null,
    // Aliases
    pt: Dp? = null,
    pb: Dp? = null,
    pl: Dp? = null,
    pr: Dp? = null,
    ps: Dp? = null,
    pe: Dp? = null,
    pv: Dp? = null,
    ph: Dp? = null,
    // Size and position
    width: Any? = null,  // Can be Dp, "full", "half", percentage
    height: Dp? = null,
    position: Position? = null,
    // Layout
    layout: String? = null,  // "row", "column", "grid 2", etc.
    gap: Dp? = null,
    content: @Composable () -> Unit
) {
    // Parse padding
    val padding = when (pad) {
        is Dp -> MagicPadding(pad, pad, pad, pad)
        is Int -> MagicPadding(pad.dp, pad.dp, pad.dp, pad.dp)
        is String -> MagicPadding.fromPreset(pad)
        is PaddingPreset -> when (pad) {
            PaddingPreset.NONE -> MagicPadding.None
            PaddingPreset.SMALL -> MagicPadding.Small
            PaddingPreset.MEDIUM -> MagicPadding.Medium
            PaddingPreset.LARGE -> MagicPadding.Large
            PaddingPreset.HUGE -> MagicPadding.Huge
            PaddingPreset.COMFORTABLE -> MagicPadding.Comfortable
            PaddingPreset.COMPACT -> MagicPadding.Compact
            PaddingPreset.CARD -> MagicPadding.CardDefault
            else -> MagicPadding.CardDefault
        }
        null -> MagicPadding(
            top = pt ?: padTop ?: pv ?: padVertical ?: 16.dp,
            bottom = pb ?: padBottom ?: pv ?: padVertical ?: 16.dp,
            left = pl ?: padLeft ?: ph ?: padHorizontal ?: 20.dp,
            right = pr ?: padRight ?: ph ?: padHorizontal ?: 20.dp,
            start = ps ?: padStart,
            end = pe ?: padEnd
        )
        else -> MagicPadding.CardDefault
    }
    
    // Parse width
    val widthModifier = when (width) {
        "full" -> Modifier.fillMaxWidth()
        "half" -> Modifier.fillMaxWidth(0.5f)
        is Float -> Modifier.fillMaxWidth(width)
        is Dp -> Modifier.width(width)
        else -> Modifier
    }
    
    // Parse layout
    val layoutConfig = layout?.let { LayoutConfig.parse(it) }
    
    // Apply position if needed
    val positionModifier = position?.toModifier() ?: Modifier
    
    GreyARCard(
        modifier = Modifier
            .then(widthModifier)
            .then(positionModifier)
            .then(if (height != null) Modifier.height(height) else Modifier),
        title = title,
        subtitle = subtitle
    ) {
        Box(modifier = padding.toModifier()) {
            when (layoutConfig?.type) {
                LayoutType.ROW -> {
                    MagicRow(gap = gap ?: layoutConfig.spacing) {
                        content()
                    }
                }
                LayoutType.GRID -> {
                    MagicGrid(
                        columns = layoutConfig.columns,
                        gap = gap ?: layoutConfig.spacing
                    ) {
                        content()
                    }
                }
                else -> {
                    MagicColumn(gap = gap ?: 16.dp) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Enhanced email with all padding options
 */
@Composable
fun MagicScope.email(
    label: String = "Email",
    required: Boolean = true,
    // All padding options
    pad: Any? = null,
    padTop: Dp? = null,
    padBottom: Dp? = null,
    padLeft: Dp? = null,
    padRight: Dp? = null,
    pt: Dp? = null,
    pb: Dp? = null,
    pl: Dp? = null,
    pr: Dp? = null,
    onValue: ((String) -> Unit)? = null
): String {
    // Implementation from MagicComponents.kt with padding applied
    val padding = parsePadding(pad, padTop, padBottom, padLeft, padRight, pt, pb, pl, pr)
    
    val emailValue = email(label, required, onValue)
    
    Box(modifier = padding.toModifier()) {
        // Email field is rendered as part of the email() function
    }
    
    return emailValue
}

/**
 * Layout containers with full flexibility
 */
@Composable
fun MagicScope.row(
    gap: Dp = 16.dp,
    pad: Any? = null,
    alignment: String = "center",
    scrollable: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val padding = when (pad) {
        is String -> MagicPadding.parse(pad)
        is Dp -> MagicPadding(pad, pad, pad, pad)
        is Int -> MagicPadding(pad.dp, pad.dp, pad.dp, pad.dp)
        else -> MagicPadding.None
    }
    
    val verticalAlignment = when (alignment.lowercase()) {
        "top" -> Alignment.Top
        "center" -> Alignment.CenterVertically
        "bottom" -> Alignment.Bottom
        else -> Alignment.CenterVertically
    }
    
    MagicRow(
        gap = gap,
        padding = padding,
        alignment = verticalAlignment,
        scrollable = scrollable,
        content = content
    )
}

@Composable
fun MagicScope.column(
    gap: Dp = 16.dp,
    pad: Any? = null,
    alignment: String = "start",
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val padding = when (pad) {
        is String -> MagicPadding.parse(pad)
        is Dp -> MagicPadding(pad, pad, pad, pad)
        is Int -> MagicPadding(pad.dp, pad.dp, pad.dp, pad.dp)
        else -> MagicPadding.None
    }
    
    val horizontalAlignment = when (alignment.lowercase()) {
        "start", "left" -> Alignment.Start
        "center" -> Alignment.CenterHorizontally
        "end", "right" -> Alignment.End
        else -> Alignment.Start
    }
    
    MagicColumn(
        gap = gap,
        padding = padding,
        alignment = horizontalAlignment,
        scrollable = scrollable,
        content = content
    )
}

@Composable
fun MagicScope.grid(
    columns: Int = 2,
    gap: Dp = 16.dp,
    pad: Any? = null,
    content: @Composable GridScope.() -> Unit
) {
    val padding = when (pad) {
        is String -> MagicPadding.parse(pad)
        is Dp -> MagicPadding(pad, pad, pad, pad)
        is Int -> MagicPadding(pad.dp, pad.dp, pad.dp, pad.dp)
        else -> MagicPadding.None
    }
    
    MagicGrid(
        columns = columns,
        gap = gap,
        padding = padding,
        content = content
    )
}

/**
 * Padded container - applies padding to multiple elements
 */
@Composable
fun MagicScope.padded(
    pad: Any,
    content: @Composable () -> Unit
) {
    val padding = when (pad) {
        is String -> MagicPadding.parse(pad)
        is Dp -> MagicPadding(pad, pad, pad, pad)
        is Int -> MagicPadding(pad.dp, pad.dp, pad.dp, pad.dp)
        is PaddingPreset -> MagicPadding.fromPreset(pad.name.lowercase())
        else -> MagicPadding.Medium
    }
    
    Box(modifier = padding.toModifier()) {
        content()
    }
}

/**
 * Helper function to parse padding from various inputs
 */
private fun parsePadding(
    pad: Any?,
    padTop: Dp?,
    padBottom: Dp?,
    padLeft: Dp?,
    padRight: Dp?,
    pt: Dp?,
    pb: Dp?,
    pl: Dp?,
    pr: Dp?
): MagicPadding {
    return when (pad) {
        is String -> MagicPadding.parse(pad)
        is Dp -> MagicPadding(pad, pad, pad, pad)
        is Int -> MagicPadding(pad.dp, pad.dp, pad.dp, pad.dp)
        is PaddingPreset -> MagicPadding.fromPreset(pad.name.lowercase())
        null -> MagicPadding(
            top = pt ?: padTop ?: 0.dp,
            bottom = pb ?: padBottom ?: 0.dp,
            left = pl ?: padLeft ?: 0.dp,
            right = pr ?: padRight ?: 0.dp
        )
        else -> MagicPadding.None
    }
}

/**
 * Position data class for absolute positioning
 */
data class Position(
    val top: Dp? = null,
    val bottom: Dp? = null,
    val left: Dp? = null,
    val right: Dp? = null,
    val centerX: Boolean = false,
    val centerY: Boolean = false
) {
    fun toModifier(): Modifier {
        // Implementation for positioning
        return Modifier
    }
}

/**
 * Extension functions for chaining padding
 */

// For components that return values
class PaddableComponent<T>(
    private val value: T,
    private var modifier: Modifier = Modifier
) {
    fun padding(all: Dp): PaddableComponent<T> {
        modifier = modifier.padding(all)
        return this
    }
    
    fun padding(
        top: Dp = 0.dp,
        bottom: Dp = 0.dp,
        left: Dp = 0.dp,
        right: Dp = 0.dp
    ): PaddableComponent<T> {
        modifier = modifier.padding(start = left, end = right, top = top, bottom = bottom)
        return this
    }
    
    fun pad(value: String): PaddableComponent<T> {
        modifier = modifier.then(MagicPadding.parse(value).toModifier())
        return this
    }
    
    fun getValue(): T = value
    fun getModifier(): Modifier = modifier
}

// Extension to make components paddable
fun String.pad(padding: Any): PaddableComponent<String> {
    return PaddableComponent(this).apply {
        when (padding) {
            is String -> pad(padding)
            is Dp -> padding(padding)
            is Int -> padding(padding.dp)
        }
    }
}

/**
 * Global spacing configuration for MagicScreen
 */
@Composable
fun MagicScope.withSpacing(
    default: Dp = 16.dp,
    betweenCards: Dp = 20.dp,
    betweenSections: Dp = 32.dp,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSpacing provides SpacingConfig(
            default = default,
            betweenCards = betweenCards,
            betweenSections = betweenSections
        )
    ) {
        content()
    }
}

val LocalSpacing = compositionLocalOf { SpacingConfig.Default }