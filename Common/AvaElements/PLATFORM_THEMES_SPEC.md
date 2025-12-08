# AvaElements Platform Themes Specification

**Comprehensive Multi-Platform UI Theme System for AvaElements**

Version: 1.0.0
Last Updated: 2025-10-29
Status: Research Complete - Implementation Pending

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Platform Overview](#platform-overview)
3. [iOS 26 - Liquid Glass](#ios-26---liquid-glass)
4. [macOS 26 Tahoe - Liquid Glass](#macos-26-tahoe---liquid-glass)
5. [visionOS - Spatial Glass](#visionos---spatial-glass)
6. [Windows 11 - Fluent Design 2](#windows-11---fluent-design-2)
7. [Android XR - Spatial Material](#android-xr---spatial-material)
8. [Samsung One UI 7](#samsung-one-ui-7)
9. [Material Design 3 Expressive](#material-design-3-expressive)
10. [Implementation Strategy](#implementation-strategy)
11. [Component Mapping Matrix](#component-mapping-matrix)

---

## Executive Summary

This specification defines the theme system architecture for AvaElements to support all major platform design languages in 2025. AvaElements will provide a unified API that generates platform-appropriate UI components with native themes.

### Supported Platforms (7)

| Platform | Design Language | Key Material | Release | Status |
|----------|----------------|--------------|---------|--------|
| iOS 26 | Liquid Glass | Translucent glass | June 2025 | ✅ Researched |
| macOS 26 | Liquid Glass | Translucent glass | June 2025 | ✅ Researched |
| visionOS 2 | Spatial Glass | 3D glass layers | 2024-2025 | ✅ Researched |
| Windows 11 | Fluent 2 | Mica/Acrylic/Smoke | Aug 2025 | ✅ Researched |
| Android XR | Spatial Material | Spatial panels | Oct 2025 | ✅ Researched |
| Android | Material 3 Expressive | Dynamic color | 2025 | ✅ Researched |
| Samsung | One UI 7 | Colored glass blur | 2025 | ✅ Researched |

---

## Platform Overview

### Design Language Evolution (2025)

The year 2025 marks a convergence toward **glass-based, translucent design systems** across all major platforms:

- **Apple Ecosystem**: Unified "Liquid Glass" across iOS, macOS, visionOS
- **Google Ecosystem**: Material 3 Expressive with dynamic color + Android XR spatial UI
- **Microsoft**: WinUI 3 now open source with Mica/Acrylic materials
- **Samsung**: One UI 7 with colored glass blur effects

### Common Themes Across Platforms

1. **Translucency & Blur**: All platforms use frosted glass effects
2. **Dynamic Materials**: Context-aware visual responses (wallpaper tinting, lighting)
3. **Spatial Depth**: Layering, elevation, Z-axis positioning
4. **Rounded Corners**: Soft, curved UI elements (circles, pills)
5. **Adaptive Layouts**: Responsive to content, context, and user interaction
6. **Accessibility First**: High contrast, dynamic type, assistive technologies

---

## iOS 26 - Liquid Glass

**Announced**: WWDC 2025 (June 9, 2025)
**Official**: [Apple Newsroom](https://www.apple.com/newsroom/2025/06/)

### Core Characteristics

**Material**: Translucent material that reflects and refracts surroundings while dynamically transforming to bring focus to content.

**Visual Identity**:
- Frosted glass with specular highlights
- Dynamic response to device movement
- Layered depth with multiple glass layers
- Subtle shimmer effects

### Key Components

#### Navigation & Controls

**Tab Bars**:
- Shrink when scrolling to focus on content
- Remain instantly accessible
- Glass material with translucency

**Toolbars**:
- No longer pinned to bezels
- Separated into contextual "bubbles"
- Appear/disappear based on context
- Example: Music app tab bar behavior

**Sidebars**:
- Updated glassy appearance
- Translucent backgrounds
- Smooth animations

#### Interface Elements

**Standard Components**:
- Text fields with glass backgrounds
- Sliders with translucent tracks
- Toggles with glass effects
- Alerts with frosted backgrounds
- Panels with layered glass
- Popovers with dynamic blur

**App Icons**:
- Layered system (multiple glass layers)
- Translucency effect
- Glass-like shimmer
- Reactive to device movement
- Specular highlights on layers

#### The Dock

- Crafted from multiple Liquid Glass layers
- Specular highlights
- Reacts to desktop wallpaper
- Can be customized with light/dark/colorful/clear appearances

### Design Resources

- **Figma UI Kit**: iOS 26 official design kit with updated:
  - Control sizes
  - Layouts and spacing
  - Corner radiuses
  - System colors
  - Glass materials

### Developer APIs

**SwiftUI Automatic Adoption**:
- Building with Xcode 26 automatically applies Liquid Glass
- No code changes required for basic adoption
- Updated APIs for custom glass materials

**UIKit Integration**:
- New Liquid Glass material APIs
- Updated controls with glass effects
- Observable object support with automatic invalidation

### Color System

- Supports light and dark modes
- Colorful tints available
- New "clear" appearance option
- Dynamic color based on wallpaper (similar to macOS)

---

## macOS 26 Tahoe - Liquid Glass

**Announced**: WWDC 2025
**Name**: macOS Tahoe

### Unified Design with iOS

macOS Tahoe 26 features the same **Liquid Glass** design language as iOS 26, creating visual harmony across Apple platforms while maintaining macOS's distinct characteristics.

### Desktop-Specific Features

**Desktop & Dock Customization**:
- Widgets can be placed on desktop
- App icons with light/dark appearances
- Colorful new tints
- Elegant "clear" look option
- Liquid Glass materials on desktop elements

**Window Management**:
- Navigation stacks with glass effects
- Tabs with translucent backgrounds
- Inspectors with frosted materials
- Toolbars with dynamic glass

### Framework Updates

#### SwiftUI

**Automatic Design Application**:
- Declarative nature enables automatic Liquid Glass adoption
- Navigation stacks: glassy, rounded, transparent
- Tabs: updated glass appearance
- Inspectors: frosted backgrounds
- Toolbars: contextual glass materials

**Performance**:
- Huge List performance improvements
- Optimized scrollable views
- Better memory management

#### AppKit Integration

**Improved SwiftUI + AppKit Interoperability**:
- Seamless integration between frameworks
- NSWindow supports SwiftUI modifiers:
  - `.toolbar` modifier
  - `.navigationTitle` modifier
- Automatic bridging when SwiftUI view is contentView

**Transferable Protocol**:
- NSImage conforms to Transferable
- NSColor conforms to Transferable
- NSSound conforms to Transferable
- Easy drag & drop in SwiftUI views
- Native sharing support

### Developer Experience

**Migration Path**:
- Mix of SwiftUI + AppKit recommended
- SwiftUI for hierarchical data
- AppKit for pixel-perfect control
- Shared resources via Transferable protocol

---

## visionOS - Spatial Glass

**Platform**: Apple Vision Pro
**Launched**: February 2024
**Updated**: visionOS 2.0 (2025)

### Spatial Design Paradigm

visionOS represents Apple's design language for **infinite 3D space**, where users engage with apps while staying connected to their surroundings.

### Core Material

**Glass Material**:
- System-designed "glass" material
- Adapts to lighting conditions
- Works in various real-world environments
- App windows rendered with glass
- Multiple layers for depth

### Key Components

#### App Windows

**3D Glass Layers**:
- Background layer
- Two foreground layers
- Automatic glass layer applied
- Adds depth, specular highlights, shadows

#### App Icons

**Layered Icons** (Up to 3 layers):
- Background layer (required)
- 2 foreground layers (optional)
- Automatic glass layer added
- Creates depth and shimmer effect
- Reacts to head movement

### Visual Design Guidelines

#### Typography

**Enhanced Readability**:
- Slightly heavier font weights vs other platforms
- Better contrast in AR environments
- System fonts recommended for legibility
- New wide, editorial-style font options

#### Glass Material Properties

**Visual Brightening**:
- System components brighten when looked at (gaze detection)
- Helps users understand interactive elements
- Provides clear affordance indicators

**Background Adaptation**:
- Glass adapts to user's environment
- Reflects and refracts real-world lighting
- Maintains app visibility across contexts

### Interaction Guidelines

#### Target Sizes

**Minimum Tap Targets**:
- Interactive elements: 60 points minimum
- Standard buttons: 44 points + 8pt spacing around
- Larger than 2D platforms due to AR constraints

#### Hover States

**Gaze-Based Interaction**:
- Hover states via eye tracking
- Visual feedback when looking at elements
- Critical for understanding affordances

### Spatial Positioning

**Optimal Viewing**:
- Primary content in 41° field of view
- Minimizes head movement
- Comfortable extended usage
- Apps launch 1.75m from user

### Design Resources

- **Figma Kit**: visionOS Design Resources (updated Jan 2025)
- Contains: UI components, views, system interfaces, text styles, color styles, materials

### Platform Integration

**iOS 26 Influence**:
- visionOS design inspiring iOS 26 updates
- Translucent menus
- Interface elements blend into background
- Unobtrusive look

---

## Windows 11 - Fluent Design 2

**Platform**: Windows 11
**Framework**: WinUI 3
**Status**: Open Source (announced August 2, 2025)

### Fluent Design System Components

Based on five key design principles:
1. **Light** - Illumination and emphasis
2. **Depth** - Layering and spatial relationships
3. **Motion** - Fluid animations and transitions
4. **Material** - Translucent surfaces
5. **Scale** - Responsive across devices

### Materials

#### Mica

**Opaque Material**:
- Takes tint from user's desktop wallpaper
- Creates connection between app and desktop
- Used for app background surfaces
- Introduced in Windows 11
- Dynamic tinting based on wallpaper colors

**Use Cases**:
- Main app windows
- Primary background surfaces
- Desktop-connected UI

#### Acrylic

**Translucent Material**:
- Frosted glass effect
- Used for transient surfaces
- Semi-transparent with blur
- Adapts to content behind

**Use Cases**:
- Context menus
- Flyouts
- Tooltips
- Prediction surfaces (search boxes)
- Temporary UI overlays

#### Smoke

**Modal Overlay Material**:
- Translucent black background
- Introduced with Windows 11
- Regardless of light/dark mode
- Creates hierarchy between layers

**Use Cases**:
- Modal dialogs
- Pop-up windows
- Creating visual separation
- Background dimming for focus

### WinUI 3 Components

**Available via WinUI Gallery**:
- Complete component showcase
- Interactive samples
- Code snippets
- Usage patterns
- All Fluent Design controls and styles

**Component Categories**:
- Navigation (NavigationView, Tabs)
- Input (TextBox, Button, CheckBox, RadioButton, Slider)
- Collections (ListView, GridView, TreeView)
- Text (TextBlock, RichTextBlock)
- Layout (Grid, StackPanel, RelativePanel)
- Dialogs & Flyouts
- Media (Image, MediaPlayer)
- Progress & Status

### Open Source Initiative (2025)

**Major Announcement** (August 2, 2025):
- WinUI 3 officially becoming open source
- Posted by Microsoft software engineer Beth Pan
- Detailed roadmap on GitHub
- Community contributions welcomed

**Implications**:
- Faster iteration cycles
- Community-driven enhancements
- Third-party component ecosystem
- Better cross-platform stories

### Developer Resources

- **WinUI Gallery**: GitHub microsoft/WinUI-Gallery
- **Documentation**: learn.microsoft.com/windows/apps/design/
- **Design Resources**: Figma kits, Adobe XD kits
- **Fluent UI System**: fluent2.microsoft.design

---

## Android XR - Spatial Material

**Platform**: Android XR (powered by Google)
**First Device**: Samsung Galaxy XR
**Announced**: October 2025
**Framework**: Jetpack XR SDK

### Design Philosophy

Android XR extends **Material Design 3** principles into spatial computing, creating familiar Android experiences in extended reality.

### Core Spatial Components

#### Spatial Panels

**Fundamental Building Blocks**:
- Containers for UI elements and interactive components
- Automatically adjust size based on distance from user
- Ensures legibility and interactivity at all distances
- Default: 32dp rounded corners

**Properties**:
- Adaptive sizing (distance-aware)
- Material Design styling
- Z-axis positioning (elevation)
- Comfortable field of view placement

#### Orbiters

**Floating UI Controls**:
- Persistent, floating UI elements
- Control content within spatial panels
- Allows content more screen space
- Quick access to features
- Hover outside main panel area

**Use Cases**:
- Media controls
- Tool palettes
- Navigation shortcuts
- Contextual actions

### Material Design for XR

**Enhanced Material 3**:
- M3 components adapted for spatial use
- Adaptive layouts (1:1 pane mapping)
- Spatial UI behaviors added
- Accessibility-first design

**Components with Spatial Enhancements**:
- Alert dialogs with elevation
- Top app bars → XR orbiters
- Navigation drawers → Spatial menus
- FABs → Floating spatial controls

### Visual Design

#### Target Sizes & Interactions

**Material Design Guidelines**:
- Optimal target size: 56dp
- Minimum interactive target: 48dp (accessibility)
- Hover states crucial for pointer inputs
- Focus states for non-pointer navigation

**Interaction States**:
- Default
- Hover (pointer/gaze)
- Focus (keyboard/controller)
- Pressed/Active
- Disabled

#### Spatial Positioning

**Comfort Zones**:
- **Primary Content**: 41° field of view (no head movement required)
- **Launch Distance**: 1.75 meters from user
  - Home Space: 1.75m
  - Full Space: 1.75m recommended

**Depth & Elevation**:
- **Spatial Elevation**: Components above panel on Z-axis
- Creates hierarchy
- Improves legibility
- Draws attention to important elements

### Color & Materials

**Material 3 Dynamic Color**:
- User wallpaper-based color schemes
- Light and dark themes
- Accessible contrast ratios
- Tonal palettes (5 palettes, 13 levels each)

**Spatial Materials**:
- Panel surfaces with depth
- Translucent overlays
- Elevated components with shadows
- Material Design elevation system

### Typography

**Scale & Legibility**:
- Follow Material Design type scale
- Account for viewing distance
- Ensure readability in AR
- Dynamic sizing based on panel distance

### Developer Resources

- **Official Docs**: developer.android.com/design/ui/xr
- **Jetpack XR SDK**: Material Design implementation
- **Design Guidelines**: Full spatial UI guides
- **Component Library**: Pre-built XR-adapted Material components

---

## Samsung One UI 7

**Platform**: Samsung Galaxy Devices
**Android Version**: Android 15 (One UI 7.0)
**Presented**: Galaxy S25 Series
**Status**: Beta testing (late 2024), Full release (2025)

### Design Philosophy

**Core Principles**:
1. **Simple**: Purposeful simplicity across UI
2. **Impactful**: New signature impressions
3. **Emotive**: Positive emotional responses through visual elements

**Tagline**: "A New Look, Evolved from Essence - Mobile Experiences Focused on the Fundamental"

### Visual Identity

#### Circle-Based Design

**Primary Shape**:
- Circle as foundational simple shape
- High scalability
- Soft and elegant curved outlines
- Appears as visual elements across screens
- Visually seamless, balanced design

**Component Styling**:
- Buttons with circular elements
- Menus with rounded corners
- Notifications with soft curves
- Control bars with pill shapes

### Materials & Effects

#### Colored Glass Blur

**Signature Effect**:
- Faint translucency
- Creates sense of depth
- Delicate texture as layers amass
- Colored glass over blur
- Adds screen sensibility
- Clearly conveys information nature

**Implementation**:
- New blur engine
- Layered translucency
- Color tinting on frosted glass
- Environmental adaptation

### Component Redesign

**Completely Overhauled**:
- Buttons: Circular, glass-like
- Menus: Rounded, translucent
- Notifications: Pill-shaped, colorful
- Control bars: Floating, glassy
- Widgets: Redesigned with colorful images, consistent layouts

**Visual Consistency**:
- Curves and circles throughout
- Consistent hierarchy across components
- Soft, elegant curved outlines
- Balanced visual design

### App Icons

**Fresh Icon System**:
- New visual metaphors
- Updated color schemes
- Easier app recognition
- Consistent styling
- More vibrant and expressive

### Information Architecture

**Organized & Concise**:
- Clarified hierarchy
- Consistent organization
- Information grouped logically
- Better content discoverability

### Color System

**Vibrant & Expressive**:
- More colorful than previous versions
- Widgets with vibrant imagery
- Consistent color application
- Harmonious palette
- Emotional color choices

### Typography

**Clear & Readable**:
- Samsung One typeface
- Optimized for screen legibility
- Multiple weights for hierarchy
- Consistent sizing scale

### Developer Resources

- **Design Guidelines**: developer.samsung.com/one-ui
- **Samsung Design System**: design.samsung.com
- **One UI Design Kit**: Figma community
- **Official PDF Guide**: One UI Design Guidelines (English)

---

## Material Design 3 Expressive

**Platform**: Android (All versions)
**Framework**: Jetpack Compose, Flutter, Web
**Status**: Current standard (2024-2025)
**Evolution**: M3 Expressive announced 2025

### Dynamic Color System

#### Core Concept

**User Personalization**:
- Generate color schemes from user's wallpaper
- Automatic light and dark themes
- Accessibility-first color selection
- 5 tonal palettes, 13 levels each = 65 total color attributes

#### Color Generation Process

**Source to Scheme**:
1. **Source**: User wallpaper or selected color
2. **Extraction**: Key color extraction algorithm
3. **Palette Generation**: 5 tonal palettes created:
   - Accent 1 (Primary)
   - Accent 2 (Secondary)
   - Accent 3 (Tertiary)
   - Neutral 1
   - Neutral 2
4. **Role Assignment**: Colors assigned to component roles
5. **Accessibility**: Contrast ratios validated

#### Color Roles

**Three High-Level Roles**:
1. **Primary**: Main brand color, key actions, highlights
2. **Secondary**: Supporting colors, less prominent elements
3. **Tertiary**: Accents, contrast, special cases

**Comprehensive Role System**:
- Primary, On-Primary, Primary Container, On-Primary Container
- Secondary, On-Secondary, Secondary Container, On-Secondary Container
- Tertiary, On-Tertiary, Tertiary Container, On-Tertiary Container
- Error, On-Error, Error Container, On-Error Container
- Background, On-Background, Surface, On-Surface
- Surface Variant, On-Surface Variant
- Outline, Outline Variant
- Scrim, Inverse Surface, Inverse On-Surface, Inverse Primary

### Material 3 Expressive (2025)

#### Research-Backed Evolution

**Unprecedented Research**:
- 46 research studies conducted
- 18,000+ participants worldwide
- Evidence-based design decisions
- Cultural and regional considerations

#### Key Updates

**Design System Expansion**:
- **Theming**: Enhanced customization options
- **Components**: New and updated components
- **Motion**: Refined animation principles
- **Typography**: Expanded type system
- **All designed for engaging, desirable products**

#### Expressive Characteristics

**Beyond Functional**:
- More personality in components
- Richer animations
- Greater customization depth
- Emotional resonance
- Brand expressiveness

### Component System

**M3 Component Categories**:

1. **Actions**: Buttons, FABs, Icon Buttons, Segmented Buttons
2. **Communication**: Badges, Progress Indicators, Snackbars
3. **Containment**: Cards, Carousels, Dividers, Lists
4. **Navigation**: Bottom Nav, Nav Drawer, Nav Rail, Tabs, Top App Bar
5. **Selection**: Checkboxes, Chips, Date Pickers, Menus, Radio Buttons, Sliders, Switches, Time Pickers
6. **Text Input**: Text Fields, Search Bars

**All Components**:
- Use dynamic color by default
- Support light/dark themes
- Meet accessibility standards
- Responsive layouts
- Adaptive sizing

### Accessibility

**Built-In Standards**:
- **Contrast**: All color combinations meet WCAG AA standards (4.5:1 minimum)
- **Tonal Palettes**: Designed for accessible combinations
- **Touch Targets**: Minimum 48dp
- **Dynamic Type**: Scalable text
- **Screen Readers**: Full TalkBack/VoiceOver support

### Developer Tools

#### Material Theme Builder

**Design Tool**:
- Figma plugin
- Online web tool
- Visual theme creation
- Export to code (Compose, Flutter, Web)
- Live preview

**Features**:
- Custom color input
- Automatic palette generation
- Accessibility checking
- Light/dark theme preview
- Export in multiple formats

#### Jetpack Compose Integration

**M3 in Compose**:
```kotlin
MaterialTheme(
    colorScheme = dynamicColorScheme(LocalContext.current),
    typography = Typography,
    shapes = Shapes
) {
    // App content
}
```

**Key APIs**:
- `MaterialTheme` - Apply M3 theme
- `dynamicColorScheme()` - Generate from wallpaper
- `lightColorScheme()` / `darkColorScheme()` - Manual schemes
- M3 components: `Button`, `TextField`, `Card`, etc.

#### Flutter Support

**Material 3 in Flutter**:
```dart
MaterialApp(
  theme: ThemeData(
    useMaterial3: true,
    colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
  ),
)
```

### Platform Support

**Available On**:
- **Android**: Jetpack Compose, Views
- **iOS**: Flutter apps
- **Web**: Material Web Components
- **Desktop**: Flutter Desktop, Compose Desktop

---

## Implementation Strategy

### Phase 1: Foundation (Weeks 1-4)

**Theme System Architecture**:
- Define `MagicTheme` base interface
- Create platform-specific theme implementations
- Build theme adapter system
- Implement theme switching mechanism

**Core Abstractions**:
```kotlin
interface MagicTheme {
    val colorScheme: ColorScheme
    val typography: Typography
    val shapes: Shapes
    val materials: Materials
    val spacing: Spacing
    val elevation: Elevation
}

interface Materials {
    val glass: GlassMaterial?
    val acrylic: AcrylicMaterial?
    val mica: MicaMaterial?
    val blur: BlurMaterial?
}
```

### Phase 2: Platform Themes (Weeks 5-12)

**Per-Platform Implementation** (2 weeks each):
1. iOS 26 Liquid Glass theme
2. macOS 26 Liquid Glass theme
3. Material 3 Expressive theme
4. Windows 11 Fluent theme

**For each platform**:
- Color scheme mapping
- Typography mapping
- Component styling
- Material effects
- Animation timings
- Interaction states

### Phase 3: XR & Advanced (Weeks 13-16)

**Spatial Platforms**:
- visionOS spatial theme (2 weeks)
- Android XR spatial theme (2 weeks)

**Advanced Features**:
- Dynamic color generation
- Wallpaper extraction
- Spatial positioning
- Depth & elevation
- Hover & gaze states

### Phase 4: Samsung & Polish (Weeks 17-20)

**Samsung One UI 7**:
- Colored glass blur implementation
- Circle-based component styling
- Samsung-specific components

**Final Polish**:
- Cross-platform testing
- Performance optimization
- Documentation
- Example apps

---

## Component Mapping Matrix

### Core Components × Platforms

| Component | iOS 26 | macOS 26 | visionOS | Win11 | Android XR | Material 3 | One UI 7 |
|-----------|--------|----------|----------|-------|------------|------------|----------|
| **Button** | Glass pill | Glass rounded | 3D glass | Acrylic | Spatial panel | Dynamic color | Circle glass |
| **TextField** | Glass input | Glass input | 3D input | Acrylic | Spatial input | M3 outlined | Glass rounded |
| **Card** | Glass panel | Glass panel | 3D panel | Mica surface | Spatial panel | M3 elevated | Glass card |
| **Dialog** | Glass alert | Glass alert | 3D modal | Smoke overlay | Spatial dialog | M3 dialog | Glass modal |
| **Menu** | Glass popup | Glass popup | 3D menu | Acrylic flyout | Orbiter | M3 menu | Glass menu |
| **Navigation** | Shrinking tab | Glass tabs | Spatial tabs | Nav rail | Orbiter | M3 nav bar | Glass nav |
| **Switch** | Glass toggle | Glass toggle | 3D toggle | Fluent toggle | Spatial toggle | M3 switch | Circle switch |
| **Slider** | Glass track | Glass track | 3D slider | Fluent slider | Spatial slider | M3 slider | Glass slider |
| **Checkbox** | Glass check | Glass check | 3D check | Fluent check | Spatial check | M3 checkbox | Circle check |

### Material Effects Mapping

| Platform | Primary Material | Secondary | Tertiary | Blur | Depth |
|----------|-----------------|-----------|----------|------|-------|
| **iOS 26** | Liquid Glass | Translucent | Shimmer | ✅ | Layers |
| **macOS 26** | Liquid Glass | Translucent | Shimmer | ✅ | Layers |
| **visionOS** | Spatial Glass | 3D Layers | Specular | ✅ | Z-axis |
| **Win11** | Mica | Acrylic | Smoke | ✅ | Elevation |
| **Android XR** | Spatial Panel | Orbiter | Elevation | ✅ | Z-axis |
| **Material 3** | Surface | Container | Dynamic | ❌ | Elevation |
| **One UI 7** | Colored Glass | Blur | Circles | ✅ | Layers |

### Color System Mapping

| Platform | Source | Palettes | Roles | Dynamic | Dark Mode |
|----------|--------|----------|-------|---------|-----------|
| **iOS 26** | System | iOS Colors | Semantic | ✅ | ✅ |
| **macOS 26** | Wallpaper | macOS Colors | Semantic | ✅ | ✅ |
| **visionOS** | System | visionOS Colors | Semantic | ✅ | ✅ |
| **Win11** | Accent | Fluent Colors | Roles | ✅ | ✅ |
| **Android XR** | Wallpaper | M3 Tonal | 65 Roles | ✅ | ✅ |
| **Material 3** | Wallpaper | 5 Tonal (13 levels) | 65 Roles | ✅ | ✅ |
| **One UI 7** | Samsung | One UI Colors | Roles | ✅ | ✅ |

---

## Next Steps

### Immediate Actions

1. **Review & Approve** this specification
2. **Prioritize platforms** (recommend: Material 3 → iOS 26 → Windows 11 → others)
3. **Prototype** one theme end-to-end
4. **Validate** technical feasibility of materials (glass, blur, etc.)
5. **Create** detailed component specs per platform

### Technical Validation Needed

- [ ] Kotlin Multiplatform can support all platform-specific materials
- [ ] Performance implications of blur/glass effects
- [ ] Platform API availability for advanced effects
- [ ] expect/actual patterns for material implementations
- [ ] Compose Multiplatform support for effects

### Design Resources to Gather

- [ ] Figma kits for all 7 platforms
- [ ] Official design tokens/values
- [ ] Component dimension specifications
- [ ] Animation timing curves
- [ ] Accessibility requirements per platform

---

## References

### Official Documentation

- **iOS 26**: [Apple Newsroom WWDC 2025](https://www.apple.com/newsroom/2025/06/)
- **macOS 26**: [Apple Developer](https://developer.apple.com)
- **visionOS**: [HIG Designing for visionOS](https://developer.apple.com/design/human-interface-guidelines/designing-for-visionos)
- **Windows 11**: [Microsoft Learn - Fluent Design](https://learn.microsoft.com/windows/apps/design/)
- **Android XR**: [Android Developers XR Design](https://developer.android.com/design/ui/xr)
- **Material 3**: [m3.material.io](https://m3.material.io)
- **One UI 7**: [Samsung Developers One UI](https://developer.samsung.com/one-ui)

### Design Resources

- **Figma**: Community files for iOS 26, Material 3, One UI 7
- **GitHub**: WinUI-Gallery (microsoft/WinUI-Gallery)
- **Tools**: Material Theme Builder, Samsung Design System

### Research Articles

- Medium: "Liquid Glass in iOS 26: The Next Evolution"
- TechRepublic: "Apple Unveils Unified Design Language"
- Android Developers Blog: "Giving Apps a New Home on Samsung Galaxy XR"

---

**Document Status**: ✅ Research Complete
**Created By**: Claude Code
**For**: AvaElements Cross-Platform UI Library
**Next**: Implementation Phase Planning

---

*This specification is a living document and will be updated as platforms evolve and implementation progresses.*
