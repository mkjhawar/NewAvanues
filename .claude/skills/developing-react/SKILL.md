---
name: developing-react
description: Develops React applications with modern hooks, patterns, and artifact bundling. Use for React components, state management, hooks, context, routing, and production-ready web artifacts.
---

# React Development

## Tech Stack

| Component | Tech | Version |
|-----------|------|---------|
| Framework | React | 18+ |
| Language | TypeScript | 5.0+ |
| Styling | Tailwind CSS | 3.4+ |
| Components | shadcn/ui | Latest |
| State | Zustand / TanStack Query | Latest |
| Routing | React Router | 6+ |
| Build | Vite | Latest |
| Bundling | Parcel (artifacts) | Latest |

## Structure

```
src/
├── components/
│   ├── ui/             # Reusable UI (shadcn)
│   └── features/       # Feature components
├── hooks/              # Custom hooks
├── stores/             # State stores
├── services/           # API calls
├── types/              # TypeScript types
└── utils/              # Helpers
```

## Component Patterns

| Pattern | Implementation |
|---------|----------------|
| Functional | `const Button: FC<Props> = ({ }) => {}` |
| Props | Destructure, type with interface |
| Children | `PropsWithChildren<Props>` |
| Composition | Prefer composition over inheritance |

## Hooks

| Hook | Usage |
|------|-------|
| useState | Local component state |
| useEffect | Side effects, cleanup |
| useMemo | Expensive computations |
| useCallback | Stable function references |
| useRef | DOM refs, mutable values |

## Rules

| Rule | Requirement |
|------|-------------|
| Keys | Unique, stable keys in lists |
| Dependencies | Complete dependency arrays |
| Cleanup | Return cleanup in useEffect |
| Memoization | Only when measured benefit |

## shadcn/ui Integration

```bash
# Initialize
npx shadcn-ui@latest init

# Add components
npx shadcn-ui@latest add button card dialog
```

```tsx
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader } from "@/components/ui/card"

export function Feature() {
  return (
    <Card>
      <CardHeader>Title</CardHeader>
      <CardContent>
        <Button variant="default">Click</Button>
      </CardContent>
    </Card>
  )
}
```

## Web Artifact Bundling

For self-contained HTML artifacts (demos, prototypes):

### Project Setup
```bash
# Initialize with Vite + React + TypeScript + Tailwind + shadcn
npm create vite@latest my-artifact -- --template react-ts
cd my-artifact
npm install
npx shadcn-ui@latest init
```

### Bundle to Single HTML
```bash
# Using Parcel for single-file output
npm install -D parcel
npx parcel build src/index.html --no-source-maps
```

### Artifact Structure
```tsx
// Self-contained artifact component
import React from 'react'
import { Button } from '@/components/ui/button'

export default function Artifact() {
  const [state, setState] = React.useState(0)

  return (
    <div className="min-h-screen bg-slate-900 text-white p-8">
      <h1 className="text-4xl font-bold">My Artifact</h1>
      <Button onClick={() => setState(s => s + 1)}>
        Count: {state}
      </Button>
    </div>
  )
}
```

## Design Guidelines

### Avoid "AI Slop"

| Anti-Pattern | Better Approach |
|--------------|-----------------|
| Excessive centering | Use asymmetric layouts |
| Purple gradients | Pick distinctive palette |
| Uniform rounded corners | Vary border-radius |
| Inter font everywhere | Use characterful fonts |

### Design Excellence

| Principle | Implementation |
|-----------|----------------|
| Typography | Space Grotesk, Clash Display, distinctive choices |
| Color | CSS variables, cohesive palette, dark/light |
| Motion | Framer Motion, orchestrated reveals |
| Layout | CSS Grid, intentional whitespace |

## State Management

### Zustand (Simple)
```tsx
import { create } from 'zustand'

interface Store {
  count: number
  increment: () => void
}

const useStore = create<Store>((set) => ({
  count: 0,
  increment: () => set((s) => ({ count: s.count + 1 })),
}))
```

### TanStack Query (Server State)
```tsx
import { useQuery, useMutation } from '@tanstack/react-query'

function Component() {
  const { data, isLoading } = useQuery({
    queryKey: ['items'],
    queryFn: fetchItems,
  })

  const mutation = useMutation({
    mutationFn: createItem,
    onSuccess: () => queryClient.invalidateQueries(['items']),
  })
}
```

## Animation (Framer Motion)

```tsx
import { motion, AnimatePresence } from 'framer-motion'

// Staggered children
<motion.div
  initial="hidden"
  animate="visible"
  variants={{
    visible: { transition: { staggerChildren: 0.1 } }
  }}
>
  {items.map(item => (
    <motion.div
      key={item.id}
      variants={{
        hidden: { opacity: 0, y: 20 },
        visible: { opacity: 1, y: 0 }
      }}
    />
  ))}
</motion.div>
```

## Quality Gates

| Gate | Target |
|------|--------|
| Bundle size | <200KB gzipped |
| Re-renders | Minimize with React DevTools |
| Accessibility | All interactive elements |
| Test coverage | 90%+ |
| Lighthouse | 90+ score |
| Core Web Vitals | Pass all |

## Testing

```tsx
import { render, screen, fireEvent } from '@testing-library/react'
import { Button } from './Button'

test('renders and responds to click', () => {
  const onClick = vi.fn()
  render(<Button onClick={onClick}>Click me</Button>)

  fireEvent.click(screen.getByText('Click me'))
  expect(onClick).toHaveBeenCalled()
})
```

## Resources

- shadcn/ui: https://ui.shadcn.com/docs/components
- Radix UI: https://www.radix-ui.com/docs/primitives
- Tailwind: https://tailwindcss.com/docs
- Framer Motion: https://www.framer.com/motion/
