/**
 * RangeSlider Component - Phase 3 Input Component
 *
 * Select range with two handles
 * Matches Android/iOS RangeSlider behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Slider, Box, Typography } from '@mui/material';

export interface RangeSliderProps {
  /** Current range [min, max] */
  value: [number, number];
  /** Change handler */
  onChange: (value: [number, number]) => void;
  /** Minimum value */
  min?: number;
  /** Maximum value */
  max?: number;
  /** Step increment */
  step?: number;
  /** Show value labels */
  showLabels?: boolean;
  /** Label */
  label?: string;
  /** Disabled state */
  disabled?: boolean;
  /** Color */
  color?: 'primary' | 'secondary';
  /** Custom class name */
  className?: string;
}

export const RangeSlider: React.FC<RangeSliderProps> = ({
  value,
  onChange,
  min = 0,
  max = 100,
  step = 1,
  showLabels = true,
  label,
  disabled = false,
  color = 'primary',
  className,
}) => {
  const handleChange = (_event: Event, newValue: number | number[]) => {
    onChange(newValue as [number, number]);
  };

  return (
    <Box className={className} sx={{ width: '100%' }}>
      {label && (
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {label}
        </Typography>
      )}
      <Slider
        value={value}
        onChange={handleChange}
        min={min}
        max={max}
        step={step}
        valueLabelDisplay={showLabels ? 'auto' : 'off'}
        disabled={disabled}
        color={color}
        disableSwap
      />
    </Box>
  );
};

export default RangeSlider;
