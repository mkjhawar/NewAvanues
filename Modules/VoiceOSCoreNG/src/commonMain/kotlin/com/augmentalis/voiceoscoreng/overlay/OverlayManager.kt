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
 * Note: This implementation is designed for single-threaded access from the main/UI thread.
 * Platform implementations should ensure all calls are made from the appropriate thread.
 * For multi-threaded scenarios, platform-specific synchronization should be applied.
 *
 * @see IOverlay for overlay contract
 * @see OverlayConfig for configuration management
 */
package com.augmentalis.voiceoscoreng.overlay

/**
 * Centralized manager for all VoiceOSCoreNG overlays.
 *
 * Provides coordinated control and lifecycle management for multiple
 * [IOverlay] instances. This is the primary interface for the voice
 * command system to interact with visual overlays.
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
    // Overlay Registry
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Internal map of registered overlays by ID.
     * Access should be from a single thread (typically main/UI thread).
     */
    private val overlays = mutableMapOf<String, IOverlay>()

    /**
     * Number of currently registered overlays.
     */
    val overlayCount: Int
        get() = overlays.size

    // ═══════════════════════════════════════════════════════════════════════
    // Registration
    // ═══════════════════════════════════════════════════════════════════════

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
    fun register(overlay: IOverlay): Boolean {
        overlays[overlay.id] = overlay
        return true
    }

    /**
     * Unregister an overlay by ID.
     *
     * The overlay is removed from the manager but NOT disposed.
     * Call [dispose] if you want to dispose the overlay as well.
     *
     * @param id The ID of the overlay to unregister
     * @return true if the overlay was found and removed
     */
    fun unregister(id: String): Boolean {
        return overlays.remove(id) != null
    }

    /**
     * Check if an overlay with the given ID is registered.
     *
     * @param id The overlay ID to check
     * @return true if an overlay with this ID is registered
     */
    fun contains(id: String): Boolean {
        return overlays.containsKey(id)
    }

    /**
     * Get a list of all registered overlay IDs.
     *
     * @return A new list containing all registered overlay IDs
     */
    fun getAllOverlayIds(): List<String> {
        return overlays.keys.toList()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lookup
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Find an overlay by ID.
     *
     * @param id The overlay ID to find
     * @return The overlay if found, null otherwise
     */
    fun findById(id: String): IOverlay? {
        return overlays[id]
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show/Hide Operations
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show all registered overlays.
     *
     * Calls [IOverlay.show] on each registered overlay.
     */
    fun showAll() {
        overlays.values.forEach { it.show() }
    }

    /**
     * Hide all registered overlays.
     *
     * Calls [IOverlay.hide] on each registered overlay.
     */
    fun hideAll() {
        overlays.values.forEach { it.hide() }
    }

    /**
     * Show a specific overlay by ID.
     *
     * @param id The overlay ID to show
     * @return true if the overlay was found and shown
     */
    fun show(id: String): Boolean {
        val overlay = overlays[id]
        return if (overlay != null) {
            overlay.show()
            true
        } else {
            false
        }
    }

    /**
     * Hide a specific overlay by ID.
     *
     * @param id The overlay ID to hide
     * @return true if the overlay was found and hidden
     */
    fun hide(id: String): Boolean {
        val overlay = overlays[id]
        return if (overlay != null) {
            overlay.hide()
            true
        } else {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility Queries
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Check if any overlay is currently visible.
     *
     * @return true if at least one overlay is visible
     */
    fun isAnyVisible(): Boolean {
        return overlays.values.any { it.isVisible }
    }

    /**
     * Check if a specific overlay is visible.
     *
     * @param id The overlay ID to check
     * @return true if the overlay exists and is visible
     */
    fun isVisible(id: String): Boolean {
        return overlays[id]?.isVisible ?: false
    }

    /**
     * Get the IDs of all currently visible overlays.
     *
     * @return A list of IDs for visible overlays
     */
    fun getVisibleOverlayIds(): List<String> {
        return overlays.filter { it.value.isVisible }.keys.toList()
    }

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
        val overlay = overlays[id]
        return if (overlay != null) {
            overlay.update(data)
            true
        } else {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Disposal
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
    fun disposeAll() {
        overlays.values.forEach { overlay ->
            overlay.hide()
            overlay.dispose()
        }
        overlays.clear()
    }

    /**
     * Dispose a specific overlay by ID.
     *
     * The overlay is disposed and removed from the manager.
     *
     * @param id The overlay ID to dispose
     * @return true if the overlay was found and disposed
     */
    fun dispose(id: String): Boolean {
        val overlay = overlays.remove(id)
        return if (overlay != null) {
            overlay.hide()
            overlay.dispose()
            true
        } else {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Clear (Without Disposal)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Clear all overlays from the manager WITHOUT disposing them.
     *
     * Use this when you want to re-use overlays elsewhere.
     * Use [disposeAll] if you want to properly clean up resources.
     */
    fun clear() {
        overlays.clear()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Iteration
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Execute an action for each registered overlay.
     *
     * @param action The action to execute for each overlay
     */
    fun forEach(action: (IOverlay) -> Unit) {
        overlays.values.forEach(action)
    }
}
