package com.augmentalis.chat.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * State of the NLU system for visual feedback.
 *
 * State transitions:
 * NOT_LOADED → INITIALIZING → READY ⇄ ACTIVE
 */
enum class NLUState {
    NOT_LOADED,     // Gray (38% opacity) - Model not in memory
    INITIALIZING,   // Gray + Spinner - Model loading (0-25s)
    READY,          // Red, Bold - Model loaded, idle
    ACTIVE          // Red + Glow (2s) - Just provided response
}

/**
 * State of the LLM system for visual feedback.
 *
 * State transitions:
 * NOT_LOADED → INITIALIZING → READY ⇄ ACTIVE
 */
enum class LLMState {
    NOT_LOADED,     // Gray (38% opacity) - Model not in memory
    INITIALIZING,   // Gray + Spinner - Model loading
    READY,          // White/Theme, Bold - Model loaded, idle
    ACTIVE          // Blue Glow (2s) - Just provided response
}

/**
 * Status indicator component that shows the state of NLU and LLM systems.
 *
 * Enhanced Visual Feedback (REQ-001, REQ-002):
 * - 4 states per system: NOT_LOADED, INITIALIZING, READY, ACTIVE
 * - "AVA" (NLU): Gray → Gray+Spinner → Red → Red+Glow
 * - "AI" (LLM): Gray → Gray+Spinner → White → Blue+Glow
 * - 2-second glow highlight after system provides response
 * - Small spinner during initialization
 *
 * This provides users with immediate visual feedback about:
 * 1. Which systems are loaded and available
 * 2. Which systems are initializing
 * 3. Which system just provided the answer
 *
 * @param nluState Current state of the NLU system
 * @param llmState Current state of the LLM system
 * @param isTestingModeEnabled If true, shows flash animation during processing (developer mode)
 * @param modifier Optional modifier for this composable
 */
@Composable
fun StatusIndicator(
    nluState: NLUState,
    llmState: LLMState,
    isTestingModeEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AVA (NLU status)
        SystemIndicator(
            text = "AVA",
            state = when (nluState) {
                NLUState.NOT_LOADED -> SystemIndicatorState.NOT_LOADED
                NLUState.INITIALIZING -> SystemIndicatorState.INITIALIZING
                NLUState.READY -> SystemIndicatorState.READY
                NLUState.ACTIVE -> SystemIndicatorState.ACTIVE
            },
            activeColor = Color(0xFFE53935), // Material Red 600
            isTestingMode = isTestingModeEnabled
        )

        // AI (LLM status)
        SystemIndicator(
            text = "AI",
            state = when (llmState) {
                LLMState.NOT_LOADED -> SystemIndicatorState.NOT_LOADED
                LLMState.INITIALIZING -> SystemIndicatorState.INITIALIZING
                LLMState.READY -> SystemIndicatorState.READY
                LLMState.ACTIVE -> SystemIndicatorState.ACTIVE
            },
            activeColor = Color(0xFF2196F3), // Material Blue 500
            isTestingMode = isTestingModeEnabled
        )
    }
}

/**
 * Internal state representation for SystemIndicator.
 */
private enum class SystemIndicatorState {
    NOT_LOADED,
    INITIALIZING,
    READY,
    ACTIVE
}

/**
 * Individual system indicator (AVA or AI).
 *
 * @param text Display text ("AVA" or "AI")
 * @param state Current state
 * @param activeColor Color when ready or active
 * @param isTestingMode If true, shows additional visual feedback
 */
@Composable
private fun SystemIndicator(
    text: String,
    state: SystemIndicatorState,
    activeColor: Color,
    isTestingMode: Boolean
) {
    // Animate glow effect for ACTIVE state
    val glowAlpha by animateFloatAsState(
        targetValue = if (state == SystemIndicatorState.ACTIVE) 0.3f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "${text}_glow"
    )

    // Flash animation for testing mode (REQ-007)
    val flashAlpha by animateFloatAsState(
        targetValue = if (isTestingMode && state == SystemIndicatorState.ACTIVE) 1.0f else 0.5f,
        animationSpec = if (isTestingMode && state == SystemIndicatorState.ACTIVE) {
            infiniteRepeatable(
                animation = tween(durationMillis = 300)
            )
        } else {
            tween(durationMillis = 300)
        },
        label = "${text}_flash"
    )

    Box(
        contentAlignment = Alignment.Center
    ) {
        // Background glow when ACTIVE
        if (state == SystemIndicatorState.ACTIVE) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 4.dp)
                    .alpha(glowAlpha)
                    .background(
                        color = activeColor,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            // Text
            Text(
                text = text,
                color = when (state) {
                    SystemIndicatorState.NOT_LOADED -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
                    SystemIndicatorState.INITIALIZING -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.38f)
                    SystemIndicatorState.READY -> activeColor
                    SystemIndicatorState.ACTIVE -> {
                        // Apply flash animation if testing mode enabled (REQ-007)
                        if (isTestingMode) {
                            activeColor.copy(alpha = flashAlpha)
                        } else {
                            activeColor
                        }
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = when (state) {
                    SystemIndicatorState.NOT_LOADED -> FontWeight.Normal
                    SystemIndicatorState.INITIALIZING -> FontWeight.Normal
                    SystemIndicatorState.READY -> FontWeight.Bold
                    SystemIndicatorState.ACTIVE -> FontWeight.Bold
                }
            )

            // Spinner for INITIALIZING state
            if (state == SystemIndicatorState.INITIALIZING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}
