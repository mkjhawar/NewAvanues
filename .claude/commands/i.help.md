---
description: Interactive help system .examples .commands .modifiers | /i.help fix .examples
---

# IDEACODE Help System

---

## IDEACODE API Integration

This command uses the IDEACODE API for token efficiency (97% savings).

API Endpoint: `http://localhost:3850/i.help`
Auto-start: API server starts automatically if not running

---


## Usage
`/i.help [topic] [.modifiers]`

## Modifiers
| Modifier | Effect |
|----------|--------|
| `.examples` | Include practical examples inline |
| `.howto` | Alias for `.examples` - show HOWTO guide |
| `.commands` | List all commands only |
| `.modifiers` | Show modifier reference only |

## Quick Reference

| Category | Commands |
|----------|----------|
| Workflows | `/idevelop`, `/iwiz`, `/refactor`, `/idea` |
| Bug Fixing | `/ifix` (auto-detects complexity) |
| Agents | `/agent`, `/swarm` |
| UI/Vision | `/analyzeui`, `/mockup`, `/debugscreenshot` |
| Reasoning | `/think`, `/issue`, `/research` |
| Management | `/list`, `/analyze`, `/archive` |
| Setup | `/newproject`, `/update`, `/principles` |
| Manual Steps | `/ispecify`, `/iplan`, `/iimplement`, `/itasks` |
| Help | `/help`, `/version`, `/projectinstructions` |
| Inline Capture | `.global`, `.project`, `.test`, `.backlog`, `.security`, `.docs`, `#ideacode-update` |

## Command Details

### Development Workflows

| Command | Purpose | When to Use | Key Feature |
|---------|---------|------------|------------|
| `/idevelop` | Full workflow (Specify→Plan→Implement→Test→Archive) | New features, automated Q&A | Auto-docs, complexity detection |
| `/iwiz` | Interactive step-by-step with tutor | Learning, complex features | Checkpoints, guidance at each step |
| `/refactor` | Guided refactoring workflow | Improve code, reduce tech debt | Systematic approach |
| `/idea` | MBA-level business + technical planning | New projects, comprehensive analysis | Business case, architecture, roadmap |

### Bug Fixing (Consolidated)

| Command | Behavior |
|---------|----------|
| `/ifix "bug"` | Fix known bug directly |
| `/ifix "symptom"` | Auto-detect if investigation needed → ask to fix |
| `/ifix .investigate "symptom"` | Investigate → ask to fix |
| `/ifix .investigate .report "issue"` | Investigate only → output report |
| `/ifix .investigate .yolo "issue"` | Investigate → auto-fix |

**Auto-Detection:**
- Known cause → direct fix
- Unknown cause → auto-investigate → ask to fix
- Simple: Linear fix
- Complex: Tree-based (CoT/ToT)
- Multi-domain: Swarm mode (specialist agents)

**Related Analysis Commands:**
| Need | Command |
|------|---------|
| Project health check | `/iscan .debug` |
| Deep reasoning | `/ithink "problem"` |

### AI Agents & Swarms

| Command | Purpose | Agents |
|---------|---------|--------|
| `/agent` | Create autonomous AI agents | Security, Code Review, Testing, Documentation, Performance, Architecture, Design System |
| `/swarm` | Orchestrate multi-agent swarms | Parallel, Sequential, Loop, Router |

### UI & Vision

| Command | Purpose | Input | Output |
|---------|---------|-------|--------|
| `/analyzeui` | Analyze UI from screenshot | Screenshot path | Layout, components, accessibility suggestions |
| `/mockup` | Generate code from design | Mockup path + profile | React/Vue/etc code matching mockup |
| `/debugscreenshot` | Debug from error screenshot | Error screenshot path | Root cause analysis + fix suggestions |

### Advanced Reasoning

| Command | Purpose | Use Case |
|---------|---------|----------|
| `/think` | Extended thinking for complex problems | Architecture decisions, strategic planning |
| `/issue` | Comprehensive issue analysis (ToT/CoT) | Complex bugs, multi-domain failures |
| `/research` | Web research on technologies/APIs | Framework updates, API exploration |

### Project Management

| Command | Purpose | Output |
|---------|---------|--------|
| `/list` | View active features, specs, archives | Summary of features by status |
| `/analyze` | Analyze spec/plan/tasks consistency | Gap report, recommendations |
| `/archive` | Archive completed feature | Merge delta to living spec, move to archive/ |

### Repository Management

| Command | Purpose |
|---------|---------|
| `/irepo .new` | Create fresh monorepo with full structure |
| `/irepo .migrate` | Import/export/upgrade repos (interactive) |
| `/irepo .add` | Add module to monorepo |
| `/irepo .validate` | Health check on repo structure |
| `/irepo .cleanup` | Fix misplaced files and naming |
| `/irepo .list` | List modules/apps |

### Setup & Configuration

| Command | Purpose | Interactive |
|---------|---------|------------|
| `/iproject` | Project operations (init, update, validate) | Yes |
| `/iprinciples` | Create/update project principles | Yes (principle definition) |

### Manual Workflow Steps

| Command | Purpose | Input | Output |
|---------|---------|-------|--------|
| `/ispecify` | Create feature specification | Feature description | Delta spec document |
| `/iplan` | Generate implementation plan | Spec file path | Implementation plan |
| `/iimplement` | Execute implementation | Plan file path | Implemented code |
| `/itasks` | Generate task breakdown | Plan file path | tasks.md with checklist |

### Documentation & Help

| Command | Purpose |
|---------|---------|
| `/help` | Interactive help system |
| `/version` | Show framework and MCP versions |
| `/projectinstructions` | View project-specific instructions |

## Command Modifiers

### Flag Modifiers

| Modifier | Purpose | Effect | Supported Commands |
|----------|---------|--------|-------------------|
| `.yolo` | Auto-progress | No approvals, auto-chain | All workflow commands |
| `.tdd` | Test-driven | Write tests first (auto-detects) | `/ifix`, `/idevelop`, `/irefactor` |
| `.skip-tdd` | Skip TDD | Skip TDD (reason if score >= 90) | `/ifix`, `/idevelop` |
| `.ood` | OOD patterns | Force OOD pattern analysis | `/idevelop`, `/ifix`, `/irefactor` |
| `.ddd` | Full DDD | Entity, Aggregate, Repository, Service | `/idevelop`, `/irefactor` |
| `.solid` | SOLID check | Enforce SOLID principles | `/irefactor`, `/ifix` |
| `.profile <name>` | Use profile | default, strict, prototype, production | All workflow commands |
| `.no-verify` | Skip verify | Skip auto-verification | `/idevelop`, `/iimplement` |
| `.visual` | Visual verify | Capture screenshots | `/icreateui`, `/ireview` |
| `.cot` | Chain of Thought | Show step-by-step reasoning | `/ifix`, `/iplan`, `/irefactor` |
| `.tot` | Tree of Thoughts | Explore 3-5 solution paths | `/ifix`, `/irefactor`, `/iplan` |
| `.tcr` | Test && Commit \|\| Revert | Auto-test, commit if pass, revert if fail | `/iimplement`, `/irefactor`, `/ifix` |
| `.swarm` | Force swarm mode | Activate multi-agent coordination | All complex commands |
| `.tutor` | Educational mode | Explain decisions and trade-offs | `/idevelop`, `/iwiz`, `/iplan` |
| `.stop` | Disable chaining | Manual control at each step | Workflow commands |
| `.prototype` | Lightweight mode | Defer testing, minimal docs | `/idevelop` |

### Hash-Tag Modifiers (Inline Capture)

| Tag | Purpose | Destination | Example |
|-----|---------|-------------|---------|
| `.global` | Universal design standard | design-standards/ | `.global Use JWT with 15min expiry` |
| `.project` | Project-specific standard | .ideacode/design-standards/ | `.project Use Material Design 3` |
| `.test` | Add test case | tests/ | `.test Should reject expired tokens` |
| `.backlog` | Future work item | backlog/ | `.backlog Add GraphQL support` |
| `.security` | Security requirement | design-standards/security/ | `.security Encrypt API keys at rest` |
| `.docs` | Documentation update | docs/ | `.docs Document hash-tag system` |
| `#ideacode-update` | Framework code change | IDEACODE codebase | `#ideacode-update Add GraphQL support` |

**Inline Capture Syntax:** `{instruction}. #{tag} {content}`

**Benefits:** 70%+ token savings, <10 sec capture time, zero workflow interruption

## Decision Helper

| Question | Answer → Command |
|----------|------------------|
| Build new feature? | `/idevelop` (auto Q&A) or `/iwiz` (interactive) |
| Fix a known bug? | `/ifix "description"` |
| Something broken, don't know why? | `/ifix "symptom"` (auto-investigates) |
| Just want to investigate? | `/ifix .investigate .report "symptom"` |
| Project health check? | `/iscan .debug` |
| Need to think it through? | `/ithink "problem"` |
| Refactor code? | `/refactor` (guided workflow) |
| Plan a project? | `/idea` (MBA-level analysis) |
| Create automation? | `/agent` (autonomous agents) |
| Get information? | `/list`, `/version` |
| Want guidance or automation? | Guidance → `/iwiz`, Automation → `/idevelop` |
| Multi-domain bug? | `/ifix .swarm` auto-activates specialists |
| Create new monorepo? | `/irepo .new "Name"` |
| Migrate/import a repo? | `/irepo .migrate` |
| Add module to monorepo? | `/irepo .add "module"` |
| Check repo structure? | `/irepo .validate` |

## Modes Comparison

| Mode | Approvals | Chaining | Testing | Use Case |
|------|-----------|----------|---------|----------|
| Interactive (default) | Yes | Optional | Full | Standard features |
| YOLO (`.yolo`) | No | Auto | Full | Simple features (<30 min) |
| Manual (`.stop`) | Yes | None | Full | Complex, needs review |
| Prototype (`.prototype`) | Optional | Optional | Deferred | Experiments, POCs |

## MCP Tools Integration

| Task | Tool |
|------|------|
| Specification | `ideacode_specify` |
| Planning | `ideacode_plan` |
| Implementation | `ideacode_implement` |
| Testing | `ideacode_test` |
| Commit | `ideacode_commit` |
| Standards | `ideacode_standards` |
| Context Management | `ideacode_context_show`, `ideacode_context_reset`, `ideacode_context_save` |

## Examples

| Command | Behavior |
|---------|----------|
| `/idevelop` | Interactive Q&A → manual approvals at each stage |
| `/develop "dark mode" .yolo` | Auto: analyze → Q&A → spec → plan → implement → test → commit |
| `/develop "OAuth" .cot` | Full workflow with step-by-step reasoning shown |
| `/develop "chat" .swarm` | Force parallel multi-agent execution |
| `/fix "bug description"` | Auto-detect complexity, choose linear/tree/swarm strategy |
| `/fix "multi-domain bug" .tcr` | Test, commit if pass, revert if fail |
| `/iwiz` | Interactive guide with explanations and checkpoints |
| `/mockup path/to/mockup.png .profile frontend-web` | Generate React code from design |
| `/analyze .swarm` | Multi-agent code analysis with coordination |
| `/think "architecture question"` | Extended thinking for complex decisions |

## TDD & OOD Auto-Detection

### TDD Scoring
| Score | Action |
|-------|--------|
| < 50 | No TDD |
| 50-69 | Recommend TDD |
| 70-89 | Strongly recommend |
| >= 90 | Enforce TDD |

**Positive:** Business logic (+30), Bug fix (+25), Critical path (+15)
**Negative:** UI only (-20), Config (-15), Trivial (-30)

### OOD Scoring
| Score | Action |
|-------|--------|
| < 40 | No OOD patterns |
| 40-59 | Suggest patterns |
| 60-79 | Recommend patterns |
| >= 80 | Strongly recommend |

**Positive:** Domain logic (+30), Data modeling (+25), Relationships (+20)
**Negative:** Simple CRUD (-25), Utility code (-20), UI layer (-15)

## Profiles

| Profile | TDD | OOD | Auto-Verify | Visual |
|---------|-----|-----|-------------|--------|
| `default` | 50 | 40 | Yes | No |
| `strict` | 30 | 30 | Yes | Yes |
| `prototype` | 90 | 80 | No | No |
| `production` | 40 | 40 | Yes | Yes |

**Usage:** `/idevelop .profile strict "payment"`

## Auto-Verification

Verification runs automatically after each phase:
- **Spec:** `spec-verifier` checks completeness
- **Plan:** `plan-verifier` checks coverage
- **Implement:** `impl-verifier` checks quality

**Disable:** `.no-verify`

## Quality Gates

| Metric | Requirement |
|--------|-------------|
| Test Coverage | 90%+ on critical paths |
| Blockers | 0 |
| Warnings | 0 |
| Function Equivalence | 100% (refactoring must preserve all behavior) |

## IDC Format (Compact Config)

IDEACODE uses `.idc` format for configurations (60-80% smaller than YAML).

| Code | Meaning | Example |
|------|---------|---------|
| `PRJ` | Project | `PRJ:ideacode:framework:10.3.0` |
| `PRF` | Profile | `PRF:strict:30:30:true:true` |
| `CFG` | Config | `CFG:auto_verify:true:bool` |
| `THR` | Threshold | `THR:tdd:50:69:recommend` |
| `SIG` | Signal | `SIG:tdd-pos:business_logic:+30` |
| `VRF` | Verifier | `VRF:spec:spec-verifier:haiku` |
| `GAT` | Gate | `GAT:coverage:90:true` |
| `AGT` | Agent | `AGT:code-reviewer:read+grep+glob:haiku` |
| `CMD` | Command | `CMD:idevelop:.yolo+.swarm:Full workflow` |

**Separators:** `:` (fields), `+` (multi-value), `,` (lists)

**Spec:** `docs/specifications/IDC-FORMAT-SPEC-V1.md`

## Related Documentation

- **Full Modifiers Guide:** `/imodifiers`
- **IDC Format Spec:** `docs/specifications/IDC-FORMAT-SPEC-V1.md`
- **Standards:** `ideacode_standards .context "task"`
- **IDEACODE Constitution:** `/Volumes/M-Drive/Coding/ideacode/.claude/CLAUDE.md`
- **Protocols:** `/Volumes/M-Drive/Coding/ideacode/protocols/`
- **Hash-Tag System:** `docs/Hash-Tag-Inline-Capture-System-v1.0.md`

## Getting Help

Type a command name (e.g., "fix", "develop", "wiz") for detailed help.
Ask: "What command should I use to [task]?" for decision support.

---

## .examples Modifier Behavior

When `/i.help .examples` is used, inline practical examples from `/ihowto`:

### Example: `/i.help fix .examples`
```
## Bug Fixing (Consolidated)

| Command | Behavior |
|---------|----------|
| `/ifix "bug"` | Fix known bug directly |
| `/ifix .investigate "symptom"` | Investigate → ask to fix |

### Examples (from /ihowto)

Know the Cause → Direct Fix:
  /ifix "null pointer in UserService.validate()"

Don't Know Cause → Auto-Investigate:
  /ifix "app running slow"

Investigate Only (No Fix):
  /ifix .investigate .report "random crashes"
```

### Topic-Specific Examples
| Topic | Shows Examples For |
|-------|-------------------|
| `/i.help fix .examples` | Bug fixing scenarios |
| `/i.help develop .examples` | Feature development workflows |
| `/i.help review .examples` | Code review scenarios |
| `/i.help .examples` | All command examples |

## Context Management

| Operation | When to Use |
|-----------|-------------|
| **Clear** | New unrelated task |
| **Save** | Before risky ops |
| **Compact** | Context 70%+, task in progress |

## MCP Lazy Loading

| Always Loaded | On-Demand |
|---------------|-----------|
| `ideacode_discover` | `ideacode_specify`, `ideacode_plan` |
| `ideacode_context` | `ideacode_implement`, `ideacode_test` |
| `ideacode_standards` | `ideacode_vision`, `ideacode_research` |

## Metadata

- **Command:** `/i.help`
- **Version:** 10.3
- **Intelligence:** YES (decision routing, complexity detection, TDD, OOD)
