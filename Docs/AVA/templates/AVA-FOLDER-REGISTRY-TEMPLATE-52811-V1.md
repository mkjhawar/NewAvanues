# {PROJECT_NAME} Monorepo - Folder Registry

**Version:** 1.0.0
**Last Updated:** {DATE}
**Purpose:** Master registry of ALL folders in monorepo - STRICT enforcement to prevent duplication

---

## ⚠️ CRITICAL RULES

1. **ALWAYS check this registry before creating folders**
2. **NEVER create folder variations** (e.g., `voice-recognition/` vs `voice_recognition/`)
3. **Follow kebab-case naming** (lowercase-with-dashes)
4. **NO type prefixes** (use `authentication/` NOT `feature-authentication/`)
5. **Update this registry when adding folders**

---

## Root-Level Folders

| Folder | Purpose | Status | Naming Rule |
|--------|---------|--------|-------------|
| `FOLDER-REGISTRY.md` | Master folder registry | Required | UPPERCASE |
| `PROJECT-INSTRUCTIONS.md` | Centralized app/platform instructions | Required | UPPERCASE |
| `android/` | Android platform apps | Required | Platform name |
| `ios/` | iOS platform apps | Required | Platform name |
| `desktop/` | Desktop platform apps | Optional | Platform name |
| `common/` | KMP shared modules (no /libs) | Required | Shared code |
| `docs/` | All documentation | Required | Documentation root |
| `scripts/` | Build, migration, automation scripts | Required | Utility scripts |
| `.ideacode/` | IDEACODE framework | Required | Framework (hidden) |
| `.claude/` | Claude Code configuration | Required | AI config (hidden) |
| `build.gradle.kts` | Root Gradle build | Required | Build file |
| `README.md` | Project overview | Required | UPPERCASE |

---

## Platform Folders (android/, ios/, desktop/)

**Pattern:** `{platform}/{app-name}/`

| Folder | Purpose | Apps |
|--------|---------|------|
| `android/{APP1}/` | {APP1} Android app | {APP1_DESCRIPTION} |
| `android/{APP2}/` | {APP2} Android app | {APP2_DESCRIPTION} |
| `ios/{APP1}/` | {APP1} iOS app | {APP1_DESCRIPTION} |
| `ios/{APP2}/` | {APP2} iOS app | {APP2_DESCRIPTION} |
| `desktop/{APP1}/` | {APP1} Desktop app | {APP1_DESCRIPTION} |
| `desktop/{APP2}/` | {APP2} Desktop app | {APP2_DESCRIPTION} |

**Rules:**
- Use app name exactly as listed (lowercase, kebab-case if multi-word)
- NO variations: `{app}/` NOT `{APP}/`, `{app}-app/`, `{app}_app/`
- NO platform suffixes: `{app}/` NOT `{app}-android/`

---

## Common Folder (KMP Shared Modules)

**Pattern:** `common/{module-name}/`
**CRITICAL:** NO `/libs` subfolder - direct placement

| Folder | Purpose | Shared By |
|--------|---------|-----------|
| `common/database/` | SQLDelight unified database | All apps |
| `common/{MODULE1}/` | {MODULE1_DESCRIPTION} | {APPS_USING} |
| `common/{MODULE2}/` | {MODULE2_DESCRIPTION} | {APPS_USING} |

**Rules:**
- Use singular or natural name: `database/` NOT `databases/`
- Kebab-case for multi-word: `browser-core/` NOT `browserCore/`
- NO technology prefixes: `database/` NOT `sqldelight-database/`

---

## Documentation Folders

### App-Level Documentation

**Pattern:** `docs/{app-name}/{Master|Platform}/`

| Folder | Purpose | Content |
|--------|---------|---------|
| `docs/{APP1}/Master/` | {APP1} universal specs | Architecture, vision, flows, specs |
| `docs/{APP1}/Platform/android/` | {APP1} Android docs | Android-specific implementation |
| `docs/{APP1}/Platform/ios/` | {APP1} iOS docs | iOS-specific implementation |
| `docs/{APP1}/Platform/desktop/` | {APP1} Desktop docs | Desktop-specific implementation |
| `docs/{APP2}/Master/` | {APP2} universal specs | Architecture, vision, flows, specs |
| `docs/{APP2}/Platform/android/` | {APP2} Android docs | Android-specific implementation |
| `docs/{APP2}/Platform/ios/` | {APP2} iOS docs | iOS-specific implementation |

**Rules:**
- Use `Master/` NOT `Main/`, `master/`, `MASTER/`
- Use `Platform/` NOT `Platforms/`, `platform/`
- Platform subfolders: `android/`, `ios/`, `desktop/` (exact match)

### Framework Documentation

| Folder | Purpose | Content |
|--------|---------|---------|
| `docs/ideacode/specs/` | Feature specifications | All specs created by /specify |
| `docs/ideacode/protocols/` | IDEACODE protocols | Workflow rules, standards |
| `docs/ideacode/guides/` | Developer guides | How-to documentation |

### Supporting Documentation

| Folder | Purpose | Content |
|--------|---------|---------|
| `docs/migration/` | Monorepo migration docs | Migration plans, progress reports |
| `docs/archive/android/` | Archived Android docs | Obsolete Android documentation |
| `docs/archive/ios/` | Archived iOS docs | Obsolete iOS documentation |
| `docs/archive/desktop/` | Archived Desktop docs | Obsolete Desktop documentation |

---

## File Naming Conventions

### Documentation Files

| Type | Pattern | Example |
|------|---------|---------|
| Specifications | `{feature}-spec.md` | `voice-recognition-spec.md` |
| Plans | `{feature}-plan.md` | `database-consolidation-plan.md` |
| Tasks | `{feature}-tasks.md` | `monorepo-migration-tasks.md` |
| Architecture | `{component}-architecture.md` | `nlu-architecture.md` |
| Guides | `{topic}-guide.md` | `database-migration-guide.md` |
| Reports | `{topic}-report-YYYYMMDD.md` | `migration-progress-report-20251128.md` |
| Timestamped | `{name}-YYYYMMDDHHMM.md` | `adr-010-20251128.md` |

**Rules:**
- Use kebab-case: `voice-recognition.md` NOT `VoiceRecognition.md`
- NO type prefixes in filename: `authentication-spec.md` NOT `spec-authentication.md`
- Timestamps: YYYYMMDDHHMM format (10 digits, 24-hour time)
- Exceptions: `README.md`, `CHANGELOG.md`, `REGISTRY.md`, `CLAUDE.md` (UPPERCASE)

### Code Files

| Type | Pattern | Example |
|------|---------|---------|
| Kotlin files | `PascalCase.kt` | `VoiceRecognitionManager.kt` |
| Gradle files | `kebab-case.gradle.kts` | `build.gradle.kts` |
| Config files | `kebab-case.yml` | `config.yml` |

---

## Enforcement Checklist

Before creating a folder, check:

1. ✅ Does it already exist in this registry?
2. ✅ Is the name kebab-case (lowercase-with-dashes)?
3. ✅ Does it follow the pattern for its location?
4. ✅ Is it NOT a variation of an existing folder?
5. ✅ Have I updated this registry after creation?

If ALL checks pass, proceed. Otherwise, use existing folder or ask user.

---

## Validation Command

```bash
# Check if folder exists in registry before creation
ideacode_validate_folder "{folder_path}"

# Returns:
# ✅ Folder matches registry
# ⚠️ Folder is variation of {existing_folder}
# ❌ Folder violates naming convention
# ℹ️ Folder not in registry - add after creation
```

---

## Update History

| Date | Change | Author |
|------|--------|--------|
| {DATE} | Initial registry creation | {AUTHOR} |

---

**CRITICAL:** This registry is the single source of truth for folder structure.
**NEVER** create folders without checking this registry first.
