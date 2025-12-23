# Vivoka VSDK ProGuard Rules
# Keep all Vivoka SDK classes
-keep class com.vivoka.** { *; }
-keep interface com.vivoka.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
