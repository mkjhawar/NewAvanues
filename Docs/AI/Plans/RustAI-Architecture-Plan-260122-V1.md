# RustAI: Cross-Platform AI Infrastructure Architecture Plan

**Version:** 1.0
**Date:** 2026-01-22
**Status:** Draft
**Author:** AI Architecture Team

---

## Executive Summary

This document presents a comprehensive plan to rebuild the `/Modules/AI/*` infrastructure using a hybrid **Rust + Kotlin Multiplatform (KMP)** architecture. The goal is to create a high-performance, cross-platform AI engine ("RustAI") that delivers native performance on Android, iOS, macOS, Linux, and Windows while maintaining development velocity through KMP for orchestration and UI layers.

### Key Benefits

| Benefit | Impact |
|---------|--------|
| **3-5x Performance** | Native Rust vs JVM overhead |
| **50-70% Smaller Binaries** | Compared to current TVM runtime (~104MB) |
| **True Cross-Platform** | Single codebase for 5+ platforms + WASM |
| **Memory Safety** | Zero-cost abstractions, no GC pauses |
| **Plugin Architecture** | Extensible for future AI capabilities |
| **Future-Proof** | Growing Rust AI ecosystem |

---

## Table of Contents

1. [Current State Analysis](#1-current-state-analysis)
2. [Technology Selection](#2-technology-selection)
3. [Architecture Design](#3-architecture-design)
4. [Module Specifications](#4-module-specifications)
5. [Cross-Platform Strategy](#5-cross-platform-strategy)
6. [Plugin System Design](#6-plugin-system-design)
7. [Migration Strategy](#7-migration-strategy)
8. [Use Cases & Examples](#8-use-cases--examples)
9. [Performance Targets](#9-performance-targets)
10. [Risk Assessment](#10-risk-assessment)
11. [Implementation Roadmap](#11-implementation-roadmap)
12. [Appendix: Research Sources](#appendix-research-sources)

---

## 1. Current State Analysis

### 1.1 Existing AI Modules

| Module | Purpose | Size (LOC) | Current Stack | Issues |
|--------|---------|------------|---------------|--------|
| **ALC** | LLM Orchestration | ~5K | Kotlin + TVM | TVM deprecating, JNI overhead |
| **LLM** | Response Generation | ~8K | Kotlin + TVM/GGUF | Migrating backends, complex |
| **NLU** | Intent Classification | ~6K | Kotlin + ONNX | Good, but ONNX via JNI |
| **RAG** | Vector Search | ~4K | Kotlin + Linear | O(n) search, needs optimization |
| **Chat** | Orchestration | ~3K | Kotlin + Compose | Keep as-is (UI layer) |
| **Memory** | Persistence | ~2K | Kotlin + SQLDelight | Keep as-is (storage layer) |
| **Teach** | User Learning | ~1K | Kotlin | Keep as-is (UI/logic) |

### 1.2 Pain Points

1. **JNI Overhead**: Every native call (TVM, ONNX) crosses JNI boundary
2. **Large Binaries**: TVM runtime is ~104MB, ONNX adds more
3. **Multiple Backends**: Managing TVM → GGUF migration is complex
4. **Platform Fragmentation**: Different solutions per platform
5. **No Plugin System**: Hard to add new AI capabilities
6. **Vector Search**: Linear O(n) doesn't scale

### 1.3 What Works Well (Keep)

- **Chat/UI Layer**: Compose-based UI works well
- **Memory/Persistence**: SQLDelight cross-platform DB is solid
- **Cloud API Integration**: Ktor-based cloud calls are fine
- **Orchestration Logic**: Business logic in Kotlin is maintainable

---

## 2. Technology Selection

### 2.1 Rust ML Framework Comparison

Based on comprehensive research of production-ready Rust ML frameworks:

| Framework | Best For | Stars | Maturity | WASM | Mobile |
|-----------|----------|-------|----------|------|--------|
| **Candle** | HuggingFace models, serverless | 17K+ | Production | Yes | Yes |
| **Burn** | Flexible training + inference | 10K+ | Growing | Yes | Yes |
| **Tract** | Pure Rust ONNX inference | 3K+ | Mature | Yes | Yes |
| **llama_cpp** | LLM inference (bindings) | - | Production | No | Yes |

#### Recommendation: **Hybrid Approach**

```
┌─────────────────────────────────────────────────────────────┐
│                    RustAI Core                               │
├─────────────────────────────────────────────────────────────┤
│  LLM Inference    │  NLU/Embeddings  │  Vector Search       │
│  ───────────────  │  ──────────────  │  ────────────        │
│  • candle (GPU)   │  • tract (ONNX)  │  • Custom HNSW       │
│  • llama_cpp (CPU)│  • candle (GPU)  │  • qdrant-core       │
│  ───────────────  │  ──────────────  │  ────────────        │
│       ↓                   ↓                  ↓               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │           HuggingFace Tokenizers (Rust)             │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Cross-Platform FFI: UniFFI

[UniFFI](https://github.com/mozilla/uniffi-rs) (Mozilla) is the industry standard for Rust → mobile bindings:

**Why UniFFI:**
- Used by Mozilla Firefox on Android/iOS in production
- Generates Kotlin, Swift, Python bindings automatically
- Handles memory management, error propagation
- Active maintenance, mature ecosystem
- New: React Native support (Dec 2024)

**UniFFI Process:**
```
Rust Code + UDL/Macros
         │
         ▼
    uniffi-bindgen
         │
    ┌────┴────┐
    ▼         ▼
  Kotlin    Swift
(Android)   (iOS)
```

### 2.3 Vector Database: Qdrant-Core

For RAG vector search, [Qdrant](https://qdrant.tech/) provides:

- **HNSW indexing**: O(log n) search vs current O(n)
- **Rust-native**: No FFI overhead
- **Embeddable**: Can run in-process (no server)
- **Filtering**: Payload-based filtering during search
- **Quantization**: Binary/scalar for memory efficiency

**Performance Comparison:**
| Operation | Current (Linear) | Qdrant (HNSW) |
|-----------|------------------|---------------|
| 10K vectors | ~100ms | ~1ms |
| 100K vectors | ~1000ms | ~5ms |
| 1M vectors | ~10s | ~10ms |

### 2.4 Tokenization: HuggingFace Tokenizers

[HuggingFace Tokenizers](https://github.com/huggingface/tokenizers) is the industry standard:

- **Rust-native**: Core is 100% Rust
- **10x faster**: Than Python implementations
- **WASM support**: Runs in browsers
- **All algorithms**: BPE, WordPiece, SentencePiece, Unigram
- **Used by**: PyTorch, TensorFlow, JAX ecosystems

### 2.5 LLM Inference Strategy

**Recommended: Dual Backend**

| Backend | Use Case | Model Format | Performance |
|---------|----------|--------------|-------------|
| **llama_cpp** (via llama_cpp_rs) | CPU inference, mobile | GGUF | Excellent |
| **candle** | GPU inference, cloud | SafeTensors | Excellent |

**Why not pure Rust LLM?**
- llama.cpp has 1000+ contributors, battle-tested
- Keeping up with llama.cpp's pace is impractical (per rustformers/llm maintainers)
- llama_cpp_rs provides safe Rust bindings with minimal overhead

---

## 3. Architecture Design

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        APPLICATION LAYER (KMP)                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Chat UI   │  │  Teach UI   │  │  Settings   │  │   Memory    │    │
│  │  (Compose)  │  │  (Compose)  │  │  (Compose)  │  │ (SQLDelight)│    │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │
├─────────────────────────────────────────────────────────────────────────┤
│                      ORCHESTRATION LAYER (KMP)                           │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                    AICoordinator (Kotlin)                        │    │
│  │  • Request routing  • Context management  • Cloud fallback       │    │
│  └─────────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────┤
│                         FFI BOUNDARY (UniFFI)                            │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │              uniffi-generated Kotlin/Swift bindings              │    │
│  └─────────────────────────────────────────────────────────────────┘    │
├─────────────────────────────────────────────────────────────────────────┤
│                        RUST AI CORE (rustai-core)                        │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐               │
│  │   LLMEngine   │  │  NLUEngine    │  │  RAGEngine    │               │
│  │  ───────────  │  │  ──────────   │  │  ──────────   │               │
│  │  • llama_cpp  │  │  • tract      │  │  • qdrant     │               │
│  │  • candle     │  │  • candle     │  │  • embeddings │               │
│  │  • streaming  │  │  • classify   │  │  • chunking   │               │
│  └───────────────┘  └───────────────┘  └───────────────┘               │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐               │
│  │  Tokenizer    │  │  VectorStore  │  │PluginManager  │               │
│  │  ───────────  │  │  ──────────   │  │  ──────────   │               │
│  │  • HF tokens  │  │  • HNSW index │  │  • dynload    │               │
│  │  • BPE/WP/SP  │  │  • filtering  │  │  • WASM       │               │
│  └───────────────┘  └───────────────┘  └───────────────┘               │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Crate Structure

```
rustai/
├── Cargo.toml                    # Workspace root
├── crates/
│   ├── rustai-core/              # Main library (UniFFI exports)
│   │   ├── src/
│   │   │   ├── lib.rs            # UniFFI entry point
│   │   │   ├── llm/              # LLM inference
│   │   │   ├── nlu/              # NLU classification
│   │   │   ├── rag/              # RAG pipeline
│   │   │   ├── tokenizer/        # Tokenization
│   │   │   ├── vector/           # Vector storage
│   │   │   └── plugin/           # Plugin system
│   │   └── uniffi.toml           # UniFFI configuration
│   │
│   ├── rustai-llm/               # LLM engine (internal)
│   │   ├── src/
│   │   │   ├── backends/
│   │   │   │   ├── llama_cpp.rs  # llama.cpp backend
│   │   │   │   ├── candle.rs     # Candle backend
│   │   │   │   └── mock.rs       # Testing backend
│   │   │   ├── streaming.rs      # Token streaming
│   │   │   ├── sampling.rs       # Sampling strategies
│   │   │   └── config.rs         # Model configuration
│   │   └── Cargo.toml
│   │
│   ├── rustai-nlu/               # NLU engine (internal)
│   │   ├── src/
│   │   │   ├── classifier.rs     # Intent classification
│   │   │   ├── entities.rs       # Entity extraction
│   │   │   ├── models/
│   │   │   │   ├── mobilebert.rs # MobileBERT via tract
│   │   │   │   └── albert.rs     # mALBERT via tract
│   │   │   └── languages.rs      # Language support
│   │   └── Cargo.toml
│   │
│   ├── rustai-rag/               # RAG engine (internal)
│   │   ├── src/
│   │   │   ├── chunker.rs        # Document chunking
│   │   │   ├── embedder.rs       # Embedding generation
│   │   │   ├── retriever.rs      # Semantic retrieval
│   │   │   └── parsers/          # Document parsers
│   │   │       ├── pdf.rs
│   │   │       ├── html.rs
│   │   │       └── docx.rs
│   │   └── Cargo.toml
│   │
│   ├── rustai-vector/            # Vector storage (internal)
│   │   ├── src/
│   │   │   ├── hnsw.rs           # HNSW implementation
│   │   │   ├── storage.rs        # Persistence
│   │   │   ├── filter.rs         # Payload filtering
│   │   │   └── quantize.rs       # Quantization
│   │   └── Cargo.toml
│   │
│   ├── rustai-tokenizer/         # Tokenization (internal)
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   ├── bpe.rs            # BPE tokenizer
│   │   │   ├── wordpiece.rs      # WordPiece
│   │   │   └── sentencepiece.rs  # SentencePiece
│   │   └── Cargo.toml
│   │
│   └── rustai-plugin/            # Plugin system (internal)
│       ├── src/
│       │   ├── manager.rs        # Plugin lifecycle
│       │   ├── loader.rs         # Dynamic loading
│       │   ├── wasm.rs           # WASM plugin support
│       │   └── api.rs            # Plugin API traits
│       └── Cargo.toml
│
├── bindings/
│   ├── kotlin/                   # Generated Kotlin bindings
│   ├── swift/                    # Generated Swift bindings
│   └── python/                   # Generated Python bindings
│
├── examples/
│   ├── cli/                      # CLI demo
│   └── wasm/                     # WASM demo
│
└── tests/
    ├── integration/              # Cross-crate tests
    └── benchmarks/               # Performance benchmarks
```

### 3.3 Data Flow

```
User Input (Voice/Text)
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                    KMP Orchestration Layer                   │
│                                                              │
│  1. Receive input                                            │
│  2. Call RustAI via UniFFI                                   │
│  3. Handle streaming response                                │
│  4. Update UI                                                │
└─────────────────────────────────────────────────────────────┘
         │
         │ UniFFI (zero-copy where possible)
         ▼
┌─────────────────────────────────────────────────────────────┐
│                      RustAI Core                             │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │                   Pipeline                            │   │
│  │                                                       │   │
│  │  Input ──▶ Tokenize ──▶ NLU Classify ──▶ Route       │   │
│  │                              │                        │   │
│  │              ┌───────────────┼───────────────┐       │   │
│  │              ▼               ▼               ▼       │   │
│  │          RAG Query      LLM Generate    Direct Action│   │
│  │              │               │               │       │   │
│  │              └───────────────┴───────────────┘       │   │
│  │                              │                        │   │
│  │                              ▼                        │   │
│  │                         Response                      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
         │
         │ Streaming callbacks
         ▼
    UI Update (Compose/SwiftUI)
```

---

## 4. Module Specifications

### 4.1 LLM Engine (rustai-llm)

#### API Design

```rust
/// LLM inference engine with multiple backends
pub struct LLMEngine {
    backend: Box<dyn LLMBackend>,
    config: LLMConfig,
    tokenizer: Arc<Tokenizer>,
}

/// Backend trait for pluggable inference
pub trait LLMBackend: Send + Sync {
    fn load_model(&mut self, path: &Path, config: &ModelConfig) -> Result<()>;
    fn generate(&self, request: &GenerateRequest) -> Result<GenerateResponse>;
    fn generate_stream(&self, request: &GenerateRequest) -> impl Stream<Item = Token>;
    fn unload(&mut self) -> Result<()>;
}

/// Generation request
#[derive(uniffi::Record)]
pub struct GenerateRequest {
    pub prompt: String,
    pub max_tokens: u32,
    pub temperature: f32,
    pub top_p: f32,
    pub top_k: u32,
    pub stop_sequences: Vec<String>,
    pub grammar: Option<String>,  // GBNF grammar constraint
}

/// Streaming token
#[derive(uniffi::Record)]
pub struct Token {
    pub text: String,
    pub logprob: f32,
    pub is_final: bool,
}
```

#### Backend Implementations

**llama_cpp Backend (Primary for mobile):**
```rust
pub struct LlamaCppBackend {
    model: Option<LlamaModel>,
    context: Option<LlamaContext>,
    params: LlamaParams,
}

impl LLMBackend for LlamaCppBackend {
    fn generate_stream(&self, request: &GenerateRequest)
        -> impl Stream<Item = Token>
    {
        // Uses llama_cpp_rs with:
        // - In-memory prompt caching
        // - GBNF grammar support
        // - INT8/INT4 quantization
    }
}
```

**Candle Backend (GPU acceleration):**
```rust
pub struct CandleBackend {
    model: Option<Box<dyn CandleModel>>,
    device: Device,  // CPU, CUDA, Metal
}

impl LLMBackend for CandleBackend {
    // GPU-accelerated inference
    // Supports: Llama, Mistral, Phi, Gemma
}
```

#### Quantization Support

| Format | Size Reduction | Quality | Mobile Suitability |
|--------|---------------|---------|-------------------|
| Q2_K | 75% | Fair | Low-end devices |
| Q4_K_M | 50% | Good | **Recommended** |
| Q5_K_M | 40% | Very Good | High-end devices |
| Q8_0 | 25% | Excellent | Tablets only |

### 4.2 NLU Engine (rustai-nlu)

#### API Design

```rust
/// NLU classification engine
pub struct NLUEngine {
    classifier: Box<dyn IntentClassifier>,
    entity_extractor: EntityExtractor,
    languages: Vec<Language>,
}

#[derive(uniffi::Record)]
pub struct ClassificationResult {
    pub intent: String,
    pub confidence: f32,
    pub entities: Vec<Entity>,
    pub language: String,
}

#[derive(uniffi::Record)]
pub struct Entity {
    pub entity_type: String,
    pub value: String,
    pub start: u32,
    pub end: u32,
    pub confidence: f32,
}

/// Intent classifier trait
pub trait IntentClassifier: Send + Sync {
    fn classify(&self, text: &str, language: &str) -> Result<ClassificationResult>;
    fn supported_languages(&self) -> Vec<Language>;
}
```

#### Model Strategy

**MobileBERT (English, Lightweight):**
```rust
pub struct MobileBERTClassifier {
    model: tract_onnx::Model,
    tokenizer: Tokenizer,
    labels: Vec<String>,
}
// 25MB, <50ms inference
```

**mALBERT (Multilingual):**
```rust
pub struct MALBERTClassifier {
    model: tract_onnx::Model,
    tokenizer: Tokenizer,  // SentencePiece
    labels: Vec<String>,
}
// 41MB, <80ms inference, 52 languages
```

### 4.3 RAG Engine (rustai-rag)

#### API Design

```rust
/// RAG pipeline for document retrieval
pub struct RAGEngine {
    embedder: Embedder,
    vector_store: VectorStore,
    chunker: Chunker,
}

#[derive(uniffi::Record)]
pub struct Document {
    pub id: String,
    pub content: String,
    pub metadata: HashMap<String, String>,
}

#[derive(uniffi::Record)]
pub struct SearchResult {
    pub document_id: String,
    pub chunk: String,
    pub score: f32,
    pub metadata: HashMap<String, String>,
}

impl RAGEngine {
    /// Index a document
    pub fn index(&mut self, doc: Document) -> Result<()>;

    /// Search for relevant chunks
    pub fn search(&self, query: &str, top_k: u32) -> Result<Vec<SearchResult>>;

    /// Search with metadata filter
    pub fn search_filtered(
        &self,
        query: &str,
        filter: Filter,
        top_k: u32
    ) -> Result<Vec<SearchResult>>;
}
```

#### Chunking Strategies

```rust
pub enum ChunkStrategy {
    /// Fixed token count with overlap
    Fixed { tokens: usize, overlap: usize },

    /// Semantic boundaries (sentences/paragraphs)
    Semantic { max_tokens: usize },

    /// Hybrid: semantic within fixed limits
    Hybrid { max_tokens: usize, min_tokens: usize },
}
```

#### Embedding Model

**all-MiniLM-L6-v2** (via tract):
- 384 dimensions
- 86MB (quantized to INT8: 22MB)
- ~10ms per embedding
- Excellent quality/size ratio

### 4.4 Vector Store (rustai-vector)

#### HNSW Implementation

```rust
/// High-performance vector index
pub struct VectorStore {
    index: HNSWIndex,
    storage: Storage,
    config: VectorConfig,
}

pub struct VectorConfig {
    pub dimensions: usize,
    pub m: usize,           // HNSW connections (default: 16)
    pub ef_construction: usize,  // Build quality (default: 100)
    pub ef_search: usize,   // Search quality (default: 50)
    pub quantization: Option<Quantization>,
}

pub enum Quantization {
    Scalar,   // 4x compression
    Binary,   // 32x compression, lower recall
    Product { segments: usize },  // Configurable
}
```

#### Performance Targets

| Vectors | Search Time | Memory (FP32) | Memory (Scalar) |
|---------|-------------|---------------|-----------------|
| 10K | <1ms | 15MB | 4MB |
| 100K | <5ms | 150MB | 40MB |
| 1M | <10ms | 1.5GB | 400MB |

### 4.5 Tokenizer (rustai-tokenizer)

#### Unified Tokenizer Interface

```rust
/// Tokenizer wrapper around HuggingFace tokenizers
pub struct Tokenizer {
    inner: tokenizers::Tokenizer,
    config: TokenizerConfig,
}

#[derive(uniffi::Record)]
pub struct TokenizeResult {
    pub ids: Vec<u32>,
    pub tokens: Vec<String>,
    pub offsets: Vec<(u32, u32)>,
}

impl Tokenizer {
    /// Load from HuggingFace model ID or local path
    pub fn from_pretrained(model_id: &str) -> Result<Self>;

    /// Tokenize text
    pub fn encode(&self, text: &str) -> Result<TokenizeResult>;

    /// Decode tokens back to text
    pub fn decode(&self, ids: &[u32]) -> Result<String>;

    /// Batch tokenization (optimized)
    pub fn encode_batch(&self, texts: &[&str]) -> Result<Vec<TokenizeResult>>;
}
```

---

## 5. Cross-Platform Strategy

### 5.1 Platform Targets

| Platform | Target Triple | Build Tool | FFI |
|----------|--------------|------------|-----|
| Android | aarch64-linux-android | cargo-ndk | UniFFI → Kotlin |
| Android (x86) | x86_64-linux-android | cargo-ndk | UniFFI → Kotlin |
| iOS | aarch64-apple-ios | cargo-lipo | UniFFI → Swift |
| iOS Simulator | aarch64-apple-ios-sim | cargo-lipo | UniFFI → Swift |
| macOS | aarch64-apple-darwin | cargo | UniFFI → Swift |
| macOS (Intel) | x86_64-apple-darwin | cargo | UniFFI → Swift |
| Linux | x86_64-unknown-linux-gnu | cargo | UniFFI → Kotlin |
| Linux (ARM) | aarch64-unknown-linux-gnu | cross | UniFFI → Kotlin |
| Windows | x86_64-pc-windows-msvc | cargo | UniFFI → Kotlin |
| **WASM** | wasm32-unknown-unknown | wasm-pack | wasm-bindgen |

### 5.2 Build Configuration

**Cargo.toml (workspace):**
```toml
[workspace]
members = [
    "crates/rustai-core",
    "crates/rustai-llm",
    "crates/rustai-nlu",
    "crates/rustai-rag",
    "crates/rustai-vector",
    "crates/rustai-tokenizer",
    "crates/rustai-plugin",
]

[workspace.dependencies]
# ML Frameworks
candle-core = "0.8"
candle-nn = "0.8"
candle-transformers = "0.8"
tract-onnx = "0.21"
llama_cpp = "0.3"

# Tokenization
tokenizers = "0.21"

# Vector Search
hnsw = "0.11"

# FFI
uniffi = "0.28"

# Async
tokio = { version = "1.40", features = ["rt-multi-thread"] }

# Serialization
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
```

**Android Build Script:**
```bash
#!/bin/bash
# build-android.sh

TARGETS="aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android"

for target in $TARGETS; do
    cargo ndk -t $target build --release -p rustai-core
done

# Generate Kotlin bindings
cargo run -p uniffi-bindgen -- \
    generate crates/rustai-core/src/rustai.udl \
    --language kotlin \
    --out-dir bindings/kotlin/
```

**iOS Build Script:**
```bash
#!/bin/bash
# build-ios.sh

# Build for all iOS targets
cargo lipo --release -p rustai-core

# Create XCFramework
xcodebuild -create-xcframework \
    -library target/aarch64-apple-ios/release/librustai_core.a \
    -headers include/ \
    -library target/aarch64-apple-ios-sim/release/librustai_core.a \
    -headers include/ \
    -output RustAI.xcframework

# Generate Swift bindings
cargo run -p uniffi-bindgen -- \
    generate crates/rustai-core/src/rustai.udl \
    --language swift \
    --out-dir bindings/swift/
```

### 5.3 UniFFI Interface Definition

```udl
// rustai.udl - UniFFI interface definition

namespace rustai {
    // Initialize the AI engine
    [Throws=RustAIError]
    RustAI create(RustAIConfig config);
};

[Error]
enum RustAIError {
    "ModelLoadError",
    "InferenceError",
    "TokenizationError",
    "VectorError",
    "ConfigError",
};

dictionary RustAIConfig {
    string model_path;
    string? tokenizer_path;
    string? vector_db_path;
    DeviceType device;
    u32 max_threads;
};

enum DeviceType {
    "Cpu",
    "Cuda",
    "Metal",
};

interface RustAI {
    // LLM Methods
    [Throws=RustAIError]
    string generate(GenerateRequest request);

    [Throws=RustAIError]
    void generate_stream(GenerateRequest request, StreamCallback callback);

    // NLU Methods
    [Throws=RustAIError]
    ClassificationResult classify(string text);

    // RAG Methods
    [Throws=RustAIError]
    void index_document(Document doc);

    [Throws=RustAIError]
    sequence<SearchResult> search(string query, u32 top_k);

    // Lifecycle
    void shutdown();
};

callback interface StreamCallback {
    void on_token(Token token);
    void on_complete();
    void on_error(string message);
};
```

### 5.4 KMP Integration

**Kotlin Wrapper (commonMain):**
```kotlin
// RustAIWrapper.kt - KMP wrapper around UniFFI bindings

expect class RustAIPlatform {
    fun create(config: RustAIConfig): RustAI
}

class AICoordinator(
    private val rustAI: RustAI,
    private val cloudProviders: List<CloudProvider>,
    private val config: AIConfig,
) {
    suspend fun generate(request: GenerateRequest): Flow<Token> = flow {
        // Try local first
        if (config.preferLocal && rustAI.isModelLoaded()) {
            rustAI.generateStream(request) { token ->
                emit(token)
            }
        } else {
            // Fallback to cloud
            cloudProviders.first().generate(request).collect { emit(it) }
        }
    }

    suspend fun classify(text: String): ClassificationResult {
        return rustAI.classify(text)
    }

    suspend fun search(query: String, topK: Int = 5): List<SearchResult> {
        return rustAI.search(query, topK.toUInt())
    }
}
```

**Android Implementation (androidMain):**
```kotlin
// RustAIPlatform.android.kt

actual class RustAIPlatform {
    actual fun create(config: RustAIConfig): RustAI {
        // Load native library
        System.loadLibrary("rustai_core")

        // Create instance via UniFFI
        return uniffi.rustai.create(config.toUniFFI())
    }
}
```

**iOS Implementation (iosMain):**
```kotlin
// RustAIPlatform.ios.kt

actual class RustAIPlatform {
    actual fun create(config: RustAIConfig): RustAI {
        // XCFramework is linked at build time
        return RustAIFFI.create(config.toUniFFI())
    }
}
```

---

## 6. Plugin System Design

### 6.1 Plugin Architecture

The plugin system supports **two loading mechanisms**:

1. **Native Plugins** (FFI/dylib) - Maximum performance
2. **WASM Plugins** - Sandboxed, portable

```
┌─────────────────────────────────────────────────────────────┐
│                    Plugin Manager                            │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                  Plugin Registry                     │    │
│  │  • Discover plugins                                  │    │
│  │  • Load/unload lifecycle                            │    │
│  │  • Capability routing                               │    │
│  └─────────────────────────────────────────────────────┘    │
│           │                           │                      │
│     ┌─────┴─────┐               ┌─────┴─────┐              │
│     ▼           ▼               ▼           ▼              │
│  ┌──────────────────┐       ┌──────────────────┐          │
│  │  Native Loader   │       │   WASM Loader    │          │
│  │  (dlopen2)       │       │   (wasmtime)     │          │
│  │  ──────────────  │       │  ──────────────  │          │
│  │  • FFI calls     │       │  • Sandboxed     │          │
│  │  • Shared memory │       │  • Portable      │          │
│  │  • Max perf      │       │  • Safe          │          │
│  └──────────────────┘       └──────────────────┘          │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 Plugin API

```rust
/// Core plugin trait - implement this for new capabilities
pub trait Plugin: Send + Sync {
    /// Plugin metadata
    fn info(&self) -> PluginInfo;

    /// Initialize plugin with config
    fn init(&mut self, config: &PluginConfig) -> Result<()>;

    /// Shutdown and cleanup
    fn shutdown(&mut self) -> Result<()>;
}

#[derive(Clone)]
pub struct PluginInfo {
    pub id: String,
    pub name: String,
    pub version: String,
    pub capabilities: Vec<Capability>,
}

pub enum Capability {
    /// Custom LLM backend
    LLMBackend {
        models: Vec<String>,
        quantizations: Vec<String>,
    },

    /// Custom embedding model
    EmbeddingModel {
        dimensions: usize,
        languages: Vec<String>,
    },

    /// Custom document parser
    DocumentParser {
        extensions: Vec<String>,
    },

    /// Custom NLU model
    NLUModel {
        languages: Vec<String>,
        intents: Vec<String>,
    },

    /// Tool/function calling
    Tool {
        name: String,
        description: String,
        parameters: serde_json::Value,
    },
}
```

### 6.3 Native Plugin Example

```rust
// plugins/custom-llm/src/lib.rs

use rustai_plugin::{Plugin, PluginInfo, Capability, export_plugin};

pub struct CustomLLMPlugin {
    model: Option<MyCustomModel>,
}

impl Plugin for CustomLLMPlugin {
    fn info(&self) -> PluginInfo {
        PluginInfo {
            id: "custom-llm".into(),
            name: "Custom LLM Backend".into(),
            version: "1.0.0".into(),
            capabilities: vec![
                Capability::LLMBackend {
                    models: vec!["my-model-7b".into()],
                    quantizations: vec!["Q4_K_M".into(), "Q8_0".into()],
                }
            ],
        }
    }

    fn init(&mut self, config: &PluginConfig) -> Result<()> {
        self.model = Some(MyCustomModel::load(config.model_path)?);
        Ok(())
    }

    fn shutdown(&mut self) -> Result<()> {
        self.model = None;
        Ok(())
    }
}

// Export for dynamic loading
export_plugin!(CustomLLMPlugin);
```

### 6.4 WASM Plugin Example

```rust
// plugins/custom-tool-wasm/src/lib.rs

use rustai_plugin_wasm::{Plugin, ToolCapability};

#[derive(Default)]
pub struct WeatherTool;

impl Plugin for WeatherTool {
    fn capabilities() -> Vec<Capability> {
        vec![Capability::Tool {
            name: "get_weather".into(),
            description: "Get current weather for a location".into(),
            parameters: json!({
                "type": "object",
                "properties": {
                    "location": { "type": "string" }
                },
                "required": ["location"]
            }),
        }]
    }

    fn call_tool(name: &str, params: &str) -> Result<String> {
        match name {
            "get_weather" => {
                let args: WeatherArgs = serde_json::from_str(params)?;
                // WASM can make HTTP calls via WASI
                Ok(format!("Weather in {}: Sunny, 72°F", args.location))
            }
            _ => Err("Unknown tool".into()),
        }
    }
}
```

### 6.5 Plugin Discovery & Loading

```rust
impl PluginManager {
    /// Discover plugins in a directory
    pub fn discover(&mut self, path: &Path) -> Result<Vec<PluginInfo>> {
        let mut plugins = Vec::new();

        for entry in fs::read_dir(path)? {
            let path = entry?.path();

            if path.extension() == Some("so".as_ref())
               || path.extension() == Some("dylib".as_ref()) {
                // Native plugin
                let info = self.probe_native(&path)?;
                plugins.push(info);
            } else if path.extension() == Some("wasm".as_ref()) {
                // WASM plugin
                let info = self.probe_wasm(&path)?;
                plugins.push(info);
            }
        }

        Ok(plugins)
    }

    /// Load a plugin by ID
    pub fn load(&mut self, id: &str) -> Result<()> {
        let info = self.registry.get(id)?;

        match &info.loader_type {
            LoaderType::Native(path) => {
                let plugin = unsafe { self.native_loader.load(path)? };
                self.loaded.insert(id.into(), LoadedPlugin::Native(plugin));
            }
            LoaderType::Wasm(path) => {
                let plugin = self.wasm_loader.load(path)?;
                self.loaded.insert(id.into(), LoadedPlugin::Wasm(plugin));
            }
        }

        Ok(())
    }
}
```

---

## 7. Migration Strategy

### 7.1 Phase Overview

```
Phase 1: Foundation (Weeks 1-4)
    │
    ├── Setup Rust workspace
    ├── Implement core traits/interfaces
    ├── Setup UniFFI bindings
    └── Create mock implementations

Phase 2: LLM Engine (Weeks 5-8)
    │
    ├── Implement llama_cpp backend
    ├── Implement candle backend
    ├── Add streaming support
    └── Benchmark against current

Phase 3: NLU & Tokenizer (Weeks 9-12)
    │
    ├── Port MobileBERT via tract
    ├── Port mALBERT via tract
    ├── Integrate HF tokenizers
    └── Benchmark accuracy

Phase 4: RAG & Vector (Weeks 13-16)
    │
    ├── Implement HNSW index
    ├── Add document parsers
    ├── Implement embedder
    └── Benchmark search

Phase 5: Integration (Weeks 17-20)
    │
    ├── KMP wrapper layer
    ├── Android integration
    ├── iOS integration
    └── End-to-end testing

Phase 6: Plugin System (Weeks 21-24)
    │
    ├── Native plugin loader
    ├── WASM plugin loader
    ├── Example plugins
    └── Documentation

Phase 7: Production (Weeks 25-28)
    │
    ├── Performance optimization
    ├── Memory profiling
    ├── Security audit
    └── Release preparation
```

### 7.2 Parallel Development Strategy

```
                    KMP Layer (existing)
                           │
     ┌─────────────────────┼─────────────────────┐
     │                     │                     │
     ▼                     ▼                     ▼
┌─────────┐         ┌─────────────┐        ┌─────────┐
│ Current │         │  Adapter    │        │ RustAI  │
│   AI    │◀───────▶│   Layer     │◀──────▶│  Core   │
│ Modules │         │ (Feature    │        │ (New)   │
│         │         │  Flags)     │        │         │
└─────────┘         └─────────────┘        └─────────┘
     │                     │                     │
     │    Gradual migration via feature flags   │
     └─────────────────────┴─────────────────────┘
```

**Feature Flag Strategy:**
```kotlin
// AIConfig.kt

enum class AIBackend {
    LEGACY,      // Current TVM/ONNX implementation
    RUST,        // New RustAI implementation
    HYBRID,      // Use Rust where available, legacy fallback
}

class AIConfig(
    val backend: AIBackend = AIBackend.HYBRID,
    val features: Set<AIFeature> = setOf(
        AIFeature.RUST_LLM,      // Enable Rust LLM engine
        AIFeature.RUST_NLU,      // Enable Rust NLU engine
        AIFeature.RUST_RAG,      // Enable Rust RAG engine
    ),
)
```

### 7.3 Testing Strategy

| Test Type | Coverage Target | Tools |
|-----------|-----------------|-------|
| Unit Tests | 80%+ | cargo test, mockall |
| Integration | All cross-crate | cargo test --workspace |
| FFI Tests | All bindings | UniFFI test harness |
| Benchmark | Critical paths | criterion.rs |
| Mobile E2E | Happy paths | Espresso, XCTest |

**Benchmark Comparison:**
```rust
// benches/llm_benchmark.rs

use criterion::{criterion_group, criterion_main, Criterion};

fn benchmark_generation(c: &mut Criterion) {
    let mut group = c.benchmark_group("LLM Generation");

    // Current implementation (JNI)
    group.bench_function("legacy_jni", |b| {
        b.iter(|| legacy_generate("Hello, world!"))
    });

    // New Rust implementation
    group.bench_function("rustai", |b| {
        b.iter(|| rustai_generate("Hello, world!"))
    });

    group.finish();
}
```

---

## 8. Use Cases & Examples

### 8.1 Voice Assistant (Android)

```kotlin
// VoiceAssistantActivity.kt

class VoiceAssistantActivity : ComponentActivity() {
    private val aiCoordinator by viewModels<AICoordinatorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VoiceAssistantScreen(
                onVoiceInput = { text ->
                    // 1. Classify intent (Rust NLU)
                    val classification = aiCoordinator.classify(text)

                    when (classification.intent) {
                        "question" -> {
                            // 2. Search relevant docs (Rust RAG)
                            val context = aiCoordinator.search(text, topK = 3)

                            // 3. Generate response (Rust LLM)
                            aiCoordinator.generateWithContext(text, context)
                                .collect { token ->
                                    // Stream to UI
                                    updateUI(token.text)
                                }
                        }
                        "command" -> {
                            // Execute device command
                            executeCommand(classification.entities)
                        }
                    }
                }
            )
        }
    }
}
```

### 8.2 Document Q&A (iOS)

```swift
// DocumentQAView.swift

struct DocumentQAView: View {
    @StateObject private var viewModel = DocumentQAViewModel()

    var body: some View {
        VStack {
            // Document picker
            DocumentPicker { url in
                Task {
                    // Index document (Rust RAG)
                    try await viewModel.indexDocument(url)
                }
            }

            // Question input
            TextField("Ask a question...", text: $viewModel.question)
                .onSubmit {
                    Task {
                        // Search & generate (Rust)
                        let answer = try await viewModel.askQuestion()
                        viewModel.response = answer
                    }
                }

            // Streaming response
            StreamingTextView(text: viewModel.response)
        }
    }
}

class DocumentQAViewModel: ObservableObject {
    private let rustAI = RustAI.shared

    func indexDocument(_ url: URL) async throws {
        let content = try String(contentsOf: url)
        let doc = Document(
            id: url.lastPathComponent,
            content: content,
            metadata: ["source": url.path]
        )
        try rustAI.indexDocument(doc)
    }

    func askQuestion() async throws -> String {
        // Search for relevant chunks
        let results = try rustAI.search(question, topK: 5)

        // Build context
        let context = results.map { $0.chunk }.joined(separator: "\n\n")

        // Generate answer
        let prompt = """
        Context: \(context)

        Question: \(question)

        Answer:
        """

        return try await rustAI.generate(GenerateRequest(
            prompt: prompt,
            maxTokens: 500,
            temperature: 0.7
        ))
    }
}
```

### 8.3 Offline Chatbot (Desktop/Linux)

```rust
// examples/cli/main.rs

use rustai_core::{RustAI, RustAIConfig, DeviceType};
use std::io::{self, Write};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Initialize RustAI
    let config = RustAIConfig {
        model_path: "models/llama-3-8b-q4.gguf".into(),
        tokenizer_path: Some("models/llama-3-tokenizer.json".into()),
        vector_db_path: Some("data/vectors.db".into()),
        device: DeviceType::Cpu,
        max_threads: 4,
    };

    let mut ai = RustAI::new(config)?;

    println!("RustAI CLI - Type 'quit' to exit");
    println!("Commands: /index <file>, /search <query>, /clear");

    loop {
        print!("> ");
        io::stdout().flush()?;

        let mut input = String::new();
        io::stdin().read_line(&mut input)?;
        let input = input.trim();

        if input == "quit" {
            break;
        }

        if input.starts_with("/index ") {
            let path = &input[7..];
            ai.index_file(path)?;
            println!("Indexed: {}", path);
            continue;
        }

        if input.starts_with("/search ") {
            let query = &input[8..];
            let results = ai.search(query, 3)?;
            for (i, r) in results.iter().enumerate() {
                println!("{}. [{}] {}", i+1, r.score, r.chunk);
            }
            continue;
        }

        // Generate response with streaming
        print!("AI: ");
        ai.generate_stream(input, |token| {
            print!("{}", token.text);
            io::stdout().flush().ok();
        })?;
        println!();
    }

    ai.shutdown()?;
    Ok(())
}
```

### 8.4 Plugin: Custom Weather Tool

```rust
// plugins/weather-tool/src/lib.rs

use rustai_plugin::{Plugin, PluginInfo, Capability, ToolHandler};
use serde::{Deserialize, Serialize};

pub struct WeatherPlugin {
    api_key: Option<String>,
}

#[derive(Deserialize)]
struct WeatherParams {
    location: String,
    units: Option<String>,
}

#[derive(Serialize)]
struct WeatherResponse {
    location: String,
    temperature: f32,
    conditions: String,
    humidity: u32,
}

impl Plugin for WeatherPlugin {
    fn info(&self) -> PluginInfo {
        PluginInfo {
            id: "weather-tool".into(),
            name: "Weather Tool Plugin".into(),
            version: "1.0.0".into(),
            capabilities: vec![
                Capability::Tool {
                    name: "get_weather".into(),
                    description: "Get current weather for a location".into(),
                    parameters: serde_json::json!({
                        "type": "object",
                        "properties": {
                            "location": {
                                "type": "string",
                                "description": "City name or coordinates"
                            },
                            "units": {
                                "type": "string",
                                "enum": ["celsius", "fahrenheit"],
                                "default": "celsius"
                            }
                        },
                        "required": ["location"]
                    }),
                }
            ],
        }
    }

    fn init(&mut self, config: &PluginConfig) -> Result<()> {
        self.api_key = config.get("api_key").map(|s| s.to_string());
        Ok(())
    }
}

impl ToolHandler for WeatherPlugin {
    fn call(&self, name: &str, params: &str) -> Result<String> {
        let params: WeatherParams = serde_json::from_str(params)?;

        // In real implementation, call weather API
        let response = WeatherResponse {
            location: params.location.clone(),
            temperature: 22.5,
            conditions: "Partly cloudy".into(),
            humidity: 65,
        };

        Ok(serde_json::to_string(&response)?)
    }
}

rustai_plugin::export_plugin!(WeatherPlugin);
```

**Using the plugin:**
```kotlin
// In KMP layer
val ai = AICoordinator(...)

// Load plugin
ai.loadPlugin("weather-tool", mapOf("api_key" to "xxx"))

// Use in conversation
val response = ai.generate("""
    User: What's the weather in Tokyo?

    Available tools: ${ai.availableTools()}
""")
// AI will call get_weather tool automatically
```

---

## 9. Performance Targets

### 9.1 Latency Targets

| Operation | Target | Acceptable | Current |
|-----------|--------|------------|---------|
| **LLM First Token** | <200ms | <500ms | ~800ms (JNI) |
| **LLM Token/sec** | 15-20 | 10 | 5-8 (JNI) |
| **NLU Classification** | <30ms | <50ms | ~60ms |
| **Embedding (single)** | <10ms | <20ms | ~15ms |
| **Vector Search (100K)** | <5ms | <20ms | ~1000ms |
| **Tokenization (1K tokens)** | <1ms | <5ms | ~3ms |

### 9.2 Memory Targets

| Component | Target | Peak | Current |
|-----------|--------|------|---------|
| **LLM Model (7B Q4)** | 4GB | 5GB | 6GB+ |
| **NLU Model** | 50MB | 80MB | ~100MB |
| **Vector Index (100K)** | 50MB | 100MB | N/A |
| **Base Runtime** | 10MB | 20MB | ~150MB |

### 9.3 Binary Size Targets

| Platform | Target | Current |
|----------|--------|---------|
| Android (arm64) | 15MB | ~120MB |
| iOS (arm64) | 15MB | ~100MB |
| Desktop | 20MB | ~150MB |

### 9.4 Benchmark Suite

```rust
// benches/comprehensive_benchmark.rs

use criterion::{criterion_group, criterion_main, Criterion, BenchmarkId};

fn llm_benchmarks(c: &mut Criterion) {
    let mut group = c.benchmark_group("LLM");

    for prompt_len in [10, 100, 500, 1000] {
        group.bench_with_input(
            BenchmarkId::new("first_token", prompt_len),
            &prompt_len,
            |b, &len| {
                let prompt = generate_prompt(len);
                b.iter(|| engine.generate_first_token(&prompt))
            },
        );
    }

    group.bench_function("throughput_tokens_per_sec", |b| {
        b.iter(|| engine.generate("Test prompt", 100))
    });

    group.finish();
}

fn nlu_benchmarks(c: &mut Criterion) {
    let mut group = c.benchmark_group("NLU");

    group.bench_function("classification_short", |b| {
        b.iter(|| nlu.classify("Turn on the lights"))
    });

    group.bench_function("classification_long", |b| {
        b.iter(|| nlu.classify(&long_text))
    });

    group.finish();
}

fn vector_benchmarks(c: &mut Criterion) {
    let mut group = c.benchmark_group("Vector");

    for size in [1_000, 10_000, 100_000, 1_000_000] {
        group.bench_with_input(
            BenchmarkId::new("search", size),
            &size,
            |b, &size| {
                let index = create_index(size);
                b.iter(|| index.search(&query_vector, 10))
            },
        );
    }

    group.finish();
}

criterion_group!(benches, llm_benchmarks, nlu_benchmarks, vector_benchmarks);
criterion_main!(benches);
```

---

## 10. Risk Assessment

### 10.1 Technical Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| llama.cpp API changes | High | Medium | Pin versions, adapter layer |
| UniFFI breaking changes | Low | High | Pin version, comprehensive tests |
| Candle immaturity | Medium | Medium | Fallback to llama_cpp |
| WASM limitations | Medium | Low | Native-only for perf-critical |
| Memory leaks across FFI | Medium | High | Extensive testing, ASAN |

### 10.2 Operational Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Rust expertise gap | Medium | High | Training, documentation |
| Build complexity | High | Medium | CI/CD automation |
| Debug difficulty (FFI) | High | Medium | Logging, error propagation |
| Model compatibility | Medium | Medium | Extensive model testing |

### 10.3 Mitigation Strategies

**1. Version Pinning:**
```toml
# Cargo.toml - Pin critical dependencies
llama_cpp = "=0.3.2"  # Exact version
uniffi = "~0.28"      # Patch updates only
```

**2. Adapter Layer:**
```rust
// Isolate external dependencies behind traits
pub trait LLMBackend: Send + Sync {
    // Stable interface, implementation can change
}

// Implementation can be swapped without API changes
pub struct LlamaCppBackend { ... }
pub struct CandleBackend { ... }
```

**3. Feature Flags for Gradual Rollout:**
```kotlin
// Gradual migration with kill switches
if (FeatureFlags.useRustLLM) {
    rustAI.generate(...)
} else {
    legacyAI.generate(...)
}
```

---

## 11. Implementation Roadmap

### 11.1 Phase 1: Foundation (Weeks 1-4)

**Week 1-2: Project Setup**
- [ ] Create Rust workspace structure
- [ ] Setup cargo-ndk, cargo-lipo toolchains
- [ ] Configure CI/CD (GitHub Actions)
- [ ] Setup UniFFI scaffolding

**Week 3-4: Core Interfaces**
- [ ] Define all trait interfaces
- [ ] Implement mock backends for testing
- [ ] Create UniFFI .udl definitions
- [ ] Generate initial Kotlin/Swift bindings
- [ ] Basic KMP wrapper

**Deliverable:** Compiling project with mock implementations, bindings work

### 11.2 Phase 2: LLM Engine (Weeks 5-8)

**Week 5-6: llama_cpp Backend**
- [ ] Integrate llama_cpp_rs
- [ ] Implement model loading
- [ ] Implement synchronous generation
- [ ] Add GBNF grammar support

**Week 7-8: Streaming & Optimization**
- [ ] Implement streaming generation
- [ ] Add prompt caching
- [ ] Implement candle backend (GPU)
- [ ] Benchmark against legacy

**Deliverable:** Working LLM inference, 2x faster than current

### 11.3 Phase 3: NLU & Tokenizer (Weeks 9-12)

**Week 9-10: Tokenizer Integration**
- [ ] Integrate HuggingFace tokenizers
- [ ] Support BPE, WordPiece, SentencePiece
- [ ] Add caching layer
- [ ] Benchmark performance

**Week 11-12: NLU Models**
- [ ] Port MobileBERT via tract
- [ ] Port mALBERT via tract
- [ ] Entity extraction
- [ ] Accuracy validation against legacy

**Deliverable:** NLU with same accuracy, 2x faster

### 11.4 Phase 4: RAG & Vector (Weeks 13-16)

**Week 13-14: Vector Store**
- [ ] Implement HNSW index
- [ ] Add persistence layer
- [ ] Implement filtering
- [ ] Add quantization options

**Week 15-16: RAG Pipeline**
- [ ] Document parsers (PDF, HTML, DOCX)
- [ ] Chunking strategies
- [ ] Embedding integration
- [ ] End-to-end RAG flow

**Deliverable:** 100x faster vector search

### 11.5 Phase 5: Integration (Weeks 17-20)

**Week 17-18: Android Integration**
- [ ] Complete KMP wrapper
- [ ] Android sample app
- [ ] Performance profiling
- [ ] Memory optimization

**Week 19-20: iOS Integration**
- [ ] iOS sample app
- [ ] XCFramework packaging
- [ ] Performance profiling
- [ ] Memory optimization

**Deliverable:** Working mobile apps with RustAI

### 11.6 Phase 6: Plugin System (Weeks 21-24)

**Week 21-22: Native Plugins**
- [ ] Plugin trait definitions
- [ ] Dynamic loading (dlopen2)
- [ ] Plugin discovery
- [ ] Example: custom LLM backend

**Week 23-24: WASM Plugins**
- [ ] WASM runtime (wasmtime)
- [ ] WASI support
- [ ] Example: tool plugin
- [ ] Documentation

**Deliverable:** Extensible plugin system

### 11.7 Phase 7: Production (Weeks 25-28)

**Week 25-26: Optimization**
- [ ] Profile all hot paths
- [ ] SIMD optimization where applicable
- [ ] Memory pooling
- [ ] Final benchmarks

**Week 27-28: Release**
- [ ] Security audit
- [ ] Documentation
- [ ] Migration guide
- [ ] v1.0 release

**Deliverable:** Production-ready RustAI

---

## 12. Appendix: Research Sources

### ML Frameworks
- [Candle vs Burn Comparison](https://medium.com/@athan.seal/candle-vs-burn-comparing-rust-machine-learning-frameworks-4dbd59c332a1)
- [Burn Framework](https://github.com/tracel-ai/burn)
- [Candle by HuggingFace](https://github.com/huggingface/candle)
- [Building Sentence Transformers in Rust](https://dev.to/mayu2008/building-sentence-transformers-in-rust-a-practical-guide-with-burn-onnx-runtime-and-candle-281k)

### Cross-Platform FFI
- [UniFFI by Mozilla](https://github.com/mozilla/uniffi-rs)
- [UniFFI for React Native](https://hacks.mozilla.org/2024/12/introducing-uniffi-for-react-native-rust-powered-turbo-modules/)
- [Running Rust on Android with UniFFI](https://sal.dev/android/intro-rust-android-uniffi/)
- [Building iOS Apps with UniFFI](https://dev.to/almaju/building-an-ios-app-with-rust-using-uniffi-200a)

### LLM Inference
- [llama.cpp](https://github.com/ggml-org/llama.cpp)
- [llama_cpp Rust bindings](https://docs.rs/llama_cpp)
- [rust-llama.cpp](https://github.com/mdrokz/rust-llama.cpp)
- [LLM Inference on Edge](https://huggingface.co/blog/llm-inference-on-edge)

### Vector Databases
- [Qdrant](https://qdrant.tech/)
- [Qdrant Benchmarks](https://qdrant.tech/benchmarks/)
- [LanceDB vs Qdrant](https://medium.com/@vinayak702010/lancedb-vs-qdrant-for-conversational-ai-vector-search-in-knowledge-bases-793ac51e0b81)
- [Top Vector Databases 2025](https://medium.com/@fendylike/top-5-open-source-vector-search-engines-a-comprehensive-comparison-guide-for-2025-e10110b47aa3)

### Tokenization
- [HuggingFace Tokenizers](https://github.com/huggingface/tokenizers)
- [Tokenizers Rust Docs](https://docs.rs/tokenizers/)
- [Porting Tokenizers to WASM](https://blog.mithrilsecurity.io/porting-tokenizers-to-wasm/)

### Plugin Architecture
- [Plugins in Rust: Dynamic Loading](https://nullderef.com/blog/plugin-dynload/)
- [How to Build a Plugin System in Rust](https://www.arroyo.dev/blog/rust-plugin-systems/)
- [Plugins in Rust by Michael Bryan](https://adventures.michaelfbryan.com/posts/plugins-in-rust/)

### Mobile Optimization
- [On-Device LLM](https://medium.com/@jiminlee-ai/on-device-llm-1ea0476a2df6)
- [Running Transformers on Mobile](https://huggingface.co/blog/tugrulkaya/running-large-transformer-models-on-mobile)
- [MobileQuant](https://arxiv.org/html/2408.13933v1)
- [cargo-ndk](https://github.com/bbqsrc/cargo-ndk)

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-22 | AI Team | Initial comprehensive plan |

---

**Next Steps:**
1. Review and approve this architecture plan
2. Allocate team resources
3. Begin Phase 1: Foundation setup
4. Schedule weekly architecture reviews
