---
description: IDEACODE v10 - Show all commands, modifiers, and instructions | /ideacode
allowed-tools: Read
---

# IDEACODE v10.0

Intelligent Devices Enhanced Architecture - AI-Powered Development Framework

---

## All Commands (API-Based - 97% Token Savings)

**All commands use IDEACODE API** - Auto-starts at http://localhost:3847

### Core Workflow

| Command | Modifiers | Description |
|---------|-----------|-------------|
| `/i.develop` | `.yolo` `.swarm` `.tdd` `.ood` `.profile` | Full development workflow |
| `/i.fix` | `.yolo` `.swarm` `.tdd` `.tcr` | Bug fixing workflow |
| `/i.specify` | `.yolo` `.cot` `.tot` | Create feature specification |
| `/i.plan` | `.yolo` `.cot` `.tot` | Generate implementation plan |
| `/i.implement` | `.yolo` `.swarm` `.tdd` | Execute implementation |
| `/i.refactor` | `.tdd` `.ood` `.solid` | Code refactoring workflow |

### Analysis & Review

| Command | Modifiers | Description |
|---------|-----------|-------------|
| `/i.analyze` | `.code` `.ui` `.workflow` `.docs` `.swarm` `.cot` `.tot` | Universal code analysis |
| `/i.review` | `.code` `.docs` `.app` `.swarm` | Code/app review |
| `/i.scan` | - | Scan structure, deps, todos, progress |
| `/i.think` | `.cot` `.rot` `.tot` | Deep reasoning mode |
| `/i.research` | `.cot` | Web research on technologies |

### UI & Design

| Command | Modifiers | Description |
|---------|-----------|-------------|
| `/i.createui` | `.app` `.research` `.demo` | Intelligent UI creation |

### Repository & Project Management

| Command | Modifiers | Description |
|---------|-----------|-------------|
| `/irepo` | `.new` `.migrate` `.add` `.validate` `.cleanup` `.list` | Unified repository management |
| `/iproject` | - | Project operations (instructions, update, validate) |

**`/irepo` Migration Modifiers:**
- `.importrepo` - Bring another repo into this one
- `.exportrepo` - Export this repo to another monorepo
- `.upgraderepo` - Upgrade this repo to be a monorepo

**Deprecated:** `/imonorepo`, `/imigrate`, `/inew` → Use `/irepo` instead

### Documentation & Planning

| Command | Modifiers | Description |
|---------|-----------|-------------|
| `/document` | `.all` `.api` `.user` `.verbose` | Comprehensive documentation |
| `/idea` | `.verbose` | Business & technical planning (MBA-level) |
| `/issue` | `.cot` `.tot` | Create issue analysis document |
| `/checklist` | - | Generate custom feature checklist |
| `/iclarify` | - | Ask clarification questions for spec |
| `/principles` | - | Create/update project constitution |

### Multi-Agent & Advanced

| Command | Modifiers | Description |
|---------|-----------|-------------|
| `/swarm` | `.cot` | Multi-agent coordination |
| `/agent` | - | Create autonomous AI agents |
| `/iwiz` | `.swarm` `.tutor` `.cot` `.verbose` | Interactive guided development |

### Utility

| Command | Modifiers | Description |
|---------|-----------|-------------|
| `/research` | `.verbose` | Web research on technologies |
| `/reviewapp` | `.verbose` | App review with spec generation |
| `/itasks` | - | View/manage task list |
| `/progress` | - | Show development progress |
| `/registry` | - | File/folder registry operations |
| `/reload` | - | Reload IDEACODE context |

### Help & Reference

| Command | Modifiers | Description |
|---------|-----------|-------------|
| `/ideacode` | - | This reference (all commands) |
| `/modifiers` | - | Quick modifier reference |
| `/help` | - | Interactive help system |
| `/version` | - | Show IDEACODE version |
| `/update` | - | Check for framework updates |

---

## All Modifiers

### Behavior

| Modifier | Purpose | Works With |
|----------|---------|------------|
| `.auto` | AI chains commands/modifiers as needed | All |
| `.yolo` | Full automation (SAFEGUARDS enforced) | All |
| `.swarm` | Multi-agent parallel execution | develop, fix, wiz, implement, analyze |
| `.tutor` | AI explanations (learning mode) | wiz, develop |
| `.stop` | Disable workflow chaining | All |
| `.tcr` | Test && Commit \|\| Revert | fix, refactor, implement |
| `.advanced` | Force advanced analysis | fix, analyze |
| `.verbose` | Detailed output (steps, decisions, changes) | All |

### Reasoning

| Modifier | Purpose | Visibility |
|----------|---------|------------|
| `.reason` | Auto-select best thinking mode | Hidden |
| `.sreason` | Auto-select + show to user | Displayed |
| `.cot` | Chain of Thought (linear) | Hidden |
| `.scot` | Show Chain of Thought | Displayed |
| `.tot` | Tree of Thought (branching) | Hidden |
| `.stot` | Show Tree of Thought | Displayed |
| `.rot` | Reflection on Thinking | Hidden |
| `.srot` | Show Reflection on Thinking | Displayed |

### Persistence

| Modifier | Purpose | Auto-Invoked |
|----------|---------|--------------|
| `.persist` | Save context to continuity | Context >70% |
| `.resume` | Load previous session | Session start |
| `.deep` | Extended context retention | Multi-day tasks |
| `.checkpoint` | Create restoration point | Before risky ops |

### Capture

| Modifier | Purpose | Destination |
|----------|---------|-------------|
| `.global` | Universal standard | design-standards/ |
| `.project` | Project-specific | .ideacode/design-standards/ |
| `.test` | Generate test case | tests/ |
| `.security` | Security standard | design-standards/security/ |
| `.backlog` | Future work | backlog/ |
| `.docs` | Documentation | docs/ |

### UI Scope

| Modifier | Purpose |
|----------|---------|
| `.repo` | Repository-wide scope |
| `.app` | Application scope |
| `.module` | Module scope |
| `.update` | Update existing UI |
| `.native` | Platform native (Compose/SwiftUI) |
| `.magicui` | MagicUI cross-platform |

---

## MCP Tools (v10 - 23 Consolidated)

| Tool | Actions | Description |
|------|---------|-------------|
| `ideacode_fs` | create, rename, move, delete, validate, sync, search | File/folder ops with validation |
| `ideacode_specify` | - | Create specifications |
| `ideacode_plan` | - | Generate plans |
| `ideacode_implement` | - | Execute implementations |
| `ideacode_test` | - | Run tests |
| `ideacode_commit` | - | Git commits |
| `ideacode_validate` | - | Validate specs |
| `ideacode_vision` | analyze_ui, from_mockup, debug_screenshot | Vision/UI analysis |
| `ideacode_context` | reset, show, save | Context management |
| `ideacode_pattern` | checkpoint, learn, list | Pattern learning |
| `ideacode_constitution` | set, check, reset, add_rule, remove_rule, validate | Session rules |
| `ideacode_guided` | develop, progress, reset | Guided development |
| `ideacode_skill` | execute, list, get, delete, discover | Code skills |
| `ideacode_monorepo` | list, inspect, validate, deps | Monorepo management |
| `ideacode_hashtag` | capture, validate, list, detect, promote | Inline capture |
| `ideacode_research` | - | Web research |
| `ideacode_think` | - | Extended thinking |
| `ideacode_standards` | - | Load coding standards |

---

## Hard Rules

| Rule | Requirement |
|------|-------------|
| **Optimum Solution** | Always choose optimal approach |
| **Token Efficiency** | Batch ops, targeted reads, minimal output |
| **Compact Format** | Tables for reference, lists for steps |
| **No Emojis** | In code, commits, docs |
| **SOLID** | Enforce on all code |
| **90% Coverage** | On critical paths |
| **No Copy-Paste** | Use imports, never duplicate |
| **Branches** | Never commit to main |

---

## YOLO Safeguards (Always Enforced)

| Safeguard | Requirement |
|-----------|-------------|
| No Shortcuts | No hacks, no skipped steps |
| No Disable Without Re-enable | Must re-enable in same commit |
| 100% Function Equivalence | Refactoring preserves ALL behavior |
| All Tests Pass | No skipped tests |
| No TODOs | Complete implementation |

---

## Quick Examples

```bash
# Full feature with automation
/develop .yolo .swarm "add dark mode toggle"

# Bug fix with test-commit-revert
/fix .yolo .tcr "login not redirecting"

# Show reasoning during fix
/fix .scot "complex auth bug"

# Create UI with verbose output
/createui .app .verbose "user settings"

# Deep thinking
/think .cot "microservices vs monolith?"

# Multi-agent analysis
/analyze .swarm .verbose

# Interactive learning mode
/wiz .tutor .swarm

# Refactor with safety
/refactor .tcr .verbose "cleanup UserService"
```

---

## Documentation

| Doc | Location |
|-----|----------|
| Global Instructions | `/Volumes/M-Drive/Coding/.claude/CLAUDE.md` |
| Full Manual | `ideacode-mcp/docs/ideacode-v10-manual.md` |
| Token Efficiency | `.ideacode/living-docs/ld-token-efficiency-v1.md` |
| UI Guidelines | `.ideacode/living-docs/ld-ui-guidelines-v1.md` |
| MagicUI System | `Avanues/docs/universal/LD-magicui-design-system.md` |

---

**Version:** 10.0 | **Author:** Manoj Jhawar | **License:** Proprietary
