# VoiceOS 4 Overlay API Reference

**Created:** 2025-10-09 04:03:00 PDT
**Module:** VoiceAccessibility
**Package:** `com.augmentalis.voiceos.accessibility.overlays`

## Quick Reference

### File Summary
| File | Size | Composables | Purpose |
|------|------|-------------|---------|
| ConfidenceOverlay.kt | 7.2KB | 1 | Real-time confidence indicator |
| NumberedSelectionOverlay.kt | 9.0KB | 4 | Numbered item selection |
| CommandStatusOverlay.kt | 10KB | 2 | Command processing status |
| ContextMenuOverlay.kt | 10KB | 4 | Voice-activated context menu |
| OverlayManager.kt | 7.7KB | 0 | Centralized overlay management |
| OverlayIntegrationExample.kt | 13KB | 0 | Integration examples (reference) |

---

## 1. ConfidenceOverlay API

### Constructor
```kotlin
class ConfidenceOverlay(
    private val context: Context,
    private val windowManager: WindowManager
)
```

### Public Methods

#### show()
```kotlin
fun show(confidenceResult: ConfidenceResult)
```
Shows the confidence overlay with initial result.

**Parameters:**
- `confidenceResult`: ConfidenceResult from SpeechRecognition module

**Example:**
```kotlin
val result = ConfidenceResult(
    text = "open settings",
    confidence = 0.92f,
    level = ConfidenceLevel.HIGH,
    alternates = emptyList(),
    scoringMethod = ScoringMethod.VOSK_ACOUSTIC
)
overlay.show(result)
```

#### updateConfidence()
```kotlin
fun updateConfidence(result: ConfidenceResult)
```
Updates confidence values without recreating overlay. Use for real-time updates.

**Example:**
```kotlin
// Update every 100ms during recognition
recognitionEngine.onPartialResult { partial ->
    overlay.updateConfidence(partial)
}
```

#### hide()
```kotlin
fun hide()
```
Hides the confidence overlay.

#### isVisible()
```kotlin
fun isVisible(): Boolean
```
Returns true if overlay is currently visible.

#### dispose()
```kotlin
fun dispose()
```
Cleans up resources. Call in onDestroy().

### Compose UI Implementation

```kotlin
@Composable
private fun ConfidenceIndicatorUI(
    confidence: Float,
    level: ConfidenceLevel,
    text: String
) {
    // Animated color based on confidence level
    val targetColor = when (level) {
        ConfidenceLevel.HIGH -> Color(0xFF4CAF50)      // Green
        ConfidenceLevel.MEDIUM -> Color(0xFFFFEB3B)    // Yellow
        ConfidenceLevel.LOW -> Color(0xFFFF9800)       // Orange
        ConfidenceLevel.REJECT -> Color(0xFFF44336)    // Red
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300)
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xEE000000)
        )
    ) {
        Row {
            // Confidence circle
            Canvas(modifier = Modifier.size(28.dp)) {
                drawCircle(color = animatedColor)
            }

            Column {
                // Percentage
                Text(
                    text = "${(confidence * 100).toInt()}%",
                    color = animatedColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Level
                Text(
                    text = level.name,
                    fontSize = 10.sp
                )
            }
        }
    }
}
```

---

## 2. NumberedSelectionOverlay API

### Constructor
```kotlin
class NumberedSelectionOverlay(
    private val context: Context,
    private val windowManager: WindowManager
)
```

### Public Methods

#### showItems()
```kotlin
fun showItems(items: List<SelectableItem>)
```
Shows overlay with numbered items.

**Parameters:**
- `items`: List of SelectableItem with bounds and actions

**Example:**
```kotlin
val items = listOf(
    SelectableItem(
        number = 1,
        label = "Settings",
        bounds = Rect(100, 200, 300, 250),
        action = { openSettings() }
    ),
    SelectableItem(
        number = 2,
        label = "System",
        bounds = Rect(100, 300, 300, 350),
        action = { openSystem() }
    )
)
overlay.showItems(items)
```

#### updateItems()
```kotlin
fun updateItems(items: List<SelectableItem>)
```
Updates items without recreating overlay.

#### selectItem()
```kotlin
fun selectItem(number: Int): Boolean
```
Selects item by number. Returns true if successful.

**Example:**
```kotlin
// User says "select 2"
val success = overlay.selectItem(2)
if (success) {
    overlay.hide()
}
```

#### hide()
```kotlin
fun hide()
```

#### isVisible()
```kotlin
fun isVisible(): Boolean
```

#### dispose()
```kotlin
fun dispose()
```

### Data Classes

#### SelectableItem
```kotlin
data class SelectableItem(
    val number: Int,
    val label: String,
    val bounds: Rect,
    val action: () -> Unit
)
```

### Compose UI Implementation

```kotlin
@Composable
private fun NumberBadge(
    number: Int,
    label: String,
    bounds: Rect
) {
    Box(
        modifier = Modifier.absoluteOffset(
            x = bounds.left.dp,
            y = bounds.top.dp
        )
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xEE2196F3))
                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Number circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Bold
                )
            }

            // Label
            Text(
                text = label.take(20),
                color = Color.White
            )
        }
    }
}
```

---

## 3. CommandStatusOverlay API

### Constructor
```kotlin
class CommandStatusOverlay(
    private val context: Context,
    private val windowManager: WindowManager
)
```

### Public Methods

#### showStatus()
```kotlin
fun showStatus(
    command: String,
    state: CommandState,
    message: String? = null
)
```
Shows status with command and state.

**Parameters:**
- `command`: Command text to display
- `state`: Current command state (LISTENING, PROCESSING, EXECUTING, SUCCESS, ERROR)
- `message`: Optional status message

**Example:**
```kotlin
overlay.showStatus(
    command = "open camera",
    state = CommandState.PROCESSING,
    message = "Recognizing..."
)
```

#### updateStatus()
```kotlin
fun updateStatus(
    command: String? = null,
    state: CommandState? = null,
    message: String? = null
)
```
Updates status without recreating overlay.

**Example:**
```kotlin
// Update only the state
overlay.updateStatus(state = CommandState.SUCCESS)

// Update state and message
overlay.updateStatus(
    state = CommandState.ERROR,
    message = "Command not found"
)
```

#### hide()
```kotlin
fun hide()
```

#### isVisible()
```kotlin
fun isVisible(): Boolean
```

#### dispose()
```kotlin
fun dispose()
```

### Enums

#### CommandState
```kotlin
enum class CommandState {
    LISTENING,      // Blue mic icon (pulsing)
    PROCESSING,     // Amber hourglass (rotating)
    EXECUTING,      // Purple play arrow (rotating)
    SUCCESS,        // Green check mark
    ERROR          // Red X icon
}
```

### Compose UI Implementation

```kotlin
@Composable
private fun StateIcon(state: CommandState) {
    val (icon, color) = getStateIconAndColor(state)

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == CommandState.LISTENING) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = state.name,
            tint = color,
            modifier = Modifier
                .size(32.dp)
                .scale(scale)
        )
    }
}

private fun getStateIconAndColor(state: CommandState): Pair<ImageVector, Color> {
    return when (state) {
        CommandState.LISTENING -> Icons.Default.Mic to Color(0xFF2196F3)
        CommandState.PROCESSING -> Icons.Default.HourglassEmpty to Color(0xFFFFC107)
        CommandState.EXECUTING -> Icons.Default.PlayArrow to Color(0xFF9C27B0)
        CommandState.SUCCESS -> Icons.Default.Check to Color(0xFF4CAF50)
        CommandState.ERROR -> Icons.Default.Close to Color(0xFFF44336)
    }
}
```

---

## 4. ContextMenuOverlay API

### Constructor
```kotlin
class ContextMenuOverlay(
    private val context: Context,
    private val windowManager: WindowManager
)
```

### Public Methods

#### showMenu()
```kotlin
fun showMenu(
    items: List<MenuItem>,
    title: String? = null
)
```
Shows menu at center of screen.

**Example:**
```kotlin
val items = listOf(
    MenuItem(
        id = "scroll_down",
        label = "Scroll Down",
        icon = Icons.Default.ArrowDownward,
        number = 1,
        action = { performScrollDown() }
    ),
    MenuItem(
        id = "go_back",
        label = "Go Back",
        icon = Icons.Default.ArrowBack,
        number = 2,
        action = { performBackNavigation() }
    )
)
overlay.showMenu(items, title = "Available Commands")
```

#### showMenuAt()
```kotlin
fun showMenuAt(
    items: List<MenuItem>,
    position: Point?,
    title: String? = null
)
```
Shows menu at specific position.

**Example:**
```kotlin
overlay.showMenuAt(
    items = menuItems,
    position = Point(500, 300),
    title = "Voice Commands"
)
```

#### updateItems()
```kotlin
fun updateItems(items: List<MenuItem>)
```

#### selectItemById()
```kotlin
fun selectItemById(id: String): Boolean
```

#### selectItemByNumber()
```kotlin
fun selectItemByNumber(number: Int): Boolean
```

#### hide()
```kotlin
fun hide()
```

#### isVisible()
```kotlin
fun isVisible(): Boolean
```

#### dispose()
```kotlin
fun dispose()
```

### Data Classes

#### MenuItem
```kotlin
data class MenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val number: Int? = null,
    val enabled: Boolean = true,
    val action: () -> Unit
)
```

### Enums

#### MenuPosition
```kotlin
enum class MenuPosition {
    CENTER,      // Center of screen
    AT_POINT,    // At specific point
    CURSOR       // At cursor/focus position
}
```

### Compose UI Implementation

```kotlin
@Composable
private fun ContextMenuItemUI(item: MenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled) { item.action() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Number badge
        item.number?.let { number ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2196F3)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Icon
        item.icon?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = item.label,
                tint = if (item.enabled) Color.White else Color.Gray
            )
        }

        // Label
        Text(
            text = item.label,
            color = if (item.enabled) Color.White else Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}
```

---

## 5. OverlayManager API

### Constructor
```kotlin
class OverlayManager(private val context: Context)
```

### Singleton Access
```kotlin
companion object {
    fun getInstance(context: Context): OverlayManager
}
```

**Example:**
```kotlin
val overlayManager = OverlayManager.getInstance(this)
```

### Confidence Overlay Methods

```kotlin
fun showConfidence(result: ConfidenceResult)
fun updateConfidence(result: ConfidenceResult)
fun hideConfidence()
```

### Numbered Selection Methods

```kotlin
fun showNumberedSelection(items: List<SelectableItem>)
fun updateNumberedSelection(items: List<SelectableItem>)
fun selectNumberedItem(number: Int): Boolean
fun hideNumberedSelection()
```

### Command Status Methods

```kotlin
fun showCommandStatus(command: String, state: CommandState, message: String? = null)
fun updateCommandStatus(command: String? = null, state: CommandState? = null, message: String? = null)
fun hideCommandStatus()
```

### Context Menu Methods

```kotlin
fun showContextMenu(items: List<MenuItem>, title: String? = null)
fun showContextMenuAt(items: List<MenuItem>, position: Point, title: String? = null)
fun selectContextMenuItem(id: String): Boolean
fun selectContextMenuByNumber(number: Int): Boolean
fun hideContextMenu()
```

### Convenience Methods

```kotlin
fun showListening(partialText: String = "")
fun showProcessing(command: String)
fun showExecuting(command: String)
fun showSuccess(command: String, message: String? = null)
fun showError(command: String, error: String)
suspend fun dismissAfterDelay(delayMs: Long = 2000)
```

**Example:**
```kotlin
overlayManager.showListening()
delay(1000)
overlayManager.showProcessing("open camera")
delay(500)
overlayManager.showExecuting("open camera")
delay(500)
overlayManager.showSuccess("open camera", "Camera opened")
overlayManager.dismissAfterDelay(2000)
```

### Global Control Methods

```kotlin
fun hideAll()
fun isAnyVisible(): Boolean
fun isOverlayVisible(overlayName: String): Boolean
fun getActiveOverlays(): Set<String>
fun dispose()
```

---

## Common Usage Patterns

### 1. Complete Voice Command Flow

```kotlin
val overlayManager = OverlayManager.getInstance(context)

// 1. Start listening
overlayManager.showListening()

// 2. Partial results
onPartialResult { partial ->
    overlayManager.showProcessing(partial)
}

// 3. Final result with confidence
onFinalResult { result ->
    overlayManager.showConfidence(result)

    when (result.level) {
        ConfidenceLevel.HIGH -> {
            // Execute immediately
            overlayManager.showExecuting(result.text)
            executeCommand(result.text)
            overlayManager.showSuccess(result.text)
        }
        ConfidenceLevel.MEDIUM, ConfidenceLevel.LOW -> {
            // Show alternatives
            val items = createSelectableItems(result.alternates)
            overlayManager.showNumberedSelection(items)
        }
        ConfidenceLevel.REJECT -> {
            overlayManager.showError(result.text, "Not recognized")
        }
    }
}
```

### 2. Show Available Commands Menu

```kotlin
fun showHelpMenu() {
    val menuItems = listOf(
        MenuItem("scroll_down", "Scroll Down", Icons.Default.ArrowDownward, 1) {
            performScrollDown()
        },
        MenuItem("scroll_up", "Scroll Up", Icons.Default.ArrowUpward, 2) {
            performScrollUp()
        },
        MenuItem("go_back", "Go Back", Icons.Default.ArrowBack, 3) {
            performBackNavigation()
        },
        MenuItem("go_home", "Go Home", Icons.Default.Home, 4) {
            performHomeNavigation()
        }
    )

    overlayManager.showContextMenu(menuItems, "Available Commands")
}
```

### 3. Disambiguation with Numbered Selection

```kotlin
fun handleMultipleMatches(matches: List<SearchResult>) {
    val items = matches.mapIndexed { index, result ->
        SelectableItem(
            number = index + 1,
            label = result.title,
            bounds = result.bounds,
            action = { selectResult(result) }
        )
    }

    overlayManager.showNumberedSelection(items)

    // User says "select 3"
    // Handle in voice recognition callback:
    onVoiceCommand("select", number = 3) {
        overlayManager.selectNumberedItem(number)
        overlayManager.hideNumberedSelection()
    }
}
```

---

## WindowManager Configuration Reference

### TYPE_ACCESSIBILITY_OVERLAY
All overlays use `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY`:
- Requires AccessibilityService to be active
- No SYSTEM_ALERT_WINDOW permission needed
- Works automatically when service is enabled
- Proper z-ordering above app windows

### Common Flags

#### Non-interactive overlay (ConfidenceOverlay):
```kotlin
flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
```

#### Interactive overlay (NumberedSelection, CommandStatus, ContextMenu):
```kotlin
flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
```

### Positioning

#### Top-Right (ConfidenceOverlay):
```kotlin
gravity = Gravity.TOP or Gravity.END
x = 16
y = 16
```

#### Top-Center (CommandStatusOverlay):
```kotlin
gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
y = 80
```

#### Center (ContextMenuOverlay):
```kotlin
gravity = Gravity.CENTER
```

#### Full Screen (NumberedSelectionOverlay):
```kotlin
gravity = Gravity.FILL
width = WindowManager.LayoutParams.MATCH_PARENT
height = WindowManager.LayoutParams.MATCH_PARENT
```

---

## Animation Reference

### Color Transitions
```kotlin
val animatedColor by animateColorAsState(
    targetValue = targetColor,
    animationSpec = tween(durationMillis = 300)
)
```

### Value Animations
```kotlin
val animatedConfidence by animateFloatAsState(
    targetValue = confidence,
    animationSpec = tween(durationMillis = 200)
)
```

### Infinite Animations (Pulsing)
```kotlin
val scale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.2f,
    animationSpec = infiniteRepeatable(
        animation = tween(600),
        repeatMode = RepeatMode.Reverse
    )
)
```

### Visibility Animations
```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + slideInVertically(),
    exit = fadeOut() + slideOutVertically()
) {
    // Content
}
```

---

## Error Handling

All overlay methods include proper error handling:

```kotlin
fun hide() {
    overlayView?.let {
        if (isShowing) {
            try {
                windowManager.removeView(it)
                isShowing = false
            } catch (e: IllegalArgumentException) {
                // View not attached, ignore
            }
        }
    }
}
```

---

## Lifecycle Management

### Initialization (onCreate)
```kotlin
override fun onCreate() {
    super.onCreate()
    overlayManager = OverlayManager.getInstance(this)
}
```

### Cleanup (onDestroy)
```kotlin
override fun onDestroy() {
    overlayManager.dispose()
    super.onDestroy()
}
```

### State Preservation
Overlays maintain state across show/hide cycles:
```kotlin
// State preserved
overlay.show(result)
overlay.hide()
overlay.show(result)  // Shows same state
```

---

## Performance Considerations

1. **Lazy Initialization**: Overlays created only when first used
2. **View Reuse**: ComposeView reused when showing/hiding
3. **State Preservation**: State maintained across lifecycle
4. **Memory Efficiency**: Small footprint when hidden
5. **Thread Safety**: All methods are main-thread safe

---

## Material Design 3 Compliance

All overlays follow Material 3 guidelines:

- **Elevation**: 8dp-16dp for cards
- **Corner Radius**: 12dp-24dp
- **Color Palette**: Consistent across overlays
- **Typography**: Material 3 font scale
- **Spacing**: 8dp grid system
- **Animations**: Material motion

---

**Last Updated:** 2025-10-09 04:03:00 PDT
**Module Version:** VOS4
**Kotlin Version:** 1.9.20
**Compose Version:** Material 3
