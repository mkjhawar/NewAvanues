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
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

// AVA Core Modules
include(":Modules:AVA:core:Domain")
include(":Modules:AVA:core:Utils")
include(":Modules:AVA:core:Data")
include(":Modules:AVA:core:Theme")

// AVA Feature Modules
include(":Modules:AVA:Actions")
include(":Modules:AVA:Chat")
include(":Modules:AVA:LLM")
include(":Modules:AVA:memory")
include(":Modules:AVA:Overlay")
include(":Modules:AVA:RAG")
include(":Modules:AVA:Teach")
include(":Modules:AVA:WakeWord")

// Shared Modules (cross-platform KMP libraries)
include(":Modules:Shared:NLU")

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
// LearnApp and LearnAppDev removed - functionality integrated into VoiceOSCore (2025-12-23)
include(":Modules:VoiceOS:apps:VoiceRecognition")
include(":Modules:VoiceOS:apps:VoiceOSCore")
include(":Modules:VoiceOS:apps:VoiceOS")  // Main launcher app with onboarding
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
include(":Modules:VoiceOS:libraries:VivokaSDK")  // Vivoka VSDK wrapper module (AAR dependencies)
include(":Modules:VoiceOS:libraries:VoiceKeyboard")
include(":Modules:VoiceOS:libraries:VoiceOsLogging")
include(":Modules:VoiceOS:libraries:VoiceUIElements")

// WebAvanue Modules
include(":Modules:WebAvanue:coredata")   // Data layer: repositories, database, models
include(":Modules:WebAvanue:universal")  // UI layer: ViewModels, screens, platform code

// AVAMagic Modules
include(":Modules:AVAMagic:MagicTools:LanguageServer")  // MagicUI Language Server (LSP)

// Android App
include(":android:apps:webavanue")