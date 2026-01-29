# ProGuard rules for DeviceManager module
# Author: Manoj Jhawar
# Purpose: Ensure proper functioning with R8/ProGuard minification

# Keep all public API classes
-keep public class com.augmentalis.devicemanager.** { public *; }

# Keep compatibility layer
-keep class com.augmentalis.devicemanager.compatibility.** { *; }

# Android XR Support (when available)
-dontwarn androidx.xr.**
-keep class androidx.xr.** { *; }

# UWB Support
-keep class androidx.core.uwb.** { *; }
-keepclassmembers class androidx.core.uwb.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }
-keep class android.hardware.biometrics.** { *; }

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keepnames class com.google.android.gms.** { *; }

# Camera2 API
-keep class androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** { *; }

# Window Manager for foldables
-keep class androidx.window.** { *; }
-keepclassmembers class androidx.window.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# DataStore
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.augmentalis.devicemanager.**$$serializer { *; }
-keepclassmembers class com.augmentalis.devicemanager.** {
    *** Companion;
}
-keepclasseswithmembers class com.augmentalis.devicemanager.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Reflection (for API compatibility)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable

# Native methods
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# Enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keepclassmembers class androidx.compose.ui.platform.ViewCompositionStrategy$DisposeOnViewTreeLifecycleDestroyed {
    public <init>();
}

# Multidex
-keep class androidx.multidex.** { *; }
-keep class com.android.multidex.** { *; }

# Prevent stripping of API compatibility methods
-keepclassmembers class * {
    @androidx.annotation.RequiresApi <methods>;
}

# Legacy support
-dontwarn androidx.legacy.**
-keep class androidx.legacy.** { *; }

# Location
-keep class android.location.** { *; }
-keep class com.google.android.gms.location.** { *; }

# Sensors
-keep class android.hardware.** { *; }
-keep interface android.hardware.** { *; }

# Vibrator
-keep class android.os.Vibrator { *; }
-keep class android.os.VibrationEffect { *; }
-keep class android.os.VibratorManager { *; }

# Connectivity
-keep class android.net.** { *; }
-keep class android.telephony.** { *; }

# Permission handling
-keep class android.content.pm.PackageManager { *; }
-keep class androidx.core.content.ContextCompat { *; }

# Keep extension functions
-keepclassmembers class com.augmentalis.devicemanager.compatibility.ApiCompatibilityKt { *; }
