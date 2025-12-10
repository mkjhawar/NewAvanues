# AVA Overlay Module - ProGuard Rules
# Created: 2025-11-09
# Purpose: Prevent stripping of Overlay service and UI classes

# Keep Overlay classes
-keep class com.augmentalis.ava.features.overlay.** { *; }

# Keep service
-keep class com.augmentalis.ava.features.overlay.service.OverlayService { *; }
-keep class com.augmentalis.ava.features.overlay.service.OverlayPermissionActivity { *; }

# Keep overlay controller
-keep class com.augmentalis.ava.features.overlay.controller.OverlayController { *; }

# Keep integration classes
-keep class com.augmentalis.ava.features.overlay.integration.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
