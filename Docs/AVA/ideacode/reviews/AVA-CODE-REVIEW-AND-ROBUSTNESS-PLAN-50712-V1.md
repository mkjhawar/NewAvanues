# AVA Codebase Review & Robustness Plan
**Date:** 2025-12-07
**Status:** Review Complete

---

## 1. Executive Summary
A comprehensive review of the `common/LLM`, `common/Chat`, and `common/NLU` modules reveals a strong **configuration** and **data** foundation, but significant gaps in the **runtime execution** layer required to fully realize the "Robust Intelligence" specification.

| Component | Status | Key Findings |
|-----------|--------|--------------|
| **ALC Engine** | 🟡 Partial | `DeviceModelSelector` is ready for Gemma 3n, but `LocalLLMProvider` lacks `LiteRT` integration. |
| **Adaptive NLU** | 🔴 Missing | `NLUCoordinator` supports a single model. No runtime switching between MobileBERT/Malbert. |
| **Robust Memory** | 🟢 Ready | Database schemas (`Memory.sq`, `RAGChunk.sq`) and Semantic NLU (Embeddings) are fully implemented. |

---

## 2. Detailed Gap Analysis

### 2.1 ALC-LLM & Gemma 3n
*   **The Good**: `DeviceModelSelector.kt` correctly identifies "Tier A" devices (Navigator 500, Vuzix M4000) and recommends `ConfigurationType.GEMMA3N`.
*   **The Gap**: `LocalLLMProvider.kt` only initializes `MLCInferenceStrategy` (TVM) or `GGUFInferenceStrategy`. It completely ignores `LiteRT`.
*   **Impact**: If a device selects a Gemma 3n config, the system will fail to load the model or fallback to a generic error, as no LiteRT runtime is wired up.

### 2.2 Adaptive NLU
*   **The Good**: `IntentClassifier.kt` has a robust "Semantic Similarity" fallback using cosine similarity on pre-computed embeddings. This is excellent for robustness.
*   **The Gap**: The logic is "One Model Only". It loads a single model path at initialization. There is no `AdaptiveNLUClassifier` to route fast queries to MobileBERT and complex ones to Malbert.
*   **Impact**: Users get either speed (MobileBERT) OR accuracy (Malbert), never both dynamically.

### 2.3 Memory Systems
*   **The Good**: The memory architecture is surprisingly complete. `RAGChunk` table implementation enables "Tier 3" semantic memory. `IntentClassifier` already uses a vector-based approach for intent recognition.
*   **Recommendation**: Ensure `MemoryManager` actively utilizes `RAGChunk` for *conversation history*, not just document retrieval.

---

## 3. Fast Offline Method Recommendations

To achieve the "Fast Offline" goal immediately without waiting for the full rewrite:

### A. Optimization of Current MobileBERT (Instant Win)
The current `IntentClassifier` uses `ONNX Runtime` with a generic backend selection.
*   **Action**: Force `NNAPI` or `Hexagon Delegate` for Qualcomm devices (RealWear/Vuzix) in `IntentClassifier.kt`.
*   **Benefit**: Reduce latency from ~50ms to ~15ms on Navigator 500.

### B. "Keyword Fast-Path" (Robustness)
`IntentClassifier.kt` currently does full BERT inference for *every* utterance.
*   **Action**: Implement a `KeywordSpotter` (Trie-based) before the BERT model.
*   **Logic**: If user says "Stop" or "Back", match immediately (0ms latency, 0 battery). Only run BERT if no keyword match.
*   **Benefit**: Extreme responsiveness for navigation commands.

---

## 4. Robustness Implementation Plan

### Phase 1: Fix the Runtime Gap (Critical)
1.  **Integrate LiteRT**: Create `LiteRTInferenceStrategy` in `common/LLM`.
2.  **Wire Provider**: Update `LocalLLMProvider` to instantiate `LiteRTInferenceStrategy` when `llmRuntime == "LiteRT"`.

### Phase 2: Enable Adaptive NLU
1.  **Refactor Coordinator**: Update `NLUCoordinator` to hold *two* classifier instances (or a composite).
2.  **Dispatcher**: Create `NLUDispatcher.kt` with logic:
    ```kotlin
    fun classify(text: String): Intent {
       if (text.length < 20) return fastModel.classify(text) // MobileBERT
       val result = fastModel.classify(text)
       if (result.confidence < 0.7) return deepModel.classify(text) // Malbert
       return result
    }
    ```

### Phase 3: "Goldfish Memory" Fix
1.  **Context Injection**: Modify `LocalLLMProvider.chat()` to query `Memory.sq` (Working Memory) and inject the last 5 relevant interactions into the `system` prompt dynamically.

---

## 5. Decision Required
Do you want to proceed with **Phase 1 (LiteRT Integration)** to unlock Gemma 3n support, or **Phase 2 (Adaptive NLU)** to improve command responsiveness first?
