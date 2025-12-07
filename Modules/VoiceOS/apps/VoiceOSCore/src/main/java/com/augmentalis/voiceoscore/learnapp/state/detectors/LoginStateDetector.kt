/**
 * LoginStateDetector.kt - Detects login/authentication screens
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13 02:35:00 PDT
 *
 * Detects login and authentication screens using multiple signals:
 * - Login-related keywords in text
 * - Login-related resource IDs (username, password fields)
 * - Material input fields (TextInputLayout)
 * - EditText fields for credentials (2+ required)
 * - Login/Sign-in buttons
 * - Negative: Many list/scroll containers = not login
 */
package com.augmentalis.voiceoscore.learnapp.state.detectors

import com.augmentalis.voiceoscore.learnapp.state.AppState
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionContext
import com.augmentalis.voiceoscore.learnapp.state.StateDetectionPatterns
import com.augmentalis.voiceoscore.learnapp.state.StateDetector

/**
 * Detector for login/authentication screens
 *
 * Identifies login screens by analyzing multiple patterns including
 * text keywords, resource IDs, input fields, and button presence.
 */
class LoginStateDetector : BaseStateDetector(AppState.LOGIN) {

    override fun detectSpecific(
        context: StateDetectionContext,
        indicators: MutableList<String>,
        score: Float
    ): Float {
        var currentScore = score

        // 1. Text keywords - login-related text
        val textResult = textMatcher.match(context, StateDetectionPatterns.LOGIN_KEYWORDS)
        if (textResult.matchCount > 0) {
            val textScore = StateDetector.WEIGHT_TEXT_KEYWORD * textResult.score
            currentScore += textScore
            indicators.add("${textResult.matchCount} login keywords (score: +${String.format("%.2f", textScore)})")
        }

        // 2. Resource IDs - login field IDs
        val idResult = idMatcher.match(context, StateDetectionPatterns.LOGIN_VIEW_ID_PATTERNS)
        if (idResult.matchCount > 0) {
            val idScore = StateDetector.WEIGHT_RESOURCE_ID * idResult.score
            currentScore += idScore
            indicators.add("${idResult.matchCount} login view IDs (score: +${String.format("%.2f", idScore)})")

            // Boost for 2+ login IDs (username + password)
            if (idResult.matchCount >= 2) {
                currentScore += 0.05f
                indicators.add("Multiple login fields detected (likely username + password)")
            }
        }

        // 3. EditText fields - need 2+ for credentials
        val editTextCount = context.classNames.count { it.contains("EditText") }
        if (editTextCount >= 2) {
            val editScore = StateDetector.WEIGHT_CLASS_NAME + 0.1f
            currentScore += editScore
            indicators.add("$editTextCount EditText fields (score: +${String.format("%.2f", editScore)})")
        }

        // 4. Material input fields - higher quality indicator
        val materialInputCount = context.classNames.count { className ->
            StateDetectionPatterns.MATERIAL_INPUT_CLASSES.any { it in className }
        }
        if (materialInputCount >= 2) {
            val materialScore = StateDetector.WEIGHT_CLASS_NAME + 0.05f
            currentScore += materialScore
            indicators.add("$materialInputCount Material input fields (score: +${String.format("%.2f", materialScore)})")
        }

        // 5. Login button - text + Button class
        val hasLoginButton = context.textContent.any { text ->
            (text.contains("login", ignoreCase = true) ||
             text.contains("sign in", ignoreCase = true) ||
             text.contains("sign up", ignoreCase = true))
        } && context.classNames.any { it.contains("Button") }

        if (hasLoginButton) {
            val buttonScore = StateDetector.WEIGHT_CONTEXTUAL + 0.05f
            currentScore += buttonScore
            indicators.add("Login/Sign-in button detected (score: +${String.format("%.2f", buttonScore)})")
        }

        // 6. Negative indicator - many list/scroll containers
        val scrollableCount = context.classNames.count { className ->
            className.contains("RecyclerView") ||
            className.contains("ListView") ||
            className.contains("ScrollView") && !className.contains("NestedScrollView")
        }
        if (scrollableCount > 3) {
            currentScore *= 0.6f
            indicators.add("Many scrollable containers detected - confidence reduced")
        }

        // 7. Boost for multiple strong signals
        if (idResult.matchCount >= 2 && editTextCount >= 2 && hasLoginButton) {
            currentScore += 0.1f
            indicators.add("Multiple strong login indicators - very high confidence")
        }

        return currentScore
    }
}
