package com.augmentalis.avaelements.components.phase1.display

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.core.Renderer
import kotlinx.serialization.Transient

/**
 * Image component for displaying images
 *
 * A cross-platform image display component supporting various sources
 * (local, network, assets), content scaling, and loading states.
 *
 * @property id Unique identifier for the component
 * @property source Image source (URL, file path, or asset name)
 * @property contentDescription Accessibility description of image
 * @property contentScale How image should be scaled within bounds
 * @property placeholder Optional placeholder image source while loading
 * @property error Optional error image source if loading fails
 * @property onLoad Callback invoked when image loads successfully (not serialized)
 * @property onError Callback invoked if image fails to load (not serialized)
 * @property style Optional style overrides
 * @property modifiers List of modifiers for declarative styling
 *
 * @since 2.0.0
 */

data class Image(
    override val type: String = "Image",
    override val id: String? = null,
    val source: String,
    val contentDescription: String? = null,
    val contentScale: ContentScale = ContentScale.Fit,
    val placeholder: String? = null,
    val error: String? = null,
    @Transient
    val onLoad: (() -> Unit)? = null,
    @Transient
    val onError: ((String) -> Unit)? = null,
    override val style: ComponentStyle? = null,
    @Transient
    override val modifiers: List<Modifier> = emptyList()
) : Component {

    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }

    /**
     * Content scaling modes
     */
    
    enum class ContentScale {
        /** Scale to fit within bounds, maintaining aspect ratio */
        Fit,

        /** Scale to fill bounds, maintaining aspect ratio (may crop) */
        Crop,

        /** Scale to fill bounds, ignoring aspect ratio (may stretch) */
        Fill,

        /** Display at original size (no scaling) */
        None,

        /** Scale down only if larger than bounds */
        Inside
    }

    companion object {
        /**
         * Create image from URL
         */
        fun url(
            url: String,
            contentDescription: String? = null,
            contentScale: ContentScale = ContentScale.Fit
        ) = Image(
            source = url,
            contentDescription = contentDescription,
            contentScale = contentScale
        )

        /**
         * Create image from asset
         */
        fun asset(
            assetName: String,
            contentDescription: String? = null,
            contentScale: ContentScale = ContentScale.Fit
        ) = Image(
            source = "asset://$assetName",
            contentDescription = contentDescription,
            contentScale = contentScale
        )

        /**
         * Create image from file
         */
        fun file(
            filePath: String,
            contentDescription: String? = null,
            contentScale: ContentScale = ContentScale.Fit
        ) = Image(
            source = "file://$filePath",
            contentDescription = contentDescription,
            contentScale = contentScale
        )
    }
}
