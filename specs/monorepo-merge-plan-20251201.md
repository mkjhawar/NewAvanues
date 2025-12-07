# NewAvanues Monorepo Merge Plan

**Date:** 2025-12-01
**Status:** PLAN ONLY - DO NOT EXECUTE WITHOUT APPROVAL
**Target Repo:** `/Volumes/M-Drive/Coding/NewAvanues`

---

## Executive Summary

Merge 5 source repositories into unified NewAvanues monorepo:
1. AVA AI (`/Volumes/M-Drive/Coding/AVA`)
2. VoiceOS (`/Volumes/M-Drive/Coding/VoiceOS`)
3. Avanues (`/Volumes/M-Drive/Coding/Avanues`)
4. WebAvanue (`/Volumes/M-Drive/Coding/MainAvanues/Modules/WebAvanue` + `/Volumes/M-Drive/Coding/WebAvanue`)
5. AvaConnect (`/Volumes/M-Drive/Coding/AvaConnect`)

**Note:** MainAvanues is an existing master monorepo - use its structure as reference. WebAvanue is a module within it.

---

## Target Folder Structure

**Reference:** `/Volumes/M-Drive/Coding/ideacode/docs/MONOREPO-STRUCTURE.md`

```
NewAvanues/
├── .claude/                          # Claude Code configuration
│   ├── commands/                     # Project-specific slash commands
│   ├── lib/                          # Shared algorithms/utilities
│   └── templates/                    # Code templates
├── .ideacode/                        # IDEACODE configuration
│   ├── config.yml                    # profile: monorepo (EXISTS)
│   ├── context/                      # AI context
│   ├── living-docs/
│   │   └── LD-modules.md             # CREATE: Module documentation
│   └── registries/
│       └── modules.registry.json     # CREATE: Module registry
├── android/                          # Android-only code
│   ├── apps/                         # Android app modules
│   │   ├── ava/                      # AVA AI Android app
│   │   ├── voiceos/                  # VoiceOS Android app
│   │   ├── avanues/                  # Avanues Android app
│   │   └── avaconnect/               # AvaConnect Android app
│   └── modules/                      # Android-only modules
│       ├── voice-engine/             # VoiceOS voice engine
│       ├── accessibility-core/       # VoiceOS accessibility
│       └── command-manager/          # VoiceOS CommandManager
├── common/                           # Shared KMP libraries
│   └── libs/
│       ├── voice/                    # Voice domain
│       │   ├── feature-nlu/          # AVA NLU engine (CRITICAL)
│       │   ├── feature-recognition/  # Voice recognition
│       │   ├── feature-commands/     # Command system
│       │   └── data-access-intents/  # Intent database
│       ├── database/                 # Database domain
│       │   ├── data-access-sqldelight/ # Unified SQLDelight DB
│       │   └── data-access-uuid/     # UUID database
│       ├── ui/                       # UI domain
│       │   ├── ui-magic/             # MagicUI components
│       │   └── ui-voice/             # Voice UI elements
│       └── utils/                    # Utilities domain
│           ├── util-logger/          # VoiceOS logging
│           └── util-uuid/            # UUID generation
├── Modules/                          # Feature modules (platform)
│   ├── WebAvanue/                    # WebAvanue feature module
│   │   ├── Android/
│   │   ├── iOS/
│   │   ├── app/
│   │   ├── universal/                # KMP shared code
│   │   └── docs/
│   └── AvaConnect/                   # AvaConnect feature module
│       ├── Android/
│       ├── app/
│       ├── universal/
│       └── docs/
├── packages/                         # Standalone packages
├── shared/                           # Shared assets/configs
│   ├── assets/
│   │   └── ml-models/                # AVA ML models
│   └── configs/
│       ├── intents/                  # AVA .ava intent files
│       └── commands/                 # VoiceOS .vos command files
├── docs/                             # Repository documentation
│   ├── ava/
│   ├── voiceos/
│   ├── avanues/
│   ├── webavanue/
│   ├── avaconnect/
│   ├── shared-libs/
│   └── architecture/
├── specs/                            # (THIS FILE)
├── scripts/                          # Build/utility scripts
├── protocols/                        # Development protocols
└── contextsave/                      # Context saves
```

### Library Naming Prefixes (MANDATORY)

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature-` | Business logic | `feature-nlu`, `feature-recognition` |
| `data-access-` | Data sources | `data-access-sqldelight`, `data-access-intents` |
| `ui-` | UI components | `ui-magic`, `ui-voice` |
| `util-` | Utilities | `util-logger`, `util-uuid` |

---

## Phase 1: Create Target Structure

### Step 1.1: Create Directory Skeleton

```bash
# Run from /Volumes/M-Drive/Coding/NewAvanues

# Android apps and modules
mkdir -p android/apps/{ava,voiceos,avanues,avaconnect}
mkdir -p android/modules/{voice-engine,accessibility-core,command-manager}

# Common KMP libraries (with required prefixes)
mkdir -p common/libs/voice/{feature-nlu,feature-recognition,feature-commands,data-access-intents}
mkdir -p common/libs/database/{data-access-sqldelight,data-access-uuid}
mkdir -p common/libs/ui/{ui-magic,ui-voice}
mkdir -p common/libs/utils/{util-logger,util-uuid}

# Feature modules (PascalCase)
mkdir -p Modules/WebAvanue/{Android,iOS,app,universal,docs}
mkdir -p Modules/AvaConnect/{Android,app,universal,docs}

# Shared assets/configs
mkdir -p shared/assets/ml-models
mkdir -p shared/configs/{intents,commands}

# Documentation
mkdir -p docs/{ava,voiceos,avanues,webavanue,avaconnect,shared-libs,architecture}

# IDEACODE directories
mkdir -p .ideacode/{context,living-docs,registries}
```

### Step 1.2: Create modules.registry.json

```json
{
  "version": "1.0",
  "lastUpdated": "2025-12-01",
  "modules": []
}
```

Location: `.ideacode/registries/modules.registry.json`

---

## Phase 2: Migrate AVA AI

### Source: `/Volumes/M-Drive/Coding/AVA`

| Source Path | Target Path | Type |
|-------------|-------------|------|
| `app/` | `android/apps/ava/` | Android app |
| `apps/` | `android/apps/ava/` | Merge with above |
| `Universal/AVA/Features/NLU/` | `common/libs/voice/feature-nlu/` | CRITICAL: NLU engine |
| `ava-ai-models-external/` | `shared/assets/ml-models/` | ML models |
| `docs/` | `docs/ava/` | Documentation |

### Files to EXCLUDE (do not copy):
- `archive/`, `archive_docs/`
- `build/`, `.gradle/`
- `*.md` in root (except README.md)
- `contextsave/`
- `.ideacode/`, `.claude/`

### Registry Entry:
```json
{
  "id": "ava-android",
  "name": "AVA",
  "type": "app",
  "profile": "android-app",
  "platforms": ["android"],
  "path": "android/apps/ava",
  "created": "2025-12-01",
  "status": "development",
  "description": "AVA AI Android app with NLU",
  "dependencies": ["feature-nlu"],
  "has_ideacode": true,
  "source": "/Volumes/M-Drive/Coding/AVA"
}
```

---

## Phase 3: Migrate VoiceOS

### Source: `/Volumes/M-Drive/Coding/VoiceOS`

| Source Path | Target Path | Type |
|-------------|-------------|------|
| `modules/apps/VoiceOSCore/` | `android/apps/voiceos/` | Main app |
| `modules/managers/CommandManager/` | `android/modules/command-manager/` | Command routing (Android-only) |
| `modules/libraries/UUIDCreator/` | `common/libs/utils/util-uuid/` | UUID library (KMP) |
| `modules/libraries/VoiceOsLogger/` | `common/libs/utils/util-logger/` | Logging (KMP) |
| `modules/libraries/MagicUI/` | `common/libs/ui/ui-magic/` | UI components (KMP) |
| `modules/libraries/VoiceUIElements/` | `common/libs/ui/ui-voice/` | Voice UI (KMP) |
| `libraries/core/database/` | `common/libs/database/data-access-sqldelight/` | SQLDelight DB (KMP) |
| `modules/apps/VoiceOSCore/.../scraping/` | `common/libs/voice/feature-recognition/` | Voice/element recognition |
| `docs/` | `docs/voiceos/` | Documentation |

### Files to EXCLUDE:
- `build/`, `.gradle/`
- `contextsave/`
- `.ideacode/`, `.claude/`
- `ideadev/`
- `*.backup`, `*.disabled`

### Registry Entries:
```json
[
  {
    "id": "voiceos-android",
    "name": "VoiceOS",
    "type": "app",
    "profile": "android-app",
    "platforms": ["android"],
    "path": "android/apps/voiceos",
    "created": "2025-12-01",
    "status": "development",
    "description": "VoiceOS accessibility service Android app",
    "dependencies": ["command-manager", "data-access-sqldelight", "util-uuid"],
    "has_ideacode": true
  },
  {
    "id": "command-manager",
    "name": "CommandManager",
    "type": "module",
    "profile": "android-library",
    "platforms": ["android"],
    "path": "android/modules/command-manager",
    "created": "2025-12-01",
    "status": "development",
    "description": "Voice command routing and execution"
  },
  {
    "id": "data-access-sqldelight",
    "name": "SQLDelightDatabase",
    "type": "package",
    "profile": "kmp-library",
    "platforms": ["android", "ios", "desktop"],
    "path": "common/libs/database/data-access-sqldelight",
    "created": "2025-12-01",
    "status": "development",
    "description": "Unified SQLDelight database layer"
  }
]
```

---

## Phase 4: Migrate Avanues

### Source: `/Volumes/M-Drive/Coding/Avanues`

| Source Path | Target Path | Type |
|-------------|-------------|------|
| `android/` | `android/apps/avanues/` | Android app |
| `apps/` | `android/apps/avanues/` | Merge |
| `browser-commands/` | `shared/configs/commands/browser/` | Browser commands |
| `docs/` | `docs/avanues/` | Documentation |

### Files to EXCLUDE:
- `archive/`, `*.backup.*`
- `build/`, `.gradle/`
- `contextsave/`
- Root markdown files (status/context)

### Registry Entry:
```json
{
  "id": "avanues-android",
  "name": "Avanues",
  "type": "app",
  "profile": "android-app",
  "platforms": ["android"],
  "path": "android/apps/avanues",
  "created": "2025-12-01",
  "status": "development",
  "description": "Avanues Android app"
}
```

---

## Phase 5: Migrate WebAvanue

**Note:** MainAvanues is the master monorepo (reference only). WebAvanue is the actual module to migrate.

### Source 1: `/Volumes/M-Drive/Coding/MainAvanues/Modules/WebAvanue`
(Primary - module within master monorepo)

### Source 2: `/Volumes/M-Drive/Coding/WebAvanue`
(Standalone copy - may have different/newer code)

**Action:** Compare both sources, use newer/more complete version

| Source Path | Target Path | Type |
|-------------|-------------|------|
| `Android/` | `Modules/WebAvanue/Android/` | Android platform |
| `iOS/` | `Modules/WebAvanue/iOS/` | iOS platform |
| `app/` | `Modules/WebAvanue/app/` | Module entry point |
| `universal/` | `Modules/WebAvanue/universal/` | KMP shared code |
| `Desktop/` | `Modules/WebAvanue/Desktop/` | Desktop platform |
| `browser-plugin/` | `Modules/WebAvanue/browser-plugin/` | Browser plugin |
| `BrowserCoreData/` | `common/libs/database/data-access-browser/` | Shared browser data (KMP) |
| `docs/` | `Modules/WebAvanue/docs/` | Module docs |

### Reference from MainAvanues (structure only, not code):
- `build.gradle.kts` - Use as template for root build
- `settings.gradle.kts` - Use as template for module includes
- `.ideacode/` - Use config patterns

### Registry Entry:
```json
{
  "id": "webavanue",
  "name": "WebAvanue",
  "type": "module",
  "profile": "feature-module",
  "platforms": ["android", "ios", "desktop"],
  "path": "Modules/WebAvanue",
  "created": "2025-12-01",
  "status": "development",
  "description": "WebAvanue browser feature module",
  "dependencies": ["data-access-browser"],
  "has_ideacode": true
}
```

---

## Phase 6: Migrate AvaConnect

### Source: `/Volumes/M-Drive/Coding/AvaConnect`

| Source Path | Target Path | Type |
|-------------|-------------|------|
| `android/` | `Modules/AvaConnect/Android/` | Android platform |
| `web/` or `webui/` | `Modules/AvaConnect/web/` | Web UI |
| Shared connectivity code | `Modules/AvaConnect/universal/` | KMP shared (WiFi-Direct, WebRTC) |
| `docs/` | `Modules/AvaConnect/docs/` | Module documentation |

### Files to EXCLUDE:
- `archive/`, `backlog/`
- `build/`
- Root markdown files (*.md except README)

### Registry Entry:
```json
{
  "id": "avaconnect",
  "name": "AvaConnect",
  "type": "module",
  "profile": "feature-module",
  "platforms": ["android", "web"],
  "path": "Modules/AvaConnect",
  "created": "2025-12-01",
  "status": "development",
  "description": "AvaConnect WiFi-Direct/WebRTC connectivity module",
  "has_ideacode": true
}
```

---

## Phase 7: Create Unified Build System

### Step 7.1: Root build.gradle.kts
```kotlin
// NewAvanues/build.gradle.kts
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.sqldelight) apply false
}
```

### Step 7.2: settings.gradle.kts
```kotlin
rootProject.name = "NewAvanues"

// Android apps
include(":android:apps:ava")
include(":android:apps:voiceos")
include(":android:apps:avanues")
include(":android:apps:avaconnect")

// Android-only modules
include(":android:modules:command-manager")
include(":android:modules:voice-engine")
include(":android:modules:accessibility-core")

// KMP libraries - Voice domain
include(":common:libs:voice:feature-nlu")
include(":common:libs:voice:feature-recognition")
include(":common:libs:voice:feature-commands")
include(":common:libs:voice:data-access-intents")

// KMP libraries - Database domain
include(":common:libs:database:data-access-sqldelight")
include(":common:libs:database:data-access-uuid")
include(":common:libs:database:data-access-browser")

// KMP libraries - UI domain
include(":common:libs:ui:ui-magic")
include(":common:libs:ui:ui-voice")

// KMP libraries - Utils domain
include(":common:libs:utils:util-logger")
include(":common:libs:utils:util-uuid")

// Feature modules
include(":Modules:WebAvanue")
include(":Modules:WebAvanue:app")
include(":Modules:WebAvanue:universal")
include(":Modules:AvaConnect")
include(":Modules:AvaConnect:app")
include(":Modules:AvaConnect:universal")
```

---

## Phase 8: Update Registry & Living Docs

### Step 8.1: Final modules.registry.json

After all migrations, registry should contain:
- 4+ Android apps
- 2+ iOS apps
- 2+ Web apps
- 6+ Core packages
- 2+ UI packages
- 2+ Utility packages

### Step 8.2: Create LD-modules.md

```markdown
# NewAvanues Modules

## Apps
| ID | Name | Platform | Path | Status |
|----|------|----------|------|--------|
| ava-android | AVA | Android | apps/android/ava | Active |
| voiceos-android | VoiceOS | Android | apps/android/voiceos | Active |
...

## Packages
| ID | Name | Type | Path | Used By |
|----|------|------|------|---------|
| nlu | NLU Engine | core | packages/core/nlu | AVA, VoiceOS |
...
```

---

## Critical Integration Points

### 1. NLU + VoiceOS Command System

**Synergy:** AVA IntentClassifier feeds VoiceOS CommandManager

```
AVA IntentClassifier (52 languages)
        ↓
  Intent + Confidence
        ↓
VoiceOS CommandManager
        ↓
  Tier 1/2/3 execution
```

**Action:** Create `packages/core/intent-bridge/` adapter

### 2. Unified Database

**Current:** Each app has own SQLDelight schema
**Target:** Single `packages/core/database/` with all tables

**Action:** Merge schemas:
- VoiceOS: learned_apps, screen_states, generated_commands
- AVA: intents, intent_examples
- AvaConnect: connections, devices

### 3. Shared Commands/Intents

**Action:** Create converters:
- `.vos` → `.ava` (exists in AVA)
- `.ava` → CommandManager format

---

## Execution Checklist

| Phase | Task | Command/Action | Verify |
|-------|------|----------------|--------|
| 1 | Create directories | `mkdir -p ...` | `tree -L 2` |
| 1 | Create registry | Write JSON | File exists |
| 2 | Copy AVA app | `cp -r` (exclude build) | Compiles |
| 2 | Copy NLU package | `cp -r` | Tests pass |
| 3 | Copy VoiceOS app | `cp -r` (exclude build) | Compiles |
| 3 | Copy packages | `cp -r` each | Tests pass |
| 4 | Copy Avanues | `cp -r` | Compiles |
| 5 | Copy WebAvanue | `cp -r` | Compiles |
| 6 | Copy AvaConnect | `cp -r` | Compiles |
| 7 | Create build files | Write gradle | `./gradlew projects` |
| 8 | Update registry | Add all entries | JSON valid |
| 8 | Create living doc | Write markdown | Links work |

---

## Post-Migration Tasks

1. **Update imports** - Package names change
2. **Fix dependencies** - Point to new package paths
3. **Run tests** - All modules
4. **Update CI/CD** - New build paths
5. **Archive originals** - Move to `/Volumes/M-Drive/Coding/archive/`

---

## Rollback Plan

If migration fails:
1. Original repos remain untouched
2. Delete NewAvanues contents (except .ideacode, .claude)
3. Investigate failure
4. Re-attempt with fixes

---

## Agent Instructions

**When executing this plan:**

1. **ALWAYS** check if target file exists before copying
2. **NEVER** overwrite without confirmation
3. **LOG** every copy operation to `specs/migration-log-YYYYMMDD.md`
4. **UPDATE** registry after each successful module migration
5. **TEST** compilation after each phase
6. **COMMIT** after each successful phase with message: `chore(monorepo): Phase N - Migrate {source}`

---

**Plan Created:** 2025-12-01
**Author:** Claude (AI Assistant)
**Approval Required:** YES - Do not execute without user approval
