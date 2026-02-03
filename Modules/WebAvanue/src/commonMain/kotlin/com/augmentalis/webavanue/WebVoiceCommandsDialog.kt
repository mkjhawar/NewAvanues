package com.augmentalis.webavanue

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.augmentalis.webavanue.OceanDesignTokens

/**
 * Web Voice Command for display in help dialog
 */
data class WebVoiceCommandDisplay(
    val phrase: String,
    val description: String,
    val elementType: String,
    val action: String,
    val usageCount: Int = 0,
    val isSaved: Boolean = false
)

/**
 * Command category for web commands
 */
enum class WebCommandCategory(
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    PAGE_ELEMENTS(
        title = "Page Elements",
        icon = Icons.Default.TouchApp,
        description = "Click buttons, links, and interactive elements"
    ),
    NAVIGATION(
        title = "Navigation",
        icon = Icons.Default.Navigation,
        description = "Navigate between pages and within the site"
    ),
    FORMS(
        title = "Forms & Input",
        icon = Icons.Default.Edit,
        description = "Fill forms, type text, select options"
    ),
    PAGE_CONTROL(
        title = "Page Control",
        icon = Icons.Default.Settings,
        description = "Scroll, zoom, and control page display"
    ),
    BROWSER(
        title = "Browser",
        icon = Icons.Default.Tab,
        description = "Tabs, bookmarks, and browser functions"
    )
}

/**
 * Enhanced Web Voice Commands Dialog
 *
 * Shows all available voice commands with categories, search, and
 * live page elements that can be activated by voice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebVoiceCommandsDialog(
    pageCommands: List<WebVoiceCommandDisplay>,
    staticCommands: List<WebVoiceCommandDisplay>,
    isScanning: Boolean,
    commandCount: Int,
    onDismiss: () -> Unit,
    onCommandExecute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<WebCommandCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }

    // Filter commands by search
    val filteredPageCommands = remember(pageCommands, searchQuery) {
        if (searchQuery.isBlank()) pageCommands
        else pageCommands.filter {
            it.phrase.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .widthIn(min = 340.dp, max = 500.dp)
                .heightIn(max = 650.dp),
            shape = RoundedCornerShape(20.dp),
            color = OceanDesignTokens.Surface.default,
            tonalElevation = 8.dp
        ) {
            Column {
                // Header
                Surface(
                    color = OceanDesignTokens.Surface.elevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedCategory != null && !showSearchField) {
                                IconButton(onClick = { selectedCategory = null }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = OceanDesignTokens.Icon.primary
                                    )
                                }
                            }

                            if (!showSearchField) {
                                Text(
                                    text = selectedCategory?.title ?: "Voice Commands",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OceanDesignTokens.Text.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row {
                                AnimatedVisibility(
                                    visible = showSearchField,
                                    enter = expandHorizontally() + fadeIn(),
                                    exit = shrinkHorizontally() + fadeOut()
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search commands...") },
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, "Clear")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        modifier = Modifier.widthIn(max = 280.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                IconButton(onClick = { showSearchField = !showSearchField }) {
                                    Icon(
                                        if (showSearchField) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = OceanDesignTokens.Icon.primary
                                    )
                                }

                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = OceanDesignTokens.Icon.secondary
                                    )
                                }
                            }
                        }

                        // Status indicator
                        if (!showSearchField) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = OceanDesignTokens.Icon.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Scanning page...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OceanDesignTokens.Text.secondary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = OceanDesignTokens.Icon.success,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$commandCount commands ready",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OceanDesignTokens.Text.secondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Content
                when {
                    showSearchField && searchQuery.isNotBlank() -> {
                        // Search results
                        SearchResults(
                            pageCommands = filteredPageCommands,
                            staticCommands = staticCommands.filter {
                                it.phrase.contains(searchQuery, ignoreCase = true)
                            },
                            onCommandClick = {
                                onCommandExecute(it)
                                onDismiss()
                            }
                        )
                    }
                    selectedCategory == null -> {
                        // Categories grid
                        CategoriesGrid(
                            pageCommandCount = pageCommands.size,
                            onCategorySelect = { selectedCategory = it }
                        )
                    }
                    selectedCategory == WebCommandCategory.PAGE_ELEMENTS -> {
                        // Page-specific commands
                        PageElementsView(
                            commands = pageCommands,
                            onCommandClick = {
                                onCommandExecute(it)
                                onDismiss()
                            }
                        )
                    }
                    else -> {
                        // Static category commands
                        StaticCommandsView(
                            category = selectedCategory!!,
                            commands = staticCommands,
                            onCommandClick = {
                                onCommandExecute(it)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Categories grid view
 */
@Composable
private fun CategoriesGrid(
    pageCommandCount: Int,
    onCategorySelect: (WebCommandCategory) -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Page elements card (highlighted)
        if (pageCommandCount > 0) {
            Surface(
                onClick = { onCategorySelect(WebCommandCategory.PAGE_ELEMENTS) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = OceanDesignTokens.Icon.primary.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(OceanDesignTokens.Icon.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            WebCommandCategory.PAGE_ELEMENTS.icon,
                            contentDescription = null,
                            tint = OceanDesignTokens.Icon.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = WebCommandCategory.PAGE_ELEMENTS.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OceanDesignTokens.Text.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = OceanDesignTokens.Icon.primary
                            ) {
                                Text(
                                    text = pageCommandCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = WebCommandCategory.PAGE_ELEMENTS.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = OceanDesignTokens.Text.secondary
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = OceanDesignTokens.Icon.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Other categories grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val categories = WebCommandCategory.entries.filter {
                it != WebCommandCategory.PAGE_ELEMENTS
            }

            items(categories) { category ->
                CategoryCard(
                    category = category,
                    onClick = { onCategorySelect(category) }
                )
            }
        }
    }
}

/**
 * Category card
 */
@Composable
private fun CategoryCard(
    category: WebCommandCategory,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = OceanDesignTokens.Surface.elevated,
        modifier = Modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = OceanDesignTokens.Icon.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = OceanDesignTokens.Text.primary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Page elements view showing all page-specific commands
 */
@Composable
private fun PageElementsView(
    commands: List<WebVoiceCommandDisplay>,
    onCommandClick: (String) -> Unit
) {
    if (commands.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = OceanDesignTokens.Icon.disabled
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No clickable elements found",
                    style = MaterialTheme.typography.titleMedium,
                    color = OceanDesignTokens.Text.secondary
                )
                Text(
                    text = "Try scrolling to reveal more content",
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanDesignTokens.Text.secondary
                )
            }
        }
    } else {
        // Group by element type
        val grouped = remember(commands) {
            commands.groupBy { it.elementType }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            grouped.forEach { (type, typeCommands) ->
                item {
                    Text(
                        text = type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OceanDesignTokens.Text.secondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(typeCommands) { command ->
                    CommandCard(
                        command = command,
                        onClick = { onCommandClick(command.phrase) }
                    )
                }
            }
        }
    }
}

/**
 * Static commands view for browser commands
 */
@Composable
private fun StaticCommandsView(
    category: WebCommandCategory,
    commands: List<WebVoiceCommandDisplay>,
    onCommandClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Category description
        Text(
            text = category.description,
            style = MaterialTheme.typography.bodyMedium,
            color = OceanDesignTokens.Text.secondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Commands list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(commands) { command ->
                CommandCard(
                    command = command,
                    onClick = { onCommandClick(command.phrase) }
                )
            }
        }
    }
}

/**
 * Search results view
 */
@Composable
private fun SearchResults(
    pageCommands: List<WebVoiceCommandDisplay>,
    staticCommands: List<WebVoiceCommandDisplay>,
    onCommandClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (pageCommands.isNotEmpty()) {
            item {
                Text(
                    text = "Page Elements",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OceanDesignTokens.Text.secondary
                )
            }
            items(pageCommands.take(10)) { command ->
                CommandCard(command = command, onClick = { onCommandClick(command.phrase) })
            }
        }

        if (staticCommands.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Browser Commands",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OceanDesignTokens.Text.secondary
                )
            }
            items(staticCommands) { command ->
                CommandCard(command = command, onClick = { onCommandClick(command.phrase) })
            }
        }

        if (pageCommands.isEmpty() && staticCommands.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No commands found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OceanDesignTokens.Text.secondary
                    )
                }
            }
        }
    }
}

/**
 * Command card - clickable command display
 */
@Composable
private fun CommandCard(
    command: WebVoiceCommandDisplay,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = OceanDesignTokens.Surface.elevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\"${command.phrase}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = OceanDesignTokens.Icon.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (command.isSaved) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Saved",
                            tint = OceanDesignTokens.Icon.warning,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = command.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanDesignTokens.Text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Execute",
                tint = OceanDesignTokens.Icon.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
