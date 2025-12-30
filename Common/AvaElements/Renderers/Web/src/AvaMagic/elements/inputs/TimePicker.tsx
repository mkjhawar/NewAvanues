/**
 * TimePicker Component - Phase 3 Input Component
 *
 * Time selection input
 * Matches Android/iOS TimePicker behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import { TextField } from '@mui/material';

export interface TimePickerProps {
  /** Selected time value (HH:mm) */
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
  /** Variant */
  variant?: 'outlined' | 'filled' | 'standard';
  /** Full width */
  fullWidth?: boolean;
  /** Size */
  size?: 'small' | 'medium';
  /** Custom class name */
  className?: string;
}

export const TimePicker: React.FC<TimePickerProps> = ({
  value,
  onChange,
  label,
  helperText,
  error = false,
  disabled = false,
  required = false,
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
      type="time"
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
    />
  );
};

export default TimePicker;
