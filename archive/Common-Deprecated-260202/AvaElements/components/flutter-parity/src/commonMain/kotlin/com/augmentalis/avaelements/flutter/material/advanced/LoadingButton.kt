package com.augmentalis.avaelements.flutter.material.advanced

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * LoadingButton component - Flutter Material parity
 *
 * A Material Design 3 button that displays a loading indicator during async operations.
 * Automatically disables during loading and shows progress indicator.
 *
 * **Web Equivalent:** `LoadingButton` (MUI)
 * **Material Design 3:** https://m3.material.io/components/buttons/overview
 *
 * ## Features
 * - Automatic disable during loading
 * - Configurable loading indicator position (start/center/end)
 * - Optional loading text override
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility with busy state
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * LoadingButton(
 *     text = "Submit",
 *     loading = isLoading,
 *     loadingText = "Submitting...",
 *     onPressed = {
 *         // Handle button press
 *     }
 * )
 * ```
 *
 * @property id Unique identifier for the component
 * @property text Button text label
 * @property icon Optional icon name/resource to display
 * @property enabled Whether the button is enabled (will be overridden to false when loading)
 * @property loading Whether the button is in loading state
 * @property loadingPosition Position of loading indicator relative to text
 * @property loadingText Optional text to show during loading
 * @property contentDescription Accessibility description for TalkBack
 * @property onPressed Callback invoked when button is pressed (not serialized)
 * @property style Optional button style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.1.0-android-parity
 */
data class LoadingButton(
    override val type: String = "LoadingButton",
    override val id: String? = null,
    val text: String,
    val icon: String? = null,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val loadingPosition: LoadingPosition = LoadingPosition.Center,
    val loadingText: String? = null,
    val contentDescription: String? = null,
    @Transient
    val onPressed: (() -> Unit)? = null,
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
        val base = contentDescription ?: text
        val state = when {
            loading -> "button, loading"
            !enabled -> "button, disabled"
            else -> "button"
        }
        return "$base, $state"
    }

    /**
     * Check if button should be disabled
     */
    fun isDisabled(): Boolean = !enabled || loading

    /**
     * Get display text based on loading state
     */
    fun getDisplayText(): String {
        return if (loading && loadingText != null) {
            loadingText
        } else {
            text
        }
    }

    /**
     * Loading indicator position
     */
    enum class LoadingPosition {
        /** Indicator at start of text */
        Start,

        /** Indicator in center (hides text) */
        Center,

        /** Indicator at end of text */
        End
    }

    companion object {
        /**
         * Create a simple loading button
         */
        fun simple(
            text: String,
            loading: Boolean = false,
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = LoadingButton(
            text = text,
            loading = loading,
            enabled = enabled,
            onPressed = onPressed
        )

        /**
         * Create a loading button with custom loading text
         */
        fun withLoadingText(
            text: String,
            loadingText: String,
            loading: Boolean = false,
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = LoadingButton(
            text = text,
            loadingText = loadingText,
            loading = loading,
            enabled = enabled,
            onPressed = onPressed
        )

        /**
         * Create a loading button with icon
         */
        fun withIcon(
            text: String,
            icon: String,
            loading: Boolean = false,
            enabled: Boolean = true,
            onPressed: (() -> Unit)? = null
        ) = LoadingButton(
            text = text,
            icon = icon,
            loading = loading,
            enabled = enabled,
            onPressed = onPressed
        )
    }
}
