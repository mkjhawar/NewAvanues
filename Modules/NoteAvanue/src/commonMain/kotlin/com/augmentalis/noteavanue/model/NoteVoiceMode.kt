package com.augmentalis.noteavanue.model

/**
 * Voice interaction mode for NoteAvanue.
 *
 * Determines how speech input is routed:
 * - COMMANDING: Speech is parsed as voice commands (bold, heading, save, etc.)
 * - DICTATING: Speech is inserted as text content with format detection
 * - CONTINUOUS: Like DICTATING but auto-resumes after each utterance pause
 *
 * Mode switching is triggered by the `note_dictate`, `note_command_mode`,
 * and `note_continuous` VOS commands.
 */
enum class NoteVoiceMode {
    /** Default: voice input is interpreted as commands */
    COMMANDING,
    /** Voice input is inserted as rich text content */
    DICTATING,
    /** Like DICTATING but auto-resumes after silence */
    CONTINUOUS
}
