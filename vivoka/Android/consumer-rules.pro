# Vivoka VSDK Consumer ProGuard Rules
# These rules are automatically applied to consuming modules

# Keep all Vivoka SDK classes
-keep class com.vivoka.** { *; }
-keep interface com.vivoka.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
