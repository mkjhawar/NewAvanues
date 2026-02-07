package com.augmentalis.universal.thememanager

import com.augmentalis.avamagic.components.core.Theme
import com.augmentalis.avamagic.components.core.Themes
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

/**
 * Examples demonstrating how to use the Universal Theme Manager
 *
 * This file contains practical examples for:
 * 1. Setting the universal theme
 * 2. Creating app-specific overrides (full and partial)
 * 3. Resolving effective themes
 * 4. Observing theme changes
 * 5. Using the builder pattern
 * 6. Importing/exporting themes
 */
object ThemeManagerExample {

    /**
     * Example 1: Initialize and set universal theme
     */
    fun example1_SetUniversalTheme() = runBlocking {
        // Initialize ThemeManager
        ThemeManager.initialize(
            repository = LocalThemeRepository(),
            syncManager = null  // Cloud sync disabled for now
        )

        // Load existing themes from storage
        ThemeManager.loadThemes()

        // Set the universal Avanues theme
        ThemeManager.setUniversalTheme(Themes.Material3Light)

        println("Universal theme set to: ${ThemeManager.getUniversalTheme().name}")
    }

    /**
     * Example 2: Create a full app override
     * VoiceOS will use iOS 26 Liquid Glass theme instead of universal theme
     */
    fun example2_FullAppOverride() = runBlocking {
        val appId = "com.augmentalis.voiceos"

        // Set a completely different theme for VoiceOS
        ThemeManager.setAppTheme(appId, Themes.iOS26LiquidGlass)

        // Get the effective theme for VoiceOS
        val voiceOSTheme = ThemeManager.getTheme(appId)
        println("VoiceOS theme: ${voiceOSTheme.name}")
        println("VoiceOS platform: ${voiceOSTheme.platform}")

        // Check if override exists
        val hasOverride = ThemeManager.hasOverride(appId)
        println("VoiceOS has override: $hasOverride")
    }

    /**
     * Example 3: Create a partial app override
     * NoteAvanue inherits typography and spacing but uses custom colors
     */
    fun example3_PartialAppOverride() = runBlocking {
        val appId = "com.augmentalis.noteavanue"

        // Create a custom theme with different colors but same typography
        val customTheme = Themes.Material3Light.copy(
            name = "NoteAvanue Custom",
            colorScheme = Themes.Windows11Fluent2.colorScheme
        )

        // Set partial override - inherit typography, shapes, spacing
        ThemeManager.setPartialAppTheme(
            appId = appId,
            theme = customTheme,
            inheritedProperties = listOf(
                ThemeProperties.TYPOGRAPHY,
                ThemeProperties.SHAPES,
                ThemeProperties.SPACING,
                ThemeProperties.ELEVATION,
                ThemeProperties.ANIMATION
            )
        )

        // Get the effective theme
        val noteTheme = ThemeManager.getTheme(appId)
        println("NoteAvanue theme: ${noteTheme.name}")
        println("Uses custom colors: ${noteTheme.colorScheme.primary}")
    }

    /**
     * Example 4: Using the builder pattern for theme overrides
     */
    fun example4_BuilderPattern() = runBlocking {
        val appId = "com.augmentalis.browseravanue"

        // Build a theme override using fluent API
        val override = appId.createThemeOverride()
            .partialOverride(Themes.Windows11Fluent2)
            .inheritTypography()
            .inheritSpacing()
            .withDescription("BrowserAvanue uses Windows 11 colors with universal typography")
            .build()

        // Apply the override
        ThemeManager.setPartialAppTheme(
            appId = override.appId,
            theme = override.theme,
            inheritedProperties = override.inheritedProperties
        )

        println("BrowserAvanue override created with builder pattern")
        println("Inherited properties: ${override.inheritedProperties}")
    }

    /**
     * Example 5: Observe theme changes
     */
    suspend fun example5_ObserveThemeChanges() {
        val appId = "com.augmentalis.voiceos"

        // Observe theme changes for a specific app
        ThemeManager.observeTheme(appId).collect { theme ->
            println("VoiceOS theme changed to: ${theme.name}")
            // Update UI with new theme
            applyThemeToUI(theme)
        }
    }

    /**
     * Example 6: Remove app override (fall back to universal theme)
     */
    fun example6_RemoveAppOverride() = runBlocking {
        val appId = "com.augmentalis.noteavanue"

        // Remove the override - app will use universal theme
        ThemeManager.removeAppTheme(appId)

        val theme = ThemeManager.getTheme(appId)
        println("NoteAvanue now uses universal theme: ${theme.name}")
    }

    /**
     * Example 7: List all apps with overrides
     */
    fun example7_ListAppOverrides() = runBlocking {
        val appsWithOverrides = ThemeManager.getAppsWithOverrides()

        println("Apps with theme overrides:")
        appsWithOverrides.forEach { appId ->
            val override = ThemeManager.getAppOverride(appId)
            println("  - $appId: ${override?.overrideType} (${override?.theme?.name})")
        }
    }

    /**
     * Example 8: Export and import themes
     */
    fun example8_ExportImportThemes() = runBlocking {
        // Export all themes for backup
        val export = ThemeManager.exportThemes()
        println("Exported themes:")
        println("  Universal: ${export.universalTheme.name}")
        println("  App overrides: ${export.appOverrides.size}")

        // Later, import themes (e.g., after reinstall or on another device)
        ThemeManager.importThemes(export)
        println("Themes imported successfully")
    }

    /**
     * Example 9: Change universal theme (affects all apps without overrides)
     */
    fun example9_ChangeUniversalTheme() = runBlocking {
        // Switch to Windows 11 Fluent 2 as the universal theme
        ThemeManager.setUniversalTheme(Themes.Windows11Fluent2)

        println("Universal theme changed to Windows 11 Fluent 2")
        println("This affects all apps except:")

        // List apps that won't be affected (have overrides)
        ThemeManager.getAppsWithOverrides().forEach { appId ->
            println("  - $appId (has override)")
        }
    }

    /**
     * Example 10: Working with theme sync
     */
    suspend fun example10_ThemeSync() {
        // Create a theme sync instance
        val sync = ThemeSync(
            localRepository = LocalThemeRepository(),
            cloudRepository = InMemoryThemeRepository(), // Use cloud repo in production
            conflictResolver = LastWriteWinsResolver()
        )

        // Observe sync state
        sync.syncState.collect { state ->
            when (state) {
                is SyncState.Idle -> println("Sync idle")
                is SyncState.Syncing -> println("Syncing ${state.direction}...")
                is SyncState.Success -> println("Sync completed successfully")
                is SyncState.Error -> println("Sync error: ${state.message}")
            }
        }
    }

    /**
     * Example 11: Complete app integration
     */
    fun example11_CompleteIntegration() {
        runBlocking {
        // 1. Initialize on app startup
        ThemeManager.initialize(
            repository = LocalThemeRepository(),
            syncManager = null
        )

        // 2. Load saved themes
        ThemeManager.loadThemes()

        // 3. Set default universal theme if none exists
        if (ThemeManager.getUniversalTheme().name.isEmpty()) {
            ThemeManager.setUniversalTheme(Themes.Material3Light)
        }

        // 4. Get theme for current app
        val currentAppId = "com.augmentalis.voiceos"
        val currentTheme = ThemeManager.getTheme(currentAppId)

        // 5. Apply theme to UI
        applyThemeToUI(currentTheme)

        // 6. Observe theme changes
        ThemeManager.observeTheme(currentAppId).collect { newTheme ->
            applyThemeToUI(newTheme)
        }
        }
    }

    /**
     * Example 12: Advanced - Create custom theme with inheritance
     */
    fun example12_CustomThemeWithInheritance() = runBlocking {
        val appId = "com.augmentalis.aiavanue"

        // Get the universal theme as base
        val baseTheme = ThemeManager.getUniversalTheme()

        // Create custom theme that modifies only colors
        val customTheme = baseTheme.copy(
            name = "AIAvanue Dark Mode",
            colorScheme = baseTheme.colorScheme.copy(
                mode = com.augmentalis.avamagic.components.core.ColorScheme.ColorMode.Dark,
                primary = com.augmentalis.avamagic.components.core.Color.hex("#BB86FC"),
                background = com.augmentalis.avamagic.components.core.Color.hex("#121212")
            )
        )

        // Set partial override - only colors change, everything else inherited
        ThemeManager.setPartialAppTheme(
            appId = appId,
            theme = customTheme,
            inheritedProperties = listOf(
                ThemeProperties.TYPOGRAPHY,
                ThemeProperties.SHAPES,
                ThemeProperties.SPACING,
                ThemeProperties.ELEVATION,
                ThemeProperties.MATERIAL,
                ThemeProperties.ANIMATION
            )
        )

        println("AIAvanue custom dark theme created")
    }

    // Helper function to simulate applying theme to UI
    private fun applyThemeToUI(theme: Theme) {
        println("Applying theme to UI: ${theme.name}")
        // In a real app, this would update Compose MaterialTheme or similar
    }
}

/**
 * Main entry point for running examples
 */
fun main() = runBlocking {
    println("=== Universal Theme Manager Examples ===\n")

    println("--- Example 1: Set Universal Theme ---")
    ThemeManagerExample.example1_SetUniversalTheme()

    println("\n--- Example 2: Full App Override ---")
    ThemeManagerExample.example2_FullAppOverride()

    println("\n--- Example 3: Partial App Override ---")
    ThemeManagerExample.example3_PartialAppOverride()

    println("\n--- Example 4: Builder Pattern ---")
    ThemeManagerExample.example4_BuilderPattern()

    println("\n--- Example 6: Remove App Override ---")
    ThemeManagerExample.example6_RemoveAppOverride()

    println("\n--- Example 7: List App Overrides ---")
    ThemeManagerExample.example7_ListAppOverrides()

    println("\n--- Example 8: Export/Import Themes ---")
    ThemeManagerExample.example8_ExportImportThemes()

    println("\n--- Example 9: Change Universal Theme ---")
    ThemeManagerExample.example9_ChangeUniversalTheme()

    println("\n--- Example 12: Custom Theme with Inheritance ---")
    ThemeManagerExample.example12_CustomThemeWithInheritance()

    println("\n=== All examples completed ===")
}
