# VoiceUI Android Theme System - Developer Guide

## 🚀 Complete Theme Control for Android

The VoiceUI Android Theme System gives developers unprecedented control over how their apps look on Android devices. Choose from **Material Design**, **device manufacturer themes**, **custom themes**, or create your own!

## 📚 Quick Navigation

1. [Getting Started](#getting-started)
2. [Available Themes](#available-themes)
3. [Basic Usage](#basic-usage)
4. [Advanced Usage](#advanced-usage)
5. [Custom Theme Creation](#custom-theme-creation)
6. [Theme Persistence](#theme-persistence)
7. [Dynamic Colors (Android 12+)](#dynamic-colors)
8. [Performance Optimization](#performance-optimization)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)

## 🚀 Getting Started

### Add Dependencies

```gradle
// app/build.gradle
dependencies {
    implementation "com.augmentalis.voiceos:voiceui-android:1.0.0"
    implementation "androidx.datastore:datastore-preferences:1.0.0"
    implementation "androidx.compose.material3:material3:1.1.0"
    implementation "com.google.android.material:material:1.9.0"
}
```

### Basic Setup

```kotlin
import com.augmentalis.voiceui.*
import com.augmentalis.voiceui.android.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Simplest usage - automatic theme selection
            SimpleVoiceUI(name = "my_app") {
                text("Hello VoiceUI!")
                button("Click Me") {
                    // Handle click
                }
            }
        }
    }
}
```

## 🎨 Available Themes

### Material Design Themes
- **Material You (Dynamic)** - Android 12+ wallpaper-based colors
- **Material 3** - Latest Material Design
- **Material 3 Dark** - Dark variant
- **Material 2** - Classic Material Design
- **Material 2 Dark** - Dark variant
- **Material 1** - Original 2014 Material Design

### Device-Optimized Themes
- **Phone Optimized** - Perfect for smartphones
- **Tablet Optimized** - Spacious layouts for tablets
- **Foldable Optimized** - Adapts to fold states
- **Android TV** - 10-foot UI for televisions
- **Android Auto** - Car-safe interfaces
- **Wear OS** - Optimized for smartwatches

### Manufacturer Themes
- **Samsung One UI 5** - Latest Samsung design
- **Xiaomi MIUI 14** - MIUI design language
- **OnePlus OxygenOS 13** - Clean OnePlus style
- **OPPO ColorOS 13** - OPPO design system
- **Google Pixel Experience** - Pure Google style

### VoiceUI Custom Themes
- **Aurora** - Beautiful gradients
- **Neumorphic** - Soft UI design
- **Glassmorphic** - Translucent surfaces
- **Cyberpunk** - Neon tech aesthetic
- **High Contrast** - Maximum accessibility
- **Nature** - Organic colors and shapes
- **Ultra Minimal** - Maximum simplicity

## 📱 Basic Usage

### Automatic Theme Selection

```kotlin
// VoiceUI automatically picks the best theme for the device
SimpleVoiceUI(name = "my_app") {
    text("Welcome to my app!")
    button("Get Started") { /* action */ }
}
```

### Manual Theme Selection

```kotlin
// Let users pick their preferred theme
VoiceUIWithThemeButton(name = "my_app") {
    card {
        text("Your content here")
        button("Action") { /* action */ }
    }
}
```

### Specific Theme

```kotlin
// Use a specific theme
val themeSystem = AndroidThemeSystem(context)
val cyberpunkTheme = themeSystem.getAllAvailableThemes()
    [VOICEUI_THEMES]?.find { it.id == "voiceui_cyberpunk" }

VoiceUIWithThemes(
    selectedTheme = cyberpunkTheme,
    name = "gaming_app"
) {
    text("Welcome to the Matrix", style = TextStyle(color = Color.Green))
    button("Enter") { /* action */ }
}
```

## 🔧 Advanced Usage

### Full Theme Control

```kotlin
@Composable
fun MyAdvancedApp() {
    var selectedTheme by remember { mutableStateOf<AndroidTheme?>(null) }
    var showThemeSelector by remember { mutableStateOf(false) }
    
    VoiceUIWithThemes(
        selectedTheme = selectedTheme,
        enableThemeSelector = showThemeSelector,
        enableDynamicColors = true,
        themeMode = ThemeMode.SYSTEM,
        name = "advanced_app",
        onThemeChanged = { theme ->
            selectedTheme = theme
            // Save to preferences, analytics, etc.
        },
        onCreateCustomTheme = {
            // Launch custom theme creator
        }
    ) {
        // Your app content
        MyAppContent()
    }
}
```

### Device-Specific Optimization

```kotlin
@Composable
fun AdaptiveApp() {
    SimpleVoiceUI(
        name = "adaptive_app",
        enableDynamicColors = true
    ) {
        // Content automatically adapts to:
        // - Phone: Compact layout, 48dp touch targets
        // - Tablet: Spacious layout, larger fonts
        // - TV: 10-foot UI, large text, focus indicators
        // - Watch: Minimal UI, gesture support
        // - Foldable: Adapts to fold state
        // - Auto: Safety-first, voice-primary
        
        text("This text looks perfect on every device!")
        button("Universal Button") { /* works everywhere */ }
    }
}
```

## 🎨 Custom Theme Creation

### Simple Custom Theme

```kotlin
val myTheme = CustomThemeBuilder()
    .name("My Brand Theme")
    .colors {
        primary(Color(0xFFFF6B35))      // Brand orange
        secondary(Color(0xFF004E89))     // Brand blue
        background(Color.White)
        surface(Color(0xFFF8F8F8))
        
        // Custom brand colors
        custom("accent", Color(0xFF00AA00))
        custom("warning", Color(0xFFFFAA00))
    }
    .typography {
        h1(48f, FontWeight.Bold, "brand-font")
        body1(16f, FontWeight.Normal, "roboto")
    }
    .build()

// Convert to Android theme
val androidTheme = AndroidTheme(
    id = "my_brand",
    name = "My Brand Theme",
    description = "Custom theme for my brand",
    theme = myTheme
)

// Use the theme
VoiceUIWithThemes(selectedTheme = androidTheme, name = "branded_app") {
    // Your themed content
}
```

### Advanced Custom Theme with Device Overrides

```kotlin
val adaptiveTheme = CustomThemeBuilder()
    .name("Universal Brand")
    .colors {
        primary(Color(0xFF2196F3))
        secondary(Color(0xFFFF5722))
    }
    .typography {
        h1(48f, FontWeight.Normal)
        body1(16f, FontWeight.Normal)
    }
    
    // Phone-specific overrides
    .forDevice(DeviceType.PHONE) {
        typography {
            h1(36f)  // Smaller on phones
            body1(14f)
        }
        spacing {
            small(8f)
            medium(16f)
        }
    }
    
    // TV-specific overrides
    .forDevice(DeviceType.TV) {
        typography {
            h1(72f, FontWeight.Bold)  // Large for 10-foot UI
            body1(24f)
        }
        colors {
            primary(Color(0xFF0D47A1))  // Higher contrast
        }
    }
    
    .build()
```

## 💾 Theme Persistence

### Automatic Persistence

```kotlin
// Themes are automatically saved and restored
val themePersistence = ThemePersistence(context)

// Save current theme
themePersistence.saveCurrentTheme(selectedTheme)

// Load saved theme
val savedThemeId = themePersistence.getCurrentTheme()

// Manage preferences
themePersistence.saveDynamicColorsEnabled(true)
themePersistence.saveThemeMode(ThemeMode.DARK)
```

### Import/Export Themes

```kotlin
val themePersistence = ThemePersistence(context)

// Export theme to share
val themeJson = themePersistence.exportTheme(myTheme)
shareTheme(themeJson) // Share via intent, upload to server, etc.

// Import theme from JSON
val importedTheme = themePersistence.importTheme(receivedJson)
if (importedTheme != null) {
    // Use imported theme
}

// Export all custom themes
val allThemesJson = themePersistence.exportAllThemes()

// Import multiple themes
val importCount = themePersistence.importThemes(themesJson)
println("Imported $importCount themes")
```

## 🌈 Dynamic Colors (Android 12+)

### Automatic Dynamic Colors

```kotlin
// Dynamic colors are enabled by default on Android 12+
SimpleVoiceUI(
    name = "dynamic_app",
    enableDynamicColors = true // Default: true
) {
    // Colors automatically match user's wallpaper
    text("This text color adapts to your wallpaper!")
    button("Dynamic Button") { /* themed automatically */ }
}
```

### Manual Dynamic Color Control

```kotlin
val dynamicColorExtractor = DynamicColorExtractor(context)

// Listen to color changes
val colorScheme by dynamicColorExtractor.colorScheme.collectAsState()

colorScheme?.let { scheme ->
    val dynamicTheme = scheme.toCustomTheme("Dynamic Theme")
    
    VoiceUIWithThemes(
        selectedTheme = AndroidTheme(
            id = "dynamic_custom",
            name = "Dynamic Custom",
            description = "Custom theme with dynamic colors",
            theme = dynamicTheme
        ),
        name = "dynamic_custom_app"
    ) {
        // Content themed with dynamic colors
    }
}
```

### Create Theme from Seed Color

```kotlin
val dynamicColorExtractor = DynamicColorExtractor(context)

// Create theme from brand color
val brandColor = Color(0xFFFF6B35)
val brandTheme = dynamicColorExtractor.createFromSeed(
    seedColor = brandColor,
    isDark = false
)

val brandAndroidTheme = AndroidTheme(
    id = "brand_dynamic",
    name = "Brand Dynamic",
    description = "Brand theme with Material You harmony",
    theme = brandTheme.toCustomTheme("Brand Dynamic")
)
```

## ⚡ Performance Optimization

### Theme Caching

```kotlin
// Themes are automatically cached
val fontManager = FontManager(context)

// Fonts are loaded once and cached
val inter = fontManager.getFontFamily("inter")
val roboto = fontManager.getFontFamily("roboto")

// Clear cache if needed (rare)
fontManager.clearCache()
```

### Lazy Loading

```kotlin
// Themes load lazily by default
val themeSystem = AndroidThemeSystem(context)

// Only loaded when accessed
val allThemes = themeSystem.getAllAvailableThemes()

// Manufacturer themes only load if device matches
val samsungThemes = allThemes[MANUFACTURER] // Only loads on Samsung devices
```

### Performance Monitoring

```kotlin
// Built-in performance tracking
@Composable
fun MonitoredApp() {
    val renderTime = remember { mutableLongStateOf(0L) }
    
    VoiceUIWithThemes(
        name = "monitored_app",
        // Performance is automatically optimized based on device
    ) {
        // Your content - automatically optimized for device capabilities
    }
}
```

## 💡 Best Practices

### 1. Start with Device Defaults

```kotlin
// ✅ Good - Let the system choose the best theme
SimpleVoiceUI(name = "my_app") {
    // Content
}

// ❌ Avoid - Forcing specific themes without user choice
VoiceUIWithThemes(selectedTheme = specificTheme) {
    // This doesn't respect user preferences
}
```

### 2. Provide Theme Options

```kotlin
// ✅ Good - Give users choice
VoiceUIWithThemeButton(name = "my_app") {
    // User can change theme via button
}
```

### 3. Respect System Settings

```kotlin
// ✅ Good - Follow system dark mode
VoiceUIWithThemes(
    themeMode = ThemeMode.SYSTEM, // Follows system setting
    enableDynamicColors = true    // Respects user's color choice
) {
    // Content
}
```

### 4. Test on Multiple Devices

```kotlin
// ✅ Good - Create device-specific overrides
val theme = CustomThemeBuilder()
    .name("Universal Theme")
    .forDevice(DeviceType.PHONE) { /* phone optimizations */ }
    .forDevice(DeviceType.TABLET) { /* tablet optimizations */ }
    .forDevice(DeviceType.TV) { /* TV optimizations */ }
    .build()
```

### 5. Consider Accessibility

```kotlin
// ✅ Good - Always provide high contrast option
val themes = listOf(
    normalTheme,
    darkTheme,
    highContrastTheme // For users with visual impairments
)
```

## 🔍 Troubleshooting

### Theme Not Loading

```kotlin
// Check if theme exists
val themeSystem = AndroidThemeSystem(context)
val allThemes = themeSystem.getAllAvailableThemes()
val theme = allThemes[MATERIAL_DESIGN]?.find { it.id == "material3_default" }

if (theme == null) {
    Log.e("Theme", "Theme not found!")
    // Use fallback theme
}
```

### Dynamic Colors Not Working

```kotlin
// Check Android version
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    // Check if dynamic colors are available
    val extractor = DynamicColorExtractor(context)
    // Will automatically fallback if not available
} else {
    // Use static theme on older versions
}
```

### Fonts Not Loading

```kotlin
val fontManager = FontManager(context)

// Check available fonts
val availableFonts = fontManager.getAvailableFonts()
Log.d("Fonts", "Available fonts: ${availableFonts.map { it.name }}")

// Use fallback fonts
val font = fontManager.getFontFamily("custom-font") // Will fallback to default
```

### Performance Issues

```kotlin
// Check device profile
val renderer = AndroidAdaptiveRenderer(themeSystem, persistence, fontManager)

// Themes are automatically optimized for device performance
// Low-end devices get reduced animations
// High-DPI devices get optimized rendering
```

## 📊 Integration Examples

### E-Commerce App

```kotlin
@Composable
fun ShoppingApp() {
    VoiceUIWithThemeButton(
        name = "shopping_app",
        buttonText = "Change Theme"
    ) {
        topAppBar("Fashion Store")
        
        card {
            text("Featured Products")
            
            productGrid {
                productCard("Blue Shirt", "$29.99")
                productCard("Red Dress", "$49.99")
                productCard("Green Hat", "$19.99")
            }
        }
        
        button("Add to Cart") { /* action */ }
    }
}
```

### Banking App

```kotlin
@Composable
fun BankingApp() {
    // Force high contrast for security visibility
    val highContrastTheme = AndroidTheme(
        id = "banking_secure",
        name = "Banking Secure",
        description = "High contrast for security",
        theme = createHighContrastTheme()
    )
    
    VoiceUIWithThemes(
        selectedTheme = highContrastTheme,
        name = "secure_banking"
    ) {
        secureCard {
            text("Account Balance")
            text("$2,547.83", style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            ))
        }
        
        secureButton("Transfer Funds") { /* secure action */ }
    }
}
```

### Gaming App

```kotlin
@Composable
fun GamingApp() {
    val cyberpunkTheme = getCyberpunkTheme()
    
    VoiceUIWithThemes(
        selectedTheme = cyberpunkTheme,
        name = "neon_game"
    ) {
        text("NEON RUNNER", style = TextStyle(
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF00FFFF) // Neon cyan
        ))
        
        button("START GAME") { /* game start */ }
        button("SETTINGS") { /* settings */ }
        button("QUIT") { /* quit */ }
    }
}
```

## 🚀 Summary

The VoiceUI Android Theme System provides:

✅ **Complete Control** - Choose from 25+ built-in themes or create custom ones  
✅ **Automatic Adaptation** - Perfect on phones, tablets, TV, watches, cars, foldables  
✅ **Material You Integration** - Android 12+ dynamic colors work seamlessly  
✅ **Manufacturer Support** - Samsung One UI, MIUI, OxygenOS themes included  
✅ **Zero Performance Impact** - Themes are compiled and cached  
✅ **Full Persistence** - User preferences automatically saved  
✅ **Import/Export** - Share themes or load from marketplace  

**Result**: Your app looks perfect on every Android device while maintaining your brand identity!

---

**Next Steps**: 
- Try the [Theme Creator Tool](./VoiceUI-Theme-Creator.md)
- Explore [Advanced Theming](./VoiceUI-Advanced-Theming.md)
- Check out [Performance Tips](./VoiceUI-Performance.md)

---

**Last Updated**: 2025-01-23  
**Version**: 1.0.0  
**Compatibility**: Android 5.0+ (API 21+)