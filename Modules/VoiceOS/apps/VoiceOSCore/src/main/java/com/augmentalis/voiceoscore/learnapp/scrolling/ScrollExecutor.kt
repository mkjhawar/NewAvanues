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

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
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
class ScrollExecutor {

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
     * @param scrollable Scrollable container
     * @return List of all elements
     */
    suspend fun scrollVerticallyAndCollect(scrollable: AccessibilityNodeInfo): List<ElementInfo> {
        val allElements = mutableSetOf<ElementInfo>()
        var previousHash = ""
        var unchangedCount = 0
        val maxAttempts = 50

        // Scroll down and collect
        repeat(maxAttempts) {
            // Collect visible elements
            val currentElements = collectVisibleElements(scrollable)
            allElements.addAll(currentElements)

            // Hash elements to detect if we've reached the end
            val currentHash = hashElements(currentElements)

            if (currentHash == previousHash) {
                unchangedCount++
                if (unchangedCount >= 2) {
                    return@repeat  // Reached end
                }
            } else {
                unchangedCount = 0
            }

            previousHash = currentHash

            // Try to scroll forward
            val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (!scrolled) {
                return@repeat  // Can't scroll anymore
            }

            // Wait for scroll animation
            delay(300)
        }

        // Scroll back to top
        scrollBackToTop(scrollable)

        return allElements.toList()
    }

    /**
     * Scroll horizontally and collect elements
     *
     * Scrolls right until no new elements appear.
     *
     * @param scrollable Scrollable container
     * @return List of all elements
     */
    suspend fun scrollHorizontallyAndCollect(scrollable: AccessibilityNodeInfo): List<ElementInfo> {
        val allElements = mutableSetOf<ElementInfo>()
        var previousHash = ""
        var unchangedCount = 0
        val maxAttempts = 20  // Horizontal scrolls usually shorter

        repeat(maxAttempts) {
            // Collect visible elements
            val currentElements = collectVisibleElements(scrollable)
            allElements.addAll(currentElements)

            // Hash to detect end
            val currentHash = hashElements(currentElements)

            if (currentHash == previousHash) {
                unchangedCount++
                if (unchangedCount >= 2) {
                    return@repeat
                }
            } else {
                unchangedCount = 0
            }

            previousHash = currentHash

            // Scroll forward (right)
            val scrolled = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (!scrolled) {
                return@repeat
            }

            delay(300)
        }

        // Scroll back to start
        scrollBackToStart(scrollable)

        return allElements.toList()
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
     * @param node Current node
     * @param visitor Visitor function
     */
    private fun traverseContainer(
        node: AccessibilityNodeInfo,
        visitor: (AccessibilityNodeInfo) -> Unit
    ) {
        visitor(node)

        for (i in 0 until node.childCount) {
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
     * Creates hash from element bounds and text to detect if scroll position changed.
     *
     * @param elements List of elements
     * @return Hash string
     */
    private fun hashElements(elements: List<ElementInfo>): String {
        val signature = elements.joinToString("\n") { element ->
            "${element.bounds}|${element.text}|${element.contentDescription}"
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

            delay(100)
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

            delay(100)
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
