# TEMPLATE: Module Migration Instructions
**Created:** 2025-11-03 11:50 PST
**Project:** Avanues
**Purpose:** Reusable instructions for migrating Avanue4 modules to Avanues
**Framework:** IDEACODE v5.3

---

## 📋 How to Use This Template

**When migrating any module from Avanue4 to Avanues, you will say:**

```
Migrate the [MODULE_NAME] module from Avanue4 to Avanues using the established pattern.

Reference: browseravanue migration (completed)
Source: /Volumes/M-Drive/Coding/Warp/Avanue4/modules/[module_name]/
Target: /Volumes/M-Drive/Coding/avanues/android/apps/[module_name]avenue/
```

**Then I will:**
1. Follow the exact same process we established for browser
2. Use the same architecture patterns
3. Apply the same Compose + IDEAMagic abstraction layer
4. Follow the same 7-phase implementation plan
5. Maintain the same quality standards (80%+ test coverage)

---

## 🎯 Standard Migration Pattern (Established by Browser)

### Architecture: Clean Architecture (3 Layers)

```
android/apps/[module]avenue/
├── shared/                         # Kotlin Multiplatform Common
│   ├── core/
│   │   ├── [Module]Result.kt       # Type-safe error handling
│   │   └── [Module]Error.kt
│   │
│   ├── domain/                     # Pure Kotlin Business Logic
│   │   ├── model/                  # Domain models (no Room)
│   │   ├── repository/             # Interface
│   │   └── usecase/                # SRP Use Cases
│   │
│   └── data/                       # Data Layer
│       ├── local/
│       │   ├── entity/             # Room entities
│       │   ├── dao/                # Room DAOs
│       │   └── [Module]Database.kt
│       ├── mapper/                 # Entity ↔ Domain
│       └── repository/             # Implementation
│
├── android/                        # Android-Specific
│   ├── presentation/
│   │   ├── [Module]ViewModel.kt    # StateFlow + Events
│   │   ├── [Module]State.kt
│   │   └── [Module]Event.kt
│   │
│   ├── ui/
│   │   ├── [Module]Screen.kt       # Main Compose UI
│   │   ├── components/             # UI Components
│   │   ├── abstraction/
│   │   │   └── AvaUIComponents.kt # IDEAMagic abstraction
│   │   └── theme/                  # Material 3
│   │
│   ├── di/
│   │   └── [Module]Dependencies.kt # Manual DI
│   │
│   ├── integration/
│   │   ├── VoiceOSBridge.kt        # VoiceOSCore integration
│   │   └── IPCBridge.kt            # IDEAMagic IPC
│   │
│   └── [module-specific]/          # Module-specific code
│       └── (e.g., webview/, voice/)
│
├── ios/                            # iOS-Specific (Future)
├── tests/                          # 80%+ coverage
└── docs/                           # Documentation
```

---

## 🎨 UI Strategy: Compose + IDEAMagic Abstraction Layer

### What is "IDEAMagic Abstraction Layer"?

**Problem:**
- IDEAMagic Foundation components are still in development
- We need to build NOW with Compose
- We want easy migration to IDEAMagic later

**Solution: Thin Wrapper Functions**

Create a single file `AvaUIComponents.kt` that wraps Compose components with a unified API. When IDEAMagic is ready, change the implementation in ONE file, and all UI code automatically uses IDEAMagic.

### How It Works

**File:** `android/ui/abstraction/AvaUIComponents.kt`

```kotlin
package com.augmentalis.[module]avenue.ui.abstraction

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * IDEAMagic Abstraction Layer
 *
 * STRATEGY:
 * - TODAY: Implementations use Compose (androidx.compose.*)
 * - TOMORROW: Change implementations to IDEAMagic (avamagic.foundation.*)
 * - UI CODE: Never changes, just uses these wrapper functions
 */

// ==============================================================================
// BUTTON WRAPPER
// ==============================================================================

@Composable
fun MagicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Filled
) {
    // TODAY (Compose Implementation):
    when (variant) {
        ButtonVariant.Filled -> {
            androidx.compose.material3.Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                androidx.compose.material3.Text(text)
            }
        }
        ButtonVariant.Outlined -> {
            androidx.compose.material3.OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                androidx.compose.material3.Text(text)
            }
        }
        // ... other variants
    }

    // TOMORROW (IDEAMagic Implementation):
    // Just change the implementation above to:
    // avamagic.foundation.MagicButton(
    //     text = text,
    //     onClick = onClick,
    //     modifier = modifier,
    //     enabled = enabled,
    //     variant = variant.toMagicVariant()
    // )
    //
    // That's it! All UI code using MagicButton() now uses IDEAMagic.
}

enum class ButtonVariant {
    Filled, Outlined, Text
}

// ==============================================================================
// TEXT WRAPPER
// ==============================================================================

@Composable
fun MagicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Body
) {
    // TODAY: Compose implementation
    val composeStyle = when (style) {
        TextStyle.Title -> androidx.compose.material3.MaterialTheme.typography.titleLarge
        TextStyle.Body -> androidx.compose.material3.MaterialTheme.typography.bodyMedium
        // ... other styles
    }

    androidx.compose.material3.Text(
        text = text,
        modifier = modifier,
        style = composeStyle
    )

    // TOMORROW: Change to IDEAMagic
    // avamagic.foundation.MagicText(
    //     text = text,
    //     modifier = modifier,
    //     preset = style.toTextPreset()
    // )
}

enum class TextStyle {
    Title, Body, Caption
}

// ==============================================================================
// CARD WRAPPER
// ==============================================================================

@Composable
fun MagicCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Elevated,
    content: @Composable () -> Unit
) {
    // TODAY: Compose implementation
    when (variant) {
        CardVariant.Elevated -> {
            androidx.compose.material3.ElevatedCard(modifier = modifier) {
                content()
            }
        }
        // ... other variants
    }

    // TOMORROW: Change to IDEAMagic
    // avamagic.foundation.MagicCard(
    //     modifier = modifier,
    //     variant = variant,
    //     content = content
    // )
}

enum class CardVariant {
    Elevated, Filled, Outlined
}

// ==============================================================================
// TEXT FIELD WRAPPER
// ==============================================================================

@Composable
fun MagicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null
) {
    // TODAY: Compose implementation
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { androidx.compose.material3.Text(it) } },
        placeholder = placeholder?.let { { androidx.compose.material3.Text(it) } }
    )

    // TOMORROW: Change to IDEAMagic
    // avamagic.foundation.MagicTextField(
    //     state = rememberMagicState(value),
    //     onValueChange = onValueChange,
    //     modifier = modifier,
    //     label = label,
    //     placeholder = placeholder
    // )
}

// Add more wrappers as needed: MagicIcon, MagicColumn, MagicRow, etc.
```

### How UI Code Uses It

**Your Module's Screen:** `[Module]Screen.kt`

```kotlin
package com.augmentalis.[module]avenue.ui

// Import the abstraction layer
import com.augmentalis.[module]avenue.ui.abstraction.*

@Composable
fun [Module]Screen(
    viewModel: [Module]ViewModel
) {
    val state by viewModel.state.collectAsState()

    MagicColumn {  // ← Uses abstraction layer
        MagicCard(variant = CardVariant.Elevated) {  // ← Uses abstraction
            MagicText(
                text = "Welcome to ${state.title}",
                style = TextStyle.Title
            )
        }

        MagicButton(
            text = "Click Me",
            onClick = { viewModel.onEvent([Module]Event.ButtonClicked) },
            variant = ButtonVariant.Filled
        )

        MagicTextField(
            value = state.inputText,
            onValueChange = { viewModel.onEvent([Module]Event.TextChanged(it)) },
            label = "Enter text"
        )
    }
}
```

### Benefits of This Approach

1. **Build NOW:** Use Compose immediately, don't wait for IDEAMagic
2. **Easy Migration:** When IDEAMagic ready, change ONE file (`AvaUIComponents.kt`)
3. **No UI Changes:** All screens (`[Module]Screen.kt`) remain unchanged
4. **Type Safety:** Abstraction catches incompatibilities early
5. **Consistent API:** Same function names/parameters across all modules
6. **Gradual Transition:** Can migrate component-by-component if needed

### Migration Timeline

**Phase 1 (NOW - Week 1-11):**
- Build all modules using `AvaUIComponents.kt` abstraction
- Implementations use Compose internally
- All modules have consistent API

**Phase 2 (LATER - When IDEAMagic Ready):**
- Update each module's `AvaUIComponents.kt` (1 hour per module)
- Change implementations from Compose to IDEAMagic
- Recompile and test
- No changes to any screen/component code

**Total Migration Effort:** ~20 hours for all 21 modules (1 file change per module)

---

## 🔗 IPC Communication Between Modules (IDEAMagic)

### Problem
Modules need to communicate with each other:
- Browser needs to open files from FileManager
- Notepad needs to save to FileManager
- Task needs to create reminders in Calendar
- VoiceOS needs to send commands to all modules

### Solution: IDEAMagic IPC System

**Architecture:**

```
┌─────────────────────────────────────────────────────────────┐
│                    IDEAMagic IPC Bus                        │
│                  (Message Broker)                           │
└─────────────────────────────────────────────────────────────┘
          ↑                    ↑                    ↑
          │                    │                    │
    ┌─────┴─────┐        ┌─────┴─────┐        ┌─────┴─────┐
    │ Browser   │        │ Notepad   │        │FileManager│
    │ Module    │        │ Module    │        │  Module   │
    └───────────┘        └───────────┘        └───────────┘
```

### IPC Implementation

**File:** `android/integration/IPCBridge.kt` (Each Module)

```kotlin
package com.augmentalis.[module]avenue.integration

import com.augmentalis.avamagic.ipc.IPCClient
import com.augmentalis.avamagic.ipc.IPCMessage
import kotlinx.coroutines.flow.Flow

/**
 * IDEAMagic IPC Bridge for [Module]
 *
 * Handles inter-module communication using IDEAMagic IPC system
 */
class IPCBridge(
    private val ipcClient: IPCClient
) {

    /**
     * Register this module with IPC Bus
     */
    fun initialize() {
        ipcClient.registerModule(
            moduleId = "[module]avenue",
            capabilities = listOf(
                // What this module can do for others
                "capability.view",
                "capability.edit",
                "capability.share"
            )
        )

        // Listen for messages FROM other modules
        ipcClient.subscribe("[module]avenue") { message ->
            handleIncomingMessage(message)
        }
    }

    /**
     * Send message TO another module
     */
    suspend fun sendMessage(
        targetModule: String,
        action: String,
        data: Map<String, Any>
    ): IPCResult {
        val message = IPCMessage(
            from = "[module]avenue",
            to = targetModule,
            action = action,
            data = data,
            timestamp = System.currentTimeMillis()
        )

        return ipcClient.send(message)
    }

    /**
     * Request data FROM another module
     */
    suspend fun request(
        targetModule: String,
        request: String,
        params: Map<String, Any> = emptyMap()
    ): IPCResult {
        return sendMessage(
            targetModule = targetModule,
            action = "request.$request",
            data = params
        )
    }

    /**
     * Observe messages from other modules
     */
    fun observeMessages(): Flow<IPCMessage> {
        return ipcClient.observe("[module]avenue")
    }

    /**
     * Handle incoming messages
     */
    private fun handleIncomingMessage(message: IPCMessage) {
        when (message.action) {
            "request.view" -> handleViewRequest(message)
            "request.edit" -> handleEditRequest(message)
            "command.execute" -> handleCommand(message)
            else -> handleUnknownAction(message)
        }
    }

    private fun handleViewRequest(message: IPCMessage) {
        // Handle view request from another module
        val itemId = message.data["itemId"] as? String
        // ... process request
    }

    private fun handleEditRequest(message: IPCMessage) {
        // Handle edit request
    }

    private fun handleCommand(message: IPCMessage) {
        // Handle command from VoiceOS or other module
    }

    private fun handleUnknownAction(message: IPCMessage) {
        // Log unknown action
    }

    fun destroy() {
        ipcClient.unregisterModule("[module]avenue")
    }
}

sealed class IPCResult {
    data class Success(val data: Map<String, Any>) : IPCResult()
    data class Error(val message: String) : IPCResult()
    object Timeout : IPCResult()
}
```

### IPC Usage Examples

**Example 1: Browser Opens File from FileManager**

```kotlin
// In BrowserViewModel.kt
class BrowserViewModel(
    private val ipcBridge: IPCBridge
) : ViewModel() {

    fun onEvent(event: BrowserEvent) {
        when (event) {
            is BrowserEvent.OpenLocalFile -> {
                viewModelScope.launch {
                    // Request file from FileManager
                    val result = ipcBridge.request(
                        targetModule = "filemanageravenue",
                        request = "selectFile",
                        params = mapOf(
                            "mimeType" to "text/html",
                            "allowMultiple" to false
                        )
                    )

                    when (result) {
                        is IPCResult.Success -> {
                            val filePath = result.data["filePath"] as? String
                            filePath?.let { loadLocalFile(it) }
                        }
                        is IPCResult.Error -> {
                            showError("Failed to open file: ${result.message}")
                        }
                        IPCResult.Timeout -> {
                            showError("File selection timed out")
                        }
                    }
                }
            }
        }
    }
}
```

**Example 2: Notepad Saves to FileManager**

```kotlin
// In NotepadViewModel.kt
fun saveNote() {
    viewModelScope.launch {
        val result = ipcBridge.sendMessage(
            targetModule = "filemanageravenue",
            action = "saveFile",
            data = mapOf(
                "fileName" to "note.txt",
                "content" to state.value.noteText,
                "directory" to "Documents/Notes"
            )
        )

        when (result) {
            is IPCResult.Success -> {
                val savedPath = result.data["path"] as? String
                showSuccess("Note saved to $savedPath")
            }
            is IPCResult.Error -> {
                showError("Save failed: ${result.message}")
            }
            IPCResult.Timeout -> {
                showError("Save timed out")
            }
        }
    }
}
```

**Example 3: VoiceOS Broadcasts Command to All Modules**

```kotlin
// In VoiceOSCore
fun processGlobalCommand(command: String) {
    when (command) {
        "go home" -> {
            // Broadcast to all modules
            ipcBus.broadcast(
                action = "command.goHome",
                data = emptyMap()
            )
        }
        "refresh all" -> {
            ipcBus.broadcast(
                action = "command.refresh",
                data = emptyMap()
            )
        }
    }
}
```

**Example 4: Module Listens for Global Events**

```kotlin
// In any ViewModel
init {
    // Listen for IPC messages
    viewModelScope.launch {
        ipcBridge.observeMessages().collect { message ->
            when (message.action) {
                "command.goHome" -> navigateToHome()
                "command.refresh" -> refreshData()
                "event.themeChanged" -> applyNewTheme()
                "event.settingsUpdated" -> reloadSettings()
            }
        }
    }
}
```

### IPC Message Types

**1. Request/Response (Synchronous)**
```kotlin
// Request data and wait for response
val result = ipcBridge.request(
    targetModule = "calendar",
    request = "getEvents",
    params = mapOf("date" to "2025-11-03")
)
```

**2. Send Message (Asynchronous)**
```kotlin
// Send command, don't wait
ipcBridge.sendMessage(
    targetModule = "notification",
    action = "show",
    data = mapOf("title" to "Alert", "message" to "Task completed")
)
```

**3. Broadcast (All Modules)**
```kotlin
// Broadcast to everyone
ipcBus.broadcast(
    action = "event.networkStatusChanged",
    data = mapOf("isConnected" to true)
)
```

**4. Observe (Subscribe)**
```kotlin
// Listen for specific events
ipcBridge.observeMessages()
    .filter { it.action.startsWith("event.") }
    .collect { event -> handleEvent(event) }
```

### IPC Standard Actions

**Common Actions (All Modules Should Support):**
- `command.execute` - Execute a command
- `command.goHome` - Navigate to home screen
- `command.refresh` - Refresh data
- `request.getState` - Get current module state
- `request.getCapabilities` - Get module capabilities
- `event.stateChanged` - Module state changed
- `event.error` - Module error occurred

**Module-Specific Actions:**
- Browser: `action.navigate`, `action.openTab`
- Notepad: `action.createNote`, `action.saveNote`
- FileManager: `action.selectFile`, `action.saveFile`
- Task: `action.createTask`, `action.completeTask`

---

## 📋 Standard 7-Phase Implementation Plan

### Phase 1: Foundation (4 hours)
1. Create directory structure
2. Set up Room database (entities, DAOs, database)
3. Create domain models (pure Kotlin)
4. Create UseCases (SRP)
5. Create Repository interface

**Deliverable:** Complete `shared/` directory

### Phase 2: Module-Specific Integration (2 hours)
1. Port critical module-specific code from original
2. Update package names
3. Test module-specific functionality

**Deliverable:** Core module features working

### Phase 3: Voice Commands (2 hours)
1. Port/create VoiceCommandProcessor
2. Integrate with VoiceOSCore
3. Test all voice commands

**Deliverable:** Voice integration complete

### Phase 4: Presentation Layer (3 hours)
1. Create ViewModel (StateFlow + Events)
2. Create State and Event sealed classes
3. Wire up UseCases
4. Implement event handling

**Deliverable:** Complete state management

### Phase 5: UI Components (4 hours)
1. Create `AvaUIComponents.kt` abstraction layer
2. Build main screen using abstraction
3. Build component screens
4. Port any missing UI components

**Deliverable:** Complete Compose UI with IDEAMagic abstraction

### Phase 6: IPC Integration (2 hours)
1. Create `IPCBridge.kt`
2. Register module with IPC Bus
3. Implement message handlers
4. Test inter-module communication

**Deliverable:** IPC communication working

### Phase 7: Testing (4 hours)
1. Write domain model tests
2. Write UseCase tests
3. Write Repository integration tests
4. Write ViewModel tests

**Deliverable:** 80%+ test coverage

**Total:** ~21 hours per module

---

## 🚨 Zero-Tolerance Compliance Checklist

**Before ANY code:**
- [ ] Read REGISTRY.md
- [ ] Check docs/context/ for previous work
- [ ] Run context save
- [ ] Check ALL project registries for duplicates
- [ ] Create TodoWrite task list
- [ ] Create SPEC document

**During implementation:**
- [ ] NO deletions without approval
- [ ] NO AI references in commits
- [ ] 100% functional equivalency
- [ ] Documentation updated WITH code
- [ ] Follow directory structure

**Commit strategy:**
- [ ] Commit 1: Documentation (SPEC + PLAN)
- [ ] Commit 2: Foundation (shared/ directory)
- [ ] Commit 3: Module-specific integration
- [ ] Commit 4: Voice commands
- [ ] Commit 5: UI components + IPC
- [ ] Commit 6: Tests

---

## 📝 How to Instruct Me for Next Module

### Option 1: Simple Command

```
Migrate the Notepad module using the browser pattern.
```

I will:
1. Use same architecture (Clean Architecture, 3 layers)
2. Use same UI approach (Compose + IDEAMagic abstraction)
3. Use same IPC integration
4. Follow same 7-phase plan
5. Achieve 80%+ test coverage
6. Follow IDEACODE v5.3 protocols

### Option 2: With Customization

```
Migrate the FileManager module using the browser pattern.

Customizations:
- Phase 2: Port file browsing tree view
- Phase 5: Add thumbnail previews for images
- IPC: Support file selection and save dialogs
```

### Option 3: Multiple Modules

```
Migrate these modules in order, using the browser pattern:
1. Notepad (text editing)
2. Task (task management)
3. FileManager (file browsing)

Work sequentially, one module at a time.
```

---

## 🎯 Success Criteria (Standard for All Modules)

### Functionality
- ✅ 100% feature parity with original Avanue4 module
- ✅ All voice commands working
- ✅ All UI components functional
- ✅ Module-specific features complete

### Architecture
- ✅ Clean Architecture (3 layers)
- ✅ SOLID principles (SRP UseCases)
- ✅ Room Database
- ✅ Type-safe errors (Result monad)
- ✅ Event-driven (sealed class events)
- ✅ Material 3 design system

### Integration
- ✅ VoiceOSCore integration
- ✅ IDEAMagic IPC communication
- ✅ Compose + IDEAMagic abstraction layer

### Testing
- ✅ 80%+ test coverage
- ✅ Domain layer: 95%+
- ✅ Data layer: 80%+
- ✅ Presentation layer: 80%+

### IDEACODE Compliance
- ✅ All 20 zero-tolerance rules followed
- ✅ Professional commits (no AI references)
- ✅ Documentation before code
- ✅ Context saved properly

---

## 📊 Module Migration Checklist

**Before Starting:**
- [ ] Read this template completely
- [ ] Understand Compose + IDEAMagic abstraction approach
- [ ] Understand IPC communication pattern
- [ ] Review browser module as reference

**During Migration:**
- [ ] Follow 7-phase implementation plan
- [ ] Create `AvaUIComponents.kt` abstraction layer
- [ ] Create `IPCBridge.kt` for inter-module communication
- [ ] Maintain 80%+ test coverage
- [ ] Follow zero-tolerance protocols

**After Completion:**
- [ ] All tests passing
- [ ] IPC communication tested
- [ ] Voice commands working
- [ ] Documentation complete
- [ ] Module registered in REGISTRY.md

---

## 🔗 Reference Files

**Browser Module (Completed Example):**
- `/Volumes/M-Drive/Coding/avanues/android/apps/browseravanue/`
- `docs/browseravanue/SPEC-*.md`
- `docs/browseravanue/PLAN-*.md`

**IDEAMagic Foundation Components:**
- `/Volumes/M-Drive/Coding/avanues/Universal/IDEAMagic/Components/Foundation/`
- 15 production-ready Compose components available NOW

**IDEACODE v5.3 Protocols:**
- `/Volumes/M-Drive/Coding/ideacode/protocols/Protocol-Zero-Tolerance-Pre-Code.md`
- `/Volumes/M-Drive/Coding/ideacode/protocols/Protocol-Context-Management-V3.md`

---

**Status:** TEMPLATE COMPLETE
**Next:** Use for migrating remaining 20 modules
**Estimated Time:** ~21 hours per module × 20 modules = 420 hours (52 days)

---

*Template Created:* 2025-11-03 11:50 PST
*Created by:* Manoj Jhawar, manoj@ideahq.net
*Project:* Avanues Module Migration
*Framework:* IDEACODE v5.3
*Purpose:* Reusable instructions for all module migrations
