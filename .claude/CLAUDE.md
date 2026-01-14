# NewAvanues - Project Instructions

Platform-centric KMP monorepo (AVA, VoiceOS, WebAvanue, Cockpit, NLU)

---

## MANDATORY: Before Any File Operation

1. **READ REGISTRIES FIRST:**
   - `.ideacode/Registries/FOLDER-REGISTRY.md`
   - `.ideacode/Registries/Modules.registry.json`
   - `.ideacode/Registries/Docs.registry.json`

2. **Case Rules:**
   - Gradle paths: `lowercase` (android/, ios/, desktop/)
   - All other folders: `PascalCase` (Common/, Docs/, Shared/)

---

## PROJECT STRUCTURE

| Path | Purpose | Case |
|------|---------|------|
| `android/` | Android apps | lowercase |
| `ios/` | iOS apps | lowercase |
| `desktop/` | Desktop apps | lowercase |
| `Common/` | KMP shared libraries | PascalCase |
| `Modules/` | Feature modules | PascalCase |
| `Modules/AVA/` | AI assistant platform | PascalCase |
| `Modules/VoiceOS/` | Voice-first accessibility | PascalCase |
| `Modules/WebAvanue/` | Web platform (Tauri + React) | PascalCase |
| `Modules/Cockpit/` | Management dashboard | PascalCase |
| `Modules/NLU/` | Natural language understanding | PascalCase |
| `Shared/` | Assets, configs | PascalCase |
| `Docs/` | Documentation | PascalCase |
| `/Vivoka/` | Voice SDK (root level) | - |

---

## BUILD REQUIREMENTS

| Requirement | Version | Notes |
|-------------|---------|-------|
| JDK | 17 | **REQUIRED** - JDK 18+ incompatible |
| Gradle | 8.x | Via wrapper |
| Android SDK | 34 | Target API |
| Vivoka SDK | - | Voice recognition at `/Vivoka/` |

### JDK 17 Setup (macOS)

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
java -version  # Should show 17.x.x
./gradlew build
```

---

## APPS

| App | Platforms | Android Path | Docs Path |
|-----|-----------|--------------|-----------|
| VoiceOS | android, ios, desktop | `android/voiceos/` | `Docs/VoiceOS/` |
| AVA | android, ios, macos, win, linux, web | `android/ava/` | `Docs/AVA/` |
| Avanues | android | `android/avanues/` | `Docs/Avanues/` |
| AvaConnect | android, web | `android/avaconnect/` | `Docs/AvaConnect/` |

---

## COMMON LIBRARIES

| Library | Path | Description |
|---------|------|-------------|
| Database | `Common/Database/` | SQLDelight |
| Voice | `Common/Voice/` | Voice recognition |
| NLU | `Common/NLU/` | NLU engine |
| UI | `Common/UI/` | MagicUI / AVAUI |
| Utils | `Common/Utils/` | Utilities |

---

## BRANCHES

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
| Main | /Volumes/M-Drive/Coding/NewAvanues | VoiceOSCoreNG |
| AVAMagic | /Volumes/M-Drive/WorkTrees/Development-AVAMagic | AVAMagic-Development |
| VoiceOS | /Volumes/M-Drive/Worktrees/Development-VoiceOS | VoiceOS-Development |

### Scripts

```bash
./scripts/worktree-add.sh <branch> [base]   # Create
./scripts/worktree-list.sh                   # List
./scripts/worktree-remove.sh <path>          # Remove
./scripts/worktree-status.sh                 # Status
```

---

## KEY RULES (Project-Specific)

| Rule | Requirement |
|------|-------------|
| Registry First | Check registries before creating files |
| Database | SQLDelight ONLY (never Room) |
| Module work | Check module-level CLAUDE.md first |
| Cross-module | Check CROSS-MODULE-DEPENDENCIES.md |

---

## DOCUMENTATION

| Path | Purpose |
|------|---------|
| `Docs/Project/` | Living docs (LD-*.md) |
| `Docs/{App}/MasterSpecs/` | Universal specs |
| `Docs/{App}/Platform/{Platform}/` | Platform-specific |
| `Docs/Common/{Domain}/` | Shared lib docs |
| `docs/appstructure/` | Architecture registries (v18) |
| `Docs/MasterDocs/` | Comprehensive platform documentation |

---

## MASTERDOCS MAINTENANCE (MANDATORY)

### Overview

MasterDocs contains comprehensive platform documentation in TWO formats:
1. **Human-Readable** - Full prose documentation for developers and marketing
2. **AI-Readable** - Structured YAML/compact format for AI assistants

**YOU MUST** keep both formats synchronized when making code changes.

### Documentation Structure

```
Docs/MasterDocs/
├── AI/                        # AI-readable documentation
│   ├── PLATFORM-INDEX.ai.md   # Module registry, dependencies, APIs
│   ├── CLASS-INDEX.ai.md      # All classes with methods/fields
│   ├── REFACTORING-GUIDE.ai.md # Current refactoring recommendations
│   └── (future files)
│
├── LD/                        # Living Documents (continuously updated)
│   ├── LD-Platform-Overview-V1.md
│   ├── LD-Module-Registry-V1.md
│   └── LD-API-Reference-V1.md
│
├── VoiceOSCoreNG/             # Module documentation
│   ├── README.md              # Human-readable
│   └── html/                  # Interactive diagrams
│
├── VoiceOS/README.md
├── AVA/README.md
├── LLM/README.md
├── NLU/README.md
├── RAG/README.md
├── WebAvanue/README.md
└── Common/README.md
```

### Update Rules

| Trigger | Action Required |
|---------|-----------------|
| New class added | Update AI/CLASS-INDEX.ai.md |
| New module added | Update AI/PLATFORM-INDEX.ai.md + create README.md |
| API changed | Update AI/PLATFORM-INDEX.ai.md + LD/LD-API-Reference |
| Architecture changed | Update module README.md + html diagrams |
| Refactoring done | Update AI/REFACTORING-GUIDE.ai.md |
| Major feature complete | Update LD/LD-Platform-Overview |

### AI-Readable Format Guidelines

```yaml
# Use YAML blocks for structured data
class_name:
  package: com.augmentalis.module.class
  type: class|interface|object|data_class|sealed_class
  purpose: One-line description
  methods:
    - methodName(params): ReturnType
  dependencies: [list, of, deps]
```

### Human-Readable Format Guidelines

- Include ASCII diagrams for architecture
- Include Mermaid diagrams in HTML files
- Use tables for quick reference
- Include code examples with comments
- Write for both novice and expert developers

### Verification

After updating MasterDocs:
1. Verify AI format is valid YAML
2. Verify human format renders correctly
3. Verify cross-references are accurate
4. Commit both formats together

---

## KEY RULES (Project-Specific)

| Rule | Requirement |
|------|-------------|
| Registry First | Check registries before creating files |
| Database | SQLDelight ONLY (never Room) |
| Module work | Check module-level CLAUDE.md first |
| Cross-module | Check CROSS-MODULE-DEPENDENCIES.md |
| **MasterDocs** | **Update BOTH AI and Human docs on code changes** |

---

## Inherited Rules

All rules from global CLAUDE.md (v18) apply.

---

**Updated:** 2026-01-11 | **Version:** 14.0
