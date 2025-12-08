# Protocol: UI Theming Architecture v1.0

**Version:** 1.0
**Status:** Active
**Effective Date:** 2025-11-15
**Framework Version:** 8.4

---

## 🚨 MANDATORY: Separate Theming Module

**CRITICAL - AI MUST ENFORCE - NOT OPTIONAL:**

EVERY app MUST have a separate, swappable theming module that allows switching between AVAMagic UI and standard platform/Material Design UI at runtime or build time.

---

## Purpose

**Problem:** Without modular theming:
- ❌ UI framework locked in (can't switch from Material to AVAMagic)
- ❌ Apps can't integrate into Avanues ecosystem seamlessly
- ❌ Theming logic scattered across codebase
- ❌ Rebranding requires changing every screen
- ❌ Can't offer theme choices to users

**Solution:** Separate theming module with clean abstraction layer

---

## Architecture

### Overview

```
┌────────────────────────────────────────────────────────┐
│                    APPLICATION                         │
│                                                        │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │  Screen A    │  │  Screen B    │  │  Screen C   │ │
│  │  (UI Layer)  │  │  (UI Layer)  │  │  (UI Layer) │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬──────┘ │
│         │                 │                  │        │
│         └─────────────────┼──────────────────┘        │
│                           │                           │
│              ┌────────────▼────────────┐              │
│              │   THEME ABSTRACTION     │              │
│              │   (Interface Layer)     │              │
│              │                         │              │
│              │  - AppTheme             │              │
│              │  - AppColors            │              │
│              │  - AppTypography        │              │
│              │  - AppComponents        │              │
│              └────────────┬────────────┘              │
│                           │                           │
│           ┌───────────────┴───────────────┐           │
│           │                               │           │
│  ┌────────▼────────┐           ┌──────────▼────────┐ │
│  │  AVAMagic      │           │  Material Design  │ │
│  │  Theme Impl     │           │  Theme Impl       │ │
│  │                 │           │                   │ │
│  │  - Magic colors │           │  - MD3 colors     │ │
│  │  - Magic fonts  │           │  - Roboto fonts   │ │
│  │  - Magic comps  │           │  - MD3 components │ │
│  └─────────────────┘           └───────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## Module Structure

### 🚨 CRITICAL: Per-Project Theming (NOT Universal Library)

**IMPORTANT:** Each project MUST have its own theming module within the app code. This is **NOT** a shared library in `libraries/ui/theme/`.

**Why Per-Project:**
- ✅ Each app has unique branding (colors, logos, custom components)
- ✅ App-specific customization (e.g., AVA uses blue, Avanues uses purple)
- ✅ Different Material You dynamic color implementations
- ✅ Project-specific voice UI components
- ✅ Independent version control and updates

**What CAN Be Shared (Optional):**
- AVAMagic base theme definitions (if multiple apps use identical Avanues branding)
- Theme utility functions (color manipulation, contrast checking)
- Theme switching logic (detection, preferences)

### Directory Layout (Per-Project)

```
{project-name}/                          # e.g., AVA, Avanues, AVAConnect
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── com/{company}/{app}/
│   │   │   │       ├── ui/
│   │   │   │       │   ├── screens/         # App screens (theme-agnostic)
│   │   │   │       │   │   ├── HomeScreen.kt
│   │   │   │       │   │   ├── ProfileScreen.kt
│   │   │   │       │   │   └── SettingsScreen.kt
│   │   │   │       │   │
│   │   │   │       │   └── theme/           # ⭐ PER-PROJECT THEME MODULE
│   │   │   │       │       │
│   │   │   │       │       ├── AppTheme.kt             # Main theme provider (PROJECT-SPECIFIC)
│   │   │   │       │       ├── ThemeConfig.kt          # Theme selection logic (PROJECT-SPECIFIC)
│   │   │   │       │       │
│   │   │   │       │       ├── abstraction/            # Theme abstraction layer
│   │   │   │       │       │   ├── AppColors.kt        # Color interface
│   │   │   │       │       │   ├── AppTypography.kt    # Typography interface
│   │   │   │       │       │   ├── AppShapes.kt        # Shapes interface
│   │   │   │       │       │   └── AppComponents.kt    # Component interface
│   │   │   │       │       │
│   │   │   │       │       ├── ideamagic/              # AVAMagic implementation (PROJECT-SPECIFIC BRANDING)
│   │   │   │       │       │   ├── AVAMagicTheme.kt   # AVA blue / Avanues purple / etc.
│   │   │   │       │       │   ├── AVAMagicColors.kt  # Custom accent colors per app
│   │   │   │       │       │   ├── AVAMagicTypography.kt
│   │   │   │       │       │   └── AVAMagicComponents.kt
│   │   │   │       │       │
│   │   │   │       │       └── material/               # Material Design implementation (PROJECT-SPECIFIC)
│   │   │   │       │           ├── MaterialTheme.kt
│   │   │   │       │           ├── MaterialColors.kt   # Dynamic colors per app
│   │   │   │       │           ├── MaterialTypography.kt
│   │   │   │       │           └── MaterialComponents.kt
│   │   │   │       │
│   │   │   │       └── MainActivity.kt
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── build.gradle.kts
│   │
│   └── ...
│
└── ...
```

**Example: Different Projects, Different Branding**

```kotlin
// AVA project: Blue branding
// ava/app/src/main/kotlin/.../ui/theme/ideamagic/AVAMagicColors.kt
class AVAMagicColors(val isDark: Boolean) : AppColors {
    override val primary = if (isDark) Color(0xFF4A90E2) else Color(0xFF2E5BDA)  // AVA Blue
    override val accent = Color(0xFF00C853)  // AVA Green
}

// Avanues project: Purple branding
// avanues/app/src/main/kotlin/.../ui/theme/ideamagic/AVAMagicColors.kt
class AVAMagicColors(val isDark: Boolean) : AppColors {
    override val primary = if (isDark) Color(0xFF6C63FF) else Color(0xFF5A52E0)  // Avanues Purple
    override val accent = Color(0xFFFF6F00)  // Avanues Orange
}

// AVAConnect project: Teal branding
// avaconnect/app/src/main/kotlin/.../ui/theme/ideamagic/AVAMagicColors.kt
class AVAMagicColors(val isDark: Boolean) : AppColors {
    override val primary = if (isDark) Color(0xFF26C6DA) else Color(0xFF00ACC1)  // AVAConnect Teal
    override val accent = Color(0xFFAB47BC)  // AVAConnect Purple
}
```

**Optional Shared Library (Only for Common Utilities):**

```
/Volumes/M-Drive/Coding/ideacode/libraries/
└── ui/
    └── theme-utils/                     # Optional: Shared utilities ONLY
        ├── src/commonMain/kotlin/
        │   └── com/ideacode/theme/
        │       ├── ColorUtils.kt        # Color manipulation functions
        │       ├── ContrastChecker.kt   # Accessibility contrast checking
        │       └── ThemeDetector.kt     # Avanues ecosystem detection
        └── build.gradle.kts

# Projects can optionally depend on utilities:
dependencies {
    implementation("com.ideacode.ui:theme-utils:1.0.0")  # Optional
}
```

---

## Implementation

### 1. Theme Abstraction Layer

#### AppColors Interface

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/abstraction/AppColors.kt

/**
 * Abstract color scheme for the application.
 *
 * Implementations: AVAMagic, Material Design, Custom
 */
interface AppColors {
    // Primary colors
    val primary: Color
    val onPrimary: Color
    val primaryContainer: Color
    val onPrimaryContainer: Color

    // Secondary colors
    val secondary: Color
    val onSecondary: Color
    val secondaryContainer: Color
    val onSecondaryContainer: Color

    // Background colors
    val background: Color
    val onBackground: Color
    val surface: Color
    val onSurface: Color

    // Error colors
    val error: Color
    val onError: Color

    // Custom semantic colors
    val success: Color
    val warning: Color
    val info: Color

    // Avanues-specific (voice UI)
    val voiceActive: Color
    val voiceInactive: Color
    val voiceListening: Color
}
```

#### AppTypography Interface

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/abstraction/AppTypography.kt

/**
 * Abstract typography system for the application.
 *
 * Implementations: AVAMagic, Material Design, Custom
 */
interface AppTypography {
    val displayLarge: TextStyle
    val displayMedium: TextStyle
    val displaySmall: TextStyle

    val headlineLarge: TextStyle
    val headlineMedium: TextStyle
    val headlineSmall: TextStyle

    val titleLarge: TextStyle
    val titleMedium: TextStyle
    val titleSmall: TextStyle

    val bodyLarge: TextStyle
    val bodyMedium: TextStyle
    val bodySmall: TextStyle

    val labelLarge: TextStyle
    val labelMedium: TextStyle
    val labelSmall: TextStyle
}
```

#### AppComponents Interface

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/abstraction/AppComponents.kt

/**
 * Abstract component styling for the application.
 *
 * Defines appearance of reusable UI components.
 */
interface AppComponents {
    // Button styles
    val primaryButton: ButtonStyle
    val secondaryButton: ButtonStyle
    val textButton: ButtonStyle

    // Card styles
    val cardElevation: Dp
    val cardCornerRadius: Dp

    // Input field styles
    val inputFieldCornerRadius: Dp
    val inputFieldBorderWidth: Dp

    // Voice-specific components
    val voiceButtonSize: Dp
    val voiceWaveformColor: Color
}
```

---

### 2. AVAMagic Theme Implementation

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/ideamagic/AVAMagicColors.kt

/**
 * AVAMagic color scheme implementation.
 *
 * Brand colors for Avanues ecosystem integration.
 */
class AVAMagicColors(
    val isDark: Boolean
) : AppColors {
    override val primary = if (isDark) Color(0xFF6C63FF) else Color(0xFF5A52E0)
    override val onPrimary = Color.White
    override val primaryContainer = if (isDark) Color(0xFF4A42C0) else Color(0xFF8B84FF)
    override val onPrimaryContainer = if (isDark) Color.White else Color.Black

    override val secondary = if (isDark) Color(0xFFFF6584) else Color(0xFFE04A6A)
    override val onSecondary = Color.White
    override val secondaryContainer = if (isDark) Color(0xFFC04258) else Color(0xFFFF8AA0)
    override val onSecondaryContainer = if (isDark) Color.White else Color.Black

    override val background = if (isDark) Color(0xFF121212) else Color(0xFFFFFBFE)
    override val onBackground = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
    override val surface = if (isDark) Color(0xFF1C1B1F) else Color.White
    override val onSurface = if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)

    override val error = Color(0xFFB3261E)
    override val onError = Color.White

    override val success = Color(0xFF4CAF50)
    override val warning = Color(0xFFFF9800)
    override val info = Color(0xFF2196F3)

    // Avanues voice colors (signature AVAMagic palette)
    override val voiceActive = Color(0xFF6C63FF)      // Vibrant purple
    override val voiceInactive = Color(0xFF9E9E9E)    // Gray
    override val voiceListening = Color(0xFF00E676)   // Bright green
}
```

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/ideamagic/AVAMagicTypography.kt

/**
 * AVAMagic typography system.
 *
 * Uses custom "AVAMagic Sans" font (fallback to Poppins/Roboto).
 */
class AVAMagicTypography : AppTypography {
    private val fontFamily = FontFamily(
        Font(R.font.ideamagic_sans_regular),
        Font(R.font.ideamagic_sans_medium, FontWeight.Medium),
        Font(R.font.ideamagic_sans_bold, FontWeight.Bold)
    )

    override val displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    )

    override val headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    )

    override val bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )

    // ... (other text styles)
}
```

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/ideamagic/AVAMagicTheme.kt

/**
 * AVAMagic theme provider.
 *
 * Wraps app content with AVAMagic theming.
 */
@Composable
fun AVAMagicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = AVAMagicColors(isDark = darkTheme)
    val typography = AVAMagicTypography()
    val components = AVAMagicComponents()

    // Provide theme to composition
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppComponents provides components
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = typography.toMaterialTypography(),
            content = content
        )
    }
}
```

---

### 3. Material Design Theme Implementation

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/material/MaterialColors.kt

/**
 * Material Design 3 color scheme implementation.
 *
 * Standard Material You dynamic colors.
 */
class MaterialColors(
    val isDark: Boolean,
    val dynamicColor: Boolean = false,  // Material You support
    val context: Context? = null
) : AppColors {
    private val materialColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context!!)
            else dynamicLightColorScheme(context!!)
        }
        isDark -> darkColorScheme()
        else -> lightColorScheme()
    }

    override val primary = materialColorScheme.primary
    override val onPrimary = materialColorScheme.onPrimary
    override val primaryContainer = materialColorScheme.primaryContainer
    override val onPrimaryContainer = materialColorScheme.onPrimaryContainer

    override val secondary = materialColorScheme.secondary
    override val onSecondary = materialColorScheme.onSecondary
    override val secondaryContainer = materialColorScheme.secondaryContainer
    override val onSecondaryContainer = materialColorScheme.onSecondaryContainer

    override val background = materialColorScheme.background
    override val onBackground = materialColorScheme.onBackground
    override val surface = materialColorScheme.surface
    override val onSurface = materialColorScheme.onSurface

    override val error = materialColorScheme.error
    override val onError = materialColorScheme.onError

    override val success = Color(0xFF4CAF50)
    override val warning = Color(0xFFFF9800)
    override val info = Color(0xFF2196F3)

    // Voice colors (Material palette)
    override val voiceActive = materialColorScheme.primary
    override val voiceInactive = materialColorScheme.outline
    override val voiceListening = Color(0xFF00C853)
}
```

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/material/MaterialTheme.kt

/**
 * Material Design 3 theme provider.
 *
 * Standard Material You theming.
 */
@Composable
fun AppMaterialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialColors(
        isDark = darkTheme,
        dynamicColor = dynamicColor,
        context = context
    )
    val typography = MaterialTypography()
    val components = MaterialComponents()

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        LocalAppComponents provides components
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = typography.toMaterialTypography(),
            content = content
        )
    }
}
```

---

### 4. Theme Configuration & Selection

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/ThemeConfig.kt

/**
 * Theme configuration for the application.
 *
 * Determines which theme implementation to use.
 */
enum class ThemeType {
    IDEAMAGIC,      // AVAMagic theme (Avanues ecosystem)
    MATERIAL,       // Material Design 3 (standard Android)
    CUSTOM          // Custom theme (future)
}

data class ThemeConfig(
    val type: ThemeType = ThemeType.MATERIAL,
    val darkMode: Boolean = false,
    val dynamicColor: Boolean = true  // Material You (Android 12+)
) {
    companion object {
        /**
         * Detects if app is running within Avanues ecosystem.
         *
         * If yes, defaults to AVAMagic theme.
         * If no, defaults to Material theme.
         */
        fun detectTheme(context: Context): ThemeType {
            // Check if Avanues launcher/framework is present
            val packageManager = context.packageManager
            val avaPackages = listOf(
                "com.avanues.launcher",
                "com.avanues.framework",
                "com.ideahq.voiceos"
            )

            val isAvanuesEcosystem = avaPackages.any { packageName ->
                try {
                    packageManager.getPackageInfo(packageName, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }
            }

            return if (isAvanuesEcosystem) {
                ThemeType.IDEAMAGIC
            } else {
                ThemeType.MATERIAL
            }
        }

        /**
         * Loads theme preference from user settings.
         */
        fun fromPreferences(context: Context): ThemeConfig {
            val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
            val themeTypeName = prefs.getString("theme_type", null)

            val type = if (themeTypeName != null) {
                ThemeType.valueOf(themeTypeName)
            } else {
                detectTheme(context)  // Auto-detect on first launch
            }

            return ThemeConfig(
                type = type,
                darkMode = prefs.getBoolean("dark_mode", false),
                dynamicColor = prefs.getBoolean("dynamic_color", true)
            )
        }

        /**
         * Saves theme preference to user settings.
         */
        fun saveToPreferences(context: Context, config: ThemeConfig) {
            context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("theme_type", config.type.name)
                .putBoolean("dark_mode", config.darkMode)
                .putBoolean("dynamic_color", config.dynamicColor)
                .apply()
        }
    }
}
```

```kotlin
// app/src/main/kotlin/com/{app}/ui/theme/AppTheme.kt

/**
 * Main theme provider for the application.
 *
 * Selects appropriate theme based on configuration.
 */
@Composable
fun AppTheme(
    config: ThemeConfig = ThemeConfig.fromPreferences(LocalContext.current),
    content: @Composable () -> Unit
) {
    when (config.type) {
        ThemeType.IDEAMAGIC -> {
            AVAMagicTheme(
                darkTheme = config.darkMode,
                content = content
            )
        }

        ThemeType.MATERIAL -> {
            AppMaterialTheme(
                darkTheme = config.darkMode,
                dynamicColor = config.dynamicColor,
                content = content
            )
        }

        ThemeType.CUSTOM -> {
            // Future: Custom theme implementation
            AppMaterialTheme(
                darkTheme = config.darkMode,
                dynamicColor = false,
                content = content
            )
        }
    }
}
```

---

### 5. Usage in App

```kotlin
// app/src/main/kotlin/com/{app}/MainActivity.kt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Automatically selects theme (AVAMagic if in Avanues, Material otherwise)
            AppTheme {
                AppNavigation()
            }
        }
    }
}
```

```kotlin
// app/src/main/kotlin/com/{app}/ui/screens/HomeScreen.kt

@Composable
fun HomeScreen() {
    // Access theme colors (works with both AVAMagic and Material)
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Scaffold(
        backgroundColor = colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Welcome to App",
                style = typography.headlineLarge,
                color = colors.onBackground
            )

            Button(
                onClick = { /* ... */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                Text("Get Started")
            }

            // Voice button (Avanues-specific)
            VoiceButton(
                isListening = false,
                onClick = { /* ... */ },
                activeColor = colors.voiceActive,
                inactiveColor = colors.voiceInactive
            )
        }
    }
}
```

---

## Build Configuration

### Gradle Build Variants

```kotlin
// app/build.gradle.kts

android {
    // ...

    buildTypes {
        release {
            // Production: Auto-detect theme
            buildConfigField("String", "DEFAULT_THEME", "\"AUTO\"")
        }

        debug {
            // Debug: Can override theme
            buildConfigField("String", "DEFAULT_THEME", "\"MATERIAL\"")
        }
    }

    flavorDimensions += "theme"
    productFlavors {
        create("ideamagic") {
            dimension = "theme"
            buildConfigField("String", "DEFAULT_THEME", "\"IDEAMAGIC\"")
            applicationIdSuffix = ".ideamagic"
        }

        create("material") {
            dimension = "theme"
            buildConfigField("String", "DEFAULT_THEME", "\"MATERIAL\"")
        }

        create("standalone") {
            dimension = "theme"
            buildConfigField("String", "DEFAULT_THEME", "\"AUTO\"")
        }
    }
}
```

**Build variants:**
- `ideamagicRelease` - AVAMagic theme only (for Avanues ecosystem)
- `materialRelease` - Material Design only (for Play Store standalone)
- `standaloneRelease` - Auto-detect theme (hybrid distribution)

---

## User Preferences

### Theme Switcher UI

```kotlin
// app/src/main/kotlin/com/{app}/ui/screens/SettingsScreen.kt

@Composable
fun ThemeSettings() {
    val context = LocalContext.current
    var currentConfig by remember { mutableStateOf(ThemeConfig.fromPreferences(context)) }

    Column {
        Text("Appearance", style = MaterialTheme.typography.headlineSmall)

        // Theme selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ThemeOption(
                label = "AVAMagic",
                selected = currentConfig.type == ThemeType.IDEAMAGIC,
                onClick = {
                    currentConfig = currentConfig.copy(type = ThemeType.IDEAMAGIC)
                    ThemeConfig.saveToPreferences(context, currentConfig)
                }
            )

            ThemeOption(
                label = "Material",
                selected = currentConfig.type == ThemeType.MATERIAL,
                onClick = {
                    currentConfig = currentConfig.copy(type = ThemeType.MATERIAL)
                    ThemeConfig.saveToPreferences(context, currentConfig)
                }
            )
        }

        // Dark mode toggle
        SwitchPreference(
            title = "Dark Mode",
            checked = currentConfig.darkMode,
            onCheckedChange = { enabled ->
                currentConfig = currentConfig.copy(darkMode = enabled)
                ThemeConfig.saveToPreferences(context, currentConfig)
            }
        )

        // Material You toggle (only for Material theme)
        if (currentConfig.type == ThemeType.MATERIAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SwitchPreference(
                title = "Material You",
                subtitle = "Use dynamic colors from wallpaper",
                checked = currentConfig.dynamicColor,
                onCheckedChange = { enabled ->
                    currentConfig = currentConfig.copy(dynamicColor = enabled)
                    ThemeConfig.saveToPreferences(context, currentConfig)
                }
            )
        }
    }
}
```

---

## Benefits

### For Standalone Apps
✅ **User choice:** Material Design (familiar Android UI)
✅ **Material You:** Dynamic colors from wallpaper (Android 12+)
✅ **Play Store ready:** Standard Android appearance

### For Avanues Ecosystem Integration
✅ **Seamless integration:** AVAMagic theme matches Avanues launcher
✅ **Consistent UX:** All Avanues apps have unified look
✅ **Voice-first design:** Optimized for voice interactions

### For Developers
✅ **Modular:** Theme logic isolated in separate module
✅ **Swappable:** Change theme with single line of code
✅ **Testable:** Can test with different themes independently
✅ **Maintainable:** Theming changes don't affect app logic

---

## Migration Guide

### Converting Existing App

#### Before (Hardcoded Material)
```kotlin
@Composable
fun HomeScreen() {
    MaterialTheme {  // ❌ Hardcoded theme
        Text(
            text = "Welcome",
            color = MaterialTheme.colorScheme.primary  // ❌ Hardcoded colors
        )
    }
}
```

#### After (Modular Theme)
```kotlin
@Composable
fun HomeScreen() {
    val colors = LocalAppColors.current  // ✅ Theme-agnostic

    Text(
        text = "Welcome",
        color = colors.primary  // ✅ Works with any theme
    )
}
```

---

## Quality Gates

**MANDATORY before shipping:**
- [ ] Separate `ui/theme/` module exists
- [ ] Abstraction layer defined (AppColors, AppTypography, AppComponents)
- [ ] AVAMagic theme implemented
- [ ] Material theme implemented
- [ ] Theme auto-detection works
- [ ] Theme switcher in settings (optional but recommended)
- [ ] All screens use abstracted theme (no hardcoded MaterialTheme.colorScheme)
- [ ] Dark mode supported in both themes
- [ ] Voice UI components styled appropriately

---

## References

- **Protocol-Modular-Architecture-v1.0.md** - Modular design principles
- **Material Design 3:** https://m3.material.io/
- **Jetpack Compose Theming:** https://developer.android.com/jetpack/compose/themes

---

## Changelog

### v1.0 (2025-11-15)
- Initial protocol creation
- Defined modular theming architecture
- Created abstraction layer (AppColors, AppTypography, AppComponents)
- Implemented AVAMagic and Material themes
- Added auto-detection for Avanues ecosystem
- Created theme switcher UI pattern
- Defined build variants for different distributions

---

**Author:** Manoj Jhawar
**Email:** manoj@ideahq.net
**License:** Proprietary

---

**IDEACODE v8.4** - Modular UI theming for standalone and Avanues ecosystem apps
