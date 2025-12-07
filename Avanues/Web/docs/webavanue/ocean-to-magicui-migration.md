# Ocean to MagicUI Migration Guide

**Version:** 1.0
**Date:** 2025-12-03
**Status:** Active

---

## Overview

WebAvanue uses a two-layer component architecture that enables seamless migration from Ocean theme (Compose Material3) to MagicUI:

```
Application Code
       │
       ├──> GlassmorphicComponents.kt  (Current: Compose Material3)
       │                                (Future: MagicUI delegates)
       └──> OceanThemeExtensions.kt    (Current: Custom modifiers)
                                       (Future: MagicUI modifiers)
```

**Key Benefits:**
- **Zero app code changes** during MagicUI migration
- **1:1 API mapping** ensures drop-in replacement
- **Gradual migration** - can coexist during transition
- **Type safety** maintained throughout

---

## Component Mapping

### Surface Components

| Current (Ocean) | Future (MagicUI) | API Changes |
|----------------|------------------|-------------|
| `GlassSurface` | `MagicUI.Surface` | None - identical API |
| `GlassCard` | `MagicUI.Card` | None - identical API |
| `GlassBubble` | `MagicUI.ChatBubble` | None - identical API |

### Button Components

| Current (Ocean) | Future (MagicUI) | API Changes |
|----------------|------------------|-------------|
| `OceanButton` | `MagicUI.Button` | None - identical API |
| `GlassFloatingActionButton` | `MagicUI.FAB` | None - identical API |
| `GlassIconButton` | `MagicUI.IconButton` | None - identical API |
| `GlassChip` | `MagicUI.Chip` | None - identical API |

### Modifiers

| Current (Ocean) | Future (MagicUI) | API Changes |
|----------------|------------------|-------------|
| `Modifier.glass()` | `Modifier.magicGlass()` | None - identical signature |
| `Modifier.glassLight()` | `Modifier.magicGlassLight()` | None - identical signature |
| `Modifier.glassMedium()` | `Modifier.magicGlassMedium()` | None - identical signature |
| `Modifier.glassHeavy()` | `Modifier.magicGlassHeavy()` | None - identical signature |
| `Modifier.glassFrosted()` | `Modifier.magicFrosted()` | None - identical signature |

### Presets

| Current (Ocean) | Future (MagicUI) | API Changes |
|----------------|------------------|-------------|
| `OceanGlass.card()` | `MagicUI.GlassPresets.card()` | None |
| `OceanGlass.surface()` | `MagicUI.GlassPresets.surface()` | None |
| `OceanGlass.elevated()` | `MagicUI.GlassPresets.elevated()` | None |
| `OceanGlass.dialog()` | `MagicUI.GlassPresets.dialog()` | None |
| `OceanGlass.bubble()` | `MagicUI.GlassPresets.bubble()` | None |
| `OceanGlass.button()` | `MagicUI.GlassPresets.button()` | None |
| `OceanGlass.chip()` | `MagicUI.GlassPresets.chip()` | None |

### Shapes

| Current (Ocean) | Future (MagicUI) | API Changes |
|----------------|------------------|-------------|
| `GlassShapes.default` | `MagicUI.Shapes.default` | None |
| `GlassShapes.small` | `MagicUI.Shapes.small` | None |
| `GlassShapes.large` | `MagicUI.Shapes.large` | None |
| `GlassShapes.bubbleStart` | `MagicUI.Shapes.bubbleStart` | None |
| `GlassShapes.bubbleEnd` | `MagicUI.Shapes.bubbleEnd` | None |

### Gradients

| Current (Ocean) | Future (MagicUI) | API Changes |
|----------------|------------------|-------------|
| `OceanGradients.surfaceGradient` | `MagicUI.Gradients.surface` | None |
| `OceanGradients.dialogGradient` | `MagicUI.Gradients.dialog` | None |
| `OceanGradients.primaryGradient` | `MagicUI.Gradients.primary` | None |

---

## Usage Examples

### Current Usage (Ocean Theme)

```kotlin
import com.augmentalis.Avanues.web.universal.presentation.ui.components.*

@Composable
fun MyScreen() {
    // Surface with glass effect
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        glassLevel = GlassLevel.MEDIUM,
        border = GlassDefaults.border
    ) {
        // Content
    }

    // Card with glass effect
    GlassCard(
        onClick = { /* ... */ },
        glass = true,
        glassLevel = GlassLevel.LIGHT
    ) {
        Text("Card content")
    }

    // Button with glass effect
    OceanButton(
        onClick = { /* ... */ },
        glass = true,
        glassLevel = GlassLevel.MEDIUM
    ) {
        Text("Click me")
    }

    // Using modifier
    Box(
        modifier = Modifier
            .glass(
                backgroundColor = OceanTheme.surface,
                glassLevel = GlassLevel.HEAVY
            )
    ) {
        // Content
    }

    // Using preset
    Box(
        modifier = Modifier.then(OceanGlass.card())
    ) {
        // Content
    }
}
```

### Future Usage (MagicUI) - SAME CODE!

```kotlin
import com.augmentalis.Avanues.web.universal.presentation.ui.components.*

@Composable
fun MyScreen() {
    // Surface with glass effect - NO CHANGES
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        glassLevel = GlassLevel.MEDIUM,
        border = GlassDefaults.border
    ) {
        // Content
    }

    // Card with glass effect - NO CHANGES
    GlassCard(
        onClick = { /* ... */ },
        glass = true,
        glassLevel = GlassLevel.LIGHT
    ) {
        Text("Card content")
    }

    // Button with glass effect - NO CHANGES
    OceanButton(
        onClick = { /* ... */ },
        glass = true,
        glassLevel = GlassLevel.MEDIUM
    ) {
        Text("Click me")
    }

    // Using modifier - NO CHANGES
    Box(
        modifier = Modifier
            .glass(
                backgroundColor = OceanTheme.surface,
                glassLevel = GlassLevel.HEAVY
            )
    ) {
        // Content
    }

    // Using preset - NO CHANGES
    Box(
        modifier = Modifier.then(OceanGlass.card())
    ) {
        // Content
    }
}
```

**Result:** Zero code changes required!

---

## Migration Process

### Phase 1: Update Imports (Internal Only)

```kotlin
// GlassmorphicComponents.kt - UPDATE ONCE

// Before (Current)
import androidx.compose.material3.*

// After (Future)
import com.magicui.compose.components.*
```

### Phase 2: Delegate to MagicUI

```kotlin
// GlassmorphicComponents.kt - UPDATE ONCE

// Before (Current)
@Composable
fun GlassSurface(
    onClick: (() -> Unit)? = null,
    // ... params
) {
    // Current implementation using Material3
    Surface(/* ... */) {
        content()
    }
}

// After (Future)
@Composable
fun GlassSurface(
    onClick: (() -> Unit)? = null,
    // ... params (SAME)
) {
    // Delegate to MagicUI - ZERO app code changes
    MagicUI.Surface(
        onClick = onClick,
        // ... pass through all params
    ) {
        content()
    }
}
```

### Phase 3: Verify & Test

1. Run full test suite
2. Visual regression tests
3. Performance benchmarks
4. Cross-platform validation (Android/iOS/Web)

---

## For New Development

### Always Use Ocean Components

```kotlin
// ✅ CORRECT
import com.augmentalis.Avanues.web.universal.presentation.ui.components.*

GlassCard(glass = true) {
    Text("Content")
}

// ❌ WRONG - Don't use Material3 directly
import androidx.compose.material3.*

Card {
    Text("Content")
}
```

### Always Use Ocean Modifiers

```kotlin
// ✅ CORRECT
Box(modifier = Modifier.glass(OceanTheme.surface))

// ❌ WRONG - Don't create custom glass effects
Box(
    modifier = Modifier
        .background(Color.Black.copy(alpha = 0.1f))
        .blur(12.dp)
)
```

### Always Use Ocean Shapes/Gradients

```kotlin
// ✅ CORRECT
GlassCard(shape = GlassShapes.large)

Box(modifier = Modifier.background(OceanGradients.primaryGradient))

// ❌ WRONG
Card(shape = RoundedCornerShape(16.dp))

Box(modifier = Modifier.background(Brush.verticalGradient(/* custom */)))
```

---

## Migration Checklist

### Before Starting New Feature

- [ ] Import `GlassmorphicComponents` and `OceanThemeExtensions`
- [ ] Use `GlassSurface`, `GlassCard`, `OceanButton`, etc.
- [ ] Apply glass modifiers via `Modifier.glass()` or presets
- [ ] Use `GlassShapes` for all shapes
- [ ] Use `OceanGradients` for all gradients
- [ ] Use `GlassDefaults` for spacing/sizing

### When Migrating to MagicUI

- [ ] Update `GlassmorphicComponents.kt` imports
- [ ] Delegate all component implementations to MagicUI
- [ ] Update `OceanThemeExtensions.kt` modifiers to call MagicUI modifiers
- [ ] Run full test suite
- [ ] Verify visual consistency
- [ ] Test on all platforms (Android, iOS, Web)

---

## FAQ

### Q: Can I mix Ocean and Material3 components?

**A:** No. Always use Ocean components. This ensures zero changes during MagicUI migration.

### Q: What if I need a component not in Ocean library?

**A:** Add it to `GlassmorphicComponents.kt` following the same 1:1 mapping pattern.

### Q: Can I customize glass effects?

**A:** Yes, use the `glass()` modifier with custom parameters. Avoid creating custom glass implementations.

### Q: When will MagicUI migration happen?

**A:** TBD. The abstraction layer is ready now, allowing development to continue without blocking on MagicUI.

### Q: Will performance change with MagicUI?

**A:** MagicUI is optimized for cross-platform performance and should match or exceed current performance.

### Q: What about existing Material3 code?

**A:** Migrate gradually. New features must use Ocean components. Existing code can be migrated incrementally.

---

## Examples

### Chat Bubble

```kotlin
// Current (Ocean)
GlassBubble(
    align = BubbleAlign.START,
    glassLevel = GlassLevel.MEDIUM
) {
    Text("Hello!")
}

// Future (MagicUI) - SAME CODE
GlassBubble(
    align = BubbleAlign.START,
    glassLevel = GlassLevel.MEDIUM
) {
    Text("Hello!")
}
```

### FAB with Glass Effect

```kotlin
// Current (Ocean)
GlassFloatingActionButton(
    onClick = { /* ... */ },
    glassLevel = GlassLevel.HEAVY
) {
    Icon(Icons.Default.Add, "Add")
}

// Future (MagicUI) - SAME CODE
GlassFloatingActionButton(
    onClick = { /* ... */ },
    glassLevel = GlassLevel.HEAVY
) {
    Icon(Icons.Default.Add, "Add")
}
```

### Complex Layout

```kotlin
@Composable
fun FavoritesScreen() {
    Column {
        // Title with heavy glass
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .oceanPadding(),
            glassLevel = GlassLevel.HEAVY
        ) {
            Text("Favorites", style = MaterialTheme.typography.headlineMedium)
        }

        // Cards in grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.oceanPadding()
        ) {
            items(favorites) { favorite ->
                GlassCard(
                    onClick = { /* ... */ },
                    glass = true,
                    glassLevel = GlassLevel.MEDIUM,
                    modifier = Modifier.aspectRatio(1f)
                ) {
                    // Card content
                    Text(favorite.title)
                }
            }
        }

        // FAB
        GlassFloatingActionButton(
            onClick = { /* ... */ },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, "Add")
        }
    }
}

// This ENTIRE screen works as-is when migrating to MagicUI!
```

---

## Summary

The Ocean component architecture provides:

1. **Current**: Full-featured glassmorphic UI with Material3
2. **Future**: Seamless MagicUI migration with zero app code changes
3. **Always**: Type-safe, consistent API across the entire app

**Golden Rule:** Always use Ocean components, never Material3 directly.

---

## Contact

For questions or issues:
- **Documentation:** `docs/webavanue/`
- **Components:** `common/webavanue/universal/presentation/ui/components/`
- **Examples:** See existing screens (BrowserScreen, TabSwitcherView, etc.)
