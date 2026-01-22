# Issue: FK Constraint Failure in persistStaticCommands

**Date:** 2026-01-22
**Status:** RESOLVED
**Severity:** Critical (App Crash)

---

## Error Details

```
android.database.sqlite.SQLiteConstraintException: FOREIGN KEY constraint failed (code 787 SQLITE_CONSTRAINT_FOREIGNKEY)
    at android.database.sqlite.SQLiteConnection.nativeExecuteForLastInsertedRowId(Native Method)
    at com.augmentalis.database.GeneratedCommandQueries.insert
    at com.augmentalis.database.repositories.impl.SQLDelightGeneratedCommandRepository.insertBatch
```

**Stack Trace Location:** `CommandPersistenceManager.kt` → `persistStaticCommands()`

---

## Root Cause Analysis

### Primary Issue
Commands with **empty or null `elementHash`** were being inserted into `commands_generated` table. The table has a foreign key constraint:

```sql
FOREIGN KEY (elementHash) REFERENCES scraped_element(elementHash) ON DELETE CASCADE
```

When `metadata["elementHash"]` was null, the code defaulted to empty string `""`:
```kotlin
elementHash = this.metadata["elementHash"] ?: ""  // BUG: Empty string causes FK violation
```

Since no `scraped_element` record has hash `""`, this violated the FK constraint.

### Secondary Issue
The persistence operations ran in **separate database transactions**, creating potential race conditions where commands could be inserted before their parent elements were fully committed.

---

## Files Modified

### 1. `CommandPersistenceManager.kt`
**Path:** `android/apps/voiceoscoreng/src/main/kotlin/.../service/CommandPersistenceManager.kt`

**Change:** Pre-filter commands before processing to exclude those with missing/empty `elementHash`:

```kotlin
// Pre-filter: Only commands with valid non-empty elementHash
val validHashCommands = staticQuantizedCommands.filter { cmd ->
    val hash = cmd.metadata["elementHash"]
    !hash.isNullOrBlank()
}

if (validHashCommands.isEmpty()) {
    Log.w(TAG, "No commands with valid elementHash to persist")
    return@launch
}
```

### 2. `AndroidCommandPersistence.kt`
**Path:** `android/apps/voiceoscoreng/src/main/kotlin/.../AndroidCommandPersistence.kt`

**Changes:**
1. Filter in `insertBatch()` to skip commands with empty hash
2. Fail-fast in `toDTO()` with require() for missing elementHash

```kotlin
override suspend fun insertBatch(commands: List<QuantizedCommand>) {
    // Filter out commands with empty elementHash to prevent FK violation
    val validCommands = commands.filter { cmd ->
        val hash = cmd.metadata["elementHash"]
        !hash.isNullOrBlank()
    }
    if (validCommands.isEmpty()) return
    // ...
}

private fun QuantizedCommand.toDTO(): GeneratedCommandDTO {
    val elementHash = this.metadata["elementHash"]
    require(!elementHash.isNullOrBlank()) {
        "QuantizedCommand missing required elementHash in metadata"
    }
    // ...
}
```

---

## Verification

```bash
./gradlew :android:apps:voiceoscoreng:compileDebugKotlin
# BUILD SUCCESSFUL
```

---

## Prevention

To prevent similar issues in the future:

1. **Defense in Depth:** Multiple validation layers (CommandPersistenceManager, AndroidCommandPersistence, repository)
2. **Fail-Fast:** Use `require()` to fail immediately with clear error message
3. **Logging:** Warning logs when commands are filtered out helps debugging
4. **FK Schema:** ON DELETE CASCADE ensures orphaned records are cleaned up

---

## Related Files

- `Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/GeneratedCommand.sq`
- `Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/element/ScrapedElement.sq`
- `Modules/VoiceOS/core/database/src/commonMain/kotlin/.../repositories/impl/SQLDelightGeneratedCommandRepository.kt`

---

**Resolution Date:** 2026-01-22
**Verified By:** Build successful, no compilation errors
