import React from 'react';
import { Box } from '@mui/material';
import { ColumnProps } from '../types';

export const Column: React.FC<ColumnProps> = ({
  children,
  spacing = 2,
  alignItems = 'stretch',
  justifyContent = 'flex-start',
  sx,
  ...props
}) => {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
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
