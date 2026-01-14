#!/bin/bash
# Build llama-android JNI wrapper for AVA
# Compiles llama-jni.cpp into libllama-android.so
# Supports: macOS, Linux

set -e

# Detect platform
detect_platform() {
    case "$(uname -s)" in
        Darwin*)
            PLATFORM="darwin-x86_64"
            DEFAULT_SDK="$HOME/Library/Android/sdk"
            ;;
        Linux*)
            PLATFORM="linux-x86_64"
            DEFAULT_SDK="$HOME/Android/Sdk"
            ;;
        *)
            echo "ERROR: Unsupported platform: $(uname -s)"
            echo "Supported: macOS (Darwin), Linux"
            exit 1
            ;;
    esac
}
detect_platform

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
LLAMA_DIR="$SCRIPT_DIR/../llama.cpp"
BUILD_DIR="$LLAMA_DIR/build-android"
NDK_HOME="${ANDROID_NDK:-$DEFAULT_SDK/ndk/25.2.9519653}"
OUTPUT_DIR="$PROJECT_ROOT/android/ava/src/main/jniLibs/arm64-v8a"

TOOLCHAIN="$NDK_HOME/toolchains/llvm/prebuilt/$PLATFORM"
CC="$TOOLCHAIN/bin/aarch64-linux-android24-clang"
CXX="$TOOLCHAIN/bin/aarch64-linux-android24-clang++"

echo "=== Building llama-android JNI wrapper ==="
echo "NDK: $NDK_HOME"
echo "llama.cpp: $LLAMA_DIR"

# Check dependencies
if [ ! -f "$BUILD_DIR/bin/libllama.so" ]; then
    echo "ERROR: libllama.so not found. Run build-android.sh first."
    exit 1
fi

# Compile JNI wrapper
echo ""
echo "=== Compiling llama-jni.cpp ==="

# Link shared libs and static common library
$CXX \
    -shared \
    -fPIC \
    -O2 \
    -DANDROID \
    -DNDEBUG \
    -I"$LLAMA_DIR/include" \
    -I"$LLAMA_DIR/common" \
    -I"$LLAMA_DIR/ggml/include" \
    -I"$LLAMA_DIR/src" \
    -I"$TOOLCHAIN/sysroot/usr/include" \
    "$SCRIPT_DIR/llama-jni.cpp" \
    -Wl,--whole-archive \
    "$BUILD_DIR/common/libcommon.a" \
    -Wl,--no-whole-archive \
    -L"$BUILD_DIR/bin" \
    -lllama \
    -lggml \
    -lggml-base \
    -lggml-cpu \
    -llog \
    -landroid \
    -lm \
    -Wl,-rpath,'$ORIGIN' \
    -o "$OUTPUT_DIR/libllama-android.so"

echo ""
echo "=== Build complete ==="
ls -la "$OUTPUT_DIR/libllama-android.so"
