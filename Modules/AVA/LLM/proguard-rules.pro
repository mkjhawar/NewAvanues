# AVA LLM Module - ProGuard Rules
# Created: 2025-11-09
# Purpose: Prevent stripping of TVM Runtime and API client dependencies

# TVM Runtime (Apache TVM)
-keep class org.apache.tvm.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin Serialization
-keep @kotlinx.serialization.Serializable class ** { *; }
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp (for API providers)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Retrofit (if used for API clients)
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }

# Keep LLM provider classes
-keep class com.augmentalis.ava.features.llm.provider.** { *; }
-keep class com.augmentalis.ava.features.llm.alc.** { *; }

# Keep model classes
-keep class com.augmentalis.ava.features.llm.model.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
