/**
 * RadioGroup Component - Phase 3 Input Component
 *
 * Group of radio buttons
 * Matches Android/iOS RadioGroup behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import {
  RadioGroup as MuiRadioGroup,
  FormControl,
  FormLabel,
  FormControlLabel,
  Radio,
  FormHelperText,
} from '@mui/material';

export interface RadioGroupOption {
  /** Option value */
  value: string;
  /** Display label */
  label: string;
  /** Disabled state */
  disabled?: boolean;
}

export interface RadioGroupProps {
  /** Selected value */
  value: string;
  /** Change handler */
  onChange: (value: string) => void;
  /** Options list */
  options: RadioGroupOption[];
  /** Group label */
  label?: string;
  /** Helper text */
  helperText?: string;
  /** Error state */
  error?: boolean;
  /** Disabled state */
  disabled?: boolean;
  /** Required field */
  required?: boolean;
  /** Layout direction */
  row?: boolean;
  /** Color */
  color?: 'primary' | 'secondary' | 'default';
  /** Size */
  size?: 'small' | 'medium';
  /** Custom class name */
  className?: string;
}

export const RadioGroup: React.FC<RadioGroupProps> = ({
  value,
  onChange,
  options,
  label,
  helperText,
  error = false,
  disabled = false,
  required = false,
  row = false,
  color = 'primary',
  size = 'medium',
  className,
}) => {
  const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    onChange(event.target.value);
  };

  return (
    <FormControl
      error={error}
      disabled={disabled}
      required={required}
      className={className}
    >
      {label && <FormLabel>{label}</FormLabel>}
      <MuiRadioGroup value={value} onChange={handleChange} row={row}>
        {options.map((option) => (
          <FormControlLabel
            key={option.value}
            value={option.value}
            control={<Radio color={color} size={size} />}
            label={option.label}
            disabled={option.disabled || disabled}
          />
        ))}
      </MuiRadioGroup>
      {helperText && <FormHelperText>{helperText}</FormHelperText>}
    </FormControl>
  );
};

export default RadioGroup;
