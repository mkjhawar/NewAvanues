package com.augmentalis.avaelements.flutter.material.carousel

import kotlinx.serialization.Serializable
import com.augmentalis.magicelements.core.types.Component

@Serializable
data class ProductCarousel(
    val products: List<ProductItem> = emptyList(),
    val autoPlay: Boolean = false,
    val autoPlayInterval: Int = 3000,
    val showIndicators: Boolean = true,
    val showArrows: Boolean = true,
    val onProductTap: String? = null
) : Component

@Serializable
data class ProductItem(
    val id: String,
    val imageUrl: String,
    val title: String,
    val price: String,
    val originalPrice: String? = null,
    val rating: Float? = null,
    val badge: String? = null
)

@Serializable
data class FullWidthCarousel(
    val items: List<CarouselSlide> = emptyList(),
    val autoPlay: Boolean = true,
    val autoPlayInterval: Int = 5000,
    val showIndicators: Boolean = true,
    val aspectRatio: Float = 16f / 9f,
    val onSlideChange: String? = null
) : Component

@Serializable
data class CarouselSlide(
    val imageUrl: String,
    val title: String? = null,
    val subtitle: String? = null,
    val ctaLabel: String? = null,
    val ctaAction: String? = null
)

@Serializable
data class FullSizeCarousel(
    val items: List<CarouselSlide> = emptyList(),
    val autoPlay: Boolean = true,
    val autoPlayInterval: Int = 5000,
    val showIndicators: Boolean = true,
    val enableParallax: Boolean = false,
    val onSlideChange: String? = null
) : Component
