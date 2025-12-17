/**
 * NavigationGraph.kt - App navigation graph data structure
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/navigation/NavigationGraph.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Directed graph representing app navigation structure
 */

package com.augmentalis.learnapp.navigation

import com.augmentalis.learnapp.models.NavigationEdge
import com.augmentalis.learnapp.models.ScreenState

/**
 * Navigation Graph
 *
 * Directed graph representing app's navigation structure.
 * Nodes = screens, Edges = transitions via element clicks.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val graph = NavigationGraph(
 *     packageName = "com.instagram.android",
 *     nodes = mapOf(
 *         "abc123" to ScreenNode(...),
 *         "def456" to ScreenNode(...)
 *     ),
 *     edges = listOf(
 *         NavigationEdge("abc123", "btn-xyz", "def456")
 *     )
 * )
 *
 * // Query operations
 * val reachable = graph.getReachableScreens("abc123")
 * val path = graph.findPath("abc123", "def456")
 * ```
 *
 * @property packageName Package name of app
 * @property nodes Map of screen hash → ScreenNode
 * @property edges List of navigation edges
 *
 * @since 1.0.0
 */
data class NavigationGraph(
    val packageName: String,
    val nodes: Map<String, ScreenNode>,
    val edges: List<NavigationEdge>
) {

    /**
     * Get outgoing edges from screen
     *
     * @param screenHash Screen hash
     * @return List of outgoing edges
     */
    fun getOutgoingEdges(screenHash: String): List<NavigationEdge> {
        return edges.filter { it.fromScreenHash == screenHash }
    }

    /**
     * Get incoming edges to screen
     *
     * @param screenHash Screen hash
     * @return List of incoming edges
     */
    fun getIncomingEdges(screenHash: String): List<NavigationEdge> {
        return edges.filter { it.toScreenHash == screenHash }
    }

    /**
     * Get reachable screens from source
     *
     * @param fromScreenHash Source screen hash
     * @return List of reachable screen hashes
     */
    fun getReachableScreens(fromScreenHash: String): List<String> {
        return getOutgoingEdges(fromScreenHash).map { it.toScreenHash }
    }

    /**
     * Find path between two screens (BFS)
     *
     * @param startHash Start screen hash
     * @param endHash End screen hash
     * @return List of screen hashes forming path (empty if no path)
     */
    fun findPath(startHash: String, endHash: String): List<String> {
        if (startHash == endHash) return listOf(startHash)

        val visited = mutableSetOf<String>()
        val queue = mutableListOf(listOf(startHash))

        while (queue.isNotEmpty()) {
            val path = queue.removeAt(0)
            val currentHash = path.last()

            if (currentHash in visited) continue
            visited.add(currentHash)

            // Check neighbors
            val neighbors = getReachableScreens(currentHash)
            for (neighbor in neighbors) {
                if (neighbor == endHash) {
                    return path + neighbor
                }

                if (neighbor !in visited) {
                    queue.add(path + neighbor)
                }
            }
        }

        return emptyList()  // No path found
    }

    /**
     * Calculate graph statistics
     *
     * @return Graph stats
     */
    fun getStats(): GraphStats {
        // Count total elements across all screen nodes
        val totalElements = nodes.values.sumOf { it.elements.size }

        return GraphStats(
            totalScreens = nodes.size,
            totalElements = totalElements,
            totalEdges = edges.size,
            averageOutDegree = if (nodes.isEmpty()) 0f else {
                edges.size.toFloat() / nodes.size.toFloat()
            },
            maxDepth = calculateMaxDepth()
        )
    }

    /**
     * Calculate maximum depth in graph (longest path from root)
     *
     * @return Max depth
     */
    private fun calculateMaxDepth(): Int {
        if (nodes.isEmpty()) return 0

        // Assume first node is root (home screen)
        val rootHash = nodes.keys.first()

        var maxDepth = 0
        val visited = mutableSetOf<String>()

        fun dfs(hash: String, depth: Int) {
            if (hash in visited) return
            visited.add(hash)

            maxDepth = maxOf(maxDepth, depth)

            getReachableScreens(hash).forEach { neighbor ->
                dfs(neighbor, depth + 1)
            }
        }

        dfs(rootHash, 0)
        return maxDepth
    }
}

/**
 * Screen Node
 *
 * Node in navigation graph representing a unique screen.
 *
 * @property screenHash SHA-256 hash of screen
 * @property activityName Activity name (if available)
 * @property elements List of element UUIDs on this screen
 * @property timestamp When screen was discovered
 */
data class ScreenNode(
    val screenHash: String,
    val activityName: String? = null,
    val elements: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Create from ScreenState
     *
     * @param screenState Screen state
     * @param elementUuids List of element UUIDs
     * @return Screen node
     */
    companion object {
        fun fromScreenState(screenState: ScreenState, elementUuids: List<String>): ScreenNode {
            return ScreenNode(
                screenHash = screenState.hash,
                activityName = screenState.activityName,
                elements = elementUuids,
                timestamp = screenState.timestamp
            )
        }
    }
}

/**
 * Graph Statistics
 *
 * @property totalScreens Total nodes in graph
 * @property totalElements Total elements across all screens
 * @property totalEdges Total edges in graph
 * @property averageOutDegree Average outgoing edges per screen
 * @property maxDepth Maximum depth (longest path from root)
 */
data class GraphStats(
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val averageOutDegree: Float,
    val maxDepth: Int
) {
    override fun toString(): String {
        return """
            Navigation Graph Stats:
            - Screens: $totalScreens
            - Elements: $totalElements
            - Edges: $totalEdges
            - Avg Out-Degree: ${"%.1f".format(averageOutDegree)}
            - Max Depth: $maxDepth
        """.trimIndent()
    }
}
