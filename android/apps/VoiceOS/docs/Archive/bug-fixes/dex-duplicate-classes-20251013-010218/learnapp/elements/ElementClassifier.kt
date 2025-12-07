/**
 * ElementClassifier.kt - Classifies UI elements for exploration
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/elements/ElementClassifier.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Main classifier for determining which elements are safe to explore
 */

package com.augmentalis.learnapp.elements

import com.augmentalis.learnapp.models.ElementClassification
import com.augmentalis.learnapp.models.ElementInfo

/**
 * Element Classifier
 *
 * Classifies UI elements into categories to determine exploration strategy.
 * Uses dangerous element detector and login screen detector.
 *
 * ## Classification Priority
 *
 * 1. Check if disabled → Disabled
 * 2. Check if EditText → EditText
 * 3. Check if dangerous → Dangerous
 * 4. Check if login field → LoginField
 * 5. Check if non-clickable → NonClickable
 * 6. Otherwise → SafeClickable
 *
 * ## Usage Example
 *
 * ```kotlin
 * val classifier = ElementClassifier()
 *
 * val element = ElementInfo(...)
 * val classification = classifier.classify(element)
 *
 * when (classification) {
 *     is ElementClassification.SafeClickable -> {
 *         // Click and explore
 *     }
 *     is ElementClassification.Dangerous -> {
 *         // Skip
 *         println("Skipped: ${classification.reason}")
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 */
class ElementClassifier {

    /**
     * Dangerous element detector
     */
    private val dangerousDetector = DangerousElementDetector()

    /**
     * Login screen detector
     */
    private val loginDetector = LoginScreenDetector()

    /**
     * Classify single element
     *
     * @param element Element to classify
     * @return Classification result
     */
    fun classify(element: ElementInfo): ElementClassification {
        // 1. Check if disabled
        if (!element.isEnabled) {
            return ElementClassification.Disabled(element)
        }

        // 2. Check if EditText (skip text input fields)
        if (element.isEditText()) {
            return ElementClassification.EditText(element)
        }

        // 3. Check if dangerous
        val (isDangerous, reason) = dangerousDetector.isDangerous(element)
        if (isDangerous) {
            return ElementClassification.Dangerous(element, reason)
        }

        // 4. Check if login field
        val loginFieldType = loginDetector.classifyLoginField(element)
        if (loginFieldType != null) {
            return ElementClassification.LoginField(element, loginFieldType)
        }

        // 5. Check if non-clickable
        if (!element.isClickable) {
            return ElementClassification.NonClickable(element)
        }

        // 6. Safe to click
        return ElementClassification.SafeClickable(element)
    }

    /**
     * Classify multiple elements
     *
     * @param elements List of elements
     * @return List of classifications
     */
    fun classifyAll(elements: List<ElementInfo>): List<ElementClassification> {
        return elements.map { classify(it) }
    }

    /**
     * Check if screen is login screen
     *
     * Convenience method delegating to loginDetector.
     *
     * @param elements All elements on screen
     * @return true if login screen
     */
    fun isLoginScreen(elements: List<ElementInfo>): Boolean {
        return loginDetector.isLoginScreen(elements)
    }

    /**
     * Filter to safe clickable elements only
     *
     * @param elements List of elements
     * @return List of safe clickable elements
     */
    fun filterSafeClickable(elements: List<ElementInfo>): List<ElementInfo> {
        return elements
            .map { classify(it) }
            .filterIsInstance<ElementClassification.SafeClickable>()
            .map { it.element }
    }

    /**
     * Get classification statistics
     *
     * @param elements List of elements
     * @return Classification stats
     */
    fun getStats(elements: List<ElementInfo>): ClassificationStats {
        val classifications = classifyAll(elements)

        var safeClickable = 0
        var dangerous = 0
        var editText = 0
        var loginFields = 0
        var nonClickable = 0
        var disabled = 0

        classifications.forEach { classification ->
            when (classification) {
                is ElementClassification.SafeClickable -> safeClickable++
                is ElementClassification.Dangerous -> dangerous++
                is ElementClassification.EditText -> editText++
                is ElementClassification.LoginField -> loginFields++
                is ElementClassification.NonClickable -> nonClickable++
                is ElementClassification.Disabled -> disabled++
            }
        }

        return ClassificationStats(
            total = elements.size,
            safeClickable = safeClickable,
            dangerous = dangerous,
            editText = editText,
            loginFields = loginFields,
            nonClickable = nonClickable,
            disabled = disabled
        )
    }

    /**
     * Get dangerous elements with reasons
     *
     * @param elements List of elements
     * @return List of (element, reason) pairs
     */
    fun getDangerousElements(elements: List<ElementInfo>): List<Pair<ElementInfo, String>> {
        return elements
            .map { classify(it) }
            .filterIsInstance<ElementClassification.Dangerous>()
            .map { it.element to it.reason }
    }
}

/**
 * Classification Statistics
 *
 * @property total Total elements classified
 * @property safeClickable Safe clickable elements
 * @property dangerous Dangerous elements
 * @property editText EditText fields
 * @property loginFields Login screen fields
 * @property nonClickable Non-clickable elements
 * @property disabled Disabled elements
 */
data class ClassificationStats(
    val total: Int,
    val safeClickable: Int,
    val dangerous: Int,
    val editText: Int,
    val loginFields: Int,
    val nonClickable: Int,
    val disabled: Int
) {
    /**
     * Calculate percentage of safe clickable elements
     *
     * @return Percentage (0.0-1.0)
     */
    fun safeClickablePercentage(): Float {
        if (total == 0) return 0f
        return safeClickable.toFloat() / total.toFloat()
    }

    override fun toString(): String {
        return """
            Classification Stats:
            - Total: $total
            - Safe Clickable: $safeClickable (${(safeClickablePercentage() * 100).toInt()}%)
            - Dangerous: $dangerous
            - EditText: $editText
            - Login Fields: $loginFields
            - Non-Clickable: $nonClickable
            - Disabled: $disabled
        """.trimIndent()
    }
}
