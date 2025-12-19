# Chapter 13: Web Interface Implementation

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~4,000 words

---

## Overview

The Web Interface provides a visual editor for creating AvaUI apps with drag-drop, live preview, and code export.

**Status:** ❌ Not implemented (planned)
**Effort:** 240 hours (Weeks 3-8)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Web Interface                           │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Canvas     │  │  Component   │  │  Property    │    │
│  │   (Editor)   │  │   Palette    │  │   Panel      │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ Code Editor  │  │ Live Preview │  │    Export    │    │
│  │   (Monaco)   │  │ (Android/iOS)│  │    System    │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Technology Stack

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "typescript": "^5.0.0",
    "@mui/material": "^5.14.0",
    "@mui/icons-material": "^5.14.0",
    "react-dnd": "^16.0.1",
    "react-dnd-html5-backend": "^16.0.1",
    "zustand": "^4.4.0",
    "monaco-editor": "^0.44.0",
    "react-split-pane": "^0.1.92"
  }
}
```

## Canvas Component

```typescript
import React, { useState } from 'react';
import { useDrop } from 'react-dnd';
import { Box } from '@mui/material';

interface CanvasProps {
  components: ComponentNode[];
  onComponentAdd: (component: ComponentNode) => void;
  onComponentSelect: (id: string) => void;
}

export const Canvas: React.FC<CanvasProps> = ({ components, onComponentAdd, onComponentSelect }) => {
  const [{ isOver }, drop] = useDrop(() => ({
    accept: 'component',
    drop: (item: { type: string }) => {
      onComponentAdd({
        id: generateId(),
        type: item.type,
        properties: {},
        children: []
      });
    },
    collect: (monitor) => ({
      isOver: monitor.isOver()
    })
  }));

  return (
    <Box
      ref={drop}
      sx={{
        flex: 1,
        minHeight: '100vh',
        backgroundColor: isOver ? '#f0f0f0' : '#fff',
        border: '2px dashed #ccc',
        padding: 2
      }}
    >
      {components.map(component => (
        <ComponentRenderer
          key={component.id}
          component={component}
          onSelect={() => onComponentSelect(component.id)}
        />
      ))}
    </Box>
  );
};
```

## Component Palette

```typescript
import React from 'react';
import { useDrag } from 'react-dnd';
import { Box, Typography, Paper } from '@mui/material';

interface PaletteItemProps {
  type: string;
  label: string;
  icon: React.ReactNode;
}

const PaletteItem: React.FC<PaletteItemProps> = ({ type, label, icon }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'component',
    item: { type },
    collect: (monitor) => ({
      isDragging: monitor.isDragging()
    })
  }));

  return (
    <Paper
      ref={drag}
      sx={{
        p: 2,
        cursor: 'grab',
        opacity: isDragging ? 0.5 : 1,
        '&:hover': { backgroundColor: '#f5f5f5' }
      }}
    >
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        {icon}
        <Typography variant="body2">{label}</Typography>
      </Box>
    </Paper>
  );
};

export const ComponentPalette: React.FC = () => {
  return (
    <Box sx={{ width: 250, borderRight: '1px solid #ddd', p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Components
      </Typography>

      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
        FOUNDATION
      </Typography>
      <PaletteItem type="BUTTON" label="Button" icon={<ButtonIcon />} />
      <PaletteItem type="TEXT" label="Text" icon={<TextIcon />} />
      <PaletteItem type="TEXT_FIELD" label="TextField" icon={<TextFieldIcon />} />
      <PaletteItem type="CARD" label="Card" icon={<CardIcon />} />

      <Typography variant="caption" sx={{ color: 'text.secondary', mt: 2 }}>
        LAYOUT
      </Typography>
      <PaletteItem type="COLUMN" label="Column" icon={<ColumnIcon />} />
      <PaletteItem type="ROW" label="Row" icon={<RowIcon />} />
      <PaletteItem type="STACK" label="Stack" icon={<StackIcon />} />
    </Box>
  );
};
```

## Property Panel

```typescript
import React from 'react';
import { TextField, Select, MenuItem, Switch, FormControlLabel, Box, Typography } from '@mui/material';

interface PropertyPanelProps {
  component: ComponentNode | null;
  onPropertyChange: (property: string, value: any) => void;
}

export const PropertyPanel: React.FC<PropertyPanelProps> = ({ component, onPropertyChange }) => {
  if (!component) {
    return (
      <Box sx={{ width: 300, borderLeft: '1px solid #ddd', p: 2 }}>
        <Typography variant="body2" color="text.secondary">
          Select a component to edit properties
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ width: 300, borderLeft: '1px solid #ddd', p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Properties
      </Typography>

      {component.type === 'BUTTON' && (
        <>
          <TextField
            label="Text"
            value={component.properties.text || ''}
            onChange={(e) => onPropertyChange('text', e.target.value)}
            fullWidth
            margin="normal"
          />
          <Select
            label="Variant"
            value={component.properties.variant || 'primary'}
            onChange={(e) => onPropertyChange('variant', e.target.value)}
            fullWidth
          >
            <MenuItem value="primary">Primary</MenuItem>
            <MenuItem value="secondary">Secondary</MenuItem>
            <MenuItem value="outline">Outline</MenuItem>
          </Select>
          <FormControlLabel
            control={
              <Switch
                checked={component.properties.enabled !== false}
                onChange={(e) => onPropertyChange('enabled', e.target.checked)}
              />
            }
            label="Enabled"
          />
        </>
      )}

      {component.type === 'TEXT' && (
        <>
          <TextField
            label="Content"
            value={component.properties.content || ''}
            onChange={(e) => onPropertyChange('content', e.target.value)}
            fullWidth
            multiline
            rows={3}
            margin="normal"
          />
          <Select
            label="Variant"
            value={component.properties.variant || 'BODY1'}
            onChange={(e) => onPropertyChange('variant', e.target.value)}
            fullWidth
          >
            <MenuItem value="H1">Heading 1</MenuItem>
            <MenuItem value="H2">Heading 2</MenuItem>
            <MenuItem value="BODY1">Body 1</MenuItem>
            <MenuItem value="CAPTION">Caption</MenuItem>
          </Select>
        </>
      )}
    </Box>
  );
};
```

## Code Editor (Monaco)

```typescript
import React from 'react';
import Editor from '@monaco-editor/react';
import { Box } from '@mui/material';

interface CodeEditorProps {
  value: string;
  language: 'json' | 'kotlin' | 'swift' | 'typescript';
  onChange: (value: string) => void;
}

export const CodeEditor: React.FC<CodeEditorProps> = ({ value, language, onChange }) => {
  return (
    <Box sx={{ height: '100%' }}>
      <Editor
        height="100%"
        language={language}
        value={value}
        onChange={(value) => onChange(value || '')}
        theme="vs-dark"
        options={{
          minimap: { enabled: false },
          fontSize: 14,
          lineNumbers: 'on',
          roundedSelection: false,
          scrollBeyondLastLine: false,
          readOnly: false
        }}
      />
    </Box>
  );
};
```

## Live Preview

```typescript
import React, { useEffect, useState } from 'react';
import { Box, ToggleButtonGroup, ToggleButton } from '@mui/material';

type PreviewPlatform = 'android' | 'ios' | 'web';

interface LivePreviewProps {
  components: ComponentNode[];
}

export const LivePreview: React.FC<LivePreviewProps> = ({ components }) => {
  const [platform, setPlatform] = useState<PreviewPlatform>('android');
  const [generatedCode, setGeneratedCode] = useState('');

  useEffect(() => {
    // Generate code for selected platform
    const generator = getGenerator(platform);
    const code = generator.generate({ name: 'Preview', root: components[0] });
    setGeneratedCode(code.code);
  }, [components, platform]);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <ToggleButtonGroup
        value={platform}
        exclusive
        onChange={(e, value) => value && setPlatform(value)}
        sx={{ mb: 2 }}
      >
        <ToggleButton value="android">Android</ToggleButton>
        <ToggleButton value="ios">iOS</ToggleButton>
        <ToggleButton value="web">Web</ToggleButton>
      </ToggleButtonGroup>

      <Box sx={{ flex: 1, border: '1px solid #ddd', borderRadius: 1, overflow: 'auto' }}>
        {platform === 'android' && <AndroidPreview code={generatedCode} />}
        {platform === 'ios' && <IOSPreview code={generatedCode} />}
        {platform === 'web' && <WebPreview components={components} />}
      </Box>
    </Box>
  );
};
```

## State Management (Zustand)

```typescript
import create from 'zustand';

interface EditorState {
  components: ComponentNode[];
  selectedComponentId: string | null;
  history: ComponentNode[][];
  historyIndex: number;

  addComponent: (component: ComponentNode) => void;
  updateComponent: (id: string, updates: Partial<ComponentNode>) => void;
  deleteComponent: (id: string) => void;
  selectComponent: (id: string | null) => void;
  undo: () => void;
  redo: () => void;
}

export const useEditorStore = create<EditorState>((set, get) => ({
  components: [],
  selectedComponentId: null,
  history: [[]],
  historyIndex: 0,

  addComponent: (component) => {
    set((state) => {
      const newComponents = [...state.components, component];
      return {
        components: newComponents,
        history: [...state.history.slice(0, state.historyIndex + 1), newComponents],
        historyIndex: state.historyIndex + 1
      };
    });
  },

  updateComponent: (id, updates) => {
    set((state) => {
      const newComponents = state.components.map(c =>
        c.id === id ? { ...c, ...updates } : c
      );
      return {
        components: newComponents,
        history: [...state.history.slice(0, state.historyIndex + 1), newComponents],
        historyIndex: state.historyIndex + 1
      };
    });
  },

  deleteComponent: (id) => {
    set((state) => {
      const newComponents = state.components.filter(c => c.id !== id);
      return {
        components: newComponents,
        selectedComponentId: state.selectedComponentId === id ? null : state.selectedComponentId,
        history: [...state.history.slice(0, state.historyIndex + 1), newComponents],
        historyIndex: state.historyIndex + 1
      };
    });
  },

  selectComponent: (id) => set({ selectedComponentId: id }),

  undo: () => {
    const { history, historyIndex } = get();
    if (historyIndex > 0) {
      set({
        components: history[historyIndex - 1],
        historyIndex: historyIndex - 1
      });
    }
  },

  redo: () => {
    const { history, historyIndex } = get();
    if (historyIndex < history.length - 1) {
      set({
        components: history[historyIndex + 1],
        historyIndex: historyIndex + 1
      });
    }
  }
}));
```

## Export System

```typescript
import { saveAs } from 'file-saver';

export class ExportManager {
  exportJSON(components: ComponentNode[]): void {
    const json = JSON.stringify({ components }, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    saveAs(blob, 'screen.json');
  }

  exportAndroidCode(components: ComponentNode[]): void {
    const generator = new KotlinComposeGenerator();
    const code = generator.generate({ name: 'Screen', root: components[0] });
    const blob = new Blob([code.code], { type: 'text/plain' });
    saveAs(blob, 'Screen.kt');
  }

  exportIOSCode(components: ComponentNode[]): void {
    const generator = new SwiftUIGenerator();
    const code = generator.generate({ name: 'Screen', root: components[0] });
    const blob = new Blob([code.code], { type: 'text/plain' });
    saveAs(blob, 'ScreenView.swift');
  }

  exportWebCode(components: ComponentNode[]): void {
    const generator = new ReactTypeScriptGenerator();
    const code = generator.generate({ name: 'Screen', root: components[0] });
    const blob = new Blob([code.code], { type: 'text/plain' });
    saveAs(blob, 'Screen.tsx');
  }

  exportAll(components: ComponentNode[]): void {
    // Create zip with all platforms
    const zip = new JSZip();
    zip.file('screen.json', JSON.stringify({ components }, null, 2));
    zip.file('android/Screen.kt', this.generateAndroidCode(components));
    zip.file('ios/ScreenView.swift', this.generateIOSCode(components));
    zip.file('web/Screen.tsx', this.generateWebCode(components));

    zip.generateAsync({ type: 'blob' }).then(blob => {
      saveAs(blob, 'avaui-export.zip');
    });
  }
}
```

## Summary

Web Interface features:
- **Drag-drop editor** - Visual component composition
- **Live preview** - Real-time platform rendering
- **Code export** - Generate Android/iOS/Web code
- **Property editing** - Visual property configuration
- **Undo/redo** - History management
- **JSON export** - Portable screen definitions

**Effort:** 240 hours across 6 workstreams

**Next:** Chapter 14 covers P2P/WebRTC collaboration.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
