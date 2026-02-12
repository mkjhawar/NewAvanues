/**
 * SpeechRecognitionServiceFactory.ios.kt - iOS speech recognition service factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-01-18
 */
package com.augmentalis.speechrecognition

/**
 * Create iOS-specific speech recognition service.
 */
actual fun createSpeechRecognitionService(): SpeechRecognitionService {
    return IosSpeechService()
}
