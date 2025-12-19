# Chapter 9: Web React + TypeScript

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~6,000 words

---

## Overview

The Web platform bridge generates **React functional components** with **TypeScript** using **Material-UI (MUI)** for styling.

## Architecture

```
AvaUI AST          React Generator        React Components
ComponentNode   →    ReactTypeScript   →    <Button>
  type: BUTTON       Generator               <TextField>
  properties                                  <Stack>
```

## ReactTypeScriptGenerator

**Location:** `Universal/IDEAMagic/CodeGen/Generators/React/src/commonMain/kotlin/.../ReactTypeScriptGenerator.kt`

```kotlin
class ReactTypeScriptGenerator : CodeGenerator {

    override fun generate(screen: ScreenNode): GeneratedCode {
        val code = StringBuilder()

        // 1. Imports
        generateImports(screen, code)
        code.appendLine()

        // 2. Component interface
        code.appendLine("export const ${screen.name}: React.FC = () => {")

        // 3. State hooks
        screen.stateVariables.forEach { stateVar ->
            generateStateHook(stateVar, code)
        }
        if (screen.stateVariables.isNotEmpty()) {
            code.appendLine()
        }

        // 4. JSX return
        code.appendLine("  return (")
        generateComponent(screen.root, code, indent = 2)
        code.appendLine("  );")

        code.appendLine("};")

        return GeneratedCode(
            code = code.toString(),
            language = Language.TYPESCRIPT,
            platform = Platform.WEB
        )
    }

    private fun generateImports(screen: ScreenNode, code: StringBuilder) {
        code.appendLine("import React, { useState } from 'react';")
        code.appendLine("import {")
        code.appendLine("  Button,")
        code.appendLine("  TextField,")
        code.appendLine("  Typography,")
        code.appendLine("  Stack,")
        code.appendLine("  Box,")
        code.appendLine("} from '@mui/material';")
    }

    private fun generateStateHook(stateVar: StateVariable, code: StringBuilder) {
        val initialValue = formatPropertyValue(stateVar.initialValue)
        code.appendLine("  const [${stateVar.name}, set${stateVar.name.capitalize()}] = " +
                        "useState<${stateVar.type}>($initialValue);")
    }
}
```

## Material-UI (MUI) Integration

### Button

```typescript
<Button
  variant="contained"
  onClick={() => handleClick()}
  fullWidth
>
  Click Me
</Button>
```

### TextField

```typescript
<TextField
  label="Username"
  value={username}
  onChange={(e) => setUsername(e.target.value)}
  fullWidth
/>
```

### Stack (Layout)

```typescript
<Stack spacing={2}>
  <Typography variant="h1">Welcome</Typography>
  <TextField label="Email" />
  <Button variant="contained">Submit</Button>
</Stack>
```

## Complete Example

**Input JSON:**
```json
{
  "name": "LoginScreen",
  "stateVariables": [
    { "name": "username", "type": "string", "initialValue": "", "mutable": true },
    { "name": "password", "type": "string", "initialValue": "", "mutable": true }
  ],
  "root": {
    "type": "COLUMN",
    "children": [
      {
        "type": "TEXT",
        "properties": { "content": "Login", "variant": "H1" }
      },
      {
        "type": "TEXT_FIELD",
        "properties": { "label": "Username" },
        "eventHandlers": { "onValueChange": "(e) => setUsername(e.target.value)" }
      },
      {
        "type": "TEXT_FIELD",
        "properties": { "label": "Password", "type": "password" },
        "eventHandlers": { "onValueChange": "(e) => setPassword(e.target.value)" }
      },
      {
        "type": "BUTTON",
        "properties": { "text": "Login" },
        "eventHandlers": { "onClick": "() => handleLogin(username, password)" }
      }
    ]
  }
}
```

**Generated TypeScript:**
```typescript
import React, { useState } from 'react';
import {
  Button,
  TextField,
  Typography,
  Stack,
} from '@mui/material';

export const LoginScreen: React.FC = () => {
  const [username, setUsername] = useState<string>('');
  const [password, setPassword] = useState<string>('');

  const handleLogin = (user: string, pass: string) => {
    // Login logic
  };

  return (
    <Stack spacing={2} sx={{ p: 2 }}>
      <Typography variant="h1">
        Login
      </Typography>

      <TextField
        label="Username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        fullWidth
      />

      <TextField
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        fullWidth
      />

      <Button
        variant="contained"
        onClick={() => handleLogin(username, password)}
        fullWidth
      >
        Login
      </Button>
    </Stack>
  );
};
```

## ReactComponentLoader

**Location:** `Universal/IDEAMagic/Components/Web/React/src/main/kotlin/ReactComponentLoader.kt`

```kotlin
class ReactComponentLoader {
    fun loadComponent(json: String): ReactComponent {
        val parser = JsonDSLParser()
        val screen = parser.parseScreen(json).getOrThrow()

        val generator = ReactTypeScriptGenerator()
        val generated = generator.generate(screen)

        return ReactComponent(
            code = generated.code,
            dependencies = listOf(
                "@mui/material@^5.14.0",
                "@emotion/react@^11.11.1",
                "@emotion/styled@^11.11.0"
            )
        )
    }
}
```

## Theme System

### MagicTheme → Material-UI Theme

```typescript
import { createTheme, ThemeProvider } from '@mui/material/styles';

const magicTheme = createTheme({
  palette: {
    primary: {
      main: '#FF5733',
    },
    secondary: {
      main: '#33C4FF',
    },
    background: {
      default: '#FFFFFF',
      paper: '#F5F5F5',
    },
  },
  typography: {
    h1: {
      fontSize: '32px',
      fontWeight: 700,
    },
    body1: {
      fontSize: '16px',
      fontWeight: 400,
    },
  },
  spacing: 8,
  shape: {
    borderRadius: 8,
  },
});

export const App: React.FC = () => {
  return (
    <ThemeProvider theme={magicTheme}>
      <LoginScreen />
    </ThemeProvider>
  );
};
```

## Component Examples

### Card

```typescript
<Card sx={{ maxWidth: 400, p: 2 }}>
  <CardContent>
    <Typography variant="h2">Card Title</Typography>
    <Typography variant="body2">Card content</Typography>
  </CardContent>
</Card>
```

### Grid Layout

```typescript
<Grid container spacing={2}>
  <Grid item xs={12} sm={6} md={4}>
    <Card>Item 1</Card>
  </Grid>
  <Grid item xs={12} sm={6} md={4}>
    <Card>Item 2</Card>
  </Grid>
  <Grid item xs={12} sm={6} md={4}>
    <Card>Item 3</Card>
  </Grid>
</Grid>
```

### Dialog

```typescript
<Dialog open={open} onClose={() => setOpen(false)}>
  <DialogTitle>Confirm Action</DialogTitle>
  <DialogContent>
    <DialogContentText>
      Are you sure you want to proceed?
    </DialogContentText>
  </DialogContent>
  <DialogActions>
    <Button onClick={() => setOpen(false)}>Cancel</Button>
    <Button onClick={handleConfirm} variant="contained">
      Confirm
    </Button>
  </DialogActions>
</Dialog>
```

## Platform-Specific Features

### Web-Only Components
- **Tooltip** - Hover tooltip
- **Popover** - Popup overlay
- **Snackbar** - Toast notification
- **Portal** - Render outside DOM hierarchy
- **ClickAwayListener** - Detect outside clicks

### Responsive Design

```typescript
<Box
  sx={{
    width: {
      xs: '100%',   // Mobile: 100%
      sm: '600px',  // Tablet: 600px
      md: '960px',  // Desktop: 960px
    },
  }}
>
  Content
</Box>
```

## Performance Optimizations

1. **React.memo** - Prevent unnecessary re-renders
2. **useMemo** - Cache expensive computations
3. **useCallback** - Stable event handlers
4. **Code splitting** - Lazy load components

```typescript
import React, { useMemo, useCallback } from 'react';

export const OptimizedComponent: React.FC = React.memo(() => {
  const expensiveValue = useMemo(() => {
    // Expensive calculation
    return calculate();
  }, [dependency]);

  const handleClick = useCallback(() => {
    // Event handler
  }, []);

  return <Button onClick={handleClick}>Click</Button>;
});
```

## Summary

The Web React bridge provides:
- React functional components with TypeScript
- Material-UI (MUI) for styling
- useState hooks for state management
- Responsive design support
- Theme customization
- Performance optimizations

**Next:** Chapter 10 covers Avanues Ecosystem integration.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
