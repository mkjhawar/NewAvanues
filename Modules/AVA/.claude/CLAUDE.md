# AVA - Module Instructions

Parent Repository: NewAvanues
Module: AVA

---

## SCOPE

Work within AVA module only.
For cross-module changes, check with user first.

---

## INHERITED RULES

1. Parent repo rules: `/Volumes/M-Drive/Coding/NewAvanues/.claude/CLAUDE.md`
2. Global rules: `/Volumes/M-Drive/Coding/.claude/CLAUDE.md`

---

## DOCUMENTATION LOCATIONS

**Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/AVA/LivingDocs/LD-*.md`
**Registries:** `Modules/AVA/.ideacode/registries/`
- FOLDER-REGISTRY.md - Folder structure for this module
- FILE-REGISTRY.md - File naming for this module
- COMPONENT-REGISTRY.md - Components in this module

**Check Registries FIRST** before creating files or folders.

---

## MODULE-SPECIFIC RULES

| Rule | Requirement |
|------|-------------|
| Platform | Kotlin Multiplatform (KMP) |
| Database | SQLDelight (shared across platforms) |
| Networking | Ktor client |
| Serialization | kotlinx.serialization |
| Testing | 90%+ coverage for core logic |

---

## KEY COMPONENTS

- **Assistant Core** - Core AI logic (shared)
- **NLU Integration** - Natural language processing
- **Platform Adapters** - Android, iOS, Web implementations
- **Response Generator** - Response generation logic

---

## PLATFORM STATUS

- **Android:** ✅ Fully implemented
- **iOS:** 🟡 In progress (SwiftUI integration)
- **Web:** ✅ React integration complete

---

## DEPENDENCIES

**Internal:**
- `Common/Core` - Shared utilities
- `NLU` - Natural language understanding

**External:**
- Kotlin Multiplatform
- Ktor (networking)
- kotlinx.serialization
- SQLDelight

See: `/Volumes/M-Drive/Coding/NewAvanues/.ideacode/registries/CROSS-MODULE-DEPENDENCIES.md`

---

## FILE NAMING

| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-AVA-{Desc}-V#.md` | `LD-AVA-Feature-V1.md` |
| Specs | `AVA-Spec-{Feature}-YDDMM-V#.md` | `AVA-Spec-Assistant-51215-V1.md` |
| Kotlin (Shared) | `{Component}.kt` | `AssistantCore.kt` |
| Kotlin (Android) | `{Component}Android.kt` | `AssistantAndroid.kt` |

---

## BUILDING & TESTING

```bash
# Build Android
./gradlew :Modules:AVA:platforms:android:assembleDebug

# Test shared code
./gradlew :Modules:AVA:shared:test

# Build Web
cd Modules/AVA/platforms/web && npm run build
```

---

Updated: 2025-12-15 | Version: 12.0.0
