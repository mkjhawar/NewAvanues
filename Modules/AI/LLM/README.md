# Features - Large Language Model (LLM)

## Purpose
Implements the Adaptive LLM Coordinator (ALC) that orchestrates between on-device models (ONNX) and cloud LLMs (GPT-4, Claude, Gemini). Handles complex queries, conversation management, and intelligent routing.

## Implementation Phase
Phase 1.0, Week 9-12 (LLM Integration)

## Responsibilities
- Implement Adaptive LLM Coordinator (ALC) routing logic
- Manage connections to cloud LLM providers (OpenAI, Anthropic, Google)
- Handle on-device LLM inference (via ONNX or GGUF)
- Implement fallback strategies (cloud -> on-device -> cached)
- Manage conversation context and history
- Handle streaming responses
- Implement prompt engineering and optimization
- Monitor token usage and costs

## Dependencies
- core/domain (conversation entities, use cases)
- core/data (persist conversations, cache responses)
- features/nlu (for intent classification to aid routing)
- Cloud LLM SDKs (OpenAI, Anthropic, Google AI)

## Architecture Notes
**Layer**: Features
**Dependency Rule**: Depends on core and features/nlu. Can be used by platform layer.
**Testing**: Mock LLM providers for unit tests, integration tests with actual APIs.

The LLM feature is the brain of AVA AI for complex reasoning. The ALC intelligently decides when to use on-device models vs cloud APIs based on query complexity, network availability, and user preferences.
