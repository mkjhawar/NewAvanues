package com.augmentalis.webavanue.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.augmentalis.webavanue.universal.commands.WebAvanueActionMapper
import com.augmentalis.webavanue.universal.telemetry.SentryManager
import com.augmentalis.webavanue.universal.utils.Logger
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import com.augmentalis.webavanue.universal.presentation.controller.AndroidWebViewController
import com.augmentalis.webavanue.universal.presentation.viewmodel.TabViewModel
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.data.repository.BrowserRepositoryImpl
import com.augmentalis.webavanue.platform.createAndroidDriver
import com.augmentalis.webavanue.platform.DownloadCompletionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * WebAvanueApp - Application class for WebAvanue browser
 *
 * Responsibilities:
 * - Initialize global dependencies (database, repository, ViewModels)
 * - Register IPC receiver for VoiceOS commands
 * - Provide access to shared instances
 *
 * Voice Command Integration (Universal IPC Protocol v2.0.0):
 * - Uses centralized architecture (VoiceOS loads commands)
 * - Listens for IPC broadcasts: com.augmentalis.avanues.web.IPC.COMMAND
 * - Receives VCM (Voice Command) messages in format: VCM:commandId:command:params
 * - ActionMapper executes browser actions
 * - Graceful degradation if VoiceOS not available
 *
 * IPC Protocol Example:
 * - VoiceOS sends: "VCM:cmd123:SCROLL_TOP"
 * - WebAvanue receives broadcast on com.augmentalis.avanues.web.IPC.COMMAND
 * - Decodes: commandId="cmd123", command="SCROLL_TOP"
 * - Executes: actionMapper.executeAction("SCROLL_TOP")
 * - Responds: "ACC:cmd123" (accept) or "ERR:cmd123:error message"
 *
 * Spec: Universal IPC Protocol v2.0.0 (77 codes, 10 categories)
 * See: /Volumes/M-Drive/Coding/AVA/docs/UNIVERSAL-IPC-SPEC.md
 */
class WebAvanueApp : Application() {
    companion object {
        private const val TAG = "WebAvanueApp"

        // WebAvanue-specific IPC receiver
        // App-specific action for secure, isolated IPC communication
        private const val IPC_ACTION = "com.augmentalis.avanues.web.IPC.COMMAND"
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_SOURCE_APP = "source_app"
    }

    // Application-level CoroutineScope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // IPC Broadcast receiver for VoiceOS commands
    private var ipcReceiver: BroadcastReceiver? = null

    // Shared instances (lazy initialization)
    private val database: BrowserDatabase by lazy {
        // Read encryption setting from bootstrap preferences
        // This is stored separately from BrowserSettings to avoid chicken-and-egg problem
        val bootstrapPrefs = applicationContext.getSharedPreferences("webavanue_bootstrap", Context.MODE_PRIVATE)
        val useEncryption = bootstrapPrefs.getBoolean("database_encryption", false) // Default: unencrypted

        val driver = createAndroidDriver(applicationContext, useEncryption)
        BrowserDatabase(driver)
    }

    private val repository by lazy {
        BrowserRepositoryImpl(database)
    }

    private val tabViewModel by lazy {
        TabViewModel(repository)
    }

    private val webViewController by lazy {
        AndroidWebViewController(
            webViewProvider = { null }, // WebView provided by composable context
            tabViewModel = tabViewModel
        )
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "WebAvanueApp initializing...")

        // Initialize Napier logging framework
        Napier.base(DebugAntilog())
        Logger.info(TAG, "Napier logging initialized")

        // Initialize Sentry crash reporting
        // TODO: Replace with actual Sentry DSN from https://sentry.io dashboard
        // Get DSN: Sentry.io → Project Settings → Client Keys (DSN)
        val sentryDsn = "https://YOUR_PUBLIC_KEY@sentry.io/YOUR_PROJECT_ID"
        if (sentryDsn.contains("YOUR_")) {
            Logger.warn(TAG, "Sentry DSN not configured - crash reporting disabled")
        } else {
            SentryManager.init(applicationContext, sentryDsn)
            Logger.info(TAG, "Sentry crash reporting initialized")
        }

        // Initialize database (triggers migration if needed)
        database
        SentryManager.addBreadcrumb("app", "Database initialized")

        // Register IPC receiver for VoiceOS commands
        registerIPCReceiver()
        SentryManager.addBreadcrumb("app", "IPC receiver registered")

        // Inject repository provider for DownloadCompletionReceiver
        DownloadCompletionReceiver.repositoryProvider = { repository }
        Logger.info(TAG, "Download completion receiver repository provider configured")

        Logger.info(TAG, "WebAvanueApp initialized successfully")
    }

    override fun onTerminate() {
        super.onTerminate()

        // Unregister IPC receiver
        ipcReceiver?.let {
            try {
                unregisterReceiver(it)
                Log.d(TAG, "IPC receiver unregistered")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister IPC receiver", e)
            }
        }
    }

    /**
     * Register IPC receiver for VoiceOS commands
     *
     * Universal IPC Protocol v2.0.0 Specification
     * Spec: /Volumes/M-Drive/Coding/AVA/docs/UNIVERSAL-IPC-SPEC.md
     *
     * Architecture: Centralized Voice Commands
     * - VoiceOS CommandManager loads ALL commands from JSON (centralized)
     * - VoiceOS sends VCM (Voice Command) messages via IPC broadcast
     * - WebAvanue receives IPC on action: com.augmentalis.avanues.web.IPC.COMMAND
     * - WebAvanue decodes VCM, executes action via ActionMapper
     * - WebAvanue responds with ACC (accept) or ERR (error)
     *
     * VCM Format (Voice Category - Code #39 of 77):
     * - Standard: VCM:commandId:command
     * - Extended: VCM:commandId:command:param1:param2...
     * - Example: "VCM:cmd123:SCROLL_TOP"
     * - Example: "VCM:cmd456:SET_ZOOM_LEVEL:level=150"
     *
     * Complete Voice Command Flow:
     * 1. User says "scroll to top"
     * 2. VoiceOS Speech Recognition → "scroll to top"
     * 3. CommandLoader finds SCROLL_TOP in en-US.json (category: browser)
     * 4. CommandManager.routeCommandToApp() → targetApp: com.augmentalis.Avanues.web
     * 5. UniversalIPCEncoder.encodeVoiceCommand() → "VCM:cmd123:SCROLL_TOP"
     * 6. VoiceOS sends IPC broadcast: com.augmentalis.avanues.web.IPC.COMMAND
     * 7. WebAvanue BroadcastReceiver.onReceive() receives message
     * 8. Decode: commandId="cmd123", command="SCROLL_TOP"
     * 9. ActionMapper.executeAction("SCROLL_TOP")
     * 10. webViewController.scrollTop() executes
     * 11. WebAvanue sends response: "ACC:cmd123" or "ERR:cmd123:message"
     *
     * Graceful Degradation:
     * - If VoiceOS not installed: no broadcasts received, no errors
     * - WebAvanue continues to work with touch/keyboard input
     * - No runtime dependencies on VoiceOS SDK
     */
    private fun registerIPCReceiver() {
        try {
            // Create ActionMapper
            val actionMapper = WebAvanueActionMapper(tabViewModel, webViewController)

            // Create IPC broadcast receiver
            ipcReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action != IPC_ACTION) return

                    val message = intent.getStringExtra(EXTRA_MESSAGE) ?: return
                    val sourceApp = intent.getStringExtra(EXTRA_SOURCE_APP) ?: "unknown"

                    Log.d(TAG, "Received IPC from $sourceApp: $message")

                    // Parse VCM message: VCM:commandId:action:param1:param2...
                    val parts = message.split(':', limit = 4)
                    if (parts.size < 3) {
                        Log.w(TAG, "Invalid VCM format: $message")
                        return
                    }

                    val code = parts[0]
                    val commandId = unescape(parts[1])
                    val action = unescape(parts[2])
                    val paramsStr = parts.getOrNull(3)

                    // Verify VCM code
                    if (code != "VCM") {
                        Log.d(TAG, "Ignoring non-VCM message: $code")
                        return
                    }

                    // Parse parameters (format: key=value:key=value)
                    val params = mutableMapOf<String, Any>()
                    paramsStr?.split(':')?.forEach { param ->
                        val keyValue = param.split('=', limit = 2)
                        if (keyValue.size == 2) {
                            params[unescape(keyValue[0])] = unescape(keyValue[1])
                        }
                    }

                    // Execute action via ActionMapper
                    applicationScope.launch {
                        try {
                            // Add breadcrumb for voice command
                            SentryManager.addBreadcrumb("voice_command", "Executing: $action")

                            val result = actionMapper.executeAction(action, params)

                            // Send IPC response
                            val response = if (result.success) {
                                "ACC:${escape(commandId)}"
                            } else {
                                "ERR:${escape(commandId)}:${escape(result.message ?: "Unknown error")}"
                            }

                            sendIPCResponse(response, sourceApp)

                            Logger.info(TAG, "Command executed: $action (${if (result.success) "success" else "failed"})")

                        } catch (e: Exception) {
                            Logger.error(TAG, "Command execution failed: ${e.message}", e)
                            val response = "ERR:${escape(commandId)}:${escape(e.message ?: "Exception")}"
                            sendIPCResponse(response, sourceApp)
                        }
                    }
                }
            }

            // Register receiver for Universal IPC Protocol v2.0.0
            val filter = IntentFilter(IPC_ACTION)
            registerReceiver(ipcReceiver, filter, Context.RECEIVER_EXPORTED)

            Log.i(TAG, "✅ IPC receiver registered: $IPC_ACTION")
            Log.i(TAG, "   Protocol: Universal IPC v2.0.0 (VCM voice commands)")

        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to register IPC receiver", e)
            // Graceful degradation - WebAvanue works without voice
        }
    }

    /**
     * Send IPC response back to VoiceOS
     *
     * @param message IPC message (e.g., "ACC:cmd123" or "ERR:cmd123:message")
     * @param targetApp Target app package (usually VoiceOS)
     */
    private fun sendIPCResponse(message: String, targetApp: String) {
        try {
            val intent = Intent(IPC_ACTION).apply {
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_SOURCE_APP, packageName)
                setPackage(targetApp)
            }
            sendBroadcast(intent)
            Log.d(TAG, "Sent IPC response: $message")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send IPC response", e)
        }
    }

    /**
     * Escape special characters per Universal IPC Protocol
     */
    private fun escape(text: String): String {
        return text
            .replace("%", "%25")   // Escape character
            .replace(":", "%3A")   // Parameter delimiter
            .replace("\n", "%0A")  // Newline
            .replace("\r", "%0D")  // Carriage return
    }

    /**
     * Unescape special characters per Universal IPC Protocol
     */
    private fun unescape(text: String): String {
        return text
            .replace("%0D", "\r")  // Carriage return
            .replace("%0A", "\n")  // Newline
            .replace("%3A", ":")   // Parameter delimiter
            .replace("%25", "%")   // Escape character
    }

    /**
     * Get shared repository instance
     * Used by MainActivity to ensure consistent database/ViewModel state
     */
    fun provideRepository(): BrowserRepositoryImpl = repository

    /**
     * Get shared TabViewModel instance
     * Used by IPC ActionMapper for voice commands
     */
    fun provideTabViewModel(): TabViewModel = tabViewModel
}
