package com.augmentalis.webavanue.ui.screen.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.domain.model.TabGroup

/**
 * TabGroupAssignmentDialog - Assign a tab to a group
 *
 * Features:
 * - List of existing groups
 * - "No Group" option to unassign
 * - "Create New Group" option
 * - Shows current group with checkmark
 * - Voice command: "add to group [name]"
 *
 * @param visible Whether dialog is visible
 * @param tabTitle Title of tab being assigned
 * @param groups List of available groups
 * @param currentGroupId Current group ID (null if no group)
 * @param onGroupSelected Callback when group is selected (groupId or null for no group)
 * @param onCreateNewGroup Callback when create new group is clicked
 * @param onDismiss Callback when dialog is dismissed
 * @param modifier Modifier for customization
 */
@Composable
fun TabGroupAssignmentDialog(
    visible: Boolean,
    tabTitle: String,
    groups: List<TabGroup>,
    currentGroupId: String?,
    onGroupSelected: (String?) -> Unit,
    onCreateNewGroup: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Add to Group",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tabTitle.take(50) + if (tabTitle.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // No Group option
                item {
                    GroupOption(
                        title = "No Group",
                        color = null,
                        isSelected = currentGroupId == null,
                        onClick = {
                            onGroupSelected(null)
                            onDismiss()
                        }
                    )
                }

                // Existing groups
                items(groups) { group ->
                    GroupOption(
                        title = group.title,
                        color = Color(android.graphics.Color.parseColor(group.color.colorHex)),
                        isSelected = group.id == currentGroupId,
                        onClick = {
                            onGroupSelected(group.id)
                            onDismiss()
                        }
                    )
                }

                // Create new group option
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onCreateNewGroup()
                                onDismiss()
                            },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Create New Group",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    )
}

/**
 * Group option in the list
 */
@Composable
private fun GroupOption(
    title: String,
    color: Color?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Color indicator
                if (color != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }

            // Checkmark if selected
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
