# AvaMagic Package Structure

**Version:** 2.0
**Date:** 2025-11-23
**Status:** Active

## Overview

AvaMagic is the branded component library for the Avanues ecosystem, restructured with clean, non-redundant package naming. The branding comes from the package path (`com.augmentalis.AvaMagic`), not from redundant class name prefixes.

## Core Principles

1. **NO redundancy**: Use `com.augmentalis.AvaMagic` as root (not avaelements.magic)
2. **Clean class names**: `Button` (not MagicButton), `Chip` (not MagicTag)
3. **Package provides branding**: AvaMagic in path = automatic branding
4. **Clear categorization**: `elements/*` (interactive) vs `layout/*` (structural)

## Complete Package Hierarchy

```
com.augmentalis.AvaMagic/
├── elements/                    # Interactive "magic" components (36 total)
│   ├── tags/                   # Tag/chip components (1)
│   │   └── Chip.kt            # Previously: MagicTagComponent
│   │
│   ├── buttons/                # Button components (1)
│   │   └── Button.kt          # Previously: MagicButton
│   │
│   ├── cards/                  # Card components (1)
│   │   └── Card.kt            # Previously: Paper
│   │
│   ├── inputs/                 # Form input components (8)
│   │   ├── DatePicker.kt
│   │   ├── Dropdown.kt
│   │   ├── FileUpload.kt
│   │   ├── Radio.kt
│   │   ├── Rating.kt
│   │   ├── SearchBar.kt
│   │   ├── Slider.kt
│   │   └── TimePicker.kt
│   │
│   ├── display/                # Display/visual components (4)
│   │   ├── Avatar.kt
│   │   ├── Divider.kt
│   │   ├── EmptyState.kt
│   │   └── Skeleton.kt
│   │
│   ├── navigation/             # Navigation components (6)
│   │   ├── AppBar.kt
│   │   ├── BottomNav.kt
│   │   ├── Breadcrumb.kt
│   │   ├── Drawer.kt
│   │   ├── Pagination.kt
│   │   └── Tabs.kt
│   │
│   ├── feedback/               # User feedback components (7)
│   │   ├── Alert.kt
│   │   ├── Badge.kt
│   │   ├── Dialog.kt
│   │   ├── ProgressBar.kt
│   │   ├── Spinner.kt
│   │   ├── Toast.kt
│   │   └── Tooltip.kt
│   │
│   └── data/                   # Data visualization components (8)
│       ├── Accordion.kt
│       ├── Carousel.kt
│       ├── DataGrid.kt
│       ├── List.kt
│       ├── Stepper.kt
│       ├── Table.kt
│       ├── Timeline.kt
│       └── TreeView.kt
│
└── layout/                      # Generic layout components (3)
    ├── Container.kt            # Generic container with padding/margins
    ├── Row.kt                  # Horizontal flex layout
    └── Column.kt               # Vertical flex layout
```

## Component Count Summary

| Category | Count | Description |
|----------|-------|-------------|
| **Tags** | 1 | Chip components |
| **Buttons** | 1 | Button variants |
| **Cards** | 1 | Card containers |
| **Inputs** | 8 | Form inputs |
| **Display** | 4 | Visual display elements |
| **Navigation** | 6 | Navigation/menu components |
| **Feedback** | 7 | User feedback/notifications |
| **Data** | 8 | Complex data visualization |
| **Layout** | 3 | Structural layout components |
| **TOTAL** | **39** | **Complete component library** |

## Directory Structure

```
Universal/Libraries/AvaElements/
├── Core/src/commonMain/kotlin/com/augmentalis/
│   ├── AvaMagic/                          # NEW: Clean branded structure
│   │   ├── elements/                      # Interactive components
│   │   │   ├── tags/                      # Chip, etc.
│   │   │   ├── buttons/                   # Button, etc.
│   │   │   ├── cards/                     # Card, etc.
│   │   │   ├── inputs/                    # Form inputs
│   │   │   ├── display/                   # Visual displays
│   │   │   ├── navigation/                # Navigation
│   │   │   ├── feedback/                  # Feedback/notifications
│   │   │   └── data/                      # Data visualization
│   │   ├── layout/                        # Layout components
│   │   └── AvaMagic.kt                   # Main entry point
│   │
│   └── magicelements/core/               # Core infrastructure (unchanged)
│       ├── Component.kt
│       ├── Renderer.kt
│       ├── Theme.kt
│       └── types/
│
├── Renderers/
│   ├── Android/                           # Android Compose renderers
│   ├── iOS/                               # iOS UIKit/SwiftUI bridge
│   ├── Desktop/                           # Compose Desktop
│   └── Web/                               # TypeScript/React
│
└── components/
    ├── unified/                           # OLD: Being deprecated
    ├── phase1/                            # OLD: Legacy structure
    └── phase3/                            # OLD: Legacy structure
```

## Import Examples

### Before (Old Structure)

```kotlin
// Redundant "magic" everywhere
import com.augmentalis.avaelements.magic.buttons.MagicButton
import com.augmentalis.avaelements.magic.MagicComponents
import com.augmentalis.avaelements.layout.Container

// Usage
MagicButton(text = "Submit")
```

### After (New Structure)

```kotlin
// Clean imports - branding in package
import com.augmentalis.AvaMagic.elements.buttons.Button
import com.augmentalis.AvaMagic.elements.tags.Chip
import com.augmentalis.AvaMagic.layout.*

// Clean usage - simple names
Container {
    Row {
        Chip(label = "Kotlin")
        Button(text = "Submit")
    }
}
```

### Module Imports

```kotlin
// Import entire modules
import com.augmentalis.AvaMagic.elements.buttons.*
import com.augmentalis.AvaMagic.elements.inputs.*
import com.augmentalis.AvaMagic.layout.*

// Or import everything
import com.augmentalis.AvaMagic.*
```

## Component Renaming

| Old Name | New Name | Package |
|----------|----------|---------|
| `MagicButton` | `Button` | `AvaMagic.elements.buttons` |
| `MagicTagComponent` | `Chip` | `AvaMagic.elements.tags` |
| `Paper` | `Card` | `AvaMagic.elements.cards` |

## Platform Renderers

Each platform has renderers that map AvaMagic components to native UI:

### Android
```kotlin
// Location: Renderers/Android/src/androidMain/kotlin/com/augmentalis/
// Update imports from:
import com.augmentalis.magicelements.renderers.android.mappers.*

// To:
import com.augmentalis.AvaMagic.renderers.android.mappers.*
```

### iOS
```kotlin
// Location: Renderers/iOS/src/iosMain/kotlin/com/augmentalis/
// Update imports from:
import com.augmentalis.magicelements.renderer.ios.mappers.*

// To:
import com.augmentalis.AvaMagic.renderers.ios.mappers.*
```

### Desktop
```kotlin
// Location: Renderers/Desktop/src/desktopMain/kotlin/com/augmentalis/
// Update imports from:
import com.augmentalis.magicelements.renderer.desktop.*

// To:
import com.augmentalis.AvaMagic.renderers.desktop.*
```

### Web
```typescript
// Location: Renderers/Web/src/
// Update imports to reference new package structure
```

## Migration Guide

### For Application Developers

1. **Update imports** from old package structure:
   ```kotlin
   // Old
   import com.augmentalis.avaelements.magic.buttons.MagicButton

   // New
   import com.augmentalis.AvaMagic.elements.buttons.Button
   ```

2. **Update class names**:
   ```kotlin
   // Old
   MagicButton(text = "Click me")
   MagicTagComponent(label = "Tag")

   // New
   Button(text = "Click me")
   Chip(label = "Tag")
   ```

3. **Rebuild project** to pick up new structure

### For Library Developers

1. **Update renderer mappers** to import from `AvaMagic.*`
2. **Update build.gradle.kts** source sets if needed
3. **Run tests** to verify compatibility
4. **Update documentation** and examples

## Benefits of New Structure

### 1. **Cleaner Imports**
No more `MagicButton`, `MagicTag`, etc. Just `Button`, `Chip`.

### 2. **Better IDE Support**
Autocomplete shows: `AvaMagic.elements.buttons.Button`
Clear categorization in IDE navigation.

### 3. **Scalability**
Easy to add new components without namespace pollution:
- `AvaMagic.elements.tags.InputChip`
- `AvaMagic.elements.tags.FilterChip`
- `AvaMagic.elements.buttons.IconButton`

### 4. **Clear Separation**
- `elements/*` = Interactive components (the "magic")
- `layout/*` = Pure layout (no branding)

### 5. **Professional**
Package structure follows industry standards (like Material Design).

## Next Steps

### Phase 1: Core Migration (COMPLETE)
- ✅ Create AvaMagic package structure
- ✅ Move 39 components to new structure
- ✅ Update package declarations
- ✅ Rename classes (MagicButton → Button, etc.)
- ✅ Create index/export files

### Phase 2: Renderer Updates (IN PROGRESS)
- ⏳ Update Android renderer imports
- ⏳ Update iOS renderer imports
- ⏳ Update Desktop renderer imports
- ⏳ Update Web renderer imports
- ⏳ Update build configurations

### Phase 3: Expansion
- Add missing components:
  - `InputChip`, `FilterChip`, `ChoiceChip`, `ActionChip`
  - `IconButton`, `FloatingActionButton`, `ToggleButton`
  - `TextField`, `Checkbox`, `Switch`, `Autocomplete`
  - `Stack`, `Padding`, `Align`, `Center`, `Spacer`
  - `Flexible`, `Expanded`, `SizedBox`, `Wrap`, `FittedBox`

### Phase 4: Documentation
- Component API documentation
- Usage examples for each component
- Migration guides
- Design guidelines

### Phase 5: Testing
- Unit tests for all components
- Integration tests for renderers
- Visual regression tests
- Performance benchmarks

## File Locations

### Core Components
```
/Volumes/M-Drive/Coding/Avanues/Universal/Libraries/AvaElements/
  Core/src/commonMain/kotlin/com/augmentalis/AvaMagic/
```

### Renderers
```
/Volumes/M-Drive/Coding/Avanues/Universal/Libraries/AvaElements/Renderers/
  ├── Android/src/androidMain/kotlin/
  ├── iOS/src/iosMain/kotlin/
  ├── Desktop/src/desktopMain/kotlin/
  └── Web/src/
```

### Documentation
```
/Volumes/M-Drive/Coding/Avanues/docs/
  ├── AVAMAGIC-PACKAGE-STRUCTURE.md (this file)
  ├── FLUTTER-PARITY-SUMMARY.md
  └── PLATFORM-PARITY-ANALYSIS.md
```

## Support

For questions or issues with the AvaMagic package structure:
- Review this documentation
- Check component-specific docs in each package
- Refer to platform renderer documentation
- Consult the AvaElements team

---

**Status:** Active
**Last Updated:** 2025-11-23
**Version:** AvaMagic 2.0
