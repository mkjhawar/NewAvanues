# WebAvanue-Plan-DOMScrapingPersistence-260210-V1

## Overview

Implement a two-tier persistence and caching system for web DOM scraping results, eliminating redundant re-scraping when users switch between apps and web, or revisit pages. Mirrors the proven app scraping deduplication pattern (screen hash + element hash) adapted for web DOM.

## Problem Statement

Currently, every time a user navigates to a web page (or returns to one after switching to another app), the full DOM is re-scraped even if the page hasn't changed. This causes:
1. **Latency**: 200-500ms scrape time per page, noticeable on element-heavy pages (100+ elements)
2. **Speech grammar churn**: Unnecessary `updateCommands()` calls to the speech engine
3. **Battery/CPU waste**: JavaScript DOM traversal + element extraction on every visit
4. **Lost context**: No persistence across app restarts for frequently visited pages

## Design: Two-Tier Cache (Branch C + D)

### Tier 1: Session Memory Cache (In-Process)

**Purpose**: Instant restoration when switching tabs or returning to a recently visited page within the same browser session.

**Implementation**: LRU cache in `BrowserVoiceOSCallback` keyed by URL hash.

```
HashMap<String, CachedPage>  // urlHash -> CachedPage
```

**CachedPage data class**:
```kotlin
data class CachedPage(
    val urlHash: String,
    val url: String,
    val domain: String,
    val structureHash: String,
    val commands: List<WebVoiceCommand>,
    val phrases: List<String>,
    val elementCount: Int,
    val cachedAt: Long,
    val accessCount: Int = 1
)
```

**Capacity**: 5 pages (LRU eviction). Configurable.

**Lifecycle**: Lives as long as `BrowserVoiceOSCallback` instance. Cleared on process death.

### Tier 2: Database Cache (Cross-Session)

**Purpose**: Persist commands for frequently visited pages across app restarts. Already ~80% implemented in existing schema.

**Existing tables used**:
- `ScrapedWebsite` — URL hash (PK), structure_hash, is_stale, access_count
- `ScrapedWebElement` — element_hash, FK to website
- `ScrapedWebCommand` — command persistence per domain/URL

**Key change**: Remove whitelist gate from the cache READ path. Whitelist remains for controlling auto-scan and save-permanently behavior, but ALL pages can be cached for the duration of a TTL.

## Architecture

```
                        ┌─────────────────────────┐
                        │  onPageLoadStarted(url)  │
                        └────────────┬────────────┘
                                     │
                        ┌────────────▼────────────┐
                        │  Compute urlHash(url)    │
                        └────────────┬────────────┘
                                     │
                   ┌─────────────────▼──────────────────┐
                   │  Tier 1: Session Cache Lookup       │
                   │  sessionCache[urlHash]              │
                   └─────┬──────────────────────┬───────┘
                    HIT  │                      │ MISS
                   ┌─────▼─────┐      ┌─────────▼─────────┐
                   │ Emit      │      │ Tier 2: DB Lookup  │
                   │ cached    │      │ ScrapedWebsite     │
                   │ phrases   │      │ by url_hash        │
                   │ instantly │      └──┬──────────────┬──┘
                   │ DONE      │    FOUND│         NOT  │
                   └───────────┘    !stale│        FOUND │
                              ┌────────▼──────┐  ┌──────▼──────┐
                              │ Load commands │  │ Proceed to  │
                              │ from DB, emit │  │ full scrape │
                              │ phrases, put  │  │             │
                              │ in session    │  │             │
                              │ cache         │  │             │
                              └───────────────┘  └──────┬──────┘
                                                        │
                              ┌──────────────────────────▼──────┐
                              │  onDOMScraped(result)           │
                              │  1. Compute structureHash(DOM)  │
                              │  2. Compare with cached hash    │
                              └──┬─────────────────────┬────────┘
                            SAME │                DIFF │ or NEW
                   ┌─────────────▼───┐    ┌────────────▼────────┐
                   │ Update          │    │ Persist new commands│
                   │ access_count    │    │ Update structure    │
                   │ Skip persist    │    │ hash in DB          │
                   │ Update session  │    │ Update session      │
                   │ cache timestamp │    │ cache               │
                   └─────────────────┘    └─────────────────────┘
```

## Implementation Plan

### Phase 1: JavaScript Structure Hash (Web-side)

**File**: `Modules/WebAvanue/src/commonMain/kotlin/com/augmentalis/webavanue/DOMScraperBridge.kt`

Add `computeStructureHash()` to the JavaScript injection that generates a structural fingerprint of the DOM, mirroring `ScreenCacheManager.generateScreenHash()` from Android.

**Hashing strategy** (structural, no text content):
```javascript
function computeStructureHash() {
    const signatures = [];
    function walk(node, depth) {
        if (depth > 5 || !node || node.nodeType !== 1) return;
        const tag = node.tagName.toLowerCase();
        const id = node.id ? '#' + node.id : '';
        const role = node.getAttribute('role') || '';
        const type = node.getAttribute('type') || '';
        const isInteractive = ['a','button','input','select','textarea'].includes(tag) ? 'I' : '';
        const childCount = node.children.length;
        signatures.push(`${tag}${id}:${role}:${type}:d${depth}:c${childCount}:${isInteractive}`);
        for (let i = 0; i < Math.min(node.children.length, 20); i++) {
            walk(node.children[i], depth + 1);
        }
    }
    walk(document.body, 0);
    signatures.sort();
    // Simple hash: djb2 on joined signatures
    let hash = 5381;
    const str = signatures.join('|');
    for (let i = 0; i < str.length; i++) {
        hash = ((hash << 5) + hash) + str.charCodeAt(i);
        hash = hash & hash; // Convert to 32bit
    }
    return (hash >>> 0).toString(16).padStart(8, '0');
}
```

**Key design decisions**:
- Max depth 5 (matches Android `ScreenCacheManager`)
- Structural properties only: tag, id, role, type, childCount, interactivity
- NO text content (avoids false invalidation from counter/timestamp changes)
- Max 20 children per node (avoids huge lists like search results)
- Sorted signatures → order-independent hash

**Return in scrape result**: Add `structureHash` field to `DOMScrapeResult`.

### Phase 2: Session Cache in BrowserVoiceOSCallback

**File**: `Modules/WebAvanue/src/commonMain/kotlin/com/augmentalis/webavanue/BrowserVoiceOSCallback.kt`

Add:
```kotlin
// Session-level LRU cache for recently visited pages
private val sessionCache = LinkedHashMap<String, CachedPage>(5, 0.75f, true)
private val maxCacheSize = 5
```

**Modify `onPageLoadStarted(url)`**:
```kotlin
override fun onPageLoadStarted(url: String) {
    val urlHash = computeUrlHash(url)

    // Tier 1: Session cache hit → emit cached phrases instantly
    sessionCache[urlHash]?.let { cached ->
        _activeWebPhrases.value = cached.phrases
        _commandCount.value = cached.commands.size
        commandGenerator.clear()
        commandGenerator.addCachedCommands(cached.commands)
        _scrapingState.value = DOMScrapingState.Complete(
            elementCount = cached.elementCount,
            commandCount = cached.commands.size,
            isWhitelisted = _isWhitelistedDomain.value,
            fromCache = true
        )
        cached.accessCount++
        println("VoiceOS: Session cache HIT for $url (${cached.commands.size} commands)")
        // Still proceed with page load for potential DOM change detection
        return  // Skip DOM scraping trigger
    }

    // Tier 2: DB cache check (async)
    scope.launch {
        checkDatabaseCache(urlHash, url)
    }

    // Normal flow continues...
}
```

**Eviction**: When `sessionCache.size > maxCacheSize`, remove eldest entry (LinkedHashMap access-order handles this).

### Phase 3: Database Cache Integration

**Modify `BrowserVoiceOSCallback`** to:

1. **On page load** (Tier 2 miss from session cache):
   - Query `ScrapedWebsite` by `url_hash`
   - If found AND `is_stale = false`:
     - Load commands from `ScrapedWebCommand` by domain+url
     - Emit phrases, populate session cache
     - Set `skipScrape = true` flag
   - If found AND `is_stale = true`:
     - Proceed to full scrape
   - If not found:
     - Proceed to full scrape

2. **On DOM scraped** (after JS runs):
   - Compute `structureHash` from result
   - Compare with DB `structure_hash`:
     - SAME → increment `access_count`, update `last_accessed_at`, skip command persist
     - DIFFERENT → persist new commands, update `structure_hash`, mark old commands deprecated
     - NEW → insert `ScrapedWebsite` + commands
   - Update session cache

3. **Cache TTL**: Pages older than 7 days without access → mark `is_stale = true`

### Phase 4: Manual Retrain Voice Command

**File**: `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/StaticCommandRegistry.kt`

Add to BROWSER category:
```kotlin
StaticCommand(
    phrase = "retrain page",
    action = "RETRAIN_PAGE",
    category = CommandCategory.BROWSER,
    description = "Force re-scrape current web page"
),
StaticCommand(
    phrase = "rescan page",
    action = "RETRAIN_PAGE",
    category = CommandCategory.BROWSER,
    description = "Force re-scrape current web page (alias)"
)
```

**Execution flow**:
1. User says "retrain page"
2. VoiceOSCore routes to ActionCoordinator → BrowserHandler
3. BrowserHandler calls `BrowserVoiceOSCallback.invalidateCurrentPage()`
4. Method marks `is_stale = true` in `ScrapedWebsite` for current URL
5. Removes from session cache
6. Triggers fresh DOM scrape

### Phase 5: Stable Element Hashing in JavaScript

**Current problem**: JS generates sequential IDs (`vos_1`, `vos_2`) that change on every scrape.

**Fix**: Generate stable element hashes based on structural properties:
```javascript
function stableElementHash(element) {
    const parts = [];
    if (element.id) parts.push('id:' + element.id);
    if (element.getAttribute('name')) parts.push('name:' + element.getAttribute('name'));
    if (element.getAttribute('aria-label')) parts.push('aria:' + element.getAttribute('aria-label'));
    if (parts.length === 0) {
        // Fallback: CSS path + tag + type
        parts.push('path:' + getCssPath(element));
        parts.push('tag:' + element.tagName);
        parts.push('type:' + (element.getAttribute('type') || ''));
    }
    return djb2Hash(parts.join('|'));
}
```

**Priority hierarchy** (mirrors Android `deriveElementHash`):
1. `id` attribute (most stable)
2. `name` attribute
3. `aria-label`
4. CSS path + tag + type (structural fallback)

This allows matching elements across scrapes for delta detection.

## File Changes Summary

| File | Change | Phase |
|------|--------|-------|
| `DOMScraperBridge.kt` (JS injection) | Add `computeStructureHash()`, `stableElementHash()` | 1, 5 |
| `DOMScrapeResult.kt` | Add `structureHash` field | 1 |
| `BrowserVoiceOSCallback.kt` | Add session cache, DB cache lookup, invalidation | 2, 3 |
| `DOMScrapingState.kt` | Add `fromCache` flag to `Complete` state | 2 |
| `StaticCommandRegistry.kt` | Add "retrain page" / "rescan page" commands | 4 |
| `BrowserHandler.kt` (if exists) | Route RETRAIN_PAGE action | 4 |
| No schema changes needed | Existing tables support everything | - |

## Existing Infrastructure Leveraged

| Component | Already Exists | Usage |
|-----------|---------------|-------|
| `ScrapedWebsite.url_hash` | Yes | URL-based cache key |
| `ScrapedWebsite.structure_hash` | Yes | DOM change detection |
| `ScrapedWebsite.is_stale` | Yes | Manual retrain flag |
| `ScrapedWebsite.access_count` | Yes | Frequency tracking |
| `ScrapedWebCommand` table | Yes | Command persistence |
| `IScrapedWebCommandRepository` | Yes | DB access layer |
| `WebAppWhitelist` | Yes | Controls permanent save (unmodified) |

## Whitelist vs Cache Distinction

| Feature | Cached (All Pages) | Whitelisted |
|---------|-------------------|-------------|
| Session cache | Yes | Yes |
| DB persistence | TTL (7 days) | Permanent |
| Auto-scan on load | After first visit | Always |
| Manual retrain | Yes | Yes |
| Usage tracking | Basic access_count | Full usage_count per command |
| User approval | Not needed | User adds to whitelist |

## Risk Assessment

| Risk | Mitigation |
|------|-----------|
| Storage growth from caching all pages | TTL eviction (7 days no access), session cache limited to 5 |
| Stale commands served after JS-heavy page changes | Structure hash catches most changes; manual retrain for edge cases |
| Sequential JS IDs break element matching | Phase 5 adds stable hashing (id > name > aria-label > CSS path) |
| Race condition: cache served while page updates | Session cache is synchronous; DB cache async with eventual consistency |
| Memory pressure from session cache | 5 pages max, CachedPage is lightweight (commands list + metadata) |

## Testing Strategy

1. **Unit tests**: Structure hash computation (same page = same hash, different page = different hash)
2. **Unit tests**: Session cache LRU eviction (add 6 pages, verify eldest evicted)
3. **Unit tests**: Stable element hashing (same element across scrapes = same hash)
4. **Integration test**: Full flow — load page → cache → switch app → return → verify cache hit
5. **Integration test**: Page change detection — load page → modify DOM → reload → verify rescrape
6. **Manual test**: "retrain page" voice command forces rescrape

## Implementation Order

1. **Phase 1** (JS structure hash) — Foundation, needed by Phase 3
2. **Phase 5** (Stable element hash) — Foundation, needed by Phase 3
3. **Phase 2** (Session cache) — Highest user-facing impact, minimal code
4. **Phase 3** (DB cache integration) — Full persistence, builds on Phase 1+5
5. **Phase 4** (Retrain command) — UX polish, can be done anytime

**Estimated effort**: ~4-6 hours across phases. Phase 2 alone (~30 min) solves the immediate tab-switching case.

## References

- App scraping hash pattern: `Modules/VoiceOSCore/src/androidMain/.../ScreenCacheManager.kt`
- App element hash: `Modules/VoiceOSCore/src/androidMain/.../CommandGenerator.kt:439`
- Web scraping tables: `Modules/Database/src/commonMain/sqldelight/.../ScrapedWebsite.sq`
- Analysis doc: `docs/analysis/WebAvanue/WebAvanue-Analysis-TestingIssues-260210-V1.md`
- Handover: `.claude/handovers/handover-260210-2100.md`
