# AvaElements Web Renderer

React + Material-UI renderer for AvaElements Phase 1 components.

## Overview

This package provides React component wrappers for all 13 Phase 1 AvaElements components, using Material-UI for consistent design.

## Installation

```bash
npm install @avaelements/web-renderer
```

### Peer Dependencies

You must also install these peer dependencies:

```bash
npm install react react-dom @mui/material @emotion/react @emotion/styled
```

## Usage

### Basic Usage

```tsx
import { AvaElementsRenderer } from '@avaelements/web-renderer';
import type { ButtonComponent } from '@avaelements/web-renderer';

const buttonComponent: ButtonComponent = {
  type: 'Button',
  text: 'Click Me',
  enabled: true,
  onClick: () => console.log('Clicked!'),
};

function App() {
  return <AvaElementsRenderer component={buttonComponent} />;
}
```

### Individual Component Usage

```tsx
import { RenderButton, RenderTextField } from '@avaelements/web-renderer';

function MyForm() {
  return (
    <>
      <RenderButton
        component={{
          type: 'Button',
          text: 'Submit',
          enabled: true,
          onClick: handleSubmit,
        }}
      />
      <RenderTextField
        component={{
          type: 'TextField',
          value: name,
          label: 'Name',
          enabled: true,
          onChange: setName,
        }}
      />
    </>
  );
}
```

## Supported Components

### Form Components (4)
- **Button** - Material Button with click handlers
- **TextField** - Material OutlinedTextField with change handlers
- **Checkbox** - Material Checkbox with FormControlLabel
- **Switch** - Material Switch with FormControlLabel

### Display Components (3)
- **Text** - Typography component with style variants
- **Image** - Responsive image with alt text
- **Icon** - Material Icon with customizable size/color

### Layout Components (4)
- **Container** - Box with padding and elevation
- **Row** - Flex row with gap spacing
- **Column** - Flex column with gap spacing
- **Card** - Material Card with elevation

### Navigation & Data Components (2)
- **ScrollView** - Scrollable container with fixed height
- **List** - Material List with items

## Theme Support

You can pass a custom theme object to any renderer:

```tsx
const customTheme = {
  colors: {
    primary: '#1976d2',
    secondary: '#dc004e',
    background: '#ffffff',
    surface: '#f5f5f5',
    error: '#f44336',
  },
  typography: {},
  spacing: {},
};

<AvaElementsRenderer component={component} theme={customTheme} />
```

## TypeScript Support

Full TypeScript support with type definitions for all components.

```tsx
import type {
  ButtonComponent,
  TextFieldComponent,
  CheckboxComponent,
  // ... other component types
} from '@avaelements/web-renderer';
```

## Build

```bash
npm install
npm run build
```

Output will be in `dist/` directory.

## Development

```bash
npm run watch
```

## Testing

```bash
npm test
```

## License

Proprietary - Manoj Jhawar (manoj@ideahq.net)

## Related

- **Android Renderer**: Jetpack Compose implementation
- **iOS Renderer**: SwiftUI implementation
- **Desktop Renderer**: Compose Desktop implementation
