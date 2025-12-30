# {PROJECT_NAME} Monorepo - Project Instructions

**Version:** 1.0.0
**Last Updated:** {DATE}
**Purpose:** Centralized instructions for all apps and platforms - single reference point

---

## 📖 How to Use This File

**For AI assistants:**
1. Read this file at session start (along with FOLDER-REGISTRY.md)
2. Reference the specific app/platform section you're working on
3. Follow the instructions exactly as specified
4. Update this file when project requirements change

**For developers:**
1. Find your app section
2. Read Master instructions for overall architecture
3. Read Platform instructions for platform-specific details
4. Follow naming conventions and folder structure from FOLDER-REGISTRY.md

---

## 🎯 Global Monorepo Rules

### Code Sharing Strategy
- **Target:** {CODE_SHARING_PERCENTAGE}% KMP code sharing across platforms
- **Priority:** Share business logic, database, models, domain code
- **Platform-specific:** UI, platform APIs, native integrations
- **Exception:** Apps may differ due to backend platform requirements

### Build System
- **Type:** Hybrid approach
- **Root build:** Single Gradle multi-module build (build.gradle.kts)
- **Shared modules:** Centralized in common/ with unified dependency versions
- **App builds:** Can have independent configurations when needed

### Team Model
- **Current:** {TEAM_MODEL} (Solo/Platform teams/Feature teams/Mixed)
- **Organization:** Platform-first structure (android/, ios/, desktop/)
- **Flexibility:** Structure supports both platform teams and feature teams

### Folder Structure Enforcement
- **ALWAYS** check FOLDER-REGISTRY.md before creating folders
- **NEVER** create folder variations (e.g., `voice-recognition/` vs `voice_recognition/`)
- **ALWAYS** use kebab-case for folders and files
- **NEVER** use type prefixes (use `authentication/` NOT `feature-authentication/`)

---

## 📱 {APP1_NAME}

### Master Instructions (All Platforms)

**Location:** `docs/{app1}/Master/`
**Project Root:** `{platform}/{app1}/` (where platform = android, ios, desktop)

#### Overview
{APP1_DESCRIPTION}

#### Architecture Principles
1. {PRINCIPLE_1}
2. {PRINCIPLE_2}
3. {PRINCIPLE_3}

#### Core Modules (common/)
- `common/{module1}/` - {MODULE1_DESCRIPTION}
- `common/{module2}/` - {MODULE2_DESCRIPTION}

#### Database Tables
See: `docs/{app1}/Master/database-schema.md`

#### Performance Requirements
- {PERFORMANCE_REQ_1}
- {PERFORMANCE_REQ_2}

#### Testing Requirements
- **Unit test coverage:** 90%+ on critical paths
- {ADDITIONAL_TEST_REQS}

---

### Platform-Specific Instructions

#### Android (android/{app1}/)

**Location:** `docs/{app1}/Platform/android/`

**Android-Specific Features:**
- {ANDROID_FEATURE_1}
- {ANDROID_FEATURE_2}

**Build Configuration:**
- Min SDK: {MIN_SDK}
- Target SDK: {TARGET_SDK}
- Kotlin version: {KOTLIN_VERSION}

**Key Dependencies:**
- {DEPENDENCY_1}
- {DEPENDENCY_2}

#### iOS (ios/{app1}/)

**Location:** `docs/{app1}/Platform/ios/`

**iOS-Specific Features:**
- {IOS_FEATURE_1}
- {IOS_FEATURE_2}

**Build Configuration:**
- Min iOS: {MIN_IOS}
- Target iOS: Latest
- Swift version: {SWIFT_VERSION}

#### Desktop (desktop/{app1}/)

**Location:** `docs/{app1}/Platform/desktop/`

**Desktop-Specific Features:**
- {DESKTOP_FEATURE_1}
- {DESKTOP_FEATURE_2}

**Build Configuration:**
- Platforms: macOS, Windows, Linux
- JVM target: 17+
- Compose Desktop (UI)

---

## 📱 {APP2_NAME}

### Master Instructions (All Platforms)

**Location:** `docs/{app2}/Master/`

{REPEAT_STRUCTURE_FOR_EACH_APP}

---

## 📦 Common Modules (KMP Shared)

**Location:** `common/{module-name}/`

### Database Module (common/database/)

**Purpose:** Unified SQLDelight database

**Tables:** {TABLE_COUNT} tables

**Strategy:**
- {DATABASE_STRATEGY}

### {MODULE_NAME} Module (common/{module}/)

**Purpose:** {MODULE_PURPOSE}

**Used By:** {APPS_USING}

**Platform Support:**
- Android: {ANDROID_SUPPORT}
- iOS: {IOS_SUPPORT}
- Desktop: {DESKTOP_SUPPORT}

---

## 🔧 Build System Instructions

### Root Build (build.gradle.kts)

**Type:** Single Gradle multi-module build

**Configuration:**
- Kotlin version: {KOTLIN_VERSION}
- Gradle version: {GRADLE_VERSION}
- AGP version: {AGP_VERSION} (for Android)
- SQLDelight version: {SQLDELIGHT_VERSION}

**Modules:**
```kotlin
include(
    // Android apps
    ":android:{app1}",
    ":android:{app2}",

    // iOS apps
    ":ios:{app1}",
    ":ios:{app2}",

    // Desktop apps
    ":desktop:{app1}",
    ":desktop:{app2}",

    // Common modules
    ":common:database",
    ":common:{module1}",
)
```

---

## 📝 Documentation Instructions

### Where to Document

| Document Type | Location | Filename Pattern |
|--------------|----------|------------------|
| **App Architecture** | `docs/{app}/Master/` | `{component}-architecture.md` |
| **App Specifications** | `docs/{app}/Master/` | `{feature}-spec.md` |
| **App Vision** | `docs/{app}/Master/` | `vision.md` or `README.md` |
| **Platform Implementation** | `docs/{app}/Platform/{platform}/` | `{feature}-implementation.md` |
| **IDEACODE Specs** | `docs/ideacode/specs/` | `{number}-{name}/spec.md` |
| **Migration Docs** | `docs/migration/` | `{migration}-{type}-YYYYMMDD.md` |
| **Obsolete Docs** | `docs/archive/{platform}/` | Original filename preserved |

### Master vs Platform Documentation

**Master Documentation (docs/{app}/Master/):**
- Universal architecture (applies to all platforms)
- Core business logic and domain models
- API contracts and interfaces
- User flows and requirements
- Vision and strategic direction
- Database schemas (shared)

**Platform Documentation (docs/{app}/Platform/{platform}/):**
- Platform-specific implementation details
- Native API integrations
- Platform-specific UI/UX
- Build and deployment instructions
- Platform-specific testing
- Performance benchmarks (per platform)

---

## 🗃️ Archive Instructions

### When to Archive

Archive documentation when:
1. Feature has been removed from codebase
2. Document is obsolete (superseded by newer version)
3. Platform is deprecated
4. Document is no longer relevant but has historical value

### Archive Structure

**Location:** `docs/archive/{platform}/`

**Process:**
1. Move to archive: `docs/archive/{platform}/{original-filename}`
2. Preserve filename
3. Add archive note header

---

## 📊 Update History

| Date | Section | Change | Author |
|------|---------|--------|--------|
| {DATE} | All | Initial creation | {AUTHOR} |

---

## 🔍 Quick Reference

**Before starting work:**
1. ✅ Read FOLDER-REGISTRY.md
2. ✅ Read this file (PROJECT-INSTRUCTIONS.md)
3. ✅ Check relevant app section
4. ✅ Check relevant platform section
5. ✅ Follow naming conventions

**Before committing:**
1. ✅ Verify folder names match FOLDER-REGISTRY.md
2. ✅ Verify file names use kebab-case
3. ✅ Update FOLDER-REGISTRY.md if new folders created
4. ✅ Run tests (90%+ coverage required)

---

**CRITICAL:** This file is the single source of truth for project instructions.
**ALWAYS** reference this file when working on any app or platform.
