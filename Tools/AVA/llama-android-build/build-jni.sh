#!/bin/bash
# Build JNI wrapper for llama.cpp
# Supports: macOS, Linux
#
# Prerequisites:
#   - build.sh has been run (llama.cpp cloned and built)
#   - Android NDK r25+
#
# Usage: ./build-jni.sh

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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LLAMA_DIR="$SCRIPT_DIR/llama.cpp"
BUILD_DIR="$SCRIPT_DIR/build-jni"
OUTPUT_DIR="$PROJECT_ROOT/android/ava/src/main/jniLibs/arm64-v8a"

# Find NDK
if [ -z "$ANDROID_NDK" ]; then
    if [ -z "$NDK" ]; then
        # Try platform-specific default locations
        if [ -d "$DEFAULT_SDK/ndk" ]; then
            ANDROID_NDK=$(ls -d "$DEFAULT_SDK/ndk"/*/ 2>/dev/null | tail -1)
        elif [ -d "$ANDROID_HOME/ndk" ]; then
            ANDROID_NDK=$(ls -d "$ANDROID_HOME/ndk"/*/ 2>/dev/null | tail -1)
        fi
    else
        ANDROID_NDK="$NDK"
    fi
fi

if [ -z "$ANDROID_NDK" ] || [ ! -d "$ANDROID_NDK" ]; then
    echo "ERROR: Android NDK not found. Set ANDROID_NDK environment variable."
    exit 1
fi

echo "Using NDK: $ANDROID_NDK"

# Check prerequisites
if [ ! -d "$LLAMA_DIR" ]; then
    echo "ERROR: llama.cpp not found. Run build.sh first."
    exit 1
fi

if [ ! -f "$OUTPUT_DIR/libllama-android.so" ]; then
    echo "ERROR: libllama-android.so not found. Run build.sh first."
    exit 1
fi

# Set up toolchain
TOOLCHAIN="$ANDROID_NDK/toolchains/llvm/prebuilt/$PLATFORM"

CC="$TOOLCHAIN/bin/aarch64-linux-android28-clang"
CXX="$TOOLCHAIN/bin/aarch64-linux-android28-clang++"

if [ ! -f "$CXX" ]; then
    echo "ERROR: Clang not found at $CXX"
    exit 1
fi

# Create build directory
mkdir -p "$BUILD_DIR"

# Compile JNI wrapper
echo "Compiling llama_jni.cpp..."
$CXX -shared -fPIC -O3 \
    -I"$LLAMA_DIR" \
    -I"$LLAMA_DIR/include" \
    -I"$LLAMA_DIR/ggml/include" \
    -L"$OUTPUT_DIR" \
    -lllama-android \
    -llog \
    -o "$BUILD_DIR/libllama-jni.so" \
    "$SCRIPT_DIR/llama_jni.cpp"

# Copy to jniLibs
echo "Copying libllama-jni.so..."
cp -v "$BUILD_DIR/libllama-jni.so" "$OUTPUT_DIR/"

echo ""
echo "JNI build complete!"
echo "Output: $OUTPUT_DIR/libllama-jni.so"
echo ""
echo "Libraries in jniLibs/arm64-v8a:"
ls -la "$OUTPUT_DIR"/*.so
