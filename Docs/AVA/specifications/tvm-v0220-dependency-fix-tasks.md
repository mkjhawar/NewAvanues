# TVM v0.22.0 Native Library Dependencies Fix - Task Breakdown

**Version**: 1.0
**Status**: Ready for Execution
**Author**: AVA AI Team
**Created**: 2025-11-27
**Based On**: [tvm-v0220-dependency-fix-plan.md](./tvm-v0220-dependency-fix-plan.md)
**Total Tasks**: 21
**Estimated Effort**: 6 hours

---

## Task Overview

| Phase | Tasks | Duration | Status |
|-------|-------|----------|--------|
| Phase 1: Investigation | 3 | 1h | ⏳ Pending |
| Phase 2: Obtain Libraries | 6 | 2h | ⏳ Pending |
| Phase 3: Build & Test | 7 | 2h | ⏳ Pending |
| Phase 4: Documentation | 5 | 1h | ⏳ Pending |
| **Total** | **21** | **6h** | |

---

## Phase 1: Investigation & Dependency Location

**Duration**: 1 hour
**Dependencies**: None

### Task 1.1: Locate libc++_shared.so in Android NDK
**Priority**: P0
**Estimated Time**: 15 minutes

**Steps**:
```bash
# 1. Check ANDROID_SDK_ROOT environment variable
echo $ANDROID_SDK_ROOT

# 2. Find libc++_shared.so for arm64-v8a
find ~/Library/Android/sdk/ndk -name "libc++_shared.so" -path "*/aarch64-linux-android/*" 2>/dev/null

# 3. Verify expected location
ls -lh ~/Library/Android/sdk/ndk/26.1.10909125/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so

# 4. Check file details
file ~/Library/Android/sdk/ndk/26.1.10909125/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so
```

**Success Criteria**:
- [ ] File found at expected location
- [ ] File size ~1.7MB
- [ ] File type: ELF 64-bit LSB shared object, ARM aarch64
- [ ] Path documented for copy step

**Deliverable**: Path to `libc++_shared.so` confirmed

---

### Task 1.2: Search for libtvm_ffi.so in project
**Priority**: P0
**Estimated Time**: 20 minutes

**Steps**:
```bash
# 1. Search entire AVA project
find /Volumes/M-Drive/Coding/AVA -name "*tvm*ffi*.so" 2>/dev/null

# 2. Search external/mlc-llm build artifacts
find /Volumes/M-Drive/Coding/AVA/external -name "*.so" | grep -i ffi

# 3. Check if TVM build directory exists
ls -la ~/Coding/ava/external/mlc-llm/android/mlc4j/build/lib/arm64-v8a/ 2>/dev/null

# 4. Search Git LFS objects (might be in LFS history)
git lfs ls-files | grep -i ffi

# 5. Check git log for when it might have been added/removed
git log --all --oneline -- "*ffi*.so"
```

**Outcomes**:
- **If found**: Note exact path, verify architecture
- **If NOT found**: Proceed to Task 1.3 to analyze rebuild needs

**Success Criteria**:
- [ ] Search completed in all locations
- [ ] Result documented (found or not found)
- [ ] If found: file verified as ARM64-v8a

**Deliverable**: Status of `libtvm_ffi.so` (found/rebuild required)

---

### Task 1.3: Analyze libtvm_runtime.so dependencies
**Priority**: P0
**Estimated Time**: 25 minutes

**Steps**:
```bash
# 1. Check if readelf/otool is available (macOS doesn't have readelf by default)
which otool  # macOS
which readelf  # Linux

# 2. For macOS: Use otool to check dependencies
otool -L /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_runtime.so 2>/dev/null

# 3. Alternative: Use nm to check undefined symbols
nm -u /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_runtime.so 2>/dev/null | grep -i ffi | head -20

# 4. Check if FFI symbols are present
nm -D /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_runtime.so 2>/dev/null | grep -E "(TVMFFIFunctionCall|tvm_ffi)" | head -10

# 5. Check libtvm4j.so dependencies
otool -L /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm4j.so 2>/dev/null
```

**Analysis Questions**:
1. Does `libtvm_runtime.so` reference `libtvm_ffi.so` as external dependency?
2. Are FFI symbols defined (statically linked) or undefined (needs external lib)?
3. Does `libtvm4j.so` require `libc++_shared.so`?

**Success Criteria**:
- [ ] Dependencies analyzed for both libraries
- [ ] Confirmed whether `libtvm_ffi.so` is actually needed
- [ ] Alternative: FFI might be statically linked (no external dependency)
- [ ] Results documented

**Deliverable**: Dependency analysis report + decision on rebuild necessity

---

## Phase 2: Obtain Missing Libraries

**Duration**: 2 hours
**Dependencies**: Phase 1 complete
**Branch Point**: Task 1.2 outcome determines path (2A or 2B)

### Path 2A: Libraries Found (1 hour)

#### Task 2A.1: Copy libc++_shared.so to project
**Priority**: P0
**Estimated Time**: 10 minutes
**Condition**: Always execute (NDK library always available)

**Steps**:
```bash
# 1. Create jniLibs directory if needed
mkdir -p /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/

# 2. Copy from NDK
cp ~/Library/Android/sdk/ndk/26.1.10909125/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so \
   /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/

# 3. Verify copy
ls -lh /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libc++_shared.so

# 4. Check file type
file /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libc++_shared.so
```

**Success Criteria**:
- [ ] File copied successfully
- [ ] Size: ~1.7MB
- [ ] Type: ELF 64-bit ARM aarch64

**Deliverable**: `libc++_shared.so` in jniLibs directory

---

#### Task 2A.2: Copy libtvm_ffi.so to project
**Priority**: P0
**Estimated Time**: 10 minutes
**Condition**: Only if Task 1.2 found the file

**Steps**:
```bash
# 1. Copy from found location (replace {PATH} with actual path from Task 1.2)
cp {PATH_FROM_TASK_1.2}/libtvm_ffi.so \
   /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/

# 2. Verify copy
ls -lh /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_ffi.so

# 3. Check file type
file /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_ffi.so

# 4. Set executable permissions
chmod +x /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_ffi.so
```

**Success Criteria**:
- [ ] File copied successfully
- [ ] Type: ELF 64-bit ARM aarch64
- [ ] Executable permission set

**Deliverable**: `libtvm_ffi.so` in jniLibs directory

---

#### Task 2A.3: Verify all libraries present
**Priority**: P0
**Estimated Time**: 10 minutes
**Condition**: Always execute after copy

**Steps**:
```bash
# 1. List all .so files
ls -lh /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/*.so

# 2. Verify expected files
expected=(
  "libc++_shared.so"
  "libtvm_ffi.so"
  "libtvm_runtime.so"  # Should already exist
  "libtvm4j.so"        # Should already exist
)

for lib in "${expected[@]}"; do
  if [ -f "/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/$lib" ]; then
    echo "✅ $lib"
  else
    echo "❌ $lib MISSING"
  fi
done

# 3. Check total size
du -sh /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/
```

**Success Criteria**:
- [ ] All 4 libraries present
- [ ] Total size reasonable (~65MB unstripped)
- [ ] All files are ARM64-v8a ELF

**Deliverable**: Complete library set verified

---

### Path 2B: Rebuild TVM v0.22.0 (2 hours)

#### Task 2B.1: Setup TVM build environment
**Priority**: P0
**Estimated Time**: 30 minutes
**Condition**: Only if Task 1.2 did NOT find `libtvm_ffi.so`

**Steps**:
```bash
# 1. Verify NDK installation
export ANDROID_NDK=~/Library/Android/sdk/ndk/26.1.10909125
echo $ANDROID_NDK
ls -ld $ANDROID_NDK

# 2. Verify CMake version
cmake --version
# Required: 3.22 or later

# 3. Navigate to MLC-LLM source
cd ~/Coding/ava/external/mlc-llm/

# 4. Check if source exists
ls -la android/mlc4j/

# 5. Verify Rust installation (needed for tokenizers)
rustc --version
# If not installed: curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

# 6. Add Android target to Rust
rustup target add aarch64-linux-android

# 7. Create build directory
mkdir -p android/mlc4j/build
```

**Success Criteria**:
- [ ] NDK path verified
- [ ] CMake 3.22+ available
- [ ] MLC-LLM source present
- [ ] Rust toolchain ready
- [ ] Build directory created

**Deliverable**: Build environment ready

---

#### Task 2B.2: Configure TVM build
**Priority**: P0
**Estimated Time**: 20 minutes
**Condition**: Continuation of Path 2B

**Steps**:
```bash
# 1. Navigate to build directory
cd ~/Coding/ava/external/mlc-llm/android/mlc4j/build

# 2. Run CMake configuration
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-28 \
  -DUSE_JAVA_FFI=ON \
  -DBUILD_STATIC=OFF \
  -DCMAKE_BUILD_TYPE=Release

# 3. Verify configuration success
# Expected: "Configuring done" and "Generating done"

# 4. Check generated Makefiles
ls -la
```

**Success Criteria**:
- [ ] CMake configuration succeeds
- [ ] No errors about missing dependencies
- [ ] Makefiles generated
- [ ] `USE_JAVA_FFI=ON` confirmed in output

**Deliverable**: Configured build directory

---

#### Task 2B.3: Build TVM libraries
**Priority**: P0
**Estimated Time**: 40 minutes
**Condition**: Continuation of Path 2B

**Steps**:
```bash
# 1. Run build (use all CPU cores)
cd ~/Coding/ava/external/mlc-llm/android/mlc4j/build
make -j$(sysctl -n hw.ncpu)

# 2. Monitor build progress
# Expected: 30-40 minutes

# 3. Check for build errors
# If errors occur, save build log for troubleshooting

# 4. Verify build outputs
ls -lh lib/arm64-v8a/

# Expected files:
# - libtvm_runtime.so
# - libtvm4j.so
# - libtvm_ffi.so (THIS IS THE KEY FILE)
```

**Success Criteria**:
- [ ] Build completes without errors
- [ ] All 3 .so files present in lib/arm64-v8a/
- [ ] `libtvm_ffi.so` exists
- [ ] Files are ARM64-v8a ELF

**Deliverable**: Built TVM libraries

---

#### Task 2B.4: Copy rebuilt libraries to project
**Priority**: P0
**Estimated Time**: 15 minutes
**Condition**: Continuation of Path 2B

**Steps**:
```bash
# 1. Create backup of existing libraries (if any)
mkdir -p /tmp/ava-tvm-backup
cp /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm*.so \
   /tmp/ava-tvm-backup/ 2>/dev/null || true

# 2. Copy all rebuilt TVM libraries
cp ~/Coding/ava/external/mlc-llm/android/mlc4j/build/lib/arm64-v8a/libtvm*.so \
   /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/

# 3. Copy libc++_shared.so from NDK
cp ~/Library/Android/sdk/ndk/26.1.10909125/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so \
   /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/

# 4. Set executable permissions
chmod +x /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/*.so

# 5. Verify all files
ls -lh /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/*.so
```

**Success Criteria**:
- [ ] All TVM libraries copied
- [ ] libc++_shared.so copied
- [ ] Permissions set correctly
- [ ] Backup created (for rollback if needed)

**Deliverable**: All libraries in jniLibs directory

---

#### Task 2B.5: Verify rebuilt libraries
**Priority**: P0
**Estimated Time**: 15 minutes
**Condition**: Continuation of Path 2B

**Steps**:
```bash
# 1. Check file sizes
ls -lh /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm*.so

# 2. Verify architecture
for lib in /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm*.so; do
  echo "=== $(basename $lib) ==="
  file "$lib"
done

# 3. Check symbols in libtvm_ffi.so
nm -D /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_ffi.so 2>/dev/null | grep -E "TVMFFIFunctionCall" | head -5

# 4. Verify dependencies resolve
otool -L /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_runtime.so 2>/dev/null | grep ffi

# Expected: libtvm_ffi.so listed as dependency
```

**Success Criteria**:
- [ ] All libraries are ARM64-v8a
- [ ] `libtvm_ffi.so` contains FFI symbols
- [ ] `libtvm_runtime.so` references `libtvm_ffi.so`
- [ ] No missing dependencies

**Deliverable**: Verified rebuilt libraries

---

## Phase 3: Build Configuration & Testing

**Duration**: 2 hours
**Dependencies**: Phase 2 complete

### Task 3.1: Update app build.gradle.kts
**Priority**: P0
**Estimated Time**: 15 minutes

**File**: `/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/build.gradle.kts`

**Steps**:
1. Open file in editor
2. Locate `packaging` block (around line 80-84)
3. Add jniLibs packaging rules

**Changes**:
```kotlin
packaging {
    resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // Add this block:
    jniLibs {
        // Handle duplicate libraries from dependencies
        pickFirsts.add("**/libc++_shared.so")
        pickFirsts.add("**/libtvm_runtime.so")
        pickFirsts.add("**/libtvm4j.so")
        pickFirsts.add("**/libtvm_ffi.so")
    }
}
```

**Success Criteria**:
- [ ] File modified correctly
- [ ] Syntax valid (no Gradle errors)
- [ ] Comments added for clarity

**Deliverable**: Updated build.gradle.kts

---

### Task 3.2: Verify LLM module build.gradle.kts
**Priority**: P1
**Estimated Time**: 10 minutes

**File**: `/Volumes/M-Drive/Coding/AVA/Universal/AVA/Features/LLM/build.gradle.kts`

**Steps**:
1. Open file and check lines 36-50
2. Verify NO `sourceSets.jniLibs` configuration exists
3. Verify comment indicates libraries are in app module

**Current Expected State**:
```kotlin
// Configure native library loading
// Native .so files are in apps/ava-app-android/src/main/jniLibs/
// This module only contains tvm4j_core.jar for Java FFI bindings

packaging {
    jniLibs {
        pickFirsts.add("**/libc++_shared.so")
    }
}
```

**Success Criteria**:
- [ ] No conflicting jniLibs configuration
- [ ] Libraries correctly delegated to app module
- [ ] Comment accurately reflects current setup

**Deliverable**: LLM module config verified

---

### Task 3.3: Configure Git LFS tracking
**Priority**: P1
**Estimated Time**: 10 minutes

**Steps**:
```bash
# 1. Navigate to project root
cd /Volumes/M-Drive/Coding/AVA

# 2. Check if Git LFS is installed
git lfs version
# If not: brew install git-lfs && git lfs install

# 3. Check current .gitattributes
cat .gitattributes | grep jniLibs

# 4. Add pattern for jniLibs if not present
echo "apps/ava-app-android/src/main/jniLibs/**/*.so filter=lfs diff=lfs merge=lfs -text" >> .gitattributes

# 5. Track the new .so files
git lfs track "apps/ava-app-android/src/main/jniLibs/**/*.so"

# 6. Verify tracking
git lfs ls-files | grep jniLibs
```

**Success Criteria**:
- [ ] Git LFS installed
- [ ] .gitattributes updated
- [ ] .so files tracked by LFS
- [ ] LFS status verified

**Deliverable**: Git LFS configured for native libraries

---

### Task 3.4: Clean build
**Priority**: P0
**Estimated Time**: 5 minutes

**Steps**:
```bash
cd /Volumes/M-Drive/Coding/AVA

# 1. Clean previous build artifacts
./gradlew clean

# 2. Verify clean
# Expected: "BUILD SUCCESSFUL"

# 3. Check build cache cleared
ls -la apps/ava-app-android/build/  # Should be minimal
```

**Success Criteria**:
- [ ] Clean completes successfully
- [ ] Build directory cleared

**Deliverable**: Clean workspace

---

### Task 3.5: Build debug APK
**Priority**: P0
**Estimated Time**: 30 minutes

**Steps**:
```bash
# 1. Start build with timing
cd /Volumes/M-Drive/Coding/AVA
time ./gradlew assembleDebug --console=plain 2>&1 | tee build.log

# 2. Monitor for errors
# Watch for "library not found" warnings
# Expected: BUILD SUCCESSFUL

# 3. Check build time
# Target: <2 minutes

# 4. Locate APK
ls -lh apps/ava-app-android/build/outputs/apk/debug/*.apk
```

**Success Criteria**:
- [ ] Build completes without errors
- [ ] Build time <2 minutes
- [ ] No library loading warnings
- [ ] APK file generated

**Deliverable**: Built debug APK

---

### Task 3.6: Verify APK contents and size
**Priority**: P0
**Estimated Time**: 15 minutes

**Steps**:
```bash
# 1. Check APK size
ls -lh apps/ava-app-android/build/outputs/apk/debug/*.apk
# Target: ≤145MB (current 134MB + 10MB budget)

# 2. List native libraries in APK
unzip -l apps/ava-app-android/build/outputs/apk/debug/ava-app-android-debug.apk | grep "lib/arm64-v8a"

# 3. Verify expected libraries present
expected_libs=(
  "libc++_shared.so"
  "libtvm_ffi.so"
  "libtvm_runtime.so"
  "libtvm4j.so"
  "libonnxruntime.so"
  "libonnxruntime4j_jni.so"
)

for lib in "${expected_libs[@]}"; do
  if unzip -l apps/ava-app-android/build/outputs/apk/debug/ava-app-android-debug.apk | grep -q "$lib"; then
    echo "✅ $lib found in APK"
  else
    echo "❌ $lib MISSING from APK"
  fi
done

# 4. Check library sizes in APK (stripped versions)
unzip -l apps/ava-app-android/build/outputs/apk/debug/ava-app-android-debug.apk | grep "lib/arm64-v8a" | grep ".so"
```

**Success Criteria**:
- [ ] APK size ≤145MB
- [ ] All 6 expected libraries present
- [ ] Libraries are stripped (smaller sizes)
- [ ] No duplicate libraries

**Deliverable**: APK verified

---

### Task 3.7: Deploy and test on physical device
**Priority**: P0
**Estimated Time**: 30 minutes

**Steps**:
```bash
# 1. Connect physical device
adb devices
# Expected: Device listed

# 2. Uninstall old version (clean slate)
adb uninstall com.augmentalis.ava.debug

# 3. Install new APK
adb install apps/ava-app-android/build/outputs/apk/debug/ava-app-android-debug.apk

# 4. Clear logcat
adb logcat -c

# 5. Launch app and monitor logs
adb shell am start -n com.augmentalis.ava.debug/com.augmentalis.ava.MainActivity &
adb logcat | grep -E "(TVM|LLM|UnsatisfiedLink|libtvm|FATAL)" | tee device-test.log

# 6. Watch for specific log messages
# ✅ Expected: "TVM native runtime loaded successfully"
# ✅ Expected: "LLMResponseGenerator initialized successfully"
# ❌ NOT Expected: "UnsatisfiedLinkError"
# ❌ NOT Expected: "library not found"

# 7. Let app run for 2 minutes to ensure stable
sleep 120

# 8. Check for crashes
adb shell dumpsys activity com.augmentalis.ava.debug | grep -i "crash\|error"
```

**Success Criteria**:
- [ ] App installs successfully
- [ ] App launches without crash
- [ ] TVM runtime loads (log message present)
- [ ] LLM initializes (log message present)
- [ ] No `UnsatisfiedLinkError` in logs
- [ ] App stable for 2+ minutes

**Deliverable**: Device test passed

---

## Phase 4: Documentation

**Duration**: 1 hour
**Dependencies**: Phase 3 complete
**Parallelizable**: Partial (can draft during build/test)

### Task 4.1: Create native-library-dependencies.md
**Priority**: P0
**Estimated Time**: 20 minutes

**File**: `/Volumes/M-Drive/Coding/AVA/docs/build/native-library-dependencies.md`

**Content Template**:
```markdown
# AVA Native Library Dependencies

**Version**: 1.0
**Last Updated**: 2025-11-27
**Platform**: Android arm64-v8a

## Overview
Complete list of native (.so) libraries required for AVA functionality.

## TVM v0.22.0 Runtime Libraries

| Library | Size | Source | Purpose | Version |
|---------|------|--------|---------|---------|
| libtvm_runtime.so | 62MB (4.4MB stripped) | TVM v0.22.0 | Core runtime | v0.22.0 |
| libtvm4j.so | 124KB (95KB stripped) | TVM v0.22.0 | JNI bridge | v0.22.0 |
| libtvm_ffi.so | {SIZE} | TVM v0.22.0 | FFI layer | v0.22.0 |
| libc++_shared.so | 1.7MB | Android NDK 26.1.10909125 | C++ stdlib | NDK r26 |

## ONNX Runtime Libraries
{list from current setup}

## Build Instructions
{detailed steps from this task list}

## Troubleshooting
{common issues and solutions}
```

**Steps**:
1. Create `/docs/build/` directory if needed
2. Create file with template above
3. Fill in actual sizes from Task 3.6 results
4. Add build instructions from Tasks 2A/2B
5. Add troubleshooting section

**Success Criteria**:
- [ ] File created with complete information
- [ ] All library sizes documented
- [ ] Build instructions tested and accurate
- [ ] Troubleshooting section useful

**Deliverable**: Complete dependency documentation

---

### Task 4.2: Create bert-llm-compilation-guide.md
**Priority**: P0
**Estimated Time**: 25 minutes

**File**: `/Volumes/M-Drive/Coding/AVA/docs/build/bert-llm-compilation-guide.md`

**Content Outline**:
1. Prerequisites (NDK, CMake, Python, Rust)
2. BERT Model Compilation (PyTorch → ONNX → INT8)
3. LLM Model Compilation (TVM/MLC → .ALM)
4. TVM v0.22.0 Build Process
5. Common Issues & Solutions
6. Version Matrix (what works with what)

**Steps**:
1. Create comprehensive guide based on Phase 2 experience
2. Document exact NDK version requirements
3. Include CMake configuration flags
4. Add troubleshooting for common build errors
5. Document path requirements (no spaces!)

**Success Criteria**:
- [ ] Complete guide for BERT compilation
- [ ] Complete guide for LLM compilation
- [ ] TVM build documented (from Task 2B if executed)
- [ ] Prerequisites clearly listed
- [ ] Troubleshooting section comprehensive

**Deliverable**: BERT & LLM compilation guide

---

### Task 4.3: Update Developer-Manual-Chapter42.md
**Priority**: P1
**Estimated Time**: 10 minutes

**File**: `/Volumes/M-Drive/Coding/AVA/docs/Developer-Manual-Chapter42-LLM-Model-Setup.md`

**Changes**:
Add new section after "## LLM Model Structure":

```markdown
## Native Library Dependencies

AVA's LLM functionality requires several native libraries for TVM v0.22.0 FFI API support.

### Required Libraries

- **TVM v0.22.0 Runtime** (3 libraries):
  - `libtvm_runtime.so` - Core TVM runtime
  - `libtvm4j.so` - JNI bridge for Java/Kotlin
  - `libtvm_ffi.so` - FFI layer for function calls

- **Android NDK Libraries**:
  - `libc++_shared.so` - C++ standard library

### Build Requirements

For complete dependency information, build instructions, and troubleshooting:
- [Native Library Dependencies](../build/native-library-dependencies.md)
- [BERT & LLM Compilation Guide](../build/bert-llm-compilation-guide.md)

### Troubleshooting Library Loading

If you encounter `UnsatisfiedLinkError` when running AVA:

1. **Verify libraries in APK**:
   ```bash
   unzip -l app-debug.apk | grep "lib/arm64-v8a"
   ```

2. **Check device logs**:
   ```bash
   adb logcat | grep -E "(TVM|UnsatisfiedLink)"
   ```

3. **Common issues**:
   - Missing `libtvm_ffi.so` - See rebuild instructions
   - Missing `libc++_shared.so` - Copy from Android NDK
   - Wrong architecture - Verify arm64-v8a

See [Native Library Dependencies](../build/native-library-dependencies.md) for detailed solutions.
```

**Success Criteria**:
- [ ] Section added in appropriate location
- [ ] Links to new docs working
- [ ] Troubleshooting steps actionable
- [ ] Formatting consistent with rest of manual

**Deliverable**: Updated developer manual

---

### Task 4.4: Update CHANGELOG.md
**Priority**: P1
**Estimated Time**: 5 minutes

**File**: `/Volumes/M-Drive/Coding/AVA/CHANGELOG.md`

**Entry to Add**:
```markdown
## [Unreleased]

### Fixed
- Fixed LLM loading failure due to missing TVM v0.22.0 native library dependencies
  - Added `libtvm_ffi.so` (FFI layer for TVM function calls)
  - Added `libc++_shared.so` (Android NDK C++ standard library)
  - Updated build configuration to package all dependencies
  - See [Native Library Dependencies](docs/build/native-library-dependencies.md)

### Added
- Comprehensive BERT and LLM compilation documentation
  - [BERT & LLM Compilation Guide](docs/build/bert-llm-compilation-guide.md)
  - [Native Library Dependencies](docs/build/native-library-dependencies.md)
- Git LFS tracking for native libraries in jniLibs directory
```

**Success Criteria**:
- [ ] Entry added under `[Unreleased]`
- [ ] Links working
- [ ] Format consistent with existing entries

**Deliverable**: Updated CHANGELOG

---

### Task 4.5: Create task completion report
**Priority**: P2
**Estimated Time**: 5 minutes (auto-generated from TodoWrite)

**Steps**:
1. Review all completed tasks
2. Document any deviations from plan
3. Note actual time vs estimated time
4. List any issues encountered and solutions

**Template**:
```markdown
# TVM v0.22.0 Dependency Fix - Completion Report

**Date**: 2025-11-27
**Status**: ✅ Complete

## Summary
- Total tasks: 21
- Completed: {count}
- Path taken: {2A or 2B}
- Total time: {actual} vs {estimated 6h}

## Deviations from Plan
{any changes made during execution}

## Issues Encountered
{problems and solutions}

## Verification
- [ ] All quality gates passed
- [ ] Device test successful
- [ ] Documentation complete
- [ ] APK size within budget

## Files Changed
{git status summary}
```

**Deliverable**: Completion report

---

## Quality Gate Checklist

Before marking implementation complete, verify:

### Build Quality
- [ ] `./gradlew assembleDebug` succeeds
- [ ] Build time <2 minutes
- [ ] Zero library loading warnings
- [ ] APK size ≤145MB

### Runtime Quality
- [ ] App installs on device
- [ ] App launches without crash
- [ ] TVM runtime loads successfully
- [ ] LLM initializes successfully
- [ ] No `UnsatisfiedLinkError` in logcat

### Code Quality
- [ ] Gradle configs follow best practices
- [ ] Git LFS configured correctly
- [ ] No hardcoded paths
- [ ] Comments explain key decisions

### Documentation Quality
- [ ] native-library-dependencies.md complete
- [ ] bert-llm-compilation-guide.md complete
- [ ] Developer Manual updated
- [ ] CHANGELOG.md updated
- [ ] All links working

---

## Next Steps After Task Completion

1. **Commit Changes**:
   ```bash
   git add .gitattributes
   git add apps/ava-app-android/build.gradle.kts
   git add apps/ava-app-android/src/main/jniLibs/
   git add docs/build/
   git add docs/Developer-Manual-Chapter42-LLM-Model-Setup.md
   git add CHANGELOG.md

   git commit -m "fix(llm): add missing TVM v0.22.0 native library dependencies

Fixes LLM loading failure (UnsatisfiedLinkError) by adding missing dependencies:
- libtvm_ffi.so (TVM FFI layer)
- libc++_shared.so (Android NDK C++ stdlib)

Updated build configuration for proper library packaging.
Added comprehensive BERT & LLM compilation documentation.

Build verified: APK size 134MB→142MB (+8MB)
Device tested: TVM runtime and LLM initialization successful

Refs: #TVM-v0.22.0-fix"
   ```

2. **Push to Remote** (if applicable):
   ```bash
   git push origin development
   ```

3. **Archive Specification Documents**:
   - Move spec/plan/tasks to `/docs/archive/tvm-v0220-fix/`
   - Keep build guides in `/docs/build/` (living documentation)

---

**Ready to implement!** Use TodoWrite to track progress through each task.
