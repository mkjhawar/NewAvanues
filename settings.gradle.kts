// Settings for NewAvanues-Cockpit multi-module project
rootProject.name = "NewAvanues-Cockpit"

// Plugin management
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Dependency resolution
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Include Android modules
include(":android:apps:cockpit-mvp")

// Include Common/Shared modules (KMP)
include(":Common:Cockpit")
include(":Common:UI")
include(":Common:Database")
include(":Common:Utils")
include(":Common:SpatialRendering")

// Note: ava and VoiceOS apps have their own build systems
