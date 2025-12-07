/**
 * TestableComposables.kt - Test-accessible UI components
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team  
 * Created: 2025-01-28
 * 
 * Provides test-accessible versions of UI components
 */
package com.augmentalis.localizationmanager.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.augmentalis.localizationmanager.data.LocalizationDatabase
import com.augmentalis.localizationmanager.repository.PreferencesRepository

/**
 * LocalizationManagerScreen - Test wrapper for LocalizationManagerContent
 */
@Composable
fun LocalizationManagerScreen() {
    val context = LocalContext.current
    
    // Initialize database and repository using direct access pattern
    val database = LocalizationDatabase.getDatabase(context)
    val preferencesRepository = PreferencesRepository(database.preferencesDao())
    
    val viewModel: LocalizationViewModel = viewModel(
        factory = LocalizationViewModelFactory(context, preferencesRepository)
    )
    LocalizationManagerContent(viewModel = viewModel)
}