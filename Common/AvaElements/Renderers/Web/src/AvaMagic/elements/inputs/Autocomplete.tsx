/**
 * Autocomplete Component - Phase 3 Input Component
 *
 * Search with suggestions
 * Matches Android/iOS Autocomplete behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Autocomplete as MuiAutocomplete, TextField } from '@mui/material';

export interface AutocompleteOption {
  /** Option value */
  value: string;
  /** Display label */
  label: string;
}

export interface AutocompleteProps {
  /** Selected value */
  value: string | null;
  /** Change handler */
  onChange: (value: string | null) => void;
  /** Options list */
  options: AutocompleteOption[];
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
  /** Allow free text */
  freeSolo?: boolean;
  /** Multiple selection */
  multiple?: boolean;
  /** Loading state */
  loading?: boolean;
  /** Variant */
  variant?: 'outlined' | 'filled' | 'standard';
  /** Full width */
  fullWidth?: boolean;
  /** Size */
  size?: 'small' | 'medium';
  /** Custom class name */
  className?: string;
}

export const Autocomplete: React.FC<AutocompleteProps> = ({
  value,
  onChange,
  options,
  label,
  placeholder,
  helperText,
  error = false,
  disabled = false,
  required = false,
  freeSolo = false,
  multiple = false,
  loading = false,
  variant = 'outlined',
  fullWidth = true,
  size = 'medium',
  className,
}) => {
  const handleChange = (_event: any, newValue: AutocompleteOption | string | null) => {
    if (typeof newValue === 'string') {
      onChange(newValue);
    } else {
      onChange(newValue?.value || null);
    }
  };

  const selectedOption = options.find(opt => opt.value === value) || null;

  return (
    <MuiAutocomplete
      value={selectedOption}
      onChange={handleChange}
      options={options}
      getOptionLabel={(option) =>
        typeof option === 'string' ? option : option.label
      }
      freeSolo={freeSolo}
      disabled={disabled}
      loading={loading}
      className={className}
      renderInput={(params) => (
        <TextField
          {...params}
          label={label}
          placeholder={placeholder}
          helperText={helperText}
          error={error}
          required={required}
          variant={variant}
          fullWidth={fullWidth}
          size={size}
        />
      )}
    />
  );
};

export default Autocomplete;
