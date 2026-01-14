package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.renderer.ios.SwiftUIRenderer

/**
 * iOS Phase 2 Feedback Component Mappers
 *
 * Maps AvaElements feedback components to SwiftUI bridge models:
 * - Alert → SwiftUI Alert
 * - Toast → Custom Toast overlay
 * - Snackbar → Custom Snackbar
 * - Modal → SwiftUI Sheet
 * - Dialog → SwiftUI Alert/Dialog
 * - Banner → Custom Banner view
 * - ContextMenu → SwiftUI Menu/ContextMenu
 */

// ============================================================================
// ALERT MAPPER
// ============================================================================

/**
 * Maps AlertComponent to SwiftUI Alert
 *
 * SwiftUI equivalent:
 * ```swift
 * .alert(title, isPresented: $showAlert) {
 *     Button("OK", role: .cancel) { }
 *     Button("Confirm") { }
 * } message: {
 *     Text(message)
 * }
 * ```
 */
fun mapAlertToSwiftUI(
    component: AlertComponent,
    renderer: SwiftUIRenderer
): SwiftUIView {
    return SwiftUIView(
        type = "Alert",
        properties = mapOf(
            "title" to (component.title ?: ""),
            "message" to (component.message ?: ""),
            "isPresented" to component.isVisible,
            "severity" to component.severity.name.lowercase(),
            "confirmText" to (component.confirmText ?: "OK"),
            "cancelText" to (component.cancelText ?: "Cancel"),
            "showCancel" to (component.onCancel != null),
            "icon" to component.icon
        ),
        actions = buildList {
            component.onConfirm?.let { action ->
                add(SwiftUIAction("confirm", action))
            }
            component.onCancel?.let { action ->
                add(SwiftUIAction("cancel", action))
            }
            component.onDismiss?.let { action ->
                add(SwiftUIAction("dismiss", action))
            }
        },
        style = SwiftUIStyle(
            foregroundColor = when (component.severity) {
                AlertSeverity.INFO -> "#007AFF"
                AlertSeverity.SUCCESS -> "#34C759"
                AlertSeverity.WARNING -> "#FF9500"
                AlertSeverity.ERROR -> "#FF3B30"
            }
        )
    )
}

// ============================================================================
// TOAST MAPPER
// ============================================================================

/**
 * Maps ToastComponent to custom iOS toast overlay
 *
 * SwiftUI equivalent:
 * ```swift
 * .overlay {
 *     if showToast {
 *         ToastView(message: message)
 *             .transition(.move(edge: .top))
 *     }
 * }
 * ```
 */
fun mapToastToSwiftUI(
    component: ToastComponent,
    renderer: SwiftUIRenderer
): SwiftUIView {
    return SwiftUIView(
        type = "Toast",
        properties = mapOf(
            "message" to (component.message ?: ""),
            "isPresented" to component.isVisible,
            "duration" to component.duration,
            "position" to "top" // iOS toasts typically at top
        ),
        actions = buildList {
            component.onShown?.let { action ->
                add(SwiftUIAction("shown", action))
            }
        },
        style = SwiftUIStyle(
            backgroundColor = "#000000",
            foregroundColor = "#FFFFFF",
            opacity = 0.85f,
            cornerRadius = 12f,
            padding = SwiftUIPadding(16f, 16f, 12f, 12f)
        )
    )
}

// ============================================================================
// SNACKBAR MAPPER
// ============================================================================

/**
 * Maps SnackbarComponent to custom iOS snackbar
 *
 * SwiftUI equivalent (custom implementation):
 * ```swift
 * VStack {
 *     Spacer()
 *     HStack {
 *         Text(message)
 *         Spacer()
 *         Button(actionText) { action() }
 *     }
 *     .padding()
 *     .background(Color.black.opacity(0.9))
 *     .cornerRadius(8)
 * }
 * ```
 */
fun mapSnackbarToSwiftUI(
    component: SnackbarComponent,
    renderer: SwiftUIRenderer
): SwiftUIView {
    return SwiftUIView(
        type = "Snackbar",
        properties = mapOf(
            "message" to (component.message ?: ""),
            "actionText" to component.actionText,
            "isPresented" to component.isVisible,
            "duration" to when (component.duration) {
                SnackbarDuration.SHORT -> 2000
                SnackbarDuration.LONG -> 4000
                SnackbarDuration.INDEFINITE -> -1
            }
        ),
        actions = buildList {
            component.onAction?.let { action ->
                add(SwiftUIAction("action", action))
            }
            component.onDismiss?.let { action ->
                add(SwiftUIAction("dismiss", action))
            }
        },
        style = SwiftUIStyle(
            backgroundColor = "#1C1C1E",
            foregroundColor = "#FFFFFF",
            cornerRadius = 8f,
            padding = SwiftUIPadding(16f, 16f, 12f, 12f)
        )
    )
}

// ============================================================================
// MODAL MAPPER
// ============================================================================

/**
 * Maps ModalComponent to SwiftUI Sheet
 *
 * SwiftUI equivalent:
 * ```swift
 * .sheet(isPresented: $showModal) {
 *     NavigationView {
 *         content
 *             .navigationTitle(title)
 *             .toolbar {
 *                 ToolbarItem(placement: .confirmationAction) {
 *                     Button("Done") { dismiss() }
 *                 }
 *             }
 *     }
 *     .presentationDetents(fullScreen ? [.large] : [.medium, .large])
 * }
 * ```
 */
fun mapModalToSwiftUI(
    component: ModalComponent,
    renderer: SwiftUIRenderer
): SwiftUIView {
    return SwiftUIView(
        type = "Sheet",
        properties = mapOf(
            "title" to component.title,
            "isPresented" to component.isVisible,
            "fullScreen" to component.fullScreen,
            "detents" to if (component.fullScreen) listOf("large") else listOf("medium", "large")
        ),
        children = listOfNotNull(
            component.content?.let { renderer.render(it) as? SwiftUIView }
        ),
        actions = buildList {
            component.actions.forEach { action ->
                add(SwiftUIAction(action.text, action.onClick ?: {}))
            }
            component.onDismiss?.let { action ->
                add(SwiftUIAction("dismiss", action))
            }
        },
        style = SwiftUIStyle(
            backgroundColor = "#FFFFFF",
            cornerRadius = 12f
        )
    )
}

// ============================================================================
// DIALOG MAPPER
// ============================================================================

/**
 * Maps DialogComponent to SwiftUI Alert (for simple) or custom View (for complex)
 *
 * SwiftUI equivalent:
 * ```swift
 * // Simple dialog
 * .alert(title, isPresented: $showDialog) {
 *     content
 * }
 *
 * // Or custom dialog
 * .overlay {
 *     if showDialog {
 *         ZStack {
 *             Color.black.opacity(0.3)
 *             VStack {
 *                 Text(title)
 *                 content
 *                 HStack { buttons }
 *             }
 *             .background(Color.white)
 *             .cornerRadius(12)
 *         }
 *     }
 * }
 * ```
 */
fun mapDialogToSwiftUI(
    component: DialogComponent,
    renderer: SwiftUIRenderer
): SwiftUIView {
    return SwiftUIView(
        type = "Dialog",
        properties = mapOf(
            "title" to component.title,
            "isPresented" to component.isVisible
        ),
        children = listOfNotNull(
            component.content?.let { renderer.render(it) as? SwiftUIView }
        ),
        actions = buildList {
            component.actions.forEach { action ->
                add(SwiftUIAction(action.text, action.onClick ?: {}))
            }
            component.onDismiss?.let { action ->
                add(SwiftUIAction("dismiss", action))
            }
        },
        style = SwiftUIStyle(
            backgroundColor = "#FFFFFF",
            cornerRadius = 12f,
            padding = SwiftUIPadding(24f, 24f, 24f, 24f)
        )
    )
}

// ============================================================================
// BANNER MAPPER
// ============================================================================

/**
 * Maps BannerComponent to custom SwiftUI banner view
 *
 * SwiftUI equivalent:
 * ```swift
 * HStack {
 *     Image(systemName: icon)
 *     Text(message)
 *     Spacer()
 *     ForEach(actions) { action in
 *         Button(action.text) { action.perform() }
 *     }
 *     if dismissable {
 *         Button { dismiss() } {
 *             Image(systemName: "xmark")
 *         }
 *     }
 * }
 * .padding()
 * .background(severityColor)
 * .cornerRadius(8)
 * ```
 */
fun mapBannerToSwiftUI(
    component: BannerComponent,
    renderer: SwiftUIRenderer
): SwiftUIView {
    return SwiftUIView(
        type = "Banner",
        properties = mapOf(
            "message" to component.message,
            "icon" to component.icon,
            "severity" to component.severity.name.lowercase(),
            "dismissable" to component.dismissable,
            "isPresented" to component.isVisible
        ),
        actions = buildList {
            component.actions.forEach { action ->
                add(SwiftUIAction(action.text, action.onClick ?: {}))
            }
            if (component.dismissable) {
                component.onDismiss?.let { action ->
                    add(SwiftUIAction("dismiss", action))
                }
            }
        },
        style = SwiftUIStyle(
            backgroundColor = when (component.severity) {
                BannerSeverity.INFO -> "#E5F2FF"
                BannerSeverity.SUCCESS -> "#E8F5E9"
                BannerSeverity.WARNING -> "#FFF4E5"
                BannerSeverity.ERROR -> "#FFEBEE"
            },
            foregroundColor = when (component.severity) {
                BannerSeverity.INFO -> "#007AFF"
                BannerSeverity.SUCCESS -> "#34C759"
                BannerSeverity.WARNING -> "#FF9500"
                BannerSeverity.ERROR -> "#FF3B30"
            },
            cornerRadius = 8f,
            padding = SwiftUIPadding(16f, 16f, 16f, 16f)
        )
    )
}

// ============================================================================
// CONTEXT MENU MAPPER
// ============================================================================

/**
 * Maps ContextMenuComponent to SwiftUI Menu or ContextMenu
 *
 * SwiftUI equivalent:
 * ```swift
 * // As Menu
 * Menu {
 *     ForEach(items) { item in
 *         Button(item.text) {
 *             item.action()
 *         }
 *         if item.hasDivider {
 *             Divider()
 *         }
 *     }
 * } label: {
 *     Image(systemName: "ellipsis.circle")
 * }
 *
 * // As ContextMenu
 * .contextMenu {
 *     ForEach(items) { item in
 *         Button(item.text) { item.action() }
 *     }
 * }
 * ```
 */
fun mapContextMenuToSwiftUI(
    component: ContextMenuComponent,
    renderer: SwiftUIRenderer
): SwiftUIView {
    return SwiftUIView(
        type = "Menu",
        properties = mapOf(
            "isPresented" to component.isVisible
        ),
        children = component.items.mapNotNull { item ->
            when (item) {
                is MenuItemData -> SwiftUIView(
                    type = "MenuItem",
                    properties = mapOf(
                        "text" to item.text,
                        "icon" to item.icon,
                        "enabled" to item.enabled,
                        "role" to if (!item.enabled) "disabled" else "normal"
                    ),
                    actions = listOfNotNull(
                        item.onClick?.let { SwiftUIAction("select", it) }
                    )
                )
                is MenuDivider -> SwiftUIView(
                    type = "Divider",
                    properties = emptyMap()
                )
            }
        },
        actions = buildList {
            component.onDismiss?.let { action ->
                add(SwiftUIAction("dismiss", action))
            }
        },
        style = SwiftUIStyle(
            backgroundColor = "#FFFFFF",
            cornerRadius = 10f
        )
    )
}

// ============================================================================
// REGISTRATION (to be called from SwiftUIRenderer)
// ============================================================================

/**
 * Register all Phase 2 feedback mappers with the renderer
 */
fun registerPhase2FeedbackMappers(renderer: SwiftUIRenderer) {
    // Note: Registration happens in SwiftUIRenderer.render() via when statement
    // This function is a placeholder for documentation
}

// ============================================================================
// DATA CLASSES (should be in commonMain)
// ============================================================================

// These match the Android mappers - should be defined once in commonMain

enum class AlertSeverity { INFO, SUCCESS, WARNING, ERROR }
enum class BannerSeverity { INFO, SUCCESS, WARNING, ERROR }
enum class SnackbarDuration { SHORT, LONG, INDEFINITE }

data class AlertComponent(
    override val type: String = "Alert",
    val title: String? = null,
    val message: String? = null,
    val icon: String? = null,
    val severity: AlertSeverity = AlertSeverity.INFO,
    val isVisible: Boolean = false,
    val confirmText: String? = "OK",
    val cancelText: String? = "Cancel",
    val onConfirm: (() -> Unit)? = null,
    val onCancel: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
) : Component

data class ToastComponent(
    override val type: String = "Toast",
    val message: String? = null,
    val duration: Int = 2000,
    val isVisible: Boolean = false,
    val onShown: (() -> Unit)? = null
) : Component

data class SnackbarComponent(
    override val type: String = "Snackbar",
    val message: String? = null,
    val actionText: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.SHORT,
    val isVisible: Boolean = false,
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
) : Component

data class ModalComponent(
    override val type: String = "Modal",
    val title: String? = null,
    val content: Component? = null,
    val actions: List<ActionData> = emptyList(),
    val isVisible: Boolean = false,
    val fullScreen: Boolean = false,
    val onDismiss: (() -> Unit)? = null
) : Component

data class DialogComponent(
    override val type: String = "Dialog",
    val title: String? = null,
    val content: Component? = null,
    val actions: List<ActionData> = emptyList(),
    val isVisible: Boolean = false,
    val onDismiss: (() -> Unit)? = null
) : Component

data class BannerComponent(
    override val type: String = "Banner",
    val message: String,
    val icon: String? = null,
    val severity: BannerSeverity = BannerSeverity.INFO,
    val actions: List<ActionData> = emptyList(),
    val dismissable: Boolean = true,
    val isVisible: Boolean = true,
    val onDismiss: (() -> Unit)? = null
) : Component

data class ContextMenuComponent(
    override val type: String = "ContextMenu",
    val items: List<MenuItem>,
    val isVisible: Boolean = false,
    val onDismiss: (() -> Unit)? = null
) : Component

sealed interface MenuItem
data class MenuItemData(
    val text: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
) : MenuItem

object MenuDivider : MenuItem

data class ActionData(
    val text: String,
    val onClick: (() -> Unit)? = null
)
