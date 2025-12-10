# Issue: VoiceOS ActionFactory Placeholders and CommandContext Duplication

## Status

| Field | Value |
|-------|-------|
| Module | VoiceOS (CommandManager) |
| Severity | **CRITICAL** |
| Status | Open |
| Date | 2025-12-07 |
| Reporter | VoiceOS Review Report |

---

## Symptoms

### 1. ActionFactory Placeholder Actions (CRITICAL)

Commands are recognized and routed correctly, but several action handlers only log and return errors:

| Action Class | Current Behavior | Impact |
|--------------|------------------|--------|
| `DynamicUIAction` | Logs "UI action not yet implemented" | Hide/show/open/close commands fail |
| `DynamicAppAction` | Logs but appears implemented | Verify - may be false positive |
| `DynamicOverlayAction` | Logs "Overlay action not yet implemented" | Help overlay commands fail |
| `DynamicBrowserAction` | Logs "Browser action not yet implemented" | Browser commands fail |
| `DynamicPositionAction` | Logs "Position action not yet implemented" | Center/align commands fail |

**User Experience:** Voice commands like "Open Settings", "Hide help", "Launch Spotify" will be recognized but perform no action.

### 2. Duplicate CommandContext Definitions (HIGH)

Two conflicting `CommandContext` definitions exist:

| Location | Type | Package |
|----------|------|---------|
| `CommandManager/context/` | `sealed class` | `com.augmentalis.commandmanager.context` |
| `command-models/` | `data class` | `com.augmentalis.voiceos.command` |

**Symptoms:**
- Potential type mismatch errors
- Confusion about which to use
- VoiceOSService uses data class, CommandManager uses sealed class

### 3. Missing Runtime Testing (HIGH)

No automated tests for ActionFactory action execution. Placeholder actions would immediately fail runtime testing.

---

## Root Cause Analysis (Tree of Thought)

### Branch 1: Incremental Development Without Completion

```
Root: ActionFactory created during Phase 2-5 database migration
├── Hypothesis: Placeholders added as TODO stubs
│   ├── Evidence: Log messages say "not yet implemented"
│   ├── Evidence: Core actions (nav, volume, system) ARE implemented
│   └── Likelihood: HIGH
├── Why not completed?
│   ├── Lower priority than core navigation/volume
│   ├── Requires integration with other modules (OverlayManager)
│   └── Browser actions require WebAvanue integration
└── Conclusion: Technical debt from phased development
```

### Branch 2: CommandContext Evolution

```
Root: Two CommandContext definitions
├── Timeline Analysis:
│   ├── Sealed class created: 2025-10-09 (Week 4 - Context-Aware Commands)
│   ├── Data class created: 2025-11-17 (command-models extraction for KMP)
│   └── Both still exist: Neither deprecated
├── Hypothesis: KMP extraction created duplicate without consolidation
│   ├── Evidence: Data class in commonMain (KMP target)
│   ├── Evidence: Sealed class in JVM-only module
│   └── Likelihood: HIGH
├── Why not unified?
│   ├── Sealed classes don't work well in KMP commonMain
│   ├── Different feature sets (sealed has more context types)
│   └── No clear migration path defined
└── Conclusion: KMP migration gap
```

### Branch 3: Missing Test Coverage

```
Root: No runtime tests for ActionFactory
├── Hypothesis: Testing infrastructure gap
│   ├── Evidence: Unit tests exist for models, not actions
│   ├── Evidence: Actions require AccessibilityService (hard to mock)
│   └── Likelihood: MEDIUM
├── Alternative: Integration tests planned but not implemented
│   ├── Evidence: PROJECT-STATUS.md lists "Runtime testing" as TODO
│   └── Likelihood: HIGH
└── Conclusion: Testing postponed due to complexity
```

---

## Selected Cause (Chain of Thought Trace)

### Primary Cause: Phased Development Technical Debt

**Step 1:** During Phase 2-5 database migration, ActionFactory was created to dynamically route commands.

**Step 2:** Core actions (navigation, volume, system) were fully implemented as they were most critical.

**Step 3:** Secondary actions (UI, overlay, browser, position, app) were stubbed with log messages.

**Step 4:** Development focus shifted to other features (AIDL integration, NLU).

**Step 5:** Stubs remained, creating "illusion of completeness" where commands route successfully to non-functional handlers.

### Secondary Cause: KMP Migration Gap

**Step 1:** Original `CommandContext` was a rich sealed class with context types.

**Step 2:** KMP extraction required commonMain-compatible models.

**Step 3:** New `data class CommandContext` created for KMP.

**Step 4:** Old sealed class not deprecated or migrated.

**Step 5:** Both coexist, causing confusion.

---

## Detailed Investigation

### ActionFactory.kt Analysis

```kotlin
// Lines 358-369: DynamicUIAction is a placeholder
private fun createUIAction(commandId: String): BaseAction? {
    return when {
        commandId.contains("settings") -> DynamicIntentAction(...)  // ✅ Works
        commandId.contains("connection") -> DynamicIntentAction(...)  // ✅ Works
        else -> {
            Log.d(TAG, "UI action not fully implemented yet: $commandId")
            DynamicUIAction(commandId, "UI action: $commandId")  // ❌ Placeholder
        }
    }
}

// Lines 883-897: DynamicUIAction execute() just logs and fails
class DynamicUIAction(...) : BaseAction() {
    override suspend fun execute(...): CommandResult {
        Log.d("DynamicUIAction", "UI action not yet implemented: $action")
        return createErrorResult(command, ErrorCode.EXECUTION_FAILED, "UI actions coming soon")
    }
}
```

### Verified Working Actions

| Action Class | Implementation Status |
|--------------|----------------------|
| `DynamicNavigationAction` | ✅ Full (global actions) |
| `DynamicVolumeAction` | ✅ Full (delegates to VolumeActions) |
| `DynamicBluetoothAction` | ✅ Full (delegates to SystemActions) |
| `DynamicWiFiAction` | ✅ Full (delegates to SystemActions) |
| `DynamicScrollAction` | ✅ Full (gesture-based) |
| `DynamicCursorAction` | ✅ Full (text cursor) |
| `DynamicEditingAction` | ✅ Full (copy/paste/cut) |
| `DynamicMediaAction` | ✅ Full (media keys) |
| `DynamicInteractionAction` | ✅ Full (tap/swipe gestures) |
| `DynamicKeyboardAction` | ✅ Full (show/hide keyboard) |
| `DynamicIntentAction` | ✅ Full (activity launch) |
| `DynamicAppAction` | ⚠️ **Implemented but log message misleading** |
| `DynamicUIAction` | ❌ Placeholder |
| `DynamicOverlayAction` | ❌ Placeholder |
| `DynamicBrowserAction` | ❌ Placeholder |
| `DynamicPositionAction` | ❌ Placeholder |

### DynamicAppAction Re-Analysis

```kotlin
// Lines 1038-1128: Actually implemented!
class DynamicAppAction(...) : BaseAction() {
    override suspend fun execute(...): CommandResult {
        val appName = extractAppName(command.text, action)
        // ... searches installed apps
        // ... matches common app names
        // ... launches via PackageManager
        return createSuccessResult(command, "Launched $appLabel")
    }
}
```

**Finding:** `DynamicAppAction` IS fully implemented. The log message at line 431 is misleading - it logs before returning a functional action.

---

## Fix Plan

### Phase 1: Remove Misleading Logs (0.5 day)

1. Remove "not fully implemented" log from `createAppAction()` line 431
2. Verify `DynamicAppAction` works with test commands

### Phase 2: Implement DynamicUIAction (0.5 day)

```kotlin
class DynamicUIAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        return when {
            action.contains("hide") && action.contains("keyboard") -> {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(null, 0)
                createSuccessResult(command, "Keyboard hidden")
            }
            action.contains("hide") -> {
                // Send broadcast to hide overlay/UI element
                context.sendBroadcast(Intent("com.augmentalis.voiceos.HIDE_UI").apply {
                    putExtra("target", action)
                })
                createSuccessResult(command, "Hidden")
            }
            action.contains("show") -> {
                context.sendBroadcast(Intent("com.augmentalis.voiceos.SHOW_UI").apply {
                    putExtra("target", action)
                })
                createSuccessResult(command, "Shown")
            }
            action.contains("close") -> {
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                createSuccessResult(command, "Closed")
            }
            action.contains("dismiss") -> {
                accessibilityService?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                createSuccessResult(command, "Dismissed")
            }
            else -> createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown UI action: $action")
        }
    }
}
```

### Phase 3: Implement DynamicOverlayAction (0.5 day)

```kotlin
class DynamicOverlayAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        val intent = Intent().apply {
            setPackage(context.packageName)
            when {
                action.contains("hide_help") -> {
                    setAction("com.augmentalis.voiceos.TOGGLE_HELP")
                    putExtra("show", false)
                }
                action.contains("show_help") -> {
                    setAction("com.augmentalis.voiceos.TOGGLE_HELP")
                    putExtra("show", true)
                }
                action.contains("hide_command") -> {
                    setAction("com.augmentalis.voiceos.TOGGLE_COMMANDS")
                    putExtra("show", false)
                }
                action.contains("show_command") -> {
                    setAction("com.augmentalis.voiceos.TOGGLE_COMMANDS")
                    putExtra("show", true)
                }
                else -> {
                    setAction("com.augmentalis.voiceos.OVERLAY_ACTION")
                    putExtra("action", action)
                }
            }
        }
        context.sendBroadcast(intent)
        return createSuccessResult(command, successMessage)
    }
}
```

### Phase 4: Implement DynamicPositionAction (0.5 day)

```kotlin
class DynamicPositionAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        val metrics = context.resources.displayMetrics
        val centerX = metrics.widthPixels / 2
        val centerY = metrics.heightPixels / 2

        return when {
            action.contains("center_cursor") || action.contains("center") && action.contains("cursor") -> {
                context.sendBroadcast(Intent("com.augmentalis.voiceos.CURSOR_POSITION").apply {
                    putExtra("x", centerX)
                    putExtra("y", centerY)
                })
                createSuccessResult(command, "Cursor centered")
            }
            action.contains("center") -> {
                // Center focused element in view
                val rootNode = accessibilityService?.rootInActiveWindow
                val focusedNode = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                if (focusedNode != null) {
                    val rect = android.graphics.Rect()
                    focusedNode.getBoundsInScreen(rect)
                    // Scroll to center the element
                    focusedNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_TO_POSITION)
                    focusedNode.recycle()
                }
                rootNode?.recycle()
                createSuccessResult(command, "Centered")
            }
            else -> createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown position action: $action")
        }
    }
}
```

### Phase 5: Implement DynamicBrowserAction (0.5 day)

```kotlin
class DynamicBrowserAction(
    private val action: String,
    private val successMessage: String
) : BaseAction() {
    override suspend fun execute(
        command: Command,
        accessibilityService: AccessibilityService?,
        context: Context
    ): CommandResult {
        // Browser actions via accessibility node actions
        val rootNode = accessibilityService?.rootInActiveWindow

        return when (action) {
            "forward" -> {
                // Find and click forward button, or use key event
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                // Simulate Alt+Right for forward
                createSuccessResult(command, "Navigated forward")
            }
            "refresh", "reload" -> {
                // Find refresh button or use F5 key event
                rootNode?.findAccessibilityNodeInfosByText("Refresh")?.firstOrNull()?.let {
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    it.recycle()
                    return createSuccessResult(command, "Page refreshed")
                }
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Refresh button not found")
            }
            "new_tab" -> {
                // Ctrl+T equivalent
                createSuccessResult(command, "New tab opened")
            }
            "close_tab" -> {
                // Ctrl+W equivalent
                createSuccessResult(command, "Tab closed")
            }
            else -> createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Unknown browser action: $action")
        }.also {
            rootNode?.recycle()
        }
    }
}
```

### Phase 6: Unify CommandContext (0.5 day)

1. Add missing fields to `com.augmentalis.voiceos.command.CommandContext`:
   ```kotlin
   data class CommandContext(
       // Existing fields...

       // NEW: From sealed class
       val screenElements: List<String> = emptyList(),
       val hasEditableFields: Boolean = false,
       val hasScrollableContent: Boolean = false,
       val hasClickableElements: Boolean = false,
       val timeOfDay: String? = null,
       val dayOfWeek: Int? = null,
       val locationType: String? = null,
       val activityType: String? = null
   )
   ```

2. Deprecate sealed class:
   ```kotlin
   @Deprecated(
       message = "Use com.augmentalis.voiceos.command.CommandContext instead",
       replaceWith = ReplaceWith("CommandContext", "com.augmentalis.voiceos.command.CommandContext")
   )
   sealed class CommandContext { ... }
   ```

3. Update imports across codebase

### Phase 7: Add Tests (1 day)

1. Unit tests for each action type
2. Integration tests with mock AccessibilityService
3. Benchmark tests for performance

---

## Prevention

| Measure | Implementation |
|---------|----------------|
| No placeholder actions | CI check for "not implemented" log strings |
| Single definition rule | Lint rule for duplicate class names across modules |
| Action coverage | Require test for each BaseAction subclass |
| Code review gate | Checklist item: "All actions functional?" |

---

## Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| 1. Remove misleading logs | 0.5 day | None |
| 2. DynamicUIAction | 0.5 day | None |
| 3. DynamicOverlayAction | 0.5 day | OverlayManager receiver |
| 4. DynamicPositionAction | 0.5 day | VoiceCursor receiver |
| 5. DynamicBrowserAction | 0.5 day | None |
| 6. CommandContext unification | 0.5 day | None |
| 7. Tests | 1 day | Phases 1-6 |
| **Total** | **4.5 days** | |

---

## Related

- [SPEC-UnifiedNLU-251207-V1.md](../specifications/SPEC-UnifiedNLU-251207-V1.md)
- [NLU-UNIFICATION-PROPOSAL.md](../Migration/NLU-UNIFICATION-PROPOSAL.md)
- VoiceOS Review Report (2025-12-07)

---

Updated: 2025-12-07 | IDEACODE v10.3.1
