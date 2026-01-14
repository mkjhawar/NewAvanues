# Global Design Standard: Development Protocols & Best Practices

**Version**: 1.0
**Last Updated**: 2025-11-11
**Scope**: Universal (applies to ANY Kotlin/KMP project)
**Purpose**: Proven protocols for preventing common development mistakes

---

## Overview

This document contains **universal development protocols** that apply to any Kotlin/Kotlin Multiplatform project. These protocols are language/platform-specific best practices, not tied to any particular codebase.

For project-specific learnings, see `/docs/Development-Learnings.md` in your project.

---

## Protocol #1: Search Before Creating (MANDATORY)

### The 5-Minute Rule

**Before writing ANY new infrastructure class, interface, or system:**

Spend **5 minutes** searching to verify it doesn't already exist.

### Search Protocol

```bash
# 1. Search for similar class names
grep -r "class.*YourClassName" --include="*.kt"
grep -r "interface.*YourClassName" --include="*.kt"

# 2. Search for similar functionality by keyword
grep -r "keyword1\|keyword2\|keyword3" --include="*.kt"

# 3. Search the target module
find modules/YourModule -name "*.kt" | xargs grep -l "keyword"

# 4. List existing files in the same package
ls -la modules/YourModule/src/commonMain/kotlin/path/to/package/

# 5. Check build.gradle.kts dependencies
cat build.gradle.kts | grep -A 10 "dependencies"
# Might already have library that provides functionality
```

### Why This Matters

**Time Investment**: 5-10 minutes of searching
**Time Saved**: Hours to weeks of duplicate work

**Common Scenarios**:
- ✅ Interface already defined, just needs implementation
- ✅ Implementation exists but not registered/used
- ✅ Functionality provided by existing dependency
- ✅ Similar code in different module can be extracted to shared library

### Example Application

**Scenario**: Need caching for Asset Manager

**Wrong Approach** ❌:
```kotlin
// Start coding immediately
class AssetCache<K, V>(private val maxSize: Int = 100) {
    private val cache = LinkedHashMap<K, V>()
    // ... 100 lines of custom LRU implementation
}
```

**Correct Approach** ✅:
```bash
# Search first
grep -r "interface.*Cache" --include="*.kt"
# Found: AssetCache interface already exists!

grep -r "class.*Cache" --include="*.kt"
# Found: InMemoryAssetCache implementation already complete!

# Result: Use existing code, save 8-12 hours
```

---

## Protocol #2: Type Mismatch Investigation

### When You See "Type mismatch" Errors

**Common Error Patterns**:
```
e: Type mismatch: inferred type is X but Y was expected
e: Unresolved reference: someMethod
e: None of the following candidates is applicable
```

### Investigation Protocol

**Step 1: Check for Dual Type Systems**

Many projects have multiple versions of same type during refactoring/migration:

```bash
# Find ALL definitions of the type
grep -r "class Color\|data class Color\|interface Color" --include="*.kt"

# Check both old and new locations
ls -la */core/Color.kt
ls -la */core/types/Color.kt
ls -la */v1/Color.kt
ls -la */v2/Color.kt
```

**Step 2: Trace Imports in Working Code**

```bash
# See which type working code uses
grep -r "import.*Color" path/to/working/code/

# Compare with your failing code
grep -r "import.*Color" path/to/failing/code/
```

**Step 3: Align Imports**

Import the same type that working code uses:

```kotlin
// If working code has:
import com.example.core.Color

// Your code should have:
import com.example.core.Color
// NOT: import com.example.types.Color
```

### Why This Matters

**What looks like**: "Missing APIs, need 2-4 weeks to refactor"
**What it actually is**: "Wrong import, need 15 minutes to fix"

90% of "architectural blockers" are import errors.

---

## Protocol #3: Nested Enum Investigation

### When You See "Unresolved reference: EnumName"

**Common Pattern**:
```
e: Unresolved reference: FontWeight
e: Unresolved reference: ColorMode
e: Unresolved reference: Status
```

### Investigation Protocol

**Check if enum is nested inside a class:**

```bash
# Search for enum definition
grep -r "enum class FontWeight" --include="*.kt"

# Often you'll find it's nested:
# data class Font(...) {
#     enum class Weight { Bold, Regular, Light }
# }
```

### Fix Pattern

```kotlin
// Wrong - Trying to use as top-level type
val weight: FontWeight = FontWeight.Bold // ❌ Unresolved reference

// Correct - Use fully qualified nested type
val weight: Font.Weight = Font.Weight.Bold // ✅

// Wrong import
import com.example.FontWeight // ❌ Doesn't exist

// Correct import
import com.example.Font // ✅ Import outer class
```

### Common Nested Enums in Kotlin

- `Font.Weight` (not `FontWeight`)
- `Font.Style` (not `FontStyle`)
- `ColorScheme.Mode` (not `ColorMode`)
- `Dialog.Result` (not `DialogResult`)
- `Http.Status` (not `HttpStatus`)

---

## Protocol #4: Kotlin Multiplatform Common Code

### Platform-Specific Code in commonMain

**Common Build Errors**:
```
e: Unresolved reference: System
e: Ambiguous reference: toByteArray
e: Cannot access class 'java.io.File'
```

### KMP API Replacements

| JVM-Only Code ❌ | KMP Compatible Code ✅ |
|------------------|------------------------|
| `System.currentTimeMillis()` | `Clock.System.now().toEpochMilliseconds()` |
| `string.toByteArray()` | `string.encodeToByteArray()` |
| `Thread.sleep(1000)` | `delay(1000)` (coroutines) |
| `File("path")` | Use expect/actual pattern |
| `System.getProperty("key")` | Use expect/actual pattern |
| `println()` for logging | Use platform logger (expect/actual) |

### Required Imports for KMP

```kotlin
// For time operations
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

// For coroutines
import kotlinx.coroutines.delay

// For byte operations
// (no import needed, built into Kotlin stdlib)
```

### expect/actual Pattern

**For truly platform-specific code:**

```kotlin
// commonMain
expect fun getCurrentPlatform(): String
expect class FileSystem {
    fun readFile(path: String): String
}

// androidMain
actual fun getCurrentPlatform(): String = "Android"
actual class FileSystem {
    actual fun readFile(path: String): String {
        return File(path).readText() // Android-specific
    }
}

// iosMain
actual fun getCurrentPlatform(): String = "iOS"
actual class FileSystem {
    actual fun readFile(path: String): String {
        // iOS-specific implementation
    }
}
```

---

## Protocol #5: Kotlin Math Functions

### pow() Type Requirements

**Common Error**:
```
e: Unresolved reference: pow
e: Type mismatch: found Float but Double was expected
```

### The Issue

`kotlin.math.pow()` requires **both parameters** to be `Double`, not `Float`.

### Fix Pattern

```kotlin
// Wrong - mixing Float and Double
val result = pow(value.toDouble(), 2.4f) // ❌

// Wrong - toDouble() on wrong part
val result = pow((value + 0.055f) / 1.055f.toDouble(), 2.4) // ❌

// Correct - convert entire expression to Double
val result = ((value + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat() // ✅

// Alternative - use standalone function
import kotlin.math.pow
val result = pow(value.toDouble(), 2.4).toFloat() // ✅
```

### General Rule

**For any Kotlin math function requiring Double**:
1. Convert Float → Double
2. Perform computation
3. Convert result back to Float (if needed)

---

## Protocol #6: Gradle Dependency Conflicts

### KMP Testing Dependencies

**Common Error**:
```
Error: JUnit5 vs JUnit4 mismatch
```

### The Issue

Not all test frameworks work with all KMP targets.

### Dependency Rules

| Target Platform | Correct Test Dependency | Wrong Dependency |
|----------------|------------------------|------------------|
| Android KMP | `kotlin("test-junit")` ✅ | `kotlin("test-junit5")` ❌ |
| JVM only | `kotlin("test-junit5")` ✅ | `kotlin("test-junit")` ⚠️ (works but old) |
| iOS/Native | `kotlin("test")` ✅ | `kotlin("test-junit")` ❌ |
| JS | `kotlin("test-js")` ✅ | `kotlin("test-junit")` ❌ |

### Configuration Example

```kotlin
kotlin {
    // Android target
    androidTarget {
        // ...
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test")) // ✅ Works for all targets
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test-junit")) // ✅ Android-specific
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5")) // ✅ JVM-specific
            }
        }
    }
}
```

---

## Quick Reference Cheat Sheet

### Before Coding Checklist

- [ ] **Searched** for existing implementations (5 minutes)
- [ ] **Read** existing files in same package
- [ ] **Checked** for interfaces that might already exist
- [ ] **Verified** imports match working code
- [ ] **Tested** building existing code before modifying

### Debugging Compilation Errors

| Error Type | First Action |
|------------|-------------|
| Type mismatch | Check for dual type systems |
| Unresolved reference | Check for nested classes/enums |
| Platform-specific error | Use KMP-compatible APIs |
| Import error | Trace imports in working code |
| Dependency conflict | Check target compatibility |

### Common Mistake Patterns

**90% of "architectural blockers" are actually**:
- ❌ Wrong imports (importing wrong package)
- ❌ Wrong type package (types.X vs core.X)
- ❌ Nested enum references (EnumName vs Class.EnumName)
- ❌ Platform-specific code in common code
- ❌ Wrong function signatures (Float vs Double)

**NOT architectural problems requiring weeks of work!**

---

## Protocol Compliance

### When to Apply These Protocols

**MANDATORY** (MUST apply):
- Protocol #1 (Search Before Creating) - Before ANY infrastructure work
- Protocol #3 (Nested Enums) - When seeing "Unresolved reference" for enums
- Protocol #4 (KMP APIs) - When writing commonMain code

**RECOMMENDED** (SHOULD apply):
- Protocol #2 (Type Mismatches) - When seeing type errors
- Protocol #5 (Math Functions) - When using kotlin.math
- Protocol #6 (Dependencies) - When configuring test frameworks

**Time Savings**:
Following these protocols can save **hours to weeks** on typical tasks.

---

## Updates

**Version History**:
- v1.0 (2025-11-11) - Initial universal protocols extracted from Avanues learnings

**Next Update**: Add protocol for coroutine context handling in KMP

---

**Scope**: Universal - applies to ANY Kotlin/KMP project
**Complement With**: `/docs/Development-Learnings.md` in your project for project-specific patterns

---

*These are universal best practices. Not all protocols may apply to every project.*
