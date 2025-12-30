---
description: Project scanning (structure, deps, todos, progress) | /i.scan
---

# /i.scan - Project Scanner

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3847/i.scan`
Auto-start: API server starts automatically if not running

---


## Usage
`/i.scan [.type] [target] [.modifiers]`

## Types
| Type | Purpose | Output |
|------|---------|--------|
| `.structure` | Project organization | Directory tree, metrics |
| `.deps` | Dependencies | Versions, CVEs, conflicts |
| `.todos` | TODO/FIXME inventory | Categorized, prioritized |
| `.progress` | Work status | Specs, plans, blockers |
| `.debug` | Debug mode scan | Detailed analysis |

## Scope
| Scope | Example |
|-------|---------|
| file | `src/Auth.kt` |
| folder | `src/auth/` |
| module | `module webavanue` |
| project | entire repo |

## Modifiers
| Modifier | Effect |
|----------|--------|
| `.swarm` | Multi-agent scanning |
| `.cot` | Show reasoning |
| `.backlog` | Export to backlog |

## Structure Scan Output
```
## Project Structure: {name}

### Metrics
| Metric | Value |
|--------|-------|
| Files | 234 |
| Lines | 45,678 |
| Modules | 8 |

### Architecture
├── android/
├── common/
├── docs/
└── ...
```

## Dependency Scan Output
```
## Dependencies: {name}

### Security
| Package | CVE | Severity |
|---------|-----|----------|

### Updates Available
| Package | Current | Latest |
|---------|---------|--------|

### Conflicts
| Package | Versions |
|---------|----------|
```

## TODO Scan Output
```
## TODOs: {name}

### By Priority
| Priority | Count |
|----------|-------|
| Critical | 3 |
| High | 12 |
| Medium | 28 |

### By Age
| Age | Count |
|-----|-------|
| >30 days | 15 |
| >7 days | 8 |
| Recent | 20 |
```

## Progress Scan Output
```
## Progress: {name}

### Specs
| Spec | Status |
|------|--------|

### Tasks
| Task | Status |
|------|--------|

### Blockers
- {blocker}
```

## Examples
| Command | Result |
|---------|--------|
| `/i.scan` | Auto-detect needs |
| `/i.scan .structure` | Project structure |
| `/i.scan .deps` | Dependency audit |
| `/i.scan .todos` | TODO inventory |
| `/i.scan .progress` | Work status |
| `/i.scan .debug` | Debug mode scan |

## Related
| Command | Purpose |
|---------|---------|
| `/ireview` | Code review |
| `/ianalyze` | Deep analysis |

## Metadata
- **Command:** `/i.scan`
- **Version:** 10.2
