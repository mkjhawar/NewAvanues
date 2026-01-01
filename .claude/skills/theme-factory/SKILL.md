---
name: theme-factory
description: Generate and apply professional themes to artifacts, presentations, documents, and web pages. Use when styling or theming is needed.
---

# Theme Factory

## Available Themes

| Theme | Primary | Accent | Fonts |
|-------|---------|--------|-------|
| Ocean Depths | #1a365d | #63b3ed | Merriweather, Open Sans |
| Sunset Boulevard | #c53030 | #fbd38d | Playfair Display, Lato |
| Forest Canopy | #276749 | #9ae6b4 | Libre Baskerville, Source Sans |
| Modern Minimalist | #2d3748 | #e2e8f0 | Inter, Inter |
| Golden Hour | #b7791f | #faf089 | Cormorant, Montserrat |
| Arctic Frost | #2c5282 | #bee3f8 | Raleway, Nunito |
| Desert Rose | #97266d | #fbb6ce | Josefin Sans, Quicksand |
| Tech Innovation | #4c51bf | #c3dafe | Space Grotesk, IBM Plex Sans |
| Botanical Garden | #2f855a | #c6f6d5 | Crimson Text, Work Sans |
| Midnight Galaxy | #1a202c | #805ad5 | Poppins, DM Sans |

## Theme Structure

```css
:root {
  /* Primary Colors */
  --color-primary: #1a365d;
  --color-primary-light: #2c5282;
  --color-primary-dark: #1a202c;

  /* Accent Colors */
  --color-accent: #63b3ed;
  --color-accent-light: #90cdf4;
  --color-accent-dark: #3182ce;

  /* Neutrals */
  --color-bg: #ffffff;
  --color-text: #2d3748;
  --color-muted: #718096;

  /* Typography */
  --font-heading: 'Merriweather', serif;
  --font-body: 'Open Sans', sans-serif;
}
```

## Apply to Web

```html
<link href="https://fonts.googleapis.com/css2?family=Merriweather:wght@700&family=Open+Sans&display=swap" rel="stylesheet">

<style>
  body {
    font-family: var(--font-body);
    color: var(--color-text);
    background: var(--color-bg);
  }

  h1, h2, h3 {
    font-family: var(--font-heading);
    color: var(--color-primary);
  }

  .accent {
    color: var(--color-accent);
  }

  .button-primary {
    background: var(--color-primary);
    color: white;
  }
</style>
```

## Apply to Tailwind

```javascript
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#1a365d',
          light: '#2c5282',
          dark: '#1a202c',
        },
        accent: {
          DEFAULT: '#63b3ed',
          light: '#90cdf4',
          dark: '#3182ce',
        },
      },
      fontFamily: {
        heading: ['Merriweather', 'serif'],
        body: ['Open Sans', 'sans-serif'],
      },
    },
  },
}
```

## Apply to Presentations

```
Background: #ffffff or #1a202c (dark)
Title: Primary color, heading font
Subtitle: Muted color, body font
Accent elements: Accent color
Charts: Primary + Accent palette
```

## Custom Theme Generation

When default themes don't fit, generate custom:

```
1. User describes aesthetic (e.g., "warm and professional")
2. Generate color palette (primary, accent, neutrals)
3. Select complementary fonts (heading, body)
4. Provide CSS variables
5. Include usage examples
```

## Color Accessibility

| Contrast | Requirement |
|----------|-------------|
| Normal text | 4.5:1 minimum |
| Large text | 3:1 minimum |
| UI elements | 3:1 minimum |

```
# Check contrast
Primary (#1a365d) on white (#ffffff): ✅ 11.4:1
Accent (#63b3ed) on white: ⚠️ 2.7:1 (use for decorative only)
```

## Usage Patterns

| Context | Application |
|---------|-------------|
| Dashboard | Primary nav, accent highlights |
| Landing page | Hero with gradient, accent CTAs |
| Document | Primary headings, body in neutral |
| Presentation | Primary title, accent charts |

## Font Pairing Rules

| Principle | Example |
|-----------|---------|
| Contrast | Serif heading + Sans body |
| Weight | Bold heading + Regular body |
| Character | Distinctive heading + Neutral body |
| Consistency | Same font, different weights |
