# Global Design Standards

**Version:** 1.0.0
**Created:** 2025-11-10
**Last Updated:** 2025-11-10
**Purpose:** Unified design standards for the entire Avanues ecosystem

---

## Overview

This directory contains **authoritative design standards** that govern how we build, structure, and communicate across all modules in the Avanues ecosystem. These standards ensure consistency, maintainability, and quality across:

- **VoiceOS** (accessibility service)
- **Avan**ues Core Platform
- **AIAvanue** (AI capabilities)
- **BrowserAvanue** (voice browser)
- **NoteAvanue** (voice notes)
- **All supporting modules** (IPC, Assets, Themes, etc.)

---

## Standards Index

### 1. [IPC Architecture](./GlobalDesignStandard-IPC-Architecture.md)
**What it covers:**
- Decision tree for choosing IPC mechanisms
- In-process communication (callbacks, StateFlow, SharedViewModel, CompositionLocal)
- Cross-process communication (AIDL, ContentProvider, BroadcastReceiver)
- Module communication patterns
- Error handling, threading, testing
- Security and performance guidelines

**When to reference:**
- Creating any new module that communicates with other modules
- Choosing between in-process vs cross-process communication
- Implementing AIDL services or ContentProviders
- Setting up callbacks or reactive data flows

---

### 2. [Module Structure](./GlobalDesignStandard-Module-Structure.md)
**What it covers:**
- Canonical directory structure for KMP modules
- Layer responsibilities (domain, data, presentation, UI)
- expect/actual pattern for platform-specific code
- Build configuration (build.gradle.kts)
- Naming conventions (packages, files, classes)
- Documentation requirements (README, ARCHITECTURE, GlobalDesignStandard)
- Testing standards and coverage targets

**When to reference:**
- Creating any new module
- Organizing existing module code
- Deciding where to place new files
- Setting up platform-specific implementations
- Writing module documentation

---

### 3. [UI Patterns](./GlobalDesignStandard-UI-Patterns.md)
**What it covers:**
- Compose best practices (naming, structure, state management)
- Side effects (LaunchedEffect, DisposableEffect, SideEffect)
- Performance optimization (remember, derivedStateOf, keys)
- Material Design 3 integration (themes, colors, typography)
- Component patterns (Cards, Dialogs, BottomSheets, Snackbars)
- Accessibility (content descriptions, touch targets, contrast)
- Navigation, Testing, Animation

**When to reference:**
- Building any UI component
- Implementing Jetpack Compose screens
- Ensuring accessibility compliance
- Optimizing UI performance
- Writing UI tests

---

### 4. [IPC Integration Guide](./GlobalDesignStandard-IPC-Integration-Guide.md) ⭐ NEW
**What it covers:**
- Step-by-step integration of IPC Foundation modules
- ARGScanner (service discovery) setup
- VoiceCommandRouter (command parsing) patterns
- IPCConnector (cross-process calls) usage
- Complete working examples
- Testing and troubleshooting
- Migration from direct AIDL to IPC Foundation

**When to reference:**
- **REQUIRED**: Any module that needs to discover or call other services
- Implementing voice command support
- Migrating from manual AIDL to IPC Foundation
- Need examples of service discovery
- Debugging IPC connection issues
- Writing tests for IPC integration

**Reference Implementation:** `/apps/ipc-foundation-demo/` (complete working app + HTML demo)

---

### 5. [Development Protocols & Best Practices](./GlobalDesignStandard-Development-Protocols.md) ⭐ NEW (2025-11-11)
**What it covers:**
- Universal development protocols for ANY Kotlin/KMP project
- Protocol #1: Search Before Creating (MANDATORY 5-minute check)
- Protocol #2: Type Mismatch Investigation (dual type systems)
- Protocol #3: Nested Enum Investigation (Font.Weight pattern)
- Protocol #4: KMP Common Code (platform-specific gotchas)
- Protocol #5: Kotlin Math Functions (pow() type requirements)
- Protocol #6: Gradle Dependency Conflicts (test frameworks)
- Quick reference cheat sheet

**When to reference:**
- **BEFORE starting ANY new infrastructure work** (MANDATORY)
- Debugging "type mismatch" or "unresolved reference" errors
- Writing KMP commonMain code (avoid JVM-specific APIs)
- Seeing compilation errors that look like architectural blockers
- Setting up test dependencies for KMP modules
- Working with kotlin.math functions

**Key Protocols:**
- ✅ Spend 5 minutes searching → save weeks of duplicate work
- ✅ 90% of "blockers" are wrong imports, not architectural issues
- ✅ Always check for dual type systems when seeing type mismatches
- ✅ Check for nested enums before assuming missing types

**Scope:** Universal (applies to ANY Kotlin/KMP project)

**Project-Specific Learnings:** See `/docs/Development-Learnings-251111.md` for Avanues-specific discoveries

---

## How to Use These Standards

### For New Modules

1. **Read [Module Structure](./GlobalDesignStandard-Module-Structure.md)** first
   - Create directory structure exactly as specified
   - Set up build.gradle.kts with KMP configuration
   - Create README.md, ARCHITECTURE.md, GlobalDesignStandard.md

2. **Review [IPC Architecture](./GlobalDesignStandard-IPC-Architecture.md)**
   - Identify communication needs with other modules
   - Choose appropriate IPC mechanism from decision tree
   - Implement following documented patterns

3. **Apply [UI Patterns](./GlobalDesignStandard-UI-Patterns.md)** (if module has UI)
   - Follow Compose best practices
   - Implement Material Design 3 theming
   - Ensure accessibility compliance
   - Write UI tests

### For Existing Modules

1. **Audit current module structure** against standards
2. **Document deviations** in module's GlobalDesignStandard.md
3. **Gradually refactor** to align with standards (don't break working code)
4. **Update module documentation** to reference these standards

### For Code Reviews

**Reviewers should verify:**
- [ ] Module structure follows standard
- [ ] IPC mechanism is appropriate for use case
- [ ] UI components follow Compose best practices
- [ ] Accessibility requirements met
- [ ] Tests written and passing
- [ ] Documentation complete

---

## Design Decision Process

### When to Create Module-Specific Standards

Some decisions are module-specific (e.g., IPCConnector has unique design considerations). Create a `GlobalDesignStandard.md` in the module directory when:

1. **Module introduces new patterns** not covered by global standards
2. **Trade-offs were made** that need justification
3. **Alternative approaches** were considered and rejected
4. **Future developers** need context for understanding design

**Example:** `modules/MagicIdea/Components/IPCConnector/GlobalDesignStandard.md`
```markdown
# IPCConnector Global Design Standard

## Circuit Breaker Implementation

**Decision:** Use CLOSED/OPEN/HALF_OPEN state machine
**Rationale:** Industry standard pattern from Hystrix/Resilience4j
**Alternatives Considered:**
- Simple retry with exponential backoff (rejected: no failure isolation)
- Custom failure detection (rejected: reinventing the wheel)

## Rate Limiting Algorithm

**Decision:** Token bucket with refill rate
**Rationale:** Allows burst traffic while preventing sustained abuse
**Alternatives Considered:**
- Leaky bucket (rejected: no burst support)
- Fixed window (rejected: boundary gaming)
```

---

## Updating Standards

### Proposing Changes

1. **Document the need** (why does the standard need to change?)
2. **Propose the solution** (what should the new standard be?)
3. **Analyze impact** (how many modules affected?)
4. **Create migration plan** (how to update existing code?)

### Approval Process

Changes to GlobalDesignStandards require:
- Technical review (does it solve the problem?)
- Impact assessment (how many modules affected?)
- Migration plan (how to update existing code?)
- Documentation update (update standard and examples)

### Versioning

- **Major version** (2.0.0): Breaking changes requiring code updates
- **Minor version** (1.1.0): New patterns/recommendations, backward compatible
- **Patch version** (1.0.1): Clarifications, typo fixes, examples

---

## Standard Compliance

### Mandatory vs Recommended

**Mandatory (MUST):**
- Module structure (directory layout, layer separation)
- Accessibility (WCAG 2.1 Level AA)
- Security (permissions, data validation)
- Testing (minimum coverage targets)

**Recommended (SHOULD):**
- Specific IPC mechanisms (can vary based on requirements)
- UI component patterns (can be adapted for specific needs)
- Performance optimizations (apply based on profiling)

**Optional (MAY):**
- Advanced optimizations
- Experimental features
- Platform-specific enhancements

---

## Reference Implementations

### Exemplary Modules

**IPCConnector** (`modules/MagicIdea/Components/IPCConnector/`)
- ✅ Complete module structure
- ✅ Full AIDL implementation
- ✅ Circuit breaker pattern
- ✅ Comprehensive tests
- ✅ Complete documentation

**Asset Manager** (`modules/MagicIdea/Components/AssetManager/AssetManager/`)
- ✅ KMP structure with expect/actual
- ✅ Platform-specific implementations
- ✅ Clean layer separation
- ✅ Built-in icon libraries

### Anti-Patterns to Avoid

❌ **Mixing concerns:**
```kotlin
// Bad: UI logic in data layer
class BrowserRepository {
    fun getBookmarks(): List<Bookmark> {
        val bookmarks = dao.getAll()
        showToast("Loaded ${bookmarks.size} bookmarks")  // ❌ UI in data layer
        return bookmarks
    }
}
```

❌ **Platform code in common:**
```kotlin
// Bad: Android-specific in commonMain
fun saveFile(file: File) {  // ❌ java.io.File doesn't exist on iOS
    // ...
}
```

❌ **Hardcoded values:**
```kotlin
// Bad: Hardcoded colors/sizes
Button(
    onClick = onClick,
    modifier = Modifier.size(48.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.Blue  // ❌ Hardcoded, won't adapt to theme
    )
) { ... }
```

---

## Quality Gates

Before merging any code, verify:

### Module Structure
- [ ] Follows canonical directory structure
- [ ] Has README.md, ARCHITECTURE.md
- [ ] Has proper package naming
- [ ] Platform-specific code in correct source sets

### IPC
- [ ] Chose appropriate mechanism from decision tree
- [ ] Error handling implemented
- [ ] Thread safety verified
- [ ] IPC methods documented

### UI
- [ ] Material Design 3 compliance
- [ ] Accessibility verified (TalkBack/VoiceOver tested)
- [ ] Light/dark themes tested
- [ ] Touch targets ≥ 48dp
- [ ] UI tests written

### Testing
- [ ] Unit tests ≥ 80% coverage (domain layer)
- [ ] Integration tests for IPC
- [ ] UI tests for critical flows
- [ ] All tests passing

### Documentation
- [ ] Public APIs documented with KDoc
- [ ] Module README complete
- [ ] Architecture documented
- [ ] Usage examples provided

---

## Tools and Resources

### Code Quality
- **Detekt**: Kotlin static analysis
- **Lint**: Android lint checks
- **ktlint**: Code formatting
- **Compose Compiler Reports**: Composition analysis

### Testing
- **JUnit 4**: Unit testing framework
- **Mockito/MockK**: Mocking framework
- **Compose Testing**: UI testing
- **Robolectric**: Android unit tests

### Documentation
- **Dokka**: API documentation generator
- **Mermaid**: Diagrams in markdown
- **PlantUML**: Architecture diagrams

---

## Migration Guide

### Migrating Existing Module to Standards

**Step 1: Structure Audit**
```bash
# Check current structure
ls -R module-name/src/

# Compare against standard
# Note deviations
```

**Step 2: Create Migration Plan**
1. List files that need moving
2. Identify code that needs refactoring
3. Plan in small, incremental steps
4. Don't break working functionality

**Step 3: Execute Migration**
1. Move files to correct locations
2. Update package declarations
3. Fix import statements
4. Run tests after each change
5. Update documentation

**Step 4: Verify Compliance**
- [ ] Directory structure matches standard
- [ ] Layers properly separated
- [ ] Tests updated and passing
- [ ] Documentation complete

---

## FAQ

### Q: What if my module doesn't need all layers?

A: Create the structure anyway (even if folders are empty). Future features may need them, and consistency is more important than minimalism.

### Q: Can I use a different IPC mechanism than recommended?

A: Yes, but document the decision in your module's GlobalDesignStandard.md with rationale.

### Q: Do I need to refactor existing modules immediately?

A: No. Refactor incrementally during feature work. Don't break working code just for compliance.

### Q: What if Android/iOS require different UI patterns?

A: Use platform-specific source sets (`androidMain/ui/`, `iosMain/ui/`). Keep shared logic in `commonMain/presentation/`.

### Q: Can I propose changes to standards?

A: Yes! Document your proposal with rationale and impact analysis. Submit for review.

---

## Version History

- **v1.0.0** (2025-11-10): Initial GlobalDesignStandards release
  - IPC Architecture standard
  - Module Structure standard
  - UI Patterns standard

---

## Contact

Questions about standards?
- Review existing standards first
- Check module-specific GlobalDesignStandard.md
- Consult reference implementations
- Document your question for future reference

---

**Created by Manoj Jhawar, manoj@ideahq.net**
