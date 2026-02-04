# Flutter Parity Components - Consumer ProGuard Rules
# These rules are applied to consuming apps automatically

# Keep all public APIs
-keep public class com.augmentalis.avaelements.flutter.** {
    public *;
}

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keep @androidx.compose.runtime.Composable interface * { *; }

# Keep Modifier extension functions
-keepclassmembers class * {
    public static androidx.compose.ui.Modifier *(androidx.compose.ui.Modifier, ...);
}

# Keep animation data classes
-keepclassmembers class com.augmentalis.avaelements.flutter.animation.** {
    *;
}

# Keep layout components
-keepclassmembers class com.augmentalis.avaelements.flutter.layout.** {
    *;
}

# Keep Material components
-keepclassmembers class com.augmentalis.avaelements.flutter.material.** {
    *;
}

# Keep enums
-keepclassmembers enum com.augmentalis.avaelements.flutter.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
