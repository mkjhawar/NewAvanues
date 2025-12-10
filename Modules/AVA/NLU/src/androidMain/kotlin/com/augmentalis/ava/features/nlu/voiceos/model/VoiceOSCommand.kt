package com.augmentalis.ava.features.nlu.voiceos.model

/**
 * VoiceOS command file (.vos) data structures
 *
 * .vos Format: VoiceOS command definition files
 * Imported from VoiceOS and converted to .ava format
 *
 * No logic, no I/O - pure data classes only
 */

/**
 * Single command from VoiceOS
 */
data class VoiceOSCommand(
    val action: String,
    val cmd: String,
    val synonyms: List<String>
)

/**
 * Metadata about a .vos file
 */
data class VoiceOSFileInfo(
    val fileName: String,
    val category: String,
    val commandCount: Int
)

/**
 * Complete .vos file structure
 */
data class VoiceOSFile(
    val schema: String,
    val version: String,
    val locale: String,
    val fileName: String,
    val category: String,
    val commands: List<VoiceOSCommand>
)
