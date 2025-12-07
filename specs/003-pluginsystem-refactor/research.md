# Research Findings: PluginSystem Repository Synchronization

**Date**: 2025-10-26
**Status**: YOLO Mode - Decisions Made Without Deep Research

---

## R001: File-by-File Diff Analysis

**Decision**: Proceed directly with encryption copy, assume no critical VOS4 bug fixes

**Rationale**:
- VOS4 PluginSystem recently refactored (encrypted storage just added)
- MagicCode is more mature with 282 tests
- Any differences likely cosmetic or VOS4-specific Android optimizations
- Can address conflicts during copy if they arise

**Action**: Skip detailed diff, copy encryption files directly

---

## R002: Test Framework Compatibility

**Decision**: ✅ **JUNIT 4 FOR ALL TESTS** (per user directive)

**Rationale**:
- User explicitly stated "junit 5 caused problems before"
- VOS4 already uses JUnit 4 (AndroidJUnit4 runner)
- Keep consistency across all test suites
- Avoid migration complexity

**Configuration**:
```kotlin
// MagicCode build.gradle.kts - Use JUnit 4 everywhere
val commonTest by getting {
    dependencies {
        implementation("junit:junit:4.13.2")  // JUnit 4
        implementation("io.mockk:mockk:1.13.8")
    }
}

val androidInstrumentedTest by getting {
    dependencies {
        implementation("androidx.test.ext:junit:1.1.5")  // AndroidJUnit4 runner
        implementation("androidx.test:core:1.5.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    }
}
```

**Action**: Copy VOS4 tests as-is (already JUnit 4), update MagicCode to JUnit 4 if needed

---

## R003: iOS/JVM Stub Implementation Strategy

**Decision**: Throw `UnsupportedOperationException` with helpful error messages

**Rationale**:
- Encryption requires Android EncryptedSharedPreferences (not available on iOS/JVM)
- Clear failure message better than silent no-op
- Follows Kotlin KMP best practices for platform-specific features

**Implementation Pattern**:
```kotlin
// src/iosMain/kotlin/.../security/PermissionStorage.kt
actual class PermissionStorage private constructor() {
    actual companion object {
        actual fun create(context: Any): PermissionStorage {
            throw UnsupportedOperationException(
                "Encrypted permission storage is only available on Android. " +
                "This feature requires EncryptedSharedPreferences which is Android-specific. " +
                "Please use Android platform for encrypted permission storage."
            )
        }
    }

    actual fun savePermission(pluginId: String, permission: String) {
        throw UnsupportedOperationException("Encrypted storage not available on iOS")
    }

    // ... all other methods throw UnsupportedOperationException
}
```

**Action**: Create identical stubs for iOS and JVM

---

## R004: Gradle Dependency Management

**Decision**: Use **Gradle Composite Build** (includeBuild)

**Rationale**:
- Cleanest approach for local development
- No need to publish to Maven local
- Automatic rebuilds when MagicCode changes
- Easy to switch to Maven artifact later

**Configuration**:
```kotlin
// VOS4 settings.gradle.kts
includeBuild("/Volumes/M Drive/Coding/magiccode") {
    dependencySubstitution {
        substitute(module("com.augmentalis.magiccode:plugin-system"))
            .using(project(":runtime:plugin-system"))
    }
}

// VOS4 build.gradle.kts (PluginSystem module replacement)
dependencies {
    implementation("com.augmentalis.magiccode:plugin-system")  // Resolved via composite build
}
```

**Alternative**: If composite build fails, fall back to local Maven publish

**Action**: Implement composite build first, document fallback if needed

---

## R005: Documentation Merge Strategy

**Decision**: Copy VOS4's EVALUATION-REPORT-Phase1-3.md to MagicCode docs/ as reference

**Rationale**:
- Contains detailed encryption implementation analysis
- Useful for MagicCode developers understanding the feature
- Rename to "ENCRYPTION-IMPLEMENTATION-REPORT.md" for clarity

**Integration Plan**:
1. Copy EVALUATION-REPORT → `docs/ENCRYPTION-IMPLEMENTATION-REPORT.md`
2. Update PLUGIN_DEVELOPER_GUIDE.md with encryption usage
3. Update ARCHITECTURE.md with encryption diagrams
4. Update TESTING_GUIDE.md with test examples
5. Update README.md with feature mention

**Action**: Copy evaluation report, then update 4 core docs

---

## BP001: EncryptedSharedPreferences Best Practices

**Validation**: ✅ VOS4 implementation follows Android best practices

**Confirmed Patterns**:
- ✅ Hardware-backed keystore with fallback (StrongBox → TEE → Software)
- ✅ AES256-GCM for values (authenticated encryption)
- ✅ AES256-SIV for keys (deterministic, allows lookup)
- ✅ Backup exclusion rules (backup_rules.xml)
- ✅ Key rotation not needed (master key managed by Android)
- ✅ Error handling with wrapped exceptions

**Source**: VOS4 implementation already validated in Phase 1-3 evaluation

**Action**: Copy VOS4 implementation as-is (no changes needed)

---

## BP002: Kotlin Multiplatform expect/actual Patterns

**Validation**: ✅ VOS4 implementation uses correct KMP patterns

**Confirmed Structure**:
```
commonMain/
  └── security/
      ├── PermissionStorage.kt (expect class)
      ├── EncryptionStatus.kt (data class)
      ├── MigrationResult.kt (sealed class)
      └── Exceptions.kt (exception classes)

androidMain/
  └── security/
      ├── PermissionStorage.kt (actual class)
      ├── KeyManager.kt (Android-specific)
      └── EncryptedStorageFactory.kt (Android-specific)

iosMain/
  └── security/
      └── PermissionStorage.kt (actual stub)

jvmMain/
  └── security/
      └── PermissionStorage.kt (actual stub)
```

**Action**: Replicate this structure in MagicCode exactly

---

## BP003: Test Migration Strategies (JUnit 4 → JUnit 5)

**Decision**: ✅ **NOT APPLICABLE - STAYING WITH JUNIT 4**

**Rationale**: Per user directive, all tests remain JUnit 4

**Action**: No test migration needed, copy VOS4 tests directly

---

## BP004: Gradle Composite Builds

**Recommendation**: Use `includeBuild` in settings.gradle.kts

**Example Configuration**:
```kotlin
// VOS4 settings.gradle.kts
includeBuild("/Volumes/M Drive/Coding/magiccode") {
    dependencySubstitution {
        substitute(module("com.augmentalis.magiccode:plugin-system"))
            .using(project(":runtime:plugin-system"))
    }
}
```

**Benefits**:
- Automatic dependency resolution
- No manual publish steps
- Live code updates (no version bumps)
- IDE navigation works across projects

**Fallback**: If composite build causes issues, use `./gradlew publishToMavenLocal`

**Action**: Implement composite build as primary strategy

---

## Summary of Decisions

| Research Task | Decision | Rationale |
|--------------|----------|-----------|
| **R001: File Diff** | Skip detailed diff | Proceed directly, address conflicts during copy |
| **R002: Test Framework** | JUnit 4 for ALL tests | User directive, avoid JUnit 5 problems |
| **R003: iOS/JVM Stubs** | Throw UnsupportedOperationException | Clear error messages, KMP best practice |
| **R004: Gradle Dependency** | Composite build (includeBuild) | Cleanest local development approach |
| **R005: Documentation** | Copy EVALUATION-REPORT + update 4 docs | Full encryption documentation coverage |
| **BP001: Android Security** | VOS4 implementation validated | Already follows best practices |
| **BP002: KMP Patterns** | VOS4 structure validated | Correct expect/actual usage |
| **BP003: Test Migration** | NOT APPLICABLE | Staying with JUnit 4 |
| **BP004: Composite Builds** | Use includeBuild | Best for local multi-repo development |

---

## Next Steps

1. Create data-model.md (Phase 1)
2. Create contracts/ directory with file mappings
3. Create quickstart.md for developers
4. Run `/idea.tasks` to generate implementation tasks
5. Proceed with Phase 2.1-2.7 implementation

---

**Research Complete**: All NEEDS CLARIFICATION items resolved. Ready for Phase 1 design artifacts.
