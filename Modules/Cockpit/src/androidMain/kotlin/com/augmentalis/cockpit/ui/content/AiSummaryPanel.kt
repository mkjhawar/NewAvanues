package com.augmentalis.cockpit.ui.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueButton
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.SummaryType

/**
 * Android-specific AI Summary panel for the Cockpit multi-window system.
 *
 * Renders the full interactive surface for AI-generated summaries:
 * - SpatialVoice gradient background
 * - Summary type selection chips (Brief / Detailed / Action Items / Q&A)
 * - Source frame identifier chips
 * - Scrollable summary text display area
 * - Generate / Regenerate button
 * - Refresh button (re-runs generation without changing type)
 * - Auto-refresh toggle switch
 * - Loading spinner during generation
 * - Last refreshed timestamp
 *
 * All interactive elements carry AVID voice semantics per the voice-first mandate.
 *
 * @param content The [FrameContent.AiSummary] state model for this frame.
 * @param onGenerateSummary Callback invoked when the user requests generation for [SummaryType].
 * @param onContentStateChanged Callback with the updated [FrameContent.AiSummary] when any
 *   user-driven state change occurs (type selection, auto-refresh toggle, etc.).
 * @param isGenerating Whether the LLM generation is currently in progress.
 * @param modifier Compose modifier applied to the root container.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSummaryPanel(
    content: FrameContent.AiSummary,
    onGenerateSummary: (SummaryType) -> Unit,
    onContentStateChanged: (FrameContent.AiSummary) -> Unit,
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val isDark = AvanueTheme.isDark
    val scrollState = rememberScrollState()

    // SpatialVoice gradient background
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            colors.background,
            colors.surface.copy(alpha = 0.6f),
            colors.background
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(brush = gradientBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {

        // ── Header ────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "AI Summary",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))

            // Refresh button — re-runs generation with the current type
            if (content.summary.isNotBlank() && !isGenerating) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Voice: click Refresh summary",
                    tint = colors.primary.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onGenerateSummary(content.summaryType) }
                        .semantics { contentDescription = "Voice: click Refresh summary" }
                )
                Spacer(Modifier.width(8.dp))
            }

            // Loading spinner
            AnimatedVisibility(
                visible = isGenerating,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "spinner")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "spinner_rotation"
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotation),
                    color = colors.primary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Summary Type Chips ────────────────────────────────────────────
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SummaryType.entries.forEach { type ->
                val isSelected = content.summaryType == type
                val label = when (type) {
                    SummaryType.BRIEF        -> "Brief"
                    SummaryType.DETAILED     -> "Detailed"
                    SummaryType.ACTION_ITEMS -> "Action Items"
                    SummaryType.QA           -> "Q&A"
                }
                val avidLabel = "Voice: click summary type $label"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) colors.primary.copy(alpha = 0.15f)
                            else colors.surface
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.primary
                                    else colors.textPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = !isGenerating) {
                            onContentStateChanged(content.copy(summaryType = type))
                        }
                        .semantics { contentDescription = avidLabel }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.primary
                                else colors.textPrimary.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Source Frame Chips ────────────────────────────────────────────
        if (content.sourceFrameIds.isNotEmpty()) {
            Text(
                text = "Sources",
                color = colors.textPrimary.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                content.sourceFrameIds.forEach { frameId ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.info.copy(alpha = if (isDark) 0.15f else 0.08f))
                            .border(
                                width = 1.dp,
                                color = colors.info.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Frame ${frameId.takeLast(5)}",
                            color = colors.info,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── Summary Content Area ──────────────────────────────────────────
        if (content.summary.isNotBlank()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface.copy(alpha = if (isDark) 0.6f else 0.8f))
                    .border(
                        width = 1.dp,
                        color = colors.textPrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = content.summary,
                    color = colors.textPrimary.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            // Last refreshed timestamp
            if (content.lastRefreshedAt.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Updated: ${content.lastRefreshedAt}",
                    color = colors.textPrimary.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        } else {
            // Empty state
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = colors.textPrimary.copy(alpha = if (isDark) 0.2f else 0.15f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (content.sourceFrameIds.isEmpty())
                        "Select source frames and tap Generate\nto create a summary"
                    else
                        "Tap Generate to create a summary\nfrom ${content.sourceFrameIds.size} source frame(s)",
                    color = colors.textPrimary.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Auto-refresh Toggle ───────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Voice: toggle Auto refresh summary" }
        ) {
            Text(
                text = "Auto-refresh",
                color = colors.textPrimary.copy(alpha = 0.6f),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = content.autoRefresh,
                onCheckedChange = { enabled ->
                    onContentStateChanged(content.copy(autoRefresh = enabled))
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.primary,
                    checkedTrackColor = colors.primary.copy(alpha = 0.4f),
                    uncheckedThumbColor = colors.textPrimary.copy(alpha = 0.4f),
                    uncheckedTrackColor = colors.textPrimary.copy(alpha = 0.15f)
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Voice: toggle Auto refresh summary"
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Generate / Regenerate Button ──────────────────────────────────
        val generateLabel = when {
            isGenerating             -> "Generating…"
            content.summary.isBlank() -> "Generate Summary"
            else                      -> "Regenerate"
        }
        AvanueButton(
            onClick = { onGenerateSummary(content.summaryType) },
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Voice: click $generateLabel" }
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = generateLabel)
        }
    }
}
