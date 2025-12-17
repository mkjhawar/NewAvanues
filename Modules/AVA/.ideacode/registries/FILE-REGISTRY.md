# File Registry - AVA Module v12.0.0

## File Naming Conventions

### Documentation Files
| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-AVA-{Desc}-V#.md` | `LD-AVA-Module-State-V1.md` |
| Specs | `AVA-Spec-{Feature}-YDDMM-V#.md` | `AVA-Spec-Assistant-51212-V1.md` |
| Plans | `AVA-Plan-{Feature}-YDDMM-V#.md` | `AVA-Plan-Integration-51212-V1.md` |

### Code Files
| Type | Pattern | Example |
|------|---------|---------|
| Kotlin (Shared) | `{Component}.kt` | `AssistantCore.kt` |
| Kotlin (Android) | `{Component}Android.kt` | `AssistantAndroid.kt` |
| Swift (iOS) | `{Component}.swift` | `AssistantIOS.swift` |

### Configuration Files
| Type | Pattern | Location |
|------|---------|----------|
| Module Config | `config.idc` | `.ideacode/config.idc` |
| Gradle | `build.gradle.kts` | Module root |

## Prohibited Patterns

❌ `AVA-AVA-*.md` (redundant module name)
❌ `*.json` for config (use .idc format)
❌ Spaces in filenames

---

Updated: 2025-12-15 | Version: 12.0.0
