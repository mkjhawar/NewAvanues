/**
 * CommandSource.kt - CommandSource enum re-export from KMP module
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Re-exports CommandSource enum from the KMP command-models module
 * for backward compatibility with legacy imports.
 *
 * The actual implementation is in:
 * core/command-models/src/commonMain/kotlin/com/augmentalis/voiceos/command/CommandModels.kt
 */
package com.augmentalis.commandmanager.models

// Re-export CommandSource from KMP module for backward compatibility
typealias CommandSource = com.augmentalis.voiceos.command.CommandSource
