/**
 * UnifiedSettingsViewModel.kt - ViewModel for the unified settings screen
 *
 * Collects all ComposableSettingsProviders via Hilt @IntoSet injection,
 * sorts them by sortOrder, and provides them to the UI.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UnifiedSettingsViewModel @Inject constructor(
    val providers: Set<@JvmSuppressWildcards ComposableSettingsProvider>
) : ViewModel() {

    /** Providers sorted by sortOrder (lower = higher in list) */
    val sortedProviders: List<ComposableSettingsProvider> by lazy {
        providers
            .filter { it.isEnabled }
            .sortedBy { it.sortOrder }
    }

    /** Find a provider by moduleId */
    fun providerById(moduleId: String): ComposableSettingsProvider? =
        sortedProviders.find { it.moduleId == moduleId }

    /** Search providers and their searchable entries */
    fun search(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()

        return sortedProviders.flatMap { provider ->
            val moduleMatch = provider.displayName.lowercase().contains(lowerQuery)
            val entryMatches = provider.searchableEntries.filter { entry ->
                entry.displayName.lowercase().contains(lowerQuery) ||
                    entry.keywords.any { it.lowercase().contains(lowerQuery) }
            }

            if (moduleMatch && entryMatches.isEmpty()) {
                listOf(SearchResult(provider, sectionId = provider.sections.firstOrNull()?.id ?: ""))
            } else {
                entryMatches.map { entry ->
                    SearchResult(provider, sectionId = entry.sectionId, settingKey = entry.key)
                }
            }
        }
    }
}

/**
 * A search result pointing to a specific provider and section.
 */
data class SearchResult(
    val provider: ComposableSettingsProvider,
    val sectionId: String,
    val settingKey: String? = null
)
