package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.feedback.*

/**
 * iOS SwiftUI Mappers for Flutter Feedback Parity Components
 *
 * This file maps cross-platform Flutter Material feedback components to iOS SwiftUI
 * bridge representations. The SwiftUI bridge models are consumed by Swift code to
 * render native iOS UI.
 *
 * Architecture:
 * Flutter Feedback Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented (12 total):
 * - Overlays: Popup, HoverCard, FullPageLoading
 * - Callouts: Callout, Disclosure
 * - Status Panels: InfoPanel, ErrorPanel, WarningPanel, SuccessPanel
 * - Refresh Controls: PullToRefresh, SwipeRefresh
 * - Celebrations: Confetti
 *
 * iOS-specific features:
 * - Native SwiftUI popover and overlay semantics
 * - UIRefreshControl integration for pull-to-refresh
 * - SF Symbols for icons (info.circle, exclamationmark.triangle, checkmark.circle, etc.)
 * - VoiceOver accessibility with semantic roles
 * - Dynamic Type support
 * - Dark mode color adaptation
 * - Smooth spring animations
 *
 * @since 3.2.0-feedback-components
 */

/**
 * Maps Popup to SwiftUI popover overlay
 *
 * SwiftUI Implementation:
 * - Uses .popover modifier for native iOS popover behavior
 * - Arrow positioning via permittedArrowDirections
 * - Auto-dismissal on outside tap
 * - Adaptive positioning to stay in viewport
 * - Material Design 3 styling with SF Symbols
 *
 * Visual Parity:
 * - Rounded corners (12pt)
 * - Subtle shadow (elevation 4dp)
 * - White background (dark mode adaptive)
 * - Arrow pointer matching Material Design
 */
object PopupMapper {
    fun map(
        component: Popup,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        if (!component.visible) {
            return SwiftUIView(type = ViewType.EmptyView, properties = emptyMap())
        }

        // Convert position to SwiftUI arrow direction
        val arrowDirection = when (component.anchorPosition) {
            Popup.Position.TopStart, Popup.Position.TopCenter, Popup.Position.TopEnd -> "down"
            Popup.Position.BottomStart, Popup.Position.BottomCenter, Popup.Position.BottomEnd -> "up"
            Popup.Position.LeftStart, Popup.Position.LeftCenter, Popup.Position.LeftEnd -> "right"
            Popup.Position.RightStart, Popup.Position.RightCenter, Popup.Position.RightEnd -> "left"
        }

        // Content container
        val contentView = SwiftUIView.vStack(
            spacing = 8f,
            alignment = HorizontalAlignment.Leading,
            children = listOf(
                SwiftUIView.text(
                    content = component.content,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Body),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("label"))
                    )
                )
            ),
            modifiers = listOf(
                SwiftUIModifier.padding(12f),
                SwiftUIModifier.background(
                    component.backgroundColor?.let { color -> parseColor(color) }
                        ?: SwiftUIColor.system("systemBackground")
                ),
                SwiftUIModifier.cornerRadius(12f),
                SwiftUIModifier.shadow(
                    radius = component.elevation,
                    x = 0f,
                    y = component.elevation / 2f
                )
            )
        )

        // Apply width constraints
        val frameModifiers = mutableListOf<SwiftUIModifier>()
        component.width?.let { width ->
            frameModifiers.add(SwiftUIModifier.frame(
                width = SizeValue.Fixed(width),
                height = null
            ))
        }
        frameModifiers.add(SwiftUIModifier.frame(
            width = SizeValue.Fixed(component.maxWidth),
            height = null
        ))

        return SwiftUIView(
            type = ViewType.Custom("CustomView"),
            id = component.id,
            properties = mapOf(
                "componentType" to "Popover",
                "content" to component.content,
                "arrowDirection" to arrowDirection,
                "showArrow" to component.showArrow,
                "arrowSize" to component.arrowSize,
                "offsetX" to component.offsetX,
                "offsetY" to component.offsetY,
                "dismissible" to component.dismissible,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(contentView),
            modifiers = frameModifiers
        )
    }
}

/**
 * Maps Callout to SwiftUI custom view with directional arrow
 *
 * SwiftUI Implementation:
 * - Custom ZStack with background shape and arrow triangle
 * - Arrow positioning via alignment and offset
 * - Colored backgrounds based on variant (info=blue, success=green, warning=orange, error=red)
 * - SF Symbols icons (info.circle, checkmark.circle, exclamationmark.triangle, xmark.octagon)
 * - Dismissible with close button (xmark)
 *
 * Visual Parity:
 * - Rounded corners (8pt)
 * - Arrow pointer (customizable size)
 * - Icon + Title + Message layout
 * - Material Design 3 color semantics
 */
object CalloutMapper {
    fun map(
        component: Callout,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        // Determine colors based on variant
        val (backgroundColor, iconColor, iconName) = when (component.variant) {
            Callout.Variant.Info -> Triple(
                SwiftUIColor.rgb(0.8f, 0.9f, 1.0f, 1.0f), // Light blue
                SwiftUIColor.rgb(0.0f, 0.48f, 1.0f, 1.0f), // Blue
                "info.circle"
            )
            Callout.Variant.Success -> Triple(
                SwiftUIColor.rgb(0.8f, 1.0f, 0.8f, 1.0f), // Light green
                SwiftUIColor.rgb(0.2f, 0.78f, 0.35f, 1.0f), // Green
                "checkmark.circle"
            )
            Callout.Variant.Warning -> Triple(
                SwiftUIColor.rgb(1.0f, 0.95f, 0.8f, 1.0f), // Light orange
                SwiftUIColor.rgb(1.0f, 0.6f, 0.0f, 1.0f), // Orange
                "exclamationmark.triangle"
            )
            Callout.Variant.Error -> Triple(
                SwiftUIColor.rgb(1.0f, 0.9f, 0.9f, 1.0f), // Light red
                SwiftUIColor.rgb(0.96f, 0.26f, 0.21f, 1.0f), // Red
                "xmark.octagon"
            )
        }

        val contentChildren = mutableListOf<SwiftUIView>()

        // Icon
        val effectiveIcon = component.icon ?: iconName
        contentChildren.add(
            SwiftUIView(
                type = ViewType.Image,
                properties = mapOf("systemName" to effectiveIcon),
                modifiers = listOf(
                    SwiftUIModifier.fontSize(20f),
                    SwiftUIModifier.foregroundColor(iconColor),
                    SwiftUIModifier.frame(width = SizeValue.Fixed(24f), height = SizeValue.Fixed(24f))
                )
            )
        )

        // Title and Message in VStack
        contentChildren.add(
            SwiftUIView.vStack(
                spacing = 4f,
                alignment = HorizontalAlignment.Leading,
                children = listOf(
                    SwiftUIView.text(
                        content = component.title,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Headline),
                            SwiftUIModifier.fontWeight(FontWeight.Semibold)
                        )
                    ),
                    SwiftUIView.text(
                        content = component.message,
                        modifiers = listOf(
                            SwiftUIModifier.font(FontStyle.Body)
                        )
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier.fillMaxWidth()
                )
            )
        )

        // Dismiss button if dismissible
        if (component.dismissible) {
            contentChildren.add(
                SwiftUIView(
                    type = ViewType.Button,
                    properties = mapOf(
                        "label" to "",
                        "action" to "dismiss"
                    ),
                    children = listOf(
                        SwiftUIView(
                            type = ViewType.Image,
                            properties = mapOf("systemName" to "xmark"),
                            modifiers = listOf(
                                SwiftUIModifier.fontSize(12f),
                                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                            )
                        )
                    ),
                    modifiers = listOf(
                        SwiftUIModifier.frame(width = SizeValue.Fixed(20f), height = SizeValue.Fixed(20f))
                    )
                )
            )
        }

        // Main content HStack
        val contentStack = SwiftUIView.hStack(
            spacing = 12f,
            alignment = VerticalAlignment.Top,
            children = contentChildren,
            modifiers = listOf(
                SwiftUIModifier.padding(16f),
                SwiftUIModifier.background(backgroundColor),
                SwiftUIModifier.cornerRadius(8f),
                SwiftUIModifier.shadow(
                    radius = component.elevation,
                    x = 0f,
                    y = component.elevation / 2f
                )
            )
        )

        // Wrap in ZStack with arrow if needed
        val finalView = if (component.arrowPosition != Callout.ArrowPosition.None) {
            SwiftUIView.zStack(
                alignment = ZStackAlignment.Center,
                children = listOf(contentStack)
                // TODO: Add arrow triangle shape overlay based on arrowPosition
            )
        } else {
            contentStack
        }

        return SwiftUIView(
            type = ViewType.Custom("CustomView"),
            id = component.id,
            properties = mapOf(
                "componentType" to "Callout",
                "title" to component.title,
                "message" to component.message,
                "variant" to component.variant.name,
                "arrowPosition" to component.arrowPosition.name,
                "dismissible" to component.dismissible,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(finalView),
            modifiers = emptyList()
        )
    }
}

/**
 * Maps HoverCard to SwiftUI hover-triggered popover
 *
 * SwiftUI Implementation:
 * - Uses .popover(isPresented:) with hover gesture (macOS/iPad) or long press (iOS)
 * - Delay timers for showDelay and hideDelay
 * - Rich content card with title, content, icon, actions
 * - Auto-positioning relative to trigger
 *
 * Visual Parity:
 * - Card elevation (4dp shadow)
 * - Rounded corners (12pt)
 * - Action buttons at bottom
 * - Smooth fade-in/out animation
 */
object FeedbackHoverCardMapper {
    fun map(
        component: HoverCard,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        // Trigger content
        val triggerView = SwiftUIView.text(
            content = component.triggerContent,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body)
            )
        )

        // Card content
        val cardChildren = mutableListOf<SwiftUIView>()

        // Icon if present
        component.cardIcon?.let { icon ->
            cardChildren.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to icon),
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(24f),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.primary)
                    )
                )
            )
        }

        // Title
        cardChildren.add(
            SwiftUIView.text(
                content = component.cardTitle,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Headline),
                    SwiftUIModifier.fontWeight(FontWeight.Semibold)
                )
            )
        )

        // Content
        cardChildren.add(
            SwiftUIView.text(
                content = component.cardContent,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                )
            )
        )

        // Action buttons if present
        if (component.actions.isNotEmpty()) {
            val actionButtons = component.actions.map { action ->
                SwiftUIView.button(
                    label = action.label,
                    action = "action_${action.label}",
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Subheadline),
                        SwiftUIModifier.fontWeight(FontWeight.Medium)
                    )
                )
            }

            cardChildren.add(
                SwiftUIView.hStack(
                    spacing = 8f,
                    alignment = VerticalAlignment.Center,
                    children = actionButtons,
                    modifiers = listOf(
                        SwiftUIModifier.padding(8f, 0f, 0f, 0f)
                    )
                )
            )
        }

        val cardView = SwiftUIView.vStack(
            spacing = 12f,
            alignment = HorizontalAlignment.Leading,
            children = cardChildren,
            modifiers = listOf(
                SwiftUIModifier.padding(16f),
                SwiftUIModifier.background(SwiftUIColor.system("systemBackground")),
                SwiftUIModifier.cornerRadius(12f),
                SwiftUIModifier.shadow(
                    radius = component.elevation,
                    x = 0f,
                    y = component.elevation / 2f
                ),
                SwiftUIModifier.frame(
                    width = component.width?.let { w -> SizeValue.Fixed(w) } ?: SizeValue.Fixed(component.maxWidth),
                    height = null
                )
            )
        )

        return SwiftUIView(
            type = ViewType.Custom("CustomView"),
            id = component.id,
            properties = mapOf(
                "componentType" to "HoverCard",
                "triggerContent" to component.triggerContent,
                "cardTitle" to component.cardTitle,
                "cardContent" to component.cardContent,
                "showDelay" to component.showDelay,
                "hideDelay" to component.hideDelay,
                "position" to component.position.name,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(triggerView, cardView),
            modifiers = emptyList()
        )
    }
}

/**
 * Maps Disclosure to SwiftUI DisclosureGroup
 *
 * SwiftUI Implementation:
 * - Native DisclosureGroup for expand/collapse
 * - Smooth spring animation
 * - Chevron icon rotation
 * - Controlled or uncontrolled state
 *
 * Visual Parity:
 * - Material Design expansion animation
 * - Chevron icon (chevron.right rotates to chevron.down)
 * - Keyboard accessible (Space/Enter to toggle)
 */
object DisclosureMapper {
    fun map(
        component: Disclosure,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val isExpanded = component.expanded ?: component.initiallyExpanded

        // Title with icon
        val titleChildren = mutableListOf<SwiftUIView>()
        titleChildren.add(
            SwiftUIView.text(
                content = component.title,
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.fontWeight(FontWeight.Medium)
                )
            )
        )

        if (component.showIcon) {
            titleChildren.add(
                SwiftUIView(
                    type = ViewType.Image,
                    properties = mapOf("systemName" to "chevron.right"),
                    modifiers = listOf(
                        SwiftUIModifier.fontSize(12f),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                        // TODO: Add rotation animation based on expanded state
                    )
                )
            )
        }

        val titleView = SwiftUIView.hStack(
            spacing = 8f,
            alignment = VerticalAlignment.Center,
            children = titleChildren
        )

        // Content view
        val contentView = SwiftUIView.text(
            content = component.content,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel")),
                SwiftUIModifier.padding(8f, 0f, 0f, 16f)
            )
        )

        return SwiftUIView(
            type = ViewType.Custom("CustomView"),
            id = component.id,
            properties = mapOf(
                "componentType" to "DisclosureGroup",
                "title" to component.title,
                "content" to component.content,
                "isExpanded" to isExpanded,
                "animationDuration" to component.animationDuration,
                "isControlled" to component.isControlled(),
                "accessibilityLabel" to component.getAccessibilityDescription(isExpanded)
            ),
            children = listOf(titleView, contentView),
            modifiers = emptyList()
        )
    }
}

/**
 * Maps InfoPanel to SwiftUI card with info styling
 *
 * SwiftUI Implementation:
 * - Blue background (systemBlue with 0.1 opacity)
 * - info.circle SF Symbol
 * - Title + Message + Actions layout
 * - Optional dismiss button
 *
 * Visual Parity:
 * - Material Design 3 info color scheme
 * - Rounded corners (8pt)
 * - Icon + text alignment
 */
object InfoPanelMapper {
    fun map(
        component: InfoPanel,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        return createPanelView(
            panelType = component.type,
            backgroundColor = SwiftUIColor.rgb(0.85f, 0.92f, 1.0f, 1.0f), // Light blue
            iconColor = SwiftUIColor.rgb(0.0f, 0.48f, 1.0f, 1.0f), // Blue
            iconName = component.icon ?: "info.circle",
            title = component.title,
            message = component.message,
            dismissible = component.dismissible,
            actions = component.actions.map { action -> PanelAction(action.label) },
            elevation = component.elevation,
            accessibilityLabel = component.getAccessibilityDescription(),
            id = component.id
        )
    }
}

/**
 * Maps ErrorPanel to SwiftUI card with error styling
 *
 * SwiftUI Implementation:
 * - Red background (systemRed with 0.1 opacity)
 * - xmark.octagon or exclamationmark.triangle SF Symbol
 * - Title + Message + Actions layout
 * - Optional dismiss button
 * - VoiceOver announces as "Error"
 *
 * Visual Parity:
 * - Material Design 3 error color scheme
 * - Rounded corners (8pt)
 * - Prominent error icon
 */
object ErrorPanelMapper {
    fun map(
        component: ErrorPanel,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        return createPanelView(
            panelType = component.type,
            backgroundColor = SwiftUIColor.rgb(1.0f, 0.92f, 0.92f, 1.0f), // Light red
            iconColor = SwiftUIColor.rgb(0.96f, 0.26f, 0.21f, 1.0f), // Red
            iconName = component.icon ?: "xmark.octagon",
            title = component.title,
            message = component.message,
            dismissible = component.dismissible,
            actions = component.actions.map { action -> PanelAction(action.label) },
            elevation = component.elevation,
            accessibilityLabel = "Error: ${component.getAccessibilityDescription()}",
            id = component.id
        )
    }
}

/**
 * Maps WarningPanel to SwiftUI card with warning styling
 *
 * SwiftUI Implementation:
 * - Orange/yellow background (systemOrange with 0.1 opacity)
 * - exclamationmark.triangle SF Symbol
 * - Title + Message + Actions layout
 * - Optional dismiss button
 * - VoiceOver announces as "Warning"
 *
 * Visual Parity:
 * - Material Design 3 warning color scheme
 * - Rounded corners (8pt)
 * - Warning triangle icon
 */
object WarningPanelMapper {
    fun map(
        component: WarningPanel,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        return createPanelView(
            panelType = component.type,
            backgroundColor = SwiftUIColor.rgb(1.0f, 0.96f, 0.87f, 1.0f), // Light orange
            iconColor = SwiftUIColor.rgb(1.0f, 0.6f, 0.0f, 1.0f), // Orange
            iconName = component.icon ?: "exclamationmark.triangle",
            title = component.title,
            message = component.message,
            dismissible = component.dismissible,
            actions = component.actions.map { action -> PanelAction(action.label) },
            elevation = component.elevation,
            accessibilityLabel = "Warning: ${component.getAccessibilityDescription()}",
            id = component.id
        )
    }
}

/**
 * Maps SuccessPanel to SwiftUI card with success styling
 *
 * SwiftUI Implementation:
 * - Green background (systemGreen with 0.1 opacity)
 * - checkmark.circle SF Symbol
 * - Title + Message + Actions layout
 * - Optional dismiss button
 * - VoiceOver announces as "Success"
 *
 * Visual Parity:
 * - Material Design 3 success color scheme
 * - Rounded corners (8pt)
 * - Checkmark icon
 */
object SuccessPanelMapper {
    fun map(
        component: SuccessPanel,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        return createPanelView(
            panelType = component.type,
            backgroundColor = SwiftUIColor.rgb(0.88f, 1.0f, 0.88f, 1.0f), // Light green
            iconColor = SwiftUIColor.rgb(0.2f, 0.78f, 0.35f, 1.0f), // Green
            iconName = component.icon ?: "checkmark.circle",
            title = component.title,
            message = component.message,
            dismissible = component.dismissible,
            actions = component.actions.map { action -> PanelAction(action.label) },
            elevation = component.elevation,
            accessibilityLabel = "Success: ${component.getAccessibilityDescription()}",
            id = component.id
        )
    }
}

/**
 * Maps FullPageLoading to SwiftUI fullscreen overlay
 *
 * SwiftUI Implementation:
 * - ZStack with semi-transparent black background
 * - ProgressView (spinning indicator) in center
 * - Optional loading message below spinner
 * - Optional cancel button at bottom
 * - Blocks all interaction (non-dismissible by default)
 *
 * Visual Parity:
 * - Material Design loading overlay
 * - Large spinner (64dp default)
 * - Centered layout
 * - 0.5 opacity backdrop
 */
object FullPageLoadingMapper {
    fun map(
        component: FullPageLoading,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        if (!component.visible) {
            return SwiftUIView(type = ViewType.EmptyView, properties = emptyMap())
        }

        val contentChildren = mutableListOf<SwiftUIView>()

        // Spinner
        contentChildren.add(
            SwiftUIView(
                type = ViewType.Custom("CustomView"),
                properties = mapOf(
                    "componentType" to "ProgressView",
                    "style" to "circular",
                    "size" to component.spinnerSize
                ),
                modifiers = listOf(
                    SwiftUIModifier.foregroundColor(SwiftUIColor.white)
                )
            )
        )

        // Message if present
        component.message?.let { msg ->
            contentChildren.add(
                SwiftUIView.text(
                    content = msg,
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Body),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                        SwiftUIModifier.padding(16f, 0f, 0f, 0f)
                    )
                )
            )
        }

        // Cancel button if cancelable
        if (component.cancelable) {
            contentChildren.add(
                SwiftUIView.button(
                    label = component.cancelText,
                    action = "cancel",
                    modifiers = listOf(
                        SwiftUIModifier.font(FontStyle.Body),
                        SwiftUIModifier.fontWeight(FontWeight.Medium),
                        SwiftUIModifier.foregroundColor(SwiftUIColor.white),
                        SwiftUIModifier.padding(24f, 0f, 0f, 0f)
                    )
                )
            )
        }

        // Content VStack
        val contentStack = SwiftUIView.vStack(
            spacing = 0f,
            alignment = HorizontalAlignment.Center,
            children = contentChildren
        )

        // Fullscreen ZStack with backdrop
        val overlayView = SwiftUIView.zStack(
            alignment = ZStackAlignment.Center,
            children = listOf(
                // Backdrop
                SwiftUIView(
                    type = ViewType.Rectangle,
                    properties = emptyMap(),
                    modifiers = listOf(
                        SwiftUIModifier.background(SwiftUIColor.black),
                        SwiftUIModifier.opacity(0.5f),
                        SwiftUIModifier.fillMaxSize()
                    )
                ),
                // Content
                contentStack
            ),
            modifiers = listOf(
                SwiftUIModifier.fillMaxSize()
            )
        )

        return SwiftUIView(
            type = ViewType.Custom("CustomView"),
            id = component.id,
            properties = mapOf(
                "componentType" to "FullPageLoading",
                "visible" to component.visible,
                "message" to (component.message ?: ""),
                "cancelable" to component.cancelable,
                "cancelText" to component.cancelText,
                "accessibilityLabel" to component.getAccessibilityDescription()
            ),
            children = listOf(overlayView),
            modifiers = emptyList()
        )
    }
}

/**
 * Maps PullToRefresh to UIRefreshControl
 *
 * SwiftUI Implementation:
 * - Uses native UIRefreshControl via UIViewRepresentable
 * - Integrated with ScrollView
 * - Pull gesture triggers refresh
 * - Spinner animates during refresh
 *
 * Note: Component definition not yet available in flutter-parity.
 * This is a placeholder mapper for future implementation.
 *
 * @since 3.2.0-feedback-components
 */
object PullToRefreshMapper {
    fun map(
        refreshing: Boolean,
        onRefresh: (() -> Unit)?,
        theme: Theme?
    ): SwiftUIView {
        // TODO: Implement when PullToRefresh component is added to flutter-parity
        return SwiftUIView(
            type = ViewType.RefreshControl,
            properties = mapOf(
                "refreshing" to refreshing,
                "tintColor" to "primary"
            ),
            modifiers = emptyList()
        )
    }
}

/**
 * Maps SwipeRefresh to UIRefreshControl
 *
 * SwiftUI Implementation:
 * - Similar to PullToRefresh
 * - Used in horizontal scroll contexts (less common on iOS)
 * - Pull-down gesture to refresh
 *
 * Note: Component definition not yet available in flutter-parity.
 * This is a placeholder mapper for future implementation.
 *
 * @since 3.2.0-feedback-components
 */
object SwipeRefreshMapper {
    fun map(
        refreshing: Boolean,
        onRefresh: (() -> Unit)?,
        theme: Theme?
    ): SwiftUIView {
        // TODO: Implement when SwipeRefresh component is added to flutter-parity
        return SwiftUIView(
            type = ViewType.RefreshControl,
            properties = mapOf(
                "refreshing" to refreshing,
                "tintColor" to "primary"
            ),
            modifiers = emptyList()
        )
    }
}

/**
 * Maps Confetti to SwiftUI particle animation
 *
 * SwiftUI Implementation:
 * - Custom particle system using Canvas or CAEmitterLayer
 * - Animated confetti shapes falling from top
 * - Configurable colors, count, duration
 * - Celebration trigger animation
 *
 * Note: Component definition not yet available in flutter-parity.
 * This is a placeholder mapper for future implementation.
 *
 * @since 3.2.0-feedback-components
 */
object ConfettiMapper {
    fun map(
        isActive: Boolean,
        particleCount: Int,
        colors: List<String>,
        theme: Theme?
    ): SwiftUIView {
        // TODO: Implement when Confetti component is added to flutter-parity
        return SwiftUIView(
            type = ViewType.Custom("CustomView"),
            properties = mapOf(
                "componentType" to "ConfettiView",
                "isActive" to isActive,
                "particleCount" to particleCount,
                "colors" to colors.joinToString(",")
            ),
            modifiers = emptyList()
        )
    }
}

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Panel action data class for common panel components
 */
private data class PanelAction(val label: String)

/**
 * Helper to create a standardized panel view with consistent styling
 *
 * This centralizes the panel creation logic for InfoPanel, ErrorPanel,
 * WarningPanel, and SuccessPanel to ensure visual consistency.
 */
private fun createPanelView(
    panelType: String,
    backgroundColor: SwiftUIColor,
    iconColor: SwiftUIColor,
    iconName: String,
    title: String,
    message: String,
    dismissible: Boolean,
    actions: List<PanelAction>,
    elevation: Float,
    accessibilityLabel: String,
    id: String?
): SwiftUIView {
    val contentChildren = mutableListOf<SwiftUIView>()

    // Icon
    contentChildren.add(
        SwiftUIView(
            type = ViewType.Image,
            properties = mapOf("systemName" to iconName),
            modifiers = listOf(
                SwiftUIModifier.fontSize(24f),
                SwiftUIModifier.foregroundColor(iconColor),
                SwiftUIModifier.frame(width = SizeValue.Fixed(28f), height = SizeValue.Fixed(28f))
            )
        )
    )

    // Title and message column
    val textChildren = mutableListOf<SwiftUIView>(
        SwiftUIView.text(
            content = title,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Headline),
                SwiftUIModifier.fontWeight(FontWeight.Semibold)
            )
        ),
        SwiftUIView.text(
            content = message,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Body),
                SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
            )
        )
    )

    contentChildren.add(
        SwiftUIView.vStack(
            spacing = 4f,
            alignment = HorizontalAlignment.Leading,
            children = textChildren,
            modifiers = listOf(
                SwiftUIModifier.fillMaxWidth()
            )
        )
    )

    // Dismiss button if dismissible
    if (dismissible) {
        contentChildren.add(
            SwiftUIView(
                type = ViewType.Button,
                properties = mapOf(
                    "label" to "",
                    "action" to "dismiss"
                ),
                children = listOf(
                    SwiftUIView(
                        type = ViewType.Image,
                        properties = mapOf("systemName" to "xmark"),
                        modifiers = listOf(
                            SwiftUIModifier.fontSize(12f),
                            SwiftUIModifier.foregroundColor(SwiftUIColor.system("secondaryLabel"))
                        )
                    )
                ),
                modifiers = listOf(
                    SwiftUIModifier.frame(width = SizeValue.Fixed(20f), height = SizeValue.Fixed(20f))
                )
            )
        )
    }

    // Main HStack
    val mainStack = SwiftUIView.hStack(
        spacing = 12f,
        alignment = VerticalAlignment.Top,
        children = contentChildren
    )

    // Actions row if present
    val finalChildren = if (actions.isNotEmpty()) {
        val actionButtons = actions.map { action ->
            SwiftUIView.button(
                label = action.label,
                action = "action_${action.label}",
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Subheadline),
                    SwiftUIModifier.fontWeight(FontWeight.Medium)
                )
            )
        }

        val actionsRow = SwiftUIView.hStack(
            spacing = 8f,
            alignment = VerticalAlignment.Center,
            children = actionButtons,
            modifiers = listOf(
                SwiftUIModifier.padding(8f, 0f, 0f, 0f)
            )
        )

        listOf(
            SwiftUIView.vStack(
                spacing = 12f,
                alignment = HorizontalAlignment.Leading,
                children = listOf(mainStack, actionsRow)
            )
        )
    } else {
        listOf(mainStack)
    }

    return SwiftUIView(
        type = ViewType.Custom("CustomView"),
        id = id,
        properties = mapOf(
            "componentType" to "Panel",
            "panelType" to panelType,
            "title" to title,
            "message" to message,
            "dismissible" to dismissible,
            "accessibilityLabel" to accessibilityLabel
        ),
        children = finalChildren,
        modifiers = listOf(
            SwiftUIModifier.padding(16f),
            SwiftUIModifier.background(backgroundColor),
            SwiftUIModifier.cornerRadius(8f),
            if (elevation > 0f) {
                SwiftUIModifier.shadow(
                    radius = elevation,
                    x = 0f,
                    y = elevation / 2f
                )
            } else {
                SwiftUIModifier.border(
                    color = SwiftUIColor.system("separator"),
                    width = 1f
                )
            }
        )
    )
}

/**
 * Parse color string to SwiftUIColor
 *
 * Supports:
 * - Hex: #RRGGBB or #AARRGGBB
 * - RGB: rgb(r,g,b) or rgba(r,g,b,a)
 * - System: systemBlue, systemRed, etc.
 */
private fun parseColor(colorString: String): SwiftUIColor {
    return when {
        colorString.startsWith("#") -> {
            val hex = colorString.removePrefix("#")
            when (hex.length) {
                6 -> {
                    val r = hex.substring(0, 2).toInt(16) / 255f
                    val g = hex.substring(2, 4).toInt(16) / 255f
                    val b = hex.substring(4, 6).toInt(16) / 255f
                    SwiftUIColor.rgb(r, g, b)
                }
                8 -> {
                    val a = hex.substring(0, 2).toInt(16) / 255f
                    val r = hex.substring(2, 4).toInt(16) / 255f
                    val g = hex.substring(4, 6).toInt(16) / 255f
                    val b = hex.substring(6, 8).toInt(16) / 255f
                    SwiftUIColor.rgb(r, g, b, a)
                }
                else -> SwiftUIColor.primary
            }
        }
        colorString.startsWith("rgb") -> {
            // Simple rgb/rgba parser
            val values = colorString.substringAfter("(").substringBefore(")")
                .split(",").map { colorValue -> colorValue.trim().toFloatOrNull() ?: 0f }
            when (values.size) {
                3 -> SwiftUIColor.rgb(values[0] / 255f, values[1] / 255f, values[2] / 255f)
                4 -> SwiftUIColor.rgb(values[0] / 255f, values[1] / 255f, values[2] / 255f, values[3])
                else -> SwiftUIColor.primary
            }
        }
        else -> SwiftUIColor.system(colorString)
    }
}
