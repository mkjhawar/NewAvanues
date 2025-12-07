# VoiceCursor Module Changelog

## [2.1.0] - 2025-01-28
### Fixed 🔧
- **Critical Bug Fix**: Resolved X=0,Y=0 coordinate stuck issue in CursorAdapter
  - Fixed mathematical scaling bug that reduced movement to ~2 pixels
  - Changed from absolute to delta-based orientation processing
  - Implemented proper tangent-based displacement calculation
  - Fixed initialization to start cursor at screen center (960,540) instead of (0,0)

### Added ✨
- Comprehensive debug logging system for cursor positioning
- Auto-recalibration mechanism for stuck cursor detection (5-second threshold)
- Dead zone filtering to prevent micro-movement jitter (0.001f threshold)
- Previous orientation tracking for frame-to-frame delta calculation
- Cursor state debugging method `getCursorState()`
- Force recalibration method `forceRecalibration()`

### Changed 🔄
- Movement calculation from linear to tangent-based (matching legacy system)
- Sensitivity system to use separate X (2.0) and Y (3.0) factors
- Screen dimension handling to validate before use
- Initialization sequence to ensure proper cursor centering

### Testing 🧪
- Added 29 integration tests in CursorAdapterTest.kt
- Added 17 mathematical validation tests in CursorAdapterMathTest.kt
- 100% test success rate for all mathematical calculations

## [2.0.0] - 2025-01-26
### Changed 🔄
- Migrated to VoiceCursor with ARVision theme
- Integrated with DeviceManager IMU system
- Updated package structure to com.augmentalis.voiceos.cursor

## [1.1.0] - 2025-01-23
### Changed 🔄
- Updated package name to com.augmentalis.voiceos.cursor

## [1.0.0] - 2025-01-23
### Added ✨
- Initial port from VoiceOS legacy system
- Basic cursor view implementation
- Touch event handling

---

## Legend
- ✨ Added - New features
- 🔄 Changed - Changes in existing functionality
- 🔧 Fixed - Bug fixes
- 🗑️ Removed - Removed features
- 🧪 Testing - Test-related changes
- 📚 Documentation - Documentation changes