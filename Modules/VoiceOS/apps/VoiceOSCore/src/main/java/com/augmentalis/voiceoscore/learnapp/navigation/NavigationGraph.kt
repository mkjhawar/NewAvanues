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

package com.augmentalis.voiceoscore.learnapp.navigation

import com.augmentalis.voiceoscore.learnapp.models.NavigationEdge
import com.augmentalis.voiceoscore.learnapp.models.ScreenState

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
     * Get all screen hashes
     *
     * @return List of all screen hashes in the graph
     */
    fun getScreens(): List<String> {
        return nodes.keys.toList()
    }

    /**
     * Get elements for a screen (as GraphElement objects)
     *
     * @param screenHash Screen hash
     * @return List of graph elements
     */
    fun getElementsForScreen(screenHash: String): List<GraphElement> {
        val node = nodes[screenHash] ?: return emptyList()
        return node.elements.map { uuid ->
            GraphElement(
                uuid = uuid,
                alias = null,  // Would need to look up from database
                type = "unknown",
                text = null,
                contentDescription = null,
                isClickable = true
            )
        }
    }

    /**
     * Get transitions from a screen
     *
     * @param screenHash Screen hash
     * @return List of graph transitions
     */
    fun getTransitionsFrom(screenHash: String): List<GraphTransition> {
        return getOutgoingEdges(screenHash).map { edge ->
            GraphTransition(
                fromScreen = edge.fromScreenHash,
                toScreen = edge.toScreenHash,
                elementUuid = edge.clickedElementUuid
            )
        }
    }

    /**
     * Get activity name for screen
     *
     * @param screenHash Screen hash
     * @return Activity name or null if not available
     */
    fun getActivityName(screenHash: String): String? {
        return nodes[screenHash]?.activityName
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
 * Navigation Edge
 *
 * Represents a directed edge in the navigation graph.
 *
 * @property fromScreenHash Source screen hash
 * @property clickedElementUuid UUID of element that was clicked
 * @property toScreenHash Destination screen hash
 * @property timestamp When transition was recorded
 */
data class NavigationEdge(
    val fromScreenHash: String,
    val clickedElementUuid: String,
    val toScreenHash: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun toString(): String = "$fromScreenHash --[$clickedElementUuid]--> $toScreenHash"
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

/**
 * Graph Element (simplified element info for graph)
 *
 * Lightweight element representation for AI context serialization.
 *
 * @property uuid Element UUID
 * @property alias User-assigned alias
 * @property type Element type (button, textfield, etc.)
 * @property text Visible text
 * @property contentDescription Content description for accessibility
 * @property isClickable Whether element is clickable
 */
data class GraphElement(
    val uuid: String?,
    val alias: String?,
    val type: String,
    val text: String?,
    val contentDescription: String?,
    val isClickable: Boolean
)

/**
 * Graph Transition (simplified transition info for graph)
 *
 * Represents a navigation transition between screens.
 *
 * @property fromScreen Source screen hash
 * @property toScreen Destination screen hash
 * @property elementUuid UUID of element that triggered transition
 */
data class GraphTransition(
    val fromScreen: String,
    val toScreen: String,
    val elementUuid: String
)
