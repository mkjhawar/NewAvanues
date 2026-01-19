/**
 * AIContext.kt - AI-consumable context data models
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI-Assisted Implementation
 * Created: 2025-12-02
 *
 * Data structures for exporting navigation graph and element data
 * in formats consumable by AI systems (LLMs, agents, automation).
 *
 * Part of Voice Command AI Integration feature
 */
package com.augmentalis.voiceoscore.learnapp.ai

/**
 * AI Context
 *
 * Complete AI-consumable representation of app's navigation structure.
 * Includes screens, elements, and navigation paths.
 *
 * ## Usage:
 * ```kotlin
 * val context = AIContext(
 *     appInfo = AppInfo("com.instagram.android", "Instagram"),
 *     screens = listOf(...),
 *     navigationPaths = listOf(...)
 * )
 * ```
 *
 * @property appInfo Basic app information
 * @property screens List of all discovered screens
 * @property navigationPaths List of screen-to-screen transitions
 * @property stats Graph statistics
 * @property timestamp When context was generated
 */
data class AIContext(
    val appInfo: AppInfo,
    val screens: List<AIScreen>,
    val navigationPaths: List<AINavigationPath>,
    val stats: AIGraphStats,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * App Info
 *
 * Basic app identification information.
 *
 * @property packageName Package name (e.g., com.instagram.android)
 * @property appName Human-readable app name
 */
data class AppInfo(
    val packageName: String,
    val appName: String? = null
)

/**
 * AI Screen
 *
 * Represents a single screen with its actionable elements.
 *
 * @property screenHash Unique screen identifier
 * @property activityName Android activity name (if available)
 * @property elements List of actionable elements on this screen
 * @property discoveredAt When screen was discovered
 */
data class AIScreen(
    val screenHash: String,
    val activityName: String? = null,
    val elements: List<AIElement>,
    val discoveredAt: Long
)

/**
 * AI Element
 *
 * Actionable element on a screen with all properties needed for AI decision-making.
 *
 * @property uuid Stable element identifier
 * @property label User-visible label (text or contentDescription)
 * @property type Element type (button, textField, etc.)
 * @property actions Available actions (click, longClick, edit, scroll)
 * @property location Screen position (for spatial reasoning)
 */
data class AIElement(
    val uuid: String,
    val label: String,
    val type: String,
    val actions: List<String>,
    val location: AILocation? = null
)

/**
 * AI Location
 *
 * Screen position for spatial reasoning.
 *
 * @property left Left coordinate
 * @property top Top coordinate
 * @property right Right coordinate
 * @property bottom Bottom coordinate
 */
data class AILocation(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

/**
 * AI Navigation Path
 *
 * Represents a navigation transition from one screen to another.
 *
 * @property fromScreen Source screen hash
 * @property toScreen Destination screen hash
 * @property triggerElement Element that triggers the navigation
 * @property timestamp When transition was discovered
 */
data class AINavigationPath(
    val fromScreen: String,
    val toScreen: String,
    val triggerElement: AITriggerElement,
    val timestamp: Long
)

/**
 * AI Trigger Element
 *
 * Element that triggers a navigation transition.
 *
 * @property uuid Element UUID
 * @property label Element label
 */
data class AITriggerElement(
    val uuid: String,
    val label: String
)

/**
 * AI Graph Stats
 *
 * Statistics about the navigation graph for AI context.
 *
 * @property totalScreens Total number of screens
 * @property totalElements Total number of actionable elements
 * @property totalPaths Total number of navigation paths
 * @property averageElementsPerScreen Average elements per screen
 * @property maxDepth Maximum navigation depth from root
 * @property coverage Estimated coverage percentage (0-100)
 */
data class AIGraphStats(
    val totalScreens: Int,
    val totalElements: Int,
    val totalPaths: Int,
    val averageElementsPerScreen: Float,
    val maxDepth: Int,
    val coverage: Float = 0f
)
