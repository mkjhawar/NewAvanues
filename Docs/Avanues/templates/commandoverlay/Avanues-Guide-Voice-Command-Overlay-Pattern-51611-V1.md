# Voice Command Overlay Pattern - Implementation Guide

**Document:** Guide-Voice-Command-Overlay-Pattern.md
**Created:** 2025-01-07
**Module:** Browser (reference implementation)
**Reusable For:** Any Avanues module requiring voice command UI

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture & Design Principles](#architecture--design-principles)
3. [Core Components](#core-components)
4. [Step-by-Step Implementation](#step-by-step-implementation)
5. [Code Templates](#code-templates)
6. [Customization Guide](#customization-guide)
7. [Integration Checklist](#integration-checklist)
8. [Best Practices](#best-practices)

---

## Overview

### What is the Voice Command Overlay Pattern?

A **zero-space, voice-first UI pattern** that provides:
- ✅ **Maximum screen space** for content (0dp when hidden)
- ✅ **Full command discoverability** via uniform grid (no scrolling)
- ✅ **Cascading navigation** (Master → Categories → Commands)
- ✅ **Responsive design** (portrait/landscape adaptive)
- ✅ **Voice-first philosophy** (FAB trigger, hands-free ready)

### Why Use This Pattern?

**Problem Solved:**
- Traditional voice command bars take permanent screen space (100-200dp)
- Users can't see available commands without scrolling
- Limited space for actual content

**Solution:**
- Overlay slides up on demand (FAB trigger)
- All commands visible in uniform grid
- Disappears when not needed (0dp overhead)

**Reference Implementation:** `BrowserCommandOverlay.kt` (515 lines)

---

## Architecture & Design Principles

### 1. Voice-First Philosophy

```
Principle: "Command discoverability over space efficiency"
- Show ALL commands at once (no scrolling)
- Uniform button sizes (aesthetic + predictable)
- Container expands vertically to fit all commands
```

### 2. Cascading Navigation

```
Master Level (Root)
  ├── Category 1 (e.g., "Tabs")
  │     ├── Command 1.1 (e.g., "New tab")
  │     ├── Command 1.2 (e.g., "Close tab")
  │     └── Command 1.3 (e.g., "Show tabs")
  ├── Category 2 (e.g., "Navigation")
  │     ├── Command 2.1 (e.g., "Back")
  │     └── Command 2.2 (e.g., "Forward")
  └── ...
```

**Navigation Rules:**
- Back button appears when NOT at master level
- Clicking category → drills down to commands
- Back button → returns to master
- Close button → dismisses entire overlay

### 3. Component Hierarchy

```
MainScreen.kt
  └── Box (content area)
        ├── WebView / Content (main)
        ├── FloatingActionButton (trigger)
        └── CommandOverlay (slides up)
              ├── CommandHeader (title, back, mic, close)
              └── CommandContent (switches based on level)
                    ├── MasterCommands (categories grid)
                    ├── CategoryCommands (specific commands)
                    └── CustomViews (e.g., "Show tabs" list)
```

---

## Core Components

### Component 1: Command Overlay Container

**File:** `ModuleCommandOverlay.kt`

**Responsibilities:**
- Manage overlay visibility state
- Handle slide-up/down animation
- Track current navigation level
- Switch between command screens

**Key State:**
```kotlin
var currentLevel by remember { mutableStateOf("master") }
var isListening by remember { mutableStateOf(false) }
```

**Animation:**
```kotlin
val slideOffset by animateFloatAsState(
    targetValue = if (visible) 0f else 1f,
    label = "overlay_slide"
)
```

### Component 2: Command Header

**Responsibilities:**
- Show current level title
- Back button (conditional)
- Voice toggle button
- Close button

**Layout:**
```
[← Back] [Title           ] [🎤 Mic] [× Close]
         (flex: 1)
```

### Component 3: Command Chip (Button)

**Specifications:**
- **Size:** 110dp × 70dp (portrait), 90dp × 60dp (landscape)
- **Shape:** RoundedCornerShape(12.dp)
- **Content:** Icon (24sp) + Label (12sp)
- **Colors:** Semi-transparent white background, white border

### Component 4: Command Grid

**Layout Engine:** `LazyVerticalGrid`
```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 110.dp),
    userScrollEnabled = false  // CRITICAL: Expands to fit all
)
```

**Why `userScrollEnabled = false`?**
- Voice-first principle: Show ALL commands at once
- Container expands vertically to accommodate all buttons
- No hidden commands = better discoverability

---

## Step-by-Step Implementation

### Step 1: Create Command Data Structure

**File:** `ModuleCommands.kt`

```kotlin
package com.augmentalis.avanue.yourmodule.presentation.components

/**
 * Command data class
 * Represents a single voice command button
 */
private data class Command(
    val icon: String,      // Emoji or icon identifier
    val label: String,     // Display text
    val action: () -> Unit // Click/voice handler
)

/**
 * Get master level categories
 */
fun getMasterCommands(
    onNavigate: (String) -> Unit
): List<Command> {
    return listOf(
        Command("📋", "Category 1") { onNavigate("category1") },
        Command("⚙️", "Category 2") { onNavigate("category2") },
        Command("🔧", "Category 3") { onNavigate("category3") }
    )
}

/**
 * Get category-specific commands
 */
fun getCategoryCommands(
    category: String,
    onEvent: (YourModuleEvent) -> Unit
): List<Command> {
    return when (category) {
        "category1" -> listOf(
            Command("✓", "Action 1") { onEvent(YourModuleEvent.Action1) },
            Command("✗", "Action 2") { onEvent(YourModuleEvent.Action2) }
        )
        // Add more categories...
        else -> emptyList()
    }
}
```

### Step 2: Create Command Overlay Component

**File:** `ModuleCommandOverlay.kt`

```kotlin
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Voice command overlay for [YourModule]
 *
 * Features:
 * - Zero-space design (hidden by default)
 * - Cascading navigation (Master → Categories → Commands)
 * - Uniform grid layout (all commands visible)
 * - Slide-up animation
 *
 * @param visible Whether overlay is visible
 * @param onEvent Event handler for module actions
 * @param onDismiss Callback when overlay is dismissed
 * @param modifier Compose modifier
 */
@Composable
fun ModuleCommandOverlay(
    visible: Boolean,
    onEvent: (YourModuleEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Navigation state
    var currentLevel by remember { mutableStateOf("master") }
    var isListening by remember { mutableStateOf(false) }

    // Reset to master when overlay becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            currentLevel = "master"
            isListening = false
        }
    }

    // Slide animation
    val slideOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        label = "overlay_slide"
    )

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
                // Header
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
                    "category1" -> Category1Commands(onEvent = onEvent)
                    "category2" -> Category2Commands(onEvent = onEvent)
                    // Add more categories...
                }
            }
        }
    }
}

/**
 * Command header with navigation
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
                        contentDescription = "Back",
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
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Uniform command chip
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

/**
 * Master level commands (categories)
 */
@Composable
private fun MasterCommands(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember {
        getMasterCommands(onNavigate)
    }
    CommandGrid(commands = commands, modifier = modifier)
}

/**
 * Category-specific commands
 */
@Composable
private fun Category1Commands(
    onEvent: (YourModuleEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onEvent) {
        getCategoryCommands("category1", onEvent)
    }
    CommandGrid(commands = commands, modifier = modifier)
}

// Add more category composables...

/**
 * Uniform grid layout
 * CRITICAL: userScrollEnabled = false (expands to fit all)
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
        userScrollEnabled = false  // Voice-first: Show all commands
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

// Helper functions
private fun getLevelTitle(level: String): String = when (level) {
    "master" -> "Voice Commands"
    "category1" -> "Category 1"
    "category2" -> "Category 2"
    else -> "Commands"
}

private fun getParentLevel(level: String): String = when (level) {
    "subcategory1" -> "category1"  // If you have sub-categories
    else -> "master"
}
```

### Step 3: Integrate into Main Screen

**File:** `YourModuleScreen.kt`

```kotlin
@Composable
fun YourModuleScreen(
    modifier: Modifier = Modifier
) {
    val viewModel = // ... get your ViewModel
    val state by viewModel.state.collectAsState()

    // Command overlay visibility state
    var showCommandOverlay by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Your main content
            YourModuleContent(
                state = state,
                onEvent = viewModel::onEvent,
                modifier = Modifier.fillMaxSize()
            )

            // FAB trigger (only when overlay hidden)
            if (!showCommandOverlay) {
                FloatingActionButton(
                    onClick = { showCommandOverlay = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice commands"
                    )
                }
            }

            // Command overlay
            ModuleCommandOverlay(
                visible = showCommandOverlay,
                onEvent = viewModel::onEvent,
                onDismiss = { showCommandOverlay = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

### Step 4: Define Module Events

**File:** `YourModuleEvent.kt`

```kotlin
package com.augmentalis.avanue.yourmodule.core

/**
 * Events for YourModule
 * Voice commands and user actions
 */
sealed class YourModuleEvent {
    // Category 1 events
    data object Action1 : YourModuleEvent()
    data object Action2 : YourModuleEvent()

    // Category 2 events
    data class Action3(val param: String) : YourModuleEvent()

    // Add more events as needed...
}
```

---

## Code Templates

### Template 1: Simple Category (3-5 Commands)

```kotlin
@Composable
private fun SimpleCategoryCommands(
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
```

### Template 2: Category with Sub-Navigation

```kotlin
@Composable
private fun ComplexCategoryCommands(
    onNavigate: (String) -> Unit,
    onEvent: (YourModuleEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onNavigate, onEvent) {
        listOf(
            // Sub-category
            Command("📂", "Sub Menu") { onNavigate("subcategory1") },
            // Direct actions
            Command("⚡", "Quick Action") { onEvent(YourModuleEvent.QuickAction) }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}
```

### Template 3: Custom View (Non-Grid)

```kotlin
@Composable
private fun CustomListView(
    items: List<YourDataModel>,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            Card(
                onClick = { onItemSelected(item.id) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
```

---

## Customization Guide

### 1. Change Colors

```kotlin
// Header background
color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

// Overlay background
background(Color.Black.copy(alpha = 0.95f))

// Button background
containerColor = Color.White.copy(alpha = 0.15f)

// Button border
color = Color.White.copy(alpha = 0.3f)
```

### 2. Adjust Button Sizes

```kotlin
// Portrait (default)
columns = GridCells.Adaptive(minSize = 110.dp)
modifier = modifier.height(70.dp)

// Landscape (smaller)
val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
val buttonSize = if (isLandscape) 60.dp else 70.dp
val gridMinSize = if (isLandscape) 90.dp else 110.dp
```

### 3. Add Icons (Material Icons)

```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

Icon(
    imageVector = Icons.Default.YourIcon,
    contentDescription = "Description"
)
```

### 4. Add Custom Animations

```kotlin
// Fade in
val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    label = "fade"
)

// Scale
val scale by animateFloatAsState(
    targetValue = if (visible) 1f else 0.8f,
    label = "scale"
)
```

---

## Integration Checklist

- [ ] Create `ModuleCommandOverlay.kt` component
- [ ] Define command categories (Master level)
- [ ] Implement category command screens
- [ ] Add FAB trigger to main screen
- [ ] Connect to ViewModel events
- [ ] Test slide animation
- [ ] Test cascading navigation (Master → Category → Back)
- [ ] Verify all commands visible (no scrolling needed)
- [ ] Test in portrait and landscape
- [ ] Add voice recognition integration (optional)
- [ ] Test with 10+ commands per category
- [ ] Verify overlay dismisses correctly

---

## Best Practices

### 1. Command Organization

```
✅ DO: Group related commands in categories (max 10 per category)
❌ DON'T: Put all commands in master level

✅ DO: Use clear, action-oriented labels ("New tab", "Delete item")
❌ DON'T: Use vague labels ("Manage", "Options")

✅ DO: Use emojis for visual recognition
❌ DON'T: Use only text (harder to scan)
```

### 2. Navigation Hierarchy

```
✅ DO: Keep hierarchy shallow (2-3 levels max)
Master → Category → Commands

❌ DON'T: Create deep nesting
Master → Category → Subcategory → Sub-subcategory → Commands
```

### 3. Performance

```
✅ DO: Use remember { } for command lists
val commands = remember { listOf(...) }

✅ DO: Use remember(key) when commands depend on state
val commands = remember(onEvent, activeId) { listOf(...) }

❌ DON'T: Recreate command lists on every recomposition
```

### 4. Accessibility

```
✅ DO: Provide contentDescription for all icons
Icon(imageVector = ..., contentDescription = "Close overlay")

✅ DO: Use semantic colors (error for destructive actions)
color = if (isDestructive) MaterialTheme.colorScheme.error else primary

✅ DO: Ensure minimum touch target size (48.dp)
```

### 5. Voice Integration

```
✅ DO: Make all commands voice-accessible
Command("New tab") → Voice: "new tab" or "open tab"

✅ DO: Provide visual feedback when listening
isListening state changes mic button color

✅ DO: Show suggested voice phrases
"Try: 'New tab' or 'Go to google.com'"
```

---

## Example: Notepad Module

Here's how you'd implement this for a Notepad module:

```kotlin
// Master commands
fun getNotepadMasterCommands(onNavigate: (String) -> Unit): List<Command> {
    return listOf(
        Command("📝", "Notes") { onNavigate("notes") },
        Command("📂", "Folders") { onNavigate("folders") },
        Command("✏️", "Edit") { onNavigate("edit") },
        Command("🔍", "Search") { onNavigate("search") },
        Command("⚙️", "Settings") { onNavigate("settings") }
    )
}

// Notes category
@Composable
private fun NotesCommands(
    onEvent: (NotepadEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = remember(onEvent) {
        listOf(
            Command("➕", "New Note") { onEvent(NotepadEvent.CreateNote) },
            Command("📋", "Show All") { onEvent(NotepadEvent.ShowAllNotes) },
            Command("❌", "Delete") { onEvent(NotepadEvent.DeleteCurrentNote) },
            Command("📤", "Share") { onEvent(NotepadEvent.ShareNote) }
        )
    }
    CommandGrid(commands = commands, modifier = modifier)
}
```

---

## Troubleshooting

### Issue: Commands are scrollable / not all visible

**Solution:** Ensure `userScrollEnabled = false` in `LazyVerticalGrid`

```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 110.dp),
    userScrollEnabled = false  // THIS IS CRITICAL
)
```

### Issue: Overlay doesn't slide up smoothly

**Solution:** Check animation offset calculation

```kotlin
.offset(y = (slideOffset * 1000).dp)  // Adjust 1000 if needed
```

### Issue: Back button doesn't show

**Solution:** Check `currentLevel` state

```kotlin
val showBackButton = currentLevel != "master"
if (showBackButton) { /* show back button */ }
```

### Issue: FAB and overlay both visible

**Solution:** Add visibility toggle

```kotlin
if (!showCommandOverlay) {
    FloatingActionButton(...)  // Only show when overlay hidden
}
```

---

## File Structure Reference

```
modules/yourmodule/
└── src/main/java/com/augmentalis/avanue/yourmodule/
    ├── presentation/
    │   ├── YourModuleScreen.kt              # Main screen
    │   └── components/
    │       ├── ModuleCommandOverlay.kt      # Overlay component (515 lines)
    │       └── ModuleCommands.kt            # Command definitions
    ├── core/
    │   └── YourModuleEvent.kt               # Event sealed class
    └── ...
```

---

## Additional Resources

- **Reference Implementation:** `/modules/browser/src/main/java/com/augmentalis/avanue/browser/presentation/components/BrowserCommandOverlay.kt`
- **Original Design Doc:** `/docs/modules/Browser/architecture/decisions/ADR-003-Voice-Command-Overlay.md` (if exists)
- **Material 3 Guidelines:** https://m3.material.io/
- **Compose Animation:** https://developer.android.com/jetpack/compose/animation

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-01-07 | Initial creation based on Browser module |

---

## Summary

This pattern provides:
- ✅ **Zero permanent space** (0dp when hidden)
- ✅ **Full command visibility** (no scrolling)
- ✅ **Voice-first UX** (FAB trigger, mic button)
- ✅ **Cascading navigation** (organized hierarchy)
- ✅ **Reusable across modules** (Notepad, FileManager, etc.)

**Estimated Implementation Time:** 2-4 hours (including testing)

**Complexity:** Medium (requires understanding of Compose state, navigation, and animation)

**Recommended For:** Any module with 10+ voice commands that needs maximum screen space for content.

---

**End of Guide**
