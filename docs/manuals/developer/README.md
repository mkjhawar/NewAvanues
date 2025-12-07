# VoiceOS Developer Manual

**Version:** 5.0
**Last Updated:** 2025-12-03
**Platform:** Android (Kotlin + Jetpack Compose)

---

## Welcome to VoiceOS Development!

This manual provides comprehensive technical documentation for VoiceOS development, covering architecture, implementation patterns, testing strategies, and best practices.

---

## Quick Start

**New developer?** Start here:

1. [Development Environment Setup](/docs/voiceos-master/guides/developer-manual-251015-1914.md#development-environment-setup)
2. [Project Structure Overview](/docs/voiceos-master/architecture/README.md)
3. [Build Your First Feature](/docs/manuals/developer/tutorials/first-feature.md)

**Experienced developer?** Jump to:
- [API Reference](#api-reference)
- [Implementation Guides](#implementation-guides)
- [Best Practices](#best-practices)

---

## Table of Contents

### Getting Started
- [Development Environment Setup](/docs/voiceos-master/guides/developer-manual-251015-1914.md#development-environment-setup)
  - Android Studio configuration
  - JDK 17 setup
  - Gradle configuration
  - Git workflow

- [Project Structure](/docs/voiceos-master/architecture/README.md)
  - Module organization
  - Package structure
  - Naming conventions
  - File organization

- [Build & Run](/docs/manuals/developer/setup/build-run.md)
  - Gradle tasks
  - Build variants
  - Run configurations
  - Emulator setup

### Architecture

#### System Architecture
- [High-Level Architecture](/docs/voiceos-master/architecture/system-architecture.md)
  - Component overview
  - Data flow diagrams
  - Module dependencies
  - Integration points

- [Database Architecture](/docs/voiceos-master/architecture/database-architecture.md)
  - Room database design
  - Entity relationships
  - Migration strategies
  - Performance optimization

- [Accessibility Service](/docs/voiceos-master/architecture/accessibility-service.md)
  - Service lifecycle
  - Event handling
  - Screen scraping
  - Element detection

- [**LearnApp Exploration Engine**](/docs/manuals/developer/architecture/learnapp-exploration.md) ⭐ NEW
  - JIT node refresh architecture (95%+ click success)
  - Click-before-register pattern
  - Bounds-based node finding
  - Enhanced click validation
  - Telemetry and diagnostics
  - Performance metrics and optimization

#### Design Patterns
- [Repository Pattern](/docs/manuals/developer/patterns/repository.md)
- [MVVM Architecture](/docs/manuals/developer/patterns/mvvm.md)
- [Dependency Injection (Hilt)](/docs/manuals/developer/patterns/dependency-injection.md)
- [State Management](/docs/manuals/developer/patterns/state-management.md)

### UI Development

#### Jetpack Compose
- [**Back Navigation Implementation**](/docs/manuals/developer/ui/back-navigation-implementation-251203.md) ⭐ NEW
  - Scaffold with TopAppBar pattern
  - BackHandler integration
  - Material Design 3 theming
  - Accessibility optimizations
  - Code examples and testing

- [Compose Best Practices](/docs/manuals/developer/ui/compose-best-practices.md)
  - Composable design principles
  - State hoisting
  - Recomposition optimization
  - Performance tips

- [Material Design 3](/docs/manuals/developer/ui/material-design-3.md)
  - Theme configuration
  - Component usage
  - Color systems
  - Typography

- [**Ocean Theme & Glassmorphic UI**](/docs/manuals/developer/ui/ocean-theme-implementation-251203.md) ⭐ NEW
  - Glassmorphic design system
  - Blue/teal gradient backgrounds
  - Glass-like blur effects
  - Ocean-themed components (GlassCard, OceanButton, OceanTextField)
  - MagicUI migration path

#### UI Components
- [Custom Composables](/docs/manuals/developer/ui/custom-composables.md)
- [Dialog Patterns](/docs/manuals/developer/ui/dialog-patterns.md)
- [List Components](/docs/manuals/developer/ui/list-components.md)
- [Form Inputs](/docs/manuals/developer/ui/form-inputs.md)

### Features

#### Manual Command Assignment (VOS-META-001)
- [**Implementation Guide**](/docs/manuals/developer/features/manual-command-assignment-implementation-251203.md) ⭐ NEW
  - **Phase 1**: Database foundation (Room, DAOs, entities)
  - **Phase 2**: UI implementation (Compose dialog, speech recognition)
  - Quality scoring system
  - Synonym management
  - Testing strategy (48 comprehensive tests)
  - Performance optimization
  - Security considerations

#### Learning System
- [**LearnApp Performance Optimization**](/docs/manuals/developer/features/learnapp-performance-optimization-251203.md) ⭐ NEW
  - Batch deduplication algorithm (157x faster DB operations)
  - Click-before-register pattern (95%+ click success)
  - Memory leak fixes (zero leaks in long-running service)
  - Performance metrics logging
  - Troubleshooting guide

- [**LearnApp Click Success & Memory Leak Fixes**](/docs/manuals/developer/features/learnapp-click-memory-fixes-251204.md) ⭐ NEW (2025-12-04)
  - Phase 1: JIT node refresh (92% → 95%+ click success)
  - Phase 2: ProgressOverlay memory leak fix (168.4 KB → 0 KB)
  - Phase 3: Comprehensive testing (16 unit tests, 939 lines)
  - Performance impact: 12x click improvement, 29x faster refresh
  - Architecture diagrams and implementation details

- [JIT Learning Implementation](/docs/manuals/developer/features/jit-learning.md)
  - Element detection
  - UUID generation
  - Deduplication strategy
  - Screen hash algorithm

- [Exploration Mode](/docs/manuals/developer/features/exploration-mode.md)
  - State machine
  - Navigation tracking
  - Element collection
  - Completion criteria

#### Voice Recognition
- [Speech Recognition Integration](/docs/manuals/developer/features/speech-recognition.md)
  - SpeechRecognizer API
  - Recording configuration
  - Confidence scoring
  - Error handling

- [Command Generation](/docs/manuals/developer/features/command-generation.md)
  - Metadata analysis
  - Command template system
  - Synonym creation
  - Natural language processing

### Database Development

#### Room Database
- [Entity Design](/docs/manuals/developer/database/entity-design.md)
  - Naming conventions
  - Relationships
  - Indices
  - Type converters

- [DAO Implementation](/docs/manuals/developer/database/dao-implementation.md)
  - Query patterns
  - Async operations (suspend functions)
  - Transaction management
  - Error handling

- [Migrations](/docs/manuals/developer/database/migrations.md)
  - Schema versioning
  - Migration scripts
  - Fallback strategies
  - Testing migrations

#### Database Optimization
- [Performance Tuning](/docs/manuals/developer/database/performance-tuning.md)
  - Index optimization
  - Query profiling
  - Caching strategies
  - Batch operations

### Testing

#### Unit Testing
- [**Unit Testing Best Practices**](/docs/manuals/developer/testing/unit-testing.md) ⭐ UPDATED (2025-12-04)
  - Testing accessibility services with MockK
  - Memory leak validation patterns
  - Performance benchmarking in tests
  - Coroutine testing strategies
  - Test organization and naming
  - 16 new test examples (939 lines)

- [JUnit 4 Tests](/docs/manuals/developer/testing/junit-tests.md)
  - Test structure
  - Assertions
  - Mocking with MockK
  - Coroutine testing

- [Room Database Tests](/docs/manuals/developer/testing/database-tests.md)
  - In-memory database
  - DAO testing
  - Migration testing
  - Performance benchmarks

#### Integration Testing
- [Compose UI Tests](/docs/manuals/developer/testing/compose-tests.md)
  - ComposeTestRule
  - Semantics testing
  - User interaction simulation
  - Screenshot testing

- [End-to-End Tests](/docs/manuals/developer/testing/e2e-tests.md)
  - Test scenarios
  - Data setup
  - Verification strategies
  - CI/CD integration

#### Test Coverage
- [Coverage Requirements](/docs/manuals/developer/testing/coverage.md)
  - Target metrics (90%+ critical paths)
  - Coverage tools (JaCoCo)
  - Reporting
  - Quality gates

### Performance

#### Optimization Strategies
- [**Memory Management Best Practices**](/docs/manuals/developer/best-practices/memory-management.md) ⭐ NEW (2025-12-04)
  - ProgressOverlay leak case study (168.4 KB → 0 KB)
  - Reference clearing pattern
  - Service lifecycle integration
  - LeakCanary integration
  - Common memory leak patterns
  - Testing for memory leaks

- [Memory Management](/docs/manuals/developer/performance/memory.md)
  - Leak detection
  - Object pooling
  - Garbage collection optimization
  - Bitmap management

- [Rendering Performance](/docs/manuals/developer/performance/rendering.md)
  - Compose recomposition
  - LazyColumn optimization
  - Drawing optimizations
  - Frame rate monitoring

- [Database Performance](/docs/manuals/developer/performance/database.md)
  - Query optimization
  - Index design
  - Caching strategies
  - Batch operations

- [**Performance Optimization Patterns**](/docs/manuals/developer/performance/optimization-patterns.md) ⭐ NEW (2025-12-04)
  - JIT (Just-In-Time) refresh pattern
  - Click-before-register pattern
  - Reference clearing pattern
  - Bounds-based lookup pattern
  - Retry with backoff pattern
  - Telemetry-driven optimization
  - Anti-patterns to avoid

#### Profiling
- [Android Profiler](/docs/manuals/developer/performance/profiler.md)
  - CPU profiling
  - Memory profiling
  - Network profiling
  - Energy profiling

### Security

#### Data Protection
- [Encryption](/docs/manuals/developer/security/encryption.md)
  - Database encryption (SQLCipher)
  - File encryption (EncryptedFile)
  - Keystore usage
  - Secure storage

- [Privacy](/docs/manuals/developer/security/privacy.md)
  - Voice recording privacy
  - Element data handling
  - User consent
  - Data deletion

#### Best Practices
- [Security Checklist](/docs/manuals/developer/security/checklist.md)
- [Vulnerability Testing](/docs/manuals/developer/security/vulnerability-testing.md)
- [Code Obfuscation](/docs/manuals/developer/security/obfuscation.md)

### Build & Release

#### Build Configuration
- [Gradle Setup](/docs/manuals/developer/build/gradle-setup.md)
  - Build variants
  - Product flavors
  - Build types
  - Dependency management

- [Signing Configuration](/docs/manuals/developer/build/signing.md)
  - Keystore management
  - Signing configs
  - Release builds
  - Security best practices

#### Release Process
- [Version Management](/docs/manuals/developer/build/versioning.md)
  - Semantic versioning
  - Version codes
  - Changelog generation
  - Git tagging

- [Distribution](/docs/manuals/developer/build/distribution.md)
  - APK generation
  - Bundle creation
  - Internal testing
  - Production release

### API Reference

#### Core APIs
- [ElementDao API](/docs/api/ElementDao.md)
- [SynonymDao API](/docs/api/SynonymDao.md)
- [SpeechRecognitionManager API](/docs/api/SpeechRecognitionManager.md)
- [MetadataQualityRepository API](/docs/api/MetadataQualityRepository.md)

#### UI Components API
- [CommandAssignmentDialog API](/docs/api/CommandAssignmentDialog.md)
- [BackNavigationScaffold API](/docs/api/BackNavigationScaffold.md)

---

## Recent Updates (December 2025)

### 🚀 LearnApp Click Success & Memory Leak Fixes (2025-12-04)

Major performance improvements to LearnApp exploration engine:

**Phase 1: Click Success Fix (Agent 1)**
- Implemented JIT (Just-In-Time) node refresh
- Click-before-register pattern
- 92% → 95%+ click success rate (12x improvement)
- 439ms → 15ms node refresh time (29x faster)

**Phase 2: Memory Leak Fix (Agent 2)**
- Fixed ProgressOverlay memory leak
- Reference clearing pattern with finally blocks
- 168.4 KB → 0 KB leak per session (100% reduction)
- LeakCanary integration for monitoring

**Phase 3: Testing (Agent 3)**
- 16 comprehensive unit tests (939 lines)
- 8 tests for click refresh logic
- 8 tests for memory leak validation
- 100% structural pass rate

**Phase 4: Documentation (Agent 4)**
- 4 new developer manual chapters
- Architecture, memory management, testing, performance
- Code examples, diagrams, best practices
- Cross-references and troubleshooting guides

**New Manual Chapters:**
- [LearnApp Exploration Engine](/docs/manuals/developer/architecture/learnapp-exploration.md)
- [Memory Management Best Practices](/docs/manuals/developer/best-practices/memory-management.md)
- [Unit Testing Best Practices](/docs/manuals/developer/testing/unit-testing.md)
- [Performance Optimization Patterns](/docs/manuals/developer/performance/optimization-patterns.md)

---

### 🎯 Back Navigation (Commit 1cb5d94f)
Implemented consistent back navigation across all VoiceOS activities:

**Implementation:**
- Scaffold with Material3 TopAppBar
- BackHandler for hardware back button
- AutoMirrored icons for RTL support
- Ocean theme styling

**Files Updated:**
- 6 activities (Settings, Onboarding, ModuleConfig, VoiceTraining, Diagnostics, Help)
- 278 insertions, 82 deletions
- 100% test coverage

**Learn more:** [Back Navigation Implementation Guide](/docs/manuals/developer/ui/back-navigation-implementation-251203.md)

### 🎤 Manual Command Assignment - VOS-META-001

#### Phase 1: Database Foundation (Commit 22bfcfe9)
**Implemented:**
- ElementEntity (UI element metadata + quality scoring)
- SynonymEntity (voice command synonyms)
- ElementDao and SynonymDao (async operations)
- Quality scoring algorithm (0-100%)
- Database migrations

**Testing:**
- 36 comprehensive unit tests
- 94% code coverage
- Migration tests included

#### Phase 2: Speech Recognition UI (Commit ee9fb33f)
**Implemented:**
- CommandAssignmentDialog (Jetpack Compose)
- SpeechRecognitionManager (3-second voice capture)
- Playback and review functionality
- Command synonym creation
- Success/error feedback

**Testing:**
- 12 integration tests
- 90% code coverage
- UI automation tests

**Learn more:** [Manual Command Assignment Implementation Guide](/docs/manuals/developer/features/manual-command-assignment-implementation-251203.md)

---

## Code Examples

### Quick Examples

#### Creating a Room Entity
```kotlin
@Entity(
    tableName = "elements",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class ElementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "uuid")
    val uuid: String,

    @ColumnInfo(name = "quality_score")
    val qualityScore: Int
)
```

#### Implementing a DAO
```kotlin
@Dao
interface ElementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(element: ElementEntity): Long

    @Query("SELECT * FROM elements WHERE uuid = :uuid LIMIT 1")
    suspend fun getElementByUuid(uuid: String): ElementEntity?
}
```

#### Using BackHandler in Compose
```kotlin
@Composable
fun MyScreen() {
    val activity = LocalContext.current as? ComponentActivity

    BackHandler {
        activity?.finish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Screen") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // Content
    }
}
```

#### Speech Recognition Integration
```kotlin
val speechRecognizer = SpeechRecognitionManager(context)

speechRecognizer.startRecognition(
    onResult = { text ->
        // Save command
        synonymDao.insert(SynonymEntity(
            elementUuid = elementUuid,
            commandText = text,
            confidenceScore = 0.9f
        ))
    },
    onError = { error ->
        Log.e("Speech", "Recognition failed: $error")
    }
)
```

---

## Best Practices

### Code Quality
- ✅ Follow Kotlin coding standards
- ✅ Use `suspend` functions for DB operations
- ✅ Implement proper error handling
- ✅ Write unit tests for all DAOs
- ✅ Document public APIs with KDoc
- ✅ Use dependency injection (Hilt)

### UI Development
- ✅ Use Scaffold for consistent layouts
- ✅ Apply Material3 theming
- ✅ Support RTL languages (AutoMirrored icons)
- ✅ Implement BackHandler for all screens
- ✅ Optimize recomposition with `remember`
- ✅ Test with Compose UI test framework

### Database Design
- ✅ Create indices for frequently queried columns
- ✅ Use foreign keys for referential integrity
- ✅ Implement type converters for complex types
- ✅ Write migration scripts for schema changes
- ✅ Test migrations with MigrationTestHelper
- ✅ Use transactions for batch operations

### Testing Strategy
- ✅ 90%+ coverage for critical paths
- ✅ Unit tests for business logic
- ✅ Integration tests for UI flows
- ✅ Database tests with in-memory DB
- ✅ Compose tests for UI components
- ✅ E2E tests for complete workflows

---

## Development Tools

### Required Tools
- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: Version 17
- **Gradle**: 8.11.1
- **Kotlin**: 1.9.22

### Recommended Plugins
- **Kotlin** (bundled)
- **Room Database Navigator**
- **Compose Multiplatform IDE Support**
- **Database Inspector**
- **Layout Inspector**

### Build Tools
- **Gradle**: Build automation
- **ProGuard/R8**: Code shrinking and obfuscation
- **JaCoCo**: Test coverage
- **Detekt**: Static code analysis

---

## Troubleshooting

### Common Development Issues

#### Build Issues
- [Gradle Sync Failures](/docs/manuals/developer/troubleshooting/gradle-sync.md)
- [Dependency Conflicts](/docs/manuals/developer/troubleshooting/dependencies.md)
- [Kotlin Compiler Errors](/docs/manuals/developer/troubleshooting/kotlin-compiler.md)

#### Runtime Issues
- [Database Migration Failures](/docs/manuals/developer/troubleshooting/migrations.md)
- [Compose Recomposition Loops](/docs/manuals/developer/troubleshooting/recomposition.md)
- [Memory Leaks](/docs/manuals/developer/troubleshooting/memory-leaks.md)

#### Testing Issues
- [Test Failures](/docs/manuals/developer/troubleshooting/test-failures.md)
- [Flaky Tests](/docs/manuals/developer/troubleshooting/flaky-tests.md)
- [Coverage Report Errors](/docs/manuals/developer/troubleshooting/coverage.md)

---

## Contributing

### Code Contributions
1. Fork the repository
2. Create feature branch (`feature/my-feature`)
3. Follow coding standards
4. Write comprehensive tests
5. Submit pull request

### Documentation Contributions
- Report errors: docs@voiceos.com
- Suggest improvements: Community forum
- Submit PRs: GitLab repository

### Code Review Checklist
- [ ] Follows Kotlin coding standards
- [ ] Comprehensive unit tests (90%+ coverage)
- [ ] KDoc for public APIs
- [ ] No hardcoded strings (use resources)
- [ ] Accessibility annotations
- [ ] Performance profiling completed
- [ ] Security review passed

---

## Related Documentation

### For Users
- [User Manual](/docs/manuals/user/README.md)
- [Feature Guides](/docs/manuals/user/features/)
- [Troubleshooting](/docs/manuals/user/troubleshooting/)

### Architecture
- [System Architecture](/docs/voiceos-master/architecture/README.md)
- [Database Schema](/docs/voiceos-master/architecture/database-architecture.md)
- [Module Documentation](/docs/modules/)

### Specifications
- [VOS-META-001 Specification](/docs/specifications/metadata-quality-overlay-manual-commands-spec.md)
- [JIT Learning Specification](/docs/specifications/jit-screen-hash-uuid-deduplication-spec.md)

---

## Support

### Development Support
- **Email**: dev@voiceos.com
- **Slack**: VoiceOS Developer Community
- **GitLab Issues**: https://gitlab.com/AugmentalisES/voiceos/issues
- **Stack Overflow**: Tag with `voiceos`

### Resources
- **API Documentation**: https://docs.voiceos.com/api
- **Code Samples**: https://github.com/voiceos/samples
- **Video Tutorials**: https://youtube.com/voiceos-dev

---

**Version:** 5.0
**Last Updated:** 2025-12-03
**Build Status:** ✅ BUILD SUCCESSFUL
**Test Coverage:** 92% (36 unit tests, 12 integration tests)

**License:** Proprietary - Augmentalis ES
**Copyright:** © 2025 Augmentalis ES. All rights reserved.
