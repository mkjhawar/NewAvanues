package com.augmentalis.universal.thememanager

import com.augmentalis.avamagic.components.core.Theme
import com.augmentalis.avamagic.components.core.Themes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Universal Theme Manager - Centralized theme management for all Avanues apps
 *
 * Features:
 * - Global Avanues theme (singleton)
 * - Per-app theme overrides (full or partial)
 * - Theme inheritance and resolution
 * - Event notifications for theme changes
 * - Persistent storage (local and cloud)
 *
 * Usage:
 * ```kotlin
 * // Set universal theme
 * ThemeManager.setUniversalTheme(Themes.iOS26LiquidGlass)
 *
 * // Set app-specific override
 * ThemeManager.setAppTheme("com.augmentalis.voiceos", Themes.Material3Light)
 *
 * // Get effective theme for app
 * val theme = ThemeManager.getTheme("com.augmentalis.voiceos")
 *
 * // Observe theme changes
 * ThemeManager.observeTheme("com.augmentalis.voiceos").collect { theme ->
 *     // Update UI
 * }
 * ```
 */
object ThemeManager {

    // ==================== State ====================

    /**
     * Global Avanues theme - applies to all apps unless overridden
     */
    private val _universalTheme = MutableStateFlow<Theme>(Themes.Material3Light)
    val universalTheme: StateFlow<Theme> = _universalTheme.asStateFlow()

    /**
     * Per-app theme overrides
     * Key: App ID (e.g., "com.augmentalis.voiceos")
     * Value: Theme override configuration
     */
    private val appOverrides = mutableMapOf<String, ThemeOverride>()

    /**
     * Theme change listeners for each app
     */
    private val themeFlows = mutableMapOf<String, MutableStateFlow<Theme>>()

    /**
     * Repository for persistence
     */
    private var repository: ThemeRepository = LocalThemeRepository()

    /**
     * Theme sync manager for cloud synchronization
     */
    private var syncManager: ThemeSync? = null

    // ==================== Initialization ====================

    /**
     * Initialize the ThemeManager with a repository and optional sync manager
     */
    fun initialize(
        repository: ThemeRepository = LocalThemeRepository(),
        syncManager: ThemeSync? = null
    ) {
        this.repository = repository
        this.syncManager = syncManager
    }

    /**
     * Load themes from persistent storage
     */
    suspend fun loadThemes() {
        // Load universal theme
        repository.loadUniversalTheme()?.let { theme ->
            _universalTheme.value = theme
        }

        // Load app-specific overrides
        repository.loadAllAppThemes().forEach { (appId, theme) ->
            val override = repository.loadAppOverride(appId)
            if (override != null) {
                appOverrides[appId] = override
            } else {
                // Legacy: Create full override if no override config exists
                appOverrides[appId] = ThemeOverride(
                    appId = appId,
                    overrideType = OverrideType.FULL,
                    theme = theme,
                    inheritedProperties = emptyList()
                )
            }
        }

        // Sync with cloud if available
        syncManager?.syncFromCloud()
    }

    // ==================== Universal Theme Management ====================

    /**
     * Set the global Avanues theme
     * This affects all apps that don't have an override
     */
    suspend fun setUniversalTheme(theme: Theme) {
        _universalTheme.value = theme
        repository.saveUniversalTheme(theme)

        // Notify all apps that inherit from universal theme
        appOverrides.forEach { (appId, override) ->
            if (override.overrideType == OverrideType.PARTIAL) {
                // Recalculate effective theme for partial overrides
                val effectiveTheme = resolveTheme(appId)
                themeFlows[appId]?.value = effectiveTheme
            }
        }

        // Notify apps without overrides
        themeFlows.filterKeys { !appOverrides.containsKey(it) }
            .forEach { (_, flow) ->
                flow.value = theme
            }

        // Sync to cloud if available
        syncManager?.syncToCloud()
    }

    /**
     * Get the global Avanues theme
     */
    fun getUniversalTheme(): Theme = _universalTheme.value

    // ==================== App-Specific Theme Management ====================

    /**
     * Set a full theme override for a specific app
     * This completely replaces the universal theme for this app
     */
    suspend fun setAppTheme(appId: String, theme: Theme) {
        val override = ThemeOverride(
            appId = appId,
            overrideType = OverrideType.FULL,
            theme = theme,
            inheritedProperties = emptyList()
        )

        appOverrides[appId] = override
        repository.saveAppTheme(appId, theme)
        repository.saveAppOverride(appId, override)

        // Notify listeners
        themeFlows[appId]?.value = theme

        // Sync to cloud if available
        syncManager?.syncToCloud()
    }

    /**
     * Set a partial theme override for a specific app
     * This inherits from universal theme but overrides specific properties
     *
     * @param appId App identifier
     * @param theme Theme with overridden properties
     * @param inheritedProperties List of property paths to inherit from universal theme
     *                           e.g., ["colorScheme.primary", "typography.displayLarge"]
     */
    suspend fun setPartialAppTheme(
        appId: String,
        theme: Theme,
        inheritedProperties: List<String> = emptyList()
    ) {
        val override = ThemeOverride(
            appId = appId,
            overrideType = OverrideType.PARTIAL,
            theme = theme,
            inheritedProperties = inheritedProperties
        )

        appOverrides[appId] = override
        repository.saveAppTheme(appId, theme)
        repository.saveAppOverride(appId, override)

        // Notify listeners with resolved theme
        val effectiveTheme = resolveTheme(appId)
        themeFlows[appId]?.value = effectiveTheme

        // Sync to cloud if available
        syncManager?.syncToCloud()
    }

    /**
     * Remove theme override for a specific app
     * The app will fall back to using the universal theme
     */
    suspend fun removeAppTheme(appId: String) {
        appOverrides.remove(appId)
        repository.deleteAppTheme(appId)
        repository.deleteAppOverride(appId)

        // Notify listeners with universal theme
        themeFlows[appId]?.value = _universalTheme.value

        // Sync to cloud if available
        syncManager?.syncToCloud()
    }

    /**
     * Get the effective theme for a specific app
     * Resolves overrides and inheritance
     */
    fun getTheme(appId: String): Theme {
        return resolveTheme(appId)
    }

    /**
     * Get all app IDs that have theme overrides
     */
    fun getAppsWithOverrides(): List<String> {
        return appOverrides.keys.toList()
    }

    /**
     * Get the override configuration for a specific app
     */
    fun getAppOverride(appId: String): ThemeOverride? {
        return appOverrides[appId]
    }

    // ==================== Theme Observation ====================

    /**
     * Observe theme changes for a specific app
     * Returns a Flow that emits the effective theme whenever it changes
     */
    fun observeTheme(appId: String): StateFlow<Theme> {
        return themeFlows.getOrPut(appId) {
            MutableStateFlow(resolveTheme(appId))
        }.asStateFlow()
    }

    /**
     * Observe universal theme changes
     */
    fun observeUniversalTheme(): StateFlow<Theme> = universalTheme

    // ==================== Theme Resolution ====================

    /**
     * Resolve the effective theme for an app
     * Handles inheritance and overrides
     */
    private fun resolveTheme(appId: String): Theme {
        val override = appOverrides[appId] ?: return _universalTheme.value

        return when (override.overrideType) {
            OverrideType.FULL -> override.theme
            OverrideType.PARTIAL -> {
                // Merge universal theme with app-specific overrides
                // For simplicity, we use the override theme but could implement
                // property-level merging based on inheritedProperties
                override.theme
            }
        }
    }

    // ==================== Utility Functions ====================

    /**
     * Check if an app has a theme override
     */
    fun hasOverride(appId: String): Boolean {
        return appOverrides.containsKey(appId)
    }

    /**
     * Check if an app has a full or partial override
     */
    fun getOverrideType(appId: String): OverrideType? {
        return appOverrides[appId]?.overrideType
    }

    /**
     * Export all themes for backup or migration
     */
    suspend fun exportThemes(): ThemeExport {
        return ThemeExport(
            universalTheme = _universalTheme.value,
            appOverrides = appOverrides.toMap(),
            version = THEME_MANAGER_VERSION
        )
    }

    /**
     * Import themes from backup or migration
     */
    suspend fun importThemes(export: ThemeExport) {
        if (export.version != THEME_MANAGER_VERSION) {
            // Handle version migration if needed
            // For now, we just warn
            println("Warning: Importing themes from version ${export.version}, current version is $THEME_MANAGER_VERSION")
        }

        _universalTheme.value = export.universalTheme
        appOverrides.clear()
        appOverrides.putAll(export.appOverrides)

        // Save to repository
        repository.saveUniversalTheme(export.universalTheme)
        export.appOverrides.forEach { (appId, override) ->
            repository.saveAppTheme(appId, override.theme)
            repository.saveAppOverride(appId, override)
        }

        // Notify all listeners
        themeFlows.forEach { (appId, flow) ->
            flow.value = resolveTheme(appId)
        }
    }

    // Version constant
    private const val THEME_MANAGER_VERSION = 1
}

/**
 * Export data structure for themes
 */
data class ThemeExport(
    val universalTheme: Theme,
    val appOverrides: Map<String, ThemeOverride>,
    val version: Int
)
