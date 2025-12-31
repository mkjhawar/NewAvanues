package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType

/**
 * Handler for Unity game engine apps.
 *
 * Unity apps render to a single surface with limited accessibility
 * exposure. This handler identifies Unity apps and provides
 * specialized handling for game UI elements.
 */
class UnityHandler : FrameworkHandler {

    override val frameworkType: FrameworkType = FrameworkType.UNITY

    private val unityPrefixes = listOf(
        "com.unity3d.",
        "com.unity."
    )

    private val unityClasses = setOf(
        "UnityPlayer",
        "UnityPlayerActivity",
        "UnityPlayerNativeActivity",
        "UnityView"
    )

    override fun canHandle(elements: List<ElementInfo>): Boolean {
        return elements.any { element ->
            unityPrefixes.any { prefix ->
                element.className.startsWith(prefix)
            } || unityClasses.any { cls ->
                element.className.contains(cls, ignoreCase = true)
            }
        }
    }

    override fun processElements(elements: List<ElementInfo>): List<ElementInfo> {
        // Unity games typically have a single view with overlays
        return elements.filter { isRelevantUnityElement(it) }
    }

    override fun getSelectors(): List<String> {
        return unityPrefixes + unityClasses.toList()
    }

    override fun isActionable(element: ElementInfo): Boolean {
        // Unity elements with accessibility labels are actionable
        return element.contentDescription.isNotBlank() ||
               element.text.isNotBlank()
    }

    override fun getPriority(): Int = 90 // High priority (below Flutter)

    /**
     * Check if element is a relevant Unity UI element.
     */
    private fun isRelevantUnityElement(element: ElementInfo): Boolean {
        // The main Unity player view is always relevant
        if (unityClasses.any { element.className.contains(it) }) return true

        // Native Android overlays on Unity games
        return element.hasVoiceContent
    }

    /**
     * Check if this is the main Unity player surface.
     */
    fun isUnityPlayerSurface(element: ElementInfo): Boolean {
        return element.className.contains("UnityPlayer")
    }

    /**
     * Get overlay elements rendered on top of Unity.
     */
    fun getOverlayElements(elements: List<ElementInfo>): List<ElementInfo> {
        val unityPlayerIndex = elements.indexOfFirst { isUnityPlayerSurface(it) }
        if (unityPlayerIndex < 0) return emptyList()

        // Elements after Unity player are overlays
        return elements.drop(unityPlayerIndex + 1)
            .filter { it.hasVoiceContent || it.isActionable }
    }
}
