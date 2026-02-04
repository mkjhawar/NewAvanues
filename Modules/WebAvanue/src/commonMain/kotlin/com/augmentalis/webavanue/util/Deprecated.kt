/**
 * Deprecated type aliases for backward compatibility
 *
 * These utilities have been moved to the Shared Foundation module.
 * Import from com.augmentalis.foundation.state.* or com.augmentalis.foundation.viewmodel.*
 *
 * This file will be removed in a future version.
 */
package com.augmentalis.webavanue.util

// State Management
@Deprecated(
    message = "Use com.augmentalis.foundation.state.ViewModelState",
    replaceWith = ReplaceWith("com.augmentalis.foundation.state.ViewModelState")
)
typealias ViewModelState<T> = com.augmentalis.foundation.state.ViewModelState<T>

@Deprecated(
    message = "Use com.augmentalis.foundation.state.NullableState",
    replaceWith = ReplaceWith("com.augmentalis.foundation.state.NullableState")
)
typealias NullableState<T> = com.augmentalis.foundation.state.NullableState<T>

@Deprecated(
    message = "Use com.augmentalis.foundation.state.UiState",
    replaceWith = ReplaceWith("com.augmentalis.foundation.state.UiState")
)
typealias UiState = com.augmentalis.foundation.state.UiState

@Deprecated(
    message = "Use com.augmentalis.foundation.state.ListState",
    replaceWith = ReplaceWith("com.augmentalis.foundation.state.ListState")
)
typealias ListState<T> = com.augmentalis.foundation.state.ListState<T>

@Deprecated(
    message = "Use com.augmentalis.foundation.state.SearchState",
    replaceWith = ReplaceWith("com.augmentalis.foundation.state.SearchState")
)
typealias SearchState = com.augmentalis.foundation.state.SearchState

@Deprecated(
    message = "Use com.augmentalis.foundation.state.SettingsUpdater",
    replaceWith = ReplaceWith("com.augmentalis.foundation.state.SettingsUpdater")
)
typealias SettingsUpdater<T> = com.augmentalis.foundation.state.SettingsUpdater<T>

// ViewModels
@Deprecated(
    message = "Use com.augmentalis.foundation.viewmodel.BaseViewModel",
    replaceWith = ReplaceWith("com.augmentalis.foundation.viewmodel.BaseViewModel")
)
typealias BaseViewModel = com.augmentalis.foundation.viewmodel.BaseViewModel

@Deprecated(
    message = "Use com.augmentalis.foundation.viewmodel.BaseStatefulViewModel",
    replaceWith = ReplaceWith("com.augmentalis.foundation.viewmodel.BaseStatefulViewModel")
)
typealias BaseStatefulViewModel = com.augmentalis.foundation.viewmodel.BaseStatefulViewModel
