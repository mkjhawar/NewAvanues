# NewAvanues Project Rules

**Project-specific rules that supplement the global IDEACODE CLAUDE.md**

---

## App Placement Rules (MANDATORY)

### Critical Rules

| Rule | Requirement |
|------|-------------|
| **NEVER** | Create apps inside `Modules/` folders |
| **NEVER** | Place Android apps anywhere except `android/apps/` |
| **NEVER** | Place iOS apps anywhere except `ios/apps/` |
| **NEVER** | Place Web apps anywhere except `web/apps/` |
| **ALWAYS** | Modules contain ONLY shared KMP libraries |
| **ALWAYS** | Platform-specific code goes in platform root folders |

### Folder Semantics

| Folder | Contains | Does NOT Contain |
|--------|----------|------------------|
| `Modules/{Name}/` | Shared KMP libraries only | Runnable apps, platform-specific code |
| `android/apps/` | Android apps | Shared libraries |
| `ios/apps/` | iOS apps | Shared libraries |
| `web/apps/` | Web apps | Shared libraries |
| `desktop/apps/` | Desktop apps | Shared libraries |

### Module vs App

| Type | Location | Example |
|------|----------|---------|
| KMP Library | `Modules/{Name}/` | `Modules/AVA/core/` |
| Android App | `android/apps/{AppName}/` | `android/apps/VoiceOS/` |
| iOS App | `ios/apps/{AppName}/` | `ios/apps/VoiceOS/` |
| Web App | `web/apps/{AppName}/` | `web/apps/dashboard/` |

### Why This Matters

1. **Build conflicts:** Apps in Modules cause gradle configuration conflicts
2. **Dependency confusion:** Apps may accidentally depend on wrong module versions
3. **Platform targeting:** Apps have platform-specific build configs that don't belong in shared modules
4. **Code duplication:** Misplaced apps get duplicated across locations

---

## Module Definitions

### What Each Module Contains

| Module | Purpose | Contains |
|--------|---------|----------|
| AVA | Core AVA library | Shared utilities, data models, APIs |
| AvaMagic | UI generation | Parsers, generators, DSL tools |
| VoiceOS | Voice processing | KMP voice logic, command processing |
| WebAvanue | Web components | Shared web utilities |
| NLU | Natural language | Language processing libraries |
| Shared | Common utilities | Cross-module shared code |

### What Modules Should NEVER Contain

- Android Manifest files
- iOS Info.plist files
- Platform-specific MainActivity/AppDelegate
- Application class definitions
- Platform-specific build.gradle configurations for apps

---

## Verification

Before committing, verify:

```bash
# Check for apps in wrong locations
find Modules -type d -name "apps" 2>/dev/null
# Expected output: (nothing)

# Apps should only be in platform folders
ls android/apps/
ls ios/apps/
ls web/apps/
```

---

**Version:** 1.0
**Updated:** 2026-01-15
**Status:** Active
