# VoiceOS ProGuard Rules

# Keep accessibility service
-keep public class com.augmentalis.voiceos.core.VoiceOSAccessibilityService {
    public <methods>;
}

# Keep all interfaces (for SOLID principles)
-keep interface com.augmentalis.voiceos.core.interfaces.** { *; }

# Keep command actions (loaded dynamically)
-keep class com.augmentalis.voiceos.commands.actions.** { *; }
-keep class com.augmentalis.voiceos.core.CommandAction { *; }

# Keep data classes
-keep class com.augmentalis.voiceos.recognition.RecognizedCommand { *; }
-keep class com.augmentalis.voiceos.core.CommandResult { *; }
-keep class com.augmentalis.voiceos.core.CommandContext { *; }
-keep class com.vos.audio.VADResult { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Vosk library
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-keepattributes *Annotation*

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimize for size
-optimizationpasses 5
-dontpreverify
-repackageclasses ''
-allowaccessmodification

# End of File