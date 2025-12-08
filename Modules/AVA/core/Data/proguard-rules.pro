# AVA Core Data Module - ProGuard Rules
# Created: 2025-11-09
# Purpose: Prevent aggressive code stripping in release builds

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}

# Keep entities and DAOs
-keep class com.augmentalis.ava.core.data.entity.** { *; }
-keep interface com.augmentalis.ava.core.data.dao.** { *; }

# Keep database class
-keep class com.augmentalis.ava.core.data.AVADatabase { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin Reflect
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
