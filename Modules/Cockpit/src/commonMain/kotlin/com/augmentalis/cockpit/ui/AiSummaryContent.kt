package com.augmentalis.cockpit.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.components.AvanueButton
import com.augmentalis.avanueui.components.AvanueChip
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.SummaryType

/**
 * Cross-platform AI summary composable.
 *
 * Displays an AI-generated summary with controls for:
 * - Summary type selection (Brief / Detailed / Action Items / Q&A)
 * - Source frame chips showing which frames are being summarized
 * - Generate / Refresh button
 * - Auto-refresh indicator
 * - Last refreshed timestamp
 *
 * The actual LLM generation is deferred to a future AI module integration.
 * This composable provides the complete interaction surface and displays
 * any existing summary text from [FrameContent.AiSummary.summary].
 *
 * @param content The AiSummary content model
 * @param onGenerateSummary Callback to trigger summary generation
 * @param onContentStateChanged Callback with updated JSON when type changes
 * @param modifier Compose modifier
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSummaryContent(
    content: FrameContent.AiSummary,
    onGenerateSummary: () -> Unit = {},
    onContentStateChanged: (FrameContent.AiSummary) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with AI icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "AI Summary",
                color = colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            if (content.autoRefresh) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Auto-refresh enabled",
                    tint = colors.success.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Summary type selector chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SummaryType.entries.forEach { type ->
                val isSelected = content.summaryType == type
                val label = when (type) {
                    SummaryType.BRIEF -> "Brief"
                    SummaryType.DETAILED -> "Detailed"
                    SummaryType.ACTION_ITEMS -> "Action Items"
                    SummaryType.QA -> "Q&A"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) colors.primary.copy(alpha = 0.15f)
                            else colors.surface
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) colors.primary else colors.textPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onContentStateChanged(content.copy(summaryType = type))
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) colors.primary else colors.textPrimary.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Source frames
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
                            .background(colors.info.copy(alpha = 0.1f))
                            .border(1.dp, colors.info.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
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
            Spacer(Modifier.height(12.dp))
        }

        // Summary text or empty state
        if (content.summary.isNotBlank()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .padding(12.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = content.summary,
                    color = colors.textPrimary.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            if (content.lastRefreshedAt.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Last updated: ${content.lastRefreshedAt}",
                    color = colors.textPrimary.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = colors.textPrimary.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
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

        // Generate button
        Spacer(Modifier.height(12.dp))
        AvanueButton(
            onClick = onGenerateSummary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (content.summary.isBlank()) "Generate Summary" else "Regenerate")
        }
    }
}
