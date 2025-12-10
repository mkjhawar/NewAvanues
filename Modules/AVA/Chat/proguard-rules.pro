# AVA Chat Module - ProGuard Rules
# Created: 2025-11-09
# Purpose: Prevent stripping of Chat UI and ViewModel classes

# Keep Chat classes
-keep class com.augmentalis.ava.features.chat.** { *; }

# Keep ViewModels
-keep class com.augmentalis.ava.features.chat.ui.ChatViewModel { *; }

# Keep built-in intents
-keep class com.augmentalis.ava.features.chat.data.BuiltInIntents { *; }
-keep class com.augmentalis.ava.features.chat.data.IntentTemplates { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
