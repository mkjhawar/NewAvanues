# VoiceOSCore-Plan-DomainActivationSystem-260216-V1

## Problem Statement

All 45 web static commands (BROWSER + WEB_GESTURE) require a WebAvanue-specific hack to route correctly. The current implementation in `VoiceAvanueAccessibilityService.webCommandCollectorJob` (lines 236-245) re-registers static commands from `StaticCommandRegistry` into the dynamic `CommandRegistry` with `source="web"` metadata. ActionCoordinator's pre-check (lines 432-440) catches these before `extractVerbAndTarget` short-circuits, and the `source="web"` bypass skips priority-based handler routing.

**This hack won't scale.** When VoiceNotes, Cockpit, DeviceManager, or other modules need their own voice commands, each would need to copy this pattern — a separate collector job, a separate source tag, a separate bypass. The domain info is already parsed from `.vos` files (`"domain": "web"`) but discarded before it reaches `StaticCommand` or the DB.

**Goal:** Replace the WebAvanue-specific hack with a general-purpose **domain activation system** where modules are first-class citizens. When a module activates, its commands become routable. When it deactivates, they don't.

## Architecture

### Domain Concept

A **domain** identifies which module "owns" a set of voice commands:

| Domain | VOS File | Module | Activation Condition |
|--------|----------|--------|---------------------|
| `app` | `.app.vos` | Core Avanues | Always active |
| `web` | `.web.vos` | WebAvanue | Browser in foreground |
| `notes` | `.notes.vos` (future) | VoiceNotes | VoiceNotes screen active |
| `cockpit` | `.cockpit.vos` (future) | Cockpit | Cockpit screen active |
| `cursor` | `.cursor.vos` (future) | VoiceCursor | Cursor overlay visible |

Domain `app` is **always active** — its commands are globally available. All other domains are conditionally active.

### Current Data Flow (domain is lost)

```
.web.vos → VosParser (domain="web") → CommandLoader (domain DISCARDED)
         → VoiceCommandEntity (no domain field)
         → commands_static DB (no domain column)
         → StaticCommand (no domain field)
         → StaticCommandRegistry (domain gone, only category remains)
```

### Proposed Data Flow (domain preserved)

```
.web.vos → VosParser (domain="web") → CommandLoader (domain PRESERVED)
         → VoiceCommandEntity (domain field)
         → commands_static DB (domain column)
         → StaticCommand (domain field)
         → StaticCommandRegistry (domain-aware queries)
         → ActionCoordinator (routes based on active domains)
```

### Routing Logic

When a voice phrase is recognized:

1. **Static registry lookup**: Find all matching `StaticCommand` entries by phrase
2. **Domain filter**: Keep only commands whose domain is in `ActionCoordinator.activeDomains`
3. **Priority resolution**: If multiple domains match (e.g., "go back" in both `app` and `web`):
   - Non-`app` domain wins when active (more specific context)
   - `app` domain is fallback when no module-specific match
4. **Handler routing**: Route to the handler associated with the winning command's category
5. **No dynamic re-registration needed**: Static commands stay in `StaticCommandRegistry`

### Phrase Conflict Resolution

| Phrase | App Domain | Web Domain | Browser Active | Result |
|--------|-----------|------------|---------------|--------|
| "go back" | BACK (SystemHandler) | PAGE_BACK (WebCommandHandler) | Yes | Web domain wins → PAGE_BACK |
| "go back" | BACK (SystemHandler) | PAGE_BACK (WebCommandHandler) | No | App domain only → BACK |
| "scroll down" | SCROLL_DOWN (AndroidGestureHandler) | SCROLL_PAGE_DOWN (WebCommandHandler) | Yes | Web domain wins |
| "zoom in" | ZOOM_IN (ScreenHandler) | ZOOM_IN (WebCommandHandler) | Yes | Web domain wins |

## Implementation Phases

### Phase 1: Schema — Add Domain Column (Foundation)

**File: `Modules/Database/src/commonMain/sqldelight/com/augmentalis/database/VoiceCommand.sq`**

Add `domain` column to `commands_static` table. Use SQLDelight migration:

```sql
-- Migration: add domain column (default "app" for existing rows)
ALTER TABLE commands_static ADD COLUMN domain TEXT NOT NULL DEFAULT 'app';
CREATE INDEX idx_vc_domain ON commands_static(domain);
```

Add domain-aware queries:

```sql
-- Get commands by domain
getCommandsByDomain:
SELECT * FROM commands_static WHERE domain = ? AND locale = ? ORDER BY priority DESC;

-- Get commands for active domains
getCommandsForDomains:
SELECT * FROM commands_static WHERE domain IN ? AND locale = ? ORDER BY priority DESC;
```

### Phase 2: Entity — Propagate Domain Through Data Layer

**File: `VoiceCommandDaoAdapter.kt`**
- Add `domain: String = "app"` to `VoiceCommandEntity`
- Update `toEntity()` mapper to read domain from DB result
- Update insert/insertBatch to write domain

**File: `CommandLoader.kt`**
- Pass domain from VOS file name to entity: `.app.vos` → domain="app", `.web.vos` → domain="web"
- In `loadLocaleFromAssets()`, tag app commands with domain="app", web commands with domain="web"

**File: `ArrayJsonParser.kt`**
- Accept domain parameter, pass through to VoiceCommandEntity

### Phase 3: Model — Add Domain to StaticCommand

**File: `StaticCommandRegistry.kt`**
- Add `domain: String = "app"` to `StaticCommand` data class
- Add `byDomain(domain: String)` query method
- Add `byDomainsAsQuantized(domains: Set<String>)` for active domain filtering
- Propagate domain into `toQuantizedCommands()` metadata: `"domain" to domain`

**File: `CommandManager.kt`**
- In `populateStaticRegistryFromDb()`, read domain from entity and pass to StaticCommand

### Phase 4: Routing — Domain-Aware ActionCoordinator

**File: `ActionCoordinator.kt`**

Add module activation API:

```kotlin
// Active domains — "app" is always present
private val activeDomains = mutableSetOf("app")

fun activateModule(domain: String) {
    activeDomains.add(domain)
    LoggingUtils.d("Module activated: $domain (active: $activeDomains)", TAG)
}

fun deactivateModule(domain: String) {
    if (domain != "app") { // Never deactivate core app domain
        activeDomains.remove(domain)
        LoggingUtils.d("Module deactivated: $domain (active: $activeDomains)", TAG)
    }
}

fun isModuleActive(domain: String): Boolean = domain in activeDomains
```

Replace current routing in `processVoiceCommand()`:

**Before (current hack):**
```kotlin
// Pre-check: Full-phrase match for web-source commands
if (commandRegistry.size > 0) {
    val fullPhraseMatch = commandRegistry.findByPhrase(normalizedText)
    if (fullPhraseMatch != null && fullPhraseMatch.metadata["source"] == "web") {
        return processCommand(fullPhraseMatch)
    }
}
```

**After (domain-aware):**
```kotlin
// Domain-aware static command routing
// Check if phrase matches a static command in an active non-app domain
// (module-specific commands take priority over app commands when module is active)
val staticMatch = StaticCommandRegistry.findByPhrase(normalizedText)
if (staticMatch != null && staticMatch.domain != "app" && staticMatch.domain in activeDomains) {
    val quantized = staticMatch.toQuantizedCommand().copy(
        metadata = staticMatch.toQuantizedCommand().metadata + mapOf("source" to staticMatch.domain)
    )
    LoggingUtils.d("Domain match: '${staticMatch.primaryPhrase}' in domain=${staticMatch.domain}", TAG)
    return processCommand(quantized)
}
```

In `processCommand()`, replace source="web" bypass:

**Before:**
```kotlin
val handler = if (command.metadata["source"] == "web") {
    handlerRegistry.getHandlersForCategory(ActionCategory.BROWSER)
        .firstOrNull { it.canHandle(command) }
        ?: handlerRegistry.findHandler(command)
} else {
    handlerRegistry.findHandler(command)
}
```

**After:**
```kotlin
// Domain-specific handler routing
val commandDomain = command.metadata["source"] ?: command.metadata["domain"] ?: "app"
val handler = if (commandDomain != "app" && commandDomain in activeDomains) {
    // Route to domain-specific handler category
    val domainCategory = domainToCategory(commandDomain)
    handlerRegistry.getHandlersForCategory(domainCategory)
        .firstOrNull { it.canHandle(command) }
        ?: handlerRegistry.findHandler(command)
} else {
    handlerRegistry.findHandler(command)
}
```

Add domain-to-category mapping:
```kotlin
private fun domainToCategory(domain: String): ActionCategory {
    return when (domain) {
        "web" -> ActionCategory.BROWSER
        "notes" -> ActionCategory.APP  // future: ActionCategory.VOICENOTES
        "cockpit" -> ActionCategory.APP  // future: ActionCategory.COCKPIT
        else -> ActionCategory.APP
    }
}
```

### Phase 5: Module Integration — Simplify WebAvanue Activation

**File: `VoiceAvanueAccessibilityService.kt`**

Simplify `webCommandCollectorJob`:

```kotlin
webCommandCollectorJob = serviceScope.launch {
    BrowserVoiceOSCallback.activeWebPhrases
        .collect { phrases ->
            try {
                voiceOSCore?.updateWebCommands(phrases)

                val callbackInstance = BrowserVoiceOSCallback.activeInstance
                if (callbackInstance != null && phrases.isNotEmpty()) {
                    // DOM-scraped element commands (truly dynamic)
                    val quantizedWebCommands = callbackInstance.getWebCommandsAsQuantized()
                    voiceOSCore?.actionCoordinator?.updateDynamicCommandsBySource("web", quantizedWebCommands)

                    // Wire executor
                    webCommandHandler?.let { wch ->
                        val executor = WebCommandExecutorImpl(callbackInstance)
                        wch.setExecutor(executor)
                    }

                    // Activate web domain (static commands now route via domain system)
                    voiceOSCore?.actionCoordinator?.activateModule("web")

                    Log.d(TAG, "Web active: ${phrases.size} phrases, ${quantizedWebCommands.size} dynamic")
                } else if (phrases.isEmpty()) {
                    // Deactivate web domain
                    voiceOSCore?.actionCoordinator?.deactivateModule("web")
                    voiceOSCore?.actionCoordinator?.clearDynamicCommandsBySource("web")
                    webCommandHandler?.setExecutor(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update web commands", e)
            }
        }
}
```

**Removed:**
- Lines 236-245: Static BROWSER/WEB_GESTURE re-registration (replaced by domain activation)
- Line 251: `clearDynamicCommandsBySource("web_static")` (no more web_static source)
- ActionCoordinator lines 432-440: web pre-check hack (replaced by domain-aware routing)

### Phase 6: VOS File Pattern for New Modules (Future)

When adding VoiceNotes commands:

1. Create `en-US.notes.vos` (and other locales):
```json
{
  "version": "2.1",
  "locale": "en-US",
  "domain": "notes",
  "category_map": { "notes": "VOICENOTES" },
  "action_map": { ... },
  "commands": [ ... ]
}
```

2. Update `CommandLoader` file extension list:
```kotlin
private val DOMAIN_EXTENSIONS = listOf(".app.vos", ".web.vos", ".notes.vos")
```

3. When VoiceNotes screen activates:
```kotlin
voiceOSCore?.actionCoordinator?.activateModule("notes")
```

4. When VoiceNotes screen deactivates:
```kotlin
voiceOSCore?.actionCoordinator?.deactivateModule("notes")
```

No re-registration hacks, no special collector jobs, no source metadata overrides.

## Files Modified

| # | File | Change |
|---|------|--------|
| 1 | `Modules/Database/.../VoiceCommand.sq` | Add `domain` column + migration + queries |
| 2 | `.../VoiceCommandDaoAdapter.kt` | Add domain to entity + mappers |
| 3 | `.../CommandLoader.kt` | Tag entities with domain from filename |
| 4 | `.../ArrayJsonParser.kt` | Accept and propagate domain parameter |
| 5 | `.../StaticCommandRegistry.kt` | Add domain to StaticCommand + query methods |
| 6 | `.../CommandManager.kt` | Pass domain through to StaticCommand |
| 7 | `.../ActionCoordinator.kt` | activeDomains + domain-aware routing |
| 8 | `.../VoiceAvanueAccessibilityService.kt` | Simplify to activateModule/deactivateModule |

## Migration

- SQLDelight migration adds `domain TEXT NOT NULL DEFAULT 'app'` — all existing rows default to "app"
- On next app launch, `CommandLoader.seedFromAssets()` re-seeds with correct domains
- No user-visible changes

## Testing

1. Browser active: "go back" → WebCommandHandler (domain="web" active)
2. Browser inactive: "go back" → SystemHandler (domain="app" only)
3. "scroll down" → WebCommandHandler in browser, AndroidGestureHandler elsewhere
4. "retrain page" → WebCommandHandler (web domain only, no app equivalent)
5. "play music" → MediaHandler (domain="app", always active)
6. Module lifecycle: activate web → verify routing → deactivate web → verify fallback

## Dependencies

All exist — no new module dependencies:
- `StaticCommandRegistry` (commonMain KMP)
- `ActionCoordinator` (commonMain KMP)
- `VoiceCommandEntity`, `VoiceCommandDaoAdapter` (androidMain)
- `CommandLoader`, `ArrayJsonParser` (androidMain)
- SQLDelight migration system (Database module)

## Risks

| Risk | Mitigation |
|------|-----------|
| DB migration on existing installs | Default `domain='app'` preserves all existing commands |
| Phrase ambiguity across domains | Domain priority: non-app active domain > app domain |
| Multiple non-app domains active simultaneously | First match by domain priority (web > notes > cockpit) |
| Performance of static lookup per voice command | O(n) over ~120 commands, negligible vs speech recognition latency |
