# IDEACODE Master Index

**Single source of truth for all conventions, file locations, and rules.**

Read this file at session start. All paths relative to `/Volumes/M-Drive/Coding/`.

---

## Quick Reference

| What | Where |
|------|-------|
| Zero Tolerance Rules | [Section 2](#2-zero-tolerance-rules) |
| KMP Folder Conventions | [Section 3](#3-kmp-folder-conventions) |
| File Naming | [Section 4](#4-file-naming-conventions) |
| Validation Rules | [Section 5](#5-validation-rules) |
| Refactor Rules | [Section 6](#6-refactor-rules) |

---

## 1. File Locations

| File | Path | Purpose |
|------|------|---------|
| Master Index | `.ideacode/MASTER-INDEX.md` | This file |
| Global Config | `.ideacode/config.idc` | Framework settings |
| Global Instructions | `.claude/CLAUDE.md` | AI instructions |
| Project Config | `{project}/.ideacode/config.idc` | Project settings |
| Project Instructions | `{project}/.claude/CLAUDE.md` | Project AI instructions |

---

## 2. Zero Tolerance Rules

**MANDATORY. No exceptions.**

| # | Rule | Enforcement |
|---|------|-------------|
| ZT-1 | Read master index at session start | Block |
| ZT-2 | No delete without approval | Block |
| ZT-3 | No commits to main/master | Block |
| ZT-4 | No hallucination | Warn |
| ZT-5 | Wait for user on destructive ops | Block |
| ZT-6 | No stubs - complete only | Block |
| ZT-7 | Source grounding - cite file:line | Warn |
| ZT-8 | Save analysis to docs | Block |
| ZT-9 | Code proximity in plans | Block |
| ZT-10 | Use KMP folder names exactly | Block |
| ZT-11 | No custom folder suffixes | Block |
| ZT-12 | Check registries before creating | Block |

---

## 3. KMP Folder Conventions

**Gradle requirements - cannot change.**

### 3.1 Required Source Set Names

```
src/
├── commonMain/              # Shared code
├── commonTest/              # Shared tests
├── androidMain/             # Android impl
├── androidUnitTest/         # Android unit tests
├── androidInstrumentedTest/ # Android instrumented
├── iosMain/                 # iOS impl
├── iosTest/                 # iOS tests
├── desktopMain/             # Desktop/JVM impl
├── desktopTest/             # Desktop tests
```

### 3.2 Rationale

| Suffix | Why Required |
|--------|--------------|
| `*Main` | Gradle KMP plugin |
| `*Test` | Gradle test plugin |
| `android*` | Android Gradle plugin |

### 3.3 Our Control (Package Structure)

```
commonMain/kotlin/com/augmentalis/{module}/
├── common/      # Shared classes
├── functions/   # Utilities
├── handlers/    # Handlers
├── features/    # Features
```

### 3.4 Forbidden

| Pattern | Issue |
|---------|-------|
| `common/classes/` | Redundant |
| `utils/helpers/` | Too deep |
| `models/data/` | Redundant |
| Custom test folders | Breaks Gradle |

---

## 4. File Naming Conventions

### Documents

| Type | Pattern |
|------|---------|
| Living Docs | `LD-{App}-{Module}-V#.md` |
| Specs | `{App}-Spec-{Feature}-YDDMM-V#.md` |
| Plans | `{App}-Plan-{Feature}-YDDMM-V#.md` |

### Code

| Type | Pattern |
|------|---------|
| Kotlin | `{PascalCase}.kt` |
| Test | `{PascalCase}Test.kt` |
| Config | `{kebab-case}.idc` |

### Paths

| Type | Case |
|------|------|
| Gradle (android/, ios/) | lowercase |
| Modules, Docs, Common | PascalCase |

---

## 5. Validation Rules

**Apply during code review and before commit.**

### 5.1 Folder Structure Validation

| Check | Rule | Action |
|-------|------|--------|
| KMP source sets | Must use exact Gradle names | Reject |
| Package depth | Max 4 levels | Warn |
| Redundant folders | No `classes/`, `helpers/` | Reject |
| Registry check | File must be in registry | Warn |

### 5.2 Code Validation

| Check | Rule | Action |
|-------|------|--------|
| No stubs | Complete implementation | Reject |
| No TODOs in prod | Remove or create issue | Warn |
| Test coverage | 90%+ | Warn |
| SOLID | No violations | Reject |

### 5.3 Validation Commands

```bash
# Folder structure
./gradlew validateFolderStructure

# Code quality
./gradlew detekt spotlessCheck

# Tests
./gradlew test
```

---

## 6. Refactor Rules

**Apply during /i.refactor operations.**

### 6.1 Folder Refactoring

| Action | Rule |
|--------|------|
| Rename source set | FORBIDDEN - breaks Gradle |
| Move between source sets | Allowed with expect/actual |
| Flatten packages | Encouraged if > 4 levels |
| Merge packages | Allowed if related |

### 6.2 Code Refactoring

| Action | Rule |
|--------|------|
| Extract class | Must be in same source set |
| Extract function | Prefer `functions/` package |
| Move to common | Add expect/actual if platform-specific |
| Rename | Update all references + tests |

### 6.3 Forbidden Refactors

| Action | Why |
|--------|-----|
| Custom source set names | Breaks Gradle |
| Deep nesting | Adds complexity |
| Circular dependencies | Architecture violation |

---

## 7. Tech Stack

| Component | Technology |
|-----------|------------|
| Multiplatform | Kotlin Multiplatform |
| Database | SQLDelight |
| DI | Koin |
| Serialization | kotlinx.serialization |
| Android UI | Compose + Material3 |
| iOS UI | SwiftUI |
| Desktop UI | Compose Desktop |
| Web | React + TypeScript |

---

## 8. Quality Gates

| Gate | Threshold |
|------|-----------|
| Test Coverage | 90%+ |
| SOLID Violations | 0 |
| Security Issues | 0 |

---

## Version

| Field | Value |
|-------|-------|
| Version | 1.0.0 |
| Updated | 2025-12-31 |
