# AVA AI - Build Instructions

**Last Updated**: 2025-10-29
**Branch**: development
**Status**: ✅ Build issues resolved

---

## ⚠️ IMPORTANT: Pull Latest Changes

The compilation errors reported in `ava-ai-compile-issue.txt` have been **FIXED** and pushed to GitLab.

### Required Action

**Before building**, ensure you have the latest code:

```bash
cd /path/to/ava-ai
git pull origin development
```

**Critical commits required**:
- `0cdf5d8` - Fixed compile issue in build files (build.gradle dependencies)
- `0155f18` - Added missing Android resources (icons + app name)

---

## Compilation Error Summary

### Error Reported

```
> Task :app:processDebugResources FAILED

ERROR: resource mipmap/ic_launcher not found
ERROR: resource string/app_name not found
ERROR: resource mipmap/ic_launcher_round not found
```

### Root Cause

Missing Android resources that are referenced in `AndroidManifest.xml`:
1. App launcher icons (`ic_launcher`, `ic_launcher_round`)
2. App name string (`app_name`)

### Resolution

**Commit `0155f18`** added 15 Android resource files:

```
app/src/main/res/
├── values/
│   └── strings.xml                    # ✅ App name: "AVA AI"
│
├── mipmap-anydpi-v26/                 # ✅ Adaptive icons (Android 8.0+)
│   ├── ic_launcher.xml
│   └── ic_launcher_round.xml
│
├── drawable/                           # ✅ Icon layers
│   ├── ic_launcher_background.xml
│   └── ic_launcher_foreground.xml
│
└── mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/  # ✅ PNG fallbacks (10 files)
    ├── ic_launcher.png
    └── ic_launcher_round.png
```

**Result**: All AAPT resource linking errors are resolved.

---

## Build Steps

### 1. Pull Latest Code

```bash
git pull origin development
```

**Expected output**:
```
Updating 0cdf5d8..8ade8a6
Fast-forward
 app/src/main/res/...   (15 files changed)
 docs/COMPILATION_FIX_REPORT.md   (1 file changed)
```

### 2. Clean Build

```bash
./gradlew clean
```

### 3. Build Debug APK

```bash
./gradlew :app:assembleDebug
```

**Expected output**:
```
BUILD SUCCESSFUL in Xs
```

### 4. Install on Device (Optional)

```bash
./gradlew :app:installDebug
```

---

## Build Configuration

### Gradle Version

- Gradle: **8.x** (specified in `gradle/wrapper/gradle-wrapper.properties`)
- Android Gradle Plugin: Check `gradle/libs.versions.toml`

### Android SDK Requirements

- **compileSdk**: 34 (Android 14)
- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 34 (Android 14)

### Kotlin Version

- Kotlin: **1.9.0** (check `gradle/libs.versions.toml`)

---

## Verification Checklist

After pulling and building, verify:

- [ ] `git log --oneline -1` shows commit `8ade8a6` or later
- [ ] `app/src/main/res/values/strings.xml` exists
- [ ] `app/src/main/res/mipmap-mdpi/ic_launcher.png` exists
- [ ] `./gradlew :app:assembleDebug` completes successfully
- [ ] APK generated at `app/build/outputs/apk/debug/app-debug.apk`

---

## Troubleshooting

### Issue: "resource not found" errors persist

**Solution**:
```bash
# Force clean
./gradlew clean --no-build-cache

# Verify you have latest code
git log --oneline -5

# Should show commits:
# 8ade8a6 docs: Add compilation issue fix report
# 0155f18 fix: Add missing Android resources
# 0cdf5d8 Fixed compile issue in build files

# Rebuild
./gradlew :app:assembleDebug
```

### Issue: Gradle wrapper not executable

**Solution**:
```bash
chmod +x gradlew
./gradlew --version
```

### Issue: Build.gradle dependency conflicts

**Already Fixed** in commit `0cdf5d8`. If you still see dependency issues after pulling:

1. Check `app/build.gradle` for duplicate dependencies
2. Verify `gradle/libs.versions.toml` has consistent versions
3. Run `./gradlew dependencies` to see dependency tree

### Issue: Out of memory during build

**Solution**:
```bash
# Add to gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

---

## Build Outputs

### Debug APK Location

```
app/build/outputs/apk/debug/app-debug.apk
```

### Build Reports

```
app/build/reports/
├── lint-results-debug.html
└── tests/
```

---

## Next Steps After Successful Build

1. **Test on Device**:
   ```bash
   ./gradlew :app:installDebug
   adb logcat | grep -i "ava"
   ```

2. **Run Tests** (when available):
   ```bash
   ./gradlew :features:chat:ui:testDebugUnitTest
   ```

3. **Replace Placeholder Icons**:
   - Current icons are development placeholders
   - Replace with branded AVA AI icons before production release
   - See `docs/COMPILATION_FIX_REPORT.md` for icon design guidelines

---

## Documentation References

- **Compilation Fix Details**: `docs/COMPILATION_FIX_REPORT.md`
- **Developer Manual**: `docs/DEVELOPER_MANUAL_PART1.md` + `PART2.md`
- **User Manual**: `docs/USER_MANUAL.md`
- **Pending Items**: `docs/PENDING_ITEMS.md`

---

## Summary

✅ **All compilation errors have been resolved**
✅ **Android resources added** (15 files)
✅ **Build.gradle dependencies fixed**
✅ **Changes pushed to GitLab** (development branch)

**Action Required**: `git pull origin development` before building

---

**Document Version**: 1.0
**Created**: 2025-10-29
**Related Commits**: 0cdf5d8, 0155f18, 8ade8a6
