/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * SafetyManager.kt - Central coordinator for all safety systems
 *
 * Part of VoiceOSCoreNG Safety System.
 * Coordinates all safety checks before element interaction:
 * - Do Not Click filtering
 * - Dynamic content detection
 * - Login screen detection
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Migrated to KMP: 2026-01-16
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 5
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ElementInfo
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Result of safety check for an element.
 *
 * @property isSafe Whether element is safe to interact with
 * @property reason Reason if not safe
 * @property category Category of safety issue
 * @property recommendation What to do instead
 */
data class SafetyCheckResult(
    val isSafe: Boolean,
    val reason: String? = null,
    val category: SafetyCategory? = null,
    val recommendation: SafetyRecommendation = SafetyRecommendation.PROCEED
) {
    companion object {
        fun safe() = SafetyCheckResult(isSafe = true)

        fun unsafe(
            reason: String,
            category: SafetyCategory,
            recommendation: SafetyRecommendation
        ) = SafetyCheckResult(
            isSafe = false,
            reason = reason,
            category = category,
            recommendation = recommendation
        )
    }
}

/**
 * Category of safety issue.
 */
enum class SafetyCategory {
    /** Element on Do Not Click list */
    DO_NOT_CLICK,

    /** Element inside dynamic content region */
    DYNAMIC_CONTENT,

    /** Element on login screen */
    LOGIN_SCREEN,

    /** Element is password field */
    PASSWORD_FIELD,

    /** Screen has been visited too many times */
    LOOP_DETECTED
}

/**
 * Recommended action for safety issue.
 */
enum class SafetyRecommendation {
    /** Safe to proceed */
    PROCEED,

    /** Skip this element */
    SKIP_ELEMENT,

    /** Log element but don't click */
    LOG_ONLY,

    /** Prompt user for action */
    PROMPT_USER,

    /** Navigate away from screen */
    NAVIGATE_AWAY,

    /** Stop exploration entirely */
    STOP_EXPLORATION
}

/**
 * Safety callback interface for exploration engine.
 */
interface SafetyCallback {
    /**
     * Called when login screen is detected.
     *
     * @param loginType Type of login
     * @param message User-facing message
     */
    fun onLoginDetected(loginType: LoginType, message: String)

    /**
     * Called when dangerous element is found.
     *
     * @param element The dangerous element
     * @param reason Why it's dangerous
     */
    fun onDangerousElement(element: ElementInfo, reason: DoNotClickReason)

    /**
     * Called when dynamic region is confirmed.
     *
     * @param region The dynamic region
     */
    fun onDynamicRegionConfirmed(region: DynamicRegion)

    /**
     * Called when potential loop is detected.
     *
     * @param screenHash Screen being visited repeatedly
     * @param visitCount Number of times visited
     */
    fun onLoopDetected(screenHash: String, visitCount: Int)
}

/**
 * Safety Manager - Central coordinator for safety systems
 *
 * Single entry point for all safety checks during exploration.
 * Call [checkElement] before interacting with any element.
 */
class SafetyManager(
    private val callback: SafetyCallback? = null
) {
    // ============================================================
    // Screen visit tracking (loop detection)
    // Thread-safe with synchronized access
    // ============================================================
    private val screenVisits = mutableMapOf<String, Int>()
    private val maxVisitsPerScreen = 3
    private val screenVisitsLock = SynchronizedObject()

    // ============================================================
    // Current state
    // ============================================================
    private var currentPackage: String = ""
    private var currentScreenHash: String = ""
    private var currentActivityName: String? = null
    private var isOnLoginScreen: Boolean = false
    private var lastLoginResult: LoginDetectionResult? = null

    /**
     * Update current screen context.
     *
     * Call when screen changes.
     *
     * @param packageName Current package
     * @param screenHash Current screen hash
     * @param activityName Current activity (optional)
     * @param elements All elements on screen
     */
    fun updateScreenContext(
        packageName: String,
        screenHash: String,
        activityName: String?,
        elements: List<ElementInfo>
    ) {
        currentPackage = packageName
        currentScreenHash = screenHash
        currentActivityName = activityName

        // Track screen visits (synchronized)
        val visits = synchronized(screenVisitsLock) {
            val current = screenVisits.getOrPut(screenHash) { 0 }
            val newCount = current + 1
            screenVisits[screenHash] = newCount
            newCount
        }

        // Check for loop
        if (visits > maxVisitsPerScreen) {
            callback?.onLoopDetected(screenHash, visits)
        }

        // Check for login screen
        lastLoginResult = LoginScreenDetector.detectLoginScreen(elements, activityName)
        isOnLoginScreen = lastLoginResult?.isLoginScreen == true

        if (isOnLoginScreen) {
            val loginType = lastLoginResult?.loginType ?: LoginType.STANDARD
            val message = LoginScreenDetector.getLoginPromptMessage(loginType)
            callback?.onLoginDetected(loginType, message)
        }
    }

    /**
     * Check if an element is safe to interact with.
     *
     * @param element Element to check
     * @return SafetyCheckResult with recommendation
     */
    fun checkElement(element: ElementInfo): SafetyCheckResult {
        // 1. Check password field FIRST (highest priority, specific category)
        if (isPasswordField(element)) {
            return SafetyCheckResult.unsafe(
                reason = "Password input field",
                category = SafetyCategory.PASSWORD_FIELD,
                recommendation = SafetyRecommendation.SKIP_ELEMENT
            )
        }

        // 2. Check login screen elements
        if (isOnLoginScreen && LoginScreenDetector.isAuthElement(element)) {
            return SafetyCheckResult.unsafe(
                reason = "Authentication element on login screen",
                category = SafetyCategory.LOGIN_SCREEN,
                recommendation = SafetyRecommendation.PROMPT_USER
            )
        }

        // 3. Check Do Not Click list (excludes password - already checked above)
        val dncReason = DoNotClickList.shouldNotClick(element)
        if (dncReason != null && dncReason != DoNotClickReason.PASSWORD_FIELD) {
            callback?.onDangerousElement(element, dncReason)
            return SafetyCheckResult.unsafe(
                reason = "Do Not Click: ${dncReason.description}",
                category = SafetyCategory.DO_NOT_CLICK,
                recommendation = SafetyRecommendation.LOG_ONLY
            )
        }

        // 4. Check dynamic content region
        val dynamicRegion = DynamicContentDetector.isInDynamicRegion(
            currentScreenHash,
            element.bounds
        )
        if (dynamicRegion != null) {
            return SafetyCheckResult.unsafe(
                reason = "Element in dynamic region: ${dynamicRegion.changeType.description}",
                category = SafetyCategory.DYNAMIC_CONTENT,
                recommendation = SafetyRecommendation.LOG_ONLY
            )
        }

        // All checks passed
        return SafetyCheckResult.safe()
    }

    /**
     * Filter elements based on safety checks.
     *
     * @param elements Elements to filter
     * @return Pair of (safe elements, unsafe elements with reasons)
     */
    fun filterElements(elements: List<ElementInfo>): Pair<List<ElementInfo>, List<Pair<ElementInfo, SafetyCheckResult>>> {
        val safe = mutableListOf<ElementInfo>()
        val unsafe = mutableListOf<Pair<ElementInfo, SafetyCheckResult>>()

        for (element in elements) {
            val result = checkElement(element)
            if (result.isSafe) {
                safe.add(element)
            } else {
                unsafe.add(element to result)
            }
        }

        return safe to unsafe
    }

    /**
     * Track potential dynamic content.
     *
     * Call when content in a region appears to change.
     *
     * @param regionElement Container element
     * @param children Child elements
     * @return DynamicRegion if confirmed, null otherwise
     */
    fun trackDynamicContent(
        regionElement: ElementInfo,
        children: List<ElementInfo>
    ): DynamicRegion? {
        val suggestedType = DynamicContentDetector.detectPotentialDynamicContainer(regionElement)
            ?: return null

        val fingerprint = DynamicContentDetector.generateContentFingerprint(children)
        val regionId = regionElement.resourceId.ifEmpty {
            "region_${regionElement.bounds.centerX}_${regionElement.bounds.centerY}"
        }

        val result = DynamicContentDetector.trackContentChange(
            screenHash = currentScreenHash,
            regionId = regionId,
            contentFingerprint = fingerprint,
            bounds = regionElement.bounds,
            className = regionElement.className,
            suggestedType = suggestedType
        )

        if (result != null) {
            callback?.onDynamicRegionConfirmed(result)
        }

        return result
    }

    /**
     * Check if current screen is a login screen.
     *
     * @return Login detection result
     */
    fun isLoginScreen(): LoginDetectionResult {
        return lastLoginResult ?: LoginDetectionResult.notLogin()
    }

    /**
     * Check if screen has been visited too many times.
     *
     * @param screenHash Screen to check
     * @return true if loop detected
     */
    fun isLoopDetected(screenHash: String): Boolean {
        return synchronized(screenVisitsLock) {
            (screenVisits[screenHash] ?: 0) > maxVisitsPerScreen
        }
    }

    /**
     * Get visit count for a screen.
     *
     * @param screenHash Screen hash
     * @return Number of visits
     */
    fun getScreenVisits(screenHash: String): Int {
        return synchronized(screenVisitsLock) {
            screenVisits[screenHash] ?: 0
        }
    }

    /**
     * Reset visit tracking for a screen.
     *
     * @param screenHash Screen to reset
     */
    fun resetScreenVisits(screenHash: String) {
        synchronized(screenVisitsLock) {
            screenVisits.remove(screenHash)
        }
    }

    /**
     * Export all safety data as AVU lines.
     *
     * @return Map of line type to lines
     */
    fun exportAvuLines(): Map<String, List<String>> {
        return mapOf(
            "DNC" to DoNotClickList.filterElements(emptyList()).second.map { (element, reason) ->
                DoNotClickList.toDncLine(element, reason)
            },
            "DYN" to DynamicContentDetector.exportDynLines()
        )
    }

    /**
     * Reset all safety state.
     *
     * Call when starting new exploration session.
     */
    fun reset() {
        synchronized(screenVisitsLock) {
            screenVisits.clear()
        }
        currentPackage = ""
        currentScreenHash = ""
        currentActivityName = null
        isOnLoginScreen = false
        lastLoginResult = null

        DynamicContentDetector.reset()
    }

    /**
     * Clear state for specific screen.
     *
     * @param screenHash Screen to clear
     */
    fun clearScreen(screenHash: String) {
        synchronized(screenVisitsLock) {
            screenVisits.remove(screenHash)
        }
        DynamicContentDetector.clearScreen(screenHash)
    }

    /**
     * Check if element is a password field.
     * Uses class name and resource ID heuristics.
     */
    private fun isPasswordField(element: ElementInfo): Boolean {
        val className = element.className.lowercase()
        val resourceId = element.resourceId.lowercase()
        val label = element.voiceLabel.lowercase()

        return className.contains("password") ||
               resourceId.contains("password") ||
               resourceId.contains("pwd") ||
               (label.contains("password") && className.contains("edittext"))
    }

    companion object {
        /**
         * Create SafetyManager with default settings.
         */
        fun create(callback: SafetyCallback? = null): SafetyManager {
            return SafetyManager(callback)
        }
    }
}
