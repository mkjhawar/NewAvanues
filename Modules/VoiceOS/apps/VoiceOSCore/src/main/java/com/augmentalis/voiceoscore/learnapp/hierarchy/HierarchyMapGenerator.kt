/*
 * Copyright (c) 2025 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * All rights reserved.
 *
 * VoiceOS - Voice-First Accessibility Platform
 * HierarchyMapGenerator - Generates visual hierarchy maps from learned app data
 */

package com.augmentalis.voiceoscore.learnapp.hierarchy

import com.augmentalis.database.VoiceOSDatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates hierarchical representations of learned app structures.
 * Supports ASCII tree, JSON, and Mermaid diagram output formats.
 */
class HierarchyMapGenerator(
    private val databaseManager: VoiceOSDatabaseManager
) {
    companion object {
        private const val MAX_LABEL_LENGTH = 30
        private const val TREE_BRANCH = "+-- "
        private const val TREE_PIPE = "|   "
        private const val TREE_LAST = "\\-- "
        private const val TREE_SPACE = "    "
    }

    /**
     * Generates the complete hierarchy for an app from database data.
     */
    suspend fun generateHierarchy(packageName: String): AppHierarchy = withContext(Dispatchers.IO) {
        val screenContexts = databaseManager.screenContexts.getByPackage(packageName)

        if (screenContexts.isEmpty()) {
            return@withContext AppHierarchy.empty(packageName)
        }

        // Build nodes with their elements
        val nodes = screenContexts.map { screen ->
            val elements = databaseManager.scrapedElements.getByScreenHash(packageName, screen.screenHash)
            HierarchyNode(
                screenHash = screen.screenHash,
                activityName = screen.activityName,
                windowTitle = screen.windowTitle,
                screenType = screen.screenType,
                navigationLevel = screen.navigationLevel.toInt(),
                elements = elements.map { element ->
                    HierarchyElement(
                        uuid = element.uuid,
                        elementHash = element.elementHash,
                        className = element.className,
                        label = element.text ?: element.contentDescription,
                        isClickable = element.isClickable == 1L,
                        isEditable = element.isEditable == 1L,
                        isScrollable = element.isScrollable == 1L,
                        semanticRole = element.semanticRole
                    )
                },
                visitCount = screen.visitCount.toInt()
            )
        }

        // Build edges from transitions
        val edges = mutableListOf<HierarchyEdge>()
        val edgeMap = mutableMapOf<String, HierarchyEdge>()

        for (node in nodes) {
            val transitions = databaseManager.screenTransitions.getFromScreen(node.screenHash)
            for (transition in transitions) {
                val edgeKey = "${transition.fromScreenHash}->${transition.toScreenHash}"
                val existingEdge = edgeMap[edgeKey]
                if (existingEdge != null) {
                    edgeMap[edgeKey] = existingEdge.copy(
                        transitionCount = existingEdge.transitionCount + 1
                    )
                } else {
                    edgeMap[edgeKey] = HierarchyEdge(
                        fromScreenHash = transition.fromScreenHash,
                        toScreenHash = transition.toScreenHash,
                        triggerElementHash = transition.triggerElementHash,
                        triggerAction = transition.triggerAction,
                        transitionCount = transition.transitionCount.toInt()
                    )
                }
            }
        }
        edges.addAll(edgeMap.values)

        // Find root screen
        val rootScreenHash = findRootScreen(nodes, edges)

        // Calculate stats
        val totalElements = nodes.sumOf { it.elements.size }
        val clickableElements = nodes.sumOf { node ->
            node.elements.count { it.isClickable }
        }
        val maxDepth = calculateMaxDepth(nodes, edges, rootScreenHash)

        val stats = HierarchyStats(
            totalScreens = nodes.size,
            totalElements = totalElements,
            totalEdges = edges.size,
            maxDepth = maxDepth,
            clickableElements = clickableElements
        )

        // Note: appName resolution should be done by caller using PackageManager
        // ScrapedAppDTO doesn't store appName - only package metadata

        AppHierarchy(
            packageName = packageName,
            appName = null,  // Caller should resolve via PackageManager if needed
            nodes = nodes,
            edges = edges,
            rootScreenHash = rootScreenHash,
            stats = stats,
            generatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Generates an ASCII tree representation of the app hierarchy.
     */
    suspend fun generateAsciiTree(packageName: String): String {
        val hierarchy = generateHierarchy(packageName)
        return toAsciiTree(hierarchy)
    }

    /**
     * Generates a JSON representation of the app hierarchy.
     */
    suspend fun generateJson(packageName: String): String {
        val hierarchy = generateHierarchy(packageName)
        return toJson(hierarchy)
    }

    /**
     * Generates a Mermaid diagram representation of the app hierarchy.
     */
    suspend fun generateMermaid(packageName: String): String {
        val hierarchy = generateHierarchy(packageName)
        return toMermaid(hierarchy)
    }

    /**
     * Converts an AppHierarchy to ASCII tree format.
     */
    fun toAsciiTree(hierarchy: AppHierarchy): String {
        if (hierarchy.nodes.isEmpty()) {
            return "No screens found for ${hierarchy.packageName}"
        }

        val sb = StringBuilder()
        sb.appendLine("App Hierarchy: ${hierarchy.appName ?: hierarchy.packageName}")
        sb.appendLine("=".repeat(50))
        sb.appendLine("Stats: ${hierarchy.stats.totalScreens} screens, ${hierarchy.stats.totalElements} elements, ${hierarchy.stats.totalEdges} edges")
        sb.appendLine("Max Depth: ${hierarchy.stats.maxDepth}")
        sb.appendLine()

        // Build adjacency list for tree traversal
        val children = mutableMapOf<String, MutableList<String>>()
        for (edge in hierarchy.edges) {
            children.getOrPut(edge.fromScreenHash) { mutableListOf() }.add(edge.toScreenHash)
        }

        // Find root and traverse
        val rootHash = hierarchy.rootScreenHash ?: hierarchy.nodes.first().screenHash
        val visited = mutableSetOf<String>()

        fun printNode(screenHash: String, prefix: String, isLast: Boolean) {
            if (screenHash in visited) {
                sb.appendLine("$prefix${if (isLast) TREE_LAST else TREE_BRANCH}[Circular: ${screenHash.take(8)}...]")
                return
            }
            visited.add(screenHash)

            val node = hierarchy.nodes.find { it.screenHash == screenHash }
            if (node == null) {
                sb.appendLine("$prefix${if (isLast) TREE_LAST else TREE_BRANCH}[Unknown: ${screenHash.take(8)}...]")
                return
            }

            val label = node.windowTitle ?: node.activityName ?: node.screenHash.take(12)
            val truncatedLabel = truncateLabel(label)
            val clickables = node.elements.count { it.isClickable }

            sb.appendLine("$prefix${if (isLast) TREE_LAST else TREE_BRANCH}$truncatedLabel ($clickables clickables)")

            val childScreens = children[screenHash] ?: emptyList()
            val childPrefix = prefix + if (isLast) TREE_SPACE else TREE_PIPE

            childScreens.forEachIndexed { index, childHash ->
                printNode(childHash, childPrefix, index == childScreens.lastIndex)
            }
        }

        printNode(rootHash, "", true)

        // Print orphan nodes (not reachable from root)
        val orphans = hierarchy.nodes.filter { it.screenHash !in visited }
        if (orphans.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Orphan Screens:")
            orphans.forEach { node ->
                val label = node.windowTitle ?: node.activityName ?: node.screenHash.take(12)
                sb.appendLine("  - ${truncateLabel(label)}")
            }
        }

        return sb.toString()
    }

    /**
     * Converts an AppHierarchy to JSON format.
     */
    fun toJson(hierarchy: AppHierarchy): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"packageName\": ${escapeJson(hierarchy.packageName)},")
        sb.appendLine("  \"appName\": ${if (hierarchy.appName != null) escapeJson(hierarchy.appName) else "null"},")
        sb.appendLine("  \"generatedAt\": ${hierarchy.generatedAt},")
        sb.appendLine("  \"rootScreenHash\": ${if (hierarchy.rootScreenHash != null) escapeJson(hierarchy.rootScreenHash) else "null"},")

        // Stats
        sb.appendLine("  \"stats\": {")
        sb.appendLine("    \"totalScreens\": ${hierarchy.stats.totalScreens},")
        sb.appendLine("    \"totalElements\": ${hierarchy.stats.totalElements},")
        sb.appendLine("    \"totalEdges\": ${hierarchy.stats.totalEdges},")
        sb.appendLine("    \"maxDepth\": ${hierarchy.stats.maxDepth},")
        sb.appendLine("    \"clickableElements\": ${hierarchy.stats.clickableElements}")
        sb.appendLine("  },")

        // Nodes
        sb.appendLine("  \"nodes\": [")
        hierarchy.nodes.forEachIndexed { nodeIndex, node ->
            sb.appendLine("    {")
            sb.appendLine("      \"screenHash\": ${escapeJson(node.screenHash)},")
            sb.appendLine("      \"activityName\": ${if (node.activityName != null) escapeJson(node.activityName) else "null"},")
            sb.appendLine("      \"windowTitle\": ${if (node.windowTitle != null) escapeJson(node.windowTitle) else "null"},")
            sb.appendLine("      \"screenType\": ${if (node.screenType != null) escapeJson(node.screenType) else "null"},")
            sb.appendLine("      \"navigationLevel\": ${node.navigationLevel},")
            sb.appendLine("      \"visitCount\": ${node.visitCount},")
            sb.appendLine("      \"elementCount\": ${node.elements.size},")
            sb.appendLine("      \"clickableCount\": ${node.elements.count { it.isClickable }}")
            sb.append("    }")
            if (nodeIndex < hierarchy.nodes.lastIndex) sb.appendLine(",") else sb.appendLine()
        }
        sb.appendLine("  ],")

        // Edges
        sb.appendLine("  \"edges\": [")
        hierarchy.edges.forEachIndexed { edgeIndex, edge ->
            sb.appendLine("    {")
            sb.appendLine("      \"fromScreenHash\": ${escapeJson(edge.fromScreenHash)},")
            sb.appendLine("      \"toScreenHash\": ${escapeJson(edge.toScreenHash)},")
            sb.appendLine("      \"triggerElementHash\": ${if (edge.triggerElementHash != null) escapeJson(edge.triggerElementHash) else "null"},")
            sb.appendLine("      \"triggerAction\": ${escapeJson(edge.triggerAction)},")
            sb.appendLine("      \"transitionCount\": ${edge.transitionCount}")
            sb.append("    }")
            if (edgeIndex < hierarchy.edges.lastIndex) sb.appendLine(",") else sb.appendLine()
        }
        sb.appendLine("  ]")

        sb.appendLine("}")
        return sb.toString()
    }

    /**
     * Converts an AppHierarchy to Mermaid diagram format.
     */
    fun toMermaid(hierarchy: AppHierarchy): String {
        if (hierarchy.nodes.isEmpty()) {
            return "graph TD\n  empty[No screens found]"
        }

        val sb = StringBuilder()
        sb.appendLine("graph TD")
        sb.appendLine("  %% App: ${hierarchy.appName ?: hierarchy.packageName}")
        sb.appendLine("  %% Screens: ${hierarchy.stats.totalScreens}, Elements: ${hierarchy.stats.totalElements}")
        sb.appendLine()

        // Generate node IDs and labels
        val nodeIds = mutableMapOf<String, String>()
        hierarchy.nodes.forEachIndexed { index, node ->
            val nodeId = "S${index}"
            nodeIds[node.screenHash] = nodeId
            val label = node.windowTitle ?: node.activityName?.substringAfterLast('.') ?: "Screen ${index + 1}"
            val truncatedLabel = truncateLabel(label)
            val clickables = node.elements.count { it.isClickable }
            sb.appendLine("  $nodeId[\"$truncatedLabel<br/>($clickables actions)\"]")
        }

        sb.appendLine()

        // Generate edges
        hierarchy.edges.forEach { edge ->
            val fromId = nodeIds[edge.fromScreenHash]
            val toId = nodeIds[edge.toScreenHash]
            if (fromId != null && toId != null) {
                val action = edge.triggerAction.lowercase()
                val label = when {
                    edge.transitionCount > 1 -> "$action (${edge.transitionCount}x)"
                    else -> action
                }
                sb.appendLine("  $fromId -->|\"$label\"| $toId")
            }
        }

        // Style the root node
        val rootId = hierarchy.rootScreenHash?.let { nodeIds[it] }
        if (rootId != null) {
            sb.appendLine()
            sb.appendLine("  style $rootId fill:#90EE90,stroke:#006400")
        }

        return sb.toString()
    }

    /**
     * Finds the root screen (no incoming edges or lowest navigation level).
     */
    private fun findRootScreen(nodes: List<HierarchyNode>, edges: List<HierarchyEdge>): String? {
        if (nodes.isEmpty()) return null

        // Find screens with no incoming edges
        val incomingEdges = edges.map { it.toScreenHash }.toSet()
        val noIncoming = nodes.filter { it.screenHash !in incomingEdges }

        if (noIncoming.isNotEmpty()) {
            // Return the one with lowest navigation level
            return noIncoming.minByOrNull { it.navigationLevel }?.screenHash
        }

        // Fallback: return screen with lowest navigation level
        return nodes.minByOrNull { it.navigationLevel }?.screenHash
    }

    /**
     * Calculates the maximum depth using DFS from the root.
     */
    private fun calculateMaxDepth(
        nodes: List<HierarchyNode>,
        edges: List<HierarchyEdge>,
        rootScreenHash: String?
    ): Int {
        if (rootScreenHash == null || nodes.isEmpty()) return 0

        val children = mutableMapOf<String, List<String>>()
        for (edge in edges) {
            children[edge.fromScreenHash] =
                (children[edge.fromScreenHash] ?: emptyList()) + edge.toScreenHash
        }

        val visited = mutableSetOf<String>()

        fun dfs(screenHash: String, depth: Int): Int {
            if (screenHash in visited) return depth
            visited.add(screenHash)

            val childScreens = children[screenHash] ?: emptyList()
            if (childScreens.isEmpty()) return depth

            return childScreens.maxOfOrNull { dfs(it, depth + 1) } ?: depth
        }

        return dfs(rootScreenHash, 1)
    }

    /**
     * Truncates a label to the maximum length.
     */
    private fun truncateLabel(label: String): String {
        return if (label.length > MAX_LABEL_LENGTH) {
            label.take(MAX_LABEL_LENGTH - 3) + "..."
        } else {
            label
        }
    }

    /**
     * Escapes a string for JSON output.
     */
    private fun escapeJson(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
