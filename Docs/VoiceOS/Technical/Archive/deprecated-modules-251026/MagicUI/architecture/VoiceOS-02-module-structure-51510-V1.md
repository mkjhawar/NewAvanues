# MagicUI Module Structure
## Complete File Organization & Package Layout

**Document:** 02 of 11  
**Version:** 1.0  
**Created:** 2025-10-13  
**Status:** Implementation Ready  

---

## Complete Module Structure

```
modules/libraries/MagicUI/
├── build.gradle.kts                    ← Module build configuration
├── proguard-rules.pro                  ← ProGuard rules
├── consumer-rules.pro                  ← Consumer ProGuard rules
├── README.md                           ← Module documentation
│
└── src/
    ├── main/
    │   ├── AndroidManifest.xml         ← Manifest
    │   ├── res/                        ← Resources
    │   │   ├── values/
    │   │   │   ├── strings.xml
    │   │   │   ├── colors.xml
    │   │   │   └── themes.xml
    │   │   └── drawable/
    │   │
    │   └── java/com/augmentalis/magicui/
    │       │
    │       ├── MagicUI.kt              ← Main entry point
    │       ├── MagicUIModule.kt        ← Module singleton
    │       │
    │       ├── core/                   ← Core DSL implementation
    │       │   ├── MagicUIScope.kt     ← DSL processor (500 lines)
    │       │   ├── MagicScreen.kt      ← Screen wrapper (200 lines)
    │       │   ├── ComponentRegistry.kt ← Component registration
    │       │   ├── StateManager.kt     ← Auto state management
    │       │   └── LifecycleManager.kt ← Lifecycle handling
    │       │
    │       ├── components/             ← UI Components (50+ files)
    │       │   ├── basic/
    │       │   │   ├── TextComponent.kt
    │       │   │   ├── ButtonComponent.kt
    │       │   │   ├── InputComponent.kt
    │       │   │   ├── ImageComponent.kt
    │       │   │   └── IconComponent.kt
    │       │   │
    │       │   ├── layout/
    │       │   │   ├── ColumnComponent.kt
    │       │   │   ├── RowComponent.kt
    │       │   │   ├── GridComponent.kt
    │       │   │   ├── ScrollComponent.kt
    │       │   │   └── StackComponent.kt
    │       │   │
    │       │   ├── forms/
    │       │   │   ├── CheckboxComponent.kt
    │       │   │   ├── RadioGroupComponent.kt
    │       │   │   ├── DropdownComponent.kt
    │       │   │   ├── SliderComponent.kt
    │       │   │   ├── ToggleComponent.kt
    │       │   │   ├── DatePickerComponent.kt
    │       │   │   ├── TimePickerComponent.kt
    │       │   │   └── ColorPickerComponent.kt
    │       │   │
    │       │   ├── containers/
    │       │   │   ├── CardComponent.kt
    │       │   │   ├── SectionComponent.kt
    │       │   │   ├── GroupComponent.kt
    │       │   │   └── PanelComponent.kt
    │       │   │
    │       │   ├── navigation/
    │       │   │   ├── NavigationComponent.kt
    │       │   │   ├── TabViewComponent.kt
    │       │   │   ├── BottomNavComponent.kt
    │       │   │   └── DrawerComponent.kt
    │       │   │
    │       │   ├── feedback/
    │       │   │   ├── AlertComponent.kt
    │       │   │   ├── ToastComponent.kt
    │       │   │   ├── SnackbarComponent.kt
    │       │   │   ├── ModalComponent.kt
    │       │   │   └── SheetComponent.kt
    │       │   │
    │       │   ├── data/
    │       │   │   ├── ListComponent.kt
    │       │   │   ├── LazyListComponent.kt
    │       │   │   ├── LazyGridComponent.kt
    │       │   │   ├── DataFormComponent.kt
    │       │   │   └── DataListComponent.kt
    │       │   │
    │       │   ├── visual/
    │       │   │   ├── BadgeComponent.kt
    │       │   │   ├── ChipComponent.kt
    │       │   │   ├── AvatarComponent.kt
    │       │   │   ├── ProgressComponent.kt
    │       │   │   ├── LoadingComponent.kt
    │       │   │   └── DividerComponent.kt
    │       │   │
    │       │   └── spatial/            ← 3D/Spatial components
    │       │       ├── SpatialButtonComponent.kt
    │       │       ├── SpatialCardComponent.kt
    │       │       ├── SpatialContainerComponent.kt
    │       │       └── VolumetricWindowComponent.kt
    │       │
    │       ├── integration/            ← VOS4 Integration
    │       │   ├── UUIDIntegration.kt  ← UUIDCreator hookup (300 lines)
    │       │   ├── CommandIntegration.kt ← CommandManager hookup (250 lines)
    │       │   ├── HUDIntegration.kt   ← HUDManager hookup (150 lines)
    │       │   ├── LocalizationIntegration.kt ← i18n (200 lines)
    │       │   ├── RoomIntegration.kt  ← Database integration (400 lines)
    │       │   └── VOS4Services.kt     ← Service access layer (100 lines)
    │       │
    │       ├── theme/                  ← Theme System
    │       │   ├── ThemeEngine.kt      ← Theme manager (400 lines)
    │       │   ├── ThemeDetector.kt    ← Host theme detection (200 lines)
    │       │   ├── ThemeMaker.kt       ← Visual theme designer (600 lines)
    │       │   │
    │       │   ├── themes/
    │       │   │   ├── GlassMorphismTheme.kt  (300 lines)
    │       │   │   ├── LiquidUITheme.kt       (350 lines)
    │       │   │   ├── NeumorphismTheme.kt    (300 lines)
    │       │   │   ├── Material3Theme.kt      (200 lines)
    │       │   │   ├── MaterialYouTheme.kt    (250 lines)
    │       │   │   ├── SamsungOneUITheme.kt   (300 lines)
    │       │   │   ├── PixelUITheme.kt        (250 lines)
    │       │   │   └── VOS4DefaultTheme.kt    (200 lines)
    │       │   │
    │       │   ├── effects/
    │       │   │   ├── GlassEffect.kt         (200 lines)
    │       │   │   ├── LiquidEffect.kt        (250 lines)
    │       │   │   ├── NeumorphicEffect.kt    (200 lines)
    │       │   │   └── BlurEffect.kt          (150 lines)
    │       │   │
    │       │   └── models/
    │       │       ├── ThemeConfig.kt
    │       │       ├── ColorScheme.kt
    │       │       └── VisualEffect.kt
    │       │
    │       ├── database/               ← Database Auto-Generation
    │       │   ├── MagicDB.kt          ← Database manager (300 lines)
    │       │   ├── EntityScanner.kt    ← Auto-detect entities (250 lines)
    │       │   ├── DaoGenerator.kt     ← Generate DAOs (400 lines)
    │       │   ├── DatabaseBuilder.kt  ← Build Room DB (300 lines)
    │       │   ├── CRUDOperations.kt   ← CRUD helpers (200 lines)
    │       │   └── MigrationHandler.kt ← Schema migrations (200 lines)
    │       │
    │       ├── converter/              ← Code Converter
    │       │   ├── CodeConverter.kt    ← Main converter (200 lines)
    │       │   ├── ComposeParser.kt    ← Parse Compose (500 lines)
    │       │   ├── XMLParser.kt        ← Parse XML (400 lines)
    │       │   ├── ASTAnalyzer.kt      ← AST analysis (300 lines)
    │       │   ├── ComponentMapper.kt  ← Map to MagicUI (400 lines)
    │       │   ├── CodeGenerator.kt    ← Generate MagicUI code (350 lines)
    │       │   └── ConfidenceScorer.kt ← Conversion confidence (150 lines)
    │       │
    │       ├── templates/              ← App Templates
    │       │   ├── TemplateEngine.kt   ← Template processor (300 lines)
    │       │   ├── VoiceAppTemplate.kt ← Voice assistant template
    │       │   ├── TranslatorTemplate.kt ← Translator app template
    │       │   ├── GameTemplate.kt     ← Simple game template
    │       │   ├── CRUDTemplate.kt     ← CRUD app template
    │       │   └── DashboardTemplate.kt ← Dashboard template
    │       │
    │       ├── spatial/                ← 3D/Spatial Support (Optional)
    │       │   ├── FilamentRenderer.kt ← Filament integration (500 lines)
    │       │   ├── SpatialManager.kt   ← 3D positioning (300 lines)
    │       │   ├── ARSupport.kt        ← ARCore integration (400 lines)
    │       │   └── VisionOSCompat.kt   ← VisionOS patterns (250 lines)
    │       │
    │       ├── utils/                  ← Utilities
    │       │   ├── CompositionLocals.kt ← Dependency injection (100 lines)
    │       │   ├── Extensions.kt       ← Kotlin extensions (200 lines)
    │       │   ├── Validators.kt       ← Input validation (150 lines)
    │       │   ├── Animations.kt       ← Animation helpers (200 lines)
    │       │   └── Performance.kt      ← Performance utils (150 lines)
    │       │
    │       ├── models/                 ← Data Models
    │       │   ├── ComponentModels.kt  ← Component definitions
    │       │   ├── ThemeModels.kt      ← Theme configurations
    │       │   ├── StateModels.kt      ← State management
    │       │   └── ErrorModels.kt      ← Error types
    │       │
    │       └── annotations/            ← Annotations
    │           ├── MagicComponent.kt   ← Component annotation
    │           ├── MagicEntity.kt      ← Database entity
    │           ├── MagicTheme.kt       ← Theme annotation
    │           └── MagicPlugin.kt      ← Plugin annotation
    │
    ├── test/                           ← Unit Tests
    │   └── java/com/augmentalis/magicui/
    │       ├── core/
    │       │   ├── MagicUIScopeTest.kt
    │       │   └── StateManagerTest.kt
    │       │
    │       ├── components/
    │       │   ├── TextComponentTest.kt
    │       │   ├── ButtonComponentTest.kt
    │       │   └── InputComponentTest.kt
    │       │
    │       ├── integration/
    │       │   ├── UUIDIntegrationTest.kt
    │       │   └── CommandIntegrationTest.kt
    │       │
    │       └── converter/
    │           ├── ComposeConverterTest.kt
    │           └── XMLConverterTest.kt
    │
    └── androidTest/                    ← Integration/UI Tests
        └── java/com/augmentalis/magicui/
            ├── ui/
            │   ├── ComponentRenderingTest.kt
            │   └── ThemeApplicationTest.kt
            │
            └── integration/
                ├── VOS4IntegrationTest.kt
                └── DatabaseIntegrationTest.kt
```

---

## File Size Estimates

### Core Files (Critical Path)

| File | Lines | Size | Priority | Complexity |
|------|-------|------|----------|------------|
| **MagicUIScope.kt** | 500 | 15KB | P0 | High |
| **MagicScreen.kt** | 200 | 6KB | P0 | Medium |
| **ComponentRegistry.kt** | 150 | 5KB | P0 | Medium |
| **StateManager.kt** | 300 | 9KB | P0 | High |
| **UUIDIntegration.kt** | 300 | 9KB | P0 | High |
| **CommandIntegration.kt** | 250 | 8KB | P0 | High |

**Total Core:** ~1800 lines, ~52KB

### Component Files

| Category | Files | Lines/File | Total Lines | Priority |
|----------|-------|------------|-------------|----------|
| **Basic** | 5 | 100 | 500 | P0 |
| **Layout** | 5 | 150 | 750 | P0 |
| **Forms** | 8 | 120 | 960 | P1 |
| **Containers** | 4 | 100 | 400 | P1 |
| **Navigation** | 4 | 200 | 800 | P2 |
| **Feedback** | 5 | 150 | 750 | P2 |
| **Data** | 5 | 180 | 900 | P2 |
| **Visual** | 6 | 100 | 600 | P2 |
| **Spatial** | 4 | 200 | 800 | P3 |

**Total Components:** ~46 files, ~6,460 lines, ~200KB

### Theme Files

| File | Lines | Size | Priority |
|------|-------|------|----------|
| **ThemeEngine.kt** | 400 | 12KB | P1 |
| **GlassMorphismTheme.kt** | 300 | 9KB | P1 |
| **LiquidUITheme.kt** | 350 | 11KB | P1 |
| **NeumorphismTheme.kt** | 300 | 9KB | P1 |
| **Material3Theme.kt** | 200 | 6KB | P1 |
| **ThemeMaker.kt** | 600 | 18KB | P2 |
| **Theme Effects (4 files)** | 200 each | 24KB total | P1 |

**Total Theme:** ~12 files, ~2,750 lines, ~89KB

### Database Files

| File | Lines | Size | Priority |
|------|-------|------|----------|
| **MagicDB.kt** | 300 | 9KB | P1 |
| **EntityScanner.kt** | 250 | 8KB | P1 |
| **DaoGenerator.kt** | 400 | 12KB | P1 |
| **DatabaseBuilder.kt** | 300 | 9KB | P1 |
| **CRUDOperations.kt** | 200 | 6KB | P2 |
| **MigrationHandler.kt** | 200 | 6KB | P2 |

**Total Database:** 6 files, ~1,650 lines, ~50KB

### Converter Files

| File | Lines | Size | Priority |
|------|-------|------|----------|
| **CodeConverter.kt** | 200 | 6KB | P2 |
| **ComposeParser.kt** | 500 | 15KB | P2 |
| **XMLParser.kt** | 400 | 12KB | P2 |
| **ComponentMapper.kt** | 400 | 12KB | P2 |
| **CodeGenerator.kt** | 350 | 11KB | P2 |

**Total Converter:** 5 files, ~1,850 lines, ~56KB

---

## Complete File Manifest

### Must Create (Phase 1 - Weeks 1-4)

**Priority 0 (Core - Week 1-2):**
1. `build.gradle.kts` - Module configuration
2. `MagicUI.kt` - Main API entry point
3. `MagicUIModule.kt` - Module singleton
4. `core/MagicUIScope.kt` - DSL processor
5. `core/MagicScreen.kt` - Screen wrapper
6. `core/ComponentRegistry.kt` - Component registry
7. `core/StateManager.kt` - State management
8. `integration/UUIDIntegration.kt` - UUID hookup
9. `integration/CommandIntegration.kt` - Command hookup
10. `integration/VOS4Services.kt` - Service access
11. `utils/CompositionLocals.kt` - DI setup

**Priority 0 (Basic Components - Week 3-4):**
12. `components/basic/TextComponent.kt`
13. `components/basic/ButtonComponent.kt`
14. `components/basic/InputComponent.kt`
15. `components/layout/ColumnComponent.kt`
16. `components/layout/RowComponent.kt`

### Should Create (Phase 2 - Weeks 5-12)

**Priority 1 (Forms - Week 5-6):**
17-24. All form components (8 files)

**Priority 1 (Themes - Week 7-8):**
25. `theme/ThemeEngine.kt`
26-30. Built-in themes (5 files)
31-34. Theme effects (4 files)

**Priority 1 (Layout - Week 9-10):**
35-39. Advanced layout components (5 files)

**Priority 1 (Containers - Week 11-12):**
40-43. Container components (4 files)

### Could Create (Phase 3 - Weeks 13-20)

**Priority 2 (Navigation - Week 13-14):**
44-47. Navigation components (4 files)

**Priority 2 (Feedback - Week 15-16):**
48-52. Feedback components (5 files)

**Priority 2 (Data - Week 17-18):**
53-57. Data components (5 files)

**Priority 2 (Database - Week 19-20):**
58-63. Database system (6 files)

### Advanced (Phase 4 - Weeks 21-28)

**Priority 3 (Converter - Week 21-22):**
64-68. Code converter (5 files)

**Priority 3 (Visual - Week 23-24):**
69-74. Visual components (6 files)

**Priority 3 (Spatial - Week 25-26):**
75-78. Spatial components (4 files)

**Priority 3 (Templates - Week 27-28):**
79-83. App templates (5 files)

**Total Files to Create: ~85 source files**

---

## Package Organization

### Package Structure

```kotlin
com.augmentalis.magicui                      ← Root package
├── .core                                    ← Core DSL
│   ├── MagicUIScope
│   ├── MagicScreen
│   ├── ComponentRegistry
│   ├── StateManager
│   └── LifecycleManager
│
├── .components                              ← All UI components
│   ├── .basic
│   ├── .layout
│   ├── .forms
│   ├── .containers
│   ├── .navigation
│   ├── .feedback
│   ├── .data
│   ├── .visual
│   └── .spatial
│
├── .integration                             ← VOS4 Integration
│   ├── UUIDIntegration
│   ├── CommandIntegration
│   ├── HUDIntegration
│   ├── LocalizationIntegration
│   ├── RoomIntegration
│   └── VOS4Services
│
├── .theme                                   ← Theme System
│   ├── ThemeEngine
│   ├── ThemeDetector
│   ├── ThemeMaker
│   ├── .themes (built-in themes)
│   ├── .effects (visual effects)
│   └── .models (theme models)
│
├── .database                                ← Database System
│   ├── MagicDB
│   ├── EntityScanner
│   ├── DaoGenerator
│   ├── DatabaseBuilder
│   ├── CRUDOperations
│   └── MigrationHandler
│
├── .converter                               ← Code Converter
│   ├── CodeConverter
│   ├── ComposeParser
│   ├── XMLParser
│   ├── ComponentMapper
│   └── CodeGenerator
│
├── .templates                               ← App Templates
│   ├── TemplateEngine
│   ├── VoiceAppTemplate
│   ├── TranslatorTemplate
│   ├── GameTemplate
│   └── CRUDTemplate
│
├── .spatial                                 ← 3D/Spatial (Optional)
│   ├── FilamentRenderer
│   ├── SpatialManager
│   ├── ARSupport
│   └── VisionOSCompat
│
├── .utils                                   ← Utilities
│   ├── CompositionLocals
│   ├── Extensions
│   ├── Validators
│   ├── Animations
│   └── Performance
│
├── .models                                  ← Data Models
│   ├── ComponentModels
│   ├── ThemeModels
│   ├── StateModels
│   └── ErrorModels
│
└── .annotations                             ← Annotations
    ├── MagicComponent
    ├── MagicEntity
    ├── MagicTheme
    └── MagicPlugin
```

---

## Build Configuration

### Root build.gradle.kts

```kotlin
// modules/libraries/MagicUI/build.gradle.kts

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")  // For Room code generation
    kotlin("kapt")
}

android {
    namespace = "com.augmentalis.magicui"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        // KSP arguments for Room
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // === VOS4 Integration ===
    implementation(project(":modules:libraries:UUIDCreator"))
    implementation(project(":modules:managers:CommandManager"))
    implementation(project(":modules:managers:HUDManager"))
    implementation(project(":modules:managers:LocalizationManager"))

    // === Room Database ===
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // === Jetpack Compose ===
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    
    // === AndroidX Core ===
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // === Kotlin Coroutines ===
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // === 3D/Spatial (Optional - Phase 3) ===
    // implementation("com.google.android.filament:filament-android:1.51.0")
    // implementation("com.google.ar:core:1.41.0")
    
    // === Code Parsing ===
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.22")
    implementation("org.w3c:dom:2.3.0-jaxb-1.0.6")
    
    // === Testing ===
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.room:room-testing:$roomVersion")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## settings.gradle.kts Update

```kotlin
// Add to root settings.gradle.kts

include(":modules:libraries:MagicUI")
```

---

## AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- No permissions needed - uses host app permissions -->
    
    <application>
        <!-- No activities - library only -->
    </application>
</manifest>
```

---

## ProGuard Rules

### proguard-rules.pro

```proguard
# MagicUI Library ProGuard Rules

# Keep all public API
-keep public class com.augmentalis.magicui.** { public *; }

# Keep annotations
-keep @interface com.augmentalis.magicui.annotations.**
-keep @com.augmentalis.magicui.annotations.** class * { *; }

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep MagicUI components
-keep class com.augmentalis.magicui.components.** { *; }

# Keep theme classes
-keep class com.augmentalis.magicui.theme.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
```

### consumer-rules.pro

```proguard
# Consumer ProGuard Rules
# Applied to apps using MagicUI

# Keep MagicUI DSL methods
-keep class com.augmentalis.magicui.core.MagicUIScope { *; }
-keep class com.augmentalis.magicui.core.MagicScreen { *; }

# Keep generated Room code
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
```

---

## Resource Files

### values/strings.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">MagicUI</string>
    
    <!-- Default labels -->
    <string name="default_button">Button</string>
    <string name="default_input">Input</string>
    <string name="default_save">Save</string>
    <string name="default_cancel">Cancel</string>
    
    <!-- Error messages -->
    <string name="error_invalid_input">Invalid input</string>
    <string name="error_required_field">This field is required</string>
    <string name="error_component_not_found">Component not found</string>
    
    <!-- Voice commands -->
    <string name="voice_click">Click</string>
    <string name="voice_tap">Tap</string>
    <string name="voice_press">Press</string>
    <string name="voice_enter">Enter</string>
    <string name="voice_type">Type</string>
</resources>
```

### values/colors.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Default theme colors -->
    <color name="magic_primary">#6200EE</color>
    <color name="magic_secondary">#03DAC6</color>
    <color name="magic_background">#FFFFFF</color>
    <color name="magic_surface">#FFFFFF</color>
    <color name="magic_error">#B00020</color>
