# File Registry - VoiceOS Module v12.0.0

## File Naming Conventions

### Documentation Files
| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-VOS-{Desc}-V#.md` | `LD-VOS-Module-State-V1.md` |
| Specs | `VOS-Spec-{Feature}-YDDMM-V#.md` | `VOS-Spec-VoiceCommands-51212-V1.md` |
| Plans | `VOS-Plan-{Feature}-YDDMM-V#.md` | `VOS-Plan-Accessibility-51212-V1.md` |

### Code Files
| Type | Pattern | Example |
|------|---------|---------|
| Kotlin | `{Module}{Component}.kt` | `VoiceOSAccessibilityService.kt` |
| Android XML | `{type}_{name}.xml` | `activity_main.xml` |

### Configuration Files
| Type | Pattern | Location |
|------|---------|----------|
| Module Config | `config.idc` | `.ideacode/config.idc` |
| Gradle | `build.gradle.kts` | Module root |

## Prohibited Patterns

❌ `VoiceOS-VoiceOS-*.md` (redundant module name)
❌ `*.json` for config (use .idc format)
❌ Spaces in filenames

---

Updated: 2025-12-15 | Version: 12.0.0
