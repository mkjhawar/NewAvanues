/**
 * AvanuesDataStore.kt - Single DataStore instance for the app
 *
 * IMPORTANT: DataStore requires exactly ONE instance per file name.
 * All access to avanues_settings DataStore MUST go through this extension property.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.avanuesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "avanues_settings"
)
