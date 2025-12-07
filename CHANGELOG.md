# Changelog

All notable changes to VoiceOS will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **VoiceOSCore:** Dynamic command real-time element search failure ([Fix Doc](docs/fixes/VoiceOSCore-dynamic-command-realtime-search-2025-11-13.md))
  - Root cause: Broken recursive node search logic + false success reporting in Tier 3
  - Solution: Kotlin extension functions for safe AccessibilityNodeInfo lifecycle + proper result checking
  - Impact: Dynamic commands now work on unscraped/partially-scraped apps
  - Files changed: `AccessibilityNodeExtensions.kt` (new), `VoiceCommandProcessor.kt`, `VoiceOSService.kt`
  - Severity: CRITICAL (complete feature breakdown)

- **VoiceOSCore:** Voice command flow - only first command processed after service enable ([Fix Doc](docs/fixes/VoiceOSCore-voice-command-flow-2025-11-13.md)) ([ADR-001](docs/architecture/decisions/ADR-001-stateflow-vs-sharedflow-for-voice-commands.md))
  - Root cause: StateFlow-based architecture didn't properly separate state from events
  - Solution: Added SharedFlow for command events while keeping StateFlow for engine state
  - Impact: All voice commands now properly reach VoiceOSService for processing
  - Files changed: `SpeechEngineManager.kt`, `VoiceOSService.kt`

- **VoiceOSCore:** Compilation errors in VoiceRecognitionManager and NodeRecyclingUtils ([Fix Doc](docs/fixes/VoiceOSCore-compilation-errors-2025-11-13.md))
  - Root cause: ConditionalLogger API ergonomics and recursive inline function
  - Solution: Added natural parameter order overload + iterative traversal algorithm
  - Impact: Code compiles, better API ergonomics, eliminated stack overflow risk
  - Files changed: `ConditionalLogger.kt`, `NodeRecyclingUtils.kt`
  - Documentation: Added inline functions guide and logging best practices

### Added
- **ConditionalLogger:** Natural parameter order overload for exception logging
  - New: `e(TAG, exception) { "message" }` with trailing lambda syntax
  - Backward compatible with existing `e(TAG, { "message" }, exception)` syntax
  - Follows Kotlin idioms and improves developer experience

- **Developer Manual:** Chapter 33.7.5 - Logging Best Practices
  - ConditionalLogger usage patterns with examples
  - Error logging with natural parameter order
  - Lazy evaluation benefits and performance impact
  - PII protection with PIILoggingWrapper
  - Common logging patterns (lifecycle, state, methods)
  - CI/CD enforcement documentation
  - Location: `docs/developer-manual/33-Code-Quality-Standards.md`

- **Developer Manual:** Chapter 33.7.6 - Inline Functions Best Practices
  - When to use/avoid inline functions with code examples
  - Visibility rules (public/internal required for accessed members)
  - Recursion limitations and iterative rewrites
  - Performance characteristics table
  - Code review checklist
  - Quick decision guide
  - Location: `docs/developer-manual/33-Code-Quality-Standards.md`

### Changed
- **Documentation:** Reorganized developer manual structure
  - Moved logging best practices from standalone guide to Chapter 33.7.5
  - Moved inline functions best practices from standalone guide to Chapter 33.7.6
  - Updated Developer Manual to v4.3.1
  - Updated table of contents with new sections
  - Removed: `docs/developer-manual/logging-api-best-practices.md` (consolidated)
  - Removed: `docs/developer-manual/inline-functions-guide.md` (consolidated)
  - Rationale: Developer manual organized by function/feature, not standalone guides

- **NodeRecyclingUtils:** Rewritten `traverseSafely()` to use iterative algorithm
  - Changed from recursive to iterative depth-first traversal
  - Enables inline optimization (~30% performance gain)
  - Eliminates stack overflow risk (handles 1000+ node trees)
  - Maintains same traversal order (DFS, left-to-right)
  - Changed TAG visibility from private to public (required for inline functions)

### Deprecated

### Removed

### Security

---

## Template for Future Releases

```markdown
## [X.Y.Z] - YYYY-MM-DD

### Added
- New features

### Changed
- Changes to existing functionality

### Fixed
- Bug fixes

### Deprecated
- Soon-to-be removed features

### Removed
- Now removed features

### Security
- Security-related changes
```
