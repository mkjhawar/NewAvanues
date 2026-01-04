# LD-IDEACODE-Context-Management-V1

## Purpose
Guidelines for managing AI context - when to clear, load, save, and compact context for optimal performance.

---

## Context Operations

| Operation | Command | When to Use |
|-----------|---------|-------------|
| **Clear** | `ideacode_context --action reset` | Start fresh, new task unrelated to previous |
| **Save** | `ideacode_context --action save` | Preserve state before complex operation |
| **Load** | `ideacode_context --action show` | Resume previous session |
| **Compact** | Pre-compact hook | Context approaching limit, need to continue |

---

## When to Clear Context

| Scenario | Action | Reason |
|----------|--------|--------|
| New unrelated task | Clear | Previous context irrelevant |
| After major completion | Clear | Free up token space |
| Context polluted | Clear | Too many failed attempts |
| Switching projects | Clear | Different codebase context |
| User requests | Clear | Explicit `/clear` command |

### Clear Triggers
```
- Task complexity score drops to 0
- User says "start fresh", "new task", "forget previous"
- Project path changes significantly
- 3+ consecutive errors in same area
```

---

## When to Save Context

| Scenario | Action | Reason |
|----------|--------|--------|
| Before risky operation | Save | Rollback point |
| Mid-complex task | Save | Checkpoint progress |
| Before swarm spawn | Save | Preserve orchestrator state |
| End of session | Save | Resume later |
| Before refactoring | Save | Safety checkpoint |

### Save Format
```
Location: {project}/contextsave/
Naming: con-{app}-{module}-{desc}-{YYYYMMDD}.md

Contents:
- Current task description
- Files modified
- Progress state
- Next steps
- Key decisions made
```

---

## When to Compact (vs Clear)

| Context State | Action | Reason |
|---------------|--------|--------|
| 70% full, task in progress | **Compact** | Preserve essential context |
| 70% full, task complete | **Clear** | No need to preserve |
| 90% full, critical task | **Compact** | Must continue |
| Any %, new unrelated task | **Clear** | Fresh start better |

### Compact Preserves
```
- Current task description
- Key file paths
- Important decisions
- Error patterns to avoid
- Progress checkpoints
```

### Compact Removes
```
- Full file contents (summarize instead)
- Redundant search results
- Failed attempt details
- Verbose tool outputs
```

---

## Decision Flow

```
Context Operation Decision:

1. Is this a new unrelated task?
   YES → CLEAR
   NO  → Continue

2. Is context > 70% full?
   NO  → Continue working
   YES → Continue to step 3

3. Is current task complete?
   YES → CLEAR (save first if important)
   NO  → COMPACT

4. Is task critical/complex?
   YES → COMPACT (preserve context)
   NO  → CLEAR (faster)
```

---

## Compact Hook (Pre-Compact)

The pre-compact hook runs automatically when context approaches limit:

```bash
# .claude/pre-compact.sh
#!/bin/bash
# Saves essential context before compaction

echo "Saving context checkpoint..."
# Auto-generates context save file
```

### Hook Behavior
| Trigger | Action |
|---------|--------|
| Context > 80% | Run pre-compact hook |
| Hook success | Proceed with compact |
| Hook failure | Warn user, manual save option |

---

## Context Save File Structure

```markdown
# Context Save: {description}

## Session Info
- **Date:** {YYYY-MM-DD HH:MM}
- **Project:** {project_name}
- **Task:** {task_description}

## Progress
- [x] Completed step 1
- [x] Completed step 2
- [ ] In progress: step 3

## Key Files
| File | Status | Notes |
|------|--------|-------|
| src/auth.ts | Modified | Added OAuth |
| tests/auth.test.ts | Created | 95% coverage |

## Decisions Made
1. Used JWT over sessions
2. Added rate limiting

## Next Steps
1. Complete step 3
2. Run integration tests
3. Update documentation

## Errors to Avoid
- Don't use deprecated API (causes TypeErrors)
- Remember to await async calls
```

---

## MCP Context Tools

| Tool | Action | Use Case |
|------|--------|----------|
| `ideacode_context --action reset` | Clear all | Fresh start |
| `ideacode_context --action save` | Save current | Checkpoint |
| `ideacode_context --action show` | Display saved | Resume |
| `ideacode_checkpoint` | Create restore point | Before risky ops |

---

## Profile-Based Context Behavior

| Profile | Auto-Save | Auto-Compact | Clear Frequency |
|---------|-----------|--------------|-----------------|
| default | On major steps | At 80% | Task completion |
| strict | Every phase | At 70% | Manual only |
| prototype | Never | At 90% | Frequent |
| production | Every step | At 70% | After review |

---

## Best Practices

| Practice | Reason |
|----------|--------|
| Save before swarm operations | Multiple agents need consistent base |
| Clear between unrelated tasks | Prevents context pollution |
| Compact mid-complex tasks | Preserves progress without bloat |
| Use descriptive save names | Easy to find and resume |
| Review context periodically | Remove irrelevant information |

---

## Metadata
- **Document:** LD-IDEACODE-Context-Management-V1
- **Version:** 1.0
- **Created:** 2025-12-05
- **Author:** IDEACODE
