# Scraping Architecture: Persistent vs Ephemeral UI Tracking

**Date:** 2025-10-18 21:35 PDT
**Author:** Manoj Jhawar
**Status:** ARCHITECTURAL DESIGN
**Goal:** Minimize redundant scraping through intelligent UI tracking

---

## Core Principle

> "Limit scraping as much as possible - that's why we have the database"

**Strategy:**
1. **Persistent UI** (views, fragments, dialogs) → Database (scrape once, reuse forever)
2. **Ephemeral UI** (transient popups, toasts) → One-time scraping (don't store)
3. **Hash-based deduplication** → Never scrape same element twice

---

## User's Architectural Vision

### The Problem
- Current scraping may re-scrape same elements repeatedly
- Wastes CPU, battery, memory
- Database exists to PREVENT redundant work
- Need to distinguish: permanent UI vs temporary UI

### The Solution
```
┌─────────────────────────────────────────┐
│ Accessibility Event                      │
│ (User navigates to new screen)          │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│ Check Element Hash in Database          │
└──────────────┬──────────────────────────┘
               │
        ┌──────┴──────┐
        │             │
   Hash EXISTS    Hash NEW
        │             │
        ▼             ▼
  ┌─────────┐   ┌──────────┐
  │ SKIP    │   │ CLASSIFY │
  │ (cached)│   │   UI     │
  └─────────┘   └─────┬────┘
                      │
              ┌───────┴───────┐
              │               │
        PERSISTENT      EPHEMERAL
              │               │
              ▼               ▼
      ┌──────────────┐  ┌──────────┐
      │ SCRAPE +     │  │ SCRAPE   │
      │ STORE in DB  │  │ ONE-TIME │
      └──────────────┘  └──────────┘
                            │
                            ▼
                    ┌──────────────┐
                    │ Use & Discard│
                    │ (no DB write)│
                    └──────────────┘
```

---

## UI Element Classification

### Persistent UI (Store in Database)

**Characteristics:**
- Stable across app sessions
- Predictable location/structure
- Part of normal app flow
- User expects to see again

**Examples:**
- **Activities/Fragments:** Main app screens
- **Dialogs:** Settings dialog, preferences dialog
- **Menus:** Navigation drawer, options menu
- **Bottom Sheets:** Standard bottom sheet content
- **Standard Views:** Buttons, text fields, lists

**Strategy:**
```kotlin
if (elementHash in database) {
    // Already scraped - use cached commands
    return getCachedCommands(elementHash)
} else {
    // New element - scrape and store
    val commands = scrapeElement(element)
    database.save(element, commands)
    return commands
}
```

**Memory:** Stored permanently, indexed by hash

### Ephemeral UI (One-Time Scraping Only)

**Characteristics:**
- Temporary/transient
- Unpredictable appearance
- Context-specific content
- May never appear again

**Examples:**
- **Toasts:** "File saved" notification
- **Snackbars:** Undo action prompts
- **Progress Dialogs:** "Loading..." spinners
- **Error Popups:** "Network error" alerts
- **Dynamic Popups:** Date pickers, time pickers
- **Ad Overlays:** Third-party ads
- **In-App Notifications:** "New message" badges

**Strategy:**
```kotlin
if (isEphemeral(element)) {
    // Scrape but don't store
    val commands = scrapeElement(element)
    cacheTemporarily(element, commands, ttl = 30.seconds)
    return commands
} else {
    // Persistent - store in database
}
```

**Memory:** Cached for 30-60 seconds, then discarded

### Hybrid UI (Context-Dependent)

**Characteristics:**
- Reusable structure, dynamic content
- Same layout, different data

**Examples:**
- **List Items:** Chat messages, email list
- **Dynamic Forms:** User profile fields
- **Search Results:** Same UI, different content

**Strategy:**
```kotlin
// Store element structure (template)
val template = element.extractTemplate()
if (template.hash in database) {
    // Reuse template structure
    return applydynamicContent(template, element.data)
} else {
    // New template - store it
    database.save(template)
}
```

---

## Hash-Based Deduplication System

### Current Implementation (Already Exists!)

**ScrapedElementEntity:**
```kotlin
@ColumnInfo(name = "element_hash")
val elementHash: String  // ← MD5 hash

@Index(value = ["element_hash"], unique = true)
```

**Hash Composition:**
```
elementHash = MD5(
    className +
    viewIdResourceName +
    text +
    contentDescription
)
```

### Enhanced Hash Strategy

**Problem with Current Hash:**
- Same button in different dialogs → same hash
- Can't distinguish context

**Solution: Context-Aware Hash**

```kotlin
/**
 * Generate context-aware element hash
 *
 * Includes parent container for proper deduplication
 */
fun generateElementHash(
    className: String,
    viewId: String?,
    text: String?,
    contentDesc: String?,
    parentContext: String? = null  // ← NEW
): String {
    val hashInput = buildString {
        append(className)
        viewId?.let { append("|vid:$it") }
        text?.let { append("|txt:$it") }
        contentDesc?.let { append("|cd:$it") }
        parentContext?.let { append("|ctx:$it") }
    }
    return md5(hashInput)
}

/**
 * Extract parent context for hash
 */
fun extractParentContext(element: AccessibilityNodeInfo): String? {
    val parent = element.parent ?: return null

    return when {
        isDialog(parent) -> "dialog:${parent.className}"
        isFragment(parent) -> "fragment:${parent.className}"
        isBottomSheet(parent) -> "bottomsheet:${parent.viewIdResourceName}"
        else -> null  // Normal view hierarchy - no context needed
    }
}
```

**Benefits:**
✅ Same button in different dialogs → different hashes
✅ Fragment-specific elements distinguished
✅ Popup context preserved

### Deduplication Flow

```kotlin
suspend fun processAccessibilityEvent(event: AccessibilityEvent) {
    val rootNode = event.source ?: return

    // Traverse UI tree
    traverseTree(rootNode) { element ->
        // Generate hash with context
        val parentContext = extractParentContext(element)
        val hash = generateElementHash(
            className = element.className,
            viewId = element.viewIdResourceName,
            text = element.text,
            contentDesc = element.contentDescription,
            parentContext = parentContext
        )

        // Check if already scraped
        if (databaseManager.elementExists(hash)) {
            // ✅ SKIP - already in database
            return@traverseTree
        }

        // Classify element
        when (classifyElement(element)) {
            ElementType.PERSISTENT -> {
                // Scrape and store permanently
                val scrapedData = scrapeElement(element)
                databaseManager.saveElement(hash, scrapedData)
            }

            ElementType.EPHEMERAL -> {
                // Scrape but don't store
                val scrapedData = scrapeElement(element)
                tempCache.put(hash, scrapedData, ttl = 30.seconds)
            }

            ElementType.HYBRID -> {
                // Store template, cache instance
                val template = extractTemplate(element)
                if (!databaseManager.templateExists(template.hash)) {
                    databaseManager.saveTemplate(template)
                }
            }
        }
    }
}
```

---

## Element Classification Logic

### Automatic Detection

```kotlin
enum class ElementType {
    PERSISTENT,   // Store in database
    EPHEMERAL,    // One-time scraping
    HYBRID        // Template + dynamic data
}

fun classifyElement(element: AccessibilityNodeInfo): ElementType {
    val className = element.className.toString()

    // Ephemeral UI patterns
    return when {
        // Toast notifications
        className.contains("Toast") -> ElementType.EPHEMERAL

        // Snackbars
        className.contains("Snackbar") -> ElementType.EPHEMERAL

        // Progress indicators
        className.contains("Progress") &&
            element.text?.contains("Loading") == true -> ElementType.EPHEMERAL

        // Temporary dialogs
        isTemporaryDialog(element) -> ElementType.EPHEMERAL

        // List items (hybrid - reusable structure)
        isListItem(element) -> ElementType.HYBRID

        // Default: persistent
        else -> ElementType.PERSISTENT
    }
}

fun isTemporaryDialog(element: AccessibilityNodeInfo): Boolean {
    val text = element.text?.toString()?.lowercase() ?: ""

    // Common temporary dialog patterns
    return text.contains("loading") ||
           text.contains("please wait") ||
           text.contains("error") ||
           text.contains("success") ||
           text.contains("saved") ||
           hasShortLivedParent(element)
}

fun hasShortLivedParent(element: AccessibilityNodeInfo): Boolean {
    var parent = element.parent
    while (parent != null) {
        val className = parent.className.toString()
        if (className.contains("Toast") ||
            className.contains("Snackbar") ||
            className.contains("Popup")) {
            return true
        }
        parent = parent.parent
    }
    return false
}
```

### Manual Override (Future Enhancement)

```kotlin
// Allow developers to mark specific elements
@Ephemeral
class CustomAdPopup : Dialog {
    // This popup will never be stored
}

@Persistent(ttl = 24.hours)
class DynamicContentDialog : Dialog {
    // Store but expire after 24 hours
}
```

---

## Scraping Optimization Strategies

### Strategy 1: Hash Check Before Scraping

```kotlin
suspend fun scrapeScreen(packageName: String) {
    val rootNode = getRootNode()
    val scrapedCount = 0
    val skippedCount = 0

    traverseTree(rootNode) { element ->
        val hash = generateElementHash(element)

        // ✅ OPTIMIZATION: Check hash FIRST
        if (databaseManager.elementExists(hash)) {
            skippedCount++
            return@traverseTree  // Skip scraping entirely
        }

        // Only scrape if NOT in database
        scrapeAndStoreElement(element, hash)
        scrapedCount++
    }

    logger.debug("Scraped: $scrapedCount, Skipped: $skippedCount")
}
```

**Benefit:** Up to 90% reduction in scraping work on revisited screens

### Strategy 2: Incremental Scraping

```kotlin
suspend fun scrapeScreenIncremental(packageName: String) {
    val currentHashes = mutableSetOf<String>()

    // First pass: collect all hashes
    traverseTree(rootNode) { element ->
        val hash = generateElementHash(element)
        currentHashes.add(hash)
    }

    // Batch check database
    val existingHashes = databaseManager.getExistingHashes(currentHashes)
    val newHashes = currentHashes - existingHashes

    // Only scrape new elements
    if (newHashes.isEmpty()) {
        logger.debug("Screen fully cached - 0 elements scraped")
        return
    }

    // Second pass: scrape only new elements
    traverseTree(rootNode) { element ->
        val hash = generateElementHash(element)
        if (hash in newHashes) {
            scrapeAndStoreElement(element, hash)
        }
    }
}
```

**Benefit:** Batch database queries, minimize traversals

### Strategy 3: Smart Caching Layer

```kotlin
class SmartScrapingCache {
    // L1: In-memory (hot data)
    private val memoryCache = LruCache<String, ScrapedElement>(maxSize = 100)

    // L2: Database (persistent)
    private val databaseManager: IDatabaseManager

    // L3: Temporary (ephemeral UI)
    private val tempCache = ExpiringCache<String, ScrapedElement>(ttl = 30.seconds)

    suspend fun getOrScrape(element: AccessibilityNodeInfo): ScrapedElement {
        val hash = generateElementHash(element)

        // Check L1: Memory
        memoryCache.get(hash)?.let { return it }

        // Check L3: Temporary (for ephemeral)
        tempCache.get(hash)?.let { return it }

        // Check L2: Database
        databaseManager.getElement(hash)?.let { cached ->
            memoryCache.put(hash, cached)  // Promote to L1
            return cached
        }

        // Cache miss - scrape element
        val scraped = scrapeElement(element)

        // Store based on classification
        when (classifyElement(element)) {
            ElementType.PERSISTENT -> {
                databaseManager.saveElement(hash, scraped)
                memoryCache.put(hash, scraped)
            }
            ElementType.EPHEMERAL -> {
                tempCache.put(hash, scraped)  // Only temp cache
            }
            ElementType.HYBRID -> {
                // Store template in DB, instance in temp cache
            }
        }

        return scraped
    }
}
```

**Benefit:** 3-tier caching minimizes database hits and redundant scraping

---

## Performance Metrics

### Before Optimization (Naive Scraping)

**Scenario:** User navigates Settings → Display → Settings (back)

```
Navigation: Settings screen
├── Elements found: 50
├── Already in DB: 0
├── Scraped: 50
└── Time: 500ms

Navigation: Display screen
├── Elements found: 30
├── Already in DB: 0
├── Scraped: 30
└── Time: 300ms

Navigation: Settings screen (BACK)
├── Elements found: 50
├── Already in DB: 0  ❌ Re-scraped everything!
├── Scraped: 50  ❌ Redundant work!
└── Time: 500ms  ❌ Wasted time!

Total: 130 elements scraped, 1300ms
```

### After Optimization (Hash-Based Deduplication)

```
Navigation: Settings screen
├── Elements found: 50
├── Already in DB: 0
├── Scraped: 50
├── Stored: 50
└── Time: 500ms

Navigation: Display screen
├── Elements found: 30
├── Already in DB: 0
├── Scraped: 30
├── Stored: 30
└── Time: 300ms

Navigation: Settings screen (BACK)
├── Elements found: 50
├── Already in DB: 50  ✅ All cached!
├── Scraped: 0  ✅ Zero work!
└── Time: 10ms  ✅ Hash check only!

Total: 80 elements scraped, 810ms
Savings: 38% less scraping, 38% faster
```

### With Ephemeral Detection

**Scenario:** User sees toast notification on Settings screen

```
Navigation: Settings screen
├── Elements found: 50
├── Persistent: 50
├── Already in DB: 50  ✅ All cached
├── Scraped: 0
└── Time: 10ms

Toast appears: "Settings saved"
├── Elements found: 51 (50 + 1 toast)
├── Toast detected: EPHEMERAL
├── Scraped: 1 (toast only)
├── Stored in DB: 0  ✅ Not stored!
├── Cached temporarily: 1
└── Time: 20ms

Toast disappears
├── Temp cache expires
├── DB unchanged  ✅ No clutter!
└── Memory freed

Total: 1 element scraped, 30ms
```

---

## Implementation Roadmap

### Phase 1: Hash Deduplication (~5 min AI time)

**Tasks:**
1. Add `elementExists(hash)` method to DatabaseManager
2. Implement hash check before scraping
3. Add scraping metrics (scraped vs skipped count)

**Deliverable:**
```kotlin
suspend fun elementExists(hash: String): Boolean
```

### Phase 2: Element Classification (~8 min AI time)

**Tasks:**
1. Create `ElementType` enum
2. Implement `classifyElement()` logic
3. Add ephemeral detection patterns
4. Create temporary cache for ephemeral UI

**Deliverable:**
```kotlin
fun classifyElement(element: AccessibilityNodeInfo): ElementType
```

### Phase 3: Context-Aware Hashing (~6 min AI time)

**Tasks:**
1. Enhance hash generation with parent context
2. Add fragment/dialog detection
3. Update existing hash generation calls
4. Migration for existing hashes

**Deliverable:**
```kotlin
fun generateElementHash(..., parentContext: String?): String
```

### Phase 4: Smart Caching Layer (~10 min AI time)

**Tasks:**
1. Implement 3-tier cache (memory, temp, database)
2. Add LRU eviction for memory cache
3. Add TTL expiration for temp cache
4. Integrate with scraping service

**Deliverable:**
```kotlin
class SmartScrapingCache { ... }
```

### Phase 5: Performance Monitoring (~3 min AI time)

**Tasks:**
1. Add scraping metrics tracking
2. Log cache hit/miss rates
3. Add performance profiling
4. Dashboard for scraping efficiency

**Deliverable:**
```kotlin
data class ScrapingMetrics(
    val elementsFound: Int,
    val elementsCached: Int,
    val elementsScraped: Int,
    val timeMs: Long
)
```

**Total Implementation Time:** ~32 minutes (AI time on M1 Pro Max)

---

## Database Schema Enhancements

### Add Element Metadata

```kotlin
@Entity(tableName = "scraped_elements")
data class ScrapedElementEntity(
    // ... existing fields ...

    /**
     * Element classification
     * PERSISTENT = store permanently
     * EPHEMERAL = one-time use
     * HYBRID = template-based
     */
    @ColumnInfo(name = "element_type")
    val elementType: String = "PERSISTENT",

    /**
     * Number of times element was seen
     * High count = definitely persistent
     */
    @ColumnInfo(name = "seen_count")
    val seenCount: Int = 1,

    /**
     * Last time element was accessed
     * For cache eviction
     */
    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = System.currentTimeMillis()
)
```

### Add Scraping Metrics Table

```kotlin
@Entity(tableName = "scraping_metrics")
data class ScrapingMetricsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "screen_hash")
    val screenHash: String,  // Hash of screen state

    @ColumnInfo(name = "elements_found")
    val elementsFound: Int,

    @ColumnInfo(name = "elements_cached")
    val elementsCached: Int,

    @ColumnInfo(name = "elements_scraped")
    val elementsScraped: Int,

    @ColumnInfo(name = "scraping_time_ms")
    val scrapingTimeMs: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
```

---

## Summary

### Architecture Answer

**Question:** Track all views/fragments/popups, ephemeral popups scraped once?

**Answer:** ✅ YES - Full support with intelligent classification

**Implementation:**
1. **Persistent UI** → Database (hash-based deduplication)
2. **Ephemeral UI** → One-time scraping (temp cache, no DB)
3. **Hybrid UI** → Template storage (reusable structure)

**Performance Impact:**
- 38-90% reduction in redundant scraping
- Hash check: ~10ms vs full scrape: ~500ms
- Memory: Only active screens cached (~1 MB)

**Database Purpose:**
> "Limit scraping as much as possible" ✅ Achieved

**Time to Implement:** ~32 minutes (AI time on M1 Pro Max)

---

**Author:** Manoj Jhawar
**Status:** READY FOR IMPLEMENTATION
**Next Step:** Approve architecture, implement Phase 1 (hash deduplication)
