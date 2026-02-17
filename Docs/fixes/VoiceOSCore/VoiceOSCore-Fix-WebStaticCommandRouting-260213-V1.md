# VoiceOSCore-Fix-WebStaticCommandRouting-260213-V1

## Summary
Fixed 45 web static commands (18 BROWSER + 27 WEB_GESTURE) that failed during device testing. Commands like "go back", "refresh page", "swipe up" were never reaching WebCommandHandler due to three interconnected routing bugs.

## Root Causes

### Bug 1: Static Web Commands Not in Dynamic Registry
`VoiceAvanueAccessibilityService.webCommandCollectorJob` only registered DOM-scraped element commands. Static BROWSER/WEB_GESTURE commands from .web.vos files (e.g., "go back", "refresh page") were never registered as dynamic commands with web source metadata.

### Bug 2: extractVerbAndTarget Short-Circuits Static Commands
`ActionCoordinator.processVoiceCommand()` calls `extractVerbAndTarget("go back")` which finds it in `StaticCommandRegistry` → returns `(null, null)`. With `target == null`, the dynamic command lookup is skipped entirely, and the command falls through to static handler lookup.

### Bug 3: Priority-Based Handler Stealing
`processCommand()` iterates handlers by priority. SystemHandler (priority 1) or AndroidGestureHandler (priority 2) steal overlapping phrases ("go back", "swipe up", "zoom in") before WebCommandHandler (BROWSER, priority 11) gets a chance.

### Missing: RETRAIN_PAGE in WebActionType
`CommandActionType.RETRAIN_PAGE` existed but `WebActionType` had no corresponding enum value.

## Fix Applied

### Phase 1: ActionCoordinator (2 changes)
- **processVoiceCommand()**: Added full-phrase pre-check for `source="web"` commands BEFORE `extractVerbAndTarget`. Web commands match immediately and route to `processCommand()`.
- **processCommand()**: For commands with `source="web"`, bypass priority-based `findHandler()` and query `getHandlersForCategory(BROWSER)` directly.

### Phase 2: VoiceAvanueAccessibilityService (1 change)
- In `webCommandCollectorJob`, after DOM-scraped command registration, also register static BROWSER + WEB_GESTURE commands as dynamic commands with `source="web"` metadata using source tag `"web_static"` (independent lifecycle from DOM-scraped `"web"`). Clear `"web_static"` source when browser deactivates.

### Phase 3: RETRAIN_PAGE Support (3 files)
- `IWebCommandExecutor.kt`: Added `RETRAIN_PAGE` to `WebActionType` enum
- `WebCommandHandler.kt`: Mapped `CommandActionType.RETRAIN_PAGE → WebActionType.RETRAIN_PAGE` in `resolveWebActionType()` + added phrase matching in `resolveFromPhrase()`
- `WebCommandExecutorImpl.kt`: Intercept RETRAIN_PAGE before `buildScript()`, call `BrowserVoiceOSCallback.requestRetrain()`

## Files Modified
| File | Change |
|------|--------|
| `Modules/VoiceOSCore/src/commonMain/.../actions/ActionCoordinator.kt` | Web pre-check + bypass |
| `apps/avanues/src/main/.../service/VoiceAvanueAccessibilityService.kt` | Register static web commands |
| `Modules/VoiceOSCore/src/commonMain/.../interfaces/IWebCommandExecutor.kt` | Add RETRAIN_PAGE enum |
| `Modules/VoiceOSCore/src/commonMain/.../handler/WebCommandHandler.kt` | Map RETRAIN_PAGE |
| `Modules/WebAvanue/src/commonMain/.../WebCommandExecutorImpl.kt` | Handle RETRAIN_PAGE |

## Verification
- `./gradlew :Modules:VoiceOSCore:compileDebugKotlinAndroid :Modules:WebAvanue:compileDebugKotlinAndroid :apps:avanues:compileDebugKotlin` — BUILD SUCCESSFUL
- Browser active: "go back" → WebCommandHandler → browser back (not SystemHandler)
- Browser NOT active: "go back" → SystemHandler → Android back (unchanged)
- "retrain page" → WebCommandHandler → RETRAIN_PAGE → requestRetrain()

## Branch
IosVoiceOS-Development
