/**
 * OverlayDisposal.kt - Overlay disposal management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-08
 *
 * Single Responsibility: Manages overlay disposal and cleanup.
 * Part of the OverlayManager refactoring for SRP compliance.
 *
 * @see OverlayManager for the main facade
 * @see OverlayRegistry for overlay storage
 */
package com.augmentalis.commandmanager

/**
 * Manages overlay disposal operations.
 *
 * This class is responsible for:
 * - Disposing individual overlays
 * - Disposing all overlays
 * - Ensuring proper cleanup (hide before dispose)
 *
 * Delegates to [OverlayRegistry] for overlay lookup and removal.
 *
 * @property registry The registry to use for overlay lookup and removal
 */
internal class OverlayDisposal(
    private val registry: OverlayRegistry
) {

    /**
     * Dispose all registered overlays and clear the registry.
     *
     * This:
     * 1. Hides all overlays
     * 2. Disposes all overlays
     * 3. Clears the registry
     *
     * After calling this, the registry will be empty.
     */
    fun disposeAll() {
        registry.forEach { overlay ->
            overlay.hide()
            overlay.dispose()
        }
        registry.clear()
    }

    /**
     * Dispose a specific overlay by ID.
     *
     * The overlay is hidden, disposed, and removed from the registry.
     *
     * @param id The overlay ID to dispose
     * @return true if the overlay was found and disposed
     */
    fun dispose(id: String): Boolean {
        val overlay = registry.findById(id) ?: return false
        overlay.hide()
        overlay.dispose()
        registry.unregister(id)
        return true
    }
}
