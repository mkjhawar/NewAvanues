# Architecture (Living Document)

## Purpose
Tracks architectural decisions and system structure.
**Use:** Before making architectural changes, check and update here.

---

## System Overview

| Location | Component |
|----------|-----------|
| .claude/commands/ | Slash Commands |
| ideacode-mcp/ | MCP Server |
| programming-standards/ | Code Standards |
| protocols/ | Workflow Protocols |
| .ideacode/ | Config & Continuity |

---

## Core Components

| Component | Role | Location |
|-----------|------|----------|
| Slash Commands | User interface | .claude/commands/*.md |
| MCP Server | Tool execution | ideacode-mcp/src/ |
| Standards | Code quality | programming-standards/ |
| Protocols | Workflow rules | protocols/ |
| Continuity | Session memory | .ideacode/continuity/ |
| Registries | File/folder tracking | .ideacode/registries/ |

---

## Data Flow

1. User Request
2. Slash Command (parse modifiers)
3. MCP Tool (ideacode_*)
4. Standards Check
5. Execution
6. Continuity Save (if needed)

---

## Key Decisions

| Decision | Choice | Rationale | Date |
|----------|--------|-----------|------|
| Config format | .ideacode extension | Unique, searchable | 2025-11 |
| Commands | Markdown-based | Human readable, AI parseable | 2025-11 |
| Memory | File-based (con-*.md) | Persistent, portable | 2025-11 |
| Naming | {app}-{module}-{desc} | Clear, searchable | 2025-11 |

---

## Constraints

| Constraint | Rule |
|------------|------|
| CLAUDE.md | Must exist at .claude/CLAUDE.md |
| MCP Server | Must be running for tools |
| Standards | Must be loaded before code gen |
| Registries | Must be checked before file ops |

---

## Extension Points

| Point | How to Extend |
|-------|---------------|
| New command | Add to .claude/commands/ |
| New tool | Add to ideacode-mcp/src/tools/ |
| New standard | Add to programming-standards/ |
| New protocol | Add to protocols/ |

---
*Updated: 2025-11-29 | IDEACODE v9.0*
