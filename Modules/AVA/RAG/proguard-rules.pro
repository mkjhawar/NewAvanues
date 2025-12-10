# AVA RAG Module - ProGuard Rules
# Created: 2025-11-09
# Purpose: Prevent stripping of ONNX Runtime and document parsing dependencies

# ONNX Runtime
-keep class ai.onnxruntime.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# PDFBox (PDF parsing)
-keep class org.apache.pdfbox.** { *; }
-dontwarn org.apache.pdfbox.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.fontbox.**

# Apache POI (DOCX parsing)
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.**

# JSoup (HTML parsing)
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# Keep RAG classes
-keep class com.augmentalis.ava.features.rag.embeddings.** { *; }
-keep class com.augmentalis.ava.features.rag.parser.** { *; }
-keep class com.augmentalis.ava.features.rag.repository.** { *; }

# Room Database (RAG-specific entities)
-keep class com.augmentalis.ava.features.rag.data.entity.** { *; }
-keep interface com.augmentalis.ava.features.rag.data.dao.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
