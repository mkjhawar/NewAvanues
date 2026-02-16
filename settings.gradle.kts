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
// include(":Modules:AVA:core:Theme")  // MOVED to :Modules:AvanueUI (2026-02-06)
include(":Modules:AvanueUI")              // Shared Compose UI: theme, design tokens, glassmorphic components

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
//include(":Modules:CommandManager")        // KMP Command management - registration, matching, execution
include(":Modules:Database")              // Unified KMP database - VoiceOS, WebAvanue, AVID persistence
// include(":Modules:VUID")               // DEPRECATED - replaced by AVID module

// Voice Modules (cross-platform KMP)
include(":Modules:VoiceOSCore")            // Unified KMP voice control (consolidates Voice:Core + VoiceOS)
// include(":Modules:Voice:Core")          // DEPRECATED - use VoiceOSCore (archived)
include(":Modules:Voice:WakeWord")         // Wake word detection
include(":Modules:VoiceIsolation")         // Audio preprocessing (noise suppression, echo cancellation, AGC)

// Actions Module (cross-platform KMP)
include(":Modules:Actions")                 // Intent handlers, action execution

// Foundation Module (cross-platform KMP utilities)
include(":Modules:Foundation")  // Common utilities: StateFlow, ViewModel, NumberToWords
project(":Modules:Foundation").projectDir = file("Modules/Foundation")
// VoiceAvanue - Unified Voice Control + Browser Module
include(":Modules:VoiceAvanue")  // Combined VoiceOSCore + WebAvanue with shared resources
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
// Modules/VoiceOS ARCHIVED (2026-01-29) - see archive/VoiceOS-Module-290129/
include(":android:apps:VoiceRecognition")
include(":android:apps:VoiceCursor")
include(":android:apps:VoiceOSIPCTest")
include(":android:apps:VoiceUI")

// VoiceOS Managers - ARCHIVED (2026-02-04)
// Consolidated into VoiceOSCore - see archive/VoiceOS-Module-260204/
// include(":Modules:VoiceOS:managers:HUDManager")
// include(":Modules:VoiceOS:managers:CommandManager")
// include(":Modules:VoiceOS:managers:VoiceDataManager")
// include(":Modules:VoiceOS:managers:LocalizationManager")  // DEPRECATED - use :Modules:Localization
include(":Modules:Localization")                             // KMP Localization module
include(":Modules:VoiceDataManager")                             // KMP Localization module

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
include(":Modules:AVU")                                  // KMP AVU format: codec (wire protocol) + DSL (language runtime)
// NOTE: JITLearning, LearnAppCore, VoiceOsLogging archived (deprecated/duplicate)

// WebAvanue Module - Unified KMP Browser Library
include(":Modules:WebAvanue")            // Merged flat structure: data + UI in single module

// KMP iOS Umbrella Module - Re-exports shared modules as single iOS framework via CocoaPods
include(":Modules:AvanuesShared")

// Cockpit — Multi-window display & session management (KMP)
include(":Modules:Cockpit")

// Avanue Content Modules — Standalone KMP viewers/editors used by Cockpit and other apps
include(":Modules:PDFAvanue")               // KMP PDF viewer (PdfRenderer + PDFKit + PDFBox)
include(":Modules:ImageAvanue")             // KMP image viewer with zoom/pan/gallery
include(":Modules:VideoAvanue")             // KMP video player (Media3 + AVPlayer)
include(":Modules:NoteAvanue")              // KMP rich notes with attachments + voice transcription
include(":Modules:CameraAvanue")            // KMP camera capture/preview (CameraX + AVCaptureSession)
include(":Modules:RemoteCast")              // KMP screen casting/sharing (MediaProjection + ReplayKit)
include(":Modules:AnnotationAvanue")        // KMP whiteboard/signature/drawing canvas

// Rpc - Cross-platform RPC module
include(":Modules:Rpc")                  // Root module with KMP + Wire

// AvanueUI Core & Base Types (promoted from AvaMagic/AvaUI — 2026-02-07)
include(":Modules:AvanueUI:Core")                       // Base types, interfaces
include(":Modules:AvanueUI:CoreTypes")                  // Shared type definitions
// include(":Modules:AvanueUI:Foundation")              // REMOVED - consolidated into AvanueUI root
include(":Modules:AvanueUI:Theme")                      // Theme system
// include(":Modules:AvanueUI:DesignSystem")            // REMOVED - consolidated into AvanueUI root
include(":Modules:AvanueUI:StateManagement")            // State management
// include(":Modules:AvanueUI:UIConvertor")             // DISABLED (2026-02-07) — references dead types (ColorRGBA, LegacyComponent)

// AvanueUI Component Modules
include(":Modules:AvanueUI:Input")                      // Input components
include(":Modules:AvanueUI:Display")                    // Display components
include(":Modules:AvanueUI:Feedback")                   // Feedback components
include(":Modules:AvanueUI:Layout")                     // Layout components
include(":Modules:AvanueUI:Navigation")                 // Navigation components
include(":Modules:AvanueUI:Floating")                   // Floating components
include(":Modules:AvanueUI:Data")                       // Data components
include(":Modules:AvanueUI:Voice")                      // Voice UI components

// AvanueUI Infrastructure
// include(":Modules:AvanueUI:Adapters")                // DISABLED (2026-02-07) — androidMain uses dead net.ideahq package, commonMain refs non-existent types
include(":Modules:AvanueUI:VoiceCommandRouter")         // Voice command routing
include(":Modules:AvanueUI:ARGScanner")                 // ARG scanning utilities
include(":Modules:AvanueUI:AssetManager")               // Asset management

// AvanueUI Platform Renderers
// include(":Modules:AvanueUI:Renderers:Android")       // DISABLED (2026-02-07) — unresolved component types cause cascading overload ambiguity in Render extensions
include(":Modules:AvanueUI:AvanueUIVoiceHandlers")      // Voice command handlers for AvanueUI components

// Promoted from AvaMagic to top-level (2026-02-07)
include(":Modules:AVACode")                             // Kotlin Builder Functions (DSL)
include(":Modules:IPC")                                 // AVU IPC Protocol (standby — gRPC via :Modules:Rpc preferred)
include(":Modules:AvanueUI:AvanueLanguageServer")        // AvanueUI Language Server (LSP for .magicui / .avp DSL files)
// include(":Modules:AvaMagic:AVURuntime")              // ARCHIVED (2026-02-07) — empty stub, no source code

// Cursor and Eye Tracking Modules (KMP)
include(":Modules:VoiceCursor")                    // KMP cursor control, dwell click, filtering
include(":Modules:Gaze")                           // KMP gaze/eye tracking with calibration

// Top-level Apps
include(":apps:avanues")                           // CONSOLIDATED app (VoiceAvanue + WebAvanue + Gaze + Cursor)

// Android Apps - Current
include(":android:apps:VoiceRecognition")          // Speech recognition testing
include(":android:apps:VoiceOSIPCTest")            // IPC testing

// Android Apps - Legacy (for comparison)
include(":apps:voiceavanue-legacy")                // Legacy VoiceAvanue app
include(":android:apps:webavanue-legacy")          // Legacy WebAvanue browser
// include(":android:apps:webavanue-ipc-legacy")   // DISABLED (2026-02-07) — legacy, missing domain layer
// include(":android:apps:ava-legacy")             // Old AVA app - archived
// include(":android:apps:browseravanue-legacy")   // Old browser - archived
// include(":android:apps:VoiceOS")                // ARCHIVED (2026-02-04)
