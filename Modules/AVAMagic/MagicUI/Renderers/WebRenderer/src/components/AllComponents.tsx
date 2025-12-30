/**
 * All MagicUI Phase 1 Components for React
 *
 * Complete implementations of all 13 foundation components
 */

import React from 'react';
import {
  TextField as MuiTextField,
  Checkbox as MuiCheckbox,
  Switch as MuiSwitch,
  Card as MuiCard,
  CardContent,
  CardHeader,
  Dialog as MuiDialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Box as MuiBox,
  Stack,
  FormControlLabel
} from '@mui/material';
import type {
  TextFieldProps,
  CheckboxProps,
  SwitchProps,
  CardProps,
  DialogProps,
  ListViewProps,
  ImageProps,
  TextProps,
  ColorPickerProps,
  ColumnProps,
  RowProps,
  BoxProps,
  ScrollableColumnProps,
  Arrangement,
  Alignment
} from '../types';
import { colorToCss } from '../types';

/**
 * TextField component
 */
export const TextField: React.FC<TextFieldProps> = ({
  label,
  placeholder,
  value,
  onChange,
  isPassword = false,
  disabled = false,
  error = false,
  helperText,
  multiline = false,
  rows = 4,
  id,
  className,
  style
}) => {
  return (
    <MuiTextField
      id={id}
      label={label}
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange?.(e.target.value)}
      type={isPassword ? 'password' : 'text'}
      disabled={disabled}
      error={error}
      helperText={helperText}
      multiline={multiline}
      rows={multiline ? rows : undefined}
      fullWidth
      className={className}
      style={style}
    />
  );
};

/**
 * Checkbox component
 */
export const Checkbox: React.FC<CheckboxProps> = ({
  label,
  checked = false,
  onChange,
  disabled = false,
  id,
  className,
  style
}) => {
  const checkbox = (
    <MuiCheckbox
      id={id}
      checked={checked}
      onChange={(e) => onChange?.(e.target.checked)}
      disabled={disabled}
      className={className}
      style={style}
    />
  );

  if (label) {
    return <FormControlLabel control={checkbox} label={label} />;
  }

  return checkbox;
};

/**
 * Switch component
 */
export const Switch: React.FC<SwitchProps> = ({
  label,
  checked = false,
  onChange,
  disabled = false,
  id,
  className,
  style
}) => {
  const switchElement = (
    <MuiSwitch
      id={id}
      checked={checked}
      onChange={(e) => onChange?.(e.target.checked)}
      disabled={disabled}
      className={className}
      style={style}
    />
  );

  if (label) {
    return <FormControlLabel control={switchElement} label={label} />;
  }

  return switchElement;
};

/**
 * Card component
 */
export const Card: React.FC<CardProps> = ({
  title,
  subtitle,
  elevation = 1,
  children,
  id,
  className,
  style,
  onClick
}) => {
  return (
    <MuiCard
      id={id}
      elevation={elevation}
      className={className}
      style={style}
      onClick={onClick}
    >
      {(title || subtitle) && (
        <CardHeader
          title={title}
          subheader={subtitle}
        />
      )}
      {children && <CardContent>{children}</CardContent>}
    </MuiCard>
  );
};

/**
 * Dialog component
 */
export const Dialog: React.FC<DialogProps> = ({
  open,
  onClose,
  title,
  children,
  actions,
  fullWidth = false,
  maxWidth = 'sm',
  id,
  className,
  style
}) => {
  return (
    <MuiDialog
      id={id}
      open={open}
      onClose={onClose}
      fullWidth={fullWidth}
      maxWidth={maxWidth}
      className={className}
      style={style}
    >
      {title && <DialogTitle>{title}</DialogTitle>}
      {children && <DialogContent>{children}</DialogContent>}
      {actions && <DialogActions>{actions}</DialogActions>}
    </MuiDialog>
  );
};

/**
 * ListView component
 */
export const ListView: React.FC<ListViewProps> = ({
  items,
  onItemClick,
  id,
  className,
  style
}) => {
  return (
    <List id={id} className={className} style={style}>
      {items.map((item) => (
        <ListItem
          key={item.id}
          button
          onClick={() => {
            item.onClick?.();
            onItemClick?.(item);
          }}
        >
          {item.icon && <ListItemIcon>{item.icon}</ListItemIcon>}
          <ListItemText
            primary={item.primaryText}
            secondary={item.secondaryText}
          />
        </ListItem>
      ))}
    </List>
  );
};

/**
 * Image component
 */
export const Image: React.FC<ImageProps> = ({
  source,
  alt = '',
  width,
  height,
  borderRadius = 0,
  id,
  className,
  style
}) => {
  return (
    <img
      id={id}
      src={source}
      alt={alt}
      width={width}
      height={height}
      className={className}
      style={{
        borderRadius: `${borderRadius}px`,
        ...style
      }}
    />
  );
};

/**
 * Text component
 */
export const Text: React.FC<TextProps> = ({
  children,
  variant = 'bodyMedium',
  color,
  id,
  className,
  style
}) => {
  // Map MagicUI typography variants to HTML elements
  const variantMap: Record<string, keyof JSX.IntrinsicElements> = {
    displayLarge: 'h1',
    displayMedium: 'h1',
    displaySmall: 'h2',
    headlineLarge: 'h2',
    headlineMedium: 'h3',
    headlineSmall: 'h4',
    titleLarge: 'h5',
    titleMedium: 'h6',
    titleSmall: 'h6',
    bodyLarge: 'p',
    bodyMedium: 'p',
    bodySmall: 'p',
    labelLarge: 'span',
    labelMedium: 'span',
    labelSmall: 'span'
  };

  const Element = variantMap[variant] || 'p';

  return React.createElement(
    Element,
    {
      id,
      className,
      style: {
        color: color ? colorToCss(color) : undefined,
        ...style
      }
    },
    children
  );
};

/**
 * ColorPicker component
 */
export const ColorPicker: React.FC<ColorPickerProps> = ({
  value,
  onChange,
  label,
  id,
  className,
  style
}) => {
  const hexValue = `#${value.red.toString(16).padStart(2, '0')}${value.green.toString(16).padStart(2, '0')}${value.blue.toString(16).padStart(2, '0')}`;

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const hex = event.target.value;
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    if (result) {
      onChange({
        red: parseInt(result[1], 16),
        green: parseInt(result[2], 16),
        blue: parseInt(result[3], 16),
        alpha: value.alpha
      });
    }
  };

  return (
    <MuiBox id={id} className={className} style={style}>
      {label && <label>{label}</label>}
      <input
        type="color"
        value={hexValue}
        onChange={handleChange}
        style={{
          width: '100%',
          height: '40px',
          border: 'none',
          borderRadius: '4px',
          cursor: 'pointer'
        }}
      />
    </MuiBox>
  );
};

/**
 * Column component (vertical stack)
 */
export const Column: React.FC<ColumnProps> = ({
  children,
  horizontalAlignment = 'Start',
  verticalArrangement = 'Start',
  fillMaxWidth = false,
  fillMaxHeight = false,
  padding = 0,
  spacing = 0,
  id,
  className,
  style
}) => {
  return (
    <Stack
      id={id}
      direction="column"
      spacing={spacing / 8} // Material-UI spacing is 8px units
      alignItems={convertAlignment(horizontalAlignment)}
      justifyContent={convertArrangement(verticalArrangement)}
      className={className}
      style={{
        width: fillMaxWidth ? '100%' : 'auto',
        height: fillMaxHeight ? '100%' : 'auto',
        padding: `${padding}px`,
        ...style
      }}
    >
      {children}
    </Stack>
  );
};

/**
 * Row component (horizontal stack)
 */
export const Row: React.FC<RowProps> = ({
  children,
  verticalAlignment = 'Start',
  horizontalArrangement = 'Start',
  fillMaxWidth = false,
  fillMaxHeight = false,
  padding = 0,
  spacing = 0,
  id,
  className,
  style
}) => {
  return (
    <Stack
      id={id}
      direction="row"
      spacing={spacing / 8}
      alignItems={convertAlignment(verticalAlignment)}
      justifyContent={convertArrangement(horizontalArrangement)}
      className={className}
      style={{
        width: fillMaxWidth ? '100%' : 'auto',
        height: fillMaxHeight ? '100%' : 'auto',
        padding: `${padding}px`,
        ...style
      }}
    >
      {children}
    </Stack>
  );
};

/**
 * Box component (container)
 */
export const Box: React.FC<BoxProps> = ({
  children,
  width,
  height,
  backgroundColor,
  borderRadius = 0,
  padding = 0,
  elevation = 0,
  id,
  className,
  style
}) => {
  return (
    <MuiBox
      id={id}
      className={className}
      style={{
        width,
        height,
        backgroundColor: backgroundColor ? colorToCss(backgroundColor) : undefined,
        borderRadius: `${borderRadius}px`,
        padding: `${padding}px`,
        boxShadow: elevation > 0 ? `0px ${elevation}px ${elevation * 2}px rgba(0,0,0,0.2)` : 'none',
        ...style
      }}
    >
      {children}
    </MuiBox>
  );
};

/**
 * ScrollableColumn component
 */
export const ScrollableColumn: React.FC<ScrollableColumnProps> = ({
  children,
  fillMaxWidth = false,
  fillMaxHeight = false,
  padding = 0,
  id,
  className,
  style
}) => {
  return (
    <MuiBox
      id={id}
      className={className}
      style={{
        width: fillMaxWidth ? '100%' : 'auto',
        height: fillMaxHeight ? '100%' : 'auto',
        padding: `${padding}px`,
        overflowY: 'auto',
        ...style
      }}
    >
      {children}
    </MuiBox>
  );
};

/**
 * Helper: Convert MagicUI alignment to CSS alignment
 */
function convertAlignment(alignment: Alignment | string): 'flex-start' | 'center' | 'flex-end' | 'stretch' {
  switch (alignment) {
    case 'Start':
      return 'flex-start';
    case 'Center':
      return 'center';
    case 'End':
      return 'flex-end';
    case 'Stretch':
      return 'stretch';
    default:
      return 'flex-start';
  }
}

/**
 * Helper: Convert MagicUI arrangement to CSS justify-content
 */
function convertArrangement(arrangement: Arrangement | string): 'flex-start' | 'center' | 'flex-end' | 'space-between' | 'space-around' | 'space-evenly' {
  switch (arrangement) {
    case 'Start':
      return 'flex-start';
    case 'Center':
      return 'center';
    case 'End':
      return 'flex-end';
    case 'SpaceBetween':
      return 'space-between';
    case 'SpaceAround':
      return 'space-around';
    case 'SpaceEvenly':
      return 'space-evenly';
    default:
      return 'flex-start';
  }
}
