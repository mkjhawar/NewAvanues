/**
 * Overlay Z-Index Manager
 *
 * Manages z-ordering for overlay windows to ensure proper layering.
 * Uses Android WindowManager.LayoutParams.type ordering with custom
 * z-offset tracking for multiple overlay components.
 *
 * Created: 2025-12-03
 * Author: AVA AI Team
 */

package com.augmentalis.overlay.service

import android.view.WindowManager
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Overlay layer types with relative ordering
 */
enum class OverlayLayer(val baseOffset: Int) {
    /** Background layer (contextual info, status) */
    BACKGROUND(0),

    /** Main content layer (panels, cards) */
    CONTENT(100),

    /** Interactive elements (orb, buttons) */
    INTERACTIVE(200),

    /** Modal dialogs */
    DIALOG(300),

    /** Toast notifications */
    TOAST(400),

    /** Critical alerts (errors, permissions) */
    ALERT(500)
}

/**
 * Registered overlay window info
 */
data class OverlayWindowInfo(
    val id: String,
    val layer: OverlayLayer,
    val zIndex: Int,
    val registeredAt: Long = System.currentTimeMillis()
)

/**
 * Singleton manager for overlay z-index coordination
 *
 * Features:
 * - Automatic z-index assignment based on layer
 * - Dynamic reordering for bring-to-front
 * - Window tracking and cleanup
 * - Thread-safe operations
 */
object OverlayZIndexManager {
    private const val TAG = "OverlayZIndex"

    /** Base z-index offset (added to WindowManager.LayoutParams) */
    private const val BASE_Z_INDEX = 1000

    /** Registered overlay windows */
    private val windows = ConcurrentHashMap<String, OverlayWindowInfo>()

    /** Counter for unique z-index within layers */
    private val layerCounters = ConcurrentHashMap<OverlayLayer, AtomicInteger>()

    init {
        // Initialize layer counters
        OverlayLayer.values().forEach { layer ->
            layerCounters[layer] = AtomicInteger(0)
        }
    }

    /**
     * Register an overlay window and get its z-index
     *
     * @param id Unique identifier for this window
     * @param layer Which layer this window belongs to
     * @return Assigned z-index value
     */
    fun register(id: String, layer: OverlayLayer = OverlayLayer.CONTENT): Int {
        val counter = layerCounters[layer] ?: AtomicInteger(0)
        val layerOffset = counter.incrementAndGet()
        val zIndex = BASE_Z_INDEX + layer.baseOffset + layerOffset

        val info = OverlayWindowInfo(
            id = id,
            layer = layer,
            zIndex = zIndex
        )

        windows[id] = info
        Timber.d("$TAG: Registered '$id' at z-index $zIndex (layer: ${layer.name})")

        return zIndex
    }

    /**
     * Bring a window to front within its layer
     *
     * @param id Window identifier
     * @return New z-index, or null if window not found
     */
    fun bringToFront(id: String): Int? {
        val existing = windows[id] ?: run {
            Timber.w("$TAG: Cannot bring '$id' to front - not registered")
            return null
        }

        val counter = layerCounters[existing.layer] ?: return null
        val newOffset = counter.incrementAndGet()
        val newZIndex = BASE_Z_INDEX + existing.layer.baseOffset + newOffset

        windows[id] = existing.copy(zIndex = newZIndex)
        Timber.d("$TAG: Brought '$id' to front: z-index $newZIndex")

        return newZIndex
    }

    /**
     * Move window to a different layer
     *
     * @param id Window identifier
     * @param newLayer Target layer
     * @return New z-index, or null if window not found
     */
    fun moveToLayer(id: String, newLayer: OverlayLayer): Int? {
        val existing = windows[id] ?: return null

        val counter = layerCounters[newLayer] ?: AtomicInteger(0)
        val newOffset = counter.incrementAndGet()
        val newZIndex = BASE_Z_INDEX + newLayer.baseOffset + newOffset

        windows[id] = existing.copy(layer = newLayer, zIndex = newZIndex)
        Timber.d("$TAG: Moved '$id' to layer ${newLayer.name}: z-index $newZIndex")

        return newZIndex
    }

    /**
     * Unregister an overlay window
     *
     * @param id Window identifier
     */
    fun unregister(id: String) {
        val removed = windows.remove(id)
        if (removed != null) {
            Timber.d("$TAG: Unregistered '$id'")
        }
    }

    /**
     * Get current z-index for a window
     *
     * @param id Window identifier
     * @return Current z-index, or null if not registered
     */
    fun getZIndex(id: String): Int? = windows[id]?.zIndex

    /**
     * Get all windows in a specific layer
     *
     * @param layer Target layer
     * @return List of window IDs in that layer, sorted by z-index
     */
    fun getWindowsInLayer(layer: OverlayLayer): List<String> {
        return windows.values
            .filter { it.layer == layer }
            .sortedBy { it.zIndex }
            .map { it.id }
    }

    /**
     * Get all registered windows
     *
     * @return Map of window IDs to their info
     */
    fun getAllWindows(): Map<String, OverlayWindowInfo> = windows.toMap()

    /**
     * Clear all registrations
     */
    fun clear() {
        windows.clear()
        layerCounters.values.forEach { it.set(0) }
        Timber.d("$TAG: Cleared all registrations")
    }

    /**
     * Apply z-index to WindowManager.LayoutParams
     *
     * Note: On Android 8+, TYPE_APPLICATION_OVERLAY doesn't support
     * custom z-ordering via LayoutParams. This method provides the
     * flags for proper overlay behavior instead.
     *
     * @param params Layout params to modify
     * @param id Window identifier
     */
    fun applyToLayoutParams(params: WindowManager.LayoutParams, id: String) {
        val info = windows[id] ?: return

        // Set overlay type (required for Android 8+)
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        // Flags for proper overlay behavior
        params.flags = params.flags or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        // For dialogs/alerts, don't allow touches to pass through
        if (info.layer >= OverlayLayer.DIALOG) {
            params.flags = params.flags and
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        }

        Timber.v("$TAG: Applied params for '$id' (layer: ${info.layer.name})")
    }
}
