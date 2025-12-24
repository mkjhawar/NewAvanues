import React from 'react';
import { CircularProgress, Box } from '@mui/material';
import { SpinnerProps } from '../types';

export const Spinner: React.FC<SpinnerProps> = ({
  size = 40,
  color = 'primary',
  thickness = 3.6,
  centered = false,
  sx,
  ...props
}) => {
  const spinner = (
    <CircularProgress
      size={size}
      color={color}
      thickness={thickness}
      sx={sx}
      {...props}
    />
  );

  if (centered) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: 100
        }}
      >
        {spinner}
      </Box>
    );
  }

  return spinner;
};
