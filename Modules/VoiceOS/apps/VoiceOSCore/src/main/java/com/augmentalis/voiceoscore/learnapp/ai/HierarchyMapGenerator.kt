/**
 * HierarchyMapGenerator.kt - Visual hierarchy map generator from learned app data
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI-Assisted Implementation
 * Created: 2025-12-30
 *
 * Generates visual hierarchy maps from learned app data in multiple formats:
 * - ASCII tree (text-based, console-friendly)
 * - JSON (machine-readable, for API/storage)
 * - Mermaid (diagram format, for documentation)
 *
 * Part of Voice Command AI Integration feature
 */
package com.augmentalis.voiceoscore.learnapp.ai

import android.content.Context
import android.util.Log
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.dto.ScreenContextDTO
import com.augmentalis.database.dto.ScreenTransitionDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.IScreenContextRepository
import com.augmentalis.database.repositories.IScreenTransitionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Hierarchy Map Generator
 *
 * Generates visual hierarchy maps from learned app data.
 * Supports multiple export formats for different use cases.
 *
 * ## Usage:
 * ```kotlin
 * val generator = HierarchyMapGenerator(
 *     context = applicationContext,
 *     screenContextRepository = screenRepo,
 *     generatedCommandRepository = commandRepo,
 *     screenTransitionRepository = transitionRepo
 * )
 *
 * // Generate hierarchy
 * val hierarchy = generator.generateHierarchy("com.android.settings")
 *
 * // Export to different formats
 * val asciiTree = generator.toAsciiTree(hierarchy)
 * val json = generator.toJson(hierarchy)
 * val mermaid = generator.toMermaid(hierarchy)
 *
 * // Export to file
 * val file = generator.exportToFile(hierarchy, ExportFormat.MERMAID, outputDir)
 * ```
 *
 * @property context Application context for file operations
 * @property screenContextRepository Repository for screen context data
 * @property generatedCommandRepository Repository for generated commands
 * @property screenTransitionRepository Repository for screen transitions (navigation edges)
 */
class HierarchyMapGenerator(
    private val context: Context,
    private val screenContextRepository: IScreenContextRepository,
    private val generatedCommandRepository: IGeneratedCommandRepository,
    private val screenTransitionRepository: IScreenTransitionRepository
) {

    companion object {
        private const val TAG = "HierarchyMapGenerator"

        // ASCII tree characters
        private const val TREE_BRANCH = "|-- "
        private const val TREE_LAST_BRANCH = "`-- "
        private const val TREE_VERTICAL = "|   "
        private const val TREE_SPACE = "    "
        private const val TREE_ARROW = " -> "

        // Unicode tree characters (prettier but may not render on all terminals)
        private const val UNICODE_BRANCH = "\u251C\u2500\u2500 " // ├──
        private const val UNICODE_LAST_BRANCH = "\u2514\u2500\u2500 " // └──
        private const val UNICODE_VERTICAL = "\u2502   " // │
        private const val UNICODE_ARROW = " \u2192 " // →
    }

    /**
     * Generate hierarchy from learned app data
     *
     * Fetches screen contexts, commands, and transitions from repositories
     * and builds a complete hierarchical representation.
     *
     * @param packageName Package name of the app to generate hierarchy for
     * @return AppHierarchy containing all nodes, edges, and statistics
     * @throws IllegalArgumentException if packageName is empty
     * @throws NoSuchElementException if no data found for package
     */
    suspend fun generateHierarchy(packageName: String): AppHierarchy = withContext(Dispatchers.IO) {
        require(packageName.isNotBlank()) { "Package name cannot be empty" }

        Log.d(TAG, "Generating hierarchy for: $packageName")

        // Fetch data from repositories
        val screens = screenContextRepository.getByPackage(packageName)
        if (screens.isEmpty()) {
            Log.w(TAG, "No screens found for package: $packageName")
            throw NoSuchElementException("No learned data found for package: $packageName")
        }

        val commands = generatedCommandRepository.getByPackage(packageName)
        val transitions = fetchTransitionsForScreens(screens.map { it.screenHash })

        Log.d(TAG, "Fetched ${screens.size} screens, ${commands.size} commands, ${transitions.size} transitions")

        // Build navigation edge set for quick lookup
        val navigationElements = transitions.mapNotNull { it.triggerElementHash }.toSet()

        // Group commands by screen
        val commandsByElement = commands.groupBy { it.elementHash }

        // Calculate depths using BFS from root
        val depths = calculateScreenDepths(screens, transitions)

        // Build hierarchy nodes
        val nodes = screens.map { screen ->
            buildHierarchyNode(
                screen = screen,
                commandsByElement = commandsByElement,
                navigationElements = navigationElements,
                depth = depths[screen.screenHash] ?: 0
            )
        }.sortedBy { it.depth }

        // Build hierarchy edges
        val edges = transitions.map { transition ->
            val label = getTransitionLabel(transition, commandsByElement)
            HierarchyEdge(
                fromScreen = transition.fromScreenHash,
                toScreen = transition.toScreenHash,
                viaElement = transition.triggerElementHash ?: "unknown",
                label = label
            )
        }

        // Calculate statistics
        val stats = calculateStats(nodes, edges)

        // Get app name from first screen's context or use package name
        val appName = extractAppName(packageName, screens)

        AppHierarchy(
            packageName = packageName,
            appName = appName,
            nodes = nodes,
            edges = edges,
            stats = stats
        ).also {
            Log.d(TAG, "Generated hierarchy: ${it.stats}")
        }
    }

    /**
     * Convert hierarchy to ASCII tree format
     *
     * Produces a text-based tree representation suitable for console output.
     *
     * @param hierarchy The hierarchy to convert
     * @param useUnicode Use Unicode box-drawing characters (default: true)
     * @return ASCII tree string representation
     */
    fun toAsciiTree(hierarchy: AppHierarchy, useUnicode: Boolean = true): String {
        val sb = StringBuilder()
        val branch = if (useUnicode) UNICODE_BRANCH else TREE_BRANCH
        val lastBranch = if (useUnicode) UNICODE_LAST_BRANCH else TREE_LAST_BRANCH
        val vertical = if (useUnicode) UNICODE_VERTICAL else TREE_VERTICAL
        val arrow = if (useUnicode) UNICODE_ARROW else TREE_ARROW

        // Header
        sb.appendLine("${hierarchy.packageName} (${hierarchy.appName})")
        sb.appendLine()

        // Build adjacency list for tree traversal
        val adjacency = buildAdjacencyList(hierarchy.edges)
        val visited = mutableSetOf<String>()

        // Find root nodes (screens with no incoming edges)
        val incomingScreens = hierarchy.edges.map { it.toScreen }.toSet()
        val rootNodes = hierarchy.nodes.filter { it.screenHash !in incomingScreens }

        // If no root found, use first node or node with depth 0
        val roots = if (rootNodes.isNotEmpty()) {
            rootNodes
        } else {
            hierarchy.nodes.filter { it.depth == 0 }.takeIf { it.isNotEmpty() }
                ?: listOf(hierarchy.nodes.first())
        }

        // Render each root and its descendants
        roots.forEachIndexed { index, root ->
            val isLast = index == roots.lastIndex
            renderNode(
                sb = sb,
                node = root,
                hierarchy = hierarchy,
                adjacency = adjacency,
                visited = visited,
                prefix = "",
                isLast = isLast,
                branch = branch,
                lastBranch = lastBranch,
                vertical = vertical,
                arrow = arrow
            )
        }

        // Footer with stats
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine("Stats: ${hierarchy.stats.totalScreens} screens, ${hierarchy.stats.totalElements} elements, ${hierarchy.stats.totalEdges} edges")

        return sb.toString()
    }

    /**
     * Convert hierarchy to JSON format
     *
     * Produces a machine-readable JSON representation.
     *
     * @param hierarchy The hierarchy to convert
     * @param prettyPrint Enable pretty printing with indentation (default: true)
     * @return JSON string representation
     */
    fun toJson(hierarchy: AppHierarchy, prettyPrint: Boolean = true): String {
        val json = JSONObject().apply {
            put("packageName", hierarchy.packageName)
            put("appName", hierarchy.appName)
            put("generatedAt", hierarchy.generatedAt)
            put("generatedAtFormatted", formatTimestamp(hierarchy.generatedAt))

            // Nodes
            put("nodes", JSONArray().apply {
                hierarchy.nodes.forEach { node ->
                    put(JSONObject().apply {
                        put("screenHash", node.screenHash)
                        put("activityName", node.activityName ?: JSONObject.NULL)
                        put("displayName", node.displayName)
                        put("depth", node.depth)
                        put("elements", JSONArray().apply {
                            node.elements.forEach { element ->
                                put(JSONObject().apply {
                                    put("uuid", element.uuid)
                                    put("label", element.label)
                                    put("type", element.type)
                                    put("isNavigational", element.isNavigational)
                                })
                            }
                        })
                    })
                }
            })

            // Edges
            put("edges", JSONArray().apply {
                hierarchy.edges.forEach { edge ->
                    put(JSONObject().apply {
                        put("fromScreen", edge.fromScreen)
                        put("toScreen", edge.toScreen)
                        put("viaElement", edge.viaElement)
                        put("label", edge.label)
                    })
                }
            })

            // Stats
            put("stats", JSONObject().apply {
                put("totalScreens", hierarchy.stats.totalScreens)
                put("totalElements", hierarchy.stats.totalElements)
                put("totalEdges", hierarchy.stats.totalEdges)
                put("maxDepth", hierarchy.stats.maxDepth)
                put("avgElementsPerScreen", hierarchy.stats.avgElementsPerScreen)
            })
        }

        return if (prettyPrint) {
            json.toString(2)
        } else {
            json.toString()
        }
    }

    /**
     * Convert hierarchy to Mermaid diagram format
     *
     * Produces a Mermaid flowchart that can be rendered in documentation.
     *
     * @param hierarchy The hierarchy to convert
     * @param direction Diagram direction: TD (top-down), LR (left-right), etc.
     * @return Mermaid diagram string
     */
    fun toMermaid(hierarchy: AppHierarchy, direction: String = "TD"): String {
        val sb = StringBuilder()

        // Header with title
        sb.appendLine("---")
        sb.appendLine("title: ${hierarchy.appName} (${hierarchy.packageName})")
        sb.appendLine("---")
        sb.appendLine("graph $direction")

        // Create node ID mapping (screen hash to safe ID)
        val nodeIds = hierarchy.nodes.mapIndexed { index, node ->
            node.screenHash to "screen$index"
        }.toMap()

        // Define nodes with display names
        hierarchy.nodes.forEach { node ->
            val id = nodeIds[node.screenHash] ?: return@forEach
            val label = sanitizeMermaidText(node.displayName)
            val elementCount = node.elements.size
            sb.appendLine("    $id[$label<br/>$elementCount elements]")
        }

        sb.appendLine()

        // Define edges with labels
        hierarchy.edges.forEach { edge ->
            val fromId = nodeIds[edge.fromScreen] ?: return@forEach
            val toId = nodeIds[edge.toScreen] ?: return@forEach
            val label = sanitizeMermaidText(edge.label)

            if (label.isNotBlank()) {
                sb.appendLine("    $fromId -->|$label| $toId")
            } else {
                sb.appendLine("    $fromId --> $toId")
            }
        }

        // Add styling for different node types
        sb.appendLine()
        sb.appendLine("    %% Styling")

        // Root nodes (depth 0)
        val rootNodes = hierarchy.nodes.filter { it.depth == 0 }
        if (rootNodes.isNotEmpty()) {
            val rootIds = rootNodes.mapNotNull { nodeIds[it.screenHash] }.joinToString(",")
            sb.appendLine("    classDef root fill:#90EE90,stroke:#228B22")
            sb.appendLine("    class $rootIds root")
        }

        // Leaf nodes (no outgoing edges)
        val outgoingScreens = hierarchy.edges.map { it.fromScreen }.toSet()
        val leafNodes = hierarchy.nodes.filter { it.screenHash !in outgoingScreens && it.depth > 0 }
        if (leafNodes.isNotEmpty()) {
            val leafIds = leafNodes.mapNotNull { nodeIds[it.screenHash] }.joinToString(",")
            sb.appendLine("    classDef leaf fill:#FFB6C1,stroke:#DC143C")
            sb.appendLine("    class $leafIds leaf")
        }

        return sb.toString()
    }

    /**
     * Export hierarchy to file
     *
     * Saves the hierarchy in the specified format to the output directory.
     *
     * @param hierarchy The hierarchy to export
     * @param format Export format (ASCII, JSON, MERMAID)
     * @param outputDir Directory to save the file (defaults to app's files directory)
     * @return The created file
     */
    suspend fun exportToFile(
        hierarchy: AppHierarchy,
        format: ExportFormat,
        outputDir: File = context.filesDir
    ): File = withContext(Dispatchers.IO) {
        // Ensure output directory exists
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // Generate filename
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safePackageName = hierarchy.packageName.replace(".", "_")
        val extension = when (format) {
            ExportFormat.ASCII -> "txt"
            ExportFormat.JSON -> "json"
            ExportFormat.MERMAID -> "md"
        }
        val filename = "hierarchy_${safePackageName}_$timestamp.$extension"

        // Generate content
        val content = when (format) {
            ExportFormat.ASCII -> toAsciiTree(hierarchy)
            ExportFormat.JSON -> toJson(hierarchy)
            ExportFormat.MERMAID -> wrapMermaidInMarkdown(hierarchy)
        }

        // Write to file
        val file = File(outputDir, filename)
        file.writeText(content)

        Log.i(TAG, "Exported hierarchy to: ${file.absolutePath} (${file.length()} bytes)")
        file
    }

    // ==================== Private Helper Methods ====================

    /**
     * Fetch transitions for given screen hashes
     */
    private suspend fun fetchTransitionsForScreens(
        screenHashes: List<String>
    ): List<ScreenTransitionDTO> {
        val transitions = mutableListOf<ScreenTransitionDTO>()
        screenHashes.forEach { hash ->
            transitions.addAll(screenTransitionRepository.getFromScreen(hash))
        }
        return transitions.distinctBy { "${it.fromScreenHash}-${it.toScreenHash}" }
    }

    /**
     * Calculate screen depths using BFS
     */
    private fun calculateScreenDepths(
        screens: List<ScreenContextDTO>,
        transitions: List<ScreenTransitionDTO>
    ): Map<String, Int> {
        val depths = mutableMapOf<String, Int>()
        val adjacency = mutableMapOf<String, MutableList<String>>()

        // Build adjacency list
        transitions.forEach { transition ->
            adjacency.getOrPut(transition.fromScreenHash) { mutableListOf() }
                .add(transition.toScreenHash)
        }

        // Find root (screen with no incoming edges or first screen)
        val incomingScreens = transitions.map { it.toScreenHash }.toSet()
        val roots = screens.filter { it.screenHash !in incomingScreens }

        // BFS from roots
        val queue = ArrayDeque<Pair<String, Int>>()
        if (roots.isNotEmpty()) {
            roots.forEach { root ->
                queue.add(root.screenHash to 0)
                depths[root.screenHash] = 0
            }
        } else if (screens.isNotEmpty()) {
            queue.add(screens.first().screenHash to 0)
            depths[screens.first().screenHash] = 0
        }

        while (queue.isNotEmpty()) {
            val (current, depth) = queue.removeFirst()
            adjacency[current]?.forEach { neighbor ->
                if (neighbor !in depths) {
                    depths[neighbor] = depth + 1
                    queue.add(neighbor to depth + 1)
                }
            }
        }

        // Assign depth 0 to any unvisited screens
        screens.forEach { screen ->
            if (screen.screenHash !in depths) {
                depths[screen.screenHash] = 0
            }
        }

        return depths
    }

    /**
     * Build a hierarchy node from screen context
     */
    private fun buildHierarchyNode(
        screen: ScreenContextDTO,
        commandsByElement: Map<String, List<GeneratedCommandDTO>>,
        navigationElements: Set<String>,
        depth: Int
    ): HierarchyNode {
        // Get elements for this screen from commands
        val elements = commandsByElement.values.flatten()
            .filter { cmd ->
                // We don't have direct screen-to-element mapping in commands,
                // so we include all elements. In production, this would be
                // filtered by screen hash if the relationship exists.
                true
            }
            .distinctBy { it.elementHash }
            .take(50) // Limit elements per node for readability
            .map { cmd ->
                HierarchyElement(
                    uuid = cmd.elementHash,
                    label = cmd.commandText,
                    type = cmd.actionType,
                    isNavigational = cmd.elementHash in navigationElements
                )
            }

        // Generate display name from activity name or screen type
        val displayName = generateDisplayName(screen)

        return HierarchyNode(
            screenHash = screen.screenHash,
            activityName = screen.activityName,
            displayName = displayName,
            elements = elements,
            depth = depth
        )
    }

    /**
     * Generate a human-readable display name for a screen
     */
    private fun generateDisplayName(screen: ScreenContextDTO): String {
        // Priority: windowTitle > activityName (cleaned) > screenType > hash prefix
        return when {
            !screen.windowTitle.isNullOrBlank() -> screen.windowTitle
            !screen.activityName.isNullOrBlank() -> cleanActivityName(screen.activityName)
            !screen.screenType.isNullOrBlank() -> screen.screenType
            else -> "Screen ${screen.screenHash.take(8)}"
        }
    }

    /**
     * Clean activity name for display (remove package prefix, etc.)
     */
    private fun cleanActivityName(activityName: String): String {
        // Extract just the class name
        val className = activityName.substringAfterLast(".")

        // Convert CamelCase to Title Case with spaces
        return className
            .replace("Activity", "")
            .replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
            .trim()
            .ifBlank { className }
    }

    /**
     * Get transition label from command data
     */
    private fun getTransitionLabel(
        transition: ScreenTransitionDTO,
        commandsByElement: Map<String, List<GeneratedCommandDTO>>
    ): String {
        val elementHash = transition.triggerElementHash ?: return transition.triggerAction
        val commands = commandsByElement[elementHash]
        return commands?.firstOrNull()?.commandText ?: transition.triggerAction
    }

    /**
     * Build adjacency list from edges
     */
    private fun buildAdjacencyList(
        edges: List<HierarchyEdge>
    ): Map<String, List<Pair<String, String>>> {
        val adjacency = mutableMapOf<String, MutableList<Pair<String, String>>>()
        edges.forEach { edge ->
            adjacency.getOrPut(edge.fromScreen) { mutableListOf() }
                .add(edge.toScreen to edge.label)
        }
        return adjacency
    }

    /**
     * Render a node and its children in ASCII tree format
     */
    private fun renderNode(
        sb: StringBuilder,
        node: HierarchyNode,
        hierarchy: AppHierarchy,
        adjacency: Map<String, List<Pair<String, String>>>,
        visited: MutableSet<String>,
        prefix: String,
        isLast: Boolean,
        branch: String,
        lastBranch: String,
        vertical: String,
        arrow: String
    ) {
        if (node.screenHash in visited) {
            // Already visited - show reference
            val currentBranch = if (isLast) lastBranch else branch
            sb.appendLine("$prefix$currentBranch[${node.screenHash.take(8)}] (see above)")
            return
        }
        visited.add(node.screenHash)

        // Render current node
        val currentBranch = if (isLast) lastBranch else branch
        val nodeLabel = "[${node.screenHash.take(8)}] ${node.displayName} (${node.elements.size} elements)"
        sb.appendLine("$prefix$currentBranch$nodeLabel")

        // Prepare prefix for children
        val childPrefix = prefix + if (isLast) TREE_SPACE else vertical

        // Get outgoing edges for this node
        val children = adjacency[node.screenHash] ?: emptyList()

        // Render navigational elements with their destinations
        children.forEachIndexed { index, (toScreen, label) ->
            val isLastChild = index == children.lastIndex
            val childBranch = if (isLastChild) lastBranch else branch

            // Find the target node
            val targetNode = hierarchy.nodes.find { it.screenHash == toScreen }
            val targetLabel = targetNode?.displayName ?: toScreen.take(8)

            sb.appendLine("$childPrefix$childBranch$label$arrow[$targetLabel]")

            // Recursively render target if not visited
            if (targetNode != null && toScreen !in visited) {
                renderNode(
                    sb = sb,
                    node = targetNode,
                    hierarchy = hierarchy,
                    adjacency = adjacency,
                    visited = visited,
                    prefix = childPrefix + if (isLastChild) TREE_SPACE else vertical,
                    isLast = true,
                    branch = branch,
                    lastBranch = lastBranch,
                    vertical = vertical,
                    arrow = arrow
                )
            }
        }
    }

    /**
     * Calculate hierarchy statistics
     */
    private fun calculateStats(
        nodes: List<HierarchyNode>,
        edges: List<HierarchyEdge>
    ): HierarchyStats {
        val totalElements = nodes.sumOf { it.elements.size }
        val maxDepth = nodes.maxOfOrNull { it.depth } ?: 0
        val avgElements = if (nodes.isNotEmpty()) {
            totalElements.toFloat() / nodes.size
        } else {
            0f
        }

        return HierarchyStats(
            totalScreens = nodes.size,
            totalElements = totalElements,
            totalEdges = edges.size,
            maxDepth = maxDepth,
            avgElementsPerScreen = avgElements
        )
    }

    /**
     * Extract app name from package name or screen data
     */
    private fun extractAppName(packageName: String, screens: List<ScreenContextDTO>): String {
        // Try to get app name from package manager
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            // Fallback: extract name from package
            packageName.substringAfterLast(".")
                .replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Format timestamp for display
     */
    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
    }

    /**
     * Sanitize text for Mermaid diagrams
     */
    private fun sanitizeMermaidText(text: String): String {
        return text
            .replace("\"", "'")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("|", "\\|")
            .replace("[", "(")
            .replace("]", ")")
            .take(30) // Limit length for readability
    }

    /**
     * Wrap Mermaid diagram in Markdown
     */
    private fun wrapMermaidInMarkdown(hierarchy: AppHierarchy): String {
        val sb = StringBuilder()

        sb.appendLine("# ${hierarchy.appName} - Navigation Hierarchy")
        sb.appendLine()
        sb.appendLine("**Package:** `${hierarchy.packageName}`")
        sb.appendLine("**Generated:** ${formatTimestamp(hierarchy.generatedAt)}")
        sb.appendLine()
        sb.appendLine("## Statistics")
        sb.appendLine()
        sb.appendLine("| Metric | Value |")
        sb.appendLine("|--------|-------|")
        sb.appendLine("| Screens | ${hierarchy.stats.totalScreens} |")
        sb.appendLine("| Elements | ${hierarchy.stats.totalElements} |")
        sb.appendLine("| Edges | ${hierarchy.stats.totalEdges} |")
        sb.appendLine("| Max Depth | ${hierarchy.stats.maxDepth} |")
        sb.appendLine("| Avg Elements/Screen | ${"%.1f".format(hierarchy.stats.avgElementsPerScreen)} |")
        sb.appendLine()
        sb.appendLine("## Navigation Graph")
        sb.appendLine()
        sb.appendLine("```mermaid")
        sb.append(toMermaid(hierarchy))
        sb.appendLine("```")
        sb.appendLine()
        sb.appendLine("## Screen Details")
        sb.appendLine()

        hierarchy.nodes.sortedBy { it.depth }.forEach { node ->
            sb.appendLine("### ${node.displayName}")
            sb.appendLine()
            sb.appendLine("- **Activity:** `${node.activityName ?: "Unknown"}`")
            sb.appendLine("- **Hash:** `${node.screenHash.take(16)}...`")
            sb.appendLine("- **Depth:** ${node.depth}")
            sb.appendLine("- **Elements:** ${node.elements.size}")
            sb.appendLine()

            if (node.elements.isNotEmpty()) {
                sb.appendLine("| Element | Type | Navigational |")
                sb.appendLine("|---------|------|--------------|")
                node.elements.take(10).forEach { element ->
                    val nav = if (element.isNavigational) "Yes" else "No"
                    sb.appendLine("| ${element.label} | ${element.type} | $nav |")
                }
                if (node.elements.size > 10) {
                    sb.appendLine("| ... | ... | ... |")
                    sb.appendLine("| *(${node.elements.size - 10} more)* | | |")
                }
                sb.appendLine()
            }
        }

        return sb.toString()
    }
}
