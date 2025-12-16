# AVA LLM Module - Consumer ProGuard Rules
# These rules are automatically applied to apps/modules that depend on this library
# Created: 2025-11-13
# Purpose: Prevent stripping of TVM Runtime and critical LLM dependencies in release builds

# ==================== TVM Runtime (Apache TVM / MLC-LLM) ====================
# Keep all TVM classes and native methods
-keep class org.apache.tvm.** { *; }
-keep class ml.** { *; }

# Keep all native methods (critical for JNI)
-keepclassmembers class * {
    native <methods>;
}

# Keep TVM core classes
-keep class org.apache.tvm.Module { *; }
-keep class org.apache.tvm.Function { *; }
-keep class org.apache.tvm.NDArray { *; }
-keep class org.apache.tvm.Device { *; }

# ==================== Kotlin Serialization ====================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep @kotlinx.serialization.Serializable class ** { *; }
-keep class kotlinx.serialization.** { *; }

-keepclassmembers class * {
    *** Companion;
}

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializer implementations for LLM models
-keep,includedescriptorclasses class com.augmentalis.ava.features.llm.**$$serializer { *; }
-keepclassmembers class com.augmentalis.ava.features.llm.** {
    *** Companion;
}
-keepclasseswithmembers class com.augmentalis.ava.features.llm.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==================== OkHttp (for API providers) ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ==================== Retrofit (if used for API clients) ====================
-keepattributes Signature
-keepattributes Annotation
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ==================== Keep LLM Module Classes ====================
# Keep all provider classes (Anthropic, OpenRouter, Local)
-keep class com.augmentalis.ava.features.llm.provider.** { *; }

# Keep ALC Engine and runtime
-keep class com.augmentalis.ava.features.llm.alc.** { *; }

# Keep model classes
-keep class com.augmentalis.ava.features.llm.model.** { *; }

# Keep response generator
-keep class com.augmentalis.ava.features.llm.response.** { *; }

# Keep security classes
-keep class com.augmentalis.ava.features.llm.security.** { *; }

# ==================== Coroutines ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ==================== Reflection (used by TVM) ====================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ==================== Enum Classes ====================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
