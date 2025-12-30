---
description: Interactive commit helper with safety checks
tags: [git, commit, workflow]
---

# /i.commit - Safe Interactive Commit Helper

Smart commit assistant that guides you through reviewing and committing changes safely.

**IMPORTANT:** Never auto-commits. Always shows changes and requires confirmation.

---

## Usage

| Command | Action |
|---------|--------|
| `/i.commit` | Interactive mode (shows changes, prompts for message) |
| `/i.commit .yolo` | Quick mode (skip some prompts, still safe) |
| `/i.commit .message "text"` | Provide message upfront |
| `/i.commit .review` | Review only (show changes, don't commit) |

---

## Process Flow

```
1. Check git status
2. Show uncommitted files with diff stats
3. Run safety checks:
   - Secret detection (.env, credentials, API keys)
   - Test status (if tests exist)
   - Branch protection (prevent commits to main)
   - Large files (>1MB warning)
4. Prompt for commit message
5. Show commit preview
6. Require confirmation (Y/n)
7. Execute commit
8. Auto-clear context (if from HANDOVER state)
```

---

## Safety Checks

### 1. Secret Detection

```bash
⚠️  Potential secrets detected:

  .env                    Line 3: API_KEY=sk-...
  config/credentials.json Line 12: "password": "..."

These files should not be committed.
Remove from staging? (Y/n):
```

**Detection patterns:**
- Files: `.env`, `credentials.json`, `secrets.yaml`
- Content: `API_KEY`, `password`, `token`, `secret`, `private_key`
- Already in .gitignore but staged

### 2. Test Status

```bash
⚠️  Tests failing:

  ✗ auth.test.ts - 3 tests failed
  ✗ login.test.ts - 1 test failed

Run tests: npm test

Commit anyway? (y/N):
```

**Checks:**
- Looks for test files (*.test.ts, *.spec.ts)
- Runs `npm test` if package.json exists
- Shows failure count
- Defaults to NO for failed tests

### 3. Branch Protection

```bash
❌ Cannot commit to 'main' branch directly

Current branch: main

Options:
  1. Create feature branch (recommended)
  2. Commit anyway (requires --force)
  3. Cancel

Choose (1/2/3):
```

**Protected branches:**
- `main`
- `master`
- `production`
- `develop` (configurable)

### 4. Large Files

```bash
⚠️  Large files detected:

  video.mp4               45.2 MB
  dataset.csv             12.8 MB

Consider using Git LFS for files >1MB.
Add to .gitignore? (Y/n):
```

---

## Examples

### Example 1: Normal Commit

```
User: /i.commit

AI:
📝 Uncommitted Changes (3 files)

Modified:
  src/auth.ts              (+45 -12)  Login validation
  src/login.ts             (+23 -8)   Error handling

Added:
  src/types.ts             (+15)      Auth types

Review changes? (Y/n):

User: y

AI: [Shows git diff with syntax highlighting]

✅ No secrets detected
✅ Tests passing (12 passed)
✅ Feature branch: feature/login-validation

Commit message:

User: feat: add login validation with error handling

AI:
Preview:
  Branch: feature/login-validation
  Files: 3 (+83 -20)
  Message: feat: add login validation with error handling

Confirm commit? (Y/n):

User: y

AI:
✅ Committed: abc1234 feat: add login validation...
✅ Context cleared (was at 85%)

Ready for next task.
```

### Example 2: Secrets Detected

```
User: /i.commit

AI:
📝 Uncommitted Changes (2 files)

Modified:
  src/config.ts            (+5 -2)
  .env                     (+3)

⚠️  STOP: Secrets detected!

  .env                    Line 3: API_KEY=sk-proj-abc123...

This file should NEVER be committed.

Actions taken:
  ✓ Added .env to .gitignore
  ✓ Removed .env from staging

Safe to commit remaining files? (Y/n):

User: y

AI: [Continues with just config.ts]
```

### Example 3: Main Branch Protection

```
User: /i.commit

AI:
❌ Cannot commit to 'main' branch

Current: main
Uncommitted: 5 files

Create feature branch?
  Suggested: feature/auth-improvements
  Custom name: _

User: feature/login-fix

AI:
✅ Created branch: feature/login-fix
✅ Switched to: feature/login-fix

Commit message: _
```

### Example 4: Review Only

```
User: /i.commit .review

AI:
📝 Uncommitted Changes (6 files)

Modified:
  src/auth.ts              (+45 -12)
  src/login.ts             (+23 -8)
  src/types.ts             (+15 -0)
  tests/auth.test.ts       (+34 -5)

Added:
  src/utils/validation.ts  (+67)
  tests/validation.test.ts (+42)

Diff stats:
  Total: +206 -25
  Test coverage: 2 test files added/modified

[Full diff shown]

Review complete. Run /i.commit to proceed with commit.
```

---

## Modifiers

### `.yolo` - Quick Mode

Skip some prompts, auto-accept safe defaults:

```bash
/i.commit .yolo

# Skips:
# - "Review changes?" prompt (still shows them)
# - "Confirm commit?" prompt (still validates)
#
# Still runs:
# - Secret detection (blocks if found)
# - Branch protection (blocks if main)
# - Shows all changes before committing
```

### `.message "text"` - Provide Message

```bash
/i.commit .message "feat: add login validation"

# Still shows changes and requires confirmation
# Just skips the message prompt
```

### `.review` - Review Only

```bash
/i.commit .review

# Shows all changes and safety checks
# Doesn't commit
# Use for "what would this commit?"
```

### `.force` - Override Protections

```bash
/i.commit .force

# Allows:
# - Commits to main (with extra confirmation)
# - Commits with failing tests (with warning)
#
# Still blocks:
# - Secrets (never allowed)
```

---

## Integration with Memory Manager

After successful commit, auto-clears if:
- Context was in HANDOVER state
- No other uncommitted files remain
- No pending tasks

```bash
# Before commit:
HANDOVER | +6 files | 85%

# After /i.commit:
CLEAR | committed abc1234 | 15%
```

---

## Configuration

### Per-Repo Settings

Create `.claude/commit-config.json`:

```json
{
  "protectedBranches": ["main", "master", "production"],
  "requireTests": true,
  "autoCreateBranch": true,
  "branchPrefix": "feature/",
  "secretPatterns": [
    "API_KEY",
    "SECRET",
    "PASSWORD",
    "TOKEN"
  ],
  "maxFileSize": 1048576,
  "commitMessageFormat": "conventional"
}
```

### Conventional Commits

If `commitMessageFormat: "conventional"`, validates:

```
Valid:
  feat: add login
  fix: resolve crash
  docs: update README

Invalid:
  added login stuff  ❌
  Fixed bug         ❌
```

---

## Error Handling

### No Uncommitted Files

```bash
User: /i.commit

AI:
✅ Working directory clean

No uncommitted files to commit.
```

### Not in Git Repo

```bash
User: /i.commit

AI:
❌ Not a git repository

Initialize git?
  git init

(Y/n):
```

### Commit Failed

```bash
User: /i.commit

AI: [Shows changes, user confirms]

❌ Commit failed:
  error: pathspec '.env' did not match any files

This usually means:
  - File was deleted mid-commit
  - Permissions issue

Run: git status
```

---

## Implementation Checklist

When AI executes `/i.commit`:

- [ ] Run `git status --porcelain` to get uncommitted files
- [ ] Calculate diff stats: `git diff --stat`
- [ ] Check for secrets in staged files
- [ ] Check test status (if applicable)
- [ ] Check current branch protection
- [ ] Show changes to user
- [ ] Prompt for commit message (if not provided)
- [ ] Show commit preview
- [ ] Require explicit confirmation
- [ ] Execute: `git add .` and `git commit -m "message"`
- [ ] Check if context should auto-clear
- [ ] Show success message with commit hash

---

## Security Notes

**NEVER:**
- Auto-commit without user confirmation
- Commit files containing secrets
- Skip showing what's being committed
- Override user's git configuration

**ALWAYS:**
- Show full list of files being committed
- Detect common secret patterns
- Require explicit confirmation
- Respect .gitignore
- Allow user to abort at any step

---

## See Also

- `/i.repo .validate` - Check repo structure
- `memory-manager.sh save` - Manual handover
- `/clear` - Clear context
