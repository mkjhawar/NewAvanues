/**
 * Rating Component - Phase 3 Input Component
 *
 * Star rating input
 * Matches Android/iOS Rating behavior
 *
 * @package com.augmentalis.AvaMagic.elements.inputs
 * @since 3.0.0-phase3
 */

import React from 'react';
import { Rating as MuiRating, Box, Typography } from '@mui/material';
import StarIcon from '@mui/icons-material/Star';

export interface RatingProps {
  /** Current value */
  value: number;
  /** Change handler */
  onChange: (value: number) => void;
  /** Maximum rating */
  max?: number;
  /** Precision (0.5 for half stars) */
  precision?: number;
  /** Size */
  size?: 'small' | 'medium' | 'large';
  /** Read only */
  readOnly?: boolean;
  /** Disabled state */
  disabled?: boolean;
  /** Show label */
  showLabel?: boolean;
  /** Label text */
  label?: string;
  /** Custom icon */
  icon?: React.ReactElement;
  /** Custom empty icon */
  emptyIcon?: React.ReactElement;
  /** Custom class name */
  className?: string;
}

export const Rating: React.FC<RatingProps> = ({
  value,
  onChange,
  max = 5,
  precision = 1,
  size = 'medium',
  readOnly = false,
  disabled = false,
  showLabel = false,
  label,
  icon,
  emptyIcon,
  className,
}) => {
  const handleChange = (_event: React.SyntheticEvent, newValue: number | null) => {
    if (newValue !== null) {
      onChange(newValue);
    }
  };

  return (
    <Box className={className} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      {label && (
        <Typography variant="body2" color="text.secondary">
          {label}
        </Typography>
      )}
      <MuiRating
        value={value}
        onChange={handleChange}
        max={max}
        precision={precision}
        size={size}
        readOnly={readOnly}
        disabled={disabled}
        icon={icon}
        emptyIcon={emptyIcon}
      />
      {showLabel && (
        <Typography variant="body2" color="text.secondary">
          {value} / {max}
        </Typography>
      )}
    </Box>
  );
};

export default Rating;
