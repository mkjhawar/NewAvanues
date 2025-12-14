package com.augmentalis.webavanue.ui.screen.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.ui.screen.theme.glassBar
import com.augmentalis.webavanue.ui.viewmodel.TabUiState
import com.augmentalis.webavanue.ui.viewmodel.TabViewModel
import kotlinx.coroutines.launch

/**
 * TabBar - Displays all open tabs in a horizontal scrollable bar (Chrome-like with glassmorphism)
 *
 * Features:
 * - Chrome-like horizontal scrollable tabs with glass effect
 * - Tab groups with colored indicators
 * - Active tab highlighting with brighter glass
 * - New tab button with glass styling
 * - Click to switch tabs
 * - Close button on each tab
 *
 * @param viewModel TabViewModel for state and actions
 * @param onNewTab Callback when new tab button is clicked
 * @param modifier Modifier for customization
 */
@Composable
fun TabBar(
    viewModel: TabViewModel,
    onNewTab: () -> Unit = { viewModel.createTab() },
    onTabLongClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to active tab when it changes
    LaunchedEffect(activeTab?.tab?.id) {
        activeTab?.let { active ->
            val activeIndex = tabs.indexOfFirst { it.tab.id == active.tab.id }
            if (activeIndex != -1) {
                // Calculate scroll position: each tab is ~180dp wide (average), spacing 6dp
                val tabWidth = 180 // Approximate width
                val spacing = 6
                val scrollPosition = activeIndex * (tabWidth + spacing)

                coroutineScope.launch {
                    scrollState.animateScrollTo(scrollPosition)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassBar()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable tab list
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { tabState ->
                    // TODO: Get tab group color from tabState.tab.groupId
                    // For now, no group color
                    TabItem(
                        tabState = tabState,
                        isActive = tabState.tab.id == activeTab?.tab?.id,
                        groupColor = null,  // TODO: Map groupId to color
                        onClick = { viewModel.switchTab(tabState.tab.id) },
                        onClose = { viewModel.closeTab(tabState.tab.id) },
                        onLongClick = { onTabLongClick(tabState.tab.id) }
                    )
                }
            }

            // New tab button with glass effect
            IconButton(
                onClick = onNewTab,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab (Voice: new tab)",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Simplified TabBar without ViewModel (for preview/testing)
 *
 * @param tabs List of tab UI states
 * @param activeTabId Currently active tab ID
 * @param onTabClick Callback when tab is clicked
 * @param onTabClose Callback when tab close button is clicked
 * @param onNewTab Callback when new tab button is clicked
 */
@Composable
fun TabBar(
    tabs: List<TabUiState>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassBar()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable tab list
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { tabState ->
                    TabItem(
                        tabState = tabState,
                        isActive = tabState.tab.id == activeTabId,
                        groupColor = null,
                        onClick = { onTabClick(tabState.tab.id) },
                        onClose = { onTabClose(tabState.tab.id) }
                    )
                }
            }

            // New tab button
            IconButton(
                onClick = onNewTab,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab (Voice: new tab)",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
