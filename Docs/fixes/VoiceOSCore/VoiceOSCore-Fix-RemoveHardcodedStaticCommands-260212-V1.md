# VoiceOSCore-Fix-RemoveHardcodedStaticCommands-260212-V1

## Summary
Removed ~800 lines of hardcoded English-only static command fallback from `StaticCommandRegistry.kt`, making `.VOS files → CommandLoader → SQLDelight DB` the single source of truth for all voice commands.

## Problem
`StaticCommandRegistry.kt` contained two command sources:
1. **DB-loaded commands** via `initialize()` — populated from `.VOS` files through `CommandLoader` seeding into SQLDelight
2. **Hardcoded Kotlin lists** — ~120 commands across 14 categories (navigationCommands, mediaCommands, systemCommands, etc.) used as fallback when `_dbCommands` was null

Issues with the dual-source approach:
- **Duplication**: Same commands existed in both `.VOS` files AND hardcoded Kotlin. Updating a `.VOS` file left the hardcoded fallback stale.
- **English-only fallback**: Hardcoded commands were only in English. Multi-locale support (es-ES, fr-FR, de-DE, hi-IN) only worked through the `.VOS → DB` path.
- **Maintenance burden**: Two places to keep in sync — developers could forget to update one.
- **False safety net**: The fallback masked bugs in the `CommandLoader` startup flow. If `.VOS` loading failed silently, the app would appear to work but only with stale English commands.

## Solution
- Deleted all 14 hardcoded command lists (`navigationCommands`, `mediaCommands`, `systemCommands`, `screenCommands`, `voiceOSCommands`, `cursorCommands`, `appCommands`, `appControlCommands`, `accessibilityCommands`, `textCommands`, `readingCommands`, `inputCommands`, `browserCommands`, `webGestureCommands`)
- Deleted `hardcodedAll()` aggregation function
- Changed `all()` from `_dbCommands ?: hardcodedAll()` to `_dbCommands ?: emptyList()`
- Retained: `initialize()`, `isInitialized()`, `reset()`, all query methods (`byCategory()`, `findByPhrase()`, `allPhrases()`, etc.), NLU/LLM integration methods, `StaticCommand` data class

## Files Modified
| File | Change |
|------|--------|
| `Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/command/StaticCommandRegistry.kt` | Removed ~800 lines of hardcoded command lists and fallback |

## Impact
- **No external consumers broken** — grep confirmed no code directly references the hardcoded list properties (`navigationCommands`, etc.)
- **Startup flow**: `CommandLoader.seedFromAssets()` MUST run before voice pipeline starts. If it fails, `all()` returns empty list (not stale English commands).
- **Testing**: Any test that relied on hardcoded fallback must ensure DB is seeded first.

## Verification
- Build: `./gradlew :Modules:VoiceOSCore:compileKotlinAndroid`
- Runtime: Verify `CommandLoader.seedFromAssets()` completes before first voice command dispatch
- Multi-locale: Switch locale in settings → verify commands change language

## Related
- `.VOS` seed files: `apps/avanues/src/main/assets/localization/commands/` (en-US.app.vos, en-US.web.vos, etc.)
- CommandLoader: `Modules/VoiceOSCore/src/androidMain/.../commandmanager/loader/CommandLoader.kt`
- Chapter 93 (Voice Command Pipeline & Localization)

## Author
Session: 260212 | Branch: VoiceOSCore-KotlinUpdate
