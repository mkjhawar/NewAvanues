// WebAvanue - Cross-Platform Browser
// Kotlin Multiplatform Project

plugins {
    kotlin("multiplatform") version "2.1.0" apply false
    kotlin("plugin.serialization") version "2.1.0" apply false
    id("com.android.library") version "8.7.3" apply false
    id("org.jetbrains.compose") version "1.7.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("app.cash.sqldelight") version "2.0.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
