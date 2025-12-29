import React from 'react';
import { CircularProgress as MuiCircularProgress, Box, Typography } from '@mui/material';

export interface CircularProgressProps {
  value?: number;
  size?: number;
  thickness?: number;
  color?: 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' | 'inherit';
  showLabel?: boolean;
}

export const CircularProgress: React.FC<CircularProgressProps> = ({
  value,
  size = 40,
  thickness = 3.6,
  color = 'primary',
  showLabel = false,
}) => {
  const isDeterminate = value !== undefined;

  if (isDeterminate && showLabel) {
    return (
      <Box sx={{ position: 'relative', display: 'inline-flex' }}>
        <MuiCircularProgress
          variant="determinate"
          value={value}
          size={size}
          thickness={thickness}
          color={color}
        />
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
          <Typography
            variant="caption"
            component="div"
            color="text.secondary"
          >
            {`${Math.round(value)}%`}
          </Typography>
        </Box>
      </Box>
    );
  }

  return (
    <MuiCircularProgress
      variant={isDeterminate ? 'determinate' : 'indeterminate'}
      value={value}
      size={size}
      thickness={thickness}
      color={color}
    />
  );
};

export default CircularProgress;
