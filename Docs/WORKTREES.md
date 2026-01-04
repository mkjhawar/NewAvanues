# Git Worktrees Guide for NewAvanues

## Overview

This repository uses **Git Worktrees** to enable parallel development across multiple branches without conflicts. Each worktree is a separate working directory that shares the same `.git` repository.

**Key benefit**: Multiple terminals/IDEs can work on different branches simultaneously without affecting each other.

---

## First-Time Setup

After cloning or creating a new worktree, run:

```bash
./scripts/setup-hooks.sh
```

This enables the automation hooks (branch switch protection, cleanup reminders).

---

## Quick Reference

| Command | Description |
|---------|-------------|
| `./scripts/setup-hooks.sh` | **First-time setup** - enable hooks |
| `./scripts/worktree-add.sh <branch>` | Create new worktree |
| `./scripts/worktree-list.sh` | List all worktrees |
| `./scripts/worktree-remove.sh <path>` | Remove a worktree |
| `./scripts/worktree-status.sh` | Health check all worktrees |
| `./scripts/worktree-cleanup.sh` | Auto-remove stale worktrees |

---

## Current Worktree Layout

```
/Volumes/M-Drive/Coding/
├── NewAvanues/                    ← Main worktree (VoiceOSCoreNG)
├── NewAvanues__feature__xyz/      ← Feature worktree (convention)
└── Worktrees/
    ├── Development-AVAMagic/      ← AVAMagic-Development
    └── Development-VoiceOS/       ← VoiceOS-Development
```

---

## Creating a Worktree

### For a new feature branch

```bash
cd /Volumes/M-Drive/Coding/NewAvanues
./scripts/worktree-add.sh feature/new-ui main
```

This creates:
- **Path**: `/Volumes/M-Drive/Coding/NewAvanues__feature__new-ui/`
- **Branch**: `feature/new-ui` (based on `main`)

### For an existing remote branch

```bash
./scripts/worktree-add.sh VoiceOS-Development
```

This tracks the existing `origin/VoiceOS-Development` branch.

### Manual creation (if needed)

```bash
# Create from existing branch
git worktree add ../NewAvanues__bugfix-123 bugfix/issue-123

# Create new branch from main
git worktree add -b feature/nlu-speed ../NewAvanues__nlu-speed main
```

---

## Listing Worktrees

```bash
./scripts/worktree-list.sh
# or
git worktree list
```

---

## Removing a Worktree

```bash
./scripts/worktree-remove.sh ../NewAvanues__feature__new-ui
# or by branch name
./scripts/worktree-remove.sh feature/new-ui
```

The script will:
1. Warn if there are uncommitted changes
2. Remove the worktree directory
3. Prune stale metadata

---

## Daily Workflow Rules

### Terminal Usage

- **Terminal A**: `/Volumes/M-Drive/Coding/NewAvanues` (main integration)
- **Terminal B**: `/Volumes/M-Drive/Coding/NewAvanues__feature__xyz` (feature work)
- **Terminal C**: `/Volumes/M-Drive/Coding/NewAvanues__bugfix-123` (bugfix)

### Branch Switching

**DO NOT** switch branches inside a worktree. If you need another branch:
1. Create a new worktree for that branch
2. Open a new terminal in that worktree

### IDE Rules

- Open each worktree in a separate IDE window
- Never open the same worktree in two IDEs simultaneously
- If using Android Studio/IntelliJ: each worktree needs its own project window

---

## Gradle / Build Hygiene

### Shared Gradle Cache

All worktrees share `~/.gradle` cache by default. This is efficient.

### Build Outputs

Each worktree has its own `build/` directory. These are:
- **NOT shared** between worktrees
- **NOT committed** (in `.gitignore`)

### Recommended gradle.properties

```properties
org.gradle.parallel=true
org.gradle.caching=true
```

---

## Troubleshooting

### "Branch is already checked out"

A branch can only be checked out in one worktree at a time.

**Fix**: Either:
- Remove the existing worktree using that branch
- Create a new branch name

### "IDE indexing is weird"

1. Ensure each worktree has its own IDE window
2. Clear IDE caches: `File > Invalidate Caches`
3. Check that `build/` and `.gradle/` are in `.gitignore`

### "Accidentally switched branch in worktree"

1. Switch back: `git checkout <correct-branch>`
2. Stash or commit changes if needed
3. Consider creating a worktree for the other branch

### "Git index write errors"

This repo has a workaround for macOS index write issues:

```bash
# Create index in temp location and copy
GIT_INDEX_FILE=/tmp/git-idx-temp git read-tree HEAD
cat /tmp/git-idx-temp > .git/index
```

---

## Naming Convention

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `NewAvanues__feature__<name>` | `NewAvanues__feature__dark-mode` |
| Bugfix | `NewAvanues__bugfix__<id>` | `NewAvanues__bugfix__123` |
| Development | `NewAvanues__<app>-Development` | `NewAvanues__VoiceOS-Development` |

---

## Automation (Mistake Prevention)

This repo has automation to prevent common worktree mistakes.

### Git Hooks (Automatic)

| Hook | Purpose |
|------|---------|
| `pre-checkout` | **Blocks branch switching** in worktrees. Prompts to create new worktree instead. |
| `post-merge` | After merge, **notifies** if any worktrees can be cleaned up. |

### Status Check

```bash
./scripts/worktree-status.sh
```

Shows:
- Active vs stale worktrees
- Uncommitted changes warning
- Ahead/behind remote status
- Last commit date

### Auto-Cleanup

```bash
# See what would be cleaned (dry run)
./scripts/worktree-cleanup.sh --dry-run

# Interactive cleanup (prompts for each)
./scripts/worktree-cleanup.sh

# Force cleanup all stale (no prompts)
./scripts/worktree-cleanup.sh --force
```

**A worktree is considered stale when:**
1. Its branch was merged to `main` or `Avanues-Main`
2. Its branch was deleted on remote
3. Its branch no longer exists locally

### Bypass Branch Switch Protection

If you absolutely must switch branches in a worktree:

```bash
WORKTREE_ALLOW_CHECKOUT=1 git checkout other-branch
```

**Warning:** This is not recommended. Create a new worktree instead.

---

## Best Practices

1. **One IDE per worktree** - Prevents file watcher conflicts
2. **Don't switch branches** - Create new worktrees instead (hook will remind you)
3. **Run status regularly** - `./scripts/worktree-status.sh`
4. **Clean up after PR merge** - `./scripts/worktree-cleanup.sh`
5. **Keep main clean** - Use feature worktrees for development

---

## Migration from Old Workflow

If you previously used single-directory branch switching:

1. Stash or commit current changes
2. Create worktrees for each branch you work on
3. Open separate IDE windows for each worktree
4. Delete worktrees when branches are merged

---

*Last updated: 2026-01-03*
