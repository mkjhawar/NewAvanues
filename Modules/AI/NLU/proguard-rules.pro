# AVA NLU Module - ProGuard Rules
# Created: 2025-11-09
# Purpose: Prevent stripping of ONNX Runtime and NLU model dependencies

# ONNX Runtime (MobileBERT)
-keep class ai.onnxruntime.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep NLU classes
-keep class com.augmentalis.ava.features.nlu.** { *; }

# Keep tokenizer and classifier
-keep class com.augmentalis.ava.features.nlu.IntentClassifier { *; }
-keep class com.augmentalis.ava.features.nlu.BertTokenizer { *; }
-keep class com.augmentalis.ava.features.nlu.ModelManager { *; }

# Keep data classes
-keep class com.augmentalis.ava.features.nlu.IntentClassification { *; }
-keep class com.augmentalis.ava.features.nlu.TokenizationResult { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin Reflect
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
