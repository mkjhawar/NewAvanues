/**
 * ScrollExecutor.kt - Executes scrolling and collects elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/scrolling/ScrollExecutor.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Executes scrolling actions and collects offscreen elements
 */

package com.augmentalis.voiceoscore.learnapp.scrolling

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import kotlinx.coroutines.delay
import java.security.MessageDigest

/**
 * Scroll Executor
 *
 * Executes scrolling to find offscreen elements.
 * Scrolls vertically and horizontally to discover all elements in container.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val executor = ScrollExecutor()
 *
 * val scrollable = findScrollableContainer(rootNode)
 * val allElements = executor.scrollAndCollectAll(scrollable)
 *
 * println("Found ${allElements.size} elements")
 * ```
 *
 * ## Algorithm
 *
 * 1. Collect visible elements
 * 2. Scroll forward (down or right)
 * 3. Wait for scroll animation
 * 4. Collect new elements
 * 5. Hash elements to detect duplicates
 * 6. If hash unchanged 2 times → reached end
 * 7. Scroll back to top
 *
 * @since 1.0.0
 */
class ScrollExecutor(
    private val context: Context
) {

    /**
     * Developer settings for scroll configuration
     */
    private val developerSettings by lazy {
        LearnAppDeveloperSettings(context)
    }

    /**
     * Scroll detector
     */
    private val scrollDetector = ScrollDetector()

    /**
     * Scroll and collect all elements from container
     *
     * Main entry point. Scrolls container and collects all elements.
     *
     * @param scrollable Scrollable container node
     * @return List of all elements (including offscreen)
     */
    suspend fun scrollAndCollectAll(scrollable: AccessibilityNodeInfo): List<ElementInfo> {
        val direction = scrollDetector.detectScrollDirection(scrollable)

        return when (direction) {
            ScrollDirection.VERTICAL -> scrollVerticallyAndCollect(scrollable)
            ScrollDirection.HORIZONTAL -> scrollHorizontallyAndCollect(scrollable)
            ScrollDirection.BOTH -> {
                // Scroll vertically first, then horizontally
                val vertical = scrollVerticallyAndCollect(scrollable)
                val horizontal = scrollHorizontallyAndCollect(scrollable)
                (vertical + horizontal).distinctBy { it.hashCode() }
            }
        }
    }

    /**
     * Scroll vertically and collect elements
     *
     * Scrolls down until no new elements appear, then scrolls back to top.
     *
     * FIX (2025-12-04): Added element limiting and enhanced scroll end detection
     * - Limits to MAX_ELEMENTS_PER_SCROLLABLE = 20
     * - 3-strategy scroll end detection: hash unchanged, element count, iteration limit
     *
     * @param scrollable Scrollable container
     * @return List of all elements
     */
    suspend fun scrollVerticallyAndCollect(scrollable: AccessibilityNodeInfo): List<ElementInfo> {
        val allElements = mutableSetOf<ElementInfo>()
        var previousHash = ""
        var previousElementCount = 0
        var unchangedCount = 0

        // Scroll down and collect
        repeat(developerSettings.getMaxVerticalScrollIterations()) { iteration ->
            // Check if we've reached element limit
            if (allElements.size >= developerSettings.getMaxElementsPerScrollable()) {
                return@repeat  // Strategy 3: Element limit reached
            }

            // Collect visible elements
            val currentElements = collectVisibleElements(scrollable)
            allElements.addAll(currentElements)

            // Strategy 1: Hash-based scroll end detection
            val currentHash = hashElements(currentElements)
            val currentElementCount = currentElements.size

            if (currentHash == previousHash) {
                unchangedCount++
                if (unchangedCount >= 2) {
                    return@repeat  // Strategy 1: Hash unchanged twice - reached end
                }
            } else {
                unchangedCount = 0
            }

            // Strategy 2: Element count unchanged
            if (currentElementCount == previousElementCount && currentElementCount > 0) {
                return@repeat  // Strategy 2: Same elements visible - reached end
            }

            previousHash = currentHash
            previousElementCount = currentElementCount

            // Try to scroll forward
            val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (!scrolled) {
                return@repeat  // Strategy 1: Can't scroll anymore
            }

            // Wait for scroll animation
            delay(developerSettings.getScrollDelayMs())
        }

        // Scroll back to top
        scrollBackToTop(scrollable)

        // Limit to MAX_ELEMENTS_PER_SCROLLABLE
        return allElements.take(developerSettings.getMaxElementsPerScrollable())
    }

    /**
     * Scroll horizontally and collect elements
     *
     * Scrolls right until no new elements appear.
     *
     * FIX (2025-12-04): Added element limiting and enhanced scroll end detection
     * - Limits to MAX_ELEMENTS_PER_SCROLLABLE = 20
     * - 3-strategy scroll end detection: hash unchanged, element count, iteration limit
     *
     * @param scrollable Scrollable container
     * @return List of all elements
     */
    suspend fun scrollHorizontallyAndCollect(scrollable: AccessibilityNodeInfo): List<ElementInfo> {
        val allElements = mutableSetOf<ElementInfo>()
        var previousHash = ""
        var previousElementCount = 0
        var unchangedCount = 0

        repeat(developerSettings.getMaxHorizontalScrollIterations()) {
            // Check if we've reached element limit
            if (allElements.size >= developerSettings.getMaxElementsPerScrollable()) {
                return@repeat  // Strategy 3: Element limit reached
            }

            // Collect visible elements
            val currentElements = collectVisibleElements(scrollable)
            allElements.addAll(currentElements)

            // Strategy 1: Hash-based scroll end detection
            val currentHash = hashElements(currentElements)
            val currentElementCount = currentElements.size

            if (currentHash == previousHash) {
                unchangedCount++
                if (unchangedCount >= 2) {
                    return@repeat  // Strategy 1: Hash unchanged twice - reached end
                }
            } else {
                unchangedCount = 0
            }

            // Strategy 2: Element count unchanged
            if (currentElementCount == previousElementCount && currentElementCount > 0) {
                return@repeat  // Strategy 2: Same elements visible - reached end
            }

            previousHash = currentHash
            previousElementCount = currentElementCount

            // Scroll forward (right)
            val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (!scrolled) {
                return@repeat  // Strategy 1: Can't scroll anymore
            }

            delay(developerSettings.getScrollDelayMs())
        }

        // Scroll back to start
        scrollBackToStart(scrollable)

        // Limit to MAX_ELEMENTS_PER_SCROLLABLE
        return allElements.take(developerSettings.getMaxElementsPerScrollable())
    }

    /**
     * Collect visible elements from container
     *
     * Traverses container and collects all child elements.
     *
     * @param container Container node
     * @return List of visible elements
     */
    private fun collectVisibleElements(container: AccessibilityNodeInfo): List<ElementInfo> {
        val elements = mutableListOf<ElementInfo>()

        traverseContainer(container) { node ->
            // Skip the container itself
            if (node != container) {
                elements.add(ElementInfo.fromNode(node))
            }
        }

        return elements
    }

    /**
     * Traverse container (DFS)
     *
     * FIX (2025-12-04): Added child limit to prevent memory exhaustion
     * - Limits to MAX_CHILDREN_PER_CONTAINER = 50
     *
     * @param node Current node
     * @param visitor Visitor function
     */
    private fun traverseContainer(
        node: AccessibilityNodeInfo,
        visitor: (AccessibilityNodeInfo) -> Unit
    ) {
        visitor(node)

        // Limit children per container to prevent memory exhaustion
        val maxChildren = minOf(node.childCount, developerSettings.getMaxChildrenPerScrollContainer())

        for (i in 0 until maxChildren) {
            node.getChild(i)?.let { child ->
                try {
                    traverseContainer(child, visitor)
                } finally {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                }
            }
        }
    }

    /**
     * Hash elements to detect duplicates
     *
     * Creates hash from element properties to detect if scroll position changed.
     *
     * FIX (2025-12-04): Enhanced hashing algorithm
     * - Uses: className|resourceId|text|contentDescription|bounds
     * - Provides better deduplication accuracy
     *
     * @param elements List of elements
     * @return Hash string
     */
    private fun hashElements(elements: List<ElementInfo>): String {
        val signature = elements.joinToString("\n") { element ->
            "${element.className}|${element.resourceId}|${element.text}|${element.contentDescription}|${element.bounds}"
        }

        val digest = MessageDigest.getInstance("MD5")
        val hashBytes = digest.digest(signature.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Scroll back to top
     *
     * Scrolls backward (up) until can't scroll anymore.
     *
     * @param scrollable Scrollable container
     */
    private suspend fun scrollBackToTop(scrollable: AccessibilityNodeInfo) {
        val maxAttempts = 50

        repeat(maxAttempts) {
            val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            if (!scrolled) {
                return@repeat  // Reached top
            }

            delay(developerSettings.getClickRetryDelayMs())
        }
    }

    /**
     * Scroll back to start (horizontal)
     *
     * Scrolls backward (left) until can't scroll anymore.
     *
     * @param scrollable Scrollable container
     */
    private suspend fun scrollBackToStart(scrollable: AccessibilityNodeInfo) {
        val maxAttempts = 20

        repeat(maxAttempts) {
            val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            if (!scrolled) {
                return@repeat
            }

            delay(developerSettings.getClickRetryDelayMs())
        }
    }

    /**
     * Get scroll statistics
     *
     * @param elements Elements collected
     * @return Scroll stats
     */
    fun getStats(elements: List<ElementInfo>): ScrollStats {
        return ScrollStats(
            totalElements = elements.size,
            clickableElements = elements.count { it.isClickable },
            scrollableElements = elements.count { it.isScrollable }
        )
    }
}

/**
 * Scroll Statistics
 *
 * @property totalElements Total elements collected
 * @property clickableElements Clickable elements found
 * @property scrollableElements Nested scrollable containers found
 */
data class ScrollStats(
    val totalElements: Int,
    val clickableElements: Int,
    val scrollableElements: Int
) {
    override fun toString(): String {
        return """
            Scroll Stats:
            - Total Elements: $totalElements
            - Clickable: $clickableElements
            - Scrollable: $scrollableElements
        """.trimIndent()
    }
}
