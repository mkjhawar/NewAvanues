# Database Migration Strategy

**Spec ID:** 0001-room-database
**Version:** 1.0
**Status:** Implemented

## Overview

This document defines the migration strategy for AVA's Room database, ensuring data preservation across schema changes.

## Migration Philosophy

1. **Additive First**: Prefer adding columns/tables over modifying existing schema
2. **Backward Compatible**: New code should work with old database versions during migration
3. **Tested Migrations**: Every migration must have tests validating data integrity
4. **Documented Changes**: Every migration documents why the change was necessary

## Version History

### Version 1 (Current)
**Schema:**
- `conversations` - Chat sessions
- `messages` - Individual messages with FK to conversations
- `train_examples` - Teach-Ava training data with hash-based uniqueness
- `decisions` - Decision logging for transparency
- `learning` - User feedback tracking
- `memory` - Long-term memory with importance tracking

**VOS4 Patterns Applied:**
- Composite indices: `(conversation_id, timestamp)`
- Hash-based uniqueness: `example_hash` with unique constraint
- Cascade deletes: `messages.conversation_id` → `conversations.id`
- Usage tracking: `usage_count`, `last_used`, `access_count`
- Importance-based indexing: `memory.importance`

## Migration Planning (Future)

### Potential Version 2 Changes
**Candidates for next version:**

1. **Attachments Support**
   ```sql
   CREATE TABLE attachments (
       id TEXT PRIMARY KEY NOT NULL,
       message_id TEXT NOT NULL,
       file_path TEXT NOT NULL,
       file_type TEXT NOT NULL,
       file_size INTEGER NOT NULL,
       created_at INTEGER NOT NULL,
       FOREIGN KEY(message_id) REFERENCES messages(id) ON DELETE CASCADE
   )
   CREATE INDEX index_attachments_message_id ON attachments(message_id)
   ```

2. **Conversation Tags**
   ```sql
   ALTER TABLE conversations ADD COLUMN tags TEXT
   -- JSON array of tags: ["work", "personal", "urgent"]
   ```

3. **Message Edit History**
   ```sql
   ALTER TABLE messages ADD COLUMN edited_at INTEGER
   ALTER TABLE messages ADD COLUMN edit_count INTEGER DEFAULT 0
   ```

4. **User Profiles (Multi-user support)**
   ```sql
   CREATE TABLE user_profiles (
       id TEXT PRIMARY KEY NOT NULL,
       name TEXT NOT NULL,
       avatar_path TEXT,
       created_at INTEGER NOT NULL,
       last_active INTEGER NOT NULL
   )
   ALTER TABLE conversations ADD COLUMN user_id TEXT
   ```

## Migration Testing Strategy

### Test Requirements
Every migration must have:

1. **Forward Migration Test**
   - Start with version N database with sample data
   - Run migration to version N+1
   - Verify all data preserved
   - Verify new schema applied correctly

2. **Data Integrity Test**
   - Verify foreign keys still valid
   - Verify indices still work
   - Verify queries still return correct results

3. **Performance Test**
   - Migration completes in < 5 seconds for 10,000 records
   - No table locks exceeding 1 second

### Test Template
```kotlin
@Test
fun migration_1_to_2_preserves_conversations() = runTest {
    // Given - Database at version 1 with test data
    val db = createTestDatabase(version = 1)
    db.execSQL("INSERT INTO conversations VALUES (...)")
    db.close()

    // When - Migrate to version 2
    val migratedDb = Room.databaseBuilder(...)
        .addMigrations(MIGRATION_1_2)
        .build()

    // Then - Verify data preserved
    val conversations = migratedDb.conversationDao().getAllConversations().first()
    assertEquals(1, conversations.size)

    // Then - Verify new schema
    val cursor = migratedDb.query("PRAGMA table_info(conversations)")
    // Verify new column exists
}
```

## Fallback Strategy

### Development Mode
- **Destructive Migration Enabled**: Database wiped and recreated
- **Use Case**: Rapid prototyping, schema experiments
- **Flag**: `enableDestructiveMigration = true`

### Production Mode
- **Destructive Migration Disabled**: Migration required or app fails
- **Use Case**: Preserve user data
- **Flag**: `enableDestructiveMigration = false` (default)

### Emergency Recovery
If migration fails in production:

1. **Backup First**: Export data to JSON before migration attempt
2. **Retry Logic**: Attempt migration up to 3 times with exponential backoff
3. **User Consent**: If all fail, ask user permission for destructive migration
4. **Data Export**: Offer to export their data before wipe

## Migration Checklist

Before deploying a new database version:

- [ ] Migration written and tested
- [ ] Forward migration test passes
- [ ] Data integrity test passes
- [ ] Performance test passes (< 5s for 10K records)
- [ ] Migration documented in this file
- [ ] Schema export updated (`app/schemas/`)
- [ ] Backup/restore mechanism tested
- [ ] Rollback plan documented

## Schema Export Location

```
app/
  schemas/
    com.augmentalis.ava.core.data.AVADatabase/
      1.json  ← Version 1 schema (current)
      2.json  ← Version 2 schema (future)
```

Room automatically exports schema JSON when:
```kotlin
@Database(
    entities = [...],
    version = 1,
    exportSchema = true  // ← Generates JSON
)
```

## Resources

- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Testing Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions#test)
- VOS4 Migration Patterns: `vos4/modules/database/migrations/`
