# Cross-Platform Component Parity Strategy

**Version**: 1.0
**Date**: November 18, 2025
**Author**: Manoj Jhawar

---

## Philosophy

AVAMagic provides a **unified component API** where every component works identically across all platforms. When a native feature exists on one platform but not another, **we create a custom implementation** to maintain parity.

> **Principle**: Write once, render everywhere with the same behavior.

---

## Architecture Overview

```
┌─────────────────────────────────────┐
│     Component Definitions           │
│   (commonMain - Kotlin/Common)      │
│  ScaffoldComponent, ListTileComponent, etc.
└─────────────────┬───────────────────┘
                  │
         ┌────────┴────────┐
         │                 │
    ┌────▼────┐      ┌─────▼────┐
    │ Android │      │   iOS    │
    │Renderer │      │ Renderer │
    │ (Compose)│     │(SwiftUI) │
    └─────────┘      └──────────┘
```

---

## Component Parity Matrix

### Legend
- ✅ Native support
- 🔧 Custom implementation needed
- 🎨 Native with styling bridge

| Component | Android Native | iOS Native | Parity Strategy |
|-----------|---------------|------------|-----------------|
| **Navigation** |
| NavigationStack | ✅ Navigation Compose | ✅ NavigationStack | Direct mapping |
| NavigationSplitView | 🔧 Custom | ✅ iOS 16+ | Create adaptive split for Android |
| NavigationDrawer | ✅ ModalNavigationDrawer | 🔧 Custom | Create drawer for iOS |
| NavigationRail | ✅ Material3 | 🔧 Custom | Create rail for iOS |
| BottomNavigation | ✅ NavigationBar | ✅ TabView | Direct mapping |
| **Layout** |
| Scaffold | ✅ Scaffold | 🔧 Custom | Create scaffold wrapper for iOS |
| LazyColumn/List | ✅ LazyColumn | ✅ List | Direct mapping |
| LazyRow | ✅ LazyRow | 🔧 ScrollView+HStack | Create lazy horizontal for iOS |
| Grid | ✅ LazyVerticalGrid | ✅ LazyVGrid | Direct mapping |
| Spacer | ✅ Spacer | ✅ Spacer | Direct mapping |
| Box/ZStack | ✅ Box | ✅ ZStack | Direct mapping |
| Surface | ✅ Surface | 🔧 Custom | Create surface for iOS |
| **Input** |
| SegmentedButton | ✅ SegmentedButton | ✅ Picker.segmented | Direct mapping |
| DatePicker | ✅ DatePickerDialog | ✅ DatePicker | Direct mapping |
| TimePicker | ✅ TimePickerDialog | ✅ DatePicker.wheel | Direct mapping |
| Slider | ✅ Slider | ✅ Slider | Direct mapping |
| Dropdown | ✅ ExposedDropdownMenu | ✅ Menu | Direct mapping |
| SearchBar | ✅ SearchBar | ✅ .searchable | Direct mapping |
| FileUpload | ✅ Intent picker | ✅ PHPicker | Platform-specific bridge |
| **Display** |
| ListTile | ✅ ListItem | 🔧 Custom | Create list tile for iOS |
| TabBar | ✅ TabRow | ✅ TabView | Direct mapping |
| CircularProgress | ✅ CircularProgressIndicator | ✅ ProgressView | Direct mapping |
| Badge | ✅ Badge | ✅ .badge | Direct mapping |
| Chip | ✅ FilterChip/InputChip | 🔧 Custom | Create chip for iOS |
| Avatar | ✅ Custom | ✅ Custom | Both need implementation |
| Tooltip | ✅ PlainTooltip | 🔧 Custom popover | Create tooltip for iOS |
| **Feedback** |
| BottomSheet | ✅ ModalBottomSheet | ✅ .sheet | Direct mapping |
| Dialog/Alert | ✅ AlertDialog | ✅ .alert | Direct mapping |
| Snackbar | ✅ Snackbar | 🔧 Custom | Create snackbar for iOS |
| Toast | ✅ Custom (via Snackbar) | 🔧 Custom | Both need implementation |
| ContextMenu | ✅ DropdownMenu | ✅ .contextMenu | Direct mapping |
| **State & Observation** |
| @State | ✅ MutableState | ✅ @State | Direct mapping |
| @Observable | ✅ StateFlow | ✅ @Observable | Platform bridge |
| **Interaction** |
| FocusState | ✅ FocusRequester | ✅ @FocusState | Direct mapping |
| HapticFeedback | ✅ Vibration API | ✅ sensoryFeedback | Platform bridge |
| DragDrop | ✅ Modifier drag | ✅ .draggable | Direct mapping |

---

## Custom Implementation Guidelines

### When Android Has Native, iOS Doesn't

**Example: Material3 Snackbar**

Android has native Snackbar, iOS doesn't. Create iOS implementation:

```swift
// iOS Custom Snackbar
struct SnackbarView: View {
    let message: String
    let action: String?
    let onAction: (() -> Void)?
    @Binding var isShowing: Bool

    var body: some View {
        VStack {
            Spacer()
            HStack {
                Text(message)
                    .foregroundColor(.white)
                if let action = action {
                    Button(action) { onAction?() }
                        .foregroundColor(.yellow)
                }
            }
            .padding()
            .background(Color(UIColor.darkGray))
            .cornerRadius(8)
            .padding()
        }
        .transition(.move(edge: .bottom))
        .animation(.easeInOut, value: isShowing)
    }
}
```

### When iOS Has Native, Android Doesn't

**Example: NavigationSplitView**

iOS 16+ has adaptive split view, Android doesn't. Create Android implementation:

```kotlin
// Android Custom NavigationSplitView
@Composable
fun NavigationSplitView(
    sidebar: @Composable () -> Unit,
    content: @Composable () -> Unit,
    detail: @Composable () -> Unit
) {
    val windowSize = calculateWindowSizeClass()

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            // Single column navigation (phone)
            NavigationHost(/* ... */)
        }
        WindowWidthSizeClass.Medium -> {
            // Two column (tablet portrait)
            Row {
                Box(Modifier.weight(0.4f)) { sidebar() }
                Box(Modifier.weight(0.6f)) { content() }
            }
        }
        WindowWidthSizeClass.Expanded -> {
            // Three column (tablet landscape, desktop)
            Row {
                Box(Modifier.weight(0.25f)) { sidebar() }
                Box(Modifier.weight(0.35f)) { content() }
                Box(Modifier.weight(0.4f)) { detail() }
            }
        }
    }
}
```

### When Both Need Custom

**Example: ListTile with leading/trailing content**

Neither platform has the exact component, both need implementation:

```kotlin
// Component Definition (commonMain)
data class ListTileComponent(
    val title: String,
    val subtitle: String? = null,
    val leading: Component? = null,
    val trailing: Component? = null,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
) : Component

// Android Implementation
@Composable
fun ListTileMapper.map(component: ListTileComponent): Unit {
    ListItem(
        headlineContent = { Text(component.title) },
        supportingContent = component.subtitle?.let { { Text(it) } },
        leadingContent = component.leading?.let { render(it) },
        trailingContent = component.trailing?.let { render(it) }
    )
}

// iOS Implementation
func ListTileMapper.map(component: ListTileComponent) -> some View {
    HStack {
        if let leading = component.leading {
            render(leading)
        }
        VStack(alignment: .leading) {
            Text(component.title)
            if let subtitle = component.subtitle {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
        }
        Spacer()
        if let trailing = component.trailing {
            render(trailing)
        }
    }
    .contentShape(Rectangle())
    .onTapGesture { component.onClick?() }
}
```

---

## Platform-Specific Bridges

### File/Media Access

```kotlin
// Component Definition
data class FileUploadComponent(
    val acceptedTypes: List<String> = listOf("*/*"),
    val multiple: Boolean = false,
    val onFilesSelected: ((List<String>) -> Unit)? = null
) : Component

// Android: Uses Intent + ActivityResult
// iOS: Uses PHPickerViewController + Coordinator
// Both return same data format: List<String> of URIs/paths
```

### Haptic Feedback

```kotlin
// Unified API
enum class HapticType {
    LIGHT, MEDIUM, HEAVY, SUCCESS, WARNING, ERROR
}

fun triggerHaptic(type: HapticType) // Platform-specific implementation

// Android: Uses VibrationEffect or HapticFeedbackConstants
// iOS: Uses UIImpactFeedbackGenerator or sensoryFeedback (iOS 17+)
```

---

## Implementation Priorities

### Phase 1: Critical Gaps (Week 1-2)
1. **Android Grid** - LazyVerticalGrid implementation
2. **iOS Snackbar** - Custom toast/snackbar view
3. **iOS ListTile** - HStack-based list row
4. **Android NavigationSplitView** - Adaptive layout
5. **iOS Chip** - Custom chip/tag view

### Phase 2: Enhanced Parity (Week 3-4)
6. **iOS Surface** - Elevation/shadow wrapper
7. **iOS NavigationDrawer** - Side panel
8. **Android MatchedGeometryEffect** - Shared element transitions
9. **Both Avatar** - Async image with fallback
10. **Both Tooltip** - Platform-appropriate popover

### Phase 3: Polish (Week 5-6)
11. **iOS Scaffold** - AppBar/FAB/Drawer coordinator
12. **Both Charts** - Charting abstraction
13. **iOS NavigationRail** - Tablet navigation
14. **Both DataGrid** - Table with sorting/filtering

---

## Testing Strategy

### Component Parity Tests

```kotlin
// Test that component renders same logical output on both platforms
class ParityTests {
    @Test
    fun listTile_rendersIdentically() {
        val component = ListTileComponent(
            title = "Test",
            subtitle = "Subtitle",
            leading = IconComponent("star"),
            trailing = IconComponent("chevron_right")
        )

        // Android
        val androidOutput = androidRenderer.render(component)
        assertThat(androidOutput).hasTitle("Test")
        assertThat(androidOutput).hasSubtitle("Subtitle")
        assertThat(androidOutput).hasLeadingIcon()
        assertThat(androidOutput).hasTrailingIcon()

        // iOS
        val iosOutput = iosRenderer.render(component)
        assertThat(iosOutput).hasTitle("Test")
        assertThat(iosOutput).hasSubtitle("Subtitle")
        assertThat(iosOutput).hasLeadingIcon()
        assertThat(iosOutput).hasTrailingIcon()
    }
}
```

---

## Versioning & Fallbacks

### Minimum Platform Versions
- **Android**: API 21 (Lollipop) with Compose 1.5+
- **iOS**: iOS 15 with SwiftUI 3+ (iOS 17 for @Observable)

### Feature Detection

```kotlin
// Check platform capabilities at runtime
interface PlatformCapabilities {
    val supportsBlur: Boolean      // iOS: yes, Android: API 31+
    val supportsHaptics: Boolean   // Both yes
    val supportsWidgets: Boolean   // Platform-specific
    val maxGridColumns: Int        // Platform/device dependent
}

// Use for graceful degradation
if (capabilities.supportsBlur) {
    applyGlassmorphism()
} else {
    applySolidBackground()
}
```

---

## Contribution Guidelines

### Adding a New Component

1. **Define in commonMain** (`UI/Core/src/commonMain/`)
2. **Implement Android mapper** (`Renderers/Android/`)
3. **Implement iOS mapper** (`Renderers/iOS/`)
4. **Add parity tests**
5. **Update this document's parity matrix**

### Custom Implementation Checklist

- [ ] Matches native behavior as closely as possible
- [ ] Supports all component properties
- [ ] Handles edge cases (empty states, errors)
- [ ] Respects theme tokens
- [ ] Accessible (contentDescription, focus order)
- [ ] Tested on real devices

---

## Related Documents

- [AVAMAGIC-STATUS.md](./AVAMAGIC-STATUS.md) - Implementation status
- [IDEACODE5-PROJECT-SPEC-251030-0304.md](./IDEACODE5-PROJECT-SPEC-251030-0304.md) - Technical specifications
- [IDEACODE5-TASKS-251030-0304.md](./IDEACODE5-TASKS-251030-0304.md) - Task breakdown

---

**Last Updated**: November 18, 2025
