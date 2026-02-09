---
title: "NewAvanues Developer Manual (Canonical)"
owner: "Platform Engineering"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
replaces:
  - "Scattered module READMEs and legacy architecture summaries"
---

# NewAvanues Developer Manual

> **Canonical Developer Manual** for the NewAvanues ecosystem.
> Scope: **VoiceOS®**, **VoiceTouch™**, **VoiceOSCore**, **AvanueUI**, **WebAvanue**, and the consolidated **New Avanues App**.

This manual is designed as a long-lived, chaptered source of truth with expandable structure. Future chapters should be added with stable numbering and linked here.

---

## Table of Contents

### Part I — Foundation
1. [Platform Vision, Terminology, and Principles](./01-Platform-Vision-And-Principles.md)
2. [System Architecture and End-to-End Flows](./02-System-Architecture-And-Flows.md)

### Part II — Core Technical Deep Dives
3. [VoiceOSCore Deep Dive (Voice Execution Engine)](./03-VoiceOSCore-Deep-Dive.md)
4. [AvanueUI Deep Dive (Design System + Glass UI)](./04-AvanueUI-Deep-Dive.md)
5. [WebAvanue Deep Dive (Voice-Controlled Browser Layer)](./05-WebAvanue-Deep-Dive.md)
6. [New Avanues App Deep Dive (Composition + Integration)](./06-NewAvanues-App-Composition.md)

### Part III — Experience and Governance
7. [Design Details (VoiceTouch™ Interaction Model)](./07-Design-Details-VoiceTouch-UX.md)
8. [Architecture Decisions and Rationale (ADR Index)](./08-Decisions-ADR-Index.md)
9. [Developer Runbook (Build, Debug, Extend)](./09-Development-Runbook.md)

---

## How to Use This Manual

- **New engineers**: Read Chapters 1 → 2 → 9 first.
- **Feature engineers**: Read 3/4/5/6 based on ownership area.
- **Architects/tech leads**: Use Chapter 8 to understand why major choices were made.
- **Design/UX collaborators**: Use Chapter 7 + Chapter 4 together.

---

## Documentation Rules

1. This index is canonical and must always reflect latest chapter set.
2. Additions should preserve numbering continuity (e.g., new chapter after 9 becomes 10-*).
3. Design and architecture changes must update:
   - Relevant deep-dive chapter(s)
   - Chapter 8 (ADR section)
   - Chapter 9 runbook if developer workflows change

---

## Current Product Statement

**Voice-first, any app, any time — including web — powered by VoiceOS® and VoiceTouch™.**

The NewAvanues stack unifies voice understanding, command execution, and adaptive UI rendering across mobile and web experiences, with NLU/AI at the center.
