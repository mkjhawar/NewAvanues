#!/bin/bash
# Download ONNX models from HuggingFace for AI Server
# Models: MobileBERT (NLU), MiniLM (Embeddings)

set -e

MODEL_DIR="$HOME/.augmentalis/models"
echo "Model directory: $MODEL_DIR"

# Create directories
mkdir -p "$MODEL_DIR/mobilebert"
mkdir -p "$MODEL_DIR/minilm"

# Check for Python
if ! command -v python3 &> /dev/null; then
    echo "Error: Python 3 is required"
    exit 1
fi

# Create virtual environment if needed
VENV_DIR="$MODEL_DIR/.venv"
if [ ! -d "$VENV_DIR" ]; then
    echo "Creating virtual environment..."
    python3 -m venv "$VENV_DIR"
fi

# Activate and install dependencies
source "$VENV_DIR/bin/activate"
pip install --quiet --upgrade pip
pip install --quiet optimum onnxruntime transformers torch sentencepiece

echo ""
echo "=== Downloading MobileBERT (NLU) ==="
python3 << 'EOF'
from optimum.onnxruntime import ORTModelForSequenceClassification
from transformers import AutoTokenizer
import os

model_dir = os.path.expanduser("~/.augmentalis/models/mobilebert")
print(f"Downloading to: {model_dir}")

# Download and convert to ONNX
model = ORTModelForSequenceClassification.from_pretrained(
    "google/mobilebert-uncased",
    export=True
)
model.save_pretrained(model_dir)

# Download tokenizer
tokenizer = AutoTokenizer.from_pretrained("google/mobilebert-uncased")
tokenizer.save_pretrained(model_dir)

print("MobileBERT downloaded successfully!")
EOF

echo ""
echo "=== Downloading MiniLM (Embeddings) ==="
python3 << 'EOF'
from optimum.onnxruntime import ORTModelForFeatureExtraction
from transformers import AutoTokenizer
import os

model_dir = os.path.expanduser("~/.augmentalis/models/minilm")
print(f"Downloading to: {model_dir}")

# Download and convert to ONNX
model = ORTModelForFeatureExtraction.from_pretrained(
    "sentence-transformers/all-MiniLM-L6-v2",
    export=True
)
model.save_pretrained(model_dir)

# Download tokenizer
tokenizer = AutoTokenizer.from_pretrained("sentence-transformers/all-MiniLM-L6-v2")
tokenizer.save_pretrained(model_dir)

print("MiniLM downloaded successfully!")
EOF

deactivate

echo ""
echo "=== Model Download Complete ==="
echo ""
echo "Models installed:"
ls -lh "$MODEL_DIR/mobilebert/"*.onnx 2>/dev/null || echo "  MobileBERT: Not found"
ls -lh "$MODEL_DIR/minilm/"*.onnx 2>/dev/null || echo "  MiniLM: Not found"
echo ""
echo "To start the AI server:"
echo "  cd NewAvanues/services/ai-server"
echo "  ./gradlew run"
