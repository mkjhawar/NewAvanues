# AvaElements Template Library

Production-ready screen templates for rapid application development with AvaUI.

## Overview

The Template Library provides 25+ pre-built, customizable screen templates covering authentication, dashboards, e-commerce, social, and utility categories. Each template includes:

- Complete AvaUI DSL code
- Generated Kotlin Compose code
- Visual preview/screenshot
- Customization guide
- Use cases and examples

## Categories

### 1. Authentication (5 Templates)

#### T001: Material Login
**Use Case**: Standard email/password login
**Components**: TextField (2), Button (2), Text (3), Image (1)
**Features**:
- Email and password inputs
- Remember me checkbox
- Forgot password link
- Social login buttons (Google, Facebook, Apple)
- Sign up redirect

#### T002: Social Login
**Use Case**: OAuth-based authentication
**Components**: Button (4), Text (2), Image (1), Divider (1)
**Features**:
- Google Sign-In
- Facebook Login
- Apple Sign-In
- Twitter Login
- OR divider
- Terms acceptance

#### T003: Biometric Auth
**Use Case**: Fingerprint/Face ID authentication
**Components**: Image (1), Text (3), Button (2), Avatar (1)
**Features**:
- Biometric icon animation
- Username display
- Alternative PIN option
- Cancel button
- Error states

#### T004: OTP Verification
**Use Case**: SMS/Email verification code
**Components**: TextField (6), Text (3), Button (2)
**Features**:
- 6-digit OTP input
- Auto-focus progression
- Resend code timer
- Verify button
- Edit phone number link

#### T005: Multi-Step Signup
**Use Case**: Complex registration flow
**Components**: TextField (6), Button (3), ProgressBar (1), Text (5)
**Features**:
- Step 1: Basic info (name, email)
- Step 2: Password creation
- Step 3: Profile details
- Progress indicator
- Back/Next navigation
- Form validation

### 2. Dashboard (5 Templates)

#### T006: Stats Dashboard
**Use Case**: KPI overview with metrics
**Components**: AppBar (1), Card (6), Text (12), Icon (6), ProgressBar (3)
**Features**:
- Total users metric
- Revenue metric
- Orders metric
- Growth indicators
- Time period selector
- Refresh button

#### T007: Analytics Dashboard
**Use Case**: Charts and graphs
**Components**: AppBar (1), Tabs (1), Card (4), Chart (4), Dropdown (1)
**Features**:
- Line chart (revenue over time)
- Bar chart (sales by category)
- Pie chart (traffic sources)
- Tab navigation (Overview/Sales/Traffic/Users)
- Date range filter
- Export button

#### T008: E-commerce Dashboard
**Use Case**: Online store management
**Components**: AppBar (1), Card (8), ListView (1), Badge (3), Button (2)
**Features**:
- Recent orders list
- Top products
- Revenue metrics
- Pending actions
- Quick actions
- Notifications

#### T009: Project Dashboard
**Use Case**: Task/project tracking
**Components**: AppBar (1), Tabs (1), Card (3), ListView (2), Avatar (4), Chip (5)
**Features**:
- Active projects
- Task list with status
- Team members
- Progress tracking
- Filter by status
- Add project button

#### T010: Monitoring Dashboard
**Use Case**: System health metrics
**Components**: AppBar (1), Card (6), ProgressBar (4), Badge (2), Alert (1)
**Features**:
- CPU usage
- Memory usage
- Network traffic
- Disk space
- Alerts/warnings
- Live updates

### 3. E-commerce (5 Templates)

#### T011: Product Grid
**Use Case**: Product catalog browse
**Components**: AppBar (1), Grid (1), Card (12), Image (12), Text (24), Badge (4)
**Features**:
- 2-column grid layout
- Product images
- Name and price
- Rating stars
- Add to cart button
- Sale badges

#### T012: Product Details
**Use Case**: Individual product view
**Components**: AppBar (1), Image (4), Text (8), Button (3), Chip (3), Rating (1)
**Features**:
- Image carousel
- Product name and description
- Price and discount
- Size/color selection
- Quantity selector
- Add to cart/Buy now
- Reviews section

#### T013: Shopping Cart
**Use Case**: Cart management
**Components**: AppBar (1), ListView (1), Card (4), Image (4), Text (12), Button (4)
**Features**:
- Cart items list
- Quantity adjustment
- Remove item
- Subtotal calculation
- Promo code input
- Checkout button

#### T014: Checkout Flow
**Use Case**: Multi-step purchase
**Components**: AppBar (1), TextField (8), RadioGroup (1), Button (2), ProgressBar (1)
**Features**:
- Step 1: Shipping address
- Step 2: Payment method
- Step 3: Order review
- Progress indicator
- Edit steps
- Place order button

#### T015: Order History
**Use Case**: Past orders list
**Components**: AppBar (1), ListView (1), Card (6), Text (18), Badge (6), Button (6)
**Features**:
- Order list by date
- Order status badges
- Order details
- Track shipment
- Reorder button
- Download invoice

### 4. Social (3 Templates)

#### T016: Social Feed
**Use Case**: Timeline/activity feed
**Components**: AppBar (1), ListView (1), Card (10), Avatar (10), Image (5), Text (30)
**Features**:
- User posts
- Like/comment buttons
- Share action
- Timestamp
- Media attachments
- Load more

#### T017: User Profile
**Use Case**: Profile view and edit
**Components**: AppBar (1), Avatar (1), Text (8), Button (3), Tabs (1), ListView (2)
**Features**:
- Profile photo
- Bio and stats
- Follow/Edit button
- Tabs (Posts/Photos/About)
- Content grid
- Settings link

#### T018: Chat Interface
**Use Case**: Messaging conversation
**Components**: AppBar (1), ListView (1), TextField (1), Button (1), Avatar (2), Text (20)
**Features**:
- Message list
- User avatars
- Timestamps
- Read receipts
- Message input
- Send button
- Typing indicator

### 5. Utility (7 Templates)

#### T019: Settings Screen
**Use Case**: App configuration
**Components**: AppBar (1), ListView (1), Switch (5), Slider (2), Text (15), Divider (4)
**Features**:
- Profile section
- Notifications toggle
- Dark mode toggle
- Language selector
- Privacy settings
- About section
- Log out button

#### T020: Onboarding Flow
**Use Case**: First-run tutorial
**Components**: Image (4), Text (12), Button (3), ProgressBar (1)
**Features**:
- Welcome screen
- Feature highlights (3 screens)
- Page indicators
- Skip button
- Next/Get Started
- Swipe gestures

#### T021: Search Results
**Use Case**: Search interface
**Components**: AppBar (1), SearchBar (1), ListView (1), Card (8), Text (16), Badge (3)
**Features**:
- Search input
- Filters/sorting
- Results list
- No results state
- Loading state
- Result count

#### T022: Form Builder
**Use Case**: Complex form entry
**Components**: AppBar (1), TextField (6), Dropdown (2), DatePicker (1), Switch (2), Button (2)
**Features**:
- Text inputs
- Select menus
- Date picker
- Checkboxes
- Form validation
- Submit/Cancel buttons

#### T023: Empty State
**Use Case**: No content placeholder
**Components**: Column (1), Image (1), Text (2), Button (1)
**Features**:
- Illustration
- Primary message
- Secondary message
- Call-to-action button
- Centered layout

#### T024: Error State
**Use Case**: Error handling
**Components**: Column (1), Image (1), Text (3), Button (2)
**Features**:
- Error icon/illustration
- Error title
- Error description
- Retry button
- Go back button

#### T025: Loading State
**Use Case**: Content loading
**Components**: Column (1), Spinner (1), Text (1), Skeleton (5)
**Features**:
- Loading spinner
- Loading message
- Skeleton placeholders
- Progress indicator (optional)

## Usage

### Installation

Templates are included in the AvaElements library:

```kotlin
dependencies {
    implementation("com.augmentalis:avaelements:1.0.0")
}
```

### Using Templates

#### Option 1: Copy DSL Code

```kotlin
// Copy from template library
val loginScreen = AvaUI {
    theme = Themes.Material3Light
    Column {
        fillMaxSize()
        // ... template code
    }
}
```

#### Option 2: Generate from Template

```kotlin
val template = TemplateLibrary.get("T001_MaterialLogin")
val screen = template.generate(
    config = mapOf(
        "appName" to "MyApp",
        "logoUrl" to "assets://logo.png"
    )
)
```

#### Option 3: Android Studio Plugin

Use the AvaUI plugin to insert templates directly:
1. Right-click in code
2. New → AvaUI Template
3. Select template category
4. Configure options
5. Insert code

## Customization

All templates support customization:

### Colors

```kotlin
template.applyTheme(Themes.CustomBrand)
```

### Content

```kotlin
template.replaceText("Welcome Back", "Sign In")
template.replaceImage("logo", "custom_logo")
```

### Layout

```kotlin
template.setProperty("spacing", 24f)
template.setProperty("padding", 16f)
```

## Template Structure

Each template follows this structure:

```
templates/
├── T001_MaterialLogin/
│   ├── template.avaui     # AvaUI DSL
│   ├── generated.kt         # Kotlin Compose
│   ├── preview.png          # Screenshot
│   ├── config.json          # Configuration schema
│   └── README.md            # Documentation
```

## Contributing

To add new templates:

1. Create template directory (TXXX_TemplateName)
2. Write AvaUI DSL code
3. Generate Kotlin Compose code
4. Create preview screenshot
5. Document customization options
6. Submit pull request

## License

MIT License - See LICENSE file

## Support

- Documentation: https://docs.avaui.com/templates
- Examples: https://github.com/augmentalis/avaui-examples
- Issues: https://github.com/augmentalis/avaui/issues

---

**Version**: 1.0.0
**Total Templates**: 25
**Last Updated**: 2025-10-30

Created by Manoj Jhawar, manoj@ideahq.net
