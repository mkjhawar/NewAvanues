package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.FrameworkType

/**
 * Handler for native Android/iOS apps.
 *
 * Default handler for apps that don't use cross-platform frameworks.
 * Provides standard element processing for native UI components.
 */
class NativeHandler : FrameworkHandler {

    override val frameworkType: FrameworkType = FrameworkType.NATIVE

    /**
     * Native handler is a fallback - it can handle any elements
     * that don't have framework-specific markers.
     */
    override val isFallbackHandler: Boolean = true

    private val androidWidgets = setOf(
        "android.widget.",
        "android.view.",
        "androidx.appcompat.",
        "androidx.recyclerview.",
        "androidx.constraintlayout.",
        "androidx.coordinatorlayout.",
        "androidx.viewpager2.",
        "com.google.android.material.",
        "androidx.compose."  // Fallback for Compose elements not caught by ComposeHandler
    )

    /**
     * Specialized framework class prefixes that indicate non-native elements.
     */
    private val frameworkMarkers = setOf(
        "io.flutter.",           // Flutter
        "com.facebook.react.",   // React Native
        "com.unity3d.",          // Unity
        "org.chromium.",         // WebView
        "android.webkit.WebView" // WebView
    )

    override fun canHandle(elements: List<ElementInfo>): Boolean {
        // As fallback handler, we can handle if no framework-specific markers are found
        // OR if all elements are native Android/iOS
        if (elements.isEmpty()) return true

        // Check if any element has framework-specific markers
        val hasFrameworkMarkers = elements.any { element ->
            frameworkMarkers.any { marker ->
                element.className.startsWith(marker)
            }
        }

        // Can handle if no framework markers detected
        return !hasFrameworkMarkers
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
