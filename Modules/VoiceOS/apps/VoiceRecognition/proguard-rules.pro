# Add project specific ProGuard rules here.
# Keep speech recognition classes
-keep class com.augmentalis.speechrecognition.** { *; }
-keep class com.augmentalis.voicerecognition.** { *; }

# Keep Vivoka classes
-keep class com.vivoka.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }