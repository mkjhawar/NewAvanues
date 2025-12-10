# TVM v0.22.0 Native Library Dependencies Fix - Implementation Plan

**Version**: 1.0
**Status**: Ready for Implementation
**Author**: AVA AI Team
**Created**: 2025-11-27
**Based On**: [tvm-v0220-dependency-fix-spec.md](./tvm-v0220-dependency-fix-spec.md)
**Compliance**: AVA v3, IDEACODE v9.0

---

## Executive Summary

This plan implements Option 1 from the specification: adding missing native library dependencies (`libtvm_ffi.so`, `libc++_shared.so`) to enable TVM v0.22.0 FFI API functionality. Total effort: 6 hours across 4 phases.

---

## Phase 1: Investigation & Dependency Location

**Duration**: 1 hour
**Dependencies**: None (blocking phase)
**Parallelizable**: No

### Objectives
1. Locate `libc++_shared.so` in Android NDK
2. Locate or determine how to obtain `libtvm_ffi.so`
3. Verify library compatibility (ARM64-v8a, API level 28+)

### Tasks

#### Task 1.1: Locate libc++_shared.so from Android NDK
```bash
# Check NDK location
echo $ANDROID_SDK_ROOT

# Find libc++_shared.so for arm64-v8a
find ~/Library/Android/sdk/ndk -name "libc++_shared.so" -path "*/aarch64-linux-android/*"

# Expected: ~/Library/Android/sdk/ndk/26.1.10909125/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so
```

**Success Criteria**:
- [ ] `libc++_shared.so` found
- [ ] File size ~1.7MB
- [ ] ARM64-v8a architecture confirmed

#### Task 1.2: Investigate libtvm_ffi.so availability
```bash
# Option A: Check if it exists in project history
find /Volumes/M-Drive/Coding/AVA -name "*tvm*ffi*.so" 2>/dev/null

# Option B: Check external/mlc-llm build artifacts
find /Volumes/M-Drive/Coding/AVA/external -name "*.so" | grep ffi

# Option C: Check TVM v0.22.0 build outputs
ls -la ~/Coding/ava/external/mlc-llm/android/mlc4j/build/lib/arm64-v8a/
```

**Outcomes**:
- **If found**: Note location, verify ARM64-v8a
- **If not found**: Proceed to rebuild task

#### Task 1.3: Analyze libtvm_runtime.so dependencies
```bash
# Use nm to check if FFI symbols are undefined (external dependency)
nm -D apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_runtime.so | grep -i ffi

# Expected: Undefined symbols like "TVMFFIFunctionCall"
```

**Outcome**: Confirm whether `libtvm_ffi.so` is actually needed or if FFI is statically linked

---

## Phase 2: Obtain Missing Libraries

**Duration**: 2 hours
**Dependencies**: Phase 1 complete
**Parallelizable**: No (investigation determines approach)

### Scenario A: libtvm_ffi.so Found (1 hour)

#### Task 2A.1: Copy libraries to project
```bash
# Create jniLibs directory if needed
mkdir -p /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/

# Copy libc++_shared.so from NDK
cp ~/Library/Android/sdk/ndk/26.1.10909125/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so \
   /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/

# Copy libtvm_ffi.so from found location
cp {found_location}/libtvm_ffi.so \
   /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/
```

#### Task 2A.2: Verify library integrity
```bash
# Check file types
file apps/ava-app-android/src/main/jniLibs/arm64-v8a/*.so

# Verify ARM64
# Expected: ELF 64-bit LSB shared object, ARM aarch64

# Check library sizes
ls -lh apps/ava-app-android/src/main/jniLibs/arm64-v8a/
```

### Scenario B: libtvm_ffi.so NOT Found - Rebuild Required (2 hours)

#### Task 2B.1: Setup TVM build environment
```bash
# Navigate to MLC-LLM source
cd ~/Coding/ava/external/mlc-llm/

# Check NDK version
echo $ANDROID_NDK
# Expected: ~/Library/Android/sdk/ndk/26.1.10909125

# Verify CMake
cmake --version
# Expected: 3.22+
```

#### Task 2B.2: Rebuild TVM v0.22.0 runtime
```bash
cd ~/Coding/ava/external/mlc-llm/android/mlc4j/

# Clean previous build
rm -rf build/
mkdir build && cd build

# Configure with FFI enabled
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-28 \
  -DUSE_JAVA_FFI=ON \
  -DBUILD_STATIC=OFF

# Build
make -j$(sysctl -n hw.ncpu)

# Check outputs
ls -lh build/lib/arm64-v8a/
# Expected: libtvm_ffi.so, libtvm_runtime.so, libtvm4j.so
```

#### Task 2B.3: Copy rebuilt libraries
```bash
# Copy all TVM libraries (replace existing)
cp build/lib/arm64-v8a/libtvm*.so \
   /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/

# Copy libc++_shared.so
cp ~/Library/Android/sdk/ndk/26.1.10909125/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so \
   /Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/jniLibs/arm64-v8a/
```

---

## Phase 3: Build Configuration & Testing

**Duration**: 2 hours
**Dependencies**: Phase 2 complete
**Parallelizable**: No

### Task 3.1: Update Gradle Build Configuration

#### File: `apps/ava-app-android/build.gradle.kts`

```kotlin
android {
    // ... existing config ...

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }

        jniLibs {
            // Handle duplicate libraries from dependencies
            pickFirsts.add("**/libc++_shared.so")
            pickFirsts.add("**/libtvm_runtime.so")
            pickFirsts.add("**/libtvm4j.so")
            pickFirsts.add("**/libtvm_ffi.so")
        }
    }
}
```

**Rationale**: Prevents conflicts if multiple modules provide the same library

#### File: `Universal/AVA/Features/LLM/build.gradle.kts`

```kotlin
// Ensure jniLibs config is removed (libraries now in app module)
android {
    // Remove any sourceSets.jniLibs configuration
    // Libraries are in apps/ava-app-android/src/main/jniLibs/
}
```

### Task 3.2: Add Git LFS Tracking

```bash
# Track large native libraries with Git LFS
cd /Volumes/M-Drive/Coding/AVA

# Add patterns to .gitattributes
echo "apps/ava-app-android/src/main/jniLibs/**/*.so filter=lfs diff=lfs merge=lfs -text" >> .gitattributes

# Track existing files
git add .gitattributes
git lfs track "apps/ava-app-android/src/main/jniLibs/**/*.so"
```

### Task 3.3: Build APK

```bash
cd /Volumes/M-Drive/Coding/AVA

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Verify success
# Expected: BUILD SUCCESSFUL
```

**Success Criteria**:
- [ ] Build completes without errors
- [ ] Build time <2 minutes
- [ ] No "library not found" warnings

### Task 3.4: Verify APK Size

```bash
# Check APK size
ls -lh apps/ava-app-android/build/outputs/apk/debug/*.apk

# Extract and check native libs
unzip -l apps/ava-app-android/build/outputs/apk/debug/ava-app-android-debug.apk | grep "lib/arm64-v8a"

# Expected libraries:
# - libc++_shared.so (~1.7MB)
# - libtvm_runtime.so (~62MB or ~4.4MB stripped)
# - libtvm4j.so (~124KB or ~95KB stripped)
# - libtvm_ffi.so (size TBD)
# - libonnxruntime.so (existing, ~14.6MB)
# - Other existing libraries
```

**Acceptance Criteria**:
- [ ] APK size ≤145MB (current 134MB + 10MB budget)
- [ ] All required .so files present in APK

### Task 3.5: Device Testing

```bash
# Install APK on device
adb install -r apps/ava-app-android/build/outputs/apk/debug/ava-app-android-debug.apk

# Monitor logs
adb logcat -c  # Clear logs
adb logcat | grep -E "(TVM|LLM|UnsatisfiedLink)"

# Expected logs:
# "TVM native runtime loaded successfully"
# "LLMResponseGenerator initialized successfully"

# NOT expected:
# "UnsatisfiedLinkError"
# "library not found"
```

**Success Criteria**:
- [ ] App launches without crash
- [ ] No `UnsatisfiedLinkError` in logs
- [ ] TVM runtime loads successfully
- [ ] LLM initialization succeeds

---

## Phase 4: Documentation

**Duration**: 1 hour
**Dependencies**: Phase 3 complete
**Parallelizable**: Partial (can run during device testing)

### Task 4.1: Create Native Library Dependencies Document

**File**: `/docs/build/native-library-dependencies.md`

**Contents**:
```markdown
# AVA Native Library Dependencies

## TVM v0.22.0 Runtime Libraries

| Library | Size | Source | Purpose |
|---------|------|--------|---------|
| libtvm_runtime.so | 62MB (4.4MB stripped) | TVM v0.22.0 build | Core TVM runtime |
| libtvm4j.so | 124KB (95KB stripped) | TVM v0.22.0 build | JNI bridge for org.apache.tvm |
| libtvm_ffi.so | TBD | TVM v0.22.0 build | FFI layer for function calls |
| libc++_shared.so | 1.7MB | Android NDK 26.1.10909125 | C++ standard library |

## ONNX Runtime Libraries

| Library | Size | Source | Purpose |
|---------|------|--------|---------|
| libonnxruntime.so | 14.6MB | ONNX Runtime 1.15+ | NLU/RAG inference |
| libonnxruntime4j_jni.so | 74KB | ONNX Runtime 1.15+ | JNI bridge |

## Build Instructions

### Obtaining Libraries

{detailed instructions from this plan}

### Rebuild Instructions

{CMake commands and configuration}
```

### Task 4.2: Create BERT & LLM Compilation Guide

**File**: `/docs/build/bert-llm-compilation-guide.md`

**Contents**:
```markdown
# BERT and LLM Model Compilation Guide

## Prerequisites

### Software Requirements
- Android NDK 26.1.10909125 or later
- CMake 3.22 or later
- Python 3.10+
- Rust 1.70+ (for tokenizers)

### Hardware Requirements
- 16GB+ RAM
- 50GB+ free disk space
- macOS/Linux (Windows via WSL2)

## BERT Model Compilation (ONNX)

### 1. Export from PyTorch
{instructions}

### 2. Quantization to INT8
{instructions}

### 3. Optimization
{instructions}

## LLM Model Compilation (TVM/MLC)

### 1. Environment Setup
{TVM build environment}

### 2. Model Compilation
{MLC-LLM compilation steps}

### 3. Packaging to .ALM Format
{ALM creation process}

## Common Issues

### Build Failures
{troubleshooting}

### Path Issues
{no spaces in paths}

### Library Loading Errors
{dependency resolution}
```

### Task 4.3: Update Developer Manual Chapter 42

**File**: `/docs/Developer-Manual-Chapter42-LLM-Model-Setup.md`

**Add Section**:
```markdown
## Native Library Dependencies

AVA's LLM functionality requires several native libraries:

- **TVM v0.22.0 Runtime**: Core inference engine
- **Android NDK Libraries**: C++ standard library support

For complete dependency list and build instructions, see:
- [Native Library Dependencies](../build/native-library-dependencies.md)
- [BERT & LLM Compilation Guide](../build/bert-llm-compilation-guide.md)

### Troubleshooting Library Loading

If you encounter `UnsatisfiedLinkError`:
1. Verify all .so files are in `apps/ava-app-android/src/main/jniLibs/arm64-v8a/`
2. Check APK contents: `unzip -l app-debug.apk | grep "lib/arm64-v8a"`
3. Review logcat for specific missing library
```

---

## Quality Gates

### Build Quality
- [ ] `./gradlew assembleDebug` succeeds
- [ ] Build time <2 minutes
- [ ] Zero warnings about missing libraries
- [ ] APK size ≤145MB

### Runtime Quality
- [ ] App launches without crash
- [ ] TVM runtime loads: `grep "TVM.*loaded" logcat`
- [ ] LLM initializes: `grep "LLM.*initialized" logcat`
- [ ] No UnsatisfiedLinkError in logs

### Code Quality
- [ ] Gradle configuration follows best practices
- [ ] Git LFS configured for .so files
- [ ] No hardcoded paths

### Documentation Quality
- [ ] All three documents created
- [ ] Build instructions tested and verified
- [ ] Troubleshooting section complete

---

## Risk Mitigation

| Risk | Mitigation | Contingency |
|------|------------|-------------|
| `libtvm_ffi.so` rebuild fails | Use known-good NDK version, follow exact build steps | Statically link FFI into `libtvm_runtime.so` |
| APK size exceeds budget | Use stripped libraries, verify ProGuard rules | Accept 145MB limit, document requirement |
| Library version mismatch | Lock NDK version, document exact versions | Rebuild all libraries with same NDK |
| Build time regression | Optimize Gradle caching, parallel builds | Accept 2-minute build time |

---

## Success Metrics

### Primary Metrics
1. **LLM Loading Success Rate**: 100% (was 0%)
2. **APK Size**: ≤145MB (current 134MB)
3. **Build Time**: <2 minutes

### Secondary Metrics
1. **Documentation Completeness**: 3/3 docs created
2. **Code Review Approval**: All changes reviewed
3. **Test Coverage**: Build + device test passed

---

## Timeline

| Phase | Start | End | Duration | Dependencies |
|-------|-------|-----|----------|--------------|
| Phase 1: Investigation | T+0h | T+1h | 1h | None |
| Phase 2: Obtain Libraries | T+1h | T+3h | 2h | Phase 1 |
| Phase 3: Build & Test | T+3h | T+5h | 2h | Phase 2 |
| Phase 4: Documentation | T+5h | T+6h | 1h | Phase 3 |
| **Total** | **T+0h** | **T+6h** | **6h** | |

---

## Rollback Plan

If implementation fails:

1. **Revert Code Changes**
   ```bash
   git reset --hard HEAD~1
   ```

2. **Remove Added Libraries**
   ```bash
   rm apps/ava-app-android/src/main/jniLibs/arm64-v8a/libc++_shared.so
   rm apps/ava-app-android/src/main/jniLibs/arm64-v8a/libtvm_ffi.so
   ```

3. **Rebuild Clean**
   ```bash
   ./gradlew clean assembleDebug
   ```

4. **Restore Packed Library** (if emergency fallback needed)
   ```bash
   git checkout bed469fe^ -- Universal/AVA/Features/LLM/libs/arm64-v8a/libtvm4j_runtime_packed.so
   # Update build.gradle.kts and TVMRuntime.kt as per previous commit
   ```

---

## Next Steps

1. **Review This Plan**: Confirm approach with team
2. **Execute Phase 1**: Begin investigation
3. **Create Task Breakdown**: Generate `tasks.md` from this plan
4. **Implement**: Execute all phases
5. **Test & Validate**: Verify all success criteria
6. **Document**: Complete all documentation tasks
7. **Commit**: Push changes to `development` branch

---

**Status**: Ready for `/tasks` command to generate task breakdown
