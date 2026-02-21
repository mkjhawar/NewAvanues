/**
 * SpeechRecognitionServiceFactory.macos.kt - macOS speech recognition service factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.speechrecognition

/**
 * Create macOS-specific speech recognition service.
 * Uses SFSpeechRecognizer via the macOS Speech framework.
 */
actual fun createSpeechRecognitionService(): SpeechRecognitionService {
    return MacosSpeechRecognitionService()
}
