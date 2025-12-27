# VoiceOS Context Protocol

**Purpose:** Ensure perfect continuity across development sessions
**Version:** 1.0
**Last Updated:** 2025-11-08

---

## 🔄 Session Start Protocol

### Mandatory Steps (Execute in Order)

#### 1. Read Current State
```bash
# Required reading order:
cat /Volumes/M-Drive/Coding/VoiceOS/docs/YOLO-IMPLEMENTATION-STATUS.md
cat /Volumes/M-Drive/Coding/VoiceOS/docs/phase1/PHASE-1-TODO.md
cat /Volumes/M-Drive/Coding/VoiceOS/docs/BLOCKERS.md
cat /Volumes/M-Drive/Coding/VoiceOS/docs/TEST-RESULTS-LATEST.md
```

#### 2. Check Git Status
```bash
cd /Volumes/M-Drive/Coding/VoiceOS
git status
git log -5 --oneline
git diff
```

#### 3. Review Test Results
```bash
# Check last test run
cat docs/TEST-RESULTS-LATEST.md

# Check if there are failures
ls -la modules/apps/VoiceOSCore/build/reports/tests/
```

#### 4. Verify Build State
```bash
# Ensure last build was clean
cat build-status.log 2>/dev/null || echo "No previous build"
```

#### 5. Load Session Context
```markdown
**Current Phase:** [From YOLO-IMPLEMENTATION-STATUS.md]
**Current Task:** [From YOLO-IMPLEMENTATION-STATUS.md]
**Last Completed:** [From git log]
**Next Action:** [From PHASE-N-TODO.md]
**Blockers:** [From BLOCKERS.md]
```

---

## 🛑 Session End Protocol

### Mandatory Steps (Execute in Order)

#### 1. Run Full Test Suite
```bash
cd /Volumes/M-Drive/Coding/VoiceOS

# Unit tests
./gradlew :modules:apps:VoiceOSCore:testDebugUnitTest

# Lint check
./gradlew :modules:apps:VoiceOSCore:lintDebug

# Generate reports
./gradlew :modules:apps:VoiceOSCore:jacocoTestReport
```

#### 2. Save Test Results
```bash
# Copy test results to docs
cp -r modules/apps/VoiceOSCore/build/reports/tests/testDebugUnitTest/ \
     docs/test-results/$(date +%Y%m%d-%H%M%S)/

# Update latest pointer
echo "Last test run: $(date)" > docs/TEST-RESULTS-LATEST.md
echo "Location: docs/test-results/$(date +%Y%m%d-%H%M%S)/" >> docs/TEST-RESULTS-LATEST.md
cat modules/apps/VoiceOSCore/build/test-results/testDebugUnitTest/*.xml | \
    grep -E "(testsuites|testsuite)" >> docs/TEST-RESULTS-LATEST.md
```

#### 3. Update Status Documents
```bash
# Update implementation status
# Manually edit: docs/YOLO-IMPLEMENTATION-STATUS.md
# - Update "Last Updated" timestamp
# - Update "Today's Focus" completed tasks
# - Update "Test Status" counts
# - Update "Metrics" progress
```

#### 4. Update Phase TODO
```bash
# Edit: docs/phaseN/PHASE-N-TODO.md
# - Mark completed tasks with [x]
# - Add new tasks discovered
# - Update priority/estimates if needed
```

#### 5. Document Blockers
```bash
# If any blockers encountered:
# Edit: docs/BLOCKERS.md
# Add:
# - Date discovered
# - Description
# - Impact
# - Mitigation plan
```

#### 6. Commit Changes
```bash
git add .
git commit -m "[Phase N] Descriptive message

- Completed: [List tasks completed]
- Tests: [X passing, Y failing]
- Coverage: [Z%]
- Next: [What's next]

Refs: #issue-number"

# DO NOT push until phase complete and reviewed
```

#### 7. Generate Session Summary
```markdown
## Session Summary - [DATE TIME]

**Duration:** [Start] to [End] ([X hours])

**Completed:**
- Task 1
- Task 2
- Task 3

**Tests Written:** [X tests]
**Tests Passing:** [Y/X]
**Code Coverage:** [Z%]

**Files Modified:**
- File1.kt
- File2.kt
- TestFile1.kt

**Compiler Status:**
- Errors: [0]
- Warnings: [0]
- Lint Issues: [0]

**Blockers:** [None / List them]

**Next Session Focus:**
- Next task 1
- Next task 2

**Notes:**
- Any important observations
- Decisions made
- Technical debt identified
```

---

## 📋 Context Checklist Template

### Start of Session
```markdown
# Session Start Checklist - [DATE]

- [ ] Read YOLO-IMPLEMENTATION-STATUS.md
- [ ] Read PHASE-N-TODO.md
- [ ] Read BLOCKERS.md
- [ ] Read TEST-RESULTS-LATEST.md
- [ ] Check git status and recent commits
- [ ] Review last build status
- [ ] Load mental model of current task
- [ ] Verify development environment ready

**Ready to code:** [YES/NO]
**Current focus:** [Task description]
```

### End of Session
```markdown
# Session End Checklist - [DATE]

- [ ] All files saved
- [ ] Run full test suite
- [ ] Save test results to docs/
- [ ] Update YOLO-IMPLEMENTATION-STATUS.md
- [ ] Update PHASE-N-TODO.md
- [ ] Document any blockers
- [ ] Commit changes with descriptive message
- [ ] Generate session summary
- [ ] Clean workspace (close unnecessary files)
- [ ] Verify 0 errors, 0 warnings

**Session complete:** [YES/NO]
**Continuity preserved:** [YES/NO]
```

---

## 📊 State Preservation

### Critical State Files

**Must be up-to-date after every session:**
1. `docs/YOLO-IMPLEMENTATION-STATUS.md` - Overall status
2. `docs/phaseN/PHASE-N-TODO.md` - Current phase tasks
3. `docs/BLOCKERS.md` - Any impediments
4. `docs/TEST-RESULTS-LATEST.md` - Last test run
5. `docs/SESSION-SUMMARIES.md` - Append each session

**Good to have:**
6. `docs/DECISIONS.md` - Architecture/design decisions
7. `docs/LESSONS-LEARNED.md` - What worked, what didn't
8. `docs/TECHNICAL-DEBT.md` - Known debt to address later

---

## 🔍 Context Recovery (If Lost)

### If starting mid-implementation with unclear state:

```bash
# 1. Check what was last worked on
git log -10 --pretty=format:"%h %ad | %s%d [%an]" --date=short

# 2. See what files were recently modified
git log --name-only --pretty=format: -10 | sort | uniq

# 3. Check for uncommitted changes
git status
git diff

# 4. Review test results
cat docs/TEST-RESULTS-LATEST.md

# 5. Check documentation for current state
grep -r "Status:" docs/*.md

# 6. If completely lost, read in order:
cat docs/YOLO-IMPLEMENTATION-ROADMAP.md     # Overall plan
cat docs/YOLO-IMPLEMENTATION-STATUS.md       # Current state
cat docs/phase1/PHASE-1-TODO.md              # Current phase
cat docs/SESSION-SUMMARIES.md | tail -20     # Recent work
```

---

## 🧠 Mental Model Reconstruction

### Key Questions to Answer Before Coding

1. **What phase are we in?**
   - Answer: Read YOLO-IMPLEMENTATION-STATUS.md

2. **What issue are we currently fixing?**
   - Answer: Read PHASE-N-TODO.md, check topmost uncompleted task

3. **What tests have been written?**
   - Answer: `ls modules/apps/VoiceOSCore/src/test/` and read test files

4. **What tests are passing/failing?**
   - Answer: Read TEST-RESULTS-LATEST.md or run `./gradlew test`

5. **Are there any blockers?**
   - Answer: Read BLOCKERS.md

6. **What was the last commit about?**
   - Answer: `git log -1`

7. **What's the next immediate action?**
   - Answer: Read "Next Steps" in YOLO-IMPLEMENTATION-STATUS.md

---

## 🎯 Development Workflow with Context

### TDD Cycle with Context Preservation

```
1. RED Phase (Write Failing Test)
   ├─ Update PHASE-N-TODO.md: Mark test writing in progress
   ├─ Write test
   ├─ Run test (should fail)
   ├─ Commit: "[Phase N] Add failing test for Issue #X"
   └─ Update STATUS: Tests: X total, Y passing

2. GREEN Phase (Make Test Pass)
   ├─ Update PHASE-N-TODO.md: Mark implementation in progress
   ├─ Implement minimal code
   ├─ Run test (should pass)
   ├─ Commit: "[Phase N] Implement fix for Issue #X"
   └─ Update STATUS: Tests: X total, X passing

3. REFACTOR Phase (Clean Up)
   ├─ Update PHASE-N-TODO.md: Mark refactoring in progress
   ├─ Refactor code
   ├─ Run tests (should still pass)
   ├─ Run lint (should be clean)
   ├─ Commit: "[Phase N] Refactor Issue #X implementation"
   └─ Update STATUS: Coverage improved to Z%

4. DOCUMENT Phase (Add Documentation)
   ├─ Update PHASE-N-TODO.md: Mark documentation in progress
   ├─ Add KDoc comments
   ├─ Update DEVELOPER-MANUAL.md
   ├─ Commit: "[Phase N] Document Issue #X fix"
   └─ Update STATUS: Task complete, move to next

5. REVIEW Phase (Verify Quality)
   ├─ Run full test suite
   ├─ Check coverage report
   ├─ Verify 0 errors, 0 warnings
   ├─ Update all status docs
   └─ Ready for next task
```

---

## 📝 Documentation Standards

### Commit Message Format
```
[Phase N] Brief description (50 chars max)

Detailed explanation of what changed and why.

- Completed: List of completed tasks
- Tests: X total, Y passing, Z% coverage
- Files: List key files modified
- Next: What's next in the plan

Refs: #issue-number
```

### Status Update Format
```markdown
**Last Updated:** YYYY-MM-DD HH:MM AM/PM
**Current Task:** Descriptive task name
**Progress:** X/Y tasks complete

### Recent Changes
- Change 1
- Change 2

### Next Actions
1. Next action 1
2. Next action 2
```

---

## 🚨 Warning Signs of Lost Context

### Indicators that context is degrading:

1. **Unclear what to do next** → Read PHASE-N-TODO.md
2. **Don't remember what tests exist** → Run `./gradlew test` and read reports
3. **Unsure what's been committed** → Run `git log -10`
4. **Don't know current state** → Read YOLO-IMPLEMENTATION-STATUS.md
5. **Multiple failing tests** → Read TEST-RESULTS-LATEST.md
6. **Working on wrong issue** → Read PHASE-N-TODO.md priority order
7. **Documentation outdated** → Check last update timestamp

### Recovery Actions:

1. **STOP CODING** immediately
2. Run full context recovery protocol
3. Update all status documents
4. Verify understanding before continuing
5. When in doubt, read the roadmap again

---

## 🔐 Continuity Guarantees

### This protocol guarantees:

✅ **Any session can pick up exactly where last session left off**
✅ **No work is lost** (all state documented)
✅ **No confusion about priorities** (clear TODO order)
✅ **No duplicate work** (clear what's done)
✅ **No broken builds** (test before commit)
✅ **No missing documentation** (update before end session)
✅ **No orphaned code** (all changes explained in commits)

### This protocol prevents:

❌ Starting work on wrong issue
❌ Forgetting what tests were written
❌ Losing track of blockers
❌ Unclear progress status
❌ Stale documentation
❌ Broken build state
❌ Lost context between sessions

---

**Remember:** 5 minutes updating status documents saves hours of context reconstruction.

**Golden Rule:** If you can't explain what you're about to do in 2 sentences by reading the status docs, stop and update the docs first.

---

**Last Updated:** 2025-11-08
**Version:** 1.0
**Status:** Active
