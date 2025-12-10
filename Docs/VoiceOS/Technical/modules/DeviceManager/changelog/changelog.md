# DeviceManager Module Changelog

## [1.2.0] - 2025-01-28
### Fixed 🔧
- **CursorAdapter**: Fixed critical X=0,Y=0 coordinate bug
  - Removed harmful 0.1x movement multiplier
  - Changed from absolute to delta-based orientation processing
  - Implemented tangent-based displacement calculation
  - Fixed cursor initialization to start at screen center

### Added ✨
- **CursorAdapter**: Comprehensive debugging system
  - Added detailed logging for all transformation steps
  - Added `getCursorState()` method for debugging
  - Added `forceRecalibration()` method
  - Added stuck cursor detection (5-second threshold)
  - Added auto-recalibration mechanism

### Changed 🔄
- **CursorAdapter**: Improved mathematical model
  - Movement calculation from linear to tangent-based
  - Separate sensitivity factors for X (2.0) and Y (3.0)
  - Added previous orientation tracking
  - Added dead zone filtering (0.001f threshold)

### Testing 🧪
- Added comprehensive test coverage for CursorAdapter
- Added mathematical validation tests
- 100% test pass rate for cursor calculations

## [1.1.0] - 2025-01-26
### Added ✨
- CursorAdapter for IMU to screen coordinate transformation
- IMUMathUtils for quaternion and vector operations
- Integration with VoiceCursor module

## [1.0.0] - 2025-01-23
### Added ✨
- Initial DeviceManager implementation
- IMUManager for sensor data collection
- OrientationProvider interface
- Basic sensor fusion algorithms
- Flow-based data streaming

---

## Legend
- ✨ Added - New features
- 🔄 Changed - Changes in existing functionality
- 🔧 Fixed - Bug fixes
- 🗑️ Removed - Removed features
- 🧪 Testing - Test-related changes
- 📚 Documentation - Documentation changes