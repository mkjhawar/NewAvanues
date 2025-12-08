# VOS4 Testing Quick Start Guide

<!--
filename: TESTING-QUICK-START-GUIDE.md
created: 2025-01-29 00:15:00 PST
author: VOS4 Development Team
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Quick reference guide for running and managing tests in VOS4
last-modified: 2025-01-29 00:15:00 PST
version: 1.0.0
-->

## 🚀 Quick Start

### ⚠️ CURRENT STATUS (2025-09-03)
**Build System Issues**: Gradle 8.11.1 + Android Gradle Plugin 8.6.1 compatibility problems.
**Workaround**: Use IDE test runner for immediate testing while we resolve build issues.

### First Time Setup (One-time)
```bash
cd "/Volumes/M Drive/Coding/vos4"
# ./scripts/setup-hooks.sh  # Currently disabled due to build issues
```

### Run Tests (Current Workarounds)
```bash
# Option 1: IDE Test Runner (Recommended)
# Right-click on test files in Android Studio/IntelliJ and run

# Option 2: Try Gradle (may fail due to compatibility issues)
./gradlew :libraries:SpeechRecognition:test

# Option 3: Standalone verification
cd libraries/SpeechRecognition/src/test/java/com/augmentalis/speechrecognition/
kotlinc SmokeTest.kt -include-runtime -d smoke-test.jar
java -jar smoke-test.jar
```

### View Coverage Report
```bash
# Currently unavailable due to build system issues
# ./gradlew jacocoTestReport
# open build/reports/jacoco/test/html/index.html
```

---

## 🔧 Recent Fixes Applied (2025-09-03)

### ✅ gradle/testing-rules.gradle.kts Compilation Errors Fixed
- **Import Issues**: Added missing JaCoCo extension imports
- **Regex Parsing**: Fixed illegal escape characters in XML parsing
- **Deprecated APIs**: Updated `buildDir` references to `layout.buildDirectory`
- **PiTest Configuration**: Temporarily disabled due to dependency conflicts
- **XML Report Generation**: Replaced Groovy XmlSlurper with Kotlin regex parsing

### ✅ Test Infrastructure Status
- **Test Files**: Comprehensive test suite already exists in SpeechRecognition module
- **Dependencies**: JUnit 4, MockK, Robolectric, Coroutines testing properly configured
- **Basic Test**: Created `SmokeTest.kt` for infrastructure verification
- **Test Utilities**: `TestConfig.kt` and `TestUtils.kt` available for safe testing

### ⚠️ Known Issues Remaining
- **Gradle Compatibility**: AGP 8.6.1 + Gradle 8.11.1 type resolution errors
- **Task Creation**: `AndroidUnitTest` task creation fails with "Type T not present"
- **Test Execution**: Cannot run tests via Gradle, must use IDE test runner

---

## 📋 Testing Commands Reference

### Basic Test Execution

| Command | Description | When to Use |
|---------|-------------|-------------|
| `./gradlew test` | Run all unit tests | Quick validation |
| `./gradlew testComprehensive` | Run all tests with coverage validation | Before commits |
| `./gradlew connectedAndroidTest` | Run instrumented tests | UI/Integration testing |
| `./gradlew clean test` | Clean build and test | After major changes |

### Module-Specific Testing

```bash
# Test individual modules
./gradlew :managers:CommandManager:test
./gradlew :apps:VoiceRecognition:test
./gradlew :libraries:SpeechRecognition:test
./gradlew :managers:HUDManager:test
./gradlew :apps:VoiceAccessibility:test
./gradlew :managers:DataMGR:test
./gradlew :libraries:DeviceMGR:test
```

### Coverage Analysis

```bash
# Generate coverage report
./gradlew jacocoTestReport

# Verify coverage meets thresholds (85%)
./gradlew verifyCoverage

# Quick coverage check
./gradlew verifyCoverage --info | grep "Coverage"

# Generate and open report in one command
./gradlew jacocoTestReport && open build/reports/jacoco/test/html/index.html
```

---

## 🎯 Test Generation

### Generate Tests for New Files

```bash
# Smart template selection (recommended)
./scripts/select-test-template.sh src/main/java/com/example/MyClass.kt

# Basic test generation
./scripts/generate-test.sh src/main/java/com/example/MyClass.kt

# Batch generate for multiple files
find src/main -name "*.kt" -newer .git/FETCH_HEAD -exec ./scripts/select-test-template.sh {} \;
```

### Template Types
- **ViewModel** → Coroutines, LiveData, StateFlow testing
- **Composable** → UI testing with Compose test APIs
- **Service** → Lifecycle and binding tests
- **AIDL** → Cross-process communication tests
- **Performance** → Benchmarks and profiling

---

## 🔧 Advanced Testing Options

### Performance Testing

```bash
# Run performance tests only
./gradlew test --tests "*PerformanceTest"

# Run with memory profiling
./gradlew test -Dorg.gradle.jvmargs="-Xmx2g -XX:+HeapDumpOnOutOfMemoryError"

# Run with benchmark timing
./gradlew test --tests "*Benchmark*" --info
```

### Debug Failing Tests

```bash
# Run specific test with stack trace
./gradlew test --tests "*.TestClassName" --stacktrace

# Run with debugging enabled
./gradlew test --debug-jvm

# Run single test method
./gradlew test --tests "*.TestClass.testMethod"

# Verbose output
./gradlew test --info --console=plain
```

### Parallel Execution

```bash
# Run tests in parallel (faster)
./gradlew test --parallel

# Specify parallelism level
./gradlew test --parallel --max-workers=4
```

---

## 🚦 Git Hook Testing

### Automatic Test Validation

Tests run automatically during git operations:

```bash
# Pre-commit: Validates test coverage for changed files
git add .
git commit -m "feat: new feature"

# Pre-push: Runs full test suite
git push origin VOS4

# Bypass hooks (emergency only)
git commit --no-verify -m "fix: critical hotfix"
git push --no-verify
```

### Manual Hook Testing

```bash
# Test pre-commit hook
./.githooks/pre-commit

# Verify hooks are installed
ls -la .git/hooks/
```

---

## 📊 Test Reports

### Location of Reports

| Report Type | Location | Format |
|-------------|----------|--------|
| Test Results | `build/test-results/test/` | XML |
| Coverage HTML | `build/reports/jacoco/test/html/index.html` | HTML |
| Coverage XML | `build/reports/jacoco/test/jacocoTestReport.xml` | XML |
| Test Summary | `build/reports/tests/test/index.html` | HTML |

### Quick Report Commands

```bash
# Open test results
open build/reports/tests/test/index.html

# Open coverage report
open build/reports/jacoco/test/html/index.html

# Generate all reports
./gradlew test jacocoTestReport testReport
```

---

## 🏃 CI/CD Testing

### What Runs in CI Pipeline

```bash
# Full CI test sequence
./gradlew clean
./gradlew test
./gradlew connectedAndroidTest
./gradlew jacocoTestReport
./gradlew verifyCoverage
```

### Local CI Simulation

```bash
# Run exactly what CI runs
./gradlew clean testComprehensive jacocoTestReport verifyCoverage
```

---

## 🐛 Troubleshooting

### Common Issues and Solutions

#### Tests Won't Run
```bash
./gradlew clean
./gradlew --stop
./gradlew test --no-build-cache
```

#### Out of Memory
```bash
./gradlew test -Dorg.gradle.jvmargs="-Xmx4g"
```

#### Flaky Tests
```bash
# Run test multiple times to identify flaky behavior
./gradlew test --tests "*.FlakyTest" --rerun-tasks
```

#### Coverage Not Generated
```bash
./gradlew clean
./gradlew test jacocoTestReport --rerun-tasks
```

#### Test Dependencies Issues
```bash
# Check test dependencies
./gradlew :app:dependencies | grep test

# Refresh dependencies
./gradlew clean build --refresh-dependencies
```

---

## 📈 Coverage Thresholds

### Current Requirements
- **Line Coverage**: 85% minimum
- **Branch Coverage**: 80% minimum  
- **Method Coverage**: 90% target
- **Class Coverage**: 95% target

### Check Current Coverage
```bash
# Quick coverage summary
./gradlew verifyCoverage --quiet | tail -5

# Detailed coverage by module
./gradlew jacocoTestReport
grep -A 5 "Coverage" build/reports/jacoco/test/html/index.html
```

---

## 🎓 Best Practices

### Daily Testing Workflow

1. **Before Starting Work:**
   ```bash
   git pull
   ./gradlew test
   ```

2. **After Making Changes:**
   ```bash
   ./gradlew test --tests "*AffectedTest*"
   ```

3. **Before Committing:**
   ```bash
   ./gradlew testComprehensive
   ```

4. **After Commit (Automatic):**
   - Pre-commit hook validates coverage
   - Pre-push hook runs full suite

### Test Naming Convention
```kotlin
// Good test names
@Test
fun `should return user when valid ID provided`() { }

@Test  
fun `throws exception when network timeout occurs`() { }

// Avoid generic names
@Test
fun test1() { } // Bad
```

---

## 🔄 Quick Command Cheatsheet

```bash
# Essential Commands
./gradlew test                    # Run unit tests
./gradlew testComprehensive       # Full test suite + coverage
./gradlew jacocoTestReport        # Generate coverage report
./gradlew verifyCoverage          # Check coverage thresholds

# Module Tests
./gradlew :managers:CommandManager:test
./gradlew :apps:VoiceRecognition:test

# Test Generation  
./scripts/select-test-template.sh MyClass.kt

# Debug
./gradlew test --stacktrace --info
./gradlew test --tests "*.SpecificTest" --debug-jvm

# Reports
open build/reports/jacoco/test/html/index.html
open build/reports/tests/test/index.html
```

---

## 📞 Support

### Getting Help
- Check test logs: `build/test-results/test/`
- Review this guide: `docs/TESTING-QUICK-START-GUIDE.md`
- Full documentation: `docs/TESTING-AUTOMATION-GUIDE.md`
- Report issues: Create issue in project repository

### Useful Resources
- [Testing Automation Guide](TESTING-AUTOMATION-GUIDE.md)
- [Test Coverage Report](TEST-COVERAGE-REPORT.md)
- [Advanced Testing Enhancement Guide](ADVANCED-TESTING-ENHANCEMENT-GUIDE.md)

---

**Last Updated**: 2025-09-03 - Testing infrastructure fixes applied  
**Maintainer**: VOS4 Development Team  
**Current Coverage**: Infrastructure ready, coverage pending build system resolution 🔧