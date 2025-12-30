/**
 * MainActivity.kt - VoiceOS IPC Test Client
 *
 * Comprehensive test activity for VoiceOSCore IPC functionality.
 * Tests all 14 AIDL methods via service binding.
 *
 * Phase 3f: Manual IPC testing and verification
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-12
 */
package com.augmentalis.voiceos.ipctest

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.augmentalis.voiceoscore.accessibility.IVoiceOSCallback
import com.augmentalis.voiceoscore.accessibility.IVoiceOSService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main test activity for VoiceOS IPC
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VoiceOSIPCTest"
        private const val IPC_ACTION = "com.augmentalis.voiceoscore.BIND_IPC"
        private const val IPC_PACKAGE = "com.augmentalis.voiceoscore"
    }

    // IPC Service
    private var voiceOSService: IVoiceOSService? = null
    private var isServiceBound = false

    // UI Components
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var bindButton: Button
    private lateinit var unbindButton: Button

    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // JSON formatter
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            voiceOSService = IVoiceOSService.Stub.asInterface(service)
            isServiceBound = true
            updateStatus("✅ Connected to VoiceOS IPC Service")
            log("onServiceConnected: VoiceOS IPC Service connected")
            enableTestButtons(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceOSService = null
            isServiceBound = false
            updateStatus("❌ Disconnected from VoiceOS IPC Service")
            log("onServiceDisconnected: VoiceOS IPC Service disconnected")
            enableTestButtons(false)
        }
    }

    // Callback for service events
    private val voiceOSCallback = object : IVoiceOSCallback.Stub() {
        override fun onCommandRecognized(command: String?, confidence: Float) {
            runOnUiThread {
                log("📢 CALLBACK: onCommandRecognized(command='$command', confidence=$confidence)")
                Toast.makeText(
                    this@MainActivity,
                    "Recognized: $command (${(confidence * 100).toInt()}%)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onCommandExecuted(command: String?, success: Boolean, message: String?) {
            runOnUiThread {
                log("📢 CALLBACK: onCommandExecuted(command='$command', success=$success, message='$message')")
                Toast.makeText(
                    this@MainActivity,
                    "Command: $command → ${if (success) "✓" else "✗"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        override fun onServiceStateChanged(state: Int, message: String?) {
            runOnUiThread {
                val stateName = when (state) {
                    0 -> "STOPPED"
                    1 -> "STARTING"
                    2 -> "READY"
                    3 -> "ERROR"
                    else -> "UNKNOWN"
                }
                log("📢 CALLBACK: onServiceStateChanged(state=$stateName, message='$message')")
            }
        }

        override fun onScrapingComplete(elementsJson: String?, elementCount: Int) {
            runOnUiThread {
                log("📢 CALLBACK: onScrapingComplete(elementCount=$elementCount)")
                log("   Elements JSON: ${formatJson(elementsJson ?: "{}")}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI
        initializeViews()
        setupClickListeners()

        log("VoiceOS IPC Test Client Started")
        log("Phase 3f: IPC Testing & Verification")
        log("═".repeat(50))
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        logScroll = findViewById(R.id.logScroll)
        bindButton = findViewById(R.id.btnBind)
        unbindButton = findViewById(R.id.btnUnbind)

        updateStatus("⚪ Not connected")
        enableTestButtons(false)
    }

    private fun setupClickListeners() {
        // Service Binding
        bindButton.setOnClickListener { bindToService() }
        unbindButton.setOnClickListener { unbindFromService() }

        // Test: Service Status Methods
        findViewById<Button>(R.id.btnTestIsServiceReady).setOnClickListener {
            testIsServiceReady()
        }
        findViewById<Button>(R.id.btnTestGetServiceStatus).setOnClickListener {
            testGetServiceStatus()
        }
        findViewById<Button>(R.id.btnTestGetAvailableCommands).setOnClickListener {
            testGetAvailableCommands()
        }

        // Test: Command Execution
        findViewById<Button>(R.id.btnTestExecuteCommand).setOnClickListener {
            testExecuteCommand()
        }
        findViewById<Button>(R.id.btnTestExecuteAccessibilityAction).setOnClickListener {
            testExecuteAccessibilityAction()
        }

        // Test: Voice Recognition
        findViewById<Button>(R.id.btnTestStartVoiceRecognition).setOnClickListener {
            testStartVoiceRecognition()
        }
        findViewById<Button>(R.id.btnTestStopVoiceRecognition).setOnClickListener {
            testStopVoiceRecognition()
        }

        // Test: App Learning
        findViewById<Button>(R.id.btnTestLearnCurrentApp).setOnClickListener {
            testLearnCurrentApp()
        }
        findViewById<Button>(R.id.btnTestGetLearnedApps).setOnClickListener {
            testGetLearnedApps()
        }
        findViewById<Button>(R.id.btnTestGetCommandsForApp).setOnClickListener {
            testGetCommandsForApp()
        }

        // Test: Dynamic Commands
        findViewById<Button>(R.id.btnTestRegisterDynamicCommand).setOnClickListener {
            testRegisterDynamicCommand()
        }

        // Test: UI Scraping
        findViewById<Button>(R.id.btnTestScrapeCurrentScreen).setOnClickListener {
            testScrapeCurrentScreen()
        }

        // Test: Callbacks
        findViewById<Button>(R.id.btnTestRegisterCallback).setOnClickListener {
            testRegisterCallback()
        }
        findViewById<Button>(R.id.btnTestUnregisterCallback).setOnClickListener {
            testUnregisterCallback()
        }

        // Utility
        findViewById<Button>(R.id.btnClearLog).setOnClickListener {
            logText.text = ""
        }
        findViewById<Button>(R.id.btnRunAllTests).setOnClickListener {
            runAllTests()
        }
    }

    // ============================================================
    // Service Binding
    // ============================================================

    private fun bindToService() {
        if (isServiceBound) {
            log("⚠️  Already bound to service")
            return
        }

        log("🔄 Binding to VoiceOS IPC Service...")
        log("   Action: $IPC_ACTION")
        log("   Package: $IPC_PACKAGE")

        try {
            val intent = Intent().apply {
                action = IPC_ACTION
                `package` = IPC_PACKAGE
            }

            val bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (bound) {
                log("✅ bindService() returned true")
                bindButton.isEnabled = false
                unbindButton.isEnabled = true
            } else {
                log("❌ bindService() returned false")
                updateStatus("❌ Failed to bind service")
            }
        } catch (e: Exception) {
            log("❌ Exception binding service: ${e.message}")
            Log.e(TAG, "Error binding service", e)
        }
    }

    private fun unbindFromService() {
        if (!isServiceBound) {
            log("⚠️  Not bound to service")
            return
        }

        log("🔄 Unbinding from VoiceOS IPC Service...")
        try {
            unbindService(serviceConnection)
            voiceOSService = null
            isServiceBound = false
            updateStatus("⚪ Not connected")
            log("✅ Unbound successfully")
            bindButton.isEnabled = true
            unbindButton.isEnabled = false
            enableTestButtons(false)
        } catch (e: Exception) {
            log("❌ Exception unbinding service: ${e.message}")
            Log.e(TAG, "Error unbinding service", e)
        }
    }

    // ============================================================
    // Test Methods (14 AIDL Methods)
    // ============================================================

    private fun testIsServiceReady() {
        executeTest("isServiceReady()") {
            val ready = voiceOSService?.isServiceReady() ?: false
            log("   Result: $ready")
        }
    }

    private fun testGetServiceStatus() {
        executeTest("getServiceStatus()") {
            val status = voiceOSService?.serviceStatus ?: "{}"
            log("   Result: ${formatJson(status)}")
        }
    }

    private fun testGetAvailableCommands() {
        executeTest("getAvailableCommands()") {
            val commands = voiceOSService?.availableCommands ?: emptyList()
            log("   Result: ${commands.size} commands")
            commands.forEachIndexed { index, cmd ->
                log("      ${index + 1}. $cmd")
            }
        }
    }

    private fun testExecuteCommand() {
        executeTest("executeCommand(\"go back\")") {
            val success = voiceOSService?.executeCommand("go back") ?: false
            log("   Result: $success")
        }
    }

    private fun testExecuteAccessibilityAction() {
        executeTest("executeAccessibilityAction(\"click\", \"{}\")") {
            val success = voiceOSService?.executeAccessibilityAction("click", "{}") ?: false
            log("   Result: $success")
        }
    }

    private fun testStartVoiceRecognition() {
        executeTest("startVoiceRecognition(\"en-US\", \"continuous\")") {
            val success = voiceOSService?.startVoiceRecognition("en-US", "continuous") ?: false
            log("   Result: $success")
        }
    }

    private fun testStopVoiceRecognition() {
        executeTest("stopVoiceRecognition()") {
            val success = voiceOSService?.stopVoiceRecognition() ?: false
            log("   Result: $success")
        }
    }

    private fun testLearnCurrentApp() {
        executeTest("learnCurrentApp()") {
            val result = voiceOSService?.learnCurrentApp() ?: "{}"
            log("   Result: ${formatJson(result)}")
        }
    }

    private fun testGetLearnedApps() {
        executeTest("getLearnedApps()") {
            val apps = voiceOSService?.learnedApps ?: emptyList()
            log("   Result: ${apps.size} apps")
            apps.forEachIndexed { index, app ->
                log("      ${index + 1}. $app")
            }
        }
    }

    private fun testGetCommandsForApp() {
        executeTest("getCommandsForApp(\"com.android.settings\")") {
            val commands = voiceOSService?.getCommandsForApp("com.android.settings") ?: emptyList()
            log("   Result: ${commands.size} commands")
            commands.forEachIndexed { index, cmd ->
                log("      ${index + 1}. $cmd")
            }
        }
    }

    private fun testRegisterDynamicCommand() {
        executeTest("registerDynamicCommand(\"test command\", \"{}\")") {
            val success = voiceOSService?.registerDynamicCommand("test command", "{}") ?: false
            log("   Result: $success")
        }
    }

    private fun testScrapeCurrentScreen() {
        executeTest("scrapeCurrentScreen()") {
            val result = voiceOSService?.scrapeCurrentScreen() ?: "{}"
            log("   Result: ${formatJson(result)}")
        }
    }

    private fun testRegisterCallback() {
        executeTest("registerCallback()") {
            voiceOSService?.registerCallback(voiceOSCallback)
            log("   Result: Callback registered")
        }
    }

    private fun testUnregisterCallback() {
        executeTest("unregisterCallback()") {
            voiceOSService?.unregisterCallback(voiceOSCallback)
            log("   Result: Callback unregistered")
        }
    }

    // ============================================================
    // Run All Tests
    // ============================================================

    private fun runAllTests() {
        if (!isServiceBound) {
            log("❌ Cannot run tests: Service not bound")
            return
        }

        log("\n" + "═".repeat(50))
        log("🚀 Running All Tests (14 AIDL Methods)")
        log("═".repeat(50))

        scope.launch {
            try {
                // Service Status Methods (3)
                testIsServiceReady()
                delay(300)
                testGetServiceStatus()
                delay(300)
                testGetAvailableCommands()
                delay(300)

                // Command Execution (2)
                testExecuteCommand()
                delay(300)
                testExecuteAccessibilityAction()
                delay(300)

                // Voice Recognition (2)
                testStartVoiceRecognition()
                delay(2000) // Let it run for 2 seconds
                testStopVoiceRecognition()
                delay(300)

                // App Learning (3)
                testLearnCurrentApp()
                delay(300)
                testGetLearnedApps()
                delay(300)
                testGetCommandsForApp()
                delay(300)

                // Dynamic Commands (1)
                testRegisterDynamicCommand()
                delay(300)

                // UI Scraping (1)
                testScrapeCurrentScreen()
                delay(300)

                // Callbacks (2)
                testRegisterCallback()
                delay(300)
                testUnregisterCallback()

                log("═".repeat(50))
                log("✅ All Tests Complete!")
                log("═".repeat(50) + "\n")

            } catch (e: Exception) {
                log("❌ Test suite error: ${e.message}")
                Log.e(TAG, "Test suite error", e)
            }
        }
    }

    // ============================================================
    // Utilities
    // ============================================================

    private fun executeTest(methodName: String, block: () -> Unit) {
        if (!isServiceBound) {
            log("❌ Cannot test $methodName: Service not bound")
            return
        }

        log("\n▶️  Testing: $methodName")
        try {
            block()
        } catch (e: RemoteException) {
            log("   ❌ RemoteException: ${e.message}")
            Log.e(TAG, "RemoteException in $methodName", e)
        } catch (e: Exception) {
            log("   ❌ Exception: ${e.message}")
            Log.e(TAG, "Exception in $methodName", e)
        }
    }

    private suspend fun delay(millis: Long) {
        withContext(Dispatchers.IO) {
            Thread.sleep(millis)
        }
    }

    private fun log(message: String) {
        runOnUiThread {
            val timestamp = System.currentTimeMillis()
            logText.append("$message\n")
            logScroll.post {
                logScroll.fullScroll(View.FOCUS_DOWN)
            }
        }
        Log.d(TAG, message)
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            statusText.text = status
        }
    }

    private fun enableTestButtons(enabled: Boolean) {
        val testButtons = listOf(
            R.id.btnTestIsServiceReady,
            R.id.btnTestGetServiceStatus,
            R.id.btnTestGetAvailableCommands,
            R.id.btnTestExecuteCommand,
            R.id.btnTestExecuteAccessibilityAction,
            R.id.btnTestStartVoiceRecognition,
            R.id.btnTestStopVoiceRecognition,
            R.id.btnTestLearnCurrentApp,
            R.id.btnTestGetLearnedApps,
            R.id.btnTestGetCommandsForApp,
            R.id.btnTestRegisterDynamicCommand,
            R.id.btnTestScrapeCurrentScreen,
            R.id.btnTestRegisterCallback,
            R.id.btnTestUnregisterCallback,
            R.id.btnRunAllTests
        )

        testButtons.forEach { buttonId ->
            findViewById<Button>(buttonId)?.isEnabled = enabled
        }
    }

    private fun formatJson(json: String): String {
        return try {
            val obj = gson.fromJson(json, Any::class.java)
            gson.toJson(obj)
        } catch (e: Exception) {
            json
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindFromService()
        }
    }
}
