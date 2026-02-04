/**
 * Dropdown Component - Phase 3 Input Component
 *
 * Select from list of options
 * Matches Android/iOS Dropdown behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormHelperText,
  SelectChangeEvent,
} from '@mui/material';

export interface DropdownOption {
  /** Option value */
  value: string | number;
  /** Display label */
  label: string;
  /** Disabled state */
  disabled?: boolean;
}

export interface DropdownProps {
  /** Selected value */
  value: string | number;
  /** Change handler */
  onChange: (value: string | number) => void;
  /** Options list */
  options: DropdownOption[];
  /** Label */
  label?: string;
  /** Placeholder */
  placeholder?: string;
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

export const Dropdown: React.FC<DropdownProps> = ({
  value,
  onChange,
  options,
  label,
  placeholder,
  helperText,
  error = false,
  disabled = false,
  required = false,
  variant = 'outlined',
  fullWidth = true,
  size = 'medium',
  className,
}) => {
  const handleChange = (event: SelectChangeEvent<string | number>) => {
    onChange(event.target.value);
  };

  return (
    <FormControl
      variant={variant}
      fullWidth={fullWidth}
      error={error}
      disabled={disabled}
      required={required}
      size={size}
      className={className}
    >
      {label && <InputLabel>{label}</InputLabel>}
      <Select
        value={value}
        onChange={handleChange}
        label={label}
        displayEmpty={!!placeholder}
      >
        {placeholder && (
          <MenuItem value="" disabled>
            {placeholder}
          </MenuItem>
        )}
        {options.map((option) => (
          <MenuItem
            key={option.value}
            value={option.value}
            disabled={option.disabled}
          >
            {option.label}
          </MenuItem>
        ))}
      </Select>
      {helperText && <FormHelperText>{helperText}</FormHelperText>}
    </FormControl>
  );
};

export default Dropdown;
