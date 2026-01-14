package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * ErrorState component - Flutter Material parity
 *
 * A Material Design 3 error placeholder that displays when an error occurs.
 * Shows icon, error message, and optional retry action.
 *
 * **Web Equivalent:** `Alert` with error severity (MUI)
 * **Material Design 3:** https://m3.material.io/components/dialogs/overview
 *
 * ## Features
 * - Clear error icon
 * - Primary error message
 * - Optional detailed description
 * - Retry action button
 * - Configurable icon and colors
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility with error announcements
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * ErrorState(
 *     message = "Failed to load data",
 *     description = "Please check your internet connection and try again.",
 *     icon = "error_outline",
 *     showRetry = true,
 *     onRetry = { /* retry action */ }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether error state is visible
 * @property message Primary error message
 * @property description Optional detailed description
 * @property icon Icon name (default "error_outline")
 * @property iconSize Icon size in dp (default 64)
 * @property showRetry Whether to show retry button
 * @property retryLabel Retry button label (default "Try Again")
 * @property onRetry Callback when retry is clicked
 * @property color Optional custom error color
 * @property contentDescription Accessibility description for TalkBack
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class ErrorState(
    override val type: String = "ErrorState",
    override val id: String? = null,
    val visible: Boolean = true,
    val message: String,
    val description: String? = null,
    val icon: String = "error_outline",
    val iconSize: Float = 64f,
    val showRetry: Boolean = true,
    val retryLabel: String = "Try Again",
    val onRetry: (() -> Unit)? = null,
    val color: String? = null,
    val contentDescription: String? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        val desc = description?.let { ". $it" } ?: ""
        val retryText = if (showRetry) ". $retryLabel button available" else ""
        return contentDescription ?: "Error: $message$desc$retryText"
    }

    /**
     * Check if has description
     */
    fun hasDescription(): Boolean = !description.isNullOrBlank()

    /**
     * Check if retry is available
     */
    fun isRetryAvailable(): Boolean = showRetry && onRetry != null

    companion object {
        /**
         * Create a network error state
         */
        fun networkError(
            onRetry: () -> Unit
        ) = ErrorState(
            message = "No Internet Connection",
            description = "Please check your connection and try again.",
            icon = "wifi_off",
            onRetry = onRetry
        )

        /**
         * Create a generic error state
         */
        fun generic(
            message: String,
            onRetry: (() -> Unit)? = null
        ) = ErrorState(
            message = message,
            description = "Something went wrong. Please try again.",
            onRetry = onRetry
        )

        /**
         * Create a server error state
         */
        fun serverError(
            onRetry: () -> Unit
        ) = ErrorState(
            message = "Server Error",
            description = "Unable to connect to server. Please try again later.",
            icon = "cloud_off",
            onRetry = onRetry
        )

        /**
         * Create a not found error state
         */
        fun notFound(
            resourceName: String = "Resource"
        ) = ErrorState(
            message = "$resourceName Not Found",
            description = "The requested $resourceName could not be found.",
            icon = "search_off",
            showRetry = false
        )
    }
}
