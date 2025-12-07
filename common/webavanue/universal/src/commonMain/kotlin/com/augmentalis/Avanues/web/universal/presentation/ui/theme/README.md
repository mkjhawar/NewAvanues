# WebAvanue Theming System

## Overview

WebAvanue uses a **dual-theme architecture** that supports:

1. **APP_BRANDING** - WebAvanue's unique purple/blue branding (standalone mode)
2. **AVAMAGIC** - Avanues system-wide theme (ecosystem mode)

**Key Principle:** Developers define their own branding, but when running in Avanues ecosystem, the device's global AvaMagic theme takes over for visual consistency.

---

## Architecture

```
ui/theme/
├── abstraction/              # Theme-agnostic interfaces
│   ├── AppColors.kt          # Color interface
│   └── AppTypography.kt      # Typography interface
│
├── webavanue/                # WebAvanue's unique branding
│   ├── WebAvanueColors.kt    # Purple/blue/teal palette
│   └── WebAvanueTypography.kt# Standard type scale
│
├── avamagic/                 # Avanues system theme
│   ├── AvaMagicColors.kt     # Queries AvanuesThemeService
│   ├── AvaMagicTypography.kt # Voice-first typography
│   └── (AvanuesThemeService) # System theme provider (placeholder)
│
├── AppTheme.kt               # Main theme provider
└── ThemeConfig.kt            # Theme detection & preferences
```

---

## How It Works

### Theme Selection Logic

```
┌─────────────────────────────────────────┐
│ 1. Check User Preference                │
│    - Settings > Theme                   │
│    - APP_BRANDING or AVAMAGIC           │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 2. If preference = AUTO (default)       │
│    - Detect environment                 │
└────────────┬────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────┐
│ 3. Check Avanues Packages (Android)     │
│    - com.avanues.launcher               │
│    - com.avanues.framework              │
│    - com.ideahq.voiceos                 │
└────────────┬────────────────────────────┘
             │
       ┌─────┴─────┐
       │           │
    Found       Not Found
       │           │
       ▼           ▼
   AVAMAGIC   APP_BRANDING
  (system     (WebAvanue
   theme)      branding)
```

---

## Two Theme Implementations

### 1. WebAvanueColors (APP_BRANDING) - Dark 3D Theme

**Purpose:** WebAvanue's unique brand identity - Dark 3D Theme

**Colors:**
- Background Primary: #1A1A2E - Deep dark base
- Background Secondary: #16213E - Elevated surfaces
- Background Surface: #0F3460 - Command bar, interactive elements
- Accent Voice: #A78BFA - Voice/listening state (purple)
- Accent Blue: #60A5FA - Links, active states
- Text Primary: #E8E8E8 - Main text
- Text Secondary: #A0A0A0 - Muted text

**When Used:**
- Standalone mode (Play Store installs, direct APK)
- User manually selects "WebAvanue Branding" in Settings

**Implementation:**
```kotlin
class WebAvanueColors(private val isDark: Boolean) : AppColors {
    private val bgPrimary = Color(0xFF1A1A2E)
    private val bgSecondary = Color(0xFF16213E)
    private val bgSurface = Color(0xFF0F3460)
    private val accentVoice = Color(0xFFA78BFA)
    private val accentBlue = Color(0xFF60A5FA)

    override val primary = accentVoice
    override val background = bgPrimary
    override val surface = bgSecondary
    // ... dark 3D theme colors
}
```

**Key Point:** Dark 3D theme with voice-first design optimized for accessibility and voice UI.

---

### 2. AvaMagicColors (AVAMAGIC)

**Purpose:** Avanues system-wide theme (replaces app branding)

**How It Works:**
```kotlin
class AvaMagicColors(private val isDark: Boolean) : AppColors {
    // Query Avanues system for current theme
    private val systemTheme = AvanuesThemeService.getCurrentTheme()

    override val primary = systemTheme.primary        // Device decides!
    override val secondary = systemTheme.secondary    // Device decides!
    override val voiceActive = systemTheme.voiceActive
    // ... all colors from system
}
```

**Key Point:** Dynamic, system-provided colors. Could be purple, blue, green, red - **user/device decides**.

**Benefits:**
- All Avanues apps look cohesive (same colors)
- User customization (device theme settings apply to all apps)
- Voice-first design language consistency

**Current Status:**
- Architecture in place ✅
- `AvanuesThemeService` is a placeholder (awaiting VoiceOS integration)
- Falls back to hardcoded AvaMagic palette temporarily

---

## Usage

### 1. Wrap your app with `AppTheme`

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme system (Android only)
        initializeThemeSystem(this)

        setContent {
            AppTheme {  // Auto-detects environment
                BrowserApp()
            }
        }
    }
}
```

### 2. Access theme in components (theme-agnostic)

```kotlin
@Composable
fun MyComponent() {
    val colors = LocalAppColors.current       // Works with BOTH themes
    val typography = LocalAppTypography.current
    val themeType = LocalThemeType.current

    Column(
        modifier = Modifier.background(colors.background)
    ) {
        Text(
            text = "Hello World",
            color = colors.onBackground,
            style = typography.bodyLarge
        )

        // Voice UI (only in AvaMagic)
        if (themeType == ThemeType.AVAMAGIC) {
            VoiceButton(
                activeColor = colors.voiceActive,
                inactiveColor = colors.voiceInactive
            )
        }
    }
}
```

### 3. Override theme manually (for testing/debugging)

```kotlin
// Force WebAvanue branding
AppTheme(themeType = ThemeType.APP_BRANDING) {
    BrowserApp()
}

// Force AvaMagic
AppTheme(themeType = ThemeType.AVAMAGIC) {
    BrowserApp()
}

// Auto-detect (default)
AppTheme(themeType = ThemeType.AUTO) {
    BrowserApp()
}
```

---

## Available Colors (AppColors Interface)

**Standard Colors:**
- `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`
- `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`
- `tertiary`, `onTertiary`, `tertiaryContainer`, `onTertiaryContainer`
- `error`, `onError`, `errorContainer`, `onErrorContainer`
- `background`, `onBackground`
- `surface`, `onSurface`, `surfaceVariant`, `onSurfaceVariant`
- `outline`, `outlineVariant`

**Voice-Specific (Avanues ecosystem):**
- `voiceActive` - Active voice indicator (vibrant purple in AvaMagic)
- `voiceInactive` - Inactive voice indicator (grey)
- `voiceListening` - Listening state (bright green in AvaMagic)

**Browser-Specific:**
- `tabActive` - Active tab background
- `tabInactive` - Inactive tab background
- `addressBarBackground` - Address bar background

---

## User Preference Override

Users can override auto-detection in Settings:

```kotlin
@Composable
fun ThemeSettingsScreen() {
    var selectedTheme by remember {
        mutableStateOf(ThemePreferences.getTheme() ?: ThemeType.AUTO)
    }

    Column {
        RadioButton(
            text = "Auto-detect (Recommended)",
            selected = selectedTheme == ThemeType.AUTO,
            onClick = {
                selectedTheme = ThemeType.AUTO
                ThemePreferences.setTheme(ThemeType.AUTO)
            }
        )

        RadioButton(
            text = "WebAvanue Branding",
            selected = selectedTheme == ThemeType.APP_BRANDING,
            onClick = {
                selectedTheme = ThemeType.APP_BRANDING
                ThemePreferences.setTheme(ThemeType.APP_BRANDING)
            }
        )

        RadioButton(
            text = "AvaMagic Theme",
            selected = selectedTheme == ThemeType.AVAMAGIC,
            onClick = {
                selectedTheme = ThemeType.AVAMAGIC
                ThemePreferences.setTheme(ThemeType.AVAMAGIC)
            }
        )
    }
}
```

---

## Comparison: WebAvanue vs AvaMagic

| Feature | WebAvanue (APP_BRANDING) | AvaMagic (AVAMAGIC) |
|---------|--------------------------|---------------------|
| **Colors** | Hardcoded (purple/teal/blue) | System-provided (varies) |
| **Who Decides** | WebAvanue developers | Avanues system / user |
| **When Used** | Standalone mode | Avanues ecosystem mode |
| **Customization** | Fixed branding | Dynamic system theme |
| **Voice Colors** | Fallback to primary/secondary | Dedicated voice colors |
| **Typography** | Standard type scale | Voice-first type scale |
| **Consistency** | WebAvanue only | All Avanues apps |

**Example Scenario:**

1. **User installs WebAvanue from Play Store**
   - Theme: APP_BRANDING (WebAvanue purple/teal)
   - Colors: Fixed, consistent WebAvanue branding

2. **Same user installs Avanues Launcher**
   - Theme: AUTO-SWITCHES to AVAMAGIC
   - Colors: Now match system-wide Avanues theme (could be green, red, etc.)
   - WebAvanue looks consistent with AVA, AVAConnect, all Avanues apps

3. **User goes to Settings > Theme > "WebAvanue Branding"**
   - Theme: Manually set to APP_BRANDING
   - Colors: Back to WebAvanue purple/teal (overrides AvaMagic)

---

## AvanuesThemeService (Future Implementation)

**Current Status:** Placeholder

**How It Will Work:**

```kotlin
// In VoiceOS/Avanues Framework
object AvanuesThemeService {
    fun getCurrentTheme(): SystemTheme {
        // Query system settings
        val userPreference = Settings.System.getString(
            contentResolver,
            "avanues_theme"
        )

        // Return system theme colors
        return SystemTheme(
            primary = userPreference.primary,
            secondary = userPreference.secondary,
            voiceActive = userPreference.voiceActive,
            // ... all colors
        )
    }
}
```

**Integration:**

```kotlin
class AvaMagicColors : AppColors {
    private val systemTheme = AvanuesThemeService.getCurrentTheme()

    override val primary = systemTheme.primary  // Device decides!
}
```

**When This Becomes Available:**
1. Update `AvaMagicColors` to use `AvanuesThemeService`
2. Remove placeholder hardcoded colors
3. Test across all Avanues apps
4. Document in VoiceOS integration guide

---

## Testing

### Test Both Themes Side-by-Side

```kotlin
@Preview(widthDp = 800)
@Composable
fun PreviewBothThemes() {
    Row {
        // WebAvanue branding
        AppTheme(themeType = ThemeType.APP_BRANDING) {
            Surface(Modifier.weight(1f)) {
                BrowserScreen()
            }
        }

        Divider(Modifier.width(2.dp))

        // AvaMagic theme
        AppTheme(themeType = ThemeType.AVAMAGIC) {
            Surface(Modifier.weight(1f)) {
                BrowserScreen()
            }
        }
    }
}
```

### Test Auto-Detection

```kotlin
// Install Avanues Launcher → should use AvaMagic
// Uninstall Avanues Launcher → should use WebAvanue branding
// Check: LocalThemeType.current == ThemeType.AVAMAGIC
```

---

## Best Practices

### ✅ DO:
- Use `LocalAppColors.current` instead of hardcoded colors
- Use `LocalAppTypography.current` instead of hardcoded text styles
- Check `LocalThemeType.current` for theme-specific UI
- Test your UI with **both** themes
- Support dark mode for both themes
- Provide theme override in Settings

### ❌ DON'T:
- Hardcode colors (use theme colors)
- Assume APP_BRANDING (support both themes)
- Ignore AvaMagic voice colors
- Couple UI to specific theme implementation
- Use Material3 colors directly (use AppColors)

---

## Migration from Old Code

**Old (Material3 directly):**
```kotlin
@Composable
fun MyComponent() {
    val colors = MaterialTheme.colorScheme  // ❌ Only works with Material
    Text(text = "Hello", color = colors.primary)
}
```

**New (Theme-agnostic):**
```kotlin
@Composable
fun MyComponent() {
    val colors = LocalAppColors.current  // ✅ Works with BOTH themes
    Text(text = "Hello", color = colors.primary)
}
```

---

## FAQ

**Q: Why not use Material Design 3 as app branding?**
A: Material Design is a design system, not your unique branding. WebAvanue has its own brand identity (purple/teal).

**Q: What happens if AvanuesThemeService is not available?**
A: AvaMagicColors falls back to hardcoded AvaMagic palette (purple/teal). Still looks good, just not dynamic.

**Q: Can I customize WebAvanue colors?**
A: Yes! Edit `WebAvanueColors.kt` to use your own hex codes. This is YOUR branding.

**Q: Why have two separate themes?**
A: **Flexibility.** Standalone apps show unique branding. Ecosystem apps show consistent system theme.

**Q: How does user know which theme is active?**
A: Settings > About > Theme (shows "WebAvanue Branding" or "AvaMagic System Theme")

**Q: Can I add custom colors?**
A: Yes! Extend `AppColors` interface, implement in both `WebAvanueColors` and `AvaMagicColors`.

**Q: Does this work on iOS/Desktop/Web?**
A: Yes! Theme detection is platform-specific, but theming system is cross-platform (KMP).

**Q: What if user uninstalls Avanues Launcher?**
A: Theme automatically switches back to APP_BRANDING (WebAvanue purple/teal).

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        AppTheme                             │
│                   (Main Theme Provider)                     │
└────────────────────┬────────────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          │                     │
    ┌─────▼─────┐       ┌──────▼──────┐
    │ WebAvanue │       │  AvaMagic   │
    │  Colors   │       │   Colors    │
    │ (Static)  │       │ (Dynamic)   │
    └─────┬─────┘       └──────┬──────┘
          │                     │
          │                ┌────▼─────┐
          │                │ Avanues  │
          │                │  Theme   │
          │                │ Service  │
          │                └──────────┘
          │                (VoiceOS API)
          │
    ┌─────▼──────────────────────────┐
    │      AppColors Interface       │
    │ (Theme-agnostic components)    │
    └────────────────────────────────┘
```

---

## Resources

- **Protocol:** `/protocols/Protocol-UI-Theming-Architecture-v1.0.md`
- **Examples:** See `BrowserScreen.kt`, `TabBar.kt`, `AddressBar.kt`
- **CLAUDE.md:** Framework instructions (v8.4)
- **AvaMagic:** (Coming soon in VoiceOS)

---

## Summary

🎨 **WebAvanue Branding** - Your unique purple/teal identity (standalone)
✨ **AvaMagic Theme** - System-wide consistency (ecosystem)
🔄 **Auto-Detection** - Seamless switching based on environment
⚙️ **User Control** - Override in Settings if desired

**Result:** Best of both worlds - unique branding when standalone, cohesive experience when in Avanues ecosystem!
