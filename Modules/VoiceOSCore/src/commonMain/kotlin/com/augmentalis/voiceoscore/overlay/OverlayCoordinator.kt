/**
 * OverlayCoordinator.kt - Overlay display management and coordination
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 *
 * Manages overlay show/hide/update operations and coordinates between multiple overlay types.
 * Ported from VoiceOSCore OverlayCoordinator with KMP multiplatform compatibility.
 *
 * Key responsibilities:
 * - Overlay registration and lifecycle management
 * - Priority-based overlay coordination (STATUS > CONFIDENCE > NUMBERED > MENU > NORMAL)
 * - Mutual exclusivity enforcement via groups
 * - Z-order management for stacked overlays
 * - Conflict resolution when overlays compete for visibility
 * - Transition animation coordination (via callbacks)
 *
 * @see IOverlay for the overlay contract
 * @see OverlayPriority for priority levels
 */
package com.augmentalis.voiceoscore

import kotlin.concurrent.Volatile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ═══════════════════════════════════════════════════════════════════════════
// Overlay Priority
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Priority levels for overlay display ordering and conflict resolution.
 *
 * Higher priority overlays (lower ordinal) are:
 * - Displayed on top (higher z-order)
 * - Can hide lower priority overlays in the same group
 *
 * Priority order (highest to lowest):
 * 1. STATUS - Command status feedback (listening, success, error)
 * 2. CONFIDENCE - Speech recognition confidence indicators
 * 3. NUMBERED - Numbered element selection overlays
 * 4. MENU - Context menus and action sheets
 * 5. NORMAL - Default priority for generic overlays
 */
enum class OverlayPriority {
    /**
     * Highest priority - Command status overlays.
     * Always visible on top for immediate user feedback.
     */
    STATUS,

    /**
     * High priority - Confidence indicators.
     * Shows speech recognition quality feedback.
     */
    CONFIDENCE,

    /**
     * Medium priority - Numbered selection overlays.
     * Used for disambiguating multiple matched elements.
     */
    NUMBERED,

    /**
     * Low-medium priority - Context menus.
     * Interactive menus for user selection.
     */
    MENU,

    /**
     * Lowest priority - Default for generic overlays.
     * Used when no specific priority is needed.
     */
    NORMAL
}

// ═══════════════════════════════════════════════════════════════════════════
// Overlay Registration Entry
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Internal registration entry storing overlay metadata.
 */
private data class OverlayEntry(
    val overlay: IOverlay,
    val priority: OverlayPriority,
    val group: String?
)

// ═══════════════════════════════════════════════════════════════════════════
// Overlay Coordinator
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Coordinates overlay display across different overlay types.
 *
 * Manages:
 * - Overlay registration and lookup
 * - Priority-based visibility ordering
 * - Mutual exclusivity within groups
 * - Show/hide coordination with conflict resolution
 * - Single-focus mode for reduced cognitive load
 *
 * ## Usage
 *
 * ```kotlin
 * val coordinator = OverlayCoordinator()
 *
 * // Enable single-focus mode for reduced cognitive load
 * coordinator.singleFocusMode = true
 *
 * // Register overlays with priority and optional group
 * coordinator.register(statusOverlay, OverlayPriority.STATUS, group = "main")
 * coordinator.register(confidenceOverlay, OverlayPriority.CONFIDENCE, group = "main")
 * coordinator.register(numberedOverlay, OverlayPriority.NUMBERED, group = "selection")
 * coordinator.register(menuOverlay, OverlayPriority.MENU)
 *
 * // Show overlays - coordinator handles conflicts
 * coordinator.show("status-overlay")
 *
 * // Show with data
 * coordinator.showWithData("confidence", OverlayData.Confidence(0.9f, "click"))
 *
 * // Hide specific overlay
 * coordinator.hide("numbered")
 *
 * // Hide all overlays
 * coordinator.hideAll()
 *
 * // Cleanup
 * coordinator.dispose()
 * ```
 *
 * ## Mutual Exclusivity
 *
 * Overlays in the same group are mutually exclusive - only one can be visible at a time.
 * When showing an overlay, other overlays in the same group are automatically hidden.
 *
 * ## Single-Focus Mode
 *
 * When [singleFocusMode] is enabled, only ONE overlay can be visible at a time across
 * the entire system. Showing any overlay will hide ALL other visible overlays.
 * This reduces cognitive load by ensuring the user focuses on one thing at a time.
 *
 * ## Priority
 *
 * Higher priority overlays are always shown on top (lower z-order index).
 * Within a group, showing a higher priority overlay hides lower priority ones.
 *
 * @see IOverlay for the overlay interface
 * @see OverlayPriority for priority levels
 */
class OverlayCoordinator {

    // ═══════════════════════════════════════════════════════════════════════
    // Internal State
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Registered overlays indexed by ID.
     * Synchronized access required for thread safety.
     */
    private val overlays = mutableMapOf<String, OverlayEntry>()
    private val overlaysLock = Any()

    /**
     * Flag indicating if coordinator has been disposed.
     */
    @kotlin.concurrent.Volatile
    private var isDisposed = false

    /**
     * Internal state flow for visible overlays.
     */
    private val _visibleOverlaysFlow = MutableStateFlow<List<IOverlay>>(emptyList())

    /**
     * Single-focus mode for reduced cognitive load.
     *
     * When enabled, only ONE overlay can be visible at any time.
     * Showing any overlay will automatically hide ALL other visible overlays.
     *
     * This is recommended for accessibility and to reduce visual clutter.
     *
     * Default: false (allows multiple overlays)
     */
    var singleFocusMode: Boolean = false

    // ═══════════════════════════════════════════════════════════════════════
    // Public Properties
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Number of registered overlays.
     */
    val registeredCount: Int
        get() = overlays.size

    /**
     * Number of currently visible overlays.
     */
    val visibleCount: Int
        get() = overlays.values.count { it.overlay.isVisible }

    /**
     * Whether any overlay is currently visible.
     */
    val isAnyVisible: Boolean
        get() = overlays.values.any { it.overlay.isVisible }

    /**
     * Flow of currently visible overlays, ordered by priority.
     *
     * Emits updates when overlay visibility changes.
     * Use this to observe overlay state changes reactively.
     */
    val visibleOverlaysFlow: StateFlow<List<IOverlay>> = _visibleOverlaysFlow.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // Callbacks
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Callback invoked when an overlay is shown.
     *
     * @param id The overlay ID that was shown
     */
    var onOverlayShown: ((String) -> Unit)? = null

    /**
     * Callback invoked when an overlay is hidden.
     *
     * @param id The overlay ID that was hidden
     */
    var onOverlayHidden: ((String) -> Unit)? = null

    /**
     * Callback invoked when a conflict is resolved by hiding an overlay.
     *
     * @param hiddenId The overlay ID that was hidden due to conflict
     * @param shownId The overlay ID that caused the conflict
     */
    var onConflictResolved: ((hiddenId: String, shownId: String) -> Unit)? = null

    // ═══════════════════════════════════════════════════════════════════════
    // Registration
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Register an overlay with the coordinator.
     *
     * Overlays must be registered before they can be shown via the coordinator.
     * If an overlay with the same ID is already registered, it is replaced.
     *
     * @param overlay The overlay to register
     * @param priority Priority level for z-ordering and conflict resolution
     * @param group Optional group for mutual exclusivity. Overlays in the same
     *              group are mutually exclusive - only one can be visible at a time.
     */
    fun register(
        overlay: IOverlay,
        priority: OverlayPriority = OverlayPriority.NORMAL,
        group: String? = null
    ) {
        if (isDisposed) return

        // If overlay with same ID exists, unregister it first
        overlays[overlay.id]?.let {
            unregister(overlay.id)
        }

        overlays[overlay.id] = OverlayEntry(overlay, priority, group)
    }

    /**
     * Unregister an overlay from the coordinator.
     *
     * Hides the overlay if visible and removes it from management.
     *
     * @param id The overlay ID to unregister
     * @return true if the overlay was found and unregistered, false otherwise
     */
    fun unregister(id: String): Boolean {
        if (isDisposed) return false

        val entry = overlays.remove(id) ?: return false

        // Hide if visible before removing
        if (entry.overlay.isVisible) {
            entry.overlay.hide()
            updateVisibleOverlaysFlow()
        }

        return true
    }

    /**
     * Get a registered overlay by ID.
     *
     * @param id The overlay ID
     * @return The overlay if registered, null otherwise
     */
    fun getOverlay(id: String): IOverlay? {
        return overlays[id]?.overlay
    }

    /**
     * Get the priority of a registered overlay.
     *
     * @param id The overlay ID
     * @return The priority if registered, null otherwise
     */
    fun getPriority(id: String): OverlayPriority? {
        return overlays[id]?.priority
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Show/Hide Operations
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show an overlay by ID.
     *
     * Behavior depends on mode:
     * - Normal mode: If the overlay is in a group, other visible overlays in the
     *   same group are automatically hidden (mutual exclusivity).
     * - Single-focus mode: ALL other visible overlays are hidden before showing
     *   the requested overlay.
     *
     * @param id The overlay ID to show
     * @return true if the overlay was shown, false if not found or already visible
     */
    fun show(id: String): Boolean {
        if (isDisposed) return false

        val entry = overlays[id] ?: return false

        // Already visible - no action needed
        if (entry.overlay.isVisible) return true

        // Single-focus mode: hide ALL other overlays
        if (singleFocusMode) {
            hideAllExcept(id)
        } else {
            // Normal mode: hide conflicting overlays in the same group
            entry.group?.let { group ->
                hideConflictingOverlays(id, group)
            }
        }

        // Show the overlay
        entry.overlay.show()

        // Update state and notify
        updateVisibleOverlaysFlow()
        onOverlayShown?.invoke(id)

        return true
    }

    /**
     * Hide all overlays except the specified one.
     * Used by single-focus mode.
     */
    private fun hideAllExcept(exceptId: String) {
        overlays.entries
            .filter { (id, entry) ->
                id != exceptId && entry.overlay.isVisible
            }
            .forEach { (id, entry) ->
                entry.overlay.hide()
                onConflictResolved?.invoke(id, exceptId)
                onOverlayHidden?.invoke(id)
            }
    }

    /**
     * Hide conflicting overlays in the same group.
     */
    private fun hideConflictingOverlays(showingId: String, group: String) {
        overlays.entries
            .filter { (id, entry) ->
                id != showingId &&
                entry.group == group &&
                entry.overlay.isVisible
            }
            .forEach { (id, entry) ->
                entry.overlay.hide()
                onConflictResolved?.invoke(id, showingId)
                onOverlayHidden?.invoke(id)
            }
    }

    /**
     * Hide an overlay by ID.
     *
     * @param id The overlay ID to hide
     * @return true if the overlay was hidden, false if not found or already hidden
     */
    fun hide(id: String): Boolean {
        if (isDisposed) return false

        val entry = overlays[id] ?: return false

        // Already hidden - no action needed
        if (!entry.overlay.isVisible) return true

        entry.overlay.hide()

        // Update state and notify
        updateVisibleOverlaysFlow()
        onOverlayHidden?.invoke(id)

        return true
    }

    /**
     * Hide all visible overlays.
     */
    fun hideAll() {
        if (isDisposed) return

        overlays.values
            .filter { it.overlay.isVisible }
            .forEach { entry ->
                entry.overlay.hide()
                onOverlayHidden?.invoke(entry.overlay.id)
            }

        updateVisibleOverlaysFlow()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Update Operations
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update an overlay with new data.
     *
     * @param id The overlay ID to update
     * @param data The data to pass to the overlay
     * @return true if the overlay was updated, false if not found
     */
    fun update(id: String, data: OverlayData): Boolean {
        if (isDisposed) return false

        val entry = overlays[id] ?: return false
        entry.overlay.update(data)
        return true
    }

    /**
     * Show an overlay and update it with data in one operation.
     *
     * Equivalent to calling update() then show(), but more convenient
     * and ensures the data is set before the overlay becomes visible.
     *
     * @param id The overlay ID
     * @param data The data to display
     * @return true if successful, false if overlay not found
     */
    fun showWithData(id: String, data: OverlayData): Boolean {
        if (isDisposed) return false

        val entry = overlays[id] ?: return false

        // Update data first, then show
        entry.overlay.update(data)
        return show(id)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Z-Order and Query Operations
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get all visible overlays ordered by priority (highest first).
     *
     * The returned list represents z-order: index 0 is the topmost overlay.
     *
     * @return List of visible overlays in priority order
     */
    fun getVisibleOverlaysOrdered(): List<IOverlay> {
        return overlays.values
            .filter { it.overlay.isVisible }
            .sortedBy { it.priority.ordinal }
            .map { it.overlay }
    }

    /**
     * Get the z-order index of a visible overlay.
     *
     * @param id The overlay ID
     * @return The z-order index (0 = top), or -1 if not visible
     */
    fun getZOrder(id: String): Int {
        val visible = getVisibleOverlaysOrdered()
        return visible.indexOfFirst { it.id == id }
    }

    /**
     * Get the topmost (highest priority) visible overlay.
     *
     * @return The top overlay, or null if none visible
     */
    fun getTopOverlay(): IOverlay? {
        return getVisibleOverlaysOrdered().firstOrNull()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // State Flow Updates
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update the visible overlays flow with current state.
     */
    private fun updateVisibleOverlaysFlow() {
        _visibleOverlaysFlow.update {
            getVisibleOverlaysOrdered()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dispose of the coordinator and all registered overlays.
     *
     * After calling dispose:
     * - All overlays are hidden and disposed
     * - All registrations are cleared
     * - Further operations have no effect
     *
     * This method is idempotent.
     */
    fun dispose() {
        if (isDisposed) return

        isDisposed = true

        // Dispose all registered overlays
        overlays.values.forEach { entry ->
            try {
                entry.overlay.dispose()
            } catch (_: Exception) {
                // Ignore disposal errors
            }
        }

        // Clear all state
        overlays.clear()
        _visibleOverlaysFlow.update { emptyList() }

        // Clear callbacks
        onOverlayShown = null
        onOverlayHidden = null
        onConflictResolved = null
    }
}
