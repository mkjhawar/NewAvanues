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

// Include the VoiceOSCoreNG library module from Modules
includeBuild("../../../Modules/VoiceOSCoreNG") {
    dependencySubstitution {
        substitute(module("com.augmentalis:voiceoscoreng")).using(project(":"))
    }
}
