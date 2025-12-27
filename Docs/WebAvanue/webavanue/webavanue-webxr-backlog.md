# WebXR Implementation Backlog

**Feature:** WebXR Support for WebAvanue Browser
**Status:** 73% Complete (Phase 5 Partial)
**Last Updated:** 2025-11-23

---

## Immediate Tasks (Phase 5 Completion - 60% Remaining)

### Priority: HIGH

#### 1. XRManager Permission Integration
**Effort:** 2-3 hours
**Dependencies:** MainActivity permission handling (complete)

**Tasks:**
- [ ] Add callback methods to XRManager
  - `onCameraPermissionGranted()`
  - `onCameraPermissionDenied(isPermanent: Boolean)`
- [ ] Wire MainActivity permission launcher to XRManager callbacks
- [ ] Update XRState.permissionState based on results
- [ ] Trigger XRPermissionDialog when state == "UNKNOWN"

**Acceptance Criteria:**
- Permission dialog appears before AR session
- Permission granted → AR session starts
- Permission denied → User sees rationale dialog
- Permanent denial → "Open Settings" button appears

---

#### 2. WebView FPS Tracking Integration
**Effort:** 4-6 hours
**Dependencies:** XRManager, XRPerformanceMonitor

**Tasks:**
- [ ] Create JavaScript injection for requestAnimationFrame
  ```javascript
  window.xrFrameCallback = () => {
    window.Android.recordFrame();
    requestAnimationFrame(window.xrFrameCallback);
  };
  ```
- [ ] Add JavaScript interface to WebView
  ```kotlin
  webView.addJavascriptInterface(object {
    @JavascriptInterface
    fun recordFrame() {
      xrManager.performanceMonitor.recordFrame()
    }
  }, "Android")
  ```
- [ ] Inject script when XR session starts
- [ ] Stop injection when session ends
- [ ] Test FPS accuracy against known framerates

**Acceptance Criteria:**
- FPS indicator shows accurate frame rate during XR sessions
- FPS tracking starts/stops with session lifecycle
- Minimal performance overhead (<1% CPU)

---

#### 3. End-to-End Device Testing
**Effort:** 3-4 hours
**Dependencies:** Real Android device with camera

**Test Sites:**
- https://immersive-web.github.io/webxr-samples/
- https://immersive-web.github.io/webxr-samples/immersive-ar-session.html
- https://immersive-web.github.io/webxr-samples/immersive-vr-session.html

**Test Cases:**
- [ ] AR session with camera permission flow
- [ ] VR session without permissions
- [ ] Performance monitoring (FPS, battery, temperature)
- [ ] Warning dialogs (low battery, overheating, low FPS)
- [ ] Settings changes during active session
- [ ] Auto-pause after timeout
- [ ] Session indicator UI visibility
- [ ] Multiple tab switching during XR session

**Acceptance Criteria:**
- AR session works on real WebXR content
- VR session works on real WebXR content
- All UI components visible and functional
- No crashes or memory leaks
- Performance metrics accurate

---

## Short-Term Tasks (Phase 6 - Polish & Documentation)

### Priority: MEDIUM

#### 4. Custom Icon Design
**Effort:** 2-3 hours
**Current State:** All icons use `Icons.Default.Info` fallback

**Tasks:**
- [ ] Design custom icons for:
  - AR mode badge
  - VR mode badge
  - Performance indicator
  - Warning types (battery, thermal, FPS)
  - WebXR settings
- [ ] Export as vector drawables
- [ ] Replace Icons.Default.Info with custom icons
- [ ] Update theme for icon colors

**Acceptance Criteria:**
- Distinct, recognizable icons for each state
- Consistent with Material Design guidelines
- Properly colored for light/dark themes

---

#### 5. Accessibility Improvements
**Effort:** 3-4 hours

**Tasks:**
- [ ] Add content descriptions to all icons
- [ ] Add semantics for screen readers
- [ ] Test with TalkBack enabled
- [ ] Add haptic feedback for warnings
- [ ] Improve keyboard navigation
- [ ] Add high-contrast mode support
- [ ] Test with large text settings

**Acceptance Criteria:**
- TalkBack announces all XR UI elements
- Users can navigate XR settings with D-pad/keyboard
- High contrast mode works correctly
- Large text doesn't break layouts

---

#### 6. Automated UI Tests
**Effort:** 4-6 hours

**Tasks:**
- [ ] Set up Compose UI Test framework
- [ ] Test XRSessionIndicator rendering
- [ ] Test XRPerformanceWarning display
- [ ] Test XRSettingsScreen interactions
- [ ] Test XRPermissionDialog flow
- [ ] Test state changes (session start/stop)
- [ ] Test warning appearance/dismissal

**Test Coverage Target:** 80%+

---

#### 7. API Documentation (KDoc)
**Effort:** 2-3 hours

**Tasks:**
- [ ] Add KDoc to all public classes
- [ ] Add KDoc to all public methods
- [ ] Add code examples to complex APIs
- [ ] Generate Dokka documentation
- [ ] Publish to docs/ directory

**Acceptance Criteria:**
- 100% public API documented
- Clear examples for integration
- Generated HTML docs available

---

## Long-Term Tasks (Future Phases)

### Priority: LOW

#### 8. ARCore Integration
**Effort:** 2-3 weeks
**Dependencies:** ARCore SDK, Google Play Services AR

**Features:**
- Plane detection
- Hit testing
- Anchors
- Light estimation
- Environment understanding
- Cloud anchors

**Use Cases:**
- Place virtual furniture in real rooms
- Measure real-world distances
- Persistent AR experiences
- Multi-user AR

---

#### 9. Advanced Performance Monitoring
**Effort:** 1-2 weeks

**Features:**
- GPU usage tracking
- Memory pressure detection
- Network bandwidth monitoring
- Jank detection (frame time spikes)
- Detailed performance graphs
- Export performance reports

---

#### 10. Multi-Session Support
**Effort:** 1-2 weeks

**Features:**
- Switch between AR and VR sessions
- Picture-in-Picture XR mode
- Session history
- Session state persistence
- Resume interrupted sessions

---

#### 11. Spatial Audio Support
**Effort:** 1-2 weeks
**Dependencies:** Microphone permission, WebAudio API

**Features:**
- 3D positional audio
- Head-related transfer function (HRTF)
- Reverb and occlusion
- Voice input for AR/VR

---

#### 12. Hand Tracking
**Effort:** 2-3 weeks
**Dependencies:** ARCore Hand Tracking, MediaPipe

**Features:**
- Hand pose estimation
- Gesture recognition
- Natural interaction in AR/VR
- Multi-hand tracking

---

## Technical Debt

### Priority: MEDIUM

#### TD-1: Replace Icons.Default.Info Fallback
**Related to:** Task #4 (Custom Icon Design)
**Current Impact:** Low (cosmetic only)

---

#### TD-2: XRPermissionManager Stub
**Impact:** Medium
**Tasks:**
- [ ] Implement full XRPermissionManager
- [ ] Add motion sensor permission checks
- [ ] Add microphone permission checks
- [ ] Add location permission checks

---

#### TD-3: WebView JavaScript Injection Security
**Impact:** Medium
**Tasks:**
- [ ] Add Content Security Policy headers
- [ ] Validate injected script origins
- [ ] Implement nonce-based script injection
- [ ] Add script injection logging

---

## Known Issues

### Issue #1: Material Icons Limited
**Status:** Documented
**Workaround:** Icons.Default.Info fallback
**Resolution:** Phase 6 - Custom icons

### Issue #2: FPS Tracking Requires Manual recordFrame() Calls
**Status:** In Progress (Task #2)
**Impact:** Medium
**Resolution:** WebView JavaScript injection

### Issue #3: No ARCore Integration
**Status:** Planned
**Impact:** Low (advanced feature)
**Resolution:** Future phase (Task #8)

---

## Metrics & Goals

### Current Metrics
- **Code Coverage:** ~80% (estimated)
- **Test Coverage:** 100% (XRSessionManager, XRPerformanceMonitor)
- **UI Test Coverage:** 0%
- **API Documentation:** ~60%
- **Build Success Rate:** 100%

### Phase 5 Goals
- Code Coverage: 85%+
- Test Coverage: 100% (all components)
- UI Test Coverage: 80%+
- API Documentation: 100%
- E2E Device Testing: Complete

### Phase 6 Goals
- Custom Icons: 100%
- Accessibility: TalkBack compatible
- Performance: <1% overhead
- Documentation: Complete (developer + user)

---

## Risk Assessment

### HIGH Risk
- **WebView FPS Tracking:** JavaScript injection may have security implications
  - *Mitigation:* CSP headers, origin validation, nonce-based injection

### MEDIUM Risk
- **Performance Overhead:** Continuous monitoring may drain battery
  - *Mitigation:* Configurable monitoring intervals, auto-pause

### LOW Risk
- **Device Compatibility:** Older devices may not support WebXR
  - *Mitigation:* Graceful degradation, feature detection

---

## Definition of Done

### Feature Complete (100%)
- [ ] All 6 phases complete
- [ ] Zero compilation errors
- [ ] Zero test failures
- [ ] 90%+ code coverage
- [ ] 80%+ UI test coverage
- [ ] 100% API documentation
- [ ] E2E testing complete
- [ ] Performance benchmarks complete
- [ ] Accessibility validated (TalkBack)
- [ ] User manual complete
- [ ] Developer guide complete

### Current Status
- [x] Phases 1-4 complete (100%)
- [x] Phase 5 partial (40%)
- [ ] Phase 6 pending

**Overall:** 73% Complete

---

**Next Review:** After Phase 5 completion
**Owner:** Development Team
**Stakeholders:** Product, QA, Documentation
