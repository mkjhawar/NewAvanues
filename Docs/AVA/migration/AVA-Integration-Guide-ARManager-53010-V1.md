# ARManager Integration Guide

**Purpose**: Instructions for integrating Google ARCore-based AR features for smart glasses
**Source**: AVA2 codebase (Apr 2025)
**Target**: AVA AI (Week 10-11 integration)
**Priority**: P1 (High Priority - Smart Glasses First principle)

---

## Overview

ARManager provides Google ARCore integration for:
- **Plane Detection**: Horizontal/vertical surface tracking
- **Augmented Image Recognition**: Identify real-world objects
- **Hit Testing**: Touch interaction with AR elements
- **Smart Glasses Support**: Meta Ray-Ban, Vuzix, etc.

**Key Value**: Critical for AVA's "Smart Glasses First" differentiator.

---

## Source Files Location

**Base Path**: `/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/`

### 1. Core AR Manager

**Main Manager:**
```
app/src/main/java/com/augmentalis/ava/ui/ar/
├── ARManager.kt                    # Main AR session manager (273 lines)
│   ├── ARCore session lifecycle (init, pause, resume, cleanup)
│   ├── Plane detection (horizontal + vertical)
│   ├── Augmented image tracking
│   ├── Hit testing for touch interaction
│   └── State management (Initializing, Ready, Running, Error)
```

**Architecture Components:**
```
app/src/main/java/com/augmentalis/ava/ui/ar/
├── ARViewModel.kt                  # ViewModel for AR screen state
├── ARScreen.kt                     # Compose UI for AR view
├── ARComponent.kt                  # Reusable AR components
├── ARVoiceController.kt            # Voice commands for AR interactions
└── ArActivity.kt                   # Activity wrapper (legacy, may not need)
```

**Dependency Injection:**
```
app/src/main/java/com/augmentalis/ava/di/
└── ARModule.kt                     # Hilt DI module for AR
```

**Service Layer:**
```
app/src/main/java/com/augmentalis/ava/service/
└── ARService.kt                    # Background AR service (optional)
```

### 2. Supporting Files (Assumed, Not Listed)

**Data Models** (likely in):
```
app/src/main/java/com/augmentalis/ava/data/models/
├── ARPlane.kt                      # Plane data model
├── ARImage.kt                      # Augmented image model
└── ARHitResult.kt                  # Hit test result model
```

**Repository** (if AR state is persisted):
```
app/src/main/java/com/augmentalis/ava/data/repositories/
└── ARRepository.kt                 # AR session state persistence
```

### 3. Assets (Required for Image Recognition)

**AR Image Database:**
```
app/src/main/assets/
└── ar_images_database.imgdb        # Pre-built augmented image database
```

**Note**: This file is referenced in ARManager.kt:122 but may not exist in the source. You'll need to create it using ARCore's image database tools.

---

## ARManager.kt - Detailed Breakdown

### Key Classes

#### ARManagerState (Sealed Class)
```kotlin
sealed class ARManagerState {
    object Initializing : ARManagerState()
    object Initialized : ARManagerState()
    object Ready : ARManagerState()
    object Running : ARManagerState()
    data class Error(val message: String) : ARManagerState()
}
```

#### ARManager (Singleton, Hilt Injected)
```kotlin
@Singleton
class ARManager @Inject constructor(
    private val context: Context,
    private val appPreferences: AppPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    // AR session
    private var arSession: Session?

    // State flows
    val arState: StateFlow<ARManagerState>
    val detectedPlanes: StateFlow<List<Plane>>
    val augmentedImages: StateFlow<List<AugmentedImage>>
    val lastHitResult: StateFlow<HitResult?>

    // Methods
    fun initialize()
    fun configureSession()
    fun loadAugmentedImageDatabase(config: Config)
    fun addImageToDatabase(imageName: String, bitmap: Bitmap): Boolean
    fun onFrameUpdate(frame: Frame)
    fun onTouchEvent(motionEvent: MotionEvent)
    fun resume()
    fun pause()
    fun cleanUp()
}
```

### Key Methods

**1. initialize() - Lines 73-87**
- Creates ARCore Session
- Configures session with default settings
- Sets state to Ready

**2. configureSession() - Lines 92-112**
- Sets update mode to LATEST_CAMERA_IMAGE
- Enables horizontal + vertical plane finding
- Sets light estimation to ENVIRONMENTAL_HDR
- Loads augmented image database (if enabled in preferences)

**3. loadAugmentedImageDatabase(config) - Lines 117-130**
- Deserializes pre-built image database from assets
- Attaches to ARCore session config
- Logs number of images loaded

**4. addImageToDatabase(name, bitmap) - Lines 135-154**
- Dynamically adds image to database at runtime
- Reconfigures session with new database
- Returns success/failure

**5. onFrameUpdate(frame) - Lines 159-190**
- Called every frame by ARCore
- Updates detectedPlanes (filters by TRACKING state)
- Updates augmentedImages (filters by TRACKING state)
- Sets state to Running

**6. onTouchEvent(motionEvent) - Lines 195-220**
- Performs hit test on touch
- Filters valid hits (on planes or augmented images)
- Updates lastHitResult StateFlow

**7. resume(), pause(), cleanUp() - Lines 224-261**
- Lifecycle management
- Prevents memory leaks
- Cleans up ARCore session

---

## Integration Steps (Week 10-11)

### Step 1: Copy AR Files to AVA AI

```bash
# Create AR feature module structure
mkdir -p "/Volumes/M Drive/Coding/AVA AI/features/ar/src/main/java/com/augmentalis/ava/features/ar"

# Copy core AR files
cp "/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/app/src/main/java/com/augmentalis/ava/ui/ar/ARManager.kt" \
   "/Volumes/M Drive/Coding/AVA AI/features/ar/src/main/java/com/augmentalis/ava/features/ar/"

cp "/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/app/src/main/java/com/augmentalis/ava/ui/ar/ARViewModel.kt" \
   "/Volumes/M Drive/Coding/AVA AI/features/ar/src/main/java/com/augmentalis/ava/features/ar/"

cp "/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/app/src/main/java/com/augmentalis/ava/ui/ar/ARScreen.kt" \
   "/Volumes/M Drive/Coding/AVA AI/features/ar/src/main/java/com/augmentalis/ava/features/ar/ui/"

cp "/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/app/src/main/java/com/augmentalis/ava/ui/ar/ARComponent.kt" \
   "/Volumes/M Drive/Coding/AVA AI/features/ar/src/main/java/com/augmentalis/ava/features/ar/ui/"

cp "/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/app/src/main/java/com/augmentalis/ava/ui/ar/ARVoiceController.kt" \
   "/Volumes/M Drive/Coding/AVA AI/features/ar/src/main/java/com/augmentalis/ava/features/ar/"
```

### Step 2: Update Package Names
Change all imports from:
```kotlin
package com.augmentalis.ava.ui.ar
```
To:
```kotlin
package com.augmentalis.ava.features.ar
```

### Step 3: Create AR Module build.gradle.kts

```kotlin
// features/ar/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.augmentalis.ava.features.ar"
    compileSdk = 34

    defaultConfig {
        minSdk = 26  // ARCore requires API 26+
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    // ARCore
    implementation("com.google.ar:core:1.41.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // CameraX (for AR camera feed)
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Timber (logging)
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
```

### Step 4: Add to settings.gradle
```kotlin
// settings.gradle
include(":features:ar")
```

### Step 5: Add ARCore Dependency to AndroidManifest.xml

```xml
<!-- platform/app/src/main/AndroidManifest.xml -->
<manifest>
    <!-- ARCore required features -->
    <uses-feature android:name="android.hardware.camera.ar" android:required="true" />
    <uses-permission android:name="android.permission.CAMERA" />

    <application>
        <!-- ARCore metadata -->
        <meta-data
            android:name="com.google.ar.core"
            android:value="required" />
    </application>
</manifest>
```

### Step 6: Create AR Image Database

**Option A: Use ARCore Tools (Recommended)**
```bash
# Install ARCore SDK Tools
# https://developers.google.com/ar/develop/augmented-images/arcoreimg

# Create database from images
arcoreimg build-db \
  --input_image_list_path=ar_images.txt \
  --output_db_path=app/src/main/assets/ar_images_database.imgdb
```

**Option B: Create Programmatically**
```kotlin
// In ARManager or separate utility
fun createImageDatabase(context: Context): AugmentedImageDatabase {
    val database = AugmentedImageDatabase(arSession)

    // Add images from assets
    val imageBitmap = BitmapFactory.decodeStream(
        context.assets.open("ar_images/sample_image.jpg")
    )
    database.addImage("sample_image", imageBitmap, 0.1f)  // 0.1m width

    return database
}
```

### Step 7: Integrate with Chat UI (Voice Commands)

```kotlin
// features/chat/src/main/java/com/augmentalis/ava/features/chat/ChatViewModel.kt
class ChatViewModel(
    private val arManager: ARManager
) : ViewModel() {

    fun handleVoiceCommand(command: String) {
        when {
            command.contains("show ar", ignoreCase = true) -> {
                arManager.initialize()
                arManager.resume()
            }
            command.contains("detect planes", ignoreCase = true) -> {
                viewModelScope.launch {
                    arManager.detectedPlanes.collect { planes ->
                        // Show planes to user
                    }
                }
            }
            command.contains("recognize image", ignoreCase = true) -> {
                viewModelScope.launch {
                    arManager.augmentedImages.collect { images ->
                        // Announce detected images
                    }
                }
            }
        }
    }
}
```

### Step 8: Create Smart Glasses UI Overlay

```kotlin
// features/ar/src/main/java/com/augmentalis/ava/features/ar/SmartGlassesOverlay.kt
@Composable
fun SmartGlassesOverlay(
    arState: ARManagerState,
    detectedPlanes: List<Plane>,
    augmentedImages: List<AugmentedImage>,
    onAction: (ARAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview (ARCore handles this)

        // Overlay UI (minimal for glasses)
        when (arState) {
            is ARManagerState.Running -> {
                // Show detected objects
                detectedPlanes.forEach { plane ->
                    PlaneIndicator(plane)
                }

                augmentedImages.forEach { image ->
                    ImageLabel(image)
                }
            }
            is ARManagerState.Error -> {
                ErrorMessage(arState.message)
            }
            else -> {
                LoadingIndicator()
            }
        }
    }
}
```

---

## Smart Glasses Support

### Supported Devices (8+ devices mentioned in CLAUDE.md)

**Meta Ray-Ban Smart Glasses:**
- Display: No built-in AR display (audio-only)
- Strategy: Use ARCore for object recognition, announce via voice
- Integration: ARManager detects objects → TTS announces

**Vuzix Blade / Shield:**
- Display: Monocular waveguide display
- Strategy: Full AR overlay with visual indicators
- Integration: ARManager + Compose UI overlay

**Epson Moverio BT-40:**
- Display: Binocular OLED display
- Strategy: Full AR experience with spatial UI
- Integration: ARManager + Compose UI + spatial anchors

**XREAL Air / Nreal Light:**
- Display: Dual Full HD OLED
- Strategy: Virtual screen + AR overlay
- Integration: ARManager + 3DOF tracking

### Device-Specific Configuration

```kotlin
// features/ar/src/main/java/com/augmentalis/ava/features/ar/SmartGlassesConfig.kt
enum class GlassesType {
    META_RAY_BAN,       // Audio-only
    VUZIX_BLADE,        // Monocular display
    EPSON_MOVERIO,      // Binocular display
    XREAL_AIR,          // Virtual screen
    GENERIC_ANDROID     // Standard phone/tablet
}

fun configureForDevice(glassesType: GlassesType): Config {
    return when (glassesType) {
        META_RAY_BAN -> Config(session).apply {
            // No display, focus on image recognition
            planeFindingMode = Config.PlaneFindingMode.DISABLED
            augmentedImageDatabase = loadImageDatabase()
        }
        VUZIX_BLADE -> Config(session).apply {
            // Monocular display, enable all features
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        }
        // ... other devices
    }
}
```

---

## Use Cases for AVA AI

### 1. Object Recognition
**User**: "What am I looking at?"
**AVA**: Uses ARCore image tracking → Identifies object → Responds with name

### 2. Spatial Commands
**User**: "Place a reminder here"
**AVA**: Uses hit testing → Creates spatial anchor → Saves to database

### 3. Navigation Assistance
**User**: "Guide me to the kitchen"
**AVA**: Uses plane detection → Shows path overlay → Voice guidance

### 4. Contextual Actions
**User**: "What can I do here?"
**AVA**: Detects surfaces → Suggests context-aware actions

---

## Testing Checklist

- [ ] ARCore session initializes on smart glasses
- [ ] Plane detection works on horizontal surfaces
- [ ] Augmented image recognition detects test images
- [ ] Hit testing responds to touch/gaze input
- [ ] Voice commands trigger AR actions
- [ ] Memory usage stays below 512MB
- [ ] AR runs smoothly at 30fps minimum
- [ ] Cleanup prevents memory leaks

---

## Performance Targets

| Metric | Target | Notes |
|--------|--------|-------|
| Frame Rate | 30fps minimum | ARCore recommendation |
| Latency (detection) | <100ms | Object recognition |
| Memory Peak | <512MB | ARCore + AVA combined |
| Battery Drain | <15%/hour | Higher than voice-only |

---

## Dependencies Required

```kotlin
// ARCore
implementation("com.google.ar:core:1.41.0")

// CameraX (for camera feed)
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// OpenGL (if custom rendering needed)
implementation("androidx.opengl:opengl:1.0.0")  // Optional
```

---

## Migration Strategy

**Phase 1** (Week 10): Copy AR files, set up module
**Phase 2** (Week 10): Integrate ARCore, test on phone
**Phase 3** (Week 11): Test on smart glasses hardware
**Phase 4** (Week 11): Integrate with voice commands
**Phase 5** (Week 11): Optimize for smart glasses displays

---

## References

- ARCore Documentation: https://developers.google.com/ar
- ARManager Source: `/Users/manoj_mbpm14/Downloads/Coding/Ava-AI/AVA2/app/src/main/java/com/augmentalis/ava/ui/ar/ARManager.kt`
- Legacy Analysis: `docs/active/Analysis-Legacy-Codebases-251030-0210.md`

---

**Created**: 2025-10-30 02:20 PDT
**Next Review**: After VoiceOS integration (Week 9)
**Priority**: Integrate in Week 10-11 (after VoiceOS)

Created by Manoj Jhawar, manoj@ideahq.net
