# VoiceOSCore-Fix-Phase2DBDrivenCommands-260211-V1

## Summary
Migrated voice command system from hardcoded English strings to a DB-driven single source of truth architecture. All 107 static voice commands now flow through: VOS seed files -> SQLDelight DB -> StaticCommandRegistry/HelpCommandDataProvider.

## Architecture (Before)
```
StaticCommandRegistry (107 hardcoded commands)
HelpCommandDataProvider (77 hardcoded help entries - DUPLICATE)
WebCommandHandler.supportedActions (60 hardcoded phrases - DUPLICATE)
```
Three separate sources, all hardcoded English, all duplicating each other.

## Architecture (After)
```
en-US.VOS (v2.0 seed file)       <- SINGLE source of command definitions
        |
        v
CommandLoader.initializeCommands()
        |
        v
commands_static table (SQLDelight) <- Runtime source of truth
        |
        v
CommandManager.populateStaticRegistryFromDb()
        |
        v
StaticCommandRegistry._dbCommands  <- Shared registry (all consumers read from here)
   |          |          |
   v          v          v
all()    byCategory()  findByPhrase()
   |          |
   v          v
WebCommandHandler  HelpCommandDataProvider
(supportedActions)  (getCategories())
```

## Files Modified

| # | File | Change |
|---|------|--------|
| 1 | `en-US.VOS` (NEW) | v2.0 seed file: 107 commands with category_map, action_map, meta_map |
| 2 | `ArrayJsonParser.kt` | v2.0 support: reads category_map/action_map/meta_map, passes actionType+metadata to entity |
| 3 | `VoiceCommandDaoAdapter.kt` | Added actionType/metadata fields to VoiceCommandEntity, resolvedAction property, updated insert/update to use resolvedAction |
| 4 | `CommandLoader.kt` | Version bump 1.0->2.0, fixed getAvailableLocales() .json->.VOS bug |
| 5 | `StaticCommandRegistry.kt` | Added _dbCommands cache, initialize()/isInitialized()/reset(), all() returns DB commands with hardcoded fallback |
| 6 | `CommandManager.kt` | Added populateStaticRegistryFromDb() — converts VoiceCommandEntity->StaticCommand after DB load |
| 7 | `HelpCommandData.kt` | Rewrote to derive from StaticCommandRegistry instead of hardcoded lists; template commands stay static |

## VOS v2.0 Format
Root-level maps added to existing v1.0 compact array format:
- `category_map`: prefix -> CommandCategory name (e.g., "nav" -> "NAVIGATION")
- `action_map`: command_id -> CommandActionType name (e.g., "nav_back" -> "BACK")
- `meta_map`: command_id -> metadata JSON (e.g., "gesture_pan_left" -> {"direction":"left"})
- Backward compatible: v1.0 files still work (prefix-derived category, id-based action)

## Key Design Decisions
1. **VoiceCommandEntity.resolvedAction**: Returns actionType for v2.0, falls back to command id for v1.0
2. **StaticCommandRegistry fallback**: _dbCommands ?? hardcodedAll() ensures the app works before DB is loaded
3. **Template commands in HelpCommandDataProvider**: Parametric patterns like "click [element]" stay static (not in DB)
4. **No ICommandPhraseProvider interface**: Deleted per Rule 2 (single implementation, unnecessary indirection)

## Verification
- VoiceOSCore: BUILD SUCCESSFUL
- WebAvanue: BUILD SUCCESSFUL
- Avanues app: BUILD SUCCESSFUL
- Zero compilation errors, only pre-existing deprecation warnings
