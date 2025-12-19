/**
 * DatePicker Component - Phase 3 Input Component
 *
 * Date selection input
 * Matches Android/iOS DatePicker behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import { TextField, TextFieldProps } from '@mui/material';

export interface DatePickerProps {
  /** Selected date value */
  value: string;
  /** Change handler */
  onChange: (value: string) => void;
  /** Label */
  label?: string;
  /** Helper text */
  helperText?: string;
  /** Error state */
  error?: boolean;
  /** Disabled state */
  disabled?: boolean;
  /** Required field */
  required?: boolean;
  /** Minimum date (YYYY-MM-DD) */
  min?: string;
  /** Maximum date (YYYY-MM-DD) */
  max?: string;
  /** Variant */
  variant?: 'outlined' | 'filled' | 'standard';
  /** Full width */
  fullWidth?: boolean;
  /** Size */
  size?: 'small' | 'medium';
  /** Custom class name */
  className?: string;
}

export const DatePicker: React.FC<DatePickerProps> = ({
  value,
  onChange,
  label,
  helperText,
  error = false,
  disabled = false,
  required = false,
  min,
  max,
  variant = 'outlined',
  fullWidth = true,
  size = 'medium',
  className,
}) => {
  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    onChange(event.target.value);
  };

  return (
    <TextField
      type="date"
      value={value}
      onChange={handleChange}
      label={label}
      helperText={helperText}
      error={error}
      disabled={disabled}
      required={required}
      variant={variant}
      fullWidth={fullWidth}
      size={size}
      className={className}
      InputLabelProps={{
        shrink: true,
      }}
      inputProps={{
        min,
        max,
      }}
    />
  );
};

export default DatePicker;
