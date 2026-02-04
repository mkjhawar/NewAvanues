/**
 * Deprecated type aliases for backward compatibility
 *
 * These utilities have been moved to the Shared Foundation module.
 * Import from com.augmentalis.shared.state.* or com.augmentalis.shared.viewmodel.*
 *
 * This file will be removed in a future version.
 */
package com.augmentalis.webavanue.util

// State Management
@Deprecated(
    message = "Use com.augmentalis.shared.state.ViewModelState",
    replaceWith = ReplaceWith("com.augmentalis.shared.state.ViewModelState")
)
typealias ViewModelState<T> = com.augmentalis.shared.state.ViewModelState<T>

@Deprecated(
    message = "Use com.augmentalis.shared.state.NullableState",
    replaceWith = ReplaceWith("com.augmentalis.shared.state.NullableState")
)
typealias NullableState<T> = com.augmentalis.shared.state.NullableState<T>

@Deprecated(
    message = "Use com.augmentalis.shared.state.UiState",
    replaceWith = ReplaceWith("com.augmentalis.shared.state.UiState")
)
typealias UiState = com.augmentalis.shared.state.UiState

@Deprecated(
    message = "Use com.augmentalis.shared.state.ListState",
    replaceWith = ReplaceWith("com.augmentalis.shared.state.ListState")
)
typealias ListState<T> = com.augmentalis.shared.state.ListState<T>

@Deprecated(
    message = "Use com.augmentalis.shared.state.SearchState",
    replaceWith = ReplaceWith("com.augmentalis.shared.state.SearchState")
)
typealias SearchState = com.augmentalis.shared.state.SearchState

@Deprecated(
    message = "Use com.augmentalis.shared.state.SettingsUpdater",
    replaceWith = ReplaceWith("com.augmentalis.shared.state.SettingsUpdater")
)
typealias SettingsUpdater<T> = com.augmentalis.shared.state.SettingsUpdater<T>

// ViewModels
@Deprecated(
    message = "Use com.augmentalis.shared.viewmodel.BaseViewModel",
    replaceWith = ReplaceWith("com.augmentalis.shared.viewmodel.BaseViewModel")
)
typealias BaseViewModel = com.augmentalis.shared.viewmodel.BaseViewModel

@Deprecated(
    message = "Use com.augmentalis.shared.viewmodel.BaseStatefulViewModel",
    replaceWith = ReplaceWith("com.augmentalis.shared.viewmodel.BaseStatefulViewModel")
)
typealias BaseStatefulViewModel = com.augmentalis.shared.viewmodel.BaseStatefulViewModel
