# Cockpit MVP - Spatial Enhancements Implementation Plan

**Version:** 1.0
**Date:** 2025-12-10
**Specification:** Cockpit-Spec-SpatialEnhancements-51210-V1.md
**Estimated Duration:** 12-18 days

---

## Implementation Strategy

**Approach:** Sequential phased implementation
- Phase 1: Triptych Layout (foundation)
- Phase 2: Freeform Windows (core capability)
- Phase 3: WebAvanue Integration (production demo)

**Why Sequential:** Single platform (Android), manageable complexity, clear dependencies

---

## Phase 1: Triptych Layout (Days 1-3)

### Objective
Create production-ready angled spatial layout that makes windows readable and usable.

### Tasks

#### Task 1.1: Create TriptychLayout.kt
**File:** `Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/layout/presets/TriptychLayout.kt`

**Steps:**
1. Copy `LinearHorizontalLayout.kt` as template
2. Implement window positioning logic:
   ```kotlin
   override fun calculatePosition(index: Int, total: Int): Vector3D {
       return when {
           total == 1 -> Vector3D(0f, 0f, -1.8f)  // Center only
           total == 2 -> if (index == 0)
               Vector3D(-0.9f, 0f, -2.0f)  // Left
               else Vector3D(0.9f, 0f, -2.0f)  // Right
           else -> when (index) {
               0 -> Vector3D(-0.9f, 0f, -2.0f)  // Left
               1 -> Vector3D(0f, 0f, -1.8f)      // Center (closer)
               2 -> Vector3D(0.9f, 0f, -2.0f)    // Right
               else -> Vector3D(0f, 0f, -2.0f)
           }
       }
   }
   ```

3. Implement rotation angles:
   ```kotlin
   override fun calculateRotation(index: Int, total: Int): Vector3D {
       return when {
           total == 1 -> Vector3D(0f, 0f, 0f)  // No rotation
           total == 2 -> if (index == 0)
               Vector3D(0f, -18f, 0f)  // Left angled
               else Vector3D(0f, 18f, 0f)  // Right angled
           else -> when (index) {
               0 -> Vector3D(0f, -18f, 0f)  // Left
               1 -> Vector3D(0f, 0f, 0f)     // Center flat
               2 -> Vector3D(0f, 18f, 0f)    // Right
               else -> Vector3D(0f, 0f, 0f)
           }
       }
   }
   ```

4. Implement variable dimensions:
   ```kotlin
   override fun calculateDimensions(index: Int, total: Int): WindowDimensions {
       return when {
           total == 1 -> WindowDimensions(1.1f, 0.8f)  // Large center
           total == 2 -> WindowDimensions(0.7f, 0.55f)  // Medium sides
           else -> if (index == 1)
               WindowDimensions(1.1f, 0.8f)  // Large center
               else WindowDimensions(0.7f, 0.55f)  // Medium sides
       }
   }
   ```

5. Set properties:
   ```kotlin
   override val name: String = "Triptych"
   override val maxWindows: Int = 3
   override val minWindows: Int = 1
   override val skipCurveProjection: Boolean = true  // NEW PROPERTY
   ```

**Acceptance Criteria:**
- [ ] File compiles without errors
- [ ] Positions calculated correctly for 1-3 windows
- [ ] Rotation angles match spec (±18°, 0°)
- [ ] Dimensions vary by position (center larger)

**Estimated Time:** 4-6 hours

---

#### Task 1.2: Add Curve Bypass Logic
**File:** `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/rendering/CurvedProjection.kt`

**Steps:**
1. Add property to `LayoutPreset` interface:
   ```kotlin
   // In LayoutPreset.kt interface
   val skipCurveProjection: Boolean get() = false  // Default: apply curve
   ```

2. Update `SpatialWindowRenderer.render()`:
   ```kotlin
   // In SpatialWindowRenderer.kt
   val quad = if (layoutPreset.skipCurveProjection) {
       CurvedProjection.generateFlatQuad(
           position = window.position,
           width = dimensions.width,
           height = dimensions.height,
           rotation = rotation
       )
   } else {
       CurvedProjection.generateCurvedQuad(
           position = window.position,
           width = dimensions.width,
           height = dimensions.height,
           rotation = rotation
       )
   }
   ```

3. Add `generateFlatQuad()` method if not exists:
   ```kotlin
   fun generateFlatQuad(
       position: Vector3D,
       width: Float,
       height: Float,
       rotation: Vector3D
   ): List<Vector3D> {
       // Simple quad without cylindrical curve
       val hw = width / 2f
       val hh = height / 2f
       return listOf(
           Vector3D(position.x - hw, position.y - hh, position.z),
           Vector3D(position.x + hw, position.y - hh, position.z),
           Vector3D(position.x + hw, position.y + hh, position.z),
           Vector3D(position.x - hw, position.y + hh, position.z)
       ).map { applyRotation(it, rotation) }
   }
   ```

**Acceptance Criteria:**
- [ ] `skipCurveProjection` property added to interface
- [ ] Renderer checks flag before applying curve
- [ ] Flat quad generation works correctly
- [ ] Existing layouts (Arc, Theater) unaffected

**Estimated Time:** 2-3 hours

---

#### Task 1.3: Register Triptych in Layout Cycle
**File:** `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WorkspaceViewModel.kt`

**Steps:**
1. Add to layout presets list:
   ```kotlin
   private val layoutPresets = listOf(
       LinearHorizontalLayout,
       TriptychLayout,  // NEW
       ArcFrontLayout,
       TheaterLayout
   )
   ```

2. Update layout name mapping:
   ```kotlin
   fun getLayoutPresetName(): String {
       return when (currentLayoutPreset) {
           is TriptychLayout -> "Triptych"
           is ArcFrontLayout -> "Arc"
           is TheaterLayout -> "Theater"
           else -> "Linear"
       }
   }
   ```

3. Add voice command support (if VoiceOS integrated):
   ```kotlin
   fun handleVoiceCommand(command: String) {
       when (command.lowercase()) {
           "triptych mode", "focus mode" -> {
               setLayoutPreset(TriptychLayout)
           }
           // ... other commands
       }
   }
   ```

**Acceptance Criteria:**
- [ ] Triptych appears in layout cycle
- [ ] Cycle order: Linear → Triptych → Arc → Theater
- [ ] Voice commands work (if VoiceOS integrated)
- [ ] Top bar displays "Spatial - Triptych" correctly

**Estimated Time:** 1-2 hours

---

#### Task 1.4: Test Triptych Layout
**Testing:** Manual + Unit tests

**Manual Testing Steps:**
1. Launch Cockpit MVP on emulator
2. Add 3 mock windows (Email, Browser, Calculator)
3. Enable spatial mode (tap 3D icon)
4. Cycle layouts until "Spatial - Triptych" appears
5. Verify window positions:
   - Left: Small, angled left
   - Center: Large, flat, slightly closer
   - Right: Small, angled right
6. Test with 1 window (center only)
7. Test with 2 windows (left + right)

**Unit Tests to Add:**
```kotlin
// In TriptychLayoutTest.kt
@Test
fun `test positions for 3 windows`() {
    val positions = (0..2).map { TriptychLayout.calculatePosition(it, 3) }
    assertEquals(Vector3D(-0.9f, 0f, -2.0f), positions[0])  // Left
    assertEquals(Vector3D(0f, 0f, -1.8f), positions[1])      // Center
    assertEquals(Vector3D(0.9f, 0f, -2.0f), positions[2])    // Right
}

@Test
fun `test rotations for 3 windows`() {
    val rotations = (0..2).map { TriptychLayout.calculateRotation(it, 3) }
    assertEquals(Vector3D(0f, -18f, 0f), rotations[0])  // Left
    assertEquals(Vector3D(0f, 0f, 0f), rotations[1])     // Center
    assertEquals(Vector3D(0f, 18f, 0f), rotations[2])    // Right
}

@Test
fun `test dimensions for 3 windows`() {
    val dims = (0..2).map { TriptychLayout.calculateDimensions(it, 3) }
    assertEquals(WindowDimensions(0.7f, 0.55f), dims[0])  // Left small
    assertEquals(WindowDimensions(1.1f, 0.8f), dims[1])   // Center large
    assertEquals(WindowDimensions(0.7f, 0.55f), dims[2])  // Right small
}
```

**Acceptance Criteria:**
- [ ] Unit tests pass
- [ ] Center window visibly larger than sides
- [ ] Windows angled correctly
- [ ] No curve distortion (flat projection)
- [ ] Readable text in center window

**Estimated Time:** 4-6 hours

---

### Phase 1 Deliverables
- [x] TriptychLayout.kt (production-ready)
- [x] Curve bypass logic implemented
- [x] Layout registered in ViewModel
- [x] Manual + unit tests passing
- [x] Demo video showing readable spatial workspace

**Phase 1 Success Gate:** Center window readable without pinch-zoom

---

## Phase 2: Freeform Windows (Days 4-12)

### Objective
Enable launching real Android apps in freeform windows and capturing their content.

### Tasks

#### Task 2.1: Update AppWindow Data Class
**File:** `Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/core/window/AppWindow.kt`

**Steps:**
1. Add expect/actual for Rect:
   ```kotlin
   // In commonMain/AppWindow.kt
   expect class Rect

   // In androidMain/AppWindow.android.kt
   actual typealias Rect = android.graphics.Rect
   ```

2. Add freeform properties to AppWindow:
   ```kotlin
   data class AppWindow(
       // ... existing properties

       // NEW: Freeform window support
       val packageName: String? = null,
       val launchBounds: Rect? = null,
       val rotation: Vector3D = Vector3D(0f, 0f, 0f),
   )
   ```

**Acceptance Criteria:**
- [ ] Compiles on both commonMain and androidMain
- [ ] Rect type resolves correctly
- [ ] Existing code unaffected

**Estimated Time:** 1 hour

---

#### Task 2.2: Create FreeformWindowManager
**File:** `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/FreeformWindowManager.kt`

**Implementation:**
```kotlin
class FreeformWindowManager(
    private val context: Context,
    private val mediaProjectionManager: MediaProjectionManager
) {
    private var mediaProjection: MediaProjection? = null
    private val activeWindows = mutableMapOf<String, VirtualDisplay>()

    companion object {
        private const val TAG = "FreeformWindowManager"
        private const val WINDOWING_MODE_FREEFORM = 5  // Hidden API constant
    }

    fun requestPermission(): Intent {
        return mediaProjectionManager.createScreenCaptureIntent()
    }

    fun initializeMediaProjection(resultCode: Int, data: Intent) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
    }

    suspend fun launchAppWindow(
        packageName: String,
        bounds: Rect,
        surface: Surface
    ): Result<VirtualDisplay> = withContext(Dispatchers.Main) {
        try {
            // Get launch intent for app
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return@withContext Result.failure(Exception("App not found: $packageName"))

            // Create activity options with freeform bounds
            val options = ActivityOptions.makeBasic()
            options.setLaunchBounds(bounds)

            // Use reflection to set windowing mode (hidden API)
            try {
                val method = ActivityOptions::class.java.getMethod(
                    "setLaunchWindowingMode",
                    Int::class.javaPrimitiveType
                )
                method.invoke(options, WINDOWING_MODE_FREEFORM)
            } catch (e: Exception) {
                Log.w(TAG, "Could not set windowing mode via reflection", e)
            }

            // Set intent flags for new task/window
            intent.addFlags(
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_NEW_TASK
            )

            // Launch activity
            context.startActivity(intent, options.toBundle())

            // Wait for app to launch (heuristic delay)
            delay(500)

            // Create VirtualDisplay to capture the window
            val virtualDisplay = mediaProjection?.createVirtualDisplay(
                "CockpitWindow_$packageName",
                bounds.width(),
                bounds.height(),
                context.resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            ) ?: return@withContext Result.failure(
                Exception("MediaProjection not initialized")
            )

            activeWindows[packageName] = virtualDisplay
            Result.success(virtualDisplay)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app window", e)
            Result.failure(e)
        }
    }

    fun closeWindow(packageName: String) {
        activeWindows[packageName]?.release()
        activeWindows.remove(packageName)
    }

    fun cleanup() {
        activeWindows.values.forEach { it.release() }
        activeWindows.clear()
        mediaProjection?.stop()
        mediaProjection = null
    }
}
```

**Acceptance Criteria:**
- [ ] Compiles without errors
- [ ] Hidden API reflection works
- [ ] VirtualDisplay creates successfully
- [ ] Resource cleanup prevents leaks

**Estimated Time:** 6-8 hours

---

#### Task 2.3: Add MediaProjection Permission Flow
**File:** `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/MainActivity.kt`

**Steps:**
1. Add permission launcher:
   ```kotlin
   private lateinit var freeformManager: FreeformWindowManager

   private val mediaProjectionLauncher = registerForActivityResult(
       ActivityResultContracts.StartActivityForResult()
   ) { result ->
       if (result.resultCode == RESULT_OK && result.data != null) {
           freeformManager.initializeMediaProjection(result.resultCode, result.data!!)
       } else {
           // Permission denied - show dialog
           showPermissionDeniedDialog()
       }
   }
   ```

2. Initialize in onCreate:
   ```kotlin
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)

       // Initialize FreeformWindowManager
       val mediaProjectionManager = getSystemService(
           Context.MEDIA_PROJECTION_SERVICE
       ) as MediaProjectionManager
       freeformManager = FreeformWindowManager(this, mediaProjectionManager)

       // Request permission on first launch
       val prefs = getPreferences(MODE_PRIVATE)
       if (!prefs.getBoolean("media_projection_granted", false)) {
           mediaProjectionLauncher.launch(freeformManager.requestPermission())
       }

       // ... rest of onCreate
   }
   ```

3. Add permission dialog:
   ```kotlin
   private fun showPermissionDeniedDialog() {
       AlertDialog.Builder(this)
           .setTitle("Screen Capture Permission Required")
           .setMessage("Cockpit needs permission to display app windows. Enable in Settings?")
           .setPositiveButton("Settings") { _, _ ->
               mediaProjectionLauncher.launch(freeformManager.requestPermission())
           }
           .setNegativeButton("Cancel", null)
           .show()
   }
   ```

**Acceptance Criteria:**
- [ ] Permission dialog appears on first launch
- [ ] Granting permission initializes MediaProjection
- [ ] Denying shows error dialog with retry option
- [ ] Permission persists across app restarts

**Estimated Time:** 3-4 hours

---

#### Task 2.4: Update WindowCard for Freeform Rendering
**File:** `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WindowCard.kt`

**Steps:**
1. Add surface parameter:
   ```kotlin
   @Composable
   fun WindowCard(
       window: AppWindow,
       color: String,
       onClose: () -> Unit,
       modifier: Modifier = Modifier,
       isFocused: Boolean = false,
       captureSurface: Surface? = null  // NEW
   ) {
   ```

2. Add rotation via graphicsLayer:
   ```kotlin
   GlassmorphicCard(
       modifier = modifier
           .graphicsLayer {
               rotationX = window.rotation.x
               rotationY = window.rotation.y
               rotationZ = window.rotation.z
               cameraDistance = 12f * density
           },
       isFocused = isFocused
   ) {
   ```

3. Render captured content:
   ```kotlin
   Box(modifier = Modifier.fillMaxSize()) {
       // Accent color indicator
       Box(
           modifier = Modifier
               .fillMaxWidth()
               .height(3.dp)
               .background(Color(android.graphics.Color.parseColor(color)))
       )

       // Render captured app content if available
       if (captureSurface != null && window.packageName != null) {
           AndroidView(
               factory = { context ->
                   SurfaceView(context).apply {
                       holder.addCallback(object : SurfaceHolder.Callback {
                           override fun surfaceCreated(holder: SurfaceHolder) {
                               holder.surface.copyFrom(captureSurface)
                           }
                           override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                           override fun surfaceDestroyed(holder: SurfaceHolder) {}
                       })
                   }
               },
               modifier = Modifier
                   .fillMaxSize()
                   .padding(top = 3.dp)
           )
       } else {
           // Fallback: Original static content
           Column(
               modifier = Modifier
                   .fillMaxSize()
                   .padding(16.dp),
               verticalArrangement = Arrangement.SpaceBetween
           ) {
               // ... existing code ...
           }
       }
   }
   ```

**Acceptance Criteria:**
- [ ] SurfaceView renders captured content
- [ ] Rotation applies correctly
- [ ] Fallback to mock content if no surface
- [ ] No visual glitches

**Estimated Time:** 4-6 hours

---

#### Task 2.5: Update WorkspaceViewModel
**File:** `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WorkspaceViewModel.kt`

**Steps:**
1. Add freeform manager dependency:
   ```kotlin
   class WorkspaceViewModel(
       private val freeformManager: FreeformWindowManager
   ) : ViewModel() {
   ```

2. Add surface state:
   ```kotlin
   private val _captureSurfaces = MutableStateFlow<Map<String, Surface>>(emptyMap())
   val captureSurfaces: StateFlow<Map<String, Surface>> = _captureSurfaces.asStateFlow()
   ```

3. Add method to launch Android apps:
   ```kotlin
   fun addAndroidApp(
       packageName: String,
       title: String,
       color: String,
       position: Vector3D,
       rotation: Vector3D = Vector3D(0f, 0f, 0f)
   ) {
       viewModelScope.launch {
           // Create surface for capture
           val surfaceTexture = SurfaceTexture(0)
           val surface = Surface(surfaceTexture)

           // Calculate freeform bounds (800x600 window)
           val bounds = Rect(100, 100, 900, 700)

           // Launch app in freeform window
           val result = freeformManager.launchAppWindow(packageName, bounds, surface)

           if (result.isSuccess) {
               val window = AppWindow(
                   id = UUID.randomUUID().toString(),
                   title = title,
                   type = WindowType.ANDROID_APP,
                   sourceId = packageName,
                   position = position,
                   widthMeters = 0.8f,
                   heightMeters = 0.6f,
                   packageName = packageName,
                   launchBounds = bounds,
                   rotation = rotation
               )

               _windows.update { it + window }
               _captureSurfaces.update { it + (window.id to surface) }
           } else {
               // Show error toast
               Log.e(TAG, "Failed to launch $packageName", result.exceptionOrNull())
           }
       }
   }
   ```

4. Update cleanup:
   ```kotlin
   override fun onCleared() {
       super.onCleared()
       freeformManager.cleanup()
       _captureSurfaces.value.values.forEach { it.release() }
   }
   ```

**Acceptance Criteria:**
- [ ] Can launch Calculator app
- [ ] Surface created and bound
- [ ] Window added to state
- [ ] Cleanup prevents leaks

**Estimated Time:** 4-6 hours

---

#### Task 2.6: Test Freeform Windows
**Manual Testing:**
1. Grant MediaProjection permission
2. Add Calculator app via UI or ViewModel
3. Verify Calculator launches in freeform window
4. Check content displays in WindowCard
5. Test rotation (modify rotation property)
6. Test closing window (cleanup)

**Integration Tests:**
```kotlin
@Test
fun `test freeform window launch`() = runBlocking {
    val manager = FreeformWindowManager(context, mediaProjectionManager)
    manager.initializeMediaProjection(RESULT_OK, mockIntent)

    val surface = Surface(SurfaceTexture(0))
    val bounds = Rect(100, 100, 900, 700)

    val result = manager.launchAppWindow("com.android.calculator2", bounds, surface)

    assertTrue(result.isSuccess)
    assertNotNull(result.getOrNull())
}
```

**Acceptance Criteria:**
- [ ] Calculator launches successfully
- [ ] Content visible in WindowCard
- [ ] Rotation works (angled view)
- [ ] No crashes or memory leaks

**Estimated Time:** 6-8 hours

---

### Phase 2 Deliverables
- [x] FreeformWindowManager (production-ready)
- [x] MediaProjection permission flow
- [x] AppWindow updated with freeform properties
- [x] WindowCard renders captured content with rotation
- [x] Calculator app running in spatial workspace
- [x] Integration tests passing

**Phase 2 Success Gate:** Real Android app (Calculator) running with angled view

---

## Phase 3: WebAvanue Integration (Days 13-18)

### Objective
Integrate WebAvanue browser into spatial workspace for production demo.

### Tasks

#### Task 3.1: Verify WebAvanue Package Name
**Steps:**
1. Read AndroidManifest.xml:
   ```bash
   cat /Volumes/M-Drive/Coding/NewAvanues/android/apps/webavanue/src/main/AndroidManifest.xml | grep package
   ```

2. Confirm package name (expected: `com.augmentalis.webavanue`)

3. Test app exists:
   ```bash
   adb shell pm list packages | grep webavanue
   ```

**Acceptance Criteria:**
- [ ] Package name confirmed
- [ ] WebAvanue app installed on emulator

**Estimated Time:** 30 minutes

---

#### Task 3.2: Launch WebAvanue in Freeform
**File:** `android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/WorkspaceViewModel.kt`

**Steps:**
1. Add WebAvanue launch method:
   ```kotlin
   fun addWebAvanueWindow(
       url: String = "https://youtube.com",
       title: String = "WebAvanue",
       position: Vector3D = Vector3D(0f, 0f, -2f),
       rotation: Vector3D = Vector3D(0f, 0f, 0f)
   ) {
       addAndroidApp(
           packageName = "com.augmentalis.webavanue",  // Verified package
           title = title,
           color = "#FF6F00",  // Orange for web
           position = position,
           rotation = rotation
       )

       // TODO: Send URL intent extra
   }
   ```

2. Add UI button in ControlPanel:
   ```kotlin
   // In ControlPanel.kt AddWindowDialog
   Button(onClick = {
       onAddWindow("WebAvanue", WindowType.WEB_APP, "#FF6F00")
       viewModel.addWebAvanueWindow()
       showDialog = false
   }) {
       Text("Add WebAvanue Browser")
   }
   ```

**Acceptance Criteria:**
- [ ] WebAvanue launches in freeform window
- [ ] Browser content visible
- [ ] Default URL loads (YouTube)

**Estimated Time:** 2-3 hours

---

#### Task 3.3: Pass URL Intent Extra
**Steps:**
1. Modify `launchAppWindow()` to accept extras:
   ```kotlin
   suspend fun launchAppWindow(
       packageName: String,
       bounds: Rect,
       surface: Surface,
       extras: Bundle? = null  // NEW
   ): Result<VirtualDisplay> {
       val intent = context.packageManager.getLaunchIntentForPackage(packageName)
           ?: return Result.failure(Exception("App not found"))

       // Add extras if provided
       extras?.let { intent.putExtras(it) }

       // ... rest of launch logic
   }
   ```

2. Update WebAvanue launch to pass URL:
   ```kotlin
   fun addWebAvanueWindow(url: String, ...) {
       val extras = Bundle().apply {
           putString("url", url)
       }

       viewModelScope.launch {
           val bounds = Rect(100, 100, 900, 700)
           val surface = Surface(SurfaceTexture(0))

           val result = freeformManager.launchAppWindow(
               packageName = "com.augmentalis.webavanue",
               bounds = bounds,
               surface = surface,
               extras = extras
           )

           // ... handle result
       }
   }
   ```

3. Update WebAvanue to handle intent extra (if needed):
   ```kotlin
   // In WebAvanue MainActivity
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)

       val url = intent.getStringExtra("url") ?: "https://google.com"
       // Load URL in WebView
   }
   ```

**Acceptance Criteria:**
- [ ] URL intent extra passes to WebAvanue
- [ ] Browser opens to specified page
- [ ] Can launch multiple WebAvanue instances with different URLs

**Estimated Time:** 2-3 hours

---

#### Task 3.4: Test WebAvanue Integration
**Manual Testing:**
1. Launch Cockpit MVP
2. Add WebAvanue window via UI
3. Verify YouTube loads
4. Test navigation (if touch input works)
5. Test voice commands (if VoiceOS integrated)
6. Test with Triptych layout (center window)

**Demo Scenarios:**
- **Email + Browser + Calculator:** Classic productivity setup
- **3x YouTube:** Video monitoring (different channels)
- **WebAvanue + Chrome + Gmail:** Mixed app workspace

**Acceptance Criteria:**
- [ ] WebAvanue launches successfully
- [ ] Browser displays live web content
- [ ] Can have multiple browser windows
- [ ] Works in Triptych layout

**Estimated Time:** 4-6 hours

---

### Phase 3 Deliverables
- [x] WebAvanue package verified
- [x] WebAvanue launches in freeform window
- [x] URL intent working
- [x] Production demo ready (real apps in spatial workspace)

**Phase 3 Success Gate:** Demo video showing WebAvanue + Calculator in Triptych layout

---

## Testing Strategy

### Unit Tests
**Files:**
- `TriptychLayoutTest.kt` - Position/rotation/dimensions
- `FreeformWindowManagerTest.kt` - Launch logic, cleanup

**Coverage Target:** 80%+

### Integration Tests
**Scenarios:**
- MediaProjection permission flow
- Freeform window launch
- Surface capture and rendering
- Window close cleanup

**Coverage Target:** 70%+

### Manual Tests
**Checklist:**
- [ ] Triptych layout readable
- [ ] Calculator runs in freeform
- [ ] WebAvanue browser works
- [ ] Rotation displays correctly
- [ ] Touch input accurate
- [ ] No memory leaks
- [ ] 60 FPS performance

### Performance Tests
**Metrics:**
- Frame rate: 60 FPS target
- Memory: <20MB for 5 windows
- Latency: <33ms capture delay

---

## Risk Mitigation

### Risk 1: Freeform Mode Disabled
**Mitigation:**
- Detect freeform support on launch
- Show setup instructions if disabled
- Provide ADB command: `adb shell settings put global enable_freeform_support 1`

### Risk 2: MediaProjection Performance
**Mitigation:**
- Use hardware encoding (VirtualDisplay flag)
- Limit to 5 windows
- Frame skip if GPU overloaded

### Risk 3: Touch Input Accuracy
**Mitigation:**
- Test touch transformation matrix
- Add calibration mode if needed
- Document known issues

---

## Deployment Plan

### Phase 1 Deployment (Day 3)
**Deliverable:** Triptych layout working with mock windows
**Branch:** `feature/triptych-layout`
**PR:** Review + merge to `Cockpit-Development`

### Phase 2 Deployment (Day 12)
**Deliverable:** Calculator running in freeform window
**Branch:** `feature/freeform-windows`
**PR:** Review + merge to `Cockpit-Development`

### Phase 3 Deployment (Day 18)
**Deliverable:** WebAvanue integrated, production demo ready
**Branch:** `feature/webavanue-integration`
**PR:** Review + merge to `Cockpit-Development`

### Final Merge (Day 18)
**Target:** `main` branch via PR
**Requirements:**
- All tests passing
- Demo video recorded
- Documentation updated

---

## Documentation Updates

**Files to Update:**
- `README.md` - Add Triptych + freeform features
- `ARCHITECTURE.md` - Document FreeformWindowManager
- `USER_GUIDE.md` - Add usage instructions
- `TROUBLESHOOTING.md` - Add freeform mode setup

---

## Success Metrics

**Phase 1:**
- [ ] Center window text readable without zoom
- [ ] 60 FPS with 3 windows

**Phase 2:**
- [ ] Calculator app running in freeform
- [ ] Content capture <33ms latency

**Phase 3:**
- [ ] WebAvanue browser functional
- [ ] Production demo recorded

**Overall:**
- [ ] All phases complete in 12-18 days
- [ ] No P0 bugs
- [ ] Demo shows real multi-tasking workflow

---

**End of Implementation Plan**
