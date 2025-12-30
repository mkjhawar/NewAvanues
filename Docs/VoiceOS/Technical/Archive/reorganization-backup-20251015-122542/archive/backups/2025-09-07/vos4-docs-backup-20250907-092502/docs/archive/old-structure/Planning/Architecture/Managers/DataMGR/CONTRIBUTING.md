# Contributing to VOS3
**Path:** /Volumes/M Drive/Coding/Warp/vos3-dev/ProjectDocs/CONTRIBUTING.md  
**Created:** 2025-01-18

## Development Guidelines

### Code Standards

1. **Language**: Kotlin
2. **Min SDK**: 26 (Android 8.0)
3. **Target SDK**: 34 (Android 14)
4. **Architecture**: SOLID principles with monolithic service
5. **Style**: Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
6. **Documentation**: KDoc for all public APIs

### Memory Guidelines

**Critical: Every feature must maintain <30MB total memory target**

- Always recycle `AccessibilityNodeInfo` objects
- Use `WeakReference` for cached objects
- Implement `ComponentCallbacks2` for memory pressure
- Profile memory before and after changes
- No memory allocations in loops
- Prefer primitive types over objects
- Use object pools for frequently created objects

Example:
```kotlin
// Good - recycles node
val node = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
try {
    // use node
} finally {
    node?.recycle()
}

// Bad - leaks node
val node = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
// forgot to recycle!
```

### File Structure Requirements

All source files MUST include:

```kotlin
/**
 * [FileName].kt - [Brief description]
 * Path: [full/path/from/project/root]
 * 
 * Created: [YYYY-MM-DD]
 * Author: [Name or Team]
 * 
 * Purpose: [Detailed purpose]
 */

package com.augmentalis.voiceos.[package]

// ... code ...

// End of File
```

### Git Workflow

#### Branching Strategy
- `main` - stable release branch
- `development` - active development
- `feature/*` - new features
- `bugfix/*` - bug fixes
- `hotfix/*` - urgent production fixes

#### Commit Messages
Follow conventional commits format:
```
type(scope): description

[optional body]

[optional footer]
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation only
- `style`: Code style (formatting, semicolons, etc)
- `refactor`: Code change that neither fixes nor adds feature
- `perf`: Performance improvement
- `test`: Adding tests
- `chore`: Maintenance, dependencies, build

Examples:
```bash
feat(recognition): add Vivoka engine integration
fix(overlay): prevent memory leak in view recycling
docs(licensing): update subscription tier documentation
perf(audio): optimize VAD buffer allocation
```

#### Pull Request Process

1. **Before Creating PR**:
   - Run all tests: `./gradlew test`
   - Check memory profile
   - Update documentation
   - Add/update tests
   - Ensure no SOLID violations

2. **PR Description Must Include**:
   - What changed and why
   - Memory impact analysis
   - Testing performed
   - Screenshots (if UI changes)
   - Breaking changes (if any)

3. **Review Criteria**:
   - Code follows standards
   - Memory target maintained
   - Tests pass
   - Documentation updated
   - No security issues

### Testing Requirements

#### Unit Tests
- Required for all business logic
- Minimum 80% code coverage
- Use JUnit 5 and MockK

```kotlin
@Test
fun `valid license passes validation`() {
    // Given
    val license = createValidLicense()
    
    // When
    val result = validator.validate(license)
    
    // Then
    assertTrue(result.isValid)
}
```

#### Memory Tests
- Profile before and after changes
- Document memory impact in PR

```kotlin
@Test
fun `command execution does not leak memory`() {
    // Measure initial memory
    val initialMemory = Runtime.getRuntime().totalMemory()
    
    // Execute command 1000 times
    repeat(1000) {
        command.execute(context)
    }
    
    // Force GC and measure
    System.gc()
    val finalMemory = Runtime.getRuntime().totalMemory()
    
    // Should not grow significantly
    assertTrue(finalMemory - initialMemory < 1_000_000) // <1MB growth
}
```

#### Accessibility Tests
- Test with TalkBack enabled
- Verify all voice commands
- Check overlay visibility

### Performance Targets

All changes must maintain these targets:

| Metric | Target | How to Measure |
|--------|--------|----------------|
| Cold start | <2 seconds | `adb shell am start -W` |
| Command response | <100ms | Profiler timing |
| Memory usage | <30MB | Android Studio Profiler |
| Battery drain | <2%/hour | Battery Historian |
| ANR rate | <0.1% | Play Console |
| Crash rate | <0.5% | Play Console |

### Localization

All user-facing strings must be localized:

```kotlin
// Good - localized
val message = context.getString(R.string.command_executed)

// Bad - hardcoded
val message = "Command executed"
```

Command patterns must support all 8 languages:
```kotlin
val CLICK_COMMANDS = mapOf(
    "en" to listOf("click", "tap"),
    "es" to listOf("clic", "tocar"),
    "fr" to listOf("cliquer", "toucher"),
    // ... all 8 languages
)
```

### Security Requirements

1. **No hardcoded secrets**: Use BuildConfig or secure storage
2. **Validate all inputs**: Especially voice commands
3. **Encrypt sensitive data**: License keys, user preferences
4. **Use ProGuard/R8**: Obfuscate release builds
5. **Certificate pinning**: For API calls
6. **No logging in production**: Remove Log.d/v in release

### Documentation

#### Code Documentation
- All public methods need KDoc
- Complex algorithms need inline comments
- Include examples for non-obvious usage

```kotlin
/**
 * Validates a license key and activates features
 * 
 * @param licenseKey The license key to validate
 * @return ValidationResult with status and activated features
 * 
 * Example:
 * ```
 * val result = validateLicense("LIFE-XXXX-XXXX-XXXX")
 * if (result.isValid) {
 *     enableFeatures(result.features)
 * }
 * ```
 */
fun validateLicense(licenseKey: String): ValidationResult
```

#### Project Documentation
- Add to `/ProjectDocs` folder
- Update MASTER-DOCUMENTATION-INDEX.md
- Use consistent naming: `CATEGORY-DESCRIPTION.md`

### Development Setup

#### Required Tools
- Android Studio Hedgehog (2023.1.1) or later
- Kotlin 1.9.20 or later
- Android SDK 34
- Git 2.30+

#### Initial Setup
```bash
# Clone repository
git clone [repository-url]
cd vos3-dev

# Set up git hooks
./scripts/setup-hooks.sh

# Build project
./gradlew build

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

#### Memory Profiling Setup
1. Open Android Studio Profiler
2. Select Memory timeline
3. Start recording before app launch
4. Perform typical usage for 5 minutes
5. Check for memory leaks
6. Document peak memory usage

### Common Issues & Solutions

#### OutOfMemoryError
- Check for node recycling
- Review bitmap usage
- Implement memory callbacks

#### Slow Recognition
- Check audio buffer size
- Verify VAD settings
- Profile recognition loop

#### Overlay Not Showing
- Check overlay permission
- Verify WindowManager.LayoutParams
- Check for crashes in overlay creation

### Code Review Checklist

- [ ] Follows Kotlin conventions
- [ ] Memory target maintained (<30MB)
- [ ] All nodes recycled
- [ ] Tests included and passing
- [ ] Documentation updated
- [ ] No hardcoded strings
- [ ] Security considerations addressed
- [ ] Performance targets met
- [ ] SOLID principles followed
- [ ] File headers and EOF markers present

### Getting Help

- **Discord**: [Development channel]
- **Email**: dev@augmentalis.com
- **Issues**: GitHub issue tracker
- **Wiki**: Internal wiki for team members

### License

By contributing, you agree that your contributions will be licensed under the same proprietary license as the project. You must have the right to grant this license for any contributions.

---

*Last Updated: 2025-01-18*