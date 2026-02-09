# Developer Documentation Blueprint

## Goal

Create a stable, canonical developer documentation system that is easy to maintain and safe to evolve.

## Recommended canonical structure

```text
Docs/
  00-Index/
    README.md                      # master docs index + navigation
    OWNERSHIP.md                   # doc owners + review cadence
  01-Getting-Started/
    Local-Setup.md
    Build-Run-Test.md
    Repo-Structure.md
  02-Architecture/
    System-Overview.md
    Module-Map.md
    Data-Flow.md
    ADR/                           # architecture decisions
  03-Development/
    Coding-Standards.md
    Feature-Development-Guide.md
    Debugging-Guide.md
  04-Modules/
    VoiceOS/
    AVA/
    Avanues/
    WebAvanue/
  05-Operations/
    Release-Process.md
    Versioning.md
    Incident-Runbook.md
  06-Testing/
    Test-Strategy.md
    Integration-Testing.md
    QA-Checklist.md
  07-Reference/
    Glossary.md
    API-References.md
  archive/
    ... historical docs moved from legacy trees
```

## Canonical metadata template (front-matter)

Use this at the top of every active developer doc:

```yaml
---
title: ""
owner: ""
status: "active" # active | draft | archived | deprecated
last_reviewed: "YYYY-MM-DD"
source_of_truth: true
replaces: []
---
```

## Consolidation workflow (repeatable)

1. Pick topic area (e.g., VoiceOS Intelligent Commands).
2. Locate all matching docs (name + folder + date variants).
3. Mark one **canonical target** file.
4. Merge missing content into canonical.
5. Mark others as archived/deprecated.
6. Update index and ownership mapping.

## First documentation tracks to write in detail

1. **Developer onboarding** (setup, build, run, test)
2. **Architecture map** (monorepo + module dependencies)
3. **Module guides** (VoiceOS, AVA, Avanues, WebAvanue)
4. **Release + testing standards**

## Immediate migration candidates (from root overlaps)

- Intelligent commands framework/implementation/test-results pair sets
- Consolidation test report pair set (`v9-*` + `VoiceOS-V9-*`)

Keep one canonical naming style and archive duplicates once diff-verified.
