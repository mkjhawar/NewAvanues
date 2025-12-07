# Build Fix: Dynamic Features Configuration Error

## Issue
- Date: 2025-01-28
- Module: VoiceAccessibility
- Error: "This application is not configured to use dynamic features"

## Root Cause
- App module attempted to depend on another app module (:apps:VoiceRecognition)
- Android build system interprets app-to-app dependencies as dynamic feature modules
- Dynamic features require special configuration not needed for this project

## Solution Applied
1. Removed app-to-app dependency from build.gradle.kts
2. Kept library dependency (:libraries:SpeechRecognition) for shared interfaces
3. Apps communicate via AIDL service binding (correct approach)

## Additional Fixes
- Added META-INF exclusions to packaging block to resolve duplicate file conflicts
- Exclusions: DEPENDENCIES, LICENSE files, INDEX.LIST, etc.

## Verification
- Build successful: ./gradlew :apps:VoiceAccessibility:assembleDebug
- APK generated at: /apps/VoiceAccessibility/build/outputs/apk/debug/

## Lessons Learned
- Apps cannot depend on other app modules in Android
- Use AIDL or Intent-based communication for app-to-app interaction
- Library modules can be shared between apps
- Packaging exclusions needed when multiple dependencies contain same META-INF files

## Technical Details

### Build Configuration Changes
```kotlin
// REMOVED: Direct app dependency (causes dynamic features error)
// implementation(project(":apps:VoiceRecognition"))

// KEPT: Library dependency for shared interfaces
implementation(project(":libraries:SpeechRecognition"))
```

### Packaging Exclusions Added
```kotlin
android {
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST"
            )
        }
    }
}
```

### Architecture Pattern
- **Inter-app Communication**: AIDL service binding
- **Shared Code**: Library modules
- **Module Dependencies**: Libraries can depend on libraries, apps can depend on libraries
- **Prohibited**: App-to-app dependencies (triggers dynamic features)

## References
- Android Dynamic Features Documentation: https://developer.android.com/guide/playcore/feature-delivery
- AIDL Service Binding: https://developer.android.com/develop/background-work/services/aidl