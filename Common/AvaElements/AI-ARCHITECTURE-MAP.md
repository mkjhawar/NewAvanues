# AvaElements AI Architecture Map

**Version:** 1.0.0
**Updated:** 2025-12-01
**Purpose:** Machine-readable architecture documentation for AI assistants

---

## CRITICAL: Parity Status

```
ANDROID: 189 components ✅ REFERENCE IMPLEMENTATION
iOS:      48 components ❌ MISSING 141 COMPONENTS
WEB:      78 components ❌ MISSING 111 COMPONENTS
DESKTOP:  ~50 components ❌ MISSING ~139 COMPONENTS
```

**ACTION REQUIRED:** iOS and Web renderers need Flutter Parity components added.

---

## System Architecture

```
AvaElements/
├── Core/                          # Component DEFINITIONS (Kotlin Multiplatform)
│   └── src/commonMain/            # 136 files - THE SOURCE OF TRUTH
│       └── kotlin/com/augmentalis/
│           ├── AvaMagic/
│           │   ├── elements/      # Component models
│           │   │   ├── buttons/
│           │   │   ├── cards/
│           │   │   ├── data/
│           │   │   ├── display/
│           │   │   ├── feedback/
│           │   │   ├── inputs/
│           │   │   ├── navigation/
│           │   │   └── tags/
│           │   └── layout/        # Layout components
│           └── magicelements/
│               └── core/          # Core infrastructure
│
├── components/                    # 243 files - Extended components
│   ├── phase1/                    # Foundation (13 components)
│   ├── phase3/                    # Advanced (32 components)
│   ├── flutter-parity/            # Flutter equivalents (144 components)
│   └── unified/                   # Migration target
│
└── Renderers/                     # Platform-specific rendering
    ├── Android/                   # Jetpack Compose - 189 components ✅
    ├── iOS/                       # SwiftUI - 48 components ❌
    ├── Web/                       # React - 78 components ❌
    └── Desktop/                   # React + Tauri (shares Web renderer)
        └── (Legacy: Compose Desktop - deprecated)
```

## Technology Stack

| Platform | Framework | Renderer | Status |
|----------|-----------|----------|--------|
| Android | Jetpack Compose | ComposeRenderer.kt | ✅ Reference |
| iOS | SwiftUI | SwiftUIRenderer.swift | Needs work |
| Web | React | ReactRenderer.tsx | Needs work |
| macOS | React + Tauri | Shares Web renderer | Planned |
| Windows | React + Tauri | Shares Web renderer | Planned |
| Linux | React + Tauri | Shares Web renderer | Planned |

**Note:** Desktop platforms (macOS/Windows/Linux) use React/Tauri which shares the Web renderer codebase. The `Renderers/Desktop/` Compose Desktop code is legacy and deprecated.

---

## Component Inventory (189 Total)

### Phase 1 - Foundation (13 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| P1-01 | Button | ✅ | ✅ | ✅ | ✅ |
| P1-02 | TextField | ✅ | ✅ | ✅ | ✅ |
| P1-03 | Checkbox | ✅ | ✅ | ✅ | ✅ |
| P1-04 | Switch | ✅ | ✅ | ✅ | ✅ |
| P1-05 | Text | ✅ | ✅ | ✅ | ✅ |
| P1-06 | Image | ✅ | ✅ | ✅ | ✅ |
| P1-07 | Icon | ✅ | ✅ | ✅ | ✅ |
| P1-08 | Container | ✅ | ✅ | ✅ | ✅ |
| P1-09 | Row | ✅ | ✅ | ✅ | ✅ |
| P1-10 | Column | ✅ | ✅ | ✅ | ✅ |
| P1-11 | Card | ✅ | ✅ | ✅ | ✅ |
| P1-12 | ScrollView | ✅ | ✅ | ✅ | ✅ |
| P1-13 | List | ✅ | ✅ | ✅ | ✅ |

### Phase 3 - Advanced (32 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| P3-01 | Slider | ✅ | ✅ | ✅ | ⚠️ |
| P3-02 | RangeSlider | ✅ | ✅ | ✅ | ⚠️ |
| P3-03 | DatePicker | ✅ | ✅ | ✅ | ⚠️ |
| P3-04 | TimePicker | ✅ | ✅ | ✅ | ⚠️ |
| P3-05 | RadioButton | ✅ | ✅ | ✅ | ⚠️ |
| P3-06 | RadioGroup | ✅ | ✅ | ✅ | ⚠️ |
| P3-07 | Dropdown | ✅ | ✅ | ✅ | ⚠️ |
| P3-08 | Autocomplete | ✅ | ✅ | ✅ | ⚠️ |
| P3-09 | FileUpload | ✅ | ✅ | ✅ | ⚠️ |
| P3-10 | ImagePicker | ✅ | ✅ | ✅ | ⚠️ |
| P3-11 | Rating | ✅ | ✅ | ✅ | ⚠️ |
| P3-12 | SearchBar | ✅ | ✅ | ✅ | ⚠️ |
| P3-13 | Badge | ✅ | ✅ | ✅ | ⚠️ |
| P3-14 | Chip/MagicTag | ✅ | ✅ | ✅ | ⚠️ |
| P3-15 | Avatar | ✅ | ✅ | ✅ | ⚠️ |
| P3-16 | Divider | ✅ | ✅ | ✅ | ⚠️ |
| P3-17 | Skeleton | ✅ | ✅ | ✅ | ⚠️ |
| P3-18 | Spinner | ✅ | ✅ | ✅ | ⚠️ |
| P3-19 | ProgressBar | ✅ | ✅ | ✅ | ⚠️ |
| P3-20 | Tooltip | ✅ | ✅ | ✅ | ⚠️ |
| P3-21 | Grid | ✅ | ✅ | ✅ | ⚠️ |
| P3-22 | Stack | ✅ | ✅ | ✅ | ⚠️ |
| P3-23 | Spacer | ✅ | ✅ | ✅ | ⚠️ |
| P3-24 | Drawer | ✅ | ✅ | ✅ | ⚠️ |
| P3-25 | Tabs | ✅ | ✅ | ✅ | ⚠️ |
| P3-26 | AppBar | ✅ | ✅ | ✅ | ⚠️ |
| P3-27 | BottomNav | ✅ | ✅ | ✅ | ⚠️ |
| P3-28 | Breadcrumb | ✅ | ✅ | ✅ | ⚠️ |
| P3-29 | Pagination | ✅ | ✅ | ✅ | ⚠️ |
| P3-30 | Alert | ✅ | ✅ | ✅ | ⚠️ |
| P3-31 | Snackbar | ✅ | ✅ | ✅ | ⚠️ |
| P3-32 | Modal | ✅ | ✅ | ✅ | ⚠️ |
| P3-33 | Toast | ✅ | ✅ | ✅ | ⚠️ |
| P3-34 | Confirm | ✅ | ✅ | ✅ | ⚠️ |
| P3-35 | ContextMenu | ✅ | ✅ | ✅ | ⚠️ |

### Flutter Parity - Layout (10 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-L01 | Wrap | ✅ | ❌ | ✅ | ✅ |
| FP-L02 | Expanded | ✅ | ❌ | ❌ | ✅ |
| FP-L03 | Flexible | ✅ | ❌ | ✅ | ✅ |
| FP-L04 | Flex | ✅ | ❌ | ✅ | ✅ |
| FP-L05 | Padding | ✅ | ❌ | ✅ | ✅ |
| FP-L06 | Align | ✅ | ❌ | ✅ | ✅ |
| FP-L07 | Center | ✅ | ❌ | ❌ | ✅ |
| FP-L08 | SizedBox | ✅ | ❌ | ✅ | ✅ |
| FP-L09 | ConstrainedBox | ✅ | ❌ | ❌ | ✅ |
| FP-L10 | FittedBox | ✅ | ❌ | ✅ | ✅ |

### Flutter Parity - Material (19 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-M01 | MagicFilter | ✅ | ❌ | ✅ | ✅ |
| FP-M02 | MagicAction | ✅ | ❌ | ✅ | ✅ |
| FP-M03 | MagicChoice | ✅ | ❌ | ✅ | ✅ |
| FP-M04 | MagicInput | ✅ | ❌ | ✅ | ✅ |
| FP-M05 | ExpansionTile | ✅ | ❌ | ❌ | ✅ |
| FP-M06 | CheckboxListTile | ✅ | ❌ | ❌ | ✅ |
| FP-M07 | SwitchListTile | ✅ | ❌ | ❌ | ✅ |
| FP-M08 | FilledButton | ✅ | ❌ | ✅ | ✅ |
| FP-M09 | PopupMenuButton | ✅ | ❌ | ❌ | ✅ |
| FP-M10 | RefreshIndicator | ✅ | ❌ | ❌ | ✅ |
| FP-M11 | IndexedStack | ✅ | ❌ | ❌ | ✅ |
| FP-M12 | VerticalDivider | ✅ | ❌ | ❌ | ✅ |
| FP-M13 | FadeInImage | ✅ | ❌ | ❌ | ✅ |
| FP-M14 | CircleAvatar | ✅ | ❌ | ❌ | ✅ |
| FP-M15 | RichText | ✅ | ❌ | ✅ | ✅ |
| FP-M16 | SelectableText | ✅ | ❌ | ❌ | ✅ |
| FP-M17 | EndDrawer | ✅ | ❌ | ❌ | ✅ |
| FP-M18 | SplitButton | ✅ | ❌ | ✅ | ✅ |
| FP-M19 | LoadingButton | ✅ | ❌ | ✅ | ✅ |
| FP-M20 | CloseButton | ✅ | ❌ | ✅ | ✅ |

### Flutter Parity - Cards (8 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-C01 | PricingCard | ✅ | ❌ | ❌ | ❌ |
| FP-C02 | FeatureCard | ✅ | ❌ | ❌ | ❌ |
| FP-C03 | TestimonialCard | ✅ | ❌ | ❌ | ❌ |
| FP-C04 | ProductCard | ✅ | ❌ | ❌ | ❌ |
| FP-C05 | ArticleCard | ✅ | ❌ | ❌ | ❌ |
| FP-C06 | ImageCard | ✅ | ❌ | ❌ | ❌ |
| FP-C07 | HoverCard | ✅ | ❌ | ❌ | ❌ |
| FP-C08 | ExpandableCard | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Display (12 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-D01 | AvatarGroup | ✅ | ❌ | ❌ | ❌ |
| FP-D02 | SkeletonText | ✅ | ❌ | ❌ | ❌ |
| FP-D03 | SkeletonCircle | ✅ | ❌ | ❌ | ❌ |
| FP-D04 | ProgressCircle | ✅ | ❌ | ❌ | ❌ |
| FP-D05 | LoadingOverlay | ✅ | ❌ | ❌ | ❌ |
| FP-D06 | Popover | ✅ | ❌ | ❌ | ❌ |
| FP-D07 | ErrorState | ✅ | ❌ | ❌ | ❌ |
| FP-D08 | NoData | ✅ | ❌ | ❌ | ❌ |
| FP-D09 | ImageCarousel | ✅ | ❌ | ❌ | ❌ |
| FP-D10 | LazyImage | ✅ | ❌ | ❌ | ❌ |
| FP-D11 | ImageGallery | ✅ | ❌ | ❌ | ❌ |
| FP-D12 | Lightbox | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Feedback (10 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-F01 | Popup | ✅ | ❌ | ❌ | ❌ |
| FP-F02 | Callout | ✅ | ❌ | ❌ | ❌ |
| FP-F03 | Disclosure | ✅ | ❌ | ❌ | ❌ |
| FP-F04 | InfoPanel | ✅ | ❌ | ❌ | ❌ |
| FP-F05 | ErrorPanel | ✅ | ❌ | ❌ | ❌ |
| FP-F06 | WarningPanel | ✅ | ❌ | ❌ | ❌ |
| FP-F07 | SuccessPanel | ✅ | ❌ | ❌ | ❌ |
| FP-F08 | FullPageLoading | ✅ | ❌ | ❌ | ❌ |
| FP-F09 | AnimatedCheck | ✅ | ❌ | ❌ | ❌ |
| FP-F10 | AnimatedError | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Layout Advanced (2 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-LA01 | MasonryGrid | ✅ | ❌ | ❌ | ❌ |
| FP-LA02 | AspectRatio | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Navigation (7 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-N01 | Menu | ✅ | ❌ | ❌ | ❌ |
| FP-N02 | Sidebar | ✅ | ❌ | ❌ | ❌ |
| FP-N03 | NavLink | ✅ | ❌ | ❌ | ❌ |
| FP-N04 | ProgressStepper | ✅ | ❌ | ❌ | ❌ |
| FP-N05 | MenuBar | ✅ | ❌ | ❌ | ❌ |
| FP-N06 | SubMenu | ✅ | ❌ | ❌ | ❌ |
| FP-N07 | VerticalTabs | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Data (13 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-DA01 | RadioListTile | ✅ | ❌ | ❌ | ❌ |
| FP-DA02 | VirtualScroll | ✅ | ❌ | ❌ | ❌ |
| FP-DA03 | InfiniteScroll | ✅ | ❌ | ❌ | ❌ |
| FP-DA04 | QRCode | ✅ | ❌ | ❌ | ❌ |
| FP-DA05 | DataList | ✅ | ❌ | ❌ | ❌ |
| FP-DA06 | DescriptionList | ✅ | ❌ | ❌ | ❌ |
| FP-DA07 | StatGroup | ✅ | ❌ | ❌ | ❌ |
| FP-DA08 | Stat | ✅ | ❌ | ❌ | ❌ |
| FP-DA09 | KPI | ✅ | ❌ | ❌ | ❌ |
| FP-DA10 | MetricCard | ✅ | ❌ | ❌ | ❌ |
| FP-DA11 | Leaderboard | ✅ | ❌ | ❌ | ❌ |
| FP-DA12 | Ranking | ✅ | ❌ | ❌ | ❌ |
| FP-DA13 | Zoom | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Calendar (5 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-CAL01 | Calendar | ✅ | ❌ | ❌ | ❌ |
| FP-CAL02 | DateCalendar | ✅ | ❌ | ❌ | ❌ |
| FP-CAL03 | MonthCalendar | ✅ | ❌ | ❌ | ❌ |
| FP-CAL04 | WeekCalendar | ✅ | ❌ | ❌ | ❌ |
| FP-CAL05 | EventCalendar | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Input Advanced (11 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-I01 | PhoneInput | ✅ | ❌ | ❌ | ❌ |
| FP-I02 | UrlInput | ✅ | ❌ | ❌ | ❌ |
| FP-I03 | ComboBox | ✅ | ❌ | ❌ | ❌ |
| FP-I04 | PinInput | ✅ | ❌ | ❌ | ❌ |
| FP-I05 | OTPInput | ✅ | ❌ | ❌ | ❌ |
| FP-I06 | MaskInput | ✅ | ❌ | ❌ | ❌ |
| FP-I07 | RichTextEditor | ✅ | ❌ | ❌ | ❌ |
| FP-I08 | MarkdownEditor | ✅ | ❌ | ❌ | ❌ |
| FP-I09 | CodeEditor | ✅ | ❌ | ❌ | ❌ |
| FP-I10 | FormSection | ✅ | ❌ | ❌ | ❌ |
| FP-I11 | MultiSelect | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Scrolling (7 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-S01 | ListViewBuilder | ✅ | ❌ | ❌ | ❌ |
| FP-S02 | GridViewBuilder | ✅ | ❌ | ❌ | ❌ |
| FP-S03 | ListViewSeparated | ✅ | ❌ | ❌ | ❌ |
| FP-S04 | PageView | ✅ | ❌ | ❌ | ❌ |
| FP-S05 | ReorderableListView | ✅ | ❌ | ❌ | ❌ |
| FP-S06 | CustomScrollView | ✅ | ❌ | ❌ | ❌ |
| FP-S07 | Slivers | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Animation (8 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-A01 | AnimatedContainer | ✅ | ❌ | ❌ | ❌ |
| FP-A02 | AnimatedOpacity | ✅ | ❌ | ❌ | ❌ |
| FP-A03 | AnimatedPositioned | ✅ | ❌ | ❌ | ❌ |
| FP-A04 | AnimatedDefaultTextStyle | ✅ | ❌ | ❌ | ❌ |
| FP-A05 | AnimatedPadding | ✅ | ❌ | ❌ | ❌ |
| FP-A06 | AnimatedSize | ✅ | ❌ | ❌ | ❌ |
| FP-A07 | AnimatedAlign | ✅ | ❌ | ❌ | ❌ |
| FP-A08 | AnimatedScale | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Transitions (19 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-T01 | FadeTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T02 | SlideTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T03 | Hero | ✅ | ❌ | ❌ | ❌ |
| FP-T04 | ScaleTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T05 | RotationTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T06 | PositionedTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T07 | SizeTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T08 | AnimatedCrossFade | ✅ | ❌ | ❌ | ❌ |
| FP-T09 | AnimatedSwitcher | ✅ | ❌ | ❌ | ❌ |
| FP-T10 | DecoratedBoxTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T11 | AlignTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T12 | DefaultTextStyleTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T13 | RelativePositionedTransition | ✅ | ❌ | ❌ | ❌ |
| FP-T14 | AnimatedList | ✅ | ❌ | ❌ | ❌ |
| FP-T15 | AnimatedModalBarrier | ✅ | ❌ | ❌ | ❌ |
| FP-T16 | SliverList | ✅ | ❌ | ❌ | ❌ |
| FP-T17 | SliverGrid | ✅ | ❌ | ❌ | ❌ |
| FP-T18 | SliverFixedExtentList | ✅ | ❌ | ❌ | ❌ |
| FP-T19 | SliverAppBar | ✅ | ❌ | ❌ | ❌ |

### Flutter Parity - Charts (11 components)

| ID | Component | Android | iOS | Web | Desktop |
|----|-----------|---------|-----|-----|---------|
| FP-CH01 | LineChart | ✅ | ✅ | ❌ | ❌ |
| FP-CH02 | BarChart | ✅ | ✅ | ❌ | ❌ |
| FP-CH03 | PieChart | ✅ | ✅ | ❌ | ❌ |
| FP-CH04 | AreaChart | ✅ | ✅ | ❌ | ❌ |
| FP-CH05 | Gauge | ✅ | ✅ | ❌ | ❌ |
| FP-CH06 | Sparkline | ✅ | ✅ | ❌ | ❌ |
| FP-CH07 | RadarChart | ✅ | ✅ | ❌ | ❌ |
| FP-CH08 | ScatterChart | ✅ | ✅ | ❌ | ❌ |
| FP-CH09 | Heatmap | ✅ | ✅ | ❌ | ❌ |
| FP-CH10 | TreeMap | ✅ | ✅ | ❌ | ❌ |
| FP-CH11 | Kanban | ✅ | ✅ | ❌ | ❌ |

---

## Renderer File Locations

### Android Renderer (REFERENCE)
```
Renderers/Android/src/androidMain/kotlin/com/augmentalis/
├── magicelements/renderers/android/
│   ├── ComposeRenderer.kt              # Main: 189 component cases
│   └── mappers/
│       ├── Phase1Mappers.kt            # 13 components
│       ├── Phase3InputMappers.kt       # 12 components
│       ├── Phase3DisplayMappers.kt     # 8 components
│       ├── Phase3LayoutMappers.kt      # 5 components
│       ├── Phase3NavigationMappers.kt  # 4 components
│       ├── Phase3FeedbackMappers.kt    # 6 components
│       └── ColorUtils.kt
└── avaelements/renderer/android/
    ├── mappers/
    │   ├── LayoutMappers.kt            # 10 Flutter layout
    │   ├── MaterialMappers.kt          # 19 Flutter material
    │   ├── AnimationMappers.kt         # 8 animations
    │   ├── TransitionMappers.kt        # 19 transitions
    │   ├── ScrollingMappers.kt         # 7 scrolling
    │   ├── ChartMappers.kt             # 11 charts
    │   ├── CalendarMappers.kt          # 5 calendars
    │   ├── CustomChartMappers.kt
    │   ├── KanbanMappers.kt
    │   ├── FeedbackMappers.kt
    │   └── DisplayMappers.kt
    ├── utils/DateUtils.kt
    ├── IconRendering.kt
    └── SharedUtilitiesBridge.kt
```

### iOS Renderer (NEEDS WORK)
```
Renderers/iOS/src/iosMain/swift/
├── com/augmentalis/avaelements/renderer/ios/
│   ├── SwiftUIRenderer.swift           # Main: 48 component cases
│   └── resources/
│       ├── IOSIconResourceManager.swift
│       └── IOSImageLoader.swift
└── Charts/                             # 16 chart views
    ├── LineChartView.swift
    ├── BarChartView.swift
    ├── PieChartView.swift
    └── ... (13 more)
```

### Web Renderer (NEEDS WORK)
```
Renderers/Web/src/
├── MagicElementsRenderer.tsx           # Legacy entry
├── renderer/
│   ├── ReactRenderer.tsx               # Main renderer
│   └── ComponentRegistry.ts
├── AvaMagic/elements/                  # 32 Phase 3 components
│   ├── display/ (7)
│   ├── feedback/ (6)
│   ├── inputs/ (13)
│   ├── layout/ (4)
│   └── navigation/ (4)
├── flutterparity/                      # 33 Flutter parity
│   ├── layout/ (9)
│   └── material/
│       ├── buttons/ (14)
│       ├── chips/ (9)
│       ├── cards/ (4)
│       ├── text/ (4)
│       ├── badges/ (4)
│       └── inputs/ (2)
└── components/Phase1Components.tsx     # 13 Phase 1
```

---

## How to Add Missing Components

### iOS Pattern
```swift
// In SwiftUIRenderer.swift, add case:
case let component as AVANewComponent:
    renderNewComponent(component)

// Add render function:
private func renderNewComponent(_ component: AVANewComponent) -> some View {
    // SwiftUI implementation
}
```

### Web Pattern
```typescript
// Create file: src/AvaMagic/elements/category/NewComponent.tsx
export const NewComponent: React.FC<Props> = ({ ... }) => {
    return <div>...</div>
}

// Register in ComponentRegistry.ts
registry.register('NewComponent', NewComponent)
```

---

## Summary Counts

| Platform | Phase 1 | Phase 3 | Flutter Parity | Total | Target | Gap |
|----------|---------|---------|----------------|-------|--------|-----|
| Android | 13 | 35 | 141 | 189 | 189 | 0 |
| iOS | 13 | 35 | 0 | 48 | 189 | 141 |
| Web | 13 | 32 | 33 | 78 | 189 | 111 |
| Desktop | 13 | ~35 | ~10 | ~58 | 189 | ~131 |

---

## Priority Order for Parity

1. **iOS Flutter Parity Layout** (10 components) - Foundation
2. **iOS Flutter Parity Material** (19 components) - UI elements
3. **Web Animation** (8 components) - Critical for UX
4. **Web Transitions** (19 components) - Critical for UX
5. **iOS/Web Scrolling** (7 components each)
6. **iOS/Web Charts** (11 components) - iOS has these, Web missing
7. **Remaining components**

---

## File Registry Checksums

```
Android ComposeRenderer.kt: 189 cases
iOS SwiftUIRenderer.swift: 48 cases + 16 charts = 64 total
Web ReactRenderer.tsx: 78 registrations
```

**Last Verified:** 2025-12-01
