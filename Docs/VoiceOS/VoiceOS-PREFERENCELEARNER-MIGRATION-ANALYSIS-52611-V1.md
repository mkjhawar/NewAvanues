# PreferenceLearner Migration Analysis
**Date:** 2025-11-26 22:22:46 PST
**Agent:** Agent 2 - PreferenceLearner Migrator
**Status:** Analysis Complete

## File Information
- **Source:** `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/context/PreferenceLearner.kt.disabled`
- **Target:** Same file, remove `.disabled` suffix
- **Dependencies:** VoiceOSDatabaseManager + 4 repositories

## Database Call Inventory (18 Total)

### ✅ Already Migrated (1/18)
| Line | Old Call | New Call | Status |
|------|----------|----------|--------|
| 89 | `database.recordUsage()` | `commandUsageRepository.recordUsage()` | ✅ DONE |

### ❌ Needs Migration (17/18)

#### Group 1: Recording Usage (1 call)
| Line | Old Call | Proposed Repository Method | Repository |
|------|----------|---------------------------|------------|
| 128 | `database.recordUsage(commandId, contextKey, success, timestamp)` | `commandUsageRepository.insert()` | ICommandUsageRepository |

**Implementation Strategy:**
- Create CommandUsageDTO with fields: commandId, contextKey, success, timestamp
- Call `commandUsageRepository.insert(dto)`

#### Group 2: Command Statistics (2 calls)
| Line | Old Call | Proposed Repository Method | Repository |
|------|----------|---------------------------|------------|
| 325 | `database.getCommandStats(commandId)` | `commandUsageRepository.getStatsForCommand(commandId)` | ICommandUsageRepository |

**Missing Method:** `getStatsForCommand(commandId: String): CommandStats`
- Returns: `CommandStats(totalExecutions, successfulExecutions, failedExecutions)`
- Implementation: Query CommandUsage, aggregate counts

#### Group 3: Context Preferences (1 call)
| Line | Old Call | Proposed Repository Method | Repository |
|------|----------|---------------------------|------------|
| 341 | `database.getContextPreference(commandId, contextKey)` | `contextPreferenceRepository.get(commandId, contextKey)` | IContextPreferenceRepository |

**Status:** ✅ Method already exists in interface (line 29)

#### Group 4: Total Usage Queries (6 calls)
| Line | Old Call | Proposed Repository Method | Repository |
|------|----------|---------------------------|------------|
| 376 | `database.getTotalUsageForCommand(commandId)` | `commandUsageRepository.countForCommand(commandId)` | ICommandUsageRepository |
| 377 | `database.getTotalUsageAllCommands()` | `commandUsageRepository.countTotal()` | ICommandUsageRepository |
| 393 | `database.getTotalUsageForContext(contextKey)` | `commandUsageRepository.countForContext(contextKey)` | ICommandUsageRepository |
| 394 | `database.getTotalUsageAllCommands()` | `commandUsageRepository.countTotal()` | ICommandUsageRepository |
| 410 | `database.getUsageForCommandInContext(commandId, contextKey)` | `commandUsageRepository.getForCommandInContext(commandId, contextKey).size` | ICommandUsageRepository |
| 411 | `database.getTotalUsageForCommand(commandId)` | `commandUsageRepository.countForCommand(commandId)` | ICommandUsageRepository |

**Status:** ✅ All methods exist in ICommandUsageRepository (lines 48, 51, 58)

#### Group 5: Time Decay (1 call)
| Line | Old Call | Proposed Repository Method | Repository |
|------|----------|---------------------------|------------|
| 447 | `database.applyTimeDecay(currentTime, decayFactor)` | **NEW METHOD NEEDED** | ICommandUsageRepository + IContextPreferenceRepository |

**Missing Method:** `applyTimeDecay(currentTime: Long, decayFactor: Float)`
- Apply to both repositories
- Implementation: Update usage counts with decay formula

#### Group 6: Clear Data (1 call)
| Line | Old Call | Proposed Repository Method | Repository |
|------|----------|---------------------------|------------|
| 466 | `database.clearAllData()` | `commandUsageRepository.deleteAll()` + `contextPreferenceRepository.deleteAll()` | Both |

**Status:** ✅ Methods exist (ICommandUsageRepository.deleteAll(), IContextPreferenceRepository.deleteAll())

#### Group 7: Learning Statistics (6 calls)
| Line | Old Call | Proposed Repository Method | Repository |
|------|----------|---------------------------|------------|
| 484 | `database.getTotalCommandsTracked()` | `contextPreferenceRepository.countCommands()` | IContextPreferenceRepository |
| 485 | `database.getTotalUsageAllCommands()` | `commandUsageRepository.countTotal()` | ICommandUsageRepository |
| 486 | `database.getTotalContextsTracked()` | `contextPreferenceRepository.countContexts()` | IContextPreferenceRepository |
| 487 | `database.getAverageSuccessRate()` | `contextPreferenceRepository.getAverageSuccessRate()` | IContextPreferenceRepository |
| 488 | `database.getMostUsedCommands(10)` | `contextPreferenceRepository.getMostUsedCommands(10)` | IContextPreferenceRepository |
| 489 | `database.getMostUsedContexts(10)` | `contextPreferenceRepository.getMostUsedContexts(10)` | IContextPreferenceRepository |

**Status:** ✅ All methods exist in IContextPreferenceRepository (lines 54, 57, 64, 39, 44)

## Missing Repository Methods Summary

### ICommandUsageRepository - 2 Missing Methods
1. ✅ **recordUsage()** - Use existing `insert()` method
2. ❌ **getStatsForCommand(commandId: String): CommandStats** - NEW METHOD NEEDED
3. ❌ **applyTimeDecay(currentTime: Long, decayFactor: Float)** - NEW METHOD NEEDED

### IContextPreferenceRepository - 1 Missing Method
1. ❌ **applyTimeDecay(currentTime: Long, decayFactor: Float)** - NEW METHOD NEEDED

## Data Class Requirements

### CommandStats (already exists in PreferenceLearner.kt)
```kotlin
data class CommandStats(
    var totalExecutions: Int = 0,
    var successfulExecutions: Int = 0,
    var failedExecutions: Int = 0
)
```

### ContextPreference (already exists in PreferenceLearner.kt)
```kotlin
data class ContextPreference(
    var usageCount: Int = 0,
    var successCount: Int = 0
)
```

## Migration Steps

### Step 1: Add Missing Methods to ICommandUsageRepository
Add to interface:
```kotlin
/**
 * Get aggregated statistics for a command.
 */
suspend fun getStatsForCommand(commandId: String): CommandStats

/**
 * Apply time decay to usage counts.
 */
suspend fun applyTimeDecay(currentTime: Long, decayFactor: Float)
```

### Step 2: Add Missing Methods to IContextPreferenceRepository
Add to interface:
```kotlin
/**
 * Apply time decay to preference counts.
 */
suspend fun applyTimeDecay(currentTime: Long, decayFactor: Float)
```

### Step 3: Implement Missing Methods in SQLDelight Repositories
- Implement in `SQLDelightCommandUsageRepository`
- Implement in `SQLDelightContextPreferenceRepository`

### Step 4: Update PreferenceLearner Constructor
```kotlin
// OLD
class PreferenceLearner(
    private val context: Context,
    private val commandUsageRepository: ICommandUsageRepository,
    private val contextPreferenceRepository: IContextPreferenceRepository
)

// NEW - Add @Inject for dependency injection
class PreferenceLearner @Inject constructor(
    private val context: Context,
    private val commandUsageRepository: ICommandUsageRepository,
    private val contextPreferenceRepository: IContextPreferenceRepository
)
```

### Step 5: Update All Database Calls
See detailed mapping in sections above.

### Step 6: Remove .disabled Suffix
```bash
mv PreferenceLearner.kt.disabled PreferenceLearner.kt
```

## Coordination Notes
- Agent 1 is re-enabling CommandManager - may need PreferenceLearner
- PreferenceLearner is not blocking CommandManager compilation
- Can proceed independently

## Testing Strategy
1. Compile CommandManager module
2. Verify all repository methods exist
3. Check dependency injection wiring
4. Run unit tests (if exist)

## Time Estimate
- Add repository methods: 30 minutes
- Implement in SQLDelight repos: 45 minutes
- Update PreferenceLearner: 30 minutes
- Testing: 15 minutes
- **Total: ~2 hours**

## Risk Assessment
- **Low Risk:** Most methods already exist
- **Medium Risk:** Time decay implementation (new logic)
- **Low Risk:** Constructor changes (straightforward)

## Success Criteria
- ✅ All 18 database calls migrated to repositories
- ✅ PreferenceLearner.kt compiles without errors
- ✅ No database references remain (except factory)
- ✅ Tests pass (if exist)

---
**Next Actions:**
1. Add missing methods to repository interfaces
2. Implement in SQLDelight repositories
3. Update PreferenceLearner calls
4. Test compilation
