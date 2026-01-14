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

// AVAMagic Modules
include(":Modules:AVAMagic:MagicTools:LanguageServer")  // AVA Language Server (LSP)

// AVAMagic Language & Runtime (at AVAMagic level)
include(":Modules:AVAMagic:AVACode")                    // Kotlin Builder Functions (DSL)
include(":Modules:AVAMagic:AVURuntime")                 // AVU file parsing/processing

// AVAUI Core & Base Types
include(":Modules:AVAMagic:AVAUI:Core")                 // Base types, interfaces
include(":Modules:AVAMagic:AVAUI:CoreTypes")            // Shared type definitions
include(":Modules:AVAMagic:AVAUI:Foundation")           // Foundation components
include(":Modules:AVAMagic:AVAUI:Theme")                // Theme system
include(":Modules:AVAMagic:AVAUI:ThemeBridge")          // Theme conversion utilities
include(":Modules:AVAMagic:AVAUI:DesignSystem")         // Design tokens
include(":Modules:AVAMagic:AVAUI:StateManagement")      // State management
include(":Modules:AVAMagic:AVAUI:UIConvertor")          // UI conversion utilities

// AVAUI Component Modules (DTOs)
include(":Modules:AVAMagic:AVAUI:Input")                // Input components (Slider, DatePicker, etc.)
include(":Modules:AVAMagic:AVAUI:Display")              // Display components (Badge, Avatar, etc.)
include(":Modules:AVAMagic:AVAUI:Feedback")             // Feedback components (Alert, Toast, etc.)
include(":Modules:AVAMagic:AVAUI:Layout")               // Layout components (Grid, Stack, etc.)
include(":Modules:AVAMagic:AVAUI:Navigation")           // Navigation components (AppBar, Tabs, etc.)
include(":Modules:AVAMagic:AVAUI:Floating")             // Floating components (CommandBar)
include(":Modules:AVAMagic:AVAUI:Data")                 // Data components (Accordion, Carousel, etc.)

// AVAUI Infrastructure
include(":Modules:AVAMagic:AVAUI:Adapters")             // Platform adapters
include(":Modules:AVAMagic:AVAUI:TemplateLibrary")      // Component templates
include(":Modules:AVAMagic:AVAUI:ThemeBuilder")         // Theme builder tools
include(":Modules:AVAMagic:AVAUI:VoiceCommandRouter")   // Voice command routing
include(":Modules:AVAMagic:AVAUI:IPCConnector")         // IPC connection layer
include(":Modules:AVAMagic:AVAUI:ARGScanner")           // ARG scanning utilities
include(":Modules:AVAMagic:AVAUI:AssetManager")         // Asset management

// AVAUI Platform Renderers
include(":Modules:AVAMagic:AVAUI:Renderers:Android")    // Android Compose renderer
include(":Modules:AVAMagic:AVAUI:Renderers:iOS")        // iOS SwiftUI renderer (45 views)
include(":Modules:AVAMagic:AVAUI:Renderers:Desktop")    // Desktop Compose renderer
include(":Modules:AVAMagic:AVAUI:Renderers:Web")        // Web React renderer

// Android Apps
include(":android:apps:webavanue")
include(":android:apps:voiceoscoreng")  // VoiceOSCoreNG test app