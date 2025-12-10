# RenameCommandHandler Integration Guide

**Document**: RenameCommandHandler-Integration-Guide.md
**Author**: Manoj Jhawar
**Code-Reviewed-By**: Claude Code (IDEACODE v10.3)
**Created**: 2025-12-08
**Purpose**: Integration guide for wiring RenameCommandHandler into VoiceCommandExecutor

---

## Overview

This guide shows how to integrate `RenameCommandHandler` into `VoiceCommandExecutor` to enable voice-activated command renaming.

**User Voice Command Examples:**
- "Rename Button 1 to Save"
- "Rename Tab 2 as Settings"
- "Change Submit Button to Send"

---

## Architecture

```
User Voice Input
      ↓
VoiceCommandExecutor
      ↓
   [Detect rename command?]
      ↓ YES
RenameCommandHandler
      ↓
Database Update + TTS Feedback
```

---

## Step 1: Add RenameCommandHandler to VoiceCommandExecutor

### File: `VoiceCommandExecutor.kt`

```kotlin
package com.augmentalis.voiceoscore.command

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.learnapp.commands.RenameCommandHandler
import com.augmentalis.voiceoscore.learnapp.commands.RenameResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Voice Command Executor
 *
 * Routes voice commands to appropriate handlers.
 * Enhanced with rename command detection.
 */
class VoiceCommandExecutor(
    private val context: Context,
    private val database: VoiceOSDatabaseManager,
    private val tts: TextToSpeech,
    private val commandManager: CommandManager
) {
    companion object {
        private const val TAG = "VoiceCommandExecutor"
    }

    // Lazy initialization of rename handler
    private val renameHandler by lazy {
        RenameCommandHandler(
            context = context,
            database = database,
            tts = tts
        )
    }

    /**
     * Execute voice command
     *
     * @param voiceInput Raw voice input
     * @param packageName Current foreground package
     */
    suspend fun execute(voiceInput: String, packageName: String): ExecutionResult {
        Log.d(TAG, "Executing: '$voiceInput' for $packageName")

        // Step 1: Check if rename command
        if (isRenameCommand(voiceInput)) {
            return handleRenameCommand(voiceInput, packageName)
        }

        // Step 2: Check if synonym exists (resolve to actual command)
        val resolvedCommand = resolveCommandWithSynonyms(voiceInput, packageName)
        if (resolvedCommand != null) {
            return executeCommand(resolvedCommand)
        }

        // Step 3: Standard command routing
        return commandManager.execute(voiceInput, packageName)
    }

    /**
     * Check if voice input is a rename command
     *
     * Detects patterns:
     * - "rename X to Y"
     * - "rename X as Y"
     * - "change X to Y"
     */
    private fun isRenameCommand(voiceInput: String): Boolean {
        val normalized = voiceInput.lowercase()
        return normalized.startsWith("rename ") ||
               normalized.startsWith("change ")
    }

    /**
     * Handle rename command
     */
    private suspend fun handleRenameCommand(
        voiceInput: String,
        packageName: String
    ): ExecutionResult {
        return when (val result = renameHandler.processRenameCommand(voiceInput, packageName)) {
            is RenameResult.Success -> {
                Log.i(TAG, "✅ Command renamed: ${result.oldName} → ${result.newName}")
                ExecutionResult.Success(
                    message = "Command renamed to ${result.newName}",
                    command = result.command.commandText
                )
            }
            is RenameResult.Error -> {
                Log.w(TAG, "❌ Rename failed: ${result.message}")
                ExecutionResult.Error(result.message)
            }
        }
    }

    /**
     * Resolve command with synonyms
     *
     * If voice input matches a synonym, return original command.
     *
     * Example:
     * - User says: "Save"
     * - Synonym: "save" → "click button 1"
     * - Returns: GeneratedCommandDTO for "click button 1"
     */
    private suspend fun resolveCommandWithSynonyms(
        voiceInput: String,
        packageName: String
    ): GeneratedCommandDTO? = withContext(Dispatchers.IO) {
        val normalized = voiceInput.lowercase()

        val commands = database.generatedCommands.getByPackage(packageName)

        commands.firstOrNull { command ->
            // Check if voice input matches any synonym
            val synonyms = command.synonyms
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            synonyms.any { synonym ->
                // Match with or without action prefix
                normalized == synonym ||
                normalized == "${command.actionType} $synonym"
            }
        }
    }

    /**
     * Execute the actual command
     */
    private suspend fun executeCommand(command: GeneratedCommandDTO): ExecutionResult {
        // TODO: Implement actual command execution
        // This will use accessibility service to perform the action

        Log.d(TAG, "Executing command: ${command.commandText}")

        // Example implementation:
        // accessibilityService.performAction(command.actionType, command.elementHash)

        return ExecutionResult.Success(
            message = "Command executed",
            command = command.commandText
        )
    }
}

/**
 * Execution result
 */
sealed class ExecutionResult {
    data class Success(
        val message: String,
        val command: String
    ) : ExecutionResult()

    data class Error(val message: String) : ExecutionResult()
}
```

---

## Step 2: Update VoiceOS Accessibility Service

### File: `VoiceOSAccessibilityService.kt`

```kotlin
class VoiceOSAccessibilityService : AccessibilityService() {

    private lateinit var voiceCommandExecutor: VoiceCommandExecutor
    private lateinit var database: VoiceOSDatabaseManager
    private lateinit var tts: TextToSpeech

    override fun onCreate() {
        super.onCreate()

        // Initialize database
        database = VoiceOSDatabaseManager.getInstance(
            DatabaseDriverFactory(this)
        )

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                Log.d(TAG, "TTS initialized successfully")
            }
        }

        // Initialize voice command executor (includes rename handler)
        voiceCommandExecutor = VoiceCommandExecutor(
            context = this,
            database = database,
            tts = tts,
            commandManager = CommandManager.getInstance(this)
        )
    }

    /**
     * Called when voice command received
     * (from voice recognition service)
     */
    fun onVoiceCommandReceived(voiceInput: String) {
        lifecycleScope.launch {
            val packageName = getCurrentForegroundPackage()
            if (packageName != null) {
                val result = voiceCommandExecutor.execute(voiceInput, packageName)

                when (result) {
                    is ExecutionResult.Success -> {
                        Log.i(TAG, "✅ Command executed: ${result.command}")
                    }
                    is ExecutionResult.Error -> {
                        Log.e(TAG, "❌ Command failed: ${result.message}")
                    }
                }
            }
        }
    }

    private fun getCurrentForegroundPackage(): String? {
        return rootInActiveWindow?.packageName?.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }

    companion object {
        private const val TAG = "VoiceOSAccessibilityService"
    }
}
```

---

## Step 3: Verify Database Schema

Ensure your database has the required tables and queries:

### Required Tables:
1. `commands_generated` - Stores generated commands
2. `scraped_element` - Links commands to apps via `appId`

### Required Queries (GeneratedCommand.sq):
```sql
-- Get commands by package (via join)
getByPackage:
SELECT gc.* FROM commands_generated gc
INNER JOIN scraped_element se ON gc.elementHash = se.elementHash
WHERE se.appId = ?
ORDER BY gc.usageCount DESC;

-- Update command (for synonyms)
update:
UPDATE commands_generated
SET elementHash = ?,
    commandText = ?,
    actionType = ?,
    confidence = ?,
    synonyms = ?,
    isUserApproved = ?,
    usageCount = ?,
    lastUsed = ?
WHERE id = ?;
```

---

## Step 4: Testing

### Unit Tests
Run the provided unit tests:
```bash
./gradlew :VoiceOSCore:testDebugUnitTest --tests RenameCommandHandlerTest
```

**Expected: 28 tests pass** (all test groups covered)

### Integration Testing

#### Test Case 1: Basic Rename
```
User: "Rename Button 1 to Save"
Expected TTS: "Renamed to Save. You can now say Save or Button 1."
Database: synonyms = "button 1,save"
```

#### Test Case 2: Use Renamed Command
```
User: "Save"
Expected: Executes "click button 1" action
```

#### Test Case 3: Original Still Works
```
User: "Button 1"
Expected: Executes "click button 1" action
```

#### Test Case 4: Command Not Found
```
User: "Rename Button 99 to Save"
Expected TTS: "Could not find command 'Button 99'"
```

#### Test Case 5: Multiple Synonyms
```
User: "Rename Button 1 to Save"
User: "Rename Button 1 as Submit"
Database: synonyms = "button 1,save,submit"
User: "Save" → works
User: "Submit" → works
User: "Button 1" → works
```

---

## Step 5: Debugging

### Enable Logging

Add to your `logcat` filter:
```
TAG:RenameCommandHandler
TAG:VoiceCommandExecutor
```

### Check Database

```kotlin
// View command synonyms
val commands = database.generatedCommands.getByPackage("com.example.app")
commands.forEach { cmd ->
    Log.d("DEBUG", "Command: ${cmd.commandText}, Synonyms: ${cmd.synonyms}")
}
```

### Test TTS Directly

```kotlin
tts.speak(
    "Test TTS feedback",
    TextToSpeech.QUEUE_FLUSH,
    null,
    "test_utterance"
)
```

---

## Step 6: Error Handling

### Common Issues

| Issue | Solution |
|-------|----------|
| TTS not initialized | Check `onCreate()` initialization and status callback |
| Database not found | Verify singleton pattern in `VoiceOSDatabaseManager` |
| Command not found | Check `appId` matches in `scraped_element` table |
| Synonyms not working | Verify `resolveCommandWithSynonyms()` is called before standard routing |
| Update fails | Check `update()` query has correct parameter order |

### Error Logging

```kotlin
try {
    val result = renameHandler.processRenameCommand(voiceInput, packageName)
} catch (e: Exception) {
    Log.e(TAG, "Rename failed", e)
    tts.speak("Sorry, rename failed. Please try again.", ...)
}
```

---

## Step 7: Performance Considerations

### Database Query Optimization
- Index on `elementHash` and `appId` (already exists)
- Query returns commands ordered by `usageCount` (most used first)

### Memory Usage
- Lazy initialization of `renameHandler` (only created when needed)
- TTS resources cleaned up in `onDestroy()`

### Threading
- All database operations use `Dispatchers.IO`
- TTS operations use `Dispatchers.Main`

---

## API Reference

### RenameCommandHandler

```kotlin
class RenameCommandHandler(
    context: Context,
    database: VoiceOSDatabaseManager,
    tts: TextToSpeech
)

suspend fun processRenameCommand(
    voiceInput: String,
    packageName: String
): RenameResult
```

### RenameResult

```kotlin
sealed class RenameResult {
    data class Success(
        val oldName: String,
        val newName: String,
        val command: GeneratedCommandDTO
    ) : RenameResult()

    data class Error(val message: String) : RenameResult()
}
```

---

## Example User Flows

### Flow 1: Rename and Use
```
1. User opens DeviceInfo app
2. User: "Button 1" → Taps first tab (works with fallback label)
3. User: "Rename Button 1 to Device Info"
4. System: "Renamed to Device Info. You can now say Device Info or Button 1."
5. User: "Device Info" → Taps first tab (works with synonym)
```

### Flow 2: Multiple Renames
```
1. User: "Rename Button 1 to Save"
2. System: "Renamed to Save..."
3. User: "Rename Button 2 to Cancel"
4. System: "Renamed to Cancel..."
5. User: "Save" → works
6. User: "Cancel" → works
```

### Flow 3: Override Existing Synonym
```
1. Database: synonyms = "save"
2. User: "Rename Button 1 to Submit"
3. Database: synonyms = "save,submit,button 1"
4. User can now say: "Save", "Submit", or "Button 1"
```

---

## Next Steps

1. **Test with real device**: Run on Android device with accessibility service enabled
2. **Add UI settings**: Create settings screen for manual synonym editing (see spec)
3. **Add hint overlay**: Show contextual hint when screen has generated labels (see spec)
4. **Monitor usage**: Track which synonyms are most used for future improvements

---

## Support

For issues or questions:
- Check unit tests for expected behavior
- Review logs with `TAG:RenameCommandHandler`
- Verify database schema matches specification
- Test TTS initialization separately

---

**End of Integration Guide**
