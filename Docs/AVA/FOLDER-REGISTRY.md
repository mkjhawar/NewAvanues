# MainAvanues Monorepo - Folder Registry

**Version:** 1.0.0
**Last Updated:** 2025-11-28
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
| `REGISTRY.md` | Master folder registry | Required | UPPERCASE |
| `PROJECT-INSTRUCTIONS.md` | Centralized app/platform instructions | Required | UPPERCASE |
| `android/` | Android platform apps | Required | Platform name |
| `ios/` | iOS platform apps | Required | Platform name |
| `desktop/` | Desktop platform apps | Required | Platform name |
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
| `android/voiceos/` | VoiceOS Android app | Voice OS for Android |
| `android/ava/` | AVA Android app | AI assistant for Android |
| `android/webavanue/` | WebAvanue Android app | Browser for Android |
| `android/avaconnect/` | AVAConnect Android app | Connectivity for Android |
| `ios/voiceos/` | VoiceOS iOS app | Voice OS for iOS |
| `ios/ava/` | AVA iOS app | AI assistant for iOS |
| `ios/webavanue/` | WebAvanue iOS app | Browser for iOS |
| `desktop/voiceos/` | VoiceOS Desktop app | Voice OS for Desktop |
| `desktop/ava/` | AVA Desktop app | AI assistant for Desktop |
| `desktop/webavanue/` | WebAvanue Desktop app | Browser for Desktop |

**Rules:**
- Use app name exactly as listed (lowercase, kebab-case if multi-word)
- NO variations: `ava/` NOT `AVA/`, `ava-app/`, `ava_app/`
- NO platform suffixes: `voiceos/` NOT `voiceos-android/`

---

## Common Folder (KMP Shared Modules)

**Pattern:** `common/{module-name}/`
**CRITICAL:** NO `/libs` subfolder - direct placement

| Folder | Purpose | Shared By |
|--------|---------|-----------|
| `common/database/` | SQLDelight unified database | All apps (post-consolidation) |
| `common/voice/` | Voice recognition/synthesis | VoiceOS, AVA |
| `common/accessibility/` | Accessibility features | VoiceOS, AVAConnect |
| `common/nlu/` | Natural Language Understanding | AVA, VoiceOS |
| `common/llm/` | Large Language Model integration | AVA |
| `common/rag/` | Retrieval-Augmented Generation | AVA |
| `common/cloud/` | Cloud sync services | All apps |
| `common/ipc/` | Inter-Process Communication | VoiceOS, AVAConnect |
| `common/webview/` | WebView components | WebAvanue |
| `common/browser-core/` | Browser engine | WebAvanue |
| `common/speech/` | Speech processing | VoiceOS, AVA |
| `common/plugin/` | Plugin system | VoiceOS, AVA |

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
| `docs/voiceos/Master/` | VoiceOS universal specs | Architecture, vision, flows, specs |
| `docs/voiceos/Platform/android/` | VoiceOS Android docs | Android-specific implementation |
| `docs/voiceos/Platform/ios/` | VoiceOS iOS docs | iOS-specific implementation |
| `docs/voiceos/Platform/desktop/` | VoiceOS Desktop docs | Desktop-specific implementation |
| `docs/ava/Master/` | AVA universal specs | Architecture, vision, flows, specs |
| `docs/ava/Platform/android/` | AVA Android docs | Android-specific implementation |
| `docs/ava/Platform/ios/` | AVA iOS docs | iOS-specific implementation |
| `docs/ava/Platform/desktop/` | AVA Desktop docs | Desktop-specific implementation |
| `docs/webavanue/Master/` | WebAvanue universal specs | Architecture, vision, flows, specs |
| `docs/webavanue/Platform/android/` | WebAvanue Android docs | Android-specific implementation |
| `docs/webavanue/Platform/ios/` | WebAvanue iOS docs | iOS-specific implementation |
| `docs/webavanue/Platform/desktop/` | WebAvanue Desktop docs | Desktop-specific implementation |
| `docs/avaconnect/Master/` | AVAConnect universal specs | Architecture, vision, flows, specs |
| `docs/avaconnect/Platform/android/` | AVAConnect Android docs | Android-specific implementation |
| `docs/avaconnect/Platform/ios/` | AVAConnect iOS docs | iOS-specific implementation |

**Rules:**
- Use `Master/` NOT `Main/`, `master/`, `MASTER/`
- Use `Platform/` NOT `Platforms/`, `platform/`
- Platform subfolders: `android/`, `ios/`, `desktop/` (exact match)

### Framework Documentation

**Pattern:** `docs/ideacode/{category}/`

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

**Rules:**
- Archive by platform: `docs/archive/{platform}/`
- Use platform name exactly: `android/` NOT `Android/`

---

## Scripts Folders

**Pattern:** `scripts/{category}/`

| Folder | Purpose | Content |
|--------|---------|---------|
| `scripts/migration/` | Migration scripts | Repo consolidation, data migration |
| `scripts/build/` | Build automation | CI/CD, build helpers |

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
| Timestamped | `{name}-YYYYMMDDHHMM.md` | `adr-010-room-sqldelight-20251128.md` |

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

## Enforcement Checklist (Use Before Creating Folders)

```
Before creating a folder, check:

1. ✅ Does it already exist in this registry?
2. ✅ Is the name kebab-case (lowercase-with-dashes)?
3. ✅ Does it follow the pattern for its location?
4. ✅ Is it NOT a variation of an existing folder?
5. ✅ Have I updated this registry after creation?

If ALL checks pass, proceed. Otherwise, use existing folder or ask user.
```

---

## Validation Command (For ideacode-mcp)

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
| 2025-11-28 | Initial registry creation | System |

---

**CRITICAL:** This registry is the single source of truth for folder structure.
**NEVER** create folders without checking this registry first.
