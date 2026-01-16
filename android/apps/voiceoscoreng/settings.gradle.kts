pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VoiceOSCoreNGApp"

// Include the VoiceOSCoreNG library module for standalone builds
// This makes :Modules:Voice:Core resolve to the composite build
includeBuild("../../../Modules/VoiceOSCoreNG") {
    dependencySubstitution {
        // Substitute project reference used in build.gradle.kts
        substitute(project(":Modules:Voice:Core")).using(project(":"))
    }
}
