# Platform-Specific Branching Strategy

**Version:** 1.0
**Date:** 2025-11-23
**Project:** AvaElements / AvaMagic Component Library

---

## Overview

To enable parallel development across multiple platforms, we've implemented a **platform-specific branching strategy** where each platform has its own dedicated branch for independent development and testing.

---

## Branch Structure

### Active Development Branches

| Branch | Purpose | Primary Code | Team |
|--------|---------|--------------|------|
| `avamagic/android` | Android platform development | `android/*`, `Renderers/Android/*` | Android team |
| `avamagic/ios` | iOS platform development | `Renderers/iOS/*` | iOS team |
| `avamagic/web` | Web platform development | `Renderers/Web/*` | Web team |
| `avamagic/desktop` | Desktop platform development | `Renderers/Desktop/*` | Desktop team |
| `avamagic/modularization` | Common/core code | `Core/*`, `components/unified/*` | All teams |
| `avamagic/integration` | Integration & testing | All platform code | Integration team |

### Branch Relationships

```
main (production)
  ├── avamagic/modularization (common code)
  │   └── avamagic/integration (merge target)
  │       ├── avamagic/android
  │       ├── avamagic/ios
  │       ├── avamagic/web
  │       └── avamagic/desktop
```

---

## Directory-to-Branch Mapping

### avamagic/android
**Contains:**
- `android/` - Android app code
- `Universal/Libraries/AvaElements/Renderers/Android/` - Android renderer
- Android-specific build configs
- Android-specific tests
- Android-specific documentation

**Excludes:**
- Core/common code (stays in modularization branch)

### avamagic/ios
**Contains:**
- `Universal/Libraries/AvaElements/Renderers/iOS/` - iOS renderer (Swift/Kotlin)
- iOS-specific build configs (Swift Package Manager)
- iOS-specific tests (XCTest)
- iOS-specific documentation

**Excludes:**
- Core/common code

### avamagic/web
**Contains:**
- `Universal/Libraries/AvaElements/Renderers/Web/` - Web renderer (React/TypeScript)
- Web-specific build configs (package.json, vite.config.ts)
- Web-specific tests (Jest, Storybook)
- Web-specific documentation

**Excludes:**
- Core/common code

### avamagic/desktop
**Contains:**
- `Universal/Libraries/AvaElements/Renderers/Desktop/` - Desktop renderer (Compose Desktop)
- Desktop-specific build configs
- Desktop-specific tests
- Desktop-specific documentation

**Excludes:**
- Core/common code

### avamagic/modularization (Common/Core)
**Contains:**
- `Universal/Libraries/AvaElements/Core/` - Shared Kotlin multiplatform code
- `Universal/Libraries/AvaElements/components/unified/` - AvaMagic components
- Common type definitions
- Shared utilities
- Platform-agnostic interfaces

**This is the "single source of truth" for component definitions.**

### avamagic/integration
**Contains:**
- All platform code merged together
- Cross-platform tests
- Integration tests
- Performance benchmarks
- Parity validation scripts

---

## Workflow

### 1. Daily Development

```bash
# Work on Android
git checkout avamagic/android
# ... make changes to Android renderer ...
git add Renderers/Android/
git commit -m "feat(android): implement MagicTag component"
git push origin avamagic/android

# Work on iOS
git checkout avamagic/ios
# ... make changes to iOS renderer ...
git add Renderers/iOS/
git commit -m "feat(ios): implement MagicTag component"
git push origin avamagic/ios
```

### 2. Updating Common Code

```bash
# Update core components (affects all platforms)
git checkout avamagic/modularization
# ... make changes to Core or unified components ...
git add Universal/Libraries/AvaElements/Core/
git commit -m "feat(core): add MagicTag component definition"
git push origin avamagic/modularization

# Merge into each platform branch
for branch in android ios web desktop; do
  git checkout avamagic/$branch
  git merge avamagic/modularization
  git push origin avamagic/$branch
done
```

### 3. Integration Testing

```bash
# Merge all platforms into integration branch
git checkout avamagic/integration
git merge avamagic/modularization
git merge avamagic/android
git merge avamagic/ios
git merge avamagic/web
git merge avamagic/desktop

# Run cross-platform tests
./gradlew test
npm run test
swift test

# If all pass, integration is ready for main
```

### 4. Production Release

```bash
# Merge integration to main
git checkout main
git merge avamagic/integration
git tag v2.0.0
git push origin main --tags
```

---

## CI/CD Strategy

### Platform-Specific Pipelines

Each platform branch triggers its own CI/CD pipeline:

**avamagic/android** → `.github/workflows/android-ci.yml`
- Build Android renderer
- Run Android tests
- Generate Android APK
- Publish to Maven

**avamagic/ios** → `.github/workflows/ios-ci.yml`
- Build iOS renderer
- Run XCTest suite
- Generate iOS framework
- Publish to CocoaPods/SPM

**avamagic/web** → `.github/workflows/web-ci.yml`
- Build Web renderer
- Run Jest tests
- Build Storybook
- Publish to npm

**avamagic/desktop** → `.github/workflows/desktop-ci.yml`
- Build Desktop renderer
- Run Desktop tests
- Generate Desktop JAR
- Publish to Maven

**avamagic/integration** → `.github/workflows/integration-ci.yml`
- Build all platforms
- Run cross-platform tests
- Validate component parity
- Performance benchmarks

---

## Merge Rules

### Platform → Integration

**Required Checks:**
- ✅ Platform-specific tests pass
- ✅ No merge conflicts with modularization
- ✅ Code review approved
- ✅ Component count unchanged (unless adding new components)

### Integration → Main

**Required Checks:**
- ✅ All platform tests pass
- ✅ Cross-platform parity validated (all platforms have same components)
- ✅ Performance benchmarks pass
- ✅ Documentation updated
- ✅ Version tagged

---

## Conflict Resolution

### Common Conflict: Component Definition Changed

**Scenario:** Core component definition changed in modularization, conflicts with platform implementation.

**Resolution:**
1. Platform branch must update to match new core definition
2. Platform-specific rendering logic can adapt but API must match
3. Update tests to reflect new behavior

### Common Conflict: Platform-Specific Enhancement

**Scenario:** Platform adds platform-specific feature not in core definition.

**Resolution:**
1. If feature is platform-specific (e.g., iOS haptics), keep in platform branch
2. If feature should be cross-platform, add to core definition in modularization branch
3. Other platforms can optionally implement or provide graceful degradation

---

## Best Practices

### ✅ DO

- Work in your platform-specific branch for renderer code
- Sync with modularization branch frequently
- Run platform tests before pushing
- Document platform-specific enhancements
- Update component registry after adding components

### ❌ DON'T

- Commit platform code to modularization branch
- Commit core code to platform branches
- Merge directly to main (use integration branch)
- Skip integration testing
- Break component API compatibility without team discussion

---

## Branching Commands Cheat Sheet

```bash
# List all branches
git branch -a | grep avamagic

# Switch to platform branch
git checkout avamagic/android
git checkout avamagic/ios
git checkout avamagic/web
git checkout avamagic/desktop

# Switch to common/core branch
git checkout avamagic/modularization

# Switch to integration branch
git checkout avamagic/integration

# Sync platform with latest core
git checkout avamagic/android
git merge avamagic/modularization

# Push platform changes
git push origin avamagic/android

# Create feature branch from platform
git checkout avamagic/android
git checkout -b avamagic/android/feature/magic-tags

# Delete local branch
git branch -d avamagic/android/feature/magic-tags

# Delete remote branch
git push origin --delete avamagic/android/feature/magic-tags
```

---

## Current Status

| Branch | Status | Components | Last Updated |
|--------|--------|------------|--------------|
| avamagic/modularization | ✅ Active | 263 definitions | 2025-11-23 |
| avamagic/android | ✅ Active | 170 implemented | 2025-11-23 |
| avamagic/ios | ✅ Active | 170 implemented | 2025-11-23 |
| avamagic/web | 🔄 In Progress | 228 implemented | 2025-11-23 |
| avamagic/desktop | 🔄 In Progress | 109 implemented | 2025-11-23 |
| avamagic/integration | 📋 Pending | - | Not yet merged |

---

## Support

For questions about the branching strategy:
1. Check this document first
2. Review `.gitignore.platform-strategy`
3. Ask in team chat
4. Create an issue in the repo

---

**Last Updated:** 2025-11-23
**Maintained By:** Platform Integration Team
