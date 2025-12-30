# ColorUtils Architecture - SOLID Refactoring

## Before (ISP Violation)

```
┌─────────────────────────────────────────────────────┐
│              ColorUtils (Monolithic)                │
│                                                     │
│  • lighten()          • contrastRatio()             │
│  • darken()           • meetsWcagAA()               │
│  • saturate()         • meetsWcagAAA()              │
│  • withAlpha()        • shiftHue()                  │
│  • mix()              • complementary()             │
│  • invert()           • triadic()                   │
│  • grayscale()        • analogous()                 │
│  • contrastingForeground()                          │
│                                                     │
│  Problem: 15+ functions, mixed responsibilities    │
└─────────────────────────────────────────────────────┘
```

## After (SOLID - Interface Segregation)

```
┌───────────────────────────────────────────────────────────────────┐
│                     ColorUtils (Facade)                           │
│                                                                   │
│  Delegates to focused objects while maintaining compatibility    │
│  • Contains UniversalColor data class                            │
│  • Contains HSL conversion helpers (rgbToHsl, hslToRgb)          │
│  • Provides all extension functions                              │
└───────────────────────────────────────────────────────────────────┘
         │                      │                        │
         ▼                      ▼                        ▼
┌──────────────────┐  ┌───────────────────┐  ┌──────────────────┐
│ ColorManipulator │  │ ColorAccessibility│  │   ColorTheory    │
│                  │  │                   │  │                  │
│ • lighten()      │  │ • contrastingFg() │  │ • shiftHue()     │
│ • darken()       │  │ • contrastRatio() │  │ • complementary()│
│ • saturate()     │  │ • meetsWcagAA()   │  │ • triadic()      │
│ • withAlpha()    │  │ • meetsWcagAAA()  │  │ • analogous()    │
│ • mix()          │  │                   │  │                  │
│ • invert()       │  │                   │  │                  │
│ • grayscale()    │  │                   │  │                  │
│                  │  │                   │  │                  │
│ Responsibility:  │  │ Responsibility:   │  │ Responsibility:  │
│ Color            │  │ WCAG & Contrast   │  │ Color Harmonies  │
│ Transformations  │  │ Accessibility     │  │ & Hue Operations │
└──────────────────┘  └───────────────────┘  └──────────────────┘
```

## Usage Patterns

### Pattern 1: Backward Compatible (via Facade)
```kotlin
import com.augmentalis.avaelements.common.color.ColorUtils

val lighter = ColorUtils.lighten(color, 0.3f)
val ratio = ColorUtils.contrastRatio(fg, bg)
val complement = ColorUtils.complementary(color)
```

### Pattern 2: Extension Functions (via Facade)
```kotlin
import com.augmentalis.avaelements.common.color.*

val lighter = color.lighten(0.3f)
val ratio = color.contrastRatio(otherColor)
val complement = color.complementary()
```

### Pattern 3: Focused Objects (NEW - Better ISP)
```kotlin
// Only import what you need
import com.augmentalis.avaelements.common.color.ColorAccessibility

fun validateContrast(fg: UniversalColor, bg: UniversalColor) {
    // No dependency on manipulation or color theory
    val meetsAA = ColorAccessibility.meetsWcagAA(fg, bg)
}
```

### Pattern 4: Mixed Usage
```kotlin
import com.augmentalis.avaelements.common.color.ColorTheory
import com.augmentalis.avaelements.common.color.ColorManipulator

fun generatePalette(base: UniversalColor) {
    // Get harmonies
    val triadic = ColorTheory.triadic(base)

    // Create variants
    val palette = triadic.map { color ->
        Palette(
            base = color,
            light = ColorManipulator.lighten(color, 0.2f),
            dark = ColorManipulator.darken(color, 0.2f)
        )
    }
}
```

## Dependency Graph

### Old (Everything depends on everything)
```
┌─────────────┐
│ Client Code │───────────────┐
└─────────────┘               │
                              ▼
                    ┌──────────────────┐
                    │   ColorUtils     │
                    │  (15+ functions) │
                    └──────────────────┘
                              │
                    Depends on everything
```

### New (Focused dependencies)
```
┌──────────────────┐        ┌───────────────────┐        ┌──────────────┐
│ Accessibility    │        │ Theme Generator   │        │ UI Component │
│ Validator        │        │                   │        │              │
└────────┬─────────┘        └─────────┬─────────┘        └──────┬───────┘
         │                            │                          │
         │ Only needs                 │ Only needs               │ Needs all
         │ accessibility              │ theory + manipulation    │ (uses facade)
         │                            │                          │
         ▼                            ▼                          ▼
┌──────────────────┐        ┌───────────────────┐        ┌──────────────┐
│ColorAccessibility│        │  ColorTheory      │        │  ColorUtils  │
│                  │        │  ColorManipulator │        │   (Facade)   │
└──────────────────┘        └───────────────────┘        └──────────────┘
```

## File Structure

```
com.augmentalis.avaelements.common.color/
├── UniversalColor (in ColorUtils.kt)
│   ├── Data class (alpha, red, green, blue)
│   ├── Companion factory methods (fromHex, fromArgb, fromRgb, fromHsl)
│   ├── Conversion methods (toArgb, toHex, toHsl)
│   └── Properties (luminance, isDark, isLight)
│
├── ColorManipulator.kt
│   └── object ColorManipulator
│       ├── lighten(color, factor) → UniversalColor
│       ├── darken(color, factor) → UniversalColor
│       ├── saturate(color, factor) → UniversalColor
│       ├── withAlpha(color, alpha) → UniversalColor
│       ├── mix(color1, color2, ratio) → UniversalColor
│       ├── invert(color) → UniversalColor
│       └── grayscale(color) → UniversalColor
│
├── ColorAccessibility.kt
│   └── object ColorAccessibility
│       ├── contrastingForeground(background) → UniversalColor
│       ├── contrastRatio(color1, color2) → Float
│       ├── meetsWcagAA(foreground, background) → Boolean
│       └── meetsWcagAAA(foreground, background) → Boolean
│
├── ColorTheory.kt
│   └── object ColorTheory
│       ├── shiftHue(color, degrees) → UniversalColor
│       ├── complementary(color) → UniversalColor
│       ├── triadic(color) → List<UniversalColor>
│       └── analogous(color) → List<UniversalColor>
│
└── ColorUtils.kt
    ├── object ColorUtils (Facade)
    │   ├── [All functions delegate to focused objects]
    │   └── [Maintains backward compatibility]
    │
    ├── HSL Helpers (internal)
    │   ├── rgbToHsl(r, g, b) → Triple<Float, Float, Float>
    │   └── hslToRgb(h, s, l) → Triple<Float, Float, Float>
    │
    └── Extension Functions
        ├── UniversalColor.lighten(factor)
        ├── UniversalColor.darken(factor)
        ├── UniversalColor.saturate(factor)
        ├── UniversalColor.withAlpha(alpha)
        ├── UniversalColor.mix(other, ratio)
        ├── UniversalColor.contrastingForeground()
        ├── UniversalColor.invert()
        ├── UniversalColor.grayscale()
        ├── UniversalColor.shiftHue(degrees)
        └── UniversalColor.complementary()
```

## Benefits Summary

| Aspect | Before | After |
|--------|--------|-------|
| **Lines per file** | 355 (monolithic) | 86-115 (focused) |
| **Responsibilities** | Mixed (manipulation + accessibility + theory) | Separated (1 per object) |
| **Dependencies** | Client needs all 15+ functions | Client imports only what it needs |
| **Testability** | Test all together | Test each concern independently |
| **Maintainability** | Changes affect all clients | Changes isolated to specific concern |
| **Discoverability** | Hard to find related functions | Grouped by responsibility |
| **ISP Compliance** | ❌ Violated | ✅ Adhered |
| **SRP Compliance** | ❌ Violated | ✅ Adhered |
| **Backward Compat** | N/A | ✅ 100% compatible |

## Real-World Example

### Accessibility Validator (Only needs ColorAccessibility)

```kotlin
import com.augmentalis.avaelements.common.color.ColorAccessibility
import com.augmentalis.avaelements.common.color.UniversalColor

class AccessibilityValidator {
    fun validateTextContrast(
        foreground: UniversalColor,
        background: UniversalColor,
        largeText: Boolean = false
    ): ValidationResult {
        val ratio = ColorAccessibility.contrastRatio(foreground, background)
        val threshold = if (largeText) 3.0f else 4.5f

        return when {
            ratio >= 7.0f -> ValidationResult.AAA
            ratio >= threshold -> ValidationResult.AA
            else -> ValidationResult.FAIL
        }
    }

    // No dependency on ColorTheory or ColorManipulator!
}
```

### Theme Generator (Needs ColorTheory + ColorManipulator)

```kotlin
import com.augmentalis.avaelements.common.color.ColorTheory
import com.augmentalis.avaelements.common.color.ColorManipulator
import com.augmentalis.avaelements.common.color.UniversalColor

class ThemeGenerator {
    fun generateMaterialTheme(primaryColor: UniversalColor): MaterialTheme {
        // Generate color harmonies
        val accent = ColorTheory.complementary(primaryColor)

        // Generate variants
        return MaterialTheme(
            primary = primaryColor,
            primaryLight = ColorManipulator.lighten(primaryColor, 0.2f),
            primaryDark = ColorManipulator.darken(primaryColor, 0.2f),
            accent = accent,
            accentLight = ColorManipulator.lighten(accent, 0.2f),
            accentDark = ColorManipulator.darken(accent, 0.2f)
        )
    }

    // No dependency on ColorAccessibility!
}
```

## Migration Impact

| Component | Status | Notes |
|-----------|--------|-------|
| Android Renderer | ✅ No changes needed | Uses ColorUtils facade |
| iOS Renderer | ✅ No changes needed | Uses ColorUtils facade |
| Desktop Renderer | ✅ No changes needed | Uses ColorUtils facade |
| Extension Functions | ✅ Preserved | All work via facade |
| Existing Code | ✅ 100% compatible | Zero breaking changes |
| New Code | ✅ Can use focused objects | Better ISP compliance |

---

**Refactoring Date:** November 26, 2025
**Status:** ✅ Complete and Tested
**Breaking Changes:** None
**SOLID Principles Applied:** Interface Segregation Principle (ISP), Single Responsibility Principle (SRP)
