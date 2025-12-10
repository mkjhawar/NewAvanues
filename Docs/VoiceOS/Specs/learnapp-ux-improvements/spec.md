# LearnApp UX Improvements - Feature Specification

**Feature ID:** LEARNAPP-UX-001
**Status:** Draft
**Platform:** Android
**Priority:** Critical
**Created:** 2025-11-28
**Last Updated:** 2025-11-28

---

## Overview

### Problem Statement

LearnApp functionality is currently **completely broken** due to initialization being commented out in VoiceOSService.kt. Additionally, even when functional, the UX has significant usability issues:

1. **Critical Bug:** LearnAppIntegration initialization is commented out (line 922)
2. **Poor UX:** Intrusive popups with no context about what "learning" means
3. **No User Control:** Cannot manually choose which apps to learn
4. **No Visibility:** Cannot see which apps are learned or learning progress
5. **No Flexibility:** Cannot learn screen-by-screen as user naturally explores apps

### Solution Summary

Implement a comprehensive 3-mode learning system:

1. **Auto-Detect Mode:** Improved consent dialog when new apps are detected
2. **Manual Learning Mode:** Settings UI for proactive app learning
3. **Just-in-Time Learning Mode:** Skip full learning, learn screen-by-screen as user naturally uses the app

---

## Business Value

**Impact:** Critical - Core VoiceOS functionality
**Users Affected:** All VoiceOS users
**Business Goals:**
- Restore broken functionality immediately
- Improve user understanding and adoption of LearnApp
- Reduce friction for accessibility users who depend on voice control
- Provide flexibility for different user preferences and workflows

---

## Target Users

### Primary User Groups

1. **All VoiceOS Users**
   - Need LearnApp to control apps with voice
   - Currently cannot use feature due to bug

2. **Accessibility Users**
   - Depend on voice commands for app navigation
   - Critical functionality for independent device use

3. **Power Users**
   - Want fine-grained control over learning
   - Prefer manual selection and screen-by-screen learning

4. **First-Time Users**
   - Need clear explanation of what "learning" means
   - Benefit from guided onboarding

---

## Technical Context

### Current Architecture

**VoiceOSCore Module:**
- LearnAppIntegration.kt - Main integration (exists but not initialized)
- ConsentDialogManager.kt - Consent popup manager (functional)
- AppLaunchDetector.kt - New app detection (functional)
- ExplorationEngine.kt - App exploration logic

**Database:**
- SQLDelight (voiceos.db)
- Tables: learned_apps, exploration_sessions, navigation_edges, screen_state

### Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose / XML Views (TBD per component)
- **Database:** SQLDelight
- **Async:** Kotlin Coroutines + Flow
- **Architecture:** MVVM with repository pattern
- **Android:** AccessibilityService, SYSTEM_ALERT_WINDOW permission

---

## ADDED: Requirements

### REQ-1: Immediate Fix (Critical)

**REQ-1.1:** The system SHALL uncomment LearnAppIntegration initialization in VoiceOSService.kt line 922

**REQ-1.2:** The system SHALL verify SYSTEM_ALERT_WINDOW permission is declared in AndroidManifest.xml

**REQ-1.3:** The system SHALL add runtime permission check in VoiceOSService.onCreate() if not present

**REQ-1.4:** The system SHALL restore consent dialog functionality for new app detection

**Acceptance Criteria:**
- LearnAppIntegration initializes successfully on VoiceOSService startup
- Consent dialog appears within 500ms when new app is launched
- Permission flow works correctly if overlay permission not granted

---

### REQ-2: Three Learning Modes

**REQ-2.1:** The system SHALL support three distinct learning modes:
1. Auto-Detect Mode (consent dialog on app launch)
2. Manual Learning Mode (user-initiated from settings)
3. Just-in-Time Learning Mode (learn screen-by-screen during use)

**REQ-2.2:** The system SHALL allow users to switch between modes at any time

**REQ-2.3:** The system SHALL persist user's mode preference across app restarts

**Acceptance Criteria:**
- All three modes work independently
- User can switch modes from settings
- Mode preference persisted to database

---

### REQ-3: Auto-Detect Mode (Improved)

**REQ-3.1:** The consent dialog SHALL explain what "learning" means in user-friendly language

**REQ-3.2:** The consent dialog SHALL show estimated time (2-3 minutes) and benefits

**REQ-3.3:** The consent dialog SHALL offer "Not Now" instead of "Decline"

**REQ-3.4:** The consent dialog SHALL include "Don't ask again for this app" checkbox

**REQ-3.5:** The consent dialog SHALL include "Disable auto-detection for all apps" option

**REQ-3.6:** The consent dialog SHALL include "Skip" option to enable just-in-time learning mode for this app

**Acceptance Criteria:**
- Consent dialog is informative and non-intrusive
- All options (Not Now, Don't Ask Again, Disable All, Skip) work correctly
- Skip option activates just-in-time mode for the specific app
- User preferences are persisted

---

### REQ-4: Manual Learning Mode (Settings UI)

**REQ-4.1:** The system SHALL provide a "Voice Learning" settings screen

**REQ-4.2:** The settings screen SHALL display all installed apps with learning status:
- ✅ Learned (shows command count, last updated)
- ⏳ Learning (shows progress %, screens explored)
- ⚪ Not Learned (shows action buttons)
- 🔄 Just-in-Time Mode (shows screens learned so far)
- ❌ Failed (shows error, retry option)

**REQ-4.3:** The settings screen SHALL provide search functionality

**REQ-4.4:** The settings screen SHALL provide filter options:
- All apps
- Learned apps only
- Not learned apps
- In progress
- Just-in-time mode

**REQ-4.5:** The settings screen SHALL provide sort options:
- Alphabetical (A-Z)
- Recently used
- Learning status

**REQ-4.6:** Each app SHALL display quick actions:
- "Learn this app" - Start full learning
- "Just-in-time mode" - Enable screen-by-screen learning
- "Re-learn" - Update existing learning (for learned apps)
- "View commands" - Show generated commands (for learned apps)

**REQ-4.7:** The system SHALL show real-time learning progress when app is being learned

**Acceptance Criteria:**
- Settings UI displays all apps correctly
- Search, filter, and sort work correctly
- All quick actions function as expected
- Learning progress updates in real-time
- Just-in-time mode can be activated from settings

---

### REQ-5: Just-in-Time Learning Mode (NEW)

**REQ-5.1:** When just-in-time mode is enabled for an app, the system SHALL:
- Monitor AccessibilityEvents when user uses the app
- Learn only the screens the user actually visits
- Learn only the elements the user actually interacts with

**REQ-5.2:** The system SHALL show a subtle notification when learning a new screen:
- "Learning this screen..." (non-intrusive toast or small overlay)
- Duration: 1-2 seconds, auto-dismiss

**REQ-5.3:** The system SHALL NOT perform automatic exploration in just-in-time mode

**REQ-5.4:** The system SHALL allow user to convert just-in-time mode to full learning at any time

**REQ-5.5:** The system SHALL persist screen learning incrementally (after each screen visited)

**REQ-5.6:** In settings, just-in-time apps SHALL show:
- Number of screens learned so far
- List of learned screens
- Option to "Complete learning" (switch to full exploration)

**Acceptance Criteria:**
- Just-in-time mode learns screens passively as user navigates
- No performance impact on app usage
- User can see what has been learned so far
- User can upgrade to full learning anytime
- Learning persists correctly to database

---

### REQ-6: Learning Flow (User-Initiated)

**REQ-6.1:** When user initiates learning, the system SHALL show pre-learning instructions:
- What will happen
- Estimated time (2-3 minutes)
- Login requirement question

**REQ-6.2:** The system SHALL ask "Do you need to log in first?" with options:
- "I'm already logged in"
- "I need to log in first"

**REQ-6.3:** If "Need to log in" selected, the system SHALL:
- Pause learning flow
- Launch the app
- Show "Please log in" dialog
- Wait for user confirmation
- Resume learning after user confirms

**REQ-6.4:** During learning, the system SHALL show real-time progress:
- Overall progress percentage
- Screens explored count
- Current screen being learned
- Pause and Cancel buttons

**REQ-6.5:** On learning completion, the system SHALL show summary:
- "Learning complete!"
- Sample voice commands generated
- Command count
- "View all commands" and "Done" buttons

**Acceptance Criteria:**
- Pre-learning flow is clear and helpful
- Login detection and pause/resume works
- Progress UI updates in real-time
- Completion summary is informative
- User can pause or cancel at any point

---

### REQ-7: Login Flow Support

**REQ-7.1:** The system SHALL detect when an app requires login:
- Presence of "Log in" button
- Presence of email/password fields
- Empty screen with minimal elements

**REQ-7.2:** When login screen detected, the system SHALL:
- Pause exploration automatically
- Show "Please log in" dialog to user
- Wait for user to confirm login complete
- Resume exploration after confirmation

**REQ-7.3:** The system SHALL NOT attempt to learn login screens or enter credentials

**REQ-7.4:** The system SHALL skip login screens in generated commands

**Acceptance Criteria:**
- Login screens detected correctly (95%+ accuracy)
- Exploration pauses automatically
- User can complete login without interference
- Learning resumes correctly after login
- Login screens not included in command generation

---

### REQ-8: Onboarding Experience

**REQ-8.1:** On first launch, the system SHALL show 3-screen onboarding flow:
1. Welcome screen (explains VoiceOS + voice control)
2. How Learning Works (explains exploration and command generation)
3. Choose Learning Mode (auto-detect, manual, or guided)

**REQ-8.2:** The onboarding SHALL explain each learning mode clearly:
- **Automatic:** "We'll ask when you open new apps"
- **Manual:** "You choose which apps to learn from settings"
- **Just-in-Time:** "Learn apps naturally as you use them"
- **Guided:** "We'll help you learn 3 popular apps now"

**REQ-8.3:** If user selects "Guided," the system SHALL:
- Offer to learn Chrome, Gmail, and one user-chosen app
- Walk through manual learning flow for each
- Explain features as they're demonstrated

**REQ-8.4:** The system SHALL remember onboarding was completed

**REQ-8.5:** The system SHALL allow users to re-run onboarding from settings

**Acceptance Criteria:**
- Onboarding shows on first launch only
- All 3 screens are clear and informative
- Mode selection works correctly
- Guided mode successfully learns 3 apps
- Onboarding can be manually triggered from settings

---

### REQ-9: Database Schema Extensions

**REQ-9.1:** The system SHALL extend learned_apps table with new fields:
- `learning_mode` (AUTO_DETECT, MANUAL, JUST_IN_TIME)
- `status` (NOT_LEARNED, LEARNING, LEARNED, FAILED, JIT_ACTIVE)
- `progress` (0-100)
- `command_count` (number of generated commands)
- `screens_explored` (count)
- `total_screens` (estimate, nullable)

**REQ-9.2:** The system SHALL create user_preferences table:
- `key` (preference name)
- `value` (preference value as string)
- Store: auto_detect_enabled, show_consent_dialog, onboarding_completed, default_learning_mode

**REQ-9.3:** The system SHALL create app_consent_history table:
- `package_name`
- `user_choice` (APPROVED, DECLINED, DONT_ASK_AGAIN, SKIPPED)
- `timestamp`

**REQ-9.4:** The system SHALL use INSERT OR REPLACE for all upsert operations (consistent with ADR-010)

**Acceptance Criteria:**
- All new database tables created correctly
- Existing data migrates without loss
- INSERT OR REPLACE prevents constraint violations
- Repository layer provides clean API for new tables

---

### REQ-10: Performance & Battery

**REQ-10.1:** App exploration SHALL run in a foreground service with notification

**REQ-10.2:** Just-in-time learning SHALL have minimal performance overhead:
- < 50ms latency on AccessibilityEvent processing
- < 10MB memory overhead
- No noticeable impact on app responsiveness

**REQ-10.3:** The system SHALL batch database writes during exploration:
- Use transactions for multi-row inserts
- Commit every 10 screens max or every 30 seconds

**REQ-10.4:** The system SHALL pause learning if device is low on battery (<15%)

**REQ-10.5:** The system SHALL use Dispatchers.Default for all heavy computation

**Acceptance Criteria:**
- Exploration service shows persistent notification
- Just-in-time mode has no perceptible performance impact
- Battery usage is reasonable (<5% per hour during active learning)
- Low battery triggers automatic pause

---

### REQ-11: Accessibility Compliance

**REQ-11.1:** All new UI elements SHALL support TalkBack

**REQ-11.2:** All interactive elements SHALL have minimum touch target 48x48dp

**REQ-11.3:** All text SHALL support dynamic font sizing

**REQ-11.4:** All UI SHALL maintain 4.5:1 contrast ratio minimum

**REQ-11.5:** All dialogs and overlays SHALL be navigable via keyboard/switch access

**Acceptance Criteria:**
- TalkBack announces all UI elements correctly
- Touch targets meet minimum size
- UI scales correctly with Large/Huge font sizes
- Contrast analyzer shows 4.5:1+ for all text
- All flows completable via switch access

---

## User Scenarios

### Scenario 1: First-Time User - Onboarding

**Given:** User installs VoiceOS and launches for first time
**When:** User completes onboarding
**Then:**
- User sees 3-screen onboarding flow
- User understands what learning means
- User chooses a learning mode
- User's choice is persisted

### Scenario 2: Power User - Manual Learning

**Given:** User wants to learn Instagram manually
**When:** User opens Settings → Voice Learning
**Then:**
- User sees Instagram in app list (Not Learned status)
- User taps "Learn this app"
- System asks about login requirement
- User confirms "Need to log in first"
- Instagram launches, user logs in
- User taps "I'm logged in - Continue"
- System explores Instagram (shows progress)
- Learning completes successfully
- User sees 47 voice commands generated

### Scenario 3: Casual User - Just-in-Time Learning

**Given:** User installs Spotify but doesn't want to wait for full learning
**When:** Spotify launches for first time
**Then:**
- Consent dialog appears
- User taps "Skip" button
- Just-in-time mode activates for Spotify
- User explores Spotify naturally (Home, Search, Library)
- Each screen is learned passively as user visits it
- User sees subtle "Learning this screen..." toasts
- After 2 weeks, user has 12 screens learned organically
- User can view learned commands in settings

### Scenario 4: Accessibility User - Auto-Detect

**Given:** User relies on voice control and wants all apps learned
**When:** User launches a new app (Gmail)
**Then:**
- Improved consent dialog appears
- Dialog explains benefits clearly
- User taps "Learn Now"
- Gmail learning starts immediately
- Progress shown in real-time
- Learning completes in 2-3 minutes
- User can now control Gmail with voice

### Scenario 5: Busy User - Defer Learning

**Given:** User is in the middle of urgent task
**When:** New app launches and consent dialog appears
**Then:**
- User taps "Not Now"
- Dialog dismisses without nagging
- App remains in "Not Learned" status
- User can learn later from settings when convenient

---

## Technical Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────┐
│ VoiceOSService                                  │
│ (AccessibilityService)                          │
├─────────────────────────────────────────────────┤
│ • onCreate() - Initialize LearnAppIntegration   │
│ • onAccessibilityEvent() - Forward to detectors │
└──────────┬──────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────┐
│ LearnAppIntegration                             │
├─────────────────────────────────────────────────┤
│ • AppLaunchDetector                             │
│ • ConsentDialogManager                          │
│ • ExplorationEngine                             │
│ • JustInTimeLearner (NEW)                       │
└──────────┬──────────────────────────────────────┘
           │
           ├─────────────────────────┐
           │                         │
           ▼                         ▼
┌──────────────────────┐  ┌──────────────────────┐
│ VoiceLearningActivity│  │ ConsentDialogWidget  │
│ (Settings UI)        │  │ (Improved)           │
├──────────────────────┤  ├──────────────────────┤
│ • AppListViewModel   │  │ • Show benefits      │
│ • Search/Filter      │  │ • Skip button (NEW)  │
│ • Learning modes     │  │ • Don't ask again    │
└──────────┬───────────┘  └──────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────┐
│ LearnAppDatabaseAdapter                         │
├─────────────────────────────────────────────────┤
│ Repositories:                                   │
│ • LearnedAppRepository                          │
│ • ExplorationSessionRepository                  │
│ • UserPreferenceRepository (NEW)                │
│ • ConsentHistoryRepository (NEW)                │
└──────────┬──────────────────────────────────────┘
           │
           ▼
┌─────────────────────────────────────────────────┐
│ SQLDelight (voiceos.db)                         │
├─────────────────────────────────────────────────┤
│ Tables:                                         │
│ • learned_apps (extended with new fields)       │
│ • exploration_sessions                          │
│ • navigation_edges                              │
│ • screen_state                                  │
│ • user_preferences (NEW)                        │
│ • app_consent_history (NEW)                     │
└─────────────────────────────────────────────────┘
```

### New Components

1. **JustInTimeLearner** - Passive screen-by-screen learning
2. **VoiceLearningActivity** - Settings UI for manual learning
3. **AppLearningViewModel** - State management for settings UI
4. **UserPreferenceRepository** - Persist user settings
5. **ConsentHistoryRepository** - Track consent decisions
6. **OnboardingActivity** - First-run experience

---

## Testing Requirements

### Unit Tests (90%+ coverage)

**Critical Paths:**
1. LearnAppIntegration initialization
2. AppLaunchDetector event filtering
3. ConsentDialogManager permission checking
4. JustInTimeLearner passive learning
5. ExplorationEngine full exploration
6. All repository operations
7. All ViewModel state transitions

### Integration Tests

1. Full learning flow (launch → consent → explore → complete)
2. Just-in-time flow (skip → passive learn → view progress)
3. Login detection and pause/resume
4. Settings UI interactions
5. Onboarding flow
6. Database persistence across modes

### UI Tests

1. Consent dialog interactions (all buttons)
2. Settings screen (search, filter, sort)
3. Learning progress updates
4. Onboarding screens
5. TalkBack navigation

### Performance Tests

1. Just-in-time learning latency (<50ms)
2. Memory usage during exploration (<50MB overhead)
3. Battery usage (< 5% per hour)
4. Database write throughput (batch operations)

---

## Success Metrics

### Functional Metrics
- ✅ Consent dialog shows within 500ms of new app launch
- ✅ Users can manually initiate learning from settings
- ✅ Just-in-time mode learns screens passively with no user interruption
- ✅ Learning progress visible in real-time
- ✅ Login flow doesn't break learning

### UX Metrics
- 🎯 80%+ of users understand what "learning" does
- 🎯 60%+ of users prefer manual or just-in-time over auto-detect
- 🎯 90%+ successful learning completions (vs failures)
- 🎯 <5% of users disable auto-detection entirely
- 🎯 40%+ of users use just-in-time mode for at least 1 app

### Performance Metrics
- 🎯 <50ms latency for just-in-time learning
- 🎯 <5% battery per hour during active learning
- 🎯 0 ANRs (Application Not Responding) during exploration
- 🎯 <2% crash rate

---

## Constraints & Assumptions

### Technical Constraints

1. **SQLDelight Database:** Must use existing SQLDelight architecture
2. **Performance:** Must not impact device performance or battery significantly
3. **Accessibility:** All UI must meet Android accessibility standards
4. **Permissions:** Requires SYSTEM_ALERT_WINDOW for overlays

### Business Constraints

1. **Timeline:** Phase 1 (immediate fix) must ship ASAP
2. **Backward Compatibility:** Existing learned apps must continue working
3. **User Trust:** Cannot learn apps without explicit user consent

### Assumptions

1. Users have granted SYSTEM_ALERT_WINDOW permission
2. Users have enabled VoiceOS AccessibilityService
3. Apps don't actively block AccessibilityService
4. Device has sufficient storage for learned app data
5. Network connectivity not required for learning (local only)

---

## Out of Scope (Future Enhancements)

### V2 Features (Post-MVP)
1. Selective Learning - Choose specific screens to learn
2. Learning Templates - Pre-learned popular apps
3. Cloud Sync - Share learned apps across devices
4. Community Learning - Crowdsourced app mappings
5. Smart Retry - Auto-retry failed learning attempts
6. Learning Analytics - Which apps users learn most

### V3 Features (Long-term)
1. AI-Powered Learning - LLM understands app purpose
2. Natural Language Commands - "Send email to John" vs "Tap compose"
3. Context-Aware Learning - Learn based on usage patterns
4. Cross-App Workflows - Chain commands across apps

---

## Dependencies

### Internal Dependencies
- VoiceOSCore module (existing)
- SQLDelight database (newly migrated)
- AccessibilityService infrastructure
- VoiceOSDatabaseManager

### External Dependencies
- Android SDK 24+ (AccessibilityService)
- AndroidX libraries
- Kotlin Coroutines
- Jetpack Compose (for new UI components)
- Material Design 3

---

## Risks & Mitigations

### Risk 1: Performance Impact (Just-in-Time Mode)

**Risk:** Passive learning on every AccessibilityEvent could slow down apps

**Likelihood:** Medium
**Impact:** High (user-facing performance degradation)

**Mitigation:**
- Aggressive event filtering (only TYPE_WINDOW_STATE_CHANGED)
- Debouncing (100ms window minimum)
- Background processing on Dispatchers.Default
- Performance tests with target <50ms latency

### Risk 2: Battery Drain

**Risk:** Continuous AccessibilityEvent monitoring drains battery

**Likelihood:** Medium
**Impact:** High (user complaints, uninstalls)

**Mitigation:**
- Efficient event filtering
- Batch database writes
- Auto-pause on low battery (<15%)
- Foreground service for full exploration only
- Performance monitoring

### Risk 3: App Detection False Positives

**Risk:** Login screen detection incorrectly pauses on non-login screens

**Likelihood:** Low
**Impact:** Medium (learning fails to complete)

**Mitigation:**
- Multi-factor login detection (button + fields + screen emptiness)
- User manual override ("I'm logged in" button)
- Logging and telemetry to improve detection
- Timeout (resume after 5 minutes if no confirmation)

### Risk 4: User Confusion (Too Many Modes)

**Risk:** Three learning modes confuse users

**Likelihood:** Medium
**Impact:** Medium (poor UX, mode switching)

**Mitigation:**
- Clear onboarding explaining each mode
- Sensible defaults (Auto-detect for most users)
- Mode recommendations in settings
- User testing to validate clarity

---

## Rollout Plan

### Stage 1: Stealth Fix (Week 1)
**Goal:** Restore basic functionality

**Tasks:**
- Uncomment LearnAppIntegration initialization
- Verify permission handling
- Test consent dialog appears
- Monitor crash logs

**Success Criteria:**
- 0 crashes related to LearnAppIntegration
- Consent dialog shows for new apps
- 90%+ permission grant rate

### Stage 2: Settings UI (Week 2-3)
**Goal:** Manual learning capability

**Tasks:**
- Implement VoiceLearningActivity
- Add all three mode buttons (Learn, Just-in-Time, Auto-detect)
- Add search/filter/sort
- Test with 10-20 apps

**Success Criteria:**
- Settings UI stable and performant
- Manual learning works end-to-end
- Just-in-time mode activates correctly

### Stage 3: Improved Consent Dialog (Week 4)
**Goal:** Better auto-detection UX

**Tasks:**
- Redesign ConsentDialogWidget
- Add Skip button for just-in-time mode
- Add "Don't ask again" options
- A/B test old vs new dialog

**Success Criteria:**
- Higher consent approval rate (target: 60%+)
- Skip button usage tracked
- <10% "disable all" usage

### Stage 4: Login Support + Onboarding (Week 5)
**Goal:** Complete feature set

**Tasks:**
- Implement login detection
- Create OnboardingActivity
- Add guided learning mode
- Final polish and bug fixes

**Success Criteria:**
- Login detection 95%+ accurate
- Onboarding completion rate 80%+
- All quality gates passing

---

## Appendix A: Database Schema

### Extended learned_apps Table

```sql
CREATE TABLE learned_apps (
    package_name TEXT PRIMARY KEY NOT NULL,
    app_name TEXT NOT NULL,
    learning_mode TEXT NOT NULL, -- AUTO_DETECT, MANUAL, JUST_IN_TIME
    status TEXT NOT NULL, -- NOT_LEARNED, LEARNING, LEARNED, FAILED, JIT_ACTIVE
    progress INTEGER NOT NULL DEFAULT 0, -- 0-100
    command_count INTEGER NOT NULL DEFAULT 0,
    screens_explored INTEGER NOT NULL DEFAULT 0,
    total_screens INTEGER, -- nullable, estimate
    last_updated INTEGER NOT NULL,
    first_learned INTEGER,
    version_code INTEGER,
    is_auto_detect_enabled INTEGER NOT NULL DEFAULT 1 -- boolean
);
```

### New user_preferences Table

```sql
CREATE TABLE user_preferences (
    key TEXT PRIMARY KEY NOT NULL,
    value TEXT NOT NULL
);

-- Example rows:
-- ('auto_detect_enabled', 'true')
-- ('show_consent_dialog', 'true')
-- ('onboarding_completed', 'true')
-- ('default_learning_mode', 'AUTO_DETECT')
```

### New app_consent_history Table

```sql
CREATE TABLE app_consent_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    package_name TEXT NOT NULL,
    user_choice TEXT NOT NULL, -- APPROVED, DECLINED, DONT_ASK_AGAIN, SKIPPED
    timestamp INTEGER NOT NULL,
    FOREIGN KEY (package_name) REFERENCES learned_apps(package_name) ON DELETE CASCADE
);

CREATE INDEX idx_consent_package ON app_consent_history(package_name);
CREATE INDEX idx_consent_timestamp ON app_consent_history(timestamp);
```

---

## Appendix B: UI Mockups

### Settings Screen - App List

```
┌─────────────────────────────────────────────┐
│ Voice Learning                         [?]  │
├─────────────────────────────────────────────┤
│ 🔍 Search apps...                      [≡]  │
│                                             │
│ ╔═══════════════════════════════════════╗ │
│ ║ 📊 Chrome                             ║ │
│ ║ ✅ Learned • 47 commands              ║ │
│ ║ Last updated: 2 hours ago             ║ │
│ ║ [Re-learn] [Commands]                 ║ │
│ ╚═══════════════════════════════════════╝ │
│                                             │
│ ┌───────────────────────────────────────┐ │
│ │ 📧 Gmail                              │ │
│ │ ⏳ Learning... 60% complete           │ │
│ │ 23 of 38 screens explored             │ │
│ │ [Cancel]                              │ │
│ └───────────────────────────────────────┘ │
│                                             │
│ ┌───────────────────────────────────────┐ │
│ │ 🎵 Spotify                            │ │
│ │ 🔄 Just-in-Time • 12 screens learned  │ │
│ │ Learning as you use it                │ │
│ │ [Complete Learning] [Commands]        │ │
│ └───────────────────────────────────────┘ │
│                                             │
│ ┌───────────────────────────────────────┐ │
│ │ 📸 Instagram                          │ │
│ │ ⚪ Not learned                        │ │
│ │ [Learn] [Just-in-Time] [Auto-detect]  │ │
│ └───────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

### Improved Consent Dialog with Skip Button

```
┌─────────────────────────────────────────────┐
│ 🆕 New App Detected: Spotify          [✕]  │
├─────────────────────────────────────────────┤
│                                             │
│ 🎤 Would you like to learn Spotify?         │
│                                             │
│ This will:                                  │
│ • Map buttons and screens (2-3 min)         │
│ • Create ~30-50 voice commands              │
│ • Let you control Spotify with voice        │
│                                             │
│ Or:                                         │
│ • Skip to learn naturally as you use it     │
│ • Learn later from Settings                 │
│                                             │
│ ☐ Don't ask again for Spotify               │
│ ☐ Disable auto-detection for all apps       │
│                                             │
│     [Not Now]  [Skip]  [Learn Now]          │
└─────────────────────────────────────────────┘
```

---

**Specification Version:** 1.0
**Status:** Ready for Review
**Next Step:** Generate Implementation Plan
