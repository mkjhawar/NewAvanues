# React + Tauri Implementation

Platform: Web (React/TypeScript) + Desktop (Tauri) | Node: 18+ | React: 18+ | Tauri: 1.5+ | Version: 1.0.0

---

## Project Setup

```bash
# Create new Tauri + React project
npm create tauri-app@latest my-app -- --template react-ts
cd my-app
npm install
```

---

## Dependencies

```json
// package.json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "@tauri-apps/api": "^1.5.0"
  },
  "devDependencies": {
    "typescript": "^5.3.0",
    "@tauri-apps/cli": "^1.5.0",
    "vite": "^5.0.0",
    "@vitejs/plugin-react": "^4.2.0",
    "tailwindcss": "^3.4.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0"
  }
}
```

---

## Tailwind Setup

```bash
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

```typescript
// tailwind.config.ts
import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {},
  },
  plugins: [],
} satisfies Config
```

```css
/* src/index.css */
@tailwind base;
@tailwind components;
@tailwind utilities;
```

---

## Vite Config

```typescript
// vite.config.ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  clearScreen: false,
  server: {
    port: 1420,
    strictPort: true,
  },
  envPrefix: ['VITE_', 'TAURI_'],
  build: {
    target: process.env.TAURI_PLATFORM === 'windows' ? 'chrome105' : 'safari13',
    minify: !process.env.TAURI_DEBUG ? 'esbuild' : false,
    sourcemap: !!process.env.TAURI_DEBUG,
  },
})
```

---

## Tauri Config

```json
// src-tauri/tauri.conf.json
{
  "build": {
    "beforeBuildCommand": "npm run build",
    "beforeDevCommand": "npm run dev",
    "devPath": "http://localhost:1420",
    "distDir": "../dist"
  },
  "package": {
    "productName": "My App",
    "version": "0.1.0"
  },
  "tauri": {
    "allowlist": {
      "all": false,
      "window": {
        "all": true
      },
      "shell": {
        "open": true
      },
      "fs": {
        "all": true,
        "scope": ["$APP/*", "$RESOURCE/*"]
      },
      "path": {
        "all": true
      },
      "os": {
        "all": true
      }
    },
    "bundle": {
      "active": true,
      "icon": [
        "icons/32x32.png",
        "icons/128x128.png",
        "icons/128x128@2x.png",
        "icons/icon.icns",
        "icons/icon.ico"
      ],
      "identifier": "com.yourcompany.app",
      "targets": "all"
    },
    "windows": [
      {
        "title": "My App",
        "width": 1200,
        "height": 800,
        "minWidth": 800,
        "minHeight": 600,
        "resizable": true,
        "fullscreen": false
      }
    ]
  }
}
```

---

## Rust Main

```rust
// src-tauri/src/main.rs
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}!", name)
}

fn main() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![greet])
        .run(tauri::generate_context!())
        .expect("error running tauri app");
}
```

---

## Platform Detection

```typescript
// src/utils/platform.ts
import { type, platform, arch } from '@tauri-apps/api/os'

export type Platform = 'web' | 'macos' | 'windows' | 'linux'

export function isTauri(): boolean {
  return '__TAURI__' in window
}

export async function getPlatform(): Promise<Platform> {
  if (!isTauri()) return 'web'

  const osType = await type()
  switch (osType) {
    case 'Darwin': return 'macos'
    case 'Windows_NT': return 'windows'
    case 'Linux': return 'linux'
    default: return 'web'
  }
}

export async function getSystemInfo() {
  if (!isTauri()) return null

  return {
    type: await type(),
    platform: await platform(),
    arch: await arch(),
  }
}
```

---

## Window API

```typescript
// src/tauri/window.ts
import { appWindow, WebviewWindow } from '@tauri-apps/api/window'

// Main window controls
export const minimize = () => appWindow.minimize()
export const maximize = () => appWindow.toggleMaximize()
export const close = () => appWindow.close()
export const setFullscreen = (fullscreen: boolean) => appWindow.setFullscreen(fullscreen)
export const startDragging = () => appWindow.startDragging()

// Window state
export const isMaximized = () => appWindow.isMaximized()
export const isFullscreen = () => appWindow.isFullscreen()
export const isVisible = () => appWindow.isVisible()

// Create new window
export function createWindow(label: string, options: {
  url: string
  title?: string
  width?: number
  height?: number
}) {
  return new WebviewWindow(label, {
    url: options.url,
    title: options.title ?? label,
    width: options.width ?? 800,
    height: options.height ?? 600,
  })
}

// Listen to window events
export function onResized(callback: () => void) {
  return appWindow.onResized(callback)
}

export function onMoved(callback: () => void) {
  return appWindow.onMoved(callback)
}

export function onCloseRequested(callback: () => void) {
  return appWindow.onCloseRequested(callback)
}
```

---

## Invoke Commands

```typescript
// src/tauri/commands.ts
import { invoke } from '@tauri-apps/api/tauri'

// Type-safe command invocation
export async function greet(name: string): Promise<string> {
  return invoke('greet', { name })
}

// Generic invoke wrapper with error handling
export async function invokeCommand<T>(
  command: string,
  args?: Record<string, unknown>
): Promise<T> {
  try {
    return await invoke<T>(command, args)
  } catch (error) {
    console.error(`Command ${command} failed:`, error)
    throw error
  }
}
```

---

## File System

```typescript
// src/tauri/fs.ts
import {
  readTextFile,
  writeTextFile,
  readDir,
  createDir,
  removeFile,
  removeDir,
  exists,
  BaseDirectory,
} from '@tauri-apps/api/fs'
import { appDataDir, join } from '@tauri-apps/api/path'

// Read file
export async function readFile(path: string): Promise<string> {
  return readTextFile(path, { dir: BaseDirectory.AppData })
}

// Write file
export async function writeFile(path: string, content: string): Promise<void> {
  await writeTextFile(path, content, { dir: BaseDirectory.AppData })
}

// Check if file exists
export async function fileExists(path: string): Promise<boolean> {
  return exists(path, { dir: BaseDirectory.AppData })
}

// List directory
export async function listDir(path: string) {
  return readDir(path, { dir: BaseDirectory.AppData, recursive: false })
}

// Create directory
export async function makeDir(path: string): Promise<void> {
  await createDir(path, { dir: BaseDirectory.AppData, recursive: true })
}

// Delete file
export async function deleteFile(path: string): Promise<void> {
  await removeFile(path, { dir: BaseDirectory.AppData })
}

// Get app data path
export async function getAppDataPath(): Promise<string> {
  return appDataDir()
}

// Join paths
export async function joinPaths(...paths: string[]): Promise<string> {
  let result = paths[0]
  for (let i = 1; i < paths.length; i++) {
    result = await join(result, paths[i])
  }
  return result
}
```

---

## Event System

```typescript
// src/tauri/events.ts
import { emit, listen, once } from '@tauri-apps/api/event'
import type { UnlistenFn } from '@tauri-apps/api/event'

// Emit event to backend
export async function emitEvent<T>(event: string, payload?: T): Promise<void> {
  await emit(event, payload)
}

// Listen to events from backend
export async function onEvent<T>(
  event: string,
  callback: (payload: T) => void
): Promise<UnlistenFn> {
  return listen<T>(event, (e) => callback(e.payload))
}

// Listen once
export async function onceEvent<T>(
  event: string,
  callback: (payload: T) => void
): Promise<UnlistenFn> {
  return once<T>(event, (e) => callback(e.payload))
}
```

---

## Custom Titlebar

```tsx
// src/components/TitleBar.tsx
import { useState, useEffect } from 'react'
import { minimize, maximize, close, startDragging, isMaximized } from '../tauri/window'
import { isTauri, getPlatform, type Platform } from '../utils/platform'

export function TitleBar() {
  const [platform, setPlatform] = useState<Platform>('web')
  const [maximized, setMaximized] = useState(false)

  useEffect(() => {
    getPlatform().then(setPlatform)
    if (isTauri()) {
      isMaximized().then(setMaximized)
    }
  }, [])

  if (!isTauri()) return null

  const isMac = platform === 'macos'

  return (
    <div
      data-tauri-drag-region
      className="h-8 flex items-center justify-between px-3 bg-gray-900 select-none"
      onMouseDown={() => startDragging()}
    >
      {/* macOS: controls left */}
      {isMac && <WindowControls />}

      <span className="text-gray-400 text-sm flex-1 text-center">App Title</span>

      {/* Windows/Linux: controls right */}
      {!isMac && <WindowControls />}
    </div>
  )
}

function WindowControls() {
  return (
    <div className="flex gap-2" onMouseDown={(e) => e.stopPropagation()}>
      <button
        onClick={minimize}
        className="w-3 h-3 rounded-full bg-yellow-500 hover:opacity-80"
        aria-label="Minimize"
      />
      <button
        onClick={maximize}
        className="w-3 h-3 rounded-full bg-green-500 hover:opacity-80"
        aria-label="Maximize"
      />
      <button
        onClick={close}
        className="w-3 h-3 rounded-full bg-red-500 hover:opacity-80"
        aria-label="Close"
      />
    </div>
  )
}
```

---

## Transparent Window (Glassmorphism)

```json
// src-tauri/tauri.conf.json (window)
{
  "tauri": {
    "windows": [
      {
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

            // macOS: vibrancy effect
            #[cfg(target_os = "macos")]
            {
                use window_vibrancy::{apply_vibrancy, NSVisualEffectMaterial};
                apply_vibrancy(&window, NSVisualEffectMaterial::HudWindow, None, None)
                    .expect("Failed to apply vibrancy");
            }

            // Windows: acrylic/mica effect
            #[cfg(target_os = "windows")]
            {
                use window_vibrancy::apply_acrylic;
                apply_acrylic(&window, Some((0, 0, 0, 125)))
                    .expect("Failed to apply acrylic");
            }

            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error running tauri app");
}
```

```toml
# src-tauri/Cargo.toml
[dependencies]
window-vibrancy = "0.4"
```

---

## React Hooks

```typescript
// src/hooks/useTauri.ts
import { useState, useEffect } from 'react'
import { isTauri, getPlatform, type Platform } from '../utils/platform'

export function usePlatform(): Platform {
  const [platform, setPlatform] = useState<Platform>('web')

  useEffect(() => {
    getPlatform().then(setPlatform)
  }, [])

  return platform
}

export function useIsTauri(): boolean {
  return isTauri()
}
```

```typescript
// src/hooks/useWindowState.ts
import { useState, useEffect } from 'react'
import { appWindow } from '@tauri-apps/api/window'
import { isTauri } from '../utils/platform'

export function useWindowState() {
  const [maximized, setMaximized] = useState(false)
  const [fullscreen, setFullscreen] = useState(false)

  useEffect(() => {
    if (!isTauri()) return

    const checkState = async () => {
      setMaximized(await appWindow.isMaximized())
      setFullscreen(await appWindow.isFullscreen())
    }

    checkState()

    const unlisten = appWindow.onResized(checkState)
    return () => { unlisten.then(fn => fn()) }
  }, [])

  return { maximized, fullscreen }
}
```

---

## App Entry

```tsx
// src/App.tsx
import { TitleBar } from './components/TitleBar'
import { useIsTauri, usePlatform } from './hooks/useTauri'

export default function App() {
  const isTauriApp = useIsTauri()
  const platform = usePlatform()

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      {isTauriApp && <TitleBar />}

      <main className="p-6">
        <h1 className="text-2xl font-bold">Hello from {platform}</h1>
        <p className="mt-2 text-gray-400">
          Running in {isTauriApp ? 'Tauri desktop' : 'web browser'}
        </p>
      </main>
    </div>
  )
}
```

---

## Build Commands

```bash
# Development
npm run tauri dev

# Build for production
npm run tauri build

# Build for specific platform
npm run tauri build -- --target x86_64-apple-darwin      # macOS Intel
npm run tauri build -- --target aarch64-apple-darwin     # macOS Apple Silicon
npm run tauri build -- --target x86_64-pc-windows-msvc   # Windows
npm run tauri build -- --target x86_64-unknown-linux-gnu # Linux
```

---

## CI/CD (GitHub Actions)

```yaml
# .github/workflows/release.yml
name: Release
on:
  push:
    tags: ['v*']

jobs:
  release:
    permissions:
      contents: write
    strategy:
      matrix:
        platform: [macos-latest, ubuntu-latest, windows-latest]
    runs-on: ${{ matrix.platform }}

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: 20

      - uses: dtolnay/rust-toolchain@stable

      - uses: Swatinem/rust-cache@v2
        with:
          workspaces: './src-tauri -> target'

      - run: npm install

      - uses: tauri-apps/tauri-action@v0
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tagName: v__VERSION__
          releaseName: 'App v__VERSION__'
          releaseBody: 'See changelog for details.'
          releaseDraft: true
          prerelease: false
```

---

**Platform:** Web (React) + Desktop (Tauri) | **Node:** 18+ | **React:** 18+ | **Tauri:** 1.5+ | **Version:** 1.0.0 | **Updated:** 2025-11-29
