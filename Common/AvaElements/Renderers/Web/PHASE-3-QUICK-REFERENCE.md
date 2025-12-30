# Phase 3 Components - Quick Reference

**Package:** `com.augmentalis.AvaMagic.elements`
**Total Components:** 32
**Status:** ✅ Production Ready

---

## Import Syntax

```typescript
// Category imports
import { Avatar, Spinner, Tooltip } from '@avaelements/renderer-web/AvaMagic/elements/display';
import { Modal, Alert, Snackbar } from '@avaelements/renderer-web/AvaMagic/elements/feedback';
import { Dropdown, DatePicker, SearchBar } from '@avaelements/renderer-web/AvaMagic/elements/inputs';
import { Drawer, Grid, Tabs } from '@avaelements/renderer-web/AvaMagic/elements/layout';
import { AppBar, Pagination, Breadcrumb } from '@avaelements/renderer-web/AvaMagic/elements/navigation';

// All-in-one import
import * as AvaMagic from '@avaelements/renderer-web/AvaMagic/elements';
```

---

## Component Cheat Sheet

### Display (6)

| Component | Key Props | Use Case |
|-----------|-----------|----------|
| **Avatar** | `src`, `size`, `variant`, `fallback` | User profile pictures |
| **Divider** | `orientation`, `variant`, `label` | Content separators |
| **ProgressBar** | `value`, `variant`, `showLabel` | Loading progress |
| **Skeleton** | `variant`, `width`, `height`, `animation` | Loading placeholders |
| **Spinner** | `size`, `color`, `centered` | Loading indicators |
| **Tooltip** | `title`, `placement`, `arrow` | Contextual help |

### Feedback (6)

| Component | Key Props | Use Case |
|-----------|-----------|----------|
| **Alert** | `severity`, `variant`, `title`, `closable` | Important messages |
| **Confirm** | `title`, `message`, `onConfirm`, `destructive` | Confirmation dialogs |
| **ContextMenu** | `items`, `children` | Right-click menus |
| **Modal** | `open`, `onClose`, `title`, `fullScreen` | Overlay dialogs |
| **Snackbar** | `message`, `severity`, `position` | Notifications |
| **Toast** | `message`, `duration`, `position` | Brief messages |

### Inputs (12)

| Component | Key Props | Use Case |
|-----------|-----------|----------|
| **Autocomplete** | `options`, `value`, `onChange`, `freeSolo` | Search with suggestions |
| **DatePicker** | `value`, `onChange`, `min`, `max` | Date selection |
| **Dropdown** | `options`, `value`, `onChange`, `placeholder` | Select from list |
| **FileUpload** | `files`, `onChange`, `accept`, `maxSize` | File selection |
| **ImagePicker** | `image`, `onChange`, `previewUrl` | Image selection |
| **RadioButton** | `value`, `checked`, `onChange`, `label` | Single radio |
| **RadioGroup** | `options`, `value`, `onChange`, `row` | Multiple radios |
| **RangeSlider** | `value`, `onChange`, `min`, `max` | Range selection |
| **Rating** | `value`, `onChange`, `max`, `precision` | Star ratings |
| **SearchBar** | `value`, `onChange`, `onSearch`, `showClearButton` | Search input |
| **Slider** | `value`, `onChange`, `min`, `max`, `step` | Value selection |
| **TimePicker** | `value`, `onChange`, `label` | Time selection |

### Layout (4)

| Component | Key Props | Use Case |
|-----------|-----------|----------|
| **Drawer** | `open`, `onClose`, `anchor`, `size` | Side panels |
| **Grid** | `container`, `xs/sm/md/lg/xl`, `spacing` | Responsive grid |
| **Spacer** | `width`, `height`, `flex` | Spacing |
| **Tabs** | `value`, `onChange`, `tabs`, `orientation` | Tab navigation |

### Navigation (4)

| Component | Key Props | Use Case |
|-----------|-----------|----------|
| **AppBar** | `title`, `leading`, `actions`, `onMenuClick` | Top app bar |
| **BottomNav** | `value`, `onChange`, `items`, `showLabels` | Bottom navigation |
| **Breadcrumb** | `items`, `separator`, `maxItems` | Navigation trail |
| **Pagination** | `page`, `count`, `onChange` | Page navigation |

---

## Common Patterns

### Form with Validation
```tsx
<form>
  <Dropdown
    value={category}
    onChange={setCategory}
    options={categories}
    label="Category"
    error={!!errors.category}
    helperText={errors.category}
  />

  <DatePicker
    value={date}
    onChange={setDate}
    label="Start Date"
    min="2024-01-01"
  />

  <SearchBar
    value={search}
    onChange={setSearch}
    onSearch={handleSearch}
  />
</form>
```

### Modal with Confirm
```tsx
<>
  <Modal
    open={isModalOpen}
    onClose={handleClose}
    title="Edit Profile"
    maxWidth={600}
  >
    {/* Form content */}
  </Modal>

  <Confirm
    open={isConfirmOpen}
    title="Delete Item"
    message="Are you sure you want to delete this?"
    destructive
    onConfirm={handleDelete}
    onCancel={handleCancel}
  />
</>
```

### Responsive Layout
```tsx
<Grid container spacing={2}>
  <Grid item xs={12} md={6}>
    <Avatar src="/user.jpg" size="large" />
  </Grid>
  <Grid item xs={12} md={6}>
    <Rating value={rating} onChange={setRating} />
  </Grid>
</Grid>
```

### Navigation Structure
```tsx
<>
  <AppBar
    title="My App"
    onMenuClick={toggleDrawer}
    actions={[
      <IconButton><NotificationsIcon /></IconButton>,
      <IconButton><SettingsIcon /></IconButton>
    ]}
  />

  <Drawer
    open={drawerOpen}
    onClose={toggleDrawer}
    anchor="left"
    header={<h2>Menu</h2>}
  >
    <nav>{/* Menu items */}</nav>
  </Drawer>

  <BottomNav
    value={currentTab}
    onChange={setCurrentTab}
    items={navItems}
  />
</>
```

### Feedback Flow
```tsx
const handleSubmit = async () => {
  try {
    // Show loading
    setLoading(true);

    await submitData();

    // Success notification
    setSnackbar({
      open: true,
      message: 'Saved successfully!',
      severity: 'success'
    });
  } catch (error) {
    // Error alert
    setAlert({
      open: true,
      severity: 'error',
      message: error.message
    });
  } finally {
    setLoading(false);
  }
};

return (
  <>
    {loading && <Spinner centered size="large" />}

    <Snackbar
      open={snackbar.open}
      message={snackbar.message}
      severity={snackbar.severity}
      onClose={closeSnackbar}
    />

    <Alert
      open={alert.open}
      severity={alert.severity}
      message={alert.message}
      closable
      onClose={closeAlert}
    />
  </>
);
```

---

## Props Inheritance

All components support:
- `className` - Custom CSS class
- `sx` - Material-UI sx prop (when using MUI base)
- Standard React props (key, ref, etc.)

---

## Accessibility

All components include:
- ✅ ARIA labels
- ✅ Keyboard navigation
- ✅ Focus management
- ✅ Screen reader support
- ✅ Color contrast (WCAG AA)

### Example
```tsx
<Button
  onClick={handleClick}
  aria-label="Save changes"
  aria-describedby="save-tooltip"
>
  Save
</Button>
<Tooltip id="save-tooltip" title="Ctrl+S to save" />
```

---

## Theming

Components respect MUI theme:
```tsx
import { ThemeProvider, createTheme } from '@mui/material';

const theme = createTheme({
  palette: {
    primary: { main: '#1976d2' },
    secondary: { main: '#dc004e' },
  },
});

<ThemeProvider theme={theme}>
  <App />
</ThemeProvider>
```

---

## Performance Tips

1. **Lazy Load Heavy Components**
```tsx
const Modal = lazy(() => import('@avaelements/renderer-web/AvaMagic/elements/feedback/Modal'));
const Drawer = lazy(() => import('@avaelements/renderer-web/AvaMagic/elements/layout/Drawer'));
```

2. **Memoize Callbacks**
```tsx
const handleChange = useCallback((value) => {
  setValue(value);
}, []);

<Dropdown value={value} onChange={handleChange} options={options} />
```

3. **Use Virtual Scrolling for Large Lists**
```tsx
import { FixedSizeList } from 'react-window';

<FixedSizeList itemCount={1000} itemSize={50}>
  {({ index }) => <Avatar src={users[index].avatar} />}
</FixedSizeList>
```

---

## Testing

### Unit Test Example
```tsx
import { render, screen } from '@testing-library/react';
import { Avatar } from '@avaelements/renderer-web/AvaMagic/elements/display';

test('renders avatar with fallback', () => {
  render(<Avatar fallback="JD" alt="John Doe" />);
  expect(screen.getByText('JD')).toBeInTheDocument();
});
```

### Accessibility Test Example
```tsx
import { axe } from 'jest-axe';

test('has no accessibility violations', async () => {
  const { container } = render(<Modal open title="Test" />);
  const results = await axe(container);
  expect(results).toHaveNoViolations();
});
```

---

## Migration from Flutter

| Flutter Component | Web Equivalent | Notes |
|-------------------|----------------|-------|
| `CircleAvatar` | `Avatar` | Same API, `variant="circle"` |
| `LinearProgressIndicator` | `ProgressBar` | Same API |
| `SnackBar` | `Snackbar` | Same API |
| `AlertDialog` | `Confirm` | Simplified API |
| `Drawer` | `Drawer` | Same API |
| `BottomNavigationBar` | `BottomNav` | Same API |
| `Slider` | `Slider` | Same API |
| `RangeSlider` | `RangeSlider` | Same API |

---

## Support

**Documentation:** See `PHASE-3-IMPLEMENTATION-REPORT.md`
**Issues:** File in project tracker
**Examples:** See Storybook (coming soon)

---

**Version:** 3.0.0-phase3
**Last Updated:** 2025-11-23
