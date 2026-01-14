# AVA Offline Mode Test Plan

**Date:** 2025-11-04
**Purpose:** Test offline functionality with ModelDownloadManager
**Status:** Ready for execution

---

## Test Overview

AVA now supports **offline-first operation** with on-demand model downloads. This test plan validates that:

1. ✅ Models download successfully when online
2. ✅ Models load from cache when offline
3. ✅ App functions without internet after initial download
4. ✅ Error messages are user-friendly for network issues
5. ✅ Download resume works after network interruption

---

## Test Environment Setup

### Prerequisites

1. **Android Device/Emulator:**
   - Android 7.0+ (API 24+)
   - 3GB+ free storage
   - WiFi and cellular network access

2. **Test APK:**
   - Location: `apps/ava-standalone/build/outputs/apk/debug/ava-standalone-debug.apk`
   - Size: 87 MB (without bundled models)
   - Build: development branch (commit 3635d2a)

3. **Network Control:**
   - Ability to toggle WiFi on/off
   - Ability to enable/disable airplane mode
   - Network speed throttling (optional - for slow network tests)

### Installation

```bash
# Install APK
adb install apps/ava-standalone/build/outputs/apk/debug/ava-standalone-debug.apk

# Clear app data (fresh start)
adb shell pm clear com.augmentalis.ava

# Enable logging
adb logcat -c
adb logcat | grep "AVA\|ModelDownload\|ModelCache"
```

---

## Test Cases

### TC1: Fresh Install - Model Download (Online)

**Preconditions:**
- App not installed OR app data cleared
- Device online (WiFi connected)
- 3GB+ storage available

**Steps:**
1. Launch AVA app
2. Navigate to Chat screen
3. Observe model download prompt
4. Tap "Download" button
5. Wait for download to complete

**Expected Results:**
- ✅ Download UI shows model name ("Gemma 2B Instruct")
- ✅ Progress bar animates smoothly
- ✅ Download speed displayed (MB/s)
- ✅ ETA displayed (time remaining)
- ✅ Download completes successfully
- ✅ "Ready to use" message displayed
- ✅ Chat interface becomes active

**Acceptance Criteria:**
- Download completes within 5 minutes on WiFi (1.5GB model)
- No crashes or ANRs during download
- Storage space updated correctly

**Log Verification:**
```bash
adb logcat | grep "ModelDownloadManager\|DownloadState"
# Should show:
# - "Starting download for model: gemma-2b-it-q4f16_1-MLC"
# - "Download progress: gemma... 10%, 20%, ..., 100%"
# - "Download completed: gemma... -> /data/user/0/.../gemma..."
```

---

### TC2: Model Already Downloaded (Cached)

**Preconditions:**
- App installed with models already downloaded (TC1 passed)
- Device online

**Steps:**
1. Force close AVA app
2. Re-launch AVA app
3. Navigate to Chat screen
4. Observe app behavior

**Expected Results:**
- ✅ No download prompt shown
- ✅ Chat interface immediately active
- ✅ "Model ready" indicator displayed
- ✅ First inference completes within 3 seconds

**Acceptance Criteria:**
- App startup <2 seconds
- No unnecessary network requests
- Model loads from cache without re-download

**Log Verification:**
```bash
adb logcat | grep "ModelCacheManager"
# Should show:
# - "Model cached: gemma-2b-it-q4f16_1-MLC"
# - "Loading model from cache: /data/user/0/.../gemma..."
```

---

### TC3: Offline Mode - Cached Model Works

**Preconditions:**
- Models already downloaded (TC1 passed)
- Device online initially

**Steps:**
1. Launch AVA app
2. Verify chat works (send 1 message)
3. **Enable airplane mode** (disable all networks)
4. Send another chat message
5. Observe app behavior

**Expected Results:**
- ✅ Chat continues to work offline
- ✅ Responses generated locally
- ✅ No network error messages for chat
- ✅ Inference latency similar to online mode

**Acceptance Criteria:**
- Chat functionality fully operational offline
- Response time <5 seconds for simple queries
- No crashes or freezes

**Log Verification:**
```bash
adb logcat | grep "LocalLLMProvider\|NetworkException"
# Should show:
# - "Using cached model: gemma-2b-it-q4f16_1-MLC"
# - NO network error logs
```

---

### TC4: Offline Mode - Model Not Downloaded

**Preconditions:**
- App freshly installed OR models deleted
- Device **offline** (airplane mode enabled)

**Steps:**
1. Enable airplane mode
2. Launch AVA app
3. Navigate to Chat screen
4. Observe behavior

**Expected Results:**
- ✅ Error message displayed: "No model available"
- ✅ Download button shown but disabled (grayed out)
- ✅ Message: "Connect to WiFi to download model"
- ✅ No app crash

**Acceptance Criteria:**
- User-friendly offline error message
- Clear instructions to go online
- App remains stable (no ANR/crash)

**Log Verification:**
```bash
adb logcat | grep "ERROR\|ModelCache"
# Should show:
# - "Model not cached: gemma-2b-it-q4f16_1-MLC"
# - "Network unavailable, cannot download"
```

---

### TC5: Download Pause/Resume

**Preconditions:**
- App freshly installed OR models deleted
- Device online (WiFi connected)

**Steps:**
1. Launch AVA app
2. Start model download
3. Wait for download to reach 30%
4. Tap "Pause" button
5. Observe paused state
6. Wait 10 seconds
7. Tap "Resume" button
8. Wait for download to complete

**Expected Results:**
- ✅ Download pauses immediately (within 1 second)
- ✅ Progress bar shows "Paused at 30%"
- ✅ Resume button enabled
- ✅ Download resumes from 30% (not 0%)
- ✅ Download completes successfully

**Acceptance Criteria:**
- Pause latency <1 second
- Resume uses HTTP range requests (doesn't re-download)
- No data loss on pause/resume

**Log Verification:**
```bash
adb logcat | grep "pauseDownload\|resumeDownload"
# Should show:
# - "Pausing download: gemma-2b-it-q4f16_1-MLC"
# - "Download paused at X bytes"
# - "Resuming download: gemma-2b-it-q4f16_1-MLC from X bytes"
```

---

### TC6: Network Interruption During Download

**Preconditions:**
- App freshly installed OR models deleted
- Device online initially

**Steps:**
1. Launch AVA app
2. Start model download
3. Wait for download to reach 50%
4. **Enable airplane mode** (simulate network loss)
5. Observe error handling
6. Wait 5 seconds
7. **Disable airplane mode** (restore network)
8. Tap "Retry" button (if shown)
9. Observe download resumption

**Expected Results:**
- ✅ Download error displayed: "Network connection lost"
- ✅ Retry button shown
- ✅ Partial download preserved
- ✅ Download resumes from 50% after retry
- ✅ Download completes successfully

**Acceptance Criteria:**
- Error message appears within 5 seconds of network loss
- Partial download not deleted
- Retry successfully resumes from last byte

**Log Verification:**
```bash
adb logcat | grep "NetworkException\|resumeDownload"
# Should show:
# - "Download error: Network unreachable"
# - "Partial download saved: X bytes"
# - "Resuming from byte offset: X"
```

---

### TC7: Low Storage Warning

**Preconditions:**
- Device storage <1GB free
- Models not downloaded

**Steps:**
1. Launch AVA app
2. Attempt to download model
3. Observe behavior

**Expected Results:**
- ✅ Warning message: "Insufficient storage"
- ✅ Shows required vs available space
- ✅ Download button disabled
- ✅ Suggestion to free up space

**Acceptance Criteria:**
- Warning appears before download starts
- No partial download attempted
- User can exit cleanly

**Log Verification:**
```bash
adb logcat | grep "InsufficientStorageException"
# Should show:
# - "Insufficient storage: Need 1.5GB, have 0.8GB"
```

---

### TC8: WiFi vs Cellular Download

**Preconditions:**
- App freshly installed
- Device has both WiFi and cellular

**Steps:**
1. **Test 8A: WiFi Download**
   - Connect to WiFi
   - Start download
   - Observe speed

2. **Test 8B: Cellular Warning**
   - Disable WiFi (cellular only)
   - Attempt to download large model (>100MB)
   - Observe warning

**Expected Results:**
- **Test 8A:**
  - ✅ Download starts immediately on WiFi
  - ✅ No warnings shown

- **Test 8B:**
  - ✅ Warning shown: "Large download on cellular"
  - ✅ Option to proceed or wait for WiFi
  - ✅ User choice respected

**Acceptance Criteria:**
- WiFi downloads unrestricted
- Cellular downloads show data usage warning
- User can override warning

**Log Verification:**
```bash
adb logcat | grep "NetworkType\|WiFi\|Cellular"
# Should show:
# - "Network type: WiFi" or "Network type: Cellular"
# - "Large download on cellular: 1.5GB"
```

---

### TC9: SHA-256 Checksum Verification

**Preconditions:**
- Models not downloaded
- Device online

**Steps:**
1. Start model download
2. Wait for download to complete
3. Observe verification step
4. Wait for verification to complete

**Expected Results:**
- ✅ "Verifying..." message shown after download
- ✅ Checksum verification completes <30 seconds
- ✅ Verification success message
- ✅ Model marked as "Ready"

**Acceptance Criteria:**
- Verification runs automatically
- Verification time proportional to file size
- Failed verification deletes corrupt file

**Log Verification:**
```bash
adb logcat | grep "SHA256\|verifyChecksum"
# Should show:
# - "Verifying model checksum: gemma-2b-it-q4f16_1-MLC"
# - "Checksum verification: PASSED"
```

---

### TC10: Multiple Concurrent Downloads

**Preconditions:**
- Multiple models not downloaded
- Device online (WiFi)

**Steps:**
1. Navigate to model management screen
2. Start download for Gemma 2B model
3. Start download for MobileBERT model
4. Observe both downloads
5. Wait for both to complete

**Expected Results:**
- ✅ Both downloads show individual progress bars
- ✅ Downloads proceed concurrently
- ✅ No interference between downloads
- ✅ Both complete successfully

**Acceptance Criteria:**
- Concurrent download limit: 3 models
- Total bandwidth shared fairly
- Both downloads complete without errors

**Log Verification:**
```bash
adb logcat | grep "concurrent\|DownloadJob"
# Should show:
# - "Active downloads: 2"
# - Individual progress for each model
```

---

## Performance Benchmarks

### Download Speeds

| Network Type | Expected Speed | Acceptable Range |
|--------------|----------------|------------------|
| WiFi (50 Mbps) | 5-6 MB/s | 3-10 MB/s |
| WiFi (10 Mbps) | 1-1.2 MB/s | 0.5-2 MB/s |
| 4G LTE | 2-3 MB/s | 1-5 MB/s |
| 3G | 0.5-1 MB/s | 0.2-1.5 MB/s |

### Model Load Times (from cache)

| Model | Expected | Acceptable |
|-------|----------|------------|
| Gemma 2B INT4 | 1-2 seconds | <5 seconds |
| MobileBERT INT8 | 0.5-1 second | <2 seconds |

### Inference Latency (offline)

| Query Type | Expected | Acceptable |
|------------|----------|------------|
| Simple (1-10 tokens) | 1-2 seconds | <5 seconds |
| Medium (10-50 tokens) | 3-5 seconds | <10 seconds |
| Complex (50+ tokens) | 5-10 seconds | <20 seconds |

---

## Error Scenarios

### Common Errors to Test

1. **Network Timeout**
   - Simulate: Use poor network connection
   - Expected: Retry with exponential backoff

2. **Corrupted Download**
   - Simulate: Manually corrupt downloaded file
   - Expected: Checksum fails, re-download triggered

3. **Disk Full During Download**
   - Simulate: Fill storage during download
   - Expected: Download fails gracefully, partial file deleted

4. **Permission Denied**
   - Simulate: Revoke storage permission
   - Expected: Permission request shown

5. **Server Unavailable (404)**
   - Simulate: Invalid model URL
   - Expected: Clear error message, no retry

---

## Test Execution Checklist

- [ ] TC1: Fresh Install - Model Download ✅
- [ ] TC2: Model Already Downloaded (Cached) ✅
- [ ] TC3: Offline Mode - Cached Model Works ✅
- [ ] TC4: Offline Mode - Model Not Downloaded ✅
- [ ] TC5: Download Pause/Resume ✅
- [ ] TC6: Network Interruption During Download ✅
- [ ] TC7: Low Storage Warning ⚠️
- [ ] TC8: WiFi vs Cellular Download ⚠️
- [ ] TC9: SHA-256 Checksum Verification ✅
- [ ] TC10: Multiple Concurrent Downloads ⚠️

**Legend:**
- ✅ Pass
- ❌ Fail
- ⚠️ Needs attention
- ⏸️ Blocked
- 🔄 In progress

---

## Bug Report Template

```markdown
### Bug Report

**TC Number:** TC3
**Test:** Offline Mode - Cached Model Works
**Status:** ❌ FAIL

**Steps to Reproduce:**
1. Download model on WiFi
2. Enable airplane mode
3. Send chat message

**Expected:** Chat works offline
**Actual:** App crashes with NullPointerException

**Logs:**
```
E/AndroidRuntime: FATAL EXCEPTION: main
    java.lang.NullPointerException: model is null
    at LocalLLMProvider.generate()
```

**Severity:** High (blocks offline usage)
**Workaround:** Stay online
```

---

## Success Criteria

All test cases must pass with:
- ✅ 0 crashes
- ✅ 0 ANRs (Application Not Responding)
- ✅ User-friendly error messages
- ✅ Performance within acceptable ranges
- ✅ Offline mode fully functional after initial download

---

## Test Results Summary

**Executed:** 2025-11-04 (pending)
**Tester:** [Name]
**Device:** [Model, Android Version]
**Build:** development @ 3635d2a

| Test Case | Status | Notes |
|-----------|--------|-------|
| TC1 | ⏸️ Pending | Awaiting physical device |
| TC2 | ⏸️ Pending | Requires TC1 completion |
| TC3 | ⏸️ Pending | Requires TC1 completion |
| TC4 | ⏸️ Pending | |
| TC5 | ⏸️ Pending | |
| TC6 | ⏸️ Pending | |
| TC7 | ⏸️ Pending | Need low storage device |
| TC8 | ⏸️ Pending | |
| TC9 | ⏸️ Pending | |
| TC10 | ⏸️ Pending | |

**Overall Result:** ⏸️ PENDING

---

**Document Version:** 1.0
**Created:** 2025-11-04
**Updated:** 2025-11-04
**Status:** Ready for execution
