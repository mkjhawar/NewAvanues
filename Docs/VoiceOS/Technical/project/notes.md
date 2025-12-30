# VOS4 Implementation Notes

**Last Updated:** 2025-10-24

---

## Quick TODOs

### High Priority
- [ ] Fix 4 test infrastructure compilation errors (SideEffectComparator, StateComparator, TimingComparator)
- [ ] Resolve test compilation blockers in testing utilities
- [ ] Verify LeakCanary integration for memory leak detection
- [ ] Add performance benchmarks to test suite for critical paths

### Medium Priority
- [ ] Review and migrate active IDEADEV specs from `docs/ideadev.bak/` to new `specs/` structure
- [ ] Update all module documentation with current architecture diagrams
- [ ] Add inline documentation for public APIs across all 20 modules
- [ ] Create quickstart guides for major features

### Low Priority
- [ ] Optimize memory footprint for Vosk engine (<30MB target)
- [ ] Explore Azure STT integration (currently experimental)
- [ ] Document interface exceptions in architecture docs (per Direct Implementation principle)
- [ ] Quarterly constitution review (scheduled: 2025-11-24)

---

## Gotchas

### Testing Infrastructure
**Issue:** Test compilation errors are in INFRASTRUCTURE (comparators), NOT implementation code.
- Files: `SideEffectComparator.kt`, `StateComparator.kt`, `TimingComparator.kt`
- Impact: Blocks test compilation but does NOT affect production code
- Context: These are test utilities used across multiple test suites
- Reminder: Don't confuse test infrastructure with test content

### Analysis Quality
**Issue:** AI code analysis has 57% false positive rate for conciseness suggestions.
- Root causes: Missing git history, no runtime verification, lack of domain knowledge
- Lesson learned: AI finds candidates, humans must verify validity
- Example: LearnApp claimed "0% functional" but was actually fully integrated since Oct 8
- Protocol: ALWAYS verify assumptions against git history and runtime behavior

### Module Dependencies
**Issue:** VoiceRecognition Hilt DI configuration often flagged incorrectly.
- Reality: Configuration is correct and verified as of Oct 19, 2025
- False alarm pattern: Automated analysis misses Hilt's annotation processing
- Reminder: Check git log for recent verification before re-investigating

### Performance Budgets
**Critical:** Performance is ZERO TOLERANCE per constitution.
- Command latency: <100ms (95th percentile)
- Module init: <50ms
- Memory: <60MB (Vivoka), <30MB (Vosk)
- Battery: <2% per hour
- XR rendering: 90-120 FPS
- Measurement: Must profile before merging performance-critical code

### Namespace Enforcement
**Critical:** `com.augmentalis.*` is MANDATORY (ZERO TOLERANCE).
- NO `com.ai.*` (deprecated namespace)
- All new code must use correct namespace
- Automated gate in PR reviews

---

## Insights

### Architecture Patterns

**Direct Implementation Works:**
- 50,000+ LOC demonstrates direct implementation pattern effectiveness
- Clearer code, faster debugging, better performance
- Interfaces only when multi-implementation proven necessary
- Strategic exceptions must be documented with rationale

**Module Independence:**
- 20 self-contained modules enable parallel development
- Independent build/test capability crucial for CI/CD
- Clear boundaries reduce coupling
- Pattern: Each module contains ALL its components

**Quality Through Automation:**
- 496+ tests with 80%+ coverage prove automated enforcement works
- Subagents (@vos4-test-specialist, @vos4-documentation-specialist) eliminate manual gates
- Manual quality processes fail under pressure
- Automation maintains standards during rapid development

### Technology Choices

**Room v7 with KSP:**
- Type-safe compile-time checks catch errors early
- Migration support critical for long-term maintenance
- KSP faster than KAPT (build time improvement)
- ObjectBox patterns may remain in legacy code (acceptable)

**Speech Recognition Multi-Engine:**
- Whisper.cpp: Best privacy (on-device), good quality
- Vosk: Best offline support, free, 8 languages
- Vivoka: Best language coverage (42+), premium
- Google Cloud STT: Reliable fallback, requires consent
- Pattern: Privacy-first with cloud fallback

**Jetpack Compose + Material Design 3:**
- Modern UI framework, better than XML layouts
- Material Design 3 provides accessibility-first components
- Voice navigation integration easier with Compose
- Performance good for XR rendering (90-120 FPS achievable)

### Development Workflow

**3-Tier Complexity Approach:**
- Tier 1 (<30 min): Direct implementation, no overhead
- Tier 2 (1-3 hours): Subagent-assisted, automatic quality gates ⭐ RECOMMENDED
- Tier 3 (>4 hours): Full IDEACODE workflow with spec-first
- Pattern: Match process overhead to task complexity

**IDEACODE Integration:**
- Slash commands (`/idea.*`) streamline complex feature development
- Constitution provides decision-making framework
- Living docs maintain continuous context
- Templates ensure consistency

**Subagent Effectiveness:**
- @vos4-orchestrator: Excellent at routing and workflow enforcement
- @vos4-test-specialist: Blocks merges without 80%+ coverage (working as intended)
- @vos4-documentation-specialist: Ensures docs don't fall behind code
- Pattern: Automation removes human error

### Testing Strategies

**Test Pyramid Works:**
- 70% unit / 25% integration / 5% E2E
- Fast feedback loop (<5s per test suite)
- Unit tests catch most bugs earliest
- Integration tests verify contracts
- E2E tests confirm user workflows

**Mock Strategy:**
- MockK for Kotlin code
- Robolectric for Android framework
- Prefer mocking over test databases (faster)
- Real database tests for migration verification

**Deterministic Tests:**
- All tests must be deterministic (no flaky tests)
- No external service dependencies in tests
- Time-based tests use test clocks
- Randomness uses seeded generators

### Performance Optimization

**Voice Control Latency:**
- <100ms is noticeable threshold for users
- Optimize entire pipeline: audio → recognition → processing → action
- Profile before optimizing (measure, don't guess)
- Cache frequently used data

**Memory Management:**
- Vosk <30MB, Vivoka <60MB achievable
- Monitor with Android Profiler
- LeakCanary for leak detection
- Release resources promptly (speech engines, databases)

**Battery Optimization:**
- <2% per hour requires aggressive optimization
- Voice recognition is battery-intensive
- Batch processing where possible
- Wake locks carefully managed

### Documentation Best Practices

**Timestamped Status Reports:**
- Format: `Status-Topic-YYMMDD-HHMM.md`
- 100+ reports in `docs/Active/` prove value
- Provides historical context
- Enables understanding of evolution

**Architecture Diagrams:**
- REQUIRED for all architecture changes per constitution
- Mermaid format preferred (text-based, version-controllable)
- System, sequence, database, integration diagram types
- Co-located with architecture docs

**Living Documentation:**
- Update DURING development, not after
- notes.md, decisions.md, bugs.md, progress.md, backlog.md
- Continuous context prevents knowledge loss
- Onboarding accelerator for new developers

---

## Useful Patterns

### Command Processing
```kotlin
// Pattern: Context-aware routing with confidence thresholds
fun processCommand(command: String, context: ScreenContext): Result {
    val recognized = speechEngine.recognize(command)
    if (recognized.confidence < CONFIDENCE_THRESHOLD) {
        return Result.Clarify // Ask user to repeat
    }
    val action = router.route(recognized, context)
    return executor.execute(action)
}
```

### UI Scraping with Hash Deduplication
```kotlin
// Pattern: Hash-based deduplication for performance
fun scrapeScreen(rootNode: AccessibilityNodeInfo): List<UIElement> {
    val currentHash = calculateHash(rootNode)
    if (currentHash == lastHash) {
        return cachedElements // Skip scraping if unchanged
    }
    val elements = performScraping(rootNode)
    lastHash = currentHash
    cachedElements = elements
    return elements
}
```

### Module Initialization
```kotlin
// Pattern: Lazy initialization with <50ms budget
class VoiceModule @Inject constructor(
    private val dependencies: ModuleDependencies
) {
    private val processor by lazy {
        CommandProcessor(dependencies) // <50ms
    }

    fun initialize() {
        // Only initialize what's needed immediately
        processor // Trigger lazy initialization
    }
}
```

### Test Organization
```kotlin
// Pattern: Given-When-Then with clear sections
@Test
fun `should route command to correct action based on screen context`() {
    // Given
    val command = "tap submit button"
    val context = ScreenContext(type = FORM, buttons = listOf("submit"))

    // When
    val action = router.route(command, context)

    // Then
    assertThat(action).isInstanceOf(TapAction::class)
    assertThat(action.target).isEqualTo("submit")
}
```

---

## Quick References

### Build Commands
```bash
# Compile all modules
./gradlew build

# Run tests
./gradlew test

# Compile specific module (faster)
./gradlew :VoiceOSCore:compileDebugKotlin

# Clean build
./gradlew clean build
```

### Git Commands
```bash
# Conventional commits
git commit -m "feat(module): Add feature"
git commit -m "fix(module): Fix bug"
git commit -m "docs(module): Update docs"
git commit -m "test(module): Add tests"
git commit -m "refactor(module): Refactor code"
git commit -m "perf(module): Optimize performance"

# Stage by category (MANDATORY)
git add docs/
git commit -m "docs: Update documentation"
git add src/
git commit -m "feat: Add feature"
git add tests/
git commit -m "test: Add tests"
```

### IDEACODE Commands
```bash
# Full workflow (Tier 3 features)
/idea.specify "feature description"
/idea.clarify           # Optional
/idea.plan
/idea.tasks
/idea.implement
/idea.analyze           # Optional
/idea.checklist         # Optional

# Subagent assistance (Tier 2 features)
@vos4-orchestrator Task description here
```

---

**End of Implementation Notes**
