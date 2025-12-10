# Add project specific ProGuard rules here.
# These rules will be applied to consuming modules.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve public API
-keep public class com.augmentalis.ava.features.overlay.** {
    public *;
}

# Keep service classes
-keep class * extends android.app.Service

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
