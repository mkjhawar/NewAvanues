# Trunk-Based Development - Team Workflow Guide

**Project:** NewAvanues
**Created:** 2025-12-21
**Version:** 1.0
**Audience:** Development Team

---

## Table of Contents

1. [Overview](#overview)
2. [Team Setup](#team-setup)
3. [Daily Workflow](#daily-workflow)
4. [Multi-Developer Scenarios](#multi-developer-scenarios)
5. [Conflict Resolution](#conflict-resolution)
6. [Best Practices](#best-practices)
7. [Common Commands](#common-commands)
8. [Troubleshooting](#troubleshooting)

---

## Overview

### What is Trunk-Based Development?

Trunk-based development is a source control branching model where developers work on **short-lived feature branches** (1-3 days max) and merge frequently into a **single main branch** (our trunk: `Avanues-Main`).

### Key Principles

| Principle | Description |
|-----------|-------------|
| **Single Trunk** | `Avanues-Main` is the single source of truth |
| **Short-lived Branches** | Feature branches live 1-3 days maximum |
| **Frequent Integration** | Merge to trunk daily or multiple times per day |
| **Small Changes** | Keep PRs small (< 400 lines preferred) |
| **Always Releasable** | Trunk is always in a deployable state |

### Benefits for Our Team

- ✅ **No merge hell** - Small, frequent merges instead of massive conflicts
- ✅ **Fast feedback** - Code reviewed and integrated quickly
- ✅ **Clear ownership** - One branch per feature, easy to track
- ✅ **Reduced context switching** - No managing multiple long-lived branches
- ✅ **Better collaboration** - Everyone sees changes quickly

---

## Team Setup

### Repository Structure

```
NewAvanues/  (Single repository, single working directory)
├── Modules/
│   ├── AVA/
│   ├── VoiceOS/
│   ├── WebAvanue/
│   ├── Cockpit/
│   └── Shared/
└── .git/
```

### Each Developer's Setup

Every team member works from **one directory** on their machine:

```bash
# Clone the repository (first time only)
git clone https://gitlab.com/AugmentalisES/newavanues.git
cd newavanues

# Set up your identity (first time only)
git config user.name "Your Name"
git config user.email "your.email@augmentalis.com"

# Verify you're on the trunk
git checkout Avanues-Main
git pull origin Avanues-Main
```

### No Worktrees Needed

**Old way (complicated):**
```
/Coding/NewAvanues-AVA       ← Separate directory for AVA
/Coding/NewAvanues-VoiceOS   ← Separate directory for VoiceOS
/Coding/NewAvanues-WebAvanue ← Separate directory for WebAvanue
```

**New way (simple):**
```
/Coding/NewAvanues           ← ONE directory, switch branches as needed
```

---

## Daily Workflow

### Developer A's Morning (Terminal 1)

**Scenario:** Developer A is adding a new feature to VoiceOS.

```bash
# === Terminal 1: Developer A ===

# 1. Start your day - pull latest trunk
cd /path/to/NewAvanues
git checkout Avanues-Main
git pull origin Avanues-Main

# 2. Create feature branch (short-lived: 1-3 days)
git checkout -b feature/voiceos-gesture-recognition

# 3. Work on your feature (only touch VoiceOS module)
cd Modules/VoiceOS/apps/VoiceOSCore/
# ... make changes to gesture recognition ...

# 4. Commit frequently with clear messages
git add src/main/java/com/augmentalis/voiceoscore/gestures/
git commit -m "feat(voiceos): add gesture recognition engine"

# 5. Keep your branch up-to-date with trunk (do this often!)
git checkout Avanues-Main
git pull origin Avanues-Main
git checkout feature/voiceos-gesture-recognition
git merge Avanues-Main  # Merge trunk into your branch

# 6. Push your branch
git push -u origin feature/voiceos-gesture-recognition

# 7. Create Pull Request in GitLab
# (Use GitLab UI or: gh pr create --title "Add gesture recognition")

# 8. After approval, merge to trunk
git checkout Avanues-Main
git pull origin Avanues-Main
git merge feature/voiceos-gesture-recognition
git push origin Avanues-Main

# 9. Delete your feature branch (done with it!)
git branch -d feature/voiceos-gesture-recognition
git push origin --delete feature/voiceos-gesture-recognition
```

### Developer B's Morning (Terminal 2)

**Scenario:** Developer B is fixing a bug in WebAvanue.

```bash
# === Terminal 2: Developer B ===

# 1. Start your day
cd /path/to/NewAvanues
git checkout Avanues-Main
git pull origin Avanues-Main

# 2. Create bugfix branch
git checkout -b bugfix/webavanue-memory-leak

# 3. Work on your fix (only touch WebAvanue module)
cd Modules/WebAvanue/universal/
# ... fix memory leak ...

# 4. Commit
git add src/commonMain/kotlin/com/augmentalis/webavanue/
git commit -m "fix(webavanue): resolve memory leak in browser tabs"

# 5. Pull latest trunk (Developer A just merged!)
git checkout Avanues-Main
git pull origin Avanues-Main  # Gets Developer A's changes
git checkout bugfix/webavanue-memory-leak
git merge Avanues-Main

# 6. Push and create PR
git push -u origin bugfix/webavanue-memory-leak
# Create PR in GitLab...

# 7. After approval, merge
git checkout Avanues-Main
git pull origin Avanues-Main
git merge bugfix/webavanue-memory-leak
git push origin Avanues-Main

# 8. Delete branch
git branch -d bugfix/webavanue-memory-leak
git push origin --delete bugfix/webavanue-memory-leak
```

### Developer C's Morning (Terminal 3)

**Scenario:** Developer C is working on a critical hotfix.

```bash
# === Terminal 3: Developer C ===

# 1. Start your day
cd /path/to/NewAvanues
git checkout Avanues-Main
git pull origin Avanues-Main

# 2. Create hotfix branch (critical fix, fast-tracked)
git checkout -b hotfix/database-crash

# 3. Fix the critical issue
# ... fix crash ...

# 4. Commit
git add Modules/VoiceOS/core/database/
git commit -m "fix(database): prevent crash on null database driver"

# 5. Push immediately (hotfix is urgent)
git push -u origin hotfix/database-crash

# 6. Request urgent review
# Notify team in chat: "Urgent hotfix PR ready for review"

# 7. After approval, merge immediately
git checkout Avanues-Main
git pull origin Avanues-Main
git merge hotfix/database-crash
git push origin Avanues-Main

# 8. Delete hotfix branch
git branch -d hotfix/database-crash
git push origin --delete hotfix/database-crash

# 9. Notify team: "Hotfix merged, please pull latest trunk!"
```

---

## Multi-Developer Scenarios

### Scenario 1: Two Developers, Same Module, Different Files

**Developer A:** Working on `VoiceOSService.kt`
**Developer B:** Working on `CommandManager.kt`

```bash
# === Developer A ===
git checkout -b feature/voiceos-service-refactor
# Edit VoiceOSService.kt
git commit -m "refactor(voiceos): extract service lifecycle manager"
git push -u origin feature/voiceos-service-refactor
# Create PR, merge to trunk

# === Developer B (later) ===
git checkout Avanues-Main
git pull origin Avanues-Main  # Gets Developer A's changes ✓
git checkout -b feature/command-manager-cache
# Edit CommandManager.kt (no conflict with VoiceOSService.kt)
git commit -m "feat(voiceos): add command cache"
git merge Avanues-Main  # No conflicts! ✓
git push -u origin feature/command-manager-cache
# Create PR, merge to trunk
```

**Result:** No conflicts because different files were modified.

---

### Scenario 2: Two Developers, Same File, Different Sections

**Developer A:** Adding function to top of `BrowserScreen.kt`
**Developer B:** Adding function to bottom of `BrowserScreen.kt`

```bash
# === Developer A (finishes first) ===
git checkout -b feature/browser-search
# Add function at line 50 in BrowserScreen.kt
git commit -m "feat(web): add search function"
git push -u origin feature/browser-search
# Merges to trunk at 10:00 AM

# === Developer B (finishes at 10:30 AM) ===
git checkout -b feature/browser-history
# Add function at line 200 in BrowserScreen.kt
git commit -m "feat(web): add history function"

# Before pushing, sync with trunk
git checkout Avanues-Main
git pull origin Avanues-Main  # Gets Developer A's changes
git checkout feature/browser-history
git merge Avanues-Main

# Git auto-merges: Developer A's change at line 50, yours at line 200 ✓
# No manual conflict resolution needed!

git push -u origin feature/browser-history
# Create PR, merge to trunk
```

**Result:** Git auto-merges because changes are in different sections.

---

### Scenario 3: Two Developers, Same File, Same Function (CONFLICT!)

**Developer A:** Refactoring `processCommand()` function
**Developer B:** Also modifying `processCommand()` function

```bash
# === Developer A (finishes first) ===
git checkout -b refactor/process-command
# Refactor processCommand() - changes lines 100-150
git commit -m "refactor(voiceos): simplify processCommand logic"
git push -u origin refactor/process-command
# Merges to trunk at 2:00 PM

# === Developer B (finishes at 2:30 PM) ===
git checkout -b feature/command-validation
# Modify processCommand() - also changes lines 100-150
git commit -m "feat(voiceos): add command validation"

# Sync with trunk
git checkout Avanues-Main
git pull origin Avanues-Main  # Gets Developer A's refactor
git checkout feature/command-validation
git merge Avanues-Main

# CONFLICT! Git can't auto-merge the same function
# Auto-merging CommandProcessor.kt
# CONFLICT (content): Merge conflict in CommandProcessor.kt
# Automatic merge failed; fix conflicts and then commit the result.

# === Resolve the conflict ===
# Option 1: Manual resolution
vim Modules/VoiceOS/apps/VoiceOSCore/src/.../CommandProcessor.kt

# File will look like this:
# <<<<<<< HEAD (your changes)
# fun processCommand(cmd: Command) {
#     validateCommand(cmd)  # Your addition
#     execute(cmd)
# }
# =======
# fun processCommand(cmd: Command) {
#     // Simplified version
#     execute(cmd)  # Developer A's refactor
# }
# >>>>>>> Avanues-Main

# Merge both changes:
# fun processCommand(cmd: Command) {
#     validateCommand(cmd)  # Keep your validation
#     execute(cmd)          # Keep simplified version
# }

# Mark conflict as resolved
git add Modules/VoiceOS/apps/VoiceOSCore/src/.../CommandProcessor.kt
git commit -m "Merge Avanues-Main into feature/command-validation"

# Option 2: Talk to Developer A
# Slack/Teams: "Hey, we both modified processCommand(). Can we pair to merge?"
# Pair program the resolution together

git push -u origin feature/command-validation
# Create PR, merge to trunk
```

**Result:** Manual conflict resolution required, but caught early (30 min gap, not 3 weeks).

---

### Scenario 4: Three Developers Working in Parallel

**Team working on VoiceOS Phase 4:**

```bash
# === Developer A: Accessibility improvements ===
# Terminal 1
git checkout -b feature/voiceos-accessibility
# Works on Modules/VoiceOS/apps/VoiceOSCore/accessibility/
git commit -m "feat(voiceos): improve screen reader support"
git push -u origin feature/voiceos-accessibility
# Merges at 11:00 AM

# === Developer B: Database optimization ===
# Terminal 2
git checkout -b feature/voiceos-database
# Works on Modules/VoiceOS/core/database/
git commit -m "perf(database): optimize query performance"
# Syncs with trunk at 11:05 AM
git checkout Avanues-Main && git pull origin Avanues-Main
git checkout feature/voiceos-database && git merge Avanues-Main
git push -u origin feature/voiceos-database
# Merges at 11:30 AM

# === Developer C: UI updates ===
# Terminal 3
git checkout -b feature/voiceos-ui
# Works on Modules/VoiceOS/apps/VoiceOSCore/ui/
git commit -m "feat(voiceos): update overlay UI"
# Syncs with trunk at 11:35 AM (gets A's and B's changes)
git checkout Avanues-Main && git pull origin Avanues-Main
git checkout feature/voiceos-ui && git merge Avanues-Main
git push -u origin feature/voiceos-ui
# Merges at 12:00 PM
```

**Timeline:**
```
11:00 AM - Developer A merges (accessibility)
11:30 AM - Developer B merges (database) - includes A's changes
12:00 PM - Developer C merges (UI) - includes A's and B's changes
```

**Result:** Each developer builds on previous work, continuous integration.

---

## Conflict Resolution

### Types of Conflicts

| Type | Likelihood | Difficulty | Prevention |
|------|-----------|------------|------------|
| **Different files** | Never conflicts | N/A | Natural isolation by module |
| **Same file, different sections** | Auto-merges | Easy | Git handles automatically |
| **Same file, same lines** | Conflicts | Medium | Communication + small PRs |
| **Rename + modify** | Conflicts | Hard | Coordinate renames in team chat |

### Conflict Resolution Strategy

#### Step 1: Identify Conflict Type

```bash
git merge Avanues-Main
# Auto-merging src/main/kotlin/MyFile.kt
# CONFLICT (content): Merge conflict in src/main/kotlin/MyFile.kt
# Automatic merge failed; fix conflicts and then commit the result.

# Check what's in conflict
git status
```

#### Step 2: Examine Conflicts

```bash
# View conflicts in file
cat src/main/kotlin/MyFile.kt
```

Example conflict:
```kotlin
<<<<<<< HEAD (your branch)
fun calculateTotal(items: List<Item>): Double {
    return items.sumOf { it.price * it.quantity }
}
=======
fun calculateTotal(items: List<Item>): Money {
    return Money(items.sumOf { it.price.amount * it.quantity })
}
>>>>>>> Avanues-Main (trunk)
```

#### Step 3: Choose Resolution Strategy

**Strategy A: Keep Your Changes**
```bash
git checkout --ours src/main/kotlin/MyFile.kt
git add src/main/kotlin/MyFile.kt
```

**Strategy B: Keep Trunk Changes**
```bash
git checkout --theirs src/main/kotlin/MyFile.kt
git add src/main/kotlin/MyFile.kt
```

**Strategy C: Merge Both (Manual)**
```bash
# Edit file manually
vim src/main/kotlin/MyFile.kt

# Combine both changes:
fun calculateTotal(items: List<Item>): Money {
    // Merge: Keep Money return type from trunk
    // Merge: Keep sumOf syntax from your branch
    return Money(items.sumOf { it.price.amount * it.quantity })
}

git add src/main/kotlin/MyFile.kt
```

**Strategy D: Pair Program (Recommended for complex conflicts)**
```bash
# Slack message: "@developer-who-made-trunk-change Hey, we have a conflict in MyFile.kt. Can we pair to resolve?"
# Screenshare or pair station
# Resolve together
git add src/main/kotlin/MyFile.kt
```

#### Step 4: Complete Merge

```bash
git commit -m "Merge Avanues-Main into feature/my-feature"
git push origin feature/my-feature
```

### Conflict Prevention Checklist

- [ ] **Pull trunk frequently** - At least twice per day
- [ ] **Communicate** - "I'm working on UserService.kt today"
- [ ] **Small PRs** - < 400 lines of changes
- [ ] **Module isolation** - AVA dev stays in Modules/AVA/, etc.
- [ ] **Short-lived branches** - Merge within 1-3 days
- [ ] **Sync before pushing** - Always merge trunk before creating PR

---

## Best Practices

### 1. Branch Naming Conventions

| Type | Pattern | Example | When to Use |
|------|---------|---------|-------------|
| Feature | `feature/module-description` | `feature/ava-login-screen` | New functionality |
| Bug Fix | `bugfix/module-description` | `bugfix/voiceos-crash` | Bug fixes |
| Hotfix | `hotfix/critical-issue` | `hotfix/security-patch` | Critical production fixes |
| Refactor | `refactor/description` | `refactor/solid-compliance` | Code improvements |

### 2. Commit Message Format

Follow **Conventional Commits**:

```
<type>(<scope>): <description>

[optional body]
```

**Types:**
- `feat` - New feature
- `fix` - Bug fix
- `refactor` - Code refactoring
- `perf` - Performance improvement
- `test` - Add/update tests
- `docs` - Documentation only
- `chore` - Build/tooling changes

**Examples:**
```bash
git commit -m "feat(ava): add voice recognition"
git commit -m "fix(voiceos): resolve memory leak in command queue"
git commit -m "refactor(database): apply SOLID SRP to repository pattern"
git commit -m "perf(web): optimize browser tab switching"
```

### 3. Pull Request Guidelines

#### Before Creating PR

- [ ] All tests pass locally: `./gradlew test`
- [ ] Code builds: `./gradlew build`
- [ ] No lint errors
- [ ] Branch is up-to-date with trunk
- [ ] Self-reviewed code

#### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Module(s) Affected
- AVA / VoiceOS / WebAvanue / Cockpit / Shared

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manual testing completed

## Screenshots (if UI changes)
[Add screenshots]

## Checklist
- [ ] Code follows SOLID principles
- [ ] No hardcoded values
- [ ] Error handling implemented
- [ ] Logging added where appropriate
```

#### PR Review Process

1. **Create PR** - Assign to 1+ reviewers
2. **Review** - Reviewer checks code, tests, standards
3. **Approval** - At least 1 approval required
4. **Merge** - Creator merges to trunk
5. **Delete** - Delete feature branch after merge

### 4. Module Isolation

**Rule:** Work within your module's directory to minimize conflicts.

```
Modules/
├── AVA/              ← AVA team works here
├── VoiceOS/          ← VoiceOS team works here
├── WebAvanue/        ← WebAvanue team works here
├── Cockpit/          ← Cockpit team works here
└── Shared/           ← Coordinate changes here
```

**For Shared/ changes:**
- Announce in team chat: "I'm modifying Shared/NLU today"
- Create PR early for visibility
- Coordinate with all teams that depend on Shared/

### 5. Daily Sync Ritual

**Every developer, twice per day:**

```bash
# Morning sync
git checkout Avanues-Main
git pull origin Avanues-Main
git checkout <your-branch>
git merge Avanues-Main

# Afternoon sync (before leaving)
git checkout Avanues-Main
git pull origin Avanues-Main
git checkout <your-branch>
git merge Avanues-Main
git push origin <your-branch>
```

---

## Common Commands

### Quick Reference Card

Print this and keep it near your desk:

```bash
# ====================
# DAILY WORKFLOW
# ====================

# Start day
git checkout Avanues-Main && git pull origin Avanues-Main

# Create feature branch
git checkout -b feature/my-feature

# Make changes
git add .
git commit -m "feat(module): description"

# Sync with trunk
git checkout Avanues-Main && git pull origin Avanues-Main
git checkout feature/my-feature && git merge Avanues-Main

# Push branch
git push -u origin feature/my-feature

# After PR approval
git checkout Avanues-Main && git pull origin Avanues-Main
git merge feature/my-feature
git push origin Avanues-Main

# Clean up
git branch -d feature/my-feature
git push origin --delete feature/my-feature

# ====================
# CONFLICT RESOLUTION
# ====================

# During merge, if conflict:
git status                    # See conflicted files
vim <conflicted-file>         # Fix conflicts manually
git add <conflicted-file>     # Mark as resolved
git commit                    # Complete merge

# Abort merge if needed
git merge --abort

# ====================
# USEFUL CHECKS
# ====================

# What branch am I on?
git branch --show-current

# What changed?
git status
git diff

# What commits are on my branch?
git log --oneline Avanues-Main..HEAD

# Who's working on what?
git branch -a

# ====================
# EMERGENCY
# ====================

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Discard all local changes
git checkout .

# Get latest trunk (destructive)
git checkout Avanues-Main
git reset --hard origin/Avanues-Main
```

---

## Troubleshooting

### Problem 1: "I'm on the wrong branch!"

```bash
# Check current branch
git branch --show-current

# Switch to correct branch
git checkout Avanues-Main

# If you made changes on wrong branch, save them first:
git stash
git checkout correct-branch
git stash pop
```

### Problem 2: "I committed to Avanues-Main by mistake!"

```bash
# If you haven't pushed yet:
git reset --soft HEAD~1    # Undo commit, keep changes
git stash                  # Save changes
git checkout -b feature/my-feature  # Create proper branch
git stash pop              # Restore changes
git commit -m "feat: proper commit message"

# If you already pushed to trunk:
# Contact team lead immediately!
# May need to revert: git revert HEAD
```

### Problem 3: "My branch is 20 commits behind trunk!"

```bash
# This means you haven't synced in a while
git checkout Avanues-Main
git pull origin Avanues-Main

git checkout your-branch
git merge Avanues-Main

# If lots of conflicts:
# Consider rebasing instead (advanced):
git rebase Avanues-Main

# Or ask for help in team chat
```

### Problem 4: "I can't push my branch!"

```bash
# Error: "Updates were rejected because the tip of your current branch is behind"

# Solution: Pull trunk and merge
git checkout Avanues-Main
git pull origin Avanues-Main
git checkout your-branch
git merge Avanues-Main
git push origin your-branch

# Never force push (unless you're 100% sure):
# git push --force  ← DON'T DO THIS
```

### Problem 5: "Someone else is working on the same file!"

```bash
# Prevention is best:
# 1. Announce in team chat: "I'm working on UserService.kt"
# 2. Check who's working on what:
git branch -a | grep feature

# If conflict happens:
# 1. Sync frequently (twice daily)
# 2. Pair program the resolution
# 3. Keep changes small
```

### Problem 6: "My PR has merge conflicts!"

```bash
# On your feature branch:
git checkout Avanues-Main
git pull origin Avanues-Main
git checkout your-feature-branch
git merge Avanues-Main

# Resolve conflicts (see Conflict Resolution section)
git add .
git commit -m "Merge Avanues-Main into feature branch"
git push origin your-feature-branch

# Refresh your PR - conflicts should be gone
```

---

## Communication Patterns

### Team Chat Announcements

**When to announce:**

1. **Starting work on shared code:**
   ```
   "🔧 Working on Shared/NLU/IntentClassifier.kt today"
   ```

2. **Creating a feature branch:**
   ```
   "🌿 Created feature/voiceos-gesture-support (targeting Week 1 merge)"
   ```

3. **Merging to trunk:**
   ```
   "✅ Merged feature/voiceos-gesture-support to trunk. Please pull!"
   ```

4. **Hotfix deployed:**
   ```
   "🚨 URGENT: Hotfix for database crash merged. Pull trunk immediately!"
   ```

5. **Breaking changes:**
   ```
   "⚠️ PR #123 changes Shared/Database API. Review before I merge!"
   ```

### Pair Programming Conflicts

```
Developer A: "Hey @DeveloperB, we both modified CommandProcessor.kt.
              Can we screenshare to resolve the conflict?"

Developer B: "Sure! Joining call now."

[15 min later]

Developer A: "✅ Conflict resolved, merging now."
```

---

## Summary

### Golden Rules

1. ✅ **One trunk** - `Avanues-Main` is the single source of truth
2. ✅ **Short branches** - 1-3 days maximum
3. ✅ **Small PRs** - < 400 lines preferred
4. ✅ **Sync twice daily** - Morning and afternoon
5. ✅ **Communicate** - Announce when working on shared code
6. ✅ **Module isolation** - Stay in your module's directory
7. ✅ **Test before merge** - All tests must pass
8. ✅ **Delete branches** - Clean up after merge

### Team Benefits

| Before (Multi-Worktree) | After (Trunk-Based) |
|-------------------------|---------------------|
| 7 separate directories | 1 directory |
| Weeks-old branches | 1-3 day branches |
| Massive merge conflicts | Small, frequent merges |
| Confusion about "latest" | Clear: trunk is latest |
| Hard to track team work | Easy: see all branches |
| Duplicate disk space | Single copy |

### Need Help?

- **Git questions:** Check this guide or ask in #dev-git channel
- **Merge conflicts:** Pair program with teammate
- **Build issues:** Ask in #dev-support channel
- **Process questions:** Ask team lead

---

## Appendix A: Visualization

### Developer Workflow Diagram

```
Developer A                Developer B                Developer C
    |                          |                          |
    | pull trunk              | pull trunk              | pull trunk
    v                          v                          v
feature/a-1                feature/b-1                feature/c-1
    |                          |                          |
    | work                    | work                    | work
    |                          |                          |
    | commit                  | commit                  | commit
    |                          |                          |
    | merge trunk             |                          |
    | push                    |                          |
    | PR review               |                          |
    |                          |                          |
    +---------> TRUNK <--------+                          |
                 |                                         |
                 | (A's changes now in trunk)             |
                 |                                         |
                 +----------> pull trunk                  |
                              |                           |
                              | merge trunk              |
                              | push                     |
                              | PR review                |
                              |                           |
                 +---------> TRUNK                       |
                              |                           |
                              | (A's + B's changes)      |
                              |                           |
                              +----------> pull trunk     |
                                           |              |
                                           | merge trunk |
                                           | push        |
                                           | PR review   |
                                           |              |
                              +---------> TRUNK          |
                                           |
                                           | (A + B + C)
```

### Timeline Example

```
Time    Developer A              Developer B              Trunk State
----    -----------              -----------              -----------
09:00   Create feature/a         Create feature/b         Clean
10:00   Commit changes           Commit changes           Clean
11:00   Merge to trunk ✓         Still working            A's changes
12:00   Delete branch            Pull trunk, merge        A's changes
13:00   Start new feature        Merge to trunk ✓         A + B changes
14:00   Working...               Delete branch            A + B changes
15:00   Merge to trunk ✓         Start new feature        A + B + A2
16:00   Done for day             Pull trunk, merge        A + B + A2
```

---

**Document Version:** 1.0
**Last Updated:** 2025-12-21
**Next Review:** 2026-01-21

---

**Questions?** Post in #dev-team channel or contact team lead.

**Share this doc:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/NAV-Guide-TrunkBasedDevelopment-251221-V1.md`
