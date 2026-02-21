package com.augmentalis.universal.thememanager

/**
 * KMP-safe file operation declarations for LocalThemeRepository.
 * Platform actuals provide the real I/O implementation.
 */
internal expect fun themeWriteFile(path: String, content: String)
internal expect fun themeReadFile(path: String): String?
internal expect fun themeDeleteFile(path: String)
internal expect fun themeListFiles(dirPath: String): List<String>
