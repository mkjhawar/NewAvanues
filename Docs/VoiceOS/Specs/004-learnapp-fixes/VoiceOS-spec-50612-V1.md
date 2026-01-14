# Feature Specification: LearnApp Critical Fixes

**Feature Branch**: `001-learnapp-fixes`
**Created**: 2025-10-28
**Status**: Draft
**Input**: User description: "Fix LearnApp critical issues: 1) Database tables (navigation_edges, screen_states) not being populated during exploration 2) Consent dialog flickering intermittently 3) Login prompt overlay manager not implemented (TODO) 4) Apps with continuous accessibility events (DeviceInfo) freezing UI/causing consent dialog to reappear 5) Package version info not retrieved from PackageManager (TODOs) 6) Error notification not implemented (TODO)"

## User Scenarios & Testing

### User Story 1 - Complete App Navigation Discovery (Priority: P1)

VoiceOS explores a third-party app and successfully captures all navigation paths between screens, storing screen states and navigation edges for future voice command generation.

**Why this priority**: Core functionality - without complete navigation data, voice commands cannot be reliably generated for app navigation. This is the foundation of the LearnApp learning system.

**Independent Test**: Launch LearnApp exploration on any standard app (e.g., Settings, Calculator), complete exploration, verify `navigation_edges` and `screen_states` tables contain records matching the explored screens.

**Acceptance Scenarios**:

1. **Given** LearnApp starts exploring an app with 5 distinct screens, **When** exploration completes successfully, **Then** `screen_states` table contains 5 records with unique screen identifiers and UI hierarchy data
2. **Given** LearnApp navigates from Screen A to Screen B via button click, **When** the navigation is captured, **Then** `navigation_edges` table contains a record showing the transition from Screen A to Screen B with the triggering action
3. **Given** LearnApp completes exploration of an app, **When** viewing the database, **Then** all explored screens have corresponding navigation edges showing how users can move between screens

---

### User Story 2 - Smooth Consent Experience (Priority: P1)

When VoiceOS detects a new app launch, users see a stable consent dialog without flickering or unexpected reappearances, allowing them to clearly choose whether to allow learning.

**Why this priority**: User trust and experience - flickering dialogs create confusion and undermine confidence in the system. Must be stable before users can make informed consent decisions.

**Independent Test**: Launch 3-5 different apps that VoiceOS hasn't learned yet, verify consent dialog appears once per app without flickering, remains stable until user responds, and doesn't reappear after user makes a choice.

**Acceptance Scenarios**:

1. **Given** user launches an unlearned app, **When** consent dialog appears, **Then** dialog remains stable without visual flickering or jittering
2. **Given** consent dialog is showing, **When** user clicks "Yes" to approve learning, **Then** dialog disappears and doesn't reappear during the current app session
3. **Given** consent dialog is showing, **When** user clicks "No" to decline learning, **Then** dialog disappears and doesn't reappear for this app during the current session
4. **Given** user selected "Don't ask again" and declined, **When** user launches the same app again, **Then** no consent dialog appears

---

### User Story 3 - Login Screen Handling (Priority: P2)

When LearnApp encounters a login screen during exploration, exploration pauses and presents users with clear options to handle the login requirement without losing exploration progress.

**Why this priority**: Prevents exploration failures on apps requiring authentication. Enhances learning coverage for authenticated apps.

**Independent Test**: Start learning an app with a login screen (e.g., banking app, social media), verify exploration pauses at login screen, login prompt overlay appears with clear options (Skip, Continue after manual login, or Stop).

**Acceptance Scenarios**:

1. **Given** LearnApp is exploring an app, **When** a login screen is detected, **Then** exploration pauses and a login prompt overlay appears explaining the situation
2. **Given** login prompt is showing "Skip" option, **When** user selects Skip, **Then** exploration resumes and bypasses the login screen
3. **Given** login prompt is showing "Continue" option, **When** user selects Continue and manually logs in, **Then** exploration resumes from the post-login screen
4. **Given** login prompt is showing, **When** user selects "Stop", **Then** exploration terminates and progress overlay disappears

---

### User Story 4 - Reliable Exploration with Noisy Apps (Priority: P1)

When learning apps that generate continuous accessibility events (e.g., DeviceInfo showing real-time stats), LearnApp maintains stable UI and doesn't freeze or show repeated consent dialogs.

**Why this priority**: System reliability - apps with continuous events are common (monitoring apps, live data displays). System must handle them gracefully to be production-ready.

**Independent Test**: Launch LearnApp on an app that generates continuous accessibility events (DeviceInfo, system monitor, live weather), verify consent dialog appears once, progress overlay remains responsive, and exploration completes without freezes.

**Acceptance Scenarios**:

1. **Given** user approves learning for an app generating 10+ accessibility events per second, **When** exploration runs, **Then** progress overlay updates smoothly without freezing
2. **Given** exploration is running on a "noisy" app, **When** accessibility events flood in, **Then** consent dialog doesn't reappear unexpectedly
3. **Given** LearnApp is exploring a real-time data app, **When** screen content updates continuously, **Then** exploration completes within reasonable time (< 5 minutes for standard app) without hanging
4. **Given** user is waiting for exploration progress updates, **When** noisy events are being processed, **Then** progress overlay shows screen count updates at least every 3 seconds

---

### User Story 5 - Accurate App Version Tracking (Priority: P3)

When LearnApp completes learning an app, the system stores the exact version code and version name, enabling version-specific voice command management and update detection.

**Why this priority**: Data quality and future-proofing - enables tracking when apps update and voice commands may need re-learning. Lower priority as it doesn't block core functionality.

**Independent Test**: Complete learning for any app, query the `learned_apps` table, verify `versionCode` and `versionName` match the app's actual values from Android PackageManager.

**Acceptance Scenarios**:

1. **Given** LearnApp completes learning Calculator app v1.2.3 (version code 123), **When** data is saved, **Then** database record shows versionCode=123 and versionName="1.2.3"
2. **Given** an app updates from v1.0 to v2.0, **When** LearnApp encounters it again, **Then** system detects version change and prompts for re-learning
3. **Given** PackageManager returns null for version info, **When** saving learned app, **Then** system uses reasonable defaults (versionCode=1, versionName="unknown") and logs a warning

---

### User Story 6 - Clear Error Communication (Priority: P2)

When LearnApp encounters errors (session creation failure, exploration crash, database issues), users see clear error notifications explaining what went wrong and what to do next.

**Why this priority**: User experience and debugging - helps users understand failures and provides actionable guidance. Important for production reliability.

**Independent Test**: Trigger various error conditions (database locked, invalid package name, exploration timeout), verify error notifications appear with clear messages and don't leave UI in broken state.

**Acceptance Scenarios**:

1. **Given** LearnApp fails to create exploration session due to database lock, **When** error occurs, **Then** user sees notification: "Failed to Start Learning: Database is busy, please try again"
2. **Given** exploration encounters an unexpected crash, **When** error is caught, **Then** progress overlay hides, error notification shows, and user can launch other apps normally
3. **Given** user sees an error notification, **When** reading the message, **Then** message explains what went wrong in plain language without technical jargon or stack traces

---

### Edge Cases

- **Rapid app switching**: What happens when user approves learning for App A, then immediately switches to App B before exploration starts?
- **Concurrent accessibility events**: How does system handle processing exploration events while other apps (notifications, system UI) generate events simultaneously?
- **Partial exploration**: What happens when user force-stops an app mid-exploration? Is partial data saved or discarded?
- **Database contention**: How does system handle multiple concurrent database writes (e.g., navigation edge + screen state + learned app update)?
- **Memory pressure**: What happens when device is low on memory during exploration? Does exploration pause gracefully or crash?
- **Malformed screen hierarchy**: How does system handle apps with unusual/broken accessibility node trees?
- **Version downgrade**: What happens if user installs an older version of an app that was already learned?

## Requirements

### Functional Requirements

- **FR-001**: System MUST store navigation edges in `navigation_edges` table for each screen transition discovered during exploration
- **FR-002**: System MUST store screen states in `screen_states` table capturing UI hierarchy and element details for each unique screen
- **FR-003**: Consent dialog MUST remain stable without visual flickering or unexpected reappearances after user makes a choice
- **FR-004**: System MUST implement event debouncing or throttling to prevent continuous accessibility events from freezing UI or triggering duplicate consent dialogs
- **FR-005**: System MUST detect login screens during exploration and pause exploration with user-facing prompt
- **FR-006**: Login prompt overlay MUST provide three options: Skip (continue without login), Continue (pause for manual login), Stop (terminate exploration)
- **FR-007**: System MUST retrieve app version code and version name from Android PackageManager when saving learned app data
- **FR-008**: System MUST display user-friendly error notifications when exploration fails, session creation fails, or database operations fail
- **FR-009**: Error notifications MUST hide progress overlay and return UI to clean state
- **FR-010**: System MUST handle apps generating 10+ accessibility events per second without freezing or becoming unresponsive
- **FR-011**: System MUST prevent consent dialog from showing more than once per app per session (unless user explicitly resets "don't ask again" preference)
- **FR-012**: Database writes MUST be atomic to prevent partial data corruption during concurrent operations

### Key Entities

- **NavigationEdge**: Represents a transition from one screen to another, including the triggering action (button click, swipe, etc.), source screen identifier, destination screen identifier
- **ScreenState**: Represents a unique screen's UI hierarchy, including accessibility node tree, visible elements, element properties, screen identifier for matching with navigation edges
- **LearnedApp**: Represents a completed app learning session, including package name, app name, version code, version name, exploration statistics (screens explored, elements found), timestamp
- **ExplorationSession**: Represents an active or historical exploration session, tracks progress, current screen, errors encountered, start/end timestamps
- **ConsentDecision**: Represents user's consent choice for an app, including package name, approved/declined status, "don't ask again" flag, timestamp

## Success Criteria

### Measurable Outcomes

- **SC-001**: After exploring any app with 5+ screens, `navigation_edges` table contains at least 4 navigation records showing screen transitions
- **SC-002**: After exploring any app with 5+ screens, `screen_states` table contains at least 5 screen records with captured UI hierarchy
- **SC-003**: Consent dialog remains visually stable (zero flickers) for 100% of app launches across 10 different test apps
- **SC-004**: Progress overlay updates at least once every 3 seconds when exploring apps generating continuous accessibility events
- **SC-005**: Exploration completes within 5 minutes for standard apps (10-20 screens) including apps with continuous events
- **SC-006**: Login prompt overlay appears within 2 seconds of detecting a login screen during exploration
- **SC-007**: 100% of learned apps have accurate version code and version name matching PackageManager data (or logged defaults if unavailable)
- **SC-008**: Error notifications appear within 1 second of error occurrence and display user-friendly messages (no stack traces or technical jargon)
- **SC-009**: System successfully learns 95% of testable apps without crashes or UI freezes
- **SC-010**: Consent dialog appears exactly once per new app until user makes a choice (zero duplicate appearances)

## Assumptions

- Android PackageManager API is available and returns version information for all installed apps (standard Android behavior)
- Apps use standard Android accessibility framework (non-standard apps may have limited learnability)
- Device has sufficient storage for navigation graphs (estimated <1MB per app for typical 20-screen app)
- Users understand "Skip" vs "Continue" options in login prompt (clear labels and explanations provided)
- Database operations complete within 100ms under normal load (Room database standard performance)
- Accessibility events arrive on main thread or are properly dispatched (Android framework guarantee)
- Progress overlay is implemented using TYPE_ACCESSIBILITY_OVERLAY window type (already established in codebase)
- LearnApp database schema already defines `navigation_edges` and `screen_states` tables (structure exists, just not being populated)

## Constraints

- Must work within Android AccessibilityService limitations (cannot interact with secure screens like lock screen, system settings password entry)
- Cannot force apps to bypass login screens programmatically (security restriction)
- Limited to apps that expose accessibility information (some games and custom UI apps may not be learnable)
- Database writes during exploration must not impact app responsiveness (< 16ms per write to maintain 60fps UI)
- Must respect Android battery optimization policies (cannot use excessive CPU during exploration)

## Dependencies

- LearnApp database schema (already defined with `navigation_edges` and `screen_states` tables)
- ExplorationEngine (already implemented, needs fixes to populate database tables)
- ConsentDialogManager (already implemented, needs event throttling to prevent flickering)
- LoginPromptOverlay class (exists but not wired up, needs integration in LearnAppIntegration)
- ProgressOverlayManager (already implemented and working)
- Android PackageManager API (system API, always available)

## Out of Scope

- Automatic login credential entry (security risk, not supported)
- Learning apps that don't expose accessibility information (games, WebView-heavy apps, custom renderers)
- Voice command generation from learned data (separate feature, happens after learning completes)
- UI redesign of consent dialog or progress overlay (functionality fixes only, not visual redesign)
- Multi-device synchronization of learned apps (future enhancement)
- Incremental learning (re-learning only changed screens after app update)
- Performance optimization for apps with >100 screens (current focus on standard 10-20 screen apps)
