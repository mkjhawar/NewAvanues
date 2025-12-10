# Phase 3: User Interaction Tracking - Implementation Plan

**Date:** 2025-10-18 23:25 PDT
**Author:** Manoj Jhawar
**Status:** ✅ Database Layer Complete (see Phase3-Implementation-Complete-251018-2333.md)
**Dependencies:** ✅ Phase 2 & 2.5 Complete

---

## Executive Summary

Implement user interaction tracking to capture:
- Click/interaction events on UI elements
- Visibility duration (how long elements are shown)
- Element state changes (checked→unchecked, expanded→collapsed)

**NOT Including (per user request):**
- ❌ Usage analytics
- ❌ Usage pattern detection
- ❌ Personalization metrics
- ❌ Frequency analysis

**Purpose:** Track interaction data to support:
- Multi-step navigation confidence scoring
- Element importance weighting
- Interaction history for voice commands
- State-aware command execution

---

## Scope Changes from Original Phase 3

**Original Phase 3 (21-30 hours):**
- User interaction tracking ✅ KEEP
- Click counts ✅ KEEP
- Visibility duration ✅ KEEP
- State transition tracking ✅ KEEP
- **Usage analytics** ❌ REMOVE
- **Personalization features** ❌ REMOVE
- **Pattern detection** ❌ REMOVE
- **Frequency-based suggestions** ❌ REMOVE

**Modified Phase 3 (15-20 hours):**
- Focus on raw data collection only
- No analysis or pattern detection
- Data stored for future use (if needed)
- Simpler implementation

---

## Database Schema Changes

### Migration v7→v8

**New Tables:** 2

#### 1. `user_interactions` Table

```sql
CREATE TABLE user_interactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    element_hash TEXT NOT NULL,
    screen_hash TEXT NOT NULL,
    interaction_type TEXT NOT NULL,     -- 'click', 'long_press', 'swipe', 'focus'
    interaction_time INTEGER NOT NULL,  -- Timestamp of interaction
    visibility_start INTEGER,           -- When element became visible
    visibility_duration INTEGER,        -- How long visible before interaction (ms)
    success INTEGER NOT NULL DEFAULT 1, -- Did interaction succeed? (1=yes, 0=no)
    created_at INTEGER NOT NULL,

    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE,
    FOREIGN KEY(screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE
)
```

**Indices:**
```sql
CREATE INDEX index_user_interactions_element_hash ON user_interactions(element_hash)
CREATE INDEX index_user_interactions_screen_hash ON user_interactions(screen_hash)
CREATE INDEX index_user_interactions_interaction_type ON user_interactions(interaction_type)
CREATE INDEX index_user_interactions_interaction_time ON user_interactions(interaction_time)
```

**Purpose:**
- Record every user interaction with UI elements
- Track when interactions occurred
- Measure time between visibility and interaction (user decision time)
- Track success/failure of interactions

#### 2. `element_state_history` Table

```sql
CREATE TABLE element_state_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    element_hash TEXT NOT NULL,
    screen_hash TEXT NOT NULL,
    state_type TEXT NOT NULL,           -- 'checked', 'selected', 'enabled', 'visible', 'focused'
    old_value TEXT,                     -- Previous state value
    new_value TEXT,                     -- New state value
    changed_at INTEGER NOT NULL,        -- Timestamp of change
    triggered_by TEXT,                  -- 'user_click', 'system', 'app_event'

    FOREIGN KEY(element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE,
    FOREIGN KEY(screen_hash) REFERENCES screen_contexts(screen_hash) ON DELETE CASCADE
)
```

**Indices:**
```sql
CREATE INDEX index_element_state_history_element_hash ON element_state_history(element_hash)
CREATE INDEX index_element_state_history_screen_hash ON element_state_history(screen_hash)
CREATE INDEX index_element_state_history_state_type ON element_state_history(state_type)
CREATE INDEX index_element_state_history_changed_at ON element_state_history(changed_at)
```

**Purpose:**
- Track element state changes over time
- Record what triggered the change (user vs system)
- Enable state-aware voice commands ("uncheck the box" vs "check the box")

---

## Entity Definitions

### 1. UserInteractionEntity

**File:** `UserInteractionEntity.kt`
**Package:** `com.augmentalis.voiceoscore.scraping.entities`

```kotlin
/**
 * User Interaction Entity
 *
 * Records user interactions with UI elements including clicks, long presses,
 * swipes, and focus events. Tracks visibility duration to measure user
 * decision time.
 */
@Entity(
    tableName = "user_interactions",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["element_hash"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenContextEntity::class,
            parentColumns = ["screen_hash"],
            childColumns = ["screen_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("element_hash"),
        Index("screen_hash"),
        Index("interaction_type"),
        Index("interaction_time")
    ]
)
data class UserInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "element_hash")
    val elementHash: String,

    @ColumnInfo(name = "screen_hash")
    val screenHash: String,

    @ColumnInfo(name = "interaction_type")
    val interactionType: String,  // "click", "long_press", "swipe", "focus"

    @ColumnInfo(name = "interaction_time")
    val interactionTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "visibility_start")
    val visibilityStart: Long? = null,

    @ColumnInfo(name = "visibility_duration")
    val visibilityDuration: Long? = null,  // milliseconds

    @ColumnInfo(name = "success")
    val success: Boolean = true,  // Did interaction succeed?

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

object InteractionType {
    const val CLICK = "click"
    const val LONG_PRESS = "long_press"
    const val SWIPE = "swipe"
    const val FOCUS = "focus"
    const val SCROLL = "scroll"
}
```

### 2. ElementStateHistoryEntity

**File:** `ElementStateHistoryEntity.kt`
**Package:** `com.augmentalis.voiceoscore.scraping.entities`

```kotlin
/**
 * Element State History Entity
 *
 * Tracks changes to element states over time including checked status,
 * selection, enabled/disabled, visibility, and focus. Records what
 * triggered the change for context.
 */
@Entity(
    tableName = "element_state_history",
    foreignKeys = [
        ForeignKey(
            entity = ScrapedElementEntity::class,
            parentColumns = ["element_hash"],
            childColumns = ["element_hash"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenContextEntity::class,
            parentColumns = ["screen_hash"],
            childColumns = ["screen_hash"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("element_hash"),
        Index("screen_hash"),
        Index("state_type"),
        Index("changed_at")
    ]
)
data class ElementStateHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "element_hash")
    val elementHash: String,

    @ColumnInfo(name = "screen_hash")
    val screenHash: String,

    @ColumnInfo(name = "state_type")
    val stateType: String,  // "checked", "selected", "enabled", "visible", "focused"

    @ColumnInfo(name = "old_value")
    val oldValue: String? = null,

    @ColumnInfo(name = "new_value")
    val newValue: String? = null,

    @ColumnInfo(name = "changed_at")
    val changedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "triggered_by")
    val triggeredBy: String? = null  // "user_click", "system", "app_event"
)

object StateType {
    const val CHECKED = "checked"
    const val SELECTED = "selected"
    const val ENABLED = "enabled"
    const val VISIBLE = "visible"
    const val FOCUSED = "focused"
    const val EXPANDED = "expanded"
}

object TriggerSource {
    const val USER_CLICK = "user_click"
    const val USER_VOICE = "user_voice"
    const val SYSTEM = "system"
    const val APP_EVENT = "app_event"
    const val UNKNOWN = "unknown"
}
```

---

## DAO Definitions

### 1. UserInteractionDao

**File:** `UserInteractionDao.kt`
**Package:** `com.augmentalis.voiceoscore.scraping.dao`

```kotlin
@Dao
interface UserInteractionDao {
    /**
     * Record a user interaction
     */
    @Insert
    suspend fun insert(interaction: UserInteractionEntity): Long

    /**
     * Record multiple interactions in batch
     */
    @Insert
    suspend fun insertAll(interactions: List<UserInteractionEntity>)

    /**
     * Get all interactions for an element
     */
    @Query("SELECT * FROM user_interactions WHERE element_hash = :elementHash ORDER BY interaction_time DESC")
    suspend fun getInteractionsForElement(elementHash: String): List<UserInteractionEntity>

    /**
     * Get all interactions for a screen
     */
    @Query("SELECT * FROM user_interactions WHERE screen_hash = :screenHash ORDER BY interaction_time DESC")
    suspend fun getInteractionsForScreen(screenHash: String): List<UserInteractionEntity>

    /**
     * Get interactions by type
     */
    @Query("SELECT * FROM user_interactions WHERE interaction_type = :type ORDER BY interaction_time DESC LIMIT :limit")
    suspend fun getInteractionsByType(type: String, limit: Int = 100): List<UserInteractionEntity>

    /**
     * Get recent interactions
     */
    @Query("SELECT * FROM user_interactions ORDER BY interaction_time DESC LIMIT :limit")
    suspend fun getRecentInteractions(limit: Int = 100): List<UserInteractionEntity>

    /**
     * Get interactions in time range
     */
    @Query("SELECT * FROM user_interactions WHERE interaction_time BETWEEN :startTime AND :endTime ORDER BY interaction_time DESC")
    suspend fun getInteractionsInTimeRange(startTime: Long, endTime: Long): List<UserInteractionEntity>

    /**
     * Count interactions for element
     */
    @Query("SELECT COUNT(*) FROM user_interactions WHERE element_hash = :elementHash")
    suspend fun getInteractionCount(elementHash: String): Int

    /**
     * Delete old interactions (cleanup)
     */
    @Query("DELETE FROM user_interactions WHERE interaction_time < :cutoffTime")
    suspend fun deleteOldInteractions(cutoffTime: Long): Int

    /**
     * Get last interaction for element
     */
    @Query("SELECT * FROM user_interactions WHERE element_hash = :elementHash ORDER BY interaction_time DESC LIMIT 1")
    suspend fun getLastInteraction(elementHash: String): UserInteractionEntity?
}
```

### 2. ElementStateHistoryDao

**File:** `ElementStateHistoryDao.kt`
**Package:** `com.augmentalis.voiceoscore.scraping.dao`

```kotlin
@Dao
interface ElementStateHistoryDao {
    /**
     * Record a state change
     */
    @Insert
    suspend fun insert(stateChange: ElementStateHistoryEntity): Long

    /**
     * Record multiple state changes in batch
     */
    @Insert
    suspend fun insertAll(stateChanges: List<ElementStateHistoryEntity>)

    /**
     * Get state history for an element
     */
    @Query("SELECT * FROM element_state_history WHERE element_hash = :elementHash ORDER BY changed_at DESC")
    suspend fun getStateHistory(elementHash: String): List<ElementStateHistoryEntity>

    /**
     * Get state history for specific state type
     */
    @Query("SELECT * FROM element_state_history WHERE element_hash = :elementHash AND state_type = :stateType ORDER BY changed_at DESC")
    suspend fun getStateHistoryByType(elementHash: String, stateType: String): List<ElementStateHistoryEntity>

    /**
     * Get current state value for element
     */
    @Query("SELECT * FROM element_state_history WHERE element_hash = :elementHash AND state_type = :stateType ORDER BY changed_at DESC LIMIT 1")
    suspend fun getCurrentState(elementHash: String, stateType: String): ElementStateHistoryEntity?

    /**
     * Get all state changes for a screen
     */
    @Query("SELECT * FROM element_state_history WHERE screen_hash = :screenHash ORDER BY changed_at DESC")
    suspend fun getStateHistoryForScreen(screenHash: String): List<ElementStateHistoryEntity>

    /**
     * Get state changes in time range
     */
    @Query("SELECT * FROM element_state_history WHERE changed_at BETWEEN :startTime AND :endTime ORDER BY changed_at DESC")
    suspend fun getStateChangesInTimeRange(startTime: Long, endTime: Long): List<ElementStateHistoryEntity>

    /**
     * Delete old state history (cleanup)
     */
    @Query("DELETE FROM element_state_history WHERE changed_at < :cutoffTime")
    suspend fun deleteOldStateHistory(cutoffTime: Long): Int

    /**
     * Get state changes by trigger source
     */
    @Query("SELECT * FROM element_state_history WHERE triggered_by = :triggerSource ORDER BY changed_at DESC LIMIT :limit")
    suspend fun getStateChangesByTrigger(triggerSource: String, limit: Int = 100): List<ElementStateHistoryEntity>
}
```

---

## Implementation Details

### Visibility Tracking

**Challenge:** How to track when elements become visible?

**Solution:** Track in AccessibilityScrapingIntegration

```kotlin
// In AccessibilityScrapingIntegration.kt

private val elementVisibilityMap = ConcurrentHashMap<String, Long>()  // elementHash → visibilityStartTime

private fun trackElementVisibility(element: ScrapedElementEntity) {
    if (element.isVisibleToUser) {
        // Element is visible - record start time if not already tracking
        elementVisibilityMap.putIfAbsent(element.elementHash, System.currentTimeMillis())
    } else {
        // Element is no longer visible - remove from tracking
        elementVisibilityMap.remove(element.elementHash)
    }
}

private fun getVisibilityDuration(elementHash: String): Long? {
    val startTime = elementVisibilityMap[elementHash] ?: return null
    return System.currentTimeMillis() - startTime
}
```

### Interaction Recording

**When to record interactions?**

1. **Click Events** - On `TYPE_VIEW_CLICKED` accessibility event
2. **Long Press** - On `TYPE_VIEW_LONG_CLICKED` event
3. **Focus** - On `TYPE_VIEW_FOCUSED` event
4. **Scroll** - On `TYPE_VIEW_SCROLLED` event

**Implementation:**

```kotlin
// In AccessibilityScrapingIntegration.kt or VoiceOSService

suspend fun recordInteraction(
    elementHash: String,
    screenHash: String,
    interactionType: String,
    success: Boolean = true
) {
    val visibilityStart = elementVisibilityMap[elementHash]
    val visibilityDuration = if (visibilityStart != null) {
        System.currentTimeMillis() - visibilityStart
    } else null

    val interaction = UserInteractionEntity(
        elementHash = elementHash,
        screenHash = screenHash,
        interactionType = interactionType,
        visibilityStart = visibilityStart,
        visibilityDuration = visibilityDuration,
        success = success
    )

    database.userInteractionDao().insert(interaction)

    Log.d(TAG, "Recorded $interactionType on element ${elementHash.take(8)} " +
               "(visible for ${visibilityDuration}ms)")
}
```

### State Change Tracking

**When to track state changes?**

1. **On element scraping** - Compare with previous state
2. **Store previous states in memory** - Use cache for comparison

**Implementation:**

```kotlin
// In AccessibilityScrapingIntegration.kt

private val elementStateCache = ConcurrentHashMap<String, ElementState>()

data class ElementState(
    val isChecked: Boolean,
    val isSelected: Boolean,
    val isEnabled: Boolean,
    val isFocused: Boolean
)

suspend fun trackStateChanges(element: ScrapedElementEntity, node: AccessibilityNodeInfo) {
    val currentState = ElementState(
        isChecked = node.isChecked,
        isSelected = node.isSelected,
        isEnabled = node.isEnabled,
        isFocused = node.isFocused
    )

    val previousState = elementStateCache.put(element.elementHash, currentState)

    if (previousState != null) {
        // Check for changes and record them
        if (previousState.isChecked != currentState.isChecked) {
            recordStateChange(
                element.elementHash,
                element.screenHash,
                StateType.CHECKED,
                previousState.isChecked.toString(),
                currentState.isChecked.toString()
            )
        }

        if (previousState.isSelected != currentState.isSelected) {
            recordStateChange(
                element.elementHash,
                element.screenHash,
                StateType.SELECTED,
                previousState.isSelected.toString(),
                currentState.isSelected.toString()
            )
        }

        // ... similar for other states
    }
}

private suspend fun recordStateChange(
    elementHash: String,
    screenHash: String,
    stateType: String,
    oldValue: String,
    newValue: String
) {
    val stateChange = ElementStateHistoryEntity(
        elementHash = elementHash,
        screenHash = screenHash,
        stateType = stateType,
        oldValue = oldValue,
        newValue = newValue,
        triggeredBy = TriggerSource.SYSTEM  // Default - can be overridden
    )

    database.elementStateHistoryDao().insert(stateChange)
}
```

---

## Integration Points

### 1. AccessibilityScrapingIntegration.kt

**Changes needed:**
- Add visibility tracking map
- Add state cache map
- Call `trackElementVisibility()` during scraping
- Call `trackStateChanges()` during scraping
- Track when elements leave screen (stop visibility timer)

### 2. VoiceOSService.kt

**Changes needed:**
- Intercept accessibility events for interactions
- Record interactions on `TYPE_VIEW_CLICKED`, `TYPE_VIEW_LONG_CLICKED`, etc.
- Set trigger source to `USER_CLICK` for user-initiated changes

### 3. AppScrapingDatabase.kt

**Changes needed:**
- Add `UserInteractionEntity` to entities list
- Add `ElementStateHistoryEntity` to entities list
- Add DAOs: `userInteractionDao()`, `elementStateHistoryDao()`
- Create migration v7→v8
- Update version to 8

---

## Data Retention & Cleanup

**Policy:** Keep interaction data for 30 days

```kotlin
// In AppScrapingDatabase cleanup routine

private suspend fun cleanupInteractionData(database: AppScrapingDatabase) {
    val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)

    val deletedInteractions = database.userInteractionDao()
        .deleteOldInteractions(cutoffTime)

    val deletedStates = database.elementStateHistoryDao()
        .deleteOldStateHistory(cutoffTime)

    Log.d(TAG, "Cleaned up $deletedInteractions interactions, $deletedStates state changes")
}
```

---

## Testing Strategy

### Unit Tests

1. **UserInteractionDao Tests**
   - Insert and retrieve interactions
   - Query by element/screen/type
   - Time range queries
   - Cleanup operations

2. **ElementStateHistoryDao Tests**
   - Insert and retrieve state changes
   - Query current state
   - State history queries
   - Cleanup operations

### Integration Tests

1. **Visibility Tracking**
   - Element becomes visible → start tracking
   - Element hidden → stop tracking
   - Calculate duration correctly

2. **Interaction Recording**
   - Click event → interaction recorded
   - Visibility duration calculated
   - Success/failure tracking

3. **State Change Detection**
   - Checkbox toggled → state change recorded
   - Multiple state changes tracked
   - Trigger source correctly identified

---

## Performance Considerations

### Database Impact

**Insert Operations:**
- ~2-5 interactions per screen view
- ~1-3 state changes per interaction
- Estimated: 10-20 DB writes per user action

**Mitigation:**
- Batch inserts where possible
- Use background coroutine (Dispatchers.IO)
- Consider write-ahead logging (WAL mode)

### Memory Impact

**Visibility Tracking Map:**
- ~50-100 elements tracked simultaneously
- Memory: ~5-10 KB

**State Cache Map:**
- ~50-100 elements cached
- Memory: ~10-20 KB

**Total Impact:** < 50 KB (negligible)

### Storage Impact

**Per Interaction:** ~150 bytes
**Per State Change:** ~100 bytes

**Daily Estimate:**
- 100 interactions/day × 150 bytes = 15 KB
- 50 state changes/day × 100 bytes = 5 KB
- **Total: ~20 KB/day**

**30-day retention:** ~600 KB (acceptable)

---

## Use Cases Enabled

### 1. Smart Command Confidence

```kotlin
// When user says "click the submit button"

val interactions = userInteractionDao.getInteractionsForElement(submitButtonHash)

val confidence = when {
    interactions.size > 10 -> 0.95f  // Frequently used
    interactions.size > 5 -> 0.85f   // Sometimes used
    interactions.size > 0 -> 0.75f   // Rarely used
    else -> 0.5f                      // Never used (uncertain)
}
```

### 2. State-Aware Commands

```kotlin
// When user says "toggle the checkbox"

val currentState = elementStateHistoryDao.getCurrentState(checkboxHash, StateType.CHECKED)

val action = when (currentState?.newValue) {
    "true" -> "uncheck"  // Currently checked → uncheck it
    "false" -> "check"   // Currently unchecked → check it
    else -> "check"      // Unknown → default to check
}
```

### 3. Visibility-Based Timing

```kotlin
// When user says "click continue"

// Find elements that have been visible long enough (user had time to read)
val candidates = elementVisibilityMap.entries.filter { (hash, startTime) ->
    val duration = System.currentTimeMillis() - startTime
    duration > 2000  // Visible for at least 2 seconds
}

// User likely means elements they can see and have had time to notice
```

---

## Documentation Requirements

**After Implementation:**
1. Update VoiceOSCore changelog
2. Update API reference with new entities/DAOs
3. Update developer guide with interaction tracking usage
4. Create usage examples document
5. Update database schema documentation

---

## Timeline Estimate

**Total: 15-20 hours**

**Week 1 (8-10 hours):**
- Day 1-2: Create entities and DAOs (4 hours)
- Day 2-3: Database migration v7→v8 (2 hours)
- Day 3-4: Visibility tracking implementation (4 hours)

**Week 2 (7-10 hours):**
- Day 1-2: Interaction recording (3 hours)
- Day 2-3: State change tracking (4 hours)
- Day 3-4: Testing and bug fixes (3 hours)

---

## Next Steps

**Immediate:**
1. Create UserInteractionEntity.kt
2. Create ElementStateHistoryEntity.kt
3. Create UserInteractionDao.kt
4. Create ElementStateHistoryDao.kt
5. Update AppScrapingDatabase.kt (add entities, DAOs, migration)

**Then:**
6. Implement visibility tracking
7. Implement interaction recording
8. Implement state change tracking
9. Testing
10. Documentation

---

**Created:** 2025-10-18 23:25 PDT
**Author:** Manoj Jhawar
**Status:** Ready for Implementation
**Next Action:** Begin entity creation
