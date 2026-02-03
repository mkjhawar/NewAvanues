package com.augmentalis.avaelements.flutter.material.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * LoadingOverlay component - Flutter Material parity
 *
 * A Material Design 3 full-screen or container loading overlay with spinner and optional message.
 * Blocks user interaction while showing loading state.
 *
 * **Web Equivalent:** `Backdrop` + `CircularProgress` (MUI)
 * **Material Design 3:** https://m3.material.io/components/progress-indicators/overview
 *
 * ## Features
 * - Full-screen or container-scoped overlay
 * - Customizable backdrop color and opacity
 * - Spinner with configurable size and color
 * - Optional loading message
 * - Optional cancel button
 * - Blur effect support
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * LoadingOverlay(
 *     visible = true,
 *     message = "Loading your data...",
 *     spinnerSize = 48f,
 *     backdropOpacity = 0.7f,
 *     cancelable = true,
 *     onCancel = {
 *         // Handle cancel
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether overlay is visible
 * @property message Optional loading message
 * @property spinnerSize Spinner diameter in dp
 * @property spinnerColor Optional spinner color
 * @property backdropColor Optional backdrop color
 * @property backdropOpacity Backdrop opacity (0.0-1.0)
 * @property blurEffect Whether to apply blur to backdrop
 * @property cancelable Whether user can cancel/dismiss
 * @property cancelText Text for cancel button
 * @property fullScreen Whether to cover full screen or just parent container
 * @property contentDescription Accessibility description for TalkBack
 * @property onCancel Callback invoked when cancel is pressed (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class LoadingOverlay(
    override val type: String = "LoadingOverlay",
    override val id: String? = null,
    val visible: Boolean = true,
    val message: String? = null,
    val spinnerSize: Float = 48f,
    val spinnerColor: String? = null,
    val backdropColor: String? = null,
    val backdropOpacity: Float = 0.5f,
    val blurEffect: Boolean = false,
    val cancelable: Boolean = false,
    val cancelText: String = "Cancel",
    val fullScreen: Boolean = true,
    val contentDescription: String? = null,
    @Transient
    val onCancel: (() -> Unit)? = null,
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
        val base = contentDescription ?: "Loading overlay"
        val msg = if (message != null) ", $message" else ""
        val cancel = if (cancelable) ", cancelable" else ""
        return "$base$msg$cancel"
    }

    /**
     * Validate backdrop opacity is in valid range
     */
    fun isBackdropOpacityValid(): Boolean {
        return backdropOpacity in 0f..1f
    }

    companion object {
        /**
         * Create a simple loading overlay
         */
        fun simple(
            visible: Boolean = true,
            message: String? = null
        ) = LoadingOverlay(
            visible = visible,
            message = message
        )

        /**
         * Create a cancelable loading overlay
         */
        fun cancelable(
            visible: Boolean = true,
            message: String,
            onCancel: (() -> Unit)? = null
        ) = LoadingOverlay(
            visible = visible,
            message = message,
            cancelable = true,
            onCancel = onCancel
        )

        /**
         * Create a loading overlay for a container (not full screen)
         */
        fun container(
            visible: Boolean = true,
            message: String? = null,
            spinnerSize: Float = 32f
        ) = LoadingOverlay(
            visible = visible,
            message = message,
            spinnerSize = spinnerSize,
            fullScreen = false
        )

        /**
         * Create a loading overlay with blur effect
         */
        fun withBlur(
            visible: Boolean = true,
            message: String? = null,
            backdropOpacity: Float = 0.3f
        ) = LoadingOverlay(
            visible = visible,
            message = message,
            blurEffect = true,
            backdropOpacity = backdropOpacity
        )
    }
}
