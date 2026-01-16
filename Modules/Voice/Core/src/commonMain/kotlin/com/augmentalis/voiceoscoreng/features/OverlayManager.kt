/**
 * OverlayManager.kt - Centralized overlay management system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * KMP-compatible centralized manager for all accessibility overlays.
 * Provides:
 * - Registration/unregistration of IOverlay instances
 * - Lookup by ID
 * - Show/hide all or individual overlays
 * - Disposal management
 * - Config propagation
 *
 * Refactored for SRP compliance using composition:
 * - [OverlayRegistry] - Registration and lookup
 * - [OverlayVisibilityManager] - Show/hide operations
 * - [OverlayDisposal] - Disposal and cleanup
 *
 * Note: This implementation is designed for single-threaded access from the main/UI thread.
 * Platform implementations should ensure all calls are made from the appropriate thread.
 * For multi-threaded scenarios, platform-specific synchronization should be applied.
 *
 * @see IOverlay for overlay contract
 * @see OverlayConfig for configuration management
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Centralized manager for all VoiceOSCoreNG overlays.
 *
 * Provides coordinated control and lifecycle management for multiple
 * [IOverlay] instances. This is the primary interface for the voice
 * command system to interact with visual overlays.
 *
 * Uses composition of focused managers for better SRP compliance:
 * - [OverlayRegistry] handles registration and lookup
 * - [OverlayVisibilityManager] handles show/hide operations
 * - [OverlayDisposal] handles disposal and cleanup
 *
 * Usage:
 * ```kotlin
 * val manager = OverlayManager()
 *
 * // Register overlays
 * manager.register(statusOverlay)
 * manager.register(confidenceOverlay)
 *
 * // Show specific overlay
 * manager.show("status")
 *
 * // Update overlay data
 * manager.update("status", OverlayData.Status("Processing...", CommandState.PROCESSING))
 *
 * // Hide all overlays
 * manager.hideAll()
 *
 * // Clean up
 * manager.disposeAll()
 * ```
 *
 * @property config Global overlay configuration
 */
class OverlayManager(
    config: OverlayConfig = OverlayConfig()
) {

    // ═══════════════════════════════════════════════════════════════════════
    // Composed Managers (SRP Delegation)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Registry for overlay storage and lookup.
     */
    private val registry = OverlayRegistry()

    /**
     * Manager for visibility operations.
     */
    private val visibility = OverlayVisibilityManager(registry)

    /**
     * Manager for disposal operations.
     */
    private val disposal = OverlayDisposal(registry)

    // ═══════════════════════════════════════════════════════════════════════
    // Configuration
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Global overlay configuration.
     *
     * Provides theme settings, accessibility options, and display preferences
     * that can be applied to all registered overlays.
     */
    var config: OverlayConfig = config
        private set

    /**
     * Update the global configuration.
     *
     * @param newConfig The new configuration to use
     */
    fun updateConfig(newConfig: OverlayConfig) {
        config = newConfig
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Registration (Delegated to OverlayRegistry)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Number of currently registered overlays.
     */
    val overlayCount: Int
        get() = registry.count

    /**
     * Register an overlay with the manager.
     *
     * If an overlay with the same ID is already registered, it will be replaced.
     * The previous overlay is NOT disposed automatically - call [dispose] first
     * if needed.
     *
     * @param overlay The overlay to register
     * @return true if registration succeeded
     */
    fun register(overlay: IOverlay): Boolean = registry.register(overlay)

    /**
     * Unregister an overlay by ID.
     *
     * The overlay is removed from the manager but NOT disposed.
     * Call [dispose] if you want to dispose the overlay as well.
     *
     * @param id The ID of the overlay to unregister
     * @return true if the overlay was found and removed
     */
    fun unregister(id: String): Boolean = registry.unregister(id)

    /**
     * Check if an overlay with the given ID is registered.
     *
     * @param id The overlay ID to check
     * @return true if an overlay with this ID is registered
     */
    fun contains(id: String): Boolean = registry.contains(id)

    /**
     * Get a list of all registered overlay IDs.
     *
     * @return A new list containing all registered overlay IDs
     */
    fun getAllOverlayIds(): List<String> = registry.getAllIds()

    // ═══════════════════════════════════════════════════════════════════════
    // Lookup (Delegated to OverlayRegistry)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Find an overlay by ID.
     *
     * @param id The overlay ID to find
     * @return The overlay if found, null otherwise
     */
    fun findById(id: String): IOverlay? = registry.findById(id)

    // ═══════════════════════════════════════════════════════════════════════
    // Show/Hide Operations (Delegated to OverlayVisibilityManager)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show all registered overlays.
     *
     * Calls [IOverlay.show] on each registered overlay.
     */
    fun showAll() = visibility.showAll()

    /**
     * Hide all registered overlays.
     *
     * Calls [IOverlay.hide] on each registered overlay.
     */
    fun hideAll() = visibility.hideAll()

    /**
     * Show a specific overlay by ID.
     *
     * @param id The overlay ID to show
     * @return true if the overlay was found and shown
     */
    fun show(id: String): Boolean = visibility.show(id)

    /**
     * Hide a specific overlay by ID.
     *
     * @param id The overlay ID to hide
     * @return true if the overlay was found and hidden
     */
    fun hide(id: String): Boolean = visibility.hide(id)

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility Queries (Delegated to OverlayVisibilityManager)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Check if any overlay is currently visible.
     *
     * @return true if at least one overlay is visible
     */
    fun isAnyVisible(): Boolean = visibility.isAnyVisible()

    /**
     * Check if a specific overlay is visible.
     *
     * @param id The overlay ID to check
     * @return true if the overlay exists and is visible
     */
    fun isVisible(id: String): Boolean = visibility.isVisible(id)

    /**
     * Get the IDs of all currently visible overlays.
     *
     * @return A list of IDs for visible overlays
     */
    fun getVisibleOverlayIds(): List<String> = visibility.getVisibleIds()

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update a specific overlay with new data.
     *
     * @param id The overlay ID to update
     * @param data The new data to display
     * @return true if the overlay was found and updated
     */
    fun update(id: String, data: OverlayData): Boolean {
        val overlay = registry.findById(id) ?: return false
        overlay.update(data)
        return true
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Disposal (Delegated to OverlayDisposal)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dispose all registered overlays and clear the registry.
     *
     * This:
     * 1. Hides all overlays
     * 2. Disposes all overlays
     * 3. Clears the registry
     *
     * After calling this, [overlayCount] will be 0.
     */
    fun disposeAll() = disposal.disposeAll()

    /**
     * Dispose a specific overlay by ID.
     *
     * The overlay is disposed and removed from the manager.
     *
     * @param id The overlay ID to dispose
     * @return true if the overlay was found and disposed
     */
    fun dispose(id: String): Boolean = disposal.dispose(id)

    // ═══════════════════════════════════════════════════════════════════════
    // Clear (Without Disposal) - Delegated to OverlayRegistry
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Clear all overlays from the manager WITHOUT disposing them.
     *
     * Use this when you want to re-use overlays elsewhere.
     * Use [disposeAll] if you want to properly clean up resources.
     */
    fun clear() = registry.clear()

    // ═══════════════════════════════════════════════════════════════════════
    // Iteration (Delegated to OverlayRegistry)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Execute an action for each registered overlay.
     *
     * @param action The action to execute for each overlay
     */
    fun forEach(action: (IOverlay) -> Unit) = registry.forEach(action)
}
