---
title: "Chapter 07 — Design Details (VoiceTouch™ Interaction Model)"
owner: "Design + Voice Platform"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 07 — Design Details (VoiceTouch™ Interaction Model)

## 7.1 VoiceTouch™ Defined

VoiceTouch™ is the interaction contract that binds spoken intent, visual affordance, and action feedback into one coherent user experience.

It is designed for mixed-mode interaction:
- voice first,
- touch fallback,
- visual confirmation always.

## 7.2 Interaction Loop

```text
User speaks
  -> system interprets
  -> target is identified
  -> action executes
  -> user receives feedback
  -> user continues / corrects
```

## 7.3 UX States

| State | UI Expectation | Voice Expectation |
|---|---|---|
| Listening | clear active indicator | low-noise acknowledgement |
| Processing | short progress signal | avoid verbose chatter |
| Confirming | clear question + options | explicit yes/no/select path |
| Success | visible action completion | optional brief success cue |
| Error | actionable message | retry guidance |

## 7.4 Confirmation Policy

Use confirmation selectively:

1. **Required** for destructive/high-impact actions.
2. **Optional** for reversible low-risk actions.
3. **Avoid** unnecessary confirmation loops for high-frequency commands.

## 7.5 Disambiguation UX

When multiple targets match:

```text
System: "I found 3 matching targets: Submit Order, Submit Review, Submit Feedback. Say one, two, or three."
```

Guidelines:
- rank by likelihood,
- keep option list short,
- provide spoken + visual mapping,
- allow cancel without penalty.

## 7.6 Accessibility-Driven Design Rules

1. Target labels must be semantically meaningful.
2. Feedback must not rely on color alone.
3. Command failure must include correction guidance.
4. Navigation commands must remain globally available.

## 7.7 Visual Language in AvanueUI Terms

VoiceTouch UI should be built from AvanueUI primitives:
- tokenized spacing/typography,
- profile-aware layout,
- glass overlays for command/status surfaces,
- consistent status badges and pulse states.

## 7.8 Error Message Style Guide

Prefer this format:

```text
What happened -> Why -> What user can do now
```

Example:

```text
"I couldn’t find that button on this screen. It may have changed. Try saying ‘show available commands’."
```

## 7.9 Interaction Anti-Patterns

- Overly chatty assistant in fast task flows.
- Hidden execution with no visible feedback.
- Inconsistent command naming across surfaces.
- Forcing touch-only recovery for voice errors.
