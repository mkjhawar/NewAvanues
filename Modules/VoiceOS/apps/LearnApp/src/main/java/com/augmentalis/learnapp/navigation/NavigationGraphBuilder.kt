/**
 * NavigationGraphBuilder.kt - Builds navigation graph during exploration
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/navigation/NavigationGraphBuilder.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Builder for incrementally constructing navigation graph
 */

package com.augmentalis.learnapp.navigation

import com.augmentalis.learnapp.models.NavigationEdge
import com.augmentalis.learnapp.models.ScreenState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Navigation Graph Builder
 *
 * Incrementally builds navigation graph during exploration.
 * Thread-safe for concurrent access.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val builder = NavigationGraphBuilder("com.instagram.android")
 *
 * // Add screen
 * builder.addScreen(screenState, listOf("uuid1", "uuid2"))
 *
 * // Add edge
 * builder.addEdge(fromHash, clickedUuid, toHash)
 *
 * // Build final graph
 * val graph = builder.build()
 * ```
 *
 * @property packageName Package name of app
 *
 * @since 1.0.0
 */
class NavigationGraphBuilder(
    private val packageName: String
) {

    /**
     * Map of screen hash → ScreenNode
     */
    private val nodes = mutableMapOf<String, ScreenNode>()

    /**
     * List of navigation edges
     */
    private val edges = mutableListOf<NavigationEdge>()

    /**
     * Mutex for thread safety
     */
    private val mutex = Mutex()

    /**
     * Add screen to graph
     *
     * @param screenState Screen state
     * @param elementUuids List of element UUIDs on screen
     */
    suspend fun addScreen(screenState: ScreenState, elementUuids: List<String>) = mutex.withLock {
        val node = ScreenNode.fromScreenState(screenState, elementUuids)
        nodes[screenState.hash] = node
    }

    /**
     * Add screen node directly
     *
     * @param screenHash Screen hash
     * @param activityName Activity name
     * @param elementUuids List of element UUIDs
     */
    suspend fun addScreen(
        screenHash: String,
        activityName: String? = null,
        elementUuids: List<String> = emptyList()
    ) = mutex.withLock {
        nodes[screenHash] = ScreenNode(
            screenHash = screenHash,
            activityName = activityName,
            elements = elementUuids,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Add navigation edge
     *
     * @param fromScreenHash Source screen hash
     * @param clickedElementUuid Clicked element UUID
     * @param toScreenHash Destination screen hash
     */
    suspend fun addEdge(
        fromScreenHash: String,
        clickedElementUuid: String,
        toScreenHash: String
    ) = mutex.withLock {
        val edge = NavigationEdge(
            fromScreenHash = fromScreenHash,
            clickedElementUuid = clickedElementUuid,
            toScreenHash = toScreenHash,
            timestamp = System.currentTimeMillis()
        )

        edges.add(edge)
    }

    /**
     * Build final navigation graph
     *
     * Thread-safe: acquires mutex before reading collections.
     *
     * @return Immutable navigation graph
     */
    suspend fun build(): NavigationGraph = mutex.withLock {
        NavigationGraph(
            packageName = packageName,
            nodes = nodes.toMap(),
            edges = edges.toList()
        )
    }

    /**
     * Get current node count
     *
     * Thread-safe: acquires mutex before reading.
     *
     * @return Number of nodes
     */
    suspend fun getNodeCount(): Int = mutex.withLock {
        nodes.size
    }

    /**
     * Get current edge count
     *
     * Thread-safe: acquires mutex before reading.
     *
     * @return Number of edges
     */
    suspend fun getEdgeCount(): Int = mutex.withLock {
        edges.size
    }

    /**
     * Check if screen already added
     *
     * Thread-safe: acquires mutex before reading.
     *
     * @param screenHash Screen hash
     * @return true if screen exists
     */
    suspend fun hasScreen(screenHash: String): Boolean = mutex.withLock {
        nodes.containsKey(screenHash)
    }

    /**
     * Clear all data (for restarting exploration)
     */
    suspend fun clear() = mutex.withLock {
        nodes.clear()
        edges.clear()
    }
}
