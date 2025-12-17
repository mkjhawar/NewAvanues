/**
 * ElementClassifier.kt - Classifies UI elements for exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Classifies UI elements by type and interaction capability.
 */
package com.augmentalis.voiceoscore.learnapp.exploration

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.models.ElementClassification
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.LoginElementType

/**
 * Element Classifier
 *
 * Classifies UI elements for exploration purposes.
 */
class ElementClassifier {

    /**
     * Classify an element
     */
    fun classifyElement(element: ElementInfo): ElementClassification {
        // Check for login elements first
        val loginType = detectLoginElementType(element)
        if (loginType != null) {
            return ElementClassification.LoginElement(element, loginType)
        }

        // Check for text input
        if (element.isEditText()) {
            return ElementClassification.TextInput(element)
        }

        // Check if clickable
        if (element.isClickable) {
            val dangerReason = getDangerousReason(element)
            return if (dangerReason != null) {
                ElementClassification.DangerousClickable(element, dangerReason)
            } else {
                ElementClassification.SafeClickable(element)
            }
        }

        // Non-clickable element
        return ElementClassification.NonClickable(element)
    }

    /**
     * Classify element from AccessibilityNodeInfo
     */
    fun classifyNode(node: AccessibilityNodeInfo): ElementClassification {
        val element = ElementInfo.fromNode(node)
        return classifyElement(element)
    }

    /**
     * Classify a list of elements
     */
    fun classifyElements(elements: List<ElementInfo>): List<ElementClassification> {
        return elements.map { classifyElement(it) }
    }

    /**
     * Check if element is dangerous to interact with
     */
    fun isDangerous(element: ElementInfo): Boolean {
        val text = element.text.lowercase()
        val contentDesc = element.contentDescription.lowercase()
        val combined = "$text $contentDesc"

        return DANGEROUS_KEYWORDS.any { it in combined }
    }

    /**
     * Get dangerous element reason
     */
    fun getDangerousReason(element: ElementInfo): String? {
        val text = element.text.lowercase()
        val contentDesc = element.contentDescription.lowercase()
        val combined = "$text $contentDesc"

        return DANGEROUS_KEYWORDS.firstOrNull { it in combined }?.let {
            "Contains dangerous keyword: $it"
        }
    }

    /**
     * Detect if element is login-related
     */
    private fun detectLoginElementType(element: ElementInfo): LoginElementType? {
        val text = element.text.lowercase()
        val desc = element.contentDescription.lowercase()
        val resourceId = element.resourceId.lowercase()
        val combined = "$text $desc $resourceId"

        return when {
            element.isPassword -> LoginElementType.PASSWORD_FIELD
            combined.contains("username") || combined.contains("email") ||
                    combined.contains("user name") -> LoginElementType.USERNAME_FIELD
            combined.contains("password") || combined.contains("passcode") ->
                LoginElementType.PASSWORD_FIELD
            combined.contains("log in") || combined.contains("login") ||
                    combined.contains("sign in") -> LoginElementType.LOGIN_BUTTON
            combined.contains("sign up") || combined.contains("register") ||
                    combined.contains("create account") -> LoginElementType.SIGNUP_BUTTON
            combined.contains("forgot") || combined.contains("reset password") ->
                LoginElementType.FORGOT_PASSWORD
            combined.contains("google") || combined.contains("facebook") ||
                    combined.contains("apple") -> LoginElementType.SOCIAL_LOGIN
            else -> null
        }
    }

    /**
     * Filter safe clickable elements
     */
    fun filterSafeClickable(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.filter { element ->
            element.isClickable && !isDangerous(element)
        }
    }

    /**
     * Filter dangerous elements
     */
    fun filterDangerous(elements: List<ElementInfo>): List<Pair<ElementInfo, String>> {
        return elements.filter { element ->
            element.isClickable && isDangerous(element)
        }.mapNotNull { element ->
            getDangerousReason(element)?.let { reason ->
                element to reason
            }
        }
    }

    companion object {
        private val DANGEROUS_KEYWORDS = listOf(
            "delete", "remove", "logout", "sign out", "uninstall",
            "clear", "erase", "reset", "format", "wipe", "cancel",
            "discard", "destroy", "terminate"
        )
    }
}
