/**
 * StateDetectionStrategy.kt - Interface for state detection strategies
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Strategy pattern interface for detecting different application states.
 * Each state (LOGIN, LOADING, ERROR, etc.) has its own detector implementation.
 *
 * SOLID Principles:
 * - Single Responsibility: Each detector handles one state type
 * - Open/Closed: Add new states without modifying existing code
 * - Dependency Inversion: Depend on abstraction, not concrete classes
 */
package com.augmentalis.voiceoscore.learnapp.state

/**
 * Strategy interface for detecting a specific application state
 *
 * Implementations analyze UI elements to determine if an app is in a particular state
 * (e.g., LOGIN, LOADING, ERROR). Each implementation focuses on one state type.
 *
 * @see LoginStateDetector
 * @see LoadingStateDetector
 * @see ErrorStateDetector
 */
interface StateDetectionStrategy {

    /**
     * The state type this detector identifies
     */
    val targetState: AppState

    /**
     * Detect if the current UI matches this state
     *
     * @param context Detection context containing UI data
     * @return Detection result with confidence score and indicators
     */
    fun detect(context: StateDetectionContext): StateDetectionResult

    /**
     * Get minimum confidence threshold for this state
     *
     * Different states may require different confidence levels.
     * For example, ERROR detection might require higher confidence (0.8)
     * to avoid false alarms, while READY might accept lower confidence (0.6).
     *
     * @return Minimum confidence threshold (0.0-1.0)
     */
    fun getMinimumConfidence(): Float = 0.7f
}

/**
 * Context object containing all data needed for state detection
 *
 * Immutable data class passed to detectors containing all UI information
 * extracted from the accessibility tree.
 *
 * @property textContent All text strings from UI elements
 * @property viewIds All resource IDs from UI elements
 * @property classNames All class names from UI elements
 * @property metadata Additional metadata about the UI state
 */
data class StateDetectionContext(
    val textContent: List<String>,
    val viewIds: List<String>,
    val classNames: List<String>,
    val metadata: UIMetadata = UIMetadata()
) {
    /**
     * Get count of elements matching a predicate
     *
     * Utility method to count how many elements match a condition.
     *
     * @param selector Function to select elements (text, viewIds, or classNames)
     * @param predicate Condition to match
     * @return Count of matching elements
     */
    fun countMatches(
        selector: StateDetectionContext.() -> List<String>,
        predicate: (String) -> Boolean
    ): Int {
        return selector().count(predicate)
    }
}

/**
 * UI metadata extracted from accessibility tree
 *
 * Additional contextual information about the UI that may be useful
 * for state detection beyond text, IDs, and class names.
 *
 * @property elementCount Total number of UI elements
 * @property interactiveCount Number of interactive elements (clickable, etc.)
 * @property hierarchyDepth Maximum depth of UI hierarchy
 * @property hasProgressIndicator Whether any progress indicator is present
 * @property hasEditTextFields Whether any text input fields are present
 */
data class UIMetadata(
    val elementCount: Int = 0,
    val interactiveCount: Int = 0,
    val hierarchyDepth: Int = 0,
    val hasProgressIndicator: Boolean = false,
    val hasEditTextFields: Boolean = false
)
