package com.augmentalis.avaelements.flutter.material.data

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * QRCode component - Flutter Material parity
 *
 * A QR code generator and display component using ZXing library.
 * Generates QR codes from string data with customizable styling.
 *
 * **Flutter Equivalent:** `qr_flutter` package
 * **Material Design 3:** Custom component with Material theming
 *
 * ## Features
 * - Generate QR codes from any string data
 * - Customizable size
 * - Error correction levels (L, M, Q, H)
 * - Color customization (foreground/background)
 * - Embedded logo/image support (center)
 * - Data validation (max 4296 characters)
 * - Material3 theming support
 * - Dark mode support
 * - TalkBack accessibility
 * - WCAG 2.1 Level AA compliant
 *
 * ## Usage Example
 * ```kotlin
 * QRCode(
 *     data = "https://example.com",
 *     size = 200f,
 *     errorCorrection = QRCode.ErrorCorrectionLevel.M,
 *     foregroundColor = 0xFF000000,
 *     backgroundColor = 0xFFFFFFFF,
 *     contentDescription = "QR code for example.com"
 * )
 * ```
 *
 * ## Error Correction Levels
 * - **L (Low):** ~7% error correction - use for clean environments
 * - **M (Medium):** ~15% error correction - standard use (default)
 * - **Q (Quartile):** ~25% error correction - use with logo overlay
 * - **H (High):** ~30% error correction - use for damaged/dirty surfaces
 *
 * @property id Unique identifier for the component
 * @property data String data to encode (max 4296 characters)
 * @property size Size of the QR code in dp (default 200)
 * @property errorCorrection Error correction level (L, M, Q, H)
 * @property foregroundColor Color for QR code modules (ARGB format)
 * @property backgroundColor Color for QR code background (ARGB format)
 * @property padding Padding around the QR code in dp
 * @property shape Custom shape for the container
 * @property embeddedImageUrl Optional center logo/image URL
 * @property embeddedImageSize Size of embedded image in dp
 * @property contentDescription Accessibility description for TalkBack
 * @property onTap Callback invoked when QR code is tapped (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 3.0.0-flutter-parity
 */
data class QRCode(
    override val type: String = "QRCode",
    override val id: String? = null,
    val data: String,
    val size: Float = 200f,
    val errorCorrection: ErrorCorrectionLevel = ErrorCorrectionLevel.M,
    val foregroundColor: Long = 0xFF000000, // Black
    val backgroundColor: Long = 0xFFFFFFFF, // White
    val padding: Float = 0f,
    val shape: String? = null,
    val embeddedImageUrl: String? = null,
    val embeddedImageSize: Float? = null,
    val contentDescription: String? = null,
    @Transient
    val onTap: (() -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * QR Code error correction level
     *
     * Higher levels allow more damage/obscuration but require more data capacity.
     */
    enum class ErrorCorrectionLevel {
        /** Low: ~7% error correction */
        L,

        /** Medium: ~15% error correction (default) */
        M,

        /** Quartile: ~25% error correction (recommended with logo) */
        Q,

        /** High: ~30% error correction */
        H
    }

    /**
     * Validate QR code data
     */
    fun isDataValid(): Boolean {
        return data.isNotBlank() && data.length <= MAX_DATA_LENGTH
    }

    /**
     * Get effective accessibility description
     */
    fun getAccessibilityDescription(): String {
        if (contentDescription != null) return contentDescription

        // Try to detect data type for better accessibility
        val dataType = when {
            data.startsWith("http://") || data.startsWith("https://") -> "URL"
            data.startsWith("mailto:") -> "Email"
            data.startsWith("tel:") -> "Phone number"
            data.startsWith("sms:") -> "SMS"
            data.startsWith("wifi:") -> "WiFi credentials"
            data.contains("@") && data.contains(".") -> "Email or contact"
            data.matches(Regex("\\d+")) -> "Numeric code"
            else -> "Text"
        }

        return "QR code containing $dataType: ${data.take(50)}"
    }

    /**
     * Get recommended error correction for current configuration
     */
    fun getRecommendedErrorCorrection(): ErrorCorrectionLevel {
        return if (embeddedImageUrl != null) {
            // Use higher error correction with embedded image
            ErrorCorrectionLevel.Q
        } else {
            ErrorCorrectionLevel.M
        }
    }

    /**
     * Calculate capacity for current error correction level
     */
    fun getCapacity(): Int {
        return when (errorCorrection) {
            ErrorCorrectionLevel.L -> 2953 // Alphanumeric
            ErrorCorrectionLevel.M -> 2331
            ErrorCorrectionLevel.Q -> 1663
            ErrorCorrectionLevel.H -> 1273
        }
    }

    /**
     * Check if data fits within capacity
     */
    fun fitsInCapacity(): Boolean {
        return data.length <= getCapacity()
    }

    /**
     * Get hex color string from Long ARGB
     */
    fun getForegroundColorHex(): String {
        return "#${foregroundColor.toString(16).padStart(8, '0')}"
    }

    /**
     * Get hex color string from Long ARGB
     */
    fun getBackgroundColorHex(): String {
        return "#${backgroundColor.toString(16).padStart(8, '0')}"
    }

    companion object {
        /** Maximum data length for QR codes (binary mode) */
        const val MAX_DATA_LENGTH = 4296

        /**
         * Create a QR code for a URL
         */
        fun url(
            url: String,
            size: Float = 200f
        ) = QRCode(
            data = url,
            size = size,
            errorCorrection = ErrorCorrectionLevel.M,
            contentDescription = "QR code for $url"
        )

        /**
         * Create a QR code for WiFi credentials
         */
        fun wifi(
            ssid: String,
            password: String,
            security: String = "WPA",
            hidden: Boolean = false
        ) = QRCode(
            data = "WIFI:T:$security;S:$ssid;P:$password;H:$hidden;;",
            errorCorrection = ErrorCorrectionLevel.M,
            contentDescription = "QR code for WiFi network $ssid"
        )

        /**
         * Create a QR code for contact information (vCard)
         */
        fun contact(
            name: String,
            phone: String? = null,
            email: String? = null
        ): QRCode {
            val vcard = buildString {
                append("BEGIN:VCARD\n")
                append("VERSION:3.0\n")
                append("FN:$name\n")
                if (phone != null) append("TEL:$phone\n")
                if (email != null) append("EMAIL:$email\n")
                append("END:VCARD")
            }
            return QRCode(
                data = vcard,
                errorCorrection = ErrorCorrectionLevel.M,
                contentDescription = "QR code for contact $name"
            )
        }

        /**
         * Create a QR code with embedded logo
         */
        fun withLogo(
            data: String,
            logoUrl: String,
            size: Float = 200f,
            logoSize: Float = 40f
        ) = QRCode(
            data = data,
            size = size,
            errorCorrection = ErrorCorrectionLevel.Q, // Higher correction for logo
            embeddedImageUrl = logoUrl,
            embeddedImageSize = logoSize
        )

        /**
         * Create a custom colored QR code
         */
        fun colored(
            data: String,
            foregroundColor: Long,
            backgroundColor: Long,
            size: Float = 200f
        ) = QRCode(
            data = data,
            size = size,
            foregroundColor = foregroundColor,
            backgroundColor = backgroundColor
        )
    }
}
