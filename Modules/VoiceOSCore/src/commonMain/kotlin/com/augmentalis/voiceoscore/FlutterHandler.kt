package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.FrameworkType

/**
 * Handler for Flutter framework apps.
 *
 * Flutter apps use a custom rendering engine and expose elements
 * through accessibility semantics. This handler identifies and
 * processes Flutter-specific UI patterns.
 */
class FlutterHandler : FrameworkHandler {

    override val frameworkType: FrameworkType = FrameworkType.FLUTTER

    private val flutterPrefixes = listOf(
        "io.flutter.",
        "flutter."
    )

    private val flutterWidgets = setOf(
        "SemanticsNode",
        "FlutterSemanticsView",
        "FlutterView",
        "FlutterSurfaceView",
        "FlutterTextureView"
    )

    override fun canHandle(elements: List<ElementInfo>): Boolean {
        return elements.any { element ->
            flutterPrefixes.any { prefix ->
                element.className.startsWith(prefix)
            } || flutterWidgets.any { widget ->
                element.className.contains(widget, ignoreCase = true)
            }
        }
    }

    override fun processElements(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { isRelevantFlutterElement(it) }
    }

    override fun getSelectors(): List<String> {
        return flutterPrefixes + flutterWidgets.toList()
    }

    override fun isActionable(element: ElementInfo): Boolean {
        // Flutter elements with semantics actions are actionable
        return element.isClickable ||
               element.contentDescription.isNotBlank() ||
               element.text.isNotBlank()
    }

    override fun getPriority(): Int = 100 // High priority

    /**
     * Check if element is a relevant Flutter semantic node.
     */
    private fun isRelevantFlutterElement(element: ElementInfo): Boolean {
        // Skip internal Flutter implementation details
        if (element.className.contains("FlutterEngine")) return false
        if (element.className.contains("FlutterJNI")) return false

        // Include elements with semantic meaning
        return element.hasVoiceContent || element.isActionable
    }

    /**
     * Get Flutter widget type from element.
     */
    fun getWidgetType(element: ElementInfo): String {
        return when {
            element.className.contains("Button", ignoreCase = true) -> "Button"
            element.className.contains("TextField", ignoreCase = true) -> "TextField"
            element.className.contains("Text", ignoreCase = true) -> "Text"
            element.className.contains("Image", ignoreCase = true) -> "Image"
            element.className.contains("ListView", ignoreCase = true) -> "ListView"
            element.className.contains("Container", ignoreCase = true) -> "Container"
            else -> "Widget"
        }
    }
}
