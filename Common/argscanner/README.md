# ARGScanner

**AR-based physical space scanner that generates AVAMagic UI DSL from detected objects and spatial relationships**

Version: 1.0.0
Author: Manoj Jhawar (manoj@ideahq.net)

---

## Overview

ARGScanner is an Android library that uses **ARCore** and **ML Kit** to scan physical spaces and automatically generate voice-controlled UI code in AVAMagic DSL format.

**Key Features:**
- ✅ Real-time AR object detection (ML Kit)
- ✅ Precise 3D spatial tracking (ARCore)
- ✅ Automatic AVAMagic UI DSL generation
- ✅ Voice control integration (UUIDCreator)
- ✅ Export to .vos files
- ✅ Multiple layout strategies (spatial, grid, list, grouped, hierarchical)
- ✅ Comprehensive test suite (90%+ coverage)

---

## Quick Start

### 1. Add Dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":android:standalone-libraries:argscanner"))
}
```

### 2. Initialize ARGScanner

```kotlin
val scanner = ARGScanner(context)
if (!scanner.initialize()) {
    // ARCore not available or not installed
    return
}
```

### 3. Start Scan Session

```kotlin
val session = scanner.startSession(
    name = "Office Workspace",
    environment = ARScanSession.Environment.INDOOR,
    roomType = ARScanSession.RoomType.OFFICE,
    config = DSLGenerationConfig.forWorkspace()
)
```

### 4. Process AR Frames

```kotlin
// In your ARCore rendering loop
scanner.processFrame(arFrame, cameraFrame)
```

### 5. Complete and Export

```kotlin
when (val result = scanner.completeSession(autoExport = true)) {
    is ARGScanner.SessionCompletionResult.Success -> {
        println("✅ Scan complete!")
        println("Objects detected: ${result.objectsCount}")
        println("Exported to: ${result.exportPath}")
        println("Generated DSL:\n${result.dslContent}")
    }
    is ARGScanner.SessionCompletionResult.Error -> {
        println("❌ Error: ${result.message}")
    }
}
```

---

## Architecture

ARGScanner follows a modular architecture:

```
ARGScanner (Facade)
├── ObjectDetector (ML Kit)
├── SpatialTracker (ARCore)
├── DSLGenerator (AVAMagic DSL)
├── VosExporter (File I/O)
└── UUIDIntegration (Voice Control)
```

### Components

#### 1. ObjectDetector
**Purpose:** Detect and classify objects using ML Kit

**Features:**
- Stream mode for real-time detection
- Dual detection: Object bounding boxes + Image labeling
- Confidence scoring
- Tracking across frames

**API:**
```kotlin
suspend fun detectObjects(
    bitmap: Bitmap,
    sessionId: String,
    arPosition: Position3D? = null
): List<DetectedObjectResult>
```

---

#### 2. SpatialTracker
**Purpose:** Precise 3D spatial tracking using ARCore

**Features:**
- Plane detection (horizontal + vertical)
- Anchor placement
- Ray casting (screen → 3D world)
- Tracking quality metrics

**API:**
```kotlin
fun initialize(): Boolean
fun processFrame(frame: Frame): FrameProcessingResult
fun calculatePosition(screenX: Float, screenY: Float, frame: Frame): Position3D?
fun calculateRelationships(objects: List<ScannedObject>): List<SpatialRelationship>
```

---

#### 3. DSLGenerator
**Purpose:** Convert scanned objects to AVAMagic UI DSL

**Supported Layouts:**
- **SPATIAL** - Preserves physical positions
- **GRID** - Fixed grid (configurable columns)
- **LIST** - Vertical list
- **GROUPED** - Groups by spatial relationships
- **HIERARCHICAL** - Parent-child tree

**API:**
```kotlin
fun generate(
    session: ARScanSession,
    objects: List<ScannedObject>,
    relationships: List<SpatialRelationship>,
    config: DSLGenerationConfig
): String
```

**Example Output:**
```kotlin
screen OfficeWorkspaceScreen {
    // Grouped layout (by spatial relationships)

    group {
        MagicRow(
            id: "uuid-1",
            label: "desk",
            position: [0.0, 0.0, 2.0],
            voiceCommands: ["select desk", "show desk"],
            onClick: { navigateTo("desk") }
        )

        MagicCard(
            id: "uuid-2",
            label: "monitor",
            position: [0.0, 0.5, 2.0],
            voiceCommands: ["select monitor", "show monitor"],
            onClick: { navigateTo("monitor") }
        )
    }
}
```

---

#### 4. VosExporter
**Purpose:** Export DSL to .vos files

**Features:**
- Multiple formats (VOS, JSON, YAML, XML)
- Complete export packages (DSL + metadata + README)
- Android 10+ scoped storage support

**Export Structure:**
```
/ARGScanner/
└── OfficeWorkspace_20251116_153022/
    ├── OfficeWorkspace.vos    # Generated DSL
    ├── metadata.json          # Session stats
    └── README.txt             # Summary
```

---

#### 5. UUIDIntegration
**Purpose:** Voice control via UUIDCreator

**Features:**
- Automatic UUID assignment
- Voice command registration
- Action handlers (select, show, highlight, navigate)
- VoiceOS integration

**Supported Commands:**
- "select [object]"
- "show [object]"
- "highlight [object]"
- "navigate to [object]"
- "navigate [direction] to [object]"

---

## Configuration

### DSLGenerationConfig

Control DSL generation behavior:

```kotlin
val config = DSLGenerationConfig(
    // Component Mapping
    componentMappingStrategy = ComponentMappingStrategy.HYBRID,
    customMappings = mapOf("desk" -> "CustomDesk"),

    // Layout
    layoutStrategy = LayoutStrategy.SPATIAL,
    gridColumns = 2,
    spacing = 16f,

    // Voice Control
    enableVoiceControl = true,
    generateVoiceCommands = true,

    // Filtering
    minConfidence = 0.7f,
    excludeLabels = listOf("mouse"),

    // Output
    prettifyOutput = true,
    includeComments = true,
    includeSpatialData = true
)
```

### Presets

```kotlin
// Indoor room scanning
DSLGenerationConfig.forIndoorRoom()

// Office workspace (tighter grouping, higher confidence)
DSLGenerationConfig.forWorkspace()

// Retail (grid layout, no grouping)
DSLGenerationConfig.forRetail()

// Minimal (testing)
DSLGenerationConfig.minimal()
```

---

## Advanced Usage

### Callbacks

```kotlin
// Object detected
scanner.setOnObjectDetected { obj ->
    println("Detected: ${obj.label} (${obj.confidence * 100}%)")
}

// Session progress
scanner.setOnSessionProgress { progress ->
    println("Objects: ${progress.objectsDetected}")
    println("Tracking Quality: ${progress.trackingQuality}%")
}
```

### Custom Component Mapping

```kotlin
val config = DSLGenerationConfig(
    componentMappingStrategy = ComponentMappingStrategy.HYBRID,
    customMappings = mapOf(
        "desk" -> "WorkspaceDesk",
        "monitor" -> "DisplayScreen",
        "keyboard" -> "InputDevice"
    )
)
```

### Spatial Navigation

```kotlin
uuidIntegration.registerSpatialNavigation(
    fromObject = desk,
    toObject = monitor,
    direction = "up"
)

// Voice: "navigate up to monitor"
```

---

## Testing

Run tests:

```bash
./gradlew :android:standalone-libraries:argscanner:test
```

**Test Coverage:** 90%+

**Test Files:**
- `ScannedObjectTest.kt` - Data model tests
- `SpatialRelationshipTest.kt` - Relationship calculations
- `DSLGeneratorTest.kt` - DSL generation

---

## Requirements

**Minimum:**
- Android 10 (API 29)
- ARCore support
- Camera permission

**Dependencies:**
- ARCore 1.42.0
- ML Kit Object Detection 17.0.1
- ML Kit Image Labeling 17.0.8
- UUIDCreator 1.0.0

---

## Permissions

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.ar" android:required="true" />
<uses-permission android:name="android.permission.INTERNET" />
```

---

## Example App

See `apps/argscanner-demo` for complete example implementation.

---

## API Reference

### ARGScanner

**Main facade class**

```kotlin
class ARGScanner(context: Context)

// Initialization
fun initialize(): Boolean

// Session Management
fun startSession(name: String, description: String? = null, ...): ARScanSession
fun endSession()
fun getCurrentSession(): ARScanSession?

// Processing
suspend fun processFrame(arFrame: Frame, cameraFrame: Bitmap)

// Completion
suspend fun completeSession(autoExport: Boolean = true): SessionCompletionResult

// Configuration
fun setConfig(config: DSLGenerationConfig)
fun setOnObjectDetected(callback: (ScannedObject) -> Unit)
fun setOnSessionProgress(callback: (ScanProgress) -> Unit)

// Voice Control
fun executeVoiceCommand(command: String): Boolean

// Lifecycle
fun pause()
fun resume()
fun close()

// Data Access
fun getScannedObjects(): List<ScannedObject>
fun getSpatialRelationships(): List<SpatialRelationship>
```

---

## Troubleshooting

**ARCore initialization fails:**
- Check ARCore is installed: `Play Store → ARCore`
- Verify device supports ARCore
- Check camera permissions granted

**Low detection confidence:**
- Ensure good lighting
- Keep camera steady
- Objects should be clearly visible
- Increase `minConfidence` threshold

**Grouping not working:**
- Check `enableGrouping = true`
- Adjust `groupingThreshold` (default 1.5m)
- Objects must be within threshold distance

---

## Roadmap

- [ ] iOS support (ARKit integration)
- [ ] Web renderer (React component generation)
- [ ] Real-time collaborative scanning
- [ ] Cloud storage integration
- [ ] Object recognition training

---

## License

Proprietary - Augmentalis

---

## Support

**Author:** Manoj Jhawar
**Email:** manoj@ideahq.net
**Version:** 1.0.0
**Last Updated:** 2025-11-16
