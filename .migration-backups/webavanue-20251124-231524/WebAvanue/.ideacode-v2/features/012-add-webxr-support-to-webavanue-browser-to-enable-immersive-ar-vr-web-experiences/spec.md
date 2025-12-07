# Delta for WebXR Support in WebAvanue Browser

**Feature:** Add WebXR support to WebAvanue browser to enable immersive AR/VR web experiences
**Feature ID:** 012
**Affected Spec:** `specs/webxr-support/spec.md`
**Created:** 2025-11-23
**Profile:** android-app

---

## Summary

This delta adds WebXR API support to the WebAvanue browser, enabling users to access immersive augmented reality (AR) and virtual reality (VR) web experiences. WebXR is the W3C standard for creating immersive 3D content on the web.

**Key Capabilities:**
- AR experiences (camera-based world tracking)
- VR experiences (360° immersive content)
- Hand tracking and controller input
- Spatial audio
- Hit testing for AR object placement

---

## ADDED Requirements

> New capabilities being introduced by this feature

### Requirement: WebXR API Support in WebView

The system SHALL enable WebXR Device API support in the Android WebView component.

**Rationale:** WebXR Device API is the standard web API for accessing VR and AR capabilities. Modern web experiences require this API to deliver immersive content.

**Priority:** High

**Acceptance Criteria:**
- [ ] WebView has WebXR API enabled via settings
- [ ] JavaScript `navigator.xr` object is available to web pages
- [ ] WebXR session requests are properly handled
- [ ] AR and VR session modes are supported

#### Scenario: WebXR API Detection

**GIVEN** a web page that uses WebXR API
**WHEN** the page checks for `navigator.xr` availability
**THEN** the API object exists and is accessible
**AND** `navigator.xr.isSessionSupported('immersive-ar')` returns true
**AND** `navigator.xr.isSessionSupported('immersive-vr')` returns true

**Test Data:**
- Test URL: `https://immersive-web.github.io/webxr-samples/`
- WebXR polyfill test page

**Expected Result:**
- Console shows "WebXR supported"
- No API errors in console
- Session capability checks return expected values

---

### Requirement: Camera Permission Management

The system MUST request and manage camera permissions for AR sessions.

**Rationale:** AR experiences require camera access to overlay virtual content on the real world. Android requires runtime permission for camera access.

**Priority:** High

**Acceptance Criteria:**
- [ ] App declares CAMERA permission in manifest
- [ ] Runtime permission requested when AR session starts
- [ ] Permission denial handled gracefully
- [ ] User prompted with clear explanation of camera usage
- [ ] Permission state persisted across sessions

#### Scenario: First AR Session Launch

**GIVEN** user has not granted camera permission
**WHEN** user initiates an AR web experience
**THEN** system prompts for camera permission
**AND** permission dialog explains "Required for AR experiences"
**AND** user can grant or deny permission

**Test Data:**
- Test AR page: `https://web.dev/webxr-ar-example/`
- Fresh app install (no permissions granted)

**Expected Result:**
- Permission dialog appears before AR session starts
- Clear explanation visible
- Grant allows AR session to proceed
- Deny shows user-friendly error message

#### Scenario: AR Session with Granted Permission

**GIVEN** user previously granted camera permission
**WHEN** user launches AR experience
**THEN** AR session starts immediately without prompting
**AND** camera feed is visible in AR view

---

### Requirement: Motion Sensor Access

The system SHALL enable access to device motion sensors for head tracking and orientation.

**Rationale:** VR and AR experiences require device orientation data for tracking user head movement and rendering appropriate viewpoints.

**Priority:** High

**Acceptance Criteria:**
- [ ] Gyroscope sensor access enabled
- [ ] Accelerometer sensor access enabled
- [ ] Magnetometer access enabled (for compass)
- [ ] Sensor data provided to WebXR at minimum 60Hz
- [ ] Proper sensor permissions declared

#### Scenario: VR Head Tracking

**GIVEN** user has launched a VR experience
**WHEN** user rotates their device
**THEN** VR viewport rotates smoothly in response
**AND** orientation data updates at ≥60Hz
**AND** no visible lag or jitter in movement

**Test Data:**
- Test VR page: `https://immersive-web.github.io/webxr-samples/immersive-vr-session.html`

**Expected Result:**
- Smooth head tracking
- No drift over time
- Proper calibration

---

### Requirement: AR Hit Testing Support

The system SHOULD support WebXR Hit Test API for AR object placement.

**Rationale:** Hit testing allows AR apps to place virtual objects on real-world surfaces (floors, tables, walls), which is essential for most AR experiences.

**Priority:** Medium

**Acceptance Criteria:**
- [ ] Hit test API available via `XRSession.requestHitTestSource()`
- [ ] Ray casting against detected planes works
- [ ] Hit test results returned for valid surface intersections
- [ ] No hit test results returned for invalid rays

#### Scenario: Placing AR Object on Floor

**GIVEN** user has started AR session
**WHEN** user taps on detected floor surface
**THEN** system performs hit test at tap location
**AND** returns hit test result with surface pose
**AND** AR app can place virtual object at returned position

**Test Data:**
- Test AR placement page with floor detection

**Expected Result:**
- Object appears on floor surface
- Object stays in place when device moves
- Multiple objects can be placed

---

### Requirement: WebXR Session Lifecycle Management

The system MUST properly manage WebXR session lifecycle in coordination with Android activity lifecycle.

**Rationale:** WebXR sessions must pause when app backgrounds and resume when foregrounded to preserve resources and maintain proper state.

**Priority:** High

**Acceptance Criteria:**
- [ ] XR session pauses when activity pauses (Home button)
- [ ] XR session resumes when activity resumes
- [ ] Camera released when session paused
- [ ] Session state properly restored on resume
- [ ] No crashes during pause/resume cycles

#### Scenario: Backgrounding During AR Session

**GIVEN** user has active AR session running
**WHEN** user presses Home button
**THEN** AR session pauses gracefully
**AND** camera is released
**AND** app enters background without crash

**Test Data:**
- Running AR experience
- Home button press simulation

**Expected Result:**
- No crash
- Clean session pause
- Resources released

#### Scenario: Returning to AR Session

**GIVEN** user previously backgrounded AR session
**WHEN** user returns to app
**THEN** AR session resumes
**AND** camera reactivates
**AND** AR experience continues from previous state

---

### Requirement: WebGL 2.0 Support for XR Rendering

The system SHALL ensure WebGL 2.0 is enabled for high-performance 3D rendering.

**Rationale:** WebXR requires WebGL 2.0 for rendering immersive 3D content with modern shaders and effects.

**Priority:** High

**Acceptance Criteria:**
- [ ] WebGL 2.0 enabled in WebView
- [ ] Hardware acceleration enabled
- [ ] Sufficient graphics memory allocated
- [ ] 60fps minimum framerate maintained
- [ ] Stereo rendering supported for VR

#### Scenario: WebGL 2.0 Availability Check

**GIVEN** web page attempts to create WebGL 2.0 context
**WHEN** page calls `canvas.getContext('webgl2')`
**THEN** valid WebGL2RenderingContext is returned
**AND** context supports required XR extensions

**Test Data:**
- WebGL 2.0 capability test page

**Expected Result:**
- `gl instanceof WebGL2RenderingContext === true`
- Extensions available: `WEBGL_multiview`, `OVR_multiview2`

---

### Requirement: Performance Optimization for XR

The system SHOULD maintain minimum 60fps rendering during XR sessions.

**Rationale:** Low framerate in VR/AR causes motion sickness and poor user experience. 60fps minimum is industry standard.

**Priority:** High

**Acceptance Criteria:**
- [ ] Frame rate maintained at ≥60fps
- [ ] No frame drops during head movement
- [ ] GPU acceleration utilized
- [ ] Thermal throttling handled gracefully
- [ ] Battery usage optimized

#### Scenario: Sustained VR Performance

**GIVEN** user has VR session running for 5 minutes
**WHEN** monitoring frame rate
**THEN** average frame rate is ≥60fps
**AND** no frame drops below 45fps occur
**AND** device temperature stays within safe range

---

### Requirement: WebXR Voice Command Integration

The system SHALL integrate WebXR controls with VoiceOS command system.

**Rationale:** WebAvanue is voice-first. Users should control XR experiences with voice commands for hands-free operation.

**Priority:** Medium

**Acceptance Criteria:**
- [ ] Voice commands: "start AR", "start VR", "exit immersive"
- [ ] Voice commands: "place object", "remove object"
- [ ] Voice commands work during XR session
- [ ] Commands mapped to WebXR session control APIs

#### Scenario: Voice-Activated AR Session

**GIVEN** user viewing XR-capable web page
**WHEN** user says "start AR mode"
**THEN** system initiates XR session request
**AND** AR session starts if permission granted

**Test Data:**
- Voice command: "start AR mode"
- XR-capable page loaded

**Expected Result:**
- AR session launches
- Camera activates
- XR overlay appears

---

## MODIFIED Requirements

### Requirement: WebView Settings Enhancement

**EXISTING:** WebView has basic web compatibility settings
**MODIFIED:** WebView settings SHALL include XR-specific configurations

**Changes:**
```kotlin
// ADD to WebView settings initialization
settings.apply {
    // ... existing settings ...

    // WebXR support
    setDomStorageEnabled(true)  // Required for XR
    setMediaPlaybackRequiresUserGesture(false)  // Allow XR auto-start

    // Performance
    setRenderPriority(RenderPriority.HIGH)

    // WebGL 2.0
    setJavaScriptEnabled(true)  // Already set, but critical
}
```

**Rationale:** WebXR requires specific WebView configurations for proper operation.

---

## REMOVED Requirements

> No requirements removed by this feature

---

## Impact Analysis

### Breaking Changes

**None.** This is additive functionality.

### Non-Breaking Changes

- New permissions added to AndroidManifest.xml
- WebView settings enhanced
- New WebXR-specific APIs exposed
- Voice command vocabulary extended

### Migration Required

- [ ] No migration required - this is a new feature
- [ ] Existing users will see new permission requests on first XR usage

---

## Testing Requirements

### New Test Scenarios

**Unit Tests:**
- WebXR API availability check
- Permission request handling
- Session lifecycle management
- Voice command mapping

**Integration Tests:**
- AR session with camera permission flow
- VR session with motion sensors
- Session pause/resume during app lifecycle
- Hit testing accuracy

**E2E Tests:**
- Full AR experience: permission → session → placement → exit
- Full VR experience: permission → session → navigation → exit
- Voice-controlled XR session
- Battery and thermal limits during extended XR use

### Coverage Goals

- Unit test coverage: ≥80%
- Integration test coverage: ≥75%
- E2E test coverage: Critical XR flows (AR start, VR start, session management)

### Test URLs

```
AR Tests:
- https://immersive-web.github.io/webxr-samples/ar-hit-test.html
- https://web.dev/webxr-ar-example/

VR Tests:
- https://immersive-web.github.io/webxr-samples/immersive-vr-session.html
- https://threejs.org/examples/webxr_vr_ballshooter.html

API Tests:
- https://get.webgl.org/webgl2/
- https://webxr-api-test.glitch.me/
```

---

## Performance Impact

**Expected Changes:**
- **CPU:** +15-25% during XR session (sensor polling, rendering)
- **GPU:** +40-60% during XR session (3D rendering, shaders)
- **Battery:** 2-3x drain rate during active XR
- **Memory:** +50-100MB for 3D assets and buffers
- **Camera:** Active during AR sessions

**Optimizations:**
- Pause XR sessions when backgrounded
- Release camera immediately on pause
- Use GPU acceleration for all rendering
- Limit frame rate to 60fps (no excess rendering)

**Benchmarking Required:**
- [ ] Measure frame rate during complex AR scene
- [ ] Measure battery drain during 10-minute VR session
- [ ] Verify no memory leaks in session start/stop cycles
- [ ] Check thermal behavior during extended use

---

## Security Impact

**New Security Considerations:**

1. **Camera Access**
   - Privacy risk: Camera feed visible to web pages
   - Mitigation: Require explicit user permission
   - Indicator: Show camera active indicator during AR

2. **Sensor Data**
   - Privacy risk: Motion data could fingerprint device
   - Mitigation: Standard sensor permission model
   - Rate limiting on sensor access

3. **Spatial Mapping**
   - Privacy risk: 3D map of user environment
   - Mitigation: Data stays local, not sent to web
   - Clear privacy notice in permission dialog

4. **Battery Drain Attack**
   - Risk: Malicious site runs XR to drain battery
   - Mitigation: Require user gesture to start session
   - Auto-pause after 30 minutes of inactivity

**Security Review Required:** Yes

**Threat Model:**
- Unauthorized camera access → Mitigated by permission
- Sensor fingerprinting → Acceptable (standard Android sensors)
- Battery drain → Mitigated by auto-pause
- Memory exhaustion → Mitigated by WebView limits

---

## Documentation Updates Required

- [ ] User guide: "Using AR/VR Web Experiences in WebAvanue"
- [ ] Permissions guide: "Understanding XR Permissions"
- [ ] Voice commands reference: XR-specific commands
- [ ] Developer guide: Testing XR web content
- [ ] Troubleshooting: XR session failures
- [ ] Performance tips: Optimizing XR experiences
- [ ] Release notes: "WebXR support added in version X.X"

---

## Dependencies

### Android Platform
- **Minimum SDK**: API 24 (Android 7.0) - for WebXR basics
- **Recommended SDK**: API 29+ (Android 10+) - for full AR features
- **ARCore**: Optional but recommended for advanced AR

### Permissions Required
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.sensor.accelerometer" />
<uses-feature android:name="android.hardware.sensor.gyroscope" />
```

### External Dependencies
- ARCore (optional): For advanced plane detection
- WebXR Polyfill (fallback): For devices without native support

---

## Implementation Notes

### WebView Configuration
```kotlin
// WebViewSettings enhancement
settings.apply {
    // Enable XR APIs
    setDomStorageEnabled(true)
    setDatabaseEnabled(true)
    setJavaScriptEnabled(true)

    // Performance
    setRenderPriority(RenderPriority.HIGH)
    setMediaPlaybackRequiresUserGesture(false)

    // WebGL 2.0
    setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE)
}
```

### Permission Handling
```kotlin
// Request camera permission for AR
if (sessionMode == "immersive-ar") {
    if (checkSelfPermission(CAMERA) != GRANTED) {
        requestPermissions(arrayOf(CAMERA), REQUEST_AR_CAMERA)
    }
}
```

### Lifecycle Management
```kotlin
override fun onPause() {
    super.onPause()
    webView?.evaluateJavascript("xrSession?.end();", null)
}

override fun onResume() {
    super.onResume()
    // XR session will auto-resume via WebXR API
}
```

---

## Merge Instructions

**When this feature is archived:**

1. **Create living spec**: `specs/webxr-support/spec.md`
   - Merge all ADDED requirements
   - Include all scenarios and acceptance criteria
   - Set version to 1.0

2. **Update AndroidManifest.xml**
   - Add XR permissions
   - Add hardware feature declarations

3. **Update voice commands spec**
   - Add XR voice command vocabulary
   - Document XR control mappings

---

## Validation Checklist

Before merging this delta:

- [x] All ADDED requirements have ≥1 scenario
- [x] All requirements use SHALL/MUST/SHOULD language
- [x] All scenarios use GIVEN/WHEN/THEN structure
- [x] Acceptance criteria are specific and testable
- [x] Test coverage requirements are defined
- [x] Documentation updates are identified
- [x] Security implications analyzed
- [x] Performance impact documented
- [x] Dependencies listed
- [x] Test URLs provided

---

**Template Version:** 6.0.0
**Last Updated:** 2025-11-23
**Reviewed By:** Pending
**Status:** Ready for Planning
