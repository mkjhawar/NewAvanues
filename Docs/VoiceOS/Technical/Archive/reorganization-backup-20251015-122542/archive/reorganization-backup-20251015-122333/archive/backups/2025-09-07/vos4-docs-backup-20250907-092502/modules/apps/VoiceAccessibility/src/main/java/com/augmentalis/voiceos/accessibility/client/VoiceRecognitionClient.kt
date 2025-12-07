/**
 * VoiceRecognitionClient.kt - AIDL client for voice recognition service
 * 
 * Purpose: Manages connection to VoiceRecognitionService via AIDL
 * Handles service binding, callbacks, and recognition control
 */
package com.augmentalis.voiceos.accessibility.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.augmentalis.voicerecognition.IRecognitionCallback
import com.augmentalis.voicerecognition.IVoiceRecognitionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Client for VoiceRecognition AIDL service
 * Manages service connection and recognition operations
 */
class VoiceRecognitionClient(private val context: Context) {
    
    companion object {
        private const val TAG = "VoiceRecognitionClient"
        private const val SERVICE_PACKAGE = "com.augmentalis.voicerecognition"
        private const val SERVICE_CLASS = "com.augmentalis.voicerecognition.service.VoiceRecognitionService"
    }
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Service connection
    private var service: IVoiceRecognitionService? = null
    private var isConnected = false
    
    // Callbacks
    private var connectionCallback: ConnectionCallback? = null
    private var recognitionCallback: RecognitionCallback? = null
    
    // AIDL callback implementation
    private val aidlCallback = object : IRecognitionCallback.Stub() {
        override fun onRecognitionResult(text: String, confidence: Float, isFinal: Boolean) {
            Log.d(TAG, "Recognition result: $text (confidence: $confidence, final: $isFinal)")
            scope.launch {
                recognitionCallback?.onResult(text, confidence, isFinal)
            }
        }
        
        override fun onError(errorCode: Int, message: String?) {
            Log.e(TAG, "Recognition error: $errorCode - $message")
            scope.launch {
                recognitionCallback?.onError(errorCode, message)
            }
        }
        
        override fun onStateChanged(state: Int, message: String?) {
            Log.d(TAG, "State changed: $state - $message")
            scope.launch {
                recognitionCallback?.onStateChanged(state, message)
            }
        }
        
        override fun onPartialResult(partialText: String?) {
            Log.d(TAG, "Partial result: $partialText")
            scope.launch {
                partialText?.let {
                    recognitionCallback?.onPartialResult(it)
                }
            }
        }
    }
    
    // Service connection handler
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "Service connected")
            service = IVoiceRecognitionService.Stub.asInterface(binder)
            isConnected = true
            
            // Register callback
            try {
                service?.registerCallback(aidlCallback)
                connectionCallback?.onConnected()
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to register callback", e)
                connectionCallback?.onError("Failed to register callback: ${e.message}")
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            service = null
            isConnected = false
            connectionCallback?.onDisconnected()
        }
    }
    
    /**
     * Connect to the voice recognition service
     */
    fun connect() {
        if (isConnected) {
            Log.w(TAG, "Already connected")
            return
        }
        
        val intent = Intent().apply {
            component = ComponentName(SERVICE_PACKAGE, SERVICE_CLASS)
        }
        
        val bound = context.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        
        if (!bound) {
            Log.e(TAG, "Failed to bind service")
            connectionCallback?.onError("Failed to bind to VoiceRecognition service")
        } else {
            Log.d(TAG, "Binding to service...")
        }
    }
    
    /**
     * Disconnect from the voice recognition service
     */
    fun disconnect() {
        if (isConnected) {
            try {
                service?.unregisterCallback(aidlCallback)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to unregister callback", e)
            }
            
            context.unbindService(serviceConnection)
            service = null
            isConnected = false
            connectionCallback?.onDisconnected()
        }
    }
    
    /**
     * Start voice recognition
     * @param engine The engine to use (empty string for default)
     * @param language Language code (e.g., "en-US")
     * @param mode Recognition mode (0=continuous, 1=single_shot, 2=streaming)
     */
    fun startRecognition(engine: String = "", language: String = "en-US", mode: Int = 0): Boolean {
        return try {
            service?.startRecognition(engine, language, mode) ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to start recognition", e)
            recognitionCallback?.onError(500, "Failed to start recognition: ${e.message}")
            false
        }
    }
    
    /**
     * Stop voice recognition
     */
    fun stopRecognition(): Boolean {
        return try {
            service?.stopRecognition() ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to stop recognition", e)
            false
        }
    }
    
    /**
     * Check if currently recognizing
     */
    fun isRecognizing(): Boolean {
        return try {
            service?.isRecognizing() ?: false
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to check recognition status", e)
            false
        }
    }
    
    /**
     * Get available engines
     */
    fun getAvailableEngines(): List<String> {
        return try {
            service?.getAvailableEngines() ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to get available engines", e)
            emptyList()
        }
    }
    
    /**
     * Get service status
     */
    fun getStatus(): String {
        return try {
            service?.getStatus() ?: "Not connected"
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to get status", e)
            "Error: ${e.message}"
        }
    }
    
    /**
     * Set connection callback
     */
    fun setConnectionCallback(callback: ConnectionCallback) {
        this.connectionCallback = callback
    }
    
    /**
     * Set recognition callback
     */
    fun setRecognitionCallback(callback: RecognitionCallback) {
        this.recognitionCallback = callback
    }
    
    /**
     * Connection callback interface
     */
    interface ConnectionCallback {
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }
    
    /**
     * Recognition callback interface
     */
    interface RecognitionCallback {
        fun onResult(text: String, confidence: Float, isFinal: Boolean)
        fun onPartialResult(text: String)
        fun onError(errorCode: Int, message: String?)
        fun onStateChanged(state: Int, message: String?)
    }
}