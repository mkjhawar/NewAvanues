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
        // Azure Cognitive Services Speech SDK
        maven("https://csspeechstorage.blob.core.windows.net/maven/")
        // Vosk (AlphaCephei) Speech Recognition
        maven("https://alphacephei.com/maven/")
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
include(":Modules:AVA:memory")
include(":Modules:AVA:Overlay")
include(":Modules:AVA:Teach")
include(":Modules:AVA:WakeWord")

// Shared AI Modules (cross-app)
include(":Modules:LLM")   // Language model providers and on-device inference
include(":Modules:RAG")   // Retrieval-augmented generation pipeline
include(":Modules:NLU")   // Natural language understanding

// Top-level Modules (cross-platform KMP)
include(":Modules:AVID")                  // Avanues Voice ID - unified identifier system
include(":Modules:Database")              // Unified KMP database - VoiceOS, WebAvanue, AVID persistence
// include(":Modules:VUID")               // DEPRECATED - replaced by AVID module

// VoiceOSCoreNG - Next-generation KMP core for VoiceOS
include(":Modules:VoiceOSCoreNG")          // KMP types, VUID, features (shared Android/iOS/Desktop)

// Shared Modules (cross-platform KMP libraries)
include(":Modules:Shared:NLU")
include(":Modules:Shared:Platform")
include(":Modules:Shared:LaasSDK")

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

// VoiceOS Core Module (moved from apps/ to module root)
include(":Modules:VoiceOS:VoiceOSCore")

// VoiceOS Apps (moved from Modules/VoiceOS/apps/ to android/apps/)
// LearnApp and LearnAppDev removed - functionality integrated into VoiceOSCore (2025-12-23)
include(":android:apps:VoiceRecognition")
include(":Modules:VoiceOS:apps:VoiceOS")  // Main launcher app with onboarding
include(":android:apps:VoiceCursor")
include(":android:apps:VoiceOSIPCTest")
include(":android:apps:VoiceUI")

// VoiceOS Managers
include(":Modules:VoiceOS:managers:LicenseManager")
include(":Modules:VoiceOS:managers:HUDManager")
include(":Modules:VoiceOS:managers:CommandManager")
include(":Modules:VoiceOS:managers:VoiceDataManager")
include(":Modules:VoiceOS:managers:LocalizationManager")

// VoiceOS Libraries
include(":Modules:VoiceOS:libraries:DeviceManager")
include(":Modules:VoiceOS:libraries:JITLearning")  // ⚠️ DEPRECATED (2026-01-06): Use VoiceOSCoreNG
include(":Modules:VoiceOS:libraries:LearnAppCore")  // ⚠️ DEPRECATED (2026-01-06): Use VoiceOSCoreNG
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

// UniversalRPC - Cross-platform gRPC module
include(":Modules:UniversalRPC")         // Root module with KMP + Wire

// AvaMagic Modules
include(":Modules:AvaMagic:MagicTools:LanguageServer")  // AVA Language Server (LSP)
include(":Modules:AvaMagic:AVACode")                    // Kotlin Builder Functions (DSL)
include(":Modules:AvaMagic:AVURuntime")                 // AVU file parsing/processing

// AvaUI Core & Base Types
include(":Modules:AvaMagic:AvaUI:Core")                 // Base types, interfaces
include(":Modules:AvaMagic:AvaUI:CoreTypes")            // Shared type definitions
include(":Modules:AvaMagic:AvaUI:Foundation")           // Foundation components
include(":Modules:AvaMagic:AvaUI:Theme")                // Theme system
include(":Modules:AvaMagic:AvaUI:ThemeBridge")          // Theme conversion utilities
include(":Modules:AvaMagic:AvaUI:DesignSystem")         // Design tokens
include(":Modules:AvaMagic:AvaUI:StateManagement")      // State management
include(":Modules:AvaMagic:AvaUI:UIConvertor")          // UI conversion utilities

// AvaUI Component Modules
include(":Modules:AvaMagic:AvaUI:Input")                // Input components
include(":Modules:AvaMagic:AvaUI:Display")              // Display components
include(":Modules:AvaMagic:AvaUI:Feedback")             // Feedback components
include(":Modules:AvaMagic:AvaUI:Layout")               // Layout components
include(":Modules:AvaMagic:AvaUI:Navigation")           // Navigation components
include(":Modules:AvaMagic:AvaUI:Floating")             // Floating components
include(":Modules:AvaMagic:AvaUI:Data")                 // Data components

// AvaUI Infrastructure
include(":Modules:AvaMagic:AvaUI:Adapters")             // Platform adapters
include(":Modules:AvaMagic:AvaUI:TemplateLibrary")      // Component templates
include(":Modules:AvaMagic:AvaUI:VoiceCommandRouter")   // Voice command routing
include(":Modules:AvaMagic:AvaUI:IPCConnector")         // IPC connection layer
include(":Modules:AvaMagic:AvaUI:ARGScanner")           // ARG scanning utilities
include(":Modules:AvaMagic:AvaUI:AssetManager")         // Asset management

// AvaUI Platform Renderers
include(":Modules:AvaMagic:AvaUI:Renderers:Android")    // Android Compose renderer

// Android Apps
include(":android:apps:webavanue")
include(":android:apps:voiceoscoreng")  // VoiceOSCoreNG test app