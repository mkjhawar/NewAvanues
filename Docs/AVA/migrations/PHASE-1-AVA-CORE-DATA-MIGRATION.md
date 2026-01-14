# Phase 1: AVA Core/Data Room → SQLDelight Migration

**Repository:** AVA (main)
**Module:** `Universal/AVA/Core/Data/`
**Priority:** HIGH
**Estimated Duration:** 3-5 days

---

## Current State

### Room Implementation Files

```
Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/
├── AVADatabase.kt                    # @Database class
├── DatabaseProvider.kt               # Singleton provider
├── converter/
│   └── TypeConverters.kt             # Map, FloatList, StringList converters
├── dao/
│   ├── ConversationDao.kt
│   ├── MessageDao.kt
│   ├── IntentExampleDao.kt
│   ├── IntentEmbeddingDao.kt
│   ├── SemanticIntentOntologyDao.kt
│   ├── TrainExampleDao.kt
│   └── DecisionDao.kt
├── entity/
│   ├── ConversationEntity.kt
│   ├── MessageEntity.kt
│   ├── IntentExampleEntity.kt
│   ├── IntentEmbeddingEntity.kt
│   ├── SemanticIntentOntologyEntity.kt
│   ├── TrainExampleEntity.kt
│   ├── DecisionEntity.kt
│   └── DecisionActionEntity.kt
└── migration/
    └── DatabaseMigrations.kt         # Room migrations 1→6
```

### Current Dependencies (build.gradle.kts)

```kotlin
// Room - use api() to expose to dependent modules
api(libs.room.runtime)
api(libs.room.ktx)
ksp(libs.room.compiler)
```

---

## Migration Tasks

### Task 1.1: Add SQLDelight Plugin and Dependencies

**File:** `Universal/AVA/Core/Data/build.gradle.kts`

**Changes:**
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)  // Change from android.library
    alias(libs.plugins.android.library)
    id("app.cash.sqldelight") version "2.0.1"
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "CoreData"
            isStatic = true
        }
    }

    // Desktop target
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":Universal:AVA:Core:Domain"))
                implementation(project(":Universal:AVA:Core:Common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                // SQLDelight
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
            }
        }
        androidMain {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.0.1")
                implementation("androidx.datastore:datastore-preferences:1.0.0")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation("app.cash.sqldelight:native-driver:2.0.1")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }
    }
}

sqldelight {
    databases {
        create("AVADatabase") {
            packageName.set("com.augmentalis.ava.core.data.db")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            deriveSchemaFromMigrations.set(true)
            verifyMigrations.set(true)
        }
    }
}
```

**Commit:**
```bash
git add Universal/AVA/Core/Data/build.gradle.kts
git commit -m "feat(db): add SQLDelight plugin and KMP configuration (Phase 1.1)

- Add SQLDelight 2.0.1 plugin
- Configure multiplatform targets (Android, iOS, Desktop)
- Add platform-specific SQLDelight drivers
- Keep Room temporarily for migration

Part of: Room→SQLDelight Cross-Platform Migration Phase 1"
```

---

### Task 1.2: Create SQLDelight Schema Files

**Directory Structure:**
```
Universal/AVA/Core/Data/src/commonMain/sqldelight/
└── com/augmentalis/ava/core/data/db/
    ├── Conversation.sq
    ├── Message.sq
    ├── IntentExample.sq
    ├── IntentEmbedding.sq
    ├── SemanticIntentOntology.sq
    ├── TrainExample.sq
    └── Decision.sq
```

**Create each .sq file with:**
1. CREATE TABLE statement
2. CREATE INDEX statements
3. CRUD queries

**Example - Conversation.sq:**
```sql
-- Conversation.sq
CREATE TABLE Conversation (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    is_archived INTEGER NOT NULL DEFAULT 0,
    metadata TEXT
);

CREATE INDEX conversation_updated_idx ON Conversation(updated_at DESC);
CREATE INDEX conversation_archived_idx ON Conversation(is_archived);

-- Get all conversations ordered by last update
getAllConversations:
SELECT * FROM Conversation ORDER BY updated_at DESC;

-- Get non-archived conversations
getActiveConversations:
SELECT * FROM Conversation WHERE is_archived = 0 ORDER BY updated_at DESC;

-- Get by ID
getConversationById:
SELECT * FROM Conversation WHERE id = ?;

-- Insert or update
upsertConversation:
INSERT OR REPLACE INTO Conversation (id, title, created_at, updated_at, is_archived, metadata)
VALUES (?, ?, ?, ?, ?, ?);

-- Delete
deleteConversation:
DELETE FROM Conversation WHERE id = ?;

-- Archive
archiveConversation:
UPDATE Conversation SET is_archived = 1, updated_at = ? WHERE id = ?;

-- Update title
updateTitle:
UPDATE Conversation SET title = ?, updated_at = ? WHERE id = ?;

-- Count
countConversations:
SELECT COUNT(*) FROM Conversation WHERE is_archived = 0;
```

**Commit:**
```bash
git add Universal/AVA/Core/Data/src/commonMain/sqldelight/
git commit -m "feat(db): add SQLDelight schema files for all entities (Phase 1.2)

Tables created:
- Conversation (id, title, created_at, updated_at, is_archived, metadata)
- Message (id, conversation_id, role, content, timestamp, metadata)
- IntentExample (example_hash, intent_id, example_text, source, locale, ...)
- IntentEmbedding (intent_id, embedding, model_version, timestamps)
- SemanticIntentOntology (intent_id, locale, synonyms, action_sequence, ...)
- TrainExample (id, input_text, output_text, feedback, ...)
- Decision (id, conversation_id, type, status, ...)

All schemas include:
- Primary keys
- Foreign key relationships
- Performance indexes
- CRUD queries

Part of: Room→SQLDelight Cross-Platform Migration Phase 1"
```

---

### Task 1.3: Create Platform Drivers

**File Structure:**
```
Universal/AVA/Core/Data/src/
├── commonMain/kotlin/com/augmentalis/ava/core/data/db/
│   └── DriverFactory.kt (expect)
├── androidMain/kotlin/com/augmentalis/ava/core/data/db/
│   └── DriverFactory.kt (actual Android)
├── iosMain/kotlin/com/augmentalis/ava/core/data/db/
│   └── DriverFactory.kt (actual iOS)
└── desktopMain/kotlin/com/augmentalis/ava/core/data/db/
    └── DriverFactory.kt (actual Desktop)
```

**Commit:**
```bash
git add Universal/AVA/Core/Data/src/*/kotlin/com/augmentalis/ava/core/data/db/
git commit -m "feat(db): add platform-specific SQLDelight drivers (Phase 1.3)

Drivers implemented:
- Android: AndroidSqliteDriver with Context
- iOS: NativeSqliteDriver
- Desktop: JdbcSqliteDriver with file path

All drivers:
- Create/open database file
- Handle schema migrations
- Implement expect/actual pattern

Part of: Room→SQLDelight Cross-Platform Migration Phase 1"
```

---

### Task 1.4: Create Repository Implementations

**Move to commonMain:**
- ConversationRepository → ConversationRepositoryImpl
- MessageRepository → MessageRepositoryImpl
- IntentExampleRepository → IntentExampleRepositoryImpl
- etc.

**Keep interfaces in Core/Domain (unchanged)**

**Commit:**
```bash
git add Universal/AVA/Core/Data/src/commonMain/kotlin/com/augmentalis/ava/core/data/repository/
git commit -m "feat(db): implement SQLDelight repositories (Phase 1.4)

Repositories migrated:
- ConversationRepositoryImpl
- MessageRepositoryImpl
- IntentExampleRepositoryImpl
- IntentEmbeddingRepositoryImpl
- SemanticIntentOntologyRepositoryImpl
- TrainExampleRepositoryImpl
- DecisionRepositoryImpl

All repositories:
- Use SQLDelight queries
- Return Flow for reactive updates
- Implement domain interfaces
- Handle null/empty cases

Part of: Room→SQLDelight Cross-Platform Migration Phase 1"
```

---

### Task 1.5: Add Data Migration Utility

**File:** `src/androidMain/kotlin/com/augmentalis/ava/core/data/migration/RoomToSQLDelightMigrator.kt`

Migrates existing Room data to SQLDelight database.

**Commit:**
```bash
git add Universal/AVA/Core/Data/src/androidMain/kotlin/com/augmentalis/ava/core/data/migration/
git commit -m "feat(db): add Room→SQLDelight data migrator (Phase 1.5)

Migration utility:
- Copies all data from Room to SQLDelight
- Handles type conversions
- Preserves timestamps and relationships
- Deletes Room database after success
- Supports rollback on failure

Part of: Room→SQLDelight Cross-Platform Migration Phase 1"
```

---

### Task 1.6: Update DI Module

**File:** `src/androidMain/kotlin/com/augmentalis/ava/core/data/di/DataModule.kt`

Update Hilt/Koin module to provide SQLDelight database instead of Room.

**Commit:**
```bash
git add Universal/AVA/Core/Data/src/*/kotlin/com/augmentalis/ava/core/data/di/
git commit -m "feat(db): update DI modules for SQLDelight (Phase 1.6)

DI changes:
- Replace Room database provider with SQLDelight
- Add DriverFactory binding
- Update repository bindings
- Add migration check on startup

Part of: Room→SQLDelight Cross-Platform Migration Phase 1"
```

---

### Task 1.7: Remove Room Dependencies

**After verification, remove:**
```kotlin
// Remove these from build.gradle.kts
// api(libs.room.runtime)
// api(libs.room.ktx)
// ksp(libs.room.compiler)
```

**Delete Room files:**
```bash
rm -rf Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/dao/
rm -rf Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/entity/
rm Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/AVADatabase.kt
rm -rf Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/migration/DatabaseMigrations.kt
```

**Commit:**
```bash
git add -A
git commit -m "refactor(db): remove Room implementation (Phase 1.7)

BREAKING CHANGE: Room database replaced with SQLDelight

Removed:
- Room entities (7 files)
- Room DAOs (7 files)
- AVADatabase.kt
- DatabaseMigrations.kt
- Room dependencies

Migration:
- Existing Room data automatically migrated on first app launch
- Old database file deleted after successful migration

Part of: Room→SQLDelight Cross-Platform Migration Phase 1"
```

---

### Task 1.8: Add Unit Tests

**Directory:** `src/commonTest/kotlin/com/augmentalis/ava/core/data/`

Test all repositories with in-memory SQLite driver.

**Commit:**
```bash
git add Universal/AVA/Core/Data/src/commonTest/
git commit -m "test(db): add SQLDelight repository unit tests (Phase 1.8)

Tests added:
- ConversationRepositoryTest (CRUD, archive, count)
- MessageRepositoryTest (CRUD, foreign keys)
- IntentExampleRepositoryTest (CRUD, usage count)
- IntentEmbeddingRepositoryTest (CRUD, BLOB handling)
- SemanticIntentOntologyRepositoryTest (CRUD, locale)
- TrainExampleRepositoryTest (CRUD, feedback)
- DecisionRepositoryTest (CRUD, status)

All tests use in-memory SQLite driver.

Part of: Room→SQLDelight Cross-Platform Migration Phase 1"
```

---

## Verification Checklist

- [ ] Build succeeds for Android target
- [ ] Build succeeds for iOS target
- [ ] Build succeeds for Desktop target
- [ ] All unit tests pass
- [ ] Data migration preserves existing data
- [ ] No Room dependencies remain
- [ ] DI bindings work correctly
- [ ] App runs and persists data correctly

---

## Rollback Instructions

If migration fails:

1. Revert to previous commit:
   ```bash
   git revert HEAD~N..HEAD
   ```

2. Keep Room database backup file
3. Re-enable Room dependencies
4. Report issue with detailed logs

---

**Next Phase:** [Phase 2: AVA Features/RAG Migration](./PHASE-2-AVA-RAG-MIGRATION.md)
