package com.augmentalis.commandmanager

import com.augmentalis.avid.TypeCode
import com.augmentalis.commandmanager.ElementInfo
import com.augmentalis.commandmanager.FrameworkType

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
        "Button" to TypeCode.BUTTON,
        "Checkbox" to TypeCode.CHECKBOX,
        "Switch" to TypeCode.SWITCH,
        "RadioButton" to TypeCode.CHECKBOX,
        "Tab" to TypeCode.TAB,
        "Slider" to TypeCode.SLIDER,
        "Image" to TypeCode.IMAGE,
        "DropdownList" to TypeCode.MENU,
        "ProgressBar" to TypeCode.ELEMENT
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

    fun getComposeTypeCode(element: ElementInfo, role: String = ""): String {
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
            className.contains("button") -> TypeCode.BUTTON
            className.contains("textfield") -> TypeCode.INPUT
            className.contains("checkbox") -> TypeCode.CHECKBOX
            className.contains("switch") -> TypeCode.SWITCH
            className.contains("slider") -> TypeCode.SLIDER
            className.contains("lazycolumn") || className.contains("lazyrow") -> TypeCode.SCROLL
            className.contains("lazygrid") -> TypeCode.SCROLL
            className.contains("navigationbar") -> TypeCode.MENU
            className.contains("tabrow") -> TypeCode.TAB
            className.contains("dialog") || className.contains("bottomsheet") -> TypeCode.DIALOG
            className.contains("card") -> TypeCode.CARD
            className.contains("image") || className.contains("icon") -> TypeCode.IMAGE
            className.contains("column") || className.contains("row") -> TypeCode.LAYOUT
            className.contains("box") || className.contains("scaffold") -> TypeCode.LAYOUT
            else -> TypeCode.ELEMENT
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
