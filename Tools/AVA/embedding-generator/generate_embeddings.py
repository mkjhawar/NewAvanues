#!/usr/bin/env python3
"""
AVA Intent Embedding Generator

Generates pre-computed embeddings for bundling in APK:
1. SQLDelight migration SQL with INSERT statements
2. .aot backup file for DB corruption recovery

Usage:
    python generate_embeddings.py \
        --model ../../android/ava/src/main/assets/models/AVA-384-Base-INT8.AON \
        --vocab ../../android/ava/src/main/assets/models/vocab.txt \
        --ava-dir ../../android/ava/src/main/assets/ava-examples \
        --ava-core-dir ../../.ava/core \
        --output-sql ../../common/core/Data/src/main/sqldelight/com/augmentalis/ava/core/data/db/PrecomputedEmbeddings.sq \
        --output-aot ../../android/ava/src/main/assets/embeddings/bundled_embeddings.aot

Author: Manoj Jhawar
Created: 2025-12-02
"""

import argparse
import json
import struct
import hashlib
import time
import re
from pathlib import Path
from typing import Dict, List, Tuple, Optional
import numpy as np

try:
    import onnxruntime as ort
except ImportError:
    print("ERROR: onnxruntime not installed. Run: pip install onnxruntime")
    exit(1)


class BertTokenizer:
    """Simple WordPiece tokenizer for MobileBERT"""

    def __init__(self, vocab_path: str):
        self.vocab = {}
        self.inv_vocab = {}
        with open(vocab_path, 'r', encoding='utf-8') as f:
            for idx, line in enumerate(f):
                token = line.strip()
                self.vocab[token] = idx
                self.inv_vocab[idx] = token

        self.cls_token_id = self.vocab.get('[CLS]', 101)
        self.sep_token_id = self.vocab.get('[SEP]', 102)
        self.pad_token_id = self.vocab.get('[PAD]', 0)
        self.unk_token_id = self.vocab.get('[UNK]', 100)
        self.max_length = 128

    def tokenize(self, text: str) -> Dict[str, List[int]]:
        """Tokenize text using WordPiece"""
        # Basic preprocessing
        text = text.lower().strip()

        # Simple word tokenization
        words = text.split()

        # WordPiece tokenization
        tokens = []
        for word in words:
            if word in self.vocab:
                tokens.append(self.vocab[word])
            else:
                # Subword tokenization
                subwords = self._wordpiece(word)
                tokens.extend(subwords)

        # Truncate if needed
        max_tokens = self.max_length - 2  # Reserve for [CLS] and [SEP]
        tokens = tokens[:max_tokens]

        # Add special tokens
        input_ids = [self.cls_token_id] + tokens + [self.sep_token_id]

        # Padding
        attention_mask = [1] * len(input_ids)
        token_type_ids = [0] * len(input_ids)

        padding_length = self.max_length - len(input_ids)
        input_ids += [self.pad_token_id] * padding_length
        attention_mask += [0] * padding_length
        token_type_ids += [0] * padding_length

        return {
            'input_ids': input_ids,
            'attention_mask': attention_mask,
            'token_type_ids': token_type_ids
        }

    def _wordpiece(self, word: str) -> List[int]:
        """WordPiece subword tokenization"""
        tokens = []
        start = 0
        while start < len(word):
            end = len(word)
            cur_token = None
            while start < end:
                substr = word[start:end]
                if start > 0:
                    substr = '##' + substr
                if substr in self.vocab:
                    cur_token = self.vocab[substr]
                    break
                end -= 1

            if cur_token is None:
                tokens.append(self.unk_token_id)
                start += 1
            else:
                tokens.append(cur_token)
                start = end

        return tokens


class EmbeddingGenerator:
    """Generates embeddings using ONNX model"""

    def __init__(self, model_path: str, vocab_path: str):
        print(f"Loading model: {model_path}")
        self.session = ort.InferenceSession(model_path)
        self.tokenizer = BertTokenizer(vocab_path)

        # Get model info
        inputs = self.session.get_inputs()
        outputs = self.session.get_outputs()
        print(f"  Inputs: {[i.name for i in inputs]}")
        print(f"  Outputs: {[o.name for o in outputs]}")

        # Determine embedding dimension from output shape
        self.embedding_dim = outputs[0].shape[-1] if outputs[0].shape else 384
        print(f"  Embedding dimension: {self.embedding_dim}")

    def compute_embedding(self, text: str) -> np.ndarray:
        """Compute embedding for text"""
        tokens = self.tokenizer.tokenize(text)

        # Prepare inputs
        input_ids = np.array([tokens['input_ids']], dtype=np.int64)
        attention_mask = np.array([tokens['attention_mask']], dtype=np.int64)
        token_type_ids = np.array([tokens['token_type_ids']], dtype=np.int64)

        # Run inference
        outputs = self.session.run(
            None,
            {
                'input_ids': input_ids,
                'attention_mask': attention_mask,
                'token_type_ids': token_type_ids
            }
        )

        # Get last hidden state [1, seq_len, hidden_size]
        hidden_states = outputs[0]

        # Mean pooling with attention mask
        mask = attention_mask[0]
        seq_len = hidden_states.shape[1]
        hidden_size = hidden_states.shape[2]

        embedding = np.zeros(hidden_size, dtype=np.float32)
        token_count = 0

        for i in range(seq_len):
            if mask[i] == 1:
                embedding += hidden_states[0, i, :]
                token_count += 1

        if token_count > 0:
            embedding /= token_count

        # L2 normalize
        norm = np.linalg.norm(embedding)
        if norm > 0:
            embedding = embedding / norm

        return embedding

    def compute_intent_embedding(self, examples: List[str]) -> np.ndarray:
        """Compute average embedding for an intent from multiple examples"""
        embeddings = []
        for example in examples:
            try:
                emb = self.compute_embedding(example)
                embeddings.append(emb)
            except Exception as e:
                print(f"    Warning: Failed to embed '{example}': {e}")

        if not embeddings:
            return None

        # Average raw embeddings, then normalize
        avg_embedding = np.mean(embeddings, axis=0)
        norm = np.linalg.norm(avg_embedding)
        if norm > 0:
            avg_embedding = avg_embedding / norm

        return avg_embedding.astype(np.float32)


def parse_ava_yaml_format(content: str) -> Dict[str, List[str]]:
    """
    Parse YAML-style .anl format:
    TYPE:intent_id:example_text
    e.g., VCM:wifi_on:turn on wifi

    .anl file structure:
    ---
    (metadata: schema, version, locale)
    ---
    (intent data: TYPE:intent_id:example lines)
    ---
    (synonyms section, optional)
    """
    intents = {}
    delimiter_count = 0

    for line in content.split('\n'):
        line = line.strip()

        # Skip empty lines and comments
        if not line or line.startswith('#'):
            continue

        # Track section markers (count instead of toggle)
        if line == '---':
            delimiter_count += 1
            continue

        # Process intent data between 2nd and 3rd delimiter
        # delimiter_count == 2 means we've passed 2 delimiters (metadata section)
        # and are now in the intent data section
        if delimiter_count == 2 and ':' in line:
            parts = line.split(':')
            if len(parts) >= 3:
                # Format: TYPE:intent_id:example
                intent_type = parts[0].strip()
                intent_id = parts[1].strip()
                example = ':'.join(parts[2:]).strip()

                # Valid intent types: VCM, INFO, LEARN, JIT, SYS, etc.
                # Skip if it looks like metadata (lowercase type or contains space)
                if ' ' in intent_type:
                    continue

                # Only accept uppercase intent types
                if not intent_type.isupper():
                    continue

                if intent_id and example:
                    if intent_id not in intents:
                        intents[intent_id] = []
                    intents[intent_id].append(example)

    return intents


def parse_ava_json_format(content: str) -> Dict[str, List[str]]:
    """
    Parse JSON-style .anl format:
    {"i": [{"id": "intent_id", "s": ["example1", "example2"]}]}
    """
    intents = {}

    try:
        data = json.loads(content)

        # Handle the "i" array format
        if 'i' in data and isinstance(data['i'], list):
            for intent in data['i']:
                intent_id = intent.get('id', '')
                samples = intent.get('s', [])
                canonical = intent.get('c', '')

                if intent_id:
                    examples = []
                    if canonical:
                        examples.append(canonical)
                    examples.extend(samples)

                    if examples:
                        intents[intent_id] = examples

    except json.JSONDecodeError:
        pass

    return intents


def parse_vos_json_format(content: str) -> Dict[str, List[str]]:
    """
    Parse VoiceOS JSON format (vos-1.0)

    Format:
    {
      "schema": "vos-1.0",
      "commands": [
        { "action": "TURN_ON_WIFI", "cmd": "turn on wifi", "syn": ["wifi on", ...] }
      ]
    }
    """
    intents = {}

    try:
        data = json.loads(content)
        commands = data.get('commands', [])

        for cmd in commands:
            action = cmd.get('action', '')
            canonical = cmd.get('cmd', '')
            synonyms = cmd.get('syn', [])

            if action and canonical:
                # Convert ACTION_NAME to action_name (lowercase)
                intent_id = action.lower()
                examples = [canonical] + synonyms
                intents[intent_id] = examples

    except json.JSONDecodeError:
        pass

    return intents


def load_ava_files(ava_dirs: List[str]) -> Dict[str, List[str]]:
    """Load all intents from .anl and .vos files in specified directories"""
    all_intents = {}
    total_files = 0

    for ava_dir in ava_dirs:
        ava_path = Path(ava_dir)
        if not ava_path.exists():
            print(f"  Warning: Directory not found: {ava_dir}")
            continue

        # Find all .anl and .vos files recursively
        ava_files = list(ava_path.rglob('*.anl')) + list(ava_path.rglob('*.vos'))
        print(f"  Found {len(ava_files)} .anl/.vos files in {ava_dir}")

        for ava_file in ava_files:
            try:
                content = ava_file.read_text(encoding='utf-8')

                # Try different formats based on content
                intents = {}

                if content.strip().startswith('{'):
                    # JSON format - try ava-1.0 first, then vos-1.0
                    intents = parse_ava_json_format(content)
                    if not intents:
                        intents = parse_vos_json_format(content)
                else:
                    # Universal Format (YAML-style with VCM entries)
                    intents = parse_ava_yaml_format(content)

                if intents:
                    total_files += 1
                    for intent_id, examples in intents.items():
                        if intent_id in all_intents:
                            # Merge examples, avoid duplicates
                            existing = set(all_intents[intent_id])
                            for ex in examples:
                                if ex not in existing:
                                    all_intents[intent_id].append(ex)
                        else:
                            all_intents[intent_id] = examples

            except Exception as e:
                print(f"  Warning: Failed to parse {ava_file}: {e}")

    print(f"  Parsed {total_files} files, found {len(all_intents)} unique intents")
    return all_intents


def load_intents(intents_path: str) -> Dict[str, List[str]]:
    """Load intents from JSON file (legacy format)"""
    print(f"Loading intents: {intents_path}")
    with open(intents_path, 'r', encoding='utf-8') as f:
        intents = json.load(f)
    print(f"  Loaded {len(intents)} intents")
    return intents


def embedding_to_blob(embedding: np.ndarray) -> bytes:
    """Convert embedding to BLOB format (little-endian float32)"""
    return struct.pack(f'<{len(embedding)}f', *embedding)


def blob_to_hex(blob: bytes) -> str:
    """Convert bytes to SQLite hex literal"""
    return "X'" + blob.hex().upper() + "'"


def generate_sql(
    embeddings: Dict[str, Tuple[np.ndarray, int]],  # intent_id -> (embedding, example_count)
    model_version: str,
    output_path: str,
    locale: str = 'en-US'
):
    """Generate SQLDelight migration SQL file"""
    print(f"Generating SQL: {output_path}")

    current_time = int(time.time() * 1000)  # milliseconds

    lines = [
        "-- Pre-computed intent embeddings",
        "-- Generated by tools/embedding-generator/generate_embeddings.py",
        f"-- Model: {model_version}",
        f"-- Locale: {locale}",
        f"-- Generated: {time.strftime('%Y-%m-%d %H:%M:%S')}",
        f"-- Total embeddings: {len(embeddings)}",
        "",
        "-- This file is auto-generated. Do not edit manually.",
        "-- Regenerate with: python tools/embedding-generator/generate_embeddings.py",
        "",
        "-- Clear existing bundled embeddings for this locale before inserting",
        f"DELETE FROM intent_embedding WHERE source = 'BUNDLED_APK' AND locale = '{locale}';",
        "",
        "-- Insert pre-computed embeddings",
    ]

    for intent_id, (embedding, example_count) in embeddings.items():
        blob_hex = blob_to_hex(embedding_to_blob(embedding))
        dimension = len(embedding)

        sql = f"""INSERT OR REPLACE INTO intent_embedding (
    intent_id, locale, embedding_vector, embedding_dimension, model_version,
    normalization_type, ontology_id, created_at, updated_at, example_count, source
) VALUES (
    '{intent_id}', '{locale}', {blob_hex}, {dimension}, '{model_version}',
    'L2', NULL, {current_time}, {current_time}, {example_count}, 'BUNDLED_APK'
);"""
        lines.append(sql)
        lines.append("")

    # Write SQL file
    Path(output_path).parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print(f"  Generated {len(embeddings)} INSERT statements")


def generate_aot(
    embeddings: Dict[str, Tuple[np.ndarray, int]],
    model_version: str,
    output_path: str
):
    """Generate .aot backup file"""
    print(f"Generating AOT: {output_path}")

    # AOT format:
    # Header: magic (4 bytes) + version (4 bytes) + count (4 bytes) + dimension (4 bytes)
    # For each embedding:
    #   intent_id_len (4 bytes) + intent_id (utf-8) + embedding (float32 array)

    MAGIC = b'AOT\x00'
    VERSION = 1

    Path(output_path).parent.mkdir(parents=True, exist_ok=True)

    with open(output_path, 'wb') as f:
        # Header
        dimension = next(iter(embeddings.values()))[0].shape[0] if embeddings else 384
        f.write(MAGIC)
        f.write(struct.pack('<I', VERSION))
        f.write(struct.pack('<I', len(embeddings)))
        f.write(struct.pack('<I', dimension))

        # Model version (32 bytes, null-padded)
        model_bytes = model_version.encode('utf-8')[:32].ljust(32, b'\x00')
        f.write(model_bytes)

        # Embeddings
        for intent_id, (embedding, _) in embeddings.items():
            intent_bytes = intent_id.encode('utf-8')
            f.write(struct.pack('<I', len(intent_bytes)))
            f.write(intent_bytes)
            f.write(embedding.tobytes())

    file_size = Path(output_path).stat().st_size
    print(f"  Generated {len(embeddings)} embeddings ({file_size / 1024:.1f} KB)")


def main():
    parser = argparse.ArgumentParser(description='Generate pre-computed intent embeddings')
    parser.add_argument('--model', required=True, help='Path to ONNX/AON model')
    parser.add_argument('--vocab', required=True, help='Path to vocab.txt')
    parser.add_argument('--intents', help='Path to intents JSON file (legacy)')
    parser.add_argument('--ava-dir', action='append', dest='ava_dirs',
                        help='Directory containing .ava files (can specify multiple)')
    parser.add_argument('--ava-core-dir', help='Core .ava directory (e.g., .ava/core)')
    parser.add_argument('--output-sql', required=True, help='Output SQLDelight .sq file')
    parser.add_argument('--output-aot', required=True, help='Output .aot backup file')
    parser.add_argument('--model-version', default='AVA-384-Base-INT8',
                        help='Model version string')
    parser.add_argument('--locale', default='en-US', help='Locale for embeddings')

    args = parser.parse_args()

    print("=" * 60)
    print("AVA Intent Embedding Generator")
    print("=" * 60)

    # Load model
    generator = EmbeddingGenerator(args.model, args.vocab)

    # Load intents from .ava files or JSON
    intents = {}

    # Collect all .ava directories
    ava_dirs = []
    if args.ava_dirs:
        ava_dirs.extend(args.ava_dirs)
    if args.ava_core_dir:
        ava_dirs.append(args.ava_core_dir)

    if ava_dirs:
        print(f"\nLoading intents from .anl files...")
        intents = load_ava_files(ava_dirs)
    elif args.intents:
        intents = load_intents(args.intents)
    else:
        print("ERROR: Must specify either --ava-dir/--ava-core-dir or --intents")
        exit(1)

    if not intents:
        print("ERROR: No intents found!")
        exit(1)

    # Compute embeddings
    print(f"\nComputing embeddings for {len(intents)} intents...")
    embeddings = {}

    for i, (intent_id, examples) in enumerate(intents.items()):
        print(f"  [{i+1}/{len(intents)}] {intent_id} ({len(examples)} examples)")
        embedding = generator.compute_intent_embedding(examples)
        if embedding is not None:
            embeddings[intent_id] = (embedding, len(examples))
        else:
            print(f"    Warning: No embedding computed for {intent_id}")

    print(f"\nSuccessfully computed {len(embeddings)}/{len(intents)} embeddings")

    # Generate outputs
    print("\nGenerating outputs...")
    generate_sql(embeddings, args.model_version, args.output_sql, args.locale)
    generate_aot(embeddings, args.model_version, args.output_aot)

    print("\n" + "=" * 60)
    print("Done!")
    print("=" * 60)


if __name__ == '__main__':
    main()
