package com.augmentalis.voiceos.ui.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.augmentalis.voiceos.AccessibilitySetupHelper

/**
 * Test Activity - Direct speech to accessibility action
 * Pure native implementation, zero overhead
 */
class TestSpeechActivity : Activity() {

    private lateinit var helper: AccessibilitySetupHelper
    private lateinit var statusText: TextView
    private lateinit var commandText: TextView
    private lateinit var speechButton: Button
    private var speechRecognizer: SpeechRecognizer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        helper = AccessibilitySetupHelper(this)

        // Simple native UI - no XML layouts
        setContentView(createUI())
        
        // Native Android speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()
    }
    
    private fun createUI(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
            
            // Title
            addView(TextView(context).apply {
                text = "VoiceOS Speech Test"
                textSize = 24f
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            })
            
            // Status
            statusText = TextView(context).apply {
                text = "Accessibility: ${if (helper.isServiceEnabled()) "✓ Connected" else "✗ Not Connected"}"
                textSize = 16f
                setTextColor(if (helper.isServiceEnabled()) Color.GREEN else Color.RED)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            }
            addView(statusText)
            
            // Command display
            commandText = TextView(context).apply {
                text = "Say a command..."
                textSize = 18f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            }
            addView(commandText)
            
            // Speech button
            speechButton = Button(context).apply {
                text = "🎤 Tap to Speak"
                textSize = 20f
                setPadding(48, 32, 48, 32)
                setBackgroundColor(Color.parseColor("#2196F3"))
                setTextColor(Color.WHITE)
                setOnClickListener { startListening() }
            }
            addView(speechButton)
            
            // Commands help
            addView(TextView(context).apply {
                text = "\nTry saying:\n" +
                       "• \"Go back\"\n" +
                       "• \"Go home\"\n" +
                       "• \"Recent apps\"\n" +
                       "• \"Scroll down\"\n" +
                       "• \"Click [text]\""
                textSize = 14f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 0)
            })
        }
    }
    
    private fun setupSpeechRecognizer() {
        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                runOnUiThread {
                    speechButton.text = "🔴 Listening..."
                    speechButton.setBackgroundColor(Color.parseColor("#F44336"))
                }
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    processCommand(command)
                }
                resetButton()
            }
            
            override fun onError(error: Int) {
                runOnUiThread {
                    commandText.text = "Error: ${getErrorText(error)}"
                    resetButton()
                }
            }
            
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    private fun startListening() {
        if (!helper.isServiceEnabled()) {
            Toast.makeText(this, "Please enable Accessibility Service first", Toast.LENGTH_LONG).show()
            return
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer?.startListening(intent)
    }
    
    private fun processCommand(command: String) {
        runOnUiThread {
            commandText.text = "Command: \"$command\""
            commandText.setTextColor(Color.BLACK)

            // TODO: Implement IPC mechanism for command execution
            // VoiceOSCore is now a separate application - need broadcast or AIDL for cross-app communication
            // val executed = VoiceOSService.executeCommand(command)

            commandText.append("\n⚠ Command execution requires IPC implementation")
            commandText.setTextColor(Color.parseColor("#FF9800"))

            // if (executed) {
            //     commandText.append("\n✓ Executed")
            //     commandText.setTextColor(Color.GREEN)
            // } else {
            //     commandText.append("\n✗ Not recognized")
            //     commandText.setTextColor(Color.RED)
            // }
        }
    }
    
    private fun resetButton() {
        runOnUiThread {
            speechButton.text = "🎤 Tap to Speak"
            speechButton.setBackgroundColor(Color.parseColor("#2196F3"))
        }
    }
    
    private fun getErrorText(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio error"
        SpeechRecognizer.ERROR_CLIENT -> "Client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Need permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        else -> "Unknown error"
    }
    
    override fun onResume() {
        super.onResume()
        statusText.text = "Accessibility: ${if (helper.isServiceEnabled()) "✓ Connected" else "✗ Not Connected"}"
        statusText.setTextColor(if (helper.isServiceEnabled()) Color.GREEN else Color.RED)
    }
    
    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}