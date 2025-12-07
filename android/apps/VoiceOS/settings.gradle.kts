pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        // vivoka (if you need it at the plugin level)
        maven("https://repo1.maven.org/maven2/")
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.objectbox") {
                // Use the real artifact from Maven Central - Updated per analysis
                useModule("io.objectbox:objectbox-gradle-plugin:4.3.1")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // mavenLocal first but exclude JNA (it has corrupted metadata in some local repos)
        mavenLocal {
            content {
                // Exclude JNA from mavenLocal - it incorrectly requests AAR instead of JAR
                excludeGroup("net.java.dev.jna")
            }
        }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://alphacephei.com/maven/") }
        flatDir {
            dirs("vivoka") // relative to project root
        }
        // Kotlin/Native compiler distribution (required for iOS targets)
        ivy("https://download.jetbrains.com/kotlin/native/builds") {
            name = "Kotlin Native"
            patternLayout {
                artifact("[revision]/[artifact]-[revision].[ext]")
            }
            metadataSources { artifact() }
            content { includeModule("org.jetbrains.kotlin", "kotlin-native-prebuilt") }
        }
    }
}

rootProject.name = "VoiceOS"

// Main application (test harness)
include(":app")

// Standalone Apps
include(":modules:apps:VoiceOSCore")  // RE-ENABLED for Agent Swarm migration
include(":modules:apps:VoiceUI")  // Voice UI with Magic components
include(":modules:apps:VoiceCursor")
include(":modules:apps:VoiceRecognition")  // Voice Recognition test app with VoiceCursor UI
include(":modules:apps:VoiceOSIPCTest")  // IPC test client (Phase 3f)

// System Managers
include(":modules:managers:CommandManager")  // RE-ENABLED: Agent Swarm Task 2.1 - CommandManager restoration
include(":modules:managers:VoiceDataManager")  // Migrated to SQLDelight (Phase 4 complete)
include(":modules:managers:LocalizationManager")
include(":modules:managers:LicenseManager")
include(":modules:managers:HUDManager")

// Shared Libraries
// MagicUI and MagicElements moved to /Coding/magicui-deprecated (2025-10-23)
include(":modules:libraries:VoiceUIElements")
include(":modules:libraries:UUIDCreator")
include(":modules:libraries:DeviceManager")  // Device management library
include(":modules:libraries:SpeechRecognition")  // Unified speech recognition module (LearningSystem stubbed)
// include(":modules:libraries:VoiceKeyboard")  // DISABLED: Depends on VoiceDataManager (Phase 4)
include(":modules:libraries:VoiceOsLogging")  // Timber-based logging with custom Trees
include(":modules:libraries:PluginSystem")  // MagicCode plugin infrastructure (KMP support)
include(":modules:libraries:UniversalIPC")  // Universal IPC Protocol encoder/decoder

// Kotlin Multiplatform Libraries (Extracted from VoiceOSCore)
include(":libraries:core:result")         // VoiceOSResult monad - Type-safe error handling
include(":libraries:core:hash")           // HashUtils - SHA-256 hashing utilities
include(":libraries:core:constants")      // VoiceOSConstants - Centralized configuration values
include(":libraries:core:validation")     // SqlEscapeUtils - Input validation and sanitization
include(":libraries:core:exceptions")     // VoiceOSException - Exception hierarchy
include(":libraries:core:command-models") // CommandModels - Command data structures
include(":libraries:core:accessibility-types") // AccessibilityTypes - Accessibility enums and states
include(":libraries:core:voiceos-logging")    // VoiceOS Logging - PII-safe logging infrastructure
include(":libraries:core:text-utils")         // Text manipulation and sanitization utilities
include(":libraries:core:json-utils")          // JSON manipulation utilities
include(":libraries:core:database")            // SQLDelight KMP database (VoiceDataManager migration)

// Code Import - Temporary modules for testing
// include(":CodeImport:SR6-Hybrid")  // DISABLED - Using unified :libraries:SpeechRecognition instead
// include(":CodeImport:VoiceOSAccessibility")  // DISABLED - Using apps:VoiceOSCore instead
include(":Vosk")  // Vosk model

// Test Modules
include(":tests:voiceoscore-unit-tests")  // Pure JVM unit tests for VoiceOSCore
// include(":tests:automated-tests")  // DISABLED: Depends on VoiceOSCore (Phase 1 Quick Fix)
