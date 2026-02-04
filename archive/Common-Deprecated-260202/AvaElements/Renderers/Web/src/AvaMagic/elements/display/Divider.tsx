/**
 * Divider Component - Phase 3 Display Component
 *
 * Visual separator for content sections
 * Matches Android/iOS Divider behavior
 *
 * @package com.augmentalis.AvaMagic.elements.display
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Divider as MuiDivider } from '@mui/material';

export interface DividerProps {
  /** Orientation */
  orientation?: 'horizontal' | 'vertical';
  /** Variant style */
  variant?: 'fullWidth' | 'inset' | 'middle';
  /** Thickness in pixels */
  thickness?: number;
  /** Color */
  color?: string;
  /** Spacing around divider */
  spacing?: number;
  /** Text content for labeled divider */
  label?: string;
  /** Label alignment */
  labelAlign?: 'left' | 'center' | 'right';
  /** Custom class name */
  className?: string;
}

export const Divider: React.FC<DividerProps> = ({
  orientation = 'horizontal',
  variant = 'fullWidth',
  thickness = 1,
  color,
  spacing,
  label,
  labelAlign = 'center',
  className,
}) => {
  return (
    <MuiDivider
      orientation={orientation}
      variant={variant}
      textAlign={labelAlign}
      className={className}
      sx={{
        borderBottomWidth: thickness,
        borderRightWidth: thickness,
        borderColor: color || 'divider',
        my: spacing || (orientation === 'horizontal' ? 1 : 0),
        mx: spacing || (orientation === 'vertical' ? 1 : 0),
      }}
    >
      {label}
    </MuiDivider>
  );
};

export default Divider;
