# MainAvanues Monorepo Structure

**Version:** 1.0
**Date:** 2025-11-24
**Organization:** Platform-First (Industry Best Practice)

---

## 📁 Complete Folder Structure

```
MainAvanues/
├── android/                          # Android platform
│   ├── apps/                         # Android applications
│   │   ├── ava/                      # AVA AI Assistant
│   │   │   └── src/main/
│   │   │       ├── kotlin/net/ideahq/ava/
│   │   │       └── res/
│   │   ├── voiceos/                  # VoiceOS Platform
│   │   │   └── src/main/
│   │   │       ├── kotlin/net/ideahq/voiceos/
│   │   │       └── res/
│   │   ├── avaconnect/               # AVA Connect
│   │   │   └── src/main/
│   │   │       ├── kotlin/net/ideahq/avaconnect/
│   │   │       └── res/
│   │   └── avanues/                  # Avanues Platform
│   │       └── src/main/
│   │           ├── kotlin/net/ideahq/avanues/
│   │           └── res/
│   └── modules/                      # Android-specific modules
│       ├── accessibility/            # Accessibility services
│       └── voice-engine/             # Android voice engine
│
├── ios/                              # iOS platform (future)
│   ├── apps/                         # iOS applications (TBD)
│   └── modules/                      # iOS-specific modules
│
├── web/                              # Web platform
│   └── apps/
│       └── webavanue/                # Web Avanue
│           ├── src/
│           └── public/
│
├── common/                           # Kotlin Multiplatform (KMP)
│   └── libs/                         # Shared libraries (80% of code)
│       ├── voice/                    # Voice scope
│       │   ├── feature-recognition/  # Voice recognition feature
│       │   │   └── src/
│       │   │       ├── commonMain/kotlin/
│       │   │       ├── androidMain/kotlin/
│       │   │       ├── iosMain/kotlin/
│       │   │       └── commonTest/kotlin/
│       │   ├── feature-dsl/          # Voice DSL feature
│       │   │   └── src/
│       │   │       ├── commonMain/kotlin/
│       │   │       ├── androidMain/kotlin/
│       │   │       └── iosMain/kotlin/
│       │   ├── data-access-api/      # Voice API data layer
│       │   ├── ui-waveform/          # Voice waveform UI
│       │   └── util-audio/           # Audio utilities
│       │
│       ├── accessibility/            # Accessibility scope
│       │   ├── feature-voice-cursor/ # Voice cursor feature
│       │   │   └── src/
│       │   │       ├── commonMain/kotlin/
│       │   │       ├── androidMain/kotlin/
│       │   │       └── iosMain/kotlin/
│       │   ├── feature-voice-keyboard/
│       │   └── util-gestures/
│       │
│       ├── browser/                  # Browser scope
│       │   ├── feature-extension/    # Browser extension
│       │   ├── data-access-tabs/     # Tab management
│       │   └── util-dom/             # DOM utilities
│       │
│       ├── cloud/                    # Cloud scope
│       │   ├── feature-sync/         # Cloud sync
│       │   ├── data-access-firebase/ # Firebase integration
│       │   └── util-auth/            # Auth utilities
│       │
│       └── shared/                   # Cross-cutting scope
│           ├── ui-design-system/     # Design system (KMP)
│           │   └── src/
│           │       ├── commonMain/kotlin/
│           │       ├── androidMain/kotlin/
│           │       └── iosMain/kotlin/
│           ├── data-access-repository/
│           ├── util-logger/
│           └── util-network/
│
├── docs/                             # Documentation (HYBRID CENTRALIZED)
│   ├── README.md                     # Monorepo overview
│   ├── ARCHITECTURE.md               # Architecture overview
│   ├── CONTRIBUTING.md               # Contribution guidelines
│   ├── SETUP.md                      # Setup instructions
│   ├── DEPLOYMENT.md                 # Deployment guide
│   ├── TESTING.md                    # Testing strategy
│   │
│   ├── ideacode/                     # Root IDEACODE (monorepo-level)
│   │   ├── specs/                    # Feature specifications
│   │   ├── features/                 # Active features
│   │   ├── archive/                  # Completed features
│   │   └── registries/
│   │       └── DOCUMENTATION-REGISTRY.json
│   │
│   ├── android/                      # Android platform docs
│   │   └── apps/
│   │       ├── ava/
│   │       │   ├── dev-overview.md
│   │       │   └── ideacode/         # AVA-specific IDEACODE
│   │       │       ├── specs/
│   │       │       ├── features/
│   │       │       └── archive/
│   │       ├── voiceos/
│   │       │   ├── dev-overview.md
│   │       │   └── ideacode/         # VoiceOS-specific IDEACODE
│   │       │       ├── specs/
│   │       │       ├── features/
│   │       │       └── archive/
│   │       ├── avaconnect/
│   │       │   ├── dev-overview.md
│   │       │   └── ideacode/
│   │       └── avanues/
│   │           ├── dev-overview.md
│   │           └── ideacode/
│   │
│   ├── common/                       # KMP library docs
│   │   └── libs/
│   │       ├── voice/
│   │       │   └── feature-recognition/
│   │       │       └── api-overview.md
│   │       ├── accessibility/
│   │       │   └── feature-voice-cursor/
│   │       │       └── api-overview.md
│   │       └── shared/
│   │           └── ui-design-system/
│   │               └── api-overview.md
│   │
│   ├── web/                          # Web platform docs
│   │   └── apps/
│   │       └── webavanue/
│   │           ├── dev-overview.md
│   │           └── ideacode/
│   │
│   └── archive/                      # Historical docs
│       └── 2024/
│
├── examples/                         # Example code and demos
├── scripts/                          # Build and automation scripts
│   └── setup-monorepo-structure.sh
├── tools/                            # Development tools
│
├── .ideacode/                        # IDEACODE configuration
│   └── config.yml
│
├── settings.gradle.kts               # Gradle module registration
├── build.gradle.kts                  # Root build configuration
└── gradle.properties                 # Gradle properties
```

---

## 🎯 Design Principles

### 1. Platform-First Organization
- **Rationale:** Mirrors deployment targets (Google, Meta, Nx standard)
- **Structure:** `/{platform}/apps/` and `/{platform}/modules/`
- **Benefits:** Clear platform boundaries, easier CI/CD, natural code ownership

### 2. 80/20 Rule
- **80% of code** → `common/libs/` (KMP shared libraries)
- **20% of code** → `{platform}/apps/` (thin platform shells)
- **Goal:** Maximum code reuse across Android/iOS/Web

### 3. Scope + Type Library Naming
- **Pattern:** `/common/libs/{scope}/{type}-{name}/`
- **Scopes:** voice, accessibility, browser, cloud, shared
- **Types:** feature, data-access, ui, util
- **Example:** `common/libs/voice/feature-recognition/`

### 4. KMP Source Sets
- **commonMain** → Platform-agnostic code (70%)
- **androidMain** → Android-specific (15%)
- **iosMain** → iOS-specific (15%)
- **Pattern:** expect/actual, Interface+Factory

### 5. Hybrid Centralized Documentation
- **All docs** → `/docs/` (single source of truth)
- **Structure mirrors code** → `/docs/{platform}/apps/{app}/`
- **Multiple IDEACODE folders:**
  - Root: `/docs/ideacode/` (monorepo-level specs)
  - Per-app: `/docs/{platform}/apps/{app}/ideacode/` (app-specific features)
- **Registry-based:** AI checks `DOCUMENTATION-REGISTRY.json` before creating files

---

## 📋 Library Organization Matrix

| Scope | Feature | Data Access | UI | Util |
|-------|---------|-------------|-----|------|
| **voice** | recognition, dsl | api | waveform | audio |
| **accessibility** | voice-cursor, voice-keyboard | - | - | gestures |
| **browser** | extension | tabs | - | dom |
| **cloud** | sync | firebase | - | auth |
| **shared** | - | repository | design-system | logger, network |

---

## 📝 Documentation Naming Convention

**Pattern:** `{type}-{context}-{topic}.md`

**Types:**
- `dev` → Developer guides
- `api` → API documentation
- `user` → User documentation
- `spec` → Feature specifications
- `adr` → Architecture Decision Records
- `rfc` → Request for Comments
- `test` → Test documentation
- `fix` → Bug fix documentation

**Examples:**
- `dev-android-setup.md`
- `api-voice-recognition.md`
- `user-accessibility-features.md`
- `spec-voice-dsl.md`
- `adr-001-kmp-migration.md`

---

## 🔄 IDEACODE Folder Selection

**Decision Tree:**

```
Is this a monorepo-wide feature? (affects multiple apps/libs)
├─ YES → Use /docs/ideacode/
└─ NO → Is it app-specific?
    ├─ YES → Use /docs/{platform}/apps/{app}/ideacode/
    └─ NO → Is it a library?
        └─ YES → Document in /docs/common/libs/{scope}/{lib}/
```

**Examples:**
- Authentication system (affects all apps) → `/docs/ideacode/specs/`
- AVA voice command feature → `/docs/android/apps/ava/ideacode/specs/`
- Voice recognition API → `/docs/common/libs/voice/feature-recognition/`

---

## 🚀 Migration Strategy

### Phase 1: Structure Setup ✅
- Run `scripts/setup-monorepo-structure.sh`
- Verify folder structure
- Create documentation registry

### Phase 2: Code Migration (Next)
1. **AVA AI** → `android/apps/ava/`
2. **VoiceOS** → `android/apps/voiceos/`
3. **AVA Connect** → `android/apps/avaconnect/`
4. **Avanues** → `android/apps/avanues/`
5. **Web Avanue** → `web/apps/webavanue/`

### Phase 3: KMP Refactor
1. Extract shared code → `common/libs/`
2. Create expect/actual interfaces
3. Implement platform-specific code
4. Add tests for each source set

### Phase 4: Build Configuration
1. Create `settings.gradle.kts` with all modules
2. Create `build.gradle.kts` for each library
3. Configure dependency graph
4. Set up composite builds

### Phase 5: Documentation Migration
1. Move existing docs to new structure
2. Update registry
3. Add API documentation
4. Create developer guides

---

## 📊 Code Distribution Target

| Platform | Percentage | Location |
|----------|-----------|----------|
| Common (KMP) | 70% | `common/libs/{scope}/` |
| Android | 15% | `android/apps/`, `android/modules/` |
| iOS | 15% | `ios/apps/`, `ios/modules/` |
| Web | Variable | `web/apps/` |

---

## 🛠️ Development Workflow

1. **Feature Request** → Create spec in appropriate IDEACODE folder
2. **Planning** → Use `/ideacode.plan` to generate implementation plan
3. **Implementation** → Code in KMP common first, then platform-specific
4. **Testing** → Test all source sets (commonTest, androidTest, iosTest)
5. **Documentation** → Update registry, create/update API docs
6. **Review** → Code review with 98-point checklist
7. **Archive** → Move completed feature to archive/

---

## 📚 Key Documents

- [MONOREPO-RESEARCH-FINDINGS.md](./migration-analysis/MONOREPO-RESEARCH-FINDINGS.md) - Industry research
- [PLATFORM-COMMON-FILE-STRATEGY.md](./migration-analysis/PLATFORM-COMMON-FILE-STRATEGY.md) - KMP strategy
- [DOCUMENTATION-STRUCTURE-FINAL.md](./migration-analysis/DOCUMENTATION-STRUCTURE-FINAL.md) - Docs design
- [ARCHITECTURE.md](./ARCHITECTURE.md) - Architecture overview
- [DOCUMENTATION-REGISTRY.json](./ideacode/registries/DOCUMENTATION-REGISTRY.json) - Doc registry

---

## 🔗 External References

- [Nx Monorepo Best Practices](https://nx.dev/concepts/more-concepts/library-types)
- [Turborepo Structure Guide](https://turbo.build/repo/docs/handbook)
- [Google Monorepo Overview](https://cacm.acm.org/magazines/2016/7/204032-why-google-stores-billions-of-lines-of-code-in-a-single-repository/fulltext)
- [Kotlin Multiplatform Docs](https://kotlinlang.org/docs/multiplatform.html)

---

**Last Updated:** 2025-11-24
**Author:** IDEACODE Framework
**Maintainer:** Manoj Jhawar (manoj@ideahq.net)
