# IDEAMagic AR/MR/XR Capabilities Specification

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net

**Purpose:** Define 3D/AR/MR/XR capabilities for IDEAMagic without becoming a game engine.

---

## Executive Summary

**Goal:** Add **limited 3D/AR/MR/XR capabilities** for spatial computing use cases WITHOUT competing with game engines.

**Strategy:** **UI-First 3D** - Focus on AR overlays, spatial UI, and 3D visualizations for apps, not games.

**Target Use Cases:**
- **AR Navigation** (NavAR app) - Turn-by-turn directions with AR overlays
- **AR Shopping** - Product visualization in real space
- **AR Spatial UI** - 3D menus, floating panels, spatial interfaces
- **Data Visualization** - 3D charts, graphs, models
- **Interior Design** - Furniture placement, room visualization
- **Education** - 3D models for learning (anatomy, architecture, etc.)

**NOT Target:**
- 3D games (use Unity/Unreal)
- Complex physics simulations
- High-end graphics (AAA games)

---

## Part 1: Technology Stack

### 1.1 Core 3D Graphics

**Platform-Specific Rendering:**

| Platform | 3D API | AR Framework | Status |
|----------|--------|--------------|--------|
| **Android** | OpenGL ES 3.0+ | ARCore | ✅ Supported |
| **iOS** | Metal | ARKit | ✅ Supported |
| **Web** | WebGL 2.0 | WebXR | ✅ Supported |
| **Desktop** | OpenGL 4.x / Vulkan | N/A | 🚧 Planned |

**Unified Abstraction Layer:**
```kotlin
// commonMain - Platform-agnostic 3D API
expect class Renderer3D {
    fun initialize(config: RenderConfig)
    fun renderScene(scene: Scene3D)
    fun renderOverlay(overlay: AROverlay)
    fun shutdown()
}

// androidMain - OpenGL ES implementation
actual class Renderer3D {
    private val gl: GLES30
    actual fun renderScene(scene: Scene3D) {
        // OpenGL ES rendering
    }
}

// iosMain - Metal implementation
actual class Renderer3D {
    private val device: MTLDevice
    actual fun renderScene(scene: Scene3D) {
        // Metal rendering
    }
}

// jsMain - WebGL implementation
actual class Renderer3D {
    private val gl: WebGLRenderingContext
    actual fun renderScene(scene: Scene3D) {
        // WebGL rendering
    }
}
```

---

### 1.2 AR Platform Integration

**ARCore (Android)**
```kotlin
// androidMain
class ARCoreAdapter(private val activity: Activity) {
    private lateinit var arSession: Session

    fun initialize() {
        arSession = Session(activity)
        val config = Config(arSession)
        config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
        arSession.configure(config)
    }

    fun update(): ARFrame {
        val frame = arSession.update()
        return ARFrame(
            camera = frame.camera.toCommonCamera(),
            planes = frame.getUpdatedTrackables(Plane::class.java).toList(),
            anchors = frame.updatedAnchors.toList(),
            lightEstimate = frame.lightEstimate
        )
    }
}
```

**ARKit (iOS)**
```kotlin
// iosMain
class ARKitAdapter {
    private val arSession = ARSession()

    fun initialize() {
        val configuration = ARWorldTrackingConfiguration()
        configuration.planeDetection = [.horizontal, .vertical]
        configuration.environmentTexturing = .automatic
        arSession.run(configuration)
    }

    fun update(): ARFrame {
        guard let frame = arSession.currentFrame else { return nil }
        return ARFrame(
            camera = frame.camera.toCommonCamera(),
            planes = frame.anchors.compactMap { $0 as? ARPlaneAnchor },
            lightEstimate = frame.lightEstimate
        )
    }
}
```

**WebXR (Web)**
```kotlin
// jsMain
class WebXRAdapter {
    private var xrSession: XRSession? = null

    suspend fun initialize() {
        val navigator = window.navigator as NavigatorXR
        xrSession = navigator.xr?.requestSession("immersive-ar")
    }

    fun update(): ARFrame {
        val frame = xrSession?.requestAnimationFrame { time, xrFrame ->
            return ARFrame(
                camera = xrFrame.getViewerPose().toCommonCamera(),
                planes = xrFrame.detectedPlanes.toList()
            )
        }
    }
}
```

---

## Part 2: IDEAMagic AR Components

### 2.1 AR-Specific Components (12 New Components)

**AR Foundation (4 components):**
1. **ARScene** - Root container for AR content
2. **ARCamera** - Camera feed with pose tracking
3. **ARPlane** - Detected surfaces (floor, walls, tables)
4. **ARLight** - Light estimation for realistic rendering

**AR Objects (4 components):**
5. **AR3DModel** - Load and display 3D models (GLTF/GLB)
6. **ARImage** - 2D images in 3D space
7. **ARText** - 3D text labels
8. **ARVideo** - Video textures in 3D space

**AR Interactions (4 components):**
9. **ARRaycaster** - Touch/gaze raycasting
10. **ARGestures** - Pan, pinch, rotate in 3D space
11. **ARPortal** - View into different environment
12. **ARMeasure** - Measure real-world distances

---

### 2.2 Component Specifications

#### Component 1: ARScene

**Purpose:** Root container for all AR content

**DSL Example:**
```dsl
#!vos:D
App {
  id: "com.example.arnav"
  name: "AR Navigation"

  ARScene {
    id: "ar_scene"
    mode: "WorldTracking"  # or ImageTracking, FaceTracking
    planeDetection: ["horizontal", "vertical"]
    lightEstimation: true

    # AR Camera (auto-managed)
    ARCamera {
      id: "ar_camera"
      position: { x: 0, y: 0, z: 0 }
    }

    # 3D arrow showing direction
    AR3DModel {
      id: "nav_arrow"
      model: "assets/arrow.glb"
      position: { x: 0, y: 0, z: -2 }  # 2 meters ahead
      rotation: { x: 0, y: navigationHeading, z: 0 }
      scale: { x: 1, y: 1, z: 1 }

      onTap: () => {
        showDirectionDetails()
      }
    }

    # Text label showing distance
    ARText {
      id: "distance_label"
      text: "200m ahead"
      position: { x: 0, y: 1.5, z: -2 }
      fontSize: 0.2  # 20cm tall text
      color: "#FFFFFF"
      backgroundColor: "#007AFF"
      billboarding: true  # Always face camera
    }

    # Place marker on detected floor
    ARPlane {
      id: "floor_plane"
      type: "horizontal"

      onDetected: (plane) => {
        placeDestinationMarker(plane)
      }
    }
  }

  # Voice commands for AR
  VoiceCommands {
    "show route" => "nav_arrow.show"
    "hide route" => "nav_arrow.hide"
    "zoom in" => "ar_camera.zoomIn"
  }
}
```

**Generated Kotlin (Android + ARCore):**
```kotlin
@Composable
fun ARNavigationScreen() {
    var arSession: Session? by remember { mutableStateOf(null) }
    var navigationHeading by remember { mutableStateOf(0f) }

    ARSceneView(
        modifier = Modifier.fillMaxSize(),
        onSessionCreated = { session ->
            arSession = session
            // Configure ARCore
            val config = Config(session)
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            session.configure(config)
        },
        onFrame = { frame ->
            // Update AR content
            frame.render3DModel(
                modelPath = "arrow.glb",
                position = Vector3(0f, 0f, -2f),
                rotation = Vector3(0f, navigationHeading, 0f)
            )
            frame.renderText(
                text = "200m ahead",
                position = Vector3(0f, 1.5f, -2f),
                billboarding = true
            )
        }
    )
}
```

---

#### Component 2: AR3DModel

**Purpose:** Load and display 3D models in AR space

**Properties:**
```kotlin
data class AR3DModel(
    val id: String,
    var model: String,  // Path to GLTF/GLB file
    var position: Vector3 = Vector3(0f, 0f, 0f),
    var rotation: Vector3 = Vector3(0f, 0f, 0f),
    var scale: Vector3 = Vector3(1f, 1f, 1f),
    var visible: Boolean = true,
    var castShadows: Boolean = true,
    var receiveShadows: Boolean = true,
    var animations: List<String> = emptyList(),
    var currentAnimation: String? = null,

    // Interactions
    var draggable: Boolean = false,
    var scalable: Boolean = false,
    var rotatable: Boolean = false,

    // Callbacks
    var onTap: (() -> Unit)? = null,
    var onLongPress: (() -> Unit)? = null,
    var onDrag: ((Vector3) -> Unit)? = null
) : ARComponent
```

**DSL Example:**
```dsl
AR3DModel {
  id: "furniture_chair"
  model: "assets/chair.glb"
  position: { x: 0, y: 0, z: -1 }  # 1 meter in front
  scale: { x: 1, y: 1, z: 1 }
  draggable: true
  rotatable: true
  castShadows: true

  onTap: () => {
    showProductDetails("chair")
  }

  onDrag: (newPosition) => {
    checkPlacement(newPosition)
  }
}
```

---

#### Component 3: ARRaycaster

**Purpose:** Raycast from touch/gaze to place objects in AR

**DSL Example:**
```dsl
ARRaycaster {
  id: "placement_ray"
  type: "touch"  # or "gaze", "controller"

  onHitPlane: (hitPoint, plane) => {
    placeObject(hitPoint)
    showPlacementIndicator(hitPoint)
  }

  onHitObject: (hitPoint, object) => {
    selectObject(object)
  }
}
```

**Generated Code:**
```kotlin
// Android ARCore raycasting
fun performRaycast(frame: Frame, x: Float, y: Float) {
    val hits = frame.hitTest(x, y)
    for (hit in hits) {
        val trackable = hit.trackable
        if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
            val hitPoint = Vector3(
                hit.hitPose.tx(),
                hit.hitPose.ty(),
                hit.hitPose.tz()
            )
            onHitPlane(hitPoint, trackable)
        }
    }
}
```

---

### 2.3 3D Visualization Components (6 New Components)

**For Data Visualization (NOT games):**

13. **Chart3D** - 3D bar/line/pie charts
14. **Graph3D** - 3D node graphs
15. **Model3DViewer** - Rotate/zoom 3D models
16. **PointCloud** - Display 3D point clouds
17. **Mesh3D** - Custom 3D meshes
18. **Texture3D** - Apply textures to 3D objects

**Example: 3D Chart**
```dsl
Chart3D {
  id: "sales_chart"
  type: "bar"
  data: salesData
  position: { x: 0, y: 0, z: -2 }
  scale: { x: 1, y: 1, z: 1 }

  xAxis: { label: "Month", values: months }
  yAxis: { label: "Revenue", values: revenue }
  zAxis: { label: "Region", values: regions }

  onBarTap: (bar) => {
    showDetails(bar.data)
  }
}
```

---

## Part 3: Implementation Plan

### Phase 1: AR Foundation (Weeks 13-16, 4 weeks, 160 hours)

**Week 13: Platform Adapters**
- ARCore adapter (Android) - 40h
- ARKit adapter (iOS) - 40h

**Week 14: WebXR adapter (Web) - 40h**

**Week 15: AR Components (4 foundation components)**
- ARScene - 10h
- ARCamera - 10h
- ARPlane - 10h
- ARLight - 10h

**Week 16: Testing & Documentation - 40h**

**Deliverables:**
- 3 platform adapters (Android/iOS/Web)
- 4 AR foundation components
- 40+ tests, 80%+ coverage

---

### Phase 2: AR Objects (Weeks 17-18, 2 weeks, 80 hours)

**Week 17: 3D Model Loading**
- GLTF/GLB parser - 20h
- AR3DModel component - 20h

**Week 18: AR Content Components**
- ARImage - 10h
- ARText - 15h
- ARVideo - 15h

**Deliverables:**
- 4 AR object components
- GLTF/GLB support
- 20+ tests

---

### Phase 3: AR Interactions (Weeks 19-20, 2 weeks, 80 hours)

**Week 19: Raycasting & Gestures**
- ARRaycaster - 20h
- ARGestures - 20h

**Week 20: Advanced Features**
- ARPortal - 20h
- ARMeasure - 20h

**Deliverables:**
- 4 AR interaction components
- Gesture recognition
- 20+ tests

---

### Phase 4: 3D Visualization (Weeks 21-24, 4 weeks, 160 hours)

**Week 21-22: Chart Components**
- Chart3D (bar, line, pie) - 40h
- Graph3D - 40h

**Week 23-24: Model Viewer & Advanced**
- Model3DViewer - 40h
- PointCloud, Mesh3D, Texture3D - 40h

**Deliverables:**
- 6 3D visualization components
- Non-game 3D use cases
- 30+ tests

---

## Part 4: Example Apps

### NavAR - AR Navigation App

**Features:**
- Turn-by-turn AR directions
- 3D arrows floating in space
- Distance labels
- POI markers
- Voice-guided navigation

**DSL:**
```dsl
#!vos:D
App {
  id: "com.avanue.navar"
  name: "NavAR"

  ARScene {
    id: "nav_scene"
    mode: "WorldTracking"
    planeDetection: ["horizontal"]

    # Direction arrow
    AR3DModel {
      id: "arrow"
      model: "assets/arrow.glb"
      position: { x: 0, y: 0, z: -3 }
      rotation: { x: 0, y: heading, z: 0 }
      animations: ["pulse"]
    }

    # Distance text
    ARText {
      id: "distance"
      text: "${distanceToTurn}m"
      position: { x: 0, y: 1.5, z: -3 }
      billboarding: true
    }

    # POI markers
    forEach(poi in nearbyPOIs) {
      AR3DModel {
        model: "assets/marker.glb"
        position: poi.position
        onTap: () => showPOIDetails(poi)
      }
    }
  }

  VoiceCommands {
    "where am i" => "showCurrentLocation()"
    "how far" => "announceDistance()"
    "show route" => "arrow.show"
  }
}
```

---

### AR Shopping - Product Visualization

**Features:**
- Place furniture in room
- Real-time shadows
- Scale/rotate products
- Product info overlays

**DSL:**
```dsl
ARScene {
  id: "shopping_scene"

  ARPlane {
    type: "horizontal"
    onDetected: (plane) => {
      enablePlacement(plane)
    }
  }

  AR3DModel {
    id: "product"
    model: selectedProduct.modelUrl
    draggable: true
    rotatable: true
    castShadows: true

    onTap: () => {
      showProductInfo(selectedProduct)
    }
  }

  ARText {
    text: selectedProduct.name
    position: { x: 0, y: 2, z: 0 }
    billboarding: true
  }
}
```

---

## Part 5: Competitive Analysis

### IDEAMagic vs AR Frameworks

| Feature | IDEAMagic AR | ARCore/ARKit | Unity AR Foundation | WebXR |
|---------|--------------|--------------|-------------------|-------|
| **Target** | App AR overlays | Platform AR | Game AR | Web AR |
| **UI Focus** | ✅ Primary | ⚠️ Secondary | ⚠️ Secondary | ✅ Primary |
| **Cross-Platform** | ✅ Android/iOS/Web | ❌ Platform-specific | ⚠️ Unity-only | ✅ Web-only |
| **DSL** | ✅ AvaUI DSL | ❌ Code-only | ❌ Code-only | ❌ Code-only |
| **Voice Integration** | ✅ VoiceOS | ❌ No | ❌ No | ❌ No |
| **Game Engine** | ❌ No (by design) | ❌ No | ✅ Yes (Unity) | ❌ No |
| **Learning Curve** | ⭐⭐⭐⭐⭐ Easy | ⭐⭐⭐ Medium | ⭐⭐ Hard | ⭐⭐⭐⭐ Easy |

**IDEAMagic AR Advantages:**
1. 🏆 **UI-First AR** - Focus on AR overlays for apps, not games
2. ✅ **Cross-Platform** - One DSL, three platforms (Android/iOS/Web)
3. ✅ **Voice Integration** - AR + voice commands (unique!)
4. ✅ **Easy DSL** - No OpenGL knowledge required
5. ✅ **App Store Compliant** - DSL interpreted as data

**What IDEAMagic AR Does NOT Have (vs Unity):**
- ❌ Complex physics engine
- ❌ Advanced lighting/shadows
- ❌ High-end graphics (AAA quality)
- ❌ Game-specific features (AI, pathfinding, etc.)

**Why That's OK:**
- Different target market (apps vs games)
- Simpler, easier to use
- Faster development
- Lower cost

---

## Part 6: Implementation Timeline

### Summary: AR/MR/XR Capabilities (12 weeks)

| Phase | Weeks | Hours | Deliverables |
|-------|-------|-------|--------------|
| **Phase 1: AR Foundation** | 13-16 | 160h | 3 platform adapters, 4 AR components |
| **Phase 2: AR Objects** | 17-18 | 80h | 4 AR object components, GLTF support |
| **Phase 3: AR Interactions** | 19-20 | 80h | 4 AR interaction components |
| **Phase 4: 3D Visualization** | 21-24 | 160h | 6 3D viz components |
| **Total** | **12 weeks** | **480h** | **18 AR/3D components** |

### Combined Timeline (with previous 12-week plan)

| Weeks | Focus | Result |
|-------|-------|--------|
| 1-2 | VoiceOSBridge | ✅ Voice integration working |
| 3-4 | iOS Renderer | ✅ iOS platform complete |
| 5-12 | 25 Common Components | ✅ 73 total components |
| **13-24** | **AR/MR/XR Capabilities** | **✅ 18 AR components, 3 platforms** |

**Total: 24 weeks (6 months) to full AR/MR/XR support!**

---

## Part 7: Effort & Cost Estimates

### Effort Breakdown

**AR Foundation (4 weeks):**
- Platform adapters: 120h
- AR components: 40h
- Total: 160h

**AR Objects (2 weeks):**
- 3D model loading: 40h
- AR content components: 40h
- Total: 80h

**AR Interactions (2 weeks):**
- Raycasting & gestures: 40h
- Advanced features: 40h
- Total: 80h

**3D Visualization (4 weeks):**
- Charts & graphs: 80h
- Model viewer & advanced: 80h
- Total: 160h

**Grand Total: 480 hours (12 weeks @ 40h/week)**

### Cost Estimate

**At $150/hour:**
- 480 hours × $150 = **$72,000**

**At $200/hour (senior dev):**
- 480 hours × $200 = **$96,000**

**Estimated Range: $72K - $96K**

---

## Conclusion

### What We Get

**After 24 Weeks (6 months):**
1. ✅ **VoiceOSBridge** - Voice integration working
2. ✅ **iOS Platform** - Complete iOS support
3. ✅ **73 Components** - Strong UI library
4. ✅ **18 AR/3D Components** - Full AR/MR/XR support
5. ✅ **3 AR Platforms** - Android (ARCore), iOS (ARKit), Web (WebXR)

**Competitive Position:**
- ✅ PARITY with Flutter/Swift (UI components)
- ✅ SUPERIOR in voice integration (VoiceOS)
- ✅ SUPERIOR in AR/MR/XR (cross-platform AR with DSL)
- ✅ DIFFERENTIATED from Unity (app AR, not game AR)

### What Makes IDEAMagic AR Unique

1. 🏆 **UI-First AR** - AR overlays for apps, not games
2. 🏆 **Cross-Platform AR DSL** - One DSL, three AR platforms
3. 🏆 **Voice + AR** - AR commands via VoiceOS (no competitor has this!)
4. ✅ **No OpenGL Knowledge Required** - DSL abstracts complexity
5. ✅ **App Store Compliant** - DSL = data (not code)

### Recommendation

**DO implement AR/MR/XR capabilities as specified:**
- Focused on app use cases (navigation, shopping, visualization)
- NOT a game engine (Unity handles that)
- Leverages IDEAMagic's strengths (DSL, voice, cross-platform)
- Differentiates from all competitors

**Timeline:** 24 weeks (6 months) total
- Weeks 1-12: Core platform parity
- Weeks 13-24: AR/MR/XR capabilities

**Result:** **Leader in voice-first, AR-enabled, cross-platform app development!**

---

**Created by Manoj Jhawar, manoj@ideahq.net**
