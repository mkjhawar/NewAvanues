---
description: Project operations (instructions, update, validate) | /project
---

# /i.project - Project Operations

## Usage
`/project [operation] [scope] [modifiers]`

## Arguments
| Arg | Description |
|-----|-------------|
| operation | INSTRUCTIONS, UPDATE, VALIDATE, or INIT |
| scope | MODULE or PROJECT (default: PROJECT) |
| target | Module name (if scope=MODULE) |

## Flags
| Flag | Effect |
|------|--------|
| `.yolo` | Auto-execute operations |
| `.swarm` | Multi-agent validation |
| `.cot` | Show reasoning |
| `.global` | Apply globally |
| `.project` | Apply to project |

## Operations
| Operation | Purpose | Auto-Detects |
|-----------|---------|--------------|
| INSTRUCTIONS | View project docs (default) | Smart selection |
| UPDATE | Update IDEACODE to v10.0 | Version check |
| VALIDATE | Check compliance | Health score |
| INIT | Initialize IDEACODE | Project type |

## Intelligence Layer (Pre-Flight)

Runs project health check before any operation:

| Detection | Score | Action |
|-----------|-------|--------|
| No IDEACODE | 50/100 | Suggest INIT |
| Outdated (8.x) | 70/100 | Suggest UPDATE |
| Compliance issues | 60/100 | Suggest VALIDATE |
| Healthy | 80+/100 | Show INSTRUCTIONS |

**Health Metrics:**
- IDEACODE presence: 50 pts
- Version currency: 20 pts
- File completeness: 10 pts each
- Config validity: 5 pts each

## Operation: INSTRUCTIONS

**Display project configuration and guidelines**

### Steps:
1. Read `.claude/CLAUDE.md`
2. Display project type and version
3. Show IDEACODE configuration
4. List applicable design standards
5. Display programming standards (context-aware)
6. Show quick command reference
7. Offer next steps

### Files Scanned:
- `.claude/CLAUDE.md` (primary)
- `.claude/README.md`
- `.ideacode/README.md`
- `CONTRIBUTING.md`
- `docs/DEVELOPMENT.md`
- `design-standards/`

## Operation: UPDATE

**Migrate to latest IDEACODE (v10.0)**

### Phases:
1. **Version Check**
   - Read current version from config
   - Compare with v10.0
   - Display changelog

2. **Pre-Update Validation**
   - Git status clean?
   - Backups available?
   - Active branches listed?

3. **Create Backups**
   - `cp -r .claude .claude.backup.YYYYMMDDHHMMSS`
   - `cp -r .ideacode .ideacode.backup.YYYYMMDDHHMMSS`

4. **Sync Files**
   - Command files (40 files)
   - Library code (lib/)
   - Templates
   - Settings.json
   - Statusline script

5. **Migration**
   - Replace deprecated commands
   - Update scripts
   - Validate new command syntax

6. **Post-Update**
   - Update version number
   - Test commands (`/help`)
   - Commit changes

### Breaking Changes (v8 → v9):
- `/analyzecode` → `/analyze .code`
- `/analyzeui` → `/analyze .ui`
- `/reviewapp` → `/review app`

## Operation: VALIDATE

**Check IDEACODE compliance and health**

### Validation Checks:
| Check | Critical | Auto-Fix |
|-------|----------|----------|
| IDEACODE exists | YES | Create via INIT |
| Version current | YES | Run UPDATE |
| Config valid YAML | YES | Fix syntax |
| Files present | YES | Create missing |
| Scripts executable | NO | chmod +x |
| Command format | YES | Fix syntax |
| Git initialized | YES | Run `git init` |
| Standards loaded | NO | Create defaults |

### Health Score Calculation:
```
Base: 100
- IDEACODE missing: -50
- Version outdated: -20
- Each missing file: -10
- Each config error: -5
- Standards missing: -5
Result: 0-100
```

### Execution:
1. Check file structure
2. Validate configuration (YAML)
3. Verify command files
4. Test scripts (executable?)
5. Validate statusline
6. Check dependencies
7. Verify design standards
8. Generate compliance score

## Operation: INIT

**Initialize IDEACODE in new project**

### Prerequisites:
- Git repository initialized
- Project has code files
- Root directory accessible

### Auto-Detection:
```
build.gradle.kts   → android-app
package.json       → frontend-web
Cargo.toml         → rust
go.mod             → go
Multiple modules   → monorepo
```

### Steps:
1. Create directory structure:
   - `.claude/commands/` `.claude/lib/` `.claude/templates/`
   - `.ideacode/design-standards/`
   - `specs/` `docs/` `scripts/` `design-standards/`

2. Copy framework files:
   - All command files (28+)
   - Library code
   - Templates
   - Configuration

3. Generate config:
   ```yaml
   project:
     name: "{detected}"
     type: "{detected}"
     ideacode_version: "9.0"
   ```

4. Configure MCP server
5. Update .gitignore
6. Create README.md
7. Validate installation

## Scope Levels
| Scope | Target | Use Case |
|-------|--------|----------|
| PROJECT (default) | Entire repo | Global changes |
| MODULE | Single app/module | Localized docs |

## Examples
| Command | Behavior |
|---------|----------|
| `/project` | Show instructions (auto-detected) |
| `/project update .yolo` | Auto-update to v10.0, auto-commit |
| `/project validate .swarm` | Multi-agent validation report |
| `/project init .yolo` | Auto-detect type, initialize |
| `/project instructions module webavanue` | Module-specific docs |

## Swarm Mode (.swarm)
Spawns specialized agents:
1. Configuration Specialist
2. Structure Specialist
3. Standards Specialist
4. Quality Specialist

Aggregates findings into comprehensive report.

## YOLO Mode (.yolo)
**For UPDATE:**
- Auto-create backups
- Auto-update all files
- Auto-migrate configurations
- Auto-commit changes

**For VALIDATE:**
- Auto-fix fixable issues
- Auto-update outdated files
- Auto-create missing directories

**For INIT:**
- Auto-detect project type
- Auto-generate configurations
- Auto-commit setup

## Error Handling
```
Not a git repo?
→ Error: "Initialize git first: git init"

IDEACODE not installed?
→ Error: "Run: /project init"

Module not found?
→ Error: "Module not found. Available: {list}"

No internet (UPDATE)?
→ Error: "No internet connection"
```

## Related Commands
| Command | Use When |
|---------|----------|
| `/scan` | Project structure analysis |
| `/analyze` | Code quality check |
| `/review` | Code/PR review |

## Quality Gates
- Git working directory clean
- All operations logged
- Configuration valid
- Backups created (UPDATE)
- Tests passing (validation)

---

**Version:** 9.0
**Consolidates:** /projectinstructions, /projectupdate, /scanproject
