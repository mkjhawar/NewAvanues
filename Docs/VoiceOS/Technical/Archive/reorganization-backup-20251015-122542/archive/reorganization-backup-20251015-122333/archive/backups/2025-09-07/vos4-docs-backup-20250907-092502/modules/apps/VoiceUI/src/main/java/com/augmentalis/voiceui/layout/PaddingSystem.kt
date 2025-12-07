package com.augmentalis.voiceui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Comprehensive Padding System for VoiceUI
 * Supports all padding approaches for maximum flexibility
 */

/**
 * Padding data class supporting all sides individually
 */
data class MagicPadding(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val left: Dp = 0.dp,
    val right: Dp = 0.dp,
    val start: Dp? = null,  // For RTL support
    val end: Dp? = null     // For RTL support
) {
    companion object {
        // Preset padding values
        val None = MagicPadding(0.dp, 0.dp, 0.dp, 0.dp)
        val Small = MagicPadding(8.dp, 8.dp, 8.dp, 8.dp)
        val Medium = MagicPadding(16.dp, 16.dp, 16.dp, 16.dp)
        val Large = MagicPadding(24.dp, 24.dp, 24.dp, 24.dp)
        val Huge = MagicPadding(32.dp, 32.dp, 32.dp, 32.dp)
        val Comfortable = MagicPadding(20.dp, 20.dp, 24.dp, 24.dp)
        val Compact = MagicPadding(4.dp, 4.dp, 8.dp, 8.dp)
        
        // Common patterns
        val CardDefault = MagicPadding(16.dp, 16.dp, 20.dp, 20.dp)
        val ButtonDefault = MagicPadding(12.dp, 12.dp, 24.dp, 24.dp)
        val InputDefault = MagicPadding(8.dp, 8.dp, 12.dp, 12.dp)
        val TextDefault = MagicPadding(4.dp, 4.dp, 0.dp, 0.dp)
        
        /**
         * Parse CSS-style padding string
         * Supports:
         * - "16" -> all sides
         * - "16 24" -> vertical horizontal
         * - "16 24 32" -> top horizontal bottom
         * - "16 24 32 40" -> top right bottom left
         */
        fun parse(value: String): MagicPadding {
            val parts = value.trim().split(Regex("\\s+")).mapNotNull { 
                it.toIntOrNull()?.dp 
            }
            
            return when (parts.size) {
                1 -> MagicPadding(parts[0], parts[0], parts[0], parts[0])
                2 -> MagicPadding(parts[0], parts[0], parts[1], parts[1])
                3 -> MagicPadding(parts[0], parts[2], parts[1], parts[1])
                4 -> MagicPadding(parts[0], parts[2], parts[3], parts[1])
                else -> Medium  // Default fallback
            }
        }
        
        /**
         * Get preset by name
         */
        fun fromPreset(preset: String): MagicPadding {
            return when (preset.lowercase()) {
                "none", "0" -> None
                "small", "s", "sm" -> Small
                "medium", "m", "md" -> Medium
                "large", "l", "lg" -> Large
                "huge", "xl", "h" -> Huge
                "comfortable", "comfort" -> Comfortable
                "compact", "tight" -> Compact
                "card" -> CardDefault
                "button", "btn" -> ButtonDefault
                "input", "field" -> InputDefault
                "text", "label" -> TextDefault
                else -> parse(preset)  // Try to parse as CSS string
            }
        }
    }
    
    /**
     * Convert to Compose Modifier
     */
    fun toModifier(): Modifier {
        return if (start != null || end != null) {
            // Use start/end for RTL support
            Modifier.padding(
                start = start ?: left,
                end = end ?: right,
                top = top,
                bottom = bottom
            )
        } else {
            // Use left/right
            Modifier.padding(
                start = left,
                end = right,
                top = top,
                bottom = bottom
            )
        }
    }
    
    /**
     * Convert to PaddingValues
     */
    fun toPaddingValues(): PaddingValues {
        return PaddingValues(
            start = start ?: left,
            end = end ?: right,
            top = top,
            bottom = bottom
        )
    }
    
    /**
     * Combine with another padding (useful for nesting)
     */
    operator fun plus(other: MagicPadding): MagicPadding {
        return MagicPadding(
            top = this.top + other.top,
            bottom = this.bottom + other.bottom,
            left = this.left + other.left,
            right = this.right + other.right,
            start = this.start?.plus(other.start ?: 0.dp),
            end = this.end?.plus(other.end ?: 0.dp)
        )
    }
}

/**
 * Extension functions for easy padding
 */

// For explicit padding with all parameters
fun Modifier.magicPadding(
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
    left: Dp = 0.dp,
    right: Dp = 0.dp,
    start: Dp? = null,
    end: Dp? = null,
    // Shortcuts
    all: Dp? = null,
    vertical: Dp? = null,
    horizontal: Dp? = null,
    // Aliases
    t: Dp? = null,
    b: Dp? = null,
    l: Dp? = null,
    r: Dp? = null,
    s: Dp? = null,
    e: Dp? = null,
    v: Dp? = null,
    h: Dp? = null
): Modifier {
    val padding = when {
        all != null -> MagicPadding(all, all, all, all)
        vertical != null || horizontal != null -> MagicPadding(
            top = vertical ?: v ?: top,
            bottom = vertical ?: v ?: bottom,
            left = horizontal ?: h ?: left,
            right = horizontal ?: h ?: right
        )
        else -> MagicPadding(
            top = t ?: top,
            bottom = b ?: bottom,
            left = l ?: left,
            right = r ?: right,
            start = s ?: start,
            end = e ?: end
        )
    }
    return this.then(padding.toModifier())
}

// For CSS-style string padding
fun Modifier.magicPadding(value: String): Modifier {
    return this.then(MagicPadding.parse(value).toModifier())
}

// For preset padding
fun Modifier.magicPadding(preset: PaddingPreset): Modifier {
    return this.then(
        when (preset) {
            PaddingPreset.NONE -> MagicPadding.None
            PaddingPreset.SMALL -> MagicPadding.Small
            PaddingPreset.MEDIUM -> MagicPadding.Medium
            PaddingPreset.LARGE -> MagicPadding.Large
            PaddingPreset.HUGE -> MagicPadding.Huge
            PaddingPreset.COMFORTABLE -> MagicPadding.Comfortable
            PaddingPreset.COMPACT -> MagicPadding.Compact
            PaddingPreset.CARD -> MagicPadding.CardDefault
            PaddingPreset.BUTTON -> MagicPadding.ButtonDefault
            PaddingPreset.INPUT -> MagicPadding.InputDefault
            PaddingPreset.TEXT -> MagicPadding.TextDefault
        }.toModifier()
    )
}

// For single value (all sides)
fun Modifier.magicPadding(all: Dp): Modifier {
    return this.padding(all)
}

// For single value (all sides) as Int
fun Modifier.magicPadding(all: Int): Modifier {
    return this.padding(all.dp)
}

/**
 * Padding presets enum
 */
enum class PaddingPreset {
    NONE,
    SMALL,
    MEDIUM,
    LARGE,
    HUGE,
    COMFORTABLE,
    COMPACT,
    CARD,
    BUTTON,
    INPUT,
    TEXT
}

/**
 * Builder pattern for padding
 */
class PaddingBuilder {
    var top: Dp = 0.dp
    var bottom: Dp = 0.dp
    var left: Dp = 0.dp
    var right: Dp = 0.dp
    var start: Dp? = null
    var end: Dp? = null
    
    // Shortcuts
    var all: Dp? = null
        set(value) {
            field = value
            value?.let {
                top = it
                bottom = it
                left = it
                right = it
            }
        }
    
    var vertical: Dp? = null
        set(value) {
            field = value
            value?.let {
                top = it
                bottom = it
            }
        }
    
    var horizontal: Dp? = null
        set(value) {
            field = value
            value?.let {
                left = it
                right = it
            }
        }
    
    fun build(): MagicPadding {
        return MagicPadding(top, bottom, left, right, start, end)
    }
}

// Extension function using builder
inline fun Modifier.magicPadding(builder: PaddingBuilder.() -> Unit): Modifier {
    val padding = PaddingBuilder().apply(builder).build()
    return this.then(padding.toModifier())
}

/**
 * Responsive padding based on screen size
 */
data class ResponsivePadding(
    val small: MagicPadding = MagicPadding.Small,
    val medium: MagicPadding = MagicPadding.Medium,
    val large: MagicPadding = MagicPadding.Large
) {
    companion object {
        val Default = ResponsivePadding()
        val Comfortable = ResponsivePadding(
            small = MagicPadding.Medium,
            medium = MagicPadding.Large,
            large = MagicPadding.Huge
        )
        val Compact = ResponsivePadding(
            small = MagicPadding.Compact,
            medium = MagicPadding.Small,
            large = MagicPadding.Medium
        )
    }
}

/**
 * Smart padding that adapts to component type
 */
object SmartPadding {
    fun forComponent(componentType: ComponentType): MagicPadding {
        return when (componentType) {
            ComponentType.CARD -> MagicPadding.CardDefault
            ComponentType.BUTTON -> MagicPadding.ButtonDefault
            ComponentType.INPUT -> MagicPadding.InputDefault
            ComponentType.TEXT -> MagicPadding.TextDefault
            ComponentType.EMAIL -> MagicPadding.InputDefault
            ComponentType.PASSWORD -> MagicPadding.InputDefault
            ComponentType.DROPDOWN -> MagicPadding.InputDefault
            ComponentType.TOGGLE -> MagicPadding(8.dp, 8.dp, 0.dp, 0.dp)
            ComponentType.SLIDER -> MagicPadding(12.dp, 12.dp, 0.dp, 0.dp)
            ComponentType.IMAGE -> MagicPadding.None
            ComponentType.LIST -> MagicPadding(0.dp, 0.dp, 0.dp, 0.dp)
            ComponentType.GRID -> MagicPadding(0.dp, 0.dp, 0.dp, 0.dp)
            else -> MagicPadding.Medium
        }
    }
}

/**
 * Component types for smart padding
 */
enum class ComponentType {
    CARD,
    BUTTON,
    INPUT,
    TEXT,
    EMAIL,
    PASSWORD,
    PHONE,
    NAME,
    ADDRESS,
    DATE,
    DROPDOWN,
    TOGGLE,
    SLIDER,
    IMAGE,
    LIST,
    GRID
}