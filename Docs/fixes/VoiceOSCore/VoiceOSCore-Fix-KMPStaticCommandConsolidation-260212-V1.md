# VoiceOSCore-Fix-KMPStaticCommandConsolidation-260212-V1

## Problem
The `commands_static` database table was empty on device. All 107 static voice commands (scroll, back, home, volume, etc.) were unavailable to the voice engine.

### Root Cause
Two loading paths existed and NEITHER was wired:
- **Path A (KMP)**: `VoiceOSCore.initialize()` -> `staticCommandPersistence.populateIfNeeded()` -- dead because `createForAndroid()` never called `.withStaticCommandPersistence()`
- **Path B (Android)**: `CommandManager.initialize()` -> `CommandLoader.initializeCommands()` -- dead because nobody called `CommandManager.initialize()`

## Solution: Full KMP Consolidation

### New Files
| File | Purpose |
|------|---------|
| `VoiceOSCore/src/commonMain/.../loader/VosParser.kt` | KMP VOS file parser (kotlinx.serialization, replaces ArrayJsonParser) |
| `VoiceOSCore/src/commonMain/.../loader/StaticCommandPersistenceImpl.kt` | KMP IStaticCommandPersistence impl, uses raw SQLDelight queries |

### Modified Files
| File | Change |
|------|--------|
| `VoiceOSCore/build.gradle.kts` | Removed dead sqldelight plugin + block; moved Database dep to commonMain |
| `VoiceOSCore/.../VoiceOSCoreAndroidFactory.kt` | Wire StaticCommandPersistenceImpl into Builder |
| `apps/avanues/.../VoiceAvanueAccessibilityService.kt` | Add CommandManager.initialize() after VoiceOSCore.initialize() |
| `apps/avanues/.../OverlayItemGenerator.kt` | Fix truncation 40->120 chars; remove deduplicateAvids() |
| `apps/avanues/.../DynamicCommandGenerator.kt` | Add fromScroll parameter to processScreen() |
| `VoiceOSCore/.../ArrayJsonParser.kt` | Marked @Deprecated (callers: CommandLoader, VosFileImporter) |
| `VoiceOSCore/.../VoiceCommandDaoAdapter.kt` | Marked @Deprecated (callers: CommandLoader, CommandManager) |

## Architecture (After)

```
VOS seed files (assets/localization/commands/*.vos)
        |
        v
VoiceOSCore.initialize()
  -> StaticCommandPersistenceImpl.populateIfNeeded()
    -> VosParser.parse() (KMP, kotlinx.serialization)
    -> voiceCommandQueries.insertCommandFull() (raw SQLDelight)
    -> databaseVersionQueries.setVersion() (version check)
        |
        v
commands_static table (SQLDelight) <- Runtime source of truth
        |
        v
CommandManager.initialize() (called right after)
  -> CommandLoader.initializeCommands() (skips, DB already populated)
  -> loadDatabaseCommands() (builds pattern cache)
  -> populateStaticRegistryFromDb() (fills StaticCommandRegistry)
        |
        v
StaticCommandRegistry._dbCommands <- All consumers read from here
```

## Overlay Fixes

### Fix 1: Gmail Duplicate Badge Numbers
- **Cause**: `.take(40)` truncation made emails with similar subjects hash identically
- **Fix**: Increased to `.take(120)` for more unique hashes
- **Also**: Removed `deduplicateAvids()` -- ordinal suffixes were unstable across scroll

### Fix 2: Overlay Not Cleared on Gmail Navigation
- **Cause**: processScreen() couldn't distinguish scroll from navigation
- **Fix**: Added `fromScroll` parameter. Caller passes `true` from onScrollSettled(), `false` from onCommandsUpdated()
- **Logic**: `if (isNewScreen && isTargetApp && !fromScroll)` -> reset numbering + clear overlays

## Follow-up (Not Done)
- Migrate CommandLoader + VosFileImporter from ArrayJsonParser to VosParser
- Remove VoiceCommandDaoAdapter after migrating CommandManager to raw queries
- Remove CommandDatabase wrapper (use VoiceOSDatabaseManager instead)

## Verification
1. Build: `./gradlew :Modules:VoiceOSCore:compileDebugKotlin :apps:avanues:compileDebugKotlin`
2. Deploy and enable accessibility service
3. Check App Inspection: `commands_static` should have ~107+ rows for en-US
4. Voice test: "scroll down", "go back", "volume up" should execute
5. Gmail overlay: unique badge numbers on duplicate emails
6. Gmail navigation: badges clear and re-number on inbox -> detail
