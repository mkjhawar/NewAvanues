import React from 'react';
import { LinearProgress, Box, Typography } from '@mui/material';
import { ProgressBarProps } from '../types';

export const ProgressBar: React.FC<ProgressBarProps> = ({
  value = 0,
  variant = 'determinate',
  color = 'primary',
  showLabel = false,
  label,
  sx,
  ...props
}) => {
  const progress = Math.min(100, Math.max(0, value));

  return (
    <Box sx={{ width: '100%', ...sx }}>
      {(showLabel || label) && (
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
          <Typography variant="body2" color="text.secondary">
            {label || `${Math.round(progress)}%`}
          </Typography>
        </Box>
      )}
      <LinearProgress
        variant={variant}
        value={progress}
        color={color}
        {...props}
      />
    </Box>
  );
};
