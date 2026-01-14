/**
 * Spinner Component - Phase 3 Display Component
 *
 * Circular loading indicator
 * Matches Android/iOS Spinner behavior
 *
 * @package com.augmentalis.AvaMagic.elements.display
 * @since 3.0.0-phase3
 */

import React from 'react';
import { CircularProgress, Box } from '@mui/material';

export interface SpinnerProps {
  /** Size in pixels */
  size?: number | 'small' | 'medium' | 'large';
  /** Color */
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' | 'inherit';
  /** Thickness of the circle */
  thickness?: number;
  /** Determinate or indeterminate */
  variant?: 'determinate' | 'indeterminate';
  /** Value for determinate mode (0-100) */
  value?: number;
  /** Center on screen */
  centered?: boolean;
  /** Accessibility label */
  accessibilityLabel?: string;
  /** Custom class name */
  className?: string;
}

const getSizeValue = (size?: number | 'small' | 'medium' | 'large'): number => {
  if (typeof size === 'number') return size;
  switch (size) {
    case 'small': return 20;
    case 'large': return 60;
    case 'medium':
    default: return 40;
  }
};

export const Spinner: React.FC<SpinnerProps> = ({
  size = 'medium',
  color = 'primary',
  thickness = 3.6,
  variant = 'indeterminate',
  value,
  centered = false,
  accessibilityLabel,
  className,
}) => {
  const sizeValue = getSizeValue(size);

  const spinner = (
    <CircularProgress
      size={sizeValue}
      color={color}
      thickness={thickness}
      variant={variant}
      value={value}
      className={className}
      aria-label={accessibilityLabel || 'Loading'}
    />
  );

  if (centered) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          width: '100%',
          height: '100%',
          minHeight: 200,
        }}
      >
        {spinner}
      </Box>
    );
  }

  return spinner;
};

export default Spinner;
