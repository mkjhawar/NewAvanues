# AI Auto-Invoke Instructions (v1)

## Core Principle: INTELLIGENT BY DEFAULT

**No modifiers needed.** AI auto-detects and applies appropriate behaviors.

---

## CLAUDE.md Maintenance (ENFORCED)

| Rule | Requirement |
|------|-------------|
| Keep minimal | <150 lines, no verbose explanations |
| Structured formats | Tables for reference, lists for steps, short prose for conditionals |
| Details elsewhere | Put details in `.ideacode/living-docs/LD-*.md` |
| Before editing | Check current format, maintain style |
| Never add | ASCII art, emojis, walls of text |

## AI-Optimized Format Guide

| Content Type | Format | Example |
|--------------|--------|---------|
| Reference data | Table | Rules, mappings, configs |
| Sequential steps | Numbered list | 1. Do X 2. Do Y |
| Options/features | Bullet list | - Option A - Option B |
| Conditional logic | Short prose | "If X, then Y. Otherwise Z." |
| Commands/syntax | Code block | `command --flag` |

**Principles:** Unambiguous, structured, scannable, consistent, minimal.

---

## File Naming (ENFORCED)

| Type | Pattern | Example |
|------|---------|---------|
| Code | `{app}-{module}-{desc}.{ext}` | `ideacode-mcp-server.ts` |
| Living Doc | `LD-{name}-v{#}.md` | `LD-architecture-v1.md` |
| Continuity | `con-{app}-{module}-{desc}-{YYYYMMDD}.md` | `con-ideacode-mcp-handover-20251129.md` |
| Spec/Plan | `{type}-{feature}-{YYYYMMDD}.md` | `spec-auth-oauth-20251129.md` |
| Archive | `{name}-{YYYYMMDD}.md` | `feature-complete-20251129.md` |

---

## SOLID Principles (ENFORCED)

| Principle | Rule | Violation Check |
|-----------|------|-----------------|
| **S**ingle Responsibility | 1 class = 1 reason to change | Class doing multiple jobs? |
| **O**pen/Closed | Open for extension, closed for modification | Modifying existing code for new features? |
| **L**iskov Substitution | Subtypes replaceable for base types | Subclass breaking parent contract? |
| **I**nterface Segregation | Many specific interfaces > 1 general | Interface with unused methods? |
| **D**ependency Inversion | Depend on abstractions, not concretions | Hardcoded dependencies? |

**Before any code:** Verify SOLID compliance. Flag violations.

---

## Auto-Invoke Logic

### Task Detection → Auto-Apply

| Detected Task | Auto-Invokes |
|---------------|--------------|
| New project | Standards check, folder registry, SOLID setup |
| Feature (simple) | `.cot`, standards, tests |
| Feature (complex) | `.tot` → `.rot`, web search, swarm if multi-domain |
| Bug (clear cause) | `.cot`, minimal fix, test |
| Bug (unclear cause) | `.tot` → `.cot`, web search if stuck |
| Refactor | `.checkpoint`, SOLID check, 100% equivalence |
| Architecture decision | `.tot` → `.rot` → `.cot`, research |

### Complexity Detection → Auto-Escalate

| Signal | Action |
|--------|--------|
| Multiple possible causes | Auto-invoke `.tot` |
| Tradeoffs detected | Auto-invoke `.rot` |
| Going in circles (2+ attempts) | Auto-invoke `.rot`, then web search |
| Multi-domain (security+perf, etc.) | Auto-invoke `.swarm` |
| Unknown API/library | Auto web search |
| Integration needed | Auto MCP discovery |

---

## Web Search (Auto-Invoke When Stuck)

| Situation | Search |
|-----------|--------|
| Unknown error | GitHub issues, Stack Overflow |
| Best practice unclear | Anthropic docs, OpenAI docs |
| Library/API usage | Official docs, GitHub examples |
| Architecture pattern | GitHub repos, OpenStack patterns |
| MCP integration | registry.modelcontextprotocol.io |

**Search order:**
1. Official docs
2. GitHub (issues, examples, repos)
3. Stack Overflow / OpenStack
4. Anthropic / OpenAI guidance
5. Community blogs

---

## MCP Discovery (Auto-Invoke for Integrations)

| Need Detected | Action |
|---------------|--------|
| Payment processing | Search: "mcp server stripe/square" |
| Database ops | Search: "mcp server postgresql/mongodb" |
| Cloud services | Search: "mcp server aws/gcp/azure" |
| External API | Search: "mcp server {service}" |

**Before building custom integration:** Search MCP registry first.

---

## IDEACODE MCP Tools (MANDATORY Auto-Invoke)

### File Operations → `ideacode_fs`

| Task | Auto-Invoke |
|------|-------------|
| Create file/folder | `ideacode_fs --action create` |
| Rename file/folder | `ideacode_fs --action rename` |
| Move file/folder | `ideacode_fs --action move` |
| Delete file/folder | `ideacode_fs --action delete` |
| Check naming | `ideacode_fs --action validate` |
| Find duplicates | `ideacode_fs --action audit --audit_type projectfolders` |
| Check doc placement | `ideacode_fs --action audit --audit_type docs` |
| Full project cleanup | `ideacode_fs --action consolidate` |

**NEVER use:** Write tool, mv command, rm command for file ops.

### Feature Development → Core Workflow Tools

| Phase | Auto-Invoke |
|-------|-------------|
| Specification | `ideacode_specify --feature "{name}"` |
| Planning | `ideacode_plan --spec_file "{spec}"` |
| Implementation | `ideacode_implement --plan_file "{plan}"` |
| Testing | `ideacode_test --path "{module}"` |
| Validation | `ideacode_validate --path "{module}"` |
| Commit | `ideacode_commit --message "{msg}"` |

### Documentation → `ideacode_manual`

| Task | Auto-Invoke |
|------|-------------|
| Create user manual | `ideacode_manual --type user --target "{app}"` |
| Create dev manual | `ideacode_manual --type developer --target "{module}"` |
| Update documentation | `ideacode_manual --action update --path "{doc}"` |
| Generate diagrams | `ideacode_manual --diagrams ascii,mermaid` |

### Research & Analysis

| Task | Auto-Invoke |
|------|-------------|
| Deep research | `ideacode_research --topic "{query}"` |
| Complex reasoning | `ideacode_think --question "{q}"` |
| Standards check | `ideacode_standards --context "{task}"` |
| Vision/mockup | `ideacode_vision --action from_mockup --image "{path}"` |

### Context & Patterns

| Task | Auto-Invoke |
|------|-------------|
| Save context | `ideacode_context --action save --name "{name}"` |
| Show context | `ideacode_context --action show` |
| Reset context | `ideacode_context --action reset` |
| Save pattern | `ideacode_pattern --action learn --pattern "{name}"` |
| Create checkpoint | `ideacode_pattern --action checkpoint` |

### Task Detection → MCP Tool Mapping

| User Intent | Primary Tool | Secondary Tools |
|-------------|--------------|-----------------|
| "Create new feature" | `ideacode_specify` | `ideacode_plan`, `ideacode_implement` |
| "Fix bug" | `ideacode_think` | `ideacode_test`, `ideacode_commit` |
| "Refactor code" | `ideacode_validate` | `ideacode_test`, `ideacode_commit` |
| "Write docs" | `ideacode_manual` | `ideacode_fs` |
| "Review code" | `ideacode_validate` | `ideacode_think` |
| "Check project structure" | `ideacode_fs --action audit` | - |
| "Create/move files" | `ideacode_fs` | - |
| "Debug screenshot" | `ideacode_vision --action debug_screenshot` | - |
| "From mockup" | `ideacode_vision --action from_mockup` | - |

### Auto-Invoke Decision Tree

```
User Request Received
       ↓
Is it file operation? ──YES──→ ideacode_fs
       ↓ NO
Is it feature development? ──YES──→ ideacode_specify → ideacode_plan → ideacode_implement
       ↓ NO
Is it bug fix? ──YES──→ ideacode_think → ideacode_test → ideacode_commit
       ↓ NO
Is it documentation? ──YES──→ ideacode_manual
       ↓ NO
Is it research/analysis? ──YES──→ ideacode_research or ideacode_think
       ↓ NO
Is it vision/mockup? ──YES──→ ideacode_vision
       ↓ NO
Use appropriate standard tool
```

---

## Workflow: New Project

```
1. [AUTO] Check FOLDER-REGISTRY.md → create structure
2. [AUTO] Check FILE-REGISTRY.md → naming conventions
3. [AUTO] Load standards: ideacode_standards --context "new project"
4. [AUTO] Create LD-architecture-v1.md
5. [AUTO] Create LD-module-state-v1.md
6. [AUTO] Initialize with SOLID-compliant structure
7. [ASK] Project type, tech stack, requirements
```

---

## Workflow: Feature Development

```
1. [AUTO] Assess complexity (simple/complex/multi-domain)
2. [AUTO] If complex → .tot (explore approaches)
3. [AUTO] If tradeoffs → .rot (evaluate)
4. [AUTO] Check standards, SOLID principles
5. [AUTO] If stuck → web search
6. [AUTO] If integration → MCP discovery
7. [AUTO] Implementation with .cot (step-by-step)
8. [AUTO] Tests (90%+ coverage)
9. [AUTO] Save progress to con-*-progress-{date}.md
```

---

## Workflow: Bug Fix

```
1. [AUTO] Analyze: clear cause or unclear?
2. [AUTO] If unclear → .tot (hypotheses)
3. [AUTO] If clear → .cot (direct trace)
4. [AUTO] Spawn domain specialist (OS/PhD/Senior level)
5. [AUTO] If stuck 2+ attempts → .rot + web search
6. [AUTO] Minimal fix (no scope creep)
7. [AUTO] Test: fail before, pass after
8. [AUTO] SOLID check on changes
9. [AUTO] Save to con-*-fix-{date}.md if complex
```

---

## Code Review (Auto-Run Before Commit)

| Check | Flag If |
|-------|---------|
| SOLID violation | Any principle broken |
| KISS violation | Over-engineered solution |
| Over-abstraction | Abstraction for <3 use cases |
| Missing tests | <90% coverage on changes |
| Scope creep | Changes beyond request |

---

## Context Management (Intelligent)

| Trigger | Auto-Action |
|---------|-------------|
| Task milestone | Save progress |
| Switching context | Save old, clear stale, load new |
| Decision made | Save to con-*-decisions-{date}.md |
| Before risky op | Checkpoint |
| Exploration exhausted | Clear failed paths |

---

## Domain Specialists (Auto-Spawn)

| Domain Detected | Specialist Level |
|-----------------|------------------|
| Memory, crash, threading | OS-Level |
| Security, crypto, auth | PhD-Level |
| Algorithm, optimization | PhD-Level |
| Network, API, database | Senior |
| UI, animations | Senior |

---

## Summary: User Says → AI Does

| User Says | AI Auto-Does |
|-----------|--------------|
| "New project" | Full setup with registries, SOLID structure |
| "Add feature X" | Complexity check → appropriate reasoning → implement |
| "Fix bug Y" | Specialist → analysis → minimal fix → test |
| "Integrate Z" | MCP search → use existing or build |
| (nothing specific) | Detect intent → apply appropriate workflow |

**User never needs to specify modifiers.** AI detects and applies.

---
*IDEACODE v1 | Auto-Invoke Intelligence*
