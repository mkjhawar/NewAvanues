# SpeechRecognition Module Changelog - October 2025

## 2025-10-14

### Vivoka Engine Initialization Fixes
**Time:** 03:07:53 PDT
**Author:** Manoj Jhawar
**Type:** Critical Bug Fix

#### Issues Resolved
- Fixed Vivoka engine initialization failures
- Resolved Android API level incompatibility (API 29 → API 28)
- Fixed corrupted asset file reference in Vivoka configuration
- Corrected initialization flow race conditions

#### Changes Made

##### 1. Android API Level Compatibility (`build.gradle.kts`)
**File:** `modules/libraries/SpeechRecognition/build.gradle.kts:22`
- **Reverted** minSdk from 29 (Android 10) back to 28 (Android 9 Pie)
- **Reason:** Vivoka SDK requires Android 9+ compatibility
- **Impact:** Restores Vivoka engine compatibility across supported devices

##### 2. Vivoka Asset File Configuration (`vsdk.json`)
**File:** `modules/libraries/SpeechRecognition/src/main/assets/vsdk/config/vsdk.json:19`
- **Changed:** `asreng-US` model file reference
- **From:** `"file": "asreng-US.fcf"` (0 bytes - corrupted/empty)
- **To:** `"file": "ctx-primary_eng-USA_vocon_car_202403201810.fcf"` (67MB - valid)
- **Reason:** Original file was empty causing `APPLICATION_SEARCH_SPACE_INVALID` error
- **Impact:** Both FreeSpeech and asreng-US models now use valid context file

#### Related VoiceOSCore Changes
See voiceos-master changelog for:
- Fixed initialization flow race conditions in VoiceOSService
- Added proper state observation for Vivoka engine
- Ensured single startListening() call at initialization
- Added UUIDCreator initialization before LearnAppIntegration

#### Errors Fixed
1. `APPLICATION_SEARCH_SPACE_INVALID (0xdcc2a25f)` - Asset file corruption
2. `ASRMANAGER_VOCON_UNEXPECTED_FAILURE (0xeab28370)` - Invalid asset reference
3. Vivoka initialization failures due to API level incompatibility
4. Race condition in engine initialization sequence

#### Impact
- Vivoka engine now initializes successfully
- Voice recognition fully operational
- Asset loading works correctly
- No more initialization timing issues

#### Testing
- Vivoka engine initialization verified
- Asset file loading confirmed
- Voice recognition flow tested
- No regressions in other speech engines (VOSK, Google STT)

---

## Previous Updates
See changelog-2025-09.md for earlier changes

**Last Updated:** 2025-10-14 03:07:53 PDT
