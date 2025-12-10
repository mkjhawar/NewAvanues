<!--
filename: Naming-Convention-Exemption-Policy-251015-1840.md
created: 2025-10-15 18:40:00 PDT
author: AI Documentation Agent
purpose: Official exemption policy for living documents - timestamp requirements
last-modified: 2025-10-15 18:40:00 PDT
version: 1.0.0
-->

# VOS4 Naming Convention Exemption Policy

**Approved:** 2025-10-15 18:40:00 PDT
**Decision:** Option A - Exempt Living Documents
**Status:** OFFICIAL POLICY

---

## Executive Summary

**Living documents** that are continuously updated and serve as master references are **EXEMPT** from timestamp requirements.

All other documentation files **MUST** include timestamps in the format: `YYMMDD-HHMM.md`

---

## Exempt File Categories

### 1. Special System Files (Always Exempt)
**Pattern:** Exact filename match
- `README.md` - Project/module readme files
- `INDEX.md` - Index and navigation files
- `CLAUDE.md` - AI agent instruction files
- `LICENSE.md` - License files
- `.gitignore` - Git configuration

**Rationale:** Standard system files with stable names

---

### 2. Protocol Files (Living Documents)
**Pattern:** `Protocol-*.md`
- `Protocol-VOS4-Documentation.md`
- `Protocol-VOS4-Coding-Standards.md`
- `Protocol-VOS4-Commit.md`
- `Protocol-VOS4-Pre-Implementation-QA.md`
- `Protocol-VOS4-Agent-Deployment.md`

**Rationale:** Continuously updated master protocols that agents reference

---

### 3. Reference Files (Living Documents)
**Pattern:** `Reference-*.md`
- `Reference-VOS4-Mandatory-Rules.md`
- `Reference-VOS4-Documentation-Structure.md`
- `Reference-VOS4-Namespace-Rules.md`
- `Reference-VOS4-Session-Learnings.md`
- `Reference-VOS3-Legacy-Design.md`
- `Reference-VOS3-Legacy-Standards.md`

**Rationale:** Master reference documents updated across sessions

---

### 4. Standards Files (Living Documents)
**Pattern:** `Standards-*.md`
- `Standards-VOS4-Architecture.md`
- `Standards-Documentation-And-Instructions-v1.md`

**Rationale:** Continuously evolving standards and guidelines

---

### 5. Context Files (Living Documents)
**Pattern:** `Context-*.md` or `*-Context.md`
- `Context-VOS4-UUIDCreator-Module.md`
- `VoiceOS-Project-Context.md`

**Rationale:** Master context documents that evolve over time

---

### 6. Template Files (Always Exempt)
**Pattern:** `*-Template.md` or `Template-*.md`
- `ADR-Template.md`
- `TODO-Template.md`

**Rationale:** Reusable templates, not instance documents

---

### 7. Master TODO/Status Files (Living Documents)
**Pattern:** `PROJECT-TODO-MASTER.md`, `PROJECT-STATUS-CURRENT.md`
- `PROJECT-TODO-MASTER.md`
- `PROJECT-TODO-PRIORITY.md`
- `PROJECT-STATUS-CURRENT.md`
- `PROJECT-STATUS-SUMMARY.md`

**Rationale:** Continuously updated tracking documents

**Note:** Instance-based TODO/Status files (e.g., module-specific) **DO require timestamps**

---

### 8. Changelog Master Files (Partial Exemption)
**Pattern:** `CHANGELOG-MASTER.md`, `CHANGELOG-CURRENT.md`
- `CHANGELOG-MASTER.md` - EXEMPT (continuously updated)
- `changelog.md` - EXEMPT (module master changelog)

**Note:** Archived changelogs and dated changelogs **DO require timestamps**

---

## Files That REQUIRE Timestamps

### 1. Status Reports (Instance-Based)
**Pattern:** `*-Status-YYMMDD-HHMM.md`
- Module status reports
- Feature status reports
- Implementation status reports
- Build status reports

**Example:**
- ✅ `VoiceAccessibility-Status-251015-1840.md`
- ❌ `VoiceAccessibility-Status.md`

---

### 2. TODO Lists (Instance-Based)
**Pattern:** `*-TODO-YYMMDD-HHMM.md`
- Module TODO lists
- Feature TODO lists
- Implementation checklists

**Example:**
- ✅ `CommandManager-TODO-251015-1840.md`
- ❌ `CommandManager-TODO.md`

---

### 3. Architecture Documents
**Pattern:** `*-Architecture-YYMMDD-HHMM.md`
- System architecture docs
- Module architecture docs
- Component architecture docs

**Example:**
- ✅ `VoiceCursor-Architecture-251015-1840.md`
- ❌ `VoiceCursor-Architecture-Plan.md`

---

### 4. Implementation Guides
**Pattern:** `*-Implementation-YYMMDD-HHMM.md`
- Feature implementation guides
- Module implementation plans
- Integration guides

**Example:**
- ✅ `UUIDCreator-Implementation-Guide-251015-1840.md`
- ❌ `DeviceDetector-Implementation-Guide.md`

---

### 5. Analysis Documents
**Pattern:** `*-Analysis-YYMMDD-HHMM.md`
- Code analysis reports
- Performance analysis
- Architecture analysis

**Example:**
- ✅ `DeviceManager-Analysis-251015-1840.md`
- ❌ `DeviceManager-Pattern-Analysis.md`

---

### 6. Developer Manuals (Module-Specific)
**Pattern:** `*-Developer-Manual-YYMMDD-HHMM.md` or `*-Guide-YYMMDD-HHMM.md`

**Example:**
- ✅ `VoiceCursor-Developer-Guide-251015-1840.md`
- ❌ `VoiceCursor-Developer-Manual.md`

**Exception:** If it's the ONLY developer manual for a module and continuously updated, may be exempt as `ModuleName-Developer-Manual.md`

---

### 7. Architecture Decision Records (ADRs)
**Pattern:** `ADR-NNN-Topic-YYMMDD-HHMM.md`
- All ADRs must have timestamps
- ADRs are historical snapshots of decisions

**Example:**
- ✅ `ADR-001-MagicUI-Implementation-Plan-251014-0313.md`
- ❌ `ADR-001-Documentation-Restructure.md`

---

## Exemption Determination Flow

```
Is the file one of:
  - README.md, INDEX.md, CLAUDE.md, LICENSE.md?
    → YES: EXEMPT
    → NO: Continue

Does the filename start with:
  - Protocol-, Reference-, Standards-, Context-?
    → YES: EXEMPT
    → NO: Continue

Does the filename end with:
  - -Template.md?
    → YES: EXEMPT
    → NO: Continue

Is it one of:
  - PROJECT-TODO-MASTER.md
  - PROJECT-TODO-PRIORITY.md
  - PROJECT-STATUS-CURRENT.md
  - PROJECT-STATUS-SUMMARY.md
  - CHANGELOG-MASTER.md
  - changelog.md (in module folder)
    → YES: EXEMPT
    → NO: Continue

ALL OTHER FILES → REQUIRE TIMESTAMP
```

---

## Implementation Rules

### When Creating New Documents

1. **Determine Category:**
   - Is this a living document that will be continuously updated?
   - Or is this a snapshot/instance document?

2. **Apply Naming:**
   - **Living Document:** Use base name (e.g., `Protocol-VOS4-NewFeature.md`)
   - **Instance Document:** Use timestamped name (e.g., `NewFeature-Status-251015-1840.md`)

3. **Get Timestamp:**
   ```bash
   date "+%y%m%d-%H%M"
   ```

---

### When Updating Existing Documents

#### Living Documents (Exempt):
- Update file in-place
- Update `last-modified` in header
- Update changelog section in document
- NO need to create new timestamped copy

#### Instance Documents (Require Timestamp):
- **Option A:** Create NEW timestamped file
- **Option B:** Update in-place if it's the most recent version
- Archive old timestamped versions to `/docs/archive/` or appropriate archive folder

---

## Migration Plan

### Phase 1: Identify Exempt Files
- All files matching exemption patterns remain unchanged
- Total exempt files: ~150

### Phase 2: Rename Non-Exempt Files
- All other files get timestamped: `YYMMDD-HHMM`
- Use current date/time: `251015-1840`
- Total files to rename: ~400

### Phase 3: Update Cross-References
- Update all documentation links
- Update agent instruction references
- Update README files

---

## Examples

### ✅ CORRECT - Exempt Files
```
docs/ProjectInstructions/Protocol-VOS4-Documentation.md
docs/ProjectInstructions/Reference-VOS4-Mandatory-Rules.md
docs/ProjectInstructions/Standards-VOS4-Architecture.md
docs/master/tasks/PROJECT-TODO-MASTER.md
docs/master/status/PROJECT-STATUS-CURRENT.md
docs/modules/VoiceCursor/README.md
docs/templates/ADR-Template.md
```

### ✅ CORRECT - Timestamped Files
```
docs/modules/VoiceCursor/status/VoiceCursor-Status-251015-1840.md
docs/modules/CommandManager/architecture/CommandManager-Architecture-251015-1840.md
docs/planning/architecture/decisions/ADR-001-MagicUI-Implementation-Plan-251014-0313.md
docs/master/tasks/completed/VOS4-TODO-Master-251011-0110.md
```

### ❌ INCORRECT - Should Be Timestamped
```
docs/modules/VoiceCursor/architecture/voice-cursor-architecture-plan.md
→ Should be: VoiceCursor-Architecture-Plan-251015-1840.md

docs/modules/DeviceManager/reference/DeviceManager-Overview.md
→ Should be: DeviceManager-Overview-251015-1840.md

docs/planning/implementation/DeviceDetector-Implementation-Guide.md
→ Should be: DeviceDetector-Implementation-Guide-251015-1840.md
```

---

## Enforcement

### Automated Tools
- Pre-commit hook to verify naming conventions
- Documentation audit script (already created)
- Rename automation script (to be generated)

### Manual Review
- All new documentation reviewed for naming compliance
- Monthly audit of documentation structure
- Immediate fixes for violations discovered

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-10-15 18:40 PDT | Initial policy - Option A approved (Exempt living documents) |

---

**Next Steps:**
1. Generate rename script for ~400 non-exempt files
2. Execute renames with backups
3. Update cross-references
4. Verify 100% compliance

**Policy Status:** OFFICIAL - Approved for Implementation
