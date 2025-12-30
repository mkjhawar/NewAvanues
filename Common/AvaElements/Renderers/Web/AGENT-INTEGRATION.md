# Agent Integration Guide - Web Renderer Flutter Parity

**Version:** 2.1.0
**Date:** 2025-11-23
**Agent 1 Status:** Complete ✅

---

## Quick Start for Agent 2 & Agent 3

This document provides **minimal, actionable instructions** for implementing Flutter Parity components on Web.

---

## Agent 1 Deliverables (Complete)

### Files Created (7 files, 2,464 LOC)

1. **ReactRenderer.tsx** (352 LOC) - Core renderer with error boundaries
2. **ComponentRegistry.ts** (220 LOC) - Component type-to-renderer mapping
3. **IconResourceManager.ts** (481 LOC) - Icon management (5 types)
4. **ImageLoader.tsx** (330 LOC) - Async image loading with lazy loading
5. **types/index.ts** (836 LOC) - TypeScript types for all 58 components
6. **flutterparity/index.ts** (43 LOC) - Integration point
7. **index.ts** (202 LOC) - Main exports

### Infrastructure Ready

- ✅ Component registry system
- ✅ Type definitions for all 58 components
- ✅ Resource managers (icons, images)
- ✅ Error boundaries
- ✅ Theme support
- ✅ Children rendering utilities

---

## Agent 2: Layout Components (25 Components)

### Scope

**Directory:** `src/flutterparity/layout/`

**Components (18 Layout):**
1. Align
2. Center
3. ConstrainedBox
4. Expanded
5. FittedBox
6. Flex
7. Flexible
8. Padding
9. SizedBox
10. Wrap
11-18. (Basic layout components)

**Subdirectory:** `src/flutterparity/layout/scrolling/`

**Components (7 Scrolling):**
1. CustomScrollView
2. GridViewBuilder
3. ListViewBuilder
4. ListViewSeparated
5. PageView
6. ReorderableListView
7. Slivers

### Implementation Template

```tsx
// src/flutterparity/layout/Center.tsx

import React from 'react';
import type { CenterComponent } from '../../types';
import { renderChildren } from '../../renderer/ReactRenderer';
import { getComponentRegistry, ComponentCategory } from '../../renderer/ComponentRegistry';

/**
 * Center Component Renderer
 * Renders a widget that centers its child within itself
 */
export const CenterRenderer: React.FC<{ component: CenterComponent }> = ({ component }) => {
  const { widthFactor, heightFactor, child } = component;

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: widthFactor ? `${widthFactor * 100}%` : '100%',
        height: heightFactor ? `${heightFactor * 100}%` : '100%',
      }}
    >
      {renderChildren(child)}
    </div>
  );
};

// Auto-register component
const registry = getComponentRegistry();
registry.register('Center', ComponentCategory.LAYOUT, (component) => (
  <CenterRenderer component={component as CenterComponent} />
));

export default CenterRenderer;
```

### Export Pattern

```tsx
// src/flutterparity/layout/index.ts

export { CenterRenderer } from './Center';
export { AlignRenderer } from './Align';
export { PaddingRenderer } from './Padding';
// ... all 18 layout components

// Scrolling components
export * from './scrolling';
```

```tsx
// src/flutterparity/layout/scrolling/index.ts

export { ListViewBuilderRenderer } from './ListViewBuilder';
export { GridViewBuilderRenderer } from './GridViewBuilder';
// ... all 7 scrolling components
```

### Checklist

- [ ] Create 18 layout component files in `src/flutterparity/layout/`
- [ ] Create 7 scrolling component files in `src/flutterparity/layout/scrolling/`
- [ ] Register all components with `ComponentRegistry`
- [ ] Export all components in `index.ts` files
- [ ] Add JSDoc documentation to each component
- [ ] Create unit tests for each component
- [ ] Create Storybook stories (optional but recommended)

---

## Agent 3: Material & Animation Components (33 Components)

### Scope

**Directory:** `src/flutterparity/material/`

**Components (15 Material):**
1. ActionChip
2. CheckboxListTile
3. ChoiceChip
4. CircleAvatar
5. EndDrawer
6. ExpansionTile
7. FilledButton
8. FilterChip
9. InputChip
10. PopupMenuButton
11. RefreshIndicator
12. RichText
13. SelectableText
14. SwitchListTile
15. VerticalDivider

**Directory:** `src/flutterparity/animation/`

**Components (18 Animation):**
1-12. Animated* (AnimatedAlign, AnimatedContainer, AnimatedOpacity, etc.)
13-18. *Transition (FadeTransition, ScaleTransition, SlideTransition, etc.)

### Material Component Template

```tsx
// src/flutterparity/material/ActionChip.tsx

import React from 'react';
import { Chip, Avatar } from '@mui/material';
import type { ActionChipComponent } from '../../types';
import { getComponentRegistry, ComponentCategory } from '../../renderer/ComponentRegistry';

/**
 * ActionChip Component Renderer
 * Material Design action chip
 */
export const ActionChipRenderer: React.FC<{ component: ActionChipComponent }> = ({ component }) => {
  const { label, onPressed, avatar } = component;

  return (
    <Chip
      label={label}
      onClick={onPressed}
      avatar={avatar ? <Avatar src={avatar} /> : undefined}
      clickable
      sx={{ m: 0.5 }}
    />
  );
};

// Auto-register component
const registry = getComponentRegistry();
registry.register('ActionChip', ComponentCategory.MATERIAL, (component) => (
  <ActionChipRenderer component={component as ActionChipComponent} />
));

export default ActionChipRenderer;
```

### Animation Component Template

```tsx
// src/flutterparity/animation/FadeTransition.tsx

import React from 'react';
import { motion } from 'framer-motion';
import type { FadeTransitionComponent } from '../../types';
import { renderChildren } from '../../renderer/ReactRenderer';
import { getComponentRegistry, ComponentCategory } from '../../renderer/ComponentRegistry';

/**
 * FadeTransition Component Renderer
 * Animates the opacity of a widget
 */
export const FadeTransitionRenderer: React.FC<{ component: FadeTransitionComponent }> = ({ component }) => {
  const { opacity, child } = component;

  return (
    <motion.div
      animate={{ opacity }}
      initial={{ opacity: 0 }}
      transition={{ duration: 0.3, ease: 'easeInOut' }}
    >
      {renderChildren(child)}
    </motion.div>
  );
};

// Auto-register component
const registry = getComponentRegistry();
registry.register('FadeTransition', ComponentCategory.ANIMATION, (component) => (
  <FadeTransitionRenderer component={component as FadeTransitionComponent} />
));

export default FadeTransitionRenderer;
```

### Export Pattern

```tsx
// src/flutterparity/material/index.ts

export { ActionChipRenderer } from './ActionChip';
export { ChoiceChipRenderer } from './ChoiceChip';
export { CircleAvatarRenderer } from './CircleAvatar';
// ... all 15 material components
```

```tsx
// src/flutterparity/animation/index.ts

export { AnimatedContainerRenderer } from './AnimatedContainer';
export { FadeTransitionRenderer } from './FadeTransition';
export { ScaleTransitionRenderer } from './ScaleTransition';
// ... all 18 animation components
```

### Checklist

- [ ] Create 15 material component files in `src/flutterparity/material/`
- [ ] Create 18 animation component files in `src/flutterparity/animation/`
- [ ] Register all components with `ComponentRegistry`
- [ ] Export all components in `index.ts` files
- [ ] Add JSDoc documentation to each component
- [ ] Create unit tests for each component
- [ ] Use framer-motion for animations
- [ ] Use @mui/material for Material components

---

## Common Patterns

### 1. Importing Types

```tsx
import type {
  CenterComponent,
  AlignComponent,
  PaddingComponent
} from '../../types';
```

### 2. Rendering Children

```tsx
import { renderChildren } from '../../renderer/ReactRenderer';

// Single child
{renderChildren(component.child)}

// Multiple children
{renderChildren(component.children)}
```

### 3. Component Registration

```tsx
import { getComponentRegistry, ComponentCategory } from '../../renderer/ComponentRegistry';

const registry = getComponentRegistry();
registry.register('Center', ComponentCategory.LAYOUT, (component) => (
  <CenterRenderer component={component as CenterComponent} />
));
```

### 4. Using Material-UI

```tsx
import { Box, Typography, Button } from '@mui/material';

// Use sx prop for styling
<Box sx={{ p: 2, m: 1, bgcolor: 'background.paper' }}>
  <Typography variant="h6">Title</Typography>
  <Button variant="contained">Action</Button>
</Box>
```

### 5. Using Framer Motion

```tsx
import { motion } from 'framer-motion';

<motion.div
  animate={{ opacity: component.opacity }}
  initial={{ opacity: 0 }}
  transition={{ duration: component.duration / 1000, ease: 'easeInOut' }}
>
  {renderChildren(component.child)}
</motion.div>
```

---

## Testing Pattern

```tsx
// Center.test.tsx
import { render, screen } from '@testing-library/react';
import { ReactRenderer } from '@avaelements/web-renderer';
import type { CenterComponent } from '@avaelements/web-renderer';

describe('CenterRenderer', () => {
  it('should render centered child', () => {
    const component: CenterComponent = {
      type: 'Center',
      child: {
        type: 'Text',
        content: 'Hello, World!'
      }
    };

    render(<ReactRenderer component={component} />);
    expect(screen.getByText('Hello, World!')).toBeInTheDocument();
  });
});
```

---

## File Naming Convention

- **Component File:** `PascalCase.tsx` (e.g., `Center.tsx`, `AnimatedContainer.tsx`)
- **Test File:** `PascalCase.test.tsx` (e.g., `Center.test.tsx`)
- **Story File:** `PascalCase.stories.tsx` (e.g., `Center.stories.tsx`)
- **Index File:** `index.ts` (exports)

---

## Quality Standards

### Required

1. ✅ TypeScript strict mode (no `any` types)
2. ✅ JSDoc documentation for all components
3. ✅ Component registration with `ComponentRegistry`
4. ✅ Error handling (let error boundary catch)
5. ✅ Accessibility (use semantic HTML, aria-labels)

### Recommended

1. 📝 Unit tests for each component
2. 📝 Storybook stories for visual testing
3. 📝 Code comments for complex logic
4. 📝 Performance optimization (React.memo where needed)

---

## Dependencies Available

```json
{
  "react": "^18.2.0",
  "react-dom": "^18.2.0",
  "@mui/material": "^5.14.20",
  "@mui/icons-material": "^5.14.20",
  "@emotion/react": "^11.11.1",
  "@emotion/styled": "^11.11.0",
  "framer-motion": "^10.0.0", // For animations
  "clsx": "^2.0.0" // For className management
}
```

---

## Final Checklist (Before Completion)

### Agent 2
- [ ] All 25 components implemented and registered
- [ ] All exports added to `src/flutterparity/layout/index.ts`
- [ ] All tests passing
- [ ] No TypeScript errors
- [ ] No console warnings

### Agent 3
- [ ] All 33 components implemented and registered
- [ ] All exports added to `src/flutterparity/material/index.ts` and `src/flutterparity/animation/index.ts`
- [ ] All tests passing
- [ ] No TypeScript errors
- [ ] No console warnings

### Final Integration
- [ ] Update `src/flutterparity/index.ts` to export all components
- [ ] Run `npm run build` - successful build
- [ ] Run `npm run test` - all tests pass
- [ ] Run `npm run lint` - no errors
- [ ] Run `npm run type-check` - no errors

---

## Getting Help

### Type Definitions
All component interfaces are in `src/types/index.ts` - refer to this for prop types.

### Reference Implementations
- **Android:** `/Volumes/M-Drive/Coding/Avanues/Universal/Libraries/AvaElements/Renderers/Android/`
- **iOS:** `/Volumes/M-Drive/Coding/Avanues/Universal/Libraries/AvaElements/Renderers/iOS/`

### Utilities Available
- `renderChildren(child)` - Render child components
- `getComponentRegistry()` - Get global registry
- `getIconResourceManager()` - Get icon manager
- `ImageLoader` - Image loading component

---

## Time Estimates

### Agent 2 (25 components)
- Layout components (18): ~90 minutes
- Scrolling components (7): ~60 minutes
- Tests: ~30 minutes
- **Total: ~3 hours**

### Agent 3 (33 components)
- Material components (15): ~90 minutes
- Animation components (18): ~120 minutes
- Tests: ~30 minutes
- **Total: ~4 hours**

---

## Contact & Questions

Refer to `FLUTTER-PARITY-ANALYSIS.md` for detailed architecture and implementation examples.

**Good luck! 🚀**
