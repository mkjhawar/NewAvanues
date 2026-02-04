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

// AVA Feature Modules (Android UI wrappers)
include(":Modules:AVA:Overlay")

// AI Modules (cross-platform KMP)
include(":Modules:AI:NLU")      // Natural language understanding
include(":Modules:AI:RAG")      // Retrieval-augmented generation
include(":Modules:AI:LLM")      // Language model providers
include(":Modules:AI:Memory")   // AI memory system (short/long-term)
include(":Modules:AI:Chat")     // AI Chat interface
include(":Modules:AI:Teach")    // AI Teaching/training system
include(":Modules:AI:ALC")      // Adaptive LLM Coordinator (local inference)

// Top-level Modules (cross-platform KMP)
include(":Modules:AVID")                  // Avanues Voice ID - unified identifier system
include(":Modules:Database")              // Unified KMP database - VoiceOS, WebAvanue, AVID persistence
// include(":Modules:VUID")               // DEPRECATED - replaced by AVID module

// Voice Modules (cross-platform KMP)
include(":Modules:VoiceOSCore")            // Unified KMP voice control (consolidates Voice:Core + VoiceOS)
// include(":Modules:Voice:Core")          // DEPRECATED - use VoiceOSCore (archived)
include(":Modules:Voice:WakeWord")         // Wake word detection

// Actions Module (cross-platform KMP)
include(":Modules:Actions")                 // Intent handlers, action execution

// Shared Modules (cross-platform KMP libraries)
include(":Modules:Logging")         // Consolidated KMP logging infrastructure
include(":Modules:Utilities")       // Platform utilities (DeviceInfo, Logger, FileSystem, etc.)
include(":Modules:LicenseSDK")      // License validation client

// VoiceOS Core Modules - ALL ARCHIVED (2026-01-27)
// Consolidated into Modules:VoiceOSCore - see Archive/VoiceOS-CoreLibs-270127/
// include(":Modules:VoiceOS:core:database")       // ARCHIVED - see archive/legacy/VoiceOS-core-database
// include(":Modules:VoiceOS:core:command-models") // ARCHIVED (2026-01-27) - imports migrated to VoiceOSCore

// DEPRECATED (2026-01-21) - Functionality consolidated into VoiceOSCore
// These modules are redundant copies with old package names (com.augmentalis.voiceos.*)
// VoiceOSCore has the same code with correct package (com.augmentalis.voiceoscore)
// include(":Modules:VoiceOS:core:accessibility-types")
// include(":Modules:VoiceOS:core:result")
// include(":Modules:VoiceOS:core:voiceos-logging")
// include(":Modules:VoiceOS:core:hash")
// include(":Modules:VoiceOS:core:json-utils")
// include(":Modules:VoiceOS:core:constants")
// include(":Modules:VoiceOS:core:exceptions")
// include(":Modules:VoiceOS:core:text-utils")
// include(":Modules:VoiceOS:core:validation")

// VoiceOS Core Module - DEPRECATED (2026-01-21)
// Replaced by top-level :Modules:VoiceOSCore, old path archived
// include(":Modules:VoiceOS:VoiceOSCore")

// VoiceOS Apps
// LearnApp and LearnAppDev removed - functionality integrated into VoiceOSCore (2025-12-23)
// VoiceOS legacy app archived to archive/deprecated/VoiceOS-LegacyApp-260121/ (2026-01-21)
include(":android:apps:VoiceRecognition")
// include(":Modules:VoiceOS:apps:VoiceOS")  // ARCHIVED - use voiceoscoreng instead
include(":android:apps:VoiceCursor")
include(":android:apps:VoiceOSIPCTest")
include(":android:apps:VoiceUI")

// VoiceOS Managers (remaining in VoiceOS - VoiceOS-specific)
include(":Modules:VoiceOS:managers:HUDManager")
include(":Modules:VoiceOS:managers:CommandManager")
include(":Modules:VoiceOS:managers:VoiceDataManager")
// include(":Modules:VoiceOS:managers:LocalizationManager")  // DEPRECATED - use :Modules:Localization
include(":Modules:Localization")                             // KMP Localization module

// Top-level Shared Modules (consolidated from VoiceOS/libraries and AvaMagic)
include(":Modules:DeviceManager")                       // Device info, sensors, audio, network
include(":Modules:SpeechRecognition")                   // Speech recognition engines (Whisper, Vivoka, etc.)
// include(":Modules:Translation")                      // Translation services (stub only - archived)
include(":Modules:VoiceKeyboard")                       // Voice keyboard input
include(":vivoka:Android")                              // Vivoka VSDK wrapper (AAR dependencies)

// Relocated Modules (moved from VoiceOS/libraries and VoiceOS/managers to top-level)
include(":Modules:PluginSystem")                         // Generic DSL plugin framework (moved from VoiceOS/libraries)
include(":Modules:AvidCreator")                          // AVID Android extensions (moved from VoiceOS/libraries)
include(":Modules:LicenseManager")                       // License validation (moved from VoiceOS/managers)
include(":Modules:AVUCodec")                             // KMP AVU Protocol encoder/decoder
// NOTE: JITLearning, LearnAppCore, VoiceOsLogging archived (deprecated/duplicate)

// WebAvanue Module - Unified KMP Browser Library
include(":Modules:WebAvanue")            // Merged flat structure: data + UI in single module

// Rpc - Cross-platform RPC module
include(":Modules:Rpc")                  // Root module with KMP + Wire

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
include(":Modules:AvaMagic:AvaUI:Voice")                // Voice UI components

// AvaUI Infrastructure
include(":Modules:AvaMagic:AvaUI:Adapters")             // Platform adapters
include(":Modules:AvaMagic:AvaUI:TemplateLibrary")      // Component templates
include(":Modules:AvaMagic:AvaUI:VoiceCommandRouter")   // Voice command routing
// MIGRATED: IPC module moved to Modules/Rpc/src/.../rpc/ipc/ (2026-02-04)
// include(":Modules:AvaMagic:IPC")                         // Unified AVU IPC Protocol
include(":Modules:AvaMagic:AvaUI:ARGScanner")           // ARG scanning utilities
include(":Modules:AvaMagic:AvaUI:AssetManager")         // Asset management

// AvaUI Platform Renderers
include(":Modules:AvaMagic:AvaUI:Renderers:Android")    // Android Compose renderer
include(":Modules:AvaMagic:MagicVoiceHandlers")          // Voice command handlers for AVAMagic UI

// Android Apps
include(":android:apps:webavanue")
include(":android:apps:voiceoscoreng")  // VoiceOSCoreNG test app