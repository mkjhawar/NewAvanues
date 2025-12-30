# Quick Reference: Intent Examples Database

**Last Updated:** 2025-11-10
**Component:** NLU Intent Classification

---

## TL;DR

Intent training examples are now stored in Room database (`intent_examples` table) instead of static JSON. The migration happens automatically on first run.

---

## For Developers

### Adding New Static Examples

1. **Update JSON file:**
   ```json
   // apps/ava-standalone/src/main/assets/intent_examples.json
   {
     "control_lights": [
       "Turn on the lights",
       "Switch off the lights",
       "NEW: Lights please"  // ← Add here
     ]
   }
   ```

2. **Clear and re-migrate:**
   ```kotlin
   val migration = IntentExamplesMigration(context)
   migration.clearDatabase()
   migration.forceMigration()
   ```

3. **Or just uninstall and reinstall the app** (database will auto-migrate)

---

### Adding User-Taught Examples

```kotlin
// Get DAO
val database = DatabaseProvider.getDatabase(context)
val dao = database.intentExampleDao()

// Create example
val example = IntentExampleEntity(
    exampleHash = IntentExamplesMigration.generateHash(
        intentId = "control_lights",
        exampleText = "Lights on please"
    ),
    intentId = "control_lights",
    exampleText = "Lights on please",
    isPrimary = false,
    source = "USER_TAUGHT",
    locale = "en-US",
    createdAt = System.currentTimeMillis()
)

// Insert
dao.insertIntentExample(example)
```

---

### Querying Examples

```kotlin
val dao = DatabaseProvider.getDatabase(context).intentExampleDao()

// Get all examples for an intent
val examples = dao.getExamplesForIntentOnce("control_lights")

// Get all examples
val allExamples = dao.getAllExamplesOnce()

// Get primary examples only
dao.getPrimaryExamples().collect { examples ->
    // Flow<List<IntentExampleEntity>>
}

// Check if database populated
if (dao.hasExamples()) {
    // Database ready
}
```

---

### Usage Analytics

```kotlin
// Track usage
dao.incrementUsage(exampleId, System.currentTimeMillis())

// Get most used examples
val topExamples = dao.getMostUsedExamples(10)

topExamples.forEach { example ->
    println("${example.intentId}: ${example.exampleText} (used ${example.usageCount} times)")
}

// Get counts per intent
val counts = dao.getExampleCountPerIntent()
// Returns: Map<String, Int> e.g., {"control_lights": 5, "check_weather": 3}
```

---

## For Testing

### Check Migration Status

```kotlin
val migration = IntentExamplesMigration(context)
val status = migration.getMigrationStatus()

println(status)
// {
//   "has_examples": true,
//   "total_count": 45,
//   "intent_counts": {
//     "control_lights": 5,
//     "check_weather": 5,
//     ...
//   }
// }
```

### Force Re-Migration

```kotlin
val migration = IntentExamplesMigration(context)

// Clear existing data
migration.clearDatabase()

// Re-migrate from JSON
val count = migration.forceMigration()
println("Migrated $count examples")
```

### Reset to JSON-Only

```kotlin
// Clear database
IntentExamplesMigration(context).clearDatabase()

// IntentClassifier will automatically fall back to JSON
```

---

## Database Schema

### Table: `intent_examples`

```sql
CREATE TABLE intent_examples (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    example_hash TEXT NOT NULL UNIQUE,
    intent_id TEXT NOT NULL,
    example_text TEXT NOT NULL,
    is_primary INTEGER NOT NULL DEFAULT 0,
    source TEXT NOT NULL DEFAULT 'STATIC_JSON',
    locale TEXT NOT NULL DEFAULT 'en-US',
    created_at INTEGER NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0,
    last_used INTEGER
)
```

### Field Meanings

| Field | Description | Example |
|-------|-------------|---------|
| `example_hash` | MD5(intent_id + example_text) | `3f5a8d2c...` |
| `intent_id` | Intent identifier | `"control_lights"` |
| `example_text` | Natural language example | `"Turn on the lights"` |
| `is_primary` | 1 = JSON, 0 = user-added | `1` |
| `source` | Origin | `"STATIC_JSON"`, `"USER_TAUGHT"`, `"AUTO_LEARNED"` |
| `locale` | Language code | `"en-US"`, `"es-ES"` |
| `usage_count` | Times used in classification | `42` |
| `last_used` | Last usage timestamp | `1699564800000` |

---

## Common Issues

### Issue: Database not populating
**Cause:** JSON file not found or malformed
**Fix:** Check `apps/ava-standalone/src/main/assets/intent_examples.json` exists

### Issue: Duplicates after migration
**Cause:** Unique constraint prevents this (by design)
**Fix:** No action needed, duplicates are automatically skipped

### Issue: Classification not working
**Cause:** Database empty, embeddings not computed
**Fix:** Check logs for migration errors, ensure JSON file is valid

### Issue: Old examples still showing
**Cause:** Database not cleared after JSON update
**Fix:** Run `migration.clearDatabase()` then `migration.forceMigration()`

---

## Logs to Check

```
I/IntentClassifier: Migrated intent examples from JSON to database
I/IntentExamplesMigration: Inserted 45 examples (0 duplicates skipped)
I/IntentClassifier: Pre-computation complete: 9 intents ready
```

---

## Performance

| Metric | Value |
|--------|-------|
| Load time | ~120ms |
| Memory | ~12KB |
| Storage | ~15KB (45 examples) |

---

## Files

- **Entity:** `/Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/entity/IntentExampleEntity.kt`
- **DAO:** `/Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/dao/IntentExampleDao.kt`
- **Migration:** `/Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentExamplesMigration.kt`
- **Classifier:** `/Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentClassifier.kt`
- **JSON Source:** `/apps/ava-standalone/src/main/assets/intent_examples.json`

---

## Documentation

- **Schema:** `/docs/DATABASE_SCHEMA_INTENT_EXAMPLES.md`
- **Migration Guide:** `/docs/MIGRATION_INTENT_EXAMPLES_JSON_TO_DATABASE.md`
- **Summary:** `/docs/AGENT2_SUMMARY_JSON_TO_DATABASE_MIGRATION.md`
- **Quick Reference:** `/docs/QUICK_REFERENCE_INTENT_EXAMPLES_DATABASE.md` (this file)

---

## Author

Manoj Jhawar
Augmentalis Inc, Intelligent Devices LLC
manoj@ideahq.net
