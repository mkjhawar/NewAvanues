# VoiceAccessibility Scraping Subsystem - Developer Documentation Index

**Module:** VoiceAccessibility
**Subsystem:** Scraping
**Last Updated:** 2025-10-10 10:34:00 PDT
**VOS4 Version:** 4.0.0

## Overview

The scraping subsystem is responsible for discovering UI elements through Android's Accessibility Service, storing them in a Room database, generating voice commands, and processing voice input to execute actions.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│          VoiceOSService                                  │
│                    (Android)                             │
└────────────────────┬────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────┐
│     AccessibilityScrapingIntegration                     │
│     (Main Integration Layer)                             │
└──┬──────────────┬──────────────┬──────────────┬─────────┘
   ↓              ↓              ↓              ↓
┌─────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐
│Database │  │Command   │  │Voice     │  │AppHash         │
│         │  │Generator │  │Processor │  │Calculator      │
└─────────┘  └──────────┘  └──────────┘  └────────────────┘
     ↓
┌─────────────────────────────────────────────────────────┐
│              Room Database                               │
│  ┌──────────┬────────────┬───────────┬────────────────┐ │
│  │Apps      │Elements    │Hierarchy  │Commands        │ │
│  └──────────┴────────────┴───────────┴────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## File Documentation

### Core Integration
- **[AccessibilityScrapingIntegration](./AccessibilityScrapingIntegration-Developer-Documentation-251010-1034.md)** - Main integration layer, coordinates all scraping operations
- **ScrapingMode** - Defines DYNAMIC vs LEARN_APP scraping modes
- **ScrapingCoordinator** - Legacy coordinator (being phased out)
- **AccessibilityTreeScraper** - Legacy tree scraper (functionality moved to AccessibilityScrapingIntegration)

### Command Generation & Processing
- **[CommandGenerator](./CommandGenerator-Developer-Documentation-251010-1034.md)** - NLP-based voice command generator with synonyms
- **VoiceCommandProcessor** - Processes voice input and executes UI actions

### Hashing & Fingerprinting
- **AppHashCalculator** - App fingerprinting (MD5 based) - DEPRECATED, use AccessibilityFingerprint
- **ElementHasher** - Element hashing - DEPRECATED, use AccessibilityFingerprint

### Database Layer

#### Database Setup
- **AppScrapingDatabase** - Room database setup with migrations

#### Entities
- **ScrapedAppEntity** - App metadata (package, version, hash)
- **ScrapedElementEntity** - UI element data (text, bounds, capabilities)
- **ScrapedHierarchyEntity** - Parent-child relationships
- **GeneratedCommandEntity** - Voice commands with synonyms

#### DAOs
- **ScrapedAppDao** - App CRUD operations
- **ScrapedElementDao** - Element CRUD, hash lookups, UPSERT
- **ScrapedHierarchyDao** - Hierarchy queries, tree traversal
- **GeneratedCommandDao** - Command queries, fuzzy matching, usage tracking

## Key Concepts

### Hash-Based Element Identification

Elements are identified using `AccessibilityFingerprint` (from UUIDCreator library):
- **SHA-256 hash** of className + viewId + text + contentDescription + hierarchy path
- **Version-scoped** to prevent cross-version conflicts
- **Collision-resistant** via hierarchy path inclusion
- **Persistent** across app restarts (as long as UI doesn't change)

**Legacy:** Older code uses `AppHashCalculator` and `ElementHasher` (MD5 based, no hierarchy awareness). These are deprecated and will be removed in v3.0.0.

### Scraping Modes

1. **DYNAMIC Mode** (Default)
   - Triggered automatically on window state changes
   - Scrapes only visible UI elements
   - Fast, lightweight, on-demand
   - Provides partial coverage based on user navigation

2. **LEARN_APP Mode** (User-triggered)
   - Comprehensive UI traversal
   - Attempts to discover all screens
   - Merges with existing data via hash-based UPSERT
   - Marks app as fully learned on completion

### Database Schema (v3)

```sql
-- Apps table
scraped_apps (
    app_id TEXT PRIMARY KEY,
    package_name TEXT,
    app_hash TEXT,      -- MD5(packageName + versionCode)
    is_fully_learned INTEGER,
    scraping_mode TEXT,
    ...
)

-- Elements table
scraped_elements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    element_hash TEXT UNIQUE,  -- SHA-256 from AccessibilityFingerprint
    app_id TEXT,               -- FK to scraped_apps
    class_name TEXT,
    text TEXT,
    ...
)

-- Hierarchy table (uses Long IDs for performance)
scraped_hierarchy (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    parent_element_id INTEGER,  -- FK to scraped_elements.id
    child_element_id INTEGER,   -- FK to scraped_elements.id
    child_order INTEGER,
    ...
)

-- Commands table
generated_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    element_hash TEXT,         -- FK to scraped_elements.element_hash
    command_text TEXT,
    action_type TEXT,
    confidence REAL,
    synonyms TEXT,             -- JSON array
    usage_count INTEGER,
    ...
)
```

### Command Processing Flow

```
1. User speaks: "click submit button"
         ↓
2. VoiceCommandProcessor.processCommand()
         ↓
3. Match command in database (exact → fuzzy → synonym)
         ↓
4. Retrieve element by hash from scraped_elements
         ↓
5. Find node in current UI tree by hash
         ↓
6. Execute AccessibilityNodeInfo.ACTION_CLICK
         ↓
7. Increment usage_count in database
```

## Quick Start Guide

### For Integration Developers

```kotlin
// 1. Initialize in VoiceAccessibilityService
class MyService : AccessibilityService() {
    private lateinit var scraping: AccessibilityScrapingIntegration

    override fun onServiceConnected() {
        scraping = AccessibilityScrapingIntegration(this, this)
    }

    // 2. Handle accessibility events
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        scraping.onAccessibilityEvent(event)
    }

    // 3. Process voice commands
    private fun onVoiceInput(text: String) {
        lifecycleScope.launch {
            val result = scraping.processVoiceCommand(text)
            speakFeedback(result.message)
        }
    }

    // 4. Optional: Trigger LearnApp mode
    private fun learnApp(packageName: String) {
        lifecycleScope.launch {
            val result = scraping.learnApp(packageName)
            showDialog(result.message)
        }
    }
}
```

### For Database Developers

```kotlin
// Query examples
val database = AppScrapingDatabase.getInstance(context)

// 1. Check if app is scraped
val appHash = AppHashCalculator.calculateAppHash(packageName, versionCode)
val app = database.scrapedAppDao().getAppByHash(appHash)

// 2. Get element by hash (O(1) lookup)
val element = database.scrapedElementDao().getElementByHash(hash)

// 3. Find commands for current app
val commands = database.generatedCommandDao().getCommandsForApp(appId)

// 4. Get hierarchy relationships
val children = database.scrapedHierarchyDao().getChildren(parentId)
```

## Data Flow Diagrams

### Scraping Flow (DYNAMIC Mode)

```
AccessibilityEvent (TYPE_WINDOW_STATE_CHANGED)
    ↓
Calculate app hash
    ↓
Check if already scraped ──→ Yes ──→ Increment scrape_count → Exit
    ↓ No
Scrape accessibility tree
    ↓
Insert app entity
    ↓
Insert elements (batch) → Capture database IDs
    ↓
Map indices to IDs → Insert hierarchy (batch)
    ↓
Generate commands (batch)
    ↓
Insert commands (batch)
    ↓
Update statistics
```

### LearnApp Flow

```
User triggers LearnApp(packageName)
    ↓
Get or create app entity
    ↓
Set scraping_mode = LEARN_APP
    ↓
Scrape all visible elements
    ↓
For each element:
    ↓
    Check if hash exists in DB
    ↓
    If exists → Update (preserve DB ID)
    ↓
    If new → Insert
    ↓
Mark app as fully_learned = true
    ↓
Generate commands for new elements only
    ↓
Set scraping_mode = DYNAMIC
    ↓
Return statistics (new count, updated count)
```

### Command Processing Flow

```
Voice input: "click submit"
    ↓
Normalize: "click submit"
    ↓
Get current app package
    ↓
Calculate app hash
    ↓
Query commands for app
    ↓
Try exact match → Found? → Use it
    ↓ Not found
Try synonym match → Found? → Use it
    ↓ Not found
Try fuzzy match (contains) → Found? → Use it
    ↓ Not found
Return "Command not recognized"
    ↓ Command found
Get element by hash
    ↓
Find node in current UI tree
    ↓
Execute action (click/type/scroll/etc)
    ↓
Increment usage_count
    ↓
Return success
```

## Testing Strategy

### Unit Tests Needed

1. **CommandGenerator**
   - Confidence calculation edge cases
   - Synonym generation accuracy
   - Action type determination

2. **VoiceCommandProcessor**
   - Fuzzy matching accuracy
   - Synonym matching
   - Hash-based element lookup

3. **Database Migrations**
   - V1→V2: Element hash migration
   - V2→V3: LearnApp mode additions

4. **Hash Collision Handling**
   - Multiple identical elements
   - Cross-version hash conflicts

### Integration Tests Needed

1. **End-to-End Scraping**
   - Scrape sample app
   - Verify element count
   - Verify hierarchy relationships
   - Verify command generation

2. **LearnApp Mode**
   - Dynamic + LearnApp merge
   - UPSERT behavior
   - Duplicate prevention

3. **Command Execution**
   - Voice input → UI action
   - Success rate tracking
   - Error handling

## Migration Guide

### From AppHashCalculator to AccessibilityFingerprint

**Old Code:**
```kotlin
val hash = AppHashCalculator.calculateElementHash(node)
```

**New Code:**
```kotlin
val fingerprint = AccessibilityFingerprint.fromNode(
    node = node,
    packageName = packageName,
    appVersion = appVersion,
    calculateHierarchyPath = { calculateNodePath(it) }
)
val hash = fingerprint.generateHash()
val stabilityScore = fingerprint.calculateStabilityScore()
```

**Benefits:**
- SHA-256 instead of MD5 (more secure)
- Hierarchy path prevents collisions
- Version scoping prevents cross-version conflicts
- Stability score indicates reliability

## Performance Considerations

### Scraping Performance

- **Element Count**: Typical app has 50-200 elements
- **Scraping Time**: ~100-500ms for average app
- **Database Insert**: ~50-200ms for batch operations
- **Command Generation**: ~10-50ms per app

**Optimization:**
- Use batch inserts for elements, hierarchy, commands
- Index element_hash for O(1) lookups
- Limit tree depth to 50 levels
- Skip system packages (launcher, systemui)

### Database Size

- **Per App**: ~10-50 KB
- **Per Element**: ~200-500 bytes
- **Per Command**: ~100-200 bytes
- **100 Apps**: ~1-5 MB total

**Cleanup:**
- Automatic: Delete apps not scraped in 7 days
- Manual: Delete low-confidence, unused commands

## Troubleshooting

### Common Issues

1. **"App not yet learned"**
   - Cause: App hasn't been scraped
   - Solution: Navigate to app, wait for auto-scrape, or trigger LearnApp

2. **"Command not recognized"**
   - Cause: No matching command in database
   - Solution: Check synonyms, try simpler phrasing, re-scrape app

3. **"Element not found"**
   - Cause: UI changed after scraping, hash mismatch
   - Solution: Re-scrape app to update elements

4. **Duplicate elements in database**
   - Cause: Hash collision (rare with AccessibilityFingerprint)
   - Solution: Check hierarchy paths, update to latest fingerprinting

5. **Hierarchy foreign key violations**
   - Cause: Incorrect ID mapping during insertion
   - Solution: Verify 3-phase insertion process, check ID capture

## Related Documentation

- **VoiceAccessibility Architecture**: `/docs/modules/voice-accessibility/architecture/`
- **Database Schema**: `/docs/modules/voice-accessibility/reference/database-schema.md`
- **UUIDCreator Library**: `/docs/modules/UUIDCreator/`
- **Accessibility Service Guide**: `/docs/modules/voice-accessibility/developer-manual/accessibility-service-integration.md`

## File Statistics

**Total Files:** 17
**Lines of Code:** ~3,500
**Documentation Coverage:** 100%

**File Breakdown:**
- Core Integration: 3 files (~1,200 LOC)
- Command Processing: 2 files (~700 LOC)
- Hashing: 2 files (~400 LOC) - DEPRECATED
- Database: 9 files (~1,200 LOC)
- Enums/Data Classes: 1 file (~60 LOC)

---

**Index Generated:** 2025-10-10 10:34:00 PDT
**Maintained By:** VOS4 Development Team
**Next Review:** TBD
