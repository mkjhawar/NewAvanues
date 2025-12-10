package com.augmentalis.voicerecognition.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.augmentalis.voicerecognition.IRecognitionCallback
import com.augmentalis.voicerecognition.IVoiceRecognitionService
import com.augmentalis.voicerecognition.service.VoiceRecognitionService

/**
 * Voice Recognition Client
 * 
 * Helper class for connecting to and interacting with the VoiceRecognitionService
 * via AIDL interfaces. Manages service binding and provides a simplified API.
 */
class VoiceRecognitionClient(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceRecognitionClient"
    }
    
    private var service: IVoiceRecognitionService? = null
    private var isBound = false
    private var clientCallback: ClientCallback? = null
    
    /**
     * Client callback interface for simplified event handling
     */
    interface ClientCallback {
        fun onServiceConnected()
        fun onServiceDisconnected()
        fun onRecognitionResult(text: String, confidence: Float, isFinal: Boolean)
        fun onError(errorCode: Int, message: String)
        fun onStateChanged(state: Int, message: String)
        fun onPartialResult(partialText: String)
    }
    
    /**
     * AIDL Callback Implementation
     */
    private val recognitionCallback = object : IRecognitionCallback.Stub() {
        override fun onRecognitionResult(text: String?, confidence: Float, isFinal: Boolean) {
            Log.d(TAG, "Recognition result: $text (confidence: $confidence, final: $isFinal)")
            clientCallback?.onRecognitionResult(text ?: "", confidence, isFinal)
        }
        
        override fun onError(errorCode: Int, message: String?) {
            Log.e(TAG, "Recognition error: $errorCode - $message")
            clientCallback?.onError(errorCode, message ?: "Unknown error")
        }
        
        override fun onStateChanged(state: Int, message: String?) {
            Log.d(TAG, "State changed: $state - $message")
            clientCallback?.onStateChanged(state, message ?: "")
        }
        
        override fun onPartialResult(partialText: String?) {
            Log.d(TAG, "Partial result: $partialText")
            clientCallback?.onPartialResult(partialText ?: "")
        }
    }
    
    /**
     * Service Connection Handler
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "Service connected")
            service = IVoiceRecognitionService.Stub.asInterface(binder)
            isBound = true
            
            // Register our callback
            service?.registerCallback(recognitionCallback)
            
            clientCallback?.onServiceConnected()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            service = null
            isBound = false
            clientCallback?.onServiceDisconnected()
        }
    }
    
    /**
     * Bind to the voice recognition service
     */
    fun bindService(callback: ClientCallback): Boolean {
        this.clientCallback = callback
        
        val intent = Intent(context, VoiceRecognitionService::class.java)
        return context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    /**
     * Unbind from the service
     */
    fun unbindService() {
        if (isBound) {
            // Unregister callback before unbinding
            service?.unregisterCallback(recognitionCallback)
            
            context.unbindService(serviceConnection)
            isBound = false
            service = null
            clientCallback = null
        }
    }
    
    /**
     * Start voice recognition
     */
    fun startRecognition(
        engine: String = "google",
        language: String = "en-US",
        mode: Int = 0 // MODE_CONTINUOUS
    ): Boolean {
        return service?.startRecognition(engine, language, mode) ?: false
    }
    
    /**
     * Stop voice recognition
     */
    fun stopRecognition(): Boolean {
        return service?.stopRecognition() ?: false
    }
    
    /**
     * Check if recognition is active
     */
    fun isRecognizing(): Boolean {
        return service?.isRecognizing() ?: false
    }
    
    /**
     * Get available recognition engines
     */
    fun getAvailableEngines(): List<String> {
        return service?.getAvailableEngines() ?: emptyList()
    }
    
    /**
     * Get current service status
     */
    fun getStatus(): String {
        return service?.getStatus() ?: "Not connected"
    }
    
    /**
     * Check if service is bound and ready
     */
    fun isServiceReady(): Boolean {
        return isBound && service != null
    }
}