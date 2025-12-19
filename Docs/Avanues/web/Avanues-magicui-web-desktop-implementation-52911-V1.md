# MagicUI Web & Desktop Implementation

Platform: Web (React/TypeScript) + Desktop (Tauri) | Node: 18+ | React: 18+ | Version: 1.0.0

**Base Setup:** See `react-tauri-implementation.md` for Tauri/React foundation.

---

## Tailwind Config (Ocean Theme)

```typescript
// tailwind.config.ts
import type { Config } from 'tailwindcss'

export default {
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        ocean: {
          deep: '#0A1929',
          depth: '#0F172A',
          mid: '#1E293B',
          shallow: '#334155',
        },
        coral: {
          blue: '#3B82F6',
          red: '#EF4444',
        },
        turquoise: '#06B6D4',
        seafoam: '#10B981',
        sunset: '#F59E0B',
        pearl: '#F8FAFC',
        mist: '#E2E8F0',
        storm: '#94A3B8',
        fog: '#475569',
      },
      backdropBlur: {
        glass: '40px',
        toast: '24px',
      },
      borderRadius: {
        window: '16px',
        modal: '24px',
        toast: '12px',
      },
    },
  },
  plugins: [],
} satisfies Config
```

---

## Ocean Theme

```typescript
// src/theme/ocean.ts
export const ocean = {
  deep: '#0A1929',
  depth: '#0F172A',
  mid: '#1E293B',
  shallow: '#334155',

  coralBlue: '#3B82F6',
  turquoise: '#06B6D4',
  seafoam: '#10B981',
  sunset: '#F59E0B',
  coralRed: '#EF4444',

  pearl: '#F8FAFC',
  mist: '#E2E8F0',
  storm: '#94A3B8',
  fog: '#475569',

  surface5: 'rgba(255, 255, 255, 0.05)',
  surface10: 'rgba(255, 255, 255, 0.10)',
  surface15: 'rgba(255, 255, 255, 0.15)',
  surface20: 'rgba(255, 255, 255, 0.20)',
  surface30: 'rgba(255, 255, 255, 0.30)',

  border10: 'rgba(255, 255, 255, 0.10)',
  border20: 'rgba(255, 255, 255, 0.20)',
  border30: 'rgba(255, 255, 255, 0.30)',

  textPrimary: 'rgba(255, 255, 255, 0.90)',
  textSecondary: 'rgba(255, 255, 255, 0.80)',
  textMuted: 'rgba(255, 255, 255, 0.60)',
  textDisabled: 'rgba(255, 255, 255, 0.40)',
} as const
```

---

## CSS Variables

```css
/* src/styles/ocean.css */
:root {
  --ocean-deep: #0A1929;
  --ocean-depth: #0F172A;
  --ocean-mid: #1E293B;
  --ocean-shallow: #334155;

  --coral-blue: #3B82F6;
  --turquoise: #06B6D4;
  --seafoam: #10B981;
  --sunset: #F59E0B;
  --coral-red: #EF4444;

  --surface-5: rgba(255, 255, 255, 0.05);
  --surface-10: rgba(255, 255, 255, 0.10);
  --surface-15: rgba(255, 255, 255, 0.15);
  --surface-20: rgba(255, 255, 255, 0.20);
  --surface-30: rgba(255, 255, 255, 0.30);

  --border-10: rgba(255, 255, 255, 0.10);
  --border-20: rgba(255, 255, 255, 0.20);
  --border-30: rgba(255, 255, 255, 0.30);

  --text-primary: rgba(255, 255, 255, 0.90);
  --text-secondary: rgba(255, 255, 255, 0.80);
  --text-muted: rgba(255, 255, 255, 0.60);
  --text-disabled: rgba(255, 255, 255, 0.40);

  --blur-glass: 40px;
  --blur-toast: 24px;
}
```

---

## Utility: cn

```typescript
// src/utils/cn.ts
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

---

## Ocean Background

```tsx
// src/components/OceanBackground.tsx
interface OceanBackgroundProps {
  showGrid?: boolean
  showAmbientLights?: boolean
  gridSpacing?: number
  gridOpacity?: number
}

export function OceanBackground({
  showGrid = true,
  showAmbientLights = true,
  gridSpacing = 50,
  gridOpacity = 0.1,
}: OceanBackgroundProps) {
  return (
    <div className="fixed inset-0 -z-10">
      <div className="absolute inset-0 bg-gradient-to-b from-ocean-deep via-ocean-depth to-ocean-mid" />

      {showGrid && (
        <div
          className="absolute inset-0"
          style={{
            backgroundImage: `
              linear-gradient(rgba(148, 163, 184, ${gridOpacity}) 1px, transparent 1px),
              linear-gradient(90deg, rgba(148, 163, 184, ${gridOpacity}) 1px, transparent 1px)
            `,
            backgroundSize: `${gridSpacing}px ${gridSpacing}px`,
          }}
        />
      )}

      {showAmbientLights && (
        <>
          <div
            className="absolute w-[400px] h-[400px] rounded-full"
            style={{
              background: 'radial-gradient(circle, rgba(59, 130, 246, 0.2) 0%, transparent 70%)',
              top: '10%',
              left: '25%',
              transform: 'translate(-50%, -50%)',
            }}
          />
          <div
            className="absolute w-[400px] h-[400px] rounded-full"
            style={{
              background: 'radial-gradient(circle, rgba(6, 182, 212, 0.2) 0%, transparent 70%)',
              bottom: '10%',
              right: '25%',
              transform: 'translate(50%, 50%)',
            }}
          />
        </>
      )}
    </div>
  )
}
```

---

## Glassmorphic Surface

```tsx
// src/components/GlassmorphicSurface.tsx
import { ReactNode } from 'react'
import { cn } from '../utils/cn'

interface GlassmorphicSurfaceProps {
  children: ReactNode
  className?: string
  background?: string
  border?: string
  blur?: string
  radius?: string
}

export function GlassmorphicSurface({
  children,
  className,
  background = 'bg-white/10',
  border = 'border-white/20',
  blur = 'backdrop-blur-glass',
  radius = 'rounded-window',
}: GlassmorphicSurfaceProps) {
  return (
    <div className={cn(background, border, blur, radius, 'border', className)}>
      {children}
    </div>
  )
}
```

---

## Data Table

```tsx
// src/components/DataTable.tsx
import { ReactNode } from 'react'
import { cn } from '../utils/cn'
import { GlassmorphicSurface } from './GlassmorphicSurface'

interface Column {
  key: string
  label: string
  width?: string
}

interface DataTableProps<T> {
  columns: Column[]
  data: T[]
  keyExtractor: (item: T) => string
  renderCell: (item: T, column: Column) => ReactNode
}

export function DataTable<T>({ columns, data, keyExtractor, renderCell }: DataTableProps<T>) {
  return (
    <GlassmorphicSurface background="bg-transparent" className="overflow-hidden">
      <table className="w-full">
        <thead>
          <tr className="bg-white/10 h-16">
            {columns.map((col) => (
              <th
                key={col.key}
                className="px-4 py-3 text-left text-white/90 font-medium"
                style={{ width: col.width }}
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((item, index) => (
            <tr
              key={keyExtractor(item)}
              className={cn(
                'h-14 border-b border-white/10',
                index % 2 === 0 ? 'bg-white/5' : 'bg-transparent'
              )}
            >
              {columns.map((col) => (
                <td key={col.key} className="px-4 py-3 text-white/80">
                  {renderCell(item, col)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </GlassmorphicSurface>
  )
}
```

---

## Todo List

```tsx
// src/components/TodoList.tsx
import { cn } from '../utils/cn'
import { GlassmorphicSurface } from './GlassmorphicSurface'

type TaskStatus = 'pending' | 'in_progress' | 'completed'
type TaskPriority = 'low' | 'medium' | 'high'

interface Task {
  id: string
  title: string
  status: TaskStatus
  priority: TaskPriority
}

interface TodoListProps {
  tasks: Task[]
  onToggle: (task: Task) => void
}

export function TodoList({ tasks, onToggle }: TodoListProps) {
  return (
    <GlassmorphicSurface background="bg-white/5" className="p-2">
      <ul className="divide-y divide-white/10">
        {tasks.map((task) => (
          <TodoItem key={task.id} task={task} onToggle={() => onToggle(task)} />
        ))}
      </ul>
    </GlassmorphicSurface>
  )
}

function TodoItem({ task, onToggle }: { task: Task; onToggle: () => void }) {
  const statusColor = {
    completed: 'text-white/60',
    in_progress: 'text-white/90',
    pending: 'text-white/80',
  }[task.status]

  const priorityColor = {
    high: 'bg-coral-red/20 text-coral-red',
    medium: 'bg-sunset/20 text-sunset',
    low: 'bg-coral-blue/20 text-coral-blue',
  }[task.priority]

  return (
    <li className="flex items-center gap-3 p-4 min-h-[72px]">
      <button
        onClick={onToggle}
        className={cn(
          'w-6 h-6 rounded-full border-2 flex items-center justify-center',
          task.status === 'completed' ? 'bg-seafoam border-seafoam' : 'border-white/20'
        )}
      >
        {task.status === 'completed' && (
          <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
            <path d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" />
          </svg>
        )}
      </button>

      <div className="flex-1">
        <p className={cn(statusColor, task.status === 'completed' && 'line-through')}>
          {task.title}
        </p>
        <div className="flex items-center gap-2 mt-1">
          <span className={cn('px-2 py-0.5 rounded text-xs', priorityColor)}>
            {task.priority}
          </span>
          <StatusIndicator status={task.status} />
          <span className="text-xs text-white/60">{task.status.replace('_', ' ')}</span>
        </div>
      </div>
    </li>
  )
}

function StatusIndicator({ status }: { status: TaskStatus }) {
  const color = {
    completed: 'bg-seafoam',
    in_progress: 'bg-coral-blue',
    pending: 'bg-white/60',
  }[status]

  return <span className={cn('w-1.5 h-1.5 rounded-full', color)} />
}
```

---

## Modal Dialog

```tsx
// src/components/OceanDialog.tsx
import { Fragment } from 'react'
import { Dialog, Transition } from '@headlessui/react'
import { XMarkIcon } from '@heroicons/react/24/outline'
import { GlassmorphicSurface } from './GlassmorphicSurface'

interface OceanDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  message: string
  confirmText?: string
  dismissText?: string
}

export function OceanDialog({
  open,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = 'Confirm',
  dismissText = 'Cancel',
}: OceanDialogProps) {
  return (
    <Transition show={open} as={Fragment}>
      <Dialog onClose={onClose} className="relative z-50">
        <Transition.Child
          as={Fragment}
          enter="ease-out duration-200"
          enterFrom="opacity-0"
          enterTo="opacity-100"
          leave="ease-in duration-150"
          leaveFrom="opacity-100"
          leaveTo="opacity-0"
        >
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm" />
        </Transition.Child>

        <div className="fixed inset-0 flex items-center justify-center p-4">
          <Transition.Child
            as={Fragment}
            enter="ease-out duration-200"
            enterFrom="opacity-0 scale-95"
            enterTo="opacity-100 scale-100"
            leave="ease-in duration-150"
            leaveFrom="opacity-100 scale-100"
            leaveTo="opacity-0 scale-95"
          >
            <Dialog.Panel className="w-full max-w-[600px]">
              <GlassmorphicSurface background="bg-white/20" border="border-white/30" radius="rounded-modal">
                <div className="flex items-center justify-between p-5 border-b border-white/10">
                  <Dialog.Title className="text-xl font-medium text-white/90">{title}</Dialog.Title>
                  <button onClick={onClose} className="text-white/60 hover:text-white/90">
                    <XMarkIcon className="w-6 h-6" />
                  </button>
                </div>

                <div className="p-6">
                  <p className="text-white/80">{message}</p>
                </div>

                <div className="flex justify-end gap-3 p-5 border-t border-white/10">
                  <button onClick={onClose} className="px-4 py-2 text-white/60 hover:text-white/90">
                    {dismissText}
                  </button>
                  <button
                    onClick={onConfirm}
                    className="px-4 py-2 bg-coral-blue rounded-lg text-white hover:bg-coral-blue/90"
                  >
                    {confirmText}
                  </button>
                </div>
              </GlassmorphicSurface>
            </Dialog.Panel>
          </Transition.Child>
        </div>
      </Dialog>
    </Transition>
  )
}
```

---

## Toast

```tsx
// src/components/OceanToast.tsx
import { XMarkIcon } from '@heroicons/react/24/outline'
import {
  InformationCircleIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
  XCircleIcon,
} from '@heroicons/react/24/solid'
import { cn } from '../utils/cn'
import { GlassmorphicSurface } from './GlassmorphicSurface'

type ToastType = 'info' | 'success' | 'warning' | 'error'

interface OceanToastProps {
  message: string
  type?: ToastType
  onDismiss: () => void
}

const toastConfig = {
  info: { icon: InformationCircleIcon, color: 'border-coral-blue text-coral-blue' },
  success: { icon: CheckCircleIcon, color: 'border-seafoam text-seafoam' },
  warning: { icon: ExclamationTriangleIcon, color: 'border-sunset text-sunset' },
  error: { icon: XCircleIcon, color: 'border-coral-red text-coral-red' },
}

export function OceanToast({ message, type = 'info', onDismiss }: OceanToastProps) {
  const { icon: Icon, color } = toastConfig[type]

  return (
    <GlassmorphicSurface
      background="bg-white/30"
      border="border-transparent"
      blur="backdrop-blur-toast"
      radius="rounded-toast"
      className={cn('max-w-[400px] border-l-4', color)}
    >
      <div className="flex items-center gap-3 px-5 py-3">
        <Icon className={cn('w-5 h-5 flex-shrink-0', color.split(' ')[1])} />
        <p className="flex-1 text-white/90">{message}</p>
        <button onClick={onDismiss} className="text-white/60 hover:text-white/90">
          <XMarkIcon className="w-4 h-4" />
        </button>
      </div>
    </GlassmorphicSurface>
  )
}
```

---

## Layout: Dashboard

```tsx
// src/layouts/Dashboard.tsx
import { OceanBackground } from '../components/OceanBackground'
import { GlassmorphicSurface } from '../components/GlassmorphicSurface'

export function DashboardLayout() {
  return (
    <div className="min-h-screen">
      <OceanBackground />

      <main className="p-6 space-y-6">
        <GlassmorphicSurface background="bg-transparent">
          <div className="bg-gradient-to-r from-coral-blue/20 to-turquoise/20 p-8 rounded-window">
            <h1 className="text-3xl font-bold text-white/90">Enterprise Dashboard</h1>
            <p className="mt-2 text-white/60">Welcome back</p>
          </div>
        </GlassmorphicSurface>

        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {['24', '156', '94%', '38'].map((value, i) => (
            <MetricCard key={i} value={value} />
          ))}
        </div>
      </main>
    </div>
  )
}

function MetricCard({ value }: { value: string }) {
  return (
    <GlassmorphicSurface background="bg-white/5" className="p-6">
      <p className="text-2xl font-semibold text-white/90">{value}</p>
    </GlassmorphicSurface>
  )
}
```

---

## Tauri TitleBar (Ocean)

```tsx
// src/components/OceanTitleBar.tsx
import { minimize, maximize, close, startDragging, isTauri } from '../tauri/window'

export function OceanTitleBar() {
  if (!isTauri()) return null

  return (
    <div
      className="h-8 flex items-center justify-between px-3 bg-ocean-deep select-none"
      onMouseDown={startDragging}
    >
      <span className="text-white/60 text-sm">MagicUI App</span>

      <div className="flex gap-2" onMouseDown={(e) => e.stopPropagation()}>
        <button onClick={minimize} className="w-3 h-3 rounded-full bg-sunset hover:opacity-80" />
        <button onClick={maximize} className="w-3 h-3 rounded-full bg-seafoam hover:opacity-80" />
        <button onClick={close} className="w-3 h-3 rounded-full bg-coral-red hover:opacity-80" />
      </div>
    </div>
  )
}
```

---

## Tauri Window Config

```json
// src-tauri/tauri.conf.json (window)
{
  "tauri": {
    "windows": [
      {
        "title": "MagicUI App",
        "width": 1200,
        "height": 800,
        "minWidth": 800,
        "minHeight": 600,
        "transparent": true,
        "decorations": false
      }
    ]
  }
}
```

```rust
// src-tauri/src/main.rs
fn main() {
    tauri::Builder::default()
        .setup(|app| {
            let window = app.get_window("main").unwrap();
            window.set_decorations(false)?;
            window.set_transparent(true)?;
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error running tauri app");
}
```

---

## Accessibility

```tsx
// Touch targets: 48px minimum
<button className="min-h-[48px] min-w-[48px]">Action</button>

// Focus states
<button className="focus:outline-none focus:ring-2 focus:ring-coral-blue focus:ring-offset-2 focus:ring-offset-ocean-deep">
  Button
</button>

// Screen reader
<button aria-label="Close dialog">
  <XMarkIcon className="w-6 h-6" aria-hidden="true" />
</button>
```

---

## Examples

`apps/web/src/examples/`

---

**Platform:** Web (React) + Desktop (Tauri) | **Node:** 18+ | **React:** 18+ | **Version:** 1.0.0 | **Updated:** 2025-11-29
