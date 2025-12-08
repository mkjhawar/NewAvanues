# VOS4 TODO/FIXME Issue Tracking

**Created**: 2025-11-13
**Status**: Active
**Purpose**: Track all TODO/FIXME comments as issues for proper project management

## Overview

This document tracks 24 TODO/FIXME comments found in production code, organized by priority and impact. Each TODO should be converted to a tracked issue (GitHub/GitLab) to ensure proper accountability and completion tracking.

---

## Priority 1 - High Impact (User-Facing) - 11 TODOs

### Issue #TODO-001: Unified Overlay System Integration
**Priority**: P1 - HIGH
**Impact**: User experience, UI consistency
**Effort**: 2-3 weeks
**Files Affected**: 3 files, 11 TODOs

**Description**: Implement unified OverlayManager to replace duplicate overlay logic across handlers.

**Current State**: Three handlers (NumberHandler, SelectHandler, HelpMenuHandler) have duplicate TODO comments for overlay integration:
- NumberHandler: Lines 450, 454, 462 (3 TODOs)
- SelectHandler: Lines 152, 190, 444, 479, 484, 489, 495, 501 (8 TODOs)
- HelpMenuHandler: Line 383 (1 TODO)

**Technical Details**:
- NumberHandler needs NumberOverlay integration (show/hide)
- SelectHandler needs context menu + cursor manager integration
- HelpMenuHandler needs overlay system for help display
- All three should use shared OverlayManager service

**Acceptance Criteria**:
1. Create `OverlayManager` class with standardized overlay lifecycle
2. Implement `NumberOverlay`, `SelectionOverlay`, `HelpOverlay` as concrete implementations
3. Refactor all three handlers to use OverlayManager
4. Remove all 11 TODO comments after integration
5. Add integration tests verifying overlay show/hide/lifecycle

**Dependencies**:
- Cursor manager API (for SelectHandler)
- WindowManager best practices (TYPE_ACCESSIBILITY_OVERLAY)

**Related Spec**: Could be part of a new "Unified Overlay System" spec

---

## Priority 2 - Medium Impact (Non-Critical Features) - 3 TODOs

### Issue #TODO-002: Analytics Integration for Rollback Events
**Priority**: P2 - MEDIUM
**Impact**: Observability, debugging
**Effort**: 1 week
**Files Affected**: 1 file

**Location**: `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/refactoring/RollbackController.kt:274`

**Description**: Integrate rollback events with analytics/monitoring system for production observability.

**Technical Details**:
```kotlin
// TODO: Send to analytics/monitoring system
// Current: Only logged locally
ConditionalLogger.i(TAG) { "Rollback completed: $type" }
```

**Acceptance Criteria**:
1. Choose analytics platform (Firebase Analytics, Amplitude, or custom)
2. Define rollback event schema (type, timestamp, reason, success)
3. Implement `AnalyticsReporter` interface
4. Send rollback events to analytics
5. Create dashboard for rollback monitoring

**Dependencies**:
- Analytics SDK integration
- Privacy compliance (GDPR/CCPA for event data)

---

### Issue #TODO-003: State Capture Implementation for Testing
**Priority**: P2 - MEDIUM
**Impact**: Test framework completeness
**Effort**: 3-5 days
**Files Affected**: 1 file

**Location**: `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/testing/StateComparator.kt:133`

**Description**: Complete state capture mechanism for before/after comparison tests.

**Technical Details**:
```kotlin
// TODO: Implement actual state capture mechanism
// Currently returns empty state
```

**Acceptance Criteria**:
1. Define what "state" means (accessibility tree, UI hierarchy, database)
2. Implement serializable state snapshot
3. Add state diffing algorithm
4. Create comparison reports for test failures
5. Add example tests using state comparison

**Dependencies**:
- AccessibilityNodeInfo serialization
- Database snapshot utilities

---

### Issue #TODO-004: Divergence-Based Test Termination
**Priority**: P2 - MEDIUM
**Impact**: Test framework enhancement
**Effort**: 2-3 days
**Files Affected**: 1 file

**Location**: `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/testing/DivergenceAlerts.kt:389`

**Description**: Implement termination logic when critical divergence detected in tests.

**Technical Details**:
```kotlin
// TODO: Implement termination logic
// Current: Only logs divergence, doesn't fail tests
```

**Acceptance Criteria**:
1. Define divergence thresholds (warning vs critical)
2. Implement test termination on critical divergence
3. Add configuration for divergence tolerance
4. Create detailed divergence reports
5. Add tests for divergence detection

---

## Priority 3 - Low Impact (Documented Limitations) - 1 TODO

### Issue #TODO-005: AccessibilityService Key Event Limitation
**Priority**: P3 - LOW (DOCUMENTATION ONLY)
**Impact**: None (platform limitation)
**Effort**: 0 (already documented)
**Files Affected**: 1 file

**Location**: `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/url/URLBarInteractionManager.kt:643`

**Description**: Document Android platform limitation - AccessibilityService cannot directly dispatch key events.

**Technical Details**:
```kotlin
// TODO: AccessibilityService does not have direct dispatchKeyEvent() support
// This is a platform limitation, not a bug
```

**Action**: NO CODE CHANGE NEEDED
- This TODO is purely documentary
- Convert to comment explaining the limitation
- Add reference to Android documentation

**Resolution**:
```kotlin
// Platform Limitation: AccessibilityService does not have direct dispatchKeyEvent() support
// See: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
// Workaround: Use AccessibilityNodeInfo.performAction(ACTION_SET_TEXT) instead
```

---

## Completed TODOs ✅

### ~~Issue #TODO-000: HelpMenuHandler Documentation URL~~
**Status**: ✅ COMPLETED (2025-11-13)
**Commit**: 589bea3

**Resolution**: Implemented 3-tier fallback documentation URL:
1. GitLab Pages (primary)
2. GitHub repo (fallback)
3. Built-in help (offline)

---

## Summary Statistics

| Priority | Count | Total Effort | Status |
|----------|-------|--------------|--------|
| P1 - High | 11 TODOs | 2-3 weeks | Open |
| P2 - Medium | 3 TODOs | 2-3 weeks | Open |
| P3 - Low | 1 TODO | 0 (doc only) | Open |
| **Total Open** | **15 TODOs** | **4-6 weeks** | |
| **Completed** | **1 TODO** | - | ✅ |
| **Original Total** | **24 TODOs** | | |

**Note**: Original count of 24 included TODOs that were part of larger issues. After consolidation:
- 11 overlay TODOs → 1 unified overlay system issue
- Remaining TODOs are independent issues

---

## Next Steps

1. **Create GitHub/GitLab Issues**: Convert each TODO section above to tracked issue
2. **Assign Priorities**: Add P1/P2/P3 labels to issues
3. **Add to Roadmap**: Include in VOS4 expansion roadmap (docs/developer-manual/21-Expansion-Roadmap.md)
4. **Milestone Planning**:
   - Milestone 1: P1 issues (overlay system)
   - Milestone 2: P2 issues (analytics, testing)
   - Milestone 3: P3 issues (documentation)

---

## Tracking Updates

| Date | Action | TODOs Resolved | TODOs Added |
|------|--------|----------------|-------------|
| 2025-11-13 | Initial tracking document created | 1 (HelpMenuHandler URL) | 0 |
| | | | |

---

**Framework**: IDEACODE v8.0
**Maintainer**: VOS4 Development Team
**Last Review**: 2025-11-13
