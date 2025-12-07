# VoiceUI Magic Freeform Windows Documentation

## 🪟 Overview

The MagicWindowSystem brings desktop-class windowing capabilities to Android, allowing multiple resizable, draggable windows that inherit VoiceUI themes and integrate with the UUID tracking system.

## 🚀 Quick Start

### Basic Window
```kotlin
@Composable
fun MyApp() {
    MagicWindow(
        title = "My Window",
        position = Offset(100f, 100f),
        size = DpSize(400.dp, 300.dp)
    ) {
        // Your content here
        Text("Hello from a freeform window!")
    }
}
```

### Animated Window
```kotlin
AnimatedMagicWindow(
    title = "Animated Window",
    animation = WindowAnimation.BOUNCE,
    theme = MagicDreamTheme
) {
    // Content with bouncy entrance
}
```

## 📦 Core Components

### MagicWindowManager
Singleton manager for all windows in the application.

```kotlin
// Create a window
val windowId = MagicWindowManager.createWindow(
    title = "New Window",
    initialPosition = Offset(200f, 200f),
    initialSize = DpSize(500.dp, 400.dp)
)

// Control windows
MagicWindowManager.focusWindow(windowId)
MagicWindowManager.minimizeWindow(windowId)
MagicWindowManager.maximizeWindow(windowId)
MagicWindowManager.closeWindow(windowId)

// Get window state
val windowState = MagicWindowManager.getWindow(windowId)
val allWindows = MagicWindowManager.getAllWindows()
```

### MagicWindowState
Tracks the complete state of a window.

```kotlin
data class MagicWindowState(
    val id: String,                          // UUID
    val title: String,                       // Window title
    val position: MutableState<Offset>,      // Current position
    val size: MutableState<DpSize>,          // Current size
    var zIndex: Int,                         // Layer order
    val isMinimized: MutableState<Boolean>,  // Minimize state
    val isMaximized: MutableState<Boolean>,  // Maximize state
    val isDragging: MutableState<Boolean>,   // Drag state
    val isResizing: MutableState<Boolean>,   // Resize state
    val config: MagicWindowConfig            // Configuration
)
```

### MagicWindowConfig
Configuration options for window behavior.

```kotlin
data class MagicWindowConfig(
    val resizable: Boolean = true,           // Allow resizing
    val draggable: Boolean = true,           // Allow dragging
    val closeable: Boolean = true,           // Show close button
    val minimizable: Boolean = true,         // Show minimize button
    val maximizable: Boolean = true,         // Show maximize button
    val alwaysOnTop: Boolean = false,        // Keep above others
    val transparency: Float = 1f,            // 0-1 opacity
    val showTitleBar: Boolean = true,        // Show title bar
    val enableSnapToEdge: Boolean = true,    // Snap to edges
    val snapThreshold: Dp = 20.dp,           // Snap distance
    val minSize: DpSize = DpSize(200.dp, 150.dp),  // Min size
    val maxSize: DpSize? = null,             // Max size (optional)
    val enableShadow: Boolean = true,        // Drop shadow
    val enableAnimation: Boolean = true,     // Animations
    val animationDuration: Int = 300         // Animation speed
)
```

## 🎨 Window Presets

Pre-configured window types for common use cases:

### Dialog Window
```kotlin
MagicWindow(
    title = "Confirm",
    config = MagicWindowPresets.dialog  // Fixed size, no maximize
) {
    // Dialog content
}
```

### Tool Window
```kotlin
MagicWindow(
    title = "Tools",
    config = MagicWindowPresets.toolWindow  // Always on top, transparent
) {
    // Tool palette content
}
```

### Notification
```kotlin
MagicWindow(
    title = "",
    config = MagicWindowPresets.notification  // No title bar, auto-dismiss
) {
    // Alert content
}
```

## ✨ Animations

Five animation types for window entrance/exit:

```kotlin
enum class WindowAnimation {
    FADE,    // Opacity transition
    SCALE,   // Zoom effect
    SLIDE,   // Slide from edge
    BOUNCE,  // Spring physics
    MORPH    // Shape transformation
}

// Usage
AnimatedMagicWindow(
    animation = WindowAnimation.BOUNCE,
    config = MagicWindowConfig(animationDuration = 500)
) {
    // Window content
}
```

## 🎤 Voice Commands

Windows automatically register voice commands:

| Command | Action |
|---------|--------|
| "minimize window" | Minimizes the focused window |
| "maximize window" | Toggles maximize state |
| "close window" | Closes the current window |
| "focus window [name]" | Brings named window to front |
| "move window left" | Moves window left |
| "move window right" | Moves window right |
| "resize window larger" | Increases window size |
| "resize window smaller" | Decreases window size |

### Custom Voice Commands
```kotlin
// Register custom commands for a window
val windowId = MagicWindowManager.createWindow(title = "Voice Window")

MagicUUIDIntegration.generateVoiceCommandUUID(
    command = "open settings in window",
    targetUUID = windowId,
    action = "open_settings"
)
```

## 🎨 Theme Integration

Windows automatically inherit the current theme:

```kotlin
@Composable
fun ThemedWindows() {
    // Window uses MagicDreamTheme
    MagicWindow(
        title = "Dream Window",
        theme = MagicDreamTheme
    ) {
        // Gradient backgrounds, rounded corners, etc.
    }
    
    // Window uses GreyARTheme  
    MagicWindow(
        title = "AR Window",
        theme = GreyARTheme
    ) {
        // Glassmorphic effects, transparency, etc.
    }
}
```

## 🆔 UUID Integration

Every window is tracked with a UUID:

```kotlin
// Create window with specific UUID
val customId = MagicUUIDIntegration.generateComponentUUID("window")
MagicWindow(
    windowId = customId,
    title = "Tracked Window"
) {
    // Window content
}

// Find window by UUID
val windowState = MagicWindowManager.getWindow(customId)

// Track window in UUID registry
val metadata = MagicUUIDIntegration.findComponent(uuid = customId)
```

## 📱 Responsive Behavior

### Screen Edge Snapping
Windows snap to screen edges when dragged near them:

```kotlin
MagicWindowConfig(
    enableSnapToEdge = true,
    snapThreshold = 20.dp  // Snap when within 20dp of edge
)
```

### Size Constraints
Set minimum and maximum window sizes:

```kotlin
MagicWindowConfig(
    minSize = DpSize(300.dp, 200.dp),
    maxSize = DpSize(800.dp, 600.dp)
)
```

### Resolution Independence
Windows adapt to different screen sizes:

```kotlin
val configuration = LocalConfiguration.current
val windowSize = if (configuration.screenWidthDp > 600) {
    DpSize(500.dp, 400.dp)  // Tablet
} else {
    DpSize(350.dp, 300.dp)  // Phone
}
```

## 🎮 Interactive Features

### Dragging
Click and drag the title bar to move windows:
- Smooth dragging with gesture detection
- Visual feedback during drag
- Snap to edges on release

### Resizing
Drag the bottom-right corner to resize:
- Visual resize handle
- Minimum size enforcement
- Aspect ratio preservation (optional)

### Z-Order Management
Click a window to bring it to front:
- Automatic z-index updates
- Focus management
- Visual focus indicators

## 💡 Advanced Usage

### Window Containers
Manage multiple windows with a container:

```kotlin
@Composable
fun MultiWindowApp() {
    MagicWindowContainer(
        theme = MagicDreamTheme
    ) {
        // Main app content
        
        // Windows are managed by the container
        repeat(3) { index ->
            MagicWindow(
                title = "Window $index",
                position = Offset(100f * index, 100f * index)
            ) {
                Text("Window $index content")
            }
        }
    }
}
```

### Custom Window Chrome
Create custom title bars and decorations:

```kotlin
MagicWindow(
    config = MagicWindowConfig(showTitleBar = false)
) {
    // Custom title bar
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(customGradient)
    ) {
        Text("Custom Title")
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { /* close */ }) {
            Icon(Icons.Default.Close, "Close")
        }
    }
    
    // Window content
    Box(modifier = Modifier.fillMaxSize()) {
        // Your content
    }
}
```

### Window Groups
Group related windows together:

```kotlin
class WindowGroup(val name: String) {
    private val windows = mutableListOf<String>()
    
    fun addWindow(id: String) {
        windows.add(id)
    }
    
    fun minimizeAll() {
        windows.forEach { 
            MagicWindowManager.minimizeWindow(it)
        }
    }
    
    fun cascadeWindows() {
        windows.forEachIndexed { index, id ->
            MagicWindowManager.getWindow(id)?.let { window ->
                window.position.value = Offset(
                    100f + (index * 30f),
                    100f + (index * 30f)
                )
            }
        }
    }
}
```

## 🎯 Best Practices

### 1. Window Lifecycle
Always clean up windows when done:
```kotlin
DisposableEffect(windowId) {
    onDispose {
        MagicWindowManager.closeWindow(windowId)
    }
}
```

### 2. Performance
Limit the number of simultaneous windows:
```kotlin
if (MagicWindowManager.getAllWindows().size < 10) {
    // Create new window
}
```

### 3. Accessibility
Provide keyboard navigation:
```kotlin
Modifier.onKeyEvent { event ->
    when (event.key) {
        Key.Escape -> {
            MagicWindowManager.closeWindow(windowId)
            true
        }
        else -> false
    }
}
```

### 4. State Persistence
Save window positions across sessions:
```kotlin
@Composable
fun PersistentWindow() {
    val savedPosition = remember {
        // Load from preferences
        Offset(100f, 100f)
    }
    
    MagicWindow(
        position = savedPosition,
        onPositionChanged = { newPos ->
            // Save to preferences
        }
    ) {
        // Content
    }
}
```

## 📊 Examples

### Chat Application
```kotlin
@Composable
fun ChatApp() {
    val windows = remember { mutableStateMapOf<String, ChatData>() }
    
    // Main chat list
    LazyColumn {
        items(chatList) { chat ->
            ChatItem(
                chat = chat,
                onClick = {
                    windows[chat.id] = chat
                }
            )
        }
    }
    
    // Chat windows
    windows.forEach { (id, chat) ->
        key(id) {
            ChatWindow(
                chat = chat,
                onClose = { windows.remove(id) }
            )
        }
    }
}
```

### Multi-Document Interface
```kotlin
@Composable
fun DocumentEditor() {
    val documents = remember { mutableStateListOf<Document>() }
    
    documents.forEach { doc ->
        MagicWindow(
            title = doc.name,
            size = DpSize(600.dp, 400.dp)
        ) {
            DocumentView(doc)
        }
    }
}
```

### Tool Palettes
```kotlin
@Composable
fun DesignApp() {
    // Main canvas
    Canvas()
    
    // Floating tool windows
    MagicWindow(
        title = "Colors",
        config = MagicWindowPresets.palette,
        position = Offset(20f, 100f)
    ) {
        ColorPicker()
    }
    
    MagicWindow(
        title = "Layers",
        config = MagicWindowPresets.palette,
        position = Offset(20f, 400f)
    ) {
        LayerList()
    }
}
```

## 🚀 Future Enhancements

### Planned Features
- Window docking and tiling
- Tab support within windows
- Window persistence across app restarts
- Remote window rendering
- Window sharing between apps
- Picture-in-picture mode
- Window animations library
- Advanced gesture controls
- Multi-monitor support (for desktop)

### API Roadmap
```kotlin
// Future API concepts
MagicWindow.dock(DockPosition.LEFT)
MagicWindow.tile(TileMode.VERTICAL)
MagicWindow.addTab(title = "Tab 2")
MagicWindow.saveLayout("my-layout")
MagicWindow.shareToApp("com.other.app")
```

## 📝 Migration Guide

### From Dialogs
```kotlin
// Before: AlertDialog
AlertDialog(
    onDismissRequest = { },
    title = { Text("Alert") },
    text = { Text("Message") },
    confirmButton = { Button(onClick = {}) { Text("OK") } }
)

// After: MagicWindow
MagicWindow(
    title = "Alert",
    config = MagicWindowPresets.dialog
) {
    Column {
        Text("Message")
        Button(onClick = {}) { Text("OK") }
    }
}
```

### From Bottom Sheets
```kotlin
// Before: ModalBottomSheet
ModalBottomSheet(
    onDismissRequest = { }
) {
    // Content
}

// After: MagicWindow with slide animation
AnimatedMagicWindow(
    animation = WindowAnimation.SLIDE,
    position = Offset(0f, screenHeight - 300f)
) {
    // Content
}
```

## 🎉 Summary

The MagicWindowSystem brings powerful windowing capabilities to VoiceUI Magic:
- **Freeform windows** with full drag, resize, minimize, maximize
- **Theme inheritance** for consistent styling
- **UUID tracking** for voice commands and state management
- **5 animation types** for smooth transitions
- **Window presets** for common use cases
- **Voice control** for accessibility
- **Performance optimized** with lazy rendering

This system enables desktop-class multi-window experiences on mobile devices while maintaining the simplicity and magic of VoiceUI Magic.