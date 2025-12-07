# MainAvanues - Project Instructions

This is the MainAvanues monorepo.

---

## Monorepo Structure

| Path | Purpose |
|------|---------|
| android/apps/ | Android applications |
| android/libs/ | Android libraries |
| common/ | Shared Kotlin Multiplatform |
| docs/ | Documentation |

---

## File Operations (MANDATORY)

**Always use `ideacode_fs` for folder/file operations:**

| Action | Command |
|--------|---------|
| Create folder | `ideacode_fs --action create --path "path" --type folder --purpose "desc"` |
| Create file | `ideacode_fs --action create --path "path" --type file` |
| Validate | `ideacode_fs --action validate --path "path"` |
| Search | `ideacode_fs --action search --query "term"` |
| Sync | `ideacode_fs --action sync` |

**Never use raw mkdir, Write tool, or mv without validation.**

---

## Session Start (Read These Files)

| File | Purpose |
|------|---------|
| FOLDER-REGISTRY.md | Master folder registry |
| PROJECT-INSTRUCTIONS.md | Centralized app instructions |
| `.ideacode/registries/script-registry.md` | Scripts registry |

When working on specific app/module:
- Read `android/apps/{app}/.claude/CLAUDE.md` if exists
- Read `android/libs/{lib}/.claude/CLAUDE.md` if exists

---

## Naming Conventions

### Document Naming

| Type | Pattern | Example |
|------|---------|---------|
| Documents | `MainAvanues-Module-Description-YDDMMHH-V#.md` | `MainAvanues-Core-Guide-5031215-V1.md` |
| Living Docs | `LD-MainAvanues-Module-Description-V#.md` | `LD-MainAvanues-Architecture-V1.md` |
| Specs | `MainAvanues-Spec-Feature-YDDMM-V#.md` | `MainAvanues-Spec-WebAvanue-50312-V1.md` |
| Plans | `MainAvanues-Plan-Feature-YDDMM-V#.md` | `MainAvanues-Plan-Migration-50312-V1.md` |

### Folder Naming

| Context | Convention | Example |
|---------|------------|---------|
| Gradle paths | lowercase | `android/`, `ios/`, `desktop/` |
| All other folders | PascalCase | `Docs/`, `Common/`, `Shared/` |
| No type prefixes | `Authentication/` | ~~feature-authentication/~~

---

## Additional MCP Tools

| Task | Tool |
|------|------|
| List modules | `ideacode_monorepo_list` |
| Validate structure | `ideacode_monorepo_validate` |
| Show dependencies | `ideacode_monorepo_deps` |

---

## Key Rules (Project-Specific)

| Rule | Requirement |
|------|-------------|
| No Delete | Never delete working features without approval + pros/cons |

---

## Inherited Rules

All rules from `/Volumes/M-Drive/Coding/.claude/CLAUDE.md` apply.

---

**Updated:** 2025-12-06 | **Version:** 10.3
