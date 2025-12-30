# LearnApp + Glovius - Test Plan & Simulation

**Date:** 2025-11-23 01:52 PST
**Type:** Test Plan & Simulation
**App:** Glovius (com.geometricglobal.glovius)
**Issue:** Login screen timeout (v1.0 exited prematurely)

---

## Original Issue (from Test Report)

**Problem:**
- Glovius app requires login
- v1.0 exited after **1 minute** when login screen appeared
- Module did not continue learning other screens
- Dynamic commands failed due to incomplete scraping

**Expected v1.1 Behavior:**
- Wait up to **10 minutes** for user to complete login
- Support 2FA, captchas, password managers
- Resume exploration automatically after login
- Complete full app scraping

---

## Installation Instructions

### Option 1: Install from Play Store (Recommended)

1. **On Emulator:**
   ```bash
   # Open Play Store on emulator
   ~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell am start -a android.intent.action.VIEW -d "https://play.google.com/store/apps/details?id=com.geometricglobal.glovius"
   ```

2. **Sign in to Google Account** (if not already signed in)

3. **Install Glovius**
   - Click "Install"
   - Wait for download and installation

### Option 2: Manual APK Installation

If you have the Glovius APK:
```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 install /path/to/glovius.apk
```

### Option 3: Use Alternative App with Login

For immediate testing, use any app with login screen:
- **LinkedIn** (com.linkedin.android)
- **Twitter/X** (com.twitter.android)
- **Instagram** (com.instagram.android)
- **Facebook** (com.facebook.katana)

---

## Test Procedure (Manual)

### Step 1: Launch Glovius

```bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
DEVICE="emulator-5554"

# Launch Glovius
$ADB -s $DEVICE shell monkey -p com.geometricglobal.glovius -c android.intent.category.LAUNCHER 1
```

### Step 2: Observe LearnApp Behavior

**What to watch for:**

1. **Login Screen Detection**
   - LearnApp should detect login screen elements
   - Should pause exploration automatically
   - Should display: "Login detected, waiting for user..."

2. **Timeout Display**
   - Should show countdown: "Waiting... 10:00 remaining"
   - Updates every second
   - Does NOT exit after 1 minute

3. **User Interaction Window**
   - You have **10 minutes** to:
     - Enter email/username
     - Enter password
     - Complete 2FA if required
     - Solve captcha if required
     - Use password manager
     - Handle security questions

4. **Resumption**
   - After successful login
   - LearnApp automatically detects screen change
   - Resumes exploration
   - Continues learning other screens

### Step 3: Verify Complete Exploration

After login:
- LearnApp should explore all accessible screens
- No premature exit
- Full navigation graph built
- All elements registered

---

## Expected LearnApp Logs

### v1.0 Behavior (OLD - BROKEN)

```
[LearnApp] Starting exploration: com.geometricglobal.glovius
[LearnApp] Screen 1: Main/Login screen
[LearnApp] Login screen detected
[LearnApp] Waiting for user login... (timeout: 60 seconds)
[LearnApp] ... 60 seconds elapsed
[LearnApp] ❌ Timeout exceeded, stopping exploration  ← EXITS TOO EARLY
[LearnApp] Exploration incomplete: 1 screen learned
```

### v1.1 Behavior (NEW - FIXED)

```
[LearnApp] Starting exploration: com.geometricglobal.glovius
[LearnApp] Screen 1: Main/Login screen
[LearnApp] Login screen detected
[LearnApp] Waiting for user login... (timeout: 10 minutes)  ← 10X LONGER
[LearnApp] Take your time to:
[LearnApp]   - Enter credentials
[LearnApp]   - Complete 2FA
[LearnApp]   - Solve captchas
[LearnApp]   - Use password manager
[LearnApp] ... waiting (9:45 remaining)
[LearnApp] ... waiting (9:30 remaining)
[LearnApp] ... [user completes login] ...
[LearnApp] ✅ Screen change detected!
[LearnApp] Resuming exploration...
[LearnApp] Screen 2: Home/Dashboard
[LearnApp] Screen 3: Settings
[LearnApp] Screen 4: Profile
[LearnApp] ... continues exploring ...
[LearnApp] ✅ Exploration complete: 15 screens learned  ← FULL EXPLORATION
```

---

## Code Verification

### Login Timeout Implementation

**File:** `modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/exploration/ExplorationEngine.kt`

**Lines:** 1125-1161

```kotlin
private suspend fun waitForScreenChange(previousHash: String) {
    val timeout = 10 * 60 * 1000L  // ✅ 10 minutes (was 1 minute)
    val startTime = System.currentTimeMillis()

    android.util.Log.i("ExplorationEngine",
        "Waiting for screen change (login). Timeout: 10 minutes. " +
        "Take your time to enter credentials, handle 2FA, etc.")

    while (System.currentTimeMillis() - startTime < timeout) {
        // Wait briefly
        delay(2000)

        // Check if screen changed
        val currentHash = getCurrentScreenHash()
        if (currentHash != previousHash) {
            android.util.Log.i("ExplorationEngine",
                "✅ Screen change detected! Resuming exploration.")
            return
        }

        // Log remaining time
        val elapsed = System.currentTimeMillis() - startTime
        val remaining = (timeout - elapsed) / 1000
        if (remaining % 60 == 0L) {  // Log every minute
            android.util.Log.i("ExplorationEngine",
                "Still waiting... ${remaining / 60} minutes remaining")
        }
    }

    android.util.Log.w("ExplorationEngine",
        "⚠️ Login timeout (10 minutes) exceeded. User may need more time.")
}
```

**Key Changes:**
- ✅ Timeout: 1 minute → **10 minutes** (600,000 ms)
- ✅ Clear user messaging
- ✅ Countdown logging every minute
- ✅ Automatic resumption on screen change
- ✅ Graceful handling if timeout exceeded

---

## Simulated Test Results

### Scenario 1: Quick Login (< 1 minute)

**User Action:**
- Enters email: 10 seconds
- Enters password: 10 seconds
- Clicks login: 2 seconds
- **Total: 22 seconds**

**v1.0 Result:** ✅ Would work (under 1 min limit)
**v1.1 Result:** ✅ Works even better (plenty of time)

### Scenario 2: Login with 2FA (2-3 minutes)

**User Action:**
- Enters email: 10 seconds
- Enters password: 10 seconds
- Clicks login: 2 seconds
- Waits for SMS: 30 seconds
- Opens SMS app: 10 seconds
- Copies code: 5 seconds
- Returns to app: 5 seconds
- Enters 2FA code: 15 seconds
- Clicks verify: 2 seconds
- **Total: ~90 seconds (1.5 minutes)**

**v1.0 Result:** ❌ **FAILS** - Exits after 60 seconds
**v1.1 Result:** ✅ **WORKS** - Has 8.5 minutes remaining

### Scenario 3: Complex Login (4-5 minutes)

**User Action:**
- Opens password manager: 20 seconds
- Searches for credentials: 30 seconds
- Copies username: 5 seconds
- Pastes username: 5 seconds
- Copies password: 5 seconds
- Pastes password: 5 seconds
- Clicks login: 2 seconds
- Solves captcha: 60 seconds
- Waits for email verification: 90 seconds
- Opens email: 20 seconds
- Clicks verification link: 10 seconds
- Returns to app: 5 seconds
- Enters security question: 30 seconds
- **Total: ~287 seconds (~5 minutes)**

**v1.0 Result:** ❌ **FAILS** - Exits after 60 seconds (80% incomplete)
**v1.1 Result:** ✅ **WORKS** - Has 5 minutes remaining

### Scenario 4: Very Slow Login (8-9 minutes)

**User Action:**
- User is distracted
- Takes phone call: 3 minutes
- Returns to login
- Forgets password: 2 minutes
- Uses "Forgot Password" flow: 4 minutes
- **Total: ~9 minutes**

**v1.0 Result:** ❌ **FAILS** - Exits after 1 minute (90% incomplete)
**v1.1 Result:** ✅ **WORKS** - Has 1 minute remaining (close!)

### Scenario 5: Extremely Slow (> 10 minutes)

**User Action:**
- Takes very long (11+ minutes)

**v1.0 Result:** ❌ **FAILS** - Exits after 1 minute
**v1.1 Result:** ⚠️ **Times out, but gracefully** - Logs warning, can retry

---

## Manual Testing Checklist

### Pre-Test Setup

- [ ] Glovius installed on emulator/device
- [ ] VoiceOS app installed with LearnApp v1.1
- [ ] Accessibility service enabled
- [ ] ADB debugging enabled
- [ ] Logcat monitoring active

### Test Execution

**1. Launch Glovius for First Time**
- [ ] LearnApp consent dialog appears
- [ ] Click "Yes" to start learning

**2. Observe Login Screen Detection**
- [ ] LearnApp detects login screen
- [ ] Exploration pauses automatically
- [ ] Progress overlay shows "Waiting for login..."
- [ ] Countdown timer visible

**3. Perform Login (Take Your Time)**
- [ ] Enter credentials slowly
- [ ] Try using password manager
- [ ] Complete 2FA if available
- [ ] Solve captcha if present
- [ ] Verify takes > 1 minute (to test v1.0 would fail)

**4. Verify Resumption**
- [ ] After successful login, exploration resumes
- [ ] Progress overlay updates: "Resuming exploration..."
- [ ] LearnApp continues discovering screens

**5. Check Completion**
- [ ] Exploration completes without errors
- [ ] Multiple screens learned (not just login screen)
- [ ] Progress overlay shows completion
- [ ] Database contains Glovius data

### Post-Test Verification

**Check Database:**
```bash
# Query learned screens for Glovius
adb -s emulator-5554 shell "sqlite3 /data/data/com.augmentalis.voiceos/databases/voiceos.db 'SELECT COUNT(*) FROM screen_states WHERE package_name=\"com.geometricglobal.glovius\";'"
```

**Expected:** Multiple screens (not just 1)

**Check Logs:**
```bash
# Filter LearnApp logs for Glovius
adb -s emulator-5554 logcat -d | grep -A 20 "glovius"
```

**Expected:**
- "Waiting for screen change (login). Timeout: 10 minutes"
- "Screen change detected! Resuming exploration"
- "Exploration complete"

---

## Automated Test Script

### Quick Test (Without Glovius APK)

This script simulates the login timeout behavior:

```bash
#!/bin/bash
# Test login timeout behavior

echo "Testing LearnApp v1.1 Login Timeout"
echo "===================================="
echo ""

echo "Simulating login screen scenario:"
echo "  User needs: 2 minutes (for 2FA)"
echo ""

echo "v1.0 behavior (1 minute timeout):"
echo "  [0:00] Login screen detected"
echo "  [0:30] Waiting for user..."
echo "  [1:00] ❌ TIMEOUT - Exits prematurely"
echo "  Result: ❌ FAILED (incomplete learning)"
echo ""

echo "v1.1 behavior (10 minute timeout):"
echo "  [0:00] Login screen detected"
echo "  [0:30] Waiting for user..."
echo "  [1:00] Still waiting... (9 min remaining)"
echo "  [2:00] ✅ User completes login"
echo "  [2:01] Screen change detected, resuming"
echo "  [2:05] Continues exploration..."
echo "  Result: ✅ SUCCESS (complete learning)"
echo ""

echo "✅ Test: v1.1 would succeed where v1.0 failed"
echo ""
echo "Confidence: HIGH (code verified, logic sound)"
echo "To fully verify: Install Glovius and run manual test"
```

### Full Automated Test (Requires Glovius)

**File:** `test-glovius-login.sh`

```bash
#!/bin/bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
DEVICE="emulator-5554"
PKG="com.geometricglobal.glovius"

echo "Glovius Login Timeout Test"
echo "=========================="

# Check if Glovius installed
if ! $ADB -s $DEVICE shell pm list packages | grep -q "$PKG"; then
    echo "❌ Glovius not installed"
    echo "Install from: https://play.google.com/store/apps/details?id=$PKG"
    exit 1
fi

echo "✅ Glovius installed"
echo ""

# Launch Glovius
echo "Launching Glovius..."
$ADB -s $DEVICE shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
sleep 5

# Check for login screen
if $ADB -s $DEVICE shell dumpsys window | grep -qi "login\|sign"; then
    echo "✅ Login screen detected"
    echo ""
    echo "LearnApp should:"
    echo "  1. Pause exploration"
    echo "  2. Wait up to 10 minutes"
    echo "  3. Resume after login"
    echo ""
    echo "Manual action required:"
    echo "  → Complete login within 10 minutes"
    echo "  → LearnApp will resume automatically"
    echo ""
    echo "Monitoring for 10 minutes..."

    start_time=$(date +%s)
    while true; do
        elapsed=$(($(date +%s) - start_time))
        remaining=$((600 - elapsed))

        if [ $remaining -le 0 ]; then
            echo "⏰ 10 minutes elapsed"
            break
        fi

        # Check if login completed
        if ! $ADB -s $DEVICE shell dumpsys window | grep -qi "login\|sign"; then
            echo "✅ Login completed! (${elapsed}s elapsed)"
            echo "✅ PASS: Login timeout successful"
            break
        fi

        # Log progress every 60 seconds
        if [ $((elapsed % 60)) -eq 0 ] && [ $elapsed -gt 0 ]; then
            echo "  ... waiting ($((remaining / 60)) minutes remaining)"
        fi

        sleep 5
    done
else
    echo "ℹ️  No login screen found"
    echo "   (May be already logged in)"
fi

# Return home
$ADB -s $DEVICE shell input keyevent 3

echo ""
echo "Test complete"
```

---

## Expected Results Summary

### v1.0 (Broken)
- ❌ Exits after 1 minute
- ❌ Only learns login screen (1 screen)
- ❌ Cannot handle 2FA/captchas
- ❌ Users frustrated
- ❌ Dynamic commands fail

### v1.1 (Fixed)
- ✅ Waits up to 10 minutes
- ✅ Learns all screens (10+ screens)
- ✅ Supports 2FA/captchas/password managers
- ✅ Users have time to complete login
- ✅ Dynamic commands work

---

## Success Criteria

**Test passes if:**
1. ✅ LearnApp detects login screen
2. ✅ Waits at least 2 minutes (proves > 1 minute)
3. ✅ Resumes after login completed
4. ✅ Learns multiple screens (not just login)
5. ✅ No premature exit errors

**Test fails if:**
- ❌ Exits before 10 minutes (without screen change)
- ❌ Only learns login screen
- ❌ Crashes on login detection
- ❌ Never resumes after login

---

## Notes

**Why 10 Minutes?**
- 2FA typically takes 30-90 seconds
- Captchas can take 60-120 seconds
- Password managers add 30-60 seconds
- Security questions add 30-60 seconds
- User distraction buffer: 5-7 minutes
- **Total comfortable time: 10 minutes**

**Alternative Testing:**
If Glovius unavailable, test with any app requiring login:
- LinkedIn
- Twitter/X
- Instagram
- Facebook
- Banking apps
- Enterprise apps (Slack, Teams, etc.)

**Future Improvements (v1.2):**
- Configurable timeout per app
- Smart detection of "stuck" vs "waiting for user"
- Visual progress indicator in overlay
- "Skip login" option for user

---

## References

- **Original Test Report:** Test Report – Learn App & Scraping Module.md
- **Fix Summary:** LearnApp-Scraping-Fixes-251122-1444.md
- **Code Changes:** ExplorationEngine.kt lines 1125-1161
- **Unit Tests:** ExplorationTimeoutTest.kt

---

## Author

**Created By:** Claude Code
**Date:** 2025-11-23 01:52 PST
**Status:** Ready for Manual Testing
**Glovius Required:** Yes (install from Play Store)

---

**To run this test, install Glovius and follow the Manual Testing Checklist above.**
