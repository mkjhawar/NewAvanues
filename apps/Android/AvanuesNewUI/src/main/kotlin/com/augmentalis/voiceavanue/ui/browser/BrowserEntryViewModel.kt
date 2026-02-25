/**
 * BrowserEntryViewModel.kt - Hilt bridge for BrowserRepository
 *
 * Exposes Hilt-provided BrowserRepository to the BrowserApp composable
 * within the NavHost. This ViewModel is Activity-scoped via hiltViewModel(),
 * so BrowserRepository survives NavHost navigation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.browser

import androidx.lifecycle.ViewModel
import com.augmentalis.webavanue.BrowserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowserEntryViewModel @Inject constructor(
    val repository: BrowserRepository
) : ViewModel()
