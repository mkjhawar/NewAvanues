package com.augmentalis.avamagic.ui.core.display

import com.augmentalis.avamagic.components.core.*

/**
 * Text component - displays text with styling
 */
data class TextComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val text: String,
    val fontSize: Float = 14f,
    val fontWeight: FontWeight = FontWeight.Normal,
    val font: TextFont? = null, // For mapper compatibility
    val color: Color = Color.Black,
    val textAlign: TextAlign = TextAlign.Start,
    val maxLines: Int? = null,
    val overflow: TextOverflow = TextOverflow.Clip,
    val lineHeight: Float? = null,
    val letterSpacing: Float? = null,
    val textDecoration: TextDecoration? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

// Font configuration for mapper
data class TextFont(
    val family: String? = null,
    val size: Float = 14f,
    val weight: FontWeight = FontWeight.Normal,
    val style: FontStyle = FontStyle.Normal
)

enum class FontStyle {
    Normal,
    Italic
}

/**
 * Icon component - displays icons from icon libraries
 */
data class IconComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val icon: String,
    val size: Float = 24f,
    val color: Color = Color.Black,
    val contentDescription: String? = null
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

/**
 * Image component - displays images from various sources
 */
data class ImageComponent(
    override val id: String? = null,
    override val style: ComponentStyle? = null,
    override val modifiers: List<Modifier> = emptyList(),
    val source: ImageSource,
    val contentDescription: String? = null,
    val contentScale: ContentScale = ContentScale.Fit,
    val placeholder: String? = null,
    val errorImage: String? = null,
    val crossfade: Boolean = true,
    val crossfadeDuration: Int = 300
) : Component {
    override fun render(renderer: Renderer): Any = renderer.renderComponent(this)
}

// Text styling enums

enum class FontWeight {
    Thin,
    ExtraLight,
    Light,
    Normal,
    Medium,
    SemiBold,
    Bold,
    ExtraBold,
    Black
}

enum class TextAlign {
    Start,
    Center,
    End,
    Justify
}

enum class TextOverflow {
    Clip,
    Ellipsis,
    Visible
}

enum class TextDecoration {
    None,
    Underline,
    LineThrough
}

// Image types

sealed class ImageSource {
    data class Url(val url: String) : ImageSource()
    data class Asset(val name: String) : ImageSource()
    data class Resource(val id: Int) : ImageSource()
    data class Base64(val data: String) : ImageSource()
}

enum class ContentScale {
    Fit,
    Fill,
    Crop,
    Inside,
    None
}

// TextScope for mapper compatibility
object TextScope {
    val Left = TextAlign.Start
    val Center = TextAlign.Center
    val Right = TextAlign.End
    val Justify = TextAlign.Justify

    val Clip = TextOverflow.Clip
    val Ellipsis = TextOverflow.Ellipsis
    val Visible = TextOverflow.Visible
}
