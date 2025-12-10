pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        // Vivoka (if you need it at the plugin level)
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
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://alphacephei.com/maven/") }
    }
}

rootProject.name = "VoiceOS"

// Main application
include(":app")

// Standalone Apps
include(":apps:VoiceUI")  // Voice UI with Magic components
include(":apps:VoiceCursor")
include(":apps:VoiceRecognition")  // Voice Recognition test app with VoiceCursor UI
include(":apps:VoiceAccessibility")  // Voice Accessibility Service

// System Managers
include(":managers:CommandManager")
include(":managers:VoiceDataManager")
include(":managers:LocalizationManager")
include(":managers:LicenseManager")
include(":managers:HUDManager")

// Shared Libraries
include(":libraries:VoiceUIElements")
include(":libraries:UUIDManager")
include(":libraries:DeviceManager")  // Device management library
include(":libraries:SpeechRecognition")  // Unified speech recognition module
include(":libraries:VoiceKeyboard")  // Voice-enabled keyboard (IME) library

// Code Import - Temporary modules for testing
// include(":CodeImport:SR6-Hybrid")  // DISABLED - Using unified :libraries:SpeechRecognition instead
// include(":CodeImport:VoiceOSAccessibility")  // DISABLED - Using apps:VoiceAccessibility instead
include(":Vosk")  // Vosk model