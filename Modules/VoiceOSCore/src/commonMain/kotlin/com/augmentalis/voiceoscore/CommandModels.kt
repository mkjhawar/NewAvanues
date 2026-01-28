/**
 * VoiceOS Command Models
 *
 * Re-exports from CommandManager for VoiceOSCore usage.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Migrated: 2026-01-28
 */
package com.augmentalis.voiceoscore

// Re-export types from CommandManager
typealias CommandSource = com.augmentalis.commandmanager.CommandSource
typealias ErrorCode = com.augmentalis.commandmanager.ErrorCode
typealias ParameterType = com.augmentalis.commandmanager.ParameterType
typealias EventType = com.augmentalis.commandmanager.EventType

// Re-export data classes from CommandManager
typealias Command = com.augmentalis.commandmanager.Command
typealias CommandContext = com.augmentalis.commandmanager.CommandContext
typealias CommandResult = com.augmentalis.commandmanager.CommandResult
typealias CommandError = com.augmentalis.commandmanager.CommandError
typealias CommandDefinition = com.augmentalis.commandmanager.CommandDefinition
typealias CommandParameter = com.augmentalis.commandmanager.CommandParameter
typealias CommandHistoryEntry = com.augmentalis.commandmanager.CommandHistoryEntry
typealias CommandEvent = com.augmentalis.commandmanager.CommandEvent
typealias CommandHandler = com.augmentalis.commandmanager.CommandHandler
