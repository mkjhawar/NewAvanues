package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.flutter.material.display.*

/**
 * iOS SwiftUI Mappers for Flutter Material Parity - State/Placeholder Components
 *
 * This file maps state and placeholder components (ErrorState, NoData) to iOS SwiftUI
 * bridge representations. These components provide user-friendly UI states for error
 * conditions and empty data scenarios.
 *
 * Architecture:
 * Flutter Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented:
 * - ErrorState: Error placeholder with retry functionality
 * - NoData: Empty state placeholder with optional action
 *
 * @since 3.1.0-android-parity
 */

// ============================================
// ERROR STATE
// ============================================

/**
 * Maps ErrorState to custom SwiftUI error placeholder view
 *
 * SwiftUI implementation uses VStack with centered content:
 * - Large SF Symbol icon (error.circle, wifi.slash, etc.)
 * - Error title in headline style
 * - Optional description in body style with secondary color
 * - Optional retry button with primary color
 * - Material Design 3 spacing and alignment
 * - Dark mode support via theme colors
 * - VoiceOver accessibility with error announcements
 *
 * Visual parity with Flutter's ErrorState widget:
 * - Icon size: 64dp
 * - Title: Headline style
 * - Description: Body style, secondary color
 * - Retry button: Filled style with primary color
 * - Vertical spacing: 16dp between elements
 *
 * Common iOS error icons:
 * - "error_outline" → "exclamationmark.triangle"
 * - "wifi_off" → "wifi.slash"
 * - "cloud_off" → "cloud.slash"
 * - "search_off" → "magnifyingglass.slash"
 */
object ErrorStateMapper {
    fun map(
        component: ErrorState,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Container fills available space and centers content
        modifiers.add(SwiftUIModifier.fillMaxSize())
        modifiers.add(SwiftUIModifier.frame(
            width = null,
            height = null,
            alignment = ZStackAlignment.Center
        ))

        // Build child views
        val children = mutableListOf<SwiftUIView>()

        // 1. Error Icon
        val iconName = convertIconToSFSymbol(component.icon)
        val iconView = SwiftUIView(
            type = ViewType.Image,
            properties = mapOf(
                "systemName" to iconName,
                "renderingMode" to "template"
            ),
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.System),
                SwiftUIModifier.fontSize(component.iconSize),
                SwiftUIModifier.foregroundColor(
                    theme?.colorScheme?.error?.let { ModifierConverter.convertColor(it) }
                        ?: SwiftUIColor.system("systemRed")
                )
            )
        )
        children.add(iconView)

        // Add spacer after icon
        children.add(SwiftUIView(
            type = ViewType.Spacer,
            properties = emptyMap(),
            modifiers = listOf(
                SwiftUIModifier.frame(
                    width = null,
                    height = SizeValue.Fixed(16f),
                    alignment = ZStackAlignment.Center
                )
            )
        ))

        // 2. Error Message (Title)
        val titleView = SwiftUIView.text(
            content = component.message,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Headline),
                SwiftUIModifier.fontWeight(FontWeight.Semibold),
                SwiftUIModifier.foregroundColor(
                    theme?.colorScheme?.onSurface?.let { ModifierConverter.convertColor(it) }
                        ?: SwiftUIColor.primary
                ),
                SwiftUIModifier.Custom("multilineTextAlignment", "center")
            )
        )
        children.add(titleView)

        // 3. Description (if present)
        if (component.hasDescription()) {
            // Add spacer before description
            children.add(SwiftUIView(
                type = ViewType.Spacer,
                properties = emptyMap(),
                modifiers = listOf(
                    SwiftUIModifier.frame(
                        width = null,
                        height = SizeValue.Fixed(8f),
                        alignment = ZStackAlignment.Center
                    )
                )
            ))

            val descriptionView = SwiftUIView.text(
                content = component.description ?: "",
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Body),
                    SwiftUIModifier.foregroundColor(
                        theme?.colorScheme?.onSurfaceVariant?.let { ModifierConverter.convertColor(it) }
                            ?: SwiftUIColor.secondary
                    ),
                    SwiftUIModifier.Custom("multilineTextAlignment", "center"),
                    SwiftUIModifier.padding(32f, 32f, 0f, 32f)
                )
            )
            children.add(descriptionView)
        }

        // 4. Retry Button (if available)
        if (component.isRetryAvailable()) {
            // Add spacer before button
            children.add(SwiftUIView(
                type = ViewType.Spacer,
                properties = emptyMap(),
                modifiers = listOf(
                    SwiftUIModifier.frame(
                        width = null,
                        height = SizeValue.Fixed(24f),
                        alignment = ZStackAlignment.Center
                    )
                )
            ))

            val buttonView = SwiftUIView.button(
                label = component.retryLabel,
                action = "onRetry",
                modifiers = listOf(
                    SwiftUIModifier.Custom("buttonStyle", "borderedProminent"),
                    SwiftUIModifier.Custom("controlSize", "large"),
                    SwiftUIModifier.foregroundColor(
                        theme?.colorScheme?.onPrimary?.let { ModifierConverter.convertColor(it) }
                            ?: SwiftUIColor.white
                    ),
                    SwiftUIModifier.background(
                        theme?.colorScheme?.primary?.let { ModifierConverter.convertColor(it) }
                            ?: SwiftUIColor.blue
                    ),
                    SwiftUIModifier.cornerRadius(8f),
                    SwiftUIModifier.padding(12f)
                )
            )
            children.add(buttonView)
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Build properties map
        val properties = mutableMapOf<String, Any>(
            "contentDescription" to component.getAccessibilityDescription()
        )

        // Add retry callback if available
        if (component.onRetry != null) {
            properties["onRetry"] = "callback"
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "spacing" to 0f,
                "alignment" to HorizontalAlignment.Center.name
            ),
            children = children,
            modifiers = modifiers,
            id = component.id
        )
    }

    /**
     * Convert Material icon names to SF Symbol names
     */
    private fun convertIconToSFSymbol(iconName: String): String {
        return when (iconName) {
            "error_outline" -> "exclamationmark.triangle"
            "error" -> "exclamationmark.circle.fill"
            "wifi_off" -> "wifi.slash"
            "cloud_off" -> "cloud.slash"
            "search_off" -> "magnifyingglass.circle.fill"
            "warning" -> "exclamationmark.triangle.fill"
            "info" -> "info.circle"
            "block" -> "nosign"
            else -> "exclamationmark.triangle" // default
        }
    }
}

// ============================================
// NO DATA (EMPTY STATE)
// ============================================

/**
 * Maps NoData to custom SwiftUI empty state placeholder view
 *
 * SwiftUI implementation uses VStack with centered content:
 * - Large SF Symbol icon (tray, inbox.circle, etc.)
 * - Message title in headline style
 * - Optional description in body style with secondary color
 * - Optional action button with primary color
 * - Material Design 3 spacing and alignment
 * - Dark mode support via theme colors
 * - VoiceOver accessibility
 *
 * Visual parity with Flutter's NoData/EmptyState widget:
 * - Icon size: 80dp (slightly larger than error state)
 * - Title: Headline style
 * - Description: Body style, secondary color
 * - Action button: Filled style with primary color
 * - Vertical spacing: 16dp between elements
 * - Lighter color scheme compared to error state
 *
 * Common iOS empty state icons:
 * - "inbox" → "tray"
 * - "search_off" → "magnifyingglass"
 * - "favorite_border" → "heart"
 * - "history" → "clock"
 * - "notifications_none" → "bell"
 * - "folder_open" → "folder"
 */
object NoDataMapper {
    fun map(
        component: NoData,
        theme: Theme?,
        renderChild: (Component) -> SwiftUIView
    ): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Container fills available space and centers content
        modifiers.add(SwiftUIModifier.fillMaxSize())
        modifiers.add(SwiftUIModifier.frame(
            width = null,
            height = null,
            alignment = ZStackAlignment.Center
        ))

        // Build child views
        val children = mutableListOf<SwiftUIView>()

        // 1. Empty State Icon
        val iconName = convertIconToSFSymbol(component.icon)
        val iconView = SwiftUIView(
            type = ViewType.Image,
            properties = mapOf(
                "systemName" to iconName,
                "renderingMode" to "template"
            ),
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.System),
                SwiftUIModifier.fontSize(component.iconSize),
                SwiftUIModifier.foregroundColor(
                    theme?.colorScheme?.onSurfaceVariant?.let { ModifierConverter.convertColor(it) }
                        ?: SwiftUIColor.system("systemGray")
                )
            )
        )
        children.add(iconView)

        // Add spacer after icon
        children.add(SwiftUIView(
            type = ViewType.Spacer,
            properties = emptyMap(),
            modifiers = listOf(
                SwiftUIModifier.frame(
                    width = null,
                    height = SizeValue.Fixed(16f),
                    alignment = ZStackAlignment.Center
                )
            )
        ))

        // 2. Message (Title)
        val titleView = SwiftUIView.text(
            content = component.message,
            modifiers = listOf(
                SwiftUIModifier.font(FontStyle.Headline),
                SwiftUIModifier.fontWeight(FontWeight.Medium),
                SwiftUIModifier.foregroundColor(
                    theme?.colorScheme?.onSurface?.let { ModifierConverter.convertColor(it) }
                        ?: SwiftUIColor.primary
                ),
                SwiftUIModifier.Custom("multilineTextAlignment", "center")
            )
        )
        children.add(titleView)

        // 3. Description (if present)
        if (component.hasDescription()) {
            // Add spacer before description
            children.add(SwiftUIView(
                type = ViewType.Spacer,
                properties = emptyMap(),
                modifiers = listOf(
                    SwiftUIModifier.frame(
                        width = null,
                        height = SizeValue.Fixed(8f),
                        alignment = ZStackAlignment.Center
                    )
                )
            ))

            val descriptionView = SwiftUIView.text(
                content = component.description ?: "",
                modifiers = listOf(
                    SwiftUIModifier.font(FontStyle.Subheadline),
                    SwiftUIModifier.foregroundColor(
                        theme?.colorScheme?.onSurfaceVariant?.let { ModifierConverter.convertColor(it) }
                            ?: SwiftUIColor.secondary
                    ),
                    SwiftUIModifier.Custom("multilineTextAlignment", "center"),
                    SwiftUIModifier.padding(32f, 32f, 0f, 32f)
                )
            )
            children.add(descriptionView)
        }

        // 4. Action Button (if available)
        if (component.isActionAvailable()) {
            // Add spacer before button
            children.add(SwiftUIView(
                type = ViewType.Spacer,
                properties = emptyMap(),
                modifiers = listOf(
                    SwiftUIModifier.frame(
                        width = null,
                        height = SizeValue.Fixed(24f),
                        alignment = ZStackAlignment.Center
                    )
                )
            ))

            val buttonView = SwiftUIView.button(
                label = component.actionLabel,
                action = "onAction",
                modifiers = listOf(
                    SwiftUIModifier.Custom("buttonStyle", "borderedProminent"),
                    SwiftUIModifier.Custom("controlSize", "large"),
                    SwiftUIModifier.foregroundColor(
                        theme?.colorScheme?.onPrimary?.let { ModifierConverter.convertColor(it) }
                            ?: SwiftUIColor.white
                    ),
                    SwiftUIModifier.background(
                        theme?.colorScheme?.primary?.let { ModifierConverter.convertColor(it) }
                            ?: SwiftUIColor.blue
                    ),
                    SwiftUIModifier.cornerRadius(8f),
                    SwiftUIModifier.padding(12f)
                )
            )
            children.add(buttonView)
        }

        // Add component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        // Build properties map
        val properties = mutableMapOf<String, Any>(
            "contentDescription" to component.getAccessibilityDescription()
        )

        // Add action callback if available
        if (component.onAction != null) {
            properties["onAction"] = "callback"
        }

        return SwiftUIView(
            type = ViewType.VStack,
            properties = mapOf(
                "spacing" to 0f,
                "alignment" to HorizontalAlignment.Center.name
            ),
            children = children,
            modifiers = modifiers,
            id = component.id
        )
    }

    /**
     * Convert Material icon names to SF Symbol names
     */
    private fun convertIconToSFSymbol(iconName: String): String {
        return when (iconName) {
            "inbox" -> "tray"
            "inbox_outline" -> "tray"
            "search_off" -> "magnifyingglass"
            "search" -> "magnifyingglass.circle"
            "favorite_border" -> "heart"
            "favorite" -> "heart.fill"
            "history" -> "clock"
            "notifications_none" -> "bell"
            "notifications" -> "bell.fill"
            "folder_open" -> "folder"
            "folder" -> "folder.fill"
            "description" -> "doc.text"
            "article" -> "doc"
            "list" -> "list.bullet"
            "assignment" -> "doc.text"
            "bookmark_border" -> "bookmark"
            "bookmark" -> "bookmark.fill"
            "shopping_cart" -> "cart"
            "mail_outline" -> "envelope"
            "mail" -> "envelope.fill"
            "event" -> "calendar"
            "group" -> "person.2"
            else -> "tray" // default
        }
    }
}

// ============================================
// SWIFTUI MODIFIER EXTENSIONS
// ============================================

/**
 * Extension functions for SwiftUI modifiers specific to state components
 */

/**
 * Custom modifier extension for special-case modifiers not in standard enum
 * Uses Animation ModifierType as a container for custom values
 */
fun SwiftUIModifier.Companion.Custom(name: String, value: Any): SwiftUIModifier {
    return SwiftUIModifier(
        type = ModifierType.Animation,
        value = mapOf("customType" to name, "customValue" to value)
    )
}
