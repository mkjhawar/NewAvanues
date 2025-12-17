/**
 * NavigationGraphBuilder.kt - Builds navigation graph during exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Builder for incrementally constructing navigation graph.
 * Migrated from LearnApp module.
 */
package com.augmentalis.voiceoscore.learnapp.navigation

import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Navigation Graph Builder
 *
 * Incrementally builds navigation graph during exploration.
 * Thread-safe for concurrent access.
 *
 * @property packageName Package name of app
 */
class NavigationGraphBuilder(
    private val packageName: String
) {

    /** Map of screen hash → ScreenNode */
    private val nodes = mutableMapOf<String, ScreenNode>()

    /** List of navigation edges */
    private val edges = mutableListOf<NavigationEdge>()

    /** Mutex for thread safety */
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
     * @return Immutable navigation graph
     */
    fun build(): NavigationGraph {
        return NavigationGraph(
            packageName = packageName,
            nodes = nodes.toMap(),
            edges = edges.toList()
        )
    }

    /** Get current node count */
    fun getNodeCount(): Int = nodes.size

    /** Get current edge count */
    fun getEdgeCount(): Int = edges.size

    /** Check if screen already added */
    fun hasScreen(screenHash: String): Boolean = nodes.containsKey(screenHash)

    /** Clear all data */
    suspend fun clear() = mutex.withLock {
        nodes.clear()
        edges.clear()
    }
}

/**
 * Navigation Graph
 *
 * Directed graph representing app's navigation structure.
 */
data class NavigationGraph(
    val packageName: String,
    val nodes: Map<String, ScreenNode>,
    val edges: List<NavigationEdge>
) {
    fun getOutgoingEdges(screenHash: String): List<NavigationEdge> =
        edges.filter { it.fromScreenHash == screenHash }

    fun getIncomingEdges(screenHash: String): List<NavigationEdge> =
        edges.filter { it.toScreenHash == screenHash }

    fun getReachableScreens(fromScreenHash: String): List<String> =
        getOutgoingEdges(fromScreenHash).map { it.toScreenHash }

    fun findPath(startHash: String, endHash: String): List<String> {
        if (startHash == endHash) return listOf(startHash)

        val visited = mutableSetOf<String>()
        val queue = mutableListOf(listOf(startHash))

        while (queue.isNotEmpty()) {
            val path = queue.removeAt(0)
            val currentHash = path.last()

            if (currentHash in visited) continue
            visited.add(currentHash)

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
        return emptyList()
    }

    fun getStats(): GraphStats {
        val totalElements = nodes.values.sumOf { it.elements.size }
        return GraphStats(
            totalScreens = nodes.size,
            totalElements = totalElements,
            totalEdges = edges.size,
            averageOutDegree = if (nodes.isEmpty()) 0f else edges.size.toFloat() / nodes.size.toFloat(),
            maxDepth = calculateMaxDepth()
        )
    }

    private fun calculateMaxDepth(): Int {
        if (nodes.isEmpty()) return 0
        val rootHash = nodes.keys.first()
        var maxDepth = 0
        val visited = mutableSetOf<String>()

        fun dfs(hash: String, depth: Int) {
            if (hash in visited) return
            visited.add(hash)
            maxDepth = maxOf(maxDepth, depth)
            getReachableScreens(hash).forEach { dfs(it, depth + 1) }
        }

        dfs(rootHash, 0)
        return maxDepth
    }
}

/**
 * Screen Node
 */
data class ScreenNode(
    val screenHash: String,
    val activityName: String? = null,
    val elements: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
) {
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
 */
data class GraphStats(
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val averageOutDegree: Float,
    val maxDepth: Int
)
