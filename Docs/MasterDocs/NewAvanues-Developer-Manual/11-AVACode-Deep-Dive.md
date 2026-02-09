---
title: "Chapter 11 — AVACode Deep Dive (Forms, Workflows, Generation Status)"
owner: "Platform Engineering"
status: "active"
last_reviewed: "2026-02-09"
source_of_truth: true
---

# Chapter 11 — AVACode Deep Dive (Forms, Workflows, Generation Status)

## 11.1 Purpose

AVACode is the generation-and-automation pillar intended to transform high-level definitions into application structures and flows. In the current repository state, AVACode is **partially active**: core form/workflow foundations are enabled, while several generator/CLI/template surfaces are intentionally disabled.

This chapter documents the truthful current status so consolidation does not overstate production readiness.

## 11.2 Current Module Shape

Active module path:

- `:Modules:AVACode`

Observed subareas:

1. **forms/** (active Kotlin sources)
2. **workflows/** (active Kotlin sources)
3. **generators/** (`*.disabled`)
4. **templates/** (`*.disabled`)
5. **dsl/** (`*.disabled`)
6. **cli/** mostly `*.disabled` (with limited platform file IO bridge active in `jvmMain`)

## 11.3 Active Foundation: Forms DSL

`FormDefinition` provides:

- strict form-id validation,
- duplicate-field protection,
- field-level validation aggregation,
- completion/progress calculation,
- schema translation hook (`toSchema()`),
- runtime binding model (`bind(...)`).

Representative intent:

```text
form("user_registration")
  -> typed field definitions
  -> validation constraints
  -> completion tracking
  -> database schema derivation
```

This gives AVACode a concrete, reusable contract for structured data capture.

## 11.4 Active Foundation: Workflow DSL

`WorkflowDefinition` + instance/state types provide:

- step-based multi-stage process modeling,
- workflow/step lifecycle states,
- basic metadata policy flags (back/skip/persist/autosave),
- controlled workflow instance creation with state history.

Representative intent:

```text
workflow("user_onboarding")
  -> step("registration")
  -> step("profile_setup")
  -> conditional progression / skip policy
```

This establishes orchestration primitives for guided multi-step user journeys.

## 11.5 Disabled Surfaces (Important)

The following are currently present but disabled in source filenames:

- Code generators (Compose/SwiftUI/React TS emitters)
- Template engine/config models
- DSL parser entry points
- CLI command implementations

Interpretation:

1. Architectural direction exists and is visible.
2. Core production path today is not full end-to-end automatic code generation from AVACode alone.
3. Docs and roadmap must treat these as staged/in-progress capability, not fully active runtime surface.

## 11.6 Recommended Capability Classification

| Capability Area | Status | Notes |
|---|---|---|
| Form modeling + validation | active | usable core contract |
| Workflow state modeling | active | usable orchestration contract |
| Multi-platform code emitters | staged/disabled | source stubs disabled |
| Template generation | staged/disabled | source stubs disabled |
| CLI-driven generation | staged/disabled | mostly disabled |

## 11.7 Integration Guidance for Engineers

Use AVACode today primarily for:

1. Defining strongly-validated forms.
2. Defining reusable workflow/state machines.
3. Building higher-level orchestration over these active primitives.

Avoid assuming ready-to-ship generator pipeline until disabled surfaces are explicitly reactivated and validated.

## 11.8 Reactivation Checklist (when roadmap advances)

- [ ] Reactivate DSL parser surfaces with tests.
- [ ] Reactivate code generators with golden-output tests.
- [ ] Reactivate CLI interfaces and non-interactive build paths.
- [ ] Add compatibility matrix (Android/iOS/Web/Desktop outputs).
- [ ] Update Chapter 09 runbook with generation workflows.
- [ ] Add/refresh ADR entries for generator architecture decisions.

## 11.9 References

- `Modules/AVACode/src/commonMain/kotlin/com/augmentalis/avacode/forms/FormDefinition.kt`
- `Modules/AVACode/src/commonMain/kotlin/com/augmentalis/avacode/workflows/WorkflowDefinition.kt`
- `Modules/AVACode/` module tree status (`*.disabled` surface map)
- `Docs/MasterDocs/AVAMagic/Avanues-Suite-Master-Documentation-V1.md`
