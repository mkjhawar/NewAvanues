# AI Instructions: Trunk-Based Development Workflow

**Purpose:** AI guidance for helping developers with trunk-based development in NewAvanues project
**Target:** Claude, GPT, and other AI assistants
**Version:** 1.0
**Created:** 2025-12-21

---

## Core Rules (ABSOLUTE)

| Rule ID | Rule | Enforcement |
|---------|------|-------------|
| R1 | NEVER commit directly to `Avanues-Main` without feature branch | BLOCKING |
| R2 | ALWAYS sync with trunk before creating PR | BLOCKING |
| R3 | Feature branches live max 3 days | WARNING |
| R4 | PRs must be < 400 lines (preferred) | WARNING |
| R5 | NEVER force push to trunk | BLOCKING |
| R6 | ALWAYS delete branch after merge | REQUIRED |
| R7 | Module isolation: work within assigned module only | RECOMMENDED |
| R8 | Sync with trunk 2x daily minimum | RECOMMENDED |

---

## Decision Trees

### Tree 1: Developer Wants to Start New Work

```
START: Developer says "I want to add feature X"
│
├─ Q1: Is developer on Avanues-Main?
│  ├─ NO → INSTRUCT: git checkout Avanues-Main
│  └─ YES → Continue
│
├─ Q2: Is Avanues-Main up-to-date?
│  ├─ NO → INSTRUCT: git pull origin Avanues-Main
│  └─ YES → Continue
│
├─ Q3: What type of work?
│  ├─ New feature → INSTRUCT: git checkout -b feature/module-description
│  ├─ Bug fix → INSTRUCT: git checkout -b bugfix/module-description
│  ├─ Hotfix → INSTRUCT: git checkout -b hotfix/critical-issue
│  └─ Refactor → INSTRUCT: git checkout -b refactor/description
│
└─ Q4: Which module?
   ├─ AVA → INSTRUCT: cd Modules/AVA/
   ├─ VoiceOS → INSTRUCT: cd Modules/VoiceOS/
   ├─ WebAvanue → INSTRUCT: cd Modules/WebAvanue/
   ├─ Cockpit → INSTRUCT: cd Modules/Cockpit/
   └─ Shared → WARN: "Shared changes affect all modules. Coordinate with team."
```

### Tree 2: Developer Ready to Push Changes

```
START: Developer says "I'm ready to push" or "Create PR"
│
├─ Q1: Are there uncommitted changes?
│  ├─ YES → INSTRUCT: git add <files> && git commit -m "..."
│  └─ NO → Continue
│
├─ Q2: Is branch synced with trunk?
│  ├─ NO → INSTRUCT SEQUENCE:
│  │   1. git checkout Avanues-Main
│  │   2. git pull origin Avanues-Main
│  │   3. git checkout <feature-branch>
│  │   4. git merge Avanues-Main
│  │   5. If conflicts → Route to Tree 3 (Conflict Resolution)
│  └─ YES → Continue
│
├─ Q3: Do tests pass?
│  ├─ NO → BLOCK: "Run ./gradlew test and fix failures first"
│  └─ YES → Continue
│
├─ Q4: Does build succeed?
│  ├─ NO → BLOCK: "Run ./gradlew build and fix errors first"
│  └─ YES → Continue
│
└─ INSTRUCT:
    1. git push -u origin <feature-branch>
    2. Create PR in GitLab
    3. Request review from teammate(s)
```

### Tree 3: Conflict Resolution

```
START: Merge conflict detected
│
├─ Q1: How many files have conflicts?
│  ├─ 1 file → Continue
│  ├─ 2-5 files → WARN: "Multiple conflicts. Consider pair programming."
│  └─ >5 files → CRITICAL: "Many conflicts. Branch may be too old. Consider:"
│      - Rebase instead of merge
│      - Pair with teammate
│      - Break into smaller PRs
│
├─ Q2: What type of conflict?
│  │
│  ├─ CONTENT (UU - both modified)
│  │  ├─ Same function/section
│  │  │  └─ RECOMMEND:
│  │  │      1. OPTION 1: Manual merge both changes
│  │  │      2. OPTION 2: Pair program resolution
│  │  │      3. Show conflict with: git diff
│  │  │
│  │  └─ Different sections
│  │     └─ INSTRUCT: Usually auto-merges. If not:
│  │         1. Edit file manually
│  │         2. git add <file>
│  │         3. git commit
│  │
│  ├─ MODIFY/DELETE (UD - modified in HEAD, deleted in trunk)
│  │  └─ ASK: "This file was deleted in trunk. Do you still need it?"
│  │      ├─ YES → INSTRUCT: git add <file>
│  │      └─ NO → INSTRUCT: git rm <file>
│  │
│  └─ DELETE/MODIFY (DU - deleted in HEAD, modified in trunk)
│     └─ ASK: "Trunk has updates to file you deleted. Keep updates?"
│         ├─ YES → INSTRUCT: git add <file>
│         └─ NO → INSTRUCT: git rm <file>
│
└─ FINAL STEPS:
    1. Verify resolution: git status
    2. Complete merge: git commit -m "Merge Avanues-Main into <branch>"
    3. Test: ./gradlew test
    4. Push: git push origin <branch>
```

### Tree 4: Merge to Trunk (Post PR Approval)

```
START: PR approved, ready to merge
│
├─ Q1: Is developer on feature branch?
│  ├─ YES → INSTRUCT: git checkout Avanues-Main
│  └─ NO → Continue
│
├─ Q2: Is trunk up-to-date?
│  ├─ NO → INSTRUCT: git pull origin Avanues-Main
│  └─ YES → Continue
│
├─ Q3: Merge feature branch
│  └─ INSTRUCT: git merge <feature-branch>
│
├─ Q4: Did merge succeed?
│  ├─ NO → Route to Tree 3 (Conflict Resolution)
│  └─ YES → Continue
│
├─ Q5: Do tests still pass?
│  ├─ NO → BLOCK: Fix tests before pushing
│  └─ YES → Continue
│
├─ INSTRUCT: Push to trunk
│  └─ git push origin Avanues-Main
│
├─ INSTRUCT: Delete feature branch
│  ├─ Local: git branch -d <feature-branch>
│  └─ Remote: git push origin --delete <feature-branch>
│
└─ RECOMMEND: Announce in team chat
    "✅ Merged <feature-branch> to trunk. Please pull!"
```

---

## Pattern Recognition

### Pattern 1: Developer Is Lost

**Indicators:**
- Asks "What branch am I on?"
- Says "I don't know where my changes went"
- Multiple failed merge attempts

**AI Response:**
```bash
# Diagnose current state
git branch --show-current  # Show current branch
git status                # Show working directory state
git log --oneline -5      # Show recent commits

# Provide clear next steps based on state
```

### Pattern 2: Developer Made Mistake

**Indicators:**
- "I committed to the wrong branch"
- "I pushed to Avanues-Main by accident"
- "I need to undo my last commit"

**AI Response:**
```
IF: Committed to wrong branch AND not pushed
  INSTRUCT:
    1. git reset --soft HEAD~1  # Undo commit, keep changes
    2. git stash                # Save changes
    3. git checkout -b correct-branch
    4. git stash pop            # Restore changes
    5. git commit -m "..."

IF: Pushed to Avanues-Main by accident
  CRITICAL:
    1. STOP - Don't make more changes
    2. Contact team lead immediately
    3. Possible solutions:
       - git revert HEAD (safe)
       - git reset --hard HEAD~1 + force push (dangerous, needs approval)

IF: Just want to undo last commit
  INSTRUCT:
    - Keep changes: git reset --soft HEAD~1
    - Discard changes: git reset --hard HEAD~1
```

### Pattern 3: Branch Too Old

**Indicators:**
- "My branch is X commits behind trunk" (X > 20)
- Many merge conflicts
- Branch created > 3 days ago

**AI Response:**
```
WARN: "Your branch is significantly behind trunk. This violates the 3-day rule."

OPTIONS:
1. RECOMMENDED: Merge trunk now
   git checkout Avanues-Main && git pull origin Avanues-Main
   git checkout <your-branch> && git merge Avanues-Main

2. ADVANCED: Rebase (cleaner history, but riskier)
   git fetch origin
   git rebase origin/Avanues-Main
   # If conflicts, resolve and: git rebase --continue

3. NUCLEAR: Start fresh (if too many conflicts)
   git checkout Avanues-Main
   git pull origin Avanues-Main
   git checkout -b <new-branch>
   # Manually reapply your changes

ASK: "Which option do you prefer?"
```

### Pattern 4: Multiple Developers, Same File

**Indicators:**
- Team member announces: "I'm working on FileX.kt"
- Developer wants to edit FileX.kt too

**AI Response:**
```
DETECT: Potential conflict ahead

RECOMMEND:
1. Coordinate with teammate:
   "Hey @teammate, I also need to modify FileX.kt. Can we coordinate?"

2. Options:
   A. Sequential: Wait for teammate to merge first
   B. Parallel: Work on different functions, sync frequently (2x daily)
   C. Pair: Pair program the changes together

3. If proceeding in parallel:
   - Sync with trunk every 2-4 hours
   - Communicate before each push
   - Keep changes small
```

### Pattern 5: Hotfix Urgency

**Indicators:**
- "Critical bug in production"
- "Database is crashing"
- "Users can't login"

**AI Response:**
```
HOTFIX PROTOCOL ACTIVATED

FAST-TRACK STEPS:
1. Create hotfix branch IMMEDIATELY:
   git checkout Avanues-Main
   git pull origin Avanues-Main
   git checkout -b hotfix/<critical-issue>

2. Fix the issue (minimal changes only)

3. Commit and push:
   git add <fix-files>
   git commit -m "fix(critical): <description>"
   git push -u origin hotfix/<critical-issue>

4. Request URGENT review in team chat:
   "🚨 URGENT: Hotfix PR for <issue>. Need immediate review!"

5. After approval, merge IMMEDIATELY:
   git checkout Avanues-Main
   git pull origin Avanues-Main
   git merge hotfix/<critical-issue>
   git push origin Avanues-Main

6. BROADCAST to team:
   "🚨 CRITICAL HOTFIX MERGED. Pull trunk NOW: git pull origin Avanues-Main"

7. Delete hotfix branch:
   git branch -d hotfix/<critical-issue>
   git push origin --delete hotfix/<critical-issue>
```

---

## Command Templates

### Template 1: Daily Workflow (Complete)

```bash
# === Morning Routine ===
cd /path/to/NewAvanues
git checkout Avanues-Main
git pull origin Avanues-Main

# === Start New Feature ===
git checkout -b feature/<module>-<description>
cd Modules/<Module>/

# === Work and Commit ===
# ... make changes ...
git add <files>
git commit -m "<type>(<scope>): <description>"

# === Sync with Trunk (Do 2x daily) ===
git checkout Avanues-Main
git pull origin Avanues-Main
git checkout feature/<branch-name>
git merge Avanues-Main
# If conflicts, resolve them

# === Push for Review ===
git push -u origin feature/<branch-name>
# Create PR in GitLab

# === After PR Approval ===
git checkout Avanues-Main
git pull origin Avanues-Main
git merge feature/<branch-name>
git push origin Avanues-Main

# === Cleanup ===
git branch -d feature/<branch-name>
git push origin --delete feature/<branch-name>

# === End of Day ===
# Make sure you're on trunk for tomorrow
git checkout Avanues-Main
```

### Template 2: Conflict Resolution

```bash
# === When Merge Conflict Occurs ===

# 1. Identify conflicted files
git status | grep "both modified"

# 2. For each conflicted file:
#    Option A: Keep your changes
git checkout --ours <file>
git add <file>

#    Option B: Keep trunk changes
git checkout --theirs <file>
git add <file>

#    Option C: Merge manually
vim <file>  # Edit to resolve conflicts
git add <file>

# 3. Complete merge
git commit -m "Merge Avanues-Main into <branch>"

# 4. Verify and test
git status  # Should be clean
./gradlew test

# 5. Push
git push origin <branch>
```

### Template 3: Emergency Rollback

```bash
# === If Bad Code Got Merged to Trunk ===

# Option 1: Revert (safe, creates new commit)
git checkout Avanues-Main
git pull origin Avanues-Main
git revert HEAD  # Reverts last commit
git push origin Avanues-Main

# Option 2: Reset (dangerous, rewrites history)
# ONLY WITH TEAM LEAD APPROVAL
git checkout Avanues-Main
git pull origin Avanues-Main
git reset --hard HEAD~1  # Go back 1 commit
git push --force origin Avanues-Main  # DANGEROUS!
```

---

## AI Assistance Patterns

### When Developer Asks: "How do I...?"

**Pattern Match:**
- "How do I start a new feature?" → Route to Tree 1
- "How do I push my changes?" → Route to Tree 2
- "How do I fix this merge conflict?" → Route to Tree 3
- "How do I merge to trunk?" → Route to Tree 4
- "How do I undo my commit?" → Pattern 2
- "How do I see what changed?" → `git diff` + `git status`

### When Developer Is Stuck

**Diagnosis Sequence:**
```bash
1. CHECK: Current state
   git branch --show-current
   git status

2. CHECK: Recent history
   git log --oneline -5

3. CHECK: Divergence from trunk
   git log --oneline Avanues-Main..HEAD

4. CHECK: Uncommitted changes
   git diff
   git diff --cached

5. BASED ON OUTPUT: Route to appropriate decision tree
```

### When Providing Commands

**AI Command Format:**
```
1. Explain WHAT the command does
2. Explain WHY it's needed
3. Show the command
4. Explain expected output
5. Provide next steps

EXAMPLE:
"You need to sync your branch with the latest trunk changes to avoid conflicts later.

This command will merge trunk into your feature branch:
git merge Avanues-Main

Expected output:
- If no conflicts: "Merge made by the 'recursive' strategy"
- If conflicts: List of conflicted files

Next steps:
- No conflicts: Continue working
- Conflicts: We'll resolve them together"
```

---

## Module-Specific Rules

### Modules/AVA/

**Isolation:** AVA team works here exclusively
**Dependencies:** May use Shared/NLU
**Coordination:** Announce if modifying Shared/ components

### Modules/VoiceOS/

**Isolation:** VoiceOS team works here exclusively
**Dependencies:** Uses Shared/NLU, core/database
**Special:** LearnApp sub-module frequently modified

### Modules/WebAvanue/

**Isolation:** WebAvanue team works here exclusively
**Tech:** Tauri + React (different from Android/Kotlin modules)
**Dependencies:** Minimal shared code

### Modules/Cockpit/

**Isolation:** Cockpit team works here exclusively
**Tech:** Android/Kotlin
**Dependencies:** May integrate with AVA, VoiceOS

### Modules/Shared/

**CRITICAL:** Changes affect ALL modules
**Protocol:**
1. Announce in team chat before modifying
2. Create PR early for visibility
3. Coordinate with all dependent teams
4. Extra testing required

---

## Success Metrics (AI Tracking)

Track these metrics when helping developers:

| Metric | Target | AI Action |
|--------|--------|-----------|
| Branch age | < 3 days | WARN if > 3 days |
| PR size | < 400 lines | WARN if > 400 lines |
| Trunk sync frequency | 2x daily | REMIND if not synced today |
| Test pass rate | 100% | BLOCK merge if tests fail |
| Conflicts per merge | < 3 files | SUGGEST pair programming if > 3 |
| Time to resolve conflict | < 30 min | ESCALATE if > 30 min |

---

## Error Messages and Responses

### Error: "Updates were rejected"

```
ERROR MESSAGE:
"Updates were rejected because the tip of your current branch is behind"

AI DIAGNOSIS:
Your local branch is outdated. Someone else pushed to trunk.

AI SOLUTION:
git pull origin Avanues-Main
git push origin Avanues-Main

EXPLANATION:
This pulls the latest changes from trunk and then pushes your changes.
```

### Error: "CONFLICT (content)"

```
ERROR MESSAGE:
"CONFLICT (content): Merge conflict in src/main/MyFile.kt"

AI DIAGNOSIS:
You and another developer modified the same part of MyFile.kt

AI SOLUTION:
1. Open MyFile.kt
2. Look for conflict markers:
   <<<<<<< HEAD (your changes)
   =======
   >>>>>>> Avanues-Main (their changes)
3. Choose which changes to keep or merge both
4. Remove conflict markers
5. git add MyFile.kt
6. git commit

OFFER: "Would you like me to show you the conflicted section?"
```

### Error: "fatal: not a git repository"

```
ERROR MESSAGE:
"fatal: not a git repository (or any of the parent directories)"

AI DIAGNOSIS:
You're not in the NewAvanues directory

AI SOLUTION:
cd /path/to/NewAvanues

ASK: "Where is your NewAvanues repository located?"
```

---

## Integration with API Server

### API Endpoints to Support

```json
{
  "endpoints": {
    "POST /git/diagnose": {
      "description": "Analyze current git state",
      "returns": "Current branch, status, conflicts, recommendations"
    },
    "POST /git/workflow": {
      "description": "Generate workflow commands",
      "input": "Developer intent (start feature, push, merge, etc.)",
      "returns": "Step-by-step command sequence"
    },
    "POST /git/resolve-conflict": {
      "description": "Guide conflict resolution",
      "input": "Conflicted file path",
      "returns": "Conflict type, resolution options, commands"
    },
    "POST /git/check-compliance": {
      "description": "Check trunk-based development compliance",
      "returns": "Violations, warnings, metrics"
    }
  }
}
```

### Example API Usage

```javascript
// Developer asks: "I want to add a new feature"

// AI calls API:
POST /git/workflow
{
  "intent": "start_feature",
  "module": "VoiceOS",
  "description": "gesture recognition"
}

// API returns:
{
  "commands": [
    {
      "step": 1,
      "command": "git checkout Avanues-Main",
      "explanation": "Switch to trunk branch"
    },
    {
      "step": 2,
      "command": "git pull origin Avanues-Main",
      "explanation": "Get latest changes from trunk"
    },
    {
      "step": 3,
      "command": "git checkout -b feature/voiceos-gesture-recognition",
      "explanation": "Create new feature branch"
    },
    {
      "step": 4,
      "command": "cd Modules/VoiceOS/",
      "explanation": "Navigate to VoiceOS module"
    }
  ],
  "next_steps": "Make your changes, then ask me when you're ready to commit."
}
```

---

## AI Personality Guidelines

### Tone

- **Supportive:** "Let's fix this together"
- **Clear:** No jargon, explain terms
- **Patient:** Repeat if needed
- **Proactive:** Warn before problems occur

### Avoid

- ❌ "Just run this command" (without explanation)
- ❌ "That's wrong" (judge developer)
- ❌ "You should have..." (blame)
- ❌ Assuming knowledge level

### Encourage

- ✅ "Great job committing frequently!"
- ✅ "Smart thinking to sync with trunk first"
- ✅ "This is a common issue, here's how to fix it"
- ✅ "Would you like me to explain why this happened?"

---

## Glossary (For AI Context)

| Term | Definition | AI Action |
|------|------------|-----------|
| Trunk | `Avanues-Main` branch - single source of truth | Always sync from here |
| Feature Branch | Short-lived branch for one feature | Warn if > 3 days old |
| PR | Pull Request for code review | Must be created before merge |
| Merge Conflict | Two developers changed same code | Guide resolution |
| Hotfix | Critical bug fix, fast-tracked | Use expedited workflow |
| Sync | Pull latest trunk, merge into branch | Recommend 2x daily |
| Module Isolation | Work within one module directory | Prevent cross-module conflicts |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-21 | Initial AI instructions |

---

**AI Usage:** Load this document into context when helping developers with git workflows in NewAvanues project.

**Human-Readable Version:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/NAV-Guide-TrunkBasedDevelopment-251221-V1.md`
