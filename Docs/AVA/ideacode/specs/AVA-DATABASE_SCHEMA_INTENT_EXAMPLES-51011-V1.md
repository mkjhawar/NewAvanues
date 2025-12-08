# Intent Examples Database Schema

**Date:** 2025-11-10
**Database Version:** 2
**Component:** NLU Intent Classification

---

## Overview

The `intent_examples` table stores training examples for NLU intent classification. It replaces the static `intent_examples.json` file with a dynamic, queryable database that supports:

- Dynamic updates (user-taught examples)
- Usage analytics and tracking
- Multi-language support
- Efficient semantic similarity matching

---

## Table Schema

### `intent_examples`

```sql
CREATE TABLE intent_examples (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    example_hash TEXT NOT NULL,
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

### Indices

```sql
CREATE INDEX index_intent_examples_intent_id ON intent_examples(intent_id)
CREATE INDEX index_intent_examples_locale ON intent_examples(locale)
CREATE INDEX index_intent_examples_is_primary ON intent_examples(is_primary)
CREATE INDEX index_intent_examples_source ON intent_examples(source)
CREATE INDEX index_intent_examples_created_at ON intent_examples(created_at)
CREATE UNIQUE INDEX index_intent_examples_example_hash ON intent_examples(example_hash)
```

---

## Field Descriptions

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | INTEGER | NO | Auto-generated primary key |
| `example_hash` | TEXT | NO | MD5(intent_id + example_text) for deduplication |
| `intent_id` | TEXT | NO | Intent identifier (e.g., "control_lights", "check_weather") |
| `example_text` | TEXT | NO | Natural language example utterance |
| `is_primary` | INTEGER | NO | 1 for core examples from JSON, 0 for user-added |
| `source` | TEXT | NO | Origin: STATIC_JSON, USER_TAUGHT, AUTO_LEARNED |
| `locale` | TEXT | NO | Language code (e.g., "en-US", "es-ES") |
| `created_at` | INTEGER | NO | Timestamp when example was created (epoch millis) |
| `usage_count` | INTEGER | NO | Number of times this example contributed to classification |
| `last_used` | INTEGER | YES | Timestamp when this example was last matched (epoch millis) |

---

## VOS4 Patterns Applied

### 1. Hash-Based Deduplication
- **Pattern:** Unique constraint on `example_hash` prevents duplicate examples
- **Implementation:** MD5(intent_id + example_text)
- **Benefit:** Safe idempotent migrations, prevents user confusion

### 2. Usage Analytics
- **Pattern:** `usage_count` and `last_used` track example effectiveness
- **Use Cases:**
  - Identify most valuable training examples
  - Prune low-value examples
  - Improve model with high-performing examples

### 3. Source Provenance
- **Pattern:** `source` field tracks origin of each example
- **Values:**
  - `STATIC_JSON`: Original examples from JSON migration
  - `USER_TAUGHT`: User-added via Teach AVA feature
  - `AUTO_LEARNED`: System-learned from corrections
- **Benefit:** Audit trail, selective resets

### 4. Primary Flag
- **Pattern:** `is_primary` distinguishes core vs. supplementary examples
- **Use Cases:**
  - Reset to defaults (keep only is_primary=1)
  - Prioritize core examples in UI
  - Separate system vs. user content

### 5. Composite Indices
- **Pattern:** Multiple indices for different query patterns
- **Queries Optimized:**
  - Get all examples for an intent (intent_id index)
  - Filter by language (locale index)
  - Get primary examples only (is_primary index)
  - Analytics queries (created_at, usage_count)

---

## Migration Strategy

### Phase 1: Database Schema (v1 → v2)
**Implemented in:** `DatabaseMigrations.MIGRATION_1_2`

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create table
        database.execSQL("CREATE TABLE IF NOT EXISTS intent_examples ...")

        // Create indices
        database.execSQL("CREATE INDEX ...")
        database.execSQL("CREATE UNIQUE INDEX ...")
    }
}
```

### Phase 2: Data Migration (JSON → Database)
**Implemented in:** `IntentExamplesMigration.kt`

**Flow:**
1. On first NLU initialization, check `dao.hasExamples()`
2. If empty, load `intent_examples.json` from assets
3. Parse JSON: `{"intent_id": ["example1", "example2", ...]}`
4. Create `IntentExampleEntity` for each example:
   - `intentId` = JSON key
   - `exampleText` = array element
   - `isPrimary` = true
   - `source` = "STATIC_JSON"
   - `exampleHash` = MD5(intentId + exampleText)
5. Bulk insert via `dao.insertIntentExamples()`
6. Log migration summary

**Idempotency:**
- Safe to run multiple times
- Unique constraint on `example_hash` prevents duplicates
- `OnConflictStrategy.IGNORE` skips existing entries

### Phase 3: Classifier Integration
**Implemented in:** `IntentClassifier.precomputeIntentEmbeddings()`

**Loading Strategy:**
1. **Primary:** Load from database
   - `dao.getAllExamplesOnce()`
   - Group by `intent_id`
   - Compute embeddings per intent
2. **Fallback:** Load from JSON if database empty
   - Preserves backward compatibility
   - Handles edge cases (corrupted database, etc.)

---

## Query Patterns

### Get All Examples for Intent
```kotlin
val examples = dao.getExamplesForIntentOnce("control_lights")
// Returns: List<IntentExampleEntity>
```

### Get Primary Examples Only
```kotlin
dao.getPrimaryExamples().collect { examples ->
    // Flow<List<IntentExampleEntity>>
}
```

### Check if Migration Needed
```kotlin
if (!dao.hasExamples()) {
    IntentExamplesMigration(context).migrateIfNeeded()
}
```

### Increment Usage Analytics
```kotlin
dao.incrementUsage(exampleId, System.currentTimeMillis())
```

### Get Top Performing Examples
```kotlin
val topExamples = dao.getMostUsedExamples(limit = 10)
```

---

## Example Data

### JSON Format (Original)
```json
{
  "control_lights": [
    "Turn on the lights",
    "Switch off the lights",
    "Dim the lights"
  ],
  "check_weather": [
    "What's the weather",
    "Will it rain today"
  ]
}
```

### Database Records (After Migration)
```
id | example_hash                     | intent_id       | example_text         | is_primary | source       | locale | created_at    | usage_count | last_used
---|----------------------------------|-----------------|----------------------|------------|--------------|--------|---------------|-------------|----------
1  | 3f5a8... | control_lights  | Turn on the lights   | 1          | STATIC_JSON  | en-US  | 1699564800000 | 0           | null
2  | 7b2c1... | control_lights  | Switch off the lights| 1          | STATIC_JSON  | en-US  | 1699564800000 | 0           | null
3  | 9d4e2... | control_lights  | Dim the lights       | 1          | STATIC_JSON  | en-US  | 1699564800000 | 0           | null
4  | 2a8f3... | check_weather   | What's the weather   | 1          | STATIC_JSON  | en-US  | 1699564800000 | 0           | null
5  | 6c1b9... | check_weather   | Will it rain today   | 1          | STATIC_JSON  | en-US  | 1699564800000 | 0           | null
```

---

## Future Enhancements

### 1. User-Taught Examples
**Status:** Schema ready, feature pending

```kotlin
// User teaches new example via Teach AVA
val newExample = IntentExampleEntity(
    exampleHash = generateHash("control_lights", "Lights on please"),
    intentId = "control_lights",
    exampleText = "Lights on please",
    isPrimary = false,  // User-added
    source = "USER_TAUGHT",
    locale = "en-US",
    createdAt = System.currentTimeMillis()
)
dao.insertIntentExample(newExample)
```

### 2. Auto-Learning from Corrections
**Status:** Schema ready, feature pending

```kotlin
// User corrects misclassification
val learnedExample = IntentExampleEntity(
    exampleHash = generateHash("check_weather", "Is it raining"),
    intentId = "check_weather",  // Corrected intent
    exampleText = "Is it raining",
    isPrimary = false,
    source = "AUTO_LEARNED",
    locale = "en-US",
    createdAt = System.currentTimeMillis()
)
dao.insertIntentExample(learnedExample)
```

### 3. Multi-Language Support
**Status:** Schema ready, feature pending

```kotlin
// Add Spanish examples
val spanishExample = IntentExampleEntity(
    exampleHash = generateHash("control_lights", "Enciende las luces"),
    intentId = "control_lights",
    exampleText = "Enciende las luces",
    isPrimary = true,
    source = "STATIC_JSON",
    locale = "es-ES",
    createdAt = System.currentTimeMillis()
)
dao.insertIntentExample(spanishExample)
```

### 4. Usage Analytics Dashboard
**Status:** DAO queries implemented, UI pending

```kotlin
// Get most effective examples
val topExamples = dao.getMostUsedExamples(10)

// Get example counts per intent
val counts = dao.getExampleCountPerIntent()
// Returns: Map<String, Int> e.g., {"control_lights": 5, "check_weather": 3}
```

---

## Testing Checklist

- [x] Migration creates table successfully
- [x] Unique constraint on example_hash works
- [x] Indices created correctly
- [x] JSON migration populates database
- [x] IntentClassifier loads from database
- [x] Backward compatibility (JSON fallback) works
- [ ] User-taught examples insert correctly
- [ ] Usage analytics update correctly
- [ ] Multi-language examples work
- [ ] Migration handles large datasets (1000+ examples)

---

## Maintenance Notes

### Adding New Static Examples
1. Update `intent_examples.json` in assets
2. Clear database: `IntentExamplesMigration.clearDatabase()`
3. Re-run migration: `IntentExamplesMigration.forceMigration()`

### Resetting User Examples
```kotlin
// Keep only primary (static) examples
val allExamples = dao.getAllExamplesOnce()
allExamples.filter { !it.isPrimary }.forEach { example ->
    dao.deleteExample(example.id)
}
```

### Database Size Estimation
- **Per Example:** ~150 bytes (text + metadata)
- **9 intents × 5 examples:** ~6.75 KB
- **100 intents × 10 examples:** ~150 KB
- **1000 user-taught examples:** ~150 KB

**Total for typical usage:** < 500 KB

---

## Related Files

- **Entity:** `/Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/entity/IntentExampleEntity.kt`
- **DAO:** `/Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/dao/IntentExampleDao.kt`
- **Database:** `/Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/AVADatabase.kt`
- **Migration:** `/Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/migration/DatabaseMigrations.kt`
- **Migrator:** `/Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentExamplesMigration.kt`
- **Classifier:** `/Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentClassifier.kt`
- **JSON Source:** `/apps/ava-standalone/src/main/assets/intent_examples.json`

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1 | 2025-11-02 | Initial schema with 6 core tables |
| 2 | 2025-11-10 | Added intent_examples table for NLU |

---

## Author

Manoj Jhawar
Augmentalis Inc, Intelligent Devices LLC
manoj@ideahq.net
