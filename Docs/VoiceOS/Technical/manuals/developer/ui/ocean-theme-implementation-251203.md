# Ocean Theme & Glassmorphic UI Implementation Guide

**Feature:** Ocean Theme Glassmorphic Design System
**Version:** 1.0
**Last Updated:** 2025-12-03
**Commit:** 655a2b19
**Status:** ✅ Implemented

---

## Overview

The Ocean Theme is VoiceOS's custom glassmorphic design system featuring blue/teal gradients, transparent glass-like effects, and smooth animations. It provides a modern, premium visual experience while maintaining accessibility and usability.

**Visual Identity:**
- **Color Palette:** Blues, teals, and ocean-inspired hues
- **Material:** Glass-like transparency with backdrop blur
- **Depth:** Layered shadows and gradient overlays
- **Animation:** Smooth transitions and pulse effects

---

## Design Philosophy

### Glassmorphism
- Transparent/translucent backgrounds (60-90% opacity)
- Backdrop blur effects for depth
- Subtle borders for definition
- Light passing through layers

### Ocean Color Palette
- **Primary:** Deep ocean blue (#0A4F7D)
- **Secondary:** Teal/aqua (#00BCD4)
- **Accent:** Light cyan (#80DEEA)
- **Glass:** White/blue with transparency

### Material Design 3 Integration
- Built on top of Material 3 components
- Compatible with Material theming system
- Extends rather than replaces Material Design

---

## Core Components

### 1. GlassCard

Glassmorphic container with backdrop blur and gradient overlay.

**File:** `ui/theme/GlassmorphicComponents.kt`

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.1f), // 10% opacity
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
                .blur(16.dp) // Backdrop blur
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
}
```

**Usage:**
```kotlin
GlassCard(
    modifier = Modifier.fillMaxWidth()
) {
    Text("Content goes here")
}
```

---

### 2. Ocean Gradients

Pre-defined gradient brushes for consistent theming.

**File:** `ui/theme/OceanThemeExtensions.kt`

```kotlin
object OceanGradients {
    // Primary gradient (deep blue to teal)
    val Primary = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0A4F7D), // Deep ocean blue
            Color(0xFF00BCD4)  // Teal
        )
    )

    // Teal gradient (light to dark teal)
    val Teal = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF80DEEA), // Light cyan
            Color(0xFF00BCD4)  // Teal
        )
    )

    // Glass overlay (transparent white)
    val GlassOverlay = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.2f),
            Color.White.copy(alpha = 0.05f)
        )
    )
}
```

**Usage:**
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(OceanGradients.Teal)
)
```

---

### 3. Ocean Colors

Semantic color definitions for text, backgrounds, and accents.

**File:** `ui/theme/OceanThemeExtensions.kt`

```kotlin
object OceanColors {
    // Text colors
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.7f)
    val TextOnGradient = Color.White

    // Background colors
    val GlassLight = Color.White.copy(alpha = 0.1f)
    val GlassMedium = Color.White.copy(alpha = 0.15f)
    val GlassDark = Color.White.copy(alpha = 0.2f)

    // Accent colors
    val OceanBlue = Color(0xFF0A4F7D)
    val OceanTeal = Color(0xFF00BCD4)
    val OceanCyan = Color(0xFF80DEEA)
}
```

---

### 4. Ocean Button

Styled button with gradient background and pulse effect.

**File:** `ui/theme/GlassmorphicComponents.kt`

```kotlin
@Composable
fun OceanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String
) {
    val scale = remember { mutableStateOf(1f) }

    Button(
        onClick = {
            scale.value = 0.95f
            onClick()
        },
        modifier = modifier
            .scale(scale.value)
            .background(
                brush = OceanGradients.Primary,
                shape = RoundedCornerShape(12.dp)
            ),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        )
    ) {
        Text(
            text = text,
            color = OceanColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }

    LaunchedEffect(scale.value) {
        if (scale.value < 1f) {
            delay(100)
            scale.value = 1f
        }
    }
}
```

---

### 5. Ocean TextField

Themed text input field with glass background.

**File:** `ui/theme/GlassmorphicComponents.kt`

```kotlin
@Composable
fun OceanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .background(
                color = OceanColors.GlassLight,
                shape = RoundedCornerShape(12.dp)
            ),
        label = label?.let { { Text(it, color = OceanColors.TextSecondary) } },
        placeholder = placeholder?.let { { Text(it, color = OceanColors.TextSecondary) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OceanColors.TextPrimary,
            unfocusedTextColor = OceanColors.TextPrimary,
            focusedBorderColor = OceanColors.OceanCyan,
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
```

---

## Implementation Example: CommandAssignmentDialog

The Ocean Theme was first applied to the CommandAssignmentDialog as a pilot implementation.

### Before (Material 3 Default)
```kotlin
Dialog(onDismissRequest = onDismiss) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Assign Voice Command", style = MaterialTheme.typography.headlineSmall)
            // Content...
        }
    }
}
```

### After (Ocean Theme)
```kotlin
Dialog(onDismissRequest = onDismiss) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OceanGradients.Teal)
                .padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            Column {
                Text(
                    text = "Assign Voice Command",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OceanColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ocean Theme Glassmorphic Design",
                    style = MaterialTheme.typography.bodySmall,
                    color = OceanColors.TextSecondary
                )
            }
        }

        // Content with glass background
        Column(modifier = Modifier.padding(20.dp)) {
            // Content...
        }
    }
}
```

---

## MagicUI Migration Path

The Ocean Theme is designed for easy migration to MagicUI when available.

### Component Mapping (1:1)

| Ocean Component | MagicUI Component | Migration |
|----------------|-------------------|-----------|
| `GlassCard` | `MagicCard` | Find/replace |
| `OceanButton` | `MagicButton` | Find/replace |
| `OceanTextField` | `MagicTextField` | Find/replace |
| `OceanGradients` | `MagicGradients` | Find/replace |
| `OceanColors` | `MagicColors` | Find/replace |

### Migration Script (Future)
```bash
# When MagicUI is ready:
find . -name "*.kt" -exec sed -i '' 's/GlassCard/MagicCard/g' {} \;
find . -name "*.kt" -exec sed -i '' 's/OceanButton/MagicButton/g' {} \;
find . -name "*.kt" -exec sed -i '' 's/OceanTextField/MagicTextField/g' {} \;
```

---

## Best Practices

### 1. Use Semantic Colors
```kotlin
// Good
Text(color = OceanColors.TextPrimary)

// Avoid
Text(color = Color.White)
```

### 2. Consistent Gradients
```kotlin
// Good - Use pre-defined gradients
Box(modifier = Modifier.background(OceanGradients.Teal))

// Avoid - Custom gradients everywhere
Box(modifier = Modifier.background(
    Brush.verticalGradient(listOf(Color.Blue, Color.Cyan))
))
```

### 3. Glass Effect Hierarchy
```kotlin
// Light glass for containers
GlassCard(...) // alpha 0.1f

// Medium glass for elevated elements
Surface(color = OceanColors.GlassMedium) // alpha 0.15f

// Dark glass for overlays
Surface(color = OceanColors.GlassDark) // alpha 0.2f
```

### 4. Accessible Contrast
- Always test text readability on gradients
- Use TextPrimary (white) on dark gradients
- Use TextSecondary (70% white) for less important text
- Minimum contrast ratio: 4.5:1 (WCAG AA)

---

## Performance Considerations

### Blur Effects
- Backdrop blur is GPU-intensive
- Limit to 2-3 blurred surfaces on screen
- Consider disabling blur on low-end devices

```kotlin
@Composable
fun AdaptiveGlassCard(content: @Composable () -> Unit) {
    val isLowEndDevice = LocalContext.current.resources.configuration.densityDpi < 240

    if (isLowEndDevice) {
        // Simple card without blur
        Surface(...) { content() }
    } else {
        // Full glassmorphic effect
        GlassCard { content() }
    }
}
```

### Gradient Rendering
- Gradients are fast on modern GPUs
- Avoid animating gradient colors (use opacity instead)
- Cache gradient brushes in `remember {}`

---

## Testing

### Visual Regression Tests
- Capture screenshots of Ocean components
- Compare against reference images
- Test on light/dark system themes

### Accessibility Tests
- Verify contrast ratios (use Android Accessibility Scanner)
- Test with TalkBack enabled
- Ensure blur doesn't obscure important content

### Performance Tests
- Monitor frame rates with Perfetto
- Test on RealWear HMT-1 (low-end device)
- Verify smooth 60fps scrolling

---

## File Structure

```
modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/ui/theme/
├── GlassmorphicComponents.kt  # GlassCard, OceanButton, OceanTextField
├── OceanThemeExtensions.kt    # OceanGradients, OceanColors
└── Theme.kt                    # Main Material 3 theme (unchanged)
```

---

## Future Enhancements

### Q1 2026
- [ ] Ocean theme for all VoiceOS dialogs
- [ ] Dark mode variant (darker blues)
- [ ] Animation library (pulse, wave, ripple effects)
- [ ] Ocean theme documentation website

### Q2 2026
- [ ] MagicUI integration (replace Ocean components)
- [ ] Custom Material 3 theme based on Ocean colors
- [ ] Ocean theme design tokens (Figma)
- [ ] Component showcase app

---

## Related Documentation

- [CommandAssignmentDialog Implementation](/docs/manuals/developer/features/manual-command-assignment-implementation-251203.md)
- [Material Design 3 Guidelines](/docs/manuals/developer/ui/material-design-3.md)
- [Compose Best Practices](/docs/manuals/developer/ui/compose-best-practices.md)

---

## Support

**Issues:** Report glassmorphic rendering issues to #voiceos-ui-team
**Design System:** Ocean theme design system in Figma (coming soon)
**MagicUI:** Track migration status at https://magicui.dev/voiceos

---

**Version:** 1.0
**Last Updated:** 2025-12-03
**Build Status:** ✅ BUILD SUCCESSFUL
**Feature:** Ocean Theme Glassmorphic UI
**Commit:** 655a2b19

**License:** Proprietary - Augmentalis ES
**Copyright:** © 2025 Augmentalis ES. All rights reserved.
