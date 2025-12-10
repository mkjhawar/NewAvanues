# LearnApp Testing Plan

**Version:** 1.0
**Created:** 2025-11-30
**Module:** LearnApp (VoiceOSCore)

---

## Overview

This document outlines the testing plan for validating the LearnApp module fixes implemented on 2025-11-30, covering database deadlock resolution, consent dialog touch handling, and system app filtering.

---

## Test Environment

| Component | Requirement |
|-----------|-------------|
| Device | Physical Android device (recommended) or Emulator API 28+ |
| APK | `voiceos-debug-v3.0.0-20251130-1947.apk` |
| Permissions | Accessibility Service, SYSTEM_ALERT_WINDOW |
| Test Apps | Any 3rd-party app (e.g., Calculator, Clock, Settings) |

---

## Test Categories

### 1. Database Deadlock Tests

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| DB-01 | Rapid app switching | Launch 10+ apps quickly within 30 seconds | No ANR, no "database locked" errors in logcat |
| DB-02 | Concurrent consent dialogs | Trigger consent for 3 apps simultaneously | Dialogs queue properly, database operations complete |
| DB-03 | Background processing | Approve consent, immediately background VoiceOS | Consent saves correctly, no crash |
| DB-04 | Cold start after crash | Force-stop VoiceOS, relaunch | Previous consents persisted, no data loss |
| DB-05 | UUID registration stress | Generate 100+ UUIDs rapidly | No deadlock, all UUIDs registered |

**Logcat Filter:**
```bash
adb logcat -s "LearnApp" "ConsentDialog" "UUIDCreator" "SQLiteDatabase"
```

**Failure Indicators:**
- `database is locked`
- `SQLITE_BUSY`
- `SQLiteDatabaseLockedException`
- ANR dialog

---

### 2. Consent Dialog Touch Handling Tests

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| CD-01 | Button touch | Tap "Allow" button | Button responds, dialog dismisses |
| CD-02 | Button touch (Deny) | Tap "Deny" button | Button responds, dialog dismisses |
| CD-03 | Checkbox touch | Tap "Don't ask again" checkbox | Checkbox toggles state |
| CD-04 | Outside touch | Tap outside dialog area | Dialog remains visible (not dismissed) |
| CD-05 | Multi-touch | Tap button with 2 fingers | Single action only, no crash |
| CD-06 | Quick dismiss | Tap button immediately after dialog appears | Action registers correctly |
| CD-07 | Dialog over app | Launch app, dialog appears over it | Dialog fully interactive |
| CD-08 | Rotation | Rotate device while dialog visible | Dialog remains visible and functional |

**Logcat Filter:**
```bash
adb logcat -s "ConsentDialog" "ConsentDialogManager"
```

**Failure Indicators:**
- Touch events not registering
- Dialog dismissing unexpectedly
- WindowManager exceptions
- Z-order issues (dialog behind app)

---

### 3. System App Filtering Tests

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| SF-01 | Core Android package | Trigger "android" package detection | Filtered out, no consent prompt |
| SF-02 | System UI | Trigger "com.android.systemui" | Filtered out, no consent prompt |
| SF-03 | Settings | Launch Settings app | May prompt (user app) |
| SF-04 | Google Play Services | Trigger "com.google.android.gms" | Filtered (system app flag) |
| SF-05 | User-installed app | Launch 3rd-party app | Consent dialog appears |

**Logcat Filter:**
```bash
adb logcat -s "AppLaunchDetector"
```

**Success Log Pattern:**
```
Filtering out system package: android
Filtering out system package: com.android.systemui
```

---

### 4. Error Handling Tests

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| EH-01 | Emit failure | Simulate flow collector disconnect | Error logged, no crash |
| EH-02 | Database timeout | Simulate lock contention | Waits up to 30s, then handles gracefully |
| EH-03 | WindowManager failure | Remove SYSTEM_ALERT_WINDOW permission | Graceful degradation, logged error |
| EH-04 | Memory pressure | Run with limited memory | Operations complete or fail gracefully |

---

### 5. Integration Tests

| ID | Test Case | Steps | Expected Result |
|----|-----------|-------|-----------------|
| IT-01 | Full workflow | 1. Enable accessibility service<br>2. Launch new app<br>3. Approve consent<br>4. Verify learning | App tracked, commands generated |
| IT-02 | Service restart | Kill and restart accessibility service | State restored, learning continues |
| IT-03 | Multi-app learning | Consent to 5 different apps | All apps tracked independently |
| IT-04 | Consent persistence | Approve app, reboot device | Consent remembered, no re-prompt |

---

## Automated Test Script

Location: `/Volumes/M-Drive/Coding/VoiceOS/scripts/test-learnapp-emulator.sh`

```bash
# Run automated tests
./scripts/test-learnapp-emulator.sh

# Expected output:
# - Accessibility service enabled
# - App launch detected
# - Consent dialog appeared
# - Touch events handled
# - Database operations completed
```

---

## Performance Benchmarks

| Metric | Target | Method |
|--------|--------|--------|
| Consent dialog show time | <100ms | Logcat timestamp analysis |
| Database write time | <50ms | Profiler or logcat |
| App detection latency | <200ms | Accessibility event to detection |
| Memory overhead | <10MB | Android Profiler |

---

## Regression Checklist

Before marking tests complete:

- [ ] No ANR in 30-minute stress test
- [ ] All consent button touches respond
- [ ] System apps filtered correctly
- [ ] Database persists across restarts
- [ ] No new logcat errors/warnings
- [ ] Memory stable over time

---

## Test Results Template

| Test ID | Result | Notes | Tester | Date |
|---------|--------|-------|--------|------|
| DB-01 | PASS/FAIL | | | |
| DB-02 | PASS/FAIL | | | |
| ... | ... | ... | ... | ... |

---

## Known Limitations

1. **Emulator vs Physical Device:** Touch handling may differ; prefer physical device for CD-* tests
2. **System App Detection:** Some OEM apps may not be properly flagged as system apps
3. **Permission Revocation:** Revoking SYSTEM_ALERT_WINDOW mid-dialog causes expected failure

---

## References

- Fix Commit: `843c66ad` - fix(LearnApp): Resolve database deadlocks and consent dialog issues
- Developer Manual: `docs/modules/LearnApp/developer-manual.md`
- Analysis Spec: `specs/learnapp-analysis-20251130.md`
- Fix Plan: `specs/learnapp-fix-plan-20251130.md`
