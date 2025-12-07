# VOS3 Design System - visionOS & iOS Guidelines
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA

## Design Philosophy
VOS3 follows Apple's visionOS design language with iOS color enhancements for a cohesive, spatial computing experience.

## visionOS Design Principles

### 1. Spatial Design
- **Glass Materials:** Use translucent glass materials for panels
- **Depth & Layering:** Create visual hierarchy with Z-axis positioning
- **Rounded Corners:** 20px radius for panels, 12px for buttons
- **Shadows:** Soft, diffused shadows for depth perception

### 2. Typography (SF Pro)
- **Extra Large Title:** SF Pro Display, 34pt, Bold
- **Title 1:** SF Pro Display, 28pt, Regular
- **Title 2:** SF Pro Display, 22pt, Regular
- **Title 3:** SF Pro Display, 20pt, Regular
- **Headline:** SF Pro Text, 17pt, Semibold
- **Body:** SF Pro Text, 17pt, Regular
- **Callout:** SF Pro Text, 16pt, Regular
- **Subheadline:** SF Pro Text, 15pt, Regular
- **Footnote:** SF Pro Text, 13pt, Regular
- **Caption:** SF Pro Text, 12pt, Regular
- **Monospace:** SF Mono, 13pt (for code/data)

### 3. iOS System Colors (Light/Dark Mode)

#### Primary Colors
- **Blue (Accent):** #007AFF / #0A84FF
- **Green (Success):** #34C759 / #32D74B
- **Indigo:** #5856D6 / #5E5CE6
- **Orange (Warning):** #FF9500 / #FF9F0A
- **Pink:** #FF2D55 / #FF375F
- **Purple:** #AF52DE / #BF5AF2
- **Red (Error):** #FF3B30 / #FF453A
- **Teal:** #5AC8FA / #64D2FF
- **Yellow:** #FFCC00 / #FFD60A

#### Semantic Colors
- **Label:** #000000 / #FFFFFF
- **Secondary Label:** #3C3C43 (60%) / #EBEBF5 (60%)
- **Tertiary Label:** #3C3C43 (30%) / #EBEBF5 (30%)
- **Quaternary Label:** #3C3C43 (18%) / #EBEBF5 (16%)
- **Placeholder Text:** #3C3C43 (30%) / #EBEBF5 (30%)
- **Separator:** #3C3C43 (20%) / #545458 (65%)
- **Opaque Separator:** #C6C6C8 / #38383A
- **Link:** #007AFF / #0A84FF

#### Background Colors
- **System Background:** #FFFFFF / #000000
- **Secondary System Background:** #F2F2F7 / #1C1C1E
- **Tertiary System Background:** #FFFFFF / #2C2C2E
- **System Grouped Background:** #F2F2F7 / #000000
- **Secondary System Grouped Background:** #FFFFFF / #1C1C1E
- **Tertiary System Grouped Background:** #F2F2F7 / #2C2C2E

#### Fill Colors
- **System Fill:** #787880 (20%) / #787880 (36%)
- **Secondary System Fill:** #787880 (16%) / #787880 (32%)
- **Tertiary System Fill:** #767680 (12%) / #767680 (24%)
- **Quaternary System Fill:** #747480 (8%) / #747480 (18%)

### 4. visionOS Glass Materials
```css
/* Glass Panel */
background: rgba(255, 255, 255, 0.7); /* Light mode */
background: rgba(30, 30, 30, 0.7);    /* Dark mode */
backdrop-filter: blur(50px);
-webkit-backdrop-filter: blur(50px);
border: 1px solid rgba(255, 255, 255, 0.18);
box-shadow: 0 8px 32px 0 rgba(31, 38, 135, 0.15);
```

### 5. Spacing System (Based on 8pt Grid)
- **XXS:** 4px
- **XS:** 8px
- **S:** 12px
- **M:** 16px
- **L:** 24px
- **XL:** 32px
- **XXL:** 48px
- **XXXL:** 64px

### 6. Component Specifications

#### Glass Panel
- Background: 70% opacity with blur
- Border radius: 20px
- Padding: 24px
- Shadow: Soft diffused shadow
- Border: 1px translucent white

#### Buttons
- Height: 44px (standard), 50px (large)
- Border radius: 12px
- Padding: 12px horizontal
- Font: SF Pro Text, 17pt, Regular
- Background: System Blue with 15% opacity
- Hover: +10% opacity
- Active: +20% opacity

#### Text Fields
- Height: 44px
- Border radius: 10px
- Padding: 12px
- Background: Quaternary System Fill
- Border: 1px System Separator on focus
- Font: SF Pro Text, 17pt

#### Cards
- Background: Secondary System Background
- Border radius: 16px
- Padding: 16px
- Shadow: 0 2px 8px rgba(0,0,0,0.1)
- Spacing between cards: 12px

#### Navigation
- Tab bar height: 49px
- Navigation bar height: 44px (compact), 96px (large)
- Side panel width: 320px minimum
- Icon size: 28px × 28px

### 7. Animation Guidelines
- **Duration:** 
  - Micro: 0.1s (hover states)
  - Short: 0.25s (transitions)
  - Medium: 0.35s (panel slides)
  - Long: 0.5s (page transitions)
- **Easing:** cubic-bezier(0.25, 0.1, 0.25, 1.0) - iOS standard
- **Spring:** damping: 0.8, stiffness: 200

### 8. Accessibility
- **Contrast Ratios:** 
  - Normal text: 4.5:1 minimum
  - Large text: 3:1 minimum
- **Touch Targets:** 44×44pt minimum
- **Focus Indicators:** 2px outline with System Blue
- **VoiceOver:** All interactive elements labeled

### 9. Icons (SF Symbols)
- Use SF Symbols 5 for consistency
- Weight: Regular for most, Medium for emphasis
- Size: 17pt (inline), 22pt (buttons), 28pt (toolbar)
- Rendering: Hierarchical or monochrome

### 10. Data Visualization Colors
- **Chart Blue:** #007AFF
- **Chart Green:** #34C759
- **Chart Orange:** #FF9500
- **Chart Red:** #FF3B30
- **Chart Purple:** #AF52DE
- **Chart Teal:** #5AC8FA

## VOS3 Decoder Tool Specific Design

### Window Layout
- **Window Size:** 1280×800 default, 1024×768 minimum
- **Glass Effect:** 70% opacity with 50px blur
- **Corner Radius:** 20px for window, 12px for panels

### Panel Distribution
- **Left Panel:** 320px fixed width (categories)
- **Right Panel:** Flexible width (data viewer)
- **Top Toolbar:** 56px height
- **Status Bar:** 32px height

### Color Usage
- **Primary Action:** System Blue (#007AFF)
- **Success States:** System Green (#34C759)
- **Warning States:** System Orange (#FF9500)
- **Error States:** System Red (#FF3B30)
- **Data Categories:** Use full spectrum for visual distinction

### Typography Hierarchy
- **Window Title:** SF Pro Display, 20pt, Semibold
- **Section Headers:** SF Pro Text, 17pt, Semibold
- **Category Labels:** SF Pro Text, 15pt, Regular
- **Data Values:** SF Mono, 13pt, Regular
- **Status Text:** SF Pro Text, 12pt, Regular

## Implementation Notes

### Flutter Material 3 Mapping
```dart
// Use Material 3 with custom theme
ThemeData(
  useMaterial3: true,
  colorScheme: ColorScheme.fromSeed(
    seedColor: Color(0xFF007AFF), // iOS System Blue
    brightness: Brightness.light,
  ),
  fontFamily: Platform.isIOS || Platform.isMacOS ? '.SF Pro Text' : 'Roboto',
)
```

### Glass Effect in Flutter
```dart
ClipRRect(
  borderRadius: BorderRadius.circular(20),
  child: BackdropFilter(
    filter: ImageFilter.blur(sigmaX: 50, sigmaY: 50),
    child: Container(
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.7),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: Colors.white.withOpacity(0.18),
        ),
      ),
    ),
  ),
)
```

## Platform Adaptations
- **macOS/iOS:** Use native SF fonts and system colors
- **Windows/Linux:** Use Segoe UI/Ubuntu with same color values
- **Android:** Use Roboto with Material You color mapping