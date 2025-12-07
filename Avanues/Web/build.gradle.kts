// MainAvanues - Monorepo Root
// Contains all Avanue project modules

plugins {
    // Android
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false

    // Kotlin 2.0
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

    // Compose Multiplatform
    id("org.jetbrains.compose") version "1.7.0" apply false

    // SQLDelight
    id("app.cash.sqldelight") version "2.0.1" apply false
}

// Common versions for all modules
ext {
    set("kotlin_version", "2.0.21")
    set("compose_version", "1.7.0")
    set("sqldelight_version", "2.0.1")
}
