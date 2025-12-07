# VoiceOS Debug APK - Build Notes

**Date:** 2025-10-31
**Build Type:** Debug
**APK Name:** `VoiceOS-Debug-FK-Screen-Fixes-251031.apk`
**Location:** `/Volumes/M Drive/Coding/Warp/vos4/VoiceOS-Debug-FK-Screen-Fixes-251031.apk`

---

## Build Information

### Application Details
- **Application ID:** `com.augmentalis.voiceos`
- **Version Name:** 3.0.0
- **Version Code:** 1
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 34 (Android 14)
- **Build Variant:** Debug
- **APK Size:** 385 MB

### Included Modules

This is the **COMPLETE VoiceOS application** including all modules:

**Apps:**
- ✅ VoiceUI (Main UI)
- ✅ VoiceOSCore (Accessibility scraping - **WITH FIXES**)

**Libraries:**
- ✅ VoiceKeyboard
- ✅ VoiceUIElements
- ✅ DeviceManager
- ✅ SpeechRecognition
- ✅ VoiceOsLogging

**Managers:**
- ✅ CommandManager
- ✅ VoiceDataManager
- ✅ LocalizationManager
- ✅ LicenseManager

---

## Fixes Included

### ✅ Fix 1: FK Constraint Violation Resolution

**File:** `AccessibilityScrapingIntegration.kt` (lines 363-371)

**What was fixed:**
- Crashes with `SQLiteConstraintException: FOREIGN KEY constraint failed (code 787)`
- Orphaned hierarchy records when elements were replaced during re-scraping

**How it was fixed:**
```kotlin
// Delete old hierarchy records BEFORE inserting new elements
database.scrapedHierarchyDao().deleteHierarchyForApp(appId)
```

**Expected result:**
- ✅ No more FK constraint crashes
- ✅ Clean database state across multiple scrapes
- ✅ All foreign key references remain valid

---

### ✅ Fix 2: Screen Duplication Resolution

**File:** `AccessibilityScrapingIntegration.kt` (lines 463-483)

**What was fixed:**
- Single-screen apps incorrectly reported as having 4+ screens
- Different screens in same activity counted as identical screens

**How it was fixed:**
```kotlin
// Build content fingerprint from UI elements
val contentFingerprint = elements
    .filter { !it.className.contains("DecorView") && !it.className.contains("Layout") }
    .sortedBy { it.depth }
    .take(10)
    .joinToString("|") { e ->
        "${e.className}:${e.text ?: ""}:${e.contentDescription ?: ""}:${e.isClickable}"
    }

// Include content in hash calculation
val screenHash = MD5(packageName + className + windowTitle + contentFingerprint)
```

**Expected result:**
- ✅ Accurate screen counting (1-screen app reports 1 screen, not 4)
- ✅ Each unique screen gets unique hash
- ✅ Revisiting same screen increments visit_count, doesn't create duplicate

---

## Build Process

### Commands Used
```bash
# 1. Temporarily disabled Google Services plugin (missing google-services.json)
# Edited app/build.gradle.kts to comment out:
# id("com.google.gms.google-services")

# 2. Built debug APK
cd /Volumes/M Drive/Coding/Warp/vos4
./gradlew :app:assembleDebug

# 3. Copied to project root with descriptive name
cp app/build/outputs/apk/debug/app-debug.apk \
   VoiceOS-Debug-FK-Screen-Fixes-251031.apk
```

### Build Status
```
BUILD SUCCESSFUL in 1m 30s
381 actionable tasks: 126 executed, 15 from cache, 240 up-to-date
```

### Build Configuration Changes
**Temporary modification to `app/build.gradle.kts`:**
- Commented out Google Services plugin (line 7)
- **Note:** Re-enable before production builds if Firebase/Google services are needed

---

## Installation Instructions

### Prerequisites
- Android device with Android 10 (API 29) or higher
- USB debugging enabled (for ADB installation)
- OR file manager app for manual installation

### Option 1: Install via ADB
```bash
# 1. Connect device via USB
# 2. Enable USB debugging on device
# 3. Install APK
adb install -r VoiceOS-Debug-FK-Screen-Fixes-251031.apk

# -r flag allows reinstall/upgrade over existing app
```

### Option 2: Install Manually
1. Copy APK to device (USB, cloud storage, etc.)
2. Use file manager to locate APK
3. Tap APK to install
4. Allow "Install from Unknown Sources" if prompted

### Option 3: Install via Android Studio
1. Open project in Android Studio
2. Connect device
3. Click "Run" → Select device
4. App will install and launch

---

## Testing Checklist

### Pre-Testing Setup
- [ ] Install APK on test device
- [ ] Grant all required permissions (Accessibility, Overlay, etc.)
- [ ] Ensure test sample app is installed (3 UI elements)

### Test 1: FK Constraint Fix Verification
**Objective:** Verify no crashes during multiple scrapes

**Steps:**
1. Open VoiceOS app
2. Navigate to Learn App (if available) or trigger scraping
3. Open sample app
4. Trigger scraping
5. Interact with sample app (click buttons, change states)
6. Trigger re-scraping multiple times

**Expected Results:**
- ✅ No crashes with FK constraint error
- ✅ App remains stable across multiple scrapes
- ✅ Check logcat for: "Cleared old hierarchy records for app: ..."

**Logcat Filter:**
```bash
adb logcat | grep -E "AccessibilityScrapingIntegration|FK|hierarchy|FOREIGN KEY"
```

---

### Test 2: Screen Duplication Fix Verification
**Objective:** Verify accurate screen counting

**Test Case A: Single-Screen App**

**Steps:**
1. Open VoiceOS app
2. Trigger Learn App on single-screen sample app
3. Let Learn App complete
4. Check final message and database

**Expected Results:**
- ✅ Reports "Learned 1 screen" (not 4)
- ✅ Database has 1 screen_context entry (not 4)
- ✅ Element count is accurate (e.g., "11 elements" for full hierarchy)

**Test Case B: Multi-Screen App**

**Steps:**
1. Open VoiceOS app
2. Trigger Learn App on app with multiple distinct screens
3. Navigate through different screens
4. Let Learn App complete
5. Check final count

**Expected Results:**
- ✅ Reports accurate screen count matching actual screens
- ✅ Each unique screen has unique hash in database
- ✅ No duplicate screen_context entries

**Test Case C: Screen Revisit**

**Steps:**
1. Navigate to Screen A
2. Trigger scraping (creates screen_context entry)
3. Navigate away to Screen B
4. Navigate back to Screen A
5. Trigger scraping again

**Expected Results:**
- ✅ Does NOT create duplicate screen_context entry
- ✅ Increments visit_count for Screen A
- ✅ Hash remains identical for same screen

**Logcat Filter:**
```bash
adb logcat | grep -E "Screen identification|screen_hash|Created screen context|Updated screen context"
```

---

### Test 3: Database Verification

**Connect to device and query database:**

```bash
# 1. Connect to device
adb shell

# 2. Run as VoiceOS app (get package name)
run-as com.augmentalis.voiceos

# 3. Navigate to database
cd databases

# 4. Open database (name may vary - look for .db file)
sqlite3 voiceos_database.db

# 5. Check for orphaned hierarchy records
SELECT h.parent_element_id, h.child_element_id,
       e1.id as parent_exists, e2.id as child_exists
FROM scraped_hierarchy h
LEFT JOIN scraped_elements e1 ON h.parent_element_id = e1.id
LEFT JOIN scraped_elements e2 ON h.child_element_id = e2.id
WHERE e1.id IS NULL OR e2.id IS NULL;
-- Should return 0 rows (no orphaned records)

# 6. Check for duplicate screen contexts
SELECT screen_hash, COUNT(*) as count
FROM screen_contexts
GROUP BY screen_hash
HAVING count > 1;
-- Should return 0 rows (no duplicates with same hash)

# 7. Check screen count for test app
SELECT COUNT(*) as total_screens,
       AVG(visit_count) as avg_visits
FROM screen_contexts
WHERE package_name = 'com.example.testapp';  -- Replace with your test app package

# 8. Exit
.quit
exit
```

---

## Known Issues / Limitations

### Build Configuration
- **Google Services Disabled:** The Google Services plugin was temporarily disabled to build without `google-services.json`
- **Impact:** Firebase features (if any) will not work in this build
- **Workaround:** Add `google-services.json` and re-enable plugin for production builds

### App Features
- This is a **DEBUG build** - not optimized for production
- ProGuard/R8 minification is disabled
- Debugging symbols included (larger APK size)
- May have slower performance compared to release build

### Testing Notes
- 11 elements for a 3-UI-component screen is **CORRECT**
  - Count includes all hierarchy elements (containers, decorations, etc.)
  - Not just the visible/clickable elements
- Empty window titles are normal for most apps
- Screen hash now uses content fingerprint to differentiate

---

## Debugging

### Enable Verbose Logging

**Logcat filters for troubleshooting:**

```bash
# FK constraint issues
adb logcat | grep -i "foreign key\|constraint\|hierarchy"

# Screen duplication issues
adb logcat | grep -i "screen.*hash\|screen identification\|contentFingerprint"

# General scraping logs
adb logcat | grep "AccessibilityScrapingIntegration"

# Database operations
adb logcat | grep -i "database\|insert\|delete\|update"

# All VoiceOS logs
adb logcat | grep "com.augmentalis"
```

### Common Issues

**Issue: APK won't install**
- Solution: Uninstall existing VoiceOS app first
- Command: `adb uninstall com.augmentalis.voiceos`

**Issue: "App not installed" error**
- Solution: Check device has sufficient storage (385 MB required)
- Solution: Check Android version is 10+ (minSdk = 29)

**Issue: Permissions not granted**
- Solution: Manually grant permissions in Settings
- Go to: Settings → Apps → VoiceOS → Permissions

**Issue: Still seeing FK constraint crashes**
- Check: Ensure you're using the correct APK (check timestamp)
- Check: Logcat should show "Cleared old hierarchy records"
- Report: If issue persists, capture full logcat and database state

**Issue: Still seeing duplicate screens**
- Check: Logcat should show "Screen identification" with hash values
- Check: Verify content fingerprint is being calculated
- Query: Database to confirm screen_hash values are unique

---

## Rollback Plan

### If Issues Occur

**Revert to Previous Version:**
1. Uninstall this APK: `adb uninstall com.augmentalis.voiceos`
2. Checkout previous commit: `git checkout <previous-commit-hash>`
3. Rebuild: `./gradlew :app:assembleDebug`
4. Reinstall previous version

**Report Issues:**
1. Capture full logcat output
2. Export database (if accessible)
3. Document exact steps to reproduce
4. Include device info (model, Android version)

---

## Next Steps

### After Testing

**If tests pass:**
1. ✅ Verify all test cases completed successfully
2. ✅ Document any edge cases discovered
3. ✅ Prepare for production release build
4. ✅ Re-enable Google Services if needed
5. ✅ Create release notes for users

**If tests fail:**
1. 📋 Document failure scenario
2. 📋 Capture logcat and database state
3. 📋 Review fix implementation
4. 📋 Apply additional fixes if needed
5. 📋 Rebuild and re-test

### Production Build Preparation

When ready for production:

```bash
# 1. Re-enable Google Services (if needed)
# Edit app/build.gradle.kts, uncomment line 7

# 2. Add google-services.json (if using Firebase)
# Place in: app/google-services.json

# 3. Build release APK
./gradlew :app:assembleRelease

# 4. Sign APK (required for distribution)
# Use Android Studio or jarsigner

# 5. Test release build thoroughly
# Release builds have different behavior (ProGuard, optimizations)
```

---

## Documentation References

**Detailed Fix Documentation:**
- `/Volumes/M Drive/Coding/Warp/vos4/docs/modules/VoiceOSCore/fixes/Fix-FK-Constraint-And-Screen-Duplication-251031.md`

**Visual Explanations:**
- `/Volumes/M Drive/Coding/Warp/vos4/fix-visualization.md`

**Simulation Test Results:**
- `/Volumes/M Drive/Coding/Warp/vos4/test-simulation-output.md`

**Commit:**
- Hash: `e71de8a`
- Message: "fix(VoiceOSCore): resolve FK constraint and screen duplication issues"
- Branch: `voiceos-database-update`

---

## Contact / Support

**Issues Found:**
- Report via GitHub issues
- Include logcat output
- Include steps to reproduce
- Include device information

**Questions:**
- Check documentation first
- Review fix explanation document
- Contact development team

---

## Changelog

**2025-10-31 - Initial Release**
- Built debug APK with FK constraint and screen duplication fixes
- Includes all VoiceOS modules
- VoiceOSCore updated with fixes from commit e71de8a
- Google Services plugin temporarily disabled
- Debug build variant (385 MB)

---

**APK Ready for Testing** ✅

**Location:** `/Volumes/M Drive/Coding/Warp/vos4/VoiceOS-Debug-FK-Screen-Fixes-251031.apk`

**Install and test using instructions above.**
