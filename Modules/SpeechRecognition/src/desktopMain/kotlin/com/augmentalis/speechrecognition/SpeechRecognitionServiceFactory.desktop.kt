/**
 * SpeechRecognitionServiceFactory.desktop.kt - Desktop speech recognition service factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-18
 */
package com.augmentalis.speechrecognition

/**
 * Create Desktop-specific speech recognition service.
 */
actual fun createSpeechRecognitionService(): SpeechRecognitionService {
    return DesktopSpeechRecognitionService()
}
