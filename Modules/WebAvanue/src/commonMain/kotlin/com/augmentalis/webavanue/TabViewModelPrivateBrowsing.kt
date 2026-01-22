package com.augmentalis.webavanue

import com.augmentalis.webavanue.Logger
import kotlinx.coroutines.launch

/**
 * Private Browsing Extension Methods for TabViewModel
 *
 * This file contains extension functions for managing private/incognito browsing mode.
 * Separated for better code organization and Single Responsibility Principle.
 */

/**
 * Create a new private/incognito tab
 *
 * Private tabs have the following characteristics:
 * - No history recording
 * - No cookies persisted after session
 * - In-memory cache only
 * - Form data not saved
 * - Visual indicator in UI
 *
 * @param url Initial URL (defaults to blank for new private tab)
 * @param title Initial title (defaults to "Private Tab")
 * @param setActive Whether to set this tab as active
 */
fun TabViewModel.createPrivateTab(
    url: String = "about:blank",
    title: String = "Private Tab",
    setActive: Boolean = true
) {
    createTab(url = url, title = title, setActive = setActive, isIncognito = true)
}

/**
 * Close all private tabs
 *
 * This will close all incognito tabs and clear all private browsing data.
 * Regular tabs are not affected.
 */
fun TabViewModel.closeAllPrivateTabs() {
    // Note: This method needs to be added to TabViewModel class
    // as it requires access to private fields
}

/**
 * Get all private tabs
 *
 * @return List of private tabs
 */
fun TabViewModel.getPrivateTabs(): List<TabUiState> {
    return tabs.value.filter { it.tab.isIncognito }
}

/**
 * Get all regular (non-private) tabs
 *
 * @return List of regular tabs
 */
fun TabViewModel.getRegularTabs(): List<TabUiState> {
    return tabs.value.filter { !it.tab.isIncognito }
}
