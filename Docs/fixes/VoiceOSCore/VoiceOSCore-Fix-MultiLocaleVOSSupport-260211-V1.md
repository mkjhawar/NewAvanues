# VoiceOSCore-Fix-MultiLocaleVOSSupport-260211-V1

## Summary
Implemented full multi-locale voice command support with 5 locales (en-US, es-ES, fr-FR, de-DE, hi-IN). Users can now switch voice command language from Settings at runtime; all 107 commands, help screen, and web command handler update automatically.

## Architecture

```
Settings UI (SettingsDropdownRow)
    -> AvanuesSettingsRepository.updateVoiceLocale()
    -> DataStore: "voice_command_locale"
        |
        v
VoiceAvanueAccessibilityService (collectLatest)
    -> previousVoiceLocale tracking detects change
    -> CommandManager.switchLocale(newLocale)
        |
        v
CommandLoader.forceReload() -> commands_static DB -> StaticCommandRegistry.reset()
    -> populateStaticRegistryFromDb() -> all consumers auto-update
```

## Files Modified

| # | File | Change |
|---|------|--------|
| 1 | `CommandManager.kt` | Fixed `switchLocale()`: use `forceReload()` instead of `initializeCommands()`, add `StaticCommandRegistry.reset()` + `populateStaticRegistryFromDb()` after `loadDatabaseCommands()` |
| 2 | `AvanuesSettingsRepository.kt` | Added `voiceLocale` field to `AvanuesSettings`, `KEY_VOICE_LOCALE` DataStore key, `updateVoiceLocale()` method |
| 3 | `VoiceControlSettingsProvider.kt` | Added `SUPPORTED_LOCALES` (5 entries), `SettingsDropdownRow` for locale picker, searchable entry for "voice_locale" |
| 4 | `VoiceAvanueAccessibilityService.kt` | Added locale change detection in `cursorSettingsJob` via `previousVoiceLocale` tracking, calls `CommandManager.switchLocale()` |

## Files Created

| # | File | Content |
|---|------|---------|
| 5 | `es-ES.VOS` | 107 Spanish voice commands (natural spoken Spanish) |
| 6 | `fr-FR.VOS` | 107 French voice commands (natural spoken French) |
| 7 | `de-DE.VOS` | 107 German voice commands (informal "du" form) |
| 8 | `hi-IN.VOS` | 107 Hindi voice commands (romanized Hinglish) |

All VOS files located at `apps/avanues/src/main/assets/localization/commands/`.

## Key Design Decisions

1. **forceReload() over initializeCommands()**: `initializeCommands()` has a version check that skips reload if version matches. Locale switching requires `forceReload()` which clears version tracking first, ensuring the new locale's VOS file gets loaded.

2. **Locale observation inside cursorSettingsJob**: Rather than creating a separate coroutine for locale observation, added to the existing `cursorSettingsJob` that already collects all `AvanuesSettings` changes. Uses `previousVoiceLocale` to distinguish initial load from actual changes.

3. **StaticCommandRegistry.reset() after locale switch**: Without reset, the cached `_dbCommands` would still contain the old locale's commands. The reset → repopulate cycle ensures all downstream consumers (help screen, web commands) reflect the new locale.

4. **Romanized Hinglish for hi-IN**: Speech engines (Vivoka/Whisper/Google) work better with ASCII-compatible triggers. Romanized Hindi with common English loanwords ("settings kholo", "volume badhao") matches how Indian speakers naturally mix Hindi and English.

5. **All maps identical across locales**: `category_map`, `action_map`, and `meta_map` are code identifiers (not user-facing). Only `primary_phrase`, `synonyms`, and `description` are translated.

## Supported Locales

| Locale | Language | Example: "go back" |
|--------|----------|---------------------|
| en-US | English (US) | "go back" |
| es-ES | Spanish (Spain) | "ir atras" |
| fr-FR | French (France) | "retour" |
| de-DE | German (Germany) | "geh zurueck" |
| hi-IN | Hindi (India) | "peeche jao" |

## Verification
- VoiceOSCore: BUILD SUCCESSFUL
- Avanues app: assembleDebug BUILD SUCCESSFUL (all 5 VOS files bundled)
- All 4 new VOS files: 107 commands each, valid JSON, IDs match en-US
- Chapter 93 updated with multi-locale runtime section

## Documentation Updated
- Chapter 93: Added Section 8 (Multi-Locale Runtime Support) and updated Section 9 (Adding New Languages)
