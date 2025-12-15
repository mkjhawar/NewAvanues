// Top-level build file for NewAvanues-Cockpit
// Multi-module project with Android apps and KMP shared libraries

plugins {
    // Android plugins
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false

    // Kotlin plugins
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("org.jetbrains.kotlin.multiplatform") version "1.9.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
