/**
 * Dialog Queue Manager
 *
 * Manages a queue of dialogs to prevent overlapping modals.
 * Ensures only one dialog is shown at a time with proper sequencing.
 *
 * Created: 2025-12-03
 * Author: AVA AI Team
 */

package com.augmentalis.overlay.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.LinkedList
import com.augmentalis.ava.core.data.util.AvidHelper

/**
 * Priority levels for dialogs
 */
enum class DialogPriority {
    /** Low priority - can be dismissed by higher priority */
    LOW,

    /** Normal priority - default for most dialogs */
    NORMAL,

    /** High priority - important user actions */
    HIGH,

    /** Critical - errors, permissions, must be acknowledged */
    CRITICAL
}

/**
 * Dialog request to be queued
 */
data class DialogRequest(
    val id: String = AvidHelper.randomDialogAVID(),
    val title: String,
    val message: String,
    val priority: DialogPriority = DialogPriority.NORMAL,
    val primaryAction: DialogAction? = null,
    val secondaryAction: DialogAction? = null,
    val dismissOnOutsideClick: Boolean = true,
    val autoDismissMs: Long? = null,
    val onShow: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
)

/**
 * Dialog button action
 */
data class DialogAction(
    val label: String,
    val onClick: () -> Unit
)

/**
 * Current dialog state
 */
sealed class DialogState {
    /** No dialog shown */
    object Hidden : DialogState()

    /** Dialog currently visible */
    data class Showing(val dialog: DialogRequest) : DialogState()
}

/**
 * Singleton manager for dialog queuing
 *
 * Features:
 * - FIFO queue with priority override
 * - Auto-dismiss support
 * - State flow for UI observation
 * - Thread-safe operations
 */
object DialogQueueManager {
    private const val TAG = "DialogQueue"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutex = Mutex()

    /** Queue of pending dialogs */
    private val queue = LinkedList<DialogRequest>()

    /** Currently displayed dialog */
    private var currentDialog: DialogRequest? = null

    /** Observable state for UI */
    private val _state = MutableStateFlow<DialogState>(DialogState.Hidden)
    val state: StateFlow<DialogState> = _state.asStateFlow()

    /** Statistics */
    private var dialogsShown = 0
    private var dialogsDismissed = 0

    /**
     * Enqueue a dialog request
     *
     * If no dialog is currently shown, it will be displayed immediately.
     * Otherwise, it's added to the queue.
     *
     * Higher priority dialogs will preempt lower priority ones.
     *
     * @param dialog Dialog request to enqueue
     */
    fun enqueue(dialog: DialogRequest) {
        scope.launch {
            mutex.withLock {
                val current = currentDialog

                when {
                    // No current dialog - show immediately
                    current == null -> {
                        show(dialog)
                    }

                    // New dialog has higher priority - preempt current
                    dialog.priority > current.priority -> {
                        Timber.d("$TAG: Preempting ${current.id} with higher priority ${dialog.id}")
                        // Re-queue current dialog
                        queue.addFirst(current)
                        show(dialog)
                    }

                    // Same or lower priority - add to queue based on priority
                    else -> {
                        val insertIndex = queue.indexOfFirst { it.priority < dialog.priority }
                        if (insertIndex >= 0) {
                            queue.add(insertIndex, dialog)
                        } else {
                            queue.addLast(dialog)
                        }
                        Timber.d("$TAG: Queued '${dialog.id}' (queue size: ${queue.size})")
                    }
                }
            }
        }
    }

    /**
     * Dismiss current dialog and show next in queue
     *
     * @param dialogId Optional - only dismiss if this ID matches current
     */
    fun dismiss(dialogId: String? = null) {
        scope.launch {
            mutex.withLock {
                val current = currentDialog ?: return@withLock

                // If dialogId specified, only dismiss if it matches
                if (dialogId != null && current.id != dialogId) {
                    Timber.d("$TAG: Dismiss skipped - ID mismatch")
                    return@withLock
                }

                Timber.d("$TAG: Dismissing '${current.id}'")
                current.onDismiss?.invoke()
                dialogsDismissed++

                currentDialog = null

                // Show next in queue if available
                val next = queue.pollFirst()
                if (next != null) {
                    show(next)
                } else {
                    _state.value = DialogState.Hidden
                }
            }
        }
    }

    /**
     * Dismiss all dialogs and clear queue
     */
    fun dismissAll() {
        scope.launch {
            mutex.withLock {
                currentDialog?.onDismiss?.invoke()
                currentDialog = null

                queue.forEach { it.onDismiss?.invoke() }
                queue.clear()

                _state.value = DialogState.Hidden
                Timber.d("$TAG: Dismissed all dialogs")
            }
        }
    }

    /**
     * Remove a specific dialog from the queue.
     *
     * This is a suspending function so it can wait for the mutex and return the actual result.
     * The previous non-suspend implementation used `scope.launch { }` which always returned
     * `false` immediately because the lambda executed asynchronously after the function returned.
     *
     * @param dialogId ID of dialog to remove
     * @return true if dialog was found in the queue and removed
     */
    suspend fun removeFromQueue(dialogId: String): Boolean {
        return withContext(Dispatchers.Main) {
            mutex.withLock {
                val iterator = queue.iterator()
                var removed = false
                while (iterator.hasNext()) {
                    if (iterator.next().id == dialogId) {
                        iterator.remove()
                        removed = true
                        Timber.d("$TAG: Removed '$dialogId' from queue")
                        break
                    }
                }
                removed
            }
        }
    }

    /**
     * Check if a dialog is currently showing
     */
    fun isShowing(): Boolean = currentDialog != null

    /**
     * Check if a specific dialog is currently showing
     */
    fun isShowing(dialogId: String): Boolean = currentDialog?.id == dialogId

    /**
     * Get queue size
     */
    fun queueSize(): Int = queue.size

    /**
     * Get statistics
     */
    fun getStats(): Map<String, Any> = mapOf(
        "dialogsShown" to dialogsShown,
        "dialogsDismissed" to dialogsDismissed,
        "queueSize" to queue.size,
        "isShowing" to isShowing()
    )

    /**
     * Clear queue without dismissing current dialog
     */
    fun clearQueue() {
        scope.launch {
            mutex.withLock {
                queue.forEach { it.onDismiss?.invoke() }
                queue.clear()
                Timber.d("$TAG: Queue cleared")
            }
        }
    }

    /**
     * Internal: Show a dialog
     */
    private fun show(dialog: DialogRequest) {
        currentDialog = dialog
        _state.value = DialogState.Showing(dialog)
        dialogsShown++

        dialog.onShow?.invoke()
        Timber.d("$TAG: Showing '${dialog.id}' (priority: ${dialog.priority})")

        // Schedule auto-dismiss if configured
        dialog.autoDismissMs?.let { delayMs ->
            scope.launch {
                kotlinx.coroutines.delay(delayMs)
                dismiss(dialog.id)
            }
        }
    }

    /**
     * Convenience builders
     */
    object Builder {
        /**
         * Create an info dialog
         */
        fun info(
            title: String,
            message: String,
            onDismiss: (() -> Unit)? = null
        ) = DialogRequest(
            title = title,
            message = message,
            priority = DialogPriority.NORMAL,
            primaryAction = DialogAction("OK") { dismiss() },
            onDismiss = onDismiss
        )

        /**
         * Create a confirmation dialog
         */
        fun confirm(
            title: String,
            message: String,
            onConfirm: () -> Unit,
            onCancel: (() -> Unit)? = null
        ) = DialogRequest(
            title = title,
            message = message,
            priority = DialogPriority.NORMAL,
            primaryAction = DialogAction("Confirm") {
                onConfirm()
                dismiss()
            },
            secondaryAction = DialogAction("Cancel") {
                onCancel?.invoke()
                dismiss()
            },
            dismissOnOutsideClick = false
        )

        /**
         * Create an error dialog
         */
        fun error(
            title: String = "Error",
            message: String,
            onDismiss: (() -> Unit)? = null
        ) = DialogRequest(
            title = title,
            message = message,
            priority = DialogPriority.HIGH,
            primaryAction = DialogAction("OK") { dismiss() },
            dismissOnOutsideClick = false,
            onDismiss = onDismiss
        )

        /**
         * Create a critical alert
         */
        fun critical(
            title: String,
            message: String,
            action: DialogAction
        ) = DialogRequest(
            title = title,
            message = message,
            priority = DialogPriority.CRITICAL,
            primaryAction = action,
            dismissOnOutsideClick = false
        )

        /**
         * Create a toast-like auto-dismiss dialog
         */
        fun toast(
            message: String,
            durationMs: Long = 3000
        ) = DialogRequest(
            title = "",
            message = message,
            priority = DialogPriority.LOW,
            dismissOnOutsideClick = true,
            autoDismissMs = durationMs
        )
    }
}
