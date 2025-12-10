# Chapter 74: Robust Intelligence Architecture

**Created:** 2025-12-07
**Author:** AVA AI Team
**Status:** IMPLEMENTED (Phase 1-3)

## 74.1. Overview

The "Robust Intelligence" initiative (AVA-ROBUST-INTELLIGENCE-SPEC-V2) addresses three critical areas of the AVA AI system:
1.  **High-Performance Inference**: Integration of **Gemma 3n** models via Google LiteRT.
2.  **Adaptive NLU**: Dynamic switching between "Fast Path" (Keyword) and "Deep Path" (Transformer) NLU.
3.  **Robust Memory**: Context injection to solve the "Goldfish Memory" problem using Working Memory.

## 74.2. LiteRT Integration (Gemma 3n)

We have extended the `ALCEngine` to support the **LiteRT** runtime (formerly TensorFlow Lite) specifically for Google's Gemma 3n models.

### Key Components
*   **`LiteRTInferenceStrategy.kt`**: Wraps the `com.google.ai.edge.litert` interpreter. Handles tensor input/output and simple inference.
*   **`LocalLLMProvider`**: Updated to detect `.tflite` model files and automatically select the `LiteRT` runtime strategy.

### Usage
Place a valid Gemma 3n `.tflite` model in a model directory. The system automatically detects it via `isLiteRTModelDirectory()` and initializes the correct engine.

## 74.3. Adaptive NLU System

To optimize latency for common commands, we implemented a dual-path NLU architecture.

### Architecture
*   **Fast Path (<1ms)**: `KeywordSpotter.kt` uses a Trie-based Aho-Corasick algorithm to instantly detect critical commands like "Stop", "Back", "Home', and "Cancel".
*   **Deep Path (~50ms)**: `IntentClassifier` (MobileBERT) handles complex natural language queries.
*   **Dispatcher**: `NLUDispatcher.kt` routes queries. It first checks the Fast Path; if no match is found, it delegates to the Deep Path.

### fast-path-keywords
| Keyword | Intent ID |
| :--- | :--- |
| "stop" | `system_stop` |
| "back" | `system_back` |
| "cancel" | `system_cancel` |
| "home" | `system_home` |

## 74.4. Robust Memory (Context Injection)

Addressed the "Goldfish Memory" issue where the LLM lacked context of immediately preceding turns.

### Implementation
*   **Working Memory**: We query the `memory` table in `AVADatabase` for items with `memory_type = 'working'`.
*   **Context Injection**: In `LocalLLMProvider.chat()`, we fetch the last 5 working memories and append them to the **System Prompt**.
*   **Format**:
    ```text
    RECENT MEMORY (Working Context):
    - User asked about weather
    - User opened settings
    ```

## 74.5. Verification

### Automated Tests
*   `LiteRTInferenceTest`: Verifies loading of `.tflite` models.
*   `NLUDispatcherTest`: Ensures "stop" routes to `system_stop` and "what's the weather" routes to MobileBERT.

### Manual Verification
1.  **Memory**: Say "Call me Iron Man". Then ask "Who am I?". The system should respond "You are Iron Man" using the injected context.
2.  **Performance**: Say "Stop". The log should show `⚡ Fast Path Match` with <1ms latency.
