// filename: features/overlay/src/main/java/com/augmentalis/ava/features/overlay/ui/GlassMorphicPanel.kt
// created: 2025-11-01 22:45:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 2 - Glassmorphic UI
// agent: Engineer | mode: ACT

package com.augmentalis.overlay.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.overlay.controller.Suggestion
import com.augmentalis.overlay.theme.OverlayAnimations
import com.augmentalis.overlay.theme.panelSolidEffect

/**
 * Glassmorphic expandable panel for AVA overlay.
 *
 * Displays voice transcript, AI response, and suggestion chips in a
 * translucent glass card. Animates smoothly between collapsed and expanded states.
 *
 * Visual specs:
 * - Width: 340dp
 * - Expanded height: 480dp
 * - Collapsed height: 0dp
 * - Corner radius: 24dp
 * - Background: #1E1E20 @ 70% opacity with 28dp blur
 * - Border: White @ 15% opacity
 * - Elevation: 10dp shadow
 *
 * @param expanded Whether panel is expanded
 * @param transcript Current voice transcript (or null)
 * @param response AI response text (or null)
 * @param suggestions List of suggestion chips
 * @param onSuggestionClick Callback when suggestion is clicked
 * @param modifier Optional modifier
 * @author Manoj Jhawar
 */
@Composable
fun GlassMorphicPanel(
    expanded: Boolean,
    transcript: String?,
    response: String?,
    suggestions: List<Suggestion>,
    onSuggestionClick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val height by animateDpAsState(
        targetValue = if (expanded) 480.dp else 0.dp,
        animationSpec = tween(
            durationMillis = if (expanded) 220 else 180,
            easing = if (expanded) androidx.compose.animation.core.FastOutSlowInEasing else androidx.compose.animation.core.LinearOutSlowInEasing
        ),
        label = "panel_height"
    )

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(OverlayAnimations.fadeIn) + expandVertically(OverlayAnimations.panelExpand),
        exit = fadeOut(OverlayAnimations.fadeOut) + shrinkVertically(OverlayAnimations.panelCollapse),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .width(340.dp)
                .height(height)
                .panelSolidEffect()  // Ocean Glass v2.3 - solid colors for stability
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title bar
            Text(
                text = "AVA Assistant",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AvanueTheme.colors.textPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            // Transcript section
            if (transcript != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "You said:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AvanueTheme.colors.textSecondary
                    )
                    Text(
                        text = transcript,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = AvanueTheme.colors.textPrimary
                    )
                }
            }

            // Response section
            if (response != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AVA:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AvanueTheme.colors.primary
                    )
                    Text(
                        text = response,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = AvanueTheme.colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Suggestion chips
            if (suggestions.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Suggestions:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AvanueTheme.colors.textSecondary
                    )
                    SuggestionChips(
                        suggestions = suggestions,
                        onClick = onSuggestionClick
                    )
                }
            }
        }
    }
}
