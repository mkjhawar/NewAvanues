# Cockpit MVP - Spatial Enhancements & Freeform Windows Specification

**Version:** 1.0
**Date:** 2025-12-10
**Author:** AI System (Claude)
**Status:** Draft - Ready for Implementation

---

## Executive Summary

Enhance Cockpit MVP's spatial rendering with a production-ready **Triptych Layout** (angled left/right, flat center) to fix unusable small windows in spatial mode, and implement **Android Freeform Window Integration** to enable real third-party apps (Chrome, WebAvanue, etc.) running in 3D spatial workspace with rotation support.

**Key Features:**
1. Triptych Layout preset for spatial mode (3 windows: ±18° angled sides, 0° flat center)
2. Freeform window management (launch Android apps in floating windows)
3. MediaProjection content capture (render real app content in spatial workspace)
4. View-level 3D rotation (graphicsLayer transformations)
5. WebAvanue browser integration (embedded or freeform)

**Impact:** Transforms Cockpit from mock windows to fully functional spatial multi-tasking environment with real Android applications.

---

## Problem Statement

### Current State

**Spatial Mode Issues:**
- Windows rendered too small (unusable without pinch-zoom)
- Cylindrical curve + linear layout creates double compression
- All windows uniform size (no hierarchy/focus)
- Center windows appear smallest due to perspective distortion

**Mock Data Limitations:**
- Only displays static Email/Browser/Calculator placeholders
- No real app content or interaction
- Cannot test actual multi-tasking workflows
- No third-party app support

### Pain Points

1. **Unusable Spatial Mode:** Users cannot read content in 3D curved mode
2. **No Real Apps:** Cannot demonstrate actual productivity (browsing, email, calculator)
3. **Missing WebAvanue:** Browser module exists but not integrated
4. **Limited Testing:** Cannot validate AR glasses workflow with real apps

### Desired State

- **Usable spatial layout** with large readable center window + comfortable side windows
- **Real Android apps** (Chrome, WebAvanue, Gmail, etc.) running in spatial workspace
- **Rotatable windows** at arbitrary angles for ergonomic viewing
- **Production-ready demo** showing actual multi-tasking capabilities

---

## Functional Requirements

### FR-1: Triptych Layout Preset

**FR-1.1 - Android - Layout Positioning**
- Create `TriptychLayout.kt` in `Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/layout/presets/`
- Support 1-3 windows with adaptive positioning:
  - 1 window: Center only (0°, z=-1.8m, 1.1m × 0.8m)
  - 2 windows: Left + right (±18°, z=-2.0m, 0.7m × 0.55m)
  - 3 windows: Full triptych (left -18°, center 0°, right +18°)

**FR-1.2 - Android - Window Dimensions**
- Left window: 0.7m × 0.55m (~30" perceived at 2m)
- Center window: 1.1m × 0.8m (~43" perceived at 1.8m) - 57% larger
- Right window: 0.7m × 0.55m (~30" perceived at 2m)

**FR-1.3 - Android - Rotation Angles**
- Left: -18° Y-axis rotation (faces slightly inward)
- Center: 0° (flat, no rotation)
- Right: +18° Y-axis rotation (faces slightly inward)

**FR-1.4 - Android - Depth Variation**
- Left/right: z = -2.0m
- Center: z = -1.8m (10% closer for focus hierarchy)

### FR-2: Curve Bypass for Triptych

**FR-2.1 - Android - Projection Control**
- Modify `CurvedProjection.kt` to check layout type before applying cylindrical curve
- Add `LayoutPreset.skipCurveProjection: Boolean` property (default: false)
- TriptychLayout sets `skipCurveProjection = true`

**FR-2.2 - Android - Rendering Pipeline**
- Update `SpatialWindowRenderer.render()` to respect skipCurveProjection flag
- Use `generateFlatQuad()` for Triptych, `generateCurvedQuad()` for Arc/Theater

### FR-3: Layout Registration

**FR-3.1 - Android - Layout Cycling**
- Add TriptychLayout to `WorkspaceViewModel` layout presets list
- Cycle order: Linear → Triptych → Arc → Theater
- Voice command: "Triptych mode" or "Focus mode"

**FR-3.2 - Android - UI Integration**
- Display "Spatial - Triptych" in top bar when active
- Support manual toggle via spatial mode button

### FR-4: Freeform Window Management

**FR-4.1 - Android - FreeformWindowManager**
- Create `FreeformWindowManager.kt` class
- Methods:
  - `requestPermission(): Intent` - Get MediaProjection permission intent
  - `initializeMediaProjection(resultCode, data)` - Initialize after permission granted
  - `launchAppWindow(packageName, bounds, surface): Result<VirtualDisplay>` - Launch app
  - `closeWindow(packageName)` - Release resources
  - `cleanup()` - Stop all captures

**FR-4.2 - Android - App Launching**
- Use `ActivityOptions.setLaunchBounds(Rect)` for freeform positioning
- Apply `FLAG_ACTIVITY_MULTIPLE_TASK` to enable multiple instances
- Use reflection to set `setLaunchWindowingMode(5)` (WINDOWING_MODE_FREEFORM)

**FR-4.3 - Android - Content Capture**
- Create `VirtualDisplay` using MediaProjection API
- Bind to `Surface` from `SurfaceView`/`TextureView`
- Capture at native resolution with hardware encoding

### FR-5: MediaProjection Permission Flow

**FR-5.1 - Android - Permission Request**
- Request on app startup via `ActivityResultContracts.StartActivityForResult()`
- Display user prompt: "Cockpit needs screen capture permission to display app windows"
- Handle denial gracefully (fallback to mock windows)

**FR-5.2 - Android - Permission Persistence**
- MediaProjection permission persists until app restarts
- No need to re-request for multiple windows in same session

### FR-6: AppWindow Data Model Updates

**FR-6.1 - KMP Common - Data Class Extension**
- Add to `AppWindow.kt`:
  ```kotlin
  val packageName: String? = null
  val launchBounds: Rect? = null
  val rotation: Vector3D = Vector3D(0f, 0f, 0f)
  ```

**FR-6.2 - Android - Platform Types**
- Use `expect/actual` for `Rect` type
- `expect class Rect` in commonMain
- `actual typealias Rect = android.graphics.Rect` in androidMain

### FR-7: WindowCard Rendering Updates

**FR-7.1 - Android - Surface Display**
- Embed `AndroidView` with `SurfaceView` in `WindowCard`
- Bind `Surface` from `FreeformWindowManager`
- Fallback to mock content if no surface available

**FR-7.2 - Android - 3D Rotation**
- Apply Compose `graphicsLayer` transformations:
  ```kotlin
  modifier.graphicsLayer {
      rotationX = window.rotation.x
      rotationY = window.rotation.y
      rotationZ = window.rotation.z
      cameraDistance = 12f * density
  }
  ```

### FR-8: WebAvanue Integration

**FR-8.1 - Android - Module Discovery**
- WebAvanue module located at `/Modules/WebAvanue`
- Package name: `com.augmentalis.webavanue` (assumed)

**FR-8.2 - Android - Launch Method**
- Phase 1: Launch WebAvanue as freeform app (same as third-party apps)
- Phase 2 (future): Direct embedding via `WebViewRenderer` component

**FR-8.3 - Android - URL Intent**
- Pass URL via intent extra when launching WebAvanue
- Enable launching to specific page (e.g., YouTube, Google)

---

## Non-Functional Requirements

### NFR-1: Performance

**NFR-1.1 - Android - Frame Rate**
- Maintain 60 FPS with 3 rotated windows
- Degrade gracefully to 30 FPS if GPU overloaded
- Target: <16ms render time per frame

**NFR-1.2 - Android - Latency**
- MediaProjection capture latency: <33ms (2 frames)
- Touch input response: <100ms

**NFR-1.3 - Android - Memory**
- Per-window memory overhead: <4MB
- Total limit: 20MB for 5 windows

### NFR-2: Compatibility

**NFR-2.1 - Android - API Level**
- Minimum SDK: API 24 (Android 7.0 Nougat) for freeform windows
- Target SDK: API 34 (Android 14)

**NFR-2.2 - Android - Device Support**
- Tablets: Full support (freeform mode default)
- Phones: Requires developer options enabled or desktop mode
- AR Glasses: Full support (target platform)

### NFR-3: User Experience

**NFR-3.1 - Android - Touch Input**
- Rotated windows must support accurate touch input
- Apply inverse transformation matrix to touch coordinates

**NFR-3.2 - Android - Visual Feedback**
- Loading state while app launches (spinner)
- Error toast if app fails to launch
- Permission denied: Show instructional dialog

### NFR-4: Reliability

**NFR-4.1 - Android - Error Handling**
- Handle app crashes (remove window, show toast)
- Handle permission denial (fallback to mock windows)
- Handle MediaProjection stop (cleanup resources)

**NFR-4.2 - Android - Resource Cleanup**
- Release VirtualDisplay on window close
- Stop MediaProjection on activity destroy
- Clear all surfaces to prevent leaks

---

## Platform-Specific Details

### Android

**Tech Stack:**
- Language: Kotlin
- UI: Jetpack Compose
- Spatial Rendering: Custom Canvas + OpenGL (existing)
- Window Capture: MediaProjection API
- Freeform Launch: ActivityOptions.setLaunchBounds()
- Rotation: Compose graphicsLayer transformations

**Components:**
| Component | File | Purpose |
|-----------|------|---------|
| TriptychLayout | `Common/Cockpit/.../TriptychLayout.kt` | Angled layout preset |
| FreeformWindowManager | `cockpit-mvp/.../FreeformWindowManager.kt` | App launching + capture |
| MediaProjectionHelper | `cockpit-mvp/.../MediaProjectionHelper.kt` | Permission flow |
| AppWindow (updated) | `Common/Cockpit/.../AppWindow.kt` | Add freeform properties |
| WindowCard (updated) | `cockpit-mvp/.../WindowCard.kt` | Render captured content + rotation |
| CurvedProjection (updated) | `cockpit-mvp/.../CurvedProjection.kt` | Curve bypass logic |
| WorkspaceViewModel (updated) | `cockpit-mvp/.../WorkspaceViewModel.kt` | Freeform lifecycle |

**Dependencies:**
- MediaProjection API (android.media.projection)
- VirtualDisplay (android.hardware.display)
- SurfaceView (android.view)

**Testing:**
- Unit tests: Layout calculations, rotation matrices
- UI tests: Window positioning, touch input
- Integration tests: Freeform launch, content capture
- Manual tests: Real apps (Chrome, Calculator, WebAvanue)

---

## User Stories

### US-1: Readable Spatial Workspace
**As a** Cockpit MVP user
**I want** windows to be large and readable in spatial mode
**So that** I can actually use the 3D workspace without pinch-zooming

**Acceptance Criteria:**
- [ ] Center window is 43" perceived size (readable at 2m distance)
- [ ] Side windows are 30" perceived size (comfortable at 2m distance)
- [ ] Windows arranged in ergonomic ±18° angles
- [ ] No pinch-zoom required to read content

### US-2: Real Android Apps
**As a** Cockpit MVP user
**I want** to run real Android apps (Chrome, Gmail, Calculator) in spatial workspace
**So that** I can demonstrate actual productivity workflows

**Acceptance Criteria:**
- [ ] Can launch Chrome browser in freeform window
- [ ] Can launch WebAvanue browser in freeform window
- [ ] Can launch Calculator in freeform window
- [ ] Apps display live content (not screenshots)
- [ ] Can interact with apps via touch

### US-3: Angled Window Viewing
**As a** Cockpit MVP user
**I want** side windows angled toward my view direction
**So that** I have comfortable ergonomic viewing angles

**Acceptance Criteria:**
- [ ] Left window rotated -18° (faces inward)
- [ ] Center window flat 0° (direct view)
- [ ] Right window rotated +18° (faces inward)
- [ ] Rotation matches natural eye movement range

### US-4: WebAvanue Integration
**As a** Cockpit MVP user
**I want** WebAvanue browser running in spatial workspace
**So that** I can browse web with voice commands in 3D

**Acceptance Criteria:**
- [ ] WebAvanue launches in freeform window
- [ ] Can open specific URL (YouTube, Google)
- [ ] Voice commands work (scroll, navigate, etc.)
- [ ] Browser state persists between uses

---

## Technical Constraints

### Constraints

**Android Platform:**
- Freeform windows require API 24+ (Android 7.0 Nougat)
- MediaProjection permission required (user prompt)
- Freeform mode may be disabled on some phones (developer option)
- Cannot rotate Activity/Window at system level (only View transformations)

**Performance:**
- MediaProjection captures entire screen (must crop to window bounds)
- Hardware encoding reduces CPU load but uses GPU bandwidth
- Multiple rotated views increase GPU workload
- Touch input on rotated views requires coordinate transformation

**KMP Limitations:**
- Freeform windows are Android-only (no iOS equivalent)
- MediaProjection API is Android-only
- TriptychLayout can be KMP common (positioning logic)
- AppWindow rotation property can be KMP common (Vector3D)

---

## Dependencies

### Implementation Order

**Phase 1: Triptych Layout (No Dependencies)**
1. Create `TriptychLayout.kt`
2. Add curve bypass to `CurvedProjection.kt`
3. Register in `WorkspaceViewModel`
4. Test with mock windows

**Phase 2: Freeform Foundation (Depends on Phase 1)**
1. Update `AppWindow.kt` data class
2. Create `FreeformWindowManager.kt`
3. Add MediaProjection permission flow in `MainActivity`
4. Test launching single app (Calculator)

**Phase 3: Window Rendering (Depends on Phase 2)**
1. Update `WindowCard.kt` with SurfaceView
2. Add rotation via graphicsLayer
3. Test with rotated freeform windows

**Phase 4: WebAvanue (Depends on Phase 3)**
1. Verify WebAvanue package name
2. Launch WebAvanue in freeform window
3. Pass URL intent extra
4. Test browsing in spatial workspace

### External Dependencies

**Android System:**
- MediaProjection permission (user grants once per session)
- Freeform window mode enabled (default on tablets/desktop mode)

**WebAvanue Module:**
- Module exists at `/Modules/WebAvanue`
- Assumed package: `com.augmentalis.webavanue`
- May need to verify actual package name in AndroidManifest.xml

---

## Swarm Assessment

**Multi-Agent Activation:** NO

**Reasoning:**
- Single platform (Android)
- Single developer domain (Android UI + system APIs)
- Sequential implementation (Triptych → Freeform → WebAvanue)
- ~12-18 days total (manageable by single developer)

**If Swarm Were Activated:**
- **Agent 1:** Spatial rendering specialist (Triptych layout)
- **Agent 2:** Android platform specialist (Freeform windows)
- **Agent 3:** Integration specialist (WebAvanue)
- **Coordination:** Architect agent (ensure compatibility)

**Current Recommendation:** Sequential implementation (no swarm needed).

---

## Success Criteria

### Triptych Layout Success

- [ ] **Readable center window:** 1.1m × 0.8m perceived size (~43" diagonal)
- [ ] **Comfortable side windows:** 0.7m × 0.55m perceived size (~30" diagonal)
- [ ] **Ergonomic angles:** Left -18°, center 0°, right +18°
- [ ] **No curve distortion:** Flat projection preserves window proportions
- [ ] **Performance:** 60 FPS with 3 windows

### Freeform Windows Success

- [ ] **Launch third-party apps:** Chrome, Calculator in freeform windows
- [ ] **Live content capture:** Real app content displays (not static)
- [ ] **Interactive:** Touch input works correctly
- [ ] **Resource cleanup:** No memory leaks on window close
- [ ] **Error handling:** Graceful permission denial + app crash recovery

### WebAvanue Integration Success

- [ ] **Browser launches:** WebAvanue opens in freeform window
- [ ] **URL intent:** Can launch to specific page (YouTube)
- [ ] **Voice commands:** Navigate, scroll work in spatial mode
- [ ] **State persistence:** Browser state saved between sessions

### Overall Success

- [ ] **Production demo ready:** Real apps running in usable spatial workspace
- [ ] **AR glasses workflow:** Validate multi-tasking on target hardware
- [ ] **Performance target:** 60 FPS with 3 real apps
- [ ] **User feedback:** Positive response to spatial ergonomics

---

## Implementation Timeline

### Phase 1: Triptych Layout (2-3 days)
- Day 1: Create TriptychLayout.kt + curve bypass
- Day 2: Register in layout cycle + test with mock windows
- Day 3: Polish + fix edge cases (1-2 windows)

### Phase 2: Freeform Foundation (4-5 days)
- Day 4-5: Update AppWindow + create FreeformWindowManager
- Day 6-7: MediaProjection permission flow + test Calculator launch
- Day 8: Error handling + resource cleanup

### Phase 3: Window Rendering (3-4 days)
- Day 9-10: Update WindowCard with SurfaceView + rotation
- Day 11: Touch input coordinate transformation
- Day 12: Test with multiple rotated apps

### Phase 4: WebAvanue Integration (2-3 days)
- Day 13-14: Verify package name + launch in freeform
- Day 15: URL intent + voice command testing

### Phase 5: Polish & Testing (2-3 days)
- Day 16-17: Performance optimization + bug fixes
- Day 18: User testing + documentation

**Total Estimated Timeline:** 12-18 days

---

## Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Freeform mode disabled on devices | High | Medium | Detect + show setup instructions |
| MediaProjection performance issues | Medium | Low | Hardware encoding + frame skipping |
| Touch input accuracy on rotated views | Medium | Medium | Coordinate transformation testing |
| App crashes during capture | Medium | Low | VirtualDisplay callbacks + recovery |
| WebAvanue package name mismatch | Low | Medium | Verify in AndroidManifest first |
| GPU overload with many windows | High | Medium | Limit to 5 windows + FPS throttling |

---

## Future Enhancements

### Post-MVP Features

**Window Management:**
- Drag-to-reposition windows in 3D space
- Pinch-to-resize freeform windows
- Window snapping (grid alignment)
- Minimize/maximize animations

**Advanced Layouts:**
- Adaptive Triptych (scales to 5 windows)
- Custom user layouts (save positions)
- Voice-activated layout switching

**Performance:**
- Selective rendering (only visible windows)
- Variable frame rate per window
- Low-power mode (30 FPS)

**WebAvanue Deep Integration:**
- Embed WebViewRenderer directly (no MediaProjection)
- Synchronized voice commands across Cockpit
- Shared clipboard between windows

---

## Appendix

### File Structure

```
Common/Cockpit/src/commonMain/kotlin/com/avanues/cockpit/
├── layout/presets/
│   ├── LinearHorizontalLayout.kt (existing)
│   ├── ArcFrontLayout.kt (existing)
│   ├── TheaterLayout.kt (existing)
│   └── TriptychLayout.kt (NEW)
├── core/window/
│   └── AppWindow.kt (MODIFIED - add freeform properties)

android/apps/cockpit-mvp/src/main/java/com/augmentalis/cockpit/mvp/
├── FreeformWindowManager.kt (NEW)
├── MediaProjectionHelper.kt (NEW)
├── WindowCard.kt (MODIFIED - add SurfaceView + rotation)
├── WorkspaceViewModel.kt (MODIFIED - freeform lifecycle)
├── MainActivity.kt (MODIFIED - permission flow)
├── rendering/
│   ├── CurvedProjection.kt (MODIFIED - curve bypass)
│   └── SpatialWindowRenderer.kt (MODIFIED - skipCurveProjection check)
```

### References

- [Android Freeform Windows - XDA Developers](https://www.xda-developers.com/android-nougats-freeform-window-mode-what-it-is-and-how-developers-can-utilize-it/)
- [MediaProjection API - Android Developers](https://developer.android.com/media/grow/media-projection)
- [Compose graphicsLayer - Android Developers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers)
- [WebAvanue Specification](../WebAvanue/MasterSpecs/WebAvanue-Spec-EnhancedWebView-51209-V1.md)

---

**End of Specification**
