# VOS4 Migration - AI Agent Instructions
**File:** VOS4MIGRATION-AI-INSTRUCTIONS-250903-0316.md  
**Task:** VOS4 SpeechRecognition Migration  
**Created:** 2025-09-03 03:16  
**Purpose:** Complete instructions for AI agents performing VOS4 migration

## 📋 File Naming Convention

### MANDATORY Format: `TASKNAME-SUMMARY-YYMMDD-HHMM.md`

**Examples:**
- `VOS4MIGRATION-AI-INSTRUCTIONS-250903-0316.md`
- `PHASE1-MANAGER-CREATION-250903-1430.md`
- `ERRORFIX-COMPILATION-250903-0945.md`
- `TESTING-INTEGRATION-250903-1615.md`

**Components:**
- **TASKNAME**: Main task identifier (no spaces, use hyphens)
- **SUMMARY**: Brief description (2-3 words max)
- **YYMMDD**: Date (year-month-day)
- **HHMM**: Time in 24-hour format

**Apply to:**
- All documents in `/docs/ainotes/`
- Migration tracking documents
- Error logs and analysis reports
- Test results and reports

---

## 🎯 Core Mission
Migrate LegacyAvenue VoiceOS to VOS4 with 100% functional equivalence while maintaining continuous compilation and functionality.

**CRITICAL UPDATE (2025-09-03):** Expert analysis reveals VOS4 has only ~15% functional equivalence. LegacyAvenue is a complete production system with:
- Multi-engine speech recognition with four-tier caching
- Full accessibility service with command scraping
- Advanced UI overlay system with gaze tracking
- 42 language support with static and dynamic commands
- Sophisticated service architecture

**Estimated Timeline:** 19-25 weeks (4.5-6 months) for complete migration.

## 🔴 Critical Requirements

### 1. Continuous Compilation Verification
**MANDATORY: After EVERY code change**
```bash
# After ANY modification:
./gradlew :module:compileDebugKotlin  # Compile specific module
./gradlew build                        # Full build if multiple modules

# If errors found:
1. TELL USER IMMEDIATELY: "Found compilation error: [exact error]"
2. Execute TOT+COT+ROT analysis
3. Present fix options to user
4. Fix upon approval
5. Recompile to verify
```

### 2. Functionality Verification
**After each compilation success:**
```bash
./gradlew test                         # Run unit tests
./gradlew installDebug                 # Install on device/emulator
adb shell am start com.augmentalis.voiceos/.MainActivity  # Launch app

# Verify:
- App launches without crash
- Basic navigation works
- No runtime errors in logcat
```

### 3. Error & Warning Protocol
```markdown
## When Compilation Fails:
1. Copy exact error message
2. Tell user: "Compilation failed with error: [error]"
3. Run COT+ROT analysis
4. If complex, run full TOT+COT+ROT
5. Present recommendation matrix
6. Fix upon user approval
7. Document in ERROR-LOG-YYMMDD-HHMM.md

## For Warnings:
1. Fix critical warnings immediately
2. Document non-critical for later
3. Tell user: "Fixed X critical warnings, Y non-critical remain"
```

### 4. Communication Requirements
**ALWAYS inform user of:**
- What you're about to do
- Compilation status
- Errors found
- Fixes being applied
- Test results

**Example Communication:**
```
"Starting Phase 1.1.1: Creating SpeechRecognitionManager skeleton..."
"Compiling module... SUCCESS"
"Running tests... 3/3 passed"
"Task complete. Moving to next task."
```

### 5. Documentation Requirements
**Update after EVERY task:**
- `MIGRATION-TODO-YYMMDD-HHMM.md` - Mark complete, add next
- `MIGRATION-STATUS-YYMMDD-HHMM.md` - Update progress
- `MIGRATION-CHANGELOG-YYMMDD-HHMM.md` - Document changes
- `COMPILATION-LOG-YYMMDD-HHMM.md` - Record build status
- `ERROR-LOG-YYMMDD-HHMM.md` - Log any issues

**Note:** Create new timestamped versions for significant milestones, continue updating current version for minor changes.

## 📋 Phase Execution Checklist

For **EVERY** sub-task (30-60 min chunks):

### Pre-Task:
- [ ] Read task from current MIGRATION-TODO
- [ ] Check COMPILATION-LOG for current state
- [ ] Tell user what task you're starting

### During Task:
- [ ] Make code changes
- [ ] Run compilation after EACH file change
- [ ] Fix errors immediately (with user approval)
- [ ] Tell user about progress/issues

### Post-Task:
- [ ] Run full module compilation
- [ ] Run relevant tests
- [ ] Verify app still launches
- [ ] Update all tracking documents
- [ ] Commit if phase complete

## 🔧 Command Reference

### Essential Build Commands
```bash
# Check current compilation state
./gradlew build --dry-run

# Compile specific module
./gradlew :modules:SpeechRecognition:compileDebugKotlin
./gradlew :apps:VoiceOS:compileDebugKotlin

# Full project build
./gradlew clean build

# Run all tests
./gradlew test
./gradlew connectedAndroidTest

# Install and launch
./gradlew installDebug
adb shell am start com.augmentalis.voiceos/.MainActivity

# Check for warnings
./gradlew lint
```

### Debugging Commands
```bash
# View compilation errors
./gradlew build --stacktrace

# Check dependencies
./gradlew dependencies

# View build tasks
./gradlew tasks

# Monitor app logs
adb logcat | grep -E "VOS4|VoiceOS|SpeechRecognition"
```

## 🔄 Recovery Instructions

### If Context Lost:
1. **Read these files in order:**
   - Latest `/docs/ainotes/VOS4MIGRATION-AI-INSTRUCTIONS-*.md`
   - Latest `/docs/Status/MIGRATION-STATUS-*.md`
   - Latest `/docs/TODO/MIGRATION-TODO-*.md`
   - Latest `/docs/Compilation/COMPILATION-LOG-*.md`
   - Latest `/docs/Errors/ERROR-LOG-*.md`

2. **Check compilation state:**
   ```bash
   ./gradlew build
   ```

3. **Resume from last incomplete task in TODO**

### If Build Broken:
1. Check last working commit:
   ```bash
   git log --oneline -5
   ```

2. Review recent changes:
   ```bash
   git diff HEAD~1
   ```

3. Run TOT+COT+ROT analysis on errors

4. Present rollback vs fix options to user

## 📊 Success Criteria

### Per-Task Success:
- ✅ Code compiles without errors
- ✅ Critical warnings resolved
- ✅ Tests pass (if applicable)
- ✅ App launches successfully
- ✅ Documentation updated

### Per-Phase Success:
- ✅ All tasks complete
- ✅ Integration tests pass
- ✅ COT+ROT verification clean
- ✅ Functionality preserved
- ✅ Committed and pushed

## 🚨 Critical Reminders

1. **NEVER** proceed with errors
2. **ALWAYS** verify compilation after changes
3. **IMMEDIATELY** report issues to user
4. **DOCUMENT** everything in tracking files
5. **TEST** functionality, not just compilation
6. **USE** proper file naming: TASKNAME-SUMMARY-YYMMDD-HHMM

## 📁 File Structure Reference

```
/Volumes/M Drive/Coding/vos4/
├── docs/
│   ├── ainotes/
│   │   ├── VOS4MIGRATION-AI-INSTRUCTIONS-250903-0316.md (THIS FILE)
│   │   └── [TASKNAME]-[SUMMARY]-YYMMDD-HHMM.md (Future files)
│   ├── TODO/
│   │   └── MIGRATION-TODO-YYMMDD-HHMM.md
│   ├── Status/
│   │   └── MIGRATION-STATUS-YYMMDD-HHMM.md
│   ├── CHANGELOG/
│   │   └── MIGRATION-CHANGELOG-YYMMDD-HHMM.md
│   ├── Compilation/
│   │   └── COMPILATION-LOG-YYMMDD-HHMM.md
│   └── Errors/
│       └── ERROR-LOG-YYMMDD-HHMM.md
├── modules/
│   └── SpeechRecognition/ (TO BE CREATED)
├── apps/
│   └── VoiceOS/ (EXISTING)
└── CodeImport/
    └── Archive/
        └── SpeechRecognition/ (SOURCE CODE)
```

## 📝 Document Creation Protocol

When creating new documents:
1. Use format: `TASKNAME-SUMMARY-YYMMDD-HHMM.md`
2. Get timestamp: `date +"%y%m%d-%H%M"`
3. Include header with:
   - File name
   - Task name
   - Creation time
   - Purpose
4. Update index/reference documents

Example header:
```markdown
# [Document Title]
**File:** TASKNAME-SUMMARY-YYMMDD-HHMM.md
**Task:** [Full task description]
**Created:** YYYY-MM-DD HH:MM
**Purpose:** [Why this document exists]
```

---

**File:** VOS4MIGRATION-AI-INSTRUCTIONS-250903-0316.md  
**Last Updated:** 2025-09-03 03:16  
**Purpose:** Ensure continuous compilation and functionality during VOS4 migration  
**Remember:** Tell user everything, fix errors immediately, document continuously, use proper file naming