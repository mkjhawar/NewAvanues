import React from 'react';
import { Box } from '@mui/material';
import { RowProps } from '../types';

export const Row: React.FC<RowProps> = ({
  children,
  spacing = 2,
  alignItems = 'center',
  justifyContent = 'flex-start',
  sx,
  ...props
}) => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'row',
        gap: spacing,
        alignItems,
        justifyContent,
        ...sx
      }}
      {...props}
    >
      {children}
    </Box>
  );
};
