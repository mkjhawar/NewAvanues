/**
 * Platform.kt - Platform identifiers for AVID system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-01-13
 */
package com.augmentalis.avid

/**
 * Platform codes for AVID generation
 *
 * Single character codes embedded in AVID format: AVID-{platform}-{sequence}
 */
enum class Platform(val code: Char, val displayName: String) {
    ANDROID('A', "Android"),
    IOS('I', "iOS"),
    WEB('W', "Web"),
    MACOS('M', "macOS"),
    WINDOWS('X', "Windows"),
    LINUX('L', "Linux");

    companion object {
        fun fromCode(code: Char): Platform? = entries.find { it.code == code }

        fun fromCodeOrThrow(code: Char): Platform =
            fromCode(code) ?: throw IllegalArgumentException("Unknown platform code: $code")
    }
}
