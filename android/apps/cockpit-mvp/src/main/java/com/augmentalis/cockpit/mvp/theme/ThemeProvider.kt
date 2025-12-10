package com.augmentalis.cockpit.mvp.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.augmentalis.cockpit.mvp.OceanTheme

/**
 * Cockpit MVP Theme System
 *
 * Runtime theme switching with MagicUI migration readiness.
 * Supports multiple UI systems through a unified provider pattern.
 *
 * MIGRATION PATH:
 * 1. Current: OceanTheme (glassmorphic tokens)
 * 2. Phase 1: Add AvaMagicTheme when AVA branding ships
 * 3. Phase 2: Add MagicCodeTheme for developer UI
 * 4. Phase 3: Replace implementations with MagicUI components
 *
 * Design: Composition locals allow any component to access current theme
 * without prop drilling, while MaterialTheme provides base Android styling.
 */

/**
 * Available theme options for Cockpit MVP
 */
enum class AppTheme {
    /**
     * Default glassmorphic theme with ocean-inspired colors
     * Uses OceanTheme tokens for consistent styling
     */
    OCEAN,

    /**
     * AVA branding theme (future)
     * TODO: Implement AvaMagicTheme when branding specs ship
     */
    AVAMAGIC,

    /**
     * Developer-focused UI theme (future)
     * TODO: Implement MagicCodeTheme for code/debug views
     */
    MAGICCODE,

    /**
     * Platform default Material3 theme
     * Fallback for system-native appearance
     */
    NATIVE
}

/**
 * Composition local for accessing current theme tokens
 *
 * Provides theme object to all descendants without prop drilling.
 * Returns null when NATIVE theme is active (uses MaterialTheme only).
 *
 * Usage:
 * ```
 * val theme = LocalTheme.current
 * if (theme != null) {
 *     // Custom theme active - use theme tokens
 *     Box(backgroundColor = theme.glassSurface)
 * } else {
 *     // NATIVE theme - use MaterialTheme
 *     Box(backgroundColor = MaterialTheme.colorScheme.surface)
 * }
 * ```
 */
val LocalTheme = staticCompositionLocalOf<Any?> { null }

/**
 * Primary theme provider for Cockpit MVP
 *
 * Wraps app content with selected theme, making tokens available
 * throughout the composition tree via LocalTheme.
 *
 * Architecture:
 * - Maps AppTheme enum to concrete theme objects
 * - Provides theme via CompositionLocalProvider
 * - Always wraps with MaterialTheme for base Android components
 * - Null theme = NATIVE mode (MaterialTheme only)
 *
 * Migration Safety:
 * - When adding new themes: Update when() expression
 * - When migrating to MagicUI: Replace theme objects with MagicUI equivalents
 * - LocalTheme pattern remains unchanged
 *
 * @param theme Selected theme (default: OCEAN glassmorphic)
 * @param content Composable content to wrap with theme
 *
 * Example:
 * ```
 * CockpitThemeProvider(theme = AppTheme.OCEAN) {
 *     // All composables here can access theme via LocalTheme.current
 *     WorkspaceScreen()
 * }
 * ```
 */
@Composable
fun CockpitThemeProvider(
    theme: AppTheme = AppTheme.OCEAN,
    content: @Composable () -> Unit
) {
    // Map theme enum to concrete theme object
    // Future themes return OceanTheme as fallback until implemented
    val themeColors = when (theme) {
        AppTheme.OCEAN -> OceanTheme

        // TODO: Replace with AvaMagicTheme when ready
        // Migration: import AvaMagicTheme, update mapping
        AppTheme.AVAMAGIC -> OceanTheme

        // TODO: Replace with MagicCodeTheme when ready
        // Migration: import MagicCodeTheme, update mapping
        AppTheme.MAGICCODE -> OceanTheme

        // NATIVE mode: null signals MaterialTheme-only styling
        AppTheme.NATIVE -> null
    }

    // Provide theme to composition tree
    CompositionLocalProvider(LocalTheme provides themeColors) {
        // Always wrap with MaterialTheme for base Android components
        // (buttons, text fields, etc. that need platform defaults)
        MaterialTheme {
            content()
        }
    }
}

/**
 * Extension function to access current theme in composables
 *
 * Convenience accessor that reads from LocalTheme composition local.
 * Returns null when NATIVE theme is active.
 *
 * Usage:
 * ```
 * @Composable
 * fun MyComponent() {
 *     val theme = currentTheme()
 *     when (theme) {
 *         is OceanTheme -> // Use Ocean tokens
 *         null -> // Use MaterialTheme
 *     }
 * }
 * ```
 *
 * Type Safety Note:
 * Returns Any? to support multiple theme types. Components should
 * use smart casting or is-checks to access specific theme tokens.
 *
 * @return Current theme object or null for NATIVE theme
 */
@Composable
fun currentTheme(): Any? = LocalTheme.current

/**
 * FUTURE MIGRATION CHECKLIST
 *
 * When adding AvaMagicTheme:
 * [ ] Create AvaMagicTheme object with AVA brand tokens
 * [ ] Import in this file
 * [ ] Update AppTheme.AVAMAGIC case to return AvaMagicTheme
 * [ ] Add usage examples in KDoc
 *
 * When adding MagicCodeTheme:
 * [ ] Create MagicCodeTheme object with developer UI tokens
 * [ ] Import in this file
 * [ ] Update AppTheme.MAGICCODE case to return MagicCodeTheme
 * [ ] Add usage examples in KDoc
 *
 * When migrating to MagicUI:
 * [ ] Replace OceanTheme with MagicUI.OceanTheme
 * [ ] Replace AvaMagicTheme with MagicUI.AvaMagicTheme
 * [ ] Replace MagicCodeTheme with MagicUI.MagicCodeTheme
 * [ ] Update imports to MagicUI package
 * [ ] Test theme switching with MagicUI components
 * [ ] Update KDoc with MagicUI-specific examples
 * [ ] No changes needed to LocalTheme or provider pattern
 */
