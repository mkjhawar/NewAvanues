# SpeechRecognition Library ProGuard Rules
# Author: Manoj Jhawar
# Created: 2025-01-27

# Keep all public API classes and methods
-keep public class com.augmentalis.speechrecognition.** {
    public protected *;
}

# Keep data classes
-keep class com.augmentalis.speechrecognition.models.** { *; }
-keep class com.augmentalis.speechrecognition.config.** { *; }
-keep class com.augmentalis.voiceos.speech.api.** { *; }

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# VOSK Speech Recognition
-keep class org.vosk.** { *; }
# JNA removed - Whisper deferred to backlog
# -keep class com.sun.jna.** { *; }
# -keepclassmembers class * extends com.sun.jna.** { public *; }

# Vivoka VSDK
-keep class com.vivoka.vsdk.** { *; }
-keepclassmembers class com.vivoka.vsdk.** { *; }
-dontwarn com.vivoka.vsdk.**

# Google Cloud Speech
-keep class com.google.cloud.speech.** { *; }
-keep class com.google.api.** { *; }
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# ObjectBox removed - migrated to SQLDelight
# -keep class io.objectbox.** { *; }
# -keep class * extends io.objectbox.annotation.Entity { *; }
# -keepclassmembers class * {
#     @io.objectbox.annotation.* <fields>;
# }

# JSON
-keep class org.json.** { *; }

# Native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}