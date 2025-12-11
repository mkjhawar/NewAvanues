# ADR-002: Dual Database Strategy (Room + SQLDelight)

**Status**: Accepted
**Date**: 2025-10-29
**Authors**: Manoj Jhawar
**Deciders**: Architecture Team
**Related**: ADR-001 (KMP Strategy)

---

## Context

AVA AI needs a database solution that:
1. Supports Android (current platform)
2. Supports iOS (planned Phase 2)
3. Maintains existing Room investments (6 repositories, 32 tests, 95% coverage)
4. Enables cross-platform data synchronization
5. Doesn't require rewriting all database code immediately

---

## Decision

**We will use a dual database strategy during transition:**

### Phase 1: Dual Backend (Current)
- **Android**: Room 2.6.1 + KSP (primary, production-ready)
- **iOS**: SQLDelight 2.0.1 native driver (planned, migration path)
- **Shared**: Repository interfaces in `core/domain/commonMain`

### Phase 2: SQLDelight Everywhere (Future)
- **Android**: Migrate to SQLDelight Android driver (replace Room)
- **iOS**: SQLDelight native driver
- **Shared**: SQL schema files (`.sq`) + generated type-safe code

### Configuration
```kotlin
// core/data/build.gradle.kts
plugins {
    id("app.cash.sqldelight") version "2.0.1"  // Already configured!
}

sqldelight {
    databases {
        create("AvaDatabase") {
            packageName.set("com.augmentalis.ava.core.data.sqldelight")
            srcDirs("src/commonMain/sqldelight")
        }
    }
}
```

---

## Rationale

### Why Dual Database (Not Immediate Migration)?

**Risk Mitigation**:
- Room implementation is working (95% test coverage, production-tested)
- Immediate migration = high risk of bugs, data loss
- Gradual migration allows validation on iOS first

**Cost-Benefit**:
- Room → SQLDelight migration = 5 days work (rewrite DAOs, schemas, tests)
- Dual backend = 0 days immediate work (SQLDelight already configured)
- Buys time to validate SQLDelight on iOS before Android migration

**Existing Investment**:
```kotlin
// 6 Room repositories already implemented:
- ConversationRepositoryImpl (tested)
- MessageRepositoryImpl (tested)
- TrainExampleRepositoryImpl (tested, MD5 dedup)
- DecisionRepositoryImpl (tested)
- LearningRepositoryImpl (tested)
- MemoryRepositoryImpl (tested)
```

### Why SQLDelight (Not Room Everywhere)?

**Cross-Platform**: Room is Android-only. SQLDelight works on Android, iOS, JVM, JS, Native.

**Type Safety**: Both are type-safe, but SQLDelight generates code from SQL (single source of truth).

**Performance**: SQLDelight ~10% faster than Room (direct SQL, no reflection).

**VoiceAvenue Alignment**: Parent app uses SQLDelight for all cross-platform data.

---

## Architecture

### Repository Pattern (Maintained)

```kotlin
// core/domain/commonMain (interfaces)
interface ConversationRepository {
    suspend fun insert(conversation: Conversation): Result<Long>
    fun observeAll(): Flow<Result<List<Conversation>>>
}

// core/data/androidMain (Room implementation)
class ConversationRepositoryImpl(
    private val dao: ConversationDao  // Room
) : ConversationRepository {
    override suspend fun insert(conversation: Conversation) = /* ... */
}

// core/data/iosMain (SQLDelight implementation - future)
class ConversationRepositoryImpl(
    private val database: AvaDatabase  // SQLDelight
) : ConversationRepository {
    override suspend fun insert(conversation: Conversation) = /* ... */
}
```

### Database Initialization (expect/actual)

```kotlin
// commonMain
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// androidMain
actual class DatabaseDriverFactory(context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(AvaDatabase.Schema, context, "ava.db")
    }
}

// iosMain
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(AvaDatabase.Schema, "ava.db")
    }
}
```

---

## Consequences

### Positive

✅ **Zero risk migration** → Android app keeps working with Room during iOS development

✅ **Gradual transition** → Validate SQLDelight on iOS first, then migrate Android

✅ **Type-safe SQL** → SQLDelight generates Kotlin code from SQL schemas

✅ **VoiceAvenue ready** → Using same database tech as parent app

✅ **Performance gain** → SQLDelight ~10% faster than Room

✅ **Multi-platform queries** → Write SQL once, works on all platforms

### Negative

⚠️ **Duplicate code (temporary)** → Two database implementations during transition (Room + SQLDelight)

⚠️ **Migration effort** → 5 days to convert Room → SQLDelight on Android

⚠️ **Learning curve** → Team needs to learn SQLDelight syntax (`.sq` files)

⚠️ **Schema duplication** → Room entities + SQLDelight schemas (until Android migration)

### Neutral

🔄 **Testing effort** → iOS SQLDelight repos need full test suite (reuse Room test logic)

🔄 **Data migration** → Need migration script if schema changes during transition

---

## Implementation Plan

### Current Status (Week 5)
- ✅ SQLDelight configured in `core/data/build.gradle.kts`
- ✅ Room repositories implemented (6 total, 32 tests)
- ✅ Repository interfaces in `core/domain/commonMain`

### Phase 1: iOS SQLDelight Implementation (Week 8-9)
1. Create SQL schema files (`src/commonMain/sqldelight/`)
   ```sql
   -- Conversation.sq
   CREATE TABLE Conversation (
       id INTEGER PRIMARY KEY AUTOINCREMENT,
       title TEXT NOT NULL,
       createdAt INTEGER NOT NULL,
       updatedAt INTEGER NOT NULL
   );

   selectAll:
   SELECT * FROM Conversation ORDER BY updatedAt DESC;

   insert:
   INSERT INTO Conversation(title, createdAt, updatedAt)
   VALUES (?, ?, ?);
   ```

2. Implement iOS repository implementations
   ```kotlin
   // core/data/iosMain
   class ConversationRepositoryImpl(
       private val database: AvaDatabase
   ) : ConversationRepository {
       override suspend fun insert(conversation: Conversation) =
           database.conversationQueries.insert(/* ... */)
   }
   ```

3. Write iOS-specific tests (reuse Room test cases)

4. Validate iOS app with SQLDelight backend

### Phase 2: Android Migration to SQLDelight (Week 10-11)
1. Keep Room implementation as backup
2. Add SQLDelight Android driver dependency
3. Implement SQLDelight repos for Android (reuse iOS code)
4. Run both implementations in parallel (A/B test)
5. Validate data consistency (Room vs SQLDelight)
6. Switch production to SQLDelight
7. Remove Room dependencies

### Phase 3: Cleanup (Week 12)
1. Delete Room DAOs, entities, type converters
2. Remove Room dependencies from `build.gradle.kts`
3. Update documentation
4. Archive Room implementation code

---

## Alternatives Considered

### 1. Room Multiplatform (Experimental)
**Rejected**: Room KMP support is alpha quality (not production-ready). Limited iOS support. Google's commitment uncertain.

### 2. Immediate SQLDelight Migration
**Rejected**: High risk during active development. Would delay iOS work by 1 week. Room implementation already validated.

### 3. Realm Database
**Rejected**: Proprietary (MongoDB). Licensing concerns. Overkill for AVA's simple schemas. VoiceAvenue doesn't use Realm.

### 4. CoreData (iOS) + Room (Android)
**Rejected**: Zero code sharing. Duplicates all database logic. Bug fixes needed twice. Schema changes coordinated across teams.

---

## Migration Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Data loss during migration | Low | High | Keep Room as backup, parallel validation, export/import scripts |
| Schema mismatch (Room vs SQLDelight) | Medium | Medium | Generate schemas from same source, validation tests |
| Performance regression | Low | Medium | Benchmark both implementations, A/B test in production |
| iOS SQLDelight bugs | Medium | High | Extensive testing on iOS before Android migration |
| Team unfamiliar with SQLDelight | High | Low | Training session, code review, pair programming |

---

## Success Metrics

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Test coverage (database) | 95% | 95% (Room) | ✅ Maintain |
| iOS SQLDelight tests | 100% of Room tests | 0% | ⏳ Week 8 |
| Android SQLDelight migration | Complete | 0% | ⏳ Week 10 |
| Data consistency (dual backend) | 100% match | N/A | ⏳ Week 10 |
| Performance (SQLDelight vs Room) | ≥100% | N/A | ⏳ Week 10 |

---

## Database Schema Overview

**Current Tables** (Room):
1. `conversations` - Chat sessions
2. `messages` - User/assistant messages
3. `train_examples` - Teach-Ava training data (MD5 dedup)
4. `decisions` - User decision tracking
5. `learnings` - Personalization learnings
6. `memories` - Long-term memory storage

**VOS4 Patterns Applied**:
- Composite indices (multi-column queries)
- Hash deduplication (MD5 on train_examples)
- Cascade deletes (orphan cleanup)
- Usage tracking (createdAt, updatedAt timestamps)

---

## References

- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [Room vs SQLDelight Comparison](https://blog.jetbrains.com/kotlin/2021/03/sqldelight-1-5-0/)
- [SQLDelight iOS Setup](https://cashapp.github.io/sqldelight/2.0.1/native_sqlite_driver/)
- [Cash App SQLDelight Case Study](https://developer.squareup.com/blog/sqldelight-at-cash-app/)
- VoiceAvenue Database Architecture (dual backend reference)

---

## Changelog

**v1.0 (2025-10-29)**: Initial decision - Dual database strategy during transition, SQLDelight target

---

**Created by Manoj Jhawar, manoj@ideahq.net**
