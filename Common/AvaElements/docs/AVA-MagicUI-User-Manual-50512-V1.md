# AvaMagicUI User Manual

**Version:** 1.0.0
**Updated:** 2025-12-05
**Audience:** Designers, Product Managers, Integration Engineers

---

## Table of Contents

1. [What is AvaMagicUI?](#what-is-avamagicui)
2. [Component Library Overview](#component-library-overview)
3. [Style Variants](#style-variants)
4. [Component Reference](#component-reference)
5. [Common Patterns](#common-patterns)
6. [Platform Differences](#platform-differences)
7. [Accessibility](#accessibility)
8. [FAQ](#faq)

---

## What is AvaMagicUI?

AvaMagicUI is a cross-platform UI component library that provides consistent components across Android, iOS, Web, and Desktop applications.

### Quick Stats

| Metric | Value |
|--------|-------|
| Total Components | 205 |
| Style Variants | 1000+ combinations |
| Platforms | Android, iOS, Web, Desktop |
| Rendering | Native per platform |

### Comparison to Alternatives

| Feature | AvaMagicUI | Flutter | React Native |
|---------|:----------:|:-------:|:------------:|
| Native Rendering | Yes | No (Skia) | Yes |
| Component Count | 205 | 150+ | 70-100 |
| Variant System | 1000+ | Manual | Manual |
| Desktop Support | Full | Full | Limited |

---

## Component Library Overview

### By Category

| Category | Count | Examples |
|----------|:-----:|----------|
| **Buttons** | 15 | Button, IconButton, FAB, LoadingButton |
| **Inputs** | 23 | TextField, Dropdown, DatePicker, Rating |
| **Layout** | 18 | Container, Row, Column, Grid, BottomSheet |
| **Navigation** | 14 | AppBar, Tabs, Breadcrumb, Sidebar |
| **Display** | 20 | Avatar, Badge, Skeleton, Tooltip |
| **Feedback** | 16 | Alert, Toast, Modal, ProgressBar |
| **Cards** | 11 | Card, PricingCard, ProductCard, ImageCard |
| **Data** | 13 | DataList, StatGroup, Leaderboard, QRCode |
| **Charts** | 11 | LineChart, BarChart, PieChart, Gauge |
| **Calendar** | 5 | Calendar, DateCalendar, EventCalendar |
| **Animation** | 19 | AnimatedContainer, FadeTransition, Hero |
| **Scrolling** | 11 | ListView, PageView, VirtualScroll |
| **Typography** | 6 | HeadingText, BodyText, CaptionText |
| **Onboarding** | 2 | IntroScreen, OnboardingStep |

---

## Style Variants

Instead of creating hundreds of component variations, AvaMagicUI uses a **variant system** that combines props to create unique styles.

### Button Variants Example

| Variant | Preview Description |
|---------|-------------------|
| Filled + Primary | Solid blue button |
| Filled + Danger | Solid red button |
| Outlined + Primary | Blue border, transparent fill |
| Ghost + Primary | No border, blue text |
| Pill + Success | Rounded ends, green |
| Elevated + Secondary | Shadow, gray |

### Available Style Options

**Button Styles:**

| Option | Values |
|--------|--------|
| Variant | Filled, Outlined, Text, Elevated, Tonal, Pill, Square, Ghost |
| Size | XSmall, Small, Medium, Large, XLarge |
| Shape | Rounded, Square, Pill, Circle |
| Color | Primary, Secondary, Success, Warning, Danger, Info, Light, Dark, Neutral |

**Card Styles:**

| Option | Values |
|--------|--------|
| Variant | Elevated, Filled, Outlined, Ghost |
| Shape | Rounded, Square, RoundedLarge |

**Input Styles:**

| Option | Values |
|--------|--------|
| Variant | Outlined, Filled, Underlined, Ghost |
| Size | Small, Medium, Large |
| State | Default, Focused, Error, Success, Disabled |

**Avatar Styles:**

| Option | Values |
|--------|--------|
| Shape | Circle, Square, Rounded |
| Size | XSmall, Small, Medium, Large, XLarge |

---

## Component Reference

### Buttons

#### Button
Standard clickable button with multiple variants.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| label | String | required | Button text |
| variant | ButtonVariant | Filled | Visual style |
| size | ButtonSize | Medium | Button size |
| color | ColorScheme | Primary | Color theme |
| icon | String? | null | Optional icon |
| disabled | Boolean | false | Disable interaction |
| onTap | Callback | null | Click handler |

#### FloatingActionButton (FAB)
Circular action button, typically floating.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| icon | String | required | Icon name |
| size | FabSize | Regular | Regular, Small, Extended |
| color | ColorScheme | Primary | Color theme |
| extended | Boolean | false | Show label |
| label | String? | null | Label for extended FAB |

#### FloatingMenu
Expandable FAB with multiple action buttons.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| items | List<MenuItem> | required | Menu items |
| mainIcon | String | "plus" | Main button icon |
| position | Position | BottomRight | Screen position |
| isOpen | Boolean | false | Expansion state |

---

### Layout

#### BottomSheet
Slide-up panel from screen bottom.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| isOpen | Boolean | false | Visibility |
| title | String? | null | Optional title |
| height | Height | Auto | Auto, Half, Full, FitContent |
| showDragHandle | Boolean | true | Show drag indicator |
| dismissible | Boolean | true | Allow dismiss on backdrop tap |

#### StickyHeader
Header that sticks to top when scrolling.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| content | List<Component> | required | Header content |
| stickyOffset | Float | 0 | Offset from top |
| elevation | Float | 4 | Shadow depth |
| backgroundColor | String? | null | Background color |

#### PullToRefresh
Pull-down gesture to refresh content.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| isRefreshing | Boolean | false | Loading state |
| threshold | Float | 80 | Pull distance to trigger |
| onRefresh | Callback | null | Refresh handler |

---

### Navigation

#### IntroScreen
Onboarding/introduction screen with pages.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| pages | List<IntroPage> | required | Onboarding pages |
| showSkip | Boolean | true | Show skip button |
| showNext | Boolean | true | Show next button |
| doneLabel | String | "Get Started" | Final button text |
| onDone | Callback | null | Completion handler |

**IntroPage Structure:**

| Property | Type | Description |
|----------|------|-------------|
| title | String | Page title |
| description | String | Page description |
| imageUrl | String? | Optional image |
| icon | String? | Optional icon |
| backgroundColor | String? | Page background |

---

### Display

#### HeadingText
Semantic heading text (H1-H6).

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| text | String | required | Heading text |
| level | HeadingLevel | H1 | H1, H2, H3, H4, H5, H6 |
| color | String? | null | Text color |
| textAlign | Alignment | Start | Start, Center, End |

#### DisplayText
Large display text for heroes.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| text | String | required | Display text |
| size | DisplaySize | Medium | Small, Medium, Large, XLarge |
| gradient | List<String>? | null | Gradient colors |

#### BorderDecorator
Decorative border wrapper.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| width | Float | 1 | Border width |
| color | String | "#000" | Border color |
| style | BorderStyle | Solid | Solid, Dashed, Dotted, Double |
| radius | Float | 0 | Corner radius |
| sides | BorderSides | All | Which sides to show |

---

### Cards

#### ProductCard
E-commerce product display card.

| Property | Type | Description |
|----------|------|-------------|
| imageUrl | String | Product image |
| title | String | Product name |
| price | String | Current price |
| originalPrice | String? | Strikethrough price |
| rating | Float? | Star rating (1-5) |
| badge | String? | "Sale", "New", etc. |

#### PricingCard
Pricing tier card for SaaS.

| Property | Type | Description |
|----------|------|-------------|
| title | String | Tier name |
| price | String | Price text |
| period | String | "month", "year" |
| features | List<String> | Feature list |
| isPopular | Boolean | Highlight as recommended |
| ctaLabel | String | Button text |

---

### Carousels

#### ProductCarousel
Horizontal scrolling product list.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| products | List<ProductItem> | required | Product items |
| autoPlay | Boolean | false | Auto-scroll |
| showArrows | Boolean | true | Navigation arrows |
| showIndicators | Boolean | true | Page dots |

#### FullWidthCarousel
Full-width hero/banner carousel.

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| items | List<CarouselSlide> | required | Slides |
| aspectRatio | Float | 16/9 | Width to height ratio |
| autoPlay | Boolean | true | Auto-advance |
| autoPlayInterval | Int | 5000 | Milliseconds per slide |

---

## Common Patterns

### Login Form

```
Column
├── HeadingText (level: H2, text: "Welcome Back")
├── TextField (label: "Email", variant: Outlined)
├── TextField (label: "Password", variant: Outlined, isPassword: true)
├── Button (label: "Sign In", variant: Filled, color: Primary)
└── Button (label: "Forgot Password?", variant: Text)
```

### Settings Screen

```
Column
├── AppBar (title: "Settings")
├── StickyHeader
│   └── SearchBar
└── ScrollView
    ├── SwitchListTile (title: "Dark Mode")
    ├── SwitchListTile (title: "Notifications")
    ├── ExpansionTile (title: "Privacy")
    │   ├── CheckboxListTile (title: "Analytics")
    │   └── CheckboxListTile (title: "Personalization")
    └── Button (label: "Sign Out", variant: Outlined, color: Danger)
```

### Product Detail

```
Column
├── FullWidthCarousel (images)
├── Row
│   ├── HeadingText (price)
│   └── Badge (variant: "Sale")
├── HeadingText (level: H3, title)
├── BodyText (description)
├── Rating (value: 4.5)
├── Divider
└── ButtonBar
    ├── Button (label: "Add to Cart", variant: Filled)
    └── IconButton (icon: "heart", variant: Outlined)
```

### Onboarding Flow

```
IntroScreen
├── IntroPage
│   ├── icon: "wand.and.stars"
│   ├── title: "Welcome to App"
│   └── description: "Discover amazing features"
├── IntroPage
│   ├── imageUrl: "/images/feature1.png"
│   ├── title: "Stay Connected"
│   └── description: "Sync across all devices"
└── IntroPage
    ├── icon: "checkmark.circle"
    ├── title: "You're All Set"
    └── description: "Let's get started!"
```

---

## Platform Differences

### Rendering Behavior

| Component | Android | iOS | Web |
|-----------|---------|-----|-----|
| Button | Material ripple | Highlight opacity | CSS hover |
| Modal | Bottom sheet default | Center default | Center |
| DatePicker | Material picker | UIDatePicker | Native input |
| Scroll | Overscroll glow | Bounce | Browser scroll |
| FAB | Material elevation | SF Symbols | CSS shadow |

### Platform-Specific Features

| Feature | Android | iOS | Web |
|---------|:-------:|:---:|:---:|
| Haptic feedback | Yes | Yes | Limited |
| Dark mode auto | Yes | Yes | Via CSS |
| Safe area insets | Yes | Yes | Via meta tag |
| Keyboard handling | Native | Native | Via JS |

---

## Accessibility

### Built-in Features

| Feature | Implementation |
|---------|----------------|
| Screen reader | Semantic labels on all interactive elements |
| Focus order | Logical tab order |
| Color contrast | WCAG AA compliant colors |
| Touch targets | Minimum 44x44pt on mobile |
| Motion | Respects reduced motion preferences |

### Component Accessibility Props

| Prop | Description |
|------|-------------|
| accessibilityLabel | Screen reader announcement |
| accessibilityHint | Additional context |
| accessibilityRole | Button, heading, link, etc. |
| isAccessibilityElement | Include in accessibility tree |

---

## FAQ

### How many components are there?
**205 components** across 14 categories, with 1000+ style combinations via the variant system.

### Which platforms are supported?
- **Android** - Jetpack Compose (native)
- **iOS** - SwiftUI (native)
- **Web** - React/TypeScript
- **Desktop** - Electron (shares Web renderer)

### How do I request a new component?
File an issue in the repository with:
1. Component name
2. Use case description
3. Reference design (if available)
4. Priority justification

### Can I customize colors?
Yes, use the ColorScheme enum for consistent theming or provide custom hex colors to the `color` prop.

### How do I report a bug?
File an issue with:
1. Component name
2. Platform (Android/iOS/Web)
3. Steps to reproduce
4. Expected vs actual behavior
5. Screenshots if applicable

---

## Appendix: Complete Component List

<details>
<summary>Click to expand full component list (205)</summary>

### Phase 1 - Foundation (13)
Button, TextField, Checkbox, Switch, Text, Image, Icon, Container, Row, Column, Card, ScrollView, List

### Phase 3 - Extended (47)
**Input:** Slider, RangeSlider, DatePicker, TimePicker, RadioButton, RadioGroup, Dropdown, Autocomplete, FileUpload, ImagePicker, Rating, SearchBar

**Display:** Badge, Chip, Avatar, Divider, Skeleton, Spinner, ProgressBar, Tooltip

**Layout:** Grid, Stack, Spacer, Drawer, Tabs, BottomSheet, StickyHeader, PullToRefresh

**Navigation:** AppBar, BottomNav, Breadcrumb, Pagination, FloatingMenu

**Feedback:** Alert, Snackbar, Modal, Toast, Confirm, ContextMenu

**Onboarding:** IntroScreen, OnboardingStep

**Typography:** HeadingText, DisplayText, LabelText, CaptionText, BodyText, BorderDecorator

### Flutter Parity (145)
**Layout:** Align, Center, ConstrainedBox, Expanded, FittedBox, Flex, Flexible, Padding, SizedBox, Wrap

**Chips:** MagicFilter, MagicAction, MagicChoice, MagicInput, MagicTag

**Buttons:** FilledButton, CloseButton, ElevatedButton, FAB, IconButton, LoadingButton, OutlinedButton, PopupMenuButton, RefreshIndicator, SegmentedButton, SplitButton, TextButton, ButtonBar, FilledTonalButton

**Lists:** ExpansionTile, CheckboxListTile, SwitchListTile, RadioListTile

**Cards:** PricingCard, FeatureCard, TestimonialCard, ProductCard, ArticleCard, ImageCard, HoverCard, ExpandableCard, ProductCarousel, FullWidthCarousel, FullSizeCarousel

**Display:** AvatarGroup, SkeletonText, SkeletonCircle, ProgressCircle, LoadingOverlay, Popover, ErrorState, NoData, ImageCarousel, LazyImage, ImageGallery, Lightbox

**Feedback:** Popup, Callout, Disclosure, InfoPanel, ErrorPanel, WarningPanel, SuccessPanel, FullPageLoading, AnimatedCheck, AnimatedError

**Navigation:** Menu, Sidebar, NavLink, ProgressStepper, MenuBar, SubMenu, VerticalTabs, MasonryGrid, AspectRatio

**Data:** DataList, DescriptionList, StatGroup, Stat, KPI, MetricCard, Leaderboard, Ranking, Zoom, VirtualScroll, InfiniteScroll, QRCode, RichText

**Input:** PhoneInput, UrlInput, ComboBox, PinInput, OTPInput, MaskInput, RichTextEditor, MarkdownEditor, CodeEditor, FormSection, MultiSelect

**Calendar:** Calendar, DateCalendar, MonthCalendar, WeekCalendar, EventCalendar

**Scrolling:** ListViewBuilder, GridViewBuilder, ListViewSeparated, PageView, ReorderableListView, CustomScrollView, IndexedStack

**Animation:** AnimatedContainer, AnimatedOpacity, AnimatedPositioned, AnimatedDefaultTextStyle, AnimatedPadding, AnimatedSize, AnimatedAlign, AnimatedScale

**Transitions:** FadeTransition, SlideTransition, Hero, ScaleTransition, RotationTransition, PositionedTransition, SizeTransition, AnimatedCrossFade, AnimatedSwitcher, DecoratedBoxTransition, AlignTransition

**Slivers:** SliverList, SliverGrid, SliverFixedExtentList, SliverAppBar

**Charts:** LineChart, BarChart, PieChart, AreaChart, Gauge, Sparkline, RadarChart, ScatterChart, Heatmap, TreeMap, Kanban

**Other:** FadeInImage, CircleAvatar, SelectableText, VerticalDivider, EndDrawer, AnimatedList, AnimatedModalBarrier, DefaultTextStyleTransition, RelativePositionedTransition

</details>

---

**Maintainer:** Engineering Team
**Last Updated:** 2025-12-05
