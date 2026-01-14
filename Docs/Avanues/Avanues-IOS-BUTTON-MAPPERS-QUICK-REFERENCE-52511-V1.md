# iOS SwiftUI Button Mappers - Quick Reference

**File:** `Universal/Libraries/AvaElements/Renderers/iOS/src/iosMain/kotlin/com/augmentalis/avaelements/renderer/ios/mappers/flutterparity/FlutterParityButtonMappers.kt`

**Created:** 2025-11-25  
**Lines of Code:** 562  
**Components:** 3 Advanced Button Mappers

---

## Overview

Complete iOS SwiftUI mappers for Flutter Material advanced button components, providing full Material Design 3 visual parity and native iOS user experience.

---

## Components Implemented

### 1. SplitButton Mapper

**Function:** `mapSplitButton(component: SplitButton): SwiftUIView`

**Features:**
- Primary action button with dropdown menu
- Menu appears on arrow button click
- Individual menu items with handlers
- Optional icons for menu items
- Menu position control (top/bottom)
- Material 3 visual styling

**iOS Implementation:**
- HStack with Button + Menu components
- Divider between main and dropdown buttons
- Native Menu with Button children
- VoiceOver: "Split button with N menu items"

**Properties Mapped:**
- `text` → Main button label
- `icon` → Optional button icon (SF Symbol)
- `enabled` → Button enabled state
- `menuItems` → List of menu items with labels and icons
- `menuPosition` → Menu appears above/below button
- `onPressed` → Main button action
- `onMenuItemPressed` → Menu item selection handler

**Touch Targets:**
- Main button: 48dp minimum height
- Menu button: 48dp minimum height
- Combined width: Auto-sized to content

---

### 2. LoadingButton Mapper

**Function:** `mapLoadingButton(component: LoadingButton): SwiftUIView`

**Features:**
- Automatic disable during loading
- Circular progress indicator (native ProgressView)
- Configurable indicator position (start/center/end)
- Optional loading text override
- Smooth fade animations
- Material 3 visual styling

**iOS Implementation:**
- HStack with ProgressView + Text
- ProgressView automatically styled for iOS
- Animation between states (0.2s default)
- VoiceOver: "Button, loading" state

**Properties Mapped:**
- `text` → Button label
- `icon` → Optional icon (hidden when loading)
- `enabled` → Button enabled state
- `loading` → Shows progress indicator
- `loadingPosition` → Start/Center/End
- `loadingText` → Override text when loading
- `onPressed` → Button action handler

**Loading Positions:**
- **Start:** `[Spinner] Text`
- **Center:** `[Spinner only]` (text hidden)
- **End:** `Text [Spinner]`

**Touch Targets:**
- Minimum: 48dp height × 64dp width
- Auto-expands to content

---

### 3. CloseButton Mapper

**Function:** `mapCloseButton(component: CloseButton): SwiftUIView`

**Features:**
- Standardized X close icon (SF Symbol "xmark")
- Three sizes: Small (18dp), Medium (24dp), Large (32dp)
- Edge positioning support
- Circular background on hover/press
- Material 3 visual styling
- Minimum 48dp touch target

**iOS Implementation:**
- Button with Image(systemName: "xmark")
- Circle background with 0.1 opacity
- Hover effect (iOS 13.4+, iPadOS, macOS)
- VoiceOver: "Close" + state

**Properties Mapped:**
- `enabled` → Button enabled state
- `size` → Small/Medium/Large icon size
- `edge` → Edge positioning (Start/End/Top/Bottom/None)
- `onPressed` → Close action handler

**Sizes:**
- **Small:** 18dp icon, 48dp touch target
- **Medium:** 24dp icon, 48dp touch target
- **Large:** 32dp icon, 48dp touch target

**Edge Positioning:**
- Adds 4dp padding to specified edge
- None: No additional padding
- Used in app bars, navigation bars

---

## Accessibility Features

All three mappers include full VoiceOver support:

### SplitButton
- Label: "{text} split button with {N} menu items"
- Hint: "Double tap to {action}"
- Menu items announced individually
- Disabled state announced

### LoadingButton
- Label: Button text
- Value: "Loading" when in loading state
- Trait: "updatesFrequently" when loading
- Hint: "Please wait" / "Double tap to {action}"
- Disabled state announced

### CloseButton
- Label: "Close" (or custom)
- Hint: "Double tap to close"
- Trait: "button"
- Disabled state announced
- Size not announced (visual only)

---

## Material Design 3 Compliance

### Color Scheme Support
- `primaryColor` / `onPrimary` (LoadingButton)
- `fillColor` / `onSurface` (SplitButton)
- `buttonBackground` (CloseButton hover)
- `separator` (SplitButton divider)
- `outline` (SplitButton border)

### Dark Mode
- All colors automatically adapt via SwiftUI environment
- Opacity values consistent (disabled = 0.38)

### Typography
- Dynamic Type support
- Font styles: caption, body, title3
- Automatic text scaling

---

## Touch Target Compliance

All buttons meet WCAG 2.1 Level AA requirements:

- **Minimum:** 48dp × 48dp touch target
- **SplitButton:** Combined width auto-sizes
- **LoadingButton:** 48dp height × 64dp min width
- **CloseButton:** Always 48dp × 48dp minimum

---

## SwiftUI Bridge Types

### Modifiers Used
- `Frame` - Size constraints and alignment
- `Padding` - Edge insets
- `Background` - Fill colors
- `ForegroundColor` - Text/icon colors
- `CornerRadius` - Rounded corners (8dp)
- `Opacity` - Transparency (1.0 enabled, 0.38 disabled)
- `Disabled` - Enabled state
- `Animation` - Smooth transitions
- `Overlay` - Border strokes
- `HoverEffect` - Pointer interaction (iOS 13.4+)
- `AccessibilityLabel` - VoiceOver label
- `AccessibilityHint` - VoiceOver hint
- `AccessibilityValue` - State value
- `AccessibilityElement` - Grouping
- `AccessibilityAddTraits` - Semantic traits

### View Types Used
- `Button` - Primary action buttons
- `Menu` - Dropdown menu
- `HStack` - Horizontal layout
- `Group` - Accessibility grouping
- `Text` - Labels
- `Image` - Icons (SF Symbols)
- `Label` - Text + Icon
- `ProgressView` - Loading spinner
- `Circle` - Background shapes
- `Divider` - Visual separator
- `RoundedRectangle` - Border overlay

---

## Usage Examples

### SplitButton

```kotlin
val splitButtonView = mapSplitButton(
    SplitButton(
        text = "Save",
        icon = "square.and.arrow.down",
        enabled = true,
        menuItems = listOf(
            SplitButton.MenuItem("draft", "Save as Draft", icon = "doc"),
            SplitButton.MenuItem("template", "Save as Template", icon = "doc.badge.plus")
        ),
        menuPosition = SplitButton.MenuPosition.Bottom,
        onPressed = { /* Save action */ },
        onMenuItemPressed = { value -> /* Handle menu item */ }
    )
)
```

### LoadingButton

```kotlin
val loadingButtonView = mapLoadingButton(
    LoadingButton(
        text = "Submit",
        icon = "paperplane",
        enabled = true,
        loading = isSubmitting,
        loadingPosition = LoadingButton.LoadingPosition.Start,
        loadingText = "Submitting...",
        onPressed = { /* Submit action */ }
    )
)
```

### CloseButton

```kotlin
val closeButtonView = mapCloseButton(
    CloseButton(
        enabled = true,
        size = CloseButton.Size.Medium,
        edge = CloseButton.EdgePosition.End,
        onPressed = { /* Close dialog */ }
    )
)
```

---

## Testing Checklist

- [ ] Visual appearance matches Material 3 specs
- [ ] Touch targets meet 48dp minimum
- [ ] VoiceOver announces all states correctly
- [ ] Dark mode colors adapt properly
- [ ] Dynamic Type scales text correctly
- [ ] Disabled state shows 0.38 opacity
- [ ] Loading animations are smooth
- [ ] Menu positioning works (top/bottom)
- [ ] Hover effects work on supported devices
- [ ] RTL languages supported (auto via SwiftUI)

---

## Performance Notes

- **Rendering:** All mappers are pure functions (no side effects)
- **Memory:** Minimal allocations (immutable data structures)
- **Animations:** Native SwiftUI animation engine (hardware accelerated)
- **Accessibility:** Zero-cost abstractions (compiled to native)

---

## Future Enhancements

Potential improvements for future versions:

1. **SplitButton**
   - Custom menu item separators
   - Menu item groups with headers
   - Keyboard navigation support

2. **LoadingButton**
   - Custom progress indicator styles
   - Progress percentage display
   - Determinate progress mode

3. **CloseButton**
   - Custom icons (beyond "xmark")
   - Rotation animation on close
   - Confirmation dialog integration

---

## Related Files

- **Component Definitions:** `Universal/Libraries/AvaElements/components/flutter-parity/src/commonMain/kotlin/com/augmentalis/avaelements/flutter/material/advanced/`
- **Bridge Models:** `Universal/Libraries/AvaElements/Renderers/iOS/src/iosMain/kotlin/com/augmentalis/magicelements/renderer/ios/bridge/SwiftUIModels.kt`
- **Other Mappers:** `Universal/Libraries/AvaElements/Renderers/iOS/src/iosMain/kotlin/com/augmentalis/avaelements/renderer/ios/mappers/flutterparity/`

---

**Version:** 3.1.0-android-parity-ios  
**Status:** Complete  
**Last Updated:** 2025-11-25
