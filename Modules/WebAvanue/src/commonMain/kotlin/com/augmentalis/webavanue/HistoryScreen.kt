package com.augmentalis.webavanue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import com.augmentalis.avanueui.theme.AvanueTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.webavanue.HistoryEntry
import com.augmentalis.webavanue.HistoryViewModel
import kotlinx.datetime.*

/**
 * HistoryScreen - Main browsing history screen
 *
 * Features:
 * - List all browsing history
 * - Search history by URL/title
 * - Group by date
 * - Clear history (all or by time range)
 * - Click to navigate to URL
 *
 * @param viewModel HistoryViewModel for state and actions
 * @param onNavigateBack Callback to navigate back
 * @param onHistoryClick Callback when history entry is clicked (navigate to URL)
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit = {},
    onHistoryClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val history by viewModel.history.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showSearchBar by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var clearTimeRange by remember { mutableStateOf<TimeRange?>(null) }

    // Group history by date
    val groupedHistory = remember(history) {
        history.groupBy { entry ->
            val localDateTime = entry.visitedAt.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.date
        }.toSortedMap(compareByDescending { it })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchHistory(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search history...") },
                            singleLine = true
                        )
                    } else {
                        Text("History")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showSearchBar) {
                            showSearchBar = false
                            viewModel.searchHistory("")
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
                    if (!showSearchBar) {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }

                        if (history.isNotEmpty()) {
                            IconButton(onClick = { showClearDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear History"
                                )
                            }
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AvanueTheme.colors.error
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { viewModel.loadHistory() }) {
                            Text("Retry")
                        }
                    }
                }

                history.isEmpty() -> {
                    EmptyHistoryState(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWide = maxWidth > 600.dp
                        val columns = if (isWide) 2 else 1

                        if (columns > 1) {
                            // Landscape / wide: grid with 2 columns
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(columns),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                groupedHistory.forEach { (date, entries) ->
                                    item(key = "header_$date", span = { GridItemSpan(columns) }) {
                                        Text(
                                            text = formatDate(date),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = AvanueTheme.colors.primary,
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp,
                                                vertical = 4.dp
                                            )
                                        )
                                    }

                                    items(entries, key = { it.id }) { entry ->
                                        HistoryItem(
                                            entry = entry,
                                            onClick = { onHistoryClick(entry.url) },
                                            onDelete = { viewModel.deleteHistoryEntry(entry.id) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        } else {
                            // Portrait: single column list
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                groupedHistory.forEach { (date, entries) ->
                                    item(key = "header_$date") {
                                        Text(
                                            text = formatDate(date),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = AvanueTheme.colors.primary,
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 8.dp
                                            )
                                        )
                                    }

                                    items(entries, key = { it.id }) { entry ->
                                        HistoryItem(
                                            entry = entry,
                                            onClick = { onHistoryClick(entry.url) },
                                            onDelete = { viewModel.deleteHistoryEntry(entry.id) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 4.dp)
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

    // Clear history dialog
    if (showClearDialog) {
        ClearHistoryDialog(
            onDismiss = { showClearDialog = false },
            onClearAll = {
                viewModel.clearHistory()
                showClearDialog = false
            },
            onClearTimeRange = { timeRange ->
                clearTimeRange = timeRange
                showClearDialog = false

                // Calculate time range
                val now = Clock.System.now()
                val timeZone = TimeZone.currentSystemDefault()
                val startTime = when (timeRange) {
                    TimeRange.LAST_HOUR -> now.minus(1, DateTimeUnit.HOUR, timeZone)
                    TimeRange.TODAY -> Clock.System.todayIn(timeZone).atStartOfDayIn(timeZone)
                    TimeRange.LAST_7_DAYS -> now.minus(7, DateTimeUnit.DAY, timeZone)
                    TimeRange.LAST_30_DAYS -> now.minus(30, DateTimeUnit.DAY, timeZone)
                }

                viewModel.clearHistoryByTimeRange(startTime, now)
            }
        )
    }
}

/**
 * EmptyHistoryState - Shown when no history exists
 */
@Composable
fun EmptyHistoryState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "No browsing history",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Your visited pages will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = AvanueTheme.colors.textSecondary
        )
    }
}

/**
 * ClearHistoryDialog - Dialog for clearing history
 */
@Composable
fun ClearHistoryDialog(
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onClearTimeRange: (TimeRange) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear Browsing History") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose what to clear:")

                // Time range options
                TextButton(
                    onClick = { onClearTimeRange(TimeRange.LAST_HOUR) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Last hour", modifier = Modifier.fillMaxWidth())
                }

                TextButton(
                    onClick = { onClearTimeRange(TimeRange.TODAY) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Today", modifier = Modifier.fillMaxWidth())
                }

                TextButton(
                    onClick = { onClearTimeRange(TimeRange.LAST_7_DAYS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Last 7 days", modifier = Modifier.fillMaxWidth())
                }

                TextButton(
                    onClick = { onClearTimeRange(TimeRange.LAST_30_DAYS) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Last 30 days", modifier = Modifier.fillMaxWidth())
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Clear all history",
                        color = AvanueTheme.colors.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * TimeRange - Time range for clearing history
 */
enum class TimeRange {
    LAST_HOUR,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS
}

/**
 * Format date for history grouping
 */
private fun formatDate(date: LocalDate): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val yesterday = today.minus(1, DateTimeUnit.DAY)

    return when (date) {
        today -> "Today"
        yesterday -> "Yesterday"
        else -> date.toString() // Format as needed
    }
}
