# ADR-001: SQLDelight for Database Layer

**Status:** Accepted
**Date:** 2025-12-12
**Deciders:** WebAvanue Architecture Team

## Context

WebAvanue requires a cross-platform database solution for persisting browser data (tabs, history, favorites, downloads, settings) across Android, iOS, and Desktop platforms. The database must:

1. Support Kotlin Multiplatform (KMP) with native drivers for each platform
2. Provide type-safe SQL queries to prevent runtime errors
3. Enable reactive data observation with Kotlin Flow integration
4. Support database migrations for schema evolution
5. Offer good performance for frequent read/write operations
6. Integrate seamlessly with Compose UI for reactive updates

## Decision

We will use **SQLDelight** as our database layer for all platforms.

SQLDelight generates Kotlin code from SQL statements, providing:
- **Type Safety**: Compile-time verification of SQL queries
- **KMP Support**: Native drivers for Android, iOS, JVM, and native platforms
- **Flow Integration**: Built-in coroutines-extensions for reactive queries
- **Migration Support**: Schema versioning and validation
- **Performance**: Direct SQL execution without ORM overhead

## Rationale

### Why SQLDelight Over Alternatives

1. **Type Safety**: Unlike Room (Android-only) or raw SQL, SQLDelight catches SQL errors at compile time
2. **True KMP**: Unlike Realm (limited KMP support), SQLDelight is KMP-first
3. **SQL-First**: We write real SQL, not DSLs (unlike Exposed), giving us full control
4. **Performance**: No reflection or runtime ORM overhead
5. **Simplicity**: Minimal boilerplate compared to Room or Exposed

### Technical Benefits

- **Shared Business Logic**: 95% of database code in `commonMain`
- **Platform Drivers**: Only driver initialization differs per platform
- **Reactive Flows**: `observeQuery()` returns `Flow<T>` for UI updates
- **ACID Transactions**: Built-in transaction support for data integrity
- **Schema Validation**: Gradle plugin validates migrations during build

### Development Benefits

- **Autocomplete**: IDE provides SQL autocomplete from schema
- **Refactoring**: Renaming columns updates all queries automatically
- **Testing**: Easy to create in-memory test databases
- **Debugging**: Raw SQL is visible and debuggable

## Consequences

### Positive

- ✅ **Single Source of Truth**: SQL schema in `.sq` files
- ✅ **Compile-Time Safety**: Invalid queries caught before runtime
- ✅ **Cross-Platform**: Same database code on Android, iOS, Desktop
- ✅ **Reactive UI**: Flow-based observation integrates with Compose
- ✅ **Easy Migrations**: SQL migration files with automatic validation
- ✅ **Performance**: Direct SQL execution, no ORM overhead
- ✅ **Developer Experience**: Great IDE support and autocomplete

### Negative

- ⚠️ **Learning Curve**: Team must learn SQL (mitigated: most devs know SQL)
- ⚠️ **No Lazy Loading**: Must explicitly fetch related entities (mitigated: we design schema appropriately)
- ⚠️ **Code Generation**: Build times slightly longer (mitigated: incremental compilation)

### Mitigation Strategies

1. **Learning SQL**: Provide SQL training materials and code examples
2. **Code Generation**: Use Gradle build cache to speed up clean builds
3. **Repository Pattern**: Wrap SQLDelight with repository for abstraction

## Alternatives Considered

### Alternative 1: Room (Android Architecture Component)

- **Pros:**
  - First-party Google library
  - Mature with extensive documentation
  - Annotation-based (less SQL boilerplate)
  - Kotlin Coroutines + Flow support

- **Cons:**
  - Android-only (no iOS or Desktop support)
  - Runtime reflection overhead
  - Cannot share database code across platforms
  - KSP required (slower builds)

- **Why Rejected:** Not a true KMP solution. Would require separate implementations for iOS and Desktop, duplicating logic and increasing maintenance burden.

### Alternative 2: Realm Kotlin

- **Pros:**
  - Object-oriented (no SQL)
  - Reactive queries
  - Multi-platform support
  - Live objects auto-update

- **Cons:**
  - Heavy runtime (large library size ~8MB)
  - Proprietary (not open standard)
  - Limited KMP maturity (beta status)
  - Migration complexity for schema changes
  - Learning curve for Realm-specific APIs

- **Why Rejected:** Large footprint and limited KMP stability. Realm's object-oriented approach doesn't fit our relational data model (tabs, history, etc.).

### Alternative 3: Exposed (Kotlin SQL Framework)

- **Pros:**
  - Type-safe Kotlin DSL
  - Multi-platform support
  - Familiar to Kotlin devs
  - ORM and DAO APIs

- **Cons:**
  - JVM-only (no native iOS support)
  - Higher learning curve (DSL syntax)
  - Less mature than SQLDelight for KMP
  - No built-in Flow support

- **Why Rejected:** JVM-only limitation prevents true multi-platform sharing. DSL syntax adds abstraction layer that obscures actual SQL.

### Alternative 4: Core Data (iOS) + Room (Android)

- **Pros:**
  - Platform-native solutions
  - Best performance per platform
  - Rich documentation

- **Cons:**
  - Duplicate database logic (Swift + Kotlin)
  - Inconsistent data models across platforms
  - 2x development and testing effort
  - Difficult to maintain parity

- **Why Rejected:** Violates DRY principle. Would require maintaining two separate codebases for the same business logic, increasing bugs and maintenance cost.

## Implementation Notes

### Database Structure

```
Modules/WebAvanue/coredata/
├── src/commonMain/
│   ├── sqldelight/
│   │   └── com/augmentalis/webavanue/data/
│   │       └── BrowserDatabase.sq        # SQL schema + queries
│   ├── kotlin/
│   │   ├── repository/
│   │   │   └── BrowserRepositoryImpl.kt  # Repository implementation
│   │   └── domain/
│   │       └── model/                    # Domain models
│   └── migrations/
│       ├── 1.sqm                         # Migration v1 → v2
│       └── 2.sqm                         # Migration v2 → v3
├── src/androidMain/
│   └── kotlin/
│       └── db/
│           └── DriverFactory.kt          # AndroidSqlDriver
└── src/iosMain/
    └── kotlin/
        └── db/
            └── DriverFactory.kt          # NativeSqlDriver
```

### Example Usage

```kotlin
// Define schema in BrowserDatabase.sq
CREATE TABLE IF NOT EXISTS tab (
    id TEXT PRIMARY KEY NOT NULL,
    url TEXT NOT NULL,
    title TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

selectAllTabs:
SELECT * FROM tab ORDER BY created_at DESC;

insertTab:
INSERT INTO tab(id, url, title, created_at)
VALUES (?, ?, ?, ?);

// Use in Repository
class BrowserRepositoryImpl(database: BrowserDatabase) {
    private val queries = database.browserDatabaseQueries

    fun observeTabs(): Flow<List<Tab>> =
        queries.selectAllTabs()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { dbTab -> dbTab.toDomainModel() } }
}
```

## References

- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [SQLDelight KMP Guide](https://cashapp.github.io/sqldelight/2.0.0/multiplatform_sqlite/)
- [Kotlin Flow Documentation](https://kotlinlang.org/docs/flow.html)
- [WebAvanue Database Schema](../../coredata/src/commonMain/sqldelight/com/augmentalis/webavanue/data/BrowserDatabase.sq)

## Revision History

| Version | Date       | Changes                           |
|---------|------------|-----------------------------------|
| 1.0     | 2025-12-12 | Initial ADR documenting decision  |
