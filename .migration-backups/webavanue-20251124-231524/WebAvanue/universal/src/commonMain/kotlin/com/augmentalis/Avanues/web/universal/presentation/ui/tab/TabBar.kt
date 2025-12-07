package com.augmentalis.Avanues.web.universal.presentation.ui.tab

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabUiState
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabViewModel

/**
 * TabBar - Displays all open tabs in a horizontal scrollable bar
 *
 * Features:
 * - Horizontal scrollable list of tabs
 * - Active tab highlighting
 * - New tab button
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
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    // Dark 3D theme colors
    val bgPrimary = Color(0xFF1A1A2E)
    val accentVoice = Color(0xFFA78BFA)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bgPrimary,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable tab list
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { tabState ->
                    TabItem(
                        tabState = tabState,
                        isActive = tabState.tab.id == activeTab?.tab?.id,
                        onClick = { viewModel.switchTab(tabState.tab.id) },
                        onClose = { viewModel.closeTab(tabState.tab.id) }
                    )
                }
            }

            // New tab button
            IconButton(
                onClick = onNewTab,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab (Voice: new tab)",
                    tint = accentVoice,
                    modifier = Modifier.size(20.dp)
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
    // Dark 3D theme colors
    val bgPrimary = Color(0xFF1A1A2E)
    val accentVoice = Color(0xFFA78BFA)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bgPrimary,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable tab list
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { tabState ->
                    TabItem(
                        tabState = tabState,
                        isActive = tabState.tab.id == activeTabId,
                        onClick = { onTabClick(tabState.tab.id) },
                        onClose = { onTabClose(tabState.tab.id) }
                    )
                }
            }

            // New tab button
            IconButton(
                onClick = onNewTab,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab (Voice: new tab)",
                    tint = accentVoice,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
