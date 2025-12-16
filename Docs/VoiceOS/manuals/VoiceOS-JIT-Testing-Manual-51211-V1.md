# VoiceOS JIT Service - Testing Manual

**Version:** 1.0
**Date:** 2025-12-11
**Module:** JIT Learning Service Integration
**Status:** Ready for Testing

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Test Environment Setup](#2-test-environment-setup)
3. [Unit Tests](#3-unit-tests)
4. [Integration Tests](#4-integration-tests)
5. [Manual Testing Procedures](#5-manual-testing-procedures)
6. [Test Cases](#6-test-cases)
7. [Expected Results](#7-expected-results)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Prerequisites

### 1.1 Hardware Requirements

| Item | Requirement |
|------|-------------|
| Android Device | API 34+ (Android 14+) |
| RAM | 4GB minimum |
| Storage | 500MB free space |

### 1.2 Software Requirements

| Software | Version |
|----------|---------|
| Android Studio | Hedgehog+ |
| JDK | 17 |
| Gradle | 8.x |
| ADB | Latest |

### 1.3 Required Apps Installed

1. **VoiceOSCore** - Main accessibility service
2. **LearnAppLite (AvaLearnLite)** - User edition
3. **LearnAppPro (AvaLearnPro)** - Developer edition (optional)

### 1.4 Permissions Required

- Accessibility Service enabled for VoiceOS
- Overlay permission (draw over other apps)
- Notification permission

---

## 2. Test Environment Setup

### 2.1 Build All Modules

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnApp:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnAppDev:assembleDebug
```

### 2.2 Install on Device

```bash
adb install -r Modules/VoiceOS/apps/VoiceOSCore/build/outputs/apk/debug/VoiceOSCore-debug.apk
adb install -r Modules/VoiceOS/apps/LearnApp/build/outputs/apk/debug/LearnApp-debug.apk
adb install -r Modules/VoiceOS/apps/LearnAppDev/build/outputs/apk/debug/LearnAppDev-debug.apk
```

### 2.3 Enable VoiceOS Accessibility Service

1. Open Settings > Accessibility
2. Find "VoiceOS" in installed services
3. Toggle ON
4. Accept permissions dialog

### 2.4 Verify Service Running

```bash
adb shell dumpsys activity services | grep -i "voiceos\|jit"
```

Expected output:
```
ServiceRecord{... com.augmentalis.voiceoscore/.accessibility.VoiceOSService}
ServiceRecord{... com.augmentalis.jitlearning.JITLearningService}
```

---

## 3. Unit Tests

### 3.1 Run JustInTimeLearner Tests

```bash
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:testDebugUnitTest --tests "*JustInTimeLearner*"
```

### 3.2 Run JITLearningService Tests

```bash
./gradlew :Modules:VoiceOS:libraries:JITLearning:testDebugUnitTest
```

### 3.3 Run LearnAppIntegration Tests

```bash
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:testDebugUnitTest --tests "*LearnAppIntegration*"
```

---

## 4. Integration Tests

### 4.1 AIDL Binding Test

```bash
./gradlew :Modules:VoiceOS:apps:LearnApp:connectedDebugAndroidTest --tests "*ServiceBindingTest*"
```

### 4.2 Event Streaming Test

```bash
./gradlew :Modules:VoiceOS:apps:LearnAppDev:connectedDebugAndroidTest --tests "*EventStreamTest*"
```

---

## 5. Manual Testing Procedures

### 5.1 Basic Connection Test

**Objective:** Verify LearnApp can connect to JITLearningService

**Steps:**
1. Ensure VoiceOS accessibility service is running
2. Open LearnAppLite
3. Observe connection status indicator

**Expected:** "Service Connected" status shown

### 5.2 State Query Test

**Objective:** Verify queryState() returns real values

**Steps:**
1. Open LearnAppLite
2. Navigate to several apps on the device (e.g., Settings, Chrome)
3. Return to LearnAppLite
4. Check "Screens Learned" and "Elements Discovered" counts

**Expected:** Counts > 0 after navigating apps

### 5.3 Pause/Resume Test

**Objective:** Verify pause/resume controls work

**Steps:**
1. Open LearnAppLite
2. Note current "Screens Learned" count
3. Tap "Pause JIT"
4. Navigate to a new app
5. Return to LearnAppLite
6. Verify count unchanged
7. Tap "Resume JIT"
8. Navigate to another new app
9. Return to LearnAppLite
10. Verify count increased

**Expected:** Count unchanged while paused, increases after resume

### 5.4 Event Streaming Test (LearnAppPro)

**Objective:** Verify real-time events appear in logs

**Steps:**
1. Open LearnAppPro (Developer edition)
2. Go to "Logs" tab
3. Navigate to different apps
4. Return to LearnAppPro

**Expected:** Log entries showing:
- `SCREEN: Screen changed: {hash}`
- `EVENT: Element discovered: {id}`

### 5.5 Element Query Test (LearnAppPro)

**Objective:** Verify getCurrentScreenInfo() returns data

**Steps:**
1. Open LearnAppPro
2. Go to "Elements" tab
3. Tap "Refresh"

**Expected:** Element tree displayed with clickable/scrollable elements

---

## 6. Test Cases

### TC-001: Service Binding

| Field | Value |
|-------|-------|
| ID | TC-001 |
| Title | JITLearningService Binding |
| Priority | P0 |
| Preconditions | VoiceOS service running |
| Steps | 1. Open LearnAppLite<br>2. Check connection status |
| Expected | Status = "Service Connected" |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

### TC-002: Query State - Screens Learned

| Field | Value |
|-------|-------|
| ID | TC-002 |
| Title | queryState() Screens Learned |
| Priority | P0 |
| Preconditions | Service connected, navigated 3+ apps |
| Steps | 1. Open LearnAppLite<br>2. Read "Screens Learned" |
| Expected | screensLearned >= 3 |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

### TC-003: Query State - Elements Discovered

| Field | Value |
|-------|-------|
| ID | TC-003 |
| Title | queryState() Elements Discovered |
| Priority | P0 |
| Preconditions | Service connected, navigated apps |
| Steps | 1. Open LearnAppLite<br>2. Read "Elements Discovered" |
| Expected | elementsDiscovered > 0 |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

### TC-004: Pause Capture

| Field | Value |
|-------|-------|
| ID | TC-004 |
| Title | pauseCapture() Functionality |
| Priority | P0 |
| Preconditions | Service connected |
| Steps | 1. Note current screen count<br>2. Tap Pause<br>3. Navigate to new app<br>4. Check screen count |
| Expected | Count unchanged |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

### TC-005: Resume Capture

| Field | Value |
|-------|-------|
| ID | TC-005 |
| Title | resumeCapture() Functionality |
| Priority | P0 |
| Preconditions | Service paused |
| Steps | 1. Note current screen count<br>2. Tap Resume<br>3. Navigate to new app<br>4. Check screen count |
| Expected | Count increased |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

### TC-006: Event Listener Registration

| Field | Value |
|-------|-------|
| ID | TC-006 |
| Title | registerEventListener() |
| Priority | P1 |
| Preconditions | LearnAppPro installed |
| Steps | 1. Open LearnAppPro<br>2. Go to Logs tab<br>3. Check registration log |
| Expected | "Event listener registered" in logs |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

### TC-007: Screen Change Events

| Field | Value |
|-------|-------|
| ID | TC-007 |
| Title | onScreenChanged() Events |
| Priority | P1 |
| Preconditions | LearnAppPro with listener registered |
| Steps | 1. Navigate to new app<br>2. Check LearnAppPro logs |
| Expected | SCREEN event with hash, elementCount |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

### TC-008: Get Current Screen Info

| Field | Value |
|-------|-------|
| ID | TC-008 |
| Title | getCurrentScreenInfo() |
| Priority | P1 |
| Preconditions | LearnAppPro, app in foreground |
| Steps | 1. Open LearnAppPro Elements tab<br>2. Tap Refresh |
| Expected | Element tree with nodes displayed |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

### TC-009: Query Elements by Selector

| Field | Value |
|-------|-------|
| ID | TC-009 |
| Title | queryElements() with Selector |
| Priority | P2 |
| Preconditions | LearnAppPro, app in foreground |
| Steps | 1. Enter selector "class:Button"<br>2. Execute query |
| Expected | List of Button elements returned |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

### TC-010: Perform Click

| Field | Value |
|-------|-------|
| ID | TC-010 |
| Title | performClick() via AIDL |
| Priority | P2 |
| Preconditions | Element UUID known |
| Steps | 1. Query elements<br>2. Select element<br>3. Execute click |
| Expected | Element clicked, action notification received |
| Actual | _To be filled_ |
| Pass/Fail | _To be filled_ |

---

## 7. Expected Results

### 7.1 Success Criteria

| Test Area | Criteria |
|-----------|----------|
| Service Binding | 100% success rate |
| State Queries | Returns non-zero values after navigation |
| Pause/Resume | State changes within 500ms |
| Event Streaming | Events received within 1s of action |
| Element Queries | Returns valid ParcelableNodeInfo |

### 7.2 Performance Targets

| Metric | Target |
|--------|--------|
| Service bind time | < 500ms |
| queryState() latency | < 50ms |
| Event dispatch latency | < 100ms |
| getCurrentScreenInfo() | < 200ms |

---

## 8. Troubleshooting

### 8.1 Service Not Connecting

**Symptoms:** LearnApp shows "Service Disconnected"

**Checks:**
1. Is VoiceOS accessibility service enabled?
   ```bash
   adb shell settings get secure enabled_accessibility_services
   ```
2. Is JITLearningService running?
   ```bash
   adb shell dumpsys activity services | grep JITLearning
   ```

**Fix:** Re-enable VoiceOS accessibility service

### 8.2 Stats Always Zero

**Symptoms:** screensLearned = 0, elementsDiscovered = 0

**Checks:**
1. Is LearnAppIntegration initialized?
   ```bash
   adb logcat -s LearnAppIntegration | grep "initialized"
   ```
2. Is JITLearnerProvider wired?
   ```bash
   adb logcat -s JITLearningService | grep "JITLearnerProvider set"
   ```

**Fix:** Restart VoiceOS service (toggle accessibility off/on)

### 8.3 Events Not Received

**Symptoms:** LearnAppPro logs tab empty

**Checks:**
1. Is listener registered?
   ```bash
   adb logcat -s JITLearningService | grep "Registering event listener"
   ```
2. Are events being dispatched?
   ```bash
   adb logcat -s JITLearningService | grep "dispatch"
   ```

**Fix:** Re-open LearnAppPro to re-register listener

### 8.4 AIDL Errors

**Symptoms:** RemoteException in logs

**Checks:**
1. Are AIDL versions compatible?
2. Is service in same process?

**Fix:** Rebuild and reinstall all APKs

---

## Appendix A: ADB Commands Reference

```bash
# Check accessibility services
adb shell settings get secure enabled_accessibility_services

# View VoiceOS logs
adb logcat -s VoiceOSService,JITLearningService,LearnAppIntegration,JustInTimeLearner

# Clear app data
adb shell pm clear com.augmentalis.voiceoscore

# Force stop service
adb shell am force-stop com.augmentalis.voiceoscore

# Start service
adb shell am startservice -n com.augmentalis.voiceoscore/.jitlearning.JITLearningService
```

---

## Appendix B: Log Tags

| Tag | Component |
|-----|-----------|
| VoiceOSService | Main accessibility service |
| JITLearningService | AIDL service |
| JustInTimeLearner | Passive learning engine |
| LearnAppIntegration | VoiceOS-LearnApp bridge |
| LearnAppActivity | LearnAppLite UI |
| LearnAppDevActivity | LearnAppPro UI |

---

**Document Version History**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-11 | Claude | Initial version |
