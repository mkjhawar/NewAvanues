# Repo-wide Doc Migration Matrix (Outside `Docs/`)

This matrix is based on the verified source list:

- `Docs/project-info/EXTERNAL_DOCS_CANDIDATES-260208.txt` (**531** entries)
- Of these, **50** are tooling/runtime artifacts (`.claude`, `.ideacode`, `.codeavenue`, `.idea`, `contextsave`) and should be excluded from product docs migration.

## Verified area distribution (after excluding tooling/runtime artifacts)

| Source Area | Count | Proposed Action | Target in `Docs/` | Notes |
|---|---:|---|---|---|
| `Archives/**` | 281 | `ARCHIVE_IMPORT` | `Docs/archive/imported/` | Keep historical; do not make canonical directly |
| `Modules/**` | 150 | `MERGE_TO_MASTER` | Feature master docs (`Docs/AI`, `Docs/AVA`, `Docs/Avanues`, `Docs/VoiceOS`, `Docs/WebAvanue`) | Consolidate by feature |
| `knowledgebase/**` | 17 | `MERGE_TO_MASTER` | `Docs/Reference/Knowledgebase/` | Normalize into reference pages |
| `Shared/DesignStandards/**` | 8 | `KEEP_AND_INDEX` | `Docs/Development/Standards/` | Canonical standards; index, not duplicate |
| `android/**` docs | 7 | `MERGE_TO_MASTER` | `Docs/Development/Apps/Android/` | App/platform docs |
| `Demo/**` docs | 5 | `ARCHIVE_IMPORT` | `Docs/archive/demos/` | Demo collateral |
| `apps/**` docs | 3 | `MERGE_TO_MASTER` | `Docs/Development/Apps/` | App architecture/usage docs |
| `Tools/**` docs | 2 | `MERGE_TO_MASTER` | `Docs/Development/Tools/` | Tooling docs |
| Root governance docs | 3 (`README`, `CONTRIBUTING`, `RECOVERY_ROADMAP`) | `KEEP_AND_INDEX` | `Docs/00-Index/` | Keep in root; reference from docs index |

## Hotspot sub-areas (priority order)

| Sub-area | Count | Priority |
|---|---:|---|
| `Archives/Common-Deprecated-260202` | 163 | P1 |
| `Archives/AvaMagic` | 61 | P1 |
| `Modules/AvanueUI` | 38 | P1 |
| `Modules/AI` | 38 | P1 |
| `Archives/AVAMagic-Core-260202` | 20 | P2 |
| `Modules/WebAvanue` | 17 | P2 |
| `knowledgebase/modules` | 16 | P2 |
| `Archives/deprecated` | 16 | P2 |

## Consolidation method (feature-first, old→new changelog continuity)

For each feature (AI, AVA, VoiceOS, WebAvanue, Avanues):

1. Identify all related files across `Modules`, `Archives`, `apps`, `android`, `knowledgebase`.
2. Select one canonical master doc path in `Docs/<Feature>/Master/`.
3. Merge useful content only (architecture, APIs, decisions, guides, known issues).
4. Record migration/deprecation in changelog.
5. Mark old files as `ARCHIVED`/`DEPRECATED` in migration index.

## Required master artifacts per feature

Create/maintain these files:

- `Master-Overview.md`
- `Master-Architecture.md`
- `Master-Developer-Guide.md`
- `Master-Testing-and-Validation.md`
- `Master-Changelog.md` (old → new merge entries)
- `Deprecations-and-Removals.md`

## Changelog entry template

```md
## YYYY-MM-DD — Consolidation Batch
- Source: <old path>
- Destination: <new master path>
- Type: merge | deprecate | archive | remove
- Summary: <what moved>
- Verification: <links / notes>
```

## Safety checklist (double-check / anti-hallucination)

- [ ] Source file exists and is readable.
- [ ] Content actually unique before merge.
- [ ] Canonical target already defined.
- [ ] Migration log updated.
- [ ] Deprecation/removal recorded.
- [ ] Cross-links updated in `Docs/00-Index/README.md`.

## Execution batches (recommended)

### Batch 1 — Exclude non-product docs

Do **not** migrate these into product master docs:

- `.claude/**`
- `.ideacode/**`
- `.codeavenue/**`
- `.idea/**`
- `contextsave/**`

Reason: tooling/session/runtime artifacts, not product developer documentation.

### Batch 2 — Module and app docs into feature masters

Priority order:

1. `Modules/AI/**` + related `knowledgebase/modules/AI*`
2. `Modules/AVA/**` + Avanues/AvanueUI docs
3. `Modules/WebAvanue/**`
4. `Modules/VoiceOSCore/**`, `Modules/SpeechRecognition/**`, `Modules/Voice/**`
5. `android/apps/**` and `apps/**` architecture/readme docs

### Batch 3 — Archives import (historical only)

Import `Archives/**` into `Docs/archive/imported/**` with clear status metadata:

- `status: archived`
- `source_of_truth: false`
- `superseded_by: <master doc path>` (if known)

### Batch 4 — Deprecation/removal bookkeeping

For every merged/superseded source file, record:

- Source path
- Destination master path
- Action (`merged`, `archived`, `deprecated`, `removed`)
- Verification note

Use a dedicated feature changelog and a central deprecations log.

## Execution log — 2026-02-09 (Completed)

### Batch executed

- **Root-level duplicate normalization (Docs):** Intelligent Commands + v9 test report pair set.

### Verification evidence

- Checksums matched exactly for each canonical/duplicate pair (`shasum`):
  - `INTELLIGENT-COMMANDS-FRAMEWORK.md` == `VoiceOS-IntelligentCommands-Framework-51207-V1.md`
  - `INTELLIGENT-COMMANDS-IMPLEMENTATION.md` == `VoiceOS-IntelligentCommands-Implementation-51207-V1.md`
  - `intelligent-commands-test-results.md` == `VoiceOS-IntelligentCommands-TestResults-51207-V1.md`
  - `v9-consolidation-test-report.md` == `VoiceOS-V9-ConsolidationTestReport-51207-V1.md`

### Actions completed

- Kept canonical docs at `Docs/` root.
- Moved superseded duplicates to:
  - `Docs/archive/superseded-root-duplicates/2026-02-09/`
- Added in-file archive warning banner with canonical pointer to each moved duplicate.

### Migration bookkeeping entries

| Source path | Destination/canonical | Action | Verification |
|---|---|---|---|
| `Docs/VoiceOS-IntelligentCommands-Framework-51207-V1.md` | `Docs/INTELLIGENT-COMMANDS-FRAMEWORK.md` | archived (superseded duplicate) | checksum match + archived banner |
| `Docs/VoiceOS-IntelligentCommands-Implementation-51207-V1.md` | `Docs/INTELLIGENT-COMMANDS-IMPLEMENTATION.md` | archived (superseded duplicate) | checksum match + archived banner |
| `Docs/VoiceOS-IntelligentCommands-TestResults-51207-V1.md` | `Docs/intelligent-commands-test-results.md` | archived (superseded duplicate) | checksum match + archived banner |
| `Docs/VoiceOS-V9-ConsolidationTestReport-51207-V1.md` | `Docs/v9-consolidation-test-report.md` | archived (superseded duplicate) | checksum match + archived banner |

## Execution log — 2026-02-09 (YOLO streamlining)

### Batch executed

- **Root docs declutter (category realignment):** moved clearly-scoped root markdown files into existing domain folders.

### Actions completed

- Moved Cockpit docs from root to `Docs/Cockpit/LivingDocs/`.
- Moved NewAvanues structure guide from root to `Docs/NewAvanues/LivingDocs/`.
- Moved VoiceOS build error report from root to `Docs/VoiceOS/Reports/`.
- Moved AvanueUI integration guide from root to `Docs/MasterDocs/AvanueUI/`.
- Moved generic phase summaries from root to `Docs/project-info/`.

### Migration bookkeeping entries

| Source path | Destination path | Action | Verification |
|---|---|---|---|
| `Docs/Cockpit-Legacy-Gaps-Analysis-51210-V1.md` | `Docs/Cockpit/LivingDocs/Cockpit-Legacy-Gaps-Analysis-51210-V1.md` | moved | destination presence verified |
| `Docs/Cockpit-Phase1-CompletionSummary-51210-V1.md` | `Docs/Cockpit/LivingDocs/Cockpit-Phase1-CompletionSummary-51210-V1.md` | moved | destination presence verified |
| `Docs/Cockpit-Phase2-CompletionSummary-51210-V1.md` | `Docs/Cockpit/LivingDocs/Cockpit-Phase2-CompletionSummary-51210-V1.md` | moved | destination presence verified |
| `Docs/Cockpit-Sprint-Status-51210-V1.md` | `Docs/Cockpit/LivingDocs/Cockpit-Sprint-Status-51210-V1.md` | moved | destination presence verified |
| `Docs/Cockpit-Testing-Guide-51210-V1.md` | `Docs/Cockpit/LivingDocs/Cockpit-Testing-Guide-51210-V1.md` | moved | destination presence verified |
| `Docs/Cockpit-Window-Controls-Spec-51210-V1.md` | `Docs/Cockpit/LivingDocs/Cockpit-Window-Controls-Spec-51210-V1.md` | moved | destination presence verified |
| `Docs/NewAvanues-Monorepo-KMP-Structure-Guide-251223-V1.md` | `Docs/NewAvanues/LivingDocs/NewAvanues-Monorepo-KMP-Structure-Guide-251223-V1.md` | moved | destination presence verified |
| `Docs/VoiceOS-Build-Errors-251223-V1.md` | `Docs/VoiceOS/Reports/VoiceOS-Build-Errors-251223-V1.md` | moved | destination presence verified |
| `Docs/AvanueUI-DesignSystem-Integration-Guide-260208.md` | `Docs/MasterDocs/AvanueUI/AvanueUI-DesignSystem-Integration-Guide-260208.md` | moved | destination presence verified |
| `Docs/PHASE-1-COMPLETION-SUMMARY.md` | `Docs/project-info/PHASE-1-COMPLETION-SUMMARY.md` | moved | destination presence verified |
| `Docs/PHASE-2-COMPLETION-SUMMARY.md` | `Docs/project-info/PHASE-2-COMPLETION-SUMMARY.md` | moved | destination presence verified |

## Execution log — 2026-02-09 (to-be-deleted staging)

### Batch executed

- **Finished fixes/issues archival:** moved completed/fixed docs from active issue/fix areas to deletion-candidate staging.

### Target archive

- `Docs/archive/to-be-deleted/2026-02-09/`

### Migration bookkeeping entries

| Source path | Destination path | Action | Verification |
|---|---|---|---|
| `Docs/Issues/Cockpit-Phase1-NotWorking-Analysis-51210-V1.md` | `Docs/archive/to-be-deleted/2026-02-09/Issues/Cockpit-Phase1-NotWorking-Analysis-51210-V1.md` | moved to to-be-deleted | destination presence verified |
| `Docs/Issues/VoiceOS-ComposeView-Lifecycle-51210-V1.md` | `Docs/archive/to-be-deleted/2026-02-09/Issues/VoiceOS-ComposeView-Lifecycle-51210-V1.md` | moved to to-be-deleted | destination presence verified |
| `Docs/Issues/VoiceOS-Database-SQLDelight2-51210-V1.md` | `Docs/archive/to-be-deleted/2026-02-09/Issues/VoiceOS-Database-SQLDelight2-51210-V1.md` | moved to to-be-deleted | destination presence verified |
| `Docs/fixes/AvanueUI/AvanueUI-Fix-GlassTransparencyReadability-260208-V1.md` | `Docs/archive/to-be-deleted/2026-02-09/fixes/AvanueUI-Fix-GlassTransparencyReadability-260208-V1.md` | moved to to-be-deleted | destination presence verified |

## Execution log — 2026-02-09 (canonical developer manual foundation)

### Batch executed

- Created canonical chaptered manual under:
  - `Docs/MasterDocs/NewAvanues-Developer-Manual/`

### Artifacts created

| Artifact | Purpose |
|---|---|
| `README.md` | canonical TOC and chapter navigation |
| `01-Platform-Vision-And-Principles.md` | platform framing and terminology |
| `02-System-Architecture-And-Flows.md` | layered architecture + end-to-end flow diagrams |
| `03-VoiceOSCore-Deep-Dive.md` | execution kernel details and extension model |
| `04-AvanueUI-Deep-Dive.md` | design system architecture and usage governance |
| `05-WebAvanue-Deep-Dive.md` | web voice-execution model and bridge contracts |
| `06-NewAvanues-App-Composition.md` | app-shell integration and lifecycle contracts |
| `07-Design-Details-VoiceTouch-UX.md` | VoiceTouch™ UX state model and interaction rules |
| `08-Decisions-ADR-Index.md` | architecture decision index and rationale |
| `09-Development-Runbook.md` | build/debug/extend operational runbook |

### Action classification

- Type: `MERGE_TO_MASTER` (foundation stage)
- Scope: canonicalization target created; source merges from legacy docs pending per-topic batches

### Outcome

- A stable canonical destination now exists for subsequent doc merges/deprecations.
- Future consolidation work can map old docs to chapter-level destinations with clear traceability.
