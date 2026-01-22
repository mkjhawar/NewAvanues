import React from 'react';
import { CircularProgress, Box, Typography } from '@mui/material';

export interface ProgressCircleProps {
  value?: number;
  max?: number;
  size?: 'small' | 'medium' | 'large';
  showLabel?: boolean;
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
}

export const ProgressCircle: React.FC<ProgressCircleProps> = ({
  value,
  max = 100,
  size = 'medium',
  showLabel = false,
  color = 'primary',
}) => {
  const sizeMap = {
    small: 24,
    medium: 40,
    large: 56,
  };

  const percentage = value !== undefined ? (value / max) * 100 : undefined;

  return (
    <Box sx={{ position: 'relative', display: 'inline-flex' }}>
      <CircularProgress
        variant={value !== undefined ? 'determinate' : 'indeterminate'}
        value={percentage}
        size={sizeMap[size]}
        color={color}
      />
      {showLabel && value !== undefined && (
        <Box
          sx={{
            top: 0,
            left: 0,
            bottom: 0,
            right: 0,
            position: 'absolute',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Typography variant="caption" component="div" color="text.secondary">
            {`${Math.round(percentage!)}%`}
          </Typography>
        </Box>
      )}
    </Box>
  );
};

export default ProgressCircle;
