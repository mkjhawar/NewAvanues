# VOS3 Data Decoder Tool
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA

## Overview
Cross-platform Flutter application for decoding, viewing, and editing VOS3 backup files. Uses visionOS design language with iOS system colors for consistency with the main VOS3 application.

## Features
- Decrypt AES-256 encrypted VOS3 backup files
- View and edit all data categories
- Validate JSON structure and data integrity
- Export selected data categories
- Search and filter capabilities
- Real-time validation

## Platforms
- Windows
- macOS  
- Linux
- iOS
- Android

## Design
Follows Apple visionOS design principles with glass materials and iOS system colors. See VOS3-DESIGN-SYSTEM.md for complete specifications.

## Security
- Uses separate developer key for decryption
- Maintains audit log of all operations
- No connection to production systems

## Development
```bash
# Run on desktop
flutter run -d macos
flutter run -d windows
flutter run -d linux

# Run on mobile
flutter run -d ios
flutter run -d android

# Build release
flutter build macos
flutter build windows
flutter build apk
```