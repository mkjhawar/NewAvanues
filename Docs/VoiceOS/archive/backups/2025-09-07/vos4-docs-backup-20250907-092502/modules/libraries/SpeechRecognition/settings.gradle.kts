/**
 * Settings for SpeechRecognition library module
 * This file is used when building the module standalone
 */

rootProject.name = "SpeechRecognition"

// Repository configuration
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
        maven { url = uri("https://alphacephei.com/maven/") } // For VOSK
    }
}