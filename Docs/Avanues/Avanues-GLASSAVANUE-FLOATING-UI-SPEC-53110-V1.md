# GlassAvanue Floating UI Components Specification

**Platform**: Avanues Ecosystem
**Theme**: GlassAvanue
**Component Type**: Floating Command Bar & Navigation
**Version**: 1.0.0
**Date**: 2025-10-31
**Created by**: Manoj Jhawar, manoj@ideahq.net

---

## Overview

Based on the VoiceOS Launcher reference design, the **FloatingCommandBar** and **FloatingNavigation** components are core UI elements of the GlassAvanue theme. These components provide:

- **Movable glass panels** that can be positioned at any screen edge
- **Portrait or landscape orientation** based on position
- **Adaptive layout** that responds to placement
- **Glass aesthetic** matching the GlassAvanue design system

---

## 🎨 Reference Design Analysis

### From Image: VoiceOS Launcher Layout

```
┌─────────────────────────────────────────────────────┐
│  ←    VoiceOS Launcher                         ⚙️  │ Top Bar (glass)
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─┐   ┌────────────────────────────────────────┐  │
│  │🏠│   │  Top Apps                              │  │
│  │🔍│   │  ┌──┐  ┌──┐  ┌──┐  ┌──┐  ┌──┐         │  │
│  │🎤│   │  │💬│  │📝│  │✓│  │📷│  │📍│         │  │
│  │📹│   │  AVA  Notes TaskFlow Lens NavAR      │  │
│  │⚙️│   └────────────────────────────────────────┘  │
│  └─┘                                                │
│  Nav    ┌────────────────┐  ┌──────────────────┐   │
│  (glass)│ Pinned Apps    │  │ Continue Sessions│   │
│         │ 🌐 💬 📅 🖼️ ⚙️  │  │ AVA Chat         │   │
│         │                │  │ Lens View        │   │
│         └────────────────┘  └──────────────────┘   │
│                                                     │
├─────────────────────────────────────────────────────┤
│  ⏻   🎤  Listening...       📶  🌐         📶  🔋  │ Bottom Bar (glass)
└─────────────────────────────────────────────────────┘

Layout Components:
- Left: FloatingNavigation (vertical, 5 icons)
- Top: TopBar (horizontal, title + actions)
- Center: MainPanel (dashboard content)
- Bottom: VoiceBar (horizontal, listening status)
```

---

## 📋 Component 1: FloatingCommandBar

### Purpose
A movable, orientation-adaptive glass panel that can be positioned at any screen edge (left, right, top, bottom) and automatically adjusts between portrait (vertical) and landscape (horizontal) layouts.

### Features
- **Movable**: Can be dragged to any screen edge
- **Adaptive**: Auto-switches between vertical/horizontal based on edge
- **Collapsible**: Can expand/collapse to show labels or icons only
- **Glass Styled**: Full GlassAvanue theme integration
- **Persistent**: Saves position across sessions

### Position Modes

| Position | Orientation | Icon Layout | Label Position |
|----------|-------------|-------------|----------------|
| **Left** | Vertical (Portrait) | Stacked vertically | Right of icon |
| **Right** | Vertical (Portrait) | Stacked vertically | Left of icon |
| **Top** | Horizontal (Landscape) | Row | Below icon |
| **Bottom** | Horizontal (Landscape) | Row | Above icon |

### Properties

```kotlin
data class FloatingCommandBar(
    val id: String,

    // Position & Orientation
    var position: CommandBarPosition = CommandBarPosition.Left,
    var orientation: Orientation = Orientation.Vertical, // Auto-set by position

    // Appearance
    var collapsed: Boolean = false,  // Icons only vs icons + labels
    var size: CommandBarSize = CommandBarSize.Medium,

    // Behavior
    var draggable: Boolean = true,
    var snapToEdge: Boolean = true,
    var autoHide: Boolean = false,  // Hide when not in use
    var showOnHover: Boolean = false,

    // Content
    var items: List<CommandBarItem>,

    // Style (inherits from GlassAvanue)
    var glassOpacity: Float = 0.75f,  // 75% glass
    var blurRadius: Float = 25f,       // 25px blur
    var cornerRadius: Float = 24f,     // 24px corners

    // Callbacks
    var onPositionChange: ((CommandBarPosition) -> Unit)? = null,
    var onItemClick: ((CommandBarItem) -> Unit)? = null
) : Component

enum class CommandBarPosition {
    Left,      // → Vertical orientation
    Right,     // → Vertical orientation
    Top,       // → Horizontal orientation
    Bottom,    // → Horizontal orientation
    FloatingCustom  // Custom x,y position (stays horizontal/vertical as set)
}

enum class CommandBarSize {
    Small,   // 48dp width/height
    Medium,  // 64dp width/height (default)
    Large    // 80dp width/height
}

data class CommandBarItem(
    val id: String,
    val icon: String,  // Icon name or SF Symbol
    val label: String,
    val badge: String? = null,  // Optional badge text
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)
```

### Layout Behavior

#### **Vertical (Left/Right Position)**
```
┌────────┐
│ 🏠     │  ← Home icon
│ Home   │  ← Label (if not collapsed)
├────────┤
│ 🔍     │  ← Search icon
│ Search │  ← Label
├────────┤
│ 🎤     │
│ Voice  │
├────────┤
│ 📹     │
│ Video  │
├────────┤
│ ⚙️     │
│ Settings│
└────────┘

Width: 64dp (icons only) or 120dp (with labels)
Height: Auto (based on item count)
Padding: 16dp vertical, 8dp horizontal
Spacing: 12dp between items
```

#### **Horizontal (Top/Bottom Position)**
```
┌───────────────────────────────────────────┐
│  🏠    🔍    🎤    📹    ⚙️                │  ← Icons
│ Home Search Voice Video Settings         │  ← Labels (if not collapsed)
└───────────────────────────────────────────┘

Width: Auto (based on item count)
Height: 64dp (icons only) or 88dp (with labels)
Padding: 8dp horizontal, 16dp vertical
Spacing: 24dp between items
```

### Glass Styling (GlassAvanue)

```kotlin
// Automatic glass styling based on GlassAvanue theme
modifier
    .background(
        color = GlassAvanue.colorScheme.surface,  // 75% white/black
        shape = RoundedCornerShape(24.dp)
    )
    .blur(25.dp)  // Frosted glass effect
    .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(24.dp),
        ambientColor = Color.Black.copy(alpha = 0.12f)
    )
```

### Animation

```kotlin
// Position change animation
AnimatedContent(
    targetState = position,
    transitionSpec = {
        fadeIn(animationSpec = tween(250)) +
        scaleIn(initialScale = 0.95f, animationSpec = tween(250)) with
        fadeOut(animationSpec = tween(200)) +
        scaleOut(targetScale = 0.95f, animationSpec = tween(200))
    }
)

// Collapse/expand animation
AnimatedVisibility(
    visible = !collapsed,
    enter = fadeIn(tween(250)) + expandHorizontally(tween(250)),
    exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200))
)
```

---

## 📋 Component 2: FloatingNavigation

### Purpose
Specialized version of FloatingCommandBar optimized for main app navigation. Pre-configured with common navigation items.

### Default Configuration

```kotlin
object FloatingNavigation {
    fun default() = FloatingCommandBar(
        id = "main_navigation",
        position = CommandBarPosition.Left,
        collapsed = false,
        items = listOf(
            CommandBarItem("home", "home", "Home"),
            CommandBarItem("search", "search", "Search"),
            CommandBarItem("voice", "mic", "Voice"),
            CommandBarItem("video", "videocam", "Video"),
            CommandBarItem("settings", "settings", "Settings")
        )
    )
}
```

### VoiceOS Launcher Variant

```kotlin
object VoiceOSNavigation {
    fun launcher() = FloatingCommandBar(
        id = "voiceos_nav",
        position = CommandBarPosition.Left,
        size = CommandBarSize.Medium,
        collapsed = false,
        items = listOf(
            CommandBarItem("home", "home", "Home") { /* Go to home */ },
            CommandBarItem("search", "search", "Search") { /* Open search */ },
            CommandBarItem("voice", "mic", "Voice") { /* Voice input */ },
            CommandBarItem("apps", "apps", "Library") { /* App drawer */ },
            CommandBarItem("settings", "settings", "Settings") { /* Open settings */ }
        ),
        glassOpacity = 0.75f,
        blurRadius = 25f,
        cornerRadius = 24f
    )
}
```

---

## 🎨 Integration with GlassAvanue Theme

### Theme Extension

Update `GlassAvanue` theme to include floating UI defaults:

```kotlin
data class Theme(
    // ... existing properties ...

    // NEW: Floating UI configuration
    val floatingUI: FloatingUIConfig = FloatingUIConfig()
)

data class FloatingUIConfig(
    // Command bar defaults
    val commandBarOpacity: Float = 0.75f,
    val commandBarBlur: Float = 25f,
    val commandBarCorners: Float = 24f,
    val commandBarPadding: Float = 16f,
    val commandBarSpacing: Float = 12f,

    // Position defaults
    val defaultPosition: CommandBarPosition = CommandBarPosition.Left,
    val allowDragging: Boolean = true,
    val snapToEdges: Boolean = true,

    // Behavior defaults
    val autoHideEnabled: Boolean = false,
    val showOnHoverEnabled: Boolean = false,
    val collapsedByDefault: Boolean = false
)
```

### Usage in AvaUI

```kotlin
val launcherUI = AvaUI {
    theme = GlassAvanue.Light

    // Floating navigation automatically gets theme styling
    FloatingNavigation.launcher()

    Column {
        TopBar("VoiceOS Launcher")

        MainDashboard {
            TopAppsGrid()
            PinnedApps()
            ContinueSessions()
        }

        BottomVoiceBar("Listening...")
    }
}
```

---

## 📱 Platform Implementation

### Android (Compose)

```kotlin
@Composable
fun FloatingCommandBar(
    commandBar: FloatingCommandBar,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableStateOf(commandBar.position) }
    var isDragging by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val orientation = when (currentPosition) {
        CommandBarPosition.Left, CommandBarPosition.Right -> Orientation.Vertical
        CommandBarPosition.Top, CommandBarPosition.Bottom -> Orientation.Horizontal
        CommandBarPosition.FloatingCustom -> commandBar.orientation
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
            .pointerInput(Unit) {
                if (commandBar.draggable) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            if (commandBar.snapToEdge) {
                                // Snap to nearest edge and update position
                                val snappedPosition = snapToNearestEdge(offset)
                                currentPosition = snappedPosition
                                commandBar.onPositionChange?.invoke(snappedPosition)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offset += dragAmount
                        }
                    )
                }
            }
    ) {
        // Glass panel
        Surface(
            modifier = Modifier
                .background(
                    color = GlassAvanue.Light.colorScheme.surface,
                    shape = RoundedCornerShape(commandBar.cornerRadius.dp)
                )
                .blur(commandBar.blurRadius.dp),
            color = Color.Transparent,
            shape = RoundedCornerShape(commandBar.cornerRadius.dp)
        ) {
            // Layout based on orientation
            if (orientation == Orientation.Vertical) {
                VerticalCommandBarLayout(commandBar)
            } else {
                HorizontalCommandBarLayout(commandBar)
            }
        }
    }
}

@Composable
private fun VerticalCommandBarLayout(commandBar: FloatingCommandBar) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        commandBar.items.forEach { item ->
            CommandBarItemView(item, commandBar.collapsed, Orientation.Vertical)
        }
    }
}

@Composable
private fun HorizontalCommandBarLayout(commandBar: FloatingCommandBar) {
    Row(
        modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        commandBar.items.forEach { item ->
            CommandBarItemView(item, commandBar.collapsed, Orientation.Horizontal)
        }
    }
}

@Composable
private fun CommandBarItemView(
    item: CommandBarItem,
    collapsed: Boolean,
    orientation: Orientation
) {
    val arrangement = if (orientation == Orientation.Vertical) {
        Column(horizontalAlignment = Alignment.CenterHorizontally)
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally)
    }

    arrangement {
        // Icon
        Icon(
            imageVector = Icons.Default.getIcon(item.icon),
            contentDescription = item.label,
            modifier = Modifier
                .size(24.dp)
                .clickable(enabled = item.enabled) { item.onClick?.invoke() },
            tint = if (item.enabled) {
                GlassAvanue.Light.colorScheme.onSurface
            } else {
                GlassAvanue.Light.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )

        // Label (if not collapsed)
        AnimatedVisibility(visible = !collapsed) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.label,
                style = GlassAvanue.Light.typography.labelSmall,
                color = GlassAvanue.Light.colorScheme.onSurface
            )
        }

        // Badge (if present)
        item.badge?.let { badge ->
            Badge(
                modifier = Modifier.offset(x = 12.dp, y = (-12).dp)
            ) {
                Text(badge, style = GlassAvanue.Light.typography.labelSmall)
            }
        }
    }
}

private fun snapToNearestEdge(currentOffset: Offset): CommandBarPosition {
    // Calculate which edge is closest
    val screenWidth = /* get screen width */
    val screenHeight = /* get screen height */

    val distToLeft = currentOffset.x
    val distToRight = screenWidth - currentOffset.x
    val distToTop = currentOffset.y
    val distToBottom = screenHeight - currentOffset.y

    return when (listOf(distToLeft, distToRight, distToTop, distToBottom).minOrNull()) {
        distToLeft -> CommandBarPosition.Left
        distToRight -> CommandBarPosition.Right
        distToTop -> CommandBarPosition.Top
        else -> CommandBarPosition.Bottom
    }
}
```

### iOS (SwiftUI)

```swift
struct FloatingCommandBar: View {
    @State var commandBar: CommandBarModel
    @State private var isDragging = false
    @State private var offset: CGSize = .zero

    var body: some View {
        ZStack {
            // Glass panel
            Group {
                if commandBar.orientation == .vertical {
                    VStack(spacing: 12) {
                        ForEach(commandBar.items) { item in
                            CommandBarItemView(item: item, collapsed: commandBar.collapsed)
                        }
                    }
                } else {
                    HStack(spacing: 24) {
                        ForEach(commandBar.items) { item in
                            CommandBarItemView(item: item, collapsed: commandBar.collapsed)
                        }
                    }
                }
            }
            .padding(16)
            .background(.ultraThinMaterial)  // Glass effect
            .cornerRadius(commandBar.cornerRadius)
            .shadow(color: .black.opacity(0.12), radius: 8, x: 0, y: 4)
        }
        .offset(offset)
        .gesture(
            DragGesture()
                .onChanged { value in
                    if commandBar.draggable {
                        isDragging = true
                        offset = value.translation
                    }
                }
                .onEnded { value in
                    isDragging = false
                    if commandBar.snapToEdge {
                        let snapped = snapToNearestEdge(offset: offset)
                        commandBar.position = snapped
                        withAnimation(.easeInOut(duration: 0.25)) {
                            offset = .zero
                        }
                    }
                }
        )
    }
}
```

---

## 🎯 VoiceOS Launcher Complete Example

### Full Launcher Layout

```kotlin
@Composable
fun VoiceOSLauncher() {
    AvaUI {
        theme = GlassAvanue.Light

        Box(modifier = Modifier.fillMaxSize()) {
            // Background (AR-transparent or wallpaper)
            BackgroundLayer()

            // Main content
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                TopBar(
                    title = "VoiceOS Launcher",
                    navigationIcon = { BackButton() },
                    actions = { SettingsButton() }
                )

                // Main dashboard
                MainDashboard(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    // Top Apps section
                    TopAppsGrid(
                        apps = listOf(
                            App("AVA Chat", "AI Companion", "chat_icon"),
                            App("Spatial Notes", "AR Notes", "notes_icon"),
                            App("TaskFlow", "Task Manager", "tasks_icon"),
                            App("Lens View", "Smart Camera", "camera_icon"),
                            App("NavAR", "Navigation", "nav_icon")
                        )
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Pinned apps
                        PinnedAppsPanel(
                            modifier = Modifier.weight(1f),
                            apps = listOf(
                                "Browser" to "12 Tabs",
                                "Messages" to "3 Threads",
                                "Calendar" to "2 Events",
                                "Gallery" to "356 Photos",
                                "Settings" to "System Tools"
                            )
                        )

                        Spacer(Modifier.width(16.dp))

                        // Continue sessions
                        ContinueSessionsPanel(
                            modifier = Modifier.weight(1f),
                            sessions = listOf(
                                Session("AVA Chat", "Conversation with Alex", "Resume"),
                                Session("Lens View", "Object Collection active", "Open in Space")
                            )
                        )
                    }
                }

                // Bottom voice bar
                BottomVoiceBar(
                    status = "Listening...",
                    onVoiceClick = { /* Handle voice input */ }
                )
            }

            // Floating navigation (LEFT SIDE)
            FloatingNavigation.launcher()
        }
    }
}
```

---

## 📊 Summary

### New Components Added
1. **FloatingCommandBar** - Movable, adaptive glass panel
2. **FloatingNavigation** - Pre-configured navigation variant
3. **Theme Extensions** - FloatingUIConfig in GlassAvanue

### Key Features
- ✅ Movable to any edge (left/right/top/bottom)
- ✅ Auto-orientation (vertical/horizontal)
- ✅ Collapsible (icons only or icons + labels)
- ✅ Draggable with snap-to-edge
- ✅ Full GlassAvanue theme integration
- ✅ Animations (250ms ease-in-out)

### Platform Support
- ✅ Android (Jetpack Compose)
- ✅ iOS (SwiftUI with .ultraThinMaterial)
- ✅ Matches reference image design exactly

---

**Next**: Implement these components in actual Kotlin/Swift code and integrate into the launcher.

**Created by**: Manoj Jhawar, manoj@ideahq.net
**Version**: 1.0.0
**Date**: 2025-10-31
