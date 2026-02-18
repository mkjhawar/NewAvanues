// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/ui/SuggestionChips.kt
// created: 2025-11-01 22:50:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 2 - Glassmorphic UI
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.overlay.controller.Suggestion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.overlay.theme.OverlayAnimations

/**
 * Suggestion chips row with glassmorphic styling.
 *
 * Displays a flow row of Material3 AssistChips with glass-like appearance
 * and press animations. Chips wrap to multiple rows if needed.
 *
 * Visual specs:
 * - Chip background: #1E1E20 @ 50% opacity
 * - Chip border: White @ 20% opacity
 * - Text: White @ 90% opacity
 * - Press scale: 0.95
 * - Animation: 100ms ease-out
 *
 * @param suggestions List of suggestion items
 * @param onClick Callback when chip is clicked
 * @param modifier Optional modifier
 * @author Manoj Jhawar
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionChips(
    suggestions: List<Suggestion>,
    onClick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { suggestion ->
            SuggestionChip(
                suggestion = suggestion,
                onClick = { onClick(suggestion) }
            )
        }
    }
}

/**
 * Individual suggestion chip with press animation
 */
@Composable
private fun SuggestionChip(
    suggestion: Suggestion,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = OverlayAnimations.pressScale,
        label = "chip_press_scale"
    )

    AssistChip(
        onClick = {
            pressed = true
            onClick()
        },
        label = {
            Text(
                text = suggestion.label,
                color = AvanueTheme.colors.textPrimary
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = AvanueTheme.colors.surfaceVariant,  // Ocean Glass v2.3 - solid color
            labelColor = AvanueTheme.colors.textPrimary,
            leadingIconContentColor = AvanueTheme.colors.textSecondary
        ),
        border = BorderStroke(1.dp, AvanueTheme.colors.borderSubtle),
        modifier = Modifier.scale(scale)
    )

    // Reset pressed state after animation
    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}
