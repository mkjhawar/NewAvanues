package com.augmentalis.webavanue

/**
 * iOS PlatformVoiceService stub implementation
 *
 * Voice service integration would use iOS Speech framework
 * For now, returns no-op implementation
 */
actual class PlatformVoiceService {
    actual fun startListening(callback: (String) -> Unit) {
        // iOS voice recognition would use SFSpeechRecognizer
        println("Voice listening not implemented for iOS")
    }

    actual fun stopListening() {
        // Stop iOS voice recognition
    }

    actual fun isListening(): Boolean {
        return false
    }

    actual fun speak(text: String) {
        // iOS TTS would use AVSpeechSynthesizer
        println("TTS not implemented for iOS: $text")
    }

    actual fun dispose() {
        // Cleanup
    }
}
