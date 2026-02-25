package com.augmentalis.cockpit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.AvanueChip
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.ContextualActionProvider
import com.augmentalis.cockpit.model.QuickAction

/**
 * Flat, single-level contextual action bar for simplified UI shells.
 *
 * Replaces the 3-depth hierarchical [CommandBar] with a simple horizontal
 * row of 5-6 action chips based on the focused content type. A "More" chip
 * at the end opens a searchable bottom sheet with all actions grouped by
 * category.
 *
 * This bar is used by all three simplified shells (AvanueViews, Lens, Canvas).
 * The [CommandBar] remains available for the Classic shell mode.
 *
 * Voice commands are NOT affected — all commands remain accessible by voice
 * regardless of what's visually shown in the action bar.
 */
@Composable
fun ContextualActionBar(
    contentTypeId: String?,
    onActionClick: (String) -> Unit,
    onMoreClick: () -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = AvanueTheme.colors
    val actions = contentTypeId?.let { ContextualActionProvider.topActionsForContent(it) } ?: emptyList()

    AnimatedVisibility(
        visible = visible && actions.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content-specific action chips
            actions.forEach { action ->
                ActionChip(
                    action = action,
                    onClick = { onActionClick(action.id) }
                )
            }

            // "More" chip — opens full action sheet
            AvanueChip(
                onClick = onMoreClick,
                label = { Text("More") },
                leadingIcon = {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = null,
                        tint = colors.textSecondary,
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = "Voice: click More"
                }
            )
        }
    }
}

/**
 * Single action chip for the contextual bar.
 */
@Composable
private fun ActionChip(
    action: QuickAction,
    onClick: () -> Unit,
) {
    AvanueChip(
        onClick = onClick,
        label = { Text(action.label) },
        modifier = Modifier.semantics {
            contentDescription = "Voice: click ${action.label}"
        }
    )
}
