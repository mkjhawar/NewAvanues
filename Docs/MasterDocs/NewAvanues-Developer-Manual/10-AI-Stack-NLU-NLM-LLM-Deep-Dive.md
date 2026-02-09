---
title: "Chapter 10 — AI Stack Deep Dive (NLU/NLM/LLM + AVA Orchestration)"
owner: "AI Platform"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 10 — AI Stack Deep Dive (NLU/NLM/LLM + AVA Orchestration)

## 10.1 Purpose

This chapter defines how NewAvanues interprets intent and generates intelligent responses using the AI stack:

- **NLU** (Natural Language Understanding): deterministic + semantic intent classification.
- **NLM** (Natural Language Mediation layer): routing/orchestration logic between recognized intent, context, and response strategy.
- **LLM** (Large Language Models): adaptive generation for conversational and long-tail requests.

In production behavior, these are coordinated by AVA modules and consumed by VoiceOSCore/WebAvanue execution paths.

## 10.2 End-to-End AI Flow

```text
User Utterance
  -> ASR text
  -> NLU (exact/fuzzy/semantic)
  -> NLM mediation (route decision)
      -> actionable intent => VoiceOSCore/Web action path
      -> conversational/complex => LLM response path
  -> response + optional action execution + feedback loop
```

## 10.3 NLU Architecture (Intent Engine)

Primary behavior is hybrid and latency-layered:

1. **Exact/pattern matching** (~sub-ms to very low latency).
2. **Fuzzy matching** for typos/near phrases.
3. **Semantic embedding match** (BERT-family) for novel phrasing.

Key documented characteristics from source docs:

| Attribute | Snapshot |
|---|---|
| Strategy | Hybrid (Pattern + Fuzzy + Semantic) |
| Core multilingual model | mALBERT |
| English-lean variant | MobileBERT |
| Typical target latency | <100ms end-to-end classification budget |
| Supported languages | 52+ in full multilingual path |

## 10.4 NLM Mediation Layer (Routing + Policy)

The NLM layer is the practical decision bridge between classification and generation/execution.

It decides:

1. Should this be executed as a command now?
2. Should it be clarified/disambiguated first?
3. Should it be escalated to LLM response generation?
4. What fallback policy applies under model/network constraints?

In current stack artifacts, this behavior is represented by coordinator/router patterns across AVA chat/action paths (intent router, response coordinator, fallback chain), even when not always labeled explicitly as “NLM” in code.

## 10.5 LLM Architecture (Adaptive Generation)

LLM layer is coordinated through adaptive/fallback generators:

- Local/on-device providers where possible.
- Cloud fallback chain where needed.
- Template fallback for deterministic safety/availability.

Representative behavior from current docs:

| Capability | Intent |
|---|---|
| Streaming chunks | responsive UI + typewriter style output |
| Hybrid fallback chain | resilience under failure/provider limits |
| Cost-aware routing | operational budget control |
| Context builder integration | mobile-optimized prompt construction |

## 10.6 AVA Orchestration Contract

AVA composes NLU + mediation + LLM with action execution modules.

```text
Chat/Voice input
  -> NLUCoordinator
  -> IntentRouter / ActionCoordinator
      -> ActionsManager/VoiceOS bridge (action path)
      -> ResponseCoordinator + LLM generator (conversation path)
  -> TTS/UI feedback
```

This ensures one user utterance can deterministically choose either:

- **command execution**, or
- **assistant response generation**,

with confidence-aware fallback.

## 10.7 Integration with VoiceOSCore and WebAvanue

AI stack outputs are consumed by execution surfaces:

1. **VoiceOSCore**: system/app/action handlers.
2. **WebAvanue**: browser/web control and DOM-targeted action routes.

Design principle: AI decides *what*; execution layers decide *how* on each platform surface.

## 10.8 Reliability and Failure Policies

| Failure Class | Expected Handling |
|---|---|
| low-confidence NLU | confirmation/disambiguation prompt |
| unavailable semantic model | degrade to deterministic commands |
| unavailable cloud LLM | use local/template fallback chain |
| action-handler failure | return user feedback + retry/alternate route |

## 10.9 Engineering Rules

Before shipping AI-stack changes:

- [ ] NLU threshold changes documented and regression-tested.
- [ ] Mediation/routing policy updates reflected in decision records.
- [ ] LLM fallback order and timeout behavior validated.
- [ ] VoiceOSCore/WebAvanue integration contracts unchanged or versioned.

## 10.10 References

- `Docs/MasterDocs/NLU/README.md`
- `Docs/MasterDocs/LLM/README.md`
- `Docs/MasterDocs/AVA/README.md`
- `Modules/AI/NLU/README.md`
- `Modules/AI/LLM/README.md`
