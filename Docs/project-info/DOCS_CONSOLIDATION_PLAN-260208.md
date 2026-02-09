# Docs Consolidation Plan (Inventory + Action Model)

## Quick inventory snapshot

- Total files in `Docs/` (excluding `.DS_Store`): **34,964**
- Largest top-level areas:
  - `Docs/VoiceOS`: **32,978**
  - `Docs/AVA`: **1,005**
  - `Docs/Avanues`: **487**
  - `Docs/WebAvanue`: **202**

## Repository-wide scope extension (requested)

To ensure cleanup/documentation decisions are correct across the entire repo (not only `Docs/`), this plan now includes top-level inventory context for `/NewAvanues`.

- Total files in repository (excluding `.git` and `.DS_Store`): **322,496**
- Top-level distribution snapshot:
  - `Modules`: **242,849**
  - `Docs`: **34,966**
  - `Archives`: **18,505**
  - `apps`: **13,948**
  - `android`: **9,117**
  - `vivoka`: **2,753**

Root files in `/NewAvanues` (top level):

- `.gitignore`
- `.java-version`
- `build.gradle.kts`
- `CONTRIBUTING.md`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `local.properties`
- `README.md`
- `RECOVERY_ROADMAP.md`
- `settings.gradle.kts`
- `VERSION`

### Repo-wide cleanup order (high-level)

1. **Canonical docs model first** (`Docs/`), so references are stable
2. **Root-level governance files** (`README`, `CONTRIBUTING`, roadmap/versioning)
3. **Module/app documentation alignment** (`Modules/`, `apps/`, `android/` doc links)
4. **Archive boundary enforcement** (`Archives/` vs active trees)

This avoids doc drift and ensures developer documentation points to actual live code locations.

## External docs discovery (outside `Docs/`) — verified snapshot

Using a pruned scan (excluding `.git`, `build`, `.gradle`, `node_modules`, `dist`, `out`, `target`, cache dirs), we identified:

- Candidate documentation files outside `Docs/`: **531**
- Source list file: `Docs/project-info/EXTERNAL_DOCS_CANDIDATES-260208.txt`

Top-level distribution of these 531 candidates:

- `Archives`: **281**
- `Modules`: **150**
- `.claude`: **24**
- `.ideacode`: **19**
- `knowledgebase`: **17**
- `Shared`: **8**
- `android`: **7**

Top hotspot sub-areas:

- `Archives/Common-Deprecated-260202`: **163**
- `Archives/AvaMagic`: **61**
- `Modules/AvanueUI`: **38**
- `Modules/AI`: **38**
- `Archives/AVAMagic-Core-260202`: **20**
- `Modules/WebAvanue`: **17**
- `knowledgebase/modules`: **16**

### Important double-check note

Some files in the candidate set are not true docs despite matching naming patterns (e.g., `*Migration*.kt`, generated artifacts, IDE/session memory files). They must be filtered before move/merge. Do not bulk-migrate blindly.

## Recommended strategy

Use **new-to-old (current-to-legacy)** as the default cleanup direction.

Why:
- Prevents deleting active source-of-truth docs by mistake.
- Lets older docs be archived only after newer canonical docs are confirmed.
- Reduces risk while we build formal developer documentation.

## Proposed classification model

For each document, assign exactly one status:

- **KEEP (Canonical)**: current source-of-truth
- **MERGE**: valuable content but should be absorbed into canonical doc
- **ARCHIVE**: historical/reference only, not active guidance
- **DELETE**: redundant/noisy/temp/duplicate with no value

## Priority cleanup waves

1. **Root-level `Docs/*.md` normalization** (quick wins, obvious overlaps)
2. **`Docs/VoiceOS/` triage by subfolder** (highest volume impact)
3. **`Docs/AVA/` + `Docs/Avanues/` harmonization**
4. **`Docs/WebAvanue/` cleanup and move user/dev split into clear sections**
5. Archive/deletion batch once canonical replacements are confirmed

## Root-level overlap candidates (review first)

Potential duplicate/near-duplicate sets:

- `INTELLIGENT-COMMANDS-FRAMEWORK.md`
- `VoiceOS-IntelligentCommands-Framework-51207-V1.md`

- `INTELLIGENT-COMMANDS-IMPLEMENTATION.md`
- `VoiceOS-IntelligentCommands-Implementation-51207-V1.md`

- `intelligent-commands-test-results.md`
- `VoiceOS-IntelligentCommands-TestResults-51207-V1.md`

- `v9-consolidation-test-report.md`
- `VoiceOS-V9-ConsolidationTestReport-51207-V1.md`

Recommendation: keep one canonical naming convention and archive/remove the alternate after content diff.

## VoiceOS concentration insight

Largest sub-areas inside `Docs/VoiceOS/`:

- `Technical` (**18,687**)
- `archive` (**12,125**)
- `modules` (**525**)
- `VoiceOSMasterOld` (**432**)

This indicates significant historical accumulation and likely high redundancy.

## Guardrails before delete

1. No hard delete until candidate is tagged `DELETE` and reviewed.
2. Prefer move to `archive/` first when uncertain.
3. Add front-matter tag to canonical docs (owner, last-reviewed, status).
4. Keep a changelog of doc moves/renames/deletions.

## Immediate next deliverables

- Consolidation matrix CSV/MD for top-level + VoiceOS high-impact folders
- Canonical doc map (`topic -> canonical file`)
- Developer documentation structure and starter templates

## Completed consolidation batch — 2026-02-09

### Batch scope (root-level overlap set)

- `INTELLIGENT-COMMANDS-FRAMEWORK.md`
- `INTELLIGENT-COMMANDS-IMPLEMENTATION.md`
- `intelligent-commands-test-results.md`
- `v9-consolidation-test-report.md`

### Verification

- File-pair checksum verification completed (`shasum`): each `VoiceOS-*` root-level counterpart matched its canonical file exactly.
- Result: no unique content required merge for this batch.

### Actions executed

- Canonical files kept in place under `Docs/` root.
- Superseded duplicates moved to:
  - `Docs/archive/superseded-root-duplicates/2026-02-09/`
- Archived files annotated with canonical pointer warning block.

### Archived files (superseded duplicates)

- `Docs/archive/superseded-root-duplicates/2026-02-09/VoiceOS-IntelligentCommands-Framework-51207-V1.md`
- `Docs/archive/superseded-root-duplicates/2026-02-09/VoiceOS-IntelligentCommands-Implementation-51207-V1.md`
- `Docs/archive/superseded-root-duplicates/2026-02-09/VoiceOS-IntelligentCommands-TestResults-51207-V1.md`
- `Docs/archive/superseded-root-duplicates/2026-02-09/VoiceOS-V9-ConsolidationTestReport-51207-V1.md`

## Completed streamlining batch — 2026-02-09 (YOLO)

### Objective

Reduce `Docs/` root clutter by relocating clearly scoped root markdown files into existing domain folders without deleting content.

### Moves executed

- Cockpit root docs → `Docs/Cockpit/LivingDocs/`
  - `Cockpit-Legacy-Gaps-Analysis-51210-V1.md`
  - `Cockpit-Phase1-CompletionSummary-51210-V1.md`
  - `Cockpit-Phase2-CompletionSummary-51210-V1.md`
  - `Cockpit-Sprint-Status-51210-V1.md`
  - `Cockpit-Testing-Guide-51210-V1.md`
  - `Cockpit-Window-Controls-Spec-51210-V1.md`
- NewAvanues root guide → `Docs/NewAvanues/LivingDocs/`
  - `NewAvanues-Monorepo-KMP-Structure-Guide-251223-V1.md`
- VoiceOS root build report → `Docs/VoiceOS/Reports/`
  - `VoiceOS-Build-Errors-251223-V1.md`
- AvanueUI integration guide → `Docs/MasterDocs/AvanueUI/`
  - `AvanueUI-DesignSystem-Integration-Guide-260208.md`
- Generic phase summaries → `Docs/project-info/`
  - `PHASE-1-COMPLETION-SUMMARY.md`
  - `PHASE-2-COMPLETION-SUMMARY.md`

### Outcome

- `Docs/` root is now significantly cleaner and more category-driven.
- All moves are reversible (no hard deletes performed).

## Completed archival batch — 2026-02-09 (to-be-deleted staging)

### Objective

Move clearly finished fix/issue documents out of active `Docs/Issues` and `Docs/fixes` paths into a deletion-candidate staging area for later review.

### Staging location

- `Docs/archive/to-be-deleted/2026-02-09/`

### Files moved

- `Docs/Issues/Cockpit-Phase1-NotWorking-Analysis-51210-V1.md`
- `Docs/Issues/VoiceOS-ComposeView-Lifecycle-51210-V1.md`
- `Docs/Issues/VoiceOS-Database-SQLDelight2-51210-V1.md`
- `Docs/fixes/AvanueUI/AvanueUI-Fix-GlassTransparencyReadability-260208-V1.md`

### Outcome

- Active issue/fix folders now focus on open/in-progress work.
- Finished items are staged for delete decision, not hard-deleted.

## Completed canonical-manual batch — 2026-02-09 (Developer Manual foundation)

### Objective

Create a detailed, chaptered, canonical developer manual that can become the single source of truth for engineering documentation and future chapter expansion.

### Canonical location created

- `Docs/MasterDocs/NewAvanues-Developer-Manual/`

### Files created

- `README.md` (master TOC + chapter map)
- `01-Platform-Vision-And-Principles.md`
- `02-System-Architecture-And-Flows.md`
- `03-VoiceOSCore-Deep-Dive.md`
- `04-AvanueUI-Deep-Dive.md`
- `05-WebAvanue-Deep-Dive.md`
- `06-NewAvanues-App-Composition.md`
- `07-Design-Details-VoiceTouch-UX.md`
- `08-Decisions-ADR-Index.md`
- `09-Development-Runbook.md`

### Scope delivered

- Voice-first platform vision and terminology
- End-to-end architectural flows and layered model
- Deep dives for VoiceOSCore, AvanueUI, WebAvanue, and the New Avanues app composition
- Design details for VoiceTouch™ interaction model
- ADR-style decision index and governance rules
- Developer runbook for build/debug/extension workflows

### Outcome

- Canonical manual foundation is now in place and expandable by chapter.
- Consolidation can proceed by merging legacy docs into this canonical chapter structure.
