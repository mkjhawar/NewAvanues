# Dependency Management - WebAvanue

**Status:** Active
**Last Updated:** 2025-12-11
**System:** Gradle Version Catalog

---

## Overview

WebAvanue uses **Gradle Version Catalogs** for centralized dependency management. This ensures consistent versions across all modules and simplifies updates.

### Benefits

| Benefit | Impact |
|---------|--------|
| Centralized Versions | Single source of truth for all dependencies |
| Type Safety | IDE autocomplete for dependency references |
| Consistent Updates | Update once, applies everywhere |
| Reduced Duplication | No version numbers scattered across build files |
| Easier Maintenance | Clear overview of all dependencies |

---

## Version Catalog Structure

**Location:** `/gradle/libs.versions.toml`

### File Organization

```toml
[versions]
# Version declarations

[libraries]
# Library coordinates with version refs

[plugins]
# Plugin declarations
```

### Current Version Catalog

**Summary of Key Dependencies:**

| Category | Library | Version |
|----------|---------|---------|
| **Build Tools** | Android Gradle Plugin | 8.7.3 |
| | Kotlin | 2.0.21 |
| **Compose** | Compose Multiplatform | 1.7.0 |
| | Material Icons Extended | 1.7.5 |
| **AndroidX** | Core KTX | 1.15.0 |
| | WebKit | 1.12.1 |
| | Lifecycle | 2.8.7 |
| | Activity Compose | 1.9.3 |
| | Navigation Compose | 2.8.5 |
| **Kotlin** | Coroutines | 1.9.0 |
| | Serialization | 1.6.3 |
| | DateTime | 0.6.1 |
| **Database** | SQLDelight | 2.0.1 |
| | SQLCipher | 4.5.4 |
| **Navigation** | Voyager | 1.0.0 |
| **Security** | Security Crypto | 1.1.0-alpha06 |
| **Monitoring** | Sentry | 7.16.0 |
| **Utilities** | Napier | 2.6.1 |
| | UUID | 0.8.1 |

---

## Using Version Catalog

### In build.gradle.kts

**Plugins:**
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
}
```

**Dependencies:**
```kotlin
dependencies {
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
```

### Naming Convention

Version catalog uses **kebab-case** for library references:

| Artifact ID | Version Catalog Reference |
|-------------|---------------------------|
| `activity-compose` | `libs.androidx.activity.compose` |
| `coroutines-android` | `libs.kotlinx.coroutines.android` |
| `material-icons-extended` | `libs.compose.material.icons.extended` |

---

## Dependency Update Schedule

### Monthly Review (Second Tuesday)

**Process:**

1. **Check for Updates**
   ```bash
   ./gradlew dependencyUpdates
   ```

2. **Review Changelog**
   - Check breaking changes
   - Review security fixes
   - Assess new features

3. **Update Version Catalog**
   ```toml
   # In gradle/libs.versions.toml
   [versions]
   androidx-webkit = "1.12.1"  # Updated from 1.9.0
   ```

4. **Test Build**
   ```bash
   ./gradlew clean build
   ./gradlew test
   ```

5. **Run Tests**
   - Unit tests
   - Integration tests
   - UI tests (if applicable)

6. **Create PR**
   - Title: `deps: Update dependencies (YYYY-MM-DD)`
   - Description: List updated libraries + changelogs

### Security Updates (Immediate)

When security vulnerabilities discovered:

1. **Identify Affected Dependency**
   - Check GitHub Security Advisories
   - Review Snyk/Dependabot alerts

2. **Update Immediately**
   ```toml
   [versions]
   vulnerable-lib = "X.Y.Z"  # Update to patched version
   ```

3. **Test Critical Paths**
   - Focus on security-related functionality
   - Verify no breaking changes

4. **Emergency Release**
   - Skip normal review cycle
   - Deploy as hotfix

---

## Major Version Updates

### Planning Phase

**Before updating major versions:**

1. **Review Migration Guide**
   - Check official documentation
   - Identify breaking changes
   - List deprecated APIs

2. **Create Feature Branch**
   ```bash
   git checkout -b deps/major-update-LIBRARY-vX
   ```

3. **Incremental Updates**
   - Update one major version at a time
   - Test after each update
   - Document issues

### Update Process

**Example: Updating Kotlin 1.9 → 2.0:**

1. **Update Version Catalog**
   ```toml
   [versions]
   kotlin = "2.0.21"  # From 1.9.x
   ```

2. **Fix Breaking Changes**
   ```kotlin
   // Example: Update deprecated APIs
   // Old (Kotlin 1.9)
   someFunction()

   // New (Kotlin 2.0)
   someNewFunction()
   ```

3. **Run Full Test Suite**
   ```bash
   ./gradlew clean test
   ./gradlew connectedAndroidTest
   ```

4. **Performance Testing**
   - Compare build times
   - Check runtime performance
   - Measure app size

5. **Document Changes**
   ```markdown
   ## Kotlin 2.0 Migration

   ### Breaking Changes
   - API X replaced with Y
   - Feature Z removed

   ### Benefits
   - Performance improvement: 15% faster builds
   - New language features: Context receivers
   ```

---

## Adding New Dependencies

### Process

1. **Evaluate Necessity**
   - Is it solving a real problem?
   - Can existing dependencies handle it?
   - What's the maintenance burden?

2. **Check Library Quality**
   - Active maintenance?
   - Good documentation?
   - Community support?
   - Security track record?

3. **Add to Version Catalog**
   ```toml
   [versions]
   new-lib = "1.0.0"

   [libraries]
   new-lib = { module = "com.example:new-lib", version.ref = "new-lib" }
   ```

4. **Use in build.gradle.kts**
   ```kotlin
   dependencies {
       implementation(libs.new.lib)
   }
   ```

5. **Document Usage**
   - Update this file with new dependency
   - Document why it was added
   - Link to official docs

### Evaluation Criteria

| Criterion | Weight | Minimum Score |
|-----------|--------|---------------|
| Active Maintenance | High | Last commit < 6 months |
| Documentation | High | Comprehensive docs |
| Security | Critical | No known vulnerabilities |
| Community | Medium | 100+ GitHub stars |
| License | Critical | Compatible with project |
| Size | Medium | < 2MB for mobile |

---

## Removing Dependencies

### Deprecation Process

1. **Identify Candidate**
   - Unused library
   - Better alternative available
   - Maintenance burden too high

2. **Find Alternatives**
   - Can we use existing dependencies?
   - Is there a better replacement?
   - Can we implement in-house?

3. **Create Removal Plan**
   ```markdown
   ## Deprecation Plan: Old-Lib

   **Reason:** Better alternative available (New-Lib)

   **Timeline:**
   - Week 1: Add New-Lib alongside Old-Lib
   - Week 2-3: Migrate all usage to New-Lib
   - Week 4: Remove Old-Lib

   **Migration:**
   - Replace X with Y
   - Update Z to use new API
   ```

4. **Execute Migration**
   - Add replacement
   - Migrate incrementally
   - Test thoroughly
   - Remove old dependency

5. **Update Documentation**
   - Remove from version catalog
   - Update this document
   - Document breaking changes

---

## Dependency Conflicts

### Resolving Conflicts

**Issue:** Two dependencies require different versions of the same library.

**Solution 1: Force Resolution**
```kotlin
configurations.all {
    resolutionStrategy {
        force("com.example:library:2.0.0")
    }
}
```

**Solution 2: Exclude Transitive Dependency**
```kotlin
dependencies {
    implementation(libs.library.a) {
        exclude(group = "com.example", module = "conflict-lib")
    }
}
```

**Solution 3: Update Version Catalog**
```toml
# Use newer version that satisfies both requirements
[versions]
conflict-lib = "2.0.0"
```

### Checking for Conflicts

```bash
# View dependency tree
./gradlew app:dependencies

# Check for version conflicts
./gradlew app:dependencyInsight --dependency conflict-lib
```

---

## Build Performance

### Optimization Tips

1. **Enable Configuration Cache**
   ```properties
   # gradle.properties
   org.gradle.configuration-cache=true
   ```

2. **Use Parallel Builds**
   ```properties
   org.gradle.parallel=true
   ```

3. **Increase Memory**
   ```properties
   org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g
   ```

4. **Enable Build Cache**
   ```properties
   org.gradle.caching=true
   ```

### Dependency Impact

| Dependency | Build Time Impact | APK Size Impact |
|------------|-------------------|-----------------|
| Compose | +30s | +2MB |
| Coroutines | +5s | +500KB |
| SQLDelight | +10s | +1MB |
| Sentry | +15s | +1.5MB |

---

## Security Best Practices

### Vulnerability Scanning

**Enable Dependabot (GitHub):**
```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
```

**Manual Check:**
```bash
# Using OWASP Dependency Check
./gradlew dependencyCheckAnalyze
```

### Trusted Sources

**Only use dependencies from:**
- Maven Central
- Google Maven Repository
- JCenter (deprecated, migrate away)
- Gradle Plugin Portal

**Avoid:**
- Unknown/personal repositories
- Unverified JitPack packages
- Repositories without HTTPS

### Checksum Verification

Gradle automatically verifies checksums for downloaded dependencies. If you see checksum errors:

```
FAILED: Could not resolve artifact with hash mismatch
```

**Actions:**
1. Clear Gradle cache
2. Re-download dependency
3. If persists, report to dependency maintainer

---

## Version Catalog Maintenance

### File Structure Best Practices

1. **Group by Category**
   ```toml
   # AndroidX
   androidx-core-ktx = "..."
   androidx-lifecycle = "..."

   # Kotlin
   kotlinx-coroutines = "..."
   kotlinx-serialization = "..."
   ```

2. **Use Semantic Versioning**
   - MAJOR.MINOR.PATCH
   - Document breaking changes in MAJOR

3. **Comment Unusual Versions**
   ```toml
   # Pinned to 1.0.0-alpha06 due to critical security fix
   securityCrypto = "1.1.0-alpha06"
   ```

### Regular Cleanup

**Monthly:**
- Remove unused dependencies
- Update comments
- Verify all versions are referenced

**Quarterly:**
- Major version planning
- Dependency audit
- Performance review

---

## Troubleshooting

### Issue: Build Fails After Dependency Update

**Symptoms:**
- Compilation errors
- Linking errors
- Runtime crashes

**Solutions:**

1. **Check Breaking Changes**
   - Review library changelog
   - Check migration guide

2. **Incremental Rollback**
   ```bash
   git checkout HEAD~1 -- gradle/libs.versions.toml
   ./gradlew clean build
   ```

3. **Isolate Problem Dependency**
   - Update one at a time
   - Test after each update

### Issue: Version Catalog Not Recognized

**Symptoms:**
- `libs` reference unresolved
- Build fails with catalog errors

**Solutions:**

1. **Check File Location**
   - Must be at `gradle/libs.versions.toml`

2. **Sync Gradle**
   ```bash
   ./gradlew --refresh-dependencies
   ```

3. **Verify Syntax**
   - TOML syntax errors break catalog
   - Use TOML validator

### Issue: Dependency Not Found

**Symptoms:**
- `Could not find artifact` error

**Solutions:**

1. **Verify Artifact Coordinates**
   ```toml
   # Correct format
   library = { module = "group:artifact", version.ref = "version" }
   ```

2. **Check Repository**
   ```kotlin
   // In build.gradle.kts
   repositories {
       google()
       mavenCentral()
   }
   ```

3. **Clear Cache**
   ```bash
   ./gradlew --refresh-dependencies
   rm -rf ~/.gradle/caches
   ```

---

## References

- **Gradle Version Catalogs:** https://docs.gradle.org/current/userguide/platforms.html
- **Android Gradle Plugin:** https://developer.android.com/build/releases/gradle-plugin
- **Kotlin Releases:** https://kotlinlang.org/docs/releases.html
- **Compose Multiplatform:** https://www.jetbrains.com/lp/compose-multiplatform/

---

## Change Log

| Date | Change | Reason |
|------|--------|--------|
| 2025-12-11 | Migrated to version catalog | Centralized dependency management |
| 2025-12-11 | Updated all dependencies | Security patches + latest stable versions |

---

## Dependency Audit Log

**Last Audit:** 2025-12-11

| Dependency | Previous Version | New Version | Reason |
|------------|------------------|-------------|--------|
| AGP | 8.2.2 | 8.7.3 | Latest stable, performance improvements |
| androidx.webkit | 1.9.0 | 1.12.1 | Security patches, bug fixes |
| androidx.activity-compose | 1.8.2 | 1.9.3 | Latest stable, new features |
| androidx.navigation-compose | 2.7.6 | 2.8.5 | Latest stable, bug fixes |
| androidx.lifecycle | 2.6.2 | 2.8.7 | Latest stable, lifecycle improvements |
| Compose Material | 1.5.4 | 1.7.5 | Latest stable, new components |
| Kotlin Coroutines | 1.7.3 | 1.9.0 | Latest stable, performance improvements |
| Sentry | 7.0.0 | 7.16.0 | Latest stable, new features |

---

## Next Review

**Scheduled:** 2025-01-14 (Monthly review)

**Focus Areas:**
- Check for new AndroidX releases
- Review Compose updates
- Kotlin 2.1 evaluation
- Security vulnerability scan

