# JIT Element Deduplication Fix - Manual Test Guide

**Feature ID:** VOS-JIT-006
**Test Date:** 2025-12-03
**Build:** 987f8a92
**Tester:** _______________
**Device:** RealWear HMT-1 / HMT-1Z / Navigator 500

---

## Prerequisites

### Required Hardware
- [ ] RealWear device (HMT-1, HMT-1Z, or Navigator 500)
- [ ] ADB connection (USB or WiFi)
- [ ] Host computer with Android Studio / adb tools

### Required Software
- [ ] VoiceOS v3.0.0+ with fix (build 987f8a92 or later)
- [ ] RealWear launcher installed
- [ ] RealWear apps: My Control, My Camera, My Files

### Setup Steps

1. **Install VoiceOS Build:**
   ```bash
   adb install -r /path/to/voiceos-debug.apk
   ```

2. **Clear Existing Data (Fresh Start):**
   ```bash
   adb shell run-as com.augmentalis.voiceos
   rm -rf databases/
   rm -rf files/learned_apps/
   exit
   ```

3. **Grant Permissions:**
   - Settings → Apps → VoiceOS
   - Grant Accessibility Service permission
   - Grant all other requested permissions

4. **Enable ADB Logcat Monitoring:**
   ```bash
   # Terminal 1: Monitor JIT logs
   adb logcat -s JitElementCapture:* JustInTimeLearner:*

   # Terminal 2: Monitor for cross-app collisions (should be ZERO)
   adb logcat | grep "APP ID MISMATCH"
   ```

---

## Test Case 1: RealWear Launcher (PRIMARY TEST)

**Objective:** Verify that RealWear launcher elements persist correctly after VoiceOS consent dialog

**Steps:**

1. **Launch VoiceOS:**
   ```bash
   adb shell am start -n com.augmentalis.voiceos/.MainActivity
   ```

2. **Navigate to RealWear Launcher:**
   - Say "GO HOME" or press home button
   - RealWear launcher should appear
   - VoiceOS consent dialog should appear overlaid

3. **Observe Log Output (Terminal 1):**
   ```
   Expected:
   ✅ JitElementCapture: Captured 4 elements in XXms for com.realwear.launcher
   ✅ JitElementCapture: Persisted 4 new elements out of 4 captured for com.realwear.launcher
   ```

   ```
   FAIL if you see:
   ❌ JitElementCapture: Persisted 0 new elements out of 4 captured
   ```

4. **Grant Consent:**
   - Say "YES" or "ALLOW"
   - Learning should proceed

5. **Verify in Database:**
   ```bash
   adb shell run-as com.augmentalis.voiceos
   sqlite3 databases/voiceos_database

   SELECT COUNT(*), appId FROM scraped_element
   WHERE appId = 'com.realwear.launcher'
   GROUP BY appId;
   ```

   **Expected Result:**
   ```
   4|com.realwear.launcher
   ```

   **FAIL Result:**
   ```
   0|  (or no rows)
   ```

6. **Check for Cross-App Collisions (Terminal 2):**
   ```
   Expected: NO OUTPUT (zero cross-app collisions)

   FAIL if you see:
   ❌ APP ID MISMATCH: Element abc123 from com.augmentalis.voiceos blocking com.realwear.launcher
   ```

---

## Test Case 2: RealWear My Camera

**Objective:** Verify My Camera app elements persist correctly

**Steps:**

1. **Launch My Camera:**
   - Say "MY CAMERA" or navigate via launcher
   - VoiceOS consent may appear

2. **Grant Consent (if prompted):**
   - Say "YES" or "ALLOW"

3. **Observe Logs:**
   ```
   Expected:
   ✅ JitElementCapture: Captured 10-25 elements in XXms for com.realwear.camera
   ✅ JitElementCapture: Persisted X new elements out of X captured for com.realwear.camera

   Where X > 0
   ```

4. **Verify Database:**
   ```sql
   SELECT COUNT(*) FROM scraped_element WHERE appId = 'com.realwear.camera';
   ```

   **Expected:** Count > 0 (typically 10-25)

---

## Test Case 3: RealWear My Control

**Objective:** Verify My Control app elements persist correctly

**Steps:**

1. **Launch My Control:**
   - Say "MY CONTROL" or navigate via launcher

2. **Grant Consent (if prompted):**
   - Say "YES"

3. **Observe Logs:**
   ```
   Expected:
   ✅ JitElementCapture: Captured 15-30 elements in XXms for com.realwear.controlpanel
   ✅ JitElementCapture: Persisted X new elements for com.realwear.controlpanel
   ```

4. **Verify Database:**
   ```sql
   SELECT COUNT(*) FROM scraped_element WHERE appId = 'com.realwear.controlpanel';
   ```

   **Expected:** Count > 0

---

## Test Case 4: Cross-App Deduplication Verification

**Objective:** Verify that elements with same hash can coexist for different apps

**Steps:**

1. **Capture Multiple Apps:**
   - Launch and allow learning for all RealWear apps
   - Ensure consent dialogs appear (VoiceOS overlay scenario)

2. **Query Database for Duplicate Hashes:**
   ```sql
   -- Find hashes that exist for multiple apps
   SELECT elementHash, GROUP_CONCAT(appId) as apps, COUNT(*) as count
   FROM scraped_element
   GROUP BY elementHash
   HAVING count > 1;
   ```

   **Expected:**
   - Multiple apps can have same hash (e.g., standard "Settings" button)
   - Each row shows different appIds
   - No elements wrongly tagged as `com.augmentalis.voiceos`

   **Example Expected Output:**
   ```
   abc123def456|com.realwear.launcher,com.realwear.camera|2
   ```

3. **Verify appIds are Correct:**
   ```sql
   SELECT appId, elementHash, text, viewIdResourceName
   FROM scraped_element
   WHERE elementHash IN (
       SELECT elementHash FROM scraped_element GROUP BY elementHash HAVING COUNT(*) > 1
   )
   ORDER BY elementHash, appId;
   ```

   **Expected:**
   - Each element has correct appId matching the actual app
   - No `com.augmentalis.voiceos` appIds for RealWear elements

---

## Test Case 5: Timeout Behavior

**Objective:** Verify that 200ms timeout handles overlay scenarios without warnings

**Steps:**

1. **Monitor Timeout Warnings:**
   ```bash
   adb logcat -s JitElementCapture:W
   ```

2. **Launch Apps with Overlays:**
   - Launch RealWear launcher → consent dialog appears
   - Launch My Camera → consent dialog appears

3. **Observe Timeout Logs:**
   ```
   Expected (Normal):
   ✅ JitElementCapture: Captured 25 elements in 134ms for com.realwear.launcher
   (No timeout warning)

   Expected (Near Limit):
   ⚠️ JitElementCapture: Element capture timed out after 205ms for com.realwear.camera
   (Only if capture actually exceeds 200ms)

   FAIL:
   ❌ JitElementCapture: Element capture timed out after 51ms
   (Indicates old 50ms timeout still in effect)
   ```

---

## Test Case 6: Duplicate Element Skip (Same App)

**Objective:** Verify that duplicate elements within the same app are skipped

**Steps:**

1. **Capture Same Screen Twice:**
   - Launch RealWear launcher
   - Wait for capture to complete
   - Go to another app
   - Return to RealWear launcher (same screen)

2. **Observe Second Capture:**
   ```
   Expected:
   ✅ JustInTimeLearner: Screen already captured, skipping: com.realwear.launcher - Hash: abc123...

   OR (if different screen):
   ✅ JitElementCapture: Persisted X new elements
   (Where X = only new elements, not duplicates)
   ```

3. **Verify No Duplicate Entries:**
   ```sql
   SELECT elementHash, COUNT(*) as count
   FROM scraped_element
   WHERE appId = 'com.realwear.launcher'
   GROUP BY elementHash
   HAVING count > 1;
   ```

   **Expected:** 0 rows (no duplicates within same app)

---

## Pass/Fail Criteria

### MUST PASS (Critical)

| Test Case | Criteria | Status |
|-----------|----------|--------|
| TC1: RealWear Launcher | Elements persisted > 0, correct appId | ☐ PASS ☐ FAIL |
| TC2: My Camera | Elements persisted > 0, correct appId | ☐ PASS ☐ FAIL |
| TC4: Cross-App Dedup | Multiple apps can have same hash | ☐ PASS ☐ FAIL |
| TC4: Correct appIds | No RealWear elements tagged as VoiceOS | ☐ PASS ☐ FAIL |

### SHOULD PASS (High Priority)

| Test Case | Criteria | Status |
|-----------|----------|--------|
| TC3: My Control | Elements persisted > 0 | ☐ PASS ☐ FAIL |
| TC5: Timeout | No false warnings <200ms | ☐ PASS ☐ FAIL |
| TC6: Duplicate Skip | Duplicates within app skipped | ☐ PASS ☐ FAIL |

### COULD PASS (Nice to Have)

| Test Case | Criteria | Status |
|-----------|----------|--------|
| TC5: Performance | Capture time <150ms average | ☐ PASS ☐ FAIL |
| All | Zero crash logs during testing | ☐ PASS ☐ FAIL |

---

## Troubleshooting

### Issue: Still seeing "0 elements persisted"

**Check:**
1. Build version contains fix (987f8a92 or later)
2. Database was cleared before testing
3. Logcat shows correct method being called:
   ```bash
   adb logcat | grep "getByHashAndApp"
   ```

### Issue: Cross-app collision errors appearing

**Check:**
1. Clear database and restart test from beginning
2. Verify no old data exists:
   ```sql
   SELECT * FROM scraped_element WHERE appId = 'com.augmentalis.voiceos';
   ```
3. If rows exist with wrong appId, this is OLD data from before fix

### Issue: Timeout warnings appearing

**Check:**
1. Verify timeout constant updated to 200ms:
   ```bash
   grep "CAPTURE_TIMEOUT_MS" modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/jit/JitElementCapture.kt
   ```
   Should show: `private const val CAPTURE_TIMEOUT_MS = 200L`

### Issue: Elements have wrong appId

**This indicates the fix is NOT working:**
1. Verify git commit: `git log --oneline -1` shows `987f8a92` or later
2. Verify SQLDelight generated code:
   ```bash
   ./gradlew :libraries:core:database:generateCommonMainDatabaseInterface
   ```
3. Check generated file for `getByHashAndApp` method

---

## Expected Metrics

### Element Counts (Approximate)

| App | Expected Element Count | Typical Range |
|-----|------------------------|---------------|
| RealWear Launcher | 4 | 4-6 |
| My Camera | 15-20 | 10-25 |
| My Control | 20-25 | 15-30 |
| My Files | 10-15 | 8-20 |

### Performance Metrics

| Metric | Target | Acceptable Range |
|--------|--------|------------------|
| Capture Time (no overlay) | <50ms | 20-100ms |
| Capture Time (with overlay) | <150ms | 100-200ms |
| Persistence Time (25 elements) | <100ms | 50-150ms |
| Database Query Time | <5ms | 1-10ms |

---

## Database Export (For Bug Reports)

If test fails, export database for analysis:

```bash
# Export database
adb shell run-as com.augmentalis.voiceos
cd databases
sqlite3 voiceos_database .dump > /sdcard/voiceos_db.sql
exit

# Pull to host
adb pull /sdcard/voiceos_db.sql ~/Desktop/

# Export logs
adb logcat -d > ~/Desktop/voiceos_logs.txt
```

**Attach to bug report:**
- `voiceos_db.sql` - Database dump
- `voiceos_logs.txt` - Full logcat
- This test guide with results filled in

---

## Test Sign-Off

**Tester Name:** _______________
**Date:** _______________
**Build Tested:** _______________
**Device:** _______________

**Overall Result:**
- [ ] ✅ ALL TESTS PASSED - Ready for production
- [ ] ⚠️ SOME TESTS FAILED - Requires fixes (see notes below)
- [ ] ❌ CRITICAL FAILURE - Do not deploy

**Notes:**
_____________________________________________________________________________
_____________________________________________________________________________
_____________________________________________________________________________

**Reviewer Name:** _______________
**Review Date:** _______________
**Approved for Deployment:** [ ] YES [ ] NO

---

**Version:** 1.0.0
**Created:** 2025-12-03
**Last Updated:** 2025-12-03
**Related Spec:** jit-element-deduplication-fix-spec.md
