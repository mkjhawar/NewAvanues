# UI Overlays Implementation Guide

**Last Updated:** 2025-10-09 03:23:38 PDT
**Module:** VoiceAccessibility
**Component:** UI Overlays System
**Status:** ✅ Complete - Week 2 Critical Overlays Implemented

---

## Overview

This document describes the implementation of critical UI overlay stubs for VOS4's VoiceAccessibility module. These overlays provide essential visual feedback for voice-controlled interactions that appear on top of all applications.

### Design Philosophy

- **TYPE_ACCESSIBILITY_OVERLAY**: All overlays use accessibility overlay type for system-wide display
- **Material Design 3**: Modern UI following Material Design guidelines
- **Jetpack Compose**: All UI built with Compose for declarative, reactive interfaces
- **Non-blocking**: Overlays are non-focusable and non-touch-modal where appropriate
- **Animated**: Smooth transitions and state changes for better UX
- **Centralized Control**: Single OverlayManager coordinates all overlays

---

## Architecture

```
OverlayManager (Singleton)
    ├── ConfidenceOverlay        - Real-time confidence indicator
    ├── NumberedSelectionOverlay - Multi-item selection
    ├── CommandStatusOverlay     - Voice command status
    └── ContextMenuOverlay       - Voice-activated menus
```

### File Locations

```
/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/overlays/
├── ConfidenceOverlay.kt           - Confidence indicator implementation
├── NumberedSelectionOverlay.kt    - Numbered selection implementation
├── CommandStatusOverlay.kt        - Command status implementation
├── ContextMenuOverlay.kt          - Context menu implementation
└── OverlayManager.kt              - Centralized overlay manager

/modules/apps/VoiceAccessibility/src/test/java/com/augmentalis/voiceos/accessibility/overlays/
├── OverlayManagerTest.kt          - OverlayManager unit tests
└── ConfidenceOverlayTest.kt       - ConfidenceOverlay unit tests
```

---

## Overlay 1: ConfidenceOverlay

### Purpose
Shows real-time confidence during speech recognition with visual feedback including color-coded confidence levels and percentage display.

### Features
- **Real-time Updates**: Updates without recreating overlay
- **Color Coding**: Green (HIGH), Yellow (MEDIUM), Orange (LOW), Red (REJECT)
- **Animated**: Smooth color transitions using animateColorAsState
- **Compact Display**: Shows percentage, level name, and partial text

### Usage

```kotlin
// Initialize (usually in AccessibilityService)
val overlayManager = OverlayManager.getInstance(context)

// Show confidence with result
val confidenceResult = ConfidenceResult(
    text = "open settings",
    confidence = 0.87f,
    level = ConfidenceLevel.HIGH,
    alternates = emptyList(),
    scoringMethod = ScoringMethod.ANDROID_STT
)
overlayManager.showConfidence(confidenceResult)

// Update confidence without hiding/showing
overlayManager.updateConfidence(confidenceResult)

// Hide confidence
overlayManager.hideConfidence()
```

### Configuration

**Position:** Top-right corner (16dp padding)
**Window Type:** TYPE_ACCESSIBILITY_OVERLAY
**Flags:** FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE
**Size:** WRAP_CONTENT (120-200dp width)

### Visual Specifications

- **Card**: RoundedCornerShape(12.dp), Semi-transparent black (0xEE000000)
- **Circle Indicator**: 28dp diameter, color based on confidence level
- **Text**: 18sp bold percentage, 10sp medium level name, 11sp partial text
- **Elevation**: 8dp shadow

---

## Overlay 2: NumberedSelectionOverlay

### Purpose
Shows numbered choices when multiple clickable items exist, allowing voice selection by number for disambiguation.

### Features
- **Fullscreen Overlay**: Semi-transparent backdrop (30% black)
- **Number Badges**: Positioned at item locations with labels
- **Voice Instructions**: Bottom panel with microphone icon and guidance
- **Animated**: Fade and scale animations for badges

### Usage

```kotlin
// Create selectable items
val items = listOf(
    SelectableItem(
        number = 1,
        label = "Settings",
        bounds = Rect(100, 200, 300, 400),
        action = { openSettings() }
    ),
    SelectableItem(
        number = 2,
        label = "About",
        bounds = Rect(100, 450, 300, 650),
        action = { openAbout() }
    )
)

// Show numbered selection
overlayManager.showNumberedSelection(items)

// Select by number (from voice command)
overlayManager.selectNumberedItem(1) // Returns true if successful

// Hide selection
overlayManager.hideNumberedSelection()
```

### Configuration

**Position:** Fullscreen
**Window Type:** TYPE_ACCESSIBILITY_OVERLAY
**Flags:** FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL
**Size:** MATCH_PARENT

### Visual Specifications

- **Backdrop**: 30% black overlay (0x4D000000)
- **Number Badge**: Blue (0xEE2196F3), 24dp circle, 2dp white border
- **Label**: 12sp medium white text, 20 char max
- **Instruction Panel**: RoundedCornerShape(24dp), centered bottom, 16dp padding

---

## Overlay 3: CommandStatusOverlay

### Purpose
Shows current voice command being processed with real-time status updates for listening, processing, executing, success, and error states.

### Features
- **State Machine**: 5 states (LISTENING, PROCESSING, EXECUTING, SUCCESS, ERROR)
- **Animated Icons**: Pulsing for LISTENING/PROCESSING, rotating for EXECUTING
- **Color-Coded**: Each state has unique color and icon
- **Auto-dismiss**: Can auto-hide after delay

### Usage

```kotlin
// Show listening state
overlayManager.showListening("") // Empty for "Listening..."

// Show processing state
overlayManager.showProcessing("open settings")

// Show executing state
overlayManager.showExecuting("open settings")

// Show success state
overlayManager.showSuccess("open settings", "Settings opened")

// Show error state
overlayManager.showError("open settings", "App not found")

// Manual control
overlayManager.showCommandStatus(
    command = "scroll down",
    state = CommandState.EXECUTING,
    message = "Scrolling..."
)

// Update status
overlayManager.updateCommandStatus(
    state = CommandState.SUCCESS,
    message = "Scrolled successfully"
)

// Hide status
overlayManager.hideCommandStatus()

// Auto-dismiss after delay (coroutine)
launch {
    overlayManager.dismissAfterDelay(2000) // 2 seconds
}
```

### Configuration

**Position:** Top-center (80dp from top)
**Window Type:** TYPE_ACCESSIBILITY_OVERLAY
**Flags:** FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL
**Size:** WRAP_CONTENT (200-320dp width)

### State Colors & Icons

| State | Color | Icon | Animation |
|-------|-------|------|-----------|
| LISTENING | Blue (#2196F3) | Mic | Pulsing |
| PROCESSING | Amber (#FFC107) | HourglassEmpty | Rotating |
| EXECUTING | Purple (#9C27B0) | PlayArrow | Rotating |
| SUCCESS | Green (#4CAF50) | Check | None |
| ERROR | Red (#F44336) | Close | None |

### Visual Specifications

- **Card**: RoundedCornerShape(16dp), Semi-transparent black (0xEE000000)
- **Icon Circle**: 48dp background, 32dp icon with animations
- **Text**: 12sp medium state label, 16sp bold command, 12sp message
- **Elevation**: 12dp shadow
- **Animation**: Slide in from top, fade in/out

---

## Overlay 4: ContextMenuOverlay

### Purpose
Shows voice-activated context menu with numbered or labeled options, supporting both position-based and screen-centered display.

### Features
- **Flexible Positioning**: Center or at specific point
- **Numbered Options**: Voice selection by number
- **Icon Support**: Optional icons for each menu item
- **Enable/Disable**: Individual item enable states
- **Voice Guidance**: Footer with voice instructions

### Usage

```kotlin
// Create menu items
val menuItems = listOf(
    MenuItem(
        id = "copy",
        label = "Copy",
        icon = Icons.Default.ContentCopy,
        number = 1,
        enabled = true,
        action = { performCopy() }
    ),
    MenuItem(
        id = "paste",
        label = "Paste",
        icon = Icons.Default.ContentPaste,
        number = 2,
        enabled = clipboardHasContent(),
        action = { performPaste() }
    ),
    MenuItem(
        id = "delete",
        label = "Delete",
        icon = Icons.Default.Delete,
        number = 3,
        enabled = true,
        action = { performDelete() }
    )
)

// Show at center
overlayManager.showContextMenu(menuItems, title = "Edit Options")

// Show at position
val position = Point(200, 400)
overlayManager.showContextMenuAt(menuItems, position, title = "Edit")

// Select by ID
overlayManager.selectContextMenuItem("copy") // Returns true if successful

// Select by number (voice command)
overlayManager.selectContextMenuByNumber(1) // Returns true if successful

// Hide menu
overlayManager.hideContextMenu()
```

### Configuration

**Position:** Center or at Point
**Window Type:** TYPE_ACCESSIBILITY_OVERLAY
**Flags:** FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_WATCH_OUTSIDE_TOUCH
**Size:** WRAP_CONTENT (200-280dp width)

### Visual Specifications

- **Card**: RoundedCornerShape(12dp), Dark semi-transparent (0xEE1E1E1E)
- **Title**: 14sp bold white, 8dp padding
- **Number Badge**: 28dp rounded rect, blue (0xFF2196F3)
- **Icon**: 24dp, white/gray based on enabled state
- **Label**: 14sp medium white/gray, single line
- **Divider**: White 10% opacity, 1dp
- **Elevation**: 16dp shadow
- **Animation**: Fade and scale in/out

---

## OverlayManager

### Purpose
Centralized singleton manager for coordinated control and lifecycle management of all overlays.

### Features
- **Singleton Pattern**: App-wide single instance
- **Lifecycle Management**: Proper creation and disposal
- **State Tracking**: Knows which overlays are active
- **Conflict Resolution**: Automatically hides conflicting overlays
- **Convenience Methods**: Quick access to common states

### Initialization

```kotlin
// In AccessibilityService onCreate or similar
class VoiceAccessibilityService : AccessibilityService() {
    private lateinit var overlayManager: OverlayManager

    override fun onCreate() {
        super.onCreate()
        overlayManager = OverlayManager.getInstance(this)
    }

    override fun onDestroy() {
        overlayManager.dispose()
        super.onDestroy()
    }
}
```

### State Management

```kotlin
// Check if any overlay is visible
if (overlayManager.isAnyVisible()) {
    // Handle overlay state
}

// Check specific overlay
if (overlayManager.isOverlayVisible("commandStatus")) {
    // Command status is showing
}

// Get all active overlays
val active = overlayManager.getActiveOverlays()
// Returns Set<String> like: ["confidence", "commandStatus"]
```

### Conflict Resolution

The OverlayManager automatically handles overlay conflicts:

- **Showing Context Menu** → Hides Numbered Selection
- **Showing Numbered Selection** → Hides Context Menu

This prevents visual clutter and user confusion.

### Convenience Methods

```kotlin
// Quick access to common command states
overlayManager.showListening()
overlayManager.showProcessing("scroll down")
overlayManager.showExecuting("scroll down")
overlayManager.showSuccess("scroll down")
overlayManager.showError("scroll down", "Failed to scroll")

// Auto-dismiss after delay (requires coroutine scope)
launch {
    overlayManager.showSuccess("Command executed")
    overlayManager.dismissAfterDelay(2000) // Hide after 2 seconds
}
```

---

## Integration with VoiceAccessibility

### Typical Usage Flow

```kotlin
class VoiceCommandProcessor(
    private val context: Context
) {
    private val overlayManager = OverlayManager.getInstance(context)

    suspend fun processVoiceCommand(audio: ByteArray) {
        // 1. Show listening state
        overlayManager.showListening()

        // 2. Recognize speech
        val result = speechEngine.recognize(audio)

        // 3. Show confidence
        overlayManager.showConfidence(result.confidence)

        // 4. Show processing
        overlayManager.showProcessing(result.text)

        // 5. Check confidence level
        when (result.confidence.level) {
            ConfidenceLevel.HIGH -> {
                // Execute immediately
                overlayManager.showExecuting(result.text)
                executeCommand(result.text)
                overlayManager.showSuccess(result.text)
            }

            ConfidenceLevel.MEDIUM -> {
                // Ask confirmation
                showConfirmationMenu(result.text)
            }

            ConfidenceLevel.LOW -> {
                // Show alternatives
                showAlternativesMenu(result.alternates)
            }

            ConfidenceLevel.REJECT -> {
                overlayManager.showError(result.text, "Command not recognized")
            }
        }

        // 6. Auto-dismiss after delay
        delay(2000)
        overlayManager.hideAll()
    }

    private fun showConfirmationMenu(command: String) {
        val items = listOf(
            MenuItem(
                id = "confirm",
                label = "Confirm: $command",
                number = 1,
                action = { executeCommand(command) }
            ),
            MenuItem(
                id = "cancel",
                label = "Cancel",
                number = 2,
                action = { overlayManager.hideContextMenu() }
            )
        )
        overlayManager.showContextMenu(items, title = "Confirm Command")
    }
}
```

---

## Testing

### Unit Tests

Two test files are provided:

1. **OverlayManagerTest.kt** - Tests manager functionality
   - Singleton instance
   - Active overlay tracking
   - Conflict resolution
   - Convenience methods
   - Dispose behavior

2. **ConfidenceOverlayTest.kt** - Tests overlay functionality
   - Show/hide behavior
   - Update without recreating
   - Dispose cleanup
   - Multiple show calls

### Running Tests

```bash
# Run all overlay tests
./gradlew :modules:apps:VoiceAccessibility:testDebugUnitTest --tests "*overlays*"

# Run specific test class
./gradlew :modules:apps:VoiceAccessibility:testDebugUnitTest --tests "OverlayManagerTest"

# Run with coverage
./gradlew :modules:apps:VoiceAccessibility:testDebugUnitTest jacocoTestReport
```

---

## Build Configuration

### Required Dependencies

The following dependencies are already configured in `build.gradle.kts`:

```kotlin
// Jetpack Compose (with BOM)
val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
implementation(composeBom)

implementation("androidx.compose.ui:ui:1.6.8")
implementation("androidx.compose.material3:material3:1.2.1")
implementation("androidx.compose.material:material-icons-extended:1.6.8")
implementation("androidx.activity:activity-compose:1.8.2")

// Dependency injection (for future integration)
implementation("com.google.dagger:hilt-android:2.51.1")
ksp("com.google.dagger:hilt-compiler:2.51.1")

// Speech Recognition (for ConfidenceResult)
implementation(project(":modules:libraries:SpeechRecognition"))
```

### Required Permissions

Add to `AndroidManifest.xml`:

```xml
<!-- Required for system overlays -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- Accessibility service (already configured) -->
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
```

### Proguard Rules

Add to `proguard-rules.pro`:

```proguard
# Keep overlay classes
-keep class com.augmentalis.voiceos.accessibility.overlays.** { *; }

# Keep Compose UI
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
```

---

## Performance Considerations

### Memory Management
- Overlays use lazy initialization
- Views are created only when first shown
- Proper disposal in OverlayManager.dispose()

### Animation Performance
- Hardware acceleration enabled by default
- Smooth 60fps animations with tween() specs
- Infinite animations only for active states

### Window Management
- Overlays reuse ComposeView when possible
- updateXXX() methods avoid view recreation
- Proper FLAG configuration for minimal impact

---

## Future Enhancements

### Week 3+ Potential Features

1. **Gesture Overlay** - Visual feedback for detected gestures
2. **Grid Overlay** - Screen grid for precise cursor control
3. **Tooltip Overlay** - Context-sensitive help tooltips
4. **Notification Overlay** - System notifications with voice dismiss
5. **Custom Positioning** - User-configurable overlay positions
6. **Theme Support** - Light/dark theme variants
7. **Accessibility** - TalkBack integration
8. **Multi-language** - Localized strings and RTL support

---

## Troubleshooting

### Overlay Not Showing

**Check:**
1. SYSTEM_ALERT_WINDOW permission granted
2. Accessibility service enabled
3. WindowManager properly initialized
4. No exceptions in logcat

**Debug:**
```kotlin
try {
    overlayManager.showConfidence(result)
} catch (e: Exception) {
    Log.e("Overlay", "Failed to show overlay", e)
}
```

### Overlay Position Wrong

**Verify:**
- Window gravity set correctly
- X/Y offsets appropriate for device
- Screen bounds considered
- Display metrics correct

### Performance Issues

**Optimize:**
- Reduce animation durations
- Simplify compose hierarchies
- Profile with Android Profiler
- Check for memory leaks

---

## API Reference

### OverlayManager

```kotlin
class OverlayManager(context: Context)

// Singleton
fun getInstance(context: Context): OverlayManager

// Confidence Overlay
fun showConfidence(result: ConfidenceResult)
fun updateConfidence(result: ConfidenceResult)
fun hideConfidence()

// Numbered Selection
fun showNumberedSelection(items: List<SelectableItem>)
fun updateNumberedSelection(items: List<SelectableItem>)
fun selectNumberedItem(number: Int): Boolean
fun hideNumberedSelection()

// Command Status
fun showCommandStatus(command: String, state: CommandState, message: String? = null)
fun updateCommandStatus(command: String? = null, state: CommandState? = null, message: String? = null)
fun hideCommandStatus()

// Context Menu
fun showContextMenu(items: List<MenuItem>, title: String? = null)
fun showContextMenuAt(items: List<MenuItem>, position: Point, title: String? = null)
fun selectContextMenuItem(id: String): Boolean
fun selectContextMenuByNumber(number: Int): Boolean
fun hideContextMenu()

// Management
fun hideAll()
fun isAnyVisible(): Boolean
fun isOverlayVisible(overlayName: String): Boolean
fun getActiveOverlays(): Set<String>
fun dispose()

// Convenience
fun showListening(partialText: String = "")
fun showProcessing(command: String)
fun showExecuting(command: String)
fun showSuccess(command: String, message: String? = null)
fun showError(command: String, error: String)
suspend fun dismissAfterDelay(delayMs: Long = 2000)
```

### Data Classes

```kotlin
data class SelectableItem(
    val number: Int,
    val label: String,
    val bounds: Rect,
    val action: () -> Unit
)

data class MenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val number: Int? = null,
    val enabled: Boolean = true,
    val action: () -> Unit
)

enum class CommandState {
    LISTENING, PROCESSING, EXECUTING, SUCCESS, ERROR
}
```

---

## Summary

This implementation provides **4 critical UI overlays** for VOS4's Week 2 milestone:

1. ✅ **ConfidenceOverlay** - Real-time confidence feedback
2. ✅ **NumberedSelectionOverlay** - Multi-item voice selection
3. ✅ **CommandStatusOverlay** - Command processing status
4. ✅ **ContextMenuOverlay** - Voice-activated menus

All overlays are:
- Built with Jetpack Compose
- Managed by centralized OverlayManager
- Tested with unit tests
- Ready for integration with VoiceAccessibility

**Total Implementation Time:** ~12 hours (as specified)

---

**Document Version:** 1.0
**Author:** VOS4 Development Team
**Review Status:** ✅ Complete
