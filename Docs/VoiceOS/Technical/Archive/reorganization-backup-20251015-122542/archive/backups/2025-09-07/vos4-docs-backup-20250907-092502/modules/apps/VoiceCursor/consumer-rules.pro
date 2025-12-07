# VoiceCursor Consumer Rules

# Keep VoiceCursor public API
-keep public class com.augmentalis.voiceos.voicecursor.** { *; }

# Keep cursor configuration classes
-keep class com.augmentalis.voiceos.voicecursor.core.CursorTypes** { *; }
-keep class com.augmentalis.voiceos.voicecursor.core.CursorConfig** { *; }

# Keep service classes
-keep class com.augmentalis.voiceos.voicecursor.service.** { *; }

# Keep accessibility service integration
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# Preserve cursor view rendering
-keep class com.augmentalis.voiceos.voicecursor.view.** { *; }