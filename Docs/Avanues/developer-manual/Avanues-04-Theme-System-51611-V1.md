# AvaElements Theme System

**Version:** 2.0.0
**Last Updated:** 2025-11-13
**Platform Coverage:** Android (complete), iOS (planned), Web (planned)

---

## Table of Contents

1. [Theme Architecture](#theme-architecture)
2. [Color Schemes](#color-schemes)
3. [Typography](#typography)
4. [Spacing System](#spacing-system)
5. [Shape System](#shape-system)
6. [Creating Custom Themes](#creating-custom-themes)
7. [Theme Switching](#theme-switching)
8. [Platform-Specific Theming](#platform-specific-theming)
9. [Best Practices](#best-practices)

---

## Theme Architecture

AvaElements uses a **centralized theme system** that works across all platforms (Android, iOS, Web).

### Core Concept

**Define once, render everywhere** - Themes are platform-agnostic data structures that get converted to platform-specific implementations at render time.

```
┌─────────────────────────────────────┐
│   AvaElements Theme               │
│   (Platform-agnostic)               │
│   - ColorScheme                     │
│   - Typography                      │
│   - Spacing                         │
│   - Shapes                          │
└─────────────┬───────────────────────┘
              │
              ├─────────────┬─────────────┬─────────────┐
              ▼             ▼             ▼             ▼
        ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
        │ Android  │  │   iOS    │  │   Web    │  │ Desktop  │
        │ Material3│  │ SwiftUI  │  │ MUI v5   │  │ Compose  │
        └──────────┘  └──────────┘  └──────────┘  └──────────┘
```

---

## Theme Structure

### Complete Theme Interface

```kotlin
data class Theme(
    val colorScheme: ColorScheme,
    val typography: Typography,
    val spacing: Spacing,
    val shapes: Shapes,
    val name: String = "Default Theme",
    val isDark: Boolean = false
)
```

### ColorScheme

```kotlin
data class ColorScheme(
    // Primary colors
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color?,
    val onPrimaryContainer: Color?,

    // Secondary colors
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color?,
    val onSecondaryContainer: Color?,

    // Tertiary colors (optional)
    val tertiary: Color?,
    val onTertiary: Color?,
    val tertiaryContainer: Color?,
    val onTertiaryContainer: Color?,

    // Surface colors
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color?,
    val onSurfaceVariant: Color?,

    // Background colors
    val background: Color,
    val onBackground: Color,

    // Error colors
    val error: Color,
    val onError: Color,
    val errorContainer: Color?,
    val onErrorContainer: Color?,

    // Outline colors
    val outline: Color?,
    val outlineVariant: Color?,

    // Inverse colors (for dark mode)
    val inverseSurface: Color?,
    val inverseOnSurface: Color?,
    val inversePrimary: Color?
)
```

### Color

```kotlin
data class Color(
    val red: Int,    // 0-255
    val green: Int,  // 0-255
    val blue: Int,   // 0-255
    val alpha: Float = 1.0f // 0.0-1.0
) {
    companion object {
        fun fromHex(hex: String): Color
        fun rgb(r: Int, g: Int, b: Int): Color
        fun rgba(r: Int, g: Int, b: Int, a: Float): Color
    }

    fun toHex(): String
}
```

**Example Usage:**

```kotlin
// From hex string
val blue = Color.fromHex("#2196F3")

// From RGB values
val red = Color.rgb(255, 0, 0)

// With alpha
val transparentBlue = Color.rgba(33, 150, 243, 0.5f)
```

---

## Color Schemes

### Material3 Light Theme

```kotlin
val MaterialLightColorScheme = ColorScheme(
    primary = Color.fromHex("#6750A4"),
    onPrimary = Color.fromHex("#FFFFFF"),
    primaryContainer = Color.fromHex("#EADDFF"),
    onPrimaryContainer = Color.fromHex("#21005D"),

    secondary = Color.fromHex("#625B71"),
    onSecondary = Color.fromHex("#FFFFFF"),
    secondaryContainer = Color.fromHex("#E8DEF8"),
    onSecondaryContainer = Color.fromHex("#1D192B"),

    tertiary = Color.fromHex("#7D5260"),
    onTertiary = Color.fromHex("#FFFFFF"),
    tertiaryContainer = Color.fromHex("#FFD8E4"),
    onTertiaryContainer = Color.fromHex("#31111D"),

    surface = Color.fromHex("#FFFBFE"),
    onSurface = Color.fromHex("#1C1B1F"),
    surfaceVariant = Color.fromHex("#E7E0EC"),
    onSurfaceVariant = Color.fromHex("#49454F"),

    background = Color.fromHex("#FFFBFE"),
    onBackground = Color.fromHex("#1C1B1F"),

    error = Color.fromHex("#B3261E"),
    onError = Color.fromHex("#FFFFFF"),
    errorContainer = Color.fromHex("#F9DEDC"),
    onErrorContainer = Color.fromHex("#410E0B"),

    outline = Color.fromHex("#79747E"),
    outlineVariant = Color.fromHex("#CAC4D0"),

    inverseSurface = Color.fromHex("#313033"),
    inverseOnSurface = Color.fromHex("#F4EFF4"),
    inversePrimary = Color.fromHex("#D0BCFF")
)
```

### Material3 Dark Theme

```kotlin
val MaterialDarkColorScheme = ColorScheme(
    primary = Color.fromHex("#D0BCFF"),
    onPrimary = Color.fromHex("#381E72"),
    primaryContainer = Color.fromHex("#4F378B"),
    onPrimaryContainer = Color.fromHex("#EADDFF"),

    secondary = Color.fromHex("#CCC2DC"),
    onSecondary = Color.fromHex("#332D41"),
    secondaryContainer = Color.fromHex("#4A4458"),
    onSecondaryContainer = Color.fromHex("#E8DEF8"),

    tertiary = Color.fromHex("#EFB8C8"),
    onTertiary = Color.fromHex("#492532"),
    tertiaryContainer = Color.fromHex("#633B48"),
    onTertiaryContainer = Color.fromHex("#FFD8E4"),

    surface = Color.fromHex("#1C1B1F"),
    onSurface = Color.fromHex("#E6E1E5"),
    surfaceVariant = Color.fromHex("#49454F"),
    onSurfaceVariant = Color.fromHex("#CAC4D0"),

    background = Color.fromHex("#1C1B1F"),
    onBackground = Color.fromHex("#E6E1E5"),

    error = Color.fromHex("#F2B8B5"),
    onError = Color.fromHex("#601410"),
    errorContainer = Color.fromHex("#8C1D18"),
    onErrorContainer = Color.fromHex("#F9DEDC"),

    outline = Color.fromHex("#938F99"),
    outlineVariant = Color.fromHex("#49454F"),

    inverseSurface = Color.fromHex("#E6E1E5"),
    inverseOnSurface = Color.fromHex("#313033"),
    inversePrimary = Color.fromHex("#6750A4")
)
```

### Custom Brand Colors

```kotlin
val AvanueBlueScheme = ColorScheme(
    primary = Color.fromHex("#2196F3"),      // Avanue Blue
    onPrimary = Color.fromHex("#FFFFFF"),
    primaryContainer = Color.fromHex("#BBDEFB"),
    onPrimaryContainer = Color.fromHex("#0D47A1"),

    secondary = Color.fromHex("#FF9800"),    // Avanue Orange
    onSecondary = Color.fromHex("#000000"),
    secondaryContainer = Color.fromHex("#FFE0B2"),
    onSecondaryContainer = Color.fromHex("#E65100"),

    surface = Color.fromHex("#FFFFFF"),
    onSurface = Color.fromHex("#212121"),
    surfaceVariant = Color.fromHex("#F5F5F5"),
    onSurfaceVariant = Color.fromHex("#757575"),

    background = Color.fromHex("#FAFAFA"),
    onBackground = Color.fromHex("#212121"),

    error = Color.fromHex("#F44336"),
    onError = Color.fromHex("#FFFFFF"),
    errorContainer = Color.fromHex("#FFCDD2"),
    onErrorContainer = Color.fromHex("#B71C1C"),

    outline = Color.fromHex("#9E9E9E"),
    outlineVariant = Color.fromHex("#E0E0E0")
)
```

---

## Typography

### Typography System

```kotlin
data class Typography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,

    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,

    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,

    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,

    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

data class TextStyle(
    val fontSize: Float,         // in SP (scalable pixels)
    val fontWeight: FontWeight,
    val lineHeight: Float?,      // optional line height
    val letterSpacing: Float?    // optional letter spacing
)

enum class FontWeight {
    THIN,        // 100
    EXTRA_LIGHT, // 200
    LIGHT,       // 300
    NORMAL,      // 400
    MEDIUM,      // 500
    SEMI_BOLD,   // 600
    BOLD,        // 700
    EXTRA_BOLD,  // 800
    BLACK        // 900
}
```

### Material3 Typography Scale

```kotlin
val MaterialTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 57f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 64f,
        letterSpacing = -0.25f
    ),
    displayMedium = TextStyle(
        fontSize = 45f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 52f,
        letterSpacing = 0f
    ),
    displaySmall = TextStyle(
        fontSize = 36f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 44f,
        letterSpacing = 0f
    ),

    headlineLarge = TextStyle(
        fontSize = 32f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 40f,
        letterSpacing = 0f
    ),
    headlineMedium = TextStyle(
        fontSize = 28f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 36f,
        letterSpacing = 0f
    ),
    headlineSmall = TextStyle(
        fontSize = 24f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 32f,
        letterSpacing = 0f
    ),

    titleLarge = TextStyle(
        fontSize = 22f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 28f,
        letterSpacing = 0f
    ),
    titleMedium = TextStyle(
        fontSize = 16f,
        fontWeight = FontWeight.MEDIUM,
        lineHeight = 24f,
        letterSpacing = 0.15f
    ),
    titleSmall = TextStyle(
        fontSize = 14f,
        fontWeight = FontWeight.MEDIUM,
        lineHeight = 20f,
        letterSpacing = 0.1f
    ),

    bodyLarge = TextStyle(
        fontSize = 16f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 24f,
        letterSpacing = 0.5f
    ),
    bodyMedium = TextStyle(
        fontSize = 14f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 20f,
        letterSpacing = 0.25f
    ),
    bodySmall = TextStyle(
        fontSize = 12f,
        fontWeight = FontWeight.NORMAL,
        lineHeight = 16f,
        letterSpacing = 0.4f
    ),

    labelLarge = TextStyle(
        fontSize = 14f,
        fontWeight = FontWeight.MEDIUM,
        lineHeight = 20f,
        letterSpacing = 0.1f
    ),
    labelMedium = TextStyle(
        fontSize = 12f,
        fontWeight = FontWeight.MEDIUM,
        lineHeight = 16f,
        letterSpacing = 0.5f
    ),
    labelSmall = TextStyle(
        fontSize = 11f,
        fontWeight = FontWeight.MEDIUM,
        lineHeight = 16f,
        letterSpacing = 0.5f
    )
)
```

---

## Spacing System

### Spacing Scale

```kotlin
data class Spacing(
    val none: Float = 0f,
    val xs: Float = 4f,
    val sm: Float = 8f,
    val md: Float = 16f,
    val lg: Float = 24f,
    val xl: Float = 32f,
    val xxl: Float = 48f,
    val xxxl: Float = 64f
)
```

**Usage:**

```kotlin
val spacing = Spacing()

// Use in layouts
padding = spacing.md  // 16dp
gap = spacing.sm      // 8dp
margin = spacing.lg   // 24dp
```

---

## Shape System

### Shape Definitions

```kotlin
data class Shapes(
    val none: CornerRadius,
    val small: CornerRadius,
    val medium: CornerRadius,
    val large: CornerRadius,
    val extraLarge: CornerRadius,
    val full: CornerRadius
)

data class CornerRadius(
    val topStart: Float,
    val topEnd: Float,
    val bottomEnd: Float,
    val bottomStart: Float
) {
    companion object {
        fun all(radius: Float) = CornerRadius(radius, radius, radius, radius)
        fun top(radius: Float) = CornerRadius(radius, radius, 0f, 0f)
        fun bottom(radius: Float) = CornerRadius(0f, 0f, radius, radius)
    }
}
```

### Material3 Shapes

```kotlin
val MaterialShapes = Shapes(
    none = CornerRadius.all(0f),
    small = CornerRadius.all(4f),
    medium = CornerRadius.all(8f),
    large = CornerRadius.all(16f),
    extraLarge = CornerRadius.all(28f),
    full = CornerRadius.all(9999f)  // Fully rounded
)
```

---

## Creating Custom Themes

### Step 1: Define Your Color Scheme

```kotlin
val MyBrandColors = ColorScheme(
    primary = Color.fromHex("#1976D2"),     // Your brand blue
    onPrimary = Color.fromHex("#FFFFFF"),
    primaryContainer = Color.fromHex("#BBDEFB"),
    onPrimaryContainer = Color.fromHex("#0D47A1"),

    secondary = Color.fromHex("#FFC107"),   // Your brand yellow
    onSecondary = Color.fromHex("#000000"),
    secondaryContainer = Color.fromHex("#FFECB3"),
    onSecondaryContainer = Color.fromHex("#FF6F00"),

    // ... complete the rest
    surface = Color.fromHex("#FFFFFF"),
    onSurface = Color.fromHex("#212121"),
    background = Color.fromHex("#FAFAFA"),
    onBackground = Color.fromHex("#212121"),
    error = Color.fromHex("#D32F2F"),
    onError = Color.fromHex("#FFFFFF")
)
```

### Step 2: Customize Typography (Optional)

```kotlin
val MyBrandTypography = Typography(
    // Use a custom font family or adjust sizes
    displayLarge = TextStyle(
        fontSize = 60f,              // Larger display
        fontWeight = FontWeight.BOLD,
        lineHeight = 68f
    ),
    bodyMedium = TextStyle(
        fontSize = 15f,              // Slightly larger body text
        fontWeight = FontWeight.NORMAL,
        lineHeight = 22f,
        letterSpacing = 0.3f
    ),
    // ... use default MaterialTypography for the rest
)
```

### Step 3: Customize Spacing (Optional)

```kotlin
val MyBrandSpacing = Spacing(
    xs = 6f,   // Slightly larger minimum spacing
    sm = 12f,
    md = 20f,
    lg = 28f,
    xl = 40f,
    xxl = 56f,
    xxxl = 72f
)
```

### Step 4: Create the Theme

```kotlin
val MyBrandTheme = Theme(
    colorScheme = MyBrandColors,
    typography = MyBrandTypography,
    spacing = MyBrandSpacing,
    shapes = MaterialShapes,  // Use Material shapes
    name = "My Brand Theme",
    isDark = false
)
```

### Step 5: Register the Theme

```kotlin
// In your app initialization
ThemeProvider.registerTheme("my-brand", MyBrandTheme)
ThemeProvider.setCurrentTheme("my-brand")
```

---

## Theme Switching

### Runtime Theme Switching

```kotlin
// Get current theme
val currentTheme = ThemeProvider.getCurrentTheme()

// Switch to dark mode
ThemeProvider.setCurrentTheme("material-dark")

// Switch to custom theme
ThemeProvider.setCurrentTheme("my-brand")

// Toggle dark mode
fun toggleDarkMode() {
    val isDark = ThemeProvider.getCurrentTheme().isDark
    if (isDark) {
        ThemeProvider.setCurrentTheme("material-light")
    } else {
        ThemeProvider.setCurrentTheme("material-dark")
    }
}
```

### Reactive Theme Updates

```kotlin
// Listen for theme changes (Android example)
@Composable
fun ThemedApp() {
    var theme by remember { mutableStateOf(ThemeProvider.getCurrentTheme()) }

    // Update when theme changes
    LaunchedEffect(Unit) {
        ThemeProvider.onThemeChange { newTheme ->
            theme = newTheme
        }
    }

    // Use the theme
    val renderer = ComposeRenderer(theme)
    // ... render your app
}
```

---

## Platform-Specific Theming

### Android (Material3)

```kotlin
@Composable
fun RenderButton(c: Button, theme: Theme) {
    androidx.compose.material3.Button(
        onClick = c.onClick ?: {},
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.colorScheme.primary.toCompose(),
            contentColor = theme.colorScheme.onPrimary.toCompose()
        )
    ) {
        Text(text = c.text)
    }
}

// Color conversion extension
fun Color.toCompose(): androidx.compose.ui.graphics.Color {
    return androidx.compose.ui.graphics.Color(
        red = red / 255f,
        green = green / 255f,
        blue = blue / 255f,
        alpha = alpha
    )
}
```

### iOS (SwiftUI) - Planned

```swift
struct RenderButton: View {
    let component: Button
    let theme: Theme

    var body: some View {
        SwiftUI.Button(action: component.onClick ?? {}) {
            Text(component.text)
                .foregroundColor(theme.colorScheme.onPrimary.toSwiftUI())
        }
        .background(theme.colorScheme.primary.toSwiftUI())
    }
}

// Color conversion
extension Color {
    func toSwiftUI() -> SwiftUI.Color {
        return SwiftUI.Color(
            .sRGB,
            red: Double(red) / 255.0,
            green: Double(green) / 255.0,
            blue: Double(blue) / 255.0,
            opacity: Double(alpha)
        )
    }
}
```

### Web (React + Material-UI) - Planned

```typescript
function RenderButton({ component, theme }: { component: Button, theme: Theme }) {
    return (
        <MuiButton
            onClick={component.onClick}
            sx={{
                backgroundColor: theme.colorScheme.primary.toHex(),
                color: theme.colorScheme.onPrimary.toHex()
            }}
        >
            {component.text}
        </MuiButton>
    );
}

// Color conversion
class Color {
    toHex(): string {
        const r = this.red.toString(16).padStart(2, '0');
        const g = this.green.toString(16).padStart(2, '0');
        const b = this.blue.toString(16).padStart(2, '0');
        return `#${r}${g}${b}`;
    }
}
```

---

## Best Practices

### 1. Always Use Theme Colors

**✅ GOOD:**
```kotlin
Text(
    text = "Hello",
    color = theme.colorScheme.onSurface.toCompose()
)
```

**❌ BAD:**
```kotlin
Text(
    text = "Hello",
    color = Color(0xFF000000)  // Hardcoded black
)
```

### 2. Use Semantic Colors

**✅ GOOD:**
```kotlin
// Use semantic color names
backgroundColor = theme.colorScheme.surface
textColor = theme.colorScheme.onSurface
```

**❌ BAD:**
```kotlin
// Avoid literal color names
backgroundColor = theme.colorScheme.white
textColor = theme.colorScheme.black
```

### 3. Support Dark Mode

```kotlin
fun getStatusColor(severity: String, theme: Theme): Color {
    return when (severity) {
        "error" -> theme.colorScheme.error
        "warning" -> if (theme.isDark) {
            Color.fromHex("#FFB74D")  // Lighter orange for dark mode
        } else {
            Color.fromHex("#FF9800")  // Standard orange for light mode
        }
        else -> theme.colorScheme.surface
    }
}
```

### 4. Use Typography Styles

**✅ GOOD:**
```kotlin
Text(
    text = "Headline",
    style = MaterialTheme.typography.headlineMedium
)
```

**❌ BAD:**
```kotlin
Text(
    text = "Headline",
    fontSize = 28.sp,
    fontWeight = FontWeight.Bold
)
```

### 5. Consistent Spacing

```kotlin
Column(
    modifier = Modifier.padding(theme.spacing.md),
    verticalArrangement = Arrangement.spacedBy(theme.spacing.sm)
) {
    // Content
}
```

### 6. Test with Multiple Themes

```kotlin
@Preview(name = "Light Theme")
@Composable
fun PreviewLight() {
    val renderer = ComposeRenderer(MaterialLightTheme)
    MyComponent().render(renderer)()
}

@Preview(name = "Dark Theme")
@Composable
fun PreviewDark() {
    val renderer = ComposeRenderer(MaterialDarkTheme)
    MyComponent().render(renderer)()
}

@Preview(name = "Custom Brand")
@Composable
fun PreviewBrand() {
    val renderer = ComposeRenderer(MyBrandTheme)
    MyComponent().render(renderer)()
}
```

### 7. Accessibility Considerations

```kotlin
// Ensure sufficient contrast
fun checkContrast(foreground: Color, background: Color): Double {
    val fgLuminance = foreground.luminance()
    val bgLuminance = background.luminance()

    val lighter = max(fgLuminance, bgLuminance)
    val darker = min(fgLuminance, bgLuminance)

    return (lighter + 0.05) / (darker + 0.05)
}

// WCAG AA: Contrast ratio >= 4.5:1 for normal text
// WCAG AAA: Contrast ratio >= 7:1 for normal text
```

---

## Theme Export/Import

### Export Theme as JSON

```kotlin
fun Theme.toJson(): String {
    return Json.encodeToString(this)
}
```

**Output:**

```json
{
  "name": "My Brand Theme",
  "isDark": false,
  "colorScheme": {
    "primary": {"red": 25, "green": 118, "blue": 210, "alpha": 1.0},
    "onPrimary": {"red": 255, "green": 255, "blue": 255, "alpha": 1.0},
    "surface": {"red": 255, "green": 255, "blue": 255, "alpha": 1.0},
    ...
  },
  "typography": { ... },
  "spacing": { ... },
  "shapes": { ... }
}
```

### Import Theme from JSON

```kotlin
fun Theme.Companion.fromJson(json: String): Theme {
    return Json.decodeFromString(json)
}

// Usage
val themeJson = File("my-theme.json").readText()
val customTheme = Theme.fromJson(themeJson)
ThemeProvider.registerTheme("imported", customTheme)
```

---

## Advanced Theming

### Dynamic Color (Android 12+)

```kotlin
@Composable
fun getDynamicTheme(): Theme {
    val context = LocalContext.current

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Use system dynamic colors
        val dynamicColorScheme = dynamicLightColorScheme(context)
        Theme(
            colorScheme = dynamicColorScheme.toAvaElements(),
            typography = MaterialTypography,
            spacing = Spacing(),
            shapes = MaterialShapes,
            name = "Dynamic",
            isDark = false
        )
    } else {
        MaterialLightTheme
    }
}
```

### Gradient Themes

```kotlin
data class GradientTheme(
    val baseTheme: Theme,
    val gradients: Map<String, Gradient>
)

data class Gradient(
    val colors: List<Color>,
    val angle: Float  // 0-360 degrees
)

// Usage
val sunsetGradient = Gradient(
    colors = listOf(
        Color.fromHex("#FF6B6B"),
        Color.fromHex("#FFD93D"),
        Color.fromHex("#6BCB77")
    ),
    angle = 135f
)
```

---

## Next Steps

📖 **Continue to:**
- [Chapter 05 - Building Custom Components](./05-Custom-Components.md)
- [Tutorial 04 - Custom Theme Creation](../tutorials/04-Custom-Theme.md)

📚 **Related Docs:**
- [Component Guide](./02-Component-Guide.md) - See how components use themes
- [Android Renderer](./03-Android-Renderer.md) - Theme integration details

---

**Version:** 2.0.0
**Platform Support:** Android (complete), iOS (planned), Web (planned)
**Framework:** AvaElements on Kotlin Multiplatform
**Methodology:** IDEACODE 7.2.0
**Author:** Manoj Jhawar (manoj@ideahq.net)
