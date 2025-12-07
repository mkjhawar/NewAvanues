# Module State (Living Document)

## Purpose
Tracks the current state of all modules across projects.
**Use:** Before modifying any module, check its state here.

---

## What This Document Does

| Function | Description |
|----------|-------------|
| **Track State** | Active, deprecated, archived, in-development |
| **Dependencies** | What modules depend on what |
| **Ownership** | Who/what is responsible |
| **Health** | Test coverage, tech debt, issues |

---

## Module Registry

### IDEACODE Core

| Module | State | Version | Dependencies | Health |
|--------|-------|---------|--------------|--------|
| ideacode-mcp | active | 9.0 | node, typescript | good |
| commands | active | 9.0 | ideacode-mcp | good |
| standards | active | 9.0 | none | good |
| protocols | active | 9.0 | none | good |

### Project Modules (Template)

| Module | State | Version | Dependencies | Health |
|--------|-------|---------|--------------|--------|
| {app}-{module} | active/dev/deprecated | x.x | (list) | good/warn/critical |

---

## State Definitions

| State | Meaning | Action |
|-------|---------|--------|
| `active` | Production ready, maintained | Normal use |
| `development` | In progress, unstable | Use with caution |
| `deprecated` | Being phased out | Migrate away |
| `archived` | No longer maintained | Do not use |
| `experimental` | Testing concepts | Not for production |

---

## Health Definitions

| Health | Criteria | Action |
|--------|----------|--------|
| `good` | >90% coverage, 0 critical issues | None |
| `warn` | 70-90% coverage, <3 issues | Monitor |
| `critical` | <70% coverage, >3 issues | Fix urgently |

---

## Dependency Rules

| Rule | Description |
|------|-------------|
| No circular deps | A→B→A forbidden |
| Declare all deps | Document in this file |
| Check before modify | Understand downstream impact |
| Update on change | Keep this file current |

---

## Before Modifying a Module

1. Check state (active? deprecated?)
2. Check dependencies (what depends on this?)
3. Check health (any existing issues?)
4. Update this document after changes

---

## Module Lifecycle

| Stage | Next | Activity |
|-------|------|----------|
| experimental | → development | Testing |
| development | → active | Building |
| active | → deprecated | Production |
| deprecated | → archived | Migration |
| archived | (end) | Removed |

---
*Updated: 2025-11-29 | IDEACODE v9.0*
