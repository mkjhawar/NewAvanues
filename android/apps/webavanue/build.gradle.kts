// WebAvanue - Monorepo Gradle Root
// KMP web application with Android/iOS/Desktop targets

plugins {
    // Android
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    // Kotlin 2.0
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Compose Multiplatform
    alias(libs.plugins.compose) apply false

    // SQLDelight
    alias(libs.plugins.sqldelight) apply false

    // Dokka - API Documentation
    alias(libs.plugins.dokka) apply false
}

// Configure Dokka for all subprojects
subprojects {
    apply(plugin = "org.jetbrains.dokka")
}
