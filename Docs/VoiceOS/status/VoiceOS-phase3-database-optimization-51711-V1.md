# Phase 3 Database Optimization Guide

**Created**: 2025-11-09
**Status**: Phase 3 Medium Priority Issue #32
**Database**: AppScrapingDatabase (Room)

## Current Index Status

### ScrapedElementEntity
**Table**: `scraped_elements`

**Existing Indexes**:
- ✅ `app_id` - Foreign key index (CASCADE delete)
- ✅ `element_hash` - Unique index for fast hash lookup
- ✅ `view_id_resource_name` - Resource ID lookup
- ✅ `uuid` - Universal identifier lookup

**Status**: Well-indexed for primary access patterns

### Recommended Additional Indexes

#### 1. Compound Index: (app_id, scrapedAt)
**Purpose**: Time-range queries per app
**Query Pattern**: `SELECT * FROM scraped_elements WHERE app_id = ? AND scrapedAt > ?`
**Impact**: High - used by data retention cleanup

```kotlin
@Entity(
    indices = [
        // ... existing indexes ...
        Index(value = ["app_id", "scraped_at"], name = "idx_app_timestamp")
    ]
)
```

#### 2. Compound Index: (app_id, isClickable)
**Purpose**: Finding interactive elements per app
**Query Pattern**: `SELECT * FROM scraped_elements WHERE app_id = ? AND isClickable = 1`
**Impact**: Medium - frequently used for command generation

```kotlin
Index(value = ["app_id", "is_clickable"], name = "idx_app_clickable")
```

#### 3. Compound Index: (app_id, semanticRole)
**Purpose**: Semantic element lookup
**Query Pattern**: `SELECT * FROM scraped_elements WHERE app_id = ? AND semanticRole = ?`
**Impact**: Medium - semantic command matching

```kotlin
Index(value = ["app_id", "semantic_role"], name = "idx_app_semantic")
```

## Query Optimization Recommendations

### 1. Use Indexed Columns in WHERE Clauses

**Bad**:
```kotlin
@Query("SELECT * FROM scraped_elements WHERE LOWER(text) LIKE :query")
```

**Good**:
```kotlin
@Query("SELECT * FROM scraped_elements WHERE app_id = :appId AND element_hash = :hash")
```

### 2. Avoid SELECT *

**Bad**:
```kotlin
@Query("SELECT * FROM scraped_elements WHERE app_id = :appId")
```

**Good**:
```kotlin
@Query("SELECT id, element_hash, text FROM scraped_elements WHERE app_id = :appId")
```

### 3. Use Covering Indexes

For frequently accessed columns, include them in the index:

```kotlin
// If query is: SELECT element_hash, text WHERE app_id = ?
Index(value = ["app_id", "element_hash", "text"], name = "idx_app_hash_text")
```

### 4. Limit Result Sets

**Bad**:
```kotlin
@Query("SELECT * FROM scraped_elements WHERE app_id = :appId")
```

**Good**:
```kotlin
@Query("SELECT * FROM scraped_elements WHERE app_id = :appId LIMIT 100")
```

## Query Performance Analysis

### High-Frequency Queries (Need Optimization)

1. **Element Lookup by Hash**
   - Query: `SELECT * FROM scraped_elements WHERE element_hash = ?`
   - Current: ✅ Indexed (unique)
   - Status: Optimal

2. **Elements by App**
   - Query: `SELECT * FROM scraped_elements WHERE app_id = ?`
   - Current: ✅ Indexed
   - Status: Optimal

3. **Clickable Elements by App**
   - Query: `SELECT * FROM scraped_elements WHERE app_id = ? AND isClickable = 1`
   - Current: ⚠️ Partial index (app_id only)
   - Recommendation: Add compound index

4. **Time-Range Queries (Data Retention)**
   - Query: `SELECT * FROM scraped_elements WHERE scrapedAt < ?`
   - Current: ❌ No index
   - Recommendation: Add timestamp index

## Migration Strategy

### Phase 1: Add Critical Indexes
Priority: High
Timeline: Immediate

```kotlin
// database/migration/DatabaseMigrations.kt
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add timestamp index for data retention
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_scraped_at " +
            "ON scraped_elements(scraped_at)"
        )

        // Add compound index for app + timestamp
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_app_timestamp " +
            "ON scraped_elements(app_id, scraped_at)"
        )
    }
}
```

### Phase 2: Add Query-Specific Indexes
Priority: Medium
Timeline: Next sprint

```kotlin
val MIGRATION_Y_Z = object : Migration(Y, Z) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add compound indexes for common query patterns
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_app_clickable " +
            "ON scraped_elements(app_id, is_clickable)"
        )

        database.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_app_semantic " +
            "ON scraped_elements(app_id, semantic_role)"
        )
    }
}
```

### Phase 3: Analyze and Optimize
Priority: Low
Timeline: After production data collection

1. Use `EXPLAIN QUERY PLAN` to analyze actual query performance
2. Monitor database size impact of additional indexes
3. Remove unused indexes if they cause more harm than good

## Index Size Analysis

### Estimated Index Overhead

Assuming 10,000 scraped elements:

- `app_id` index: ~40 KB
- `element_hash` unique index: ~400 KB (hash strings)
- `uuid` index: ~400 KB (UUID strings)
- `scraped_at` index: ~80 KB (timestamps)
- Compound indexes: ~120 KB each

**Total overhead**: ~1.2 MB for full indexing

**Benefit**: 10-100x query speed improvement

## Monitoring and Maintenance

### 1. Query Performance Metrics

Track in DataRetentionPolicy:
- Average query time
- 95th percentile query time
- Slow query count (>100ms)

### 2. Database Size Monitoring

```kotlin
fun getDatabaseMetrics(): Map<String, Any> {
    return mapOf(
        "sizeBytes" to getDatabaseSize(),
        "indexSizeBytes" to getIndexSize(),
        "tableCount" to getTableCount(),
        "indexCount" to getIndexCount()
    )
}
```

### 3. ANALYZE Command

Run periodically to update query planner statistics:

```kotlin
fun optimizeDatabase() {
    database.query("ANALYZE", null).use { it.moveToFirst() }
}
```

## Best Practices Summary

1. ✅ **DO**: Index foreign keys (already done)
2. ✅ **DO**: Index frequently queried columns
3. ✅ **DO**: Use compound indexes for multi-column queries
4. ✅ **DO**: Add LIMIT clauses to unbounded queries
5. ❌ **DON'T**: Index low-cardinality columns (true/false)
6. ❌ **DON'T**: Create too many indexes (write performance penalty)
7. ❌ **DON'T**: Use SELECT * in production queries
8. ✅ **DO**: Monitor query performance in production
9. ✅ **DO**: Run VACUUM after large deletions
10. ✅ **DO**: Use data retention policies to prevent unbounded growth

## Implementation Checklist

- [x] Document existing indexes
- [x] Identify optimization opportunities
- [x] Design migration strategy
- [ ] Create database migration for critical indexes
- [ ] Add query performance monitoring
- [ ] Test index performance impact
- [ ] Deploy to production
- [ ] Monitor and tune based on real usage

## Related Documents

- `DataRetentionPolicy.kt` - Automatic cleanup implementation
- `CircuitBreaker.kt` - Database fault tolerance
- `VoiceOSConstants.kt` - Database configuration constants

Last updated: 2025-11-09
