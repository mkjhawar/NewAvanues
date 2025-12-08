# Feature Specification: LearnApp UI Migration to Legacy Android Widgets

**Spec Number:** 001
**Feature:** LearnApp Compose to Widget Migration
**Created:** 2025-10-24
**Status:** Draft
**Priority:** P1 (High - Blocking Production)

---

## Problem Statement

### Current Situation

LearnApp currently uses Jetpack Compose for its overlay UI components (consent dialogs, login prompts, progress indicators). When these Compose views are displayed from an AccessibilityService context, they fail with multiple exceptions because Compose expects to run within an Activity lifecycle, which AccessibilityServices don't provide.

### Core Issues

1. **ViewTreeLifecycleOwner not found** - Compose cannot find a lifecycle owner in the AccessibilityService window
2. **IllegalStateException on main thread** - Lifecycle observers must be added on main thread but AccessibilityService runs on different thread
3. **BadTokenException** - Window token is null because there's no Activity context

### Impact

- ❌ **BLOCKING:** LearnApp UI components crash when displayed
- ❌ **BLOCKING:** Users cannot give consent for learned commands
- ❌ **BLOCKING:** Login prompts fail to display
- ❌ **BLOCKING:** Progress indicators crash the service
- 🔴 **Critical accessibility feature non-functional**

---

## Goals & Objectives

### Primary Goal

Migrate all LearnApp UI components from Jetpack Compose to legacy Android widget APIs (`android.widget.*`) to work reliably in AccessibilityService context.

### Success Criteria

1. All 5 LearnApp UI components display without crashes in AccessibilityService context
2. UI components are functionally equivalent to current Compose implementations
3. All existing features preserved (consent flow, login, progress tracking)
4. Zero ViewTreeLifecycleOwner exceptions
5. Zero thread-related exceptions
6. Zero window token exceptions
7. Maintains current UX/visual design as closely as possible
8. Performance remains within budget (<100ms display time)

### Non-Goals

- NOT redesigning the UX flow
- NOT changing the consent/login logic
- NOT migrating other modules to widgets
- NOT adding new features during migration

---

## User Stories

### P1: Critical - Must Have (MVP)

#### Story 1.1: Consent Dialog Display
**As a** VoiceOS user
**I want** the consent dialog to display without crashing
**So that** I can approve learned voice commands

**Acceptance Criteria:**
- Consent dialog appears when new command is learned
- Dialog displays all required information (command, explanation, privacy notice)
- Dialog has "Allow" and "Deny" buttons
- Dialog does not crash with ViewTreeLifecycleOwner exception
- Dialog is displayed from AccessibilityService context successfully

**Affected Files:**
- `ConsentDialogManager.kt`
- `ConsentDialog.kt`

---

#### Story 1.2: Login Prompt Display
**As a** VoiceOS user
**I want** the login prompt to display without crashing
**So that** I can authenticate to use LearnApp features

**Acceptance Criteria:**
- Login prompt appears when authentication is required
- Prompt displays username/password fields (or appropriate auth method)
- Prompt has login/cancel buttons
- Prompt does not crash with BadTokenException
- Prompt is displayed from AccessibilityService context successfully

**Affected Files:**
- `LoginPromptOverlay.kt`

---

#### Story 1.3: Progress Indicator Display
**As a** VoiceOS user
**I want** progress indicators to display without crashing
**So that** I can see feedback during LearnApp operations

**Acceptance Criteria:**
- Progress overlay appears during long operations
- Overlay shows loading indicator and message
- Overlay does not crash with thread exceptions
- Overlay dismisses properly when operation completes
- Overlay is displayed from AccessibilityService context successfully

**Affected Files:**
- `ProgressOverlay.kt`
- `ProgressOverlayManager.kt`

---

### P2: Important - Should Have

#### Story 2.1: Visual Parity with Compose
**As a** VoiceOS user
**I want** the migrated widgets to look similar to the current Compose UI
**So that** I have a consistent experience

**Acceptance Criteria:**
- Widget layouts match Compose layouts (spacing, sizes, colors)
- Material Design 3 styling applied to widgets where possible
- Rounded corners, shadows, elevation preserved
- Typography matches current design
- Icons and images displayed correctly

---

#### Story 2.2: Accessibility Features Preserved
**As a** VoiceOS user with accessibility needs
**I want** all accessibility features to work in the widget implementation
**So that** I can navigate the UI with assistive technologies

**Acceptance Criteria:**
- All interactive elements have content descriptions
- Focus order is logical
- TalkBack compatibility verified
- Touch targets meet minimum size requirements (48dp)
- Contrast ratios meet WCAG AA standards

---

### P3: Nice to Have - Could Have

#### Story 3.1: Animation Support
**As a** VoiceOS user
**I want** smooth animations when dialogs appear/disappear
**So that** I have a polished experience

**Acceptance Criteria:**
- Fade in/out animations for dialogs
- Slide animations for overlays
- Animation duration <300ms
- No jank or frame drops

---

## Technical Context

### Current Implementation (Compose)

**Technology:**
- Jetpack Compose 1.6.8
- Material Design 3 components
- Compose runtime and lifecycle integration
- ComposeView for window attachment

**Lifecycle Management:**
- Custom `MyLifecycleOwner` to provide lifecycle in non-Activity context
- SavedStateRegistry for state preservation
- ViewTreeLifecycleOwner association

**Thread Handling:**
- Coroutine-based with Dispatchers
- Main thread enforcement for lifecycle operations
- Recomposer management

### Target Implementation (Widgets)

**Technology:**
- Legacy Android widget APIs (`android.widget.*`)
- XML layouts or programmatic layout creation
- Traditional View hierarchy
- AlertDialog or custom Dialog for modal overlays

**Lifecycle Management:**
- Direct window manager interaction
- No lifecycle owner required
- Manual state management

**Thread Handling:**
- Handler-based main thread posting
- No lifecycle observer overhead
- WindowManager.addView() for overlay display

---

## Constraints

### Technical Constraints

1. **AccessibilityService Context:**
   - MUST work in AccessibilityService, not Activity
   - MUST use TYPE_ACCESSIBILITY_OVERLAY window type
   - MUST handle window permissions correctly

2. **Performance:**
   - Display time <100ms (per VOS4 constitution)
   - Memory footprint should not increase >5MB
   - No memory leaks (LeakCanary verification required)

3. **Threading:**
   - All UI operations on main thread
   - No lifecycle observer thread violations
   - Coroutine integration preserved for non-UI logic

4. **Compatibility:**
   - Support Android API 29-34 (Android 10-15)
   - No deprecated API usage
   - Forward compatibility with future Android versions

### Design Constraints

1. **Visual Consistency:**
   - Match current Compose UI as closely as possible
   - Maintain Material Design 3 principles
   - Preserve brand colors and typography

2. **Accessibility:**
   - Meet WCAG AA standards
   - TalkBack compatible
   - Voice-navigable (primary VOS4 requirement)

3. **UX Flow:**
   - No changes to consent flow logic
   - No changes to login flow logic
   - Same button placement and labels

---

## Success Metrics

### Functional Metrics

- ✅ **0 crashes** from ViewTreeLifecycleOwner exceptions (currently: multiple)
- ✅ **0 crashes** from thread-related exceptions (currently: multiple)
- ✅ **0 crashes** from window token exceptions (currently: multiple)
- ✅ **100% feature parity** with current Compose implementation
- ✅ **All 5 files** successfully migrated and tested

### Performance Metrics

- ✅ **Display latency <100ms** (measured from trigger to visible)
- ✅ **Memory increase <5MB** (compared to Compose baseline)
- ✅ **0 memory leaks** (LeakCanary verification)
- ✅ **Build time unchanged** or faster (Compose compilation removed)

### Quality Metrics

- ✅ **80%+ test coverage** for migrated components (per constitution)
- ✅ **0 compiler warnings** (per constitution)
- ✅ **UI tests passing** for all user stories
- ✅ **Manual QA passed** on 3+ Android versions (API 29, 32, 34)

---

## Dependencies

### Internal Dependencies

- `VoiceOSCore` - AccessibilityService context provider
- `LearnAppIntegration` - Event listeners and flow integration
- `WindowManager` - Overlay display management

### External Dependencies

- Android SDK (API 29-34)
- Material Components library (for Material Design widgets)
- Kotlin Coroutines (for async operations, non-UI)

### Breaking Changes

**NONE** - This is an internal implementation change. External APIs remain unchanged.

---

## Risk Assessment

### High Risk

1. **UX Degradation:**
   - Risk: Widget implementation may not look as polished as Compose
   - Mitigation: Invest time in XML styling, use Material Components library
   - Fallback: Accept minor visual differences if functionally equivalent

2. **Thread Complexity:**
   - Risk: Manual thread management may introduce new bugs
   - Mitigation: Use Handler.post() consistently, thorough testing
   - Fallback: Wrapper utilities for thread safety

### Medium Risk

3. **State Management:**
   - Risk: Losing saved state functionality without SavedStateRegistry
   - Mitigation: Implement custom state saving with Bundle
   - Fallback: Simplified state management if complex state not needed

4. **Testing Coverage:**
   - Risk: UI testing may be harder with widgets vs Compose
   - Mitigation: Use Espresso for widget testing, Robolectric for unit tests
   - Fallback: Increase manual testing if automated testing difficult

### Low Risk

5. **Performance Regression:**
   - Risk: Widget rendering slower than Compose
   - Mitigation: Profile before/after, optimize as needed
   - Likelihood: Low - widgets typically faster

---

## Open Questions

### Resolved

1. ~~Should we use XML layouts or programmatic view creation?~~
   - **Decision:** Use XML layouts for complex views (easier to maintain), programmatic for simple overlays
   - **Rationale:** XML provides better separation of layout from logic

2. ~~Which dialog mechanism: AlertDialog vs custom Dialog vs WindowManager?~~
   - **Decision:** Use AlertDialog for consent/login, WindowManager for progress overlay
   - **Rationale:** AlertDialog handles lifecycle better for modal dialogs, WindowManager needed for non-modal progress

### To Be Resolved During Planning

3. How to handle Material Design 3 theming in legacy widgets?
   - Options: Material Components library, custom styling, simplified design
   - Decision needed in planning phase

4. Should we preserve animations or simplify to instant display?
   - Options: View animations, property animations, no animations
   - Decision based on performance budget in planning phase

5. How to structure test suite for widgets?
   - Options: Espresso integration tests, Robolectric unit tests, manual QA only
   - Decision needed in planning phase

---

## Acceptance Tests

### Test 1: Consent Dialog Displays Without Crash

**Given:** User triggers a learned command
**When:** LearnApp needs to show consent dialog
**Then:**
- Dialog appears on screen within 100ms
- No ViewTreeLifecycleOwner exception thrown
- No BadTokenException thrown
- Dialog content is readable
- "Allow" and "Deny" buttons are clickable

---

### Test 2: Login Prompt Displays Without Crash

**Given:** User accesses LearnApp feature requiring authentication
**When:** Login prompt is triggered
**Then:**
- Prompt appears on screen within 100ms
- No thread-related exceptions thrown
- No window token exceptions thrown
- Input fields are editable
- Login button is clickable

---

### Test 3: Progress Overlay Displays Without Crash

**Given:** User initiates long-running LearnApp operation
**When:** Progress overlay is shown
**Then:**
- Overlay appears on screen within 100ms
- Loading indicator is animating
- No lifecycle-related exceptions thrown
- Overlay dismisses when operation completes

---

### Test 4: Functional Equivalency Verified

**Given:** All 5 components migrated to widgets
**When:** Each component is triggered in normal usage
**Then:**
- All features work identically to Compose version
- User cannot tell the difference in functionality
- All data flows work correctly
- All callbacks fire correctly

---

### Test 5: Memory and Performance Verified

**Given:** Migrated widget implementation
**When:** Components are displayed and dismissed 50 times
**Then:**
- Memory usage stable (no leaks)
- Display latency <100ms every time
- No frame drops or jank
- LeakCanary reports 0 leaks

---

## Out of Scope

### Explicitly NOT Included

1. Migrating other modules from Compose to widgets
2. Redesigning the LearnApp UX flow
3. Adding new features to LearnApp
4. Changing the consent logic or data model
5. Performance optimizations beyond meeting budgets
6. Supporting Android versions <API 29

---

## References

### Issue Documentation

- LearnApp Compose UI Issue: `/Users/manoj_mbpm14/Downloads/junk/LearnApp Compose UI Issue_20251024.md`

### Exception Stack Traces

1. ViewTreeLifecycleOwner not found - Line 26-56 of issue doc
2. Method addObserver must be called on main thread - Line 58-73
3. BadTokenException - Line 75-144

### VOS4 Constitution References

- Performance-First Architecture (Principle I)
- Quality Through Enforcement (Principle V)
- Performance budgets: <100ms latency, <60MB memory
- Testing requirement: 80%+ coverage

---

**Next Steps:**

1. ✅ Specification approved
2. → Create implementation plan (`/idea.plan`)
3. → Generate task breakdown (`/idea.tasks`)
4. → Execute with IDE Loop (`/idea.implement`)

---

**End of Specification**
