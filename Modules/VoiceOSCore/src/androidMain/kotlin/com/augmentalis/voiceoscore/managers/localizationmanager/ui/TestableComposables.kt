/**
 * TestableComposables.kt - Test-accessible UI components
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-28
 * 
 * Provides test-accessible versions of UI components
 */
package com.augmentalis.voiceoscore.managers.localizationmanager.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.augmentalis.voiceoscore.managers.localizationmanager.data.LocalizationDatabase
import com.augmentalis.voiceoscore.managers.localizationmanager.repository.PreferencesRepository

/**
 * LocalizationManagerScreen - Test wrapper for LocalizationManagerContent
 */
@Composable
fun LocalizationManagerScreen() {
    val context = LocalContext.current
    
    // Initialize repository using SQLDelight adapter (direct access pattern)
    val preferencesRepository = PreferencesRepository(
        LocalizationDatabase.getPreferencesDao(context)
    )
    
    val viewModel: LocalizationViewModel = viewModel(
        factory = LocalizationViewModelFactory(context, preferencesRepository)
    )
    LocalizationManagerContent(viewModel = viewModel)
}