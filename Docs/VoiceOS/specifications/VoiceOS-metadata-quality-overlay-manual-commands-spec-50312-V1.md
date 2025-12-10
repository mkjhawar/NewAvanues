# Metadata Quality Overlay & Manual Command Assignment - Feature Specification

**Project:** VoiceOS
**Feature ID:** VOS-META-001
**Created:** 2025-12-03
**Status:** Draft
**Platform:** Android (VoiceOS Core)
**Priority:** High
**Complexity:** High (Multi-modal UI, Accessibility Service Integration, Voice Recording)

---

## Executive Summary

Implement a comprehensive metadata quality feedback system that visually shows users which UI elements have voice commands, allows manual command assignment for elements without metadata, and provides developer-oriented accessibility audit export. This addresses the critical gap where 3rd-party apps with poor accessibility implementation (missing text/contentDescription/resourceId) result in "learned but unusable" elements.

**Key Features:**
1. **Post-Learning Overlay**: Show generic commands on screen after exploration/JIT learning
2. **Manual Command Assignment**: Click element → record voice command → save as synonym
3. **Developer Mode**: Element Quality Indicator overlay with visual highlighting
4. **Accessibility Audit Export**: Generate report for app developers

---

## Problem Statement

### Current State
When VoiceOS learns 3rd-party apps with poor accessibility implementation:
- Elements without metadata get generic aliases (`button_1`, `framelayout_2`)
- No voice commands are generated (CommandGenerator returns empty list)
- Users are NOT notified about unusable elements
- No mechanism exists to manually add voice commands
- Developers have no feedback about accessibility issues

### Pain Points
1. **Silent Failure**: Elements appear "learned" but can't be voice-controlled
2. **No User Recourse**: Users can't fix the problem themselves
3. **Poor UX**: Users don't know which elements work vs don't work
4. **Developer Blindness**: App developers unaware of accessibility problems

### Desired State
- Users see which elements lack voice commands immediately after learning
- Users can manually assign voice commands via speech
- Developer mode shows real-time element quality visualization
- Accessibility audits can be exported and shared with app developers

---

## Functional Requirements

### FR-001: Post-Learning Generic Command Overlay
**Platform:** Android
**Description:** After exploration or JIT learning completes, show overlay displaying generic commands assigned to elements without metadata.

**Acceptance Criteria:**
- [ ] Overlay appears automatically when learning completes with elements lacking metadata
- [ ] Each element with generic alias is highlighted on screen
- [ ] Overlay shows: element bounds, generic alias, className
- [ ] Overlay displays count: "5 elements need voice commands"
- [ ] User can tap element to start manual assignment flow
- [ ] Overlay dismisses on "Done" button or back gesture

**Example Display:**
```
┌──────────────────────────────────────┐
│ 🎤 Voice Commands Needed             │
│ 5 elements have generic commands     │
│                                      │
│ [Highlighted Button]                 │
│   • Generic: button_1                │
│   • Type: android.widget.Button      │
│   • Tap to assign voice command      │
│                                      │
│ [Done] [Assign Commands]             │
└──────────────────────────────────────┘
```

---

### FR-002: Manual Command Assignment Dialog
**Platform:** Android
**Description:** Allow users to click element and speak custom voice command to create manual mapping.

**Acceptance Criteria:**
- [ ] Tapping highlighted element opens command assignment dialog
- [ ] Dialog shows: element type, current generic alias, visual preview
- [ ] "Record Command" button triggers speech recognition
- [ ] Speech input validated (3-50 characters, no profanity)
- [ ] User can record multiple synonyms for same element
- [ ] Confirmation shows: "Say 'tap submit button' to activate this element"
- [ ] Commands saved to `custom_commands` table with UUID mapping
- [ ] Newly assigned commands immediately available for voice control

**Flow:**
1. User taps `button_1` on overlay
2. Dialog opens: "This button has no label. Add voice command?"
3. User taps "Record Command"
4. User speaks: "submit button"
5. System confirms: "Saved! Say 'tap submit button' to activate"
6. User can add synonym: "send" → both work

**Database Schema:**
```sql
CREATE TABLE custom_commands (
    id INTEGER PRIMARY KEY,
    element_uuid TEXT NOT NULL,
    command_phrase TEXT NOT NULL,
    confidence REAL DEFAULT 1.0,
    created_at INTEGER NOT NULL,
    created_by TEXT DEFAULT 'user',
    is_synonym INTEGER DEFAULT 0,
    FOREIGN KEY (element_uuid) REFERENCES uuid_registry(uuid)
);

CREATE INDEX idx_custom_commands_uuid ON custom_commands(element_uuid);
CREATE INDEX idx_custom_commands_phrase ON custom_commands(command_phrase);
```

---

### FR-003: Synonym Management
**Platform:** Android
**Description:** When user assigns command to element that already has commands, add as synonym.

**Acceptance Criteria:**
- [ ] If element has existing command, show: "Current: 'submit button'. Add synonym?"
- [ ] User can add unlimited synonyms per element
- [ ] Synonyms stored with `is_synonym = 1` flag
- [ ] Voice recognition accepts any command or synonym
- [ ] User can view all synonyms in command list
- [ ] User can delete individual synonyms (not primary command)

**Example:**
```
Element: button_1 (Submit Button)
├─ Primary: "submit button" (auto-generated or first manual)
├─ Synonym: "send" (user-added)
├─ Synonym: "go" (user-added)
└─ Synonym: "submit" (user-added short form)
```

---

### FR-004: Developer Mode - Element Quality Indicator
**Platform:** Android
**Description:** Settings toggle to enable visual overlay showing which elements have voice commands vs don't.

**Acceptance Criteria:**
- [ ] Setting in VoiceOS Settings → Developer Options → "Show Element Quality"
- [ ] When enabled, overlay appears on ALL apps (not just learning)
- [ ] Elements color-coded by metadata quality:
  - 🟢 Green border: Excellent metadata (text + contentDesc + resourceId)
  - 🟡 Yellow border: Acceptable metadata (any 1 of 3)
  - 🔴 Red border: Poor metadata (generic alias, no voice commands)
  - 🔵 Blue border: Manual command assigned
- [ ] Tap element shows popup with details
- [ ] Performance: <16ms frame time impact (60 FPS maintained)
- [ ] Accessibility Service permission required
- [ ] Toggle in notification shade for quick access

**Visual Design:**
```
Screen with 3 buttons:
┌─────────────────┐
│ [  Settings  ] │ ← Green border (has text)
│                │
│ [  button_1  ] │ ← Red border (generic, no command)
│                │
│ [  My Button ] │ ← Blue border (manual command: "go")
└─────────────────┘
```

---

### FR-005: Element Quality Detail Popup
**Platform:** Android
**Description:** In developer mode, tapping element shows detailed metadata quality report.

**Acceptance Criteria:**
- [ ] Shows: className, UUID, alias, metadata present, voice commands
- [ ] Quality score (0-100) with breakdown
- [ ] Suggestions for improvement (if poor quality)
- [ ] Copy UUID button (for debugging)
- [ ] "Add Command" shortcut button
- [ ] Bounds coordinates for precise identification

**Example Popup:**
```
┌────────────────────────────────────┐
│ Element Quality Report             │
├────────────────────────────────────┤
│ Class: android.widget.Button       │
│ UUID: abc-123-def                  │
│ Alias: button_1 (generic)          │
│                                    │
│ Metadata Quality: 🔴 Poor (20/100) │
│ ├─ Text: ❌ Missing                │
│ ├─ ContentDesc: ❌ Missing          │
│ └─ ResourceID: ❌ Missing           │
│                                    │
│ Voice Commands: 0                  │
│ ├─ Auto-generated: 0               │
│ └─ User-assigned: 0                │
│                                    │
│ Suggestions:                       │
│ • Ask developer to add text        │
│ • Add manual voice command         │
│                                    │
│ [Add Command] [Copy UUID] [Close]  │
└────────────────────────────────────┘
```

---

### FR-006: Developer Feedback Mode - Accessibility Audit Export
**Platform:** Android
**Description:** Generate comprehensive accessibility audit report for app developers.

**Acceptance Criteria:**
- [ ] Export button in VoiceOS Settings → Developer Options → "Export Audit"
- [ ] Prompts to select learned app from list
- [ ] Generates JSON + Markdown report
- [ ] Report includes:
  - App metadata (package, version, screens learned)
  - Element-by-element quality scores
  - Missing metadata breakdown by screen
  - Actionable recommendations
  - WCAG 2.1 compliance notes
- [ ] Export formats: JSON (machine-readable), Markdown (human-readable)
- [ ] Share via Android Share Sheet (email, Drive, Slack, etc.)
- [ ] Option to anonymize data (remove user-specific info)

**Report Structure (Markdown):**
```markdown
# Accessibility Audit Report
**App:** RealWear Test App (com.realwear.testcomp)
**Version:** 1.0.0
**Audit Date:** 2025-12-03
**Screens Analyzed:** 5

## Summary
- Total Elements: 47
- Excellent Metadata: 12 (25.5%)
- Acceptable Metadata: 20 (42.6%)
- Poor Metadata: 15 (31.9%) ⚠️

## Critical Issues
### Screen: MainActivity (hash: 255bc891...)
1. **Button** (button_1)
   - Missing: text, contentDescription, resourceId
   - Impact: Not voice-controllable
   - Recommendation: Add android:contentDescription="Submit"

2. **FrameLayout** (framelayout_1)
   - Missing: text, contentDescription, resourceId
   - Impact: Not voice-controllable
   - Note: Consider if this should be clickable

## WCAG 2.1 Compliance
- ❌ 4.1.2 Name, Role, Value: 15 violations
- ⚠️ 2.5.3 Label in Name: 8 warnings

## Recommendations
1. Add contentDescription to all clickable elements
2. Use meaningful resource IDs (e.g., btn_submit vs button1)
3. Consider accessibility testing tools (Espresso, Accessibility Scanner)
```

---

### FR-007: Command Persistence & Synchronization
**Platform:** Android
**Description:** Manual commands persist across app updates and sync across devices (future).

**Acceptance Criteria:**
- [ ] Custom commands stored in SQLDelight database
- [ ] Commands survive app updates if UUID remains stable
- [ ] Commands backed up to local storage
- [ ] Export/import custom commands via JSON
- [ ] Future: Cloud sync via AVA Account (Phase 2)
- [ ] Conflict resolution: user command > auto-generated command

---

### FR-008: Voice Command Integration
**Platform:** Android
**Description:** Manual commands integrate seamlessly with existing voice recognition system.

**Acceptance Criteria:**
- [ ] Custom commands registered in CommandManager
- [ ] CommandProcessor checks custom_commands table before auto-generated
- [ ] Speech recognition confidence threshold: ≥0.7 for custom commands
- [ ] Ambiguous commands trigger disambiguation dialog
- [ ] Commands scoped to app (same phrase can mean different things in different apps)
- [ ] Real-time updates: command available immediately after save

**Command Resolution Order:**
1. Custom commands (user-defined) - Priority 1
2. Auto-generated commands (from metadata) - Priority 2
3. Generic aliases (button_1) - Priority 3 (disabled by default)

---

## Non-Functional Requirements

### NFR-001: Performance
- [ ] Overlay rendering: <50ms to display
- [ ] Element highlighting: <16ms per frame (60 FPS)
- [ ] Command lookup: <10ms average (indexed queries)
- [ ] Speech recognition: <2 seconds end-to-end
- [ ] Audit export: <5 seconds for 100-screen app
- [ ] Memory footprint: <20MB additional for overlay

### NFR-002: Accessibility
- [ ] Overlay itself must be accessible (TalkBack compatible)
- [ ] Voice recording has visual feedback (for hearing-impaired)
- [ ] Keyboard navigation support for command assignment
- [ ] High contrast mode support
- [ ] Font scaling respects system settings

### NFR-003: Security & Privacy
- [ ] Voice recordings NOT stored (processed in-memory only)
- [ ] Command phrases sanitized (no SQL injection)
- [ ] Audit export anonymization option
- [ ] Custom commands encrypted at rest
- [ ] No telemetry without explicit consent

### NFR-004: Usability
- [ ] First-time user sees tutorial on overlay usage
- [ ] Clear visual distinction between auto vs manual commands
- [ ] Undo option for accidentally assigned commands
- [ ] Bulk command assignment (select multiple elements)
- [ ] Search/filter in command list

### NFR-005: Compatibility
- [ ] Android 8.0+ (API 26+)
- [ ] Works with AccessibilityService in background
- [ ] Compatible with existing VoiceOS learned apps
- [ ] Backward compatible (old databases migrate smoothly)
- [ ] No impact on apps without poor metadata

---

## Platform-Specific Details

### Android Components

**1. UI Components:**
- `MetadataQualityOverlay` - FloatingWindow service for overlay
- `CommandAssignmentDialog` - Material3 dialog for recording
- `ElementQualityIndicatorService` - Developer mode overlay
- `QualityPopupView` - Element detail popup

**2. Services:**
- `CustomCommandManager` - CRUD operations for custom commands
- `SpeechRecorder` - Voice input capture and processing
- `AccessibilityAuditor` - Report generation engine

**3. Database (SQLDelight):**
```sql
-- custom_commands table (defined in FR-002)
-- quality_metrics table
CREATE TABLE quality_metrics (
    element_uuid TEXT PRIMARY KEY,
    quality_score INTEGER NOT NULL,
    has_text INTEGER NOT NULL,
    has_content_desc INTEGER NOT NULL,
    has_resource_id INTEGER NOT NULL,
    command_count INTEGER DEFAULT 0,
    last_assessed INTEGER NOT NULL
);
```

**4. Dependencies:**
- Android Speech Recognition API (built-in)
- Material3 Components (already in project)
- Jetpack Compose for overlay UI
- kotlinx.serialization for JSON export

**5. Permissions:**
- `RECORD_AUDIO` - For voice command recording
- `SYSTEM_ALERT_WINDOW` - For overlay display (already granted)

**6. Testing Strategy:**
- Unit tests: CustomCommandManager, AccessibilityAuditor
- Integration tests: Command resolution order, database operations
- UI tests: Overlay interaction, dialog flow (Espresso)
- Manual tests: RealWear Test App, 5+ third-party apps
- Performance tests: Overlay rendering, command lookup latency

---

## User Stories

### US-001: User Assigns Command After Learning
**As a** VoiceOS user
**I want to** assign voice commands to elements without metadata
**So that** I can control all app features by voice, not just well-labeled ones

**Acceptance Criteria:**
- Given I completed app exploration
- When overlay shows `button_1` with no voice command
- Then I can tap it, record "submit button", and use that command immediately

---

### US-002: User Views Element Quality
**As a** VoiceOS power user
**I want to** see which elements have good vs poor metadata
**So that** I understand app voice-control coverage

**Acceptance Criteria:**
- Given developer mode is enabled
- When I open any learned app
- Then elements are color-coded by quality
- And I can tap any element to see detailed quality report

---

### US-003: Developer Receives Audit
**As an** app developer
**I want to** receive accessibility audit from VoiceOS users
**So that** I can improve my app's voice-control support

**Acceptance Criteria:**
- Given user enabled audit export
- When user selects my app and exports audit
- Then I receive Markdown report via email
- And report shows specific elements needing improvement
- And recommendations are actionable (add contentDescription, etc.)

---

### US-004: User Manages Synonyms
**As a** VoiceOS user
**I want to** add multiple voice commands for the same element
**So that** I can use natural variations ("submit" vs "send" vs "go")

**Acceptance Criteria:**
- Given element has command "submit button"
- When I add synonym "send"
- Then both "submit button" and "send" activate element
- And I can see all synonyms in command list
- And I can delete individual synonyms

---

## Technical Constraints

### Constraints
1. **Accessibility Service Limitation**: Overlay must not block AccessibilityService events
2. **Speech Recognition Accuracy**: Android's built-in SR has ~85% accuracy baseline
3. **UUID Stability**: Manual commands rely on ThirdPartyUuidGenerator stability
4. **Performance**: Overlay must not degrade app performance (<5% CPU impact)
5. **Memory**: Limited by Android OS (background service constraints)

### Assumptions
1. User grants `RECORD_AUDIO` permission willingly
2. Device has working microphone
3. User understands overlay is VoiceOS feature (not target app)
4. App elements remain stable across versions (UUIDs don't change)

---

## Dependencies

### Internal Dependencies
| Component | Dependency | Reason |
|-----------|------------|--------|
| CustomCommandManager | VoiceOSDatabaseManager | Store custom commands |
| CommandAssignmentDialog | SpeechRecognition library | Voice input capture |
| MetadataQualityOverlay | ExplorationEngine | Get learned elements |
| AccessibilityAuditor | MetadataValidator | Quality scoring |

### External Dependencies
| API | Purpose | Fallback |
|-----|---------|----------|
| Android Speech Recognition | Voice command recording | Keyboard input |
| AccessibilityService | Overlay positioning | Best-effort bounds |

### Implementation Order
1. **Phase 1**: Database schema + CustomCommandManager
2. **Phase 2**: Manual command assignment dialog + speech recording
3. **Phase 3**: Post-learning overlay
4. **Phase 4**: Developer mode quality indicator
5. **Phase 5**: Accessibility audit export

---

## Swarm Assessment

### Complexity Indicators
- ✅ **Multi-modal UI**: Overlay + Dialog + Service
- ✅ **Real-time performance requirements**: 60 FPS overlay
- ✅ **Speech processing**: Voice input capture and validation
- ✅ **Database design**: New tables + migrations
- ✅ **Accessibility Service integration**: Sensitive system interaction

### Recommended Agents
| Agent | Role | Scope |
|-------|------|-------|
| `@vos4-android-expert` | Lead implementation | All Android components |
| `@vos4-database-expert` | Database schema + migrations | SQLDelight tables, indexes |
| `@vos4-test-specialist` | Comprehensive testing | Unit + integration + UI tests |
| `@vos4-architecture-reviewer` | Design review | Overlay architecture, performance |
| `@vos4-documentation-specialist` | User + dev docs | Tutorial, API docs, audit guide |

### Swarm Activation
**Recommended:** YES
**Reason:** High complexity (5 major components), performance-critical, user-facing, testing-intensive

**Estimated Effort:**
- Without swarm: 3-4 weeks (sequential)
- With swarm: 1.5-2 weeks (parallel)

---

## Success Criteria

### Measurable Goals
1. [ ] **Manual Command Assignment**: Users can assign ≥3 custom commands in <2 minutes
2. [ ] **Overlay Performance**: 60 FPS maintained with ≤100 elements on screen
3. [ ] **Command Accuracy**: Speech recognition + validation ≥90% success rate
4. [ ] **Audit Quality**: Reports generated for ≥5 third-party apps, validated by developers
5. [ ] **User Adoption**: ≥70% of beta testers use manual assignment feature at least once

### Definition of Done
- [ ] All functional requirements implemented and tested
- [ ] Performance benchmarks met (FR-008, NFR-001)
- [ ] Accessibility compliance verified (TalkBack testing)
- [ ] Documentation complete (user guide + developer API docs)
- [ ] Beta testing completed with ≥10 users, ≥5 different apps
- [ ] Code review passed by 2+ reviewers
- [ ] Unit test coverage ≥90% for new components
- [ ] Integration tests cover all user stories
- [ ] Merged to `main` branch with clean build

---

## Open Questions

1. **Q:** Should generic aliases (`button_1`) be voice-controllable by default or require manual assignment?
   **A:** TBD - Get user feedback during beta. Leaning toward "disabled by default" (safer UX)

2. **Q:** Should audit export include screenshots of problematic elements?
   **A:** TBD - Privacy concern, but very useful for developers. Make opt-in?

3. **Q:** How to handle element UUID changes across app updates?
   **A:** TBD - Need UUID stability research, possible "fuzzy matching" fallback

4. **Q:** Should voice recordings be processed on-device only or allow cloud SR for better accuracy?
   **A:** TBD - On-device for privacy, but cloud could be opt-in setting

5. **Q:** What's the UX for bulk command assignment (e.g., 20 buttons without metadata)?
   **A:** TBD - Possible "guided tour" mode: highlight → user speaks → next element

---

## Risks & Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Overlay blocks user interaction | High | Medium | Z-order management, tap-through areas |
| Speech recognition accuracy too low | High | Medium | Keyboard fallback, confidence threshold tuning |
| UUID instability breaks commands | High | Low | Fuzzy matching, manual re-assignment flow |
| Performance degradation on low-end devices | Medium | High | Feature flag, disable overlay on <2GB RAM |
| Developer pushback on audit format | Low | Low | Iterate based on feedback, multiple formats |

---

## Related Documentation

- `/docs/modules/learnapp/developer-manual.md` - LearnApp architecture
- `/docs/specifications/jit-element-deduplication-fix-spec.md` - Recent fix for element persistence
- `ExplorationEngine.kt:1115-1151` - Current generic alias generation
- `CommandGenerator.kt:89-97` - Voice command generation logic
- `MetadataValidator.kt` - Existing quality validation

---

## Approval

**Specification Author:** AI Assistant (Claude)
**Review Required:** Product Owner, Lead Android Engineer, UX Designer
**Target Review Date:** 2025-12-04
**Target Implementation Start:** Upon approval

---

**Version:** 1.0
**Last Updated:** 2025-12-03
**Status:** Ready for Review
