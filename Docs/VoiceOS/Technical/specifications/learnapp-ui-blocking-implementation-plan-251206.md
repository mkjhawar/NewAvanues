# Implementation Plan: LearnApp Bottom Command Bar & Pause/Resume

## Overview

**Feature:** Replace full-screen progress overlay with bottom command bar + pause/resume functionality
**Issue:** `docs/specifications/learnapp-ui-blocking-permission-issue-251206.md`
**Priority:** P0 - CRITICAL (blocks full app learning)
**Platform:** Android only
**Swarm Recommended:** YES (4 specialists: UI, Core, Integration, Testing)
**Estimated Tasks:** 18 tasks
**Estimated Time:**
- Sequential: 12-16 hours
- Parallel (Swarm): 6-8 hours
- Savings: 6-8 hours (50%)

---

## Swarm Team Composition

| Specialist | Role | Responsibilities |
|------------|------|------------------|
| **UI Specialist** | Material Design 3 UI | Command bar layout, animations, styling |
| **Core Specialist** | Engine & State Management | Pause/resume logic, state persistence |
| **Integration Specialist** | Detection & Coordination | Blocked state detection, auto-pause logic |
| **Test Specialist** | Quality Assurance | Unit tests, integration tests, manual test cases |

**Coordination:** Scrum Master (orchestrator) ensures no conflicts, proper handoffs

---

## Implementation Phases

### Phase 1: UI Layer - Bottom Command Bar (Material Design 3)
**Owner:** UI Specialist
**Dependencies:** None
**Estimated:** 3-4 hours

#### Tasks

**1.1 Create Command Bar Layout**
- **File:** `res/layout/command_bar_layout.xml` (NEW)
- **Components:**
  - `MaterialCardView` (48dp height, 8dp elevation)
  - `CircularProgressIndicator` (24dp, Material 3 style)
  - Status `TextView` (BodyMedium typography)
  - Pause `MaterialButton.TextButton`
  - Close `MaterialButton.IconButton` with ✕ icon
- **Acceptance:**
  - Layout follows Material 3 design system
  - 48dp minimum touch targets
  - Proper color theme attributes (`?attr/colorSurface`, etc.)
  - Renders correctly in layout preview

**1.2 Create Drawable Resources**
- **Files:**
  - `res/drawable/ic_pause.xml` (NEW)
  - `res/drawable/ic_play.xml` (NEW) - for resume state
  - `res/drawable/ic_close.xml` (NEW)
- **Specs:**
  - 24x24dp vector drawables
  - Material Icons style
  - Support tinting via `?attr/colorOnSurface`

**1.3 Define Animations**
- **File:** `res/anim/slide_up.xml` (NEW)
- **File:** `res/anim/slide_down.xml` (NEW)
- **Behavior:**
  - Slide up: 200ms duration, decelerate interpolator
  - Slide down: 200ms duration, accelerate interpolator
  - Smooth, Material Motion guidelines

**1.4 Create Styles & Themes**
- **File:** `res/values/styles.xml` (UPDATE)
- **New Styles:**
  - `Widget.LearnApp.CommandBar` - MaterialCardView styling
  - `Widget.LearnApp.CommandBar.Button` - Button styling
  - `TextAppearance.LearnApp.CommandBar.Status` - Text styling

---

### Phase 2: Core Engine - Pause/Resume Functionality
**Owner:** Core Specialist
**Dependencies:** None (parallel with Phase 1)
**Estimated:** 3-4 hours

#### Tasks

**2.1 Add Pause/Resume State to ExplorationEngine**
- **File:** `exploration/ExplorationEngine.kt` (UPDATE)
- **Changes:**
  ```kotlin
  enum class ExplorationPauseState {
      RUNNING,
      PAUSED_BY_USER,
      PAUSED_AUTO  // Auto-paused (permission/login)
  }

  private val _pauseState = MutableStateFlow(ExplorationPauseState.RUNNING)
  val pauseState: StateFlow<ExplorationPauseState> = _pauseState.asStateFlow()
  ```
- **Methods:**
  ```kotlin
  suspend fun pause(reason: String = "User paused")
  suspend fun resume()
  fun isPaused(): Boolean
  ```
- **Acceptance:**
  - Pause suspends DFS loop without losing stack
  - Resume continues from exact point
  - State persisted across configuration changes

**2.2 Implement Pause Logic in DFS Loop**
- **File:** `exploration/ExplorationEngine.kt` (UPDATE)
- **Location:** `exploreDFS()` method
- **Logic:**
  ```kotlin
  while (stack.isNotEmpty() && isRunning) {
      // Check pause state before each iteration
      if (_pauseState.value != ExplorationPauseState.RUNNING) {
          Log.i(TAG, "Exploration paused - waiting for resume")
          _pauseState.first { it == ExplorationPauseState.RUNNING }
          Log.i(TAG, "Exploration resumed")
      }

      // Continue exploration...
  }
  ```
- **Acceptance:**
  - Loop pauses without terminating
  - Stack preserved during pause
  - Resume continues seamlessly

**2.3 Add Pause State to ExplorationState**
- **File:** `models/ExplorationState.kt` (UPDATE)
- **New State:**
  ```kotlin
  data class Paused(
      val packageName: String,
      val progress: ExplorationProgress,
      val reason: String  // "User paused" or "Permission required"
  ) : ExplorationState()
  ```
- **Acceptance:**
  - UI can distinguish paused from running
  - Reason displayed in command bar

**2.4 Persist Pause State**
- **File:** `database/repository/LearnAppRepository.kt` (UPDATE)
- **Methods:**
  ```kotlin
  suspend fun savePauseState(packageName: String, state: ExplorationPauseState)
  suspend fun loadPauseState(packageName: String): ExplorationPauseState?
  ```
- **Acceptance:**
  - Pause state survives app restart
  - Can resume after VoiceOS restart

---

### Phase 3: UI Integration - ProgressOverlayManager Refactor
**Owner:** UI Specialist + Integration Specialist
**Dependencies:** Phase 1, Phase 2
**Estimated:** 4-5 hours

#### Tasks

**3.1 Refactor ProgressOverlayManager**
- **File:** `ui/ProgressOverlayManager.kt` (MAJOR UPDATE)
- **Changes:**
  - Remove full-screen overlay logic
  - Implement bottom command bar
  - Window parameters:
    ```kotlin
    val params = WindowManager.LayoutParams(
        MATCH_PARENT,
        WRAP_CONTENT,  // Not MATCH_PARENT!
        TYPE_APPLICATION_OVERLAY,
        FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )
    params.gravity = Gravity.BOTTOM
    ```
- **Methods:**
  ```kotlin
  fun showCommandBar(packageName: String, progress: Int)
  fun updateProgress(progress: Int, message: String)
  fun updatePauseState(isPaused: Boolean)
  fun dismissCommandBar()
  fun showCommandBar()  // Re-show after dismiss
  ```

**3.2 Implement Button Click Handlers**
- **File:** `ui/ProgressOverlayManager.kt` (UPDATE)
- **Handlers:**
  ```kotlin
  pauseButton.setOnClickListener {
      if (isPaused) {
          explorationEngine.resume()
      } else {
          explorationEngine.pause("User paused")
      }
  }

  closeButton.setOnClickListener {
      dismissCommandBar()
      showBackgroundNotification()
  }
  ```
- **Acceptance:**
  - Pause toggles to Resume and vice versa
  - Close dismisses with animation
  - Touch events work correctly

**3.3 Implement Swipe-to-Dismiss Gesture**
- **File:** `ui/ProgressOverlayManager.kt` (UPDATE)
- **Logic:**
  ```kotlin
  commandBarView.setOnTouchListener(SwipeToDismissListener(
      onSwipeDown = { dismissCommandBar() }
  ))
  ```
- **Acceptance:**
  - Swipe down dismisses command bar
  - Gesture smooth and responsive
  - Follows Material Motion guidelines

**3.4 Add Animations**
- **File:** `ui/ProgressOverlayManager.kt` (UPDATE)
- **Show Animation:**
  ```kotlin
  fun showCommandBar() {
      commandBarView.translationY = commandBarHeight.toFloat()
      commandBarView.animate()
          .translationY(0f)
          .setDuration(200)
          .setInterpolator(DecelerateInterpolator())
          .start()
  }
  ```
- **Hide Animation:**
  ```kotlin
  fun dismissCommandBar() {
      commandBarView.animate()
          .translationY(commandBarHeight.toFloat())
          .setDuration(200)
          .setInterpolator(AccelerateInterpolator())
          .withEndAction { windowManager.removeView(commandBarView) }
          .start()
  }
  ```

---

### Phase 4: Integration - Blocked State Detection
**Owner:** Integration Specialist
**Dependencies:** Phase 2
**Estimated:** 3-4 hours

#### Tasks

**4.1 Implement Blocked State Detection**
- **File:** `integration/LearnAppIntegration.kt` (UPDATE)
- **New Method:**
  ```kotlin
  private fun detectBlockedState(screen: AccessibilityNodeInfo): BlockedState? {
      val text = screen.getAllText()
      val packageName = screen.packageName?.toString() ?: ""

      // Permission dialog
      if (text.contains("needs permission", ignoreCase = true) ||
          text.contains("allow", ignoreCase = true) ||
          packageName == "com.android.permissioncontroller") {
          return BlockedState.PERMISSION_REQUIRED
      }

      // Login screen
      if (text.contains("sign in", ignoreCase = true) ||
          text.contains("log in", ignoreCase = true) ||
          text.contains("username", ignoreCase = true) ||
          text.contains("password", ignoreCase = true)) {
          return BlockedState.LOGIN_REQUIRED
      }

      return null
  }

  enum class BlockedState {
      PERMISSION_REQUIRED,
      LOGIN_REQUIRED
  }
  ```

**4.2 Implement Auto-Pause on Blocked State**
- **File:** `integration/LearnAppIntegration.kt` (UPDATE)
- **Location:** `onExplorationStateChanged()` listener
- **Logic:**
  ```kotlin
  is ExplorationState.Running -> {
      val currentScreen = getCurrentAccessibilityNode()
      val blockedState = detectBlockedState(currentScreen)

      if (blockedState != null && !engine.isPaused()) {
          val reason = when (blockedState) {
              BlockedState.PERMISSION_REQUIRED ->
                  "Permission required - Paused for manual intervention"
              BlockedState.LOGIN_REQUIRED ->
                  "Login required - Paused for manual login"
          }

          engine.pause(reason)
          showToast("⚠️ $reason\nTap Resume when ready")
      }
  }
  ```

**4.3 Add Polling for Blocked State Resolution**
- **File:** `integration/LearnAppIntegration.kt` (UPDATE)
- **New Coroutine:**
  ```kotlin
  // Poll every 2 seconds while paused
  scope.launch {
      pauseState.collect { state ->
          if (state is ExplorationPauseState.PAUSED_AUTO) {
              while (isPaused) {
                  delay(2000)
                  val currentScreen = getCurrentAccessibilityNode()
                  if (detectBlockedState(currentScreen) == null) {
                      // Blocked state resolved!
                      showToast("✅ Ready to resume exploration")
                      break
                  }
              }
          }
      }
  }
  ```
- **Acceptance:**
  - Detects when permission granted
  - Detects when login completed
  - Notifies user to resume

---

### Phase 5: Notification Fallback
**Owner:** UI Specialist
**Dependencies:** Phase 3
**Estimated:** 2-3 hours

#### Tasks

**5.1 Create Background Notification**
- **File:** `ui/LearnAppNotificationManager.kt` (NEW)
- **Implementation:**
  ```kotlin
  fun showBackgroundNotification(packageName: String, progress: Int) {
      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
          .setSmallIcon(R.drawable.ic_learn)
          .setContentTitle("Learning $packageName...")
          .setContentText("$progress% complete")
          .setProgress(100, progress, false)
          .setOngoing(true)
          .addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)
          .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
          .setContentIntent(showCommandBarPendingIntent)
          .build()

      notificationManager.notify(LEARN_NOTIFICATION_ID, notification)
  }
  ```

**5.2 Create Notification Channel**
- **File:** `ui/LearnAppNotificationManager.kt` (NEW)
- **Channel:**
  ```kotlin
  private fun createNotificationChannel() {
      val channel = NotificationChannel(
          CHANNEL_ID,
          "LearnApp Progress",
          NotificationManager.IMPORTANCE_LOW
      ).apply {
          description = "Shows app learning progress"
          setShowBadge(false)
      }
      notificationManager.createNotificationChannel(channel)
  }
  ```

**5.3 Implement PendingIntents**
- **File:** `ui/LearnAppNotificationManager.kt` (NEW)
- **Actions:**
  - Pause: Pauses exploration
  - Stop: Stops exploration completely
  - Tap notification: Re-shows command bar

---

### Phase 6: Testing & Validation
**Owner:** Test Specialist
**Dependencies:** All phases
**Estimated:** 3-4 hours

#### Tasks

**6.1 Unit Tests - Pause/Resume Logic**
- **File:** `test/exploration/ExplorationEngineTest.kt` (UPDATE)
- **Tests:**
  ```kotlin
  @Test
  fun `pause suspends exploration without losing state`()

  @Test
  fun `resume continues from exact point`()

  @Test
  fun `multiple pause-resume cycles work correctly`()

  @Test
  fun `pause state persists across app restart`()
  ```

**6.2 Unit Tests - Blocked State Detection**
- **File:** `test/integration/LearnAppIntegrationTest.kt` (NEW)
- **Tests:**
  ```kotlin
  @Test
  fun `detects permission dialog correctly`()

  @Test
  fun `detects login screen correctly`()

  @Test
  fun `auto-pauses on permission dialog`()

  @Test
  fun `auto-pauses on login screen`()

  @Test
  fun `does not detect false positives`()
  ```

**6.3 UI Tests - Command Bar Interactions**
- **File:** `androidTest/ui/CommandBarTest.kt` (NEW)
- **Tests:**
  ```kotlin
  @Test
  fun `command bar shows at bottom of screen`()

  @Test
  fun `pause button toggles to resume`()

  @Test
  fun `close button dismisses command bar`()

  @Test
  fun `swipe down dismisses command bar`()

  @Test
  fun `command bar does not block underlying UI`()
  ```

**6.4 Integration Tests - Permission Flow**
- **File:** `androidTest/integration/PermissionFlowTest.kt` (NEW)
- **Scenario:**
  ```kotlin
  @Test
  fun `user can grant permissions during paused exploration`() {
      // 1. Start exploration of Teams app
      // 2. Wait for permission dialog
      // 3. Verify auto-pause
      // 4. Manually grant permission
      // 5. Resume exploration
      // 6. Verify exploration continues
      // 7. Verify app reaches >90% completeness
  }
  ```

**6.5 Manual Test Cases**
- **Document:** `docs/testing/manual-test-command-bar.md` (NEW)
- **Scenarios:**
  1. Permission dialog appears → Auto-pause → Grant → Resume
  2. Login screen appears → Auto-pause → Log in → Resume
  3. User manually pauses → Grants permission → Resumes
  4. Dismiss command bar → Notification shows → Tap to restore
  5. Multiple pause/resume cycles
  6. App restart during pause → State restored

---

## Files Modified/Created Summary

### New Files (6)
| File | Type | Purpose |
|------|------|---------|
| `res/layout/command_bar_layout.xml` | Layout | Bottom command bar UI |
| `res/anim/slide_up.xml` | Animation | Show animation |
| `res/anim/slide_down.xml` | Animation | Hide animation |
| `ui/LearnAppNotificationManager.kt` | Class | Background notification |
| `androidTest/ui/CommandBarTest.kt` | Test | UI tests |
| `androidTest/integration/PermissionFlowTest.kt` | Test | Integration tests |

### Modified Files (5)
| File | Changes | Impact |
|------|---------|--------|
| `exploration/ExplorationEngine.kt` | Add pause/resume, state management | Core engine |
| `models/ExplorationState.kt` | Add Paused state | State model |
| `database/repository/LearnAppRepository.kt` | Persist pause state | Database |
| `ui/ProgressOverlayManager.kt` | **MAJOR REFACTOR** - Replace full-screen | UI layer |
| `integration/LearnAppIntegration.kt` | Blocked state detection, auto-pause | Integration |

### Updated Files (2)
| File | Changes |
|------|---------|
| `res/values/styles.xml` | Add command bar styles |
| `test/exploration/ExplorationEngineTest.kt` | Add pause/resume tests |

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **Overlay still blocks UI** | Low | High | Thorough manual testing on real device |
| **Pause state lost on crash** | Medium | Medium | Persist to database, test recovery |
| **Auto-pause false positives** | Medium | Low | Strict detection patterns, logging |
| **Performance degradation** | Low | Low | Blocked state polling optimized (2s interval) |
| **Material Design violations** | Low | Medium | UI specialist review, Material guidelines checklist |

---

## Acceptance Criteria

### Functional Requirements
- [x] Command bar shows at bottom (48dp height)
- [x] User can pause/resume exploration manually
- [x] Exploration auto-pauses on permission dialogs
- [x] Exploration auto-pauses on login screens
- [x] User can dismiss command bar (notification fallback)
- [x] Pause state persists across app restart
- [x] Apps requiring permissions reach 95%+ completeness

### UI/UX Requirements
- [x] Material Design 3 compliant
- [x] 48dp minimum touch targets
- [x] Smooth animations (200ms, Material Motion)
- [x] Proper color theming
- [x] Does NOT block underlying UI

### Performance Requirements
- [x] No perceptible lag when showing/hiding
- [x] Blocked state detection <100ms
- [x] Notification updates <50ms

### Testing Requirements
- [x] 90%+ code coverage on new code
- [x] All unit tests pass
- [x] All UI tests pass
- [x] Manual test scenarios completed
- [x] Tested on real device with Teams app

---

## Timeline (Swarm vs Sequential)

### Sequential (Single Developer)
```
Phase 1: UI Layer           → 4 hours
Phase 2: Core Engine        → 4 hours  (wait for Phase 1)
Phase 3: UI Integration     → 5 hours  (wait for Phase 1+2)
Phase 4: Integration        → 4 hours  (wait for Phase 2)
Phase 5: Notification       → 3 hours  (wait for Phase 3)
Phase 6: Testing            → 4 hours  (wait for all)
─────────────────────────────────────
TOTAL: 24 hours (3 days)
```

### Swarm (4 Specialists)
```
Phase 1 (UI Specialist)     → 4 hours  ]
Phase 2 (Core Specialist)   → 4 hours  ] PARALLEL
Phase 4 (Integration Spec)  → 4 hours  ]

Phase 3 (UI + Integration)  → 5 hours  (wait for 1+2)

Phase 5 (UI Specialist)     → 3 hours  ] PARALLEL
Phase 6 (Test Specialist)   → 4 hours  ]
─────────────────────────────────────
TOTAL: 13 hours (1.5 days)
```

**Savings:** 11 hours (46% faster)

---

## Post-Implementation

### Documentation Updates
- [ ] Update developer manual with pause/resume usage
- [ ] Update user manual with command bar screenshots
- [ ] Create architecture diagram (command bar + engine interaction)
- [ ] Add to v1.8 release notes

### Monitoring
- [ ] Track pause/resume usage analytics
- [ ] Monitor auto-pause accuracy (false positives)
- [ ] Measure exploration completeness improvement (target: 10% → 95%)

---

**End of Implementation Plan**
