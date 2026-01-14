# AvaElements Plugin System - Usage Guide

**Smart Defaults with Full Control Over Bundle Size**

---

## 🎯 Quick Start

### 1. Choose a Preset (Recommended)

Add to your app's `build.gradle.kts`:

```kotlin
// In your app module
dependencies {
    // Core is always required
    implementation("com.augmentalis:avaelements-core:1.0.0")

    // Choose a preset bundle
    implementation("com.augmentalis:avaelements-standard:1.0.0")
    // This includes 28 components + multi-platform themes + popular icons
    // Total: ~350 KB
}
```

### 2. Configure Plugin Behavior

```kotlin
// In your Application.onCreate() or main()
AvaElementsConfig.configure(PluginConfigs.STANDARD)
```

That's it! You now have:
- ✅ 28 most common components bundled
- ✅ Material3 + iOS26 themes
- ✅ 30 popular icons cached
- ✅ CDN enabled for additional components
- ✅ Total bundle size: ~350 KB

---

## 📦 Available Presets

### Ultra Minimal (~90 KB)
**Perfect for:** Watch apps, widgets, embedded systems

```kotlin
implementation("com.augmentalis:avaelements-ultra-minimal:1.0.0")
```

**Includes:**
- 5 components: Button, Text, Column, Row, Container
- No themes (CDN only)
- No assets (CDN only)
- CDN enabled

**Use case:**
```kotlin
// Everything works, but loads from CDN on first use
MagicButton { /* works! */ }
MagicTextField { /* downloads component first time */ }
```

---

### Minimal (~180 KB)
**Perfect for:** Simple apps, calculators, notes, utilities

```kotlin
implementation("com.augmentalis:avaelements-minimal:1.0.0")
```

**Includes:**
- 15 components: All basic UI elements
- 1 theme: Material3 Light + Dark
- 15 popular icons cached
- CDN enabled

**Components:**
Button, TextField, Text, Checkbox, Switch, Icon, Image,
Column, Row, Container, Card, ScrollView, Spinner, ProgressBar, Alert

---

### Standard (~350 KB) ⭐ RECOMMENDED
**Perfect for:** Most mobile apps (80% use case)

```kotlin
implementation("com.augmentalis:avaelements-standard:1.0.0")
```

**Includes:**
- 28 components: All Phase 1 + common Phase 3
- 4 themes: Material3 + iOS26 (light/dark)
- 30 popular icons cached
- CDN enabled

**Components:**
All from Minimal, plus:
Slider, DatePicker, Dropdown, SearchBar, Badge, Chip, Avatar,
Divider, Snackbar, Modal, AppBar, BottomNav

---

### Complete (~1 MB)
**Perfect for:** Feature-rich apps, dashboards, admin panels

```kotlin
implementation("com.augmentalis:avaelements-complete:1.0.0")
```

**Includes:**
- 48 components: ALL Phase 1 + Phase 3
- 8 themes: All available themes
- 200 essential icons bundled
- CDN enabled

**All components available immediately, no downloads!**

---

### Offline First (~1.5 MB)
**Perfect for:** Offline apps, field apps, no-internet scenarios

```kotlin
implementation("com.augmentalis:avaelements-offline-first:1.0.0")
```

**Includes:**
- 48 components: ALL bundled
- 8 themes: ALL bundled
- 200 essential icons bundled
- CDN **disabled** (everything works offline)

---

### CDN Only (~90 KB)
**Perfect for:** Experimental, prototyping, always-online apps

```kotlin
implementation("com.augmentalis:avaelements-cdn-only:1.0.0")
```

**Includes:**
- 5 minimal components
- Everything else loads from CDN on-demand
- Smallest possible bundle
- Requires internet connection

---

## 🎨 Custom Configuration

### Option 1: Modify a Preset

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.augmentalis:avaelements-core:1.0.0")
    implementation("com.augmentalis:avaelements-plugin-system:1.0.0")

    // Add specific components
    implementation("com.augmentalis:avaelements-button:1.0.0")
    implementation("com.augmentalis:avaelements-textfield:1.0.0")
    implementation("com.augmentalis:avaelements-card:1.0.0")

    // Add specific theme
    implementation("com.augmentalis:avaelements-theme-material3:1.0.0")
}

// In code
AvaElementsConfig.configure(
    PluginConfigs.custom(
        components = ComponentSet.CUSTOM, // Will use what you bundled
        themes = ThemeSet.CUSTOM,
        assets = AssetSet.POPULAR_ONLY,
        enableCDN = true
    )
)
```

### Option 2: Mix Preset + Custom

```kotlin
// Start with Standard preset
implementation("com.augmentalis:avaelements-standard:1.0.0")

// Add extra components you need
implementation("com.augmentalis:avaelements-rating:1.0.0")
implementation("com.augmentalis:avaelements-autocomplete:1.0.0")

// Configure
AvaElementsConfig.configure(
    PluginConfigs.STANDARD.copy(
        // Keep standard config but customize assets
        assets = AssetSet.ESSENTIAL_PACK
    )
)
```

### Option 3: Build from Scratch

```kotlin
val customConfig = magicElementsConfig {
    components(ComponentSet.MINIMAL)
    themes(ThemeSet.SINGLE_MATERIAL3)
    assets(AssetSet.POPULAR_ONLY)
    enableCDN(true)
    autoCachePopular(true)
}

AvaElementsConfig.configure(customConfig)
```

---

## 🔧 Fine-Grained Component Selection

### Bundle Only What You Need

```kotlin
dependencies {
    // Core (required)
    implementation("com.augmentalis:avaelements-core:1.0.0")

    // Pick components individually
    implementation("com.augmentalis:avaelements-button:1.0.0")      // ~5 KB
    implementation("com.augmentalis:avaelements-textfield:1.0.0")   // ~8 KB
    implementation("com.augmentalis:avaelements-card:1.0.0")        // ~6 KB
    implementation("com.augmentalis:avaelements-list:1.0.0")        // ~10 KB

    // Total: ~90 KB (core) + ~29 KB (components) = ~119 KB
}
```

### Component Packs

```kotlin
// Instead of individual components, use packs
implementation("com.augmentalis:avaelements-pack-forms:1.0.0")
// Includes: Button, TextField, Checkbox, Switch, Dropdown, DatePicker

implementation("com.augmentalis:avaelements-pack-navigation:1.0.0")
// Includes: AppBar, BottomNav, Drawer, Tabs, Breadcrumb

implementation("com.augmentalis:avaelements-pack-feedback:1.0.0")
// Includes: Alert, Snackbar, Modal, Toast, Spinner, ProgressBar
```

---

## 🎨 Theme Configuration

### Single Platform Theme

```kotlin
dependencies {
    implementation("com.augmentalis:avaelements-core:1.0.0")
    implementation("com.augmentalis:avaelements-essentials:1.0.0")

    // Android only
    implementation("com.augmentalis:avaelements-theme-material3:1.0.0")
}

// In code
AvaElementsConfig.configure(
    PluginConfigs.MINIMAL.copy(
        themes = ThemeSet.SINGLE_MATERIAL3
    )
)
```

### Cross-Platform Themes

```kotlin
dependencies {
    // Both Material3 (Android) and iOS26 (iOS)
    implementation("com.augmentalis:avaelements-theme-material3:1.0.0")
    implementation("com.augmentalis:avaelements-theme-ios26:1.0.0")
}

AvaElementsConfig.configure(
    PluginConfigs.STANDARD.copy(
        themes = ThemeSet.MULTI_PLATFORM
    )
)
```

### Custom Theme

```kotlin
dependencies {
    // Load theme from CDN or bundle custom
    implementation("com.augmentalis:avaelements-theme-custom:1.0.0")
}
```

---

## 🖼️ Asset Configuration

### Popular Icons Only (Recommended)

```kotlin
AvaElementsConfig.configure(
    PluginConfigs.STANDARD.copy(
        assets = AssetSet.POPULAR_ONLY
        // Caches 30 most common icons (~50 KB)
        // Others load from CDN on-demand
    )
)
```

**Popular icons cached:**
Material: home, search, settings, menu, close, add, remove, edit, delete, check, star, favorite, share, person, notifications

Font Awesome: house, magnifying-glass, gear, bars, xmark, plus, minus, pen, trash, check, star, heart, share, user, bell

### Essential Pack (200 icons)

```kotlin
AvaElementsConfig.configure(
    PluginConfigs.STANDARD.copy(
        assets = AssetSet.ESSENTIAL_PACK
        // Bundles 200 most common icons (~400 KB)
        // Good for offline apps
    )
)
```

### Metadata Only (Smallest)

```kotlin
AvaElementsConfig.configure(
    PluginConfigs.MINIMAL.copy(
        assets = AssetSet.METADATA_ONLY
        // Only icon metadata bundled (~10 KB)
        // All icons load from CDN
        // Requires internet
    )
)
```

---

## 🔍 How It Works

### 1. Build Time

When you build your app:

```kotlin
implementation("com.augmentalis:avaelements-standard:1.0.0")
```

Gradle bundles:
- ✅ Core system (90 KB)
- ✅ 28 components (250 KB)
- ✅ 4 themes (50 KB)
- ✅ 30 popular icons (50 KB)
- **Total: ~440 KB in APK/IPA**

### 2. Runtime Initialization

In your app:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize with preset
        AvaElementsConfig.configure(PluginConfigs.STANDARD)

        // This prints configuration summary
    }
}
```

Output:
```
╔═══════════════════════════════════════════════════════════════╗
║          AvaElements Plugin Configuration                  ║
╠═══════════════════════════════════════════════════════════════╣
║  Preset: Standard                                             ║
║  Components: STANDARD (28 components)                         ║
║  Themes: MULTI_PLATFORM (4 themes)                            ║
║  Assets: POPULAR_ONLY                                         ║
║  CDN Enabled: Yes                                             ║
║  Auto-cache Popular: Yes                                      ║
╠═══════════════════════════════════════════════════════════════╣
║  Estimated Bundle Size: 350 KB                                ║
╚═══════════════════════════════════════════════════════════════╝
```

### 3. Component Usage

**Bundled component (instant):**
```kotlin
MagicButton {
    text = "Click Me"
    // Renders immediately, no download
}
```

**Non-bundled component (auto-download):**
```kotlin
// RangeSlider not in STANDARD preset
MagicRangeSlider {
    // First use: Downloads from CDN (~8 KB, takes ~1 sec)
    // Cached locally
    // Subsequent uses: Instant
}
```

---

## 📊 Preset Comparison

| Preset | Components | Themes | Assets | CDN | Size | Best For |
|--------|-----------|--------|--------|-----|------|----------|
| **Ultra Minimal** | 5 | None | None | ✅ | 90 KB | Watch apps, widgets |
| **Minimal** | 15 | 1 | Popular | ✅ | 180 KB | Simple utilities |
| **Standard** ⭐ | 28 | 2 | Popular | ✅ | 350 KB | Most apps |
| **Complete** | 48 | All | Essential | ✅ | 1 MB | Feature-rich |
| **Offline First** | 48 | All | Essential | ❌ | 1.5 MB | No internet |
| **CDN Only** | 5 | None | None | ✅ | 90 KB | Prototyping |

---

## 🎯 Recommendations by App Type

### Watch App / Widget
```kotlin
implementation("com.augmentalis:avaelements-ultra-minimal:1.0.0")
// 90 KB - Only essentials, CDN for extras
```

### Calculator / Notes / Simple Utility
```kotlin
implementation("com.augmentalis:avaelements-minimal:1.0.0")
// 180 KB - Common components, single theme
```

### Social App / Chat App / Standard Mobile App
```kotlin
implementation("com.augmentalis:avaelements-standard:1.0.0")
// 350 KB - Full UI toolkit, multi-platform
```

### Dashboard / Admin Panel / CRM
```kotlin
implementation("com.augmentalis:avaelements-complete:1.0.0")
// 1 MB - All components, all themes
```

### Field App / Offline App / Medical App
```kotlin
implementation("com.augmentalis:avaelements-offline-first:1.0.0")
// 1.5 MB - Everything bundled, no CDN dependency
```

---

## ✅ Migration from Monolithic

### Old Way (Before Plugin System)

```kotlin
// Used to bundle everything
implementation("com.augmentalis:avaelements:1.0.0")
// Result: 1.15 MB always
```

### New Way (With Plugins)

```kotlin
// Bundle only what you need
implementation("com.augmentalis:avaelements-standard:1.0.0")
// Result: 350 KB (70% reduction!)
```

**Migration is seamless:**
- Old dependency still works (maps to `complete` preset)
- No code changes needed
- Opt-in to smaller bundles when ready

---

## 🚀 Next Steps

1. **Choose a preset** based on your app type
2. **Add dependency** to build.gradle.kts
3. **Configure** in Application.onCreate()
4. **Build and enjoy** smaller app size!

Questions? Check:
- [Component Plugin Architecture](./COMPONENT-PLUGIN-ARCHITECTURE.md)
- [Asset Manager README](../AssetManager/README.md)
