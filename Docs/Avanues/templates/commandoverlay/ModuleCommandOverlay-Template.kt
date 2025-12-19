/**
 * TEMPLATE: Voice Command Overlay for [YourModule]
 *
 * INSTRUCTIONS:
 * 1. Copy this file to: modules/yourmodule/src/main/java/.../presentation/components/
 * 2. Rename: ModuleCommandOverlay.kt (e.g., NotepadCommandOverlay.kt)
 * 3. Find/Replace: "YourModule" → "Notepad" (your actual module name)
 * 4. Find/Replace: "yourmodule" → "notepad" (lowercase package name)
 * 5. Define your command categories in getMasterCommands()
 * 6. Implement category composables (Category1Commands, Category2Commands, etc.)
 * 7. Update when() statement in main composable to include your categories
 * 8. Integrate into main screen (see Step 3 in guide)
 *
 * REFERENCE: /docs/modules/Browser/developer-manual/Guide-Voice-Command-Overlay-Pattern.md
 */

package com.augmentalis.avanue.yourmodule.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// TODO: Import your module's event class
// import com.augmentalis.avanue.yourmodule.core.YourModuleEvent

/**
 * Voice command overlay for [YourModule]
 *
 * Provides zero-space voice command UI with:
 * - Cascading navigation (Master → Categories → Commands)
 * - Uniform grid layout (all commands visible, no scrolling)
 * - Slide-up animation
 * - Voice toggle button
 *
 * @param visible Whether overlay is visible
 * @param onEvent Event handler for module actions
 * @param onDismiss Callback when overlay is dismissed
 * @param modifier Compose modifier
 */
@Composable
fun YourModuleCommandOverlay(
    visible: Boolean,
    onEvent: (YourModuleEvent) -> Unit,  // TODO: Replace with your event type
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ============================================
    // STATE MANAGEMENT
    // ============================================

    var currentLevel by remember { mutableStateOf("master") }
    var isListening by remember { mutableStateOf(false) }

    // Reset to master when overlay becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            currentLevel = "master"
            isListening = false
        }
    }

    // ============================================
    // ANIMATION
    // ============================================

    val slideOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        label = "overlay_slide"
    )

    // ============================================
    // UI RENDERING
    // ============================================

    // Only render when visible or animating
    if (slideOffset < 1f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .offset(y = (slideOffset * 1000).dp)
        ) {
            // Semi-transparent background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable(enabled = false) { }
            )

            // Command content at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Header with navigation
                CommandHeader(
                    currentLevel = currentLevel,
                    isListening = isListening,
                    onBack = { currentLevel = getParentLevel(currentLevel) },
                    onVoiceToggle = { isListening = !isListening },
                    onDismiss = onDismiss
                )

                // Command content (switches based on level)
                when (currentLevel) {
                    "master" -> MasterCommands(
                        onNavigate = { level -> currentLevel = level }
                    )
                    // TODO: Add your categories here
                    // "category1" -> Category1Commands(onEvent = onEvent)
                    // "category2" -> Category2Commands(onEvent = onEvent)
                }
            }
        }
    }
}

// ============================================
// HEADER COMPONENT
// ============================================

/**
 * Command header with navigation controls
 */
@Composable
private fun CommandHeader(
    currentLevel: String,
    isListening: Boolean,
    onBack: () -> Unit,
    onVoiceToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showBackButton = currentLevel != "master"
    val title = getLevelTitle(currentLevel)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back button (conditional)
            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to previous menu",
                        tint = Color.White
                    )
                }
            }

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            // Voice button
            IconButton(
                onClick = onVoiceToggle,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isListening) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop listening" else "Start listening",
                    tint = Color.White
                )
            }

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close command overlay",
                    tint = Color.White
                )
            }
        }
    }
}

// ============================================
// COMMAND CHIP (BUTTON)
// ============================================

/**
 * Uniform command button
 * Size: 110dp × 70dp (portrait), 90dp × 60dp (landscape)
 */
@Composable
private fun CommandChip(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================
// COMMAND DATA CLASS
// ============================================

/**
 * Command representation
 */
private data class Command(
    val icon: String,      // Emoji icon (e.g., "📝", "✅")
    val label: String,     // Display text (e.g., "New Note")
    val action: () -> Unit // Click handler
)

// ============================================
// MASTER LEVEL (CATEGORIES)
// ============================================

/**
 * Master level showing main categories
 * TODO: Customize with your module's categories
 */
@Composable
private fun MasterCommands(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember {
        listOf(
            // TODO: Replace with your actual categories
            Command("📋", "Category 1") { onNavigate("category1") },
            Command("⚙️", "Category 2") { onNavigate("category2") },
            Command("🔧", "Category 3") { onNavigate("category3") },
            Command("📊", "Category 4") { onNavigate("category4") }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}

// ============================================
// CATEGORY COMMANDS
// ============================================

// TODO: Implement your category composables
// Example:
/*
@Composable
private fun Category1Commands(
    onEvent: (YourModuleEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onEvent) {
        listOf(
            Command("✅", "Do This") { onEvent(YourModuleEvent.DoThis) },
            Command("❌", "Do That") { onEvent(YourModuleEvent.DoThat) },
            Command("🔄", "Reset") { onEvent(YourModuleEvent.Reset) }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}
*/

// ============================================
// COMMAND GRID LAYOUT
// ============================================

/**
 * Uniform grid layout
 * CRITICAL: userScrollEnabled = false (expands to fit all commands)
 */
@Composable
private fun CommandGrid(
    commands: List<Command>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false  // Voice-first: Show all commands without scrolling
    ) {
        items(commands) { command ->
            CommandChip(
                icon = command.icon,
                label = command.label,
                onClick = command.action
            )
        }
    }
}

// ============================================
// HELPER FUNCTIONS
// ============================================

/**
 * Get level title for header
 * TODO: Add your category titles
 */
private fun getLevelTitle(level: String): String = when (level) {
    "master" -> "Voice Commands"
    // TODO: Add your categories
    // "category1" -> "Category 1 Title"
    // "category2" -> "Category 2 Title"
    else -> "Commands"
}

/**
 * Get parent level for back navigation
 * TODO: Add your navigation hierarchy
 */
private fun getParentLevel(level: String): String = when (level) {
    // TODO: If you have sub-categories, map them here
    // "subcategory1" -> "category1"
    else -> "master"  // All categories return to master by default
}

// ============================================
// INTEGRATION EXAMPLE
// ============================================

/*
To integrate in your main screen:

@Composable
fun YourModuleScreen(
    modifier: Modifier = Modifier
) {
    var showCommandOverlay by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Your main content
        YourModuleContent(...)

        // FAB trigger
        if (!showCommandOverlay) {
            FloatingActionButton(
                onClick = { showCommandOverlay = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.Mic, "Voice commands")
            }
        }

        // Command overlay
        YourModuleCommandOverlay(
            visible = showCommandOverlay,
            onEvent = viewModel::onEvent,
            onDismiss = { showCommandOverlay = false },
            modifier = Modifier.fillMaxSize()
        )
    }
}
*/
