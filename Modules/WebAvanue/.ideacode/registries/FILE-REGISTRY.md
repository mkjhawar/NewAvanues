# File Registry - WebAvanue Module v12.0.0

## File Naming Conventions

### Documentation Files
| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-WEB-{Desc}-V#.md` | `LD-WEB-Module-State-V1.md` |
| Specs | `WEB-Spec-{Feature}-YDDMM-V#.md` | `WEB-Spec-Dashboard-51212-V1.md` |
| Plans | `WEB-Plan-{Feature}-YDDMM-V#.md` | `WEB-Plan-UI-51212-V1.md` |

### Code Files
| Type | Pattern | Example |
|------|---------|---------|
| React Components | `{Component}.tsx` | `Dashboard.tsx` |
| Pages | `{Page}Page.tsx` | `DashboardPage.tsx` |
| Services | `{Service}Service.ts` | `ApiService.ts` |
| Utilities | `{Util}.ts` | `formatDate.ts` |

### Configuration Files
| Type | Pattern | Location |
|------|---------|----------|
| Module Config | `config.idc` | `.ideacode/config.idc` |
| Package | `package.json` | Module root |
| TypeScript | `tsconfig.json` | Module root |

## Prohibited Patterns

❌ `WebAvanue-WebAvanue-*.md` (redundant module name)
❌ `*.json` for config (use .idc format for IDEACODE configs)
❌ Spaces in filenames

---

Updated: 2025-12-15 | Version: 12.0.0
