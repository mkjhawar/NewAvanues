# AVA Theme System Guide

**Created:** 2025-11-04
**Author:** AVA AI Team
**Version:** 1.0
**Purpose:** Complete guide to AVA's centralized theme system

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Quick Start](#quick-start)
4. [Theme Components](#theme-components)
5. [Customization Guide](#customization-guide)
6. [Migration Guide](#migration-guide)
7. [Best Practices](#best-practices)
8. [Examples](#examples)

---

## Overview

AVA's theme system provides a centralized, single-source-of-truth for all UI styling including colors, typography, spacing, shapes, and responsive breakpoints. The glassmorphic design is inspired by AVAConnect, featuring:

- **Purple gradient backgrounds** (#667eea → #764ba2)
- **Teal accent color** (#03DAC6)
- **Semi-transparent glass panels** with backdrop blur
- **Responsive design** with Material 3 breakpoints
- **Dark mode support** out of the box

### Key Benefits

✅ **Single Source of Truth** - Change one value to update the entire app
✅ **Type Safety** - All values are strongly typed
✅ **Consistency** - Enforces design system compliance
✅ **Maintainability** - Easy to update and extend
✅ **Documentation** - Self-documenting code with inline comments

---

## Architecture

```
apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/theme/
├── Color.kt          # AvaColors - All color definitions
├── Dimensions.kt     # AvaDimensions - Spacing, sizing, breakpoints
├── Shape.kt          # AvaShapes - Corner radii and shapes
├── Type.kt           # Typography - Text styles (Material 3)
└── Theme.kt          # AvaTheme - Main theme composable
```

### Theme Objects

| Object | Purpose | Examples |
|--------|---------|----------|
| `AvaColors` | Color palette | `AvaColors.AccentPrimary`, `AvaColors.GlassLight` |
| `AvaDimensions` | Spacing & sizing | `AvaDimensions.SpaceMd`, `AvaDimensions.IconLg` |
| `AvaShapes` | Corner radii | `AvaShapes.Medium`, `AvaShapes.ChatBubble.User` |
| `Typography` | Text styles | `Typography.bodyLarge`, `Typography.headlineMedium` |

---

## Quick Start

### Using the Theme

Wrap your app with `AvaTheme`:

```kotlin
@Composable
fun MyApp() {
    AvaTheme {
        // Your app content
        MainScreen()
    }
}
```

### Accessing Theme Values

```kotlin
@Composable
fun MyComposable() {
    // Colors from MaterialTheme (uses AvaColors)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Or access directly
    val accentColor = AvaColors.AccentPrimary
    val glassPanel = AvaColors.GlassMedium

    // Dimensions
    val spacing = AvaDimensions.SpaceMd
    val cornerRadius = AvaDimensions.CornerLg

    // Shapes
    val cardShape = AvaShapes.Medium
    val chatBubble = AvaShapes.ChatBubble.User

    // Typography
    val headlineStyle = MaterialTheme.typography.headlineMedium
}
```

---

## Theme Components

### 1. Colors (`AvaColors`)

#### Primary Brand Colors

```kotlin
AvaColors.GradientStart      // #667EEA - Light purple-blue
AvaColors.GradientEnd        // #764BA2 - Deep purple
AvaColors.GradientMid        // #6E69C7 - Interpolated middle
AvaColors.AccentPrimary      // #03DAC6 - Bright teal
AvaColors.AccentDark         // #018786 - Darker teal
AvaColors.AccentLight        // #64FFDA - Lighter teal
```

#### Glassmorphism

```kotlin
AvaColors.GlassUltraLight    // 5% white - Subtle overlay
AvaColors.GlassLight         // 10% white - Light panels
AvaColors.GlassMedium        // 15% white - Standard panels
AvaColors.GlassHeavy         // 20% white - Emphasized panels
AvaColors.GlassDense         // 30% white - Dense overlays
```

#### Text Colors

```kotlin
AvaColors.TextPrimary        // 100% white - Primary text
AvaColors.TextSecondary      // 90% white - Secondary text
AvaColors.TextTertiary       // 70% white - Tertiary text
AvaColors.TextDisabled       // 38% white - Disabled text
AvaColors.TextOnAccent       // Black - Text on teal buttons
```

#### Semantic Colors

```kotlin
AvaColors.Success            // #10B981 - Green
AvaColors.Warning            // #F59E0B - Amber
AvaColors.Error              // #EF4444 - Red
AvaColors.Info               // #3B82F6 - Blue
```

### 2. Dimensions (`AvaDimensions`)

#### Spacing Scale (8dp grid)

```kotlin
AvaDimensions.SpaceNone      // 0dp
AvaDimensions.SpaceXs        // 4dp
AvaDimensions.SpaceSm        // 8dp
AvaDimensions.SpaceMd        // 16dp (base unit)
AvaDimensions.SpaceLg        // 24dp
AvaDimensions.SpaceXl        // 32dp
AvaDimensions.SpaceXxl       // 48dp
```

#### Component Sizes

```kotlin
// Icons
AvaDimensions.IconSm         // 16dp
AvaDimensions.IconMd         // 24dp
AvaDimensions.IconLg         // 32dp

// Buttons
AvaDimensions.ButtonHeightMd // 48dp
AvaDimensions.ButtonMinWidthMd // 88dp

// App bars
AvaDimensions.AppBarHeight   // 56dp
AvaDimensions.BottomNavHeight // 56dp
```

#### Borders & Corners

```kotlin
AvaDimensions.BorderDefault  // 1dp
AvaDimensions.BorderThick    // 2dp

AvaDimensions.CornerSm       // 8dp
AvaDimensions.CornerMd       // 12dp
AvaDimensions.CornerLg       // 16dp
AvaDimensions.CornerXl       // 20dp
AvaDimensions.CornerRound    // 999dp (fully rounded)
```

#### Responsive Breakpoints

```kotlin
AvaDimensions.CompactMaxWidth   // 600dp - Phones
AvaDimensions.MediumMinWidth    // 600dp - Tablets
AvaDimensions.ExpandedMinWidth  // 840dp - Desktops
```

### 3. Shapes (`AvaShapes`)

#### Basic Shapes

```kotlin
AvaShapes.Small              // 8dp corners
AvaShapes.Medium             // 12dp corners
AvaShapes.Large              // 16dp corners
AvaShapes.ExtraLarge         // 20dp corners
AvaShapes.Round              // Fully rounded (pill)
```

#### Asymmetric Shapes

```kotlin
AvaShapes.TopRounded         // Rounded top only
AvaShapes.BottomRounded      // Rounded bottom only
AvaShapes.LeftRounded        // Rounded left only
AvaShapes.RightRounded       // Rounded right only
```

#### Special Shapes

```kotlin
AvaShapes.ChatBubble.User       // User message bubble
AvaShapes.ChatBubble.Assistant  // Assistant message bubble
AvaShapes.GlassPanel.Medium     // Glass panel (16dp corners)
```

### 4. Typography

Defined in `Type.kt`, follows Material 3 type scale:

```kotlin
Typography.displayLarge      // 57sp - Hero text
Typography.headlineLarge     // 32sp - Page titles
Typography.headlineMedium    // 28sp - Section headers
Typography.titleLarge        // 22sp - Card titles
Typography.bodyLarge         // 16sp - Primary body text
Typography.bodyMedium        // 14sp - Secondary body text
Typography.labelLarge        // 14sp - Button labels
```

---

## Customization Guide

###  Changing the Color Scheme

**To change ALL colors in the app:**

1. Open `apps/ava-standalone/src/main/kotlin/com/augmentalis/ava/ui/theme/Color.kt`
2. Modify the values in `AvaColors`:

```kotlin
object AvaColors {
    // Change these to your brand colors
    val GradientStart = Color(0xFF667EEA)  // Your gradient start
    val GradientEnd = Color(0xFF764BA2)    // Your gradient end
    val AccentPrimary = Color(0xFF03DAC6)  // Your accent color

    // All other colors automatically update throughout the app
}
```

That's it! The entire app updates instantly.

### Changing Spacing

**To adjust spacing across the entire app:**

1. Open `Dimensions.kt`
2. Modify the spacing scale:

```kotlin
object AvaDimensions {
    val SpaceMd = 16.dp  // Change from 16dp to your preferred base unit
    // All other spacing uses this as reference
}
```

### Changing Corner Radii

**To make the app more/less rounded:**

1. Open `Dimensions.kt`
2. Adjust corner values:

```kotlin
object AvaDimensions {
    val CornerMd = 12.dp  // Increase for rounder, decrease for sharper
}
```

### Adding New Colors

```kotlin
object AvaColors {
    // Add your custom color
    val MyCustomColor = Color(0xFFAB12CD)

    // Use it anywhere:
    // color = AvaColors.MyCustomColor
}
```

---

## Migration Guide

### For New Modules

When creating a new feature module, use the centralized theme:

```kotlin
@Composable
fun NewFeatureScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AvaDimensions.SpaceMd)
    ) {
        Text(
            text = "Title",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            shape = AvaShapes.Medium,
            colors = CardDefaults.cardColors(
                containerColor = AvaColors.GlassMedium
            )
        ) {
            // Card content
        }
    }
}
```

### Migrating Existing Code

**Before (hardcoded values):**

```kotlin
Box(
    modifier = Modifier
        .padding(16.dp)
        .background(Color(0x26FFFFFF), RoundedCornerShape(12.dp))
) {
    Text(
        text = "Hello",
        color = Color.White,
        fontSize = 16.sp
    )
}
```

**After (using theme):**

```kotlin
Box(
    modifier = Modifier
        .padding(AvaDimensions.SpaceMd)
        .background(AvaColors.GlassMedium, AvaShapes.Medium)
) {
    Text(
        text = "Hello",
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyLarge
    )
}
```

---

## Best Practices

### ✅ DO

- **Use theme values** instead of hardcoded colors/sizes
- **Access via MaterialTheme** for standard properties
- **Use AvaColors/AvaDimensions** directly for custom needs
- **Follow the 8dp spacing grid** from `AvaDimensions`
- **Use semantic colors** (`Success`, `Error`, `Warning`)
- **Leverage responsive breakpoints** for adaptive layouts

### ❌ DON'T

- **Hardcode colors** - Use `AvaColors` instead
- **Hardcode spacing** - Use `AvaDimensions` instead
- **Create custom shapes** - Use or extend `AvaShapes`
- **Ignore Material 3 guidelines** - The theme follows them
- **Mix color systems** - Stick to the centralized theme

---

## Examples

### Example 1: Glass Panel Card

```kotlin
@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AvaDimensions.SpaceMd),
        shape = AvaShapes.GlassPanel.Medium,
        colors = CardDefaults.cardColors(
            containerColor = AvaColors.GlassMedium,
            contentColor = AvaColors.TextPrimary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = AvaDimensions.ElevationMd
        )
    ) {
        content()
    }
}
```

### Example 2: Chat Message Bubble

```kotlin
@Composable
fun ChatMessage(
    message: String,
    isUser: Boolean
) {
    Box(
        modifier = Modifier
            .widthIn(max = AvaDimensions.ChatBubbleMaxWidth)
            .background(
                color = if (isUser) AvaColors.AccentPrimary else AvaColors.GlassHeavy,
                shape = if (isUser) AvaShapes.ChatBubble.User else AvaShapes.ChatBubble.Assistant
            )
            .padding(AvaDimensions.SpaceMd)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isUser) AvaColors.TextOnAccent else AvaColors.TextPrimary
        )
    }
}
```

### Example 3: Responsive Layout

```kotlin
@Composable
fun ResponsiveScreen() {
    BoxWithConstraints {
        val isCompact = maxWidth < AvaDimensions.MediumMinWidth
        val spacing = if (isCompact) AvaDimensions.SpaceSm else AvaDimensions.SpaceLg

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing)
        ) {
            if (isCompact) {
                // Phone layout
                CompactLayout()
            } else {
                // Tablet/Desktop layout
                ExpandedLayout()
            }
        }
    }
}
```

### Example 4: Gradient Background

```kotlin
@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AvaColors.GradientStart,
                        AvaColors.GradientEnd
                    )
                )
            )
    ) {
        content()
    }
}
```

---

## Summary

The AVA theme system provides:

1. **Centralized configuration** - Single source of truth
2. **Easy customization** - Change values in one place
3. **Type safety** - Compile-time checks
4. **Consistency** - Enforced design system
5. **Responsive design** - Built-in breakpoints
6. **Dark mode** - Automatic support

**To change the entire app's look:**
- Edit `Color.kt` for colors
- Edit `Dimensions.kt` for spacing/sizing
- Edit `Shape.kt` for corner radii

That's it! The changes propagate throughout the entire application automatically.

---

**Questions? Check the code comments in each theme file for detailed documentation.**
