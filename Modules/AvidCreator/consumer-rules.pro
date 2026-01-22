# UUIDCreator Library Consumer ProGuard Rules

# Keep all public API classes
-keep public class com.ai.uuidcreator.** { *; }

# Keep all model classes
-keep class com.ai.uuidcreator.models.** { *; }

# Keep API interfaces
-keep interface com.ai.uuidcreator.api.** { *; }

# Preserve annotations
-keepattributes *Annotation*

# Keep Compose integration if used
-keep class com.ai.uuidcreator.integration.ComposeExtensions** { *; }