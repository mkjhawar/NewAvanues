package com.augmentalis.Avanues.web.universal.xr

import android.app.Activity
import android.content.Context
import androidx.lifecycle.LifecycleOwner

/**
 * Android-specific XR manager factory
 *
 * @param platformContext Must be an Activity that implements LifecycleOwner
 */
actual fun createXRManager(platformContext: Any): CommonXRManager {
    require(platformContext is Activity && platformContext is LifecycleOwner) {
        "platformContext must be an Activity that implements LifecycleOwner"
    }
    return AndroidXRManager(
        context = platformContext,
        lifecycle = platformContext.lifecycle
    )
}
