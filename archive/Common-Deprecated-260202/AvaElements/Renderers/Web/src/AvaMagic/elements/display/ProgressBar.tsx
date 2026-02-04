/**
 * ProgressBar Component - Phase 3 Display Component
 *
 * Linear progress indicator for loading states
 * Matches Android/iOS ProgressBar behavior
 *
 * @package com.augmentalis.AvaMagic.elements.display
 * @since 3.0.0-phase3
 */

import React from 'react';
import { LinearProgress, Box, Typography } from '@mui/material';

export interface ProgressBarProps {
  /** Current value (0-100) */
  value?: number;
  /** Determinate or indeterminate mode */
  variant?: 'determinate' | 'indeterminate' | 'buffer' | 'query';
  /** Buffer value for buffer variant */
  valueBuffer?: number;
  /** Height in pixels */
  height?: number;
  /** Primary color */
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' | 'inherit';
  /** Show percentage label */
  showLabel?: boolean;
  /** Label position */
  labelPosition?: 'top' | 'bottom' | 'inline';
  /** Custom label formatter */
  labelFormatter?: (value: number) => string;
  /** Accessibility label */
  accessibilityLabel?: string;
  /** Custom class name */
  className?: string;
}

export const ProgressBar: React.FC<ProgressBarProps> = ({
  value = 0,
  variant = 'determinate',
  valueBuffer,
  height = 4,
  color = 'primary',
  showLabel = false,
  labelPosition = 'top',
  labelFormatter,
  accessibilityLabel,
  className,
}) => {
  const labelText = labelFormatter
    ? labelFormatter(value)
    : `${Math.round(value)}%`;

  const progressBar = (
    <LinearProgress
      variant={variant}
      value={value}
      valueBuffer={valueBuffer}
      color={color}
      className={className}
      aria-label={accessibilityLabel}
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={100}
      sx={{
        height,
        borderRadius: height / 2,
      }}
    />
  );

  if (!showLabel || variant === 'indeterminate') {
    return progressBar;
  }

  return (
    <Box sx={{ width: '100%' }}>
      {labelPosition === 'top' && (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
          <Typography variant="body2" color="text.secondary">
            {labelText}
          </Typography>
        </Box>
      )}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        {labelPosition === 'inline' && (
          <Typography variant="body2" color="text.secondary" sx={{ minWidth: 45 }}>
            {labelText}
          </Typography>
        )}
        <Box sx={{ flex: 1 }}>{progressBar}</Box>
      </Box>
      {labelPosition === 'bottom' && (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 0.5 }}>
          <Typography variant="body2" color="text.secondary">
            {labelText}
          </Typography>
        </Box>
      )}
    </Box>
  );
};

export default ProgressBar;
