---
title: "Chapter 01 — Platform Vision, Terminology, and Principles"
owner: "Platform Engineering"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 01 — Platform Vision, Terminology, and Principles

## 1.1 What NewAvanues Is

NewAvanues is a **voice-first application platform** where users can operate digital systems using natural speech, across app surfaces and web surfaces, with accessibility-grade interaction and deterministic execution.

The core promise is:

> **Any app, any time, voice first.**

This promise is implemented by combining:

- **VoiceOS®** — command understanding + execution platform
- **VoiceTouch™** — interaction model for voice-triggered UI action and confirmation
- **AvanueUI** — adaptive design system and component model
- **WebAvanue** — web/browser integration layer with DOM-aware voice mapping
- **New Avanues App** — product composition layer that binds modules into user experiences

## 1.2 Product Philosophy

### Principle A — Voice is primary, not secondary
Voice is not a shortcut; it is a first-class command bus.

### Principle B — Accessibility drives architecture
Execution paths must be compatible with accessibility semantics and resilient fallback behavior.

### Principle C — Privacy-first processing
Prefer on-device processing and explicit data boundaries, with cloud integration as an opt-in architecture choice.

### Principle D — Cross-surface continuity
Commands should preserve intent across mobile/native/web contexts.

### Principle E — Explainable decisions
Major architecture decisions must be captured in ADR form (see Chapter 08).

## 1.3 Key Terms

| Term | Meaning |
|---|---|
| VoiceOS® | Platform layer that interprets voice command intent and executes actions |
| VoiceTouch™ | UX contract for how voice maps to tactile/visual outcomes |
| VoiceOSCore | KMP command-processing and handler coordination engine |
| AVA | Intelligence layer (NLU/LLM/Context) used to resolve intent |
| AvanueUI | Shared UI system with tokens, themes, responsive profiles, glass primitives |
| WebAvanue | Voice-capable browser + DOM scraping + web element execution layer |
| AVID/VUID | Stable identifiers/fingerprints for targetable UI elements |

## 1.4 Strategic Platform Position

The platform sits between human intent and software affordances:

```text
User Intent (speech)
    -> NLU/LLM understanding (AVA)
    -> Command resolution (VoiceOSCore)
    -> Surface execution (native/web)
    -> Feedback loop (audio/visual/haptic)
```

## 1.5 Design Constraints

1. Low-latency command interpretation.
2. Predictable execution paths with explicit fallback.
3. Strong module boundaries in monorepo structure.
4. Documentation must remain canonical and merge-friendly.

## 1.6 What This Manual Optimizes For

- Faster onboarding for new contributors.
- Better architecture consistency for maintainers.
- Better decision traceability for long-lived development.
- Faster migration from fragmented docs to one canonical source.
