package com.augmentalis.Avanues.web.universal.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Android implementation of PlatformVoiceService using SpeechRecognizer
 */
actual class PlatformVoiceService(private val context: Context) {

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    actual val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    actual val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private var speechRecognizer: SpeechRecognizer? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceState.Listening
        }

        override fun onBeginningOfSpeech() {
            // User started speaking
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changed - could be used for visualization
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Sound buffer received
        }

        override fun onEndOfSpeech() {
            _voiceState.value = VoiceState.Processing
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error: $error"
            }
            _voiceState.value = VoiceState.Error(errorMessage)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                val command = VoiceCommandParser.parse(spokenText)
                _voiceState.value = VoiceState.Result(command, spokenText)
            } else {
                _voiceState.value = VoiceState.Error("No speech recognized")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Could show partial results for feedback
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Reserved for future events
        }
    }

    actual fun startListening() {
        if (!isAvailable) {
            _voiceState.value = VoiceState.Error("Speech recognition not available")
            return
        }

        // Create recognizer if needed
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        }

        // Create intent for speech recognition
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        _voiceState.value = VoiceState.Listening
        speechRecognizer?.startListening(intent)
    }

    actual fun stopListening() {
        speechRecognizer?.stopListening()
        _voiceState.value = VoiceState.Idle
    }

    actual fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        _voiceState.value = VoiceState.Idle
    }
}
