package com.augmentalis.avaelements.flutter.material.feedback

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * FullPageLoading component - Flutter Material parity
 *
 * A Material Design 3 full-screen loading state that ALWAYS covers the entire viewport.
 * Unlike LoadingOverlay, this component is specifically designed for full-page loading states.
 *
 * **Web Equivalent:** `Backdrop` fullscreen (MUI), `Spin` fullscreen (Ant Design)
 * **Material Design 3:** https://m3.material.io/components/progress-indicators/overview
 *
 * ## Features
 * - Always full-screen (no container mode)
 * - Modal backdrop blocks all interaction
 * - Large centered spinner
 * - Optional loading message
 * - Optional cancel button
 * - Fixed dimmed background (no transparency control)
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * FullPageLoading(
 *     visible = true,
 *     message = "Loading your dashboard...",
 *     cancelable = true,
 *     onCancel = {
 *         // Handle cancel/navigation away
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property visible Whether loading overlay is visible
 * @property message Optional loading message
 * @property spinnerSize Spinner diameter in dp (default 64)
 * @property cancelable Whether user can cancel/dismiss
 * @property cancelText Text for cancel button
 * @property contentDescription Accessibility description for TalkBack
 * @property onCancel Callback invoked when cancel is pressed (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.2.0-feedback-components
 */
data class FullPageLoading(
    override val type: String = "FullPageLoading",
    override val id: String? = null,
    val visible: Boolean = true,
    val message: String? = null,
    val spinnerSize: Float = 64f,
    val cancelable: Boolean = false,
    val cancelText: String = "Cancel",
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
        val base = contentDescription ?: "Loading, please wait"
        val msg = if (message != null) ". $message" else ""
        val cancel = if (cancelable) ". Double tap cancel to dismiss" else ""
        return "$base$msg$cancel"
    }

    /**
     * Validate spinner size
     */
    fun isSpinnerSizeValid(): Boolean {
        return spinnerSize > 0f && spinnerSize <= 200f
    }

    companion object {
        /** Default spinner size for full page loading */
        const val DEFAULT_SPINNER_SIZE = 64f

        /** Fixed backdrop opacity (not customizable) */
        const val BACKDROP_OPACITY = 0.7f

        /**
         * Create a simple full-page loading
         */
        fun simple(
            visible: Boolean = true,
            message: String? = null
        ) = FullPageLoading(
            visible = visible,
            message = message
        )

        /**
         * Create a cancelable full-page loading
         */
        fun cancelable(
            visible: Boolean = true,
            message: String,
            onCancel: (() -> Unit)? = null
        ) = FullPageLoading(
            visible = visible,
            message = message,
            cancelable = true,
            onCancel = onCancel
        )

        /**
         * Create a loading state with custom spinner size
         */
        fun withCustomSpinner(
            visible: Boolean = true,
            message: String? = null,
            spinnerSize: Float
        ) = FullPageLoading(
            visible = visible,
            message = message,
            spinnerSize = spinnerSize
        )
    }
}
