/**
 * LearnAppActivity.kt - Main activity for LearnApp standalone app
 *
 * Provides UI for manual app exploration and JIT service coordination.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: JIT-LearnApp Separation (Phase 3)
 *
 * ## Features:
 *
 * - App selection for exploration
 * - Start/stop exploration controls
 * - JIT statistics display (screens learned, elements discovered)
 * - Pause/resume JIT capture buttons
 * - AIDL binding to JIT service
 *
 * ## Architecture:
 *
 * ```
 * LearnAppActivity
 * ├─ Compose UI (Material 3)
 * ├─ ServiceConnection (AIDL binding to JIT)
 * ├─ ExplorationEngine (DFS exploration) [TODO: Phase 4]
 * └─ ViewState (UI state management)
 * ```
 *
 * @since 2.0.0 (JIT-LearnApp Separation)
 */

package com.augmentalis.learnapp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.jitlearning.IElementCaptureService
import com.augmentalis.jitlearning.JITState

/**
 * LearnApp Main Activity
 *
 * Launcher activity providing UI for app exploration and JIT coordination.
 */
class LearnAppActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LearnAppActivity"
    }

    // AIDL service binding
    private var jitService: IElementCaptureService? = null
    private var isBound = false

    // UI state
    private val jitState = mutableStateOf<JITState?>(null)
    private val isExploring = mutableStateOf(false)

    /**
     * Service connection for JIT service binding
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i(TAG, "Connected to JIT service")
            jitService = IElementCaptureService.Stub.asInterface(service)
            isBound = true
            updateJITState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "Disconnected from JIT service")
            jitService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "LearnApp activity created")

        // Bind to JIT service
        bindToJITService()

        // Set Compose UI
        setContent {
            MaterialTheme {
                LearnAppUI(
                    jitState = jitState.value,
                    isExploring = isExploring.value,
                    onStartExploration = { startExploration() },
                    onStopExploration = { stopExploration() },
                    onPauseJIT = { pauseJIT() },
                    onResumeJIT = { resumeJIT() },
                    onRefreshState = { updateJITState() }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    /**
     * Bind to JIT learning service
     */
    private fun bindToJITService() {
        val intent = Intent().apply {
            component = ComponentName(
                "com.augmentalis.voiceoscore",  // VoiceOSCore package
                "com.augmentalis.jitlearning.JITLearningService"
            )
        }

        try {
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.i(TAG, "Binding to JIT service...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bind to JIT service", e)
        }
    }

    /**
     * Update JIT state from service
     */
    private fun updateJITState() {
        try {
            jitState.value = jitService?.queryState()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query JIT state", e)
        }
    }

    /**
     * Start app exploration
     */
    private fun startExploration() {
        Log.i(TAG, "Starting exploration...")
        isExploring.value = true
        // TODO: Start ExplorationEngine in Phase 4
        // Pause JIT while exploring
        pauseJIT()
    }

    /**
     * Stop app exploration
     */
    private fun stopExploration() {
        Log.i(TAG, "Stopping exploration...")
        isExploring.value = false
        // TODO: Stop ExplorationEngine in Phase 4
        // Resume JIT after exploring
        resumeJIT()
    }

    /**
     * Pause JIT capture
     */
    private fun pauseJIT() {
        try {
            jitService?.pauseCapture()
            updateJITState()
            Log.i(TAG, "JIT capture paused")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause JIT", e)
        }
    }

    /**
     * Resume JIT capture
     */
    private fun resumeJIT() {
        try {
            jitService?.resumeCapture()
            updateJITState()
            Log.i(TAG, "JIT capture resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume JIT", e)
        }
    }
}

/**
 * LearnApp Compose UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnAppUI(
    jitState: JITState?,
    isExploring: Boolean,
    onStartExploration: () -> Unit,
    onStopExploration: () -> Unit,
    onPauseJIT: () -> Unit,
    onResumeJIT: () -> Unit,
    onRefreshState: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LearnApp Explorer") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // JIT Status Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("JIT Learning Status", style = MaterialTheme.typography.titleMedium)

                    if (jitState != null) {
                        Text("Active: ${if (jitState.isActive) "Yes" else "Paused"}")
                        Text("Screens Learned: ${jitState.screensLearned}")
                        Text("Elements Discovered: ${jitState.elementsDiscovered}")
                        jitState.currentPackage?.let {
                            Text("Current Package: $it")
                        }
                    } else {
                        Text("Not connected to JIT service")
                    }

                    Button(onClick = onRefreshState) {
                        Text("Refresh")
                    }
                }
            }

            // JIT Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPauseJIT,
                    enabled = jitState?.isActive == true
                ) {
                    Text("Pause JIT")
                }

                Button(
                    onClick = onResumeJIT,
                    enabled = jitState?.isActive == false
                ) {
                    Text("Resume JIT")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exploration Controls
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("App Exploration", style = MaterialTheme.typography.titleMedium)

                    Text("Status: ${if (isExploring) "Exploring..." else "Idle"}")

                    if (isExploring) {
                        Button(onClick = onStopExploration) {
                            Text("Stop Exploration")
                        }
                    } else {
                        Button(onClick = onStartExploration) {
                            Text("Start Exploration")
                        }
                    }
                }
            }
        }
    }
}
