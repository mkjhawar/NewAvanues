# AvaUI DSL Examples

Complete examples demonstrating the AvaUI DSL for building cross-platform UIs.

## Directory Structure

```
Examples/
├── screens/          # Complete screen definitions
│   ├── LoginScreen.json
│   └── ProfileScreen.json
├── components/       # Reusable component templates
│   └── ProductCard.json
└── themes/          # Theme configurations
    └── DarkTheme.json
```

## Usage

### Generating Code

**Android (Kotlin + Compose):**
```bash
avacode gen -i screens/LoginScreen.json -p android -o LoginScreen.kt
```

**iOS (SwiftUI):**
```bash
avacode gen -i screens/LoginScreen.json -p ios -o LoginScreenView.swift
```

**Web (React + TypeScript):**
```bash
avacode gen -i screens/LoginScreen.json -p web -l typescript -o LoginScreen.tsx
```

### Validating DSL

```bash
avacode validate screens/LoginScreen.json
```

## Screen Examples

### 1. LoginScreen.json
Complete login screen with:
- Email and password text fields
- Remember me checkbox
- Forgot password link
- Sign in button with loading state
- Sign up link

**Features:**
- State management (email, password, rememberMe, isLoading)
- Event handlers (onValueChange, onCheckedChange, onClick)
- Responsive layout with proper spacing
- Icon support
- Input validation ready

**Components used:** 12 components
- Container, Column, Row, Image, Text (x4), TextField (x2), Checkbox, Spacer (x2), Button, Divider

### 2. ProfileScreen.json
User profile screen with:
- Profile header with avatar
- User information display
- Settings list with switches
- Edit profile button
- Logout button

**Features:**
- Read-only state (userName, userEmail)
- Mutable state (notificationsEnabled, darkModeEnabled)
- App bar navigation
- Card-based layout
- List items with icons and switches

**Components used:** 15 components
- Column, AppBar, Card (x2), Image, Text (x4), Button, ListItem (x4), Switch (x2)

## Component Examples

### ProductCard.json
E-commerce product card component with:
- Product image
- Title and category
- Description with truncation
- Star rating
- Price with discount
- Stock badge
- Add to cart button
- Favorite toggle

**Features:**
- Dynamic content via template variables (${product.*})
- Conditional rendering (isFavorite, stock level)
- Complex nested layout
- Rich interactions

**Components used:** 20+ components
- Card, Column, Row, Image, Text (x5), Icon, Chip, Rating, Badge, Button

## Theme Examples

### DarkTheme.json
Complete dark theme specification:

**Color Palette:**
- Primary: #2196F3 (Blue)
- Secondary: #FF5722 (Orange)
- Background: #121212 (Near Black)
- Surface: #1E1E1E (Dark Gray)
- Semantic colors (error, success, warning, info)

**Typography Scale:**
- 11 text styles (h1-h6, body1-2, button, caption, overline)
- Roboto font family
- Responsive sizing (10pt-96pt)
- Proper line heights

**Spacing System:**
- 6 spacing values (xs: 4dp → xxl: 48dp)
- Consistent 8dp grid

**Shape System:**
- Corner radius definitions
- Border configurations
- Component-specific shapes

**Additional:**
- Elevation levels (none → highest)
- Animation durations (fast → slow)

## DSL Structure

### Root Screen Object
```json
{
  "name": "ScreenName",
  "imports": ["package.imports"],
  "state": [/* state variables */],
  "root": {/* component tree */}
}
```

### State Variables
```json
{
  "name": "variableName",
  "type": "String|Int|Boolean|...",
  "initialValue": value,
  "mutable": true|false
}
```

### Component Node
```json
{
  "id": "uniqueId",
  "type": "COMPONENT_TYPE",
  "properties": {/* key-value props */},
  "children": [/* nested components */],
  "events": {/* event handlers */}
}
```

## Component Types Reference

**Foundation (9):**
- BUTTON, CARD, CHECKBOX, CHIP, DIVIDER, IMAGE, LIST_ITEM, TEXT, TEXT_FIELD

**Core (2):**
- COLOR_PICKER, ICON_PICKER

**Basic (6):**
- ICON, LABEL, CONTAINER, ROW, COLUMN, SPACER

**Advanced (18):**
- SWITCH, SLIDER, PROGRESS_BAR, SPINNER, ALERT, DIALOG, TOAST, TOOLTIP
- RADIO, DROPDOWN, DATE_PICKER, TIME_PICKER, SEARCH_BAR, RATING, BADGE
- FILE_UPLOAD, APP_BAR, BOTTOM_NAV

**Total:** 35 component types

## Property Types

- **String:** Text values
- **Int/Double:** Numeric values
- **Boolean:** true/false
- **Enum:** Predefined options (e.g., "PRIMARY", "H1")
- **Template:** Variable references (${variableName})
- **Conditional:** Ternary expressions (${condition ? value : value})

## Event Handlers

Supported events:
- onClick
- onValueChange
- onCheckedChange
- onColorChange
- onIconChange
- onSubmit
- onDelete

## Best Practices

1. **State Management:**
   - Use mutable state for user inputs
   - Use immutable state for constants
   - Initialize with sensible defaults

2. **Layout:**
   - Use Column for vertical layouts
   - Use Row for horizontal layouts
   - Use Container for padding/centering
   - Add Spacer for explicit spacing

3. **Accessibility:**
   - Always provide alt text for images
   - Use semantic component types
   - Add labels to form fields
   - Use proper text variants (H1-H3 for headings)

4. **Performance:**
   - Limit nesting depth (max 5-6 levels)
   - Use maxLines for text truncation
   - Lazy load images when possible

5. **Reusability:**
   - Extract common patterns into components
   - Use template variables for dynamic content
   - Create theme configurations for consistency

## App Store Compliance

✅ The AvaUI DSL is **100% App Store compliant**:
- JSON-based (interpreted as **data**, not code)
- No dynamic code execution
- No eval() or similar mechanisms
- Declarative component composition
- Static type system

The DSL is rendered by native platform frameworks:
- Android: Jetpack Compose
- iOS: SwiftUI
- Web: React + Material-UI

## Next Steps

1. Try generating code from examples
2. Validate DSL files
3. Customize themes
4. Create your own components
5. Build complete applications

## Resources

- AvaUI Documentation: `/docs/`
- Component Reference: `/docs/components/`
- Theme Guide: `/docs/theming/`
- Code Generator Guide: `/docs/codegen/`

**Created by Manoj Jhawar, manoj@ideahq.net**
