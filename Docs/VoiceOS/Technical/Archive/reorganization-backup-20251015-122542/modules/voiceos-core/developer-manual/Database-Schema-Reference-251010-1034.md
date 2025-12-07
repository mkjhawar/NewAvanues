# Database Schema Reference - Scraping Subsystem

**Module:** VoiceAccessibility
**Database:** AppScrapingDatabase
**Current Version:** 3
**Last Updated:** 2025-10-10 10:34:00 PDT

## Database Overview

The scraping database stores UI element data, hierarchical relationships, and generated voice commands using Room persistence library.

**Database Name:** `app_scraping_database`
**Storage Location:** `/data/data/com.augmentalis.voiceaccessibility/databases/`
**Retention Policy:** 7 days (auto-cleanup)

## Tables

### 1. scraped_apps

Stores metadata about scraped applications.

```sql
CREATE TABLE scraped_apps (
    app_id TEXT PRIMARY KEY,              -- UUID generated per scraping session
    package_name TEXT NOT NULL,           -- Android package name
    app_name TEXT NOT NULL,               -- Human-readable app name
    version_code INTEGER NOT NULL,        -- Android version code
    version_name TEXT NOT NULL,           -- Android version name (e.g., "1.2.3")
    app_hash TEXT NOT NULL,               -- MD5(packageName + versionCode)
    first_scraped INTEGER NOT NULL,       -- Timestamp (milliseconds)
    last_scraped INTEGER NOT NULL,        -- Timestamp (milliseconds)
    scrape_count INTEGER NOT NULL DEFAULT 1,
    element_count INTEGER NOT NULL DEFAULT 0,
    command_count INTEGER NOT NULL DEFAULT 0,
    is_fully_learned INTEGER NOT NULL DEFAULT 0,  -- Boolean (0/1)
    learn_completed_at INTEGER,           -- Nullable timestamp
    scraping_mode TEXT NOT NULL DEFAULT 'DYNAMIC'  -- DYNAMIC or LEARN_APP
);
```

**Indexes:**
- PRIMARY KEY on `app_id`
- Index on `app_hash` (for version detection)
- Index on `package_name`

**Key Columns:**
- `app_hash`: Unique fingerprint per app version. Changes when app updates.
- `scrape_count`: Incremented each time app is re-scraped.
- `is_fully_learned`: `1` if LearnApp mode has completed, `0` otherwise.
- `scraping_mode`: Current mode (typically DYNAMIC except during LearnApp).

---

### 2. scraped_elements

Stores individual UI elements discovered through accessibility tree.

```sql
CREATE TABLE scraped_elements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    element_hash TEXT NOT NULL UNIQUE,    -- SHA-256 from AccessibilityFingerprint
    app_id TEXT NOT NULL,                 -- FK to scraped_apps.app_id

    -- Accessibility Properties
    class_name TEXT NOT NULL,
    view_id_resource_name TEXT,
    text TEXT,
    content_description TEXT,
    bounds TEXT NOT NULL,                 -- JSON: {"left":0,"top":0,"right":100,"bottom":50}

    -- Action Capabilities
    is_clickable INTEGER NOT NULL,        -- Boolean (0/1)
    is_long_clickable INTEGER NOT NULL,
    is_editable INTEGER NOT NULL,
    is_scrollable INTEGER NOT NULL,
    is_checkable INTEGER NOT NULL,
    is_focusable INTEGER NOT NULL,
    is_enabled INTEGER NOT NULL,

    -- Hierarchy Position
    depth INTEGER NOT NULL,               -- Depth in accessibility tree
    index_in_parent INTEGER NOT NULL,     -- Index among siblings

    -- Metadata
    scraped_at INTEGER NOT NULL,          -- Timestamp (milliseconds)

    FOREIGN KEY(app_id) REFERENCES scraped_apps(app_id) ON DELETE CASCADE
);
```

**Indexes:**
- PRIMARY KEY on `id`
- UNIQUE index on `element_hash`
- Index on `app_id`
- Index on `view_id_resource_name`

**Key Columns:**
- `element_hash`: Unique identifier for element (SHA-256 hash)
- `bounds`: JSON string with screen coordinates
- Action capabilities: Used by CommandGenerator to determine command types

**Foreign Keys:**
- `app_id` → `scraped_apps.app_id` (CASCADE on delete)

---

### 3. scraped_hierarchy

Stores parent-child relationships between UI elements.

```sql
CREATE TABLE scraped_hierarchy (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_element_id INTEGER NOT NULL,   -- FK to scraped_elements.id
    child_element_id INTEGER NOT NULL,    -- FK to scraped_elements.id
    child_order INTEGER NOT NULL,         -- Order among siblings
    depth INTEGER NOT NULL DEFAULT 1,     -- Depth difference (usually 1)

    FOREIGN KEY(parent_element_id) REFERENCES scraped_elements(id) ON DELETE CASCADE,
    FOREIGN KEY(child_element_id) REFERENCES scraped_elements(id) ON DELETE CASCADE
);
```

**Indexes:**
- PRIMARY KEY on `id`
- Index on `parent_element_id`
- Index on `child_element_id`

**Design Note:**
- Uses `Long` IDs (not hashes) for performance
- Enables fast tree traversal queries
- `child_order` preserves visual layout order

**Foreign Keys:**
- `parent_element_id` → `scraped_elements.id` (CASCADE)
- `child_element_id` → `scraped_elements.id` (CASCADE)

**Why Not Hash-Based FKs?**
See: `/coding/DECISIONS/ScrapedHierarchy-Migration-Analysis-251010-0220.md`
- Long IDs provide better query performance
- Hierarchy is transient (recreated on each scrape)
- Hash-based lookups not needed for tree traversal

---

### 4. generated_commands

Stores voice commands generated from UI elements.

```sql
CREATE TABLE generated_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    element_hash TEXT NOT NULL,           -- FK to scraped_elements.element_hash
    command_text TEXT NOT NULL,           -- Primary command (e.g., "click submit")
    action_type TEXT NOT NULL,            -- click, long_click, type, scroll, focus
    confidence REAL NOT NULL,             -- 0.0 to 1.0
    synonyms TEXT NOT NULL,               -- JSON array of alternatives

    -- User Feedback
    is_user_approved INTEGER NOT NULL DEFAULT 0,
    usage_count INTEGER NOT NULL DEFAULT 0,
    last_used INTEGER,                    -- Nullable timestamp

    -- Metadata
    generated_at INTEGER NOT NULL,        -- Timestamp (milliseconds)

    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE
);
```

**Indexes:**
- PRIMARY KEY on `id`
- Index on `element_hash`
- Index on `command_text` (for fuzzy matching)
- Index on `action_type`

**Key Columns:**
- `element_hash`: Links to element (uses hash instead of ID for stability)
- `command_text`: Primary phrase user can speak
- `synonyms`: JSON array of alternative phrases
- `confidence`: Quality score (0.0 = low, 1.0 = high)
- `usage_count`: Incremented each time command is executed

**Foreign Keys:**
- `element_hash` → `scraped_elements.element_hash` (CASCADE)

**Migration Note:**
- v1→v2: Migrated from `element_id` (Long FK) to `element_hash` (String FK)
- Reason: Hash-based references survive element re-insertion

---

## Relationships

```
scraped_apps (1) ──→ (N) scraped_elements
                              ↓
                          (1) → (N) scraped_hierarchy (self-referential)
                              ↓
                          (1) → (N) generated_commands
```

**Cascade Deletions:**
- Delete app → deletes all elements → deletes all hierarchy + commands
- Delete element → deletes hierarchy relationships + commands for that element

## Migrations

### Migration 1 → 2

**Changes:**
1. Add UNIQUE constraint to `scraped_elements.element_hash`
2. Migrate `generated_commands` from `element_id` (Long FK) to `element_hash` (String FK)

**Strategy:**
1. Create unique index on `element_hash`
2. Create new `generated_commands` table with `element_hash` FK
3. Migrate data using JOIN to map `element_id` → `element_hash`
4. Drop old table, rename new table
5. Recreate indexes

**SQL:**
```sql
-- Step 1: Unique constraint
CREATE UNIQUE INDEX index_scraped_elements_element_hash
ON scraped_elements(element_hash);

-- Step 2: New table
CREATE TABLE generated_commands_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    element_hash TEXT NOT NULL,
    -- ... other columns
    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE
);

-- Step 3: Migrate data
INSERT INTO generated_commands_new
SELECT gc.id, se.element_hash, gc.command_text, -- ...
FROM generated_commands gc
INNER JOIN scraped_elements se ON gc.element_id = se.id;

-- Step 4: Replace table
DROP TABLE generated_commands;
ALTER TABLE generated_commands_new RENAME TO generated_commands;

-- Step 5: Indexes
CREATE INDEX index_generated_commands_element_hash ON generated_commands(element_hash);
CREATE INDEX index_generated_commands_command_text ON generated_commands(command_text);
```

---

### Migration 2 → 3

**Changes:**
1. Add `is_fully_learned` column to `scraped_apps` (default: 0)
2. Add `learn_completed_at` column to `scraped_apps` (nullable)
3. Add `scraping_mode` column to `scraped_apps` (default: 'DYNAMIC')

**SQL:**
```sql
ALTER TABLE scraped_apps ADD COLUMN is_fully_learned INTEGER NOT NULL DEFAULT 0;
ALTER TABLE scraped_apps ADD COLUMN learn_completed_at INTEGER;
ALTER TABLE scraped_apps ADD COLUMN scraping_mode TEXT NOT NULL DEFAULT 'DYNAMIC';
```

**No Data Migration:** All existing apps default to not fully learned.

---

## Query Patterns

### 1. Check if App is Scraped

```kotlin
val appHash = AppHashCalculator.calculateAppHash(packageName, versionCode)
val app = database.scrapedAppDao().getAppByHash(appHash)
if (app != null) {
    // App already scraped
    database.scrapedAppDao().incrementScrapeCount(app.appId)
}
```

---

### 2. Get Element by Hash (O(1) Lookup)

```kotlin
val element = database.scrapedElementDao().getElementByHash(hash)
```

**Index Used:** Unique index on `element_hash`
**Performance:** O(1) - direct hash lookup

---

### 3. Find Commands for App

```kotlin
val commands = database.generatedCommandDao().getCommandsForApp(appId)
```

**SQL Generated:**
```sql
SELECT gc.* FROM generated_commands gc
JOIN scraped_elements se ON gc.element_hash = se.element_hash
WHERE se.app_id = :appId
ORDER BY gc.confidence DESC
```

---

### 4. Tree Traversal - Get Children

```kotlin
val children = database.scrapedHierarchyDao().getChildren(parentId)
```

**SQL:**
```sql
SELECT * FROM scraped_hierarchy
WHERE parent_element_id = :parentId
ORDER BY child_order
```

**Index Used:** Index on `parent_element_id`

---

### 5. UPSERT Element (LearnApp Mode)

```kotlin
val existing = database.scrapedElementDao().getElementByHash(element.elementHash)
if (existing != null) {
    val updated = element.copy(id = existing.id)  // Preserve DB ID
    database.scrapedElementDao().update(updated)
} else {
    database.scrapedElementDao().insert(element)
}
```

**Purpose:** Merge LearnApp data with existing Dynamic data
**Key:** Uses hash to find duplicates, preserves database ID

---

### 6. Cleanup Old Data

```kotlin
val retentionTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
database.scrapedAppDao().deleteAppsOlderThan(retentionTimestamp)
```

**SQL:**
```sql
DELETE FROM scraped_apps WHERE last_scraped < :timestamp
```

**Cascade:** Automatically deletes elements, hierarchy, and commands

---

## Performance Optimization

### Indexes

**Critical Indexes:**
- `element_hash` (UNIQUE) - O(1) element lookup
- `app_hash` - Fast app version detection
- `parent_element_id` - Fast hierarchy traversal
- `command_text` - Fuzzy command matching

**Query Performance:**
- Element lookup by hash: O(1)
- Commands for app: O(n) where n = command count
- Hierarchy traversal: O(depth) for tree walk

### Batch Operations

**Always Use Batch Inserts:**
```kotlin
// Good
database.scrapedElementDao().insertBatch(elements)

// Bad (N queries)
elements.forEach { database.scrapedElementDao().insert(it) }
```

**Performance:**
- Batch insert: ~50-100ms for 100 elements
- Individual inserts: ~500-1000ms for 100 elements

### Memory Considerations

**Large UI Trees:**
- Typical app: 50-200 elements
- Complex app: 500-1000 elements
- Pathological app: 2000+ elements (rare)

**Mitigation:**
- MAX_DEPTH limit (50 levels)
- Batch operations
- Proper node recycling

---

## Data Integrity

### Foreign Key Constraints

**Enforced:**
- `scraped_elements.app_id` → `scraped_apps.app_id`
- `scraped_hierarchy.parent_element_id` → `scraped_elements.id`
- `scraped_hierarchy.child_element_id` → `scraped_elements.id`
- `generated_commands.element_hash` → `scraped_elements.element_hash`

**ON DELETE CASCADE:** All FKs cascade deletes

### Unique Constraints

- `scraped_apps.app_id` (PRIMARY KEY)
- `scraped_elements.id` (PRIMARY KEY)
- `scraped_elements.element_hash` (UNIQUE)
- `scraped_hierarchy.id` (PRIMARY KEY)
- `generated_commands.id` (PRIMARY KEY)

### Data Validation

**Room Annotations:**
- `@PrimaryKey(autoGenerate = true)` - Auto-increment IDs
- `@ColumnInfo(name = "...")` - Explicit column names
- `@ForeignKey(onDelete = CASCADE)` - Cascade deletions
- `@Index(unique = true)` - Unique constraints

---

## Troubleshooting

### Issue: Foreign Key Violation

**Symptom:** `SQLiteConstraintException: FOREIGN KEY constraint failed`

**Causes:**
1. Inserting hierarchy before elements
2. Incorrect element ID mapping
3. Orphaned commands (element deleted)

**Solution:**
- Follow 3-phase insertion (app → elements → hierarchy → commands)
- Verify ID capture: `insertBatchWithIds()` returns IDs in order
- Use hash-based FKs for commands (not affected by element deletion/reinsertion)

---

### Issue: Hash Collision

**Symptom:** `SQLiteConstraintException: UNIQUE constraint failed: scraped_elements.element_hash`

**Cause:** Two elements with identical hash (very rare with AccessibilityFingerprint)

**Solution:**
1. Verify using latest hashing (AccessibilityFingerprint with hierarchy path)
2. Log collision details for analysis
3. Fallback: Skip duplicate element

---

### Issue: Performance Degradation

**Symptom:** Slow queries, high CPU usage

**Causes:**
1. Missing indexes
2. Large dataset without cleanup
3. Inefficient queries (N+1 problem)

**Solution:**
1. Verify indexes exist: `.indices` in entity annotations
2. Enable auto-cleanup (7-day retention)
3. Use batch operations and JOINs

---

**Schema Version:** 3
**Last Updated:** 2025-10-10 10:34:00 PDT
**Maintained By:** VOS4 Development Team
