# MainAvanues OS Design Tokens

Universal design system tokens for all MainAvanues applications.

## Overview

This document defines the design tokens (colors, spacing, typography, motion, etc.) used across all MainAvanues OS applications including WebAvanue, Ava, AvaConnect, and VoiceOS.

## Color Themes

### Available Theme Palettes

All applications should support these 8 standard themes:

#### 1. Ocean Blue (Default/Recommended)
```css
--primary: #2563eb;
--primary-hover: #1d4ed8;
--surface: #1e293b;
--surface-elevated: #334155;
--surface-high: #475569;
--on-surface: #f1f5f9;
--on-surface-variant: #cbd5e1;
--accent: #3b82f6;
--background: #0f172a;
```

#### 2. Slate Professional (Corporate Neutral)
```css
--primary: #64748b;
--primary-hover: #475569;
--surface: #1e293b;
--surface-elevated: #334155;
--surface-high: #475569;
--on-surface: #f1f5f9;
--on-surface-variant: #cbd5e1;
--accent: #94a3b8;
--background: #0f172a;
```

#### 3. Deep Teal (Professional Calming)
```css
--primary: #0d9488;
--primary-hover: #0f766e;
--surface: #134e4a;
--surface-elevated: #115e59;
--surface-high: #0f766e;
--on-surface: #ccfbf1;
--on-surface-variant: #99f6e4;
--accent: #14b8a6;
--background: #042f2e;
```

#### 4. Navy Corporate (Classic)
```css
--primary: #1e40af;
--primary-hover: #1e3a8a;
--surface: #172554;
--surface-elevated: #1e3a8a;
--surface-high: #1e40af;
--on-surface: #dbeafe;
--on-surface-variant: #bfdbfe;
--accent: #3b82f6;
--background: #0c1e3e;
```

#### 5. Charcoal (Maximum Contrast)
```css
--primary: #71717a;
--primary-hover: #52525b;
--surface: #27272a;
--surface-elevated: #3f3f46;
--surface-high: #52525b;
--on-surface: #fafafa;
--on-surface-variant: #d4d4d8;
--accent: #a1a1aa;
--background: #18181b;
```

#### 6. Warm Taupe (Warm Neutral)
```css
--primary: #78716c;
--primary-hover: #57534e;
--surface: #292524;
--surface-elevated: #44403c;
--surface-high: #57534e;
--on-surface: #fafaf9;
--on-surface-variant: #d6d3d1;
--accent: #a8a29e;
--background: #1c1917;
```

#### 7. Emerald Green (Creative)
```css
--primary: #10b981;
--primary-hover: #059669;
--surface: #1e3a2f;
--surface-elevated: #2d4a3e;
--surface-high: #3d5a4e;
--on-surface: #d1fae5;
--on-surface-variant: #a7f3d0;
--accent: #34d399;
--background: #0f291e;
```

#### 8. Purple Majesty (Branding)
```css
--primary: #8b5cf6;
--primary-hover: #7c3aed;
--surface: #2d1b4e;
--surface-elevated: #3d2565;
--surface-high: #4d3075;
--on-surface: #f3e8ff;
--on-surface-variant: #ddd6fe;
--accent: #a78bfa;
--background: #1e1233;
```

## Universal Settings Tokens

### Glass/Material Effects

```css
/* Glass Mode (toggleable) */
--glass-blur: 0px;              /* 0px (solid) or 40px (glass) */
--glass-opacity: 1;             /* 1 (solid) or 0.7 (glass) */
--glass-border-opacity: 0.1;    /* 0.1 (solid) or 0.15 (glass) */
```

### Border Radius

```css
--radius-sm: 6px;               /* Buttons, small elements */
--radius-md: 10px;              /* Containers, cards */
--radius-lg: 14px;              /* Address bars, panels */
--radius-xl: 18px;              /* Large containers */
```

### Elevation/Shadows

```css
--elevation-1: 0 1px 3px rgba(0, 0, 0, 0.4), 0 1px 2px rgba(0, 0, 0, 0.3);
--elevation-2: 0 3px 6px rgba(0, 0, 0, 0.4), 0 2px 4px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.05);
--elevation-3: 0 6px 12px rgba(0, 0, 0, 0.4), 0 3px 6px rgba(0, 0, 0, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.08);
```

### Motion/Animation

```css
--motion-emphasized: cubic-bezier(0.2, 0, 0, 1);
--motion-standard: cubic-bezier(0.4, 0, 0.2, 1);
--duration-quick: 200ms;
--duration-medium: 300ms;
```

### Depth (3D Transform)

```css
--depth-base: translateZ(0);
--depth-raised: translateZ(2px);
--depth-elevated: translateZ(4px);
```

## Spacing Scale

```css
--space-xs: 4px;
--space-sm: 8px;
--space-md: 12px;
--space-lg: 16px;
--space-xl: 24px;
--space-2xl: 32px;
--space-3xl: 48px;
```

## Typography Scale

### Font Families

```css
--font-sans: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Segoe UI", Roboto, sans-serif;
--font-mono: 'SF Mono', 'Courier New', Consolas, monospace;
```

### Font Sizes

```css
--text-xs: 11px;
--text-sm: 13px;
--text-base: 14px;
--text-lg: 16px;
--text-xl: 18px;
--text-2xl: 24px;
--text-3xl: 30px;
```

### Font Weights

```css
--weight-normal: 400;
--weight-medium: 500;
--weight-semibold: 600;
--weight-bold: 700;
```

## Component-Specific Tokens

### Buttons

```css
.button {
    height: 40px;
    padding: 0 16px;
    border-radius: var(--radius-sm);
    background: var(--surface-elevated);
    color: var(--on-surface);
    font-size: var(--text-base);
    font-weight: var(--weight-medium);
    box-shadow: var(--elevation-2);
    transition: all var(--duration-quick) var(--motion-standard);
}

.button-primary {
    background: var(--primary);
    color: white;
}
```

### Navigation Bars

```css
.nav-bar {
    height: 56px;
    padding: 8px 12px;
    background: rgba(var(--surface-rgb), var(--glass-opacity));
    backdrop-filter: blur(var(--glass-blur)) saturate(120%);
    border-bottom: 1px solid rgba(255, 255, 255, var(--glass-border-opacity));
    box-shadow: var(--elevation-1);
}
```

### Address Bar

```css
.address-bar {
    min-height: 48px;
    padding: 8px 12px;
    background: rgba(var(--surface-elevated-rgb), var(--glass-opacity));
    backdrop-filter: blur(var(--glass-blur)) saturate(120%);
    border: 1px solid rgba(255, 255, 255, calc(var(--glass-border-opacity) + 0.05));
    border-radius: var(--radius-lg);
    box-shadow: var(--elevation-3);
}
```

### Cards

```css
.card {
    padding: var(--space-lg);
    background: var(--surface);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: var(--radius-md);
    box-shadow: var(--elevation-2);
}
```

## Accessibility Tokens

### Focus Indicators

```css
--focus-ring: 0 0 0 3px rgba(59, 130, 246, 0.5);
--focus-ring-width: 3px;
--focus-ring-color: #3b82f6;
```

### High Contrast Mode

```css
.high-contrast {
    --glass-blur: 0px;
    --glass-opacity: 1;
    --elevation-1: 0 0 0 2px var(--on-surface);
    --elevation-2: 0 0 0 2px var(--on-surface);
    --elevation-3: 0 0 0 2px var(--on-surface);
}
```

## Voice-First Tokens

### Voice Hint Styles

```css
.voice-hint {
    padding: 6px 12px;
    background: rgba(59, 130, 246, 0.95);
    color: white;
    font-size: var(--text-xs);
    font-weight: var(--weight-semibold);
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
}
```

### Listening Indicator

```css
.listening-indicator {
    background: rgba(239, 68, 68, 0.95);
    color: white;
    padding: 12px 20px;
    border-radius: 24px;
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
}
```

## Platform-Specific Overrides

### iOS/visionOS

```css
@supports (-webkit-touch-callout: none) {
    --glass-blur: 60px;  /* iOS handles blur better */
    --radius-sm: 8px;    /* iOS uses slightly larger radius */
}
```

### Android

```css
/* Material Design influence */
.android {
    --elevation-1: 0 2px 4px rgba(0, 0, 0, 0.14), 0 3px 4px rgba(0, 0, 0, 0.12);
}
```

### Desktop

```css
@media (hover: hover) and (pointer: fine) {
    --radius-sm: 4px;    /* Sharper corners on desktop */
}
```

## Usage Guidelines

### Theme Integration

```javascript
// Load theme engine
import ThemeEngine from '@shared/ui/theme-engine';

const themeEngine = new ThemeEngine();
themeEngine.switchTheme('ocean');
```

### Component Usage

```css
/* Use tokens, not hardcoded values */
.my-component {
    background: var(--surface);           /* ✅ Good */
    padding: var(--space-md);             /* ✅ Good */
    border-radius: var(--radius-md);      /* ✅ Good */
}

.bad-component {
    background: #1e293b;                  /* ❌ Bad - hardcoded */
    padding: 12px;                        /* ❌ Bad - hardcoded */
    border-radius: 10px;                  /* ❌ Bad - hardcoded */
}
```

### Responsive Tokens

```css
@media (max-width: 768px) {
    :root {
        --space-md: 10px;   /* Reduce on mobile */
        --space-lg: 14px;
    }
}
```

## Design Principles

1. **Consistency** - Use tokens across all apps
2. **Accessibility** - WCAG 2.1 Level AAA compliance
3. **Voice-First** - Every element has voice commands
4. **Performance** - GPU-optimized, 60fps target
5. **Adaptability** - Light/dark themes, high contrast
6. **Persistence** - Settings saved across sessions

## References

- Theme Engine: `docs/shared-libs/ui/theme-engine/`
- Voice Commands: `docs/shared-libs/voice/`
- Demos: `docs/webavanue/demos/`

## Version

**Version:** 1.0.0
**Last Updated:** 2025-11-28
**Status:** Production Ready
