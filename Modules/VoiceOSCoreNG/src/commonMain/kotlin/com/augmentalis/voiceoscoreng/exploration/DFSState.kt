/**
 * DFSState.kt - DFS exploration algorithm state container
 *
 * Contains all state needed for iterative DFS exploration.
 * Uses ArrayDeque instead of java.util.Stack for KMP compatibility.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

/**
 * DFS exploration state container.
 *
 * Holds all mutable state for the exploration algorithm including:
 * - Exploration stack (screens to process)
 * - Visited screens tracking
 * - Navigation path tracking (for loop prevention)
 * - Element click tracking
 *
 * Uses ArrayDeque for KMP compatibility (java.util.Stack is JVM-only).
 *
 * ## Stack Operations Mapping
 * - push → addLast
 * - pop → removeLast
 * - peek → lastOrNull
 * - isEmpty → isEmpty
 */
data class DFSState(
    /**
     * Stack of screens to explore (LIFO order)
     * Using ArrayDeque for KMP compatibility
     */
    val explorationStack: ArrayDeque<ExplorationFrame> = ArrayDeque(),

    /**
     * Set of visited screen hashes to prevent infinite loops
     */
    val visitedScreens: MutableSet<String> = mutableSetOf(),

    /**
     * Navigation paths with visit counts for loop prevention.
     * Key: "sourceHash->destHash", Value: visit count
     */
    val navigationPaths: MutableMap<String, Int> = mutableMapOf(),

    /**
     * Mapping from element key to destination screen hash.
     * Key: "screenHash:stableId", Value: destination screen hash
     */
    val elementToDestinationScreen: MutableMap<String, String> = mutableMapOf(),

    /**
     * Set of registered element UUIDs (to avoid duplicate registration)
     */
    val registeredElementUuids: MutableSet<String> = mutableSetOf(),

    /**
     * Set of clicked element stable IDs (to avoid re-clicking)
     */
    val clickedStableIds: MutableSet<String> = mutableSetOf()
) {
    // ═══════════════════════════════════════════════════════════════════
    // STACK OPERATIONS (ArrayDeque as Stack)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Push a frame onto the exploration stack
     */
    fun push(frame: ExplorationFrame) {
        explorationStack.addLast(frame)
    }

    /**
     * Pop a frame from the exploration stack
     * @return The popped frame
     * @throws NoSuchElementException if stack is empty
     */
    fun pop(): ExplorationFrame {
        return explorationStack.removeLast()
    }

    /**
     * Peek at the top frame without removing it
     * @return Top frame or null if empty
     */
    fun peek(): ExplorationFrame? {
        return explorationStack.lastOrNull()
    }

    /**
     * Check if exploration stack is empty
     */
    fun isEmpty(): Boolean = explorationStack.isEmpty()

    /**
     * Check if exploration stack is not empty
     */
    fun isNotEmpty(): Boolean = explorationStack.isNotEmpty()

    /**
     * Get current stack depth
     */
    fun stackDepth(): Int = explorationStack.size

    // ═══════════════════════════════════════════════════════════════════
    // NAVIGATION PATH TRACKING
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Record navigation from source screen to destination screen
     * @return Current visit count for this path
     */
    fun recordNavigation(sourceHash: String, destHash: String): Int {
        val key = "${sourceHash.take(8)}->${destHash.take(8)}"
        val count = navigationPaths.getOrDefault(key, 0) + 1
        navigationPaths[key] = count
        return count
    }

    /**
     * Get visit count for a navigation path
     */
    fun getPathVisitCount(sourceHash: String, destHash: String): Int {
        val key = "${sourceHash.take(8)}->${destHash.take(8)}"
        return navigationPaths.getOrDefault(key, 0)
    }

    // ═══════════════════════════════════════════════════════════════════
    // ELEMENT TRACKING
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Record element to destination screen mapping
     */
    fun recordElementDestination(screenHash: String, stableId: String, destHash: String) {
        val key = "$screenHash:$stableId"
        elementToDestinationScreen[key] = destHash
    }

    /**
     * Get destination screen for an element (if known)
     */
    fun getElementDestination(screenHash: String, stableId: String): String? {
        val key = "$screenHash:$stableId"
        return elementToDestinationScreen[key]
    }

    /**
     * Check if clicking this element would lead to an already-visited screen
     */
    fun wouldLeadToVisitedScreen(screenHash: String, stableId: String): Boolean {
        val dest = getElementDestination(screenHash, stableId)
        return dest != null && dest in visitedScreens
    }

    // ═══════════════════════════════════════════════════════════════════
    // RESET
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Clear all state for a new exploration session
     */
    fun clear() {
        explorationStack.clear()
        visitedScreens.clear()
        navigationPaths.clear()
        elementToDestinationScreen.clear()
        registeredElementUuids.clear()
        clickedStableIds.clear()
    }
}
