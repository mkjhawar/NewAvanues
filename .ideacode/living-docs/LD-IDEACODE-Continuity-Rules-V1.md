# Continuity Rules (Living Document)

## Intelligent Context Management

### When to SAVE

| Trigger | Action | File |
|---------|--------|------|
| Task milestone complete | Save progress | con-{app}-{module}-progress-{date}.md |
| Decision made | Save reasoning | con-{app}-{module}-decisions-{date}.md |
| Before risky operation | Checkpoint | con-{app}-{module}-checkpoint-{date}.md |
| Session ending | Handover | con-{app}-{module}-handover-{date}.md |

### When to CLEAR

| Trigger | What to Clear |
|---------|---------------|
| Task context switch | Old task exploration |
| Exploration exhausted | Failed search paths |
| Decision finalized | Alternative options |
| File read no longer relevant | Stale file contents |

### What to KEEP

| Always Keep | Why |
|-------------|-----|
| Current task context | Active work |
| Unresolved blockers | Need resolution |
| Active decisions | Guide work |
| File change list | Commit reference |

### What NEVER to Save

| Skip | Why |
|------|-----|
| Failed grep results | Noise |
| Superseded approaches | Outdated |
| Repeated reads of same file | Redundant |
| Exploratory dead ends | No value |

---

## Persistence Modifiers

| Modifier | Use When |
|----------|----------|
| `.persist` | Multi-file changes, important progress |
| `.resume` | Starting session, loading handover |
| `.deep` | Architecture work, multi-day tasks |
| `.checkpoint` | Before refactor, delete, risky ops |

---

## File Naming

```
con-{app}-{module}-{description}-{YYYYMMDD}.md
```

| Field | Values |
|-------|--------|
| app | ideacode, voiceos, shared, etc. |
| module | mcp, core, auth, ui, etc. |
| description | handover, decisions, progress, checkpoint |
| date | YYYYMMDD format |

---
*IDEACODE v9.0*
