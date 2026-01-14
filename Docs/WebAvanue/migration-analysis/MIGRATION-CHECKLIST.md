# Migration Checklist - MainAvanues Monorepo

**Version:** 1.0
**Date:** 2025-11-24
**Purpose:** Step-by-step checklist for each project migration

---

## 🎯 Quick Reference

| Phase | Time | Critical Steps |
|-------|------|----------------|
| **1. Analysis** | 30-60 min | Structure analysis, path mapping |
| **2. Git Prep** | 30 min | Filter-repo, path rewrite |
| **3. Integration** | 1-2 hours | Merge, build config |
| **4. Build/Test** | 2-4 hours | Compile, test, fix errors |
| **5. Verification** | 1-2 weeks | Team validation, cleanup |

**Total:** 5-8 hours active + 1-2 weeks testing

---

## ✅ PHASE 1: Analysis & Planning (30-60 min)

### 1.1 Project Information
- [ ] Project name: ________________
- [ ] Source path: ________________
- [ ] Project type: □ Android App  □ KMP Library  □ KMP Browser  □ Web App
- [ ] Git commits: ______
- [ ] Contributors: ______
- [ ] Last commit hash: ________________

### 1.2 File Inventory
- [ ] Kotlin files: ______
- [ ] Java files: ______
- [ ] XML files: ______
- [ ] Documentation files: ______
- [ ] IDEACODE features: ______

### 1.3 Structure Analysis
- [ ] Has `app/` directory
- [ ] Has `src/` directory
- [ ] Has `universal/` (KMP)
- [ ] Has platform directories (Android/, iOS/, Desktop/)
- [ ] Has `docs/` directory
- [ ] Has `.ideacode/` or `.ideacode-v2/`
- [ ] Has build files (build.gradle.kts, settings.gradle.kts)

### 1.4 Path Mapping Design
- [ ] Created `migration-paths.txt` with all mappings
- [ ] Applied clean naming principles:
  - [ ] No type prefixes (feature-, data-, ui-)
  - [ ] Parent/child for "part of" relationships
  - [ ] Platform grouping (webview/android, /ios)
  - [ ] No scope redundancy
- [ ] Reviewed with team

---

## ✅ PHASE 2: Git History Preparation (30 min)

### 2.1 Backup Original Repo
- [ ] Tagged current state: `pre-monorepo-migration`
- [ ] Pushed tag to remote (if applicable)
- [ ] Created backup copy: `project-backup-YYYYMMDD`
- [ ] Backup location: ________________

### 2.2 Install Tools
- [ ] Git 2.24+ installed: `git --version`
- [ ] git-filter-repo installed: `git filter-repo --version`
- [ ] Temp directory ready: `/tmp/`

### 2.3 Clone to Temp
- [ ] Cloned to `/tmp/project-filtered`
- [ ] Verified git log shows history
- [ ] Noted commit count: ______

### 2.4 Filter and Rewrite Paths
For each path mapping:
- [ ] app/ → target/path/
- [ ] src/ → target/path/
- [ ] universal/ → target/path/
- [ ] docs/ → target/path/
- [ ] .ideacode/ → target/path/
- [ ] (add more as needed)

Commands used:
```bash
git filter-repo --force --path-rename old/:new/
```

### 2.5 Verify Filtered Repo
- [ ] Ran `git log --name-status` - paths look correct
- [ ] Commit count matches original: ______
- [ ] Contributors preserved: `git shortlog -sn`
- [ ] Spot-checked 3 files with `git log --follow`

---

## ✅ PHASE 3: Monorepo Integration (1-2 hours)

### 3.1 Backup Monorepo
- [ ] Created .git backup in `.migration-backups/git-backup-TIMESTAMP`
- [ ] Noted current HEAD: ________________

### 3.2 Add as Remote
- [ ] Added remote: `git remote add project-history /tmp/project-filtered`
- [ ] Fetched: `git fetch project-history`
- [ ] Verified remote log visible

### 3.3 Merge with History
- [ ] Ran merge command with --allow-unrelated-histories
- [ ] Merge commit message includes:
  - [ ] Source path
  - [ ] Migration date
  - [ ] Last original commit hash
  - [ ] Path mappings summary
- [ ] Merge successful (no conflicts or resolved)
- [ ] Removed temporary remote
- [ ] Cleaned up /tmp/project-filtered

### 3.4 Verify Merge
- [ ] New files visible in `git log`
- [ ] Old history accessible: `git log path/to/file`
- [ ] Blame works: `git blame path/to/file`
- [ ] Contributors show in `git shortlog -sn`

---

## ✅ PHASE 4: Build & Test Configuration (2-4 hours)

### 4.1 Update settings.gradle.kts
- [ ] Removed old `includeBuild()` reference (if exists)
- [ ] Added all new module includes:
  - [ ] Android app module
  - [ ] Common libraries
  - [ ] Platform modules
- [ ] Set correct `projectDir` for each module
- [ ] Saved file

### 4.2 Create Build Files
For each module created build.gradle.kts:
- [ ] android/apps/[project]/build.gradle.kts
- [ ] common/libs/[project]/build.gradle.kts
- [ ] common/libs/[project]/[submodule]/build.gradle.kts
- [ ] Platform modules build.gradle.kts

Build files include:
- [ ] Correct plugins
- [ ] Dependencies (including inter-module)
- [ ] Android config (if applicable)
- [ ] KMP targets (if applicable)
- [ ] Version alignment with monorepo

### 4.3 Update Package Names/Imports
- [ ] Identified import changes needed
- [ ] Updated AndroidManifest.xml package name
- [ ] Updated imports in Kotlin files:
  ```bash
  find . -name "*.kt" -exec grep -l "old.package" {} +
  ```
- [ ] Used IDE refactor or sed to replace
- [ ] Spot-checked 5 files manually

### 4.4 Gradle Sync
- [ ] Ran `./gradlew --refresh-dependencies`
- [ ] Sync completed successfully
- [ ] IDE shows no sync errors

### 4.5 Compile Project
- [ ] Compiled specific module: `./gradlew :module:build`
- [ ] Compiled all modules: `./gradlew build`
- [ ] Build successful OR errors logged to:
  - [ ] `docs/develop/[project]/[Project]-Build-Error-YYYYMMDDHHMM.md`

### 4.6 Fix Build Errors (if any)
Error count: ______

For each error:
- [ ] Error 1: ________________ - FIXED
- [ ] Error 2: ________________ - FIXED
- [ ] Error 3: ________________ - FIXED
(add more as needed)

Common fixes applied:
- [ ] Added missing dependencies
- [ ] Fixed package names
- [ ] Aligned versions
- [ ] Updated source sets
- [ ] Fixed resource paths

### 4.7 Run Tests
- [ ] Unit tests: `./gradlew test`
  - [ ] Passed: ______
  - [ ] Failed: ______
- [ ] Android instrumented tests: `./gradlew connectedDebugAndroidTest`
  - [ ] Passed: ______
  - [ ] Failed: ______
- [ ] Test results logged to:
  - [ ] `docs/develop/[project]/[Project]-Test-Results-YYYYMMDDHHMM.md`

### 4.8 Test on Emulator (Android Apps Only)
- [ ] Started emulator: ________________
- [ ] Installed app: `./gradlew :module:installDebug`
- [ ] App launches successfully
- [ ] Tested key features:
  - [ ] Feature 1: ________________ - WORKS
  - [ ] Feature 2: ________________ - WORKS
  - [ ] Feature 3: ________________ - WORKS
- [ ] No crashes or errors
- [ ] Logcat errors captured (if any):
  - [ ] `docs/develop/[project]/[Project]-Logcat-Error-YYYYMMDDHHMM.md`

---

## ✅ PHASE 5: Documentation & Verification (ongoing)

### 5.1 Create Migration Documentation
- [ ] Created `docs/common/libs/[project]/MIGRATION.md` including:
  - [ ] Migration date
  - [ ] Source path
  - [ ] Git history info (commits, contributors)
  - [ ] Files migrated count
  - [ ] Structure changes table
  - [ ] Git commands verification
  - [ ] Original repo status/location

### 5.2 Verify Git Features
- [ ] `git log` shows full history
- [ ] `git blame` shows original authors
- [ ] `git bisect` works across migration
- [ ] IDE git integration works
- [ ] Team members can see history

### 5.3 Update Monorepo Documentation
- [ ] Updated `docs/README.md` with project info
- [ ] Updated `docs/MONOREPO-STRUCTURE.md`
- [ ] Updated `docs/ARCHITECTURE.md`
- [ ] Added to migration registry:
  ```
  [Project],YYYY-MM-DD,[file count],Full history preserved
  ```

### 5.4 Team Validation (1-2 weeks)
Team members tested:
- [ ] Member 1: ________________ - ✅ Approved
- [ ] Member 2: ________________ - ✅ Approved
- [ ] Member 3: ________________ - ✅ Approved

Validation results:
- [ ] All features work correctly
- [ ] No regressions found
- [ ] Tests pass on CI/CD
- [ ] Can build locally
- [ ] Git history accessible
- [ ] No performance issues

Issues found (if any):
- [ ] Issue 1: ________________ - RESOLVED
- [ ] Issue 2: ________________ - RESOLVED

### 5.5 Archive/Delete Old Repo
**Only after 2+ weeks successful testing**

- [ ] Testing period complete: ______ days
- [ ] All issues resolved
- [ ] Team approval obtained
- [ ] Archived to: `/archive/deprecated-repos/[Project]-deprecated-YYYY-MM-DD`
  OR
- [ ] Deleted: `rm -rf /path/to/old/project` (with team approval)
- [ ] Updated settings.gradle.kts (removed any old references)
- [ ] Committed cleanup changes

---

## 📊 Final Summary

### Migration Stats
- **Start date:** ________________
- **Completion date:** ________________
- **Total time:** ______ hours active work + ______ days testing
- **Files migrated:** ______
- **Git commits preserved:** ______
- **Contributors preserved:** ______
- **Build errors:** ______ (all resolved)
- **Test failures:** ______ (all resolved)
- **Team approval:** ✅ / ❌

### Success Criteria
- [ ] ✅ All files in correct locations
- [ ] ✅ Full git history preserved
- [ ] ✅ Git blame works
- [ ] ✅ Project compiles
- [ ] ✅ Tests pass
- [ ] ✅ App runs on emulator
- [ ] ✅ Team validated
- [ ] ✅ No regressions
- [ ] ✅ Documentation complete
- [ ] ✅ Old repo archived/deleted

### Lessons Learned
What went well:
1. ________________
2. ________________
3. ________________

What could be improved:
1. ________________
2. ________________
3. ________________

Notes for next migration:
________________

---

## 🚨 Emergency Rollback (if needed)

If migration fails critically:

### Option 1: Revert Git Merge
```bash
cd /Volumes/M-Drive/Coding/MainAvanues
git log --oneline -5  # Find merge commit
git revert <merge-commit-hash>
```

### Option 2: Restore from Backup
```bash
cd /Volumes/M-Drive/Coding/MainAvanues
rm -rf .git
cp -r .migration-backups/git-backup-<timestamp>/.git .
git reset --hard HEAD
```

### Option 3: Start Fresh
```bash
# Remove migrated files
git rm -r android/apps/[project]
git rm -r common/libs/[project]
git commit -m "Rollback failed migration"

# Restore original repo from backup
cp -r /backup/project-backup-YYYYMMDD /original/location/
```

---

## 📞 Support

**If stuck:**
1. Check [COMPLETE-MIGRATION-GUIDE.md](./COMPLETE-MIGRATION-GUIDE.md) - detailed steps
2. Review [MIGRATION-LESSONS-LEARNED.md](./MIGRATION-LESSONS-LEARNED.md) - common pitfalls
3. Check [foldernaming.md](/Volumes/M-Drive/Coding/ideacode/updateideas/foldernaming.md) - naming rules
4. Ask team lead
5. Create issue in IDEACODE repo

---

**Checklist Version:** 1.0
**Last Updated:** 2025-11-24
**Status:** Production Ready

**Print this checklist and check off items as you complete the migration!**
