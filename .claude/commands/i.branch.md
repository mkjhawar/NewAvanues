---
description: Worktree isolation .createworktree .changeworktree .list .remove .current .lock | /i.branch .createworktree auth
tags: [git, terminal, isolation, worktree]
---

# /i.branch - Worktree & Terminal Isolation

## ⚠️ ZERO TOLERANCE: TERMINAL ISOLATION MANDATORY

**EACH TERMINAL ONLY COMMITS ITS OWN FILES**
- Stage/commit/push ONLY files created/modified in THIS terminal
- NEVER touch files from other terminals unless explicitly instructed
- Each AI instance is responsible for its own worktree ONLY
- Violation = IMMEDIATE STOP

User requested: `/i.branch .createworktree {name}` or `/i.branch .changeworktree {name}`

## Usage

| Command | Action |
|---------|--------|
| `/i.branch .createworktree auth` | Create new worktree with branch feature/auth |
| `/i.branch .createworktree auth .branch custom .from main` | Create with custom branch/base |
| `/i.branch .changeworktree auth` | Switch to existing worktree |
| `/i.branch .list` | List all worktrees |
| `/i.branch .remove auth` | Remove worktree (cleanup) |
| `/i.branch .current` | Show current worktree status |
| `/i.branch .lock auth` | Lock terminal to worktree (legacy alias) |

## Worktree Explanation

**No duplication:**
- Main repo: `/NewAvanues/` has .git (2GB)
- Worktree: `/NewAvanues-auth/` links to same .git (~1MB metadata)
- Shares all commits/history
- Zero data duplication

## Implementation

### .createworktree {name} [.branch branch] [.from base] [.type type]

1. Get repo: `basename $(git rev-parse --show-toplevel)`
2. Determine branch name:
   - If `.branch custom`: use `custom`
   - If `.type bugfix`: prefix `bugfix/{name}`
   - If `.type hotfix`: prefix `hotfix/{name}`
   - Default: `feature/{name}`
3. Determine base branch:
   - If `.from main`: base on `main`
   - Default: current branch
4. Create worktree path: `../{repo}-{name}`
5. Create worktree:
   ```bash
   git worktree add {path} -b {branch} {base}
   ```
6. Output:
   ```
   ✓ Worktree created
     Branch: {branch}
     Path: {path}

     cd {path}
   ```

### .changeworktree {name}

1. List worktrees: `git worktree list`
2. Find worktree matching `*-{name}` or exact path
3. If exists: show `cd {path}`
4. If not exists: offer `/i.branch .createworktree {name}`

### .lock (legacy alias for .createworktree)

Same as `.createworktree` - kept for backwards compatibility

### .list

```bash
git worktree list
```

Show formatted:
```
main     /NewAvanues/          [main]
auth     /NewAvanues-auth/     [feature/auth]
hotfix   /NewAvanues-hotfix/   [hotfix/payment]
```

### .remove

1. Verify not in worktree being removed
2. `git worktree remove ../{repo}-{simplified}`
3. Confirm

### .current

Show:
- Path: `pwd`
- Branch: `git branch --show-current`
- Type: Main repo or worktree
- Disk usage: `du -sh .git`

## Error Handling

| Error | Response |
|-------|----------|
| Branch missing | Create branch? y/n |
| Worktree exists | Show path, cd command |
| Not git repo | Exit |

## Output Format

Compact, command-focused:
```
✓ Worktree created
  Branch: feature/auth
  Path: /Volumes/M-Drive/Coding/NewAvanues-auth

  cd /Volumes/M-Drive/Coding/NewAvanues-auth
```
