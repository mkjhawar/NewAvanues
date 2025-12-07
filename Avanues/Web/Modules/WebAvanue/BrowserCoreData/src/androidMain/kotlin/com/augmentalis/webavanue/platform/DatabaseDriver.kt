package com.augmentalis.webavanue.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.webavanue.data.db.BrowserDatabase

/**
 * Creates an Android-specific SQLDelight driver for the browser database.
 *
 * @param context The Android application context
 * @return A SqlDriver configured for Android
 */
fun createAndroidDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = BrowserDatabase.Schema,
        context = context,
        name = "browser.db"
    )
}
