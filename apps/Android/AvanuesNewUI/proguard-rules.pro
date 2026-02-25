# AVA Unified ProGuard Rules

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class **$$serializer {
    *** INSTANCE;
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }

# Keep RPC message classes
-keep class com.augmentalis.voiceoscore.rpc.messages.** { *; }
-keep class com.augmentalis.webavanue.rpc.messages.** { *; }
-keep class com.augmentalis.voicecursor.core.** { *; }

# Keep accessibility service
-keep class com.augmentalis.avaunified.service.AvaAccessibilityService { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
