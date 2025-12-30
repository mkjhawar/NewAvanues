/**
 * Slider Component - Phase 3 Input Component
 *
 * Select value from range
 * Matches Android/iOS Slider behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Slider as MuiSlider, Box, Typography } from '@mui/material';

export interface SliderProps {
  /** Current value */
  value: number;
  /** Change handler */
  onChange: (value: number) => void;
  /** Minimum value */
  min?: number;
  /** Maximum value */
  max?: number;
  /** Step increment */
  step?: number;
  /** Show marks */
  marks?: boolean | { value: number; label?: string }[];
  /** Show value label */
  showLabel?: boolean;
  /** Label */
  label?: string;
  /** Disabled state */
  disabled?: boolean;
  /** Color */
  color?: 'primary' | 'secondary';
  /** Vertical orientation */
  vertical?: boolean;
  /** Height for vertical slider */
  height?: number;
  /** Custom class name */
  className?: string;
}

export const Slider: React.FC<SliderProps> = ({
  value,
  onChange,
  min = 0,
  max = 100,
  step = 1,
  marks = false,
  showLabel = true,
  label,
  disabled = false,
  color = 'primary',
  vertical = false,
  height = 300,
  className,
}) => {
  const handleChange = (_event: Event, newValue: number | number[]) => {
    onChange(newValue as number);
  };

  return (
    <Box className={className} sx={{ width: '100%' }}>
      {label && (
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {label}
        </Typography>
      )}
      <MuiSlider
        value={value}
        onChange={handleChange}
        min={min}
        max={max}
        step={step}
        marks={marks}
        valueLabelDisplay={showLabel ? 'auto' : 'off'}
        disabled={disabled}
        color={color}
        orientation={vertical ? 'vertical' : 'horizontal'}
        sx={{
          height: vertical ? height : undefined,
        }}
      />
    </Box>
  );
};

export default Slider;
