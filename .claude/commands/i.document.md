---
description: Documentation generation | /i.document .all
---

# /i.document - Documentation Generator

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3847/i.document`
Auto-start: API server starts automatically if not running

---


## Usage
`/i.document [.type] [target] [.modifiers]`

## Types
| Type | Generates | Time |
|------|-----------|------|
| `.all` | All documentation | 20 min |
| `.manuals` | User + developer manuals | 15 min |
| `.dev` | Architecture, modules | 5 min |
| `.user` | Feature guides, quick ref | 4 min |
| `.api` | Class/function/endpoint docs | 3 min |
| `.logs` | Changelog, issues, TODOs | 2 min |
| `.status` | Progress, roadmap | 2 min |
| `.code` | KDoc/JSDoc inline comments | 6 min |

## Scope
| Scope | Example |
|-------|---------|
| file | `src/AuthService.kt` |
| folder | `src/auth/` |
| module | `module webavanue` |
| project | entire repository |

## Modifiers
| Modifier | Effect |
|----------|--------|
| `.yolo` | Auto-generate and commit |
| `.swarm` | Multi-agent (tech + user) |
| `.cot` | Show reasoning |
| `.debug` | Verbose output |

## Output Locations
| Type | Location |
|------|----------|
| User manual | `docs/manuals/user/` |
| Dev manual | `docs/manuals/developer/` |
| API docs | `docs/api/` |
| Architecture | `docs/architecture/` |
| Changelog | `CHANGELOG.md` |

## Detection Logic
| Condition | Action |
|-----------|--------|
| No README | Generate README.md |
| Code changes > docs | Update affected docs |
| New public API | Generate API docs |
| New module | Generate module docs |

## Examples
| Command | Result |
|---------|--------|
| `/i.document` | Interactive (detect needs) |
| `/i.document .all` | Full documentation suite |
| `/i.document .api src/` | API docs for src/ |
| `/i.document .manuals .yolo` | Generate and commit manuals |
| `/i.document .dev module auth` | Dev docs for auth module |

## Manual Structure
```
docs/manuals/
├── user/
│   ├── getting-started.md
│   ├── features/
│   └── troubleshooting.md
└── developer/
    ├── architecture.md
    ├── api/
    ├── setup.md
    └── contributing.md
```

## Quality Checks
| Check | Requirement |
|-------|-------------|
| Links | All internal links valid |
| Code examples | All examples compile |
| Coverage | Public APIs documented |
| Freshness | Matches current code |

## Related
| Command | Purpose |
|---------|---------|
| `/iscan .docs` | Scan doc coverage |
| `/ireview .docs` | Review documentation |

## Metadata
- **Command:** `/i.document`
- **Version:** 10.2
