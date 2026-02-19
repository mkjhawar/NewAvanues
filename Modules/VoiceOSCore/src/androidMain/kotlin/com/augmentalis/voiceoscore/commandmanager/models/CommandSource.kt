/**
 * CommandSource.kt - CommandManager model
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Defines the source of a command
 */
package com.augmentalis.voiceoscore.commandmanager.models

/**
 * Source of a command
 */
enum class CommandSource {
    /**
     * Command came from voice input
     */
    VOICE,

    /**
     * Command came from keyboard input
     */
    KEYBOARD,

    /**
     * Command came from gesture input
     */
    GESTURE,

    /**
     * Command came from system (automated)
     */
    SYSTEM,

    /**
     * Command came from external API
     */
    API,

    /**
     * Command source is unknown
     */
    UNKNOWN
}
