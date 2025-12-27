---
description: Repository management (new, init, migrate, add, validate, cleanup) | /i.repo .init | /i.repo .new "ProjectName" | /i.repo .cleanup
---

# /i.repo - Unified Repository Management

## Usage
`/i.repo [.operation] [target] [.modifiers]`

## Operations

| Operation | Purpose |
|-----------|---------|
| `.init` | Initialize existing directory with IDEACODE structure |
| `.new` | Create fresh monorepo with full structure |
| `.migrate` | Flexible migration (import/export/upgrade) |
| `.sync` | Sync/merge changes between branches or worktrees |
| `.add` | Add module/app to existing monorepo |
| `.validate` | Health check on structure |
| `.fix` | Fix misplaced files and structure violations |
| `.cleanup` | Clean bloat (cache, logs, context >7 days) |
| `.list` | List modules/apps |

---

## .init - Initialize Repository

### Usage
`/i.repo .init [path] [.type <repo-type>]`

### Purpose
Initialize an existing directory with full IDEACODE structure. Perfect for:
- Converting existing projects to IDEACODE
- Setting up git worktrees
- Adding IDEACODE to cloned repositories

### Questions Asked
| # | Question | Options | Default |
|---|----------|---------|---------|
| 1 | Repository type? | android-app, ios-app, web-app, monorepo, library | android-app |
| 2 | Path to initialize? | Directory path | Current directory |

### Structure Created
```
{RepositoryPath}/
├── .git/                    # Git initialized if not exists
│   └── hooks/
│       └── pre-commit → .claude/hooks/pre-commit-compliance.sh
├── .claude/
│   ├── CLAUDE.md           # Project instructions
│   ├── hooks/
│   │   ├── pre-commit-compliance.sh
│   │   └── memory-manager.sh
│   ├── agents/
│   │   └── compliance-verifier.md
│   ├── settings.json       # Statusline config
│   └── statusline-command.sh
├── .ideacode/
│   ├── config.idc          # Project config (IDC format)
│   ├── living-docs/
│   └── registries/
└── .gitignore
```

### Special Handling

**Git Worktrees:**
- Detects worktree structure automatically
- Creates local .claude/ and .ideacode/
- Hooks managed by parent repository
- Preserves existing branch

**Regular Repositories:**
- Initializes git if not exists
- Creates pre-commit hook symlink
- Makes initial commit on main
- Switches to feature/setup branch

### Process
1. ✅ Detect git structure (regular repo or worktree)
2. ✅ Create .claude/ structure with hooks
3. ✅ Create .ideacode/ with config.idc
4. ✅ Generate CLAUDE.md with repository name
5. ✅ Install compliance system (hooks + agents)
6. ✅ Deploy memory manager
7. ✅ Deploy statusline
8. ✅ Create .gitignore if missing
9. ✅ Make initial commit (regular repos only)

### Examples
| Command | Result |
|---------|--------|
| `/i.repo .init` | Initialize current directory (interactive) |
| `/i.repo .init .` | Initialize current directory |
| `/i.repo .init /path/to/repo` | Initialize specific path |
| `/i.repo .init /path/to/repo .type monorepo` | Initialize as monorepo |
| `/i.repo .init .type web-app` | Initialize current dir as web-app |

### After Initialization

**Next Steps Shown:**
```
✅ Repository initialized successfully!

Next steps:
1. Add to PROJECT-REGISTRY.json (if not already)
2. Restart Claude Code to load configuration
3. Work on feature branches (not main)
```

### Integration
- Runs: `scripts/init-repo.sh` internally
- Updates: PROJECT-REGISTRY.json (if needed)
- Deploys: Compliance system to repository

---

## .new - Create Fresh Monorepo

### Usage
`/i.repo .new "ProjectName" [.platforms <list>]`

### Questions Asked
| # | Question | Options |
|---|----------|---------|
| 1 | Platforms? | android, ios, web, backend (multi-select) |
| 2 | KMP shared code? | Y/n (if android+ios selected) |
| 3 | Initialize git? | Y/n |
| 4 | Copy global commands or symlink? | copy/symlink |

### Structure Created
```
{ProjectName}/
├── .claude/
│   ├── CLAUDE.md
│   ├── commands/           # Symlink or copy
│   ├── skills/
│   └── settings.json
├── .ideacode/
│   ├── config.yml
│   ├── registries/
│   │   ├── FILE-REGISTRY.md
│   │   └── FOLDER-REGISTRY.md
│   └── living-docs/
├── android/                 # If platform selected
│   ├── apps/
│   └── libs/
├── ios/                     # If platform selected
│   ├── apps/
│   └── libs/
├── web/                     # If platform selected
│   ├── apps/
│   └── libs/
├── backend/                 # If platform selected
├── common/                  # KMP shared code (if enabled)
├── Docs/
│   ├── Specs/
│   ├── Plans/
│   └── Manuals/
├── scripts/
├── PROJECT-REGISTRY.json
├── .gitignore
└── README.md
```

### Examples
| Command | Result |
|---------|--------|
| `/i.repo .new "MyApp"` | Interactive wizard |
| `/i.repo .new "MyApp" .platforms android,ios` | Android + iOS monorepo |
| `/i.repo .new "MyApp" .platforms android,ios,web,backend` | Full stack monorepo |

---

## .migrate - Flexible Migration

### Usage
`/i.repo .migrate [.scenario] [path]`

### Initial Questions (if no scenario modifier)
```
What would you like to do?

1. Import another repo into this one (.importrepo)
   → Bring external repo here, upgrade if needed

2. Export this repo to another monorepo (.exportrepo)
   → Move this project into an existing monorepo

3. Upgrade this repo to be a monorepo (.upgraderepo)
   → In-place upgrade with full structure

Select (1/2/3): _
```

### Then Always Ask
```
→ Would you like to see a dry-run preview first? (Y/n): _
```

### Scenario Modifiers
| Modifier | Action | Next Question |
|----------|--------|---------------|
| `.importrepo` | Bring another repo INTO this one | "Which repo to import? (path)" |
| `.exportrepo` | Migrate this repo TO another monorepo | "Which monorepo? (path)" |
| `.upgraderepo` | Upgrade THIS repo to be a monorepo | "Confirm upgrade? (Y/n)" |

---

## Migration Phases

### Phase 1: Analysis + Tracking Table

```
Analyzing repository...

## Migration Tracking Table

| # | File | Current Path | New Path | Action | Status |
|---|------|--------------|----------|--------|--------|
| 1 | readme.md | /docs/readme.md | /Docs/README.md | Rename + Move | Pending |
| 2 | auth-spec.md | /specs/auth.md | /Docs/Specs/Auth-Spec-V1.md | Rename + Move | Pending |
| 3 | UserService.kt | /src/UserService.kt | /android/apps/.../UserService.kt | Move | Pending |
| ... | ... | ... | ... | ... | ... |

Total: 234 files | Renames: 45 | Moves: 120 | Converts: 12 | Unchanged: 57
```

**→ ASK: Proceed with Phase 2?**

### Phase 2: Document Fixes
| Action | Description |
|--------|-------------|
| Fix folder structure | Docs/, Specs/, Plans/ |
| Fix document naming | App-Module-Desc-YDDMMHH-V#.md |
| Update tracking table | Mark completed items |

**→ ASK: Apply document fixes?**

### Phase 3: Code Structure Fixes
| Action | Description |
|--------|-------------|
| Move code to platform folders | android/, ios/, web/, backend/ |
| Extract common code | common/ for KMP |
| Update tracking table | Mark completed items |

**→ ASK: Apply code structure fixes?**

### Phase 4: Reference Updates (Comprehensive)
| Action | Description |
|--------|-------------|
| Update imports/paths | All file references |
| Update package names | com.old.* → com.new.* |
| Update namespaces | Kotlin/Swift namespaces |
| Update build files | gradle, package.json, etc. |
| Add missed files to table | Double-check logic |

**→ ASK: Apply reference updates?**

### Phase 5: Copy/Move Execution
| Action | Description |
|--------|-------------|
| Execute file operations | Based on tracking table |
| Verify each file | Check against table |
| Add any missed files | Update table if found |

**→ ASK: Confirm destination?**

### Phase 6: Verification
| Check | Description |
|-------|-------------|
| Run all tests | Ensure nothing broken |
| Check all builds | gradle, npm, etc. |
| Verify references | No broken imports |
| Report results | Pass/fail summary |

### Phase 7: Finalize
| Action | Description |
|--------|-------------|
| Update registries | FILE-REGISTRY, FOLDER-REGISTRY |
| Create CLAUDE.md | If missing |
| Create .ideacode/ | Full structure |
| Update PROJECT-REGISTRY.json | Add new entry |

**→ ASK: Commit changes?**

---

## Tracking Table Format

| Column | Description |
|--------|-------------|
| # | Sequential ID |
| File | Filename |
| Current Path | Where it is now |
| New Path | Where it should be |
| Action | Rename / Move / Convert / Update refs / None |
| Status | Pending / In Progress / ✓ Done / ⚠ Error / **Added** |

### Double-Check Logic
```
For each file operation:
1. Check if file is in tracking table
   ├── YES → Execute planned action, mark ✓ Done
   └── NO →
       ├── Add to table with "Added" flag
       ├── Analyze what action is needed
       ├── Execute action
       └── Mark ✓ Done

2. After action, verify:
   ├── File exists at new location
   ├── References updated in dependent files
   └── No broken imports/paths
```

---

## .sync - Sync/Merge Branches

### Usage
`/i.repo .sync <source-branch> [<target-branch>] [.modifiers]`

### Purpose
Sync changes between branches or worktrees in a monorepo. Perfect for:
- Merging feature branches into development
- Syncing worktree changes to main branch
- Keeping multiple development branches in sync

### Parameters
| Parameter | Description | Required | Default |
|-----------|-------------|----------|---------|
| `source-branch` | Branch to merge FROM | Yes | - |
| `target-branch` | Branch to merge TO | No | Current branch |

### Questions Asked
| # | Question | Options | When |
|---|----------|---------|------|
| 1 | Target branch? | List of branches | If not specified |
| 2 | Merge strategy? | merge, rebase, squash | Always |
| 3 | Preview diff first? | Y/n | Always |
| 4 | Proceed with sync? | Y/n | After preview |

### Process

**Phase 1: Validation**
```
Checking branches...
✅ Source: Cockpit-Development (45 commits ahead)
✅ Target: NewAvanues-Development (12 commits ahead)
⚠️  Potential conflicts: 3 files

Conflicts:
- src/common/Config.kt (both modified)
- build.gradle.kts (both modified)
- README.md (both modified)
```

**Phase 2: Preview**
```
Changes to sync from Cockpit-Development → NewAvanues-Development:

Files Changed: 23
  • Added: 8 files
  • Modified: 12 files
  • Deleted: 3 files

Key changes:
  + src/cockpit/SpatialUI.kt (new)
  + src/cockpit/GazeController.kt (new)
  ~ src/common/Config.kt (modified)
  ~ build.gradle.kts (modified)
  - src/old/Legacy.kt (deleted)
```

**Phase 3: Execution**
| Strategy | Action |
|----------|--------|
| `merge` | `git merge <source> --no-ff` (preserves history) |
| `rebase` | `git rebase <source>` (linear history) |
| `squash` | `git merge <source> --squash` (single commit) |

**Phase 4: Conflict Resolution** (if needed)
```
Conflicts detected in 3 files:

1. src/common/Config.kt
   <<<<<<< HEAD (NewAvanues-Development)
   val API_URL = "https://api-dev.example.com"
   =======
   val API_URL = "https://api-cockpit.example.com"
   >>>>>>> Cockpit-Development

   Options:
   a) Keep current (NewAvanues-Development)
   b) Use incoming (Cockpit-Development)
   c) Edit manually
   d) Skip file

   Choose (a/b/c/d): _
```

**Phase 5: Completion**
```
✅ Sync completed successfully!

Summary:
  • 20 files merged cleanly
  • 3 conflicts resolved
  • Merge commit: abc123f

Next steps:
  1. Run tests to verify
  2. Review changes: git log --oneline -10
  3. Push: git push origin NewAvanues-Development
```

### Modifiers
| Modifier | Effect |
|----------|--------|
| `.merge` | Use merge strategy (default) |
| `.rebase` | Use rebase strategy |
| `.squash` | Squash all commits into one |
| `.nopreview` | Skip diff preview |
| `.yolo` | Auto-resolve conflicts (use incoming) |
| `.dryrun` | Show what would happen |

### Safety Features
- ✅ Validates both branches exist
- ✅ Checks for uncommitted changes (blocks if dirty)
- ✅ Shows preview before merging
- ✅ Detects conflicts early
- ✅ Interactive conflict resolution
- ✅ Can abort at any step

### Examples
| Command | Result |
|---------|--------|
| `/i.repo .sync Cockpit-Development` | Merge into current branch |
| `/i.repo .sync Cockpit-Development NewAvanues-Development` | Merge Cockpit → NewAvanues |
| `/i.repo .sync feature/auth main .squash` | Squash merge to main |
| `/i.repo .sync hotfix/bug production .merge` | Merge hotfix to production |
| `/i.repo .sync dev staging .dryrun` | Preview sync (no changes) |

### Worktree Support
For git worktrees (like NewAvanues-Cockpit):
```bash
# Sync Cockpit worktree changes to main development
/i.repo .sync Cockpit-Development NewAvanues-Development

# Or from within Cockpit worktree
cd /Volumes/M-Drive/Coding/NewAvanues-Cockpit
/i.repo .sync . NewAvanues-Development
```

### Common Workflows

**Feature → Development:**
```bash
# After completing feature in Cockpit
/i.repo .sync Cockpit-Development NewAvanues-Development .squash
```

**Development → Production:**
```bash
# Release to production
/i.repo .sync NewAvanues-Development main .merge
```

**Hotfix → All Branches:**
```bash
# Apply hotfix everywhere
/i.repo .sync hotfix/critical-bug NewAvanues-Development
/i.repo .sync hotfix/critical-bug Cockpit-Development
/i.repo .sync hotfix/critical-bug main
```

### Error Handling
| Error | Cause | Solution |
|-------|-------|----------|
| "Branch not found" | Invalid branch name | Check: `git branch -a` |
| "Working tree dirty" | Uncommitted changes | Commit or stash first |
| "Merge conflicts" | Overlapping changes | Resolve interactively |
| "Already up to date" | No new commits | Nothing to sync |

---

## .add - Add Module

### Usage
`/i.repo .add "ModuleName"`

### Questions Asked
| # | Question | Options |
|---|----------|---------|
| 1 | Module type? | app / lib / package |
| 2 | Platform? | android / ios / web / backend / common |
| 3 | Description? | Free text |

### Structure Created
```
{platform}/{type}s/{ModuleName}/
├── .claude/
│   └── CLAUDE.md
├── src/
│   ├── main/
│   └── test/
├── build.gradle.kts (or package.json)
└── README.md
```

### Examples
| Command | Result |
|---------|--------|
| `/i.repo .add "auth"` | Interactive wizard |
| `/i.repo .add "auth-service" .type lib .platform android` | Android library |

---

## .validate - Health Check

### Usage
`/i.repo .validate [target]`

### Checks Performed
| Check | Description |
|-------|-------------|
| Folder structure | Compliance with monorepo standard |
| Document naming | App-Module-Desc-YDDMMHH-V#.md |
| Registry accuracy | Files match registries |
| CLAUDE.md presence | All modules have instructions |
| Broken references | Import/path verification |

### Output
```
## Repository Health: {name}

| Metric | Score |
|--------|-------|
| Structure | 85/100 |
| Naming | 72/100 |
| Registries | 90/100 |
| References | 95/100 |
| **Overall** | **85/100** |

### Violations Found
| # | Issue | Location | Severity |
|---|-------|----------|----------|
| 1 | Wrong naming | docs/readme.md | Medium |
| 2 | Missing CLAUDE.md | android/apps/auth/ | High |
```

---

## .fix - Fix Structure Issues

### Usage
`/i.repo .fix [target]`

### Process
1. Run `.validate` checks
2. Show violations table
3. **ASK: Fix these issues? (Y/n)**
4. Execute fixes with tracking table
5. Re-validate and report

---

## .cleanup - Clean Bloat

### Usage
`/i.repo .cleanup [.modifiers]`

### Purpose
Clean cache, logs, and transient state from `.claude/` directories across all repos.
Based on Anthropic best practices: preserve configuration, delete cache/logs/transient state.

### Modifiers
| Modifier | Target |
|----------|--------|
| `.claude` | .claude directories (default) |
| `.tmp` | Temp files |
| `.context` | Context archives |
| `.backups` | Old backups |
| `.all` | Everything |
| `.dryrun` | Preview only (no changes) |
| `.force` | Skip confirmations |

### What Gets Cleaned

**Safe Auto-Delete (with backup):**
- `~/.claude/debug/` → 2.3GB (cache)
- `~/.claude/file-history/` → 201MB (cache)
- `~/.claude/history.jsonl` → 5MB (logs)
- `~/.claude/shell-snapshots/` → 1.6MB (transient)
- `~/.claude/telemetry/`, `downloads/`, `ide/` (if exist)
- `{repo}/contextsave/*.md` → Files >7 days old only

**Always Preserved:**
- `settings.json`, `settings.local.json`, `CLAUDE.md`
- `.instruction_checksums_*`, `.ideacode_*` tracking files
- `mcp.json`, `statusline-command.sh`
- Context logs <7 days old (recent work)

**Review Recommended:**
- `~/.claude/projects/` (may contain data)
- `~/.claude/todos/` (active lists)

### 7-Day Context Retention
Context logs in `{repo}/contextsave/` are scanned across all repos:
- Files <7 days: **PRESERVED** (recent context)
- Files >7 days: **AUTO-DELETED** (with backup)

### Workflow
1. **Discovery:** Scan all repos in `/Volumes/M-Drive/Coding/`
2. **Analysis:** Categorize files by safety (safe/review/preserve)
3. **Backup:** Create timestamped backup before deletion
4. **Deletion:** Remove safe items only
5. **Report:** Show recovery size and review recommendations

### Examples
```bash
# Preview what will be cleaned
/i.repo .cleanup .dryrun

# Clean .claude directories (default)
/i.repo .cleanup

# Skip confirmations
/i.repo .cleanup .force

# Clean everything
/i.repo .cleanup .all
```

### Safety Features
- Timestamped backups: `~/.claude-cleanup-backup-YYYYMMDD-HHMMSS/`
- Confirmation prompts (unless `.force`)
- Detailed log: `~/.claude/cleanup-log.txt`
- Preserves recent context (7-day retention)

### Test Results
```
Found: 20 repositories
Safe to delete: 2.5GB (6 items)
  • Debug logs: 2GB
  • File versions: 201MB
  • Conversation log: 5MB
  • Shell states: 1MB
  • Old context logs: 1MB (>7 days)

Review: 2.1GB (projects + todos)
Recovered: 2.5GB (54% reduction)
```

---

## .list - List Modules

### Usage
`/i.repo .list [.filter]`

### Filters
| Filter | Description |
|--------|-------------|
| `.apps` | Apps only |
| `.libs` | Libraries only |
| `.platform android` | Android modules only |

### Output
```
## Modules in {repo}

| Module | Type | Platform | Path | Status |
|--------|------|----------|------|--------|
| main | app | android | android/apps/main/ | Active |
| auth | lib | common | common/auth/ | Active |
| web-app | app | web | web/apps/main/ | Active |

Total: 8 modules (3 apps, 5 libs)
```

---

## All Modifiers

### Operation Modifiers
| Modifier | Effect |
|----------|--------|
| `.init` | Initialize existing directory |
| `.new` | Create fresh monorepo |
| `.migrate` | Start migration flow |
| `.sync` | Sync/merge branches |
| `.add` | Add module |
| `.validate` | Health check |
| `.fix` | Fix structure issues |
| `.cleanup` | Clean bloat |
| `.list` | List modules |

### Migration Scenario Modifiers
| Modifier | Effect |
|----------|--------|
| `.importrepo` | Bring another repo into this one |
| `.exportrepo` | Export this repo to another monorepo |
| `.upgraderepo` | Upgrade this repo to be a monorepo |

### Behavior Modifiers
| Modifier | Effect |
|----------|--------|
| `.yolo` | Auto-approve all phases |
| `.resume` | Continue from last checkpoint |
| `.skip-docs` | Don't fix document naming |
| `.skip-code` | Don't update code references |

### Filter Modifiers (for .list)
| Modifier | Effect |
|----------|--------|
| `.apps` | Show apps only |
| `.libs` | Show libraries only |
| `.platform <name>` | Filter by platform |

---

## Examples

| Command | Result |
|---------|--------|
| `/i.repo .init` | Initialize current directory |
| `/i.repo .init /path/to/repo .type monorepo` | Initialize specific repo |
| `/i.repo .new "MyApp"` | Create new monorepo (wizard) |
| `/i.repo .migrate` | Migration wizard (asks scenario) |
| `/i.repo .migrate .importrepo /path/to/repo` | Import repo directly |
| `/i.repo .migrate .upgraderepo` | Upgrade current repo |
| `/i.repo .add "auth"` | Add module (wizard) |
| `/i.repo .validate` | Health check |
| `/i.repo .fix` | Fix structure issues |
| `/i.repo .cleanup` | Clean bloat (cache/logs) |
| `/i.repo .cleanup .dryrun` | Preview cleanup |
| `/i.repo .list` | List all modules |
| `/i.repo .list .apps .platform android` | List Android apps |

---

## MCP Integration
Uses: `ideacode_fs`, `ideacode_validate`, `ideacode_monorepo`, `ideacode_context`

## Consolidates
- `/imonorepo` → `/i.repo .list`, `/i.repo .add`, `/i.repo .fix`
- `/imigrate` → `/i.repo .migrate .importrepo`
- `/inew` → `/i.repo .new`
- `/i.cleanup` → `/i.repo .cleanup`

## Related
| Command | Purpose |
|---------|---------|
| `/iproject` | Project operations (not repo structure) |
| `/iscan` | Project scanning |

## Metadata
- **Command:** `/i.repo`
- **Version:** 10.2
- **Consolidates:** /imonorepo, /imigrate, /inew
