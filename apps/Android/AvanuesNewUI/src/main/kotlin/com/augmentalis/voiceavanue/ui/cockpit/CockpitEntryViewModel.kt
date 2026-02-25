/*
 * Copyright (c) 2026 Manoj Jhawar, Aman Jhawar
 * Intelligent Devices LLC
 * All rights reserved.
 */

package com.augmentalis.voiceavanue.ui.cockpit

import androidx.lifecycle.ViewModel
import com.augmentalis.cockpit.repository.AndroidCockpitRepository
import com.augmentalis.cockpit.viewmodel.CockpitViewModel
import com.augmentalis.database.VoiceOSDatabaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Hilt entry-point ViewModel for the Cockpit screen.
 *
 * Follows the same pattern as [BrowserEntryViewModel]: a thin Hilt wrapper
 * that creates the KMP-compatible [CockpitViewModel] with its required
 * [AndroidCockpitRepository] dependency from the shared SQLDelight database.
 *
 * The [CockpitViewModel] is a plain Kotlin class (not Android ViewModel)
 * for KMP compatibility — this wrapper provides the Hilt injection bridge.
 */
@HiltViewModel
class CockpitEntryViewModel @Inject constructor(
    databaseManager: VoiceOSDatabaseManager
) : ViewModel() {

    private val repository = AndroidCockpitRepository(databaseManager.getDatabase())

    val cockpitViewModel = CockpitViewModel(repository = repository).also {
        it.initialize()
    }

    override fun onCleared() {
        super.onCleared()
        cockpitViewModel.dispose()
    }
}
