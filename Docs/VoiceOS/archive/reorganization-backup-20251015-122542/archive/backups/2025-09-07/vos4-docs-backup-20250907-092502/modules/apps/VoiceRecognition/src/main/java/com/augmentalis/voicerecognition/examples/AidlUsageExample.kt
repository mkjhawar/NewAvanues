package com.augmentalis.voicerecognition.examples

import android.content.Context
import android.util.Log
import com.augmentalis.voicerecognition.client.VoiceRecognitionClient
import com.augmentalis.voicerecognition.service.VoiceRecognitionService

/**
 * AIDL Usage Example
 * 
 * Demonstrates how to use the Voice Recognition AIDL interfaces
 * for inter-process communication with the speech recognition service.
 */
class AidlUsageExample(private val context: Context) {
    
    companion object {
        private const val TAG = "AidlUsageExample"
    }
    
    private lateinit var client: VoiceRecognitionClient
    
    /**
     * Initialize and start using the AIDL interface
     */
    fun startExample() {
        Log.d(TAG, "Starting AIDL usage example")
        
        // Create client instance
        client = VoiceRecognitionClient(context)
        
        // Set up callback to handle service events
        val callback = object : VoiceRecognitionClient.ClientCallback {
            
            override fun onServiceConnected() {
                Log.d(TAG, "Service connected - ready to use!")
                
                // Service is now ready, let's try some operations
                demonstrateServiceUsage()
            }
            
            override fun onServiceDisconnected() {
                Log.d(TAG, "Service disconnected")
            }
            
            override fun onRecognitionResult(text: String, confidence: Float, isFinal: Boolean) {
                Log.d(TAG, "Recognition result: '$text' (confidence: $confidence, final: $isFinal)")
                
                if (isFinal) {
                    // Handle final recognition result
                    handleFinalResult(text, confidence)
                }
            }
            
            override fun onError(errorCode: Int, message: String) {
                Log.e(TAG, "Recognition error: $errorCode - $message")
                handleError(errorCode, message)
            }
            
            override fun onStateChanged(state: Int, message: String) {
                val stateText = when (state) {
                    0 -> "IDLE"
                    1 -> "LISTENING"
                    2 -> "PROCESSING"
                    3 -> "ERROR"
                    else -> "UNKNOWN"
                }
                Log.d(TAG, "State changed to $stateText: $message")
            }
            
            override fun onPartialResult(partialText: String) {
                Log.d(TAG, "Partial result: '$partialText'")
                // Update UI with partial results for real-time feedback
            }
        }
        
        // Bind to the service
        val success = client.bindService(callback)
        if (!success) {
            Log.e(TAG, "Failed to bind to voice recognition service")
        }
    }
    
    /**
     * Demonstrate various service operations
     */
    private fun demonstrateServiceUsage() {
        if (!client.isServiceReady()) {
            Log.w(TAG, "Service not ready")
            return
        }
        
        // Get available engines
        val engines = client.getAvailableEngines()
        Log.d(TAG, "Available engines: $engines")
        
        // Get current status
        val status = client.getStatus()
        Log.d(TAG, "Current status: $status")
        
        // Check if recognition is active
        val isActive = client.isRecognizing()
        Log.d(TAG, "Is recognizing: $isActive")
        
        // Start recognition with different configurations
        startRecognitionExamples()
    }
    
    /**
     * Show examples of starting recognition with different parameters
     */
    private fun startRecognitionExamples() {
        Log.d(TAG, "Starting recognition examples...")
        
        // Example 1: Start with Google engine, English, continuous mode
        Log.d(TAG, "Starting continuous recognition with Google...")
        var success = client.startRecognition(
            engine = "google",
            language = "en-US",
            mode = 0 // MODE_CONTINUOUS
        )
        Log.d(TAG, "Google recognition started: $success")
        
        // Wait a bit, then stop
        Thread.sleep(2000)
        client.stopRecognition()
        
        // Example 2: Start with Vivoka engine, French, single shot mode
        Log.d(TAG, "Starting single-shot recognition with Vivoka...")
        success = client.startRecognition(
            engine = "vivoka",
            language = "fr-FR", 
            mode = 1 // MODE_SINGLE_SHOT
        )
        Log.d(TAG, "Vivoka recognition started: $success")
    }
    
    /**
     * Handle final recognition result
     */
    private fun handleFinalResult(text: String, confidence: Float) {
        Log.i(TAG, "Final result received: '$text' with confidence $confidence")
        
        // Process the final recognition result
        if (confidence > 0.7f) {
            // High confidence - process the command
            processVoiceCommand(text)
        } else {
            // Low confidence - might want to ask for confirmation
            Log.w(TAG, "Low confidence result, might need confirmation")
        }
    }
    
    /**
     * Handle recognition errors
     */
    private fun handleError(errorCode: Int, message: String) {
        Log.e(TAG, "Handling recognition error: $errorCode - $message")
        
        // Could restart recognition, show error to user, etc.
        when (errorCode) {
            // Handle specific error codes from Android SpeechRecognizer
            -1 -> Log.e(TAG, "Network timeout")
            -2 -> Log.e(TAG, "Network error") 
            -3 -> Log.e(TAG, "Audio error")
            -4 -> Log.e(TAG, "Server error")
            -5 -> Log.e(TAG, "Client error")
            -6 -> Log.e(TAG, "Speech timeout")
            -7 -> Log.e(TAG, "No match")
            -8 -> Log.e(TAG, "Recognition busy")
            -9 -> Log.e(TAG, "Insufficient permissions")
            else -> Log.e(TAG, "Unknown error code: $errorCode")
        }
    }
    
    /**
     * Process voice commands
     */
    private fun processVoiceCommand(command: String) {
        Log.d(TAG, "Processing voice command: '$command'")
        
        // Example command processing
        when {
            command.contains("hello", ignoreCase = true) -> {
                Log.d(TAG, "Greeting detected")
            }
            command.contains("start", ignoreCase = true) -> {
                Log.d(TAG, "Start command detected")
            }
            command.contains("stop", ignoreCase = true) -> {
                Log.d(TAG, "Stop command detected")
                client.stopRecognition()
            }
            else -> {
                Log.d(TAG, "Unknown command: $command")
            }
        }
    }
    
    /**
     * Stop the example and clean up
     */
    fun stopExample() {
        Log.d(TAG, "Stopping AIDL usage example")
        
        // Stop any ongoing recognition
        if (client.isServiceReady() && client.isRecognizing()) {
            client.stopRecognition()
        }
        
        // Unbind from service
        client.unbindService()
    }
    
    /**
     * Quick test method to verify AIDL functionality
     */
    fun quickTest() {
        Log.d(TAG, "Running quick AIDL test")
        
        startExample()
        
        // Give some time for service to connect and run tests
        Thread.sleep(5000)
        
        stopExample()
        
        Log.d(TAG, "Quick test completed")
    }
}