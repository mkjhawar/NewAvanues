/**
 * OverlayRegistry.kt - Overlay registration management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-08
 *
 * Single Responsibility: Manages overlay registration and lookup.
 * Part of the OverlayManager refactoring for SRP compliance.
 *
 * @see OverlayManager for the main facade
 * @see IOverlay for overlay contract
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Manages overlay registration and lookup.
 *
 * This class is responsible for:
 * - Storing registered overlays
 * - Looking up overlays by ID
 * - Tracking registered overlay IDs
 * - Providing iteration over overlays
 *
 * Thread safety note: This implementation is designed for single-threaded
 * access from the main/UI thread. Platform implementations should ensure
 * all calls are made from the appropriate thread.
 */
internal class OverlayRegistry {

    /**
     * Internal map of registered overlays by ID.
     */
    private val overlays = mutableMapOf<String, IOverlay>()

    /**
     * Number of currently registered overlays.
     */
    val count: Int
        get() = overlays.size

    /**
     * Register an overlay with the registry.
     *
     * If an overlay with the same ID is already registered, it will be replaced.
     * The previous overlay is NOT disposed automatically - call dispose first
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
     * The overlay is removed from the registry but NOT disposed.
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
    fun getAllIds(): List<String> {
        return overlays.keys.toList()
    }

    /**
     * Find an overlay by ID.
     *
     * @param id The overlay ID to find
     * @return The overlay if found, null otherwise
     */
    fun findById(id: String): IOverlay? {
        return overlays[id]
    }

    /**
     * Get all registered overlays.
     *
     * @return A collection of all registered overlays
     */
    fun getAll(): Collection<IOverlay> {
        return overlays.values
    }

    /**
     * Clear all overlays from the registry WITHOUT disposing them.
     *
     * Use this when you want to re-use overlays elsewhere.
     */
    fun clear() {
        overlays.clear()
    }

    /**
     * Execute an action for each registered overlay.
     *
     * @param action The action to execute for each overlay
     */
    fun forEach(action: (IOverlay) -> Unit) {
        overlays.values.forEach(action)
    }
}
