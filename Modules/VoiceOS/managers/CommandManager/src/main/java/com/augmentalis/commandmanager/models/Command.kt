/**
 * Command.kt - Command model re-export from KMP module
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Re-exports Command data class from the KMP command-models module
 * for backward compatibility with legacy imports.
 *
 * The actual implementation is in:
 * core/command-models/src/commonMain/kotlin/com/augmentalis/voiceos/command/CommandModels.kt
 */
package com.augmentalis.commandmanager.models

// Re-export Command from KMP module for backward compatibility
typealias Command = com.augmentalis.voiceos.command.Command
