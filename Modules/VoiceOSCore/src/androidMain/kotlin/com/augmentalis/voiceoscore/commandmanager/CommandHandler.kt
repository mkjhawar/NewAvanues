/**
 * CommandHandler.kt
 *
 * Created: 2025-10-10 19:08 PDT
 * Version: 1.0.0
 *
 * Purpose: Interface for module-level command handlers in VOS4 system-wide routing
 * Features: Module identification, command support declaration, execution
 * Location: CommandManager module
 *
 * Changelog:
 * - v1.0.0 (2025-10-10): Initial implementation for CommandRegistry infrastructure
 */

package com.augmentalis.voiceoscore.commandmanager

/**
 * Interface for command handlers in the VOS4 command routing system.
 *
 * Each module (VoiceCursor, VoiceKeyboard, etc.) implements this interface
 * to participate in system-wide command routing via CommandRegistry.
 *
 * Design Principles:
 * - Module identification: Each handler has a unique moduleId
 * - Command declaration: Handlers declare supported commands upfront
 * - Capability checking: `canHandle()` allows quick pre-validation
 * - Async execution: `handleCommand()` uses suspend for async operations
 *
 * Example Implementation:
 * ```kotlin
 * class CursorCommandHandler : CommandHandler {
 *     override val moduleId = "voicecursor"
 *     override val supportedCommands = listOf(
 *         "cursor up", "cursor down", "click", "show cursor"
 *     )
 *
 *     override fun canHandle(command: String): Boolean {
 *         return command.startsWith("cursor") || command == "click"
 *     }
 *
 *     override suspend fun handleCommand(command: String): Boolean {
 *         // Delegate to CursorActions
 *         return CursorActions.execute(command)
 *     }
 * }
 * ```
 *
 * Thread Safety:
 * - Implementations must be thread-safe (multiple concurrent calls to handleCommand)
 * - CommandRegistry handles synchronization of registration/unregistration
 *
 * @since 1.0.0
 */
interface CommandHandler {
    /**
     * Unique identifier for this module's command handler.
     *
     * Must be unique across the VOS4 system. Convention: lowercase module name.
     *
     * Examples: "voicecursor", "voicekeyboard", "hudmanager"
     */
    val moduleId: String

    /**
     * List of supported command patterns.
     *
     * Used for documentation, UI display, and help systems.
     * Can include parameter placeholders like "[distance]" or "{app_name}".
     *
     * Examples:
     * - "cursor up [distance]"
     * - "open {app_name}"
     * - "click here"
     *
     * Note: This is declarative - actual command matching happens in canHandle()
     */
    val supportedCommands: List<String>

    /**
     * Check if this handler can process the given command.
     *
     * This is a fast pre-check before handleCommand() is called.
     * Should return true if the command matches this module's patterns.
     *
     * Performance: Must be < 1ms (no expensive operations, no async calls)
     *
     * @param command The normalized command text (lowercase, trimmed)
     * @return true if this handler can process the command, false otherwise
     */
    fun canHandle(command: String): Boolean

    /**
     * Execute the given voice command.
     *
     * Called only after canHandle() returns true.
     * Performs the actual command execution (show cursor, type text, etc.)
     *
     * Performance Target: < 100ms for most commands
     *
     * Error Handling:
     * - Return false if command execution fails
     * - Log errors internally, don't throw exceptions
     * - Exceptions will be caught by CommandRegistry and logged
     *
     * @param command The normalized command text (lowercase, trimmed)
     * @return true if command was successfully executed, false otherwise
     */
    suspend fun handleCommand(command: String): Boolean
}
