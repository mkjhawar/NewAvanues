---
description: End of day - commit, push, cleanup worktree | /i.close | /i.close .force | /i.close .all
tags: [git, worktree, cleanup, end-of-day]
---

# /i.close - End of Day Cleanup

Close current worktree session. Commits pending changes, pushes to remote, and optionally removes worktree.

## Usage

| Command | Action |
|---------|--------|
| `/i.close` | Interactive - asks what to do |
| `/i.close .commit` | Commit + push, keep worktree |
| `/i.close .remove` | Remove worktree (must be clean) |
| `/i.close .force` | Commit + push + remove worktree |
| `/i.close .all` | Close ALL worktrees for this repo |
| `/i.close .list` | List all active worktrees |
| `/i.close .prune` | Remove stale/orphaned worktrees |

## Implementation

### Step 1: Detect Context

```bash
# Check if in worktree
if [[ -f .git ]]; then
    echo "In worktree: $(basename $PWD)"
    WORKTREE_PATH="$PWD"
    MAIN_REPO=$(cat .git | sed 's/gitdir: //' | sed 's/\.git\/worktrees.*//')
else
    echo "In main repo - nothing to close"
    exit 0
fi
```

### Step 2: Check Status

```bash
git status --short
```

| Status | Action |
|--------|--------|
| Clean | Can remove worktree |
| Uncommitted changes | Must commit or stash first |
| Unpushed commits | Must push first |

### Step 3: Commit (if needed)

If uncommitted changes exist:

```bash
git add -A
git commit -m "WIP: End of day commit from worktree"
```

### Step 4: Push

```bash
git push origin HEAD
```

### Step 5: Remove Worktree (if requested)

```bash
# From main repo
cd $MAIN_REPO
git worktree remove $WORKTREE_PATH
```

### Step 6: Output

```
╔══════════════════════════════════════════════════════════════════════════╗
║  SESSION CLOSED                                                          ║
║                                                                          ║
║  Worktree: NewAvanues__t12345                                           ║
║  Committed: 3 files                                                      ║
║  Pushed to: origin/NewAvanues-Development                               ║
║  Worktree: REMOVED                                                       ║
║                                                                          ║
║  To continue tomorrow:                                                   ║
║    cd /Volumes/M-Drive/Coding/NewAvanues && claude                      ║
║                                                                          ║
╚══════════════════════════════════════════════════════════════════════════╝
```

## Modifiers

| Modifier | Effect |
|----------|--------|
| `.commit` | Commit and push only (keep worktree) |
| `.remove` | Remove worktree (must be clean) |
| `.force` | Full close: commit + push + remove |
| `.all` | Close all worktrees for this repo |
| `.list` | List worktrees with status |
| `.prune` | Remove orphaned worktrees |
| `.stash` | Stash instead of commit |
| `.discard` | Discard changes (DANGEROUS) |

## Safety

| Check | Behavior |
|-------|----------|
| Uncommitted changes | Asks to commit or stash |
| Unpushed commits | Warns before remove |
| Main repo | Exits - nothing to close |
| Force without push | Blocks - must push first |

## .list Output

```
ACTIVE WORKTREES:

| Worktree | Branch | Status | Age |
|----------|--------|--------|-----|
| NewAvanues__t12345 | NewAvanues-Development | Clean | 2h |
| NewAvanues__t67890 | feature/auth | 3 uncommitted | 5h |
| VoiceOS__t11111 | main | Clean | 1d |

Orphaned: 2 (run /i.close .prune to remove)
```

## .all Behavior

Closes all worktrees for current repo:

1. List all worktrees matching `{repo}__*`
2. For each: commit, push
3. For each: remove worktree
4. Summary of closed sessions

## Error Handling

| Error | Response |
|-------|----------|
| Merge conflicts | Cannot close - resolve first |
| Push rejected | Pull and retry |
| Worktree locked | Force unlock or manual intervention |

## Examples

```bash
# End of day - commit everything and clean up
/i.close .force

# Just save progress, keep worktree for tomorrow
/i.close .commit

# Clean up all my worktrees
/i.close .all

# See what's active
/i.close .list
```
