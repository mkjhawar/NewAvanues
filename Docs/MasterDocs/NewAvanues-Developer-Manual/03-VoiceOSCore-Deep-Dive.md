---
title: "Chapter 03 — VoiceOSCore Deep Dive"
owner: "Voice Platform"
status: "active"
last_reviewed: "2026-02-10"
source_of_truth: true
---

# Chapter 03 — VoiceOSCore Deep Dive

## 3.1 Purpose

VoiceOSCore is the deterministic execution kernel of the voice platform. It receives interpreted command text and transforms it into concrete actions through handler orchestration.

## 3.2 Core Roles

| Component | Responsibility |
|---|---|
| `VoiceOSCoreNG` facade | Lifecycle + command processing API |
| `ActionCoordinator` | Multi-stage command resolution |
| `HandlerRegistry` | Priority-based handler routing |
| `CommandRegistry` | Dynamic, screen-aware command set |
| `StaticCommandRegistry` | Predefined command corpus |

## 3.3 Command Resolution Pipeline

```text
Voice Text
  -> exact phrase resolution
  -> fuzzy match
  -> static handler route
  -> NLU semantic classification
  -> LLM fallback interpretation
  -> failure/clarification
```

### Why this ordering
- Keeps common commands fast and deterministic.
- Reduces unnecessary model invocation.
- Preserves long-tail natural language support.

## 3.4 Handler System Architecture

Implemented categories include (not exhaustive):

- System (home/back/notifications)
- Navigation (scroll/swipe)
- UI interaction (click/focus/long-press)
- Input (type/clear/select/copy/paste)
- App launch/switch
- Media/device/accessibility/custom categories

## 3.5 Dynamic Command Lifecycle

```text
Screen changes
  -> discover candidate target elements
  -> generate/refresh dynamic commands
  -> optionally update speech engine grammar
  -> route utterances against current screen context
```

This is key for context-aware command precision.

## 3.6 Speech Engine Strategy

VoiceOSCore integrates multiple engines (Android STT, Google/Azure cloud, Vivoka, Vosk, Apple Speech), abstracted behind a consistent speech interface.

Design result:
- Pluggable engine choice by environment.
- Offline/privacy options.
- Custom vocabulary and grammar updates where supported.

## 3.7 State Model

Typical service progression:

```text
Uninitialized -> Initializing -> Ready -> Listening -> Processing -> Ready
                                     \-> Error
```

`StateFlow`/`Flow` exposure enables UI feedback and diagnostics integration.

## 3.8 Integration Contract

### Required from host app
1. Lifecycle ownership (`initialize`/`dispose`).
2. Platform executor wiring (e.g., Android accessibility service).
3. Speech result and command result handling for UX feedback.

### Optional but recommended
1. NLU model integration.
2. LLM fallback integration.
3. Dynamic command updates per screen transition.

## 3.9 Failure Modes and Recovery

| Failure Class | Recovery Pattern |
|---|---|
| Low confidence recognition | reprompt + alternatives |
| Multiple UI matches | disambiguation prompt |
| Handler unavailable | category fallback / not handled |
| Platform action failure | retry with alternate action path |
| Model unavailable | degrade to deterministic command set |

## 3.10 Practical Extension Pattern

To add a new command category:

1. Add/extend action type.
2. Implement new handler with explicit `supportedActions`.
3. Register handler in registry with intentional priority.
4. Add static/dynamic command phrases.
5. Add test cases for ambiguity and fallback behavior.
