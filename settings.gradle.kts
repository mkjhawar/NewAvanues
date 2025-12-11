/*
 * NewAvanues-VoiceOS - Root Settings
 *
 * Defines the project structure for the VoiceOS monorepo.
 */

rootProject.name = "NewAvanues-VoiceOS"

// Enable Gradle version catalogs
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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

// VoiceOS Core Modules
include(":Modules:VoiceOS:core:database")
include(":Modules:VoiceOS:core:accessibility-types")
include(":Modules:VoiceOS:core:result")
include(":Modules:VoiceOS:core:voiceos-logging")
include(":Modules:VoiceOS:core:command-models")
include(":Modules:VoiceOS:core:hash")
include(":Modules:VoiceOS:core:json-utils")
include(":Modules:VoiceOS:core:constants")
include(":Modules:VoiceOS:core:exceptions")
include(":Modules:VoiceOS:core:text-utils")
include(":Modules:VoiceOS:core:validation")

// VoiceOS Apps
include(":Modules:VoiceOS:apps:LearnApp")  // Standalone app exploration tool
include(":Modules:VoiceOS:apps:VoiceRecognition")
include(":Modules:VoiceOS:apps:VoiceOSCore")
include(":Modules:VoiceOS:apps:VoiceCursor")
include(":Modules:VoiceOS:apps:VoiceOSIPCTest")
include(":Modules:VoiceOS:apps:VoiceUI")

// VoiceOS Managers
include(":Modules:VoiceOS:managers:LicenseManager")
include(":Modules:VoiceOS:managers:HUDManager")
include(":Modules:VoiceOS:managers:CommandManager")
include(":Modules:VoiceOS:managers:VoiceDataManager")
include(":Modules:VoiceOS:managers:LocalizationManager")

// VoiceOS Libraries
include(":Modules:VoiceOS:libraries:DeviceManager")
include(":Modules:VoiceOS:libraries:JITLearning")  // JIT learning service with AIDL interface
include(":Modules:VoiceOS:libraries:LearnAppCore")  // Shared business logic for JIT and LearnApp
include(":Modules:VoiceOS:libraries:PluginSystem")
include(":Modules:VoiceOS:libraries:SpeechRecognition")
include(":Modules:VoiceOS:libraries:Translation")
include(":Modules:VoiceOS:libraries:UniversalIPC")
include(":Modules:VoiceOS:libraries:UUIDCreator")
include(":Modules:VoiceOS:libraries:VoiceKeyboard")
include(":Modules:VoiceOS:libraries:VoiceOsLogging")
include(":Modules:VoiceOS:libraries:VoiceUIElements")
