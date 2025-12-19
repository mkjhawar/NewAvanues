# Unified Input Architecture

**Version:** 1.0
**Date:** 2025-11-25
**Status:** PROPOSED

---

## Problem Statement

Current input handling is fragmented:
- **Desktop:** Mouse, keyboard, touchpad - IMPLEMENTED
- **Android:** Touch only - MISSING mouse/keyboard/VoiceCursor
- **iOS:** Touch only - MISSING mouse/keyboard (iPadOS)
- **Web:** Mouse, keyboard, touch - PARTIAL

This creates inconsistent UX and code duplication.

---

## Target Input Matrix

| Input Type | Android | iOS | Desktop | Web |
|------------|---------|-----|---------|-----|
| **Touch** | ✅ | ✅ | ✅ | ✅ |
| **Mouse** | ✅ Tablets/DeX/Chromebook | ✅ iPadOS | ✅ | ✅ |
| **Keyboard** | ✅ External/Virtual | ✅ External | ✅ | ✅ |
| **Trackpad** | ✅ Chromebook | ✅ iPadOS | ✅ | ✅ |
| **Stylus** | ✅ S Pen | ✅ Apple Pencil | ✅ Wacom | ✅ |
| **VoiceCursor** | ✅ VoiceOS | Future | Future | Future |
| **Gamepad** | ✅ Android TV | ✅ tvOS | ✅ | ✅ |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    AvaElements Components                        │
│  (Button, TextField, List, Card, etc.)                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Unified Input Abstraction                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ InputState   │  │ InputEvent  │  │ FocusManager │          │
│  │ - hover      │  │ - Tap       │  │ - keyboard   │          │
│  │ - pressed    │  │ - LongPress │  │ - spatial    │          │
│  │ - focused    │  │ - Scroll    │  │ - voice      │          │
│  │ - selected   │  │ - Drag      │  │              │          │
│  └──────────────┘  │ - Hover     │  └──────────────┘          │
│                    │ - KeyPress  │                              │
│                    │ - Voice     │                              │
│                    └──────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│ Platform Input  │  │ Platform Input  │  │ Platform Input  │
│ Android         │  │ iOS             │  │ Desktop/Web     │
├─────────────────┤  ├─────────────────┤  ├─────────────────┤
│ • Touch         │  │ • Touch         │  │ • Mouse         │
│ • Mouse (DeX)   │  │ • Mouse (iPad)  │  │ • Keyboard      │
│ • Keyboard      │  │ • Keyboard      │  │ • Trackpad      │
│ • VoiceCursor   │  │ • Trackpad      │  │ • Touch         │
│ • Stylus        │  │ • Apple Pencil  │  │ • Stylus        │
│ • Gamepad       │  │ • Gamepad       │  │                 │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

---

## Shared Input Module

### Location
```
Universal/Libraries/AvaElements/Core/src/commonMain/kotlin/
└── com/augmentalis/avaelements/input/
    ├── InputState.kt           # Unified input state
    ├── InputEvent.kt           # Unified input events
    ├── InputModifier.kt        # Cross-platform input modifiers
    ├── FocusManager.kt         # Focus navigation
    ├── HoverState.kt           # Hover detection
    ├── KeyboardShortcuts.kt    # Keyboard handling
    └── VoiceInput.kt           # Voice command integration
```

### Core Data Types

```kotlin
// ═══════════════════════════════════════════════════════════════
// InputState.kt - Unified input state for all platforms
// ═══════════════════════════════════════════════════════════════

package com.augmentalis.avaelements.input

/**
 * Unified input state that works across all platforms.
 * Components observe this state to update their appearance.
 */
data class InputState(
    val isHovered: Boolean = false,      // Mouse/cursor over element
    val isPressed: Boolean = false,      // Currently being pressed
    val isFocused: Boolean = false,      // Has keyboard focus
    val isSelected: Boolean = false,     // Selected state
    val isDragging: Boolean = false,     // Being dragged
    val cursorPosition: Offset? = null,  // Cursor position if hovering
    val inputSource: InputSource = InputSource.Unknown
)

/**
 * Input source identification for adaptive behavior
 */
enum class InputSource {
    Touch,          // Finger touch
    Mouse,          // Mouse device
    Trackpad,       // Trackpad gestures
    Stylus,         // Stylus/pen input
    Keyboard,       // Keyboard navigation
    VoiceCursor,    // VoiceOS cursor
    Gamepad,        // Game controller
    Unknown
}

/**
 * Platform capabilities - detected at runtime
 */
data class InputCapabilities(
    val hasTouch: Boolean,
    val hasMouse: Boolean,
    val hasKeyboard: Boolean,
    val hasTrackpad: Boolean,
    val hasStylus: Boolean,
    val hasVoiceCursor: Boolean,
    val hasGamepad: Boolean
)
```

### Input Events

```kotlin
// ═══════════════════════════════════════════════════════════════
// InputEvent.kt - Unified input events
// ═══════════════════════════════════════════════════════════════

package com.augmentalis.avaelements.input

sealed class InputEvent {
    // Pointer events (touch, mouse, stylus)
    data class Tap(val position: Offset, val source: InputSource) : InputEvent()
    data class DoubleTap(val position: Offset, val source: InputSource) : InputEvent()
    data class LongPress(val position: Offset, val source: InputSource) : InputEvent()
    data class Hover(val position: Offset, val isEnter: Boolean) : InputEvent()

    // Drag events
    data class DragStart(val position: Offset) : InputEvent()
    data class Drag(val position: Offset, val delta: Offset) : InputEvent()
    data class DragEnd(val position: Offset, val velocity: Velocity) : InputEvent()

    // Scroll events
    data class Scroll(val delta: Offset, val source: ScrollSource) : InputEvent()

    // Keyboard events
    data class KeyDown(val key: Key, val modifiers: KeyModifiers) : InputEvent()
    data class KeyUp(val key: Key, val modifiers: KeyModifiers) : InputEvent()

    // Voice events (VoiceCursor integration)
    data class VoiceCommand(val command: String, val parameters: Map<String, Any>) : InputEvent()
    data class VoiceCursorMove(val position: Offset) : InputEvent()
    data class VoiceCursorClick(val clickType: ClickType) : InputEvent()

    // Focus events
    data class FocusGained(val source: FocusSource) : InputEvent()
    object FocusLost : InputEvent()
}

enum class ScrollSource { MouseWheel, Trackpad, Touch, Keyboard, Voice }
enum class ClickType { Single, Double, LongPress }
enum class FocusSource { Keyboard, Mouse, Touch, Voice, Programmatic }

data class KeyModifiers(
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false  // Cmd on Mac, Win on Windows
)
```

### Input Modifier

```kotlin
// ═══════════════════════════════════════════════════════════════
// InputModifier.kt - Cross-platform input handling modifiers
// ═══════════════════════════════════════════════════════════════

package com.augmentalis.avaelements.input

/**
 * Unified input modifier that provides consistent behavior across platforms.
 * Replaces platform-specific modifiers.
 */
interface UnifiedInputModifier {
    /**
     * Handle hover state changes (mouse, VoiceCursor)
     */
    fun onHover(onEnter: () -> Unit, onExit: () -> Unit): Modifier

    /**
     * Handle all click types uniformly
     */
    fun onClick(
        onClick: () -> Unit,
        onDoubleClick: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null
    ): Modifier

    /**
     * Handle keyboard focus and navigation
     */
    fun focusable(
        onFocusChanged: (Boolean) -> Unit,
        focusOrder: Int? = null
    ): Modifier

    /**
     * Handle keyboard shortcuts
     */
    fun keyboardShortcut(
        key: Key,
        modifiers: KeyModifiers = KeyModifiers(),
        action: () -> Unit
    ): Modifier

    /**
     * Handle VoiceCursor specific interactions
     */
    fun voiceCursorTarget(
        voiceLabel: String,  // "Click [label]" voice command
        onVoiceSelect: () -> Unit
    ): Modifier

    /**
     * Combined input state observer
     */
    fun inputState(
        onStateChange: (InputState) -> Unit
    ): Modifier
}
```

---

## Platform Implementations

### Android Implementation

```kotlin
// ═══════════════════════════════════════════════════════════════
// AndroidInputHandler.kt
// ═══════════════════════════════════════════════════════════════

package com.augmentalis.avaelements.input.android

import com.augmentalis.avaelements.input.*

/**
 * Android-specific input handling including:
 * - Touch (standard)
 * - Mouse (tablets, DeX, Chromebooks)
 * - Keyboard (external, virtual)
 * - VoiceCursor (VoiceOS integration)
 * - Stylus (S Pen)
 * - Gamepad (Android TV)
 */
class AndroidInputHandler : PlatformInputHandler {

    // Detect input capabilities at runtime
    override fun detectCapabilities(context: Context): InputCapabilities {
        val config = context.resources.configuration
        val packageManager = context.packageManager

        return InputCapabilities(
            hasTouch = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN),
            hasMouse = isMouseConnected() || isDeXMode() || isChromeOS(),
            hasKeyboard = config.keyboard != Configuration.KEYBOARD_NOKEYS,
            hasTrackpad = isChromeOS() || hasExternalTrackpad(),
            hasStylus = packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT),
            hasVoiceCursor = VoiceCursor.isAvailable(context),
            hasGamepad = hasGameController()
        )
    }

    // Mouse support for tablets/DeX/Chromebooks
    @Composable
    fun Modifier.mouseSupport(): Modifier = this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    when {
                        event.type == PointerEventType.Enter -> onHoverEnter()
                        event.type == PointerEventType.Exit -> onHoverExit()
                        event.type == PointerEventType.Move -> onHoverMove(event.position)
                    }
                }
            }
        }
        .pointerHoverIcon(PointerIcon.Hand)  // Show hand cursor on hover

    // VoiceCursor integration
    @Composable
    fun Modifier.voiceCursorSupport(
        voiceLabel: String,
        onVoiceSelect: () -> Unit
    ): Modifier = this
        .semantics {
            // Register with VoiceCursor system
            contentDescription = voiceLabel
            customActions = listOf(
                CustomAccessibilityAction("Select $voiceLabel") {
                    onVoiceSelect()
                    true
                }
            )
        }
        .onGloballyPositioned { coordinates ->
            // Register bounds with VoiceCursor for spatial awareness
            VoiceCursor.registerTarget(
                id = voiceLabel,
                bounds = coordinates.boundsInWindow(),
                onSelect = onVoiceSelect
            )
        }

    // Keyboard navigation
    @Composable
    fun Modifier.keyboardNavigation(): Modifier = this
        .onKeyEvent { event ->
            when {
                event.key == Key.Enter && event.type == KeyEventType.KeyDown -> {
                    performClick()
                    true
                }
                event.key == Key.Tab && event.type == KeyEventType.KeyDown -> {
                    moveFocusNext()
                    true
                }
                else -> false
            }
        }
}
```

### iOS Implementation

```kotlin
// ═══════════════════════════════════════════════════════════════
// IOSInputHandler.kt (Kotlin/Native)
// ═══════════════════════════════════════════════════════════════

package com.augmentalis.avaelements.input.ios

/**
 * iOS-specific input handling including:
 * - Touch (standard)
 * - Mouse (iPadOS pointer support)
 * - Keyboard (external keyboards)
 * - Trackpad (Magic Trackpad, built-in)
 * - Apple Pencil
 */
class IOSInputHandler : PlatformInputHandler {

    // Maps to SwiftUI pointer interaction
    fun createHoverModifier(): SwiftUIModifier {
        return SwiftUIModifier.custom("onHover") { isHovered ->
            // Update component state
        }
    }

    // Maps to SwiftUI keyboard shortcuts
    fun createKeyboardShortcut(
        key: Key,
        modifiers: KeyModifiers
    ): SwiftUIModifier {
        val swiftModifiers = buildList {
            if (modifiers.meta) add("command")
            if (modifiers.shift) add("shift")
            if (modifiers.alt) add("option")
            if (modifiers.ctrl) add("control")
        }

        return SwiftUIModifier.custom("keyboardShortcut") {
            mapOf(
                "key" to key.toSwiftKey(),
                "modifiers" to swiftModifiers
            )
        }
    }

    // iPadOS pointer support
    fun createPointerStyle(): SwiftUIModifier {
        return SwiftUIModifier.custom("hoverEffect") {
            mapOf("effect" to "highlight")  // .highlight, .lift, .automatic
        }
    }
}
```

---

## VoiceCursor Integration for AvaElements

### Registration Pattern

```kotlin
// ═══════════════════════════════════════════════════════════════
// VoiceCursorIntegration.kt
// ═══════════════════════════════════════════════════════════════

package com.augmentalis.avaelements.input.voice

/**
 * Integration layer between AvaElements and VoiceOS VoiceCursor.
 * Enables voice-controlled UI navigation.
 */
object VoiceCursorIntegration {

    private val registeredTargets = mutableMapOf<String, VoiceTarget>()

    /**
     * Register an AvaElements component as a VoiceCursor target.
     * Users can say "Click [label]" to activate.
     */
    fun registerTarget(
        id: String,
        label: String,
        bounds: Rect,
        onSelect: () -> Unit,
        onHover: ((Boolean) -> Unit)? = null
    ) {
        registeredTargets[id] = VoiceTarget(
            id = id,
            label = label,
            bounds = bounds,
            onSelect = onSelect,
            onHover = onHover
        )

        // Register with VoiceOS CommandManager
        VoiceCursor.getInstance().registerClickTarget(
            targetId = id,
            voiceLabel = label,
            bounds = bounds,
            callback = { onSelect() }
        )
    }

    /**
     * Handle cursor position updates from VoiceCursor.
     * Updates hover states on registered components.
     */
    fun onCursorPositionChanged(position: CursorOffset) {
        registeredTargets.values.forEach { target ->
            val isHovered = target.bounds.contains(position.x, position.y)
            if (isHovered != target.isCurrentlyHovered) {
                target.isCurrentlyHovered = isHovered
                target.onHover?.invoke(isHovered)
            }
        }
    }

    /**
     * Handle voice click command.
     * VoiceOS routes "Click [label]" commands here.
     */
    fun onVoiceClick(label: String): Boolean {
        val target = registeredTargets.values.find {
            it.label.equals(label, ignoreCase = true)
        }
        return if (target != null) {
            target.onSelect()
            true
        } else {
            false
        }
    }
}

data class VoiceTarget(
    val id: String,
    val label: String,
    val bounds: Rect,
    val onSelect: () -> Unit,
    val onHover: ((Boolean) -> Unit)?,
    var isCurrentlyHovered: Boolean = false
)
```

### Component Usage

```kotlin
// Example: Voice-enabled Button in AvaElements

@Composable
fun Button(
    component: Button,
    modifier: Modifier = Modifier
) {
    val inputState = remember { mutableStateOf(InputState()) }

    Box(
        modifier = modifier
            // Standard touch/click
            .clickable(onClick = component.onClick)

            // Mouse hover (tablets, DeX, Chromebooks)
            .mouseSupport(
                onHoverChange = { isHovered ->
                    inputState.value = inputState.value.copy(isHovered = isHovered)
                }
            )

            // Keyboard focus & shortcuts
            .focusable()
            .onKeyEvent { handleKeyEvent(it, component.onClick) }

            // VoiceCursor integration
            .voiceCursorTarget(
                voiceLabel = component.accessibilityLabel ?: component.label,
                onVoiceSelect = component.onClick,
                onVoiceHover = { isHovered ->
                    inputState.value = inputState.value.copy(
                        isHovered = isHovered,
                        inputSource = InputSource.VoiceCursor
                    )
                }
            )

            // Apply hover visual feedback
            .background(
                if (inputState.value.isHovered)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else
                    Color.Transparent
            )
    ) {
        // Button content
    }
}
```

---

## Keyboard Shortcuts

### Shared Shortcut Registry

```kotlin
// ═══════════════════════════════════════════════════════════════
// KeyboardShortcuts.kt
// ═══════════════════════════════════════════════════════════════

package com.augmentalis.avaelements.input

/**
 * Cross-platform keyboard shortcut registry.
 * Handles platform differences (Cmd vs Ctrl, etc.)
 */
object KeyboardShortcuts {

    // Standard shortcuts that work across platforms
    val Copy = Shortcut(Key.C, meta = true)      // Cmd+C / Ctrl+C
    val Paste = Shortcut(Key.V, meta = true)     // Cmd+V / Ctrl+V
    val Cut = Shortcut(Key.X, meta = true)       // Cmd+X / Ctrl+X
    val Undo = Shortcut(Key.Z, meta = true)      // Cmd+Z / Ctrl+Z
    val Redo = Shortcut(Key.Z, meta = true, shift = true)
    val SelectAll = Shortcut(Key.A, meta = true)
    val Save = Shortcut(Key.S, meta = true)
    val Find = Shortcut(Key.F, meta = true)

    // Navigation
    val FocusNext = Shortcut(Key.Tab)
    val FocusPrevious = Shortcut(Key.Tab, shift = true)
    val Activate = Shortcut(Key.Enter)
    val Cancel = Shortcut(Key.Escape)

    // Platform-aware meta key
    fun Shortcut.toPlatform(): KeyModifiers {
        return when (Platform.current) {
            Platform.MacOS, Platform.IOS -> KeyModifiers(meta = this.meta)
            else -> KeyModifiers(ctrl = this.meta)  // Windows, Linux, Android
        }
    }
}

data class Shortcut(
    val key: Key,
    val ctrl: Boolean = false,
    val alt: Boolean = false,
    val shift: Boolean = false,
    val meta: Boolean = false  // Platform-aware: Cmd on Mac, Ctrl elsewhere
)
```

---

## Migration Plan

### Phase 1: Create Shared Input Module (Week 1)
1. Create `AvaElements/Core/src/commonMain/.../input/` package
2. Implement `InputState`, `InputEvent`, `InputModifier` interfaces
3. Add unit tests for shared logic

### Phase 2: Android Mouse/Keyboard Support (Week 2)
1. Implement `AndroidInputHandler`
2. Add mouse hover to all interactive components
3. Add keyboard navigation (Tab, Enter, Arrow keys)
4. Test on tablet, DeX, Chromebook

### Phase 3: VoiceCursor Integration (Week 3)
1. Create `VoiceCursorIntegration` bridge
2. Register all interactive components as voice targets
3. Handle cursor position updates for hover states
4. Test voice navigation end-to-end

### Phase 4: iOS Mouse/Keyboard Support (Week 4)
1. Implement `IOSInputHandler`
2. Add iPadOS pointer effects (.highlight, .lift)
3. Add external keyboard support
4. Test on iPad with Magic Keyboard/Trackpad

### Phase 5: Unify Desktop/Web (Week 5)
1. Refactor Desktop to use shared module
2. Refactor Web to use shared patterns
3. Remove duplicate code
4. Final cross-platform testing

---

## Benefits

| Benefit | Impact |
|---------|--------|
| **Consistent UX** | Same hover/focus behavior everywhere |
| **Code Reduction** | ~40% less input handling code |
| **Accessibility** | VoiceCursor enables hands-free use |
| **Future-Proof** | Easy to add new input types |
| **Testing** | Single test suite for input logic |

---

## Summary

This architecture provides:

1. **Unified Input State** - Single `InputState` model across all platforms
2. **Platform Adapters** - Android, iOS, Desktop, Web implementations
3. **VoiceCursor Integration** - Voice-controlled navigation on Android
4. **Mouse/Keyboard Everywhere** - Tablets, laptops, desktops all supported
5. **Shared Logic** - No duplication of hover, focus, shortcut handling

**Estimated Effort:** 5 weeks for full implementation
**Code Reduction:** ~40% in input handling
**New Capabilities:** VoiceCursor, gamepad, stylus pressure
