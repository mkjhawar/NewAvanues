<!--
SYNC IMPACT REPORT
==================
Version Change: [template] → 1.0.0
Modified Principles: Initial creation - all principles are new
Added Sections:
  - Core Principles (5 principles)
  - Technical Constraints
  - Development Workflow
  - Governance
Removed Sections: None (replaced template)
Templates Status:
  ✅ .ideacode/templates/plan-template.md (Constitution Check section references this)
  ✅ .ideacode/templates/spec-template.md (Aligned with user story priorities)
  ✅ .ideacode/templates/tasks-template.md (Aligned with task organization)
  ✅ CLAUDE.md (IDEACODE workflow already documented)
Follow-up TODOs: None - constitution complete and ready
-->

# VOS4 Constitution

## Core Principles

### I. Performance-First Architecture

All design decisions MUST prioritize performance and responsiveness over abstraction:

- Command processing latency MUST be <100ms
- Module initialization MUST be <50ms
- Memory footprint MUST be <60MB (Vivoka), <30MB (Vosk)
- Battery consumption MUST be <2% per hour active use
- XR rendering MUST maintain 90-120 FPS
- Direct implementation preferred over abstraction layers
- Performance is a feature, not an optimization

**Rationale**: Voice control requires real-time responsiveness. Users expect instant
feedback when speaking commands. Delays break the illusion of natural interaction.
Performance constraints are especially critical for accessibility users who depend on
voice control as their primary interaction method.

### II. Direct Implementation (No Interfaces)

Code MUST use concrete implementations directly unless strategic value is documented:

- NO interfaces by default (ZERO TOLERANCE)
- Exceptions MUST be explicitly documented with rationale in architecture docs
- Performance and debugging clarity take precedence over abstraction
- SOLID principles applied without interface overhead
- When multi-implementation scenarios are proven necessary, interfaces can be added
- Document the specific scenario requiring abstraction

**Rationale**: Interfaces add cognitive overhead, reduce performance, and complicate
debugging without proven benefit in most cases. VOS4's 50,000+ LOC codebase demonstrates
that direct implementation yields clearer, faster, more maintainable code. Strategic
interfaces are allowed when justified, but the burden of proof is on abstraction.

### III. Privacy & Accessibility First

Voice processing MUST be private, secure, and universally accessible:

- On-device speech recognition as default (Whisper.cpp, Vosk)
- Cloud services (Google Cloud STT, Azure STT) only with explicit user consent
- Support for 42+ languages (Vivoka), 8 languages (Vosk)
- Designed primarily for users with mobility limitations
- Smart glasses integration for XR accessibility (8+ brands supported)
- All UI must be voice-navigable without touch input
- Privacy-preserving AI context inference (on-device processing)

**Rationale**: VOS4 serves users who depend on voice control for accessibility. Privacy
violations or accessibility barriers are not acceptable trade-offs for features. Voice
data is sensitive, and users must maintain control over where processing occurs.

### IV. Modular Independence

Modules MUST be self-contained and independently deployable:

- Each module contains all its components (no cross-module dependencies for core functionality)
- Independent build and test capability
- Clear module boundaries maintained (20 modules: 5 apps, 9 libraries, 5 managers)
- Namespace: `com.augmentalis.*` (MANDATORY - no exceptions)
- Module communication through well-defined contracts
- Each module can be versioned independently

**Rationale**: VOS4's 20-module architecture enables parallel development, simplifies
testing, reduces coupling, and allows selective updates. The `com.augmentalis.*`
namespace ensures professionalism and consistency across all modules.

### V. Quality Through Enforcement

Quality MUST be enforced by automated gates, not encouraged through guidelines:

- Testing is MANDATORY (80%+ coverage, BLOCKS merge if tests fail)
- Subagents enforce quality automatically:
  - @vos4-test-specialist (auto-invoked, blocks on failure)
  - @vos4-documentation-specialist (auto-invoked, ensures docs updated)
  - @vos4-architecture-reviewer (validates design decisions)
- IDE Loop required for Tier 3 features (Implement → Defend → Evaluate → Commit)
- Zero compiler warnings in production code (ZERO TOLERANCE)
- All quality gates must pass before merge (no exceptions)

**Rationale**: High-quality voice control is critical for accessibility users. Manual
quality processes are inconsistent and fail under pressure. Automation removes human
error, ensures consistency, and maintains standards even during rapid development.
The 496+ test suite with 80%+ coverage proves this approach works.

## Technical Constraints

### Platform & Technology Stack

**Mandatory Technologies:**

- **Language**: Kotlin 1.9.25 + Java 17
- **Platform**: Android API 29-34 (Android 10-15)
- **Build System**: Gradle 8.11.1 with Kotlin DSL
- **Database**: Room v7 with KSP (NOT ObjectBox - legacy only)
- **Dependency Injection**: Hilt 2.51.1
- **UI Framework**: Jetpack Compose 1.6.8 + Material Design 3 v1.2.1
- **Async Programming**: Kotlin Coroutines + Flow
- **Namespace**: `com.augmentalis.*` (ZERO TOLERANCE - no `com.ai.*`)

**Speech Recognition Stack:**

- Whisper.cpp (on-device, privacy-first, primary)
- Vosk SDK (offline, 8 languages, free)
- Vivoka SDK (premium, 42+ languages)
- Google Cloud STT (fallback, requires consent)
- Azure STT (experimental)

### Quality Gates

All features MUST pass these gates before merging:

1. **Architecture Gate**: Direct implementation pattern verified (no unapproved interfaces)
2. **Testing Gate**: 80%+ coverage, all tests passing (BLOCKS if failed)
3. **Performance Gate**: Meets latency/memory/battery budgets
4. **Namespace Gate**: All code uses `com.augmentalis.*`
5. **Documentation Gate**: Module docs updated with diagrams
6. **Subagent Gate**: @vos4-test-specialist and @vos4-documentation-specialist approval

### Development Standards

**Naming Conventions (MANDATORY):**

- Kotlin/Java files: `PascalCase.kt`
- Documentation: `PascalCase-With-Hyphens-YYMMDD-HHMM.md`
- Packages: `lowercase.dot.separated`
- Classes: `PascalCase`
- Methods: `camelCase`
- Constants: `SCREAMING_SNAKE_CASE`
- Modules: `PascalCase` (code and docs must match exactly)

**Testing Requirements:**

- Test Pyramid: 70% unit / 25% integration / 5% E2E
- Frameworks: JUnit 5, MockK, Robolectric, Espresso
- Performance benchmarks included for critical paths
- All tests must be deterministic and fast (<5s per suite)
- Tests must not depend on external services
- Mocking preferred over test databases where appropriate

**Documentation Requirements:**

- All modules must have developer manual and user manual
- Architecture changes require updated diagrams
- All public APIs require inline documentation
- Living docs (notes, decisions, bugs, progress, backlog) updated during development
- Timestamped status reports for significant changes

### Performance Budgets

**Hard Limits (ZERO TOLERANCE):**

- Command processing latency: <100ms (95th percentile)
- Module initialization: <50ms per module
- Memory footprint: <60MB (Vivoka), <30MB (Vosk)
- Battery consumption: <2% per hour active use
- XR rendering: 90-120 FPS maintained
- App startup time: <1 second to ready state
- UI scraping with hash-based dedup: <50ms per screen

**Measurement:**

- Performance benchmarks in test suite
- Profiling before merging performance-critical code
- Memory leak detection with LeakCanary
- Battery monitoring during manual testing

## Development Workflow

### 3-Tier Complexity-Based Approach

VOS4 uses a tiered approach based on task complexity:

#### Tier 1: Direct Implementation (<30 minutes, simple tasks)

**When to use:**
- Single file modifications
- Bug fixes with known solution
- Documentation updates
- Simple refactoring

**Process:**
1. Make changes directly
2. Write tests if applicable
3. Update relevant docs
4. Commit with conventional commit format

**Example**: "Fix null pointer in VoiceCommandProcessor.kt line 42"

**Skip**: Formal planning, subagents, IDEACODE workflow

#### Tier 2: Subagent-Assisted (1-3 hours, medium complexity) ⭐ RECOMMENDED

**When to use:**
- 2-3 module changes
- Known technical approach
- Medium scope features

**Process:**
1. Call `@vos4-orchestrator` with task description
2. Orchestrator delegates to specialist agents:
   - @vos4-android-expert (Android platform)
   - @vos4-kotlin-expert (Kotlin/coroutines)
   - @vos4-database-expert (Room/KSP)
   - @vos4-performance-analyzer (optimization)
3. @vos4-test-specialist **automatically invoked** (BLOCKS if tests fail)
4. @vos4-documentation-specialist **automatically updates docs**
5. @vos4-architecture-reviewer validates design

**Example**: "@vos4-orchestrator Implement DatabaseManagerImpl TODO #3"

**Quality Enforcement**: Automatic testing and documentation - no manual gates to forget

#### Tier 3: Full IDEACODE Workflow (>4 hours, complex features)

**When to use:**
- Multiple modules affected
- High risk or unfamiliar domain
- New architectural patterns
- Complex feature development

**Mandatory IDEACODE Process:**

1. **`/idea.principles`** - Verify alignment with this constitution (already done!)

2. **`/idea.specify <feature>`** - Define requirements (WHAT/WHY)
   - Creates: `specs/###-feature-name/spec.md`
   - Technology-agnostic requirements
   - User stories with priorities (P1, P2, P3)
   - Success criteria
   - Acceptance tests

3. **`/idea.clarify`** - Resolve ambiguities (OPTIONAL)
   - Q&A session for unclear requirements
   - Stakeholder alignment
   - Edge case exploration

4. **`/idea.plan`** - Create implementation plan (HOW)
   - Creates: `specs/###-feature-name/plan.md`
   - Technical approach with phases
   - Architecture decisions
   - **Constitution Check** (mandatory section)
   - Data models and contracts

5. **`/idea.tasks`** - Generate task breakdown
   - Creates: `specs/###-feature-name/tasks.md`
   - Actionable tasks by priority
   - Effort estimates
   - Dependencies mapped

6. **`/idea.implement`** - Execute with IDE Loop
   - **FOR EACH PHASE:**
     - **I**mplement → Write code following standards
     - **D**efend → Write tests (MANDATORY, 80%+ coverage, @vos4-test-specialist enforces)
     - **E**valuate → Verify acceptance criteria met
     - **Commit** → Lock in progress (atomic commits)

7. **`/idea.analyze`** - Verify compliance (OPTIONAL)
   - Constitution alignment check
   - Code quality verification
   - Performance validation

8. **`/idea.checklist`** - Final validation (OPTIONAL)
   - Requirements coverage
   - Documentation completeness
   - All quality gates passed

**Example**: "Add multi-language context-aware command routing with AI inference"

### Git Workflow

**Branch Strategy:**
- `main` - Production-ready code only
- `feature/###-feature-name` - Feature branches (matches spec number)
- `bugfix/description` - Bug fixes
- `refactor/description` - Refactoring work

**Commit Standards:**
- Format: Conventional Commits (feat:, fix:, docs:, refactor:, test:, perf:)
- Stage by category: docs → code → tests (NEVER mixed in one commit)
- Functional equivalency verified (unless explicitly changing behavior)
- NO AI/Claude/Anthropic references in commit messages

**Example Commits:**
```
feat(CommandManager): Add context-aware routing with confidence thresholds
test(CommandManager): Add 15 tests for context inference engine
docs(CommandManager): Update architecture diagram with routing flow
```

### Living Documentation (Continuous Updates)

**5 Living Documents** in `docs/ProjectInstructions/`:

1. **`notes.md`** - Implementation insights
   - Quick TODOs
   - Gotchas discovered
   - Useful patterns

2. **`decisions.md`** - Architectural decisions
   - Context, Decision, Rationale, Consequences
   - Date-stamped entries

3. **`bugs.md`** - Known issues tracker
   - Severity: P0 (critical), P1 (high), P2 (medium), P3 (low)
   - Status, reproduction steps, workarounds

4. **`progress.md`** - Sprint tracking
   - Current goals
   - Completed/in-progress/blocked tasks

5. **`backlog.md`** - Future work
   - Prioritized features (high/medium/low)

**Update Rule**: These MUST be updated during development, not after

### Code Review Requirements

All pull requests MUST be reviewed for:

- ✅ Alignment with approved specification (Tier 3 only)
- ✅ Direct implementation pattern (no unapproved interfaces)
- ✅ `com.augmentalis.*` namespace compliance
- ✅ Tests passing with 80%+ coverage
- ✅ Documentation updated (diagrams included)
- ✅ Performance within budgets
- ✅ Subagent approvals obtained (@vos4-test-specialist, @vos4-documentation-specialist)
- ✅ Zero compiler warnings
- ✅ Functional equivalency verified (or behavioral changes documented)

## Governance

### Amendment Process

This constitution can be amended through the following process:

1. **Proposal** - Document proposed change with rationale
   - Why is the change needed?
   - What problem does it solve?
   - What's the impact on existing work?

2. **Impact Analysis** - Review effect on:
   - Existing features and code
   - Active development work
   - Templates and workflows
   - Subagent configurations

3. **Version Determination** - Semantic versioning rules:
   - **MAJOR (X.0.0)**: Backward incompatible (remove/redefine principles)
   - **MINOR (1.X.0)**: New principles or sections added
   - **PATCH (1.0.X)**: Clarifications, wording fixes, non-semantic changes

4. **Update Execution** - Update this document
   - Modify principle text
   - Update version number
   - Update "Last Amended" date
   - Add entry to version history

5. **Propagation** - Sync dependent artifacts:
   - Update `.ideacode/templates/` if affected
   - Update `CLAUDE.md` if workflow changes
   - Update subagent prompts if enforcement changes
   - Notify active developers

6. **Approval** - Requires explicit sign-off before taking effect

### Versioning Policy

**Current Version**: 1.0.0

**Version History:**
- `1.0.0` (2025-10-24) - Initial VOS4 constitution ratified

**Semantic Versioning Examples:**
- Add new principle → MINOR bump (1.1.0)
- Remove "no interfaces" exception → MAJOR bump (2.0.0)
- Clarify performance budget wording → PATCH bump (1.0.1)

### Compliance Enforcement

**Automated Enforcement:**
- @vos4-test-specialist enforces Quality Through Enforcement principle
- @vos4-documentation-specialist enforces documentation requirements
- @vos4-architecture-reviewer validates Direct Implementation principle
- @vos4-performance-analyzer validates performance budgets
- @vos4-orchestrator ensures IDEACODE workflow adherence for Tier 3 features

**Manual Reviews:**
- All Tier 3 features require constitution compliance check in plan.md
- Pull requests include "Constitution Compliance" checklist
- Quarterly constitution review for relevance and effectiveness

**Violation Handling:**
1. Identify violation during code review
2. Document in PR comments with specific principle reference
3. Block merge until resolved
4. If principle is impractical, propose amendment (not exception)
5. Track repeat violations to identify systemic issues

### Living Document Philosophy

This constitution is a **living document** that evolves with VOS4:

**Principles Reflect Reality:**
- Based on 50,000+ LOC of proven VOS4 architecture
- Informed by lessons from 496+ tests across 20 modules
- Grounded in real performance requirements (<100ms latency, <60MB memory)
- Updated as we learn what works in production

**Continuous Improvement:**
- Regular reviews ensure principles remain practical
- Feedback from development informs refinements
- Templates stay synchronized with constitution changes
- Balance between stability (for consistency) and adaptation (for learning)

**Single Source of Truth:**
- This document defines "the VOS4 way"
- Architecture conflicts resolved by referring to principles
- New developers onboard through this constitution
- Decision-making framework when choices are unclear

### Ratification

**Version**: 1.0.0
**Ratified**: 2025-10-24
**Ratified By**: VOS4 Project Lead
**Last Amended**: 2025-10-24
**Next Review**: 2025-11-24 (30 days)

---

**End of VOS4 Constitution**
