# File Registry - Cockpit Module v12.0.0

## File Naming Conventions

### Documentation Files
| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-CPT-{Desc}-V#.md` | `LD-CPT-Module-State-V1.md` |
| Specs | `CPT-Spec-{Feature}-YDDMM-V#.md` | `CPT-Spec-Management-51212-V1.md` |
| Plans | `CPT-Plan-{Feature}-YDDMM-V#.md` | `CPT-Plan-Dashboard-51212-V1.md` |

### Code Files
| Type | Pattern | Example |
|------|---------|---------|
| Kotlin | `{Component}.kt` | `ModuleManager.kt` |
| Configuration | `{Config}.kt` | `SystemConfig.kt` |
| Plugins | `{Plugin}Plugin.kt` | `MonitoringPlugin.kt` |

### Configuration Files
| Type | Pattern | Location |
|------|---------|----------|
| Module Config | `config.idc` | `.ideacode/config.idc` |
| Gradle | `build.gradle.kts` | Module root |

## Prohibited Patterns

❌ `Cockpit-Cockpit-*.md` (redundant module name)
❌ `*.json` for config (use .idc format)
❌ Spaces in filenames

---

Updated: 2025-12-15 | Version: 12.0.0
