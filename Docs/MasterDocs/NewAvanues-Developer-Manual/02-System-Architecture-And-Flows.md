---
title: "Chapter 02 — System Architecture and End-to-End Flows"
owner: "Platform Engineering"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 02 — System Architecture and End-to-End Flows

## 2.1 Architecture at a Glance

NewAvanues is a layered architecture that converts natural language into deterministic action across native and web surfaces.

```text
┌───────────────────────────────────────────────────────────────────┐
│ User Layer                                                        │
│ Voice input, confirmations, corrections                           │
└───────────────────────────────┬───────────────────────────────────┘
                                │
┌───────────────────────────────▼───────────────────────────────────┐
│ Intelligence Layer (AVA)                                           │
│ Intent classification, entities, context, ambiguity resolution     │
└───────────────────────────────┬───────────────────────────────────┘
                                │
┌───────────────────────────────▼───────────────────────────────────┐
│ Execution Core (VoiceOSCore)                                       │
│ ActionCoordinator, handler routing, command registries             │
└───────────────────────────────┬───────────────────────────────────┘
                                │
               ┌────────────────┴───────────────┐
               │                                │
┌──────────────▼─────────────┐      ┌──────────▼───────────────────┐
│ Native App Surface          │      │ Web Surface (WebAvanue)      │
│ Accessibility/UI semantics  │      │ DOM scraping + VUID mapping  │
└──────────────┬─────────────┘      └──────────┬───────────────────┘
               │                                │
┌──────────────▼────────────────────────────────▼───────────────────┐
│ Feedback Loop                                                     │
│ Success/failure, disambiguation prompts, confidence adaptation    │
└───────────────────────────────────────────────────────────────────┘
```

## 2.2 Monorepo Architectural Intent

From root settings and module inclusion patterns:

- **Kotlin Multiplatform** for shared domain/runtime logic.
- **Feature modules** for AI, VoiceOSCore, AvanueUI, WebAvanue, Actions, Foundation.
- **Consolidated app shell** (`apps:avanues`) to unify VoiceAvanue + WebAvanue + gaze/cursor capabilities.
- Legacy and duplicate modules are intentionally archived/deprecated, with active module consolidation around top-level `Modules/*` entries.

## 2.3 Primary Runtime Components

| Layer | Core Components | Responsibility |
|---|---|---|
| Speech | STT engine adapters | Convert waveform → text/confidence |
| Understanding | AVA (NLU + LLM) | Map text to intent + entities + confidence |
| Command Core | VoiceOSCore | Resolve command, choose handler, execute |
| Surface Adaptation | Native UI / WebAvanue DOM bridge | Find target elements and act |
| Experience | AvanueUI + VoiceTouch™ | User feedback, visual state, voice interaction contract |

## 2.4 Canonical End-to-End Flow

### 2.4.1 Flow: “Click Submit” in native app

```text
1) Speech recognized -> "click submit"
2) AVA classifies intent: UI_INTERACTION
3) VoiceOSCore resolves target phrase against dynamic/static command registries
4) UI handler executes accessibility click on target element
5) Result emitted -> feedback rendered (audio/visual/haptic)
```

### 2.4.2 Flow: “Click login button” on web page

```text
1) Speech recognized -> "click login button"
2) AVA classifies as UI interaction
3) WebAvanue DOMScraperBridge returns candidate elements with VUIDs
4) VoiceCommandGenerator fuzzy-matches utterance to candidates
5) Execute click via element selector/VUID path
6) Confirmation and error fallback (alternatives if ambiguous)
```

## 2.5 Decision Flow (Routing)

VoiceOSCore routing strategy is effectively a priority cascade:

```text
Exact command match
  -> Fuzzy match
    -> Static handler match
      -> NLU semantic resolution
        -> LLM fallback
          -> Not recognized / clarification
```

This structure ensures deterministic low-latency paths for common commands while preserving long-tail natural language support.

## 2.6 System Boundaries

### In Scope
- Voice command lifecycle from input to execution.
- Cross-module contracts among VoiceOSCore, AvanueUI, WebAvanue, and app composition.
- Developer extension points (handlers, commands, design components).

### Out of Scope (for this chapter)
- Full API reference of each module (covered in module deep-dive chapters).
- Product marketing narrative.

## 2.7 Architecture Quality Attributes

| Attribute | Architectural Mechanism |
|---|---|
| Low latency | Priority-based resolver + local handlers |
| Extensibility | Handler registry + modularized components |
| Portability | KMP shared cores + platform executors |
| Accessibility | Semantic trees, accessibility APIs, command discoverability |
| Privacy | On-device first execution path and configurable engine choices |

## 2.8 Chapter Links

- VoiceOSCore detail: Chapter 03
- AvanueUI detail: Chapter 04
- WebAvanue detail: Chapter 05
- App composition detail: Chapter 06
- ADR rationale: Chapter 08
