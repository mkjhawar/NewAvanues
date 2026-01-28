package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ElementInfo
import com.augmentalis.commandmanager.FrameworkInfo
import com.augmentalis.commandmanager.FrameworkType

/**
 * Utility functions for filtering UI elements during scraping.
 *
 * Provides filters for:
 * - Actionable elements (clickable, scrollable)
 * - Voice-targetable elements (has content for voice recognition)
 * - System elements (status bar, navigation, etc.)
 * - Framework-specific elements
 */
object ElementFilterUtils {

    /**
     * System package prefixes that should typically be filtered out.
     */
    private val SYSTEM_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.android.keyguard"
    )

    /**
     * Class names that represent system/framework elements.
     */
    private val SYSTEM_CLASSES = setOf(
        "StatusBarWindowView",
        "NavigationBarView",
        "NotificationStackScrollLayout",
        "KeyguardBottomAreaView",
        "LauncherRootView"
    )

    /**
     * Class name patterns for non-interactive container elements.
     */
    private val CONTAINER_CLASSES = setOf(
        "FrameLayout",
        "LinearLayout",
        "RelativeLayout",
        "ConstraintLayout",
        "ViewGroup",
        "View"
    )

    /**
     * Filter elements to only those that are actionable.
     *
     * @param elements List of elements to filter
     * @return List of elements that are clickable or scrollable
     */
    fun filterActionable(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { it.isActionable }
    }

    /**
     * Filter elements to only those that have voice content.
     *
     * @param elements List of elements to filter
     * @return List of elements with text, content description, or resource ID
     */
    fun filterWithVoiceContent(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { it.hasVoiceContent }
    }

    /**
     * Filter out system UI elements.
     *
     * @param elements List of elements to filter
     * @return List of elements not from system packages
     */
    fun filterOutSystemElements(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { element ->
            !isSystemElement(element)
        }
    }

    /**
     * Filter to get elements suitable for voice commands.
     * Combines actionable + has voice content + not system.
     *
     * @param elements List of elements to filter
     * @return List of elements suitable for voice targeting
     */
    fun filterForVoiceCommands(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { element ->
            element.isActionable &&
            element.hasVoiceContent &&
            !isSystemElement(element)
        }
    }

    /**
     * Filter based on framework type.
     *
     * @param elements List of elements to filter
     * @param frameworkInfo The detected framework
     * @return Filtered elements appropriate for the framework
     */
    fun filterForFramework(
        elements: List<ElementInfo>,
        frameworkInfo: FrameworkInfo
    ): List<ElementInfo> {
        return when (frameworkInfo.type) {
            FrameworkType.FLUTTER -> filterFlutterElements(elements)
            FrameworkType.REACT_NATIVE -> filterReactNativeElements(elements)
            FrameworkType.WEBVIEW -> filterWebViewElements(elements)
            else -> elements
        }
    }

    /**
     * Check if an element is a system element.
     */
    fun isSystemElement(element: ElementInfo): Boolean {
        // Check package
        if (SYSTEM_PACKAGES.any { element.packageName.startsWith(it) }) {
            return true
        }

        // Check class name
        val simpleClassName = element.className.substringAfterLast(".")
        if (SYSTEM_CLASSES.contains(simpleClassName)) {
            return true
        }

        return false
    }

    /**
     * Check if an element is a pure container (no content, not actionable).
     */
    fun isContainerElement(element: ElementInfo): Boolean {
        val simpleClassName = element.className.substringAfterLast(".")
        return CONTAINER_CLASSES.contains(simpleClassName) &&
               !element.isActionable &&
               !element.hasVoiceContent
    }

    /**
     * Get elements grouped by their voice label.
     * Useful for finding duplicate labels that need disambiguation.
     *
     * @param elements List of elements
     * @return Map of voice label to list of elements with that label
     */
    fun groupByVoiceLabel(elements: List<ElementInfo>): Map<String, List<ElementInfo>> {
        return elements.groupBy { it.voiceLabel }
    }

    /**
     * Find elements with duplicate voice labels.
     *
     * @param elements List of elements
     * @return List of elements that share a voice label with at least one other element
     */
    fun findDuplicateLabels(elements: List<ElementInfo>): List<ElementInfo> {
        val grouped = groupByVoiceLabel(elements)
        return grouped.filter { it.value.size > 1 }
            .flatMap { it.value }
    }

    /**
     * Filter elements within a bounds region.
     *
     * @param elements List of elements
     * @param left Left bound
     * @param top Top bound
     * @param right Right bound
     * @param bottom Bottom bound
     * @return Elements whose bounds intersect with the specified region
     */
    fun filterInRegion(
        elements: List<ElementInfo>,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): List<ElementInfo> {
        return elements.filter { element ->
            val bounds = element.bounds
            bounds.right > left &&
            bounds.left < right &&
            bounds.bottom > top &&
            bounds.top < bottom
        }
    }

    /**
     * Sort elements by their position (top-to-bottom, left-to-right).
     *
     * @param elements List of elements
     * @return Sorted list
     */
    fun sortByPosition(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
    }

    // ==================== Framework-Specific Filters ====================

    private fun filterFlutterElements(elements: List<ElementInfo>): List<ElementInfo> {
        // Flutter elements often have "SemanticsNode" or platform view classes
        // Filter out internal Flutter framework elements
        return elements.filter { element ->
            !element.className.contains("FlutterPlatformView") ||
            element.hasVoiceContent
        }
    }

    private fun filterReactNativeElements(elements: List<ElementInfo>): List<ElementInfo> {
        // React Native elements include ReactViewGroup, ReactTextView, etc.
        // Keep elements that are actionable or have content
        return elements.filter { element ->
            element.isActionable || element.hasVoiceContent
        }
    }

    private fun filterWebViewElements(elements: List<ElementInfo>): List<ElementInfo> {
        // WebView content may have synthetic accessibility nodes
        // Filter out empty WebView containers
        return elements.filter { element ->
            if (element.className.contains("WebView")) {
                element.hasVoiceContent
            } else {
                true
            }
        }
    }
}
