# Add project specific ProGuard rules here.
# AVA AI - Standalone Android App

# Keep all model classes
-keep class com.augmentalis.ava.core.domain.model.** { *; }

# Keep ONNX Runtime
-keep class com.microsoft.onnxruntime.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Keep application components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service

# Keep ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
}

#################################################################################
# Added by Quick Fix Script - 2025-11-09
#################################################################################

# ==================== TVM Runtime (MLC-LLM) ====================
-keep class org.apache.tvm.** { *; }
-keep class ml.** { *; }
-keepclassmembers class * {
    native <methods>;
}

# Keep TVM module loading
-keep class org.apache.tvm.Module { *; }
-keep class org.apache.tvm.Function { *; }
-keep class org.apache.tvm.NDArray { *; }

# ==================== ONNX Runtime ====================
-keep class ai.onnxruntime.** { *; }
-keep class com.microsoft.onnxruntime.** { *; }

# Keep ONNX model loading
-keepclassmembers class * extends ai.onnxruntime.OrtSession {
    *;
}

# ==================== Apache POI (DOCX Parsing) ====================
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.etsi.**
-dontwarn org.openxmlformats.**
-dontwarn com.microsoft.schemas.**

# ==================== PDF Box Android (PDF Parsing) ====================
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-keep class org.apache.fontbox.** { *; }
-dontwarn org.apache.fontbox.**

# ==================== JSoup (HTML Parsing) ====================
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ==================== OkHttp (LLM Module Networking) ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ==================== Security Crypto ====================
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class * extends androidx.security.crypto.EncryptedSharedPreferences {
    *;
}

# ==================== Retrofit (if used) ====================
-keepattributes Signature
-keepattributes Annotation
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ==================== Kotlin Coroutines ====================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    *;
}

# ==================== Kotlin Serialization ====================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Serializer implementations
-keep,includedescriptorclasses class com.augmentalis.ava.**$$serializer { *; }
-keepclassmembers class com.augmentalis.ava.** {
    *** Companion;
}
-keepclasseswithmembers class com.augmentalis.ava.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==================== Room Database ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ==================== Native Methods ====================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ==================== Reflection (used by some parsers) ====================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ==================== Firebase Crashlytics ====================
# Keep source file names and line numbers for readable stack traces
-keepattributes SourceFile,LineNumberTable

# Keep exception classes for crash reporting
-keep public class * extends java.lang.Exception

# Keep Firebase Crashlytics SDK
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Keep Firebase Analytics (bundled with Crashlytics)
-keep class com.google.android.gms.measurement.** { *; }
-dontwarn com.google.android.gms.measurement.**

# Keep CrashReporter for stack traces
-keep class com.augmentalis.ava.crashreporting.** { *; }

# Keep custom crash keys and breadcrumbs
-keepattributes *Annotation*

# NDK crash reporting (for native libraries)
-keep class com.google.firebase.crashlytics.ndk.** { *; }
-dontwarn com.google.firebase.crashlytics.ndk.**

#################################################################################
# End of Quick Fix ProGuard Rules
#################################################################################
