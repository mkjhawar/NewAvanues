---
title: "Chapter 09 — Developer Runbook (Build, Debug, Extend)"
owner: "Developer Experience"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 09 — Developer Runbook (Build, Debug, Extend)

## 9.1 Goal of this Runbook

Provide a pragmatic operating guide for contributors who need to build, run, debug, and extend the NewAvanues stack safely.

## 9.2 Baseline Orientation

- Monorepo root build orchestration uses Gradle Kotlin DSL.
- Module graph includes VoiceOSCore, AvanueUI, WebAvanue, AI modules, and app shells.
- Consolidated app path includes `apps:avanues`.

## 9.3 Typical Developer Workflow

```text
1) Pull latest branch
2) Sync Gradle / dependencies
3) Build affected modules
4) Run target app/surface
5) Verify voice command path
6) Add/adjust tests
7) Update manual + ADR if architecture changed
```

## 9.4 Build and Verification Checklist

- [ ] Repository sync and dependency resolution successful
- [ ] Affected modules compile in isolation and in aggregate
- [ ] Voice command critical path tested (recognition -> execution -> feedback)
- [ ] UI validated in at least two display profiles
- [ ] Web command flow validated for key paths if web changes are included

## 9.5 Debugging by Layer

### Speech Layer
Symptoms: command not recognized or low confidence.

Actions:
1. confirm active speech engine,
2. inspect confidence and alternatives,
3. verify grammar update for dynamic commands.

### Resolver Layer (VoiceOSCore)
Symptoms: recognized text but wrong/no action.

Actions:
1. inspect resolver stage reached,
2. verify command registry entry quality,
3. inspect handler routing and action category support.

### Surface Layer (Native/Web)
Symptoms: command resolved but execution fails.

Actions:
1. verify target element still discoverable,
2. check accessibility/DOM selector changes,
3. retry via alternate action path.

### UX Layer (VoiceTouch™/AvanueUI)
Symptoms: action happened but user uncertain.

Actions:
1. ensure feedback state is visible,
2. tighten confirmation/disambiguation prompts,
3. ensure consistent status components.

## 9.6 Extension Playbooks

### Add new voice command behavior
1. define action type and expected semantics,
2. add phrase mapping (static and/or dynamic generation),
3. implement handler logic,
4. add fallback/disambiguation rules,
5. add tests and update relevant chapter(s).

### Add new UI feature with voice affordance
1. build using AvanueUI tokens/components,
2. ensure semantic labels are voice-meaningful,
3. verify command discoverability,
4. validate profile behavior on multiple form factors.

### Extend web action coverage
1. enrich DOM extraction metadata,
2. add command matching rules for new patterns,
3. validate ambiguous page states,
4. add error-safe fallback.

## 9.7 Documentation Change Rules

If your change affects behavior, update at least one of:

- Chapter 03 (execution behavior)
- Chapter 04 (design-system behavior)
- Chapter 05 (web behavior)
- Chapter 06 (app composition behavior)
- Chapter 08 (decision rationale)

## 9.8 Ready-to-Merge Gate

A change is ready only if:

1. implementation is complete,
2. critical path is tested,
3. regression risk is documented,
4. manual/ADR updates are included.
