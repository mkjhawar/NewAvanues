# AVA Robust Intelligence Specification v2.0

**Version:** 2.0
**Date:** 2025-12-07
**Status:** DRAFT
**Author:** IDEACODE (Antigravity Agent)
**System:** AVA (Android Voice Assistant)

---

## 1. Executive Summary

This specification defines the "Robust Intelligence" architecture for AVA v3.0, moving from a static NLU/LLM pipeline to a dynamic, hybrid intelligence system. The core engines are **ALC-llm** (AVA's optimized fork of MLC-llm) for generation and an Adaptive NLU system for understanding. The system introduces a **Robust Memory System (RMS)** with four distinct tiers to ensure context retention across sessions and device reboots.

---

## 2. Core Engine: ALC-LLM (v2.0)

**ALC-llm** is the custom inference engine derived from MLC-llm, optimized for Android devices using TVM/OpenCL.

### 2.1 Model Tiers & Device Selection

The system automatically selects the LLM model based on the device capabilities (RAM/NPU).

| Tier | Device Class | Model | Params | Quantization | Use Case |
|------|--------------|-------|--------|--------------|----------|
| **Tier A** | High-End (12GB+ RAM)<br>Snapdragon 8 Gen 2+ | **Gemma 3n** | 4B (Mini) | `q4bf16_1` | Complex reasoning, Multilingual, Creative writing |
| **Tier B** | Mid-Range (6-8GB RAM)<br>Snapdragon 888+ | **Gemma 2** | 2B | `q4f16_1` | Standard chat, Summarization, basic Logic |
| **Tier C** | Low-End / Older | **N/A** | - | - | Fallback to Cloud or Basic NLU only |

### 2.2 Integration
-   **Runtime**: Apache TVM with customized Android JNI bindings.
-   **Optimization**: OpenCL GPU acceleration for prefill and decode.
-   **Lifecycle**: Managed by `InferenceManager` with strict thermal/battery throttling rules.

---

## 3. Adaptive NLU System

To balance latency and understanding depth, the NLU system switches models dynamically.

### 3.1 Dual-Model Architecture

1.  **Fast Path: MobileBERT**
    -   **Model**: `mobilebert-uncased` (Running on TFLite/ONNX)
    -   **Latency**: < 20ms
    -   **Purpose**: Wake word validation, media controls, simple utility commands (e.g., "Stop music", "Turn on lights").
    -   **Trigger**: Always valid for short utterances (< 5 words).

2.  **Deep Path: Malbert**
    -   **Model**: `malbert` (Modified ALBERT optimized for mobile)
    -   **Latency**: 50-100ms
    -   **Purpose**: Sentiment analysis, complex routing, entity extraction, ambiguous queries.
    -   **Trigger**: activated when MobileBERT confidence < 0.8 or utterance length > 5 words.

### 3.2 Self-Learning Loop
If BOTH NLU models fail (confidence < threshold), the query is routed to the **ALC-LLM**.
-   The LLM classifies the intent.
-   The result is fed back into the `TrainExample` database (sqlite).
-   `EmbeddingComputeWorker` retrains the vector space in the background.

---

## 4. Robust Memory System (RMS)

A multi-tiered memory architecture designed to solve the "goldfish memory" problem.

### 4.1 The 4 Tiers of Memory

| Tier | Type | Persistance | Storage | Purpose |
|------|------|-------------|---------|---------|
| **1. Ephemeral** | **KV Cache** | Request-only | RAM (Native) | Immediate token generation key-values. Cleared after generation. |
| **2. Working** | **Short-term** | Session | SQLite (Vector) | The current conversation window. Stores last ~10 turns. Vectorized for relevance retrieval. |
| **3. Semantic** | **Long-term** | Permanent | Vector DB (SQLDelight) | "infinite" memory. Stores summaries, facts, and important context. Retrieved via RAG using **Malbert** embeddings. |
| **4. Structured** | **Entity Graph** | Permanent | SQL (Relational) | Hard facts about the user. <br>Ex: `User.name="Manoj"`, `Home.location="Seattle"`. |

### 4.2 Memory Flow
1.  **Input**: User says "Remember I like sushi".
2.  **NLU**: Classifies as `save_preference`.
3.  **RMS (Structured)**: Updates `UserPreferences` table: `{ food: "sushi" }`.
4.  **RMS (Semantic)**: Embeds "I like sushi" and saves to Vector DB.
5.  **Later**: User says "Order dinner".
6.  **Retrieval**: RMS searches Semantic memory -> finds "sushi".
7.  **Context**: "Sushi" preference injected into **Working** memory context.
8.  **Output**: ALC-LLM suggests: "Shall I order sushi again?"

---

## 5. Implementation Roadmap

### Phase 1: Core Engine Update
- [ ] Integrate **Gemma 3n** into ALC build pipeline.
- [ ] Implement `DeviceTierManager` to detect Tier A/B.
- [ ] Update `InferenceManager` to select model at startup.

### Phase 2: NLU Refactor
- [ ] Deploy **MobileBERT** .tflite model.
- [ ] Deploy **Malbert** .onnx model.
- [ ] Create `AdaptiveNLUClassifier` class to orchestrate switching.

### Phase 3: Robust Memory
- [ ] Schema update for `SemanticMemory` table (with vector columns).
- [ ] Implement `RAGRetriever` using Malbert for embeddings.
- [ ] Build `ContextAssembler` to merge layers before LLM inference.

---

**Approvals**
- [x] Architecture Review (Agent Antigravity)
- [ ] Lead Engineer (User)
