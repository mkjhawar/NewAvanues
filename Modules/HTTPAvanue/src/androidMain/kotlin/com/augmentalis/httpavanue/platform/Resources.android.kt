package com.augmentalis.httpavanue.platform

import android.content.res.AssetManager
import java.io.IOException

private var assetManager: AssetManager? = null

fun initializeResources(assets: AssetManager) { assetManager = assets }

actual fun readResource(path: String): String? {
    val assets = assetManager ?: return null
    return try { assets.open(path).bufferedReader().use { it.readText() } } catch (_: IOException) { null }
}
