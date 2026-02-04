package com.augmentalis.voiceos.themebridge

import com.augmentalis.avanue.core.managers.ThemeManager as LegacyThemeManager
import com.augmentalis.avanue.core.managers.ThemeObserver as LegacyThemeObserver
import com.augmentalis.avanue.core.models.Theme as LegacyTheme
import com.augmentalis.avanue.core.models.ThemeComponent as LegacyComponent
import com.augmentalis.avanues.avaui.theme.ThemeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Theme Migration Bridge - Avanue4 ↔ AvaUI compatibility layer.
 *
 * Enables gradual migration from legacy Avanue4 theme system to modern AvaUI
 * theme engine. Maintains bidirectional sync between both systems during transition.
 *
 * ## Key Features
 *
 * - **Bidirectional Sync**: Changes in either system propagate to the other
 * - **Observer Pattern**: Implements [LegacyThemeObserver] to track legacy changes
 * - **Reactive State**: Exposes AvaUI theme as [StateFlow] for Compose integration
 * - **Loop Prevention**: Guards against infinite sync loops
 * - **Type Safety**: Compile-time guarantees for theme mappings
 *
 * ## Architecture
 *
 * ```
 * Legacy ThemeManager → Bridge → AvaUI ThemeConfig
 *       ↑                          ↓
 *       └──────── Bidirectional ───┘
 * ```
 *
 * ## Usage
 *
 * ### Basic Setup
 *
 * ```kotlin
 * class MyActivity : BaseActivity() {
 *     private lateinit var bridge: ThemeMigrationBridge
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // Create bridge connecting both systems
 *         bridge = ThemeMigrationBridge(
 *             legacyThemeManager = themeManager,
 *             enableBidirectionalSync = true
 *         )
 *         bridge.initialize()
 *
 *         // Observe AvaUI theme in Compose
 *         lifecycleScope.launch {
 *             bridge.magicUiTheme.collect { theme ->
 *                 theme?.let { applyMagicUiTheme(it) }
 *             }
 *         }
 *
 *         // Legacy components automatically receive updates via ThemeManager
 *     }
 *
 *     override fun onDestroy() {
 *         bridge.cleanup()
 *         super.onDestroy()
 *     }
 * }
 * ```
 *
 * ### Updating Themes
 *
 * ```kotlin
 * // Update via AvaUI (syncs to legacy if bidirectional enabled)
 * val newTheme = currentTheme.copy(
 *     palette = currentTheme.palette.copy(primary = "#FF5722")
 * )
 * bridge.updateMagicUiTheme(newTheme)
 *
 * // Update via legacy API (syncs to AvaUI automatically)
 * bridge.updateComponent(LegacyComponent.PRIMARY_COLOR, 0xFFFF5722.toInt())
 *
 * // Reset both systems
 * bridge.resetToDefault()
 * ```
 *
 * ## Sync Behavior
 *
 * **With Bidirectional Sync Enabled** (default):
 * - Legacy → AvaUI: Always syncs
 * - AvaUI → Legacy: Always syncs
 * - Loop prevention: Detects and breaks sync loops
 *
 * **With Bidirectional Sync Disabled**:
 * - Legacy → AvaUI: Always syncs
 * - AvaUI → Legacy: No sync (manual control only)
 *
 * ## Thread Safety
 *
 * This class is thread-safe for concurrent access to [magicUiTheme].
 * Observer callbacks are executed on the caller's thread.
 *
 * @property legacyThemeManager Legacy Avanue4 theme manager instance
 * @property enableBidirectionalSync Enable AvaUI → Legacy sync (default: true)
 * @property converter Theme converter for type conversions
 * @property mapper Structure mapper for incremental updates
 *
 * @since 3.1.0
 */
class ThemeMigrationBridge(
    private val legacyThemeManager: LegacyThemeManager,
    private val enableBidirectionalSync: Boolean = true,
    private val converter: ThemeConverter = ThemeConverter(),
    private val mapper: ThemeStructureMapper = ThemeStructureMapper()
) : LegacyThemeObserver {

    private val _magicUiTheme = MutableStateFlow<ThemeConfig?>(null)

    /**
     * Current AvaUI theme (reactive state).
     *
     * Collect this flow in Compose or coroutines to observe theme changes:
     *
     * ```kotlin
     * lifecycleScope.launch {
     *     bridge.magicUiTheme.collect { theme ->
     *         theme?.let { applyTheme(it) }
     *     }
     * }
     * ```
     *
     * The flow emits:
     * - `null` initially (before [initialize] is called)
     * - Non-null [ThemeConfig] after initialization
     * - Updates whenever theme changes in either system
     */
    val magicUiTheme: StateFlow<ThemeConfig?> = _magicUiTheme.asStateFlow()

    /**
     * Flag to prevent infinite sync loops during bidirectional updates.
     *
     * When true, AvaUI → Legacy sync is temporarily disabled to prevent
     * the legacy observer callback from triggering another AvaUI update.
     */
    private var isSyncingToLegacy = false

    /**
     * Flag to prevent infinite sync loops during legacy updates.
     *
     * When true, Legacy → AvaUI sync is temporarily disabled to prevent
     * the AvaUI update from triggering another legacy update.
     */
    private var isSyncingFromLegacy = false

    /**
     * Initialization state tracking.
     */
    private var isInitialized = false

    /**
     * Initialize bridge and perform initial sync.
     *
     * **Must be called before using the bridge.**
     *
     * Performs the following:
     * 1. Registers this bridge as a legacy theme observer
     * 2. Loads current legacy theme
     * 3. Converts to AvaUI format
     * 4. Emits initial AvaUI theme to [magicUiTheme] flow
     *
     * Safe to call multiple times (subsequent calls are ignored).
     *
     * @throws IllegalStateException if legacy theme manager is not initialized
     */
    fun initialize() {
        if (isInitialized) return

        // Register as observer of legacy theme manager
        legacyThemeManager.addObserver(this)

        // Perform initial sync: Legacy → AvaUI
        val currentLegacyTheme = legacyThemeManager.getCurrentTheme()
        if (currentLegacyTheme != null) {
            val magicTheme = converter.convertLegacyToAvaUI(currentLegacyTheme)
            _magicUiTheme.value = magicTheme
        } else {
            // If no theme exists, initialize legacy manager first
            legacyThemeManager.initialize()
            legacyThemeManager.getCurrentTheme()?.let { theme ->
                val magicTheme = converter.convertLegacyToAvaUI(theme)
                _magicUiTheme.value = magicTheme
            }
        }

        isInitialized = true
    }

    /**
     * Update AvaUI theme (triggers sync to legacy if bidirectional enabled).
     *
     * Updates the AvaUI theme and optionally syncs to the legacy system.
     *
     * **Sync Behavior**:
     * - If [enableBidirectionalSync] is true: Converts theme and applies to legacy
     * - If false: Only updates AvaUI theme, legacy unchanged
     *
     * **Loop Prevention**: Uses [isSyncingToLegacy] guard to prevent infinite loops.
     *
     * @param theme New AvaUI theme to apply
     *
     * @see enableBidirectionalSync
     */
    fun updateMagicUiTheme(theme: ThemeConfig) {
        // Update AvaUI theme
        _magicUiTheme.value = theme

        // Sync to legacy if bidirectional sync enabled and not already syncing
        if (enableBidirectionalSync && !isSyncingFromLegacy) {
            try {
                isSyncingToLegacy = true
                val legacyTheme = converter.convertAvaUIToLegacy(theme)
                legacyThemeManager.applyTheme(legacyTheme)
            } finally {
                isSyncingToLegacy = false
            }
        }
    }

    /**
     * Update specific theme component (legacy API support).
     *
     * Delegates to legacy theme manager's [LegacyThemeManager.updateThemeComponent].
     * The update will be propagated back to AvaUI via the observer pattern.
     *
     * **Use Case**: When migrating apps need to update themes using legacy API
     * while still keeping AvaUI in sync.
     *
     * @param component Legacy theme component to update
     * @param value New value (Int for colors, Float/Boolean for others)
     *
     * @see onThemeComponentChanged
     */
    fun updateComponent(component: LegacyComponent, value: Any) {
        legacyThemeManager.updateThemeComponent(component, value)
        // Observer callback will handle sync to AvaUI
    }

    /**
     * Reset to default theme (both systems).
     *
     * Resets the legacy theme to defaults, which triggers an observer callback
     * that syncs the default theme to AvaUI.
     *
     * **Result**: Both systems will have their default themes applied.
     *
     * @see onThemeReset
     */
    fun resetToDefault() {
        legacyThemeManager.resetToDefault()
        // Observer callback will handle sync to AvaUI
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LegacyThemeObserver Implementation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called when the legacy theme is loaded.
     *
     * Converts the loaded theme to AvaUI format and updates [magicUiTheme].
     *
     * **Loop Prevention**: Not needed here as this is only called during
     * initial load, not during sync operations.
     *
     * @param theme Loaded legacy theme
     */
    override fun onThemeLoaded(theme: LegacyTheme) {
        val magicTheme = converter.convertLegacyToAvaUI(theme)
        _magicUiTheme.value = magicTheme
    }

    /**
     * Called when the entire legacy theme changes.
     *
     * Converts the new theme to AvaUI format and updates [magicUiTheme].
     *
     * **Loop Prevention**: Skips sync if currently syncing AvaUI → Legacy
     * to prevent infinite loops.
     *
     * @param theme New legacy theme
     */
    override fun onThemeChanged(theme: LegacyTheme) {
        // Skip update if we're currently syncing to legacy to prevent loop
        if (isSyncingToLegacy) return

        try {
            isSyncingFromLegacy = true
            val magicTheme = converter.convertLegacyToAvaUI(theme)
            _magicUiTheme.value = magicTheme
        } finally {
            isSyncingFromLegacy = false
        }
    }

    /**
     * Called when a single legacy theme component changes.
     *
     * Performs incremental update on the AvaUI theme by only updating
     * the corresponding palette field, preserving all other theme properties.
     *
     * **Efficiency**: Avoids full theme conversion for single component changes.
     *
     * **Loop Prevention**: Skips sync if currently syncing AvaUI → Legacy.
     *
     * @param component Legacy component that changed
     * @param value New value (Int for colors, Float/Boolean for others)
     *
     * @see ThemeStructureMapper.updateComponentInMagicTheme
     */
    override fun onThemeComponentChanged(component: LegacyComponent, value: Any) {
        // Skip update if we're currently syncing to legacy to prevent loop
        if (isSyncingToLegacy) return

        try {
            isSyncingFromLegacy = true

            // Incremental update: only update changed component in AvaUI theme
            _magicUiTheme.value?.let { currentTheme ->
                val updatedTheme = mapper.updateComponentInMagicTheme(
                    currentTheme, component, value
                )
                _magicUiTheme.value = updatedTheme
            }
        } finally {
            isSyncingFromLegacy = false
        }
    }

    /**
     * Called when the legacy theme is reset to default.
     *
     * Converts the default theme to AvaUI format and updates [magicUiTheme].
     *
     * **Loop Prevention**: Skips sync if currently syncing AvaUI → Legacy.
     *
     * @param theme Default legacy theme
     */
    override fun onThemeReset(theme: LegacyTheme) {
        // Skip update if we're currently syncing to legacy to prevent loop
        if (isSyncingToLegacy) return

        try {
            isSyncingFromLegacy = true
            val magicTheme = converter.convertLegacyToAvaUI(theme)
            _magicUiTheme.value = magicTheme
        } finally {
            isSyncingFromLegacy = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle Management
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Cleanup bridge resources.
     *
     * **Must be called when done using the bridge** (typically in Activity.onDestroy).
     *
     * Performs the following:
     * 1. Unregisters this bridge as a legacy theme observer
     * 2. Clears initialization state
     *
     * After cleanup, [initialize] must be called again to reuse the bridge.
     *
     * Safe to call multiple times.
     */
    fun cleanup() {
        if (isInitialized) {
            legacyThemeManager.removeObserver(this)
            isInitialized = false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Debugging & Introspection
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Check if bridge is initialized.
     *
     * @return true if [initialize] has been called and [cleanup] has not
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Check if bidirectional sync is enabled.
     *
     * @return true if AvaUI → Legacy sync is enabled
     */
    fun isBidirectionalSyncEnabled(): Boolean = enableBidirectionalSync

    /**
     * Get current sync state for debugging.
     *
     * @return Map of sync state flags
     */
    fun getSyncState(): Map<String, Boolean> {
        return mapOf(
            "isInitialized" to isInitialized,
            "isSyncingToLegacy" to isSyncingToLegacy,
            "isSyncingFromLegacy" to isSyncingFromLegacy,
            "bidirectionalSyncEnabled" to enableBidirectionalSync,
            "hasMagicTheme" to (_magicUiTheme.value != null)
        )
    }
}
