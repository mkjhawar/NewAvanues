# Centralized Voice Command Architecture

**Category:** architecture
**Priority:** 10
**Enforced:** Yes
**Created:** 2025-11-21
**Used By:** WebAvanue, NewAvanue, all Avanues ecosystem apps
**Related ADR:** ADR-007
**Related Documentation:** Developer Manual Chapter 46

---

## Rule

**All Avanues ecosystem applications MUST use centralized voice command architecture.**

Voice commands SHALL be defined in VoiceOS's centralized JSON files (`assets/localization/commands/{locale}.json`). Apps SHALL implement ActionMappers to execute app-specific actions. Apps SHALL NOT load commands, manage VOS files, or interact with VoiceCommandDao directly.

**Core Requirements:**
1. ✅ Commands defined in VoiceOS JSON (centralized)
2. ✅ App implements ActionMapper (~50 lines)
3. ✅ App registers handler with IntentDispatcher (~10 lines)
4. ❌ **NO per-app command files**
5. ❌ **NO CommandRegistrar**
6. ❌ **NO database operations in apps**

---

## Rationale

### Problem: Distributed Command Management

Apps managing their own commands creates:

**1. Duplicate Definitions**
```
❌ Commands scattered across apps:
VoiceOS/assets/commands/en-US.json     → "scroll to top"
WebAvanue/assets/commands/browser.vos  → "scroll to top"
BrowserLite/assets/commands/lite.vos   → "scroll to top"

Problem: 3 copies → sync nightmare, conflicts, inconsistency
```

**2. Deployment Issues**
```
❌ Update command phrase:
- Modify VoiceOS JSON
- Modify WebAvanue VOS
- Modify BrowserLite VOS
- Release 3 app updates
```

**3. Code Bloat**
```
❌ Per-app code:
- ActionMapper: 50 lines
- CommandRegistrar: 80 lines
- VOS file management: 20 lines
Total: 150 lines × 15 apps = 2,250 lines
```

**4. No Single Source of Truth**
- Which version is correct?
- How to ensure consistency?
- What if apps define conflicting commands?

### Solution: Centralized Architecture

**VoiceOS = Voice Layer (like Android Framework)**
- Android handles touch gestures → Apps respond to touch
- VoiceOS handles voice commands → Apps respond to voice

**Key Benefits:**

**1. Single Source of Truth**
```
✅ Commands in ONE place:
VoiceOS/assets/commands/en-US.json → "scroll to top"

All browser apps use same definition
```

**2. Deployment Independence**
```
✅ Update command phrase:
- Modify VoiceOS JSON only
- No app updates needed
- All apps get update automatically
```

**3. 60% Code Reduction**
```
✅ Per-app code:
- ActionMapper: 50 lines
- Handler registration: 10 lines
Total: 60 lines × 15 apps = 900 lines

Savings: 1,350 lines (60% reduction)
```

**4. Consistent UX**
- Same command phrases across all apps
- Predictable behavior
- No conflicts

---

## Requirements

### 1. Commands in VoiceOS JSON

**Location:** `VoiceOS/managers/CommandManager/src/main/assets/localization/commands/{locale}.json`

**Format:**
```json
{
  "version": "1.0",
  "locale": "en-US",
  "commands": [
    {
      "id": "SCROLL_TOP",
      "category": "browser",
      "text": "scroll to top",
      "synonyms": ["top of page", "jump to top", "page top"],
      "description": "Scroll to top of web page"
    },
    {
      "id": "SEND_EMAIL",
      "category": "email",
      "text": "send email",
      "synonyms": ["compose email", "new email"],
      "description": "Compose new email"
    }
  ]
}
```

**Categories:**
- `browser`: WebAvanue, browser apps
- `email`: Email clients
- `calendar`: Calendar apps
- `system`: System-wide commands
- `launcher`: Home screen, app drawer
- `files`: File manager

**Validation Rules:**
- ✅ `id` MUST be SCREAMING_SNAKE_CASE
- ✅ `category` MUST be lowercase, no spaces
- ✅ `text` and `synonyms` MUST be lowercase
- ✅ Minimum 1 synonym recommended

### 2. ActionMapper Implementation

**Location:** `{App}/src/.../commands/{App}ActionMapper.kt`

**Template:**
```kotlin
package com.augmentalis.{app}.commands

class {App}ActionMapper(
    private val viewModel: {App}ViewModel,
    private val controller: {App}Controller
) {
    suspend fun executeAction(
        commandId: String,
        parameters: Map<String, Any> = emptyMap()
    ): ActionResult {
        return when (commandId) {
            "COMMAND_1" -> controller.action1()
            "COMMAND_2" -> controller.action2()
            "COMMAND_3" -> {
                val param = parameters["value"] as? Int ?: 100
                controller.action3(param)
            }
            else -> ActionResult.error("Unknown command: $commandId")
        }
    }
}

data class ActionResult(
    val success: Boolean,
    val message: String? = null
) {
    companion object {
        fun success(msg: String? = null) = ActionResult(true, msg)
        fun error(msg: String) = ActionResult(false, msg)
    }
}
```

**Requirements:**
- ✅ Accept `commandId: String` and `parameters: Map<String, Any>`
- ✅ Return `ActionResult`
- ✅ Handle unknown commands gracefully
- ✅ Use suspend functions for async operations
- ✅ Delegate to ViewModel/Controller (no business logic in mapper)

### 3. Handler Registration

**Location:** `{App}/src/.../Application.kt` or `MainActivity.kt`

**Template:**
```kotlin
class {App}Application : Application() {
    override fun onCreate() {
        super.onCreate()

        val actionMapper = {App}ActionMapper(viewModel, controller)
        registerVoiceCommands(actionMapper)
    }

    private fun registerVoiceCommands(actionMapper: {App}ActionMapper) {
        try {
            val commandManager = CommandManager.getInstance(this)
            val dispatcher = commandManager.getIntentDispatcher()

            dispatcher.registerHandler("{category}") { command ->
                val result = actionMapper.executeAction(command.id, emptyMap())
                CommandResult(
                    success = result.success,
                    command = command,
                    error = if (!result.success) {
                        CommandError(ErrorCode.EXECUTION_FAILED, result.message ?: "")
                    } else null
                )
            }

            Log.i(TAG, "✅ Voice commands registered")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ VoiceOS not available - voice disabled", e)
            // Graceful degradation
        }
    }
}
```

**Requirements:**
- ✅ Register in `Application.onCreate()` or `Activity.onCreate()`
- ✅ Use correct category (matches VoiceOS JSON)
- ✅ Wrap in try-catch for graceful degradation
- ✅ Log success/failure

---

## Examples

### ✅ CORRECT: Centralized Architecture

**File Structure:**
```
VoiceOS/
└── managers/CommandManager/
    └── src/main/assets/localization/commands/
        └── en-US.json          ← ALL commands here

WebAvanue/
└── universal/src/androidMain/.../commands/
    └── WebAvanueActionMapper.kt    ← ~50 lines
```

**VoiceOS JSON (en-US.json):**
```json
{
  "version": "1.0",
  "locale": "en-US",
  "commands": [
    {
      "id": "SCROLL_TOP",
      "category": "browser",
      "text": "scroll to top",
      "synonyms": ["top of page", "jump to top"]
    },
    {
      "id": "NEW_TAB",
      "category": "browser",
      "text": "new tab",
      "synonyms": ["open new tab", "create tab"]
    }
  ]
}
```

**WebAvanueActionMapper.kt:**
```kotlin
class WebAvanueActionMapper(
    private val tabViewModel: TabViewModel,
    private val webViewController: WebViewController
) {
    suspend fun executeAction(commandId: String, parameters: Map<String, Any>): ActionResult {
        return when (commandId) {
            "SCROLL_TOP" -> webViewController.scrollTop()
            "NEW_TAB" -> {
                tabViewModel.createTab()
                ActionResult.success("New tab created")
            }
            else -> ActionResult.error("Unknown: $commandId")
        }
    }
}
```

**WebAvanueApp.kt:**
```kotlin
class WebAvanueApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val actionMapper = WebAvanueActionMapper(tabViewModel, webViewController)

        try {
            CommandManager.getInstance(this)
                .getIntentDispatcher()
                .registerHandler("browser") { command ->
                    val result = actionMapper.executeAction(command.id, emptyMap())
                    CommandResult(success = result.success, command = command)
                }
        } catch (e: Exception) {
            Log.w(TAG, "VoiceOS not available", e)
        }
    }
}
```

**Total Code:** ~60 lines (ActionMapper + registration)

---

### ❌ WRONG: Distributed Architecture

**File Structure:**
```
WebAvanue/
├── assets/commands/vos/
│   └── browser-commands.vos    ❌ Commands in app
└── commands/
    ├── WebAvanueActionMapper.kt
    └── WebAvanueCommandRegistrar.kt    ❌ Registrar not needed
```

**browser-commands.vos (in WebAvanue):**
```json
❌ Commands duplicated in app
{
  "schema": "vos-1.0",
  "commands": [
    {"action": "SCROLL_TOP", "cmd": "scroll to top", ...}
  ]
}
```

**WebAvanueCommandRegistrar.kt:**
```kotlin
❌ NOT NEEDED - VoiceOS loads commands centrally
class WebAvanueCommandRegistrar {
    suspend fun registerCommands() {
        ingestion.ingestVOSFile("browser-commands.vos")
        registry.registerActionHandler("com.augmentalis.webavanue") { ... }
    }
}
```

**Problems:**
- ❌ Commands in two places (VoiceOS + WebAvanue)
- ❌ 150 lines of code vs 60 lines
- ❌ Synchronization issues
- ❌ Deployment dependency (update commands = update app)
- ❌ Violates single source of truth

**This approach is FORBIDDEN.**

---

## Exceptions

**NO EXCEPTIONS.** Centralized architecture is MANDATORY for all Avanues ecosystem applications.

**Rationale:**
- Voice commands are system-level (like touch gestures)
- Single source of truth is non-negotiable
- Consistency across apps is required
- Code reduction benefits entire ecosystem

**If you believe your use case requires an exception, escalate to architecture review.**

---

## Testing Requirements

### Unit Tests (App-level)

```kotlin
@Test
fun `ActionMapper routes SCROLL_TOP correctly`() = runTest {
    val result = actionMapper.executeAction("SCROLL_TOP")

    verify(webViewController).scrollTop()
    assertTrue(result.success)
}

@Test
fun `ActionMapper handles unknown command`() = runTest {
    val result = actionMapper.executeAction("UNKNOWN")

    assertFalse(result.success)
    assertTrue(result.message?.contains("Unknown") == true)
}
```

### Integration Tests (VoiceOS-level)

```kotlin
@Test
fun `VoiceOS loads browser commands`() = runTest {
    val loader = CommandLoader.create(context)
    loader.initializeCommands()

    val commands = commandDao.getCommandsForCategory("browser")
    assertTrue(commands.size >= 13)
}

@Test
fun `IntentDispatcher routes to browser handler`() = runTest {
    val dispatcher = IntentDispatcher(context)
    var called = false

    dispatcher.registerHandler("browser") { command ->
        called = true
        CommandResult(success = true, command = command)
    }

    dispatcher.routeCommand(
        Command(id = "SCROLL_TOP", category = "browser"),
        RoutingContext(currentApp = "com.augmentalis.webavanue")
    )

    assertTrue(called)
}
```

---

## Performance

### Centralized Loading Overhead

```
Load ALL commands at VoiceOS startup: ~50-100ms (first launch)
Database cached: ~5ms (subsequent launches)

Impact: Negligible - happens once at system startup
```

### Per-Command Routing

```
Database query: ~5ms
IntentDispatcher routing: ~1ms
Total: ~6ms

Voice command execution: 500ms - 5s
Overhead: 0.1-1% (imperceptible)
```

---

## Migration Guide

**For existing distributed apps:**

1. **Delete per-app command files**
   - Remove `{app}/assets/commands/*.vos`
   - Remove `{app}/commands/*CommandRegistrar.kt`

2. **Add commands to VoiceOS JSON**
   - Move command definitions to `VoiceOS/assets/localization/commands/en-US.json`
   - Use appropriate category

3. **Keep ActionMapper**
   - ActionMapper stays the same
   - Only mapping logic, no loading

4. **Update registration**
   - Remove CommandRegistrar calls
   - Register handler directly with IntentDispatcher

5. **Test**
   - Verify commands load from VoiceOS
   - Verify handler receives commands

**Time:** 30-60 minutes per app

---

## Related Standards

- **ADR-006:** VoiceOS Command Delegation Pattern (execution delegation)
- **ADR-007:** Centralized Voice Command Architecture (this standard)
- **Developer Manual Chapter 46:** Complete implementation guide
- **Protocol-File-Organization-v2.0:** File structure requirements

---

## Enforcement

**Enforced via:**

1. **Code Review:** PRs with distributed command loading rejected
2. **Architecture Review:** Design documents validated
3. **CI/CD:** Detect per-app VOS files (fail build)
4. **Documentation:** Chapter 46 required reading

**Violations:**

| Violation | Consequence |
|-----------|-------------|
| Commands in app assets | PR rejected, must move to VoiceOS |
| CommandRegistrar created | PR rejected, must use IntentDispatcher |
| Missing graceful degradation | PR blocked until added |

---

## Summary

### What Apps Must Do

1. ✅ Implement ActionMapper (~50 lines)
2. ✅ Register handler with IntentDispatcher (~10 lines)
3. ✅ Handle VoiceOS unavailability gracefully

### What Apps Must NOT Do

1. ❌ Load commands from files
2. ❌ Create CommandRegistrar
3. ❌ Manage VOS files
4. ❌ Interact with VoiceCommandDao
5. ❌ Duplicate command definitions

### Benefits

- ✅ 60% code reduction per app
- ✅ Single source of truth
- ✅ Deployment independence
- ✅ Consistent UX
- ✅ Easier localization
- ✅ Multi-app command sharing

---

**Last Updated:** 2025-11-21
**Status:** Active (Enforced)
**Review Cycle:** Quarterly
**Next Review:** 2026-02-21
