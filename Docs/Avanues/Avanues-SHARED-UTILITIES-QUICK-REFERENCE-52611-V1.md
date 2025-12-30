# AVAElements Shared Utilities - Quick Reference

**Last Updated:** 2025-11-26

---

## Import Statements

```kotlin
// Alignment utilities
import com.augmentalis.avaelements.common.alignment.*

// Color utilities
import com.augmentalis.avaelements.common.color.*

// Property extraction
import com.augmentalis.avaelements.common.properties.*

// Spacing utilities
import com.augmentalis.avaelements.common.spacing.*

// Input system
import com.augmentalis.avaelements.input.*
```

---

## Alignment (RTL-Aware)

```kotlin
// Convert alignment with RTL support
val horizontal = AlignmentConverter.wrapToHorizontal(
    WrapAlignment.Start,
    LayoutDirection.Rtl
)

// Extension functions
val h = WrapAlignment.Center.toHorizontalArrangement(layoutDir)
val v = MainAxisAlignment.SpaceBetween.toVerticalArrangement()

// Types
WrapAlignment { Start, End, Center, SpaceBetween, SpaceAround, SpaceEvenly }
MainAxisAlignment { Start, End, Center, SpaceBetween, SpaceAround, SpaceEvenly }
CrossAxisAlignment { Start, End, Center, Stretch, Baseline }
```

---

## Colors

```kotlin
// Create colors
val blue = UniversalColor.fromHex("#1E88E5")
val red = UniversalColor.fromArgb(0xFFFF0000.toInt())
val green = UniversalColor.fromHsl(120f, 0.8f, 0.5f)

// Manipulate colors
val lighter = color.lighten(0.2f)       // 20% lighter
val darker = color.darken(0.3f)         // 30% darker
val saturated = color.saturate(0.5f)    // More saturated
val translucent = color.withAlpha(0.5f) // 50% opacity

// Mix colors
val purple = UniversalColor.fromHex("#FF0000")
    .mix(UniversalColor.fromHex("#0000FF"), 0.5f)

// WCAG Accessibility
val contrast = ColorUtils.contrastRatio(foreground, background)
val meetsAA = ColorUtils.meetsWcagAA(fg, bg)   // 4.5:1
val meetsAAA = ColorUtils.meetsWcagAAA(fg, bg) // 7:1
val optimalFg = ColorUtils.contrastingForeground(bg)

// Color schemes
val complement = color.complementary()           // 180° opposite
val triad = ColorUtils.triadic(color)           // 120° apart
val analogous = ColorUtils.analogous(color)     // 30° apart
```

---

## Property Extraction

```kotlin
val props: Map<String, Any?> = mapOf(...)

// Basic types (with defaults)
val label = props.getString("label", "Default")
val enabled = props.getBoolean("enabled", true)
val count = props.getInt("count", 0)
val opacity = props.getFloat("opacity", 1.0f)

// Enums (case-insensitive)
val type = props.getEnum("type", ButtonType.Primary)

// Colors
val color = props.getColorArgb("color", 0xFF000000.toInt())
// Supports: #RGB, #RRGGBB, #AARRGGBB, named colors, integers

// Dimensions
val width = props.getDimension("width", 0f)
// Supports: 200dp, 48sp, 16px, 80%
val widthPx = width.toPx(density, fontScale)

// Lists
val items = props.getStringList("items", emptyList())
val numbers = props.getIntList("numbers", emptyList())

// Callbacks
val onClick = props.getCallback("onClick")
onClick?.invoke()

val onChange = props.getCallback1<String>("onChange")
onChange?.invoke("value")
```

---

## Spacing

```kotlin
// EdgeInsets (padding/margin)
val uniform = EdgeInsets.all(16f)
val symmetric = EdgeInsets.symmetric(horizontal = 24f, vertical = 16f)
val horizontal = EdgeInsets.horizontal(20f)
val vertical = EdgeInsets.vertical(12f)
val custom = EdgeInsets(start = 8f, top = 16f, end = 8f, bottom = 24f)

// Operations
val total = insets1 + insets2
val scaled = insets * 1.5f

// SpacingScale (Material Design 4dp base)
SpacingScale.XXS  // 2dp
SpacingScale.XS   // 4dp
SpacingScale.SM   // 8dp
SpacingScale.MD   // 12dp
SpacingScale.LG   // 16dp
SpacingScale.XL   // 24dp
SpacingScale.XXL  // 32dp
SpacingScale.XXXL // 48dp

val custom = SpacingScale.get(5f)      // 20dp (5 * 4)
val named = SpacingScale.byName("lg")  // 16dp

// Size
val size = Size(200f, 100f)
val square = Size.square(100f)
val ratio = size.aspectRatio  // 2.0

// CornerRadius
val rounded = CornerRadius.all(8f)
val topRounded = CornerRadius(
    topStart = 16f,
    topEnd = 16f,
    bottomStart = 0f,
    bottomEnd = 0f
)

// Border
val border = Border.solid(2f, 0xFF1E88E5.toInt())
val dashed = Border.dashed(1f, 0xFF000000.toInt())
val dotted = Border.dotted(1f, 0xFF888888.toInt())

// Shadow
val shadow = Shadow.elevation(4f)  // Material elevation
val custom = Shadow(
    color = 0x40000000,
    blurRadius = 10f,
    spreadRadius = 2f,
    offsetX = 0f,
    offsetY = 4f
)
```

---

## Input Events

```kotlin
// Handle input events
fun handleInput(event: InputEvent) {
    when (event) {
        is InputEvent.Tap -> {
            val (x, y) = event.position
            val source = event.source  // Touch, Mouse, etc.
        }
        is InputEvent.KeyDown -> {
            if (event.modifiers.ctrl && event.key == Key.C) {
                // Handle Ctrl+C
            }
        }
        is InputEvent.VoiceCommand -> {
            val command = event.command      // "click", "scroll up", etc.
            val params = event.parameters
            val confidence = event.confidence
        }
        // ... other events
    }
}

// Event types
InputEvent.Tap, DoubleTap, LongPress
InputEvent.HoverEnter, HoverMove, HoverExit
InputEvent.DragStart, Drag, DragEnd, DragCancel
InputEvent.Scroll, Fling
InputEvent.KeyDown, KeyUp, TextInput
InputEvent.FocusGained, FocusLost
InputEvent.VoiceCommand, VoiceCursorMove, VoiceCursorClick
InputEvent.StylusPressure, StylusButton
```

---

## Input State

```kotlin
// Track input state
val state = InputState(
    isHovered = true,
    isPressed = false,
    isFocused = false,
    isSelected = false,
    isDragging = false,
    isDisabled = false,
    cursorPosition = Offset(100f, 200f),
    inputSource = InputSource.Mouse,
    pressure = 1.0f,
    tilt = 0f
)

// Visual state
val visual = state.visualState
// Returns: Default, Hovered, Pressed, Focused, Selected, Dragging, Disabled

// Input source
InputSource.Touch      // Finger touch
InputSource.Mouse      // Mouse device
InputSource.Trackpad   // Trackpad gestures
InputSource.Stylus     // Stylus/pen
InputSource.Keyboard   // Keyboard navigation
InputSource.VoiceCursor // Voice-controlled cursor
InputSource.Gamepad    // Game controller

// Capabilities
val isPrecise = source.isPrecisePointer  // true for Mouse, Trackpad, Stylus
val supportsHover = source.supportsHover
val isVoice = source.isVoice

// Platform capabilities
val capabilities = InputCapabilities.Desktop  // or .Mobile, .Web
val enableHover = capabilities.enableHoverStates
val primaryMode = capabilities.primaryInputMode
```

---

## VoiceCursor (Android VoiceOS)

```kotlin
// Initialize (in Application.onCreate)
initializeVoiceCursor(applicationContext)

// Get manager
val voiceCursor = getVoiceCursorManager()

// Check availability
if (voiceCursor.isAvailable) {
    // Register voice target
    voiceCursor.registerTarget(
        VoiceTarget(
            id = "button_submit",
            label = "submit",
            bounds = Rect(left, top, right, bottom),
            onSelect = { /* handle click */ },
            onHover = { hovered -> /* update UI */ }
        )
    )
}

// Voice commands
VoiceCommands.CLICK          // "click"
VoiceCommands.DOUBLE_CLICK   // "double click"
VoiceCommands.SCROLL_UP      // "scroll up"
VoiceCommands.NEXT           // "next"
VoiceCommands.FOCUS          // "focus"
VoiceCommands.TYPE           // "type"
VoiceCommands.CURSOR_CENTER  // "cursor center"

// Voice-accessible component
@Composable
fun VoiceButton(label: String, onClick: () -> Unit) {
    val voiceCursor = remember { getVoiceCursorManager() }
    var isVoiceHovered by remember { mutableStateOf(false) }

    LaunchedEffect(bounds) {
        if (voiceCursor.isAvailable) {
            voiceCursor.registerTarget(
                VoiceTarget(
                    id = "btn_$label",
                    label = label.lowercase(),
                    bounds = bounds,
                    onSelect = onClick,
                    onHover = { isVoiceHovered = it }
                )
            )
        }
    }

    Button(
        onClick = onClick,
        modifier = Modifier.onGloballyPositioned {
            bounds = it.boundsInRoot().toRect()
        }
    ) {
        Text(label)
        if (voiceCursor.isActive) {
            Text(" (\"$label\")", style = captionStyle)
        }
    }
}
```

---

## Common Patterns

### Component with Full Input Support
```kotlin
@Composable
fun InteractiveComponent(props: Map<String, Any?>) {
    // Extract properties
    val label = props.getString("label", "Button")
    val enabled = props.getBoolean("enabled", true)
    val color = UniversalColor.fromHex(props.getString("color", "#1E88E5"))
    val padding = EdgeInsets.fromMap(props.getMap("padding", emptyMap()))

    // Input state
    var inputState by remember { mutableStateOf(InputState.Default) }
    val capabilities = remember { detectInputCapabilities() }

    // Voice support
    val voiceCursor = remember { getVoiceCursorManager() }
    var isVoiceHovered by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .padding(padding.start.dp, padding.top.dp, padding.end.dp, padding.bottom.dp)
            .pointerInput(Unit) {
                // Handle mouse/touch events
            }
            .onGloballyPositioned { coordinates ->
                // Update voice target bounds
                if (voiceCursor.isAvailable) {
                    voiceCursor.updateTargetBounds(id, coordinates.boundsInRoot().toRect())
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Content
    }
}
```

### Accessible Color Scheme
```kotlin
fun createAccessibleScheme(primary: UniversalColor): ColorScheme {
    val bg = primary
    val fg = ColorUtils.contrastingForeground(bg)

    // Ensure WCAG AA compliance
    val adjustedFg = if (ColorUtils.meetsWcagAA(fg, bg)) {
        fg
    } else {
        if (bg.isDark) bg.lighten(0.5f) else bg.darken(0.5f)
    }

    return ColorScheme(
        primary = primary,
        onPrimary = adjustedFg,
        secondary = primary.shiftHue(30f),
        background = UniversalColor.White,
        surface = UniversalColor.White
    )
}
```

### RTL-Aware Layout
```kotlin
@Composable
fun ResponsiveRow(alignment: WrapAlignment) {
    val layoutDir = LocalLayoutDirection.current
    val arrangement = AlignmentConverter.wrapToHorizontal(
        alignment,
        layoutDir.toCommon()
    )

    Row(
        horizontalArrangement = arrangement.toCompose(),
        modifier = Modifier.padding(SpacingScale.MD.dp)
    ) {
        // Content
    }
}
```

---

## Platform-Specific Notes

### Android
- Convert colors: `Color(color.toArgb())`
- Convert dimensions: `width.toDp(density).dp`
- Use `LocalDensity.current.density`
- Full VoiceCursor support

### iOS
- Convert colors: `Color(red: r, green: g, blue: b, opacity: a)`
- Convert dimensions: `CGFloat(width.toPx(scale))`
- Use `UIScreen.main.scale`
- VoiceCursor: NoOp (stub)

### Desktop
- Same as Android (Compose Desktop)
- VoiceCursor: NoOp (stub)

### Web
- Convert colors: `color.toHex()` or `rgba(r, g, b, a)`
- Convert dimensions: CSS units (`px`, `rem`, `%`)
- VoiceCursor: NoOp (stub)

---

## Cheat Sheet

| Task | Code |
|------|------|
| Get string property | `props.getString("key", "default")` |
| Get color property | `props.getColorArgb("key", 0xFF000000.toInt())` |
| Lighten color 20% | `color.lighten(0.2f)` |
| Check WCAG AA | `ColorUtils.meetsWcagAA(fg, bg)` |
| Standard padding | `EdgeInsets.all(SpacingScale.MD)` |
| RTL-aware alignment | `alignment.toHorizontalArrangement(layoutDir)` |
| Check hover support | `inputSource.supportsHover` |
| Voice-accessible | `voiceCursor.registerTarget(...)` |

---

## Quick Links

- **Full Documentation:** `docs/manuals/DEVELOPER-MANUAL-SHARED-UTILITIES-SECTION.md`
- **Source Code:** `Universal/Libraries/AvaElements/Core/src/commonMain/kotlin/com/augmentalis/avaelements/common/`
- **Developer Manual:** `docs/manuals/DEVELOPER-MANUAL.md`

---

**Questions?** Refer to the complete documentation for detailed examples and best practices.
