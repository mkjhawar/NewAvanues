# Cockpit - Module Instructions

Parent Repository: NewAvanues
Module: Cockpit

---

## SCOPE

Work within Cockpit module only.
For cross-module changes, check with user first.

**IMPORTANT:** Cockpit manages all other modules - changes here may affect the entire system.

---

## INHERITED RULES

1. Parent repo rules: `/Volumes/M-Drive/Coding/NewAvanues/.claude/CLAUDE.md`
2. Global rules: `/Volumes/M-Drive/Coding/.claude/CLAUDE.md`

---

## DOCUMENTATION LOCATIONS

**Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/Cockpit/LivingDocs/LD-*.md`
**Registries:** `Modules/Cockpit/.ideacode/registries/`
- FOLDER-REGISTRY.md - Folder structure for this module
- FILE-REGISTRY.md - File naming for this module
- COMPONENT-REGISTRY.md - Components in this module

**Check Registries FIRST** before creating files or folders.

---

## MODULE-SPECIFIC RULES

| Rule | Requirement |
|------|-------------|
| Language | Kotlin |
| Configuration | IDC format for all configs |
| Database | SQLDelight (configuration storage) |
| Testing | 90%+ coverage for management logic |
| Validation | Validate all configs before applying |

---

## KEY COMPONENTS

- **Module Manager** - Manages VoiceOS, AVA, WebAvanue, NLU
- **System Monitor** - Health checks and metrics
- **Config Manager** - IDC configuration management
- **Dashboard UI** - Management interface

---

## MANAGED MODULES

- **VoiceOS** - Android accessibility service
- **AVA** - AI assistant platform
- **WebAvanue** - Web platform
- **NLU** - Natural language understanding
- **Common** - Shared libraries

**CRITICAL:** Changes to module configurations must be validated before deployment.

---

## DEPENDENCIES

**Internal:**
- All modules (for management)
- `Common/Core` - Shared utilities

**External:**
- Kotlin
- SQLDelight
- IDC parser

See: `/Volumes/M-Drive/Coding/NewAvanues/.ideacode/registries/CROSS-MODULE-DEPENDENCIES.md`

---

## FILE NAMING

| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-CPT-{Desc}-V#.md` | `LD-CPT-Feature-V1.md` |
| Specs | `CPT-Spec-{Feature}-YDDMM-V#.md` | `CPT-Spec-Management-51215-V1.md` |
| Kotlin | `{Component}.kt` | `ModuleManager.kt` |
| Plugins | `{Plugin}Plugin.kt` | `MonitoringPlugin.kt` |

---

## BUILDING & TESTING

```bash
# Build
./gradlew :Modules:Cockpit:build

# Test
./gradlew :Modules:Cockpit:test

# Validate configs
./gradlew :Modules:Cockpit:validateConfigs
```

---

Updated: 2025-12-15 | Version: 12.0.0
