# Feature Specification: Chat UI Specification Remediation

**Feature ID**: 007-chat-ui-remediation
**Version**: 1.0.0
**Created**: 2025-11-12
**Profile**: android-app
**Complexity**: Tier 2 (2-4 hours, 3-6 files, medium risk)
**Phase**: 1.0 - MVP Cleanup
**Parent Feature**: 006-chat-ui

---

## Executive Summary

Remediate 18 identified issues in Feature 006 (Chat UI with NLU Integration) specification artifacts, including 2 CRITICAL constitution violations, 4 HIGH priority ambiguities/gaps, and 12 MEDIUM/LOW inconsistencies. This ensures spec.md, plan.md, and tasks.md are aligned, unambiguous, and ready for implementation without blocking issues.

**Key Objectives**:
- Fix 2 CRITICAL constitution violations (JSON format, VOS4 integration)
- Resolve 4 HIGH priority issues (duplication, ambiguity, underspecification)
- Address 12 MEDIUM/LOW inconsistencies and gaps
- Achieve 100% requirement coverage with clear acceptance criteria
- Validate all 8 constitution quality gates

---

## Problem Statement

### Current State

**What Exists**:
- ✅ Feature 006 specification created (spec.md, plan.md, tasks.md)
- ✅ Cross-artifact analysis completed (18 issues identified)
- ✅ Constitution validation performed
- ✅ 91% requirement coverage (10/11 requirements)

**What's Broken**:
- ❌ **C1 (CRITICAL)**: intent_examples.json violates constitution JSON formatting standards
- ❌ **C2 (CRITICAL)**: VOS4 integration requirement conflicts between constitution and spec
- ❌ **D1 (HIGH)**: Intent templates duplicated in spec.md and plan.md
- ❌ **A1 (HIGH)**: NLU performance thresholds ambiguous (<50ms target vs <100ms max)
- ❌ **A2 (HIGH)**: Microphone button placement undefined
- ❌ **U1 (HIGH)**: "show_history" intent missing from BuiltInIntents
- ❌ 12 additional MEDIUM/LOW issues (inconsistencies, terminology, missing validations)

### Pain Points

1. **Constitution Violations Block Implementation**: Cannot proceed with C1/C2 unresolved
2. **Ambiguity Causes Rework**: Unclear requirements lead to wrong implementation choices
3. **Duplication Wastes Time**: Developers unsure which source is authoritative
4. **Missing Coverage Risks Bugs**: Accessibility validation gap means potential production issues

### Desired State

**Post-Remediation**:
1. All 18 issues resolved and documented
2. 100% requirement coverage with dedicated tasks
3. Zero constitution violations
4. Unambiguous acceptance criteria for all features
5. Single source of truth for all technical decisions
6. All 8 quality gates validated in test plan

---

## Requirements

### Functional Requirements

#### FR-007-001: Fix Constitution Violation C1 (JSON Formatting)

**Description**: Validate and refactor intent_examples.json to use compact array format per constitution standards.

**Acceptance Criteria**:
- **AC-001**: Read current intent_examples.json format
- **AC-002**: If verbose format detected (nested "examples" objects), refactor to compact arrays
- **AC-003**: Format: `{"intent_name": ["example1", "example2"]}` (not `{"intent_name": {"examples": [...]}}`)
- **AC-004**: Verify file size reduction (target: ~30% smaller)
- **AC-005**: Validate JSON parses correctly after refactoring
- **AC-006**: Update any code that depends on old format (if applicable)

**Dependencies**:
- Constitution JSON standards (constitution.md:L146-187)
- Current intent_examples.json file

**Priority**: P0 (CRITICAL - blocks implementation)

---

#### FR-007-002: Fix Constitution Violation C2 (VOS4 Integration)

**Description**: Resolve conflict between constitution Principle II (VOS4 integration mandatory) and spec/plan (VOS4 optional for Week 6).

**Acceptance Criteria**:
- **AC-001**: Choose resolution path:
  - **Option A (Recommended)**: Update constitution to allow phased VOS4 integration (add exception for Phase 1.0 MVP)
  - **Option B**: Make FR-006-005 (voice input) P0 and require VOS4 by Day 7
- **AC-002**: If Option A: Add constitution amendment to Principle II
- **AC-003**: If Option B: Update spec.md FR-006-005 priority from P1 to P0
- **AC-004**: Update plan.md Phase 5 to reflect chosen option
- **AC-005**: Document decision rationale in spec.md
- **AC-006**: No further constitution conflicts in cross-artifact analysis

**Dependencies**:
- Constitution Principle II (constitution.md:L34-48)
- Feature 006 spec.md:L519-528, plan.md:L650-698

**Priority**: P0 (CRITICAL - constitution compliance required)

**Recommended Resolution**: Option A (phased VOS4 integration)

---

#### FR-007-003: Eliminate Duplication D1 (Intent Templates)

**Description**: Remove duplicate intent template definitions from spec.md, establish IntentTemplates.kt as single source of truth.

**Acceptance Criteria**:
- **AC-001**: Remove intent template mapping from spec.md:L130-140
- **AC-002**: Keep intent template mapping in plan.md:L264-280 (technical decisions section)
- **AC-003**: Add reference in spec.md: "See plan.md Technical Decisions for template mapping"
- **AC-004**: Add note: "Implementation source of truth: IntentTemplates.kt"
- **AC-005**: Verify no other duplicate definitions exist across artifacts

**Dependencies**:
- Feature 006 spec.md:L130-140, plan.md:L264-280

**Priority**: P0 (HIGH - causes confusion during implementation)

---

#### FR-007-004: Clarify Ambiguity A1 (NLU Performance Thresholds)

**Description**: Resolve conflicting NLU performance targets between spec (<50ms target, <100ms max) and tasks (<100ms only).

**Acceptance Criteria**:
- **AC-001**: Update spec.md NFR-006-001 to clarify dual thresholds:
  - **Target**: <50ms (p50 latency, ideal performance)
  - **Maximum**: <100ms (p95 latency, acceptance threshold)
- **AC-002**: Update tasks.md P2T07 "Performance Validation":
  - Log both p50 and p95 latency
  - **Success**: p50 <50ms AND p95 <100ms
  - **Acceptable**: p95 <100ms (warn if p50 >50ms)
  - **Failure**: p95 ≥100ms (blocks completion)
- **AC-003**: Add performance budget table to spec.md with both thresholds
- **AC-004**: Document measurement methodology (100 classification runs, cold/warm states)

**Dependencies**:
- Feature 006 spec.md:L287-290, tasks.md:L286

**Priority**: P0 (HIGH - affects performance validation)

---

#### FR-007-005: Specify Ambiguity A2 (Microphone Button Placement)

**Description**: Define exact placement for floating microphone button or defer to Phase 5.

**Acceptance Criteria**:
- **AC-001**: Choose resolution:
  - **Option A**: Specify exact placement (bottom-right, 16dp margin, above input field)
  - **Option B**: Remove from Phase 1 spec, defer to FR-006-005 (Phase 5 voice input)
- **AC-002**: Update spec.md:L472 with chosen option
- **AC-003**: Update plan.md:L175 to match spec decision
- **AC-004**: If Option A: Add Compose code snippet to plan.md
- **AC-005**: If Option B: Mark as "Phase 5 implementation detail"

**Dependencies**:
- Feature 006 spec.md:L472, plan.md:L175

**Priority**: P1 (HIGH - affects UI consistency)

**Recommended Resolution**: Option B (defer to Phase 5)

---

#### FR-007-006: Add Missing Intent U1 (show_history)

**Description**: Add "show_history" intent to BuiltInIntents with training examples to enable voice-triggered history overlay.

**Acceptance Criteria**:
- **AC-001**: Add to spec.md BuiltInIntents list:
  - Intent: `show_history`
  - Examples: "AVA show history", "show transcript", "show conversations", "show my chats"
- **AC-002**: Add to plan.md:L264-280 intent template mapping:
  - `"show_history" to "Here's your conversation history."`
- **AC-003**: Update tasks.md P2T02 to include show_history in candidate intents
- **AC-004**: Add show_history to BuiltInIntents.kt implementation note
- **AC-005**: Update FR-006-006:AC-001 to reference show_history intent explicitly

**Dependencies**:
- Feature 006 spec.md:L240, tasks.md:L666-690

**Priority**: P0 (HIGH - blocks history overlay feature)

---

#### FR-007-007: Clarify Underspecification U2 (Settings Screen Location)

**Description**: Specify whether SettingsScreen.kt needs to be created or if adding to existing settings.

**Acceptance Criteria**:
- **AC-001**: Search codebase for existing SettingsScreen:
  ```bash
  find . -name "*Settings*Screen*.kt" -o -name "*SettingsActivity*.kt"
  ```
- **AC-002**: If exists: Update tasks.md P5T03 to reference existing screen location
- **AC-003**: If not exists: Update tasks.md P5T03 to explicitly state "Create new SettingsScreen.kt"
- **AC-004**: Add file path to task deliverables
- **AC-005**: Update spec.md FR-006-007 with settings screen location

**Dependencies**:
- Feature 006 spec.md:L272-275, tasks.md:L863-867

**Priority**: P1 (MEDIUM - affects task clarity)

---

#### FR-007-008: Fix Coverage Gap G1 (History Navigation Priority)

**Description**: Change task P4T04 (voice navigation) from optional to P1 per FR-006-006:AC-005 acceptance criteria.

**Acceptance Criteria**:
- **AC-001**: Update tasks.md P4T04 header:
  - Change "Optional" to "P1 (High Priority)"
  - Remove "depends on VOS4" qualifier
- **AC-002**: Update task description: "Implement text-based navigation first, voice commands Phase 5"
- **AC-003**: Add manual navigation options:
  - Button: "← Previous Conversation"
  - Button: "Next Conversation →"
  - Keyboard shortcuts: Arrow keys (desktop mode)
- **AC-004**: Voice navigation remains Phase 5 (VOS4 dependency)
- **AC-005**: Verify FR-006-006:AC-005 fully covered by updated task

**Dependencies**:
- Feature 006 spec.md:FR-006-006, tasks.md:P4T04

**Priority**: P1 (MEDIUM - feature completeness)

---

#### FR-007-009: Resolve Coverage Gap G2 (Data Export Deferred)

**Description**: Either implement data export or remove from NFR-006-003 and defer to future phase.

**Acceptance Criteria**:
- **AC-001**: Choose resolution:
  - **Option A**: Defer to Phase 1.1 (Week 8+)
  - **Option B**: Implement minimal JSON export in Week 6
- **AC-002**: If Option A (Recommended):
  - Remove data export from spec.md NFR-006-003
  - Add to "Out of Scope (Week 6)" section
  - Create placeholder spec: "Feature 008: Data Export"
- **AC-003**: If Option B:
  - Add task P5T06B: "Implement conversation export to JSON"
  - Add acceptance criteria to NFR-006-003
- **AC-004**: Document decision rationale

**Dependencies**:
- Feature 006 spec.md:L327-333, tasks.md

**Priority**: P2 (MEDIUM - scope clarification)

**Recommended Resolution**: Option A (defer to Phase 1.1)

---

#### FR-007-010: Standardize Inconsistencies (I1, I2, I3, T1, T2)

**Description**: Fix 5 inconsistencies across spec/plan/tasks (confidence badges, bottom sheet height, overlay width, terminology).

**Acceptance Criteria**:
- **AC-001 (I1)**: Confidence badge format = "🟢 85%" (emoji + percentage)
- **AC-002 (I2)**: TeachAvaBottomSheet height = "33% screen height (max 40%)"
- **AC-003 (I3)**: History overlay width = "25% screen width (vertical side panel)"
- **AC-004 (T1)**: Terminology: "utterance" for NLU, "message" for UI/DB
- **AC-005 (T2)**: Terminology: "confidence badge" (not "indicator")
- **AC-006**: Update all 3 artifacts (spec.md, plan.md, tasks.md) with consistent values
- **AC-007**: Create terminology glossary in spec.md

**Dependencies**:
- Feature 006 spec.md, plan.md, tasks.md (multiple locations)

**Priority**: P1 (MEDIUM - consistency critical for implementation)

---

#### FR-007-011: Add Missing Quality Gate Validations (M4)

**Description**: Add Battery Gate and Accessibility Gate validation to tasks.md TDT01 per constitution requirements.

**Acceptance Criteria**:
- **AC-001**: Update tasks.md TDT01 to include Battery Gate:
  - Tool: Android Profiler → Battery
  - Measurement: 1 hour active use (send messages, classify, teach)
  - Success: <10% battery drain
  - Platform: Physical device (Pixel 6 or equivalent)
- **AC-002**: Update tasks.md TDT01 to include Accessibility Gate:
  - Tool: Android Accessibility Scanner
  - Validation: WCAG 2.1 AAA compliance
  - Check: contentDescription, touch targets (≥48dp), color contrast (7:1)
  - Test: TalkBack navigation works
- **AC-003**: Add validation commands to TDT01 task
- **AC-004**: Update quality gate summary in tasks.md to show 8/8 gates

**Dependencies**:
- Constitution quality gates (constitution.md:L191-200)
- Feature 006 tasks.md:TDT01

**Priority**: P0 (MEDIUM - constitution compliance)

---

### Non-Functional Requirements

#### NFR-007-001: Remediation Speed

**Requirements**:
- Complete all 18 issue fixes in ≤4 hours (Tier 2 complexity)
- Zero introduction of new issues during remediation
- All edits validated via cross-artifact re-analysis

**Validation**:
- Re-run /ideacode.analyze after remediation
- Verify issue count = 0
- Generate before/after comparison report

---

#### NFR-007-002: Documentation Quality

**Requirements**:
- All changes documented with rationale
- Create remediation summary report
- Update living docs (notes.md, decisions.md)

**Validation**:
- Each fix has decision record in decisions.md
- Remediation report includes issue ID mapping

---

#### NFR-007-003: Backward Compatibility

**Requirements**:
- No breaking changes to existing spec/plan/tasks structure
- All original requirements preserved (may be re-worded for clarity)
- Version spec.md to 1.1.0 after remediation

**Validation**:
- All 11 original requirements still present
- All 35 tasks still present (may be updated)
- Requirement coverage remains ≥91%

---

### Success Criteria

**Remediation Complete When**:
1. ✅ All 18 issues resolved (CRITICAL → LOW)
2. ✅ Constitution violations = 0
3. ✅ Requirement coverage = 100% (11/11)
4. ✅ Cross-artifact analysis shows 0 issues
5. ✅ All 8 quality gates validated in tasks.md
6. ✅ Single source of truth established for duplicated content
7. ✅ Terminology glossary created
8. ✅ Remediation report generated

---

## User Stories

### Story 1: Constitution Compliance

**As a** project maintainer
**I want to** ensure all specifications comply with AVA constitution
**So that** we maintain privacy-first, integration-first principles throughout development

**Acceptance Criteria**:
- Given the constitution defines 8 quality gates
- When I run cross-artifact analysis after remediation
- Then zero constitution violations are reported
- And all 8 quality gates have validation tasks defined

---

### Story 2: Unambiguous Implementation

**As a** developer implementing Feature 006
**I want to** have clear, unambiguous acceptance criteria for all requirements
**So that** I don't waste time on rework or incorrect implementations

**Acceptance Criteria**:
- Given spec.md has performance requirements
- When I read NFR-006-001
- Then I see both target (<50ms p50) and maximum (<100ms p95) thresholds clearly defined
- And I know which threshold causes task failure

---

### Story 3: Single Source of Truth

**As a** developer implementing intent templates
**I want to** know the authoritative source for template definitions
**So that** I don't implement conflicting templates

**Acceptance Criteria**:
- Given intent templates appear in multiple files
- When I read spec.md
- Then I see a clear reference: "See plan.md for templates, IntentTemplates.kt for implementation"
- And spec.md does not duplicate template definitions

---

## Technical Constraints

### Files to Modify

**Specification Artifacts** (3 files):
- `specs/006-chat-ui/spec.md`
- `specs/006-chat-ui/plan.md`
- `specs/006-chat-ui/tasks.md`

**Constitution** (1 file, conditional):
- `.ideacode/memory/principles.md` (only if Option A chosen for C2)

**Intent Examples** (1 file):
- `apps/ava-standalone/src/main/assets/intent_examples.json`

**Total**: 4-5 files modified

### Edit Constraints

- Use exact line-by-line edits (no full file rewrites)
- Preserve existing structure and formatting
- Add version history section to spec.md
- Track all changes in remediation log

---

## Dependencies

### Internal Dependencies (AVA AI)

- ✅ **Feature 006 artifacts** - spec.md, plan.md, tasks.md exist
- ✅ **Constitution** - principles.md v1.3.1 exists
- ✅ **Analysis report** - 18 issues identified with locations

### External Dependencies

- ✅ **IDEACODE Framework** - v8.0 for re-analysis
- ✅ **Text editor** - for file modifications
- ✅ **JSON validator** - for intent_examples.json validation

---

## Out of Scope

**Not Part of This Remediation**:

1. **Implementation of Feature 006** - This spec only fixes specification artifacts, not code
2. **New features** - No feature additions, only clarifications
3. **Test code changes** - Specification-level fixes only
4. **Database migrations** - No schema changes
5. **UI mockups** - Spec clarifications sufficient

---

## Risks & Mitigation

### Risk 1: Cascading Changes

**Probability**: Medium (40%)
**Impact**: Medium (extends remediation time)

**Mitigation**:
- Fix issues in priority order (CRITICAL → HIGH → MEDIUM → LOW)
- Validate each fix before proceeding to next
- Re-run analysis after each CRITICAL/HIGH fix

**Contingency**:
- If new issues discovered, add to remediation spec
- Extend time estimate by 1 hour per additional CRITICAL issue

---

### Risk 2: Constitution Amendment Rejected

**Probability**: Low (20%)
**Impact**: High (requires different resolution for C2)

**Mitigation**:
- Document clear rationale for phased VOS4 integration
- Propose minimal amendment (add exception, not rewrite)

**Contingency**:
- If amendment rejected, implement Option B (make voice input P0)
- Accept extended timeline for Feature 006 (Day 7 → Day 10)

---

## Implementation Notes

### Remediation Order

**Phase 1: CRITICAL (30 minutes)**
1. C1: Fix intent_examples.json format
2. C2: Resolve VOS4 integration conflict

**Phase 2: HIGH (60 minutes)**
3. D1: Remove duplicate intent templates
4. A1: Clarify NLU performance thresholds
5. U1: Add show_history intent

**Phase 3: MEDIUM (60 minutes)**
6. I1, I2, I3, T1, T2: Standardize inconsistencies
7. U2: Clarify settings screen location
8. G1: Fix history navigation priority
9. M4: Add missing quality gate validations

**Phase 4: LOW + Validation (60 minutes)**
10. A2, G2, M1, M2, M3: Remaining low-priority fixes
11. Re-run /ideacode.analyze
12. Generate remediation report
13. Update living docs

**Total**: ~4 hours (Tier 2 complexity)

---

## Testing Strategy

### Validation Tests

**Test 1: Cross-Artifact Re-Analysis**
```bash
/ideacode.analyze
# Expected: 0 issues, 100% coverage
```

**Test 2: JSON Format Validation**
```bash
cat apps/ava-standalone/src/main/assets/intent_examples.json | jq .
# Expected: Valid JSON, compact array format
```

**Test 3: Grep for Terminology Consistency**
```bash
# Should find "utterance" in NLU contexts only
grep -n "utterance" specs/006-chat-ui/*.md

# Should find "confidence badge" (not "indicator")
grep -n "confidence" specs/006-chat-ui/*.md | grep -v "badge"
# Expected: Empty (all use "badge")
```

**Test 4: Constitution Compliance**
```bash
# Check quality gate count in tasks.md
grep -c "Quality Gate" specs/006-chat-ui/tasks.md
# Expected: 8
```

---

## Success Metrics

**Remediation Completion**:
- ✅ 18/18 issues resolved
- ✅ 0 constitution violations
- ✅ 100% requirement coverage (11/11)
- ✅ 0 ambiguities in acceptance criteria
- ✅ Single source of truth for all duplicated content
- ✅ All 8 quality gates validated

**Quality Gates**:
- ✅ Remediation time ≤ 4 hours
- ✅ Zero new issues introduced
- ✅ All original requirements preserved
- ✅ Documentation updated (decisions.md, notes.md)

---

## References

- **Parent Feature**: specs/006-chat-ui/spec.md (v1.0.0)
- **Analysis Report**: /ideacode.analyze output (2025-11-12)
- **Constitution**: .ideacode/memory/principles.md (v1.3.1)
- **IDEACODE Framework**: v8.0 methodology

---

**Status**: ✅ Specification Complete
**Next Step**: `/ideacode.plan` to create remediation implementation plan
**Estimated Effort**: 4 hours (Tier 2 complexity)
**YOLO Mode**: ENABLED (full automation with safety backups)

---

**Approved By**: User (YOLO mode authorized)
**Date**: 2025-11-12
**Version**: 1.0.0
