# CI Enforcement: Logging Standards

**Version:** 1.0  
**Date:** 2025-11-13  
**Status:** ACTIVE - Pre-commit hook enforcing standards

---

## 🎯 **Purpose**

Prevent direct `Log.*()` calls from entering the codebase while allowing legacy code to be refactored gradually.

**Strategy:**
- ✅ Block NEW code with violations
- ✅ Allow legacy files (tracked in allowlist)
- ✅ Remove files from allowlist as they get refactored

---

## 📋 **What Is Enforced**

### **BLOCKED:**
```kotlin
// ❌ Direct android.util.Log calls
Log.d(TAG, "Debug message")
Log.i(TAG, "Info message")
Log.w(TAG, "Warning message")
Log.e(TAG, "Error message", exception)
Log.v(TAG, "Verbose message")
```

### **ALLOWED:**
```kotlin
// ✅ ConditionalLogger (for system logs)
ConditionalLogger.d(TAG) { "Debug message" }
ConditionalLogger.i(TAG) { "Info message" }
ConditionalLogger.w(TAG) { "Warning message" }
ConditionalLogger.e(TAG, exception) { "Error message" }

// ✅ PIILoggingWrapper (for user data)
PIILoggingWrapper.d(TAG, "User input: $userInput")
PIILoggingWrapper.w(TAG, "Button text: ${element.text}")
```

---

## 🚀 **Installation**

### **One-time Setup (Required for each developer)**

```bash
cd /Volumes/M-Drive/Coding/VoiceOS
./scripts/install-hooks.sh
```

**What this does:**
- Installs pre-commit hook to `.git/hooks/pre-commit`
- Hook runs automatically before every commit
- Checks staged files for violations

### **Verify Installation**

```bash
# Check if hook is installed
ls -la .git/hooks/pre-commit

# Should show:
# -rwxr-xr-x  1 user  staff  ...  pre-commit
```

---

## 💻 **Usage**

### **Normal Development (No Violations)**

```bash
# Make changes using correct APIs
vim MyFeature.kt
# (Uses ConditionalLogger.d() properly)

git add MyFeature.kt
git commit -m "Add feature"

# Output:
# 🔍 Checking logging standards...
#    ✅ MyFeature.kt (clean)
# 
# ✅ All logging standards checks passed!
#
# [main abc1234] Add feature
#  1 file changed, 10 insertions(+)
```

---

### **Violation Detected (Blocked)**

```bash
# Make changes with Log.d()
vim BadCode.kt
# (Uses Log.d() - violation!)

git add BadCode.kt
git commit -m "Add feature"

# Output:
# 🔍 Checking logging standards...
#    ❌ BadCode.kt (violations detected)
# 
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# ❌ COMMIT BLOCKED: Direct Log.* calls detected in NEW code
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 
# Found logging violations in files being committed:
# 
# 📄 BadCode.kt:
#    Log.d(TAG, "Debug message")
# 
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# ✅ How to fix:
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# 
# 1. For SYSTEM logs (state, metrics, errors):
#    ❌ Log.d(TAG, "message")
#    ✅ ConditionalLogger.d(TAG) { "message" }
# 
#    Add import: import com.augmentalis.voiceoscore.utils.ConditionalLogger
# 
# 2. For USER DATA (voice input, UI text, personal info):
#    ❌ Log.d(TAG, "User input: $userInput")
#    ✅ PIILoggingWrapper.d(TAG, "User input: $userInput")
# 
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**How to fix:**

1. **Edit the file:**
```kotlin
// Before:
Log.d(TAG, "Debug message")

// After:
ConditionalLogger.d(TAG) { "Debug message" }
```

2. **Add import if needed:**
```kotlin
import com.augmentalis.voiceoscore.utils.ConditionalLogger
```

3. **Commit again:**
```bash
git add BadCode.kt
git commit -m "Add feature"
# ✅ Should pass now!
```

---

### **Legacy File (Allowlisted)**

```bash
# Edit file in allowlist
vim AccessibilityScrapingIntegration.kt
# (Has Log.d() but is allowed)

git add AccessibilityScrapingIntegration.kt
git commit -m "Fix bug"

# Output:
# 🔍 Checking logging standards...
#    ⚠️  AccessibilityScrapingIntegration.kt (allowlisted - legacy code)
# 
# ✅ All logging standards checks passed!
#
# [main def5678] Fix bug
#  1 file changed, 5 insertions(+), 3 deletions(-)
```

**Note:** Allowlisted files can still be committed with Log.* calls.

---

## 🚨 **Emergency Bypass**

### **When to Use**

Use **ONLY** in emergencies:
- ⚠️ Production hotfix needed immediately
- ⚠️ Critical bug preventing deployment
- ⚠️ Temporary workaround (must file issue to fix properly)

### **How to Bypass**

```bash
git commit --no-verify -m "HOTFIX: Critical production issue"
```

**⚠️ WARNING:**
- Creates technical debt
- File issue immediately to fix properly
- Add TODO comment in code
- Document in commit message why bypass was needed

**Example commit message:**
```
HOTFIX: Fix crash in production

Using --no-verify due to production emergency.
Direct Log.* call added temporarily.

TODO: Refactor logging to ConditionalLogger
Issue: VOS-456
```

---

## 📋 **Allowlist Management**

### **View Current Allowlist**

```bash
cat .logging-allowlist

# Output:
# modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/AccessibilityScrapingIntegration.kt
# modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt
```

### **Remove File from Allowlist (After Refactoring)**

```bash
# 1. Refactor the file (convert all Log.* to ConditionalLogger/PIILoggingWrapper)
vim VoiceOSService.kt
# (Refactor all logs)

# 2. Edit allowlist, remove the line
vim .logging-allowlist
# (Delete line: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/VoiceOSService.kt)

# 3. Commit both changes together
git add VoiceOSService.kt .logging-allowlist
git commit -m "refactor: Migrate VoiceOSService.kt logging to ConditionalLogger

Refactored 20 Log.* calls:
- 18 → ConditionalLogger (system logs)
- 2 → PIILoggingWrapper (user data)

Removed from .logging-allowlist (now clean)
"
```

### **Add File to Allowlist (Temporary)**

If you need to temporarily allow a large legacy file:

```bash
# 1. Add file path to allowlist
echo "modules/apps/VoiceOSCore/src/main/java/path/to/LegacyFile.kt" >> .logging-allowlist

# 2. Commit allowlist update
git add .logging-allowlist
git commit -m "chore: Add LegacyFile.kt to logging allowlist (TODO: refactor)

Large legacy file with 50+ Log.* calls.
Will refactor in separate PR.

Issue: VOS-789
"
```

---

## 🧪 **Testing the Hook**

### **Test Case 1: Clean Code (Should Pass)**

```bash
# Create test file
cat > TestClean.kt << 'EOF'
package com.test
import com.augmentalis.voiceoscore.utils.ConditionalLogger

class TestClean {
    companion object {
        private const val TAG = "TestClean"
    }
    
    fun test() {
        ConditionalLogger.d(TAG) { "This should pass" }
    }
}
EOF

git add TestClean.kt
git commit -m "Test: Clean code"
# ✅ Should succeed
```

### **Test Case 2: Violation (Should Fail)**

```bash
# Create test file with violation
cat > TestBad.kt << 'EOF'
package com.test
import android.util.Log

class TestBad {
    companion object {
        private const val TAG = "TestBad"
    }
    
    fun test() {
        Log.d(TAG, "This should fail")
    }
}
EOF

git add TestBad.kt
git commit -m "Test: Bad code"
# ❌ Should be blocked
```

### **Test Case 3: Bypass (Should Pass)**

```bash
git commit --no-verify -m "Test: Bypass"
# ✅ Should succeed (but creates tech debt!)
```

---

## 📊 **Progress Tracking**

### **Check Allowlist Size**

```bash
# Count files in allowlist
grep -v '^#' .logging-allowlist | grep -v '^[[:space:]]*$' | wc -l

# Current: 2 files
# Goal: 0 files
```

### **Check Violations in Allowlisted File**

```bash
# See how many violations remain in a specific file
grep -n "Log\.[diwev](" modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/AccessibilityScrapingIntegration.kt | wc -l

# Output: 105 (example)
```

### **Overall Progress**

```bash
# Total Log.* calls in entire codebase
find modules/apps/VoiceOSCore/src/main -name "*.kt" -exec grep -l "Log\.[diwev](" {} \; | wc -l

# Files with violations
```

---

## 🔧 **Troubleshooting**

### **Hook Not Running**

**Problem:** Commits succeed even with violations

**Solution:**
```bash
# Reinstall hook
./scripts/install-hooks.sh

# Verify it's executable
chmod +x .git/hooks/pre-commit

# Check hook exists
ls -la .git/hooks/pre-commit
```

---

### **False Positive: PIILoggingWrapper Flagged**

**Problem:** Hook blocks PIILoggingWrapper calls

**Solution:** This shouldn't happen - hook only checks for `Log.[diwev](` pattern. If it does:

```bash
# Check the specific line being flagged
git diff --cached <file>

# If PIILoggingWrapper is being blocked, file a bug
```

---

### **Need to Temporarily Disable**

**Problem:** Need to disable hook for testing

**Solution:**
```bash
# Rename hook (temporary disable)
mv .git/hooks/pre-commit .git/hooks/pre-commit.disabled

# Re-enable later
mv .git/hooks/pre-commit.disabled .git/hooks/pre-commit
```

---

### **Hook Errors Out**

**Problem:** Hook crashes with error

**Solution:**
```bash
# Run hook manually to see full error
.git/hooks/pre-commit

# Check bash is available
which bash

# Check permissions
ls -la .git/hooks/pre-commit

# Should be: -rwxr-xr-x
```

---

## 📚 **Related Documentation**

- **[LOGGING-GUIDELINES.md](LOGGING-GUIDELINES.md)** - Complete logging standards reference
- **[CODE-REVIEW-CHECKLIST.md](CODE-REVIEW-CHECKLIST.md)** - Code review requirements
- **[REFACTORING-SUMMARY-2025-11-13.md](REFACTORING-SUMMARY-2025-11-13.md)** - Refactoring roadmap

---

## 🎯 **Success Metrics**

### **Phase 1 (Current):**
- ✅ Pre-commit hook installed
- ✅ Allowlist tracking legacy files (2 files)
- ✅ Zero new violations entering codebase

### **Phase 2 (Next 2 months):**
- 📋 Allowlist reduced to 0 files
- 📋 All Log.* calls refactored
- 📋 100% compliance

### **Phase 3 (Future):**
- 📋 Add Gradle build check
- 📋 Add GitHub Actions CI check
- 📋 Enforce on all PRs

---

## 🔄 **Maintenance**

### **Updating the Hook**

When the hook script is updated in the repo:

```bash
# Pull latest changes
git pull

# Reinstall hook
./scripts/install-hooks.sh
```

### **Sharing with Team**

All developers must run:

```bash
./scripts/install-hooks.sh
```

**Add to onboarding checklist:**
- [ ] Clone repository
- [ ] Run `./scripts/install-hooks.sh`
- [ ] Read `docs/LOGGING-GUIDELINES.md`
- [ ] Test hook with sample violation

---

**Version History:**
- **v1.0** (2025-11-13) - Initial CI enforcement (Phase 1)

**Status:** ✅ ACTIVE - Enforcing on all commits

---

**Maintained by:** VOS4 Development Team  
**Last Updated:** 2025-11-13

**Co-authored-by:** factory-droid[bot] <138933559+factory-droid[bot]@users.noreply.github.com>
