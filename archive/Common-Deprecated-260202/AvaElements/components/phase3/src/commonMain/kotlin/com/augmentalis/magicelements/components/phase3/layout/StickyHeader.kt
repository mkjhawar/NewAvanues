package com.augmentalis.magicelements.components.phase3.layout

import kotlinx.serialization.Serializable
import com.augmentalis.magicelements.core.types.Component

@Serializable
data class StickyHeader(
    val content: List<Component> = emptyList(),
    val stickyOffset: Float = 0f,
    val backgroundColor: String? = null,
    val elevation: Float = 4f,
    val zIndex: Int = 100
) : Component

@Serializable
data class PullToRefresh(
    val isRefreshing: Boolean = false,
    val content: List<Component> = emptyList(),
    val indicatorColor: String? = null,
    val backgroundColor: String? = null,
    val threshold: Float = 80f,
    val onRefresh: String? = null
) : Component
