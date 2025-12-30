#!/bin/bash
# llama.cpp Android Build Script
# Builds libllama-android.so for arm64-v8a
# Supports: macOS, Linux
#
# Prerequisites:
#   - Android NDK r25+ installed
#   - NDK path set in $ANDROID_NDK or $NDK (optional)
#   - CMake 3.18+
#
# Usage: ./build.sh [clean]

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
BUILD_DIR="$SCRIPT_DIR/build-android"
LLAMA_DIR="$SCRIPT_DIR/llama.cpp"
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
    echo "ERROR: Android NDK not found. Set ANDROID_NDK or NDK environment variable."
    exit 1
fi

echo "Using NDK: $ANDROID_NDK"

# Clean if requested
if [ "$1" = "clean" ]; then
    echo "Cleaning build directory..."
    rm -rf "$BUILD_DIR"
    exit 0
fi

# Clone llama.cpp if not present
if [ ! -d "$LLAMA_DIR" ]; then
    echo "Cloning llama.cpp..."
    git clone --depth 1 https://github.com/ggerganov/llama.cpp.git "$LLAMA_DIR"
fi

# Create build directory
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Configure with CMake
echo "Configuring CMake for Android arm64-v8a..."
cmake "$LLAMA_DIR" \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-28 \
    -DBUILD_SHARED_LIBS=ON \
    -DLLAMA_NATIVE=OFF \
    -DLLAMA_BUILD_TESTS=OFF \
    -DLLAMA_BUILD_EXAMPLES=OFF \
    -DLLAMA_BUILD_SERVER=OFF \
    -DCMAKE_BUILD_TYPE=Release

# Build
echo "Building llama.cpp..."
cmake --build . --config Release -j$(nproc 2>/dev/null || sysctl -n hw.ncpu)

# Copy output
echo "Copying libllama.so to jniLibs..."
mkdir -p "$OUTPUT_DIR"
cp -v "$BUILD_DIR/libllama.so" "$OUTPUT_DIR/libllama-android.so"

# Also copy ggml if separate
if [ -f "$BUILD_DIR/libggml.so" ]; then
    cp -v "$BUILD_DIR/libggml.so" "$OUTPUT_DIR/"
fi

echo ""
echo "Build complete!"
echo "Output: $OUTPUT_DIR/libllama-android.so"
echo ""
echo "Next steps:"
echo "1. Build the JNI wrapper: cd $SCRIPT_DIR && ./build-jni.sh"
echo "2. Run ./gradlew assembleDebug to verify"
