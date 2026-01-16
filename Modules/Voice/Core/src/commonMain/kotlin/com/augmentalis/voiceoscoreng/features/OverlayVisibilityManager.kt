/**
 * OverlayVisibilityManager.kt - Overlay visibility control
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-08
 *
 * Single Responsibility: Manages overlay visibility operations.
 * Part of the OverlayManager refactoring for SRP compliance.
 *
 * @see OverlayManager for the main facade
 * @see OverlayRegistry for overlay storage
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Manages overlay visibility operations.
 *
 * This class is responsible for:
 * - Showing/hiding individual overlays
 * - Showing/hiding all overlays
 * - Querying visibility state
 *
 * Delegates to [OverlayRegistry] for overlay lookup.
 *
 * @property registry The registry to use for overlay lookup
 */
internal class OverlayVisibilityManager(
    private val registry: OverlayRegistry
) {

    /**
     * Show all registered overlays.
     *
     * Calls [IOverlay.show] on each registered overlay.
     */
    fun showAll() {
        registry.forEach { it.show() }
    }

    /**
     * Hide all registered overlays.
     *
     * Calls [IOverlay.hide] on each registered overlay.
     */
    fun hideAll() {
        registry.forEach { it.hide() }
    }

    /**
     * Show a specific overlay by ID.
     *
     * @param id The overlay ID to show
     * @return true if the overlay was found and shown
     */
    fun show(id: String): Boolean {
        val overlay = registry.findById(id) ?: return false
        overlay.show()
        return true
    }

    /**
     * Hide a specific overlay by ID.
     *
     * @param id The overlay ID to hide
     * @return true if the overlay was found and hidden
     */
    fun hide(id: String): Boolean {
        val overlay = registry.findById(id) ?: return false
        overlay.hide()
        return true
    }

    /**
     * Check if any overlay is currently visible.
     *
     * @return true if at least one overlay is visible
     */
    fun isAnyVisible(): Boolean {
        return registry.getAll().any { it.isVisible }
    }

    /**
     * Check if a specific overlay is visible.
     *
     * @param id The overlay ID to check
     * @return true if the overlay exists and is visible
     */
    fun isVisible(id: String): Boolean {
        return registry.findById(id)?.isVisible ?: false
    }

    /**
     * Get the IDs of all currently visible overlays.
     *
     * @return A list of IDs for visible overlays
     */
    fun getVisibleIds(): List<String> {
        return registry.getAll()
            .filter { it.isVisible }
            .map { it.id }
    }
}
