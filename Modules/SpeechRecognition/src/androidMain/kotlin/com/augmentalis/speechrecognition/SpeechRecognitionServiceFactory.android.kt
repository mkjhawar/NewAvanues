/**
 * SpeechRecognitionServiceFactory.android.kt - Android speech recognition service factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude (AI Assistant)
 * Created: 2026-01-18
 */
package com.augmentalis.speechrecognition

/**
 * Create Android-specific speech recognition service.
 */
actual fun createSpeechRecognitionService(): SpeechRecognitionService {
    return AndroidSpeechRecognitionService()
}
