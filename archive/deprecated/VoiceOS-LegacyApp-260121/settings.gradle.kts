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

// Cockpit MVP Demo
include(":android:apps:cockpit-mvp")
include(":Common:Cockpit")

// VoiceOS Core Module (moved from apps/ to module root)
include(":Modules:VoiceOS:VoiceOSCore")  // RE-ENABLED for Agent Swarm migration

// Standalone Apps (Moved from Modules/VoiceOS/apps/ to android/apps/)
include(":android:apps:VoiceUI")  // Voice UI with Magic components
include(":android:apps:VoiceCursor")
include(":android:apps:VoiceRecognition")  // Voice Recognition test app with VoiceCursor UI
include(":android:apps:VoiceOSIPCTest")  // IPC test client (Phase 3f)

// System Managers (Moved to monorepo Modules/)
include(":Modules:VoiceOS:managers:CommandManager")  // RE-ENABLED: Agent Swarm Task 2.1 - CommandManager restoration
include(":Modules:VoiceOS:managers:VoiceDataManager")  // Migrated to SQLDelight (Phase 4 complete)
include(":Modules:VoiceOS:managers:LocalizationManager")
include(":Modules:LicenseManager")  // Moved from VoiceOS/managers
include(":Modules:VoiceOS:managers:HUDManager")

// Shared Libraries (Moved to monorepo Modules/)
// MagicUI and MagicElements moved to /Coding/magicui-deprecated (2025-10-23)
include(":Modules:AvaMagic:AvaUI:Voice")  // Voice UI components (moved from VoiceUIElements)
include(":Modules:AVID")  // Migrated from UUIDCreator
include(":Modules:DeviceManager")  // Device management library
include(":Modules:SpeechRecognition")  // Unified speech recognition module (LearningSystem stubbed)
// include(":Modules:VoiceOS:libraries:VoiceKeyboard")  // DISABLED: Depends on VoiceDataManager (Phase 4)
include(":Modules:VoiceOS:core:voiceos-logging")  // Use core logging module (VoiceOsLogging archived)
include(":Modules:PluginSystem")  // Moved from VoiceOS/libraries
include(":Modules:AvaMagic:IPC")  // Universal IPC Protocol (consolidated)

// Kotlin Multiplatform Libraries (Moved to monorepo Common/)
include(":Modules:VoiceOS:core:result")         // VoiceOSResult monad - Type-safe error handling
include(":Modules:VoiceOS:core:hash")           // HashUtils - SHA-256 hashing utilities
include(":Modules:VoiceOS:core:constants")      // VoiceOSConstants - Centralized configuration values
include(":Modules:VoiceOS:core:validation")     // SqlEscapeUtils - Input validation and sanitization
include(":Modules:VoiceOS:core:exceptions")     // VoiceOSException - Exception hierarchy
include(":Modules:VoiceOS:core:command-models") // CommandModels - Command data structures
include(":Modules:VoiceOS:core:accessibility-types") // AccessibilityTypes - Accessibility enums and states
include(":Modules:VoiceOS:core:voiceos-logging")    // VoiceOS Logging - PII-safe logging infrastructure
include(":Modules:VoiceOS:core:text-utils")         // Text manipulation and sanitization utilities
include(":Modules:VoiceOS:core:json-utils")          // JSON manipulation utilities
include(":Modules:VoiceOS:core:database")            // SQLDelight KMP database (VoiceDataManager migration)

// Code Import - Temporary modules for testing
// include(":CodeImport:SR6-Hybrid")  // DISABLED - Using unified :libraries:SpeechRecognition instead
// include(":CodeImport:VoiceOSAccessibility")  // DISABLED - Using apps:VoiceOSCore instead
include(":Common:ThirdParty:Vosk")  // Vosk model (Moved to monorepo Common/)

// Test Modules
include(":tests:voiceoscore-unit-tests")  // Pure JVM unit tests for VoiceOSCore
// include(":tests:automated-tests")  // DISABLED: Depends on VoiceOSCore (Phase 1 Quick Fix)

// Project directory mappings (monorepo structure)
project(":cockpit-mvp").projectDir = file("../cockpit-mvp")
project(":Common:Cockpit").projectDir = file("../../../Common/Cockpit")

project(":Modules:VoiceOS:VoiceOSCore").projectDir = file("../../../Modules/VoiceOS/VoiceOSCore")
project(":android:apps:VoiceUI").projectDir = file("../VoiceUI")
project(":android:apps:VoiceCursor").projectDir = file("../VoiceCursor")
project(":android:apps:VoiceRecognition").projectDir = file("../VoiceRecognition")
project(":android:apps:VoiceOSIPCTest").projectDir = file("../VoiceOSIPCTest")

project(":Modules:VoiceOS:managers:CommandManager").projectDir = file("../../../Modules/VoiceOS/managers/CommandManager")
project(":Modules:VoiceOS:managers:VoiceDataManager").projectDir = file("../../../Modules/VoiceOS/managers/VoiceDataManager")
project(":Modules:VoiceOS:managers:LocalizationManager").projectDir = file("../../../Modules/VoiceOS/managers/LocalizationManager")
project(":Modules:LicenseManager").projectDir = file("../../../Modules/LicenseManager")
project(":Modules:VoiceOS:managers:HUDManager").projectDir = file("../../../Modules/VoiceOS/managers/HUDManager")

project(":Modules:AvaMagic:AvaUI:Voice").projectDir = file("../../../Modules/AvaMagic/AvaUI/Voice")
project(":Modules:AVID").projectDir = file("../../../Modules/AVID")
project(":Modules:DeviceManager").projectDir = file("../../../Modules/DeviceManager")
project(":Modules:SpeechRecognition").projectDir = file("../../../Modules/SpeechRecognition")
project(":Modules:PluginSystem").projectDir = file("../../../Modules/PluginSystem")
project(":Modules:AvaMagic:IPC").projectDir = file("../../../Modules/AvaMagic/IPC")

project(":Modules:VoiceOS:core:result").projectDir = file("../../../Modules/VoiceOS/core/result")
project(":Modules:VoiceOS:core:hash").projectDir = file("../../../Modules/VoiceOS/core/hash")
project(":Modules:VoiceOS:core:constants").projectDir = file("../../../Modules/VoiceOS/core/constants")
project(":Modules:VoiceOS:core:validation").projectDir = file("../../../Modules/VoiceOS/core/validation")
project(":Modules:VoiceOS:core:exceptions").projectDir = file("../../../Modules/VoiceOS/core/exceptions")
project(":Modules:VoiceOS:core:command-models").projectDir = file("../../../Modules/VoiceOS/core/command-models")
project(":Modules:VoiceOS:core:accessibility-types").projectDir = file("../../../Modules/VoiceOS/core/accessibility-types")
project(":Modules:VoiceOS:core:voiceos-logging").projectDir = file("../../../Modules/VoiceOS/core/voiceos-logging")
project(":Modules:VoiceOS:core:text-utils").projectDir = file("../../../Modules/VoiceOS/core/text-utils")
project(":Modules:VoiceOS:core:json-utils").projectDir = file("../../../Modules/VoiceOS/core/json-utils")
project(":Modules:VoiceOS:core:database").projectDir = file("../../../Modules/VoiceOS/core/database")

project(":Common:ThirdParty:Vosk").projectDir = file("../../../Common/ThirdParty/Vosk")

project(":android:apps:cockpit-mvp").projectDir = file("../cockpit-mvp")
project(":Common:Cockpit").projectDir = file("../../../Common/Cockpit")
