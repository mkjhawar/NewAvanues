/**
 * RadioButton Component - Phase 3 Input Component
 *
 * Single selection radio button
 * Matches Android/iOS RadioButton behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Radio, FormControlLabel } from '@mui/material';

export interface RadioButtonProps {
  /** Radio value */
  value: string;
  /** Label text */
  label?: string;
  /** Checked state */
  checked: boolean;
  /** Change handler */
  onChange: (value: string) => void;
  /** Disabled state */
  disabled?: boolean;
  /** Color */
  color?: 'primary' | 'secondary' | 'default';
  /** Size */
  size?: 'small' | 'medium';
  /** Label placement */
  labelPlacement?: 'end' | 'start' | 'top' | 'bottom';
  /** Custom class name */
  className?: string;
}

export const RadioButton: React.FC<RadioButtonProps> = ({
  value,
  label,
  checked,
  onChange,
  disabled = false,
  color = 'primary',
  size = 'medium',
  labelPlacement = 'end',
  className,
}) => {
  const handleChange = () => {
    if (!disabled) {
      onChange(value);
    }
  };

  const radio = (
    <Radio
      checked={checked}
      onChange={handleChange}
      value={value}
      disabled={disabled}
      color={color}
      size={size}
    />
  );

  if (!label) {
    return radio;
  }

  return (
    <FormControlLabel
      control={radio}
      label={label}
      labelPlacement={labelPlacement}
      disabled={disabled}
      className={className}
    />
  );
};

export default RadioButton;
