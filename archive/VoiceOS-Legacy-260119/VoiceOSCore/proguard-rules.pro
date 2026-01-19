# VoiceOSCore Proguard Rules
# Optimized for security, performance, and debugging
# Author: Manoj Jhawar
# Created: 2025-11-09
# Phase: 3 (Medium Priority)

# ========================================
# General Android Optimizations
# ========================================

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Rename source file to obfuscate while keeping stack traces
-renamesourcefileattribute SourceFile

# Remove logging in release builds (performance optimization)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep crash reporting info
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ========================================
# Kotlin Optimizations
# ========================================

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep companion objects
-keepclassmembers class * {
    public static ** Companion;
}

# Keep data class constructors and properties
-keepclassmembers class * {
    public <init>(...);
}

# ========================================
# VoiceOS Public APIs
# ========================================

# Keep all public APIs in VoiceOSCore package
-keep public class com.augmentalis.voiceoscore.** {
    public protected *;
}

# Keep AIDL interfaces (IPC communication)
-keep interface com.augmentalis.voiceoscore.aidl.** { *; }
-keep class * implements android.os.IInterface { *; }

# Keep ContentProvider implementations
-keep public class * extends android.content.ContentProvider {
    public *;
}

# Keep BroadcastReceiver implementations
-keep public class * extends android.content.BroadcastReceiver {
    public *;
}

# Keep Service implementations
-keep public class * extends android.app.Service {
    public *;
}

# ========================================
# Room Database
# ========================================

# Keep Room entities
-keep @androidx.room.Entity class * { *; }

# Keep Room DAOs
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Room Database classes
-keep class * extends androidx.room.RoomDatabase { *; }

# Keep Room migration classes
-keep class androidx.room.migration.Migration { *; }
-keep class * extends androidx.room.migration.Migration { *; }

# Keep Room type converters
-keep @androidx.room.TypeConverters class * { *; }
-keep class * {
    @androidx.room.TypeConverter *;
}

# ========================================
# Data Classes and Models
# ========================================

# Keep all data classes (used for serialization)
-keep class com.augmentalis.voiceoscore.data.** { *; }
-keep class com.augmentalis.voiceoscore.model.** { *; }

# Keep all database entity classes
-keep class com.augmentalis.voiceoscore.database.entity.** { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========================================
# Security & Privacy
# ========================================

# Keep encryption classes (DataEncryptionManager)
-keep class com.augmentalis.voiceoscore.security.DataEncryptionManager { *; }
-keep class com.augmentalis.voiceoscore.security.** { *; }

# Keep privacy consent classes (UserConsentManager)
-keep class com.augmentalis.voiceoscore.privacy.** { *; }

# Keep security validators
-keep class com.augmentalis.voiceoscore.security.ContentProviderSecurityValidator { *; }

# ========================================
# Analytics & Metrics
# ========================================

# Keep metrics classes (CommandMetricsCollector, ScrapingAnalytics)
-keep class com.augmentalis.voiceoscore.metrics.** { *; }
-keep class com.augmentalis.voiceoscore.analytics.** { *; }

# ========================================
# Utilities & Managers
# ========================================

# Keep utility classes that might use reflection
-keep class com.augmentalis.voiceoscore.utils.** { *; }

# Keep database backup/integrity classes
-keep class com.augmentalis.voiceoscore.database.MigrationRollbackManager { *; }
-keep class com.augmentalis.voiceoscore.utils.DatabaseBackupManager { *; }
-keep class com.augmentalis.voiceoscore.utils.DatabaseIntegrityChecker { *; }

# Keep command classes
-keep class com.augmentalis.voiceoscore.commands.** { *; }

# Keep manager classes
-keep class com.augmentalis.voiceoscore.managers.** { *; }

# ========================================
# Accessibility
# ========================================

# Keep AccessibilityService implementations
-keep public class * extends android.accessibilityservice.AccessibilityService {
    public *;
}

# Keep accessibility scraping classes
-keep class com.augmentalis.voiceoscore.accessibility.** { *; }

# ========================================
# Enums
# ========================================

# Keep all enum classes and their values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep enum ordinals for serialization
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========================================
# Native Methods
# ========================================

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ========================================
# View Classes
# ========================================

# Keep custom view constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep onClick methods
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# ========================================
# Gson/JSON Serialization (if used)
# ========================================

# Keep Gson-specific classes
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ========================================
# R8 Optimizations
# ========================================

# Enable aggressive optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Allow optimization
-allowaccessmodification

# Repackage classes into single package (reduces APK size)
-repackageclasses 'voiceos.obf'

# ========================================
# Warnings Suppression
# ========================================

# Suppress warnings about missing classes (optional dependencies)
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# ========================================
# Testing Classes (excluded from release)
# ========================================

# Don't obfuscate test classes (not included in release anyway)
-keep class **.*Test { *; }
-keep class **.*Tests { *; }
-keep class androidx.test.** { *; }
-keep class org.junit.** { *; }

# ========================================
# Debugging Helpers
# ========================================

# Keep class names for better debugging (remove in production if needed)
-keepnames class com.augmentalis.voiceoscore.** { *; }

# Keep method names that might be called via reflection
-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# ========================================
# Third-Party Libraries
# ========================================

# Add rules for specific third-party libraries here as needed
# Example:
# -keep class com.squareup.okhttp3.** { *; }
# -keep class retrofit2.** { *; }

# ========================================
# Custom Rules (Project-Specific)
# ========================================

# Keep VoiceOS command registration
-keep class * implements com.augmentalis.voiceoscore.commands.Command { *; }

# Keep VoiceOS plugin interfaces
-keep interface com.augmentalis.voiceoscore.plugins.** { *; }
-keep class * implements com.augmentalis.voiceoscore.plugins.** { *; }

# Keep constants
-keep class com.augmentalis.voiceoscore.utils.VoiceOSConstants { *; }
-keep class com.augmentalis.voiceoscore.utils.VoiceOSConstants$* { *; }

# ========================================
# End of Proguard Rules
# ========================================
