# NLU & LLM Compilation Guide

**Author:** Manoj Jhawar
**Version:** 1.0
**Updated:** 2025-12-03

## Overview

This guide covers the compilation of TVM v0.22.0 for AVA's on-device LLM inference capabilities.

## Prerequisites

### Software Requirements

| Component | Version | Purpose |
|-----------|---------|---------|
| Android NDK | 25.2.9519653 | Native compilation toolchain |
| CMake | 3.22+ | Build system |
| Java | 17 | JAR compilation (must NOT use Java 24) |
| Python | 3.10+ | Build scripts |
| Git | 2.40+ | Submodule management |

### Environment Setup

```bash
# Set NDK path
export ANDROID_NDK=$HOME/Library/Android/sdk/ndk/25.2.9519653

# Set Java 17 (critical - Java 24 will cause DEX errors)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

# Verify
java -version  # Should show 17.x
```

## Step 1: Initialize Submodules

```bash
cd external-models/mlc-llm

# Initialize all submodules
git submodule update --init --recursive

# Critical: xgrammar submodule
git submodule update --init --recursive 3rdparty/xgrammar
```

## Step 2: Create Build Directory

```bash
cd android/mlc4j
mkdir -p build
cd build
```

## Step 3: Configure CMake

```bash
cmake .. -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_NATIVE_API_LEVEL=android-24 \
  -DANDROID_PLATFORM=android-24 \
  -DANDROID_STL=c++_shared \
  -DUSE_HEXAGON_SDK=OFF \
  -DMLC_LLM_INSTALL_STATIC_LIB=ON \
  -DUSE_OPENCL=ON \
  -DUSE_CUSTOM_LOGGING=ON \
  -DTVM_FFI_USE_LIBBACKTRACE=OFF \
  -DTVM_FFI_BACKTRACE_ON_SEGFAULT=OFF \
  -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
  -DCMAKE_CXX_STANDARD=17 \
  -DCMAKE_CXX_STANDARD_REQUIRED=ON
```

### Important CMake Flags

| Flag | Value | Purpose |
|------|-------|---------|
| `ANDROID_STL` | `c++_shared` | Required for C++17 features |
| `TVM_FFI_USE_LIBBACKTRACE` | `OFF` | Avoids missing function errors |
| `CMAKE_CXX_STANDARD` | `17` | Required for `std::optional`, etc. |
| `USE_OPENCL` | `ON` | GPU acceleration support |

## Step 4: Create Stub Library (if needed)

If CMake complains about missing `libmodel_android.a`:

```bash
# Create stub library
echo 'void __dummy_model_android() {}' > /tmp/dummy.c
$ANDROID_NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android24-clang \
  -c /tmp/dummy.c -o /tmp/dummy.o
llvm-ar rcs lib/libmodel_android.a /tmp/dummy.o
```

## Step 5: Build

```bash
cmake --build . --target tvm4j_runtime_packed --config release -j8
```

Build time: ~15-20 minutes on Apple Silicon.

## Step 6: Build Java JAR

```bash
cd external-models/mlc-llm/3rdparty/tvm/jvm/core

# Compile with Java 17
mkdir -p build/classes
find src/main/java -name "*.java" > sources.txt
javac -d build/classes -source 17 -target 17 @sources.txt

# Create JAR
cd build/classes
jar cf tvm4j_core.jar org/
```

## Step 7: Install Libraries

```bash
# Copy native library
cp build/lib/libtvm4j_runtime_packed.so \
   android/ava/src/main/jniLibs/arm64-v8a/

# Copy C++ stdlib (if not present)
cp $ANDROID_NDK/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/libc++_shared.so \
   android/ava/src/main/jniLibs/arm64-v8a/

# Copy JAR
cp external-models/mlc-llm/3rdparty/tvm/jvm/core/build/classes/tvm4j_core.jar \
   common/LLM/libs/

# Backup
mkdir -p external-models/tvm-v0220-binaries
cp android/ava/src/main/jniLibs/arm64-v8a/libtvm4j_runtime_packed.so \
   external-models/tvm-v0220-binaries/
cp common/LLM/libs/tvm4j_core.jar \
   external-models/tvm-v0220-binaries/
```

## Step 8: Update Code

In `TVMRuntime.kt`, load the packed library:

```kotlin
// Load TVM v0.22.0 packed runtime with MLC-LLM
System.loadLibrary("tvm4j_runtime_packed")
```

## Common Build Errors

### Error: `tvm_ffi_add_apple_dsymutil` not found

**Cause:** TVM CMakeLists calls function before definition.
**Fix:** Add `-DTVM_FFI_USE_LIBBACKTRACE=OFF`

### Error: CMake policy version < 3.5

**Cause:** msgpack uses old CMake version requirement.
**Fix:** Add `-DCMAKE_POLICY_VERSION_MINIMUM=3.5`

### Error: `xgrammar/xgrammar.h` not found

**Cause:** Missing submodule.
**Fix:** `git submodule update --init --recursive 3rdparty/xgrammar`

### Error: `no template named 'optional' in namespace 'std'`

**Cause:** C++17 not enabled or using wrong STL.
**Fix:** Use `-DANDROID_STL=c++_shared` and `-DCMAKE_CXX_STANDARD=17`

### Error: Unsupported class file major version 68

**Cause:** JAR compiled with Java 24.
**Fix:** Rebuild JAR with Java 17.

## Output Verification

After successful build:

```bash
# Check library exists
ls -lh android/ava/src/main/jniLibs/arm64-v8a/libtvm4j_runtime_packed.so
# Should show ~104 MB

# Check JAR
file common/LLM/libs/tvm4j_core.jar
# Should show: Java archive data (JAR)

# Build APK and verify
./gradlew assembleDebug
unzip -l android/ava/build/outputs/apk/debug/ava-debug.apk | grep tvm
# Should show: lib/arm64-v8a/libtvm4j_runtime_packed.so
```

## Model Format

TVM-compiled models use the `.amm` (Ava Model MLC) format, which contains:

- `mlc-chat-config.json` - Model configuration
- `tokenizer.json` - HuggingFace tokenizer
- `*.bin` - Model weights

Models are stored on external storage at:
```
/sdcard/ava-ai-models/llm/MODEL_NAME/
```
