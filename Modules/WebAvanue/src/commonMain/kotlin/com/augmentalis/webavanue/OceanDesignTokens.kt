package com.augmentalis.webavanue

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.avanues.themes.OceanTheme

/**
 * Ocean Design Tokens
 *
 * Single source of truth for all design values in WebAvanue.
 * This token system enables:
 * 1. Consistent styling across all components
 * 2. Easy theme switching (light/dark)
 * 3. Simple migration to MagicUI (tokens remain, implementations change)
 *
 * Architecture:
 * - Semantic naming (primary, success, error vs blue, green, red)
 * - Categorized by usage (Icon, Text, Surface, etc.)
 * - All components use ONLY these tokens (no direct OceanTheme.* access)
 *
 * MagicUI Migration:
 * When transitioning to MagicUI, this file maps to MagicUI's token system.
 * Component implementations change, but token names remain stable.
 */
object OceanDesignTokens {

    /**
     * Icon color tokens
     * Usage: All icons throughout the app
     */
    object Icon {
        /** Primary blue - always visible on all backgrounds */
        val primary: Color = OceanTheme.primary  // #3B82F6

        /** Secondary gray - less emphasis */
        val secondary: Color = OceanTheme.textSecondary  // #CBD5E1

        /** Disabled gray - inactive controls */
        val disabled: Color = OceanTheme.textDisabled  // #64748B

        /** Success green - positive actions/states */
        val success: Color = OceanTheme.success  // #10B981

        /** Warning amber - caution states */
        val warning: Color = OceanTheme.warning  // #F59E0B

        /** Error red - errors/destructive actions */
        val error: Color = OceanTheme.error  // #EF4444

        /** On primary - icon color when on primary background */
        val onPrimary: Color = OceanTheme.textOnPrimary  // White
    }

    /**
     * Text color tokens
     * Usage: All text content
     */
    object Text {
        /** Primary text - headings, body text */
        val primary: Color = OceanTheme.textPrimary  // #E2E8F0

        /** Secondary text - captions, labels */
        val secondary: Color = OceanTheme.textSecondary  // #CBD5E1

        /** Disabled text - inactive labels */
        val disabled: Color = OceanTheme.textDisabled  // #64748B

        /** On primary - text color when on primary background */
        val onPrimary: Color = OceanTheme.textOnPrimary  // White
    }

    /**
     * Surface color tokens
     * Usage: Backgrounds, cards, surfaces
     */
    object Surface {
        /** Default surface - main backgrounds */
        val default: Color = OceanTheme.surface  // #1E293B

        /** Elevated surface - cards, dialogs */
        val elevated: Color = OceanTheme.surfaceElevated  // #334155

        /** Input surface - text fields, inputs */
        val input: Color = OceanTheme.surfaceInput  // #475569

        /** Primary surface - accent surfaces */
        val primary: Color = OceanTheme.primary  // #3B82F6
    }

    /**
     * Border color tokens
     * Usage: Component borders, dividers
     */
    object Border {
        /** Default border - standard borders */
        val default: Color = Color(0x33FFFFFF)  // 20% white

        /** Subtle border - minimal borders */
        val subtle: Color = Color(0x1AFFFFFF)  // 10% white

        /** Strong border - emphasized borders */
        val strong: Color = Color(0x4DFFFFFF)  // 30% white

        /** Primary border - accent borders */
        val primary: Color = OceanTheme.primary  // #3B82F6
    }

    /**
     * State color tokens
     * Usage: Status indicators, feedback
     */
    object State {
        /** Success - positive states */
        val success: Color = OceanTheme.success  // #10B981

        /** Warning - caution states */
        val warning: Color = OceanTheme.warning  // #F59E0B

        /** Error - error states */
        val error: Color = OceanTheme.error  // #EF4444

        /** Info - informational states */
        val info: Color = OceanTheme.primary  // #3B82F6
    }

    /**
     * Spacing scale
     * Usage: Padding, margins, gaps
     */
    object Spacing {
        /** 4dp - Minimal spacing */
        val xs: Dp = 4.dp

        /** 8dp - Small spacing */
        val sm: Dp = 8.dp

        /** 12dp - Medium spacing */
        val md: Dp = 12.dp

        /** 16dp - Large spacing */
        val lg: Dp = 16.dp

        /** 24dp - Extra large spacing */
        val xl: Dp = 24.dp

        /** 32dp - Extra extra large spacing */
        val xxl: Dp = 32.dp

        /** 48dp - Minimum touch target size */
        val touchTarget: Dp = 48.dp
    }

    /**
     * Elevation scale
     * Usage: Shadow elevation, z-index
     */
    object Elevation {
        /** 0dp - No elevation */
        val none: Dp = 0.dp

        /** 2dp - Subtle elevation */
        val sm: Dp = 2.dp

        /** 4dp - Medium elevation */
        val md: Dp = 4.dp

        /** 8dp - Large elevation */
        val lg: Dp = 8.dp

        /** 12dp - Extra large elevation */
        val xl: Dp = 12.dp

        /** 16dp - Maximum elevation */
        val xxl: Dp = 16.dp
    }

    /**
     * Corner radius scale
     * Usage: Rounded corners
     */
    object CornerRadius {
        /** 4dp - Small radius */
        val sm: Dp = 4.dp

        /** 8dp - Medium radius */
        val md: Dp = 8.dp

        /** 12dp - Large radius */
        val lg: Dp = 12.dp

        /** 16dp - Extra large radius */
        val xl: Dp = 16.dp

        /** 24dp - Extra extra large radius */
        val xxl: Dp = 24.dp

        /** Full circle/pill shape */
        val full: Dp = 9999.dp
    }

    /**
     * Animation duration scale
     * Usage: Transitions, animations
     */
    object Animation {
        /** 100ms - Very fast */
        const val fast: Int = 100

        /** 200ms - Fast */
        const val normal: Int = 200

        /** 300ms - Medium */
        const val medium: Int = 300

        /** 500ms - Slow */
        const val slow: Int = 500
    }

    /**
     * Glass effect levels
     * Usage: Glassmorphic components
     */
    object Glass {
        /** Light glass - 5% opacity, subtle blur */
        const val light: Float = 0.05f

        /** Medium glass - 8% opacity, moderate blur */
        const val medium: Float = 0.08f

        /** Heavy glass - 12% opacity, strong blur */
        const val heavy: Float = 0.12f
    }
}
