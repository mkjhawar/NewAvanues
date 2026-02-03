# AvaElements Renderer Registry

**Generated:** 2025-12-01
**Purpose:** Track renderer parity across platforms

---

## PARITY STATUS - CRITICAL

| Platform | Framework | Implemented | Target | Gap | Status |
|----------|-----------|-------------|--------|-----|--------|
| Android | Jetpack Compose | 189 | 189 | 0 | ✅ REFERENCE |
| iOS | SwiftUI | 48 (+16 charts) | 189 | 141 | ❌ NEEDS WORK |
| Web | React | 78 | 189 | 111 | ❌ NEEDS WORK |
| macOS | React + Tauri | 78* | 189 | 111 | ❌ SHARES WEB |
| Windows | React + Tauri | 78* | 189 | 111 | ❌ SHARES WEB |
| Linux | React + Tauri | 78* | 189 | 111 | ❌ SHARES WEB |

**Android is the reference implementation. All platforms must match.**

*Desktop platforms share the Web React renderer via Tauri wrapper.

---

## Android Renderer

**Path:** `Renderers/Android/src/androidMain/`

### Main Files
| File | Purpose |
|------|---------|
| `ComposeRenderer.kt` | Main renderer (190 component cases) |
| `IconRendering.kt` | Icon resource handling |
| `SharedUtilitiesBridge.kt` | Shared utilities |

### Mappers (13 files)
| Mapper | Components |
|--------|------------|
| `Phase1Mappers.kt` | Button, Text, TextField, Checkbox, Switch, Icon, Image, Container, Row, Column, Card, ScrollView, List |
| `Phase3InputMappers.kt` | Slider, RangeSlider, DatePicker, TimePicker, RadioButton, RadioGroup, Dropdown, Autocomplete, FileUpload, ImagePicker, Rating, SearchBar |
| `Phase3DisplayMappers.kt` | Badge, Chip, Avatar, Divider, Skeleton, Spinner, ProgressBar, Tooltip |
| `Phase3LayoutMappers.kt` | Grid, Stack, Spacer, Drawer, Tabs |
| `Phase3NavigationMappers.kt` | AppBar, BottomNav, Breadcrumb, Pagination |
| `Phase3FeedbackMappers.kt` | Alert, Snackbar, Modal, Toast, Confirm, ContextMenu |
| `LayoutMappers.kt` | Wrap, Expanded, Flexible, Flex, Padding, Align, Center, SizedBox, ConstrainedBox, FittedBox |
| `MaterialMappers.kt` | FilterChip, ActionChip, ChoiceChip, InputChip, ExpansionTile, CheckboxListTile, SwitchListTile, etc. |
| `AnimationMappers.kt` | AnimatedContainer, AnimatedOpacity, AnimatedPositioned, etc. |
| `TransitionMappers.kt` | FadeTransition, SlideTransition, Hero, ScaleTransition, etc. |
| `ScrollingMappers.kt` | ListView, GridView, PageView, ReorderableListView |
| `ChartMappers.kt` | LineChart, BarChart, PieChart, etc. |
| `CalendarMappers.kt` | Calendar, DateCalendar, MonthCalendar, WeekCalendar, EventCalendar |

### Performance Files (3)
- `AnimationOptimizer.kt`
- `PerformanceOptimizer.kt`
- `ScrollingOptimizer.kt`

---

## iOS Renderer

**Path:** `Renderers/iOS/src/iosMain/swift/`

### Main Files
| File | Purpose | LOC |
|------|---------|-----|
| `SwiftUIRenderer.swift` | Main renderer (49 render functions) | 710 |
| `IOSIconResourceManager.swift` | Icon caching & loading | ~200 |
| `IOSImageLoader.swift` | Async image loading | ~150 |

### Charts (16 files)
| File | Component |
|------|-----------|
| `LineChartView.swift` | Line charts |
| `BarChartView.swift` | Bar charts |
| `PieChartView.swift` | Pie charts |
| `AreaChartView.swift` | Area charts |
| `ScatterChartView.swift` | Scatter plots |
| `RadarChartView.swift` | Radar charts |
| `GaugeView.swift` | Gauge displays |
| `SparklineView.swift` | Sparklines |
| `HeatmapView.swift` | Heatmaps |
| `TreeMapView.swift` | Tree maps |
| `KanbanView.swift` | Kanban boards |
| `KanbanColumnView.swift` | Kanban columns |
| `KanbanCardView.swift` | Kanban cards |
| `ChartHelpers.swift` | Chart utilities |
| `ChartColors.swift` | Color definitions |
| `ChartAccessibility.swift` | A11y support |

### Components Rendered (49)
**Phase 1 - Form:** Checkbox, TextField, Button, Switch
**Phase 1 - Display:** Text, Image, Icon
**Phase 1 - Layout:** Container, Row, Column, Card, ScrollView, List
**Phase 3 - Input:** Slider, RangeSlider, DatePicker, TimePicker, RadioButton, RadioGroup, Dropdown, Autocomplete, FileUpload, ImagePicker, Rating, SearchBar
**Phase 3 - Display:** Badge, Chip, Avatar, Divider, Skeleton, Spinner, ProgressBar, Tooltip
**Phase 3 - Layout:** Grid, Stack, Spacer, Drawer, Tabs
**Phase 3 - Navigation:** AppBar, BottomNav, Breadcrumb, Pagination
**Phase 3 - Feedback:** Alert, Snackbar, Modal, Toast, Confirm, ContextMenu

---

## Web Renderer

**Path:** `Renderers/Web/src/`

### Core Files
| File | Purpose |
|------|---------|
| `MagicElementsRenderer.tsx` | Main renderer entry |
| `renderer/ReactRenderer.tsx` | React rendering logic |
| `renderer/ComponentRegistry.ts` | Component registration |

### AvaMagic Elements (32 files)
**Display (7):** Avatar, Divider, ProgressBar, Skeleton, Spinner, Tooltip
**Feedback (6):** Alert, Confirm, ContextMenu, Modal, Snackbar, Toast
**Inputs (13):** Autocomplete, DatePicker, Dropdown, FileUpload, ImagePicker, RadioButton, RadioGroup, RangeSlider, Rating, SearchBar, Slider, TimePicker
**Layout (4):** Drawer, Grid, Spacer, Tabs
**Navigation (4):** AppBar, BottomNav, Breadcrumb, Pagination

### Flutter Parity (60+ files)
**Layout (10):** Container, Padding, Flex, SizedBox, FittedBox, Align, Stack, Wrap, Flexible, helpers
**Material Buttons (14):** FilledButton, FilledTonalButton, OutlinedButton, TextButton, ElevatedButton, IconButton, FloatingActionButton, SegmentedButton, SplitButton, LoadingButton, CloseButton, ButtonBar, ButtonTheme
**Material Chips (9):** MagicTag, MagicFilter, MagicAction, MagicChoice, MagicInput, MagicTagBase, MagicTagTheme
**Material Cards (4):** Card, CardTheme
**Material Text (4):** Text, RichText
**Material Badges (4):** Badge, BadgeTheme
**Material Inputs (2):** AllInputs

### Resources
- `IconResourceManager.ts`
- `ImageLoader.tsx`
- `sharedUtilitiesBridge.ts`

---

## Desktop Renderer (macOS/Windows/Linux)

**Framework:** React + Tauri
**Implementation:** Shares Web renderer (`Renderers/Web/`)

Desktop platforms use the same React codebase as Web, wrapped with Tauri for native desktop execution.

### Architecture
```
Desktop App = Web React Renderer + Tauri Native Shell
├── UI Layer: React (shared with Web)
├── Native Bridge: Tauri
└── Platforms: macOS, Windows, Linux
```

### Legacy (Deprecated)
**Path:** `Renderers/Desktop/src/desktopMain/` (Compose Desktop - NOT IN USE)

| File | Status |
|------|--------|
| `ComposeDesktopRenderer.kt` | DEPRECATED |
| Other mappers | DEPRECATED |

---

## Core Components

**Path:** `Core/src/commonMain/`

### Total: 136 files

**Elements (8 categories):**
- buttons/
- cards/
- data/
- display/
- feedback/
- inputs/
- navigation/
- tags/

**Layout:**
- Row, Column, Container, Layout

---

## Component Definitions

**Path:** `components/`

### Total: 243 files

**Directories:**
- `phase1/` - Foundation components
- `phase3/` - Advanced components
- `flutter-parity/` - Flutter equivalents
- `unified/` - Unified structure (WIP)

---

## Parity Matrix

| Component Category | Android | iOS | Web | Desktop (Tauri) |
|-------------------|---------|-----|-----|-----------------|
| Phase 1 Form | ✅ | ✅ | ✅ | ✅ (via Web) |
| Phase 1 Display | ✅ | ✅ | ✅ | ✅ (via Web) |
| Phase 1 Layout | ✅ | ✅ | ✅ | ✅ (via Web) |
| Phase 3 Input | ✅ | ✅ | ✅ | ✅ (via Web) |
| Phase 3 Display | ✅ | ✅ | ✅ | ✅ (via Web) |
| Phase 3 Layout | ✅ | ✅ | ✅ | ✅ (via Web) |
| Phase 3 Navigation | ✅ | ✅ | ✅ | ✅ (via Web) |
| Phase 3 Feedback | ✅ | ✅ | ✅ | ✅ (via Web) |
| Flutter Layout | ✅ | ❌ | ✅ | ✅ (via Web) |
| Flutter Material | ✅ | ❌ | ✅ | ✅ (via Web) |
| Flutter Animation | ✅ | ❌ | ❌ | ❌ |
| Flutter Transitions | ✅ | ❌ | ❌ | ❌ |
| Charts | ✅ | ✅ | ❌ | ❌ |

**Legend:** ✅ Complete | ⚠️ Partial | ❌ Missing

---

## Status Summary

| Platform | Framework | Score | Notes |
|----------|-----------|-------|-------|
| Android | Jetpack Compose | 10/10 | Full reference implementation |
| iOS | SwiftUI | 6/10 | Core complete, Flutter parity missing |
| Web | React | 7/10 | Core complete, animations/charts missing |
| macOS | React + Tauri | 7/10 | Shares Web renderer |
| Windows | React + Tauri | 7/10 | Shares Web renderer |
| Linux | React + Tauri | 7/10 | Shares Web renderer |

---

## Last Updated
2025-12-01 - Updated technology stack: Desktop now React + Tauri (shares Web renderer)
