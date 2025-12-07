# UUIDCreator Library ProGuard Rules

# Standard Android library rules
-dontwarn com.ai.uuidcreator.**
-keep class com.ai.uuidcreator.** { *; }

# Keep model classes for serialization
-keep class com.ai.uuidcreator.models.** { 
    <fields>;
    <methods>;
}

# Preserve UUID generation methods
-keep class com.ai.uuidcreator.core.UUIDGenerator { *; }

# Keep public API
-keep public class com.ai.uuidcreator.UUIDCreator { 
    public *; 
}

# Keep interface methods
-keep class * implements com.ai.uuidcreator.api.IUUIDCreator { 
    public *; 
}