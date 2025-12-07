/**
 * NotificationSystem.kt - Custom notification system replacing Android defaults
 * Replaces: Toast, Snackbar, AlertDialog, BottomSheet, PopupMenu, ProgressDialog
 */

package com.augmentalis.voiceui.notifications

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.voiceui.theme.ThemeEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Comprehensive notification system with voice readout support
 * Replaces all Android default notification components
 */
class NotificationSystem(
    private val context: Context,
    private val themeEngine: ThemeEngine
) {
    
    companion object {
        private const val TAG = "VoiceUINotificationSystem"
        private const val DEFAULT_DURATION = 3000L
        private const val ANIMATION_DURATION = 300
    }
    
    // Notification types
    enum class NotificationType {
        TOAST,
        SNACKBAR,
        ALERT,
        BOTTOM_SHEET,
        POPUP,
        PROGRESS,
        INPUT,
        CUSTOM
    }
    
    // Notification priority levels
    enum class Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
    
    // Notification data class
    data class VoiceUINotification(
        val id: String = UUID.randomUUID().toString(),
        val type: NotificationType,
        val title: String? = null,
        val message: String,
        val priority: Priority = Priority.NORMAL,
        val duration: Long = DEFAULT_DURATION,
        val voiceReadout: Boolean = true,
        val actions: List<NotificationAction> = emptyList(),
        val customView: (@Composable () -> Unit)? = null,
        val position: NotificationPosition = NotificationPosition.TOP,
        val isDismissible: Boolean = true,
        val progress: Float? = null
    )
    
    // Notification action
    data class NotificationAction(
        val label: String,
        val voiceCommand: String? = null,
        val onClick: () -> Unit
    )
    
    // Notification position
    enum class NotificationPosition {
        TOP,
        CENTER,
        BOTTOM,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
    
    // State management
    private val _activeNotifications = MutableStateFlow<List<VoiceUINotification>>(emptyList())
    val activeNotifications: StateFlow<List<VoiceUINotification>> = _activeNotifications
    
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    /**
     * Show a VoiceUI Toast notification
     */
    fun showToast(
        message: String,
        duration: Long = 2000L,
        position: NotificationPosition = NotificationPosition.BOTTOM,
        voiceReadout: Boolean = true
    ) {
        show(VoiceUINotification(
            type = NotificationType.TOAST,
            message = message,
            duration = duration,
            position = position,
            voiceReadout = voiceReadout
        ))
    }
    
    /**
     * Show a VoiceUI Snackbar notification
     */
    fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        duration: Long = 4000L,
        voiceCommand: String? = null
    ) {
        val actions = if (actionLabel != null && onAction != null) {
            listOf(NotificationAction(actionLabel, voiceCommand, onAction))
        } else {
            emptyList()
        }
        
        show(VoiceUINotification(
            type = NotificationType.SNACKBAR,
            message = message,
            duration = duration,
            position = NotificationPosition.BOTTOM,
            actions = actions
        ))
    }
    
    /**
     * Show a VoiceUI Alert Dialog
     */
    fun showAlert(
        title: String,
        message: String,
        positiveButton: String = "OK",
        negativeButton: String? = null,
        onPositive: () -> Unit = {},
        onNegative: () -> Unit = {},
        isDismissible: Boolean = true
    ) {
        val actions = mutableListOf(
            NotificationAction(positiveButton, "confirm", onPositive)
        )
        
        negativeButton?.let {
            actions.add(NotificationAction(it, "cancel", onNegative))
        }
        
        show(VoiceUINotification(
            type = NotificationType.ALERT,
            title = title,
            message = message,
            duration = 0L, // No auto-dismiss
            position = NotificationPosition.CENTER,
            actions = actions,
            isDismissible = isDismissible,
            priority = Priority.HIGH
        ))
    }
    
    /**
     * Show a VoiceUI Bottom Sheet
     */
    fun showBottomSheet(
        title: String? = null,
        content: @Composable () -> Unit,
        isDismissible: Boolean = true
    ) {
        show(VoiceUINotification(
            type = NotificationType.BOTTOM_SHEET,
            title = title,
            message = "",
            duration = 0L,
            position = NotificationPosition.BOTTOM,
            customView = content,
            isDismissible = isDismissible
        ))
    }
    
    /**
     * Show a VoiceUI Progress notification
     */
    fun showProgress(
        message: String,
        progress: Float? = null,
        isIndeterminate: Boolean = false
    ) {
        show(VoiceUINotification(
            type = NotificationType.PROGRESS,
            message = message,
            duration = 0L,
            position = NotificationPosition.CENTER,
            isDismissible = false,
            progress = if (isIndeterminate) null else progress,
            priority = Priority.HIGH
        ))
    }
    
    /**
     * Show a VoiceUI Input Dialog
     */
    fun showInputDialog(
        title: String,
        hint: String = "",
        initialValue: String = "",
        onSubmit: (String) -> Unit,
        voiceDictation: Boolean = true
    ) {
        var inputValue by mutableStateOf(initialValue)
        
        val content: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    label = { Text(hint) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { dismiss("input_dialog") }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { 
                        onSubmit(inputValue)
                        dismiss("input_dialog")
                    }) {
                        Text("Submit")
                    }
                }
            }
        }
        
        show(VoiceUINotification(
            id = "input_dialog",
            type = NotificationType.INPUT,
            message = "",
            duration = 0L,
            position = NotificationPosition.CENTER,
            customView = content,
            isDismissible = true
        ))
    }
    
    /**
     * Show a VoiceUI Popup Menu
     */
    fun showPopupMenu(
        items: List<String>,
        onItemSelected: (Int, String) -> Unit,
        position: NotificationPosition = NotificationPosition.TOP_RIGHT
    ) {
        val content: @Composable () -> Unit = {
            Card(
                modifier = Modifier
                    .widthIn(min = 150.dp, max = 250.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column {
                    items.forEachIndexed { index, item ->
                        TextButton(
                            onClick = { 
                                onItemSelected(index, item)
                                dismiss("popup_menu")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                        if (index < items.size - 1) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        
        show(VoiceUINotification(
            id = "popup_menu",
            type = NotificationType.POPUP,
            message = "",
            duration = 0L,
            position = position,
            customView = content,
            isDismissible = true
        ))
    }
    
    /**
     * Core show notification method
     */
    private fun show(notification: VoiceUINotification) {
        scope.launch {
            // Add to active notifications
            _activeNotifications.value = _activeNotifications.value + notification
            
            // Voice readout if enabled
            if (notification.voiceReadout) {
                speakNotification(notification)
            }
            
            // Create or update overlay
            ensureOverlay()
            
            // Auto-dismiss if duration is set
            if (notification.duration > 0) {
                delay(notification.duration)
                dismiss(notification.id)
            }
        }
    }
    
    /**
     * Dismiss a notification
     */
    fun dismiss(notificationId: String) {
        _activeNotifications.value = _activeNotifications.value.filter { it.id != notificationId }
        
        // Remove overlay if no active notifications
        if (_activeNotifications.value.isEmpty()) {
            removeOverlay()
        }
    }
    
    /**
     * Dismiss all notifications
     */
    fun dismissAll() {
        _activeNotifications.value = emptyList()
        removeOverlay()
    }
    
    /**
     * Ensure overlay view exists
     */
    private fun ensureOverlay() {
        if (overlayView != null) return
        
        // Check for overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
                Log.e(TAG, "Overlay permission not granted")
                return
            }
        }
        
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        val composeView = ComposeView(context).apply {
            setContent {
                themeEngine.VoiceUITheme {
                    NotificationOverlay()
                }
            }
        }
        
        overlayView = composeView
        windowManager?.addView(overlayView, params)
    }
    
    /**
     * Remove overlay view
     */
    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
    }
    
    /**
     * Notification overlay composable
     */
    @Composable
    private fun NotificationOverlay() {
        val notifications by activeNotifications.collectAsState()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            notifications.forEach { notification ->
                NotificationView(
                    notification = notification,
                    modifier = Modifier.align(getAlignment(notification.position))
                )
            }
        }
    }
    
    /**
     * Individual notification view
     */
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun NotificationView(
        notification: VoiceUINotification,
        modifier: Modifier = Modifier
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = modifier
        ) {
            when (notification.type) {
                NotificationType.TOAST -> ToastView(notification)
                NotificationType.SNACKBAR -> SnackbarView(notification)
                NotificationType.ALERT -> AlertView(notification)
                NotificationType.PROGRESS -> ProgressView(notification)
                else -> notification.customView?.invoke() ?: Box {}
            }
        }
    }
    
    @Composable
    private fun ToastView(notification: VoiceUINotification) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 300.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.inverseSurface
            )
        ) {
            Text(
                text = notification.message,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    
    @Composable
    private fun SnackbarView(notification: VoiceUINotification) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = notification.actions.firstOrNull()?.let { action ->
                {
                    TextButton(onClick = action.onClick) {
                        Text(action.label)
                    }
                }
            }
        ) {
            Text(notification.message)
        }
    }
    
    @Composable
    private fun AlertView(notification: VoiceUINotification) {
        AlertDialog(
            onDismissRequest = { if (notification.isDismissible) dismiss(notification.id) },
            title = notification.title?.let { { Text(it) } },
            text = { Text(notification.message) },
            confirmButton = {
                notification.actions.firstOrNull()?.let { action ->
                    TextButton(onClick = {
                        action.onClick()
                        dismiss(notification.id)
                    }) {
                        Text(action.label)
                    }
                }
            },
            dismissButton = {
                notification.actions.getOrNull(1)?.let { action ->
                    TextButton(onClick = {
                        action.onClick()
                        dismiss(notification.id)
                    }) {
                        Text(action.label)
                    }
                }
            }
        )
    }
    
    @Composable
    private fun ProgressView(notification: VoiceUINotification) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(min = 200.dp, max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (notification.progress == null) {
                    CircularProgressIndicator()
                } else {
                    LinearProgressIndicator(
                        progress = { notification.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(notification.message)
            }
        }
    }
    
    private fun getAlignment(position: NotificationPosition): Alignment {
        return when (position) {
            NotificationPosition.TOP -> Alignment.TopCenter
            NotificationPosition.CENTER -> Alignment.Center
            NotificationPosition.BOTTOM -> Alignment.BottomCenter
            NotificationPosition.TOP_LEFT -> Alignment.TopStart
            NotificationPosition.TOP_RIGHT -> Alignment.TopEnd
            NotificationPosition.BOTTOM_LEFT -> Alignment.BottomStart
            NotificationPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        }
    }
    
    private fun speakNotification(notification: VoiceUINotification) {
        // TODO: Integrate with TTS system
        val text = buildString {
            notification.title?.let { append("$it. ") }
            append(notification.message)
            if (notification.actions.isNotEmpty()) {
                append(". Available actions: ")
                append(notification.actions.joinToString(", ") { it.label })
            }
        }
        // TTS.speak(text)
    }
    
    fun shutdown() {
        dismissAll()
        scope.cancel()
    }
    
    // Additional data class for provider support
    data class Notification(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val message: String,
        val style: String = "STANDARD",
        val actions: List<String> = emptyList(),
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // Additional methods for intent/provider support
    fun showCustomNotification(
        notificationId: String,
        title: String,
        message: String,
        style: String,
        actions: List<String>
    ) {
        val notificationActions = actions.map { action ->
            NotificationAction(label = action, onClick = {})
        }
        
        val type = when(style) {
            "SPATIAL" -> NotificationType.CUSTOM
            "MINIMAL" -> NotificationType.TOAST
            else -> NotificationType.SNACKBAR
        }
        
        show(VoiceUINotification(
            id = notificationId,
            type = type,
            title = title,
            message = message,
            actions = notificationActions
        ))
    }
    
    fun clear(notificationId: String) {
        dismiss(notificationId)
    }
    
    fun clearAll() {
        dismissAll()
    }
    
    fun getActiveNotifications(): List<Notification> {
        return _activeNotifications.value.map { notification ->
            Notification(
                id = notification.id,
                title = notification.title ?: "",
                message = notification.message,
                style = notification.type.name,
                actions = notification.actions.map { it.label },
                timestamp = System.currentTimeMillis()
            )
        }
    }
}