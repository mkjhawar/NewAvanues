package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType

/**
 * Handler for native Android/iOS apps.
 *
 * Default handler for apps that don't use cross-platform frameworks.
 * Provides standard element processing for native UI components.
 */
class NativeHandler : FrameworkHandler {

    override val frameworkType: FrameworkType = FrameworkType.NATIVE

    private val androidWidgets = setOf(
        "android.widget.",
        "android.view.",
        "androidx.appcompat.",
        "androidx.recyclerview.",
        "com.google.android.material."
    )

    override fun canHandle(elements: List<ElementInfo>): Boolean {
        // Native handler is the fallback - always can handle
        return true
    }

    override fun processElements(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { isRelevantNativeElement(it) }
    }

    override fun getSelectors(): List<String> {
        return androidWidgets.toList()
    }

    override fun isActionable(element: ElementInfo): Boolean {
        return element.isClickable ||
               element.isScrollable ||
               element.text.isNotBlank() ||
               element.contentDescription.isNotBlank()
    }

    override fun getPriority(): Int = 0 // Lowest priority (fallback)

    /**
     * Check if element is a relevant native element.
     */
    private fun isRelevantNativeElement(element: ElementInfo): Boolean {
        // Skip purely layout containers without content
        if (isLayoutContainer(element) && !element.hasVoiceContent) return false

        // Include elements with content or actions
        return element.hasVoiceContent || element.isActionable
    }

    /**
     * Check if element is a layout container.
     */
    fun isLayoutContainer(element: ElementInfo): Boolean {
        val className = element.className.lowercase()
        return className.contains("layout") ||
               className.contains("framelayout") ||
               className.contains("linearlayout") ||
               className.contains("relativelayout") ||
               className.contains("constraintlayout")
    }

    /**
     * Get the widget type for native Android element.
     */
    fun getWidgetType(element: ElementInfo): String {
        val className = element.className.substringAfterLast(".")
        return when {
            className.contains("Button", ignoreCase = true) -> "Button"
            className.contains("EditText", ignoreCase = true) -> "EditText"
            className.contains("TextView", ignoreCase = true) -> "TextView"
            className.contains("ImageView", ignoreCase = true) -> "ImageView"
            className.contains("RecyclerView", ignoreCase = true) -> "RecyclerView"
            className.contains("ListView", ignoreCase = true) -> "ListView"
            className.contains("ScrollView", ignoreCase = true) -> "ScrollView"
            className.contains("Switch", ignoreCase = true) -> "Switch"
            className.contains("CheckBox", ignoreCase = true) -> "CheckBox"
            className.contains("RadioButton", ignoreCase = true) -> "RadioButton"
            className.contains("Spinner", ignoreCase = true) -> "Spinner"
            className.contains("SeekBar", ignoreCase = true) -> "SeekBar"
            className.contains("ProgressBar", ignoreCase = true) -> "ProgressBar"
            else -> className
        }
    }

    /**
     * Check if element is a Material Design component.
     */
    fun isMaterialComponent(element: ElementInfo): Boolean {
        return element.className.startsWith("com.google.android.material.")
    }

    /**
     * Check if element is an AndroidX component.
     */
    fun isAndroidXComponent(element: ElementInfo): Boolean {
        return element.className.startsWith("androidx.")
    }
}
