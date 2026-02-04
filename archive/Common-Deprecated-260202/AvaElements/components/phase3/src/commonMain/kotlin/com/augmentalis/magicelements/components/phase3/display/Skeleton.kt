package com.augmentalis.avaelements.components.phase3.display

import com.augmentalis.avaelements.core.*
import com.augmentalis.avaelements.core.types.ComponentStyle
import com.augmentalis.avaelements.core.types.Size
import kotlinx.serialization.Transient

data class Skeleton(
    override val type: String = "Skeleton",
    override val id: String? = null,
    val variant: SkeletonVariant = SkeletonVariant.Text,
    val width: Size? = null,
    val height: Size? = null,
    val animation: SkeletonAnimation = SkeletonAnimation.Pulse,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}

enum class SkeletonVariant {
    Text,
    Rectangular,
    Circular,
    Rounded
}

enum class SkeletonAnimation {
    Pulse,
    Wave,
    None
}
