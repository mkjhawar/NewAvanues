# VoiceOSCore — Handlers & Commands Quality Report — 260222

## Summary
SCORE: 72 / 100 | HEALTH: YELLOW
SCOPE: Handlers (Wave 1–4), Command Dispatch, HandlerRegistry, StaticCommandRegistry, VoiceOSAccessibilityService, CommandLoader, CursorCommandHandler

Overall the Wave 2–4 handler architecture is well-designed — thin, declarative, phrase-based routing with clean executor delegation to module controllers. The dispatch pipeline (VOS seed → CommandLoader → SQLDelight → StaticCommandRegistry → HandlerRegistry) is architecturally sound. Several structural issues and one critical bug lower the score.

---

## Handler Inventory

| Handler | Category | Commands | Dispatch Style | Issues |
|---|---|---|---|---|
| AndroidGestureHandler | NAVIGATION | ~42 phrases | Phrase + ActionType | Swipe/scroll direction comment inconsistency |
| SystemHandler | SYSTEM | ~8 | ActionType | None found |
| AppHandler | APP | ~varies | ActionType | Not reviewed (pre-existing) |
| AndroidCursorHandler | NAVIGATION | varies | Phrase | Not reviewed (pre-existing) |
| MediaHandler | MEDIA | 13 | Phrase exact-match | STREAM_MUSIC only; no ring/alarm/notification volume |
| ScreenHandler | DEVICE | 20 | Phrase exact-match | `openBluetoothSettings` returns `HandlerResult.failure` on success case; `clearNotifications` fallback incorrect |
| TextHandler | INPUT | 8 | Phrase exact-match | `ACTION_SELECT` wrong for "select all"; undo/redo explicit non-stubs |
| InputHandler | INPUT | 6 | Phrase exact-match | `showKeyboard` dead variable `success` (L49) |
| AppControlHandler | APP | 4 | Phrase exact-match | Close strategy is unreliable |
| ReadingHandler | ACCESSIBILITY | 8 | Phrase exact-match | TTS race: `ttsReady` written from callback thread, read from coroutine without sync |
| VoiceControlHandler | SYSTEM | 16 | Phrase exact-match via callback | Callbacks are global object; no lifecycle guard on cleared callbacks |
| NoteCommandHandler | NOTE | 43 | ActionType via executor | None found |
| CockpitCommandHandler | COCKPIT | 22 | ActionType via executor | "scroll up/down/zoom in/zoom out" overlap with AndroidGestureHandler |
| AnnotationCommandHandler | ANNOTATION | 15 | ActionType via executor | None found |
| ImageCommandHandler | IMAGE | 19 | ActionType via executor | "rotate left/right" overlap with ScreenHandler `toggleRotation` |
| VideoCommandHandler | VIDEO | 13 | ActionType via executor | None found |
| CastCommandHandler | CAST | 5 | ActionType via executor | None found |
| AICommandHandler | CUSTOM | 5 | ActionType via executor | None found |
| CursorCommandHandler | (legacy registry) | 22+ | String-based via CommandRegistry | Rule 7 violation; dead code; parallel registry |

---

## P0 Critical Issues

- **[InputHandler.kt:49]** Dead variable `success` assigned but never read. `softKeyboardController.showMode == SHOW_MODE_AUTO` result stored in `val success` then discarded; the actual `setShowMode()` call below it is unconditional. The check was intended as a guard but has no effect, leaving the logic branch semantically broken.

- **[ReadingHandler.kt:34-45]** `ttsReady` is a plain `Boolean` field written from the TTS init callback thread and read on Dispatchers.Default coroutine in `readScreen()`. This is an unsynchronized read/write across threads — a data race. On JVM the write may not be visible to the reader without `@Volatile` or synchronization. Consequence: `readScreen()` may see stale `false` and return "not available" even after TTS initialized successfully.

- **[VoiceOSAccessibilityService.kt:509-514]** `processVoiceCommand()` always synthesizes a dummy `QuantizedCommand(phrase=utterance, actionType=EXECUTE, targetAvid=null)` for the `onCommandExecuted` callback, ignoring the actual command the coordinator matched. Telemetry/UI feedback always receives the wrong command type. The correct command object should come from the coordinator's result, but `ActionCoordinator.processVoiceCommand()` returns `HandlerResult` not the matched command. This is an architectural gap — callers cannot know which command was actually matched. Tracked in prior memory as known issue.

- **[VoiceOSAccessibilityService.kt:482-489]** `refreshScreen()` calls `AccessibilityEvent.obtain()` but does NOT recycle the event. `AccessibilityEvent.obtain()` takes from a pool; un-recycled events cause pool exhaustion over repeated calls. Tracked in prior memory as known issue.

---

## P1 High Issues

- **[ScreenHandler.kt:210-219]** `openBluetoothSettings()` opens Bluetooth settings (which is the intended fallback action) but then returns `HandlerResult.failure(message)`. A successful fallback that navigates the user to the correct settings screen should return `HandlerResult.success(message)` or a distinct `HandlerResult.Partial`/`Recoverable`. Using `failure` here means the feedback toast tells the user "failed" when the action actually succeeded (they see Bluetooth settings open). This is a UX lie.

- **[ScreenHandler.kt:290-298]** `clearNotifications()` fallback on Android < 12: opens the notification shade but then returns `HandlerResult.success("Notification shade opened")`. The user said "clear notifications" and the shade merely opened — it was not cleared. The label is misleading and the behavior diverges from the intent.

- **[TextHandler.kt:48-49]** `ACTION_SELECT` is used for "select all". `AccessibilityNodeInfo.ACTION_SELECT` sets accessibility selection focus on a single node, NOT text selection within an EditText. The correct action for "select all text" is `Bundle.putBoolean(ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true)` with appropriate selection APIs, or dispatching `Ctrl+A` via `dispatchTextViewShortcut`. Using `ACTION_SELECT` either does nothing visible or selects the entire widget (not text range). This is a silent wrong behavior.

- **[CockpitCommandHandler.kt:46-47]** Declares `"scroll up"`, `"scroll down"`, `"zoom in"`, `"zoom out"` in `supportedActions`. These exact phrases also appear in `AndroidGestureHandler.supportedActions`. Since `AndroidGestureHandler` is registered under `NAVIGATION` category (priority 2) and `CockpitCommandHandler` under `COCKPIT` (priority 13), the gesture handler wins every time for these phrases — the Cockpit handler's entries are unreachable. If Cockpit-specific scroll is needed, phrases must be prefixed (e.g., `"frame scroll up"`).

- **[ImageCommandHandler.kt:32]** Declares `"rotate left"` and `"rotate right"`. `ScreenHandler` declares `"rotate"` and `"change orientation"`. These are adjacent but distinct intent — "rotate left/right" in image context means rotating the image 90°, not toggling auto-rotation. However, `BaseHandler.canHandle()` uses prefix matching: `"rotate left".startsWith("rotate" + " ")` → true. `ScreenHandler` is category DEVICE (priority 7) vs IMAGE (priority 16); DEVICE wins. Images cannot be rotated via voice as long as ScreenHandler intercepts "rotate left/right". Fix: use `"image rotate left"` / `"image rotate right"`.

- **[CursorCommandHandler.kt:370]** `"voice cursor help"` action explicitly returns `false` with comment "Not implemented yet". This is a Rule 1 violation (stub in production). Either implement help (show a UI overlay) or remove the phrase from `supportedCommands`.

- **[CursorCommandHandler.kt:1, header]** `Author: VOS4 Development Team` on a file in `commandmanager/handlers/`. This violates Rule 7. Must be "Manoj Jhawar" or omitted.

- **[CursorCommandHandler.kt — architecture]** `CursorCommandHandler` lives in the legacy `commandmanager` sub-package and uses the old `CommandRegistry` (Android-only singleton, ConcurrentHashMap, string-based dispatch). It is NOT integrated with the `HandlerRegistry` (coroutine-Mutex, KMP, priority-ordered). It is also NOT listed in `AndroidHandlerFactory.createHandlers()`. As a result: cursor commands are routed through a completely separate, unordered dispatch system that runs in parallel with the main VoiceOS handler pipeline. There is no priority arbitration between the two systems. Command overlaps (e.g., "click", "long press") are resolved by whichever system processes the utterance first — which is undefined.

- **[StaticCommandRegistry.kt:9]** `Author: VOS4 Development Team` — Rule 7 violation.

- **[HandlerRegistry.kt:8]** `Author: VOS4 Development Team` — Rule 7 violation.

- **[ActionCategory.kt:8]** `Author: VOS4 Development Team` — Rule 7 violation.

---

## P2 Medium Issues

- **[AppControlHandler.kt:44-55]** `closeApp()` uses `GLOBAL_ACTION_BACK` + 300ms delay + `GLOBAL_ACTION_HOME`. This is a heuristic, not a real "close app" — it navigates out of the app but does not terminate it. On apps with back-stack management (e.g., activities in back stack), one BACK may not even leave the app. The user expectation of "close app" is app termination, not backgrounding. Consider `ActivityManager.killBackgroundProcesses()` (requires KILL_BACKGROUND_PROCESSES permission) for the app-specific case, or at minimum document the limitation as a comment.

- **[InputHandler.kt:73-77]** `postDelayed` on a `Handler(Looper.getMainLooper())` used inside a suspend function. The handler posts a 500ms callback that resets `SHOW_MODE_HIDDEN` back to `SHOW_MODE_AUTO`. If the service is destroyed within 500ms (e.g., user disables accessibility), the callback fires on a dead service, causing potential crashes or leaked state. Use `serviceScope.launch { delay(500); ... }` instead so the coroutine is tied to the service lifecycle.

- **[ReadingHandler.kt:95-127]** `extractScreenText()` uses recursive DFS capped at depth 15. For very large view hierarchies (e.g., complex RecyclerView nesting), this produces a flat, unstructured dump. Consider limiting total character count (e.g., 2000 chars) to avoid `tts.speak()` queue overflow and unnecessarily long TTS output.

- **[ReadingHandler.kt:37-46]** `initialize()` is a suspend function but the TTS callback is a non-suspend lambda. TTS readiness is checked in `readScreen()` via `ttsReady`, but there is a window between `initialize()` returning and TTS calling back — if `execute()` is called immediately, `ttsReady == false` and the user gets "not available". A proper approach: use a `CompletableDeferred<Boolean>` and await it in `execute()`.

- **[VoiceControlHandler.kt:29-61]** `VoiceControlCallbacks` is a global `object` with `@Volatile` lambdas. If the accessibility service crashes and restarts, the callbacks may point to stale closures from the previous service instance. The `clear()` method exists but is only called on `onDestroy` — a crash bypasses `onDestroy`. Consider using WeakReferences or a generation counter to detect stale callbacks.

- **[AndroidGestureHandler.kt:240-256]** Comment says `"swipe left" maps to scroll("right") intentionally` — this is correct and the comment explains it. However, the initial phrase-based routing block and the later `CommandActionType.SWIPE_LEFT` block implement the same mapping independently. If the mapping changes, it must be updated in two places. Extract the direction-inversion logic to a single function.

- **[BaseHandler.kt:144-148]** The gerund/inflection matching logic (`normalized.startsWith(supportedLower.dropLast(1)) && normalized.length > supportedLower.length`) is fragile. For a 4-char action like `"copy"`, `dropLast(1)` gives `"cop"`, and `"copper"` would match "copy". Any phrase starting with the 3-char prefix of a supported action (>4 chars) is accepted. This could cause false positives on voice input containing unrelated words beginning with those prefixes.

- **[CommandLoader.kt:76]** The `requiredVersion` is hardcoded as `"3.2"` inline. This requires a source code change every time commands are updated. A constants file or asset-embedded version would be more maintainable.

- **[ModuleCommandCallbacks.kt:119-127]** `activeModules()` reads 7 `@Volatile` fields non-atomically. Between reading `noteExecutor != null` and `cockpitExecutor != null`, another thread could null one of them. The list is best-effort (diagnostic), so this is acceptable, but should be documented as approximate.

---

## Command Dispatch Analysis

### Pipeline Overview
```
VOS Asset Files (.app.vos + .web.vos)
  └─> CommandLoader.initializeCommands()
        └─> VosParser.parse() [KMP]
              └─> VoiceCommandDaoAdapter.insertBatch() [SQLDelight]
                    └─> StaticCommandPersistenceImpl
                          └─> StaticCommandRegistry.initialize()
                                └─> HandlerRegistry.findHandler(QuantizedCommand)
                                      └─> Handler.execute()
```

### Strengths
1. **Atomic command replacement**: `ActionCoordinator.updateDynamicCommandsBySource()` uses source tagging so accessibility and web commands coexist without race-clearing.
2. **Priority-ordered dispatch**: `ActionCategory.PRIORITY_ORDER` ensures SYSTEM > NAVIGATION > ... > CUSTOM with deterministic precedence.
3. **Coroutine-safe HandlerRegistry**: Uses `kotlinx.coroutines.sync.Mutex` correctly — no `ReentrantLock` in suspend paths.
4. **Cached handler list**: `cachedHandlerList` snapshot eliminates per-call `flatten()` allocation on the hot dispatch path.
5. **Version-gated DB reload**: CommandLoader skips reload if `jsonVersion == requiredVersion && count > 0`. Avoids startup latency on subsequent launches.

### Weaknesses
1. **Dual registry systems**: `HandlerRegistry` (KMP, coroutine, priority) and legacy `CommandRegistry` (`commandmanager`, Android, ConcurrentHashMap) coexist. `CursorCommandHandler` uses the legacy one only. There is no coordination between them. Commands that match both handlers' patterns (e.g., "click") may be dispatched by whichever system processes the utterance, with no priority guarantee between systems.
2. **StaticCommandRegistry global mutable state**: `_dbCommands` is a `@Volatile` list reference. `initialize()` replaces the reference atomically, but `all()` readers between the old and new reference see a consistent (if briefly stale) list. The risk is low but a `CopyOnWriteArrayList` or immutable snapshot pattern would be more explicit.
3. **`findByPhraseInDomains` prioritization**: If two commands match the same phrase with different domains (one "app", one "web"), the non-"app" domain wins. This is correctly documented but the logic uses `find { it.domain != "app" }` which returns the first non-app match. If multiple non-app domains match the same phrase (e.g., "notes" and "cockpit" both register "scroll up"), only one wins non-deterministically.
4. **No fallback handler**: `HandlerRegistry.findHandler()` returns `null` if no handler matches. The caller must handle null gracefully. If not, the voice command silently drops with only a log warning. There is no dead-letter queue or user feedback path inside the registry itself.

---

## Accessibility Service Analysis

### Architecture
`VoiceOSAccessibilityService` is an `abstract class` — a good pattern that separates platform concerns (event reception, service lifecycle) from app-level logic (ActionCoordinator, overlays). The template-method pattern (`getActionCoordinator()`, `onServiceReady()`, `onCommandsUpdated()`, etc.) is clean and testable.

### Lifecycle
- **onServiceConnected**: Initializes `screenExtractor` and `gestureDispatcher`. Calls `onServiceReady()` on main thread. Correct.
- **onDestroy**: Cancels `serviceScope` with a try/finally. Correct; `isServiceReady = false` prevents processing after destroy.
- **onInterrupt**: Only logs. Acceptable — interrupts are transient accessibility events, not requiring cleanup.

### Thread Safety
- `serviceScope` uses `Dispatchers.Main + SupervisorJob()`. Correct: ensures child coroutines don't cancel the parent scope on failure.
- `handleScreenChange` dispatches to `Dispatchers.Default` for CPU-bound element extraction. Correct.
- `lastScreenHash` is a `var String` on the class, written from `Dispatchers.Default`. This is a data race — `lastScreenHash` is not `@Volatile`. On JVM, without `@Volatile`, the write may not be visible across threads. Add `@Volatile` annotation.
- `currentPackageName` is also a plain `var String?` written from `onAccessibilityEvent` (main thread) and read inside the debounce logic (also called from main thread via event). This is main-thread-only so it is safe as long as `onAccessibilityEvent` is always on main. Android guarantees this — acceptable.

### Event Handling
- Debounce logic correctly differentiates `TYPE_WINDOW_STATE_CHANGED` (immediate) from `TYPE_WINDOW_CONTENT_CHANGED` (debounced). Good.
- Scroll events schedule `onScrollSettled` before checking `getBoundsResolver() ?: return` — this avoids a previously reported bug where the scroll callback never fired.
- The `pendingScrollRefreshJob` and `pendingScreenChangeJob` are cancelled correctly before scheduling new ones.

### Error Recovery
- `handleScreenChange` wraps extraction in try/catch and logs. No retry logic, but this is appropriate — accessibility events are continuous, so missing one is not catastrophic.
- `processVoiceCommand` propagates `HandlerResult` variants but the `onCommandExecuted` callback always receives a synthetic command (P0 issue).

---

## Code Smells

- **Duplicate phrase blocks in AndroidGestureHandler**: Scroll direction mapping implemented twice (phrase-based block L236-260 and ActionType-based block L437-694). Single source of truth needed.
- **`Author: VOS4 Development Team` in multiple files**: StaticCommandRegistry.kt, HandlerRegistry.kt, ActionCategory.kt, CursorCommandHandler.kt — violates Rule 7. 4 occurrences.
- **`CommandRegistry` (legacy) vs `HandlerRegistry` (current)**: Two parallel dispatch systems with overlapping command surface. The legacy one has no priority ordering, no coroutine safety, and is only used by `CursorCommandHandler`. This is dead weight that creates undefined behavior on overlapping commands.
- **CursorCommandHandler `SimpleCommandRouter`**: An interface + private class defined at the bottom of CursorCommandHandler.kt (`CommandRouter`, `SimpleCommandRouter`) that is only used within the same file. These are unnecessary indirection layers (Rule 2) — removing them would simplify the file.
- **MediaHandler volume on STREAM_MUSIC only**: Volume up/down only adjusts media stream. Users saying "volume up" while on a phone call, alarm clock, or notification context will get unexpected behavior. Should either route to the active stream or document the limitation.
- **`VoiceControlCallbacks` + `ModuleCommandCallbacks` as global objects**: Pattern is pragmatic for a single-process accessibility service, but both objects need careful lifecycle discipline. `clearAll()` / `clear()` methods exist but there is no compile-time enforcement that they are called. Consider using a structured dependency injection approach or at minimum a lifecycle-aware wrapper.
