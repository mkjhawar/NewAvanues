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

import com.augmentalis.voiceoscore.learnapp.models.NavigationEdge
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

