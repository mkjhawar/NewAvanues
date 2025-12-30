import React from 'react';
import { CircularProgress, CircularProgressProps, Box } from '@mui/material';

/**
 * MagicSpinner - React/Material-UI Spinner Component
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

export enum SpinnerSize {
  SMALL = 'small',
  MEDIUM = 'medium',
  LARGE = 'large'
}

export interface MagicSpinnerProps {
  size?: SpinnerSize;
  color?: string;
  className?: string;
}

export const MagicSpinner: React.FC<MagicSpinnerProps> = ({
  size = SpinnerSize.MEDIUM,
  color,
  className
}) => {
  const sizeMap = {
    [SpinnerSize.SMALL]: 20,
    [SpinnerSize.MEDIUM]: 40,
    [SpinnerSize.LARGE]: 60
  };

  const progressProps: CircularProgressProps = {
    size: sizeMap[size],
    className,
    sx: { color }
  };

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
      <CircularProgress {...progressProps} />
    </Box>
  );
};

export default MagicSpinner;
