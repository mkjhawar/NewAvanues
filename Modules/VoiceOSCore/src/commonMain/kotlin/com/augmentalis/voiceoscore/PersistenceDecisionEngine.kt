/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * PersistenceDecisionEngine.kt - Unified persistence decision making
 *
 * Part of VoiceOSCore Hybrid Persistence System (Phase 1.9).
 * Combines all 4 classification layers (App, Container, Screen, Content)
 * to make intelligent persistence decisions for UI elements.
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-22
 * Related: AppCategoryClassifier, ContainerClassifier, ScreenClassifier, ContentAnalyzer
 *
 * ## Decision Rules (applied in order):
 *
 * 1. ALWAYS_DYNAMIC containers NEVER persist
 * 2. Settings/System apps persist (unless dynamic patterns detected)
 * 3. Settings screens persist (any app)
 * 4. Form screens persist short content
 * 5. Email/Messaging with ScrollView - context dependent
 * 6. Unknown apps - stability threshold
 *
 * @since 2.1.0 (Hybrid Persistence)
 */

package com.augmentalis.voiceoscore

/**
 * Aggregates all classification inputs needed for a persistence decision.
 *
 * This context object combines outputs from all 4 classification layers,
 * providing a comprehensive view of an element's persistence characteristics.
 *
 * ## Usage
 *
 * ```kotlin
 * val context = PersistenceContext(
 *     element = elementInfo,
 *     appCategory = AppCategory.EMAIL,
 *     containerBehavior = ContainerBehavior.CONDITIONALLY_DYNAMIC,
 *     screenType = ScreenType.LIST_SCREEN,
 *     contentSignal = ContentSignal(TextLength.LONG, hasResourceId = false, hasDynamicPatterns = true, stabilityScore = 35)
 * )
 * val decision = PersistenceDecisionEngine.decide(context)
 * ```
 *
 * @property element The UI element being evaluated
 * @property appCategory Category of the host application (from AppCategoryClassifier)
 * @property containerBehavior Behavior of the element's container (from ContainerClassifier)
 * @property screenType Type of screen the element is on (from ScreenClassifier)
 * @property contentSignal Content analysis signals (from ContentAnalyzer)
 */
data class PersistenceContext(
    val element: ElementInfo,
    val appCategory: AppCategory,
    val containerBehavior: ContainerBehavior,
    val screenType: ScreenType,
    val contentSignal: ContentSignal
)

/**
 * The result of a persistence decision with explanation.
 *
 * Provides not just a boolean decision but also:
 * - Human-readable explanation for debugging/logging
 * - Which rule was decisive (for analytics and tuning)
 * - Confidence level (for borderline cases)
 *
 * ## Confidence Levels
 *
 * - **1.0**: Rule is definitive (e.g., ALWAYS_DYNAMIC container)
 * - **0.8-0.9**: High confidence (e.g., Settings app without dynamic patterns)
 * - **0.6-0.7**: Medium confidence (stability threshold decisions)
 * - **0.5 or below**: Low confidence (edge cases, may need tuning)
 *
 * @property shouldPersist Whether the element should be persisted to database
 * @property reason Human-readable explanation of the decision
 * @property ruleApplied Which rule (1-6) was decisive in making this decision
 * @property confidence Confidence level of the decision (0.0 to 1.0)
 */
data class PersistenceDecision(
    val shouldPersist: Boolean,
    val reason: String,
    val ruleApplied: Int,
    val confidence: Float
) {
    companion object {
        /**
         * Pre-defined decision for ALWAYS_DYNAMIC containers.
         * Rule 1 - highest priority, definitive rejection.
         */
        val DYNAMIC_CONTAINER = PersistenceDecision(
            shouldPersist = false,
            reason = "Element is in an ALWAYS_DYNAMIC container (RecyclerView/ListView) - children never persist",
            ruleApplied = 1,
            confidence = 1.0f
        )

        /**
         * Pre-defined decision for elements without actionable content.
         * Not a numbered rule - pre-filter for non-actionable elements.
         */
        val NOT_ACTIONABLE = PersistenceDecision(
            shouldPersist = false,
            reason = "Element is not actionable (not clickable or scrollable)",
            ruleApplied = 0,
            confidence = 1.0f
        )

        /**
         * Pre-defined decision for elements without voice content.
         * Not a numbered rule - pre-filter for content-less elements.
         */
        val NO_CONTENT = PersistenceDecision(
            shouldPersist = false,
            reason = "Element has no voice-accessible content (no text, description, or resourceId)",
            ruleApplied = 0,
            confidence = 1.0f
        )
    }
}

/**
 * Unified persistence decision engine for the Hybrid Persistence system.
 *
 * Combines all 4 classification layers to make intelligent decisions about
 * whether a UI element should be persisted to the database or kept in memory only.
 *
 * ## Decision Rules
 *
 * The engine applies these rules in order until a decision is reached:
 *
 * | Rule | Condition | Decision |
 * |------|-----------|----------|
 * | 1 | ALWAYS_DYNAMIC container | Never persist |
 * | 2 | Settings/System app | Persist (unless dynamic patterns) |
 * | 3 | Settings screen (any app) | Persist (unless dynamic patterns) |
 * | 4 | Form screen | Persist short content only |
 * | 5 | Email/Messaging/Social app | Persist if (short + resourceId) OR stability > 70 |
 * | 6 | Unknown/Other apps | Persist if stability > 60 AND no dynamic patterns |
 *
 * ## Usage Examples
 *
 * ### Direct decision with pre-built context:
 * ```kotlin
 * val context = PersistenceContext(
 *     element = element,
 *     appCategory = AppCategory.EMAIL,
 *     containerBehavior = ContainerBehavior.STATIC,
 *     screenType = ScreenType.LIST_SCREEN,
 *     contentSignal = ContentAnalyzer.analyze(element)
 * )
 * val decision = PersistenceDecisionEngine.decide(context)
 * if (decision.shouldPersist) {
 *     database.save(element)
 * }
 * ```
 *
 * ### Convenience method with automatic classification:
 * ```kotlin
 * val decision = PersistenceDecisionEngine.decideForElement(
 *     element = buttonElement,
 *     packageName = "com.android.settings",
 *     allElements = screenElements
 * )
 * println("${decision.reason} (Rule ${decision.ruleApplied}, confidence: ${decision.confidence})")
 * ```
 *
 * ## Extending the Engine
 *
 * To add new rules:
 * 1. Add the rule to the `decide()` method in priority order
 * 2. Update `RULE_COUNT` constant
 * 3. Document the rule in KDoc
 * 4. Add test cases for the new rule
 *
 * @see PersistenceContext
 * @see PersistenceDecision
 * @see AppCategoryClassifier
 * @see ContainerClassifier
 * @see ScreenClassifier
 * @see ContentAnalyzer
 */
object PersistenceDecisionEngine {

    // ============================================================
    // Constants
    // ============================================================

    /** Total number of decision rules */
    private const val RULE_COUNT = 6

    /** Stability threshold for unknown apps (Rule 6) */
    private const val UNKNOWN_APP_STABILITY_THRESHOLD = 60

    /** Stability threshold for email/messaging apps (Rule 5) */
    private const val DYNAMIC_APP_STABILITY_THRESHOLD = 70

    // ============================================================
    // App Category Provider (Hybrid 4-Layer)
    // ============================================================

    /**
     * Optional app category provider for 4-layer hybrid classification.
     *
     * When set, uses the provider's hybrid approach (Database → PackageManager
     * → Permissions → Patterns) instead of pattern-only classification.
     *
     * Set this on app startup after initializing the ACD system:
     * ```kotlin
     * PersistenceDecisionEngine.appCategoryProvider = app.appCategoryProvider
     * ```
     */
    var appCategoryProvider: IAppCategoryProvider? = null

    // ============================================================
    // Public API
    // ============================================================

    /**
     * Makes a persistence decision based on a pre-built context.
     *
     * This is the core decision method that applies all rules in order.
     * Use this when you've already classified the element through all layers.
     *
     * @param context The complete classification context
     * @return A [PersistenceDecision] with the result and explanation
     *
     * ## Example
     * ```kotlin
     * val context = buildContext(element, "com.google.android.gmail", allElements)
     * val decision = decide(context)
     * when {
     *     decision.shouldPersist -> persistToDatabase(element)
     *     else -> keepInMemoryOnly(element)
     * }
     * ```
     */
    fun decide(context: PersistenceContext): PersistenceDecision {
        val element = context.element
        val appCategory = context.appCategory
        val containerBehavior = context.containerBehavior
        val screenType = context.screenType
        val contentSignal = context.contentSignal

        // Pre-filter: Check if element is actionable and has content
        if (!element.isActionable) {
            return PersistenceDecision.NOT_ACTIONABLE
        }
        if (!element.hasVoiceContent) {
            return PersistenceDecision.NO_CONTENT
        }

        // ============================================================
        // Rule 1: Always-dynamic containers NEVER persist
        // ============================================================
        if (containerBehavior == ContainerBehavior.ALWAYS_DYNAMIC) {
            return PersistenceDecision.DYNAMIC_CONTAINER
        }

        // ============================================================
        // Rule 2: Settings/System apps persist (unless dynamic patterns)
        // ============================================================
        if (appCategory == AppCategory.SETTINGS || appCategory == AppCategory.SYSTEM) {
            return if (contentSignal.hasDynamicPatterns) {
                PersistenceDecision(
                    shouldPersist = false,
                    reason = "Settings/System app but element contains dynamic patterns (timestamps, counters)",
                    ruleApplied = 2,
                    confidence = 0.85f
                )
            } else {
                PersistenceDecision(
                    shouldPersist = true,
                    reason = "Settings/System app - UI is typically static and safe to persist",
                    ruleApplied = 2,
                    confidence = 0.95f
                )
            }
        }

        // ============================================================
        // Rule 3: Settings screens persist (any app)
        // ============================================================
        if (screenType == ScreenType.SETTINGS_SCREEN) {
            return if (contentSignal.hasDynamicPatterns) {
                PersistenceDecision(
                    shouldPersist = false,
                    reason = "Settings screen but element contains dynamic patterns",
                    ruleApplied = 3,
                    confidence = 0.80f
                )
            } else {
                PersistenceDecision(
                    shouldPersist = true,
                    reason = "Settings screen detected - typically static UI elements",
                    ruleApplied = 3,
                    confidence = 0.90f
                )
            }
        }

        // ============================================================
        // Rule 4: Form screens persist short content
        // ============================================================
        if (screenType == ScreenType.FORM_SCREEN) {
            return when (contentSignal.textLength) {
                TextLength.LONG -> PersistenceDecision(
                    shouldPersist = false,
                    reason = "Form screen but element has long text (likely dynamic content or instructions)",
                    ruleApplied = 4,
                    confidence = 0.75f
                )
                TextLength.SHORT, TextLength.MEDIUM -> PersistenceDecision(
                    shouldPersist = true,
                    reason = "Form screen with ${contentSignal.textLength.name.lowercase()} content - likely stable form field",
                    ruleApplied = 4,
                    confidence = if (contentSignal.textLength == TextLength.SHORT) 0.90f else 0.80f
                )
            }
        }

        // ============================================================
        // Rule 5: Email/Messaging/Social with ScrollView - context dependent
        // ============================================================
        if (appCategory in listOf(AppCategory.EMAIL, AppCategory.MESSAGING, AppCategory.SOCIAL)) {
            // Dynamic patterns (timestamps, status indicators) are a strong signal
            // of temporal content — reject before navigation/stability checks
            if (contentSignal.hasDynamicPatterns) {
                return PersistenceDecision(
                    shouldPersist = false,
                    reason = "Dynamic app (${appCategory.name}) - element contains dynamic patterns (timestamps, status indicators)",
                    ruleApplied = 5,
                    confidence = 0.85f
                )
            }

            // Check for stable navigation elements
            val isNavigationElement = screenType == ScreenType.NAVIGATION_SCREEN ||
                    (contentSignal.textLength == TextLength.SHORT && contentSignal.hasResourceId)

            // Check stability threshold
            val meetsStabilityThreshold = contentSignal.stabilityScore > DYNAMIC_APP_STABILITY_THRESHOLD

            return if (isNavigationElement || meetsStabilityThreshold) {
                val reason = when {
                    isNavigationElement && meetsStabilityThreshold ->
                        "Dynamic app but element is navigation with high stability (${contentSignal.stabilityScore})"
                    isNavigationElement ->
                        "Dynamic app but element appears to be stable navigation (short text + resourceId)"
                    else ->
                        "Dynamic app but element has high stability score (${contentSignal.stabilityScore} > $DYNAMIC_APP_STABILITY_THRESHOLD)"
                }
                PersistenceDecision(
                    shouldPersist = true,
                    reason = reason,
                    ruleApplied = 5,
                    confidence = when {
                        isNavigationElement && meetsStabilityThreshold -> 0.85f
                        isNavigationElement -> 0.75f
                        else -> 0.70f
                    }
                )
            } else {
                PersistenceDecision(
                    shouldPersist = false,
                    reason = "Dynamic app (${appCategory.name}) - element doesn't meet stability criteria (score: ${contentSignal.stabilityScore})",
                    ruleApplied = 5,
                    confidence = 0.80f
                )
            }
        }

        // ============================================================
        // Rule 6: Unknown/Other apps - stability threshold
        // ============================================================
        val meetsStabilityThreshold = contentSignal.stabilityScore > UNKNOWN_APP_STABILITY_THRESHOLD
        val noDynamicPatterns = !contentSignal.hasDynamicPatterns

        return if (meetsStabilityThreshold && noDynamicPatterns) {
            PersistenceDecision(
                shouldPersist = true,
                reason = "Element meets stability threshold (${contentSignal.stabilityScore} > $UNKNOWN_APP_STABILITY_THRESHOLD) with no dynamic patterns",
                ruleApplied = 6,
                confidence = calculateRule6Confidence(contentSignal.stabilityScore)
            )
        } else {
            val reasons = mutableListOf<String>()
            if (!meetsStabilityThreshold) {
                reasons.add("stability score ${contentSignal.stabilityScore} <= $UNKNOWN_APP_STABILITY_THRESHOLD")
            }
            if (!noDynamicPatterns) {
                reasons.add("contains dynamic patterns")
            }
            PersistenceDecision(
                shouldPersist = false,
                reason = "Element doesn't meet persistence criteria: ${reasons.joinToString(", ")}",
                ruleApplied = 6,
                confidence = if (contentSignal.stabilityScore < 40) 0.85f else 0.70f
            )
        }
    }

    /**
     * Convenience method that builds context and makes a decision in one call.
     *
     * This method automatically classifies the element through all 4 layers
     * and then makes a persistence decision. Use this when you have raw inputs
     * rather than pre-classified data.
     *
     * @param element The UI element to evaluate
     * @param packageName The package name of the host application
     * @param allElements All elements on the current screen (for screen classification)
     * @return A [PersistenceDecision] with the result and explanation
     *
     * ## Example
     * ```kotlin
     * // In your scraping code:
     * val elements = scrapeCurrentScreen()
     * for (element in elements) {
     *     val decision = PersistenceDecisionEngine.decideForElement(
     *         element = element,
     *         packageName = currentPackage,
     *         allElements = elements
     *     )
     *     if (decision.shouldPersist) {
     *         commandRegistry.persist(element)
     *     } else {
     *         memoryCache.store(element)
     *     }
     * }
     * ```
     */
    fun decideForElement(
        element: ElementInfo,
        packageName: String,
        allElements: List<ElementInfo>
    ): PersistenceDecision {
        val context = buildContext(element, packageName, allElements)
        return decide(context)
    }

    /**
     * Builds a complete [PersistenceContext] from raw inputs.
     *
     * Runs all 4 classifiers to produce a unified context for decision making.
     * This is useful when you need to inspect the classification results
     * before making a decision.
     *
     * @param element The UI element to classify
     * @param packageName The package name of the host application
     * @param allElements All elements on the current screen
     * @return A complete [PersistenceContext] ready for decision making
     *
     * ## Example
     * ```kotlin
     * val context = PersistenceDecisionEngine.buildContext(element, packageName, allElements)
     *
     * // Inspect classifications
     * println("App: ${context.appCategory}, Screen: ${context.screenType}")
     * println("Container: ${context.containerBehavior}, Stability: ${context.contentSignal.stabilityScore}")
     *
     * // Make decision
     * val decision = PersistenceDecisionEngine.decide(context)
     * ```
     */
    fun buildContext(
        element: ElementInfo,
        packageName: String,
        allElements: List<ElementInfo>
    ): PersistenceContext {
        // Layer 1: App Category Classification (Hybrid 4-Layer when provider available)
        val appCategory = appCategoryProvider?.getCategory(packageName)
            ?: AppCategoryClassifier.classifyByPattern(packageName)

        // Layer 2: Container Behavior Classification
        val containerBehavior = classifyElementContainer(element)

        // Layer 3: Screen Type Classification
        val screenType = ScreenClassifier.classifyScreen(allElements, packageName)

        // Layer 4: Content Analysis
        val contentSignal = ContentAnalyzer.analyze(element)

        return PersistenceContext(
            element = element,
            appCategory = appCategory,
            containerBehavior = containerBehavior,
            screenType = screenType,
            contentSignal = contentSignal
        )
    }

    /**
     * Batch process multiple elements and return decisions.
     *
     * Efficiently processes multiple elements by reusing app and screen
     * classifications (which are the same for all elements on a screen).
     *
     * @param elements The UI elements to evaluate
     * @param packageName The package name of the host application
     * @return Map of element to its persistence decision
     *
     * ## Example
     * ```kotlin
     * val elements = scrapeCurrentScreen()
     * val decisions = PersistenceDecisionEngine.batchDecide(elements, packageName)
     *
     * val toPersist = decisions.filter { it.value.shouldPersist }.keys
     * val toCache = decisions.filter { !it.value.shouldPersist }.keys
     *
     * database.saveAll(toPersist)
     * memoryCache.storeAll(toCache)
     * ```
     */
    fun batchDecide(
        elements: List<ElementInfo>,
        packageName: String
    ): Map<ElementInfo, PersistenceDecision> {
        if (elements.isEmpty()) return emptyMap()

        // Compute shared classifications once (Hybrid 4-Layer when provider available)
        val appCategory = appCategoryProvider?.getCategory(packageName)
            ?: AppCategoryClassifier.classifyByPattern(packageName)
        val screenType = ScreenClassifier.classifyScreen(elements, packageName)

        // Process each element
        return elements.associateWith { element ->
            val containerBehavior = classifyElementContainer(element)
            val contentSignal = ContentAnalyzer.analyze(element)

            val context = PersistenceContext(
                element = element,
                appCategory = appCategory,
                containerBehavior = containerBehavior,
                screenType = screenType,
                contentSignal = contentSignal
            )

            decide(context)
        }
    }

    /**
     * Returns statistics about a batch of decisions.
     *
     * Useful for debugging and analytics to understand how decisions
     * are being distributed across rules.
     *
     * @param decisions Map of decisions from [batchDecide]
     * @return Human-readable summary of decision statistics
     */
    fun summarizeDecisions(decisions: Map<ElementInfo, PersistenceDecision>): String {
        if (decisions.isEmpty()) return "No decisions to summarize"

        val total = decisions.size
        val persistCount = decisions.values.count { it.shouldPersist }
        val skipCount = total - persistCount

        val byRule = decisions.values.groupBy { it.ruleApplied }
            .mapValues { it.value.size }
            .toList().sortedBy { it.first }.toMap()

        val avgConfidence = decisions.values.map { it.confidence }.average()

        return buildString {
            appendLine("=== Persistence Decision Summary ===")
            appendLine("Total elements: $total")
            appendLine("Persist: $persistCount (${(persistCount * 100 / total)}%)")
            appendLine("Skip: $skipCount (${(skipCount * 100 / total)}%)")
            appendLine("Average confidence: ${(avgConfidence * 100).toLong() / 100.0}")
            appendLine()
            appendLine("Decisions by rule:")
            byRule.forEach { (rule, count) ->
                val ruleName = getRuleName(rule)
                appendLine("  Rule $rule ($ruleName): $count")
            }
        }
    }

    // ============================================================
    // Private Helpers
    // ============================================================

    /**
     * Classifies the container behavior for an element.
     *
     * Uses the element's containerType if available, otherwise checks
     * if the element is marked as being in a dynamic container.
     */
    private fun classifyElementContainer(element: ElementInfo): ContainerBehavior {
        // If we have explicit container type, use it
        if (element.containerType.isNotBlank()) {
            return ContainerClassifier.classifyContainer(element.containerType)
        }

        // If marked as in dynamic container, treat as ALWAYS_DYNAMIC
        if (element.isInDynamicContainer) {
            return ContainerBehavior.ALWAYS_DYNAMIC
        }

        // Default to STATIC
        return ContainerBehavior.STATIC
    }

    /**
     * Calculates confidence for Rule 6 based on stability score.
     *
     * Higher stability scores get higher confidence, but we cap at 0.80
     * since Rule 6 is the fallback rule with inherent uncertainty.
     */
    private fun calculateRule6Confidence(stabilityScore: Int): Float {
        return when {
            stabilityScore >= 90 -> 0.80f
            stabilityScore >= 80 -> 0.75f
            stabilityScore >= 70 -> 0.70f
            else -> 0.65f
        }
    }

    /**
     * Returns a human-readable name for a rule number.
     */
    private fun getRuleName(ruleNumber: Int): String {
        return when (ruleNumber) {
            0 -> "Pre-filter"
            1 -> "Dynamic Container"
            2 -> "Settings/System App"
            3 -> "Settings Screen"
            4 -> "Form Screen"
            5 -> "Email/Messaging/Social"
            6 -> "Stability Threshold"
            else -> "Unknown"
        }
    }
}
