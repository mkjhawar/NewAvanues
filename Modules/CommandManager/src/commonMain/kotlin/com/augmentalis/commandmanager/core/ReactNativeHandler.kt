package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ElementInfo
import com.augmentalis.commandmanager.FrameworkType

/**
 * Handler for React Native framework apps.
 *
 * React Native apps use native views with JavaScript bridge.
 * This handler identifies RN-specific patterns and processes
 * elements accordingly.
 */
class ReactNativeHandler : FrameworkHandler {

    override val frameworkType: FrameworkType = FrameworkType.REACT_NATIVE

    private val rnPrefixes = listOf(
        "com.facebook.react.",
        "com.swmansion.",
        "com.reactnative."
    )

    private val rnClasses = setOf(
        "ReactRootView",
        "ReactTextView",
        "ReactViewGroup",
        "ReactEditText",
        "ReactImageView",
        "RCTView",
        "RCTText",
        "RCTScrollView"
    )

    override fun canHandle(elements: List<ElementInfo>): Boolean {
        return elements.any { element ->
            rnPrefixes.any { prefix ->
                element.className.startsWith(prefix)
            } || rnClasses.any { cls ->
                element.className.contains(cls, ignoreCase = true)
            }
        }
    }

    override fun processElements(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { isRelevantRNElement(it) }
    }

    override fun getSelectors(): List<String> {
        return rnPrefixes + rnClasses.toList()
    }

    override fun isActionable(element: ElementInfo): Boolean {
        // RN elements with accessibilityLabel are actionable
        return element.isClickable ||
               element.contentDescription.isNotBlank() ||
               element.text.isNotBlank()
    }

    override fun getPriority(): Int = 80 // Medium-high priority

    /**
     * Check if element is a relevant React Native element.
     */
    private fun isRelevantRNElement(element: ElementInfo): Boolean {
        // Skip internal RN implementation
        if (element.className.contains("ReactNativeHost")) return false
        if (element.className.contains("ReactInstanceManager")) return false

        // Include elements with content
        return element.hasVoiceContent || element.isActionable
    }

    /**
     * Get the RN component type from element.
     */
    fun getComponentType(element: ElementInfo): String {
        return when {
            element.className.contains("Button", ignoreCase = true) -> "Button"
            element.className.contains("TextInput", ignoreCase = true) -> "TextInput"
            element.className.contains("EditText", ignoreCase = true) -> "TextInput"
            element.className.contains("Text", ignoreCase = true) -> "Text"
            element.className.contains("Image", ignoreCase = true) -> "Image"
            element.className.contains("ScrollView", ignoreCase = true) -> "ScrollView"
            element.className.contains("FlatList", ignoreCase = true) -> "FlatList"
            element.className.contains("View", ignoreCase = true) -> "View"
            else -> "Component"
        }
    }

    /**
     * Check if element has testID (common RN testing pattern).
     */
    fun hasTestId(element: ElementInfo): Boolean {
        return element.resourceId.contains("testID", ignoreCase = true) ||
               element.contentDescription.startsWith("testID:")
    }
}
