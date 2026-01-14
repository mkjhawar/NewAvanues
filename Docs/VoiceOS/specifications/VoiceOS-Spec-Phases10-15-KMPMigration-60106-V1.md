# VoiceOSCoreNG KMP Migration - Phases 10-15 Specification

**Document:** VoiceOS-Spec-Phases10-15-KMPMigration-60106-V1.md
**Date:** 2026-01-06
**Version:** V1
**Author:** VOS4 Development Team
**Status:** APPROVED
**Platforms:** Android, iOS, Desktop (KMP)

---

## Executive Summary

Complete the remaining ~50% of VoiceOSCore functionality migration to VoiceOSCoreNG KMP module. This specification covers Phases 10-15, totaling **~17,000 lines** of code with **~290 TDD tests** across **6 phases**.

### Scope

| Phase | Focus | Priority | Lines | Tests |
|-------|-------|----------|-------|-------|
| 10 | Overlay System | P0 CRITICAL | ~6,810 | 80 |
| 11 | Cursor/Focus System | P0 CRITICAL | ~4,924 | 60 |
| 12 | Additional Handlers | P1 HIGH | ~3,359 | 50 |
| 13 | Speech Engine Ports | P1 HIGH | ~900 | 30 |
| 14 | LearnApp Complete | P2 MEDIUM | ~2,500 | 40 |
| 15 | iOS/Desktop Executors | P2 MEDIUM | ~1,500 | 30 |

**Estimated Duration:** 9.5 weeks

---

## Problem Statement

VoiceOSCore contains critical voice accessibility features that are:
1. Android-only (no cross-platform support)
2. Tightly coupled to legacy UUID system (deprecated)
3. Not testable in isolation (monolithic architecture)
4. Missing from VoiceOSCoreNG KMP module (50% migrated)

---

## Functional Requirements

### FR-1: Overlay System (Phase 10)

| ID | Requirement | Platform | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-1.1 | Visual feedback overlays | All | Overlays display on top of all apps |
| FR-1.2 | Number overlay for selection | All | "tap 3" selects numbered element |
| FR-1.3 | Command status display | All | Shows listening/processing/success states |
| FR-1.4 | Confidence visualization | All | Shows recognition confidence percentage |
| FR-1.5 | Context menu overlay | All | Displays contextual actions for element |
| FR-1.6 | Theme customization | All | Dark/light/high-contrast themes |
| FR-1.7 | Accessibility settings | All | Large text, high contrast options |

**Key Components:**
- `IOverlay` - Interface contract
- `BaseOverlay` - Base implementation with lifecycle
- `OverlayManager` - Show/hide overlay coordination
- `OverlayCoordinator` - Multi-overlay orchestration
- `CommandStatusOverlay` - Listening/processing feedback
- `ConfidenceOverlay` - Recognition confidence display
- `ContextMenuOverlay` - Element context actions
- `NumberedSelectionOverlay` - Element numbering
- `NumberOverlayRenderer` - Number badge rendering
- `OverlayConfig` - Theme and accessibility configuration
- `OverlayTheme` / `OverlayThemes` - Theme definitions

### FR-2: Cursor/Focus System (Phase 11)

| ID | Requirement | Platform | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-2.1 | Spatial cursor | All | Cursor visible and movable on screen |
| FR-2.2 | Cursor movement | All | "cursor up/down/left/right" moves cursor |
| FR-2.3 | Focus indicator | All | Visual indicator shows current selection |
| FR-2.4 | Screen bounds | All | Cursor constrained to screen edges |
| FR-2.5 | Speed control | All | Adjustable cursor movement speed |
| FR-2.6 | Snap to element | All | Cursor snaps to nearby interactive elements |
| FR-2.7 | Position history | All | Undo/redo cursor positions |
| FR-2.8 | Gesture mapping | All | "tap at position" executes at cursor |

**Key Components:**
- `CursorPosition` - Position data class
- `CursorPositionTracker` - Position state management
- `CursorStyleManager` - Visual cursor styling
- `CursorVisibilityManager` - Show/hide logic
- `CursorGestureHandler` - Gesture dispatch at position
- `CursorHistoryTracker` - Undo/redo positions
- `CommandMapper` - Map commands to cursor actions
- `SpeedController` - Movement speed configuration
- `BoundaryDetector` - Screen edge detection
- `SnapToElementHandler` - Element proximity snapping
- `FocusIndicator` - Focus visualization overlay
- `CursorManager` - Facade for cursor system

### FR-3: Additional Handlers (Phase 12)

| ID | Requirement | Platform | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-3.1 | Selection handler | All | "select", "copy", "paste" work |
| FR-3.2 | Gesture handler | All | Multi-touch gestures simulated |
| FR-3.3 | Number handler | All | "tap 3" selects numbered element |
| FR-3.4 | Drag handler | All | "drag from X to Y" works |
| FR-3.5 | Device handler | Android, iOS | Volume/brightness control |
| FR-3.6 | Bluetooth handler | Android, iOS | Toggle Bluetooth state |
| FR-3.7 | Help menu handler | All | Show help/command list |
| FR-3.8 | App handler | All | App launch by name |

**Key Components:**
- `SelectHandler` - Selection/clipboard operations
- `GestureHandler` - Multi-touch gesture simulation
- `NumberHandler` - Numbered element selection
- `DragHandler` - Drag-and-drop operations
- `DeviceHandler` - Volume/brightness controls
- `BluetoothHandler` - Bluetooth connectivity
- `HelpMenuHandler` - Help documentation
- `AppHandler` - Application launching

### FR-4: Speech Engine Ports (Phase 13)

| ID | Requirement | Platform | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-4.1 | Vosk offline recognition | Android | Works without internet |
| FR-4.2 | Azure cloud recognition | Android | High accuracy cloud STT |
| FR-4.3 | Grammar constraints | All | Limited vocabulary mode |
| FR-4.4 | Phrase list boost | All | Prioritize VoiceOS commands |
| FR-4.5 | Fallback mechanism | All | Switch engines on failure |

**Key Components:**
- `VoskEngineImpl` - Vosk offline speech recognition
- `AzureEngineImpl` - Azure cloud speech recognition

### FR-5: LearnApp Complete (Phase 14)

| ID | Requirement | Platform | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-5.1 | Exploration engine | All | Orchestrates UI exploration |
| FR-5.2 | JIT learning | All | Learn commands on-demand |
| FR-5.3 | Consent management | All | User consent before learning |
| FR-5.4 | Session management | All | Track exploration sessions |
| FR-5.5 | VUID persistence | All | Store learned elements via VUID |

**Key Components:**
- `ExplorationEngine` - Exploration orchestration
- `ExplorationSession` - Session state
- `ExplorationState` - State machine
- `JITLearner` - Just-in-time learning
- `CommandLearner` - Command learning logic
- `ConsentManager` - Consent dialog management
- `SessionManager` - Session lifecycle

### FR-6: iOS/Desktop Executors (Phase 15)

| ID | Requirement | Platform | Acceptance Criteria |
|----|-------------|----------|---------------------|
| FR-6.1 | iOS navigation | iOS | Navigate iOS UI via VoiceOver |
| FR-6.2 | iOS UI actions | iOS | Tap/scroll/swipe via UIAccessibility |
| FR-6.3 | iOS input | iOS | Text input simulation |
| FR-6.4 | Desktop navigation | Desktop | Navigate desktop windows |
| FR-6.5 | Desktop UI actions | Desktop | Click/scroll via Robot/AWT |
| FR-6.6 | Desktop input | Desktop | Keyboard input simulation |

**Key Components (iOS):**
- `IOSNavigationExecutor` - Navigation commands
- `IOSUIExecutor` - UI actions
- `IOSInputExecutor` - Input simulation
- `IOSSystemExecutor` - System commands

**Key Components (Desktop):**
- `DesktopNavigationExecutor` - Window navigation
- `DesktopUIExecutor` - Mouse actions via Robot
- `DesktopInputExecutor` - Keyboard via Robot
- `DesktopSystemExecutor` - System commands

---

## Non-Functional Requirements

### NFR-1: Performance

| Metric | Target |
|--------|--------|
| Overlay render time | < 16ms (60fps) |
| Command recognition latency | < 500ms |
| Cursor movement latency | < 50ms |
| Memory overhead | < 50MB per overlay |

### NFR-2: Testing

| Metric | Target |
|--------|--------|
| Unit test coverage | ≥ 80% |
| Integration test coverage | ≥ 60% |
| Edge case coverage | 100% |
| TDD compliance | All new code |

### NFR-3: Compatibility

| Platform | Minimum Version |
|----------|-----------------|
| Android | API 26 (Oreo) |
| iOS | iOS 15 |
| Desktop | JDK 17 |

### NFR-4: Accessibility

| Requirement | Standard |
|-------------|----------|
| Color contrast | WCAG 2.1 AA |
| Touch targets | 48x48dp minimum |
| Screen reader support | TalkBack/VoiceOver |

---

## Technical Constraints

1. **Database:** Must use VUID tables via SQLDelightVuidRepositoryAdapter
2. **No UUID:** Legacy UUID tables removed - use VUID system only
3. **KMP Structure:** 4-package IDEACODE model (common, functions, handlers, features)
4. **Compose:** Use Compose Multiplatform for UI (Android + experimental iOS)
5. **Testing:** TDD methodology - tests before implementation

---

## Architecture Decisions

### AD-1: Overlay System Architecture

**Decision:** Compose Multiplatform with platform-specific WindowManager

**Rationale:**
- Single UI codebase for overlays
- Platform-specific window management (Android WindowManager, iOS UIWindow, Desktop JFrame)
- Experimental iOS support can be stubbed initially

### AD-2: Cursor System Architecture

**Decision:** Simplified facade pattern with CursorManager

**Rationale:**
- Reduces complexity from 11 tightly-coupled components
- CursorManager provides clean public API
- Internal components remain modular for testing

### AD-3: Speech Engine Integration

**Decision:** Plugin architecture with ISpeechEngine interface

**Rationale:**
- Multiple engines (Vosk offline, Azure cloud, Google)
- Runtime switching based on connectivity
- Easy to add new engines

---

## Dependencies

### Phase Dependencies

```
Phase 10 (Overlay) ──┬──► Phase 11 (Cursor)
                     │
                     └──► Phase 14 (LearnApp)

Phase 11 (Cursor) ───┬──► Phase 12 (Handlers)
                     │
                     └──► Phase 13 (Speech)

Phase 14 (LearnApp) ─────► Phase 15 (Executors)
```

### External Dependencies

| Dependency | Version | Phase |
|------------|---------|-------|
| Vosk Android | 0.3.47 | 13 |
| Azure Speech SDK | 1.38.0 | 13 |
| Compose Multiplatform | 1.6.0 | 10-11 |

---

## Out of Scope

1. ~~Room database~~ - Use SQLDelight via adapters
2. ~~Web platform~~ - Focus on Android/iOS/Desktop
3. ~~New accessibility frameworks~~ - Port existing only
4. ~~Custom speech model training~~ - Use pre-trained models

---

## Success Criteria

- [ ] All 290+ tests passing
- [ ] 80%+ code coverage for new code
- [ ] No regression from VoiceOSCore functionality
- [ ] Overlay system works on Android (iOS/Desktop stubbed)
- [ ] Cursor/Focus visible and controllable
- [ ] All P0/P1 handlers functional
- [ ] Vosk + Azure speech engines operational
- [ ] LearnApp exploration sessions work
- [ ] iOS/Desktop have basic executor implementations

---

## Approval

| Role | Name | Date | Status |
|------|------|------|--------|
| Author | VOS4 Dev Team | 2026-01-06 | APPROVED |
| Reviewer | - | - | PENDING |

---

## References

- Implementation Plan: `Docs/VoiceOS/plans/VoiceOS-Plan-Phases10-15-KMPMigration-60106-V1.md`
- API Reference: `Docs/VoiceOS/manuals/developer/VoiceOSCoreNG-API-Reference-60106-V1.md`
- Migration Guide: `Docs/VoiceOS/manuals/developer/VoiceOSCoreNG-Migration-Guide-60106-V1.md`
