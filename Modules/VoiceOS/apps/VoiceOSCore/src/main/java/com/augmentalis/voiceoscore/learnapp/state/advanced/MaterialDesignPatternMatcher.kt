/**
 * MaterialDesignPatternMatcher.kt - Detects Material Design UI components
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Specialized pattern matcher for Material Design components including buttons,
 * text inputs, progress indicators, and dialogs. Provides confidence scoring
 * for Material-specific UI patterns.
 */
package com.augmentalis.voiceoscore.learnapp.state.advanced

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Material Design component types
 */
enum class MaterialComponent {
    BUTTON,              // Material Button (filled, outlined, text)
    FAB,                 // Floating Action Button
    TEXT_INPUT,          // TextInputLayout with EditText
    PROGRESS_LINEAR,     // Linear progress indicator
    PROGRESS_CIRCULAR,   // Circular progress indicator
    DIALOG,              // Material Dialog
    SNACKBAR,            // Snackbar notification
    CHIP,                // Material Chip
    UNKNOWN              // Not a recognized Material component
}

/**
 * Material component detection result
 */
data class MaterialComponentMatch(
    val component: MaterialComponent,
    val confidence: Float,
    val node: AccessibilityNodeInfo?,
    val indicators: List<String>
)

/**
 * Detects Material Design UI components from accessibility tree
 *
 * Uses class name analysis, resource ID patterns, and visual hierarchy
 * to identify Material Design components with confidence scoring.
 */
class MaterialDesignPatternMatcher {

    companion object {
        private const val TAG = "MaterialDesignMatcher"

        // Material Design class name patterns
        private val MATERIAL_BUTTON_CLASSES = setOf(
            "com.google.android.material.button.MaterialButton",
            "androidx.material.button.MaterialButton"
        )

        private val FAB_CLASSES = setOf(
            "com.google.android.material.floatingactionbutton.FloatingActionButton",
            "androidx.material.floatingactionbutton.FloatingActionButton"
        )

        private val TEXT_INPUT_CLASSES = setOf(
            "com.google.android.material.textfield.TextInputLayout",
            "androidx.material.textfield.TextInputEditText"
        )

        private val PROGRESS_CLASSES = setOf(
            "com.google.android.material.progressindicator.LinearProgressIndicator",
            "com.google.android.material.progressindicator.CircularProgressIndicator",
            "androidx.material.progressindicator.LinearProgressIndicator",
            "androidx.material.progressindicator.CircularProgressIndicator"
        )

        private val DIALOG_CLASSES = setOf(
            "androidx.appcompat.app.AlertDialog",
            "com.google.android.material.dialog.MaterialAlertDialogBuilder"
        )

        private val CHIP_CLASSES = setOf(
            "com.google.android.material.chip.Chip",
            "androidx.material.chip.Chip"
        )

        // Resource ID patterns for Material components
        private val MATERIAL_RESOURCE_PATTERNS = mapOf(
            MaterialComponent.TEXT_INPUT to listOf("text_input", "textinput", "til_"),
            MaterialComponent.FAB to listOf("fab", "floating_action"),
            MaterialComponent.SNACKBAR to listOf("snackbar"),
            MaterialComponent.CHIP to listOf("chip")
        )
    }

    /**
     * Detect all Material Design components in accessibility tree
     *
     * @param rootNode Root of accessibility tree
     * @return List of detected Material components with confidence scores
     */
    fun detectMaterialComponents(rootNode: AccessibilityNodeInfo?): List<MaterialComponentMatch> {
        if (rootNode == null) return emptyList()

        val matches = mutableListOf<MaterialComponentMatch>()
        traverseAndDetect(rootNode, matches)
        return matches
    }

    /**
     * Check if node is a Material Design component
     *
     * @param node Node to check
     * @return Match result with confidence, or null if not Material component
     */
    fun detectMaterialComponent(node: AccessibilityNodeInfo?): MaterialComponentMatch? {
        if (node == null) return null

        val className = node.className?.toString() ?: return null
        val resourceId = node.viewIdResourceName ?: ""
        val indicators = mutableListOf<String>()
        var component = MaterialComponent.UNKNOWN
        var confidence = 0f

        // Check Material Button
        if (MATERIAL_BUTTON_CLASSES.any { className.contains(it) }) {
            component = MaterialComponent.BUTTON
            confidence = 0.95f
            indicators.add("Material Button class: $className")
        }
        // Check FAB
        else if (FAB_CLASSES.any { className.contains(it) }) {
            component = MaterialComponent.FAB
            confidence = 0.95f
            indicators.add("FAB class: $className")
        }
        // Check Text Input
        else if (TEXT_INPUT_CLASSES.any { className.contains(it) }) {
            component = MaterialComponent.TEXT_INPUT
            confidence = 0.95f
            indicators.add("Material TextInput class: $className")
        }
        // Check Progress Indicators
        else if (PROGRESS_CLASSES.any { className.contains(it) }) {
            component = if (className.contains("Linear")) {
                MaterialComponent.PROGRESS_LINEAR
            } else {
                MaterialComponent.PROGRESS_CIRCULAR
            }
            confidence = 0.95f
            indicators.add("Material Progress class: $className")
        }
        // Check Dialog
        else if (DIALOG_CLASSES.any { className.contains(it) }) {
            component = MaterialComponent.DIALOG
            confidence = 0.90f
            indicators.add("Material Dialog class: $className")
        }
        // Check Chip
        else if (CHIP_CLASSES.any { className.contains(it) }) {
            component = MaterialComponent.CHIP
            confidence = 0.95f
            indicators.add("Material Chip class: $className")
        }
        // Fallback: Check resource ID patterns
        else {
            for ((compType, patterns) in MATERIAL_RESOURCE_PATTERNS) {
                if (patterns.any { resourceId.contains(it, ignoreCase = true) }) {
                    component = compType
                    confidence = 0.60f
                    indicators.add("Material pattern in resource ID: $resourceId")
                    break
                }
            }
        }

        return if (component != MaterialComponent.UNKNOWN && confidence > 0f) {
            MaterialComponentMatch(component, confidence, node, indicators)
        } else {
            null
        }
    }

    /**
     * Recursively traverse tree and detect Material components
     */
    private fun traverseAndDetect(
        node: AccessibilityNodeInfo,
        matches: MutableList<MaterialComponentMatch>
    ) {
        // Check current node
        detectMaterialComponent(node)?.let { matches.add(it) }

        // Traverse children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                traverseAndDetect(child, matches)
            }
        }
    }
}
