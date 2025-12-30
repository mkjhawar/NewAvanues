# AvaElements Package Structure Guide

**Version:** 3.0.0
**Date:** 2025-11-23
**Status:** In Progress - Prototype Complete

---

## Overview

AvaElements v3.0 introduces a clean, logical package structure separating **generic layout components** from **branded Magic components**. This guide covers the new structure, migration patterns, and usage examples.

---

## Package Organization

### New Structure

```
com.augmentalis.avaelements/
├── layout/                  # Generic layout components (NO prefix)
│   ├── Container.kt
│   ├── Row.kt
│   ├── Column.kt
│   ├── Stack.kt
│   ├── Padding.kt
│   ├── ... (more layout components)
│   └── LayoutComponents.kt  # Package documentation
│
├── magic/                   # Branded components (Magic* prefix)
│   ├── tags/
│   │   ├── MagicTag.kt
│   │   ├── MagicInput.kt
│   │   ├── MagicFilter.kt
│   │   ├── MagicChoice.kt
│   │   └── MagicAction.kt
│   ├── buttons/
│   │   ├── MagicButton.kt
│   │   ├── MagicIconButton.kt
│   │   └── ...
│   ├── cards/
│   │   └── MagicCard.kt
│   ├── inputs/
│   │   ├── MagicTextField.kt
│   │   ├── MagicCheckbox.kt
│   │   └── ...
│   ├── display/
│   ├── navigation/
│   ├── feedback/
│   ├── lists/
│   ├── animation/
│   └── MagicComponents.kt   # Package documentation
│
└── core/                    # Core utilities (unchanged)
```

---

## Component Categories

### Layout Components (No Prefix)

**Purpose:** Generic, un-branded components for arranging UI elements

**Naming:** No prefix (e.g., `Row`, `Column`, `Container`)

**Package:** `com.augmentalis.avaelements.layout`

**Available Components:**
- **Container** - Flexible wrapper with alignment
- **Row** - Horizontal layout (left-to-right)
- **Column** - Vertical layout (top-to-bottom)
- **Stack** - Z-index stacking
- **Padding** - Padding wrapper
- **Align** - Alignment wrapper
- **Center** - Centering wrapper
- **SizedBox** - Fixed-size box
- **Flexible** - Flex child with weight
- **Expanded** - Auto-expanding flex child
- **Positioned** - Absolute positioning
- **FittedBox** - Fit/scale content
- **Wrap** - Wrapping flow layout
- **Spacer** - Empty spacing
- **Grid** - Grid layout
- **ConstrainedBox** - Constraint wrapper

### Magic Components (Magic* Prefix)

**Purpose:** Branded, interactive components for Avanues ecosystem

**Naming:** Magic* prefix (e.g., `MagicButton`, `MagicTag`, `MagicCard`)

**Package:** `com.augmentalis.avaelements.magic.*`

**Categories:**
1. **Tags** (`magic/tags/`) - Tag-like components
2. **Buttons** (`magic/buttons/`) - Button components
3. **Cards** (`magic/cards/`) - Card containers
4. **Inputs** (`magic/inputs/`) - Form inputs
5. **Display** (`magic/display/`) - Display components
6. **Navigation** (`magic/navigation/`) - Navigation components
7. **Feedback** (`magic/feedback/`) - Dialogs, toasts, alerts
8. **Lists** (`magic/lists/`) - List components
9. **Animation** (`magic/animation/`) - Animated components

---

## Usage Examples

### Before (Old Structure)

```kotlin
import com.augmentalis.avaelements.components.phase1.layout.Row
import com.augmentalis.avaelements.components.phase1.layout.Column
import com.augmentalis.avaelements.components.phase1.form.Button
import com.augmentalis.avaelements.components.phase3.display.Chip

fun oldUI() {
    Row {
        Column {
            Button(text = "Submit")
            Chip(label = "Kotlin")
        }
    }
}
```

### After (New Structure)

```kotlin
import com.augmentalis.avaelements.layout.*
import com.augmentalis.avaelements.magic.buttons.*
import com.augmentalis.avaelements.magic.tags.*

fun newUI() {
    Row {  // No prefix - generic layout
        Column {  // No prefix - generic layout
            MagicButton(text = "Submit")  // Magic* prefix - branded component
            MagicTag(label = "Kotlin")    // Magic* prefix - branded component
        }
    }
}
```

### Clean Import Pattern

```kotlin
// Import entire layout package
import com.augmentalis.avaelements.layout.*

// Import specific Magic categories
import com.augmentalis.avaelements.magic.buttons.*
import com.augmentalis.avaelements.magic.tags.*
import com.augmentalis.avaelements.magic.inputs.*

// Usage
Container {
    Column {
        MagicButton.primary("Submit")
        MagicTextField(hint = "Enter name")
        Row {
            MagicTag(label = "Kotlin")
            MagicTag(label = "Compose")
        }
    }
}
```

---

## Migration Guide

### Component Name Mapping

| Old Name | New Name | Package |
|----------|----------|---------|
| `Button` | `MagicButton` | `magic.buttons` |
| `TextField` | `MagicTextField` | `magic.inputs` |
| `Card` | `MagicCard` | `magic.cards` |
| `Chip` | `MagicTag` | `magic.tags` |
| `InputChip` | `MagicInput` | `magic.tags` |
| `FilterChip` | `MagicFilter` | `magic.tags` |
| `ChoiceChip` | `MagicChoice` | `magic.tags` |
| `ActionChip` | `MagicAction` | `magic.tags` |
| `Row` | `Row` | `layout` (no change to name) |
| `Column` | `Column` | `layout` (no change to name) |
| `Container` | `Container` | `layout` (no change to name) |

### Package Migration

| Old Package | New Package |
|-------------|-------------|
| `com.augmentalis.avaelements.components.phase1.form` | `com.augmentalis.avaelements.magic.buttons` or `magic.inputs` |
| `com.augmentalis.avaelements.components.phase1.layout` | `com.augmentalis.avaelements.layout` |
| `com.augmentalis.avaelements.components.phase3.display` | `com.augmentalis.avaelements.magic.display` or `magic.tags` |
| `com.augmentalis.avaelements.flutter.layout` | `com.augmentalis.avaelements.layout` |
| `com.augmentalis.avaelements.flutter.material.chips` | `com.augmentalis.avaelements.magic.tags` |

### Automated Migration Script

Use the provided migration script to update imports:

```bash
# Run migration script
./scripts/migrate-to-v3.sh

# Options:
#   --dry-run    : Preview changes without applying
#   --platform   : Migrate specific platform (android, ios, web, desktop)
#   --component  : Migrate specific component type (layout, magic)
```

### Manual Migration Steps

1. **Update imports:**
   ```kotlin
   // OLD
   import com.augmentalis.avaelements.components.phase1.form.Button

   // NEW
   import com.augmentalis.avaelements.magic.buttons.MagicButton
   ```

2. **Rename component usage:**
   ```kotlin
   // OLD
   Button(text = "Click me")

   // NEW
   MagicButton(text = "Click me")
   ```

3. **Update renderer mappings:**
   ```kotlin
   // OLD
   is Button -> renderButton(component)

   // NEW
   is MagicButton -> renderMagicButton(component)
   ```

4. **Update serialization type names:**
   ```kotlin
   // OLD
   override val type: String = "Button"

   // NEW
   override val type: String = "MagicButton"
   ```

---

## Renderer Updates

### Android Renderer

**File:** `Renderers/Android/src/androidMain/.../ComposeRenderer.kt`

```kotlin
// OLD
import com.augmentalis.avaelements.components.phase1.form.Button
import com.augmentalis.avaelements.components.phase1.layout.Row

fun render(component: Component) = when (component) {
    is Button -> renderButton(component)
    is Row -> renderRow(component)
    // ...
}

// NEW
import com.augmentalis.avaelements.layout.Row
import com.augmentalis.avaelements.magic.buttons.MagicButton

fun render(component: Component) = when (component) {
    is MagicButton -> renderMagicButton(component)
    is Row -> renderRow(component)  // No change to name
    // ...
}
```

### iOS Renderer

**File:** `Renderers/iOS/src/iosMain/.../SwiftUIRenderer.kt`

```swift
// Similar pattern - update import paths and component names
```

### Web Renderer

**File:** `Renderers/Web/src/renderer/ComponentRegistry.ts`

```typescript
// OLD
import { Button } from '../components/phase1/form/Button';

// NEW
import { MagicButton } from '../components/magic/buttons/MagicButton';
```

---

## Build Configuration

### Update settings.gradle.kts

```kotlin
// Add unified module
include(":Universal:Libraries:AvaElements:components:unified")

// Optionally deprecate old modules
// include(":Universal:Libraries:AvaElements:components:phase1")  // Deprecated
// include(":Universal:Libraries:AvaElements:components:phase3")  // Deprecated
```

### Update Renderer Dependencies

```kotlin
// Renderers/Android/build.gradle.kts
dependencies {
    // Replace phase1/phase3 dependencies
    // implementation(project(":Universal:Libraries:AvaElements:components:phase1"))

    // With unified dependency
    implementation(project(":Universal:Libraries:AvaElements:components:unified"))
}
```

---

## Testing Strategy

### Unit Tests

```kotlin
import com.augmentalis.avaelements.layout.Row
import com.augmentalis.avaelements.magic.buttons.MagicButton
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentTests {
    @Test
    fun testMagicButtonType() {
        val button = MagicButton(text = "Test")
        assertEquals("MagicButton", button.type)
    }

    @Test
    fun testRowType() {
        val row = Row()
        assertEquals("Row", row.type)
    }
}
```

### Visual Regression Tests

Run visual tests after migration:

```bash
./gradlew :Renderers:Android:connectedAndroidTest
./gradlew :Renderers:iOS:iosTest
npm test --prefix Renderers/Web
```

---

## Deprecation Strategy

### Phase 1: Dual Support (Current)

Both old and new packages work simultaneously using type aliases:

```kotlin
// Legacy support
package com.augmentalis.avaelements.components.phase1.form

@Deprecated("Use com.augmentalis.avaelements.magic.buttons.MagicButton", ReplaceWith("MagicButton"))
typealias Button = com.augmentalis.avaelements.magic.buttons.MagicButton
```

### Phase 2: Deprecation Warnings (v3.1)

Add deprecation warnings to old packages.

### Phase 3: Removal (v4.0)

Remove old packages entirely.

---

## Platform-Specific Notes

### Android

- Update `AndroidManifest.xml` namespaces if needed
- Update ProGuard rules for renamed components
- Update R8 rules for serialization

### iOS

- Update Swift bridging headers
- Update Kotlin/Native exports
- Update Package.swift if needed

### Web

- Update TypeScript type definitions
- Update package.json exports
- Update webpack/vite configuration

### Desktop

- Update Compose Desktop imports
- Update namespace in build configuration

---

## FAQ

### Q: Do I need to update all components at once?

A: No. The dual-support phase allows gradual migration. Start with new features, then migrate existing code incrementally.

### Q: Will this break my existing code?

A: Not immediately. Phase 1 provides backwards compatibility via type aliases. However, you should migrate to avoid issues in future versions.

### Q: Why separate layout and magic components?

A: This separation provides:
- **Clarity:** Layout vs interactive components
- **Branding:** Magic* prefix for Avanues ecosystem
- **Modularity:** Import only what you need
- **Maintainability:** Easier to find and organize components

### Q: What about flutter-parity components?

A: Flutter-parity layout components (Align, Center, Padding, etc.) move to the generic `layout` package. Flutter Material components (chips, etc.) become Magic* components in the `magic` package.

### Q: How do I migrate a renderer?

A:
1. Update imports to new packages
2. Rename component class names (Button → MagicButton)
3. Update type checks in render() method
4. Update mapper files
5. Run tests to verify

---

## Example: Complete Migration

### Before

```kotlin
// File: MyScreen.kt
package com.example.myapp

import com.augmentalis.avaelements.components.phase1.layout.Column
import com.augmentalis.avaelements.components.phase1.layout.Row
import com.augmentalis.avaelements.components.phase1.form.Button
import com.augmentalis.avaelements.components.phase3.display.Chip

fun MyScreen() {
    Column {
        Row {
            Chip(label = "Kotlin")
            Chip(label = "Compose")
        }
        Button(text = "Submit") {
            // onClick
        }
    }
}
```

### After

```kotlin
// File: MyScreen.kt
package com.example.myapp

import com.augmentalis.avaelements.layout.*
import com.augmentalis.avaelements.magic.buttons.*
import com.augmentalis.avaelements.magic.tags.*

fun MyScreen() {
    Column {  // Layout component - no prefix
        Row {  // Layout component - no prefix
            MagicTag(label = "Kotlin")    // Magic component - Magic* prefix
            MagicTag(label = "Compose")   // Magic component - Magic* prefix
        }
        MagicButton(text = "Submit") {    // Magic component - Magic* prefix
            // onClick
        }
    }
}
```

---

## Resources

- **Full Analysis:** `/docs/PACKAGE-RESTRUCTURE-ANALYSIS.md`
- **Component Registry:** `/docs/COMPLETE-COMPONENT-REGISTRY-LIVING.md`
- **Migration Scripts:** `/scripts/migrate-to-v3.sh`
- **Prototype Location:** `/Universal/Libraries/AvaElements/components/unified/`

---

## Support

For questions or issues during migration:

1. Check this guide
2. Review the analysis document
3. Run migration scripts with `--dry-run` first
4. Test thoroughly before committing

---

**Last Updated:** 2025-11-23
**Status:** Prototype Complete - Full Migration Pending
