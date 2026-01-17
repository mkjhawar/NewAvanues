# VoiceOSCore ProGuard Rules
# Consolidated from Voice/Core and VoiceOS/VoiceOSCore

# Keep all classes in the voiceoscore package
-keep class com.augmentalis.voiceoscore.** { *; }

# Keep AIDL interfaces
-keep class com.augmentalis.voiceoscore.service.** { *; }

# SQLDelight
-keep class com.augmentalis.database.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Speech Recognition SDKs
-keep class org.vosk.** { *; }
-keep class com.microsoft.cognitiveservices.speech.** { *; }

# Vivoka SDK
-keep class com.vivoka.** { *; }
-keep class com.augmentalis.vivoka.** { *; }

# Don't warn about missing classes in optional dependencies
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
