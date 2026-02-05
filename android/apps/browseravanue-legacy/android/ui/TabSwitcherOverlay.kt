package com.augmentalis.browseravanue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.augmentalis.browseravanue.domain.model.Tab

/**
 * Tab switcher overlay
 *
 * Architecture:
 * - Full-screen overlay
 * - Grid/list of open tabs
 * - Swipe to close
 * - New tab button
 *
 * Layout:
 * ```
 * ┌────────────────────────────┐
 * │  Tabs (5)          [X]     │
 * ├────────────────────────────┤
 * │  ┌─────────────┐           │
 * │  │ Tab 1       │  [X]      │
 * │  │ google.com  │           │
 * │  └─────────────┘           │
 * │  ┌─────────────┐           │
 * │  │ Tab 2       │  [X]      │
 * │  │ github.com  │           │
 * │  └─────────────┘           │
 * │                            │
 * │       [+ New Tab]          │
 * └────────────────────────────┘
 * ```
 *
 * Features:
 * - Scrollable tab list
 * - Current tab highlighted
 * - Quick close button per tab
 * - New tab button at bottom
 * - Empty state
 *
 * Usage:
 * ```
 * if (showTabSwitcher) {
 *     TabSwitcherOverlay(
 *         tabs = tabs,
 *         onTabClick = { ... },
 *         onDismiss = { ... }
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherOverlay(
    tabs: List<Tab>,
    currentTabIndex: Int,
    onTabClick: (Tab) -> Unit,
    onTabClose: (Tab) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagicSurface(
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                MagicTopBar(
                    title = "Tabs (${tabs.size})",
                    navigationIcon = {
                        MagicIconButton(
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            },
                            onClick = onDismiss
                        )
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNewTab
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Tab"
                    )
                }
            }
        ) { paddingValues ->
            if (tabs.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MagicSpacing.md)
                    ) {
                        Text(
                            text = "No tabs open",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        MagicButton(
                            text = "New Tab",
                            onClick = onNewTab
                        )
                    }
                }
            } else {
                // Tab list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(MagicSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(MagicSpacing.sm)
                ) {
                    itemsIndexed(
                        items = tabs,
                        key = { _, tab -> tab.id }
                    ) { index, tab ->
                        TabCard(
                            tab = tab,
                            isCurrentTab = index == currentTabIndex,
                            onClick = { onTabClick(tab) },
                            onClose = { onTabClose(tab) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual tab card
 */
@Composable
private fun TabCard(
    tab: Tab,
    isCurrentTab: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagicCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentTab) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                MagicSpacer(SpacingSize.SMALL)

                Text(
                    text = tab.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Incognito badge
                if (tab.isIncognito) {
                    MagicSpacer(SpacingSize.SMALL)
                    MagicChip(
                        text = "Incognito",
                        onClick = {},
                        selected = true
                    )
                }
            }

            // Close button
            MagicIconButton(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close tab"
                    )
                },
                onClick = onClose
            )
        }
    }
}
