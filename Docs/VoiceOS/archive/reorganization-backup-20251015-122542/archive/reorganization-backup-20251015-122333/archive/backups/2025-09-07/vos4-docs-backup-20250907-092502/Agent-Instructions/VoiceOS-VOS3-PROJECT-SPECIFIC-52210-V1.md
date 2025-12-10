# VOS3 Project-Specific Instructions
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA

## VOS3 Database Requirements (ObjectBox)
**MANDATORY:** All VOS3 modules MUST use ObjectBox for local data persistence.

### ObjectBox Configuration for VOS3
```kotlin
// build.gradle.kts
plugins {
    id("io.objectbox") version "3.6.0"
}

dependencies {
    implementation("io.objectbox:objectbox-android:3.6.0")
    implementation("io.objectbox:objectbox-kotlin:3.6.0")
}
```

## VOS3 Data Module Specific Requirements
- NO streaming needed (small data size <3MB typical)
- Validate gesture data JSON before storage
- Create automatic backup before retention cleanup
- Implement checksum verification for all imports
- Store complex nested data as compact JSON arrays in ObjectBox
- JSON format: Use arrays, short keys, no whitespace (e.g., {"k":[1,2,3],"v":"data"})
- Keep user-created content (gestures, sequences) permanently
- Retain top 50 most used commands during cleanup (user configurable 25-200)

## VOS3 Module Architecture Requirements
- All modules must implement IModule interface
- Use EventBus for inter-module communication
- Follow repository pattern for data access
- Memory target: <30MB with Vosk, <60MB with Vivoka
- Module load time target: <50ms per module

## VOS3 Voice Command Requirements
- Support 70+ built-in commands
- Multi-language support (9 languages minimum)
- Context-aware command execution
- Custom command registration support
- Command history tracking with learning

## VOS3 Accessibility Requirements
- Use device keyboard (not custom keyboard)
- Support all standard Android accessibility actions
- Element extraction time <100ms
- Duplicate resolution for similar UI elements
- Touch gesture learning and playback

## VOS3 Gesture Support Requirements
### Supported Gestures (up to 3 fingers)
- **Basic:** Tap, Double Tap, Long Press, Hold
- **Directional:** Swipe (8 directions - up, down, left, right, up-left, up-right, down-left, down-right)
- **Rotation:** Rotate with degrees or percentage (0-360°)
- **Pinch/Spread:** Grab gestures for zoom
- **Hold & Drag:** Hold then move
- **Flick:** Quick directional flicks
- **Shapes:** Circle (CW/CCW), Check mark, X mark, L-shape, Zigzag
- **Patterns:** Custom tap patterns with timing
- **Pressure:** Light/Medium/Hard (if supported)
- **Edge:** Swipes from screen edges
- **Corner:** Actions from corners
### Gesture Data Format
- Store as compact JSON arrays to minimize size
- Example: {"p":[[100,150,0,0.5,1],[120,170,50,0.5,1]],"a":"CLICK","v":2.5}
- Arrays: [x,y,time,pressure,fingers] for each point
- Use short keys: "p"=points, "a"=action, "v"=velocity, "z"=zones
- No whitespace, no pretty printing
- Support percentage-to-degree conversion for rotations

## VOS3 Recognition Requirements
- Dual engine support (Vosk free, Vivoka premium)
- Offline-first operation
- Wake word detection optional
- VAD (Voice Activity Detection) required
- Recognition latency <200ms local, <500ms cloud

## VOS3 Smart Glasses Requirements
- Support 8+ brands minimum
- Auto-detection via USB/Bluetooth
- Companion app integration where available
- Display adaptation for each device type
- Hot-plugging support

## VOS3 Licensing Requirements
- Free tier with Vosk engine
- 30-day trial for premium features
- Premium tier with Vivoka engine
- User configurable retention settings
- Encrypted export/import functionality

## VOS3 Backup Strategy
- Automatic daily backups at 3 AM (user-configurable)
- AOSP devices: Prompt before shutdown or end of shift
- Keep last 7 daily backups
- Location: /Android/data/com.augmentalis.voiceos/backups/
- Encrypted format (same as export)
- Size limit: Skip if >50MB
- User can disable/configure schedule
- Optional completion notification

## VOS3 Analytics & Error Reporting
- Track command success/failure reasons locally
- Performance metrics ON by default (for testing phase)
- Auto-enable metrics if error rate >10% (for production)
- Optional anonymous error reporting to developers
- User must consent to data transmission
- Send via email with anonymized data
- Include sanitized context and error details
- Optional device ID for user support (user choice)
- No personally identifiable information
- User can opt-out anytime
- Requires privacy policy agreement
- 7-day detailed logs, then aggregate data only
- Track user correction patterns for learning

## VOS3 Design System
- Follow Apple visionOS design language for spatial computing
- Use iOS system colors for consistency across platforms
- Glass materials with 70% opacity and 50px blur
- SF Pro font family for Apple platforms
- 20px corner radius for panels, 12px for buttons
- 44px minimum touch targets for accessibility
- See VOS3-DESIGN-SYSTEM.md for complete specifications

## VOS3 Data Decoder Tool
- Flutter-based cross-platform tool (Windows, macOS, Linux, iOS, Android)
- visionOS-inspired glass UI with iOS system colors
- AES-256 encryption/decryption for VOS3 backup files
- JSON validation and data integrity checks
- Edit and export capabilities for troubleshooting
- Developer key separate from production app
- Maintains audit log of all decode operations