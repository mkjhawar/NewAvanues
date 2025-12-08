# AvaElements Plugin Architecture

**Modular, On-Demand Component System**

---

## 🎯 Vision

Transform AvaElements from a monolithic library into a **plugin-based ecosystem** where apps only bundle what they need, with the ability to dynamically load additional components, themes, and templates.

---

## 📊 Current vs Proposed

### Current (Monolithic)

```
Every App Bundles:
├── 48 components (Phase 1 + 3)               ~500 KB
├── 3 platform renderers                      ~200 KB
├── 10+ themes                                ~100 KB
├── State management                          ~50 KB
├── Examples/templates                        ~300 KB
└── TOTAL                                     ~1.15 MB

Problem: App using 5 components still bundles all 48!
```

### Proposed (Plugin-Based)

```
Base App Bundle:
├── AvaElements Core                        ~50 KB
│   ├── Component interface
│   ├── Renderer interface
│   ├── Plugin system
│   └── Registry
├── State Management                          ~30 KB
├── Component Registry (metadata)             ~10 KB
└── TOTAL                                     ~90 KB

Component Plugins (load on-demand):
├── @avaelements/button                     ~5 KB
├── @avaelements/textfield                  ~8 KB
├── @avaelements/card                       ~6 KB
└── [Only bundle what you import]

Theme Plugins (load on-demand):
├── @avaelements/theme-material3            ~15 KB
├── @avaelements/theme-ios26                ~20 KB
└── [Only bundle active theme]

Template Plugins (download on-demand):
├── @avaelements/template-login             ~25 KB
├── @avaelements/template-dashboard         ~40 KB
└── [Download from CDN when needed]
```

**Savings Example:**
- Simple app (5 components): **90 KB + 40 KB** = **130 KB** (vs 1.15 MB = **88% reduction**)
- Complex app (30 components): **90 KB + 200 KB** = **290 KB** (vs 1.15 MB = **75% reduction**)

---

## 🏗️ Architecture

### 1. Component Registry System

```kotlin
/**
 * Component Registry - Metadata for all available components
 */
object ComponentRegistry {
    private val components = mutableMapOf<String, ComponentMetadata>()

    /**
     * Register a component
     */
    fun register(metadata: ComponentMetadata) {
        components[metadata.id] = metadata
    }

    /**
     * Get component metadata
     */
    fun getMetadata(componentId: String): ComponentMetadata? {
        return components[componentId]
    }

    /**
     * Search components
     */
    fun search(query: String): List<ComponentMetadata> {
        return components.values.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }
}

/**
 * Component metadata (bundled in app, ~1 KB per component)
 */
data class ComponentMetadata(
    val id: String,                      // "button", "textfield"
    val name: String,                    // "Button"
    val category: ComponentCategory,     // FORM, DISPLAY, LAYOUT
    val phase: Int,                      // 1, 3
    val version: String,                 // "1.0.0"
    val tags: List<String>,              // ["input", "clickable"]
    val description: String,
    val cdnUrl: String?,                 // Remote URL for on-demand loading
    val bundled: Boolean = false,        // Is it bundled in app?
    val sizeBytes: Int,                  // Component size
    val dependencies: List<String> = emptyList()  // Other components needed
)
```

### 2. Plugin Loader System

```kotlin
/**
 * Component Plugin Loader
 */
interface ComponentPluginLoader {
    /**
     * Load component plugin (from bundle or CDN)
     */
    suspend fun loadComponent(componentId: String): ComponentPlugin?

    /**
     * Preload components for offline use
     */
    suspend fun preloadComponents(
        componentIds: List<String>,
        onProgress: ((Int, Int) -> Unit)? = null
    )

    /**
     * Check if component is available locally
     */
    fun isAvailable(componentId: String): Boolean

    /**
     * Clear component cache
     */
    suspend fun clearCache()
}

/**
 * Component plugin (runtime-loadable)
 */
interface ComponentPlugin {
    val metadata: ComponentMetadata

    /**
     * Create component instance
     */
    fun createComponent(props: Map<String, Any?>): Component

    /**
     * Get renderer for platform
     */
    fun getRenderer(platform: Renderer.Platform): ComponentRenderer?
}
```

### 3. Gradle Module Structure

```
AvaElements/
├── Core/                           # Base (always bundled)
│   ├── Component.kt
│   ├── Renderer.kt
│   ├── ComponentRegistry.kt
│   └── PluginLoader.kt
│
├── StateManagement/                # State (always bundled)
│
├── Components/                     # Component plugins (optional)
│   ├── Phase1/
│   │   ├── Button/                # Separate Gradle module
│   │   ├── TextField/             # Separate Gradle module
│   │   └── Checkbox/              # Separate Gradle module
│   └── Phase3/
│       ├── Slider/                # Separate Gradle module
│       └── DatePicker/            # Separate Gradle module
│
├── Themes/                         # Theme plugins (optional)
│   ├── Material3/                 # Separate Gradle module
│   ├── iOS26LiquidGlass/          # Separate Gradle module
│   └── Custom/
│
└── Templates/                      # Template plugins (CDN only)
    ├── Auth/
    ├── Dashboard/
    └── ECommerce/
```

### 4. Dependency Declaration (build.gradle.kts)

**Option A: Selective Component Dependencies**
```kotlin
dependencies {
    // Core (required)
    implementation("com.augmentalis:avaelements-core:1.0.0")
    implementation("com.augmentalis:avaelements-state:1.0.0")

    // Only bundle components you use
    implementation("com.augmentalis:avaelements-button:1.0.0")
    implementation("com.augmentalis:avaelements-textfield:1.0.0")
    implementation("com.augmentalis:avaelements-card:1.0.0")
    // Others loaded on-demand from CDN

    // Only bundle theme you use
    implementation("com.augmentalis:avaelements-theme-material3:1.0.0")
}
```

**Option B: Component Groups**
```kotlin
dependencies {
    // Core
    implementation("com.augmentalis:avaelements-core:1.0.0")

    // Predefined component packs
    implementation("com.augmentalis:avaelements-essentials:1.0.0")
    // Includes: Button, TextField, Text, Icon, Card (most common)

    // Or full pack (like current)
    implementation("com.augmentalis:avaelements-complete:1.0.0")
}
```

**Option C: Runtime Registration + CDN**
```kotlin
dependencies {
    // Only core
    implementation("com.augmentalis:avaelements-core:1.0.0")

    // Everything else loaded at runtime from CDN
}

// In code
ComponentRegistry.registerCDN(
    baseUrl = "https://cdn.avaelements.io/components"
)

// Components auto-download on first use
val button = MagicButton { /*...*/ }  // Downloads button plugin if needed
```

---

## 📦 Component Plugin Format

### Bundled Plugin (Gradle Module)

```kotlin
// avaelements-button/build.gradle.kts
plugins {
    kotlin("multiplatform")
}

dependencies {
    implementation(project(":AvaElements:Core"))
}

// avaelements-button/src/commonMain/.../ButtonPlugin.kt
class ButtonPlugin : ComponentPlugin {
    override val metadata = ComponentMetadata(
        id = "button",
        name = "Button",
        category = ComponentCategory.FORM,
        phase = 1,
        version = "1.0.0",
        tags = listOf("input", "clickable", "action"),
        description = "Interactive button component",
        bundled = true,
        sizeBytes = 5120
    )

    override fun createComponent(props: Map<String, Any?>): Component {
        return ButtonComponent(/*...*/)
    }

    override fun getRenderer(platform: Renderer.Platform): ComponentRenderer? {
        return when (platform) {
            Renderer.Platform.Android -> ButtonAndroidRenderer()
            Renderer.Platform.iOS -> ButtonIosRenderer()
            else -> null
        }
    }
}

// Auto-register on app startup
@AutoService(ComponentPlugin::class)
class ButtonPluginProvider : ComponentPlugin by ButtonPlugin()
```

### CDN Plugin (Kotlin/JS + WASM)

```
CDN Structure:
https://cdn.avaelements.io/
├── components/
│   ├── button/
│   │   ├── 1.0.0/
│   │   │   ├── button.klib           # Kotlin library
│   │   │   ├── button.android.aar    # Android renderer
│   │   │   ├── button.ios.framework  # iOS renderer
│   │   │   └── metadata.json         # Component metadata
│   │   └── latest -> 1.0.0
│   └── textfield/
│       └── 1.0.0/
│           └── [...]
├── themes/
│   └── material3/
│       └── [...]
└── templates/
    └── login-form/
        └── [...]
```

---

## 🎨 Theme Plugin System

### Theme as Plugin

```kotlin
/**
 * Theme plugin
 */
interface ThemePlugin {
    val metadata: ThemeMetadata

    /**
     * Get theme definition
     */
    fun getTheme(): Theme
}

data class ThemeMetadata(
    val id: String,              // "material3", "ios26"
    val name: String,
    val platform: ThemePlatform,
    val version: String,
    val cdnUrl: String?,
    val bundled: Boolean,
    val sizeBytes: Int
)

// Usage
dependencies {
    // Only bundle active theme
    implementation("com.augmentalis:avaelements-theme-material3:1.0.0")
}

// Or load from CDN
val theme = ThemeRegistry.loadTheme("ios26")  // Downloads if not cached
```

---

## 📝 Template Plugin System

### AvaCode Templates as Plugins

```kotlin
/**
 * UI Template (AvaCode snippet)
 */
data class UITemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,  // AUTH, DASHBOARD, ECOMMERCE
    val description: String,
    val preview: String?,            // Screenshot URL
    val code: String,                // AvaUI DSL
    val components: List<String>,    // Required components
    val sizeBytes: Int
)

// Template Browser UI
val templates = TemplateRegistry.search("login")
// Returns:
// - "Login with Email/Password"
// - "Login with Social Media"
// - "Login with Biometric"

// Download and apply
val template = TemplateRegistry.download("login-social")
val ui = AvaUI.parse(template.code)
renderer.render(ui)
```

---

## 🚀 Implementation Plan

### Phase 1: Core Plugin System (Week 1)
- [ ] Create ComponentRegistry
- [ ] Create ComponentPluginLoader
- [ ] Refactor existing components to plugin format
- [ ] Create Gradle module structure

### Phase 2: CDN Infrastructure (Week 2)
- [ ] Set up CDN (Cloudflare, AWS CloudFront)
- [ ] Component upload pipeline
- [ ] Versioning system
- [ ] Download & caching logic

### Phase 3: Theme Plugins (Week 3)
- [ ] Extract themes to separate modules
- [ ] Theme loader
- [ ] Hot-swap theme support

### Phase 4: Template Marketplace (Week 4)
- [ ] Template registry
- [ ] Template browser UI
- [ ] Template download & preview
- [ ] AvaCode → Component generation

---

## 📈 Benefits

### For Developers

1. **Smaller Apps** - Only bundle what you use (90% reduction possible)
2. **Faster Builds** - Fewer components = faster compilation
3. **Modular** - Easy to add/remove components
4. **Version Control** - Different apps can use different versions
5. **Testing** - Test individual components in isolation

### For Users

1. **Smaller Downloads** - Apps under 10 MB vs 50+ MB
2. **Faster Updates** - Update components without full app update
3. **Better Performance** - Less code to load
4. **Flexible** - Download additional features as needed

### For AvaElements Ecosystem

1. **Scalable** - Add 100s of components without bloating base
2. **Marketplace Ready** - Component marketplace potential
3. **Community** - Developers can contribute components
4. **Revenue** - Premium component packs
5. **Innovation** - Faster iteration on new components

---

## 🔄 Migration Path

### Step 1: Make It Optional (Backward Compatible)

```kotlin
// Old way (still works)
dependencies {
    implementation("com.augmentalis:avaelements:1.0.0")  // All components
}

// New way (opt-in)
dependencies {
    implementation("com.augmentalis:avaelements-core:1.0.0")
    implementation("com.augmentalis:avaelements-essentials:1.0.0")
}
```

### Step 2: Provide Migration Tool

```bash
# Analyze your project
./gradlew analyzeAvaElementsUsage

# Output:
# You are using:
# - Button (5 times)
# - TextField (3 times)
# - Card (8 times)
#
# Recommended dependencies:
# implementation("com.augmentalis:avaelements-button:1.0.0")
# implementation("com.augmentalis:avaelements-textfield:1.0.0")
# implementation("com.augmentalis:avaelements-card:1.0.0")
#
# Potential savings: 900 KB
```

### Step 3: Gradual Rollout

- **v1.0**: Monolithic (current)
- **v1.5**: Introduce plugin system (optional)
- **v2.0**: Plugin-first (with compatibility layer)
- **v3.0**: Plugin-only (remove monolithic bundle)

---

## 💡 Next Steps

**Should we proceed with this plugin architecture?**

Options:
1. **Yes, implement it now** - Refactor AvaElements to plugin system
2. **Yes, but after Asset Manager** - Finish current work first
3. **Prototype first** - Create proof-of-concept with 3-5 components
4. **Different approach** - You have other ideas

**My recommendation:** Option 3 (Prototype) with Button, TextField, Card to validate the architecture before full refactor.

What do you think?
