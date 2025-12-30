#!/bin/bash
# Build script to create .ads shared library with TVM compatibility shim
# Author: Manoj Jhawar
# Date: 2025-12-04

set -e

# Configuration
NDK_PATH="${ANDROID_NDK:-$HOME/Library/Android/sdk/ndk/25.2.9519653}"
TOOLCHAIN="$NDK_PATH/toolchains/llvm/prebuilt/darwin-x86_64"
CC="$TOOLCHAIN/bin/aarch64-linux-android24-clang"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SHIM_SRC="$SCRIPT_DIR/tvm_compat.c"

# Check arguments
if [ $# -lt 2 ]; then
    echo "Usage: $0 <model_dir> <output.ads>"
    echo "Example: $0 /path/to/AVA-GE2-2B16 /path/to/AVA-GE2-2B16.ads"
    exit 1
fi

MODEL_DIR="$1"
OUTPUT="$2"

# Find .adm files in model directory
ADM_FILES=$(find "$MODEL_DIR" -maxdepth 1 -name "*.adm" -type f)
if [ -z "$ADM_FILES" ]; then
    echo "Error: No .adm files found in $MODEL_DIR"
    exit 1
fi

echo "Building TVM compatibility shim..."
echo "  NDK: $NDK_PATH"
echo "  Model: $MODEL_DIR"
echo "  Output: $OUTPUT"

# Compile shim to object file
SHIM_OBJ="$SCRIPT_DIR/tvm_compat.o"
echo "Compiling tvm_compat.c..."
$CC -c -fPIC -O2 -o "$SHIM_OBJ" "$SHIM_SRC"

# Link everything into shared library
echo "Linking model with shim..."
echo "  ADM files: $ADM_FILES"

$CC -shared -fPIC -O2 \
    -o "$OUTPUT" \
    $ADM_FILES \
    "$SHIM_OBJ" \
    -llog -landroid

# Verify output
if [ -f "$OUTPUT" ]; then
    SIZE=$(ls -lh "$OUTPUT" | awk '{print $5}')
    echo "Success! Created $OUTPUT ($SIZE)"

    # Check undefined symbols
    UNDEFINED=$($TOOLCHAIN/bin/llvm-nm -u "$OUTPUT" 2>/dev/null | grep TVM || true)
    if [ -n "$UNDEFINED" ]; then
        echo "Warning: Remaining undefined TVM symbols:"
        echo "$UNDEFINED"
    else
        echo "All TVM symbols resolved."
    fi
else
    echo "Error: Failed to create $OUTPUT"
    exit 1
fi
