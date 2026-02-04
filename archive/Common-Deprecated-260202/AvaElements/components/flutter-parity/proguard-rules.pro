# Flutter Parity Components - ProGuard/R8 Optimization Rules
# Version: 1.0.0
# Target: APK size reduction <500 KB, maintaining 100% functionality

##########################
# KOTLIN SERIALIZATION
##########################
# Keep serialized classes and their fields
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all serializable data classes
-keep @kotlinx.serialization.Serializable class com.augmentalis.avaelements.flutter.** {
    *;
}

##########################
# COMPOSE OPTIMIZATION
##########################
# Keep Compose runtime classes for proper rendering
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# Inline composable functions - allows aggressive inlining
-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    void sourceInformation(androidx.compose.runtime.Composer, java.lang.String);
    void sourceInformationMarkerStart(androidx.compose.runtime.Composer, int, java.lang.String);
    void sourceInformationMarkerEnd(androidx.compose.runtime.Composer);
}

##########################
# ANIMATION OPTIMIZATION
##########################
# Keep animation controller classes (23 animation components)
-keep class com.augmentalis.avaelements.flutter.animation.** { *; }
-keep class com.augmentalis.avaelements.flutter.animation.transitions.** { *; }

# Optimize animation curves - can be inlined
-assumenosideeffects class com.augmentalis.avaelements.flutter.animation.Curve {
    *** Linear;
    *** EaseIn;
    *** EaseOut;
}

##########################
# SCROLLING OPTIMIZATION
##########################
# Keep scrolling components (ListView, GridView, PageView)
-keep class com.augmentalis.avaelements.flutter.layout.scrolling.** { *; }

# Allow aggressive optimization of scroll physics
-assumenosideeffects class com.augmentalis.avaelements.flutter.layout.scrolling.ScrollPhysics {
    *** Platform;
}

##########################
# LAYOUT OPTIMIZATION
##########################
# Keep layout components but allow optimization
-keep class com.augmentalis.avaelements.flutter.layout.** { *; }

# Optimize spacing calculations - can be inlined
-assumenosideeffects class com.augmentalis.avaelements.core.types.Spacing {
    *** all(...);
    *** symmetric(...);
}

##########################
# MATERIAL COMPONENTS
##########################
# Keep Material Design components
-keep class com.augmentalis.avaelements.flutter.material.** { *; }
-keep class com.augmentalis.avaelements.flutter.material.chips.** { *; }
-keep class com.augmentalis.avaelements.flutter.material.lists.** { *; }
-keep class com.augmentalis.avaelements.flutter.material.advanced.** { *; }

##########################
# CODE SHRINKING
##########################
# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Remove debug assertions
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
}

##########################
# OPTIMIZATION SETTINGS
##########################
# Enable full optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Allow aggressive optimization
-allowaccessmodification
-repackageclasses ''

# Optimize method calls
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

##########################
# RESOURCE SHRINKING
##########################
# Keep resources referenced from code
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Shrink unused resources
-dontwarn android.graphics.drawable.**
-dontwarn androidx.compose.ui.graphics.**

##########################
# PERFORMANCE TARGETS
##########################
# Target APK size increase: <500 KB
# Target test coverage: 90%+
# Target scroll performance: 60 FPS @ 100K items
# Target animation performance: 60 FPS for all 23 components
# Target memory usage: <100 MB for large lists

##########################
# WARNINGS SUPPRESSION
##########################
-dontwarn kotlinx.serialization.**
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
