# VoiceOS APK Size Reduction Analysis - 2025-11-28

## Current APK Size

**Debug APK:** 161.7 MB (compressed), 308 MB (uncompressed)

---

## Size Breakdown

### 1. DEX Files (Code): 88.67 MB
- classes.dex: 32.79 MB (main code)
- classes24-26.dex: 40+ MB (Compose UI, Hilt)
- Remaining 22 DEX files: ~15 MB

### 2. Native Libraries: 59.25 MB (84 files)
- **Speech engines:** ~32 MB
- **Other native libs:** ~27 MB

### 3. Resources: 2.58 MB

---

## REDUNDANCIES FOUND ⚠️

### 🔴 MAJOR: Multiple Speech Recognition Engines (32 MB)

**Problem:** VoiceOS includes **THREE** speech recognition engines, but likely only uses ONE:

#### Vivoka SDK: ~22 MB
```
lib/arm64-v8a/libnds_asr5.so                 6.72 MB
lib/arm64-v8a/libnds_asr5_stub_textproc.so   6.69 MB
lib/arm64-v8a/libtextproc.so                 5.27 MB
lib/arm64-v8a/libdd_common.so                3.66 MB
+ 10 more Vivoka libraries
```

**Source:** app/build.gradle.kts includes:
```kotlin
implementation(files("${rootDir}/vivoka/vsdk-6.0.0.aar"))           // 126 KB
implementation(files("${rootDir}/vivoka/vsdk-csdk-asr-2.0.0.aar"))  // 36 MB
implementation(files("${rootDir}/vivoka/vsdk-csdk-core-1.0.1.aar")) // 33 MB
```

#### Vocon SDK: ~10 MB
```
lib/arm64-v8a/libvocon_pron.so              4.82 MB
lib/arm64-v8a/libvocon_sem3.so              2.10 MB
lib/arm64-v8a/libvocon_asr2.so              1.81 MB
lib/arm64-v8a/libvocon_gram2.so             0.71 MB
lib/arm64-v8a/libvocon_base.so              0.59 MB
lib/arm64-v8a/libvocon_curl.so              0.57 MB
+ 6 more Vocon libraries
```

#### Vosk SDK: 8.45 MB
```
lib/arm64-v8a/libvosk.so                    8.45 MB
```

**Usage Analysis:**
- Vivoka imports in VoiceOSCore: **1 file only**
- Vosk imports in VoiceOSCore: **1 file only**
- Most code appears to be **stub implementations**

**Recommendation:** Choose ONE primary speech engine and remove others.

**Potential Savings:** 20-25 MB (keeping one engine)

---

### 🟡 MEDIUM: Debug-Only Dependencies

#### Issue: Debug build includes development dependencies
- Hilt compiler artifacts
- Compose UI tooling
- Debug symbols
- Test libraries

**Recommendation:** Use release build with ProGuard/R8

**Potential Savings:** 30-40 MB (via code shrinking)

---

### 🟢 LOW: Single Architecture Only

**Current State:** ✅ GOOD
- Only arm64-v8a is included (no redundant architectures)
- This is already optimized

---

## RECOMMENDED SIZE REDUCTIONS

### Priority 1: Remove Unused Speech Engines (HIGH IMPACT)

**Choose ONE engine to keep:**

#### Option A: Keep Vosk (Open Source)
- Remove Vivoka AAR files (69 MB)
- Remove Vocon libraries
- **Savings: ~32 MB**

#### Option B: Keep Vivoka (Commercial)
- Remove Vosk dependency
- Remove Vocon libraries
- **Savings: ~18 MB**

#### Option C: Keep Google Cloud Speech (Recommended)
- Remove all offline engines
- Use Google Cloud Speech API
- **Savings: ~32 MB**
- **Benefit:** No local libraries, smaller APK, better accuracy

**Implementation:**
```kotlin
// app/build.gradle.kts - REMOVE:
implementation(files("${rootDir}/vivoka/vsdk-6.0.0.aar"))
implementation(files("${rootDir}/vivoka/vsdk-csdk-asr-2.0.0.aar"))
implementation(files("${rootDir}/vivoka/vsdk-csdk-core-1.0.1.aar"))

// modules/apps/VoiceOSCore/build.gradle.kts - REMOVE:
implementation("com.alphacephei:vosk-android:0.3.47")
```

---

### Priority 2: Enable ProGuard/R8 for Release Builds (HIGH IMPACT)

**Current:** `isMinifyEnabled = false`

**Change to:**
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

**Benefits:**
- Remove unused code from libraries
- Reduce DEX file size by 30-40%
- Obfuscate code

**Potential Savings:** 30-40 MB

---

### Priority 3: Use Android App Bundle (MEDIUM IMPACT)

**Instead of APK, build AAB:**
```bash
./gradlew bundleRelease
```

**Benefits:**
- Google Play splits APK by architecture, language, screen density
- Users only download what they need
- APK size appears smaller on device

**Potential Savings:** 20-30% smaller download size

---

### Priority 4: Remove Unused Compose UI Components (LOW IMPACT)

**Check for unused Compose Material 3 icons and components**

**Potential Savings:** 2-5 MB

---

## PROJECTED SIZE REDUCTIONS

### Conservative Approach (Keep One Speech Engine)
| Action | Savings |
|--------|---------|
| Remove 2 speech engines | -22 MB |
| Enable ProGuard/R8 | -35 MB |
| **Total** | **~126 MB → 69 MB** |

### Aggressive Approach (Cloud Speech API)
| Action | Savings |
|--------|---------|
| Remove all speech engines | -32 MB |
| Enable ProGuard/R8 | -35 MB |
| Use App Bundle | -15 MB |
| **Total** | **~126 MB → 44 MB** |

---

## IMPLEMENTATION PLAN

### Phase 1: Quick Wins (Immediate)

1. **Remove unused speech engines** (15 minutes)
   ```bash
   # Decide which engine to keep
   # Edit app/build.gradle.kts
   # Remove unused implementations
   ```

2. **Enable ProGuard for release builds** (30 minutes)
   ```bash
   # Edit app/build.gradle.kts
   # Test release build
   # Fix ProGuard rules if needed
   ```

   **Expected Result:** APK size 70-80 MB

### Phase 2: Optimization (1-2 hours)

3. **Create ProGuard rules for kept dependencies**
4. **Test release build thoroughly**
5. **Create App Bundle**

   **Expected Result:** APK size 50-60 MB

### Phase 3: Advanced (2-4 hours)

6. **Migrate to cloud-based speech recognition**
7. **Remove all offline speech engines**
8. **Implement dynamic feature modules**

   **Expected Result:** APK size 40-50 MB

---

## VERIFICATION COMMANDS

### Check current size:
```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

### Analyze APK composition:
```bash
./gradlew :app:analyzeDebugBundle
# or
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app.apks --mode=universal
```

### Compare release vs debug:
```bash
./gradlew assembleRelease
ls -lh app/build/outputs/apk/release/app-release.apk
```

---

## RECOMMENDATIONS BY USE CASE

### For Development (Debug Builds)
- **Keep current size** - 161 MB is acceptable for debugging
- Faster builds without ProGuard
- All engines available for testing

### For Internal Testing (Alpha/Beta)
- **Enable ProGuard** → 80-90 MB
- Remove one speech engine → 70 MB
- Good balance of size and functionality

### For Production Release
- **Enable ProGuard + R8** → 60-70 MB
- **Use App Bundle** → Effective size 40-50 MB
- **Choose ONE speech engine** → 50 MB
- **Consider cloud API** → 40 MB

---

## RISKS & CONSIDERATIONS

### Removing Speech Engines

**Risk:** Feature loss if users rely on offline recognition

**Mitigation:**
- Test thoroughly before removing
- Document which engine is kept
- Consider feature flags for engine selection

### ProGuard/R8 Issues

**Risk:** Runtime crashes from over-aggressive shrinking

**Mitigation:**
- Add keep rules for reflection-based code
- Test all features after enabling
- Use `-dontobfuscate` initially for debugging

### App Bundle

**Risk:** Cannot sideload on non-Google Play devices

**Mitigation:**
- Still generate universal APK for sideloading
- Use `--mode=universal` flag with bundletool

---

## CONCLUSION

**Current APK:** 161.7 MB (debug)

**Recommended Target:** 50-70 MB (release)

**Primary Actions:**
1. ✅ Remove unused speech engines (save 20-25 MB)
2. ✅ Enable ProGuard/R8 (save 30-40 MB)
3. ✅ Use App Bundle for distribution

**Total Potential Savings:** 50-65 MB (30-40% reduction)

---

## NEXT STEPS

**Immediate (Today):**
1. Decide which speech engine to keep (Vosk, Vivoka, or Cloud API)
2. Remove unused engine dependencies from build.gradle.kts
3. Test build to ensure no breakage

**Short Term (This Week):**
1. Enable ProGuard for release builds
2. Create and test release APK
3. Fix any ProGuard-related issues

**Long Term (Next Sprint):**
1. Migrate to App Bundle distribution
2. Consider cloud-based speech recognition
3. Implement dynamic feature delivery

---

**Author:** Claude Code (VoiceOS Size Analysis)
**Date:** 2025-11-28 02:00
**APK Analyzed:** app-debug.apk (161.7 MB)
