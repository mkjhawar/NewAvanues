package com.augmentalis.magicelements.components.phase3.onboarding

import kotlinx.serialization.Serializable
import com.augmentalis.magicelements.core.types.Component

@Serializable
data class IntroScreen(
    val pages: List<IntroPage> = emptyList(),
    val currentPage: Int = 0,
    val showSkip: Boolean = true,
    val showNext: Boolean = true,
    val showDone: Boolean = true,
    val skipLabel: String = "Skip",
    val nextLabel: String = "Next",
    val doneLabel: String = "Get Started",
    val indicatorColor: String? = null,
    val activeIndicatorColor: String? = null,
    val onSkip: String? = null,
    val onDone: String? = null,
    val onPageChange: String? = null
) : Component

@Serializable
data class IntroPage(
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val icon: String? = null,
    val backgroundColor: String? = null
)

@Serializable
data class OnboardingStep(
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val icon: String? = null,
    val isCompleted: Boolean = false
) : Component
