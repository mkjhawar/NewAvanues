/**
 * StateDetectionHelpers.kt - Helper functions for state detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 01:41:12 PDT
 *
 * Provides utility functions for analyzing UI components and detecting frameworks.
 */
package com.augmentalis.voiceoscore.learnapp.state

/**
 * Helper functions for state detection
 */
object StateDetectionHelpers {

    /**
     * Extract the resource ID name from a fully qualified view ID
     *
     * Examples:
     * - "com.example.app:id/btn_login" -> "btn_login"
     * - "android:id/button1" -> "button1"
     *
     * @param resourceId Fully qualified resource ID
     * @return Simple resource name, or original string if not in expected format
     */
    fun extractResourceIdName(resourceId: String): String {
        return resourceId.substringAfterLast('/', resourceId)
    }

    /**
     * Check if UI contains web content (WebView)
     *
     * @param classNames List of class names from accessibility tree
     * @return true if web content is detected
     */
    fun isWebContent(classNames: List<String>): Boolean {
        return classNames.any { className ->
            StateDetectionPatterns.WEBVIEW_CLASSES.any { it in className }
        }
    }

    /**
     * Check if UI is Jetpack Compose-based
     *
     * @param classNames List of class names from accessibility tree
     * @return true if Compose UI is detected
     */
    fun isComposeUI(classNames: List<String>): Boolean {
        return classNames.any { className ->
            StateDetectionPatterns.COMPOSE_UI_PATTERNS.any { className.startsWith(it) }
        }
    }

    /**
     * Detect the UI framework being used
     *
     * @param classNames List of class names from accessibility tree
     * @return UI framework type (Jetpack Compose, WebView, or Traditional Views)
     */
    fun detectUIFramework(classNames: List<String>): UIFramework {
        return when {
            isComposeUI(classNames) -> UIFramework.JETPACK_COMPOSE
            isWebContent(classNames) -> UIFramework.WEBVIEW
            else -> UIFramework.TRADITIONAL_VIEWS
        }
    }

    /**
     * Check if a framework-specific dialog class is present
     *
     * @param classNames List of class names from accessibility tree
     * @return true if framework dialog class detected
     */
    fun isFrameworkDialog(classNames: List<String>): Boolean {
        return classNames.any { className ->
            StateDetectionPatterns.DIALOG_FRAMEWORK_CLASSES.any { it in className }
        }
    }

    /**
     * Check if a framework-specific progress indicator class is present
     *
     * @param classNames List of class names from accessibility tree
     * @return true if framework progress indicator detected
     */
    fun isFrameworkLoading(classNames: List<String>): Boolean {
        return classNames.any { className ->
            StateDetectionPatterns.PROGRESS_FRAMEWORK_CLASSES.any { it in className }
        }
    }

    /**
     * Count pattern matches in a list of strings
     *
     * @param items List of strings to search
     * @param patterns Set of patterns to match
     * @param ignoreCase Whether to ignore case when matching
     * @return Number of items that contain at least one pattern
     */
    fun countPatternMatches(
        items: List<String>,
        patterns: Set<String>,
        ignoreCase: Boolean = true
    ): Int {
        return items.count { item ->
            patterns.any { pattern ->
                item.contains(pattern, ignoreCase = ignoreCase)
            }
        }
    }

    /**
     * Find all patterns that match in a list of strings
     *
     * @param items List of strings to search
     * @param patterns Set of patterns to match
     * @param ignoreCase Whether to ignore case when matching
     * @return List of patterns that were found
     */
    fun findMatchingPatterns(
        items: List<String>,
        patterns: Set<String>,
        ignoreCase: Boolean = true
    ): List<String> {
        return patterns.filter { pattern ->
            items.any { item ->
                item.contains(pattern, ignoreCase = ignoreCase)
            }
        }
    }
}

/**
 * UI framework types
 */
enum class UIFramework {
    TRADITIONAL_VIEWS,
    JETPACK_COMPOSE,
    WEBVIEW
}
