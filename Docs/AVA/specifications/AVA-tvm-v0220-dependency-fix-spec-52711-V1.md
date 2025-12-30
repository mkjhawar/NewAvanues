# TVM v0.22.0 Native Library Dependencies Fix - Specification

**Version**: 1.0
**Status**: Draft
**Author**: AVA AI Team
**Created**: 2025-11-27
**Last Updated**: 2025-11-27
**Compliance**: AVA v3, IDEACODE v9.0

---

## Executive Summary

Fix LLM loading failure caused by missing native library dependencies (`libtvm_ffi.so`, `libc++_shared.so`) for TVM v0.22.0 FFI API. The current separate library approach (`libtvm_runtime.so` + `libtvm4j.so`) has unresolved dependencies. Solution must maintain TVM v0.22.0 FFI API compatibility as all LLM models are compiled for this version.

---

## Problem Statement

### Current State
- TVM v0.22.0 split libraries in `/apps/ava-app-android/src/main/jniLibs/arm64-v8a/`:
  - `libtvm_runtime.so` (62MB) - requires `libtvm_ffi.so` ❌ (MISSING)
  - `libtvm4j.so` (124KB) - requires `libc++_shared.so` ❌ (MISSING)

### Impact
- **Severity**: P0 (Blocker)
- **User Impact**: LLM feature completely non-functional
- **Error**: `UnsatisfiedLinkError: dlopen failed: library "libtvm_ffi.so" not found`
- **Affected**: All AVA users attempting to use LLM features

### Root Cause
TVM v0.22.0 libraries were added (commit `0b0a2db1`) as separate binaries but their runtime dependencies were not included in the build.

---

## Requirements

### Functional Requirements

**FR1**: LLM must load successfully on device startup
- TVM runtime must initialize without `UnsatisfiedLinkError`
- All native library dependencies must resolve correctly

**FR2**: Maintain TVM v0.22.0 FFI API compatibility
- Must use new FFI API (`TVMFFIFunctionCall`)
- All compiled LLM models (.ALM format) depend on v0.22.0 API
- Cannot downgrade to older TVM API

**FR3**: Support GPU acceleration backends
- Vulkan (Adreno 5xx+, modern GPUs)
- OpenCL (Adreno 4xx+, legacy GPUs)
- CPU fallback (ARM NEON SIMD)

### Non-Functional Requirements

**NFR1**: Build Performance
- APK build time: <2 minutes (current baseline)
- No significant increase in build complexity

**NFR2**: APK Size
- Target: Minimize APK size increase
- Acceptable: +10MB max (compressed)
- Critical: +31MB is too large (reject packed library approach)

**NFR3**: Maintainability
- Clear documentation of all native library dependencies
- Reproducible build process
- Version-controlled build scripts

---

## Constraints

1. **TVM v0.22.0 FFI API**: MANDATORY - cannot use older API
2. **Android NDK**: Must use available NDK version (26.1.10909125)
3. **ARM64-v8a only**: Current ABI filter configuration
4. **Git LFS**: Large binaries must use LFS for version control
5. **Zero downtime**: Fix must not break existing NLU functionality

---

## Solution Options

### Option 1: Add Missing Dependencies from Android NDK/TVM Build ✅ RECOMMENDED

**Approach:**
1. Obtain `libc++_shared.so` from Android NDK sysroot
2. Rebuild TVM v0.22.0 with `libtvm_ffi.so` or locate in build artifacts
3. Add both to `apps/ava-app-android/src/main/jniLibs/arm64-v8a/`
4. Update Gradle packaging rules

**Pros:**
- ✅ Maintains TVM v0.22.0 FFI API
- ✅ Minimal APK size (+2-5MB)
- ✅ Modular approach
- ✅ Standard Android NDK practice

**Cons:**
- ❌ Requires locating/rebuilding `libtvm_ffi.so`
- ❌ More complex dependency management

**Estimated Effort:** 4-6 hours

---

### Option 2: Statically Link Dependencies (Rebuild TVM)

**Approach:**
1. Rebuild TVM v0.22.0 with static linking flags
2. Create self-contained `libtvm_runtime.so` and `libtvm4j.so`
3. Replace existing libraries

**Pros:**
- ✅ Self-contained libraries
- ✅ No external dependencies
- ✅ Maintains TVM v0.22.0 FFI API

**Cons:**
- ❌ Requires TVM build environment setup
- ❌ Complex build process (30-60 minutes)
- ❌ Larger library files
- ❌ Path issues (no spaces in build path)

**Estimated Effort:** 8-12 hours

---

### Option 3: Use Packed Library (REJECTED)

**Approach:**
Restore `libtvm4j_runtime_packed.so` from commit `7a2c0e2e`

**Rejection Reason:**
- ❌ Uses OLD TVM API, not v0.22.0 FFI
- ❌ +31MB APK size (unacceptable)
- ❌ Incompatible with compiled .ALM models

---

## Selected Solution: Option 1

### Implementation Steps

1. **Locate/Obtain Dependencies**
   - `libc++_shared.so`: Copy from Android NDK
   - `libtvm_ffi.so`: Check TVM build artifacts or rebuild

2. **Add to Project**
   - Place in `apps/ava-app-android/src/main/jniLibs/arm64-v8a/`
   - Add to Git LFS tracking

3. **Update Build Configuration**
   - Update `apps/ava-app-android/build.gradle.kts`
   - Add packaging rules for dependency conflicts

4. **Document Dependencies**
   - Create `docs/build/native-library-dependencies.md`
   - Document BERT and LLM compilation requirements
   - Include rebuild instructions

5. **Test**
   - Build APK
   - Test on physical device
   - Verify LLM loading succeeds

---

## Success Criteria

### Testing

**TC1**: Build succeeds without errors
```bash
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL
```

**TC2**: Libraries load on device
```bash
adb logcat | grep "TVM"
# Expected: "TVM packed runtime loaded successfully"
```

**TC3**: LLM initialization succeeds
```kotlin
// ChatViewModel.kt initializeLLM()
// Expected: No UnsatisfiedLinkError
```

**TC4**: APK size within budget
```bash
ls -lh apps/ava-app-android/build/outputs/apk/debug/*.apk
# Expected: <145MB (current + 10MB max)
```

### Quality Gates

- [ ] All tests pass
- [ ] No native library loading errors
- [ ] APK size ≤145MB
- [ ] Build time <2 minutes
- [ ] Documentation complete
- [ ] Git commit includes all dependencies

---

## Documentation Requirements

### Required Documents

**DOC1**: `/docs/build/native-library-dependencies.md`
- Complete list of all native libraries
- Source for each library (NDK, custom build, etc.)
- Version information
- Rebuild instructions

**DOC2**: `/docs/build/bert-llm-compilation-guide.md`
- BERT model compilation requirements
- LLM model compilation requirements
- TVM build environment setup
- NDK version requirements
- Common build issues and solutions

**DOC3**: Update `/docs/Developer-Manual-Chapter42-LLM-Model-Setup.md`
- Add native library dependency section
- Link to new build documentation

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| `libtvm_ffi.so` not available | Medium | High | Rebuild TVM v0.22.0 from source |
| Incompatible library versions | Low | High | Use exact NDK versions from original build |
| APK size exceeds budget | Low | Medium | Use stripped libraries, verify compression |
| Build time regression | Low | Low | Optimize Gradle caching |

---

## Dependencies

### External
- Android NDK 26.1.10909125
- TVM v0.22.0 source (if rebuild needed)
- CMake 3.22+ (if rebuild needed)

### Internal
- `apps/ava-app-android/build.gradle.kts`
- `Universal/AVA/Features/LLM/build.gradle.kts`
- `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/alc/TVMRuntime.kt`

---

## Timeline

| Phase | Duration | Tasks |
|-------|----------|-------|
| Investigation | 1 hour | Locate `libtvm_ffi.so`, verify NDK paths |
| Implementation | 2 hours | Copy libraries, update Gradle |
| Documentation | 2 hours | Create build docs, update developer manual |
| Testing | 1 hour | Build, deploy, test on device |
| **Total** | **6 hours** | |

---

## Acceptance Criteria

1. ✅ LLM loads without `UnsatisfiedLinkError`
2. ✅ TVM v0.22.0 FFI API functional
3. ✅ APK size increase ≤10MB
4. ✅ Build succeeds in <2 minutes
5. ✅ Documentation complete
6. ✅ All quality gates passed

---

## Related Documents

- [AVA File Formats](../standards/AVA-FILE-FORMATS.md)
- [AVA File Placement Guide](../AVA-File-Placement-Guide.md)
- [Developer Manual Chapter 42: LLM Model Setup](../Developer-Manual-Chapter42-LLM-Model-Setup.md)
- [TVM Samsung Integration](../TVM-Samsung-Integration.md)

---

## Changelog

### v1.0 (2025-11-27)
- Initial specification
- Problem statement and requirements defined
- Solution options evaluated
- Option 1 selected (Add missing dependencies)

---

**Next Steps**: Create implementation plan → Task breakdown → Implementation
