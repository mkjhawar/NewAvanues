# AVA Teach Module - ProGuard Rules
# Created: 2025-11-09
# Purpose: Prevent stripping of teaching interface classes

# Keep Teach classes
-keep class com.augmentalis.ava.features.teach.** { *; }

# Keep ViewModel
-keep class com.augmentalis.ava.features.teach.TeachAvaViewModel { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
