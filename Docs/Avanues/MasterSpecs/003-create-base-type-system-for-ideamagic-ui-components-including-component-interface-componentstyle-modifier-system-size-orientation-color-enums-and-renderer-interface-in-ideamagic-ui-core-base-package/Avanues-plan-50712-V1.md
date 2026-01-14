# IDEAMagic Base Type System - Implementation Plan

**Feature ID:** 003
**Created:** 2025-11-05T08:54:21.098Z
**Updated:** 2025-11-05T09:15:00.000Z
**Profile:** library (KMP)
**Estimated Effort:** 8-10 hours (1.5 days)
**Complexity Tier:** 2

---

## Executive Summary

Implement foundational type system for IDEAMagic UI component library including:
- **Component interface** - Base contract for all UI elements
- **ComponentStyle** - Unified styling system with composition support
- **Modifier system** - Decorator pattern for component behaviors (6 modifiers)
- **Type-safe enums** - Size, Orientation, Color, Position, Alignment, Severity (6 enums)
- **Renderer interface** - Cross-platform rendering abstraction
- **Supporting types** - Padding, Margin, Animation, DragEvent

**Goal:** Unblock restoration of 15 removed components and enable implementation of 56 missing components for Flutter/SwiftUI parity.

**Location:** `Universal/IDEAMagic/UI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/base/`

**Targets:** JVM, Android, iOS (Kotlin Multiplatform)

---

## Architecture Overview

### Components


#### Public API
**Responsibility:** External-facing API surface
**Dependencies:** Core


#### Core
**Responsibility:** Core functionality and algorithms
**Dependencies:** Utilities


#### Utilities
**Responsibility:** Helper functions and common utilities
**Dependencies:** None


### Data Flow

```
Public API → Core logic → Utilities → Return value
```

### Integration Points

- Existing public APIs
- Version compatibility
- Platform interfaces

---

## Implementation Phases


### Phase 1: Core Interfaces

**Duration:** 1-1.5 hours
**Complexity:** Tier 1

**Location:** `Universal/IDEAMagic/UI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/base/`

**Tasks:**
- [ ] **T1.1** - Create package structure: `base/`, `base/enums/`, `base/types/`, `base/modifiers/` (0.25h)
- [ ] **T1.2** - Implement `Component.kt` interface with id, style, modifiers, render() (0.5h)
  ```kotlin
  interface Component {
      val id: String? get() = null
      val style: ComponentStyle? get() = null
      val modifiers: List<Modifier> get() = emptyList()
      fun render(renderer: Renderer): Any
  }
  ```
- [ ] **T1.3** - Implement `Renderer.kt` interface with renderComponent() and withContext() (0.25h)
- [ ] **T1.4** - Add KDoc documentation for Component and Renderer (0.25h)
- [ ] **T1.5** - Write unit tests for interface default implementations (0.25h)

**Agents Required:**
- kotlin-expert (Kotlin interfaces and default implementations)
- architect (API design)

**Quality Gates:**
- [ ] Component interface compiles on all targets (JVM, Android, iOS)
- [ ] Renderer interface compiles on all targets
- [ ] KDoc coverage 100% for public APIs
- [ ] Tests pass for default implementations
- [ ] No compiler warnings

**Risks:**

- **Risk:** Default implementations may not work on all KMP targets
  - **Mitigation:** Test compilation on JVM, Android, iOS targets immediately
  - **Contingency:** Make properties abstract if defaults don't compile



---

### Phase 2: Styling System

**Duration:** 1.5-2 hours
**Complexity:** Tier 1

**Location:** `Universal/IDEAMagic/UI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/base/`

**Tasks:**
- [ ] **T2.1** - Implement `types/Padding.kt` data class with convenience constructors (0.25h)
- [ ] **T2.2** - Implement `types/Margin.kt` data class with convenience constructors (0.25h)
- [ ] **T2.3** - Implement `ComponentStyle.kt` data class with 8 properties (padding, margin, backgroundColor, etc.) (0.5h)
- [ ] **T2.4** - Implement `ComponentStyle.merge()` function for style composition (0.25h)
- [ ] **T2.5** - Implement `ComponentStyle.plus()` operator for style merging (0.1h)
- [ ] **T2.6** - Add KDoc documentation for all styling types (0.25h)
- [ ] **T2.7** - Write unit tests for style composition and merging (0.5h)

**Agents Required:**
- kotlin-expert (Data classes, operator overloading)
- api-design-expert (Style composition patterns)

**Quality Gates:**
- [ ] ComponentStyle compiles on all targets
- [ ] Style merging works correctly (non-null values override)
- [ ] Padding/Margin convenience constructors work
- [ ] KDoc coverage 100%
- [ ] Tests cover all merge scenarios
- [ ] No compiler warnings

**Risks:**

- **Risk:** Style merging logic becomes complex with nested nulls
  - **Mitigation:** Use simple "non-null wins" rule for merging
  - **Contingency:** Add merge strategy parameter if needed

---

### Phase 3: Modifier System

**Duration:** 1.5-2 hours
**Complexity:** Tier 2

**Location:** `Universal/IDEAMagic/UI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/base/`

**Tasks:**
- [ ] **T3.1** - Implement `Modifier.kt` sealed interface with then() method (0.25h)
- [ ] **T3.2** - Implement `modifiers/Clickable.kt` data class (0.1h)
- [ ] **T3.3** - Implement `modifiers/Draggable.kt` data class (0.1h)
- [ ] **T3.4** - Implement `modifiers/Focusable.kt` data class (0.1h)
- [ ] **T3.5** - Implement `modifiers/Testable.kt` data class (0.1h)
- [ ] **T3.6** - Implement `modifiers/Accessible.kt` data class (0.1h)
- [ ] **T3.7** - Implement `modifiers/Animated.kt` data class (0.1h)
- [ ] **T3.8** - Implement private `CombinedModifier` for chaining (0.15h)
- [ ] **T3.9** - Add KDoc documentation for all modifiers (0.25h)
- [ ] **T3.10** - Write unit tests for modifier chaining (0.5h)

**Agents Required:**
- kotlin-expert (Sealed interfaces, lambda functions)
- architecture-expert (Decorator pattern)

**Quality Gates:**
- [ ] All 6 modifiers compile on all targets
- [ ] Modifier chaining works (then() method)
- [ ] Lambda functions don't cause KMP issues
- [ ] KDoc coverage 100%
- [ ] Tests cover modifier combinations
- [ ] No compiler warnings

**Risks:**

- **Risk:** Lambda functions in data classes may not serialize/deserialize
  - **Mitigation:** Document that modifiers are runtime-only (not for serialization)
  - **Contingency:** Add @Transient annotations if serialization needed

---

### Phase 4: Enums & Supporting Types

**Duration:** 1.5-2 hours
**Complexity:** Tier 1

**Location:** `Universal/IDEAMagic/UI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/base/`

**Tasks:**
- [ ] **T4.1** - Implement `enums/Size.kt` enum (XS, SM, MD, LG, XL) (0.1h)
- [ ] **T4.2** - Implement `enums/Orientation.kt` enum (HORIZONTAL, VERTICAL) (0.1h)
- [ ] **T4.3** - Implement `enums/Color.kt` enum with RGB values (0.25h)
- [ ] **T4.4** - Implement `enums/Position.kt` enum (9 positions) (0.15h)
- [ ] **T4.5** - Implement `enums/Alignment.kt` enum (6 alignments) (0.15h)
- [ ] **T4.6** - Implement `enums/Severity.kt` enum (5 levels) (0.1h)
- [ ] **T4.7** - Implement `types/Animation.kt` data class with AnimationType and Easing enums (0.25h)
- [ ] **T4.8** - Implement `types/DragEvent.kt` data class (0.1h)
- [ ] **T4.9** - Add KDoc documentation for all enums and types (0.25h)
- [ ] **T4.10** - Write unit tests for enum exhaustiveness (0.25h)

**Agents Required:**
- kotlin-expert (Enums, when expressions)

**Quality Gates:**
- [ ] All 6 enums compile on all targets
- [ ] Color enum has RGB values for all colors
- [ ] Animation and DragEvent data classes compile
- [ ] KDoc coverage 100%
- [ ] Tests verify when expressions are exhaustive
- [ ] No compiler warnings

**Risks:**

- **Risk:** Color RGB values may be platform-specific
  - **Mitigation:** Use standard hex string format (#RRGGBB)
  - **Contingency:** Add platform-specific color conversion utilities



---

### Phase 5: Testing & Quality

**Duration:** 1.5-2 hours
**Complexity:** Tier 1

**Location:** `Universal/IDEAMagic/UI/Core/src/commonTest/kotlin/com/augmentalis/avamagic/ui/core/base/`

**Tasks:**
- [ ] **T5.1** - Write unit tests for Component interface default implementations (0.25h)
- [ ] **T5.2** - Write unit tests for ComponentStyle merge() and plus() (0.5h)
- [ ] **T5.3** - Write unit tests for Padding and Margin constructors (0.25h)
- [ ] **T5.4** - Write unit tests for Modifier chaining (then() method) (0.25h)
- [ ] **T5.5** - Write unit tests for all 6 modifiers (0.25h)
- [ ] **T5.6** - Write unit tests for enum exhaustiveness (when expressions) (0.25h)
- [ ] **T5.7** - Write unit tests for Animation and DragEvent (0.1h)
- [ ] **T5.8** - Verify test coverage ≥ 90% using Gradle coverage report (0.25h)
- [ ] **T5.9** - Run tests on all targets (JVM, Android, iOS) (0.25h)
- [ ] **T5.10** - Fix any failing tests or compilation issues (0.5h)

**Agents Required:**
- test-specialist (Unit testing, coverage analysis)
- kotlin-expert (KMP testing)

**Quality Gates:**
- [ ] Test coverage ≥ 90% (target 95%)
- [ ] All tests passing on JVM target
- [ ] All tests passing on Android target
- [ ] All tests passing on iOS target
- [ ] No critical bugs
- [ ] No flaky tests

**Risks:**

- **Risk:** Tests fail on iOS target due to lambda serialization
  - **Mitigation:** Test on iOS early in development
  - **Contingency:** Mark iOS-specific tests as @IgnoreIos if needed

- **Risk:** Test coverage below 90% threshold
  - **Mitigation:** Write tests alongside implementation (TDD approach)
  - **Contingency:** Add edge case tests to reach threshold



---

### Phase 6: Documentation & Polish

**Duration:** 1-1.5 hours
**Complexity:** Tier 1

**Locations:**
- Code: `Universal/IDEAMagic/UI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/ui/core/base/`
- Docs: `docs/COMPONENT-BASE-TYPES-COMPLETE-[TIMESTAMP].md`
- Architecture: `docs/ARCHITECTURE-BASE-TYPE-SYSTEM.md`

**Tasks:**
- [ ] **T6.1** - Verify 100% KDoc coverage for all public APIs (0.25h)
- [ ] **T6.2** - Create usage examples document showing Component implementation (0.25h)
- [ ] **T6.3** - Create migration guide for converting data classes to Components (0.25h)
- [ ] **T6.4** - Update architecture docs with base type system diagrams (0.25h)
- [ ] **T6.5** - Create status document: `docs/COMPONENT-BASE-TYPES-COMPLETE-[TIMESTAMP].md` (0.25h)
- [ ] **T6.6** - Update gap analysis to reflect base types completion (0.1h)
- [ ] **T6.7** - Code cleanup: Remove any unused imports, fix formatting (0.15h)

**Agents Required:**
- documentation-specialist (KDoc, guides, examples)
- architecture-expert (Architecture docs, diagrams)

**Quality Gates:**
- [ ] KDoc coverage 100% for all public APIs
- [ ] Usage examples compile and run
- [ ] Migration guide is clear and actionable
- [ ] Architecture docs updated
- [ ] Status document created
- [ ] Gap analysis updated
- [ ] Code passes ktlint/detekt

**Deliverables:**
1. `docs/COMPONENT-BASE-TYPES-COMPLETE-[TIMESTAMP].md` - Implementation status
2. `docs/ARCHITECTURE-BASE-TYPE-SYSTEM.md` - Architecture documentation
3. `docs/examples/ComponentUsageExamples.kt` - Working code examples
4. `docs/guides/MIGRATION-TO-COMPONENT-INTERFACE.md` - Migration guide




---

## Technical Decisions


### Architecture Pattern

**Options Considered:**
1. **Component-based**
   - Pros: Established pattern, Team familiarity, Good separation of concerns
   - Cons: May be overkill for simple features

**Selected:** Component-based

**Rationale:** Aligns with project architecture and team expertise


---

## Dependencies

### Internal Dependencies

_None identified_

### External Dependencies

_None identified_

---

## Quality Gates (Profile: library)

- **Test Coverage:** ≥ 90% (target: 95%)
- **Build Time:** ≤ 120 seconds
- **Documentation:** all
- **Review Required:** Yes

---

## Success Criteria

- [ ] All base types compile in Core module (JVM + Android + iOS)
- [ ] Component interface can be implemented by data classes
- [ ] ComponentStyle supports composition and merging
- [ ] Modifier system supports chaining
- [ ] All enums have complete value sets
- [ ] Renderer interface supports all platforms
- [ ] 15 removed components can be restored using these types
- [ ] Unit tests pass with >90% coverage
- [ ] KDoc documentation complete
- [ ] Example usage provided for each type

---

## Next Steps

1. Review this plan for completeness
2. Run `ideacode_implement` to execute the plan
3. Or use `/ideacode.implement` for manual implementation with guidance

---

**Generated:** Autonomously by IDEACODE MCP Server
**Last Updated:** 2025-11-05T08:54:21.098Z
