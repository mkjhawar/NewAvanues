---
title: "Chapter 08 — Architecture Decisions and Rationale (ADR Index)"
owner: "Architecture Group"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 08 — Architecture Decisions and Rationale (ADR Index)

This chapter captures key platform decisions and why they were made, so future changes remain intentional rather than accidental.

---

## ADR-001 — Voice-first as primary interaction model

**Decision:** Treat voice as primary command channel; touch is fallback/complement.

**Why:** Accessibility and hands-free workflows require first-class voice, not add-on shortcuts.

**Trade-off:** Higher demand on command disambiguation and feedback design.

---

## ADR-002 — Layered architecture (AVA -> VoiceOSCore -> surface executors)

**Decision:** Separate understanding from execution.

**Why:** Keeps intent interpretation adaptable while preserving deterministic execution paths.

**Trade-off:** More interfaces/contracts to maintain.

---

## ADR-003 — Priority resolver before heavy AI fallback

**Decision:** Exact/fuzzy/handler resolution runs before NLU/LLM fallback.

**Why:** Lower latency and reduced resource cost on common command paths.

**Trade-off:** Requires disciplined command registry quality.

---

## ADR-004 — Multi-engine speech abstraction

**Decision:** Support multiple speech backends under one interface.

**Why:** Different deployments need different balances of privacy, offline capability, language coverage, and enterprise integrations.

**Trade-off:** Cross-engine behavior normalization complexity.

---

## ADR-005 — AvanueUI as canonical design system dependency

**Decision:** Converge UI design and components around `:Modules:AvanueUI`.

**Why:** Eliminates style drift and duplicated component logic across modules.

**Trade-off:** Migration overhead from legacy imports/components.

---

## ADR-006 — Display-profile aware rendering

**Decision:** Use profile-driven density/layout strategies (phone/tablet/glass variants).

**Why:** VoiceTouch™ interactions require predictable target sizing and readability on diverse hardware.

**Trade-off:** Need profile verification in QA matrix.

---

## ADR-007 — WebAvanue DOM bridge for web execution

**Decision:** Resolve web commands through DOM scraping and VUID-mapped actions.

**Why:** Preserves voice-first behavior in browser surfaces where native accessibility trees are insufficient.

**Trade-off:** DOM volatility and ambiguity handling burden.

---

## ADR-008 — Archive-first documentation cleanup

**Decision:** Move superseded docs to archive before deletion.

**Why:** Prevents irreversible loss of historical decisions and edge-case knowledge.

**Trade-off:** Temporary documentation footprint remains larger until post-validation deletion.

---

## ADR-009 — Canonical developer manual with chaptered growth model

**Decision:** Establish a single canonical manual with stable chapter numbering.

**Why:** Reduces duplication/conflict and improves onboarding continuity.

**Trade-off:** Requires active ownership discipline to keep canonical docs up to date.

---

## ADR Governance Rules

1. New architectural decision -> add/update ADR entry.
2. Deprecated decision -> mark superseded with replacement ADR.
3. Every major module refactor must reference impacted ADRs.
