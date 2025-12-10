# WebAvanue

WebAvanue is a voice-controlled browser feature module for the NewAvanues platform.

## Overview

WebAvanue enables hands-free browser control through voice commands, allowing users to:
- Navigate web pages (go back, forward, refresh)
- Manage tabs (new tab, close tab, switch tabs)
- Control zoom (zoom in, zoom out, reset zoom)
- Access browser features (bookmarks, downloads, history, settings)
- Use browser-specific functions

## Module Structure

```
WebAvanue/
├── ui/
│   ├── overlays/
│   │   └── HelpOverlay.kt      # Voice command discovery overlay
│   └── utils/                  # UI utilities
├── commands/                   # Browser command handlers
└── README.md
```

## Components

### HelpOverlay
The HelpOverlay component displays available voice commands organized by category:
- **Navigation**: Browser navigation commands
- **Tabs**: Tab management
- **Zoom**: Zoom controls
- **Features**: Bookmarks, history, downloads, settings
- **Mode**: Reader mode, dark mode
- **Scroll**: Page scrolling

### Responsive Design
The overlay adapts to device orientation:
- **Portrait**: Full-width layout with standard padding
- **Landscape**: 85% width, centered, reduced padding for optimal content visibility

## Platform Support

| Platform | Status |
|----------|--------|
| Android  | Active |
| iOS      | Planned |
| Desktop  | Planned |

## Integration

WebAvanue integrates with:
- VoiceOS for speech recognition
- Common/UI for MagicUI components
- Platform-specific browser engines

## Development

**Author**: WebAvanue Development Team
**Created**: 2025-12-10
**Status**: Active Development

## Related Modules

- VoiceOS: Core voice recognition system
- Common/UI: Shared UI components
- AvaConnect: Device connectivity
