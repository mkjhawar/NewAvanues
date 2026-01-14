# AVA Native Library Dependencies

**Author:** Manoj Jhawar
**Version:** 1.0
**Updated:** 2025-12-03

## Overview

AVA includes several native libraries for on-device AI capabilities. This document details each library and its purpose.

## Native Libraries (arm64-v8a)

| Library | Size | Purpose | Source |
|---------|------|---------|--------|
| `libtvm4j_runtime_packed.so` | 104 MB | TVM v0.22.0 runtime with MLC-LLM | Built from MLC-LLM |
| `libonnxruntime.so` | 14.6 MB | ONNX model inference | Apache 2.0 |
| `libonnxruntime4j_jni.so` | 74 KB | ONNX JNI bindings | Apache 2.0 |
| `libtensorflowlite_jni.so` | 3.7 MB | TensorFlow Lite inference | Apache 2.0 |
| `libpv_porcupine.so` | 208 KB | Wake word detection | Picovoice (commercial) |
| `libc++_shared.so` | 1.3 MB | C++ standard library | Android NDK |

## TVM Runtime (libtvm4j_runtime_packed.so)

### Included Components

The packed TVM runtime includes:

1. **TVM Runtime** - Core computation engine
2. **TVM FFI** - Foreign function interface
3. **TVM4J JNI Bridge** - Java Native Interface bindings
4. **Tokenizers** - HuggingFace tokenizers via Rust FFI
5. **MLC-LLM** - Machine learning compilation for LLMs
6. **xgrammar** - Grammar parsing support

### Build Requirements

- Android NDK 25.2.9519653
- CMake 3.22+
- C++17 compiler (clang)
- Python 3.10+

### Build Command

```bash
cd external-models/mlc-llm/android/mlc4j/build

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

cmake --build . --target tvm4j_runtime_packed --config release -j8
```

### Output Files

| File | Location | Purpose |
|------|----------|---------|
| `libtvm4j_runtime_packed.so` | `android/ava/src/main/jniLibs/arm64-v8a/` | Native library |
| `tvm4j_core.jar` | `common/LLM/libs/` | Java FFI bindings |

### Java Version Requirements

The `tvm4j_core.jar` must be compiled with Java 17 (class file major version 61). Building with Java 24 (major version 68) will cause DEX compilation errors.

```bash
# Rebuild JAR with Java 17
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
cd external-models/mlc-llm/3rdparty/tvm/jvm/core
mkdir -p build/classes
find src/main/java -name "*.java" > sources.txt
javac -d build/classes -source 17 -target 17 @sources.txt
cd build/classes
jar cf tvm4j_core.jar org/
cp tvm4j_core.jar /path/to/common/LLM/libs/
```

## Backup Location

Pre-built binaries are stored in:
```
external-models/tvm-v0220-binaries/
├── libtvm4j_runtime_packed.so  (104 MB)
└── tvm4j_core.jar              (51 KB)
```

## Git LFS Configuration

Large native libraries are tracked with Git LFS:

```gitattributes
*.so filter=lfs diff=lfs merge=lfs -text
android/ava/src/main/jniLibs/**/*.so filter=lfs diff=lfs merge=lfs -text
```

## Troubleshooting

### "libtvm_ffi.so not found"

This error occurs when using the old separate library setup. Solution: Use the packed runtime `libtvm4j_runtime_packed.so` instead of separate `libtvm_runtime.so` and `libtvm4j.so`.

### "Unsupported class file major version 68"

The tvm4j_core.jar was compiled with Java 24. Rebuild with Java 17.

### APK size concerns

The TVM packed runtime adds ~13.7 MB to the APK (compressed). Consider using ProGuard/R8 for release builds and App Bundle for smaller downloads.
