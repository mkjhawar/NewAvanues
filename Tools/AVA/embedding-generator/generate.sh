#!/bin/bash
# AVA Intent Embedding Generator
# Run this script to generate pre-computed embeddings for bundling in APK
#
# Usage:
#   ./generate.sh              # Run with default paths
#   ./generate.sh --help       # Show help
#
# Prerequisites:
#   pip install -r requirements.txt

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Default paths
MODEL_PATH="$PROJECT_ROOT/android/ava/src/main/assets/models/AVA-384-Base-INT8.AON"
VOCAB_PATH="$PROJECT_ROOT/android/ava/src/main/assets/models/vocab.txt"
AVA_EXAMPLES_DIR="$PROJECT_ROOT/android/ava/src/main/assets/ava-examples"
AVA_CORE_DIR="$PROJECT_ROOT/.ava/core"
OUTPUT_SQL="$PROJECT_ROOT/common/core/Data/src/main/sqldelight/com/augmentalis/ava/core/data/db/PrecomputedEmbeddings.sq"
OUTPUT_AOT="$PROJECT_ROOT/android/ava/src/main/assets/embeddings/bundled_embeddings.aot"
MODEL_VERSION="AVA-384-Base-INT8"
LOCALE="en-US"

# Help message
if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "AVA Intent Embedding Generator"
    echo ""
    echo "Usage: ./generate.sh [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --model PATH       Path to ONNX/AON model (default: $MODEL_PATH)"
    echo "  --vocab PATH       Path to vocab.txt (default: $VOCAB_PATH)"
    echo "  --ava-dir PATH     Path to .ava files directory"
    echo "  --ava-core PATH    Path to core .ava directory"
    echo "  --output-sql PATH  Output SQLDelight file"
    echo "  --output-aot PATH  Output .aot backup file"
    echo "  --locale LOCALE    Locale for embeddings (default: en-US)"
    echo "  --help             Show this help message"
    echo ""
    echo "Example:"
    echo "  ./generate.sh --locale es-ES"
    exit 0
fi

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --model) MODEL_PATH="$2"; shift 2 ;;
        --vocab) VOCAB_PATH="$2"; shift 2 ;;
        --ava-dir) AVA_EXAMPLES_DIR="$2"; shift 2 ;;
        --ava-core) AVA_CORE_DIR="$2"; shift 2 ;;
        --output-sql) OUTPUT_SQL="$2"; shift 2 ;;
        --output-aot) OUTPUT_AOT="$2"; shift 2 ;;
        --locale) LOCALE="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# Check prerequisites
echo "============================================="
echo "AVA Intent Embedding Generator"
echo "============================================="
echo ""

# Check Python
if ! command -v python3 &> /dev/null; then
    echo "ERROR: python3 is required but not installed."
    exit 1
fi

# Check model file
if [[ ! -f "$MODEL_PATH" ]]; then
    echo "ERROR: Model file not found: $MODEL_PATH"
    echo "Please ensure the NLU model is in the assets directory."
    exit 1
fi

# Check vocab file
if [[ ! -f "$VOCAB_PATH" ]]; then
    echo "ERROR: Vocab file not found: $VOCAB_PATH"
    exit 1
fi

# Create output directories
mkdir -p "$(dirname "$OUTPUT_SQL")"
mkdir -p "$(dirname "$OUTPUT_AOT")"

# Install dependencies if needed
echo "Checking Python dependencies..."
pip3 install -q -r "$SCRIPT_DIR/requirements.txt"

echo ""
echo "Configuration:"
echo "  Model: $MODEL_PATH"
echo "  Vocab: $VOCAB_PATH"
echo "  AVA Examples: $AVA_EXAMPLES_DIR"
echo "  AVA Core: $AVA_CORE_DIR"
echo "  Output SQL: $OUTPUT_SQL"
echo "  Output AOT: $OUTPUT_AOT"
echo "  Locale: $LOCALE"
echo ""

# Run generator
python3 "$SCRIPT_DIR/generate_embeddings.py" \
    --model "$MODEL_PATH" \
    --vocab "$VOCAB_PATH" \
    --ava-dir "$AVA_EXAMPLES_DIR" \
    --ava-core-dir "$AVA_CORE_DIR" \
    --output-sql "$OUTPUT_SQL" \
    --output-aot "$OUTPUT_AOT" \
    --model-version "$MODEL_VERSION" \
    --locale "$LOCALE"

echo ""
echo "============================================="
echo "Generation complete!"
echo "============================================="
echo ""
echo "Next steps:"
echo "1. Verify $OUTPUT_SQL contains INSERT statements"
echo "2. Verify $OUTPUT_AOT exists as backup"
echo "3. Run: ./gradlew assembleDebug"
echo ""
