/**
 * GroupedListDetail.kt - Apple iOS Settings-style grouped list/detail navigation.
 *
 * Pattern name: **GroupedListDetail** (Avanues UI pattern library)
 *
 * Displays sections of items in a scrollable list. Tapping an item navigates
 * to a full-screen detail view. The TopAppBar dynamically shows either the
 * list title or the detail title with appropriate back navigation.
 *
 * Usage examples:
 * - Open Source License viewer (sections by license type, detail shows license text)
 * - Browser settings (sections by category, detail shows editable settings)
 * - Any grouped navigation requiring list → detail flow
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.avanueui.components.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A section of items in a GroupedListDetail view.
 *
 * @param T The type of items in this section
 * @param title Section header text (displayed uppercase in the list)
 * @param items Items belonging to this section
 */
data class GroupedListSection<T>(
    val title: String,
    val items: List<T>
)

/**
 * Full-screen grouped list/detail navigation with Scaffold.
 *
 * Manages list ↔ detail state internally via [rememberSaveable].
 * The TopAppBar title and back button adapt automatically:
 * - **List view**: Shows [title], back navigates out via [onNavigateBack]
 * - **Detail view**: Shows [detailTitle], back returns to list
 *
 * @param T The type of list items
 * @param title Title shown in TopAppBar on the list view
 * @param sections List of grouped sections to display
 * @param itemKey Unique string key for each item (must be stable across recomposition)
 * @param onNavigateBack Called when back is pressed from the list view
 * @param listRow Composable for each row. Call the provided onClick to navigate to detail.
 * @param detailTitle Returns the TopAppBar title for a given detail item
 * @param detailContent Composable for the detail view (receives padding from Scaffold)
 * @param modifier Modifier for the Scaffold
 * @param subtitle Optional subtitle below the title on list view
 * @param loading When true, shows a centered CircularProgressIndicator
 * @param savingIndicator When true, shows a LinearProgressIndicator under the TopAppBar
 * @param error Error message to display instead of content
 * @param topBarActions Additional actions for the TopAppBar (visible on list view only)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GroupedListDetailScaffold(
    title: String,
    sections: List<GroupedListSection<T>>,
    itemKey: (T) -> String,
    onNavigateBack: () -> Unit,
    listRow: @Composable (item: T, onClick: () -> Unit) -> Unit,
    detailTitle: (T) -> String,
    detailContent: @Composable (item: T, contentPadding: PaddingValues) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    loading: Boolean = false,
    savingIndicator: Boolean = false,
    error: String? = null,
    topBarActions: @Composable RowScope.() -> Unit = {}
) {
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedItem: T? = if (selectedKey != null) {
        sections.flatMap { it.items }.firstOrNull { itemKey(it) == selectedKey }
    } else null

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (selectedItem != null) {
                            Text(
                                text = detailTitle(selectedItem),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Column {
                                Text(title)
                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (selectedKey != null) {
                                selectedKey = null
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        if (selectedKey == null) {
                            topBarActions()
                        }
                    }
                )
                if (savingIndicator) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null && selectedItem == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            selectedItem != null -> {
                detailContent(selectedItem, paddingValues)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    sections.forEach { section ->
                        if (section.title.isNotBlank()) {
                            item(key = "header_${section.title}") {
                                GroupedListSectionHeader(section.title)
                            }
                        }
                        section.items.forEachIndexed { index, sectionItem ->
                            item(key = "item_${itemKey(sectionItem)}") {
                                listRow(sectionItem) {
                                    selectedKey = itemKey(sectionItem)
                                }
                                if (index < section.items.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section header — uppercase label in onSurfaceVariant color.
 *
 * Matches Apple iOS grouped table view section header style.
 */
@Composable
fun GroupedListSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp)
    )
}

/**
 * Standard list row — icon, title, subtitle, trailing chevron.
 *
 * Apple Settings style row for category navigation. Leading icon, primary title,
 * secondary subtitle showing current value or description, and a trailing
 * chevron indicating detail navigation.
 *
 * @param title Primary text
 * @param onClick Called when the row is tapped
 * @param subtitle Secondary description or current value
 * @param icon Leading icon
 * @param iconTint Leading icon tint color
 */
@Composable
fun GroupedListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            {
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else null,
        leadingContent = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint
                )
            }
        } else null,
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}
