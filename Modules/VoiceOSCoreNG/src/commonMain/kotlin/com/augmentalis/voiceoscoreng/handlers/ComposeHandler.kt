package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode

/**
 * Handler for Jetpack Compose applications.
 *
 * Compose apps render via AndroidComposeView and expose UI elements
 * through accessibility semantics rather than traditional View classes.
 *
 * Detection: AndroidComposeView, ComposeView, androidx.compose.*
 * Priority: 90 (between Flutter:100 and ReactNative:80)
 */
class ComposeHandler : FrameworkHandler {

    override val frameworkType: FrameworkType = FrameworkType.COMPOSE

    private val composeContainers = setOf(
        "androidx.compose.ui.platform.AndroidComposeView",
        "AndroidComposeView",
        "ComposeView",
        "androidx.compose.ui.platform.ComposeView"
    )

    private val composePatterns = setOf(
        "androidx.compose.",
        "compose.material",
        "compose.material3"
    )

    private val roleToTypeCode = mapOf(
        "Button" to VUIDTypeCode.BUTTON,
        "Checkbox" to VUIDTypeCode.CHECKBOX,
        "Switch" to VUIDTypeCode.SWITCH,
        "RadioButton" to VUIDTypeCode.CHECKBOX,
        "Tab" to VUIDTypeCode.TAB,
        "Slider" to VUIDTypeCode.SLIDER,
        "Image" to VUIDTypeCode.IMAGE,
        "DropdownList" to VUIDTypeCode.MENU,
        "ProgressBar" to VUIDTypeCode.ELEMENT
    )

    override fun canHandle(elements: List<ElementInfo>): Boolean {
        return elements.any { element ->
            composeContainers.any { marker ->
                element.className.contains(marker, ignoreCase = true)
            } || composePatterns.any { pattern ->
                element.className.startsWith(pattern, ignoreCase = true)
            }
        }
    }

    override fun processElements(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { isRelevantComposeElement(it) }
    }

    override fun getSelectors(): List<String> {
        return composeContainers.toList() + composePatterns.toList()
    }

    override fun isActionable(element: ElementInfo): Boolean {
        return element.isClickable ||
               element.isLongClickable ||
               element.isScrollable ||
               element.hasVoiceContent
    }

    override fun getPriority(): Int = 90

    fun getComposeTypeCode(element: ElementInfo, role: String = ""): VUIDTypeCode {
        // Check provided role parameter first
        if (role.isNotBlank()) {
            roleToTypeCode[role]?.let { return it }
        }
        // Check element's semanticsRole (extracted from accessibility)
        if (element.semanticsRole.isNotBlank()) {
            roleToTypeCode[element.semanticsRole]?.let { return it }
        }

        val className = element.className.lowercase()
        return when {
            className.contains("button") -> VUIDTypeCode.BUTTON
            className.contains("textfield") -> VUIDTypeCode.INPUT
            className.contains("checkbox") -> VUIDTypeCode.CHECKBOX
            className.contains("switch") -> VUIDTypeCode.SWITCH
            className.contains("slider") -> VUIDTypeCode.SLIDER
            className.contains("lazycolumn") || className.contains("lazyrow") -> VUIDTypeCode.SCROLL
            className.contains("lazygrid") -> VUIDTypeCode.SCROLL
            className.contains("navigationbar") -> VUIDTypeCode.MENU
            className.contains("tabrow") -> VUIDTypeCode.TAB
            className.contains("dialog") || className.contains("bottomsheet") -> VUIDTypeCode.DIALOG
            className.contains("card") -> VUIDTypeCode.CARD
            className.contains("image") || className.contains("icon") -> VUIDTypeCode.IMAGE
            className.contains("column") || className.contains("row") -> VUIDTypeCode.LAYOUT
            className.contains("box") || className.contains("scaffold") -> VUIDTypeCode.LAYOUT
            else -> VUIDTypeCode.ELEMENT
        }
    }

    private fun isRelevantComposeElement(element: ElementInfo): Boolean {
        if (composeContainers.any { element.className.contains(it) }) {
            return false
        }
        if (isLayoutContainer(element) && !element.hasVoiceContent && !element.isClickable) {
            return false
        }
        return element.hasVoiceContent || element.isActionable
    }

    private fun isLayoutContainer(element: ElementInfo): Boolean {
        val className = element.className.lowercase()
        return className.contains("column") ||
               className.contains("row") ||
               className.contains("box") ||
               className.contains("surface") ||
               className.contains("scaffold")
    }

    fun isComposeContainer(element: ElementInfo): Boolean {
        return composeContainers.any { element.className.contains(it, ignoreCase = true) }
    }
}
