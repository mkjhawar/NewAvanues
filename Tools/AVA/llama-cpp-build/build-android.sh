#!/bin/bash
# Build llama.cpp for Android arm64-v8a
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
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
LLAMA_DIR="$SCRIPT_DIR/llama.cpp"
BUILD_DIR="$LLAMA_DIR/build-android"
NDK_HOME="${ANDROID_NDK:-$DEFAULT_SDK/ndk/25.2.9519653}"
OUTPUT_DIR="$PROJECT_ROOT/android/ava/src/main/jniLibs/arm64-v8a"

echo "=== Building llama.cpp for Android arm64-v8a ==="
echo "NDK: $NDK_HOME"
echo "Source: $LLAMA_DIR"
echo "Build: $BUILD_DIR"
echo "Output: $OUTPUT_DIR"

# Check NDK exists
if [ ! -d "$NDK_HOME" ]; then
    echo "ERROR: NDK not found at $NDK_HOME"
    exit 1
fi

# Clean and create build directory
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Configure with CMake
echo ""
echo "=== Configuring CMake ==="
cmake "$LLAMA_DIR" \
    -DCMAKE_TOOLCHAIN_FILE="$NDK_HOME/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-24 \
    -DBUILD_SHARED_LIBS=ON \
    -DGGML_OPENMP=OFF \
    -DLLAMA_CURL=OFF \
    -DLLAMA_BUILD_TESTS=OFF \
    -DLLAMA_BUILD_EXAMPLES=OFF \
    -DLLAMA_BUILD_SERVER=OFF \
    -DGGML_NATIVE=OFF \
    -DCMAKE_BUILD_TYPE=Release

# Build
echo ""
echo "=== Building ==="
cmake --build . --config Release -j8

# Copy output
echo ""
echo "=== Copying output ==="
mkdir -p "$OUTPUT_DIR"

# Find and copy the shared library
if [ -f "src/libllama.so" ]; then
    cp "src/libllama.so" "$OUTPUT_DIR/libllama-android.so"
    echo "Copied src/libllama.so -> $OUTPUT_DIR/libllama-android.so"
elif [ -f "libllama.so" ]; then
    cp "libllama.so" "$OUTPUT_DIR/libllama-android.so"
    echo "Copied libllama.so -> $OUTPUT_DIR/libllama-android.so"
else
    echo "Looking for libllama.so..."
    find . -name "*.so" -type f
fi

# Also copy ggml if separate
if [ -f "ggml/src/libggml.so" ]; then
    cp "ggml/src/libggml.so" "$OUTPUT_DIR/"
    echo "Copied libggml.so"
fi

echo ""
echo "=== Build complete ==="
ls -la "$OUTPUT_DIR"/*.so 2>/dev/null || echo "No .so files found in output dir"
