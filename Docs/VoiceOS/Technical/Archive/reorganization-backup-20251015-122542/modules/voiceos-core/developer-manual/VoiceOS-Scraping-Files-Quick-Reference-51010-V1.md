# Scraping Subsystem - Quick Reference Guide

**Last Updated:** 2025-10-10 10:34:00 PDT
**Purpose:** Quick reference for all scraping subsystem files

---

## VoiceCommandProcessor.kt

**File:** `src/main/java/com/augmentalis/voiceaccessibility/scraping/VoiceCommandProcessor.kt`
**Purpose:** Process voice commands and execute UI actions

### Key Methods

#### `processCommand(voiceInput: String): CommandResult`
- Normalizes input, matches to database commands
- Uses fuzzy matching + synonym matching
- Executes action on target element
- Updates usage statistics

**Matching Strategy:**
1. Exact match on `command_text`
2. Exact match on synonyms (JSON array)
3. Fuzzy match (contains)  on `command_text`
4. Fuzzy match on synonyms

#### `executeTextInput(voiceInput: String, text: String): CommandResult`
- Two-step text input process
- First call: Identify field
- Second call: Input text using `ACTION_SET_TEXT`

#### Private: `findNodeByHash(node, targetHash): AccessibilityNodeInfo?`
- Recursively searches UI tree for element with matching hash
- Returns node for action execution

### Action Execution

**Supported Actions:**
- `click` → `ACTION_CLICK`
- `long_click` → `ACTION_LONG_CLICK`
- `focus` → `ACTION_FOCUS`
- `scroll` → `ACTION_SCROLL_FORWARD`
- `type` → `ACTION_FOCUS` then `ACTION_SET_TEXT`

### Threading
- Main: Action execution (UI operations)
- IO: Database queries, command matching

---

## AppHashCalculator.kt

**File:** `src/main/java/com/augmentalis/voiceaccessibility/scraping/AppHashCalculator.kt`
**Status:** DEPRECATED - Use AccessibilityFingerprint instead
**Purpose:** MD5-based app and element fingerprinting

### Methods

#### `calculateAppHash(packageName: String, versionCode: Int): String`
- Returns: MD5 hash of `"$packageName:$versionCode"`
- Purpose: Detect app version changes

#### `calculateElementHash(node: AccessibilityNodeInfo): String`
- Returns: MD5 hash of className + viewId + text + contentDesc
- Problem: No hierarchy awareness → potential collisions

### Deprecation
- **Replacement:** `AccessibilityFingerprint` from UUIDCreator
- **Removal:** Planned for v3.0.0
- **Migration:** Update all calls to use AccessibilityFingerprint

---

## ElementHasher.kt

**File:** `src/main/java/com/augmentalis/voiceaccessibility/scraping/ElementHasher.kt`
**Status:** DEPRECATED - Use AccessibilityFingerprint
**Purpose:** MD5 element hashing (legacy)

### Methods

#### `calculateHash(node: AccessibilityNodeInfo): String`
- MD5 of className|viewId|text|contentDesc
- No hierarchy path

#### `calculateHashWithPosition(node, includePosition: Boolean): String`
- Optional: Include screen bounds in hash
- More specific but less stable across screen rotations

#### `calculateSimilarity(hash1, hash2): Float`
- Hamming distance similarity score
- Returns: 0.0 (different) to 1.0 (identical)

### Why Deprecated
- Lacks hierarchy awareness
- MD5 less secure than SHA-256
- No version scoping
- Replaced by AccessibilityFingerprint

---

## AppScrapingDatabase.kt

**File:** `src/main/java/com/augmentalis/voiceaccessibility/scraping/database/AppScrapingDatabase.kt`
**Purpose:** Room database setup and migrations

### Configuration
- **Database Name:** `app_scraping_database`
- **Version:** 3
- **Entities:** ScrapedAppEntity, ScrapedElementEntity, ScrapedHierarchyEntity, GeneratedCommandEntity

### DAOs
- `scrapedAppDao()` - App operations
- `scrapedElementDao()` - Element operations
- `scrapedHierarchyDao()` - Hierarchy operations
- `generatedCommandDao()` - Command operations

### Auto-Cleanup
- **Callback:** `DatabaseCallback.onOpen()`
- **Trigger:** On database open
- **Actions:**
  - Delete apps not scraped in 7 days
  - Delete low-quality commands (unused, confidence < 0.3)

### Migrations
- **V1→V2:** Element hash migration, commands FK change
- **V2→V3:** LearnApp mode columns

---

## Entity Classes

### ScrapedAppEntity.kt
**Stores:** App metadata
**Key Fields:**
- `appId`: UUID primary key
- `appHash`: MD5 fingerprint
- `isFullyLearned`: LearnApp completion flag
- `scrapingMode`: DYNAMIC or LEARN_APP

### ScrapedElementEntity.kt
**Stores:** UI element data
**Key Fields:**
- `id`: Auto-generated primary key
- `elementHash`: SHA-256 unique identifier
- `appId`: Foreign key to ScrapedAppEntity
- Action capabilities: isClickable, isEditable, etc.

### ScrapedHierarchyEntity.kt
**Stores:** Parent-child relationships
**Key Fields:**
- `parentElementId`: FK to ScrapedElementEntity.id (Long)
- `childElementId`: FK to ScrapedElementEntity.id (Long)
- `childOrder`: Sibling order preservation

**Design:** Uses Long IDs for performance, not hashes

### GeneratedCommandEntity.kt
**Stores:** Voice commands
**Key Fields:**
- `elementHash`: FK to ScrapedElementEntity.elementHash (String)
- `commandText`: Primary command phrase
- `actionType`: click, type, scroll, etc.
- `confidence`: 0.0-1.0 quality score
- `synonyms`: JSON array of alternatives
- `usageCount`: Execution tracking

---

## DAO Classes

### ScrapedAppDao.kt
**Key Queries:**
- `getAppByHash(appHash)` - Version detection
- `incrementScrapeCount(appId)` - Update statistics
- `deleteAppsOlderThan(timestamp)` - Cleanup
- `markAsFullyLearned(appId)` - LearnApp completion
- `updateScrapingMode(appId, mode)` - Mode switching

### ScrapedElementDao.kt
**Key Queries:**
- `getElementByHash(hash)` - O(1) lookup
- `insertBatchWithIds(elements)` - Capture auto-generated IDs
- `upsertElement(element)` - Hash-based merge
- `getElementsByAppId(appId)` - Get all elements
- Filter queries: getClickableElements, getEditableElements, etc.

**UPSERT Logic:**
```kotlin
val existing = getElementByHash(element.elementHash)
if (existing != null) {
    update(element.copy(id = existing.id))  // Preserve ID
} else {
    insert(element)
}
```

### ScrapedHierarchyDao.kt
**Key Queries:**
- `getChildren(parentId)` - Tree traversal
- `getParent(childId)` - Reverse lookup
- `getSiblings(elementId)` - Same-parent elements
- `getRootElements(appId)` - Top-level nodes
- `getLeafElements(appId)` - Bottom-level nodes

### GeneratedCommandDao.kt
**Key Queries:**
- `getCommandsForApp(appId)` - All commands for app
- `getCommandByText(commandText)` - Exact match
- `searchCommandsByText(searchText)` - Fuzzy match
- `incrementUsage(commandId)` - Track execution
- `deleteLowQualityCommands(threshold)` - Cleanup

---

## Supporting Files

### ScrapingMode.kt
**Enum:** DYNAMIC, LEARN_APP

**DYNAMIC:**
- Triggered: Automatically on window changes
- Coverage: Partial (only visited screens)
- Performance: Fast, lightweight

**LEARN_APP:**
- Triggered: User-initiated
- Coverage: Comprehensive (all screens)
- Performance: Slower, more thorough
- Merges: With existing dynamic data

### ScrapingCoordinator.kt
**Status:** LEGACY - Being phased out
**Purpose:** Coordinate scraping workflow
**Replacement:** AccessibilityScrapingIntegration

**Key Methods:**
- `coordinateScraping(rootNode)` - Full scraping workflow
- `forceRescrape(rootNode)` - Delete and re-scrape
- `isAppScraped(packageName, versionCode)` - Check existence

**Result Types:**
- `CoordinatorResult.AlreadyScraped` - App exists
- `CoordinatorResult.NewScrape` - Fresh scrape
- `CoordinatorResult.Excluded` - System package
- `CoordinatorResult.Error` - Failure

### AccessibilityTreeScraper.kt
**Status:** LEGACY - Functionality moved to AccessibilityScrapingIntegration
**Purpose:** Tree traversal and element extraction

**Key Methods:**
- `scrapeTree(rootNode, appId)` - Full tree scrape
- `scrapeTreeFiltered(rootNode, appId)` - Actionable elements only
- `scrapeNodeRecursive()` - Depth-first traversal

**Output:** `ScrapingResult(elements, hierarchy)`

---

## Common Patterns

### 1. Check if App is Scraped
```kotlin
val appHash = AppHashCalculator.calculateAppHash(packageName, versionCode)
val app = database.scrapedAppDao().getAppByHash(appHash)
if (app != null) {
    // Already scraped
}
```

### 2. Generate and Store Commands
```kotlin
val elements = database.scrapedElementDao().getElementsByAppId(appId)
val commands = commandGenerator.generateCommandsForElements(elements)
database.generatedCommandDao().insertBatch(commands)
```

### 3. Process Voice Command
```kotlin
val result = voiceCommandProcessor.processCommand(userSpeech)
if (result.success) {
    speakFeedback(result.message)
} else {
    speakError(result.message)
}
```

### 4. Execute Text Input
```kotlin
// Step 1: Identify field
val result1 = voiceCommandProcessor.processCommand("type in email")
// Step 2: Input text
val result2 = voiceCommandProcessor.executeTextInput("type in email", "user@example.com")
```

### 5. LearnApp Mode
```kotlin
val result = accessibilityScrapingIntegration.learnApp(packageName)
Log.i(TAG, "Discovered: ${result.elementsDiscovered}")
Log.i(TAG, "New: ${result.newElements}, Updated: ${result.updatedElements}")
```

---

## File Dependencies

```
VoiceAccessibilityService
         ↓
AccessibilityScrapingIntegration
    ↓           ↓            ↓
Database  CommandGen   VoiceProcessor
    ↓                        ↓
  DAOs                  findNodeByHash()
    ↓                        ↓
Entities              executeAction()
```

### Import Graph
- **Core:** AccessibilityScrapingIntegration imports all
- **Database:** All DAOs import their Entity
- **Legacy:** ScrapingCoordinator, AccessibilityTreeScraper
- **Deprecated:** AppHashCalculator, ElementHasher

---

## Testing Checklist

### Unit Tests
- [ ] CommandGenerator confidence calculation
- [ ] VoiceCommandProcessor fuzzy matching
- [ ] DAO UPSERT logic
- [ ] Hash collision handling

### Integration Tests
- [ ] End-to-end scraping flow
- [ ] LearnApp merge behavior
- [ ] Command execution accuracy
- [ ] Database migrations

### Edge Cases
- [ ] Null root node handling
- [ ] Deep UI tree (>50 levels)
- [ ] Hash collision resolution
- [ ] Stale element detection
- [ ] Cross-version hash conflicts

---

## Performance Metrics

**Typical App Scraping:**
- Elements: 50-200
- Scraping time: 100-500ms
- Database insert: 50-200ms
- Command generation: 10-50ms

**Database Size:**
- Per app: 10-50 KB
- Per element: 200-500 bytes
- Per command: 100-200 bytes

**Query Performance:**
- Element lookup (hash): O(1)
- Commands for app: O(n)
- Hierarchy traversal: O(depth)

---

## Migration Checklist

### To AccessibilityFingerprint
- [ ] Replace AppHashCalculator.calculateElementHash()
- [ ] Replace ElementHasher.calculateHash()
- [ ] Update all hash generation to include hierarchy path
- [ ] Test hash stability across scraping sessions
- [ ] Remove deprecated classes (v3.0.0)

### Database Schema Updates
- [ ] Test migration V1→V2 (hash-based FKs)
- [ ] Test migration V2→V3 (LearnApp columns)
- [ ] Verify cascade deletions work correctly
- [ ] Validate UPSERT behavior

---

**Quick Reference Generated:** 2025-10-10 10:34:00 PDT
**Total Files Documented:** 17
**Comprehensive Docs:** 4 files (AccessibilityScrapingIntegration, CommandGenerator, Index, Database Schema)
**Quick Reference:** 13 files (this document)
