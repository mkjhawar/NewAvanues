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

## Completed canonical-manual expansion batch — 2026-02-09 (AI stack + AVACode + AvanueUI lineage)

### Objective

Address identified documentation gaps requested by user feedback:

- dedicated NLU/NLM/LLM coverage,
- AvanueUI lineage from AvaMagic/AvaUI,
- truthful AVACode status including disabled surfaces.

### Changes executed

1. Updated canonical TOC:
   - `Docs/MasterDocs/NewAvanues-Developer-Manual/README.md`
   - Added Chapter 10 and Chapter 11 entries under a new Part IV.
2. Expanded AvanueUI chapter:
   - `04-AvanueUI-Deep-Dive.md`
   - Added lineage, migration rules, and governance contract sections.
3. Added AI stack chapter:
   - `10-AI-Stack-NLU-NLM-LLM-Deep-Dive.md`
   - Documents NLU/NLM/LLM flow, AVA orchestration, reliability policies.
4. Added AVACode chapter:
   - `11-AVACode-Deep-Dive.md`
   - Documents active forms/workflow foundations and disabled generator/CLI/template surfaces.

### Outcome

- Canonical developer manual now explicitly covers the AI stack and AVACode.
- AvanueUI historical lineage is now documented inside canonical chapter set.
- Consolidation accuracy improved by clearly distinguishing active vs staged/disabled AVACode capabilities.

## Completed safe bulk archival wave — 2026-02-09 (exact-hash backup/old-path duplicates)

### Objective

Accelerate consolidation throughput by executing a deterministic, high-volume archival move wave for exact-hash duplicates where:

- source file is in backup/old-structure paths, and
- at least one non-backup canonical twin exists.

### Inputs

- Candidate list: `build/bulk_move_candidates_2026-02-09.csv`
- Candidate rows: **3134**
- Runner: `build/safe_bulk_move_runner_2026_02_09.py`

### Execution result

- Timestamp: `2026-02-09T14:15:15`
- Moved: **3134**
- Skipped: **0**
- Errors: **0**

### Outputs / audit artifacts

- Execution log CSV: `build/bulk_move_execution_2026-02-09.csv`
- Execution summary: `build/bulk_move_execution_2026-02-09_summary.txt`
- Archive destination root: `Docs/archive/superseded-safe-bulk/2026-02-09/`

### Outcome

- High-throughput duplicate cleanup completed in one safe wave with full move traceability.
- Canonical non-backup documents retained in-place; backup/old-path exact duplicates were relocated to archive.

## Completed merge-review execution wave — 2026-02-10 (wave5d: refined-candidate bucket B / VoiceOS)

### Objective

Run guarded near-duplicate execution for Bucket B (`Docs/VoiceOS/**`) using refined wave4b candidates.

### Inputs

- Candidate CSV: `build/merge_review_candidates_2026-02-10-wave4b.csv`
- Runner: `build/merge_review_wave5_runner_2026_02_10.py`
- Command:
  - `python3 build/merge_review_wave5_runner_2026_02_10.py --label 2026-02-10-wave5d --input-label 2026-02-10-wave4b --ratio-threshold 0.98 --bucket B`

### Execution result

- Candidate rows evaluated: **217**
- Archived: **0**
- Needs manual review: **140**
- Skipped due to bucket filter: **77**
- Errors: **0**

### Outputs / audit artifacts

- Execution log CSV: `build/merge_review_execution_2026-02-10-wave5d.csv`
- Execution summary: `build/merge_review_execution_2026-02-10-wave5d_summary.txt`
- High-confidence subset CSV: `build/merge_review_wave5d_high_confidence_voiceos.csv`
- High-confidence summary: `build/merge_review_wave5d_high_confidence_voiceos_summary.txt`

### Key findings

- Bucket B manual-review pool: **140**
- High-confidence-by-ratio subset (`>=0.98`): **38**
- Exact-hash pairs inside high-confidence subset: **0**
- Content-contained pairs inside high-confidence subset: **0**

### Outcome

- Guardrails prevented unsafe auto-archival in VoiceOS domain.
- VoiceOS near-duplicates require topic-batched manual merge flow.

## Created Wave6 VoiceOS topic batch — 2026-02-10 (batch1 from wave5d manual queue)

### Implementation

- Script: `build/merge_review_wave6_batch1_runner_2026_02_10.py`
- Output CSV: `build/merge_review_wave6_voiceos_topic_batch1.csv`
- Output summary: `build/merge_review_wave6_voiceos_topic_batch1_summary.txt`

### Batch1 result

- Manual-review pool considered: **140**
- Selected topics: `todo` (11), `spec` (7), `changelog` (5), `plan` (5), `voiceos-developer-manual` (5)
- Batch1 rows prepared: **33**

## Created Wave6 VoiceOS topic batch — 2026-02-10 (batch2 from remaining wave5d queue)

### Implementation

- Script: `build/merge_review_wave6_batch2_runner_2026_02_10.py`
- Excluded batch1 topics: `todo`, `spec`, `changelog`, `plan`, `voiceos-developer-manual`
- Output CSV: `build/merge_review_wave6_voiceos_topic_batch2.csv`
- Output summary: `build/merge_review_wave6_voiceos_topic_batch2_summary.txt`

### Batch2 result

- Manual-review pool considered: **140**
- Remaining queue after exclusion: **107**
- Selected topics: `tasks` (4), `voiceos-spec` (4), `voiceos-user-manual` (4), `voiceos-changelog-2025-10` (4), `developer-manual` (3)
- Batch2 rows prepared: **19**

### Outcome

- Wave6 batching now proceeds in deterministic, script-based slices (`batch1` then `batch2`) for safe high-throughput manual consolidation.

## Created Wave6 VoiceOS topic batch — 2026-02-10 (batch3 from remaining wave5d queue)

### Implementation

- Script: `build/merge_review_wave6_batch3_runner_2026_02_10.py`
- Excluded prior topics from batch1 + batch2:
  - `todo`, `spec`, `changelog`, `plan`, `voiceos-developer-manual`, `tasks`, `voiceos-spec`, `voiceos-user-manual`, `voiceos-changelog-2025-10`, `developer-manual`
- Output CSV: `build/merge_review_wave6_voiceos_topic_batch3.csv`
- Output summary: `build/merge_review_wave6_voiceos_topic_batch3_summary.txt`

### Batch3 result

- Manual-review pool considered: **140**
- Remaining queue after exclusion: **88**
- Selected topics: `project-status` (2), `voiceos-plan` (2), `00-index` (2), `voiceos-current-status` (2), `reorganization-plan` (2)
- Batch3 rows prepared: **10**

### Outcome

- Wave6 queue slicing now spans three deterministic batches with full audit artifacts.
- Remaining queue can continue with the same script-first approach (`batch4+`) without heredoc fragility.

## Created Wave6 VoiceOS topic batch — 2026-02-10 (batch4 from remaining wave5d queue)

### Implementation

- Script: `build/merge_review_wave6_batch4_runner_2026_02_10.py`
- Excluded prior topics from batch1 + batch2 + batch3:
  - `todo`, `spec`, `changelog`, `plan`, `voiceos-developer-manual`, `tasks`, `voiceos-spec`, `voiceos-user-manual`, `voiceos-changelog-2025-10`, `developer-manual`, `project-status`, `voiceos-plan`, `00-index`, `voiceos-current-status`, `reorganization-plan`
- Output CSV: `build/merge_review_wave6_voiceos_topic_batch4.csv`
- Output summary: `build/merge_review_wave6_voiceos_topic_batch4_summary.txt`

### Batch4 result

- Manual-review pool considered: **140**
- Remaining queue after exclusion: **78**
- Selected topics: `backlog` (1), `voiceos-uuid-vuid-migration` (1), `voiceoscoreng-fix-plan` (1), `decisions` (1), `architecture` (1)
- Batch4 rows prepared: **5**

### Outcome

- Wave6 queue slicing now spans four deterministic batches with full audit artifacts.
- Throughput remains stable via script-first continuation with explicit exclusion governance.

## Recovery continuation — 2026-02-10 (wave6 batch5→finish on durable path)

### Incident-aware recovery basis

- Previous `build/` execution artifacts were non-durable (`**/build/` ignored + clean task deletes root build dir).
- Recovered source candidates from:
  - `Docs/MasterDocs/merge_review_candidates_2026-02-10-wave4.xlsx`
- Recovery converter:
  - `contextsave/recover_wave4_xlsx_to_csv.py`
- Recovered CSV:
  - `contextsave/merge_review_candidates_2026-02-10-wave4_recovered.csv`

### Durable recovery execution

- Runner script:
  - `Docs/project-info/execution-artifacts/recovery_wave6_batch5_to_finish_2026_02_10.py`
- Durable artifact root:
  - `Docs/project-info/execution-artifacts/2026-02-10-wave6-recovery/`

### Reconstructed wave5d-equivalent summary

- Execution CSV:
  - `Docs/project-info/execution-artifacts/2026-02-10-wave6-recovery/merge_review_execution_2026-02-10-wave5d-recovered.csv`
- Execution summary:
  - `Docs/project-info/execution-artifacts/2026-02-10-wave6-recovery/merge_review_execution_2026-02-10-wave5d-recovered_summary.txt`
- Metrics:
  - candidate_rows=279
  - needs_manual_review=172
  - skipped_bucket_filter=107
  - archived=0
  - errors=0

### Wave6 continuation (starting after completed batch1–batch4 exclusions)

- Deterministic continuation generated **batch5 through batch20** (**16 batches**) under durable path.
- Final rollup:
  - `Docs/project-info/execution-artifacts/2026-02-10-wave6-recovery/merge_review_wave6_recovery_batch5_to_finish_summary.txt`

### Outcome

- User-requested continuation (`batch5→finish`) is completed with reproducible, non-`build/` artifacts.
- Recovery run is explicitly traceable and can be re-executed safely without dependence on transient build outputs.
