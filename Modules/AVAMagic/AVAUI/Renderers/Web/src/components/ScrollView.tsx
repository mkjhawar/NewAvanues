import React from 'react';
import { Box } from '@mui/material';
import { ScrollViewProps } from '../types';

export const ScrollView: React.FC<ScrollViewProps> = ({
  children,
  orientation = 'vertical',
  maxHeight,
  maxWidth,
  sx,
  ...props
}) => {
  const isHorizontal = orientation === 'horizontal';

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: isHorizontal ? 'row' : 'column',
        overflowX: isHorizontal ? 'auto' : 'hidden',
        overflowY: isHorizontal ? 'hidden' : 'auto',
        maxHeight: isHorizontal ? undefined : maxHeight,
        maxWidth: isHorizontal ? maxWidth : undefined,
        ...sx
      }}
      {...props}
    >
      {children}
    </Box>
  );
};
