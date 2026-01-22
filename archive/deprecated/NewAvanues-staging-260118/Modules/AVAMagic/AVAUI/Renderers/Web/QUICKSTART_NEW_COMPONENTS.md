# Quick Start - New AvaUI Components

**8 New Components Added** | **Updated: 2025-11-09**

## Installation

No additional dependencies needed! All new components use existing Material-UI 5.

```bash
# Already installed in package.json
npm install @mui/material @mui/icons-material @emotion/react @emotion/styled
```

## Import Components

```tsx
import {
  // NEW Components
  Chip,
  Divider,
  Image,
  DatePicker,
  TimePicker,
  Dropdown,
  SearchBar,
  Dialog
} from '@avaui/web-renderer';
```

## Quick Examples

### 1. Chip Component

```tsx
import { Chip } from '@avaui/web-renderer';
import { Star } from '@mui/icons-material';

// Basic chip
<Chip label="Active" />

// With icon and delete
<Chip
  label="Featured"
  icon={<Star />}
  color="primary"
  deletable
  onDelete={() => console.log('Removed')}
/>

// Clickable chip
<Chip
  label="React"
  variant="outlined"
  clickable
  onClick={() => console.log('Clicked')}
/>
```

### 2. Divider Component

```tsx
import { Divider } from '@avaui/web-renderer';

// Horizontal divider
<Divider />

// Vertical divider in flex container
<Box display="flex">
  <Item />
  <Divider orientation="vertical" flexItem />
  <Item />
</Box>

// With text
<Divider textAlign="center">OR</Divider>

// Custom style
<Divider thickness={2} color="#1976d2" />
```

### 3. Image Component

```tsx
import { Image } from '@avaui/web-renderer';

// Basic image
<Image src="/logo.png" alt="Logo" />

// Fixed size with cover
<Image
  src="/banner.jpg"
  alt="Banner"
  width={800}
  height={400}
  fit="cover"
/>

// Rounded clickable image
<Image
  src="/profile.jpg"
  alt="Profile"
  width={100}
  height={100}
  borderRadius={50}
  clickable
  onClick={() => console.log('Clicked')}
/>

// Lazy loaded
<Image
  src="/large.jpg"
  alt="Large Image"
  loading="lazy"
/>
```

### 4. DatePicker Component

```tsx
import { DatePicker } from '@avaui/web-renderer';
import { useState } from 'react';

function MyForm() {
  const [date, setDate] = useState('2024-01-01');

  return (
    <DatePicker
      value={date}
      onChange={setDate}
      label="Select Date"
      min="2024-01-01"
      max="2024-12-31"
      required
    />
  );
}
```

### 5. TimePicker Component

```tsx
import { TimePicker } from '@avaui/web-renderer';
import { useState } from 'react';

function AppointmentForm() {
  const [time, setTime] = useState('09:00');

  return (
    <TimePicker
      value={time}
      onChange={setTime}
      label="Appointment Time"
      min="09:00"
      max="17:00"
      step={1800} // 30-minute intervals
    />
  );
}
```

### 6. Dropdown Component

```tsx
import { Dropdown } from '@avaui/web-renderer';
import { useState } from 'react';

function CountrySelector() {
  const [country, setCountry] = useState('');

  return (
    <Dropdown
      value={country}
      onChange={setCountry}
      label="Country"
      placeholder="Select country..."
      options={[
        { value: 'us', label: 'United States' },
        { value: 'uk', label: 'United Kingdom' },
        { value: 'ca', label: 'Canada' },
        { value: 'au', label: 'Australia', disabled: true }
      ]}
      helperText="Choose your country"
      required
      error={!country}
    />
  );
}
```

### 7. SearchBar Component

```tsx
import { SearchBar } from '@avaui/web-renderer';
import { useState } from 'react';

function ProductSearch() {
  const [query, setQuery] = useState('');

  return (
    <SearchBar
      value={query}
      onChange={setQuery}
      onSearch={(q) => console.log('Searching:', q)}
      placeholder="Search products..."
      debounceMs={300}
    />
  );
}
```

### 8. Dialog Component

```tsx
import { Dialog } from '@avaui/web-renderer';
import { useState } from 'react';

function ConfirmDialog() {
  const [open, setOpen] = useState(false);

  return (
    <>
      <Button onClick={() => setOpen(true)}>Delete</Button>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title="Confirm Delete"
        description="This action cannot be undone."
        primaryAction={{
          label: 'Delete',
          onClick: handleDelete,
          color: 'error'
        }}
        secondaryAction={{
          label: 'Cancel',
          onClick: () => setOpen(false)
        }}
        maxWidth="sm"
      >
        <Text>Are you sure you want to delete this item?</Text>
      </Dialog>
    </>
  );
}
```

## Real-World Examples

### User Profile Form

```tsx
function UserProfileForm() {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    country: '',
    birthDate: '',
    avatar: '',
    tags: []
  });

  return (
    <Column spacing={3}>
      <Image
        src={formData.avatar || '/default-avatar.png'}
        alt="Avatar"
        width={120}
        height={120}
        borderRadius={60}
        fit="cover"
      />

      <TextField
        label="Name"
        value={formData.name}
        onChange={(e) => setFormData({...formData, name: e.target.value})}
      />

      <DatePicker
        label="Birth Date"
        value={formData.birthDate}
        onChange={(date) => setFormData({...formData, birthDate: date})}
        max={new Date().toISOString().split('T')[0]}
      />

      <Dropdown
        label="Country"
        value={formData.country}
        onChange={(country) => setFormData({...formData, country})}
        options={countryOptions}
      />

      <Box>
        <Text variant="caption">Interests</Text>
        <Row spacing={1}>
          <Chip label="React" icon={<CodeIcon />} />
          <Chip label="TypeScript" icon={<CodeIcon />} />
          <Chip label="Design" icon={<PaletteIcon />} deletable />
        </Row>
      </Box>

      <Divider />

      <Button variant="contained" fullWidth>
        Save Profile
      </Button>
    </Column>
  );
}
```

### Search & Filter Interface

```tsx
function ProductBrowser() {
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [minDate, setMinDate] = useState('');
  const [maxDate, setMaxDate] = useState('');

  return (
    <Column spacing={2}>
      <SearchBar
        value={search}
        onChange={setSearch}
        onSearch={performSearch}
        placeholder="Search products..."
        debounceMs={300}
      />

      <Row spacing={2}>
        <Dropdown
          label="Category"
          value={category}
          onChange={setCategory}
          options={categoryOptions}
          fullWidth
        />

        <DatePicker
          label="From Date"
          value={minDate}
          onChange={setMinDate}
        />

        <DatePicker
          label="To Date"
          value={maxDate}
          onChange={setMaxDate}
        />
      </Row>

      <Divider />

      <Row spacing={1}>
        <Chip label="Electronics" deletable />
        <Chip label="In Stock" variant="outlined" />
        <Chip label="2024" deletable />
      </Row>

      {/* Product grid */}
    </Column>
  );
}
```

### Appointment Booking Dialog

```tsx
function AppointmentDialog({ open, onClose }) {
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [service, setService] = useState('');

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Book Appointment"
      maxWidth="sm"
      fullWidth
      primaryAction={{
        label: 'Book',
        onClick: handleBook,
        disabled: !date || !time || !service
      }}
      secondaryAction={{
        label: 'Cancel',
        onClick: onClose
      }}
    >
      <Column spacing={3}>
        <Dropdown
          label="Service"
          value={service}
          onChange={setService}
          options={[
            { value: 'haircut', label: 'Haircut ($30)' },
            { value: 'color', label: 'Hair Color ($60)' },
            { value: 'styling', label: 'Styling ($40)' }
          ]}
          required
        />

        <DatePicker
          label="Date"
          value={date}
          onChange={setDate}
          min={new Date().toISOString().split('T')[0]}
          required
        />

        <TimePicker
          label="Time"
          value={time}
          onChange={setTime}
          min="09:00"
          max="17:00"
          step={1800} // 30-minute slots
          required
        />

        <Divider />

        <Text variant="caption">
          Selected: {service && date && time ?
            `${service} on ${date} at ${time}` :
            'Please select all fields'}
        </Text>
      </Column>
    </Dialog>
  );
}
```

## Component Comparison

| Component | Use Case | Best For |
|-----------|----------|----------|
| Chip | Tags, filters, selections | Compact labels, removable items |
| Divider | Visual separation | Sections, groups, lists |
| Image | Display images | Photos, avatars, icons |
| DatePicker | Select dates | Forms, filters, scheduling |
| TimePicker | Select times | Appointments, schedules |
| Dropdown | Select from options | Forms, filters, settings |
| SearchBar | Search input | Search interfaces, filters |
| Dialog | Modal interactions | Confirmations, forms, details |

## Tips & Best Practices

### Chip
- Use deletable chips for removable tags/filters
- Use clickable chips for navigation
- Combine with icon for better visual communication
- Keep labels short (1-3 words)

### Divider
- Use sparingly - too many dividers clutters UI
- Vertical dividers need flexItem prop in flex containers
- Text dividers great for "OR" in login forms

### Image
- Always provide alt text for accessibility
- Use lazy loading for images below fold
- Use cover fit for backgrounds, contain for logos
- Consider using borderRadius for avatars

### DatePicker / TimePicker
- Set min/max to prevent invalid selections
- Use required prop with error states
- Consider @mui/x-date-pickers for advanced features
- Format dates consistently across your app

### Dropdown
- Provide placeholder for better UX
- Use helper text for additional guidance
- Mark required fields clearly
- Consider disabled state for unavailable options

### SearchBar
- Use debouncing (300ms) for API calls
- Show clear button for better UX
- Provide search handler for Enter key
- Keep placeholder text descriptive

### Dialog
- Use maxWidth to control size
- fullWidth for forms, compact for confirmations
- Show close button for non-critical dialogs
- Primary action should be most common choice

## TypeScript Support

All components are fully typed:

```tsx
import type {
  ChipProps,
  DividerProps,
  ImageProps,
  DatePickerProps,
  TimePickerProps,
  DropdownProps,
  DropdownOption,
  SearchBarProps,
  DialogProps,
  DialogAction
} from '@avaui/web-renderer';
```

## Browser Support

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- Mobile browsers (iOS Safari, Chrome Mobile)

## Need Help?

- Full documentation: `README.md`
- Implementation details: `IMPLEMENTATION_REPORT.md`
- Component status: `IMPLEMENTATION_STATUS.md`

---

Created by Manoj Jhawar, manoj@ideahq.net
