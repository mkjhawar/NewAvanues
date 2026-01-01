# NewAvanues - Project Instructions

NewAvanues - Platform-centric KMP monorepo (AVA, VoiceOS, WebAvanue, Cockpit, NLU)

---

## MANDATORY: Before Any File Operation

1. **READ MASTER INDEX:** `/Volumes/M-Drive/Coding/.ideacode/MASTER-INDEX.md` (single source of truth)
2. **READ REGISTRIES:**
   - `.ideacode/Registries/FOLDER-REGISTRY.md` - Folder structure
   - `.ideacode/Registries/Modules.registry.json` - Module paths
   - `.ideacode/Registries/Docs.registry.json` - Doc paths

3. **Use correct case:**
   - Gradle paths: `lowercase` (android/, ios/, desktop/)
   - All other folders: `PascalCase` (Common/, Docs/, Shared/)

---

## KMP FOLDER CONVENTIONS (Zero Tolerance)

**Gradle KMP plugin requires exact names - CANNOT be changed.**

| Source Set | Purpose |
|------------|---------|
| `commonMain/` | Shared code |
| `commonTest/` | Shared tests |
| `androidMain/` | Android implementation |
| `androidUnitTest/` | Android unit tests |
| `androidInstrumentedTest/` | Android instrumented tests |
| `iosMain/` | iOS implementation |
| `iosTest/` | iOS tests |
| `desktopMain/` | Desktop/JVM implementation |
| `desktopTest/` | Desktop tests |

**Forbidden Patterns:**
| Pattern | Issue |
|---------|-------|
| `common/classes/` | Redundant nesting |
| `utils/helpers/` | Too deep |
| Custom test folder names | Breaks Gradle |
| Renaming source sets | Breaks Gradle |

See: `/Volumes/M-Drive/Coding/.ideacode/MASTER-INDEX.md` Section 3

---

## PROJECT STRUCTURE

| Path | Purpose | Case |
|------|---------|------|
| `android/` | Android apps | lowercase (Gradle) |
| `ios/` | iOS apps | lowercase (Gradle) |
| `desktop/` | Desktop apps | lowercase (Gradle) |
| `Common/` | KMP shared libraries | PascalCase |
| `Modules/` | Feature modules | PascalCase |
| `Modules/AVA/` | AI assistant platform | PascalCase |
| `Modules/VoiceOS/` | Voice-first accessibility | PascalCase |
| `Modules/WebAvanue/` | Web platform (Tauri + React) | PascalCase |
| `Modules/Cockpit/` | Management dashboard | PascalCase |
| `Modules/NLU/` | Natural language understanding | PascalCase |
| `Shared/` | Assets, configs | PascalCase |
| `Docs/` | Documentation | PascalCase |

---

## BUILD REQUIREMENTS

| Requirement | Version | Notes |
|-------------|---------|-------|
| JDK | 17 | Required (JDK 24 incompatible with Gradle) |
| Gradle | 8.x | Via wrapper |
| Android SDK | 34 | Target API |

---

## PROJECT-SPECIFIC TECH

| Component | Technology |
|-----------|------------|
| Build | Gradle composite builds + version catalogs |
| Shared logic | KMP (Kotlin Multiplatform) |
| Database | SQLDelight (NOT Room) |
| DI | Koin |
| Web | Tauri + React + TypeScript |
| Language Server | Kotlin LSP (for DSL/tooling) |
| Web Components | React + TypeScript (embeddable UI) |
| IDE Plugins | VS Code (TypeScript) + IntelliJ (Kotlin + JCEF) |

---

## LSP ARCHITECTURE (AVAMagic Tooling)

**Layer 1: Core Language Server (Kotlin)**
- DSL parsing, validation, code generation
- Implements: Language Server Protocol (LSP)
- Size: ~25MB (shared across all tools)
- Reuses: ThemeCompiler.kt, VUIDCreator, existing MagicUI logic

**Layer 2: Web UI Components (React + TypeScript)**
- Theme color picker, property inspector, live preview
- Monaco Editor integration (VS Code's editor)
- Size: ~5MB (cached, embeddable everywhere)

**Layer 3: Platform Integrations**
- VS Code Extension: TypeScript + LSP client (~2MB)
- IntelliJ Plugin: Kotlin + JCEF browser (~3MB)
- Standalone Apps: Compose Desktop + CEF OR Tauri v2

**Total Ecosystem:** ~45MB (90% code reuse across all tools)

---

## Apps

| App | Platforms | Android Path | Docs Path |
|-----|-----------|--------------|-----------|
| VoiceOS | android, ios, desktop | `android/voiceos/` | `Docs/VoiceOS/` |
| AVA | android, ios, macos, win, linux, web | `android/ava/` | `Docs/AVA/` |
| Avanues | android | `android/avanues/` | `Docs/Avanues/` |
| AvaConnect | android, web | `android/avaconnect/` | `Docs/AvaConnect/` |

---

## Common Libraries

| Library | Path | Description |
|---------|------|-------------|
| Database | `Common/Database/` | SQLDelight |
| Voice | `Common/Voice/` | Voice recognition |
| NLU | `Common/NLU/` | NLU engine |
| UI | `Common/UI/` | MagicUI |
| Utils | `Common/Utils/` | Utilities |

---

## DEVELOPMENT BRANCHES

| Branch | Purpose |
|--------|---------|
| Avanues-Main | Main integration |
| AVA-Development | AVA features |
| VoiceOS-Development | VoiceOS features |
| WebAvanue-Development | WebAvanue features |
| Cockpit-Development | Cockpit features |
| NLU-Development | NLU features |

---

## WORKTREES

| Worktree | Path | Branch |
|----------|------|--------|
| NewAvanues-AVA | /Volumes/M-Drive/Coding/NewAvanues-AVA | AVA-Development |
| NewAvanues-VoiceOS | /Volumes/M-Drive/Coding/NewAvanues-VoiceOS | VoiceOS-Development |
| NewAvanues-WebAvanue | /Volumes/M-Drive/Coding/NewAvanues-WebAvanue | WebAvanue-Development |
| NewAvanues-Cockpit | /Volumes/M-Drive/Coding/NewAvanues-Cockpit | Cockpit-Development |
| NewAvanues-NLU | /Volumes/M-Drive/Coding/NewAvanues-NLU | NLU-Development |

---

## Documentation Structure

| Path | Purpose |
|------|---------|
| `Docs/Project/` | Living docs (LD-*.md) |
| `Docs/{App}/MasterSpecs/` | Universal specs |
| `Docs/{App}/Platform/{Platform}/` | Platform-specific |
| `Docs/Common/{Domain}/` | Shared lib docs |
| `Docs/{Module}/LivingDocs/LD-*.md` | Living Docs |
| `.ideacode/registries/` | Registries |

---

## Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Documents | App-Module-Description-YDDMMHH-V#.md | `VoiceOS-NLU-Integration-5031215-V1.md` |
| Living Docs | LD-App-Module-Description-V#.md | `LD-VoiceOS-Architecture-V1.md` |
| Specs | App-Spec-Feature-YDDMM-V#.md | `AVA-Spec-VoiceCommands-50312-V1.md` |
| Plans | App-Plan-Feature-YDDMM-V#.md | `AVA-Plan-Migration-50312-V1.md` |

### Date Formats

| Format | Use | Parts |
|--------|-----|-------|
| YDDMMHH | Documents | Y + DD + MM + HH |
| YDDMM | Specs, Plans | Y + DD + MM |

Y = last digit of year (5 for 2025), DD = day, MM = month, HH = hour

---

## Forbidden Actions

| Action | Why |
|--------|-----|
| Create folder not in registry | Causes sprawl |
| Use lowercase for non-Gradle paths | Inconsistent |
| Use PascalCase for Gradle paths | Gradle breaks |
| Put docs in wrong App/Platform folder | Hard to find |
| Skip MasterSpecs | Missing universal docs |
| Create file without checking registry | May duplicate |

---

## Key Rules

| Rule | Requirement |
|------|-------------|
| Registry First | ALWAYS check registries before creating files |
| Clean Architecture | SOLID principles, no legacy code |
| Cross-Platform | Design for all platforms from start |
| No Delete | Never delete without approval + pros/cons |
| Database | SQLDelight ONLY (never Room) |
| Module work | Check module-level .claude/CLAUDE.md first |
| Cross-module | Check CROSS-MODULE-DEPENDENCIES.md |
| Worktrees | NEVER switch worktrees without approval |

---

## Git Index Lock Workaround

When encountering `fatal: unable to write new index file` (common with large repos, multiple terminals, or slow drives), bypass using temp index + plumbing commands:

### Quick Reference

```bash
# 1. Create temp index from HEAD
GIT_INDEX_FILE=/tmp/git-index-temp git read-tree HEAD

# 2. Stage files to temp index
GIT_INDEX_FILE=/tmp/git-index-temp git add path/to/files

# 3. Write tree (capture SHA)
TREE=$(GIT_INDEX_FILE=/tmp/git-index-temp git write-tree)

# 4. Create commit
COMMIT=$(git commit-tree $TREE -p HEAD -m "message")

# 5. Update branch ref
git update-ref refs/heads/BRANCH_NAME $COMMIT

# 6. Push
git push origin BRANCH_NAME
```

### For Merges (non-fast-forward)

```bash
git fetch origin TARGET_BRANCH
MERGE_TREE=$(git merge-tree origin/TARGET_BRANCH $COMMIT)
MERGE=$(git commit-tree $MERGE_TREE -p origin/TARGET_BRANCH -p $COMMIT -m "merge msg")
git update-ref refs/heads/TARGET_BRANCH $MERGE
git push origin TARGET_BRANCH
```

### One-Liner

```bash
GIT_INDEX_FILE=/tmp/idx git read-tree HEAD && \
GIT_INDEX_FILE=/tmp/idx git add FILE1 FILE2 && \
TREE=$(GIT_INDEX_FILE=/tmp/idx git write-tree) && \
COMMIT=$(git commit-tree $TREE -p HEAD -m "msg") && \
git update-ref refs/heads/BRANCH $COMMIT && \
git push origin BRANCH
```

---

## Inherited Rules

All rules from `/Volumes/M-Drive/Coding/.claude/CLAUDE.md` apply.

---

**Updated:** 2025-12-30 | **Version:** 12.4.1
