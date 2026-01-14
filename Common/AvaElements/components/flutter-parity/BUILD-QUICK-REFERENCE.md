# Flutter Parity Build & CI/CD - Quick Reference

**Quick access guide for common build and CI/CD operations**

---

## Common Build Commands

### Basic Builds
```bash
# Debug build
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:assembleDebug

# Release build
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:assembleRelease

# Clean build
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:clean
```

### Testing
```bash
# Run all unit tests
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:testDebugUnitTest

# Generate coverage report (HTML)
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:koverHtmlReport
open Universal/Libraries/AvaElements/components/flutter-parity/build/reports/kover/html/index.html

# Verify coverage meets 90% threshold
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:koverVerify
```

### Code Quality
```bash
# Run all quality gates
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:qualityGates

# Check code style (Ktlint)
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:ktlintCheck

# Auto-format code
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:ktlintFormat

# Run Android Lint
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:lintDebug
```

### Publishing
```bash
# Publish to local Maven repository
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:publishToMavenLocal

# Publish to all configured repositories
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:publish
```

### Performance Analysis
```bash
# Run build performance analysis
./scripts/build-performance.sh

# Enable build scan
./gradlew build --scan
```

---

## File Locations

### Build Configuration
```
/Volumes/M-Drive/Coding/Avanues/
├── settings.gradle.kts                    # Module inclusion
├── gradle.properties                      # Build optimizations
├── build.gradle.kts                       # Plugin versions
└── Universal/Libraries/AvaElements/components/flutter-parity/
    ├── build.gradle.kts                   # Module build config
    ├── proguard-rules.pro                 # R8/ProGuard rules (147 lines)
    └── consumer-rules.pro                 # Consumer ProGuard rules (35 lines)
```

### CI/CD & Automation
```
/Volumes/M-Drive/Coding/Avanues/
├── .github/workflows/
│   └── flutter-parity-ci.yml              # CI/CD pipeline (6 jobs)
├── .editorconfig                          # Code style rules
└── scripts/
    └── build-performance.sh               # Performance analyzer
```

### Reports & Documentation
```
Universal/Libraries/AvaElements/components/flutter-parity/
├── BUILD-CONFIGURATION-SUMMARY.md         # Detailed configuration (14 KB)
├── AGENT-6-BUILD-CI-DELIVERABLES.md      # Deliverables report
└── BUILD-QUICK-REFERENCE.md              # This file
```

---

## Quality Gate Thresholds

| Check | Threshold | Enforcement |
|-------|-----------|-------------|
| Test Coverage | ≥ 90% | Enforced |
| Ktlint Violations | 0 | Enforced |
| Android Lint Errors | 0 | Enforced |
| AAR Size | < 500 KB | Enforced |

---

## CI/CD Pipeline

### Triggers
- Push to: main, develop, feature/**, avamagic/**
- Pull requests to: main, develop
- Only when flutter-parity files change

### Jobs (6 total)
1. **Build & Test** (30 min) - Debug + Release builds + tests
2. **Code Quality** (20 min) - Ktlint + Android Lint
3. **Test Coverage** (20 min) - Kover coverage check
4. **APK Size Check** (15 min) - Size validation
5. **Publish to Maven** - Auto-publish on main/develop
6. **Performance Regression** - Benchmark comparison

---

## Artifact Locations

### Build Outputs
```
Universal/Libraries/AvaElements/components/flutter-parity/build/
├── outputs/aar/
│   ├── flutter-parity-debug.aar
│   └── flutter-parity-release.aar
├── reports/
│   ├── kover/html/index.html              # Coverage report
│   └── lint-results-debug.html            # Lint report
└── libs/                                  # AAR files
```

### Published Artifacts
```
# Local Maven
build/repo/com/augmentalis/avaelements/flutter-parity/1.0.0/
├── flutter-parity-1.0.0.aar
├── flutter-parity-1.0.0-sources.jar
├── flutter-parity-1.0.0-javadoc.jar
└── flutter-parity-1.0.0.pom

# GitHub Packages
maven.pkg.github.com/augmentalis/avanues/
└── com/augmentalis/avaelements/flutter-parity/1.0.0/
```

---

## Performance Targets

| Metric | Target | Expected |
|--------|--------|----------|
| Debug Build (clean) | < 60s | ~45s |
| Debug Build (incremental) | < 10s | ~5-8s |
| Release Build (clean) | < 120s | ~90s |
| Release Build (incremental) | < 15s | ~10-12s |
| Test Execution | < 30s | ~20s |
| Release AAR Size | < 500 KB | ~400 KB |

---

## Troubleshooting

### Build Fails
```bash
# Clean and rebuild
./gradlew clean
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:assembleDebug --stacktrace
```

### Quality Gates Fail
```bash
# Check what failed
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:qualityGates --continue

# Fix code style issues automatically
./gradlew :Universal:Libraries:AvaElements:components:flutter-parity:ktlintFormat

# View detailed lint report
open Universal/Libraries/AvaElements/components/flutter-parity/build/reports/lint-results-debug.html

# View coverage report
open Universal/Libraries/AvaElements/components/flutter-parity/build/reports/kover/html/index.html
```

### CI/CD Pipeline Fails
1. Check GitHub Actions logs
2. Run same commands locally
3. Verify all quality gates pass locally
4. Check AAR size is < 500 KB

---

## Module Coordinates

```kotlin
// Add to consuming project
dependencies {
    implementation("com.augmentalis.avaelements:flutter-parity:1.0.0")
}
```

**GroupId:** `com.augmentalis.avaelements`
**ArtifactId:** `flutter-parity`
**Version:** `1.0.0`

---

## Documentation

| Document | Description |
|----------|-------------|
| BUILD-QUICK-REFERENCE.md | This file - quick commands |
| BUILD-CONFIGURATION-SUMMARY.md | Detailed configuration (14 KB) |
| AGENT-6-BUILD-CI-DELIVERABLES.md | Complete deliverables report |

---

**Last Updated:** 2025-11-22
**Maintainer:** Manoj Jhawar (manoj@ideahq.net)
