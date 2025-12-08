<!--
Filename: Manual-Testing-Guide-251026.md
Created: 2025-10-26
Project: AvaCode Plugin Infrastructure
Purpose: Comprehensive manual testing guide for all platforms before production deployment
Last Modified: 2025-10-26
Version: v1.0.0
-->

# Manual Testing Guide - AvaCode Plugin Infrastructure

**Date:** 2025-10-26
**Purpose:** Pre-production manual testing on all platforms (Android, JVM, iOS)
**Prerequisites:** Build system configured, test plugins available

---

## Executive Summary

While the plugin infrastructure has comprehensive unit test coverage (75-80%, 282+ tests), **manual testing on real devices is essential** before production deployment.

**Testing Goals:**
1. Verify real-world functionality on actual platforms
2. Test UI/UX for permission dialogs
3. Validate permission persistence across app restarts
4. Verify plugin loading and asset resolution
5. Test security enforcement (signatures, permissions, isolation)

**Estimated Time:** 1-2 days (4-8 hours)

---

## Test Environment Setup

### Prerequisites

**All Platforms:**
- AvaCode runtime built and installed
- At least 2 test plugins available:
  - Simple plugin (no permissions, basic assets)
  - Complex plugin (requires permissions: CAMERA, STORAGE, LOCATION)

**Android:**
- Android device or emulator (API 24+)
- ADB installed and configured
- Test APK signed with debug certificate

**JVM (Desktop):**
- Java 11+ installed
- Desktop environment (Windows/macOS/Linux)
- Test JAR files with manifests

**iOS:**
- Xcode 14+
- iOS simulator or physical device (iOS 13+)
- Test app built with plugin support

---

## Test Plugins

### Plugin 1: Simple Plugin (No Permissions)

**Plugin ID:** `com.avacode.test.simple`

**Manifest:**
```yaml
id: com.avacode.test.simple
name: "Simple Test Plugin"
version: "1.0.0"
minApiVersion: 1
permissions: []
assets:
  - id: "greeting"
    path: "assets/greeting.txt"
```

**Assets:**
- `assets/greeting.txt`: "Hello from Simple Plugin!"

**Purpose:** Test basic plugin loading without permission complexity

---

### Plugin 2: Complex Plugin (Requires Permissions)

**Plugin ID:** `com.avacode.test.complex`

**Manifest:**
```yaml
id: com.avacode.test.complex
name: "Complex Test Plugin"
version: "1.0.0"
minApiVersion: 1
permissions:
  - CAMERA
  - STORAGE
  - LOCATION
assets:
  - id: "config"
    path: "assets/config.json"
  - id: "icon"
    path: "assets/icon.png"
```

**Purpose:** Test permission requests, persistence, and revocation

---

## Android Testing Checklist

### Setup

- [ ] Build AvaCode Android app
- [ ] Install on device/emulator
- [ ] Copy test plugins to device storage
- [ ] Clear app data (start fresh)

### Test 1: Simple Plugin Loading

**Steps:**
1. Launch AvaCode app
2. Navigate to "Load Plugin" screen
3. Select `simple-plugin.apk` or `.zip`
4. Tap "Install"

**Expected Results:**
- ✅ Plugin loads successfully
- ✅ No permission dialogs shown
- ✅ Plugin appears in "Installed Plugins" list
- ✅ Plugin metadata shows: name, version, ID

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 2: Asset Resolution (Simple Plugin)

**Steps:**
1. With Simple Plugin loaded, navigate to asset viewer
2. Enter asset URI: `plugin://com.avacode.test.simple/assets/greeting.txt`
3. Tap "Load Asset"

**Expected Results:**
- ✅ Asset resolves successfully
- ✅ Content displays: "Hello from Simple Plugin!"
- ✅ No errors in logs

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 3: Permission Request Dialog (Complex Plugin)

**Steps:**
1. Load `complex-plugin.apk`
2. Plugin requests CAMERA, STORAGE, LOCATION
3. Observe permission dialog

**Expected Results:**
- ✅ AlertDialog appears with permission list
- ✅ Three buttons visible: "Allow All", "Deny All", "Choose"
- ✅ Permission descriptions clear and readable
- ✅ No UI glitches or crashes

**Test 3a: Allow All**
- Tap "Allow All"
- ✅ Plugin loads successfully
- ✅ All 3 permissions granted
- ✅ No second dialog

**Test 3b: Deny All**
- Uninstall plugin, reinstall
- Tap "Deny All"
- ✅ Plugin still loads (permissions optional for testing)
- ✅ All permissions denied
- ✅ Plugin cannot access restricted APIs

**Test 3c: Choose (Selective)**
- Uninstall plugin, reinstall
- Tap "Choose"
- Select: CAMERA (allow), STORAGE (allow), LOCATION (deny)
- ✅ Only selected permissions granted
- ✅ Denied permissions enforced

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 4: Permission Persistence (Restart)

**Steps:**
1. Load Complex Plugin with permissions granted (CAMERA, STORAGE)
2. Close AvaCode app completely (swipe away from recents)
3. Reopen AvaCode app
4. Check permission status for Complex Plugin

**Expected Results:**
- ✅ Permissions still granted after restart
- ✅ No permission re-request on load
- ✅ Plugin can access previously granted APIs
- ✅ SharedPreferences persisted correctly

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 5: Permission Revocation

**Steps:**
1. With Complex Plugin loaded (permissions granted), navigate to "Plugin Settings"
2. Tap "Manage Permissions"
3. Revoke CAMERA permission
4. Attempt to use camera API

**Expected Results:**
- ✅ Permission revoked successfully
- ✅ UI updates to show "Denied" status
- ✅ Camera API blocked with PermissionDeniedException
- ✅ Revocation persists after restart

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 6: Multiple Plugins (Namespace Isolation)

**Steps:**
1. Load Simple Plugin
2. Load Complex Plugin
3. Verify both appear in plugin list
4. Access assets from both plugins simultaneously

**Expected Results:**
- ✅ Both plugins loaded successfully
- ✅ Separate permission states maintained
- ✅ Assets resolve correctly for both (no conflicts)
- ✅ Simple plugin cannot access Complex plugin's assets
- ✅ Complex plugin cannot access Simple plugin's assets

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 7: Plugin Signature Verification (Security)

**Steps:**
1. Create unsigned plugin APK
2. Attempt to load unsigned plugin
3. Observe rejection

**Expected Results:**
- ✅ Unsigned plugin rejected with error
- ✅ Error message: "Plugin signature verification failed"
- ✅ App does not crash
- ✅ No partial loading

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 8: Malformed Manifest (Error Handling)

**Steps:**
1. Create plugin with invalid YAML manifest
2. Attempt to load malformed plugin

**Expected Results:**
- ✅ Plugin rejected with validation error
- ✅ Clear error message explaining issue
- ✅ App remains stable (no crash)

**Pass/Fail:** ___________

**Notes:** ___________

---

## JVM (Desktop) Testing Checklist

### Setup

- [ ] Build AvaCode JVM application (JAR or executable)
- [ ] Install Java 11+ runtime
- [ ] Prepare test plugin JARs
- [ ] Clear `~/.config/AvaCode` (Linux) or equivalent (start fresh)

### Test 1: Simple Plugin Loading

**Steps:**
1. Launch AvaCode desktop app
2. File → Load Plugin
3. Select `simple-plugin.jar`
4. Click "Install"

**Expected Results:**
- ✅ Plugin loads successfully
- ✅ No permission dialogs
- ✅ Plugin visible in plugins list
- ✅ Metadata displays correctly

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 2: Permission Request Dialog (Swing UI)

**Steps:**
1. Load `complex-plugin.jar` (requires CAMERA, STORAGE, LOCATION)
2. Observe Swing permission dialog

**Expected Results:**
- ✅ JDialog appears with permission checkboxes
- ✅ Three permissions listed with descriptions
- ✅ "Allow All" and "Deny All" buttons functional
- ✅ Individual checkboxes toggleable
- ✅ Dialog modal (blocks main window)

**Test 2a: Allow All**
- Click "Allow All"
- ✅ All checkboxes selected
- ✅ Permissions granted

**Test 2b: Selective Grant**
- Uninstall plugin, reinstall
- Check CAMERA only, uncheck others
- Click "OK"
- ✅ Only CAMERA granted
- ✅ Others denied

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 3: Permission Persistence (File Storage)

**Steps:**
1. Load Complex Plugin with CAMERA + STORAGE granted
2. Close app completely
3. Reopen app
4. Check permission status

**Expected Results:**
- ✅ Permissions persisted in JSON file
- ✅ File location: `~/.config/AvaCode/plugin_permissions/com.avacode.test.complex.json`
- ✅ Permissions reloaded on restart
- ✅ No re-prompt

**Manual File Inspection:**
```bash
cat ~/.config/AvaCode/plugin_permissions/com.avacode.test.complex.json
```

**Expected JSON:**
```json
{
  "pluginId": "com.avacode.test.complex",
  "pluginName": "Complex Test Plugin",
  "permissions": {
    "CAMERA": {
      "permission": "CAMERA",
      "status": "GRANTED",
      "grantedAt": 1729900000000,
      "askedCount": 1
    },
    "STORAGE": {
      "permission": "STORAGE",
      "status": "GRANTED",
      "grantedAt": 1729900000000,
      "askedCount": 1
    },
    "LOCATION": {
      "permission": "LOCATION",
      "status": "DENIED",
      "deniedAt": 1729900000000,
      "askedCount": 1
    }
  }
}
```

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 4: Asset Resolution

**Steps:**
1. Load Simple Plugin
2. Use asset resolver to load `plugin://com.avacode.test.simple/assets/greeting.txt`

**Expected Results:**
- ✅ Asset resolves successfully
- ✅ Content: "Hello from Simple Plugin!"
- ✅ File path: `~/.config/AvaCode/plugins/com.avacode.test.simple/assets/greeting.txt`

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 5: URLClassLoader Isolation

**Steps:**
1. Load two plugins with same class name (e.g., `PluginMain.class`)
2. Instantiate both plugins
3. Verify separate ClassLoader instances

**Expected Results:**
- ✅ Both plugins load without class conflict
- ✅ Each plugin has separate URLClassLoader
- ✅ Classes do not interfere with each other
- ✅ No `ClassCastException` or `LinkageError`

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 6: Cross-Platform File Paths (Windows/macOS/Linux)

**Platform:** ___________ (Windows/macOS/Linux)

**Steps:**
1. Check storage directory location
2. Verify path separator handling
3. Load plugin with assets

**Expected Storage Locations:**
- **Windows:** `%APPDATA%\AvaCode\plugin_permissions\`
- **macOS:** `~/Library/Application Support/AvaCode/plugin_permissions/`
- **Linux:** `~/.config/AvaCode/plugin_permissions/`

**Expected Results:**
- ✅ Correct platform path used
- ✅ Directories created automatically
- ✅ File paths use correct separators (/ or \)

**Pass/Fail:** ___________

**Notes:** ___________

---

## iOS Testing Checklist

### Setup

- [ ] Build AvaCode iOS app in Xcode
- [ ] Run on iOS simulator or device
- [ ] Register test plugins statically in code
- [ ] Clear app data (delete and reinstall)

### Test 1: Static Plugin Registration

**Code:**
```kotlin
// In app initialization
PluginRegistry.registerPlugin(
    id = "com.avacode.test.simple",
    factory = { SimpleTestPlugin() }
)
```

**Steps:**
1. Launch app
2. Check registered plugins list

**Expected Results:**
- ✅ Simple Plugin registered successfully
- ✅ Plugin appears in registry
- ✅ No dynamic loading (static only)

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 2: Permission Request Dialog (UIAlertController)

**Steps:**
1. Register and initialize Complex Plugin (requires CAMERA, LOCATION, PHOTOS)
2. Plugin requests permissions on first use
3. Observe UIAlertController

**Expected Results:**
- ✅ UIAlertController appears
- ✅ Title: "Permission Request"
- ✅ Message lists permissions: CAMERA, LOCATION, PHOTOS
- ✅ Three buttons:
  - "Allow All"
  - "Deny All"
  - "Choose"
- ✅ Alert style (not action sheet)

**Test 2a: Allow All**
- Tap "Allow All"
- ✅ All permissions granted
- ✅ Alert dismisses
- ✅ Plugin continues initialization

**Test 2b: Deny All**
- Reset app, try again
- Tap "Deny All"
- ✅ All permissions denied
- ✅ Plugin handles denial gracefully

**Test 2c: Choose**
- Reset app, try again
- Tap "Choose"
- ✅ Individual permission selection UI appears (or handled appropriately)

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 3: Permission Persistence (UserDefaults)

**Steps:**
1. Grant permissions to Complex Plugin (CAMERA, LOCATION)
2. Force quit app (swipe up from app switcher)
3. Relaunch app
4. Initialize Complex Plugin

**Expected Results:**
- ✅ Permissions reloaded from UserDefaults
- ✅ No permission re-request
- ✅ Plugin accesses granted APIs immediately

**Manual Verification (Xcode Console):**
```bash
# Print UserDefaults keys
po UserDefaults.standard.dictionaryRepresentation()
```

**Expected Keys:**
- `plugin_permission_com.avacode.test.complex` (JSON string)

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 4: Permission Rationale Dialog

**Steps:**
1. Request permission for CAMERA
2. Deny permission
3. Request again
4. Observe rationale dialog

**Expected Results:**
- ✅ Rationale UIAlertController appears
- ✅ Message explains why permission needed
- ✅ Buttons: "Grant" and "Deny"
- ✅ If "Grant" → permission granted
- ✅ If "Deny" → permission denied

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 5: Permission Settings Dialog

**Steps:**
1. Navigate to plugin settings (tap "Settings" button in permission UI)
2. Observe settings UIAlertController

**Expected Results:**
- ✅ Settings dialog appears
- ✅ Shows current permission status (Granted/Denied for each)
- ✅ "Open Settings" button present
- ✅ Tapping "Open Settings" → opens iOS Settings app (`app-settings:` URL)

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 6: OS-Level App Sandboxing

**Steps:**
1. Attempt to access file outside app sandbox from plugin
2. Verify rejection

**Expected Results:**
- ✅ File access denied by iOS
- ✅ Error: "Operation not permitted" or similar
- ✅ Plugin cannot escape app sandbox
- ✅ App container isolation enforced by OS

**Pass/Fail:** ___________

**Notes:** ___________

---

### Test 7: Asset Resolution (Bundle Resources)

**Steps:**
1. Register plugin with bundled asset (e.g., `icon.png`)
2. Resolve asset URI: `plugin://com.avacode.test.simple/assets/icon.png`

**Expected Results:**
- ✅ Asset resolves to bundle path
- ✅ Image loads successfully
- ✅ No file not found errors

**Pass/Fail:** ___________

**Notes:** ___________

---

## Cross-Platform Security Testing

### Test 1: Permission Enforcement (All Platforms)

**Steps:**
1. Load plugin with CAMERA permission DENIED
2. Attempt to call camera API from plugin
3. Verify rejection

**Expected Results:**
- ✅ API call blocked
- ✅ Exception thrown: `PermissionDeniedException`
- ✅ No camera access granted
- ✅ Error message clear

**Platform Results:**
- Android: Pass/Fail ___________
- JVM: Pass/Fail ___________
- iOS: Pass/Fail ___________

---

### Test 2: Signature Verification (Android/JVM)

**Android:**
- Create unsigned plugin APK
- Attempt to load
- ✅ Rejected with signature error

**JVM:**
- Create unsigned plugin JAR
- Attempt to load
- ✅ Rejected with signature error (or warning if configured)

**iOS:**
- N/A (code signing enforced by Xcode build)

**Platform Results:**
- Android: Pass/Fail ___________
- JVM: Pass/Fail ___________
- iOS: N/A ✅

---

### Test 3: ClassLoader Isolation (Android/JVM)

**Steps:**
1. Load Plugin A with class `com.example.Utils`
2. Load Plugin B with class `com.example.Utils` (same name)
3. Verify no conflict

**Expected Results:**
- ✅ Both plugins load successfully
- ✅ Separate ClassLoader instances
- ✅ No `LinkageError` or class collision
- ✅ Each plugin uses its own `Utils` class

**Platform Results:**
- Android: Pass/Fail ___________
- JVM: Pass/Fail ___________
- iOS: N/A (static compilation)

---

### Test 4: Namespace Isolation (All Platforms)

**Steps:**
1. Load Simple Plugin with asset `config.json`
2. Load Complex Plugin with asset `config.json` (same name)
3. Resolve both: `plugin://com.avacode.test.simple/assets/config.json` and `plugin://com.avacode.test.complex/assets/config.json`

**Expected Results:**
- ✅ Both assets resolve to different files
- ✅ No asset collision
- ✅ Plugin A cannot access Plugin B's `config.json`
- ✅ Namespace isolation enforced

**Platform Results:**
- Android: Pass/Fail ___________
- JVM: Pass/Fail ___________
- iOS: Pass/Fail ___________

---

## Performance Testing

### Test 1: Plugin Load Time

**Steps:**
1. Measure time from "Install" tap/click to plugin loaded
2. Test with Simple Plugin (small, no permissions)

**Expected Results:**
- ✅ Android: < 500ms
- ✅ JVM: < 1000ms
- ✅ iOS: < 100ms (static registration)

**Actual Results:**
- Android: ___________ ms
- JVM: ___________ ms
- iOS: ___________ ms

**Pass/Fail:** ___________

---

### Test 2: Asset Resolution Time

**Steps:**
1. Resolve 10 assets sequentially
2. Measure average resolution time

**Expected Results:**
- ✅ Average < 50ms per asset
- ✅ No memory leaks

**Actual Results:**
- Android: ___________ ms avg
- JVM: ___________ ms avg
- iOS: ___________ ms avg

**Pass/Fail:** ___________

---

### Test 3: Permission Persistence Read Time

**Steps:**
1. Load 10 plugins with permissions
2. Restart app
3. Measure time to reload all permission states

**Expected Results:**
- ✅ Android (SharedPreferences): < 200ms
- ✅ JVM (JSON files): < 500ms
- ✅ iOS (UserDefaults): < 200ms

**Actual Results:**
- Android: ___________ ms
- JVM: ___________ ms
- iOS: ___________ ms

**Pass/Fail:** ___________

---

## Regression Testing (Known Issues)

### Issue 1: iOS Dynamic Loading

**Status:** NOT SUPPORTED (by design)

**Test:** Attempt dynamic plugin loading on iOS
**Expected:** Compile-time error or clear runtime message
**Pass/Fail:** ___________

---

### Issue 2: JVM SecurityManager Deprecation

**Status:** KNOWN LIMITATION (Java 17+)

**Test:** Run on Java 17+ without SecurityManager
**Expected:** No SecurityManager warnings, ClassLoader isolation still works
**Pass/Fail:** ___________

---

## Test Summary Report

### Platform Results

| Platform | Tests Passed | Tests Failed | Notes |
|----------|--------------|--------------|-------|
| **Android** | _____/15 | _____/15 | _____________ |
| **JVM** | _____/10 | _____/10 | _____________ |
| **iOS** | _____/9 | _____/9 | _____________ |
| **TOTAL** | _____/34 | _____/34 | _____________ |

### Critical Issues Found

| Issue | Severity | Platform | Blocks Production? |
|-------|----------|----------|-------------------|
| _____ | _____ | _____ | _____ |

### Non-Critical Issues Found

| Issue | Severity | Platform | Notes |
|-------|----------|----------|-------|
| _____ | _____ | _____ | _____ |

---

## Recommendations

**Before Production:**
- [ ] All critical tests must pass (100%)
- [ ] At least 90% of tests must pass overall
- [ ] Any failures documented with workarounds
- [ ] Performance benchmarks met

**Post-Testing Actions:**
1. Document any issues found
2. Create tickets for bugs
3. Update validation analysis with test results
4. Make go/no-go decision for production

---

## Tester Sign-Off

**Tester Name:** _____________________

**Date Completed:** _____________________

**Overall Status:** ✅ PASS / ❌ FAIL / ⚠️ PASS WITH ISSUES

**Production Ready?** ✅ YES / ❌ NO

**Notes:**
_____________________________________________
_____________________________________________
_____________________________________________

---

**Created by Manoj Jhawar, manoj@ideahq.net**

**Testing Method:** Manual platform testing (pre-production)
**Platforms Tested:** Android, JVM (Desktop), iOS
**Test Scope:** Plugin loading, permissions, persistence, security, performance

**End of Manual Testing Guide**
