/**
 * MagicVoiceHandlerRegistry.kt - Registry for MagicVoiceHandlers integration with VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 *
 * Purpose: Wire all MagicVoiceHandlers to VoiceOSCore's HandlerRegistry
 */

package com.augmentalis.avamagic.voice.handlers

import com.avanues.logging.LoggerFactory
import com.augmentalis.avamagic.voice.handlers.display.*
import com.augmentalis.avamagic.voice.handlers.feedback.*
import com.augmentalis.avamagic.voice.handlers.input.*
import com.augmentalis.avamagic.voice.handlers.navigation.*
import com.augmentalis.voiceoscore.IHandler
import com.augmentalis.voiceoscore.IHandlerRegistry

/**
 * Provider interface for handler executors.
 * Platform implementations provide executors for each handler type.
 */
interface MagicVoiceExecutorProvider {
    // Input executors
    val autocompleteExecutor: AutocompleteExecutor
    val colorPickerExecutor: ColorPickerExecutor
    val datePickerExecutor: DatePickerExecutor
    val fileUploadExecutor: FileUploadExecutor
    val iconPickerExecutor: IconPickerExecutor
    val multiSelectExecutor: MultiSelectExecutor
    val rangeSliderExecutor: RangeSliderExecutor
    val ratingExecutor: RatingExecutor
    val searchBarExecutor: SearchBarExecutor
    val sliderExecutor: SliderExecutor
    val stepperExecutor: StepperExecutor
    val tagInputExecutor: TagInputExecutor
    val timePickerExecutor: TimePickerExecutor
    val toggleExecutor: ToggleExecutor

    // Display executors
    val avatarExecutor: AvatarExecutor
    val badgeExecutor: BadgeExecutor
    val canvas3DExecutor: Canvas3DExecutor
    val carouselExecutor: CarouselExecutor
    val chipExecutor: ChipExecutor
    val progressExecutor: ProgressExecutor
    val tableExecutor: TableExecutor
    val treeViewExecutor: TreeViewExecutor

    // Feedback executors
    val alertExecutor: AlertExecutor
    val confirmExecutor: ConfirmExecutor
    val dialogExecutor: DialogExecutor
    val drawerExecutor: DrawerExecutor
    val modalExecutor: ModalExecutor
    val snackbarExecutor: SnackbarExecutor
    val toastExecutor: ToastExecutor

    // Navigation executors
    val appBarExecutor: AppBarExecutor
    val bottomNavExecutor: BottomNavExecutor
    val breadcrumbExecutor: BreadcrumbExecutor
    val paginationExecutor: PaginationExecutor
    val tabsExecutor: TabsExecutor
}

/**
 * Registry for MagicVoiceHandlers.
 *
 * @param provider Platform-specific executor provider
 */
class MagicVoiceHandlerRegistry(
    private val provider: MagicVoiceExecutorProvider
) {
    companion object {
        private const val TAG = "MagicVoiceHandlerRegistry"
        private val Log = LoggerFactory.getLogger(TAG)
    }

    private val inputHandlers: List<IHandler> by lazy {
        listOf(
            AutocompleteHandler(provider.autocompleteExecutor),
            ColorPickerHandler(provider.colorPickerExecutor),
            DatePickerHandler(provider.datePickerExecutor),
            FileUploadHandler(provider.fileUploadExecutor),
            IconPickerHandler(provider.iconPickerExecutor),
            MultiSelectHandler(provider.multiSelectExecutor),
            RangeSliderHandler(provider.rangeSliderExecutor),
            RatingHandler(provider.ratingExecutor),
            SearchBarHandler(provider.searchBarExecutor),
            SliderHandler(provider.sliderExecutor),
            StepperHandler(provider.stepperExecutor),
            TagInputHandler(provider.tagInputExecutor),
            TimePickerHandler(provider.timePickerExecutor),
            ToggleHandler(provider.toggleExecutor)
        )
    }

    private val displayHandlers: List<IHandler> by lazy {
        listOf(
            AvatarHandler(provider.avatarExecutor),
            BadgeHandler(provider.badgeExecutor),
            Canvas3DHandler(provider.canvas3DExecutor),
            CarouselHandler(provider.carouselExecutor),
            ChipHandler(provider.chipExecutor),
            ProgressHandler(provider.progressExecutor),
            TableHandler(provider.tableExecutor),
            TreeViewHandler(provider.treeViewExecutor)
        )
    }

    private val feedbackHandlers: List<IHandler> by lazy {
        listOf(
            AlertHandler(provider.alertExecutor),
            ConfirmHandler(provider.confirmExecutor),
            DialogHandler(provider.dialogExecutor),
            DrawerHandler(provider.drawerExecutor),
            ModalHandler(provider.modalExecutor),
            SnackbarHandler(provider.snackbarExecutor),
            ToastHandler(provider.toastExecutor)
        )
    }

    private val navigationHandlers: List<IHandler> by lazy {
        listOf(
            AppBarHandler(provider.appBarExecutor),
            BottomNavHandler(provider.bottomNavExecutor),
            BreadcrumbHandler(provider.breadcrumbExecutor),
            PaginationHandler(provider.paginationExecutor),
            TabsHandler(provider.tabsExecutor)
        )
    }

    val allHandlers: List<IHandler>
        get() = inputHandlers + displayHandlers + feedbackHandlers + navigationHandlers

    /**
     * Register all MagicVoiceHandlers with VoiceOSCore's HandlerRegistry.
     */
    suspend fun registerAll(registry: IHandlerRegistry): Int {
        var registered = 0
        allHandlers.forEach { handler ->
            try {
                registry.register(handler)
                registered++
                Log.d { "Registered: ${handler::class.simpleName}" }
            } catch (e: Exception) {
                Log.e({ "Failed to register ${handler::class.simpleName}" }, e)
            }
        }
        Log.i { "Registered $registered/${allHandlers.size} handlers" }
        return registered
    }

    suspend fun registerInputHandlers(registry: IHandlerRegistry): Int =
        registerList(registry, inputHandlers, "input")

    suspend fun registerDisplayHandlers(registry: IHandlerRegistry): Int =
        registerList(registry, displayHandlers, "display")

    suspend fun registerFeedbackHandlers(registry: IHandlerRegistry): Int =
        registerList(registry, feedbackHandlers, "feedback")

    suspend fun registerNavigationHandlers(registry: IHandlerRegistry): Int =
        registerList(registry, navigationHandlers, "navigation")

    suspend fun unregisterAll(registry: IHandlerRegistry): Int {
        var unregistered = 0
        allHandlers.forEach { handler ->
            try {
                if (registry.unregister(handler)) unregistered++
            } catch (e: Exception) {
                Log.e({ "Failed to unregister ${handler::class.simpleName}" }, e)
            }
        }
        return unregistered
    }

    fun getAllSupportedActions(): List<String> =
        allHandlers.flatMap { it.supportedActions }

    fun getHandlerCountByCategory(): Map<String, Int> = mapOf(
        "input" to inputHandlers.size,
        "display" to displayHandlers.size,
        "feedback" to feedbackHandlers.size,
        "navigation" to navigationHandlers.size,
        "total" to allHandlers.size
    )

    private suspend fun registerList(
        registry: IHandlerRegistry,
        handlers: List<IHandler>,
        category: String
    ): Int {
        var registered = 0
        handlers.forEach { handler ->
            try {
                registry.register(handler)
                registered++
            } catch (e: Exception) {
                Log.e({ "Failed to register $category/${handler::class.simpleName}" }, e)
            }
        }
        Log.i { "Registered $registered/${handlers.size} $category handlers" }
        return registered
    }
}
