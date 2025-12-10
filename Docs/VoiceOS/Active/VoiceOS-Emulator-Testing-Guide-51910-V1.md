# Android Emulator Testing Guide

**Date:** 2025-10-19 05:02:00 PDT
**Author:** Manoj Jhawar
**Purpose:** Manual testing guide using Android Studio emulator
**Emulator:** Navigator_500
**Status:** Ready to test

---

## Quick Answer: YES! ✅

**You CAN test on Android Studio emulator!**

You have an emulator available: **Navigator_500**

However, there are some **limitations** for VoiceOS testing:

### ✅ What WILL Work on Emulator
- App installation and launch
- UI testing (VoiceCursor settings, etc.)
- Most VoiceCursor features
- Cursor type persistence testing
- Settings controls verification
- Resource loading validation
- Database operations
- Basic accessibility service functionality

### ⚠️ What MAY NOT Work on Emulator
- **Voice recognition** (no microphone by default on emulator)
- **IMU/head tracking** (no motion sensors on emulator)
- **Real speech input** (Vivoka SDK may not work without audio hardware)
- **Performance testing** (emulator is slower than real device)

### 🔴 What DEFINITELY Won't Work on Emulator
- Real-world voice command accuracy comparison
- Actual speech-to-text recognition
- Production performance metrics

---

## Testing Strategy

### Phase 1: VoiceCursor Testing (FULLY TESTABLE) ✅

**What to Test:**
1. Cursor type persistence (our recent fix)
2. Settings UI controls
3. Resource loading validation
4. Cursor rendering
5. Filter settings
6. Configuration persistence

**Confidence:** 95% - Emulator is sufficient for VoiceCursor testing

---

### Phase 2: VoiceRecognition Testing (LIMITED) ⚠️

**What Can Be Tested:**
1. App doesn't crash on initialization
2. Hilt DI works (services inject correctly)
3. UI elements render
4. Settings are accessible

**What Cannot Be Tested:**
1. Actual voice recognition
2. Vivoka SDK functionality
3. Speech-to-text accuracy
4. Microphone input processing

**Confidence:** 40% - Can verify no crashes, but not functionality

---

## Step-by-Step Emulator Testing

### Step 1: Start the Emulator

```bash
# Start emulator in background
~/Library/Android/sdk/emulator/emulator -avd Navigator_500 &

# Wait for emulator to boot (usually 30-60 seconds)
# You'll see emulator window appear
```

**Alternative:** Open Android Studio → Tools → AVD Manager → Click ▶️ on Navigator_500

---

### Step 2: Verify Emulator is Running

```bash
# Check connected devices
~/Library/Android/sdk/platform-tools/adb devices

# Should show:
# List of devices attached
# emulator-5554   device
```

---

### Step 3: Install the APK

```bash
cd "/Volumes/M Drive/Coding/vos4"

# Install app
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# Should show: Success
```

---

### Step 4: Enable VoiceOS Accessibility Service

**Manual Steps on Emulator:**

1. **Open Settings** on emulator
   - Swipe up from bottom to open app drawer
   - Click "Settings" (gear icon)

2. **Navigate to Accessibility**
   - Settings → Accessibility
   - Or search for "Accessibility"

3. **Enable VoiceOS**
   - Find "VoiceOS" in the list
   - Toggle it ON
   - Accept permission dialog

4. **Verify Service is Running**
   - Should show "On" status
   - No crash = ✅ Good sign

---

### Step 5: Test VoiceCursor

#### 5A. Test Cursor Type Persistence (Our Bug Fix) 🎯

**Test Steps:**
1. Open VoiceOS app
2. Navigate to VoiceCursor settings
3. Change cursor type: Normal → Hand
4. Close app completely (swipe up from recent apps, swipe away VoiceOS)
5. Reopen VoiceOS app
6. Check VoiceCursor settings
7. **Expected:** Cursor type is still "Hand" ✅

**Repeat for:**
- Hand → Custom
- Custom → Normal
- Normal → Hand

**Success Criteria:** Cursor type persists across app restarts

---

#### 5B. Test Settings Controls

**Test Each Control:**

1. **Cursor Size**
   - Change slider
   - Verify preview updates (if preview is visible)
   - Save and reopen
   - Verify setting persisted

2. **Cursor Speed**
   - Change slider
   - Save and reopen
   - Verify setting persisted

3. **Cursor Color**
   - Change color
   - Save and reopen
   - Verify setting persisted

4. **Jitter Filter**
   - Toggle enable/disable
   - Change strength (Low/Medium/High)
   - Save and reopen
   - Verify settings persisted

5. **Motion Sensitivity**
   - Change slider
   - Save and reopen
   - Verify setting persisted

**Success Criteria:** All settings persist correctly

---

#### 5C. Test Resource Loading (Our Enhancement)

**Monitor Logcat:**

```bash
# Open logcat in separate terminal
~/Library/Android/sdk/platform-tools/adb logcat | grep -i "ResourceProvider\|cursor.*resource"
```

**Test:**
1. Change cursor types multiple times
2. Watch logcat for any errors
3. **Expected:** No "Resource not found" errors
4. **Expected:** No crashes when loading cursor resources

**Success Criteria:**
- No resource loading errors in logcat
- All cursor types render correctly

---

### Step 6: Test VoiceRecognition (Limited)

#### 6A. Test Service Initialization

**Monitor Logcat:**

```bash
~/Library/Android/sdk/platform-tools/adb logcat | grep -i "VoiceOS\|Vivoka\|Speech"
```

**Test:**
1. Launch app
2. Watch logcat for initialization messages
3. Look for:
   - "VoiceOSService started"
   - "SpeechManager initialized"
   - "VivokaEngine initialized"
   - NO crashes or errors

**Success Criteria:**
- Services initialize without crashes
- Hilt DI injection works (no "lateinit not initialized" errors)

---

#### 6B. Test Settings UI

**Test:**
1. Open VoiceOS settings
2. Navigate to Voice Recognition settings (if available)
3. Verify UI renders correctly
4. Change any available settings
5. Save and reopen
6. Verify settings persisted

**Success Criteria:**
- UI renders without crashes
- Settings persist

---

### Step 7: Check for Crashes

**Monitor for Crashes:**

```bash
# Watch for crashes
~/Library/Android/sdk/platform-tools/adb logcat | grep -i "crash\|exception\|error\|fatal"
```

**Test:**
1. Use app for 5-10 minutes
2. Change various settings
3. Navigate through different screens
4. Restart app multiple times

**Success Criteria:**
- No crashes during normal usage
- No unhandled exceptions in logcat

---

## Test Checklist

### VoiceCursor Tests ✅

- [ ] Cursor type persistence (Normal → Hand → Custom)
- [ ] Cursor type persists after app restart
- [ ] Cursor size setting works
- [ ] Cursor speed setting works
- [ ] Cursor color setting works
- [ ] Jitter filter enable/disable works
- [ ] Filter strength (Low/Medium/High) works
- [ ] Motion sensitivity slider works
- [ ] All settings persist after restart
- [ ] No resource loading errors in logcat
- [ ] All cursor types render correctly
- [ ] No crashes during settings changes

### VoiceRecognition Tests (Limited) ⚠️

- [ ] App installs successfully
- [ ] Accessibility service enables without crash
- [ ] VoiceOSService initializes
- [ ] SpeechManager initializes
- [ ] VivokaEngine initializes
- [ ] No Hilt DI errors in logcat
- [ ] Settings UI renders
- [ ] Settings persist
- [ ] No crashes during normal usage

### General Tests ✅

- [ ] App launches successfully
- [ ] No crashes during 10-minute usage
- [ ] Database operations work
- [ ] Settings persist across restarts
- [ ] UI renders correctly
- [ ] No memory leaks (check logcat)

---

## Expected Test Results

### ✅ High Confidence (Should Work)

**VoiceCursor:**
- Cursor type persistence: ✅ Should work perfectly
- Settings persistence: ✅ Should work perfectly
- Resource loading: ✅ Should work with validation
- UI rendering: ✅ Should work

**Confidence:** 95%

---

### ⚠️ Medium Confidence (May Work Partially)

**VoiceRecognition:**
- Service initialization: ⚠️ May work, but without speech
- Hilt DI: ✅ Should work
- Settings UI: ✅ Should work
- Vivoka SDK init: ⚠️ May initialize but not function

**Confidence:** 50%

---

### 🔴 Low Confidence (Won't Work)

**Voice Features:**
- Actual voice recognition: 🔴 Won't work (no mic hardware)
- Speech-to-text: 🔴 Won't work
- Vivoka accuracy: 🔴 Cannot test
- Real-world performance: 🔴 Cannot test

**Confidence:** 10%

---

## Workarounds for Emulator Limitations

### For Voice Testing (if needed)

**Option 1: Enable Emulator Microphone**
```bash
# Start emulator with host audio
~/Library/Android/sdk/emulator/emulator -avd Navigator_500 -qemu -audiodev id=mic,driver=coreaudio
```

**Note:** May not work reliably for Vivoka SDK

**Option 2: Simulated Input**
- Manually trigger commands via UI buttons (if available)
- Use test mode to bypass speech recognition
- Focus testing on non-voice features

---

## Troubleshooting

### Problem: Emulator Won't Start

**Solution:**
```bash
# Kill any existing emulator processes
killall qemu-system-x86_64

# Start fresh
~/Library/Android/sdk/emulator/emulator -avd Navigator_500
```

---

### Problem: APK Install Fails

**Solution:**
```bash
# Uninstall old version first
~/Library/Android/sdk/platform-tools/adb uninstall com.augmentalis.voiceos

# Reinstall
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### Problem: Accessibility Service Won't Enable

**Possible Causes:**
1. Service crashed on initialization
2. Permissions not granted
3. Hilt DI error

**Check Logcat:**
```bash
~/Library/Android/sdk/platform-tools/adb logcat | grep -i "voiceos\|accessibility"
```

---

### Problem: App Crashes Immediately

**Debug Steps:**
```bash
# Get crash logs
~/Library/Android/sdk/platform-tools/adb logcat | grep -A 20 "FATAL EXCEPTION"

# Check for Hilt errors
~/Library/Android/sdk/platform-tools/adb logcat | grep -i "hilt\|dagger"
```

---

## Alternative: Use Android Studio's Run/Debug

**Easier Method:**

1. **Open Android Studio**
2. **Open VOS4 project**
   - File → Open → `/Volumes/M Drive/Coding/vos4`
3. **Select Emulator**
   - Device dropdown → Navigator_500
4. **Click Run (▶️)**
   - Android Studio will:
     - Build APK
     - Start emulator if needed
     - Install app
     - Launch app
     - Show logcat automatically
5. **Use Debugger**
   - Set breakpoints in code
   - Step through execution
   - Inspect variables

**Advantages:**
- Integrated logcat viewer
- Debugger support
- Easier crash investigation
- Automatic APK rebuild

---

## Recommended Testing Workflow

### Quick Test (15 minutes)

1. Start emulator
2. Install APK
3. Test cursor type persistence (3 types)
4. Test 3-4 settings changes
5. Check logcat for errors
6. **Done!**

---

### Thorough Test (45 minutes)

1. Start emulator with logcat monitoring
2. Install APK
3. Enable accessibility service
4. Test all VoiceCursor settings
5. Test cursor type persistence (all combinations)
6. Monitor resource loading in logcat
7. Test settings persistence (restart app 3 times)
8. Navigate through all UI screens
9. Check for memory leaks
10. Verify no crashes
11. **Done!**

---

## What You'll Learn from Emulator Testing

### ✅ Will Confirm
- Cursor type persistence fix works
- Resource validation prevents crashes
- Settings persist correctly
- UI renders properly
- No Hilt DI errors
- App doesn't crash on basic usage

### ⚠️ Won't Confirm
- Voice recognition accuracy
- Vivoka SDK functionality in production
- Real device performance
- Actual speech processing

### 🎯 Recommended Approach

**Use emulator for:**
- VoiceCursor testing (comprehensive)
- Basic app stability testing
- Settings persistence verification
- UI/UX verification

**Use real device for:**
- Voice recognition testing
- Production performance testing
- Vivoka SDK verification
- End-to-end voice command testing

---

## Quick Start Commands

```bash
# 1. Start emulator
~/Library/Android/sdk/emulator/emulator -avd Navigator_500 &

# 2. Wait 30 seconds, then check
~/Library/Android/sdk/platform-tools/adb devices

# 3. Install app
cd "/Volumes/M Drive/Coding/vos4"
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Monitor logcat
~/Library/Android/sdk/platform-tools/adb logcat | grep -i "VoiceOS\|ResourceProvider\|cursor"

# 5. Test manually on emulator screen
# - Enable accessibility service
# - Test cursor type persistence
# - Test settings controls
```

---

## Conclusion

**YES, you can test on Android Studio emulator!**

**Best for:**
- ✅ VoiceCursor testing (95% coverage)
- ✅ Settings persistence testing (100% coverage)
- ✅ Resource validation testing (100% coverage)
- ✅ UI/UX testing (100% coverage)
- ✅ Basic stability testing (90% coverage)

**Not suitable for:**
- 🔴 Voice recognition accuracy
- 🔴 Vivoka SDK production testing
- 🔴 Real-world performance metrics

**Recommendation:**
1. Test VoiceCursor thoroughly on emulator (15-45 min)
2. Test VoiceRecognition basics on emulator (10 min)
3. Plan real device testing for voice features (when available)

**Confidence in Emulator Testing:**
- VoiceCursor: 95% ✅
- VoiceRecognition (non-voice features): 70% ⚠️
- VoiceRecognition (voice features): 10% 🔴

---

**Ready to start? Run the Quick Start Commands above!**

Author: Manoj Jhawar
Date: 2025-10-19 05:02:00 PDT
