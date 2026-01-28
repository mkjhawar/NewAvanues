package com.augmentalis.actions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import com.augmentalis.voiceoscore.accessibility.IVoiceOSCallback
import com.augmentalis.voiceoscore.accessibility.IVoiceOSService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * VoiceOS Connection Manager
 *
 * Manages AIDL IPC connection to VoiceOS accessibility service for forwarding
 * commands that require accessibility permissions.
 *
 * Architecture:
 * ```
 * AVA → VoiceOSConnection → AIDL Binding → VoiceOSService
 *                                               ↓
 *                                     Accessibility API
 *                                  (Gestures, Cursor, etc.)
 * ```
 *
 * Updated: 2025-12-07 - Migrated from ContentProvider to AIDL binding
 */
class VoiceOSConnection private constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "VoiceOSConnection"

        @Volatile
        private var instance: VoiceOSConnection? = null

        /**
         * Get singleton instance of VoiceOSConnection.
         */
        fun getInstance(context: Context): VoiceOSConnection {
            return instance ?: synchronized(this) {
                instance ?: VoiceOSConnection(context.applicationContext).also {
                    instance = it
                    Log.d(TAG, "VoiceOSConnection singleton created")
                }
            }
        }

        // VoiceOS package and service names
        private const val VOICEOS_PACKAGE = "com.augmentalis.voiceoscore"
        private const val VOICEOS_SERVICE = "com.augmentalis.voiceoscore.accessibility.VoiceOSService"

        // Legacy package names for backwards compatibility
        private val VOICEOS_PACKAGES = listOf(
            VOICEOS_PACKAGE,
            "com.augmentalis.voiceos",
            "com.avanues.voiceos"
        )

        // Accessibility service class names
        private val VOICEOS_ACCESSIBILITY_SERVICES = listOf(
            "$VOICEOS_PACKAGE/.accessibility.VoiceOSService",
            "$VOICEOS_PACKAGE/com.augmentalis.voiceoscore.accessibility.VoiceOSService"
        )

        // Timeouts
        private const val BIND_TIMEOUT_MS = 5_000L
        private const val COMMAND_TIMEOUT_MS = 30_000L
    }

    /**
     * Connection state
     */
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    /**
     * Command execution result
     */
    sealed class CommandResult {
        data class Success(val message: String, val executedSteps: Int = 0, val executionTimeMs: Long = 0) : CommandResult()
        data class Failure(val error: String, val failedAtStep: Int? = null) : CommandResult()
    }

    // AIDL service reference
    private var voiceOSService: IVoiceOSService? = null
    private val isBound = AtomicBoolean(false)
    private var connectionState: ConnectionState = ConnectionState.Disconnected

    // Callback for service events
    private var serviceCallback: IVoiceOSCallback.Stub? = null

    // Service connection handler
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.i(TAG, "VoiceOS service connected: $name")
            voiceOSService = IVoiceOSService.Stub.asInterface(binder)
            isBound.set(true)
            connectionState = ConnectionState.Connected

            // Register callback if set
            serviceCallback?.let { callback ->
                try {
                    voiceOSService?.registerCallback(callback)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Failed to register callback: ${e.message}")
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "VoiceOS service disconnected: $name")
            voiceOSService = null
            isBound.set(false)
            connectionState = ConnectionState.Disconnected
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.e(TAG, "VoiceOS service binding died: $name")
            voiceOSService = null
            isBound.set(false)
            connectionState = ConnectionState.Error("Service binding died")
        }
    }

    /**
     * Check if VoiceOS is installed
     */
    fun isVoiceOSInstalled(): Boolean {
        for (pkg in VOICEOS_PACKAGES) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                Log.d(TAG, "VoiceOS found: $pkg")
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not found, try next
            }
        }
        Log.d(TAG, "VoiceOS not installed")
        return false
    }

    /**
     * Check if VoiceOS accessibility service is running
     */
    fun isAccessibilityServiceRunning(): Boolean {
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            for (serviceName in VOICEOS_ACCESSIBILITY_SERVICES) {
                if (enabledServices.contains(serviceName.substringBefore("/"))) {
                    Log.d(TAG, "VoiceOS accessibility service is running: $serviceName")
                    return true
                }
            }

            // Also check for partial matches (package name only)
            for (pkg in VOICEOS_PACKAGES) {
                if (enabledServices.contains(pkg)) {
                    Log.d(TAG, "VoiceOS accessibility service found via package: $pkg")
                    return true
                }
            }

            Log.d(TAG, "VoiceOS accessibility service not running")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service status: ${e.message}")
            return false
        }
    }

    /**
     * Bind to VoiceOS service
     */
    suspend fun bind(): Boolean = withContext(Dispatchers.Main) {
        if (isBound.get() && voiceOSService != null) {
            Log.d(TAG, "Already bound to VoiceOS")
            return@withContext true
        }

        if (!isVoiceOSInstalled()) {
            connectionState = ConnectionState.Error("VoiceOS not installed")
            return@withContext false
        }

        connectionState = ConnectionState.Connecting
        Log.i(TAG, "Binding to VoiceOS service...")

        val intent = Intent().apply {
            component = ComponentName(VOICEOS_PACKAGE, VOICEOS_SERVICE)
        }

        return@withContext try {
            val bindResult = context.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            if (bindResult) {
                // Wait for connection with timeout
                val connected = withTimeoutOrNull(BIND_TIMEOUT_MS) {
                    suspendCancellableCoroutine<Boolean> { continuation ->
                        // Poll for connection
                        val checkConnection = object : Runnable {
                            override fun run() {
                                if (isBound.get()) {
                                    continuation.resume(true)
                                } else if (connectionState is ConnectionState.Error) {
                                    continuation.resume(false)
                                } else {
                                    android.os.Handler(android.os.Looper.getMainLooper())
                                        .postDelayed(this, 100)
                                }
                            }
                        }
                        android.os.Handler(android.os.Looper.getMainLooper()).post(checkConnection)
                    }
                } ?: false

                if (connected) {
                    Log.i(TAG, "Successfully bound to VoiceOS")
                } else {
                    Log.w(TAG, "Bind timeout - VoiceOS service not responding")
                    connectionState = ConnectionState.Error("Bind timeout")
                }
                connected
            } else {
                Log.e(TAG, "Failed to initiate bind to VoiceOS")
                connectionState = ConnectionState.Error("Bind failed")
                false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception binding to VoiceOS: ${e.message}")
            connectionState = ConnectionState.Error("Permission denied")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error binding to VoiceOS: ${e.message}", e)
            connectionState = ConnectionState.Error(e.message ?: "Unknown error")
            false
        }
    }

    /**
     * Unbind from VoiceOS service
     */
    fun unbind() {
        if (isBound.get()) {
            Log.i(TAG, "Unbinding from VoiceOS service")

            // Unregister callback
            serviceCallback?.let { callback ->
                try {
                    voiceOSService?.unregisterCallback(callback)
                } catch (e: RemoteException) {
                    Log.e(TAG, "Failed to unregister callback: ${e.message}")
                }
            }

            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                Log.e(TAG, "Error unbinding: ${e.message}")
            }

            voiceOSService = null
            isBound.set(false)
            connectionState = ConnectionState.Disconnected
        }
    }

    /**
     * Execute command via VoiceOS AIDL
     */
    suspend fun executeCommand(
        intent: String,
        category: String,
        parameters: Map<String, String> = emptyMap()
    ): CommandResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Executing command via VoiceOS: $intent (category: $category)")

        // Ensure connected
        if (!isBound.get() || voiceOSService == null) {
            val connected = bind()
            if (!connected) {
                return@withContext CommandResult.Failure(
                    "Cannot connect to VoiceOS. Is VoiceOS installed and accessibility enabled?"
                )
            }
        }

        val service = voiceOSService ?: return@withContext CommandResult.Failure(
            "VoiceOS service not available"
        )

        try {
            val startTime = System.currentTimeMillis()

            // Try executeCommand first for simple commands
            val success = service.executeCommand(intent)

            val executionTime = System.currentTimeMillis() - startTime

            return@withContext if (success) {
                Log.i(TAG, "Command executed successfully: $intent (${executionTime}ms)")
                CommandResult.Success(
                    message = "Command executed: $intent",
                    executionTimeMs = executionTime
                )
            } else {
                // Try executeAccessibilityAction for complex actions
                val paramsJson = org.json.JSONObject().apply {
                    parameters.forEach { (key, value) -> put(key, value) }
                    put("category", category)
                }.toString()

                val actionSuccess = service.executeAccessibilityAction(intent, paramsJson)

                if (actionSuccess) {
                    CommandResult.Success(
                        message = "Accessibility action executed: $intent",
                        executionTimeMs = System.currentTimeMillis() - startTime
                    )
                } else {
                    CommandResult.Failure("Command not recognized: $intent")
                }
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote exception executing command: ${e.message}")
            // Service died, reset connection
            isBound.set(false)
            voiceOSService = null
            connectionState = ConnectionState.Disconnected
            return@withContext CommandResult.Failure("VoiceOS service disconnected: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: ${e.message}", e)
            return@withContext CommandResult.Failure("Error: ${e.message}")
        }
    }

    /**
     * Execute a simple command (convenience method)
     */
    suspend fun execute(intent: String, category: String): CommandResult {
        return executeCommand(intent, category)
    }

    /**
     * Scrape current screen via VoiceOS
     */
    fun scrapeCurrentScreen(): String? {
        val service = voiceOSService ?: run {
            Log.w(TAG, "Cannot scrape - not connected to VoiceOS")
            return null
        }

        return try {
            service.scrapeCurrentScreen()
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote exception scraping screen: ${e.message}")
            null
        }
    }

    /**
     * Get available commands from VoiceOS
     */
    fun getAvailableCommands(): List<String> {
        val service = voiceOSService ?: return emptyList()

        return try {
            service.availableCommands ?: emptyList()
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote exception getting commands: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get service status
     */
    fun getServiceStatus(): String? {
        val service = voiceOSService ?: return null

        return try {
            service.serviceStatus
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote exception getting status: ${e.message}")
            null
        }
    }

    /**
     * Check if service is ready
     */
    fun isServiceReady(): Boolean {
        val service = voiceOSService ?: return false

        return try {
            service.isServiceReady
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote exception checking ready: ${e.message}")
            false
        }
    }

    /**
     * Set callback for service events
     */
    fun setCallback(
        onCommandRecognized: ((String, Float) -> Unit)? = null,
        onCommandExecuted: ((String, Boolean, String) -> Unit)? = null,
        onServiceStateChanged: ((Int, String) -> Unit)? = null,
        onScrapingComplete: ((String, Int) -> Unit)? = null
    ) {
        serviceCallback = object : IVoiceOSCallback.Stub() {
            override fun onCommandRecognized(command: String?, confidence: Float) {
                command?.let { onCommandRecognized?.invoke(it, confidence) }
            }

            override fun onCommandExecuted(command: String?, success: Boolean, message: String?) {
                command?.let { onCommandExecuted?.invoke(it, success, message ?: "") }
            }

            override fun onServiceStateChanged(state: Int, message: String?) {
                onServiceStateChanged?.invoke(state, message ?: "")
            }

            override fun onScrapingComplete(elementsJson: String?, elementCount: Int) {
                elementsJson?.let { onScrapingComplete?.invoke(it, elementCount) }
            }
        }

        // Register if already connected
        if (isBound.get()) {
            try {
                voiceOSService?.registerCallback(serviceCallback)
            } catch (e: RemoteException) {
                Log.e(TAG, "Failed to register callback: ${e.message}")
            }
        }
    }

    /**
     * Get current connection state
     */
    fun getConnectionState(): ConnectionState = connectionState

    /**
     * Check if connected and ready
     */
    fun isReady(): Boolean {
        return isBound.get() && voiceOSService != null && isServiceReady()
    }

    /**
     * Legacy connect method for compatibility
     */
    suspend fun connect(): Boolean = bind()

    /**
     * Legacy disconnect method for compatibility
     */
    fun disconnect() = unbind()
}
