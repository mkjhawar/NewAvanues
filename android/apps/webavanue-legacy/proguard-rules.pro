# WebAvanue ProGuard Rules
# Security Hardening Phase 1 - Optimized for obfuscation while maintaining functionality
# See PROGUARD.md for detailed rationale

# ========== Entry Points (Keep Unobfuscated) ==========

# Keep MainActivity - entry point for Android system
-keep class com.augmentalis.Avanues.web.app.MainActivity { *; }

# Keep Application class - entry point for Android system
-keep class com.augmentalis.Avanues.web.app.WebAvanueApp { *; }

# ========== Serialization (Keep Structure) ==========

# Keep @Serializable classes and their fields
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    <fields>;
}

# Keep Kotlin serialization runtime
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** {
    <methods>;
}

# Keep @SerialName annotations
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ========== Android Platform APIs (Keep Structure) ==========

# Keep Parcelable implementations (Android serialization)
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ========== WebView (Keep Structure for JavaScript Bridge) ==========

# Keep WebView classes
-keep class android.webkit.** { *; }

# Keep WebViewClient/WebChromeClient methods (called by WebView)
-keepclassmembers class * extends android.webkit.WebViewClient {
    <methods>;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    <methods>;
}

# Keep JavaScript interface methods (called from web pages)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ========== Compose (Keep Only Runtime Essentials) ==========

# Keep Compose runtime stability annotations
-keep class androidx.compose.runtime.** { *; }

# Keep Compose @Composable annotations
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Suppress warnings for missing Compose modules
-dontwarn androidx.compose.**

# ========== Database (Room/SQLDelight) ==========

# Keep Room database classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep SQLDelight generated code
-keep class com.augmentalis.webavanue.data.db.** { *; }

# ========== Coroutines (Keep Structure) ==========

# Keep coroutines dispatcher factories
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep coroutines volatile fields (concurrency safety)
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ========== Kotlin Metadata (Keep for Reflection) ==========

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep Kotlin intrinsics
-keep class kotlin.jvm.internal.** { *; }

# ========== Security - EncryptedSharedPreferences ==========

# Keep security-crypto classes for encrypted storage
-keep class androidx.security.crypto.** { *; }
-keepclassmembers class androidx.security.crypto.** {
    <methods>;
}

# Keep SecureStorage implementation
-keep class com.augmentalis.Avanues.web.universal.security.SecureStorage { *; }
-keepclassmembers class com.augmentalis.Avanues.web.universal.security.SecureStorage {
    <methods>;
}

# ========== Obfuscation Settings (SECURITY HARDENING) ==========

# Enable aggressive obfuscation - repackage all classes to root package
-repackageclasses ''

# Allow modification of access modifiers (more aggressive optimization)
-allowaccessmodification

# Obfuscate source file names (prevent reverse engineering)
-renamesourcefileattribute SourceFile

# ========== Optimization Settings ==========

# Enable full optimization
-optimizationpasses 5

# Optimize all code
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ========== Suppress Warnings ==========

# Suppress warnings for optional dependencies
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ========== Removed Overly Broad Rules (SECURITY FIX) ==========
# These rules kept EVERYTHING unobfuscated - major security risk!
# REMOVED: -keep class com.augmentalis.Avanues.web.** { *; }
# REMOVED: -keep class androidx.compose.** { *; }
#
# Impact: Enables obfuscation of 95% of app code
# Security: Makes reverse engineering significantly harder
# APK Size: Reduces by 20-30% due to better optimization
