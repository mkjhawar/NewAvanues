---
name: developing-web
description: Develops web applications with HTML, CSS, JavaScript/TypeScript. Use for DOM manipulation, CSS layouts, responsive design, accessibility, web APIs, and browser compatibility.
---

# Web Development

## Tech Stack

| Component | Tech | Standard |
|-----------|------|----------|
| Markup | HTML5 | Semantic elements |
| Styling | CSS3 | Flexbox/Grid |
| Scripting | TypeScript | 5.0+ preferred |
| Build | Vite | Latest |
| Package | npm/pnpm | Latest |

## Structure

```
src/
├── index.html          # Entry HTML
├── styles/
│   ├── main.css        # Global styles
│   └── components/     # Component styles
├── scripts/
│   ├── main.ts         # Entry script
│   └── modules/        # Feature modules
└── assets/             # Images, fonts
```

## HTML Rules

| Rule | Implementation |
|------|----------------|
| Semantic | `<header>`, `<main>`, `<article>`, `<nav>` |
| Accessibility | ARIA labels, alt text, roles |
| Forms | Labels, validation, autocomplete |
| Meta | viewport, charset, description |

## CSS Patterns

| Pattern | Usage |
|---------|-------|
| Layout | CSS Grid for 2D, Flexbox for 1D |
| Variables | `--color-primary: #007bff` |
| Responsive | Mobile-first, `min-width` queries |
| Units | `rem` for text, `px` for borders |

## TypeScript Rules

| Rule | Implementation |
|------|----------------|
| Strict mode | `"strict": true` |
| Types | Interface for objects, Type for unions |
| DOM | Type assertions `as HTMLElement` |
| Events | Typed event handlers |

## Quality Gates

| Gate | Target |
|------|--------|
| Lighthouse | 90+ all categories |
| Accessibility | WCAG 2.1 AA |
| Browser support | Last 2 versions |
