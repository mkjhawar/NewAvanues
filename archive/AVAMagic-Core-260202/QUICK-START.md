# VoiceOS KMP Libraries - Quick Start Guide

**⚡ 5-Minute Integration Guide**

---

## Step 1: Add Dependencies (30 seconds)

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    // Type-safe error handling
    implementation("com.augmentalis.voiceos:result:1.0.0")

    // SHA-256 hashing
    implementation("com.augmentalis.voiceos:hash:1.0.0")

    // Configuration constants
    implementation("com.augmentalis.voiceos:constants:1.0.0")

    // SQL wildcard escaping
    implementation("com.augmentalis.voiceos:validation:1.0.0")

    // Exception hierarchy
    implementation("com.augmentalis.voiceos:exceptions:1.0.0")
}
```

**Sync Gradle** ✅

---

## Step 2: Common Usage Patterns (4 minutes)

### Replace Exceptions with Results

```kotlin
// ❌ Before: Exception-based
fun loadUser(id: String): User {
    return database.get(id) ?: throw NotFoundException()
}

// ✅ After: Type-safe
import com.augmentalis.voiceos.result.VoiceOSResult

fun loadUser(id: String): VoiceOSResult<User, Error> {
    return database.get(id)
        ?.let { VoiceOSResult.Success(it) }
        ?: VoiceOSResult.Failure(Error.NotFound)
}

// Usage:
when (val result = loadUser("123")) {
    is VoiceOSResult.Success -> show(result.value)
    is VoiceOSResult.Failure -> showError(result.error)
}
```

### Hash Content for Deduplication

```kotlin
import com.augmentalis.voiceos.hash.HashUtils

val hash = HashUtils.sha256(content)
if (seenHashes.contains(hash)) {
    println("Duplicate!")
}
```

### Use Constants Instead of Magic Numbers

```kotlin
// ❌ Before
if (depth > 50) return
delay(500)

// ✅ After
import com.augmentalis.voiceos.constants.VoiceOSConstants

if (depth > VoiceOSConstants.TreeTraversal.MAX_DEPTH) return
delay(VoiceOSConstants.Timing.THROTTLE_DELAY_MS)
```

### Escape SQL LIKE Queries

```kotlin
import com.augmentalis.voiceos.validation.SqlEscapeUtils

// User searches for "50% off"
val safe = SqlEscapeUtils.wrapWithWildcards(userInput)
// Returns: "%50\\% off%"

@Query("SELECT * FROM products WHERE name LIKE :pattern ESCAPE '\\'")
fun search(pattern: String): List<Product>

dao.search(safe)  // ✅ Safe from injection
```

### Use Structured Exceptions

```kotlin
import com.augmentalis.voiceos.exceptions.*

try {
    database.backup()
} catch (e: Exception) {
    throw DatabaseException.BackupException(
        "Backup failed",
        cause = e
    )
}

// Handle:
try {
    performBackup()
} catch (e: DatabaseException) {
    logger.error(e.getFullMessage())  // "[DB_BACKUP_FAILED] Backup failed"
}
```

---

## Step 3: Verify (30 seconds)

Build your project:

```bash
./gradlew build
```

✅ **Done!** You're using VoiceOS KMP libraries.

---

## Cheat Sheet

### Import Statements

```kotlin
// Result
import com.augmentalis.voiceos.result.VoiceOSResult

// Hash
import com.augmentalis.voiceos.hash.HashUtils

// Constants
import com.augmentalis.voiceos.constants.VoiceOSConstants

// Validation
import com.augmentalis.voiceos.validation.SqlEscapeUtils

// Exceptions
import com.augmentalis.voiceos.exceptions.*
```

### Most Common Operations

```kotlin
// Result monad
VoiceOSResult.Success(value)
VoiceOSResult.Failure(error)
result.map { /* transform */ }
result.mapError { /* transform error */ }

// Hashing
HashUtils.sha256("content")

// Constants (examples)
VoiceOSConstants.TreeTraversal.MAX_DEPTH
VoiceOSConstants.Cache.DEFAULT_CACHE_SIZE
VoiceOSConstants.Timing.THROTTLE_DELAY_MS
VoiceOSConstants.Network.HTTP_TIMEOUT_MS

// SQL escaping
SqlEscapeUtils.escapeLikePattern(text)
SqlEscapeUtils.wrapWithWildcards(text)
SqlEscapeUtils.containsWildcards(text)

// Exceptions
DatabaseException.BackupException(msg)
SecurityException.EncryptionException(msg)
CommandException.ExecutionException(msg)
exception.getFullMessage()
exception.isCausedBy<T>()
```

---

## When to Use What

| Problem | Library | Method |
|---------|---------|--------|
| Network call can fail | result | `VoiceOSResult<T, E>` |
| Detect duplicate content | hash | `HashUtils.sha256()` |
| Remove magic number | constants | `VoiceOSConstants.*` |
| User search input | validation | `SqlEscapeUtils.wrapWithWildcards()` |
| Database error | exceptions | `DatabaseException.*` |
| Security error | exceptions | `SecurityException.*` |
| Command error | exceptions | `CommandException.*` |

---

## Troubleshooting

### "Unresolved reference" error?

**Solution:** Publish to Maven Local:
```bash
./gradlew publishToMavenLocal
```

### Need more examples?

**See:**
- Full README: `/libraries/core/README.md`
- Developer Guide: `/app/DEVELOPER_GUIDE.md` (Chapter 7)
- Test files: `/libraries/core/{library}/src/commonTest/kotlin/`

---

**Total Time:** 5 minutes ⏱️
**Difficulty:** Easy 🟢
**Support:** Contact VoiceOS development team
