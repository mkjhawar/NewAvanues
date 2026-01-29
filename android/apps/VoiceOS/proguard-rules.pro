# VoiceOSCoreNG ProGuard Rules

# Keep VoiceOSCoreNG classes
-keep class com.augmentalis.voiceoscoreng.** { *; }

# Compose
-dontwarn androidx.compose.**

# Kotlin
-dontwarn kotlin.**
-keepclassmembers class kotlin.Metadata { *; }
