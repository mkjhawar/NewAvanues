package com.augmentalis.voiceoscoreng.scanning

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.functions.HashUtils
import com.augmentalis.voiceoscoreng.service.ScreenCacheManager

private const val TAG = "ScanOptimizer"

/**
 * Optimizes accessibility tree scanning through intelligent change detection.
 *
 * Key Optimizations:
 * 1. Element-level change detection - only re-scan changed subtrees
 * 2. Hash comparison at each tree level for quick change detection
 * 3. Skips static elements (status bar, navigation bar)
 * 4. Integrates with ScreenCacheManager for efficient caching
 *
 * This dramatically reduces scan time for screens with minor changes
 * (e.g., timer updates, notification badges) by avoiding full tree traversal.
 */
class ScanOptimizer(
    private val screenCacheManager: ScreenCacheManager
) {

    /**
     * Package names for static system UI elements to skip.
     */
    private val staticPackages = setOf(
        "com.android.systemui"  // Status bar, navigation bar
    )

    /**
     * Resource ID patterns for static elements to skip scanning.
     * These elements rarely change and don't have actionable content.
     */
    private val staticResourcePatterns = setOf(
        "status_bar",
        "navigation_bar",
        "notification_panel",
        "quick_settings",
        "battery_level",
        "clock",
        "system_icon"
    )

    /**
     * Class names for elements that typically contain dynamic content
     * and should be re-scanned more aggressively.
     */
    private val dynamicClassTypes = setOf(
        "RecyclerView",
        "ListView",
        "GridView",
        "ScrollView",
        "ViewPager",
        "ViewPager2",
        "NestedScrollView"
    )

    /**
     * Cached subtree hashes from previous scan.
     * Key: Path (comma-separated indices from root)
     * Value: Hash of subtree structure
     */
    private val subtreeHashes = mutableMapOf<String, String>()

    /**
     * Paths that were changed in the last scan.
     * Used to identify which subtrees need full re-exploration.
     */
    private val changedPaths = mutableSetOf<String>()

    /**
     * Result of optimized scan analysis.
     */
    data class ScanAnalysis(
        val requiresFullScan: Boolean,
        val changedSubtrees: List<String>,
        val unchangedSubtrees: List<String>,
        val skippedStaticElements: Int,
        val totalNodesAnalyzed: Int,
        val optimizationRatio: Float
    )

    /**
     * Result of a subtree scan.
     */
    data class SubtreeScanResult(
        val hash: String,
        val nodeCount: Int,
        val hasChanged: Boolean,
        val depth: Int,
        val childResults: List<SubtreeScanResult>
    )

    /**
     * Analyze the accessibility tree and determine what needs scanning.
     *
     * @param rootNode Root of the accessibility tree
     * @param forceFullScan If true, bypass optimization and scan everything
     * @return Analysis result with optimization recommendations
     */
    fun analyzeTree(
        rootNode: AccessibilityNodeInfo,
        forceFullScan: Boolean = false
    ): ScanAnalysis {
        changedPaths.clear()

        if (forceFullScan) {
            Log.d(TAG, "Full scan forced - bypassing optimization")
            return ScanAnalysis(
                requiresFullScan = true,
                changedSubtrees = listOf("root"),
                unchangedSubtrees = emptyList(),
                skippedStaticElements = 0,
                totalNodesAnalyzed = 0,
                optimizationRatio = 0f
            )
        }

        val startTime = System.currentTimeMillis()
        var skippedStatic = 0
        var totalAnalyzed = 0
        val unchangedPaths = mutableListOf<String>()

        // Analyze tree recursively with hash comparison
        val result = analyzeSubtree(
            node = rootNode,
            path = "0",
            depth = 0,
            analysisCallback = { path, isStatic, isUnchanged ->
                totalAnalyzed++
                if (isStatic) skippedStatic++
                if (isUnchanged) unchangedPaths.add(path)
            }
        )

        val duration = System.currentTimeMillis() - startTime
        val optimizationRatio = if (totalAnalyzed > 0) {
            (unchangedPaths.size + skippedStatic).toFloat() / totalAnalyzed
        } else 0f

        Log.d(TAG, "Tree analysis completed in ${duration}ms - " +
                "analyzed=$totalAnalyzed, unchanged=${unchangedPaths.size}, " +
                "skipped=$skippedStatic, optimization=${(optimizationRatio * 100).toInt()}%")

        return ScanAnalysis(
            requiresFullScan = changedPaths.isEmpty() && subtreeHashes.isEmpty(),
            changedSubtrees = changedPaths.toList(),
            unchangedSubtrees = unchangedPaths,
            skippedStaticElements = skippedStatic,
            totalNodesAnalyzed = totalAnalyzed,
            optimizationRatio = optimizationRatio
        )
    }

    /**
     * Analyze a subtree and compare with cached hash.
     *
     * @param node Current node
     * @param path Path from root (e.g., "0,2,1")
     * @param depth Current depth
     * @param analysisCallback Callback for each analyzed node
     * @return Hash of this subtree
     */
    private fun analyzeSubtree(
        node: AccessibilityNodeInfo,
        path: String,
        depth: Int,
        analysisCallback: (path: String, isStatic: Boolean, isUnchanged: Boolean) -> Unit
    ): String {
        // Check if this is a static element to skip
        if (isStaticElement(node)) {
            analysisCallback(path, true, false)
            return "static"
        }

        // Build signature for this node
        val nodeSignature = buildNodeSignature(node)

        // Recursively process children and build combined hash
        val childHashes = StringBuilder()
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val childPath = "$path,$i"
                val childHash = analyzeSubtree(child, childPath, depth + 1, analysisCallback)
                childHashes.append(childHash)
                child.recycle()
            }
        }

        // Combine node signature with children hashes
        val subtreeSignature = "$nodeSignature|${childHashes}"
        val currentHash = HashUtils.generateHash(subtreeSignature, 16)

        // Compare with cached hash
        val previousHash = subtreeHashes[path]
        val isUnchanged = previousHash == currentHash

        if (!isUnchanged && previousHash != null) {
            // This subtree has changed
            changedPaths.add(path)
            Log.v(TAG, "Change detected at path=$path depth=$depth")
        }

        // Update cache
        subtreeHashes[path] = currentHash

        analysisCallback(path, false, isUnchanged)
        return currentHash
    }

    /**
     * Build a structural signature for a single node.
     * Does NOT include dynamic content like text to avoid false positives.
     */
    private fun buildNodeSignature(node: AccessibilityNodeInfo): String {
        val className = node.className?.toString()?.substringAfterLast(".") ?: ""
        val resourceId = node.viewIdResourceName?.substringAfterLast("/") ?: ""
        val isClickable = if (node.isClickable) "C" else ""
        val isScrollable = if (node.isScrollable) "S" else ""
        val childCount = node.childCount

        // For scrollable containers, don't include child count as it varies
        val childInfo = if (isDynamicContainer(className)) "v" else "c$childCount"

        return "$className:$resourceId:$childInfo:$isClickable$isScrollable"
    }

    /**
     * Check if a class name represents a dynamic container.
     */
    private fun isDynamicContainer(className: String): Boolean {
        return dynamicClassTypes.any { className.contains(it, ignoreCase = true) }
    }

    /**
     * Check if a node represents a static system UI element.
     */
    private fun isStaticElement(node: AccessibilityNodeInfo): Boolean {
        // Check package name
        val packageName = node.packageName?.toString() ?: ""
        if (staticPackages.contains(packageName)) {
            return true
        }

        // Check resource ID patterns
        val resourceId = node.viewIdResourceName?.lowercase() ?: ""
        if (staticResourcePatterns.any { resourceId.contains(it) }) {
            return true
        }

        return false
    }

    /**
     * Get nodes at specific paths that need re-scanning.
     *
     * @param rootNode Root of the accessibility tree
     * @param paths Paths to extract (e.g., ["0,2,1", "0,3"])
     * @return Map of path to node (caller must recycle)
     */
    fun getNodesAtPaths(
        rootNode: AccessibilityNodeInfo,
        paths: List<String>
    ): Map<String, AccessibilityNodeInfo?> {
        val result = mutableMapOf<String, AccessibilityNodeInfo?>()

        for (path in paths) {
            result[path] = navigateToPath(rootNode, path)
        }

        return result
    }

    /**
     * Navigate to a specific path in the tree.
     *
     * @param rootNode Starting node
     * @param path Path as comma-separated indices (e.g., "0,2,1")
     * @return Node at path or null if not found
     */
    private fun navigateToPath(rootNode: AccessibilityNodeInfo, path: String): AccessibilityNodeInfo? {
        if (path == "0") return rootNode

        val indices = path.split(",").drop(1).map { it.toIntOrNull() ?: return null }

        var current: AccessibilityNodeInfo? = rootNode
        for (index in indices) {
            current = current?.getChild(index)
            if (current == null) return null
        }

        return current
    }

    /**
     * Determine if an element is within a changed subtree.
     *
     * @param elementPath Path of the element to check
     * @return true if element is in a changed subtree
     */
    fun isInChangedSubtree(elementPath: String): Boolean {
        return changedPaths.any { changedPath ->
            elementPath.startsWith(changedPath)
        }
    }

    /**
     * Clear all cached hashes.
     * Call when the app changes or a full rescan is needed.
     */
    fun clearCache() {
        subtreeHashes.clear()
        changedPaths.clear()
        Log.d(TAG, "Scan cache cleared")
    }

    /**
     * Clear cache for a specific subtree.
     *
     * @param pathPrefix Path prefix to clear (e.g., "0,2" clears all under that subtree)
     */
    fun clearSubtreeCache(pathPrefix: String) {
        val keysToRemove = subtreeHashes.keys.filter { it.startsWith(pathPrefix) }
        keysToRemove.forEach { subtreeHashes.remove(it) }
        Log.d(TAG, "Cleared ${keysToRemove.size} cached entries for subtree: $pathPrefix")
    }

    /**
     * Get current cache statistics.
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            cachedSubtrees = subtreeHashes.size,
            changedSubtrees = changedPaths.size,
            memorySizeEstimate = subtreeHashes.size * 50L // ~50 bytes per entry estimate
        )
    }

    /**
     * Statistics about the scan cache.
     */
    data class CacheStats(
        val cachedSubtrees: Int,
        val changedSubtrees: Int,
        val memorySizeEstimate: Long
    )

    companion object {
        /**
         * Maximum depth to analyze for optimization.
         * Deeper levels are scanned normally without caching.
         */
        const val MAX_OPTIMIZATION_DEPTH = 8

        /**
         * Minimum tree size before optimization kicks in.
         * Small trees don't benefit from incremental scanning.
         */
        const val MIN_TREE_SIZE_FOR_OPTIMIZATION = 20
    }
}
