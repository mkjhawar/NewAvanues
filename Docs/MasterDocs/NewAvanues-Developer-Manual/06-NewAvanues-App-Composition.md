---
title: "Chapter 06 — New Avanues App Deep Dive (Composition + Integration)"
owner: "Applications Engineering"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 06 — New Avanues App Deep Dive (Composition + Integration)

## 6.1 What the New Avanues App Is

The New Avanues App is the **integration shell** that composes VoiceOS-powered interaction, AvanueUI rendering, and WebAvanue browser capabilities into one operational experience.

It is not “just another client UI”; it is the orchestration boundary where platform modules become end-user workflows.

## 6.2 Functional Responsibilities

| Responsibility | Description |
|---|---|
| Session orchestration | Coordinates startup, service readiness, and cross-module lifecycle |
| Command surface unification | Exposes one voice interaction model across app and web contexts |
| Context ownership | Maintains current execution context (screen, app mode, web state) |
| UX state rendering | Uses AvanueUI components to render status/feedback/controls |
| Workflow composition | Chains voice actions into user-visible multi-step tasks |

## 6.3 Integration Topology

```text
                  ┌───────────────────────────────┐
                  │      New Avanues App Shell    │
                  └───────────────┬───────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
┌───────▼────────┐      ┌────────▼─────────┐      ┌───────▼────────┐
│ VoiceOSCore     │      │ AvanueUI          │      │ WebAvanue       │
│ (execution)     │      │ (presentation)    │      │ (web control)   │
└───────┬────────┘      └────────┬─────────┘      └───────┬────────┘
        │                         │                         │
        └─────────────────────────┴─────────────────────────┘
                                  │
                         ┌────────▼─────────┐
                         │ Shared AI/NLU/LLM │
                         │ and data services │
                         └───────────────────┘
```

## 6.4 Runtime Interaction Modes

1. **Direct command mode** — single command to single action.
2. **Guided interaction mode** — command with follow-up confirmations.
3. **Workflow mode** — sequence of dependent actions.
4. **Web-augmented mode** — command resolved against DOM-driven state.

## 6.5 Lifecycle Model

```text
App launch
  -> dependency graph init
  -> voice core init
  -> UI profile selection
  -> dynamic command bootstrap
  -> ready state
```

On teardown:

```text
stop listening
  -> flush state/events
  -> dispose module resources
  -> persist recoverable context
```

## 6.6 Cross-Module Coordination Contracts

### Contract A — Command Execution Contract
App shell submits command text + context to VoiceOSCore and receives structured result.

### Contract B — UI Feedback Contract
App shell maps command state transitions to AvanueUI components and VoiceTouch™ cues.

### Contract C — Web Command Contract
When active surface is web, app shell routes element targeting through WebAvanue integration.

## 6.7 Error Strategy in App Composition

| Error Type | UX Strategy |
|---|---|
| Recognition uncertainty | Ask user to confirm intent |
| Ambiguous targets | Offer ranked alternatives |
| Action failure | Explain failure + suggest retry path |
| Module unavailable | Graceful degradation to available capability |

## 6.8 Observability Expectations

The app composition layer should emit traceable events for:

- command receipt,
- resolver stage transitions,
- execution outcomes,
- user confirmation/cancellation,
- recovery actions.

These events enable tuning and troubleshooting of false positive/negative behavior.

## 6.9 Definition of “Good Experience”

The integrated app is considered successful when:

1. users can execute common tasks without leaving voice flow,
2. ambiguity is resolved without confusion,
3. web/native transitions remain seamless,
4. accessibility behavior remains predictable,
5. advanced features remain optional, not mandatory.
