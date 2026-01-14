/**
 * CursorIntegration.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/integration/CursorIntegration.kt
 * 
 * Created: 2025-09-05
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Integration example showing how to use the enhanced cursor features
 * Module: VoiceCursor System
 */

package com.augmentalis.voiceos.cursor.integration

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceos.cursor.calibration.ClickAccuracyManager
import com.augmentalis.voiceos.cursor.calibration.ClickTargetResult
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.core.CursorPositionManager
import com.augmentalis.voiceos.cursor.view.*
import com.augmentalis.voiceos.speech.help.SpeechRecognitionHelpMenu
import kotlinx.coroutines.delay

/**
 * Enhanced cursor integration showing all new features working together
 */
@Composable
fun EnhancedCursorSystem(
    cursorPosition: CursorOffset,
    screenWidth: Int,
    screenHeight: Int,
    rootView: View?,
    isMenuVisible: Boolean,
    onMenuDismiss: () -> Unit,
    onMenuAction: (CursorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // State management
    var isAtEdge by remember { mutableStateOf(false) }
    var edgeType by remember { mutableStateOf(EdgeType.NONE) }
    var targetAssistance by remember { mutableStateOf<ClickTargetResult?>(null) }
    
    // Managers
    val clickAccuracyManager = remember { ClickAccuracyManager(context) }
    val speechHelpMenu = remember { SpeechRecognitionHelpMenu(context) }
    
    // Apply calibration to cursor position
    val calibratedPosition = remember(cursorPosition) {
        clickAccuracyManager.applyCalibratedPosition(
            Offset(cursorPosition.x, cursorPosition.y)
        )
    }
    
    // Find click targets near cursor
    LaunchedEffect(calibratedPosition, rootView) {
        while (true) {
            rootView?.let { root ->
                targetAssistance = clickAccuracyManager.findBestTarget(calibratedPosition, root)
            }
            delay(100) // Update every 100ms
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Edge visual feedback
        EdgeVisualFeedback(
            isAtEdge = isAtEdge,
            edgeType = edgeType,
            screenWidth = screenWidth,
            screenHeight = screenHeight
        )
        
        // Click target assistance
        targetAssistance?.let { target ->
            ClickTargetAssistance(
                targetPosition = target.snapPosition,
                confidence = target.confidence
            )
        }
        
        // Enhanced radial menu
        MenuView(
            isVisible = isMenuVisible,
            position = cursorPosition,
            onAction = { action ->
                when (action) {
                    CursorAction.SHOW_HELP -> {
                        speechHelpMenu.showHelpMenu()
                    }
                    CursorAction.SHOW_SETTINGS -> {
                        showCursorSettings(context, clickAccuracyManager)
                    }
                    CursorAction.CALIBRATE_CLICK -> {
                        startClickCalibration(clickAccuracyManager, cursorPosition)
                    }
                    else -> {
                        onMenuAction(action)
                    }
                }
            },
            onDismiss = onMenuDismiss
        )
        
        // Calibration status display
        if (!clickAccuracyManager.isCalibrationReliable()) {
            CalibrationStatusOverlay(
                manager = clickAccuracyManager,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Calibration status overlay
 */
@Composable
fun CalibrationStatusOverlay(
    manager: ClickAccuracyManager,
    modifier: Modifier = Modifier
) {
    val status = remember(manager) { manager.getCalibrationStatus() }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0x80000000)
        )
    ) {
        Text(
            text = "Click Accuracy: $status",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * Show cursor settings with calibration info
 */
fun showCursorSettings(context: Context, manager: ClickAccuracyManager) {
    val calibrationData = manager.getCalibrationData()
    val status = manager.getCalibrationStatus()
    
    val settingsText = """
        |Cursor Settings
        |
        |Click Accuracy: $status
        |Sample Count: ${calibrationData.sampleCount}
        |Offset X: ${String.format("%.1f", calibrationData.offsetX)}px
        |Offset Y: ${String.format("%.1f", calibrationData.offsetY)}px
        |Accuracy Score: ${(calibrationData.accuracy * 100).toInt()}%
        |
        |Actions:
        |• Use "Calibrate Click" from menu to improve accuracy
        |• Reset calibration if accuracy is poor
        |• More samples = better accuracy
    """.trimMargin()
    
    android.app.AlertDialog.Builder(context)
        .setTitle("Cursor Settings")
        .setMessage(settingsText)
        .setPositiveButton("OK", null)
        .setNeutralButton("Reset Calibration") { _, _ ->
            manager.resetCalibration()
        }
        .show()
}

/**
 * Start click calibration process
 */
fun startClickCalibration(manager: ClickAccuracyManager, cursorPosition: CursorOffset) {
    // This would typically be handled by the cursor click handler
    // For demo purposes, we'll simulate adding a calibration sample
    val intendedPosition = Offset(cursorPosition.x, cursorPosition.y)
    
    // In real implementation, this would be the actual click position
    // detected by accessibility service or touch system
    val simulatedActualPosition = Offset(
        cursorPosition.x + (Math.random() * 20 - 10).toFloat(),
        cursorPosition.y + (Math.random() * 20 - 10).toFloat()
    )
    
    manager.addCalibrationSample(intendedPosition, simulatedActualPosition)
}

/**
 * Enhanced position manager integration
 */
class EnhancedCursorPositionManager(
    screenWidth: Int,
    screenHeight: Int
) : CursorPositionManager(screenWidth, screenHeight) {
    
    private var edgeCallback: ((Boolean, EdgeType) -> Unit)? = null
    
    fun setEdgeDetectionCallback(callback: (Boolean, EdgeType) -> Unit) {
        edgeCallback = callback
    }
    
    override fun calculatePosition(
        alpha: Float,
        beta: Float,
        gamma: Float,
        timestamp: Long,
        speedFactor: Int
    ): PositionResult {
        val result = super.calculatePosition(alpha, beta, gamma, timestamp, speedFactor)
        
        // Notify about edge detection
        edgeCallback?.invoke(result.edgeDetected, result.edgeType)
        
        return result
    }
}

/**
 * Complete integration helper
 */
class CursorSystemIntegration(private val context: Context) {
    
    private val clickAccuracyManager = ClickAccuracyManager(context)
    private val speechHelpMenu = SpeechRecognitionHelpMenu(context)
    
    fun getClickAccuracyManager(): ClickAccuracyManager = clickAccuracyManager
    fun getSpeechHelpMenu(): SpeechRecognitionHelpMenu = speechHelpMenu
    
    /**
     * Handle cursor click with accuracy improvement
     */
    fun handleCursorClick(
        cursorPosition: CursorOffset,
        rootView: View?,
        onActualClick: (Offset) -> Unit
    ) {
        val rawPosition = Offset(cursorPosition.x, cursorPosition.y)
        
        // Apply calibration
        val calibratedPosition = clickAccuracyManager.applyCalibratedPosition(rawPosition)
        
        // Check for target assistance
        val targetResult = rootView?.let { 
            clickAccuracyManager.findBestTarget(calibratedPosition, it) 
        }
        
        // Use snap position if available and confident
        val finalPosition = if (targetResult != null && targetResult.confidence > 0.7f) {
            targetResult.snapPosition
        } else {
            calibratedPosition
        }
        
        // Perform the actual click
        onActualClick(finalPosition)
        
        // Add calibration sample (in real implementation, this would use the
        // actual detected click position from accessibility service)
        clickAccuracyManager.addCalibrationSample(rawPosition, finalPosition)
    }
    
    /**
     * Show appropriate help based on context
     */
    fun showContextualHelp(context: String) {
        when (context) {
            "speech" -> speechHelpMenu.showHelpMenu()
            "cursor" -> speechHelpMenu.showCategory(0) // Show cursor commands
            "calibration" -> showCalibrationHelp()
            else -> speechHelpMenu.showQuickReference()
        }
    }
    
    private fun showCalibrationHelp() {
        val helpText = """
            |Click Calibration Help
            |
            |The cursor learns from your clicking patterns to improve accuracy.
            |
            |• Use the cursor normally - it automatically learns
            |• Select "Calibrate Click" from menu for manual calibration
            |• More samples = better accuracy
            |• Reset calibration if it becomes less accurate
            |
            |Current Status: ${clickAccuracyManager.getCalibrationStatus()}
        """.trimMargin()
        
        android.app.AlertDialog.Builder(context)
            .setTitle("Click Calibration")
            .setMessage(helpText)
            .setPositiveButton("OK", null)
            .show()
    }
}

/**
 * Click target assistance visual indicator
 */
@Composable
fun ClickTargetAssistance(
    targetPosition: Offset?,
    confidence: Float,
    modifier: Modifier = Modifier
) {
    if (targetPosition != null && confidence > 0.3f) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // Target indicator
            Box(
                modifier = Modifier
                    .offset(
                        x = (targetPosition.x - 12).dp,
                        y = (targetPosition.y - 12).dp
                    )
                    .size(24.dp)
                    .background(
                        Color.Green.copy(alpha = 0.3f * confidence),
                        shape = CircleShape
                    )
            )
        }
    }
}