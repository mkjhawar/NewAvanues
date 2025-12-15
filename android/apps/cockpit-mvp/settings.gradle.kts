pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.5.2" apply false
        id("com.android.library") version "8.5.2" apply false
        id("org.jetbrains.kotlin.android") version "1.9.25" apply false
        id("org.jetbrains.kotlin.multiplatform") version "1.9.25" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "cockpit-mvp"

include(":Common:Cockpit")

// Map to actual project directories
project(":Common:Cockpit").projectDir = file("../../../Common/Cockpit")

// Phase 2: Curved rendering components ready but HUDManager dependencies pending
// See: docs/issues/Cockpit-MVP-Phase2-Architecture-Ready-50912.md
// include(":Modules:VoiceOS:managers:HUDManager")
